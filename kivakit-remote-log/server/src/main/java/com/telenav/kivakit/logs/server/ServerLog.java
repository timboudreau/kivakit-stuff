////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
//
// © 2011-2021 Telenav, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
// https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

package com.telenav.kivakit.logs.server;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.component.ComponentMixin;
import com.telenav.kivakit.core.collections.map.VariableMap;
import com.telenav.kivakit.core.logging.LogEntry;
import com.telenav.kivakit.core.logging.loggers.ConsoleLogger;
import com.telenav.kivakit.core.logging.logs.text.BaseTextLog;
import com.telenav.kivakit.core.messaging.Listener;
import com.telenav.kivakit.core.object.Lazy;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.core.progress.reporters.ProgressiveInputStream;
import com.telenav.kivakit.core.progress.reporters.ProgressiveOutputStream;
import com.telenav.kivakit.core.thread.KivaKitThread;
import com.telenav.kivakit.core.thread.Monitor;
import com.telenav.kivakit.core.time.Duration;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.core.version.Version;
import com.telenav.kivakit.core.version.VersionedObject;
import com.telenav.kivakit.core.vm.JavaVirtualMachineHealth;
import com.telenav.kivakit.core.vm.ShutdownHook;
import com.telenav.kivakit.logs.server.session.Session;
import com.telenav.kivakit.logs.server.session.SessionStore;
import com.telenav.kivakit.network.socket.server.ConnectionListener;
import com.telenav.kivakit.resource.serialization.SerializableObject;
import com.telenav.kivakit.serialization.core.SerializationSession;
import com.telenav.kivakit.serialization.core.SerializationSessionFactory;
import com.telenav.kivakit.service.registry.Scope;
import com.telenav.kivakit.service.registry.ServiceMetadata;
import com.telenav.kivakit.service.registry.ServiceType;
import com.telenav.kivakit.service.registry.client.ServiceRegistryClient;

import java.io.IOException;
import java.net.Socket;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import static com.telenav.kivakit.core.ensure.Ensure.fail;
import static com.telenav.kivakit.core.project.Project.resolveProject;
import static com.telenav.kivakit.core.time.Duration.FOREVER;
import static com.telenav.kivakit.core.vm.ShutdownHook.Order.LAST;
import static com.telenav.kivakit.serialization.core.SerializationSession.SessionType.CLIENT;
import static com.telenav.kivakit.serialization.core.SerializationSession.SessionType.SERVER;

@SuppressWarnings("UnusedReturnValue")
public class ServerLog extends BaseTextLog implements ComponentMixin
{
    public static final ServiceType SERVER_LOG = new ServiceType("kivakit-server-log");

    private static final ConsoleLogger LOGGER = new ConsoleLogger();

    private static final Lazy<ServerLog> singleton = Lazy.of(ServerLog::new);

    public static ServerLog get()
    {
        return singleton.get();
    }

    private final LinkedList<LogEntry> entries = new LinkedList<>();

    private Maximum maximumEntries = Maximum.maximum(20_000);

    private int port;

    private final Monitor serializationLock = new Monitor();

    private SerializationSession serializer;

    private final Time started = Time.now();

    private final Lazy<Session> session = Lazy.of(() ->
    {
        var application = Application.get();
        if (application != null)
        {
            var session = new Session(application.name(), started, null);
            SessionStore.get().add(session);
            return session;
        }
        return null;
    });

    public ServerLog()
    {
        ShutdownHook.register(LAST, () -> SessionStore.get().save(session.get()));

        resolveProject(ServerLogProject.class).initialize();

        var client = LOGGER.listenTo(new ServiceRegistryClient());
        var metadata = new ServiceMetadata()
                .kivakitVersion(kivakitVersion())
                .description("KivaKit server log")
                .version(projectVersion());
        var service = client.register(Scope.network(), SERVER_LOG, metadata);
        if (service.failed())
        {
            fail("Unable to register server log: $", service.get());
        }
        else
        {
            port = service.get().port().number();
        }
    }

    @Override
    public void configure(VariableMap<String> properties)
    {
        super.configure(properties);

        var maximum = properties.get("maximum-entries");
        if (maximum != null)
        {
            maximumEntries = Maximum.parseMaximum(Listener.consoleListener(), maximum);
        }
        listen(BroadcastingProgressReporter.create(LOGGER, "bytes"));
    }

    public ServerLog listen(ProgressReporter reporter)
    {
        LOGGER.listenTo(new ConnectionListener(port(), Maximum.maximum(8))).listen(socket -> handleRequest(socket, reporter));
        return this;
    }

    @Override
    public String name()
    {
        return "Server";
    }

    @Override
    public synchronized void onLog(LogEntry newEntry)
    {
        // While there are too many log entries
        while (entries.size() > maximumEntries.asInt())
        {
            // remove the first one
            entries.removeFirst();
        }

        // then add the new entry
        entries.add(newEntry);
        var store = SessionStore.get();
        store.addAll(session.get(), Collections.singletonList(newEntry));

        // and if there is a serializer to write to
        synchronized (serializationLock)
        {
            if (serializer != null)
            {
                // go through each entry to send
                for (var entry : entries)
                {
                    try
                    {
                        // format the message because we can't serialize arbitrary objects,
                        entry.formattedMessage();

                        // and send the entry
                        serializer.write(new SerializableObject<>(entry, projectVersion()));
                        serializer.flush(FOREVER);
                    }
                    catch (Exception e)
                    {
                        LOGGER.warning(e, "Unable to send log entry $", entry);
                        closeConnection();
                        return;
                    }
                }
            }

            // Entries have been sent so clear the list
            entries.clear();
        }
    }

    public int port()
    {
        return port;
    }

    public void synchronizeSessions(SerializationSession serializationSession, ProgressReporter reporter)
    {
        // Send the sessions we have to the client
        serializationSession.write(new SerializableObject<>(SessionStore.get().sessions(), kivakit().projectVersion()));

        // then read back the sessions that the client wants,
        VersionedObject<List<Session>> sessionsToSend = serializationSession.read();

        // then send each desired session back to the client
        for (var session : sessionsToSend.object())
        {
            serializationSession.write(new SerializableObject<>(SessionStore.get().read(session, reporter), kivakit().projectVersion()));
        }
    }

    private void closeConnection()
    {
        synchronized (serializationLock)
        {
            if (serializer != null)
            {
                serializer.close();
                serializer = null;
            }
        }
    }

    /**
     * @param socket The server socket
     * @param reporter The reporter to call during connection and synchronization of a new session. This can take a
     * while because the client can download logs.
     */
    private void handleRequest(Socket socket, ProgressReporter reporter)
    {
        // Close any existing connection,
        closeConnection();

        try
        {
            // then open the socket output stream
            var input = socket.getInputStream();
            var output = socket.getOutputStream();
            if (input != null && output != null)
            {
                // layer in progress reporting
                input = new ProgressiveInputStream(input, reporter);
                output = new ProgressiveOutputStream(output, reporter);

                // get the set of sessions the log has stored,
                var store = SessionStore.get();
                store.load();

                synchronized (serializationLock)
                {
                    // Create a serializer and start writing to the connection
                    var serializer = require(SerializationSessionFactory.class).newSession(this);
                    serializer.open(input, CLIENT);
                    serializer.open(output, SERVER, kivakit().kivakitVersion());

                    // then send the client our application name
                    serializer.write(new SerializableObject<>(Application.get().name(), Application.get().version()));

                    // and synchronize sessions with it
                    synchronizeSessions(serializer, reporter);

                    // then flush the serializer
                    serializer.flush(FOREVER);

                    // tell the progress reporter that the initialization process is done
                    reporter.end();

                    // Next, make the serializer available to the log for sending entries.
                    this.serializer = serializer;

                    // and start the health reporting thread
                    LOGGER.listenTo(new KivaKitThread("Health", () ->
                    {
                        // that loops
                        while (true)
                        {
                            // and every 15 seconds
                            Duration.seconds(15).sleep();
                            try
                            {
                                synchronized (serializationLock)
                                {
                                    // sends a health report on the JVM
                                    serializer.write(new SerializableObject<>(new JavaVirtualMachineHealth(), Application.get().version()));
                                }
                            }
                            catch (Exception e)
                            {
                                break;
                            }
                        }
                    })).start();
                }
            }
        }
        catch (IOException e)
        {
            LOGGER.warning(e, "Socket connection failed");
        }
    }

    private Version projectVersion()
    {
        return project(ServerLogProject.class).projectVersion();
    }
}
