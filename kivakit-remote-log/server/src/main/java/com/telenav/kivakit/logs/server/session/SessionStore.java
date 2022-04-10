package com.telenav.kivakit.logs.server.session;

import com.telenav.kivakit.component.BaseComponent;
import com.telenav.kivakit.conversion.core.time.LocalDateTimeConverter;
import com.telenav.kivakit.core.logging.LogEntry;
import com.telenav.kivakit.core.object.Lazy;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.core.string.Strings;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.logs.server.ServerLogProject;
import com.telenav.kivakit.resource.Extension;
import com.telenav.kivakit.resource.serialization.SerializableObject;
import com.telenav.kivakit.serialization.core.SerializationSession;
import com.telenav.kivakit.serialization.core.SerializationSessionFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static com.telenav.kivakit.core.logging.logs.text.formatters.ColumnarLogFormatter.DEFAULT;
import static com.telenav.kivakit.core.string.Formatter.Format.WITHOUT_EXCEPTION;
import static com.telenav.kivakit.resource.Extension.KRYO;
import static com.telenav.kivakit.resource.Extension.TXT;
import static com.telenav.kivakit.serialization.core.SerializationSession.SessionType.RESOURCE;

/**
 * @author jonathanl (shibo)
 */
@SuppressWarnings("unused")
public class
SessionStore extends BaseComponent
{
    private static final Lazy<SessionStore> store = Lazy.of(SessionStore::new);

    public static SessionStore get()
    {
        return store.get();
    }

    private final Map<Session, LinkedList<LogEntry>> sessionNameToEntries = new HashMap<>();

    private Set<Session> sessions = Collections.synchronizedSet(new HashSet<>());

    private SessionStore()
    {
    }

    public synchronized void add(Session session)
    {
        sessions.add(session);
        sessionNameToEntries.put(session, new LinkedList<>());
    }

    public synchronized void add(Session session, byte[] bytes, ProgressReporter reporter)
    {
        add(session);
        sessionFile(session, KRYO).reader(reporter).bytes();
    }

    public synchronized void addAll(Session session, List<LogEntry> toAdd)
    {
        if (session != null)
        {
            var entries = sessionNameToEntries.get(session);
            if (entries != null)
            {
                entries.addAll(toAdd);
            }
        }
    }

    public synchronized void delete(Session session)
    {
        sessions.remove(session);
        sessionFile(session, KRYO).delete();
        sessionFile(session, TXT).delete();
    }

    /**
     * @return A copy of the list of log entries for the given session
     */
    @SuppressWarnings("unchecked")
    public synchronized LinkedList<LogEntry> entries(Session session)
    {
        if (session != null)
        {
            var entries = sessionNameToEntries.get(session);
            if (entries == null)
            {
                try (var input = sessionFile(session, KRYO).openForReading())
                {
                    var serializationSession = session();
                    var version = serializationSession.open(input, RESOURCE);
                    trace("Loaded session '$' (KivaKit version $)", session, version);
                    entries = (LinkedList<LogEntry>) serializationSession.read().object();
                    sessionNameToEntries.put(session, entries);
                }
                catch (IOException ignored)
                {
                    return new LinkedList<>();
                }
            }
            return new LinkedList<>(entries);
        }

        return new LinkedList<>();
    }

    public boolean has(Session session)
    {
        return sessions().contains(session);
    }

    public synchronized void load()
    {
        sessions = new HashSet<>();
        for (var file : logFolder().files(KRYO))
        {
            var parts = file.fileName().withoutExtension(KRYO).name().split("-");
            if (parts.length == 2)
            {
                var name = parts[0];
                var time = new LocalDateTimeConverter(this).convert(parts[1]);
                if (!Strings.isEmpty(name) && time != null)
                {
                    sessions.add(new Session(name, time.asTime(), file.sizeInBytes()));
                }
            }
        }
    }

    public byte[] read(Session session, ProgressReporter reporter)
    {
        return sessionFile(session, KRYO).reader(reporter).bytes();
    }

    public synchronized void save(Session session)
    {
        var entries = entries(session);
        if (!entries.isEmpty())
        {
            try (var output = sessionFile(session, KRYO).openForWriting())
            {
                var serializer = session();
                serializer.open(output, RESOURCE, kivakitVersion());
                serializer.write(new SerializableObject<>(entries, project(ServerLogProject.class).projectVersion()));
                serializer.close();
            }
            catch (IOException ignored)
            {
            }

            try (var output = sessionFile(session, TXT).printWriter())
            {
                for (var row : entries)
                {
                    output.println(row.format(DEFAULT, WITHOUT_EXCEPTION));
                }
            }
        }
    }

    public Set<Session> sessions()
    {
        return new HashSet<>(sessions);
    }

    private Folder logFolder()
    {
        return Folder.kivakitCache()
                .folder("logs")
                .mkdirs();
    }

    private SerializationSession session()
    {
        return require(SerializationSessionFactory.class).newSession(this);
    }

    private File sessionFile(Session session, Extension extension)
    {
        return logFolder().file(session.fileName().withExtension(extension));
    }
}
