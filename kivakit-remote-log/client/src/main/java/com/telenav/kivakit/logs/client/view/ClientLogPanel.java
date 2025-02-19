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

package com.telenav.kivakit.logs.client.view;

import com.telenav.kivakit.core.logging.LogEntry;
import com.telenav.kivakit.core.messaging.Broadcaster;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.core.progress.reporters.ProgressiveInputStream;
import com.telenav.kivakit.core.string.Strings;
import com.telenav.kivakit.core.thread.KivaKitThread;
import com.telenav.kivakit.core.time.Duration;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.core.version.VersionedObject;
import com.telenav.kivakit.core.vm.JavaVirtualMachineHealth;
import com.telenav.kivakit.core.vm.ShutdownHook;
import com.telenav.kivakit.logs.client.ClientLog;
import com.telenav.kivakit.logs.client.ClientLogFrame;
import com.telenav.kivakit.logs.client.network.Connection;
import com.telenav.kivakit.logs.client.network.Connector;
import com.telenav.kivakit.logs.client.network.Receiver;
import com.telenav.kivakit.logs.client.project.LogsClientTheme;
import com.telenav.kivakit.logs.client.view.panels.connection.ConnectionPanel;
import com.telenav.kivakit.logs.client.view.panels.search.SearchPanel;
import com.telenav.kivakit.logs.client.view.panels.session.SessionPanel;
import com.telenav.kivakit.logs.client.view.panels.table.TablePanel;
import com.telenav.kivakit.logs.client.view.panels.tool.ToolPanel;
import com.telenav.kivakit.logs.server.session.Session;
import com.telenav.kivakit.logs.server.session.SessionStore;
import com.telenav.kivakit.ui.desktop.component.KivaKitPanel;
import com.telenav.kivakit.ui.desktop.component.panel.output.OutputPanel;
import com.telenav.kivakit.ui.desktop.component.progress.ProgressPanel;
import com.telenav.kivakit.ui.desktop.component.status.StatusPanel;
import com.telenav.kivakit.ui.desktop.layout.Borders;
import com.telenav.kivakit.ui.desktop.layout.Margins;
import com.telenav.kivakit.ui.desktop.layout.Size;
import com.telenav.kivakit.ui.desktop.layout.VerticalBoxLayout;
import org.jetbrains.annotations.NotNull;

import javax.swing.BoxLayout;
import javax.swing.JPanel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.util.List;
import java.util.function.Consumer;

import static com.telenav.kivakit.core.vm.ShutdownHook.Order.FIRST;
import static com.telenav.kivakit.ui.desktop.component.panel.output.OutputPanel.Type.FIXED_WIDTH;
import static com.telenav.kivakit.ui.desktop.component.progress.ProgressPanel.CompletionStatus.CANCELLED;
import static com.telenav.kivakit.ui.desktop.component.status.StatusPanel.Display.SHOW_HEALTH_PANEL;

/**
 * A panel that displays, filters and searches log entries.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings({ "SameParameterValue" })
public class ClientLogPanel extends KivaKitPanel
{
    static
    {
        LogsClientTheme.initialize();
    }

    private final ClientLogFrame frame;

    private final ClientLog log;

    private ConnectionPanel connectionPanel;

    private SessionPanel sessionPanel;

    private SearchPanel searchPanel;

    private TablePanel tablePanel;

    private OutputPanel consolePanel;

    private StatusPanel statusPanel;

    private final Connector connector;

    private final Receiver receiver;

    private Session connectedSession;

    public ClientLogPanel(ClientLogFrame frame, ClientLog log, Maximum maximumEntries)
    {
        this.frame = frame;
        this.log = log;

        ShutdownHook.register(FIRST, this::saveConnectedSession);

        receiver = statusPanel().listenTo(new Receiver());

        Consumer<Connector.State> connectionStateListener = connectionStateListener();
        Consumer<VersionedObject<?>> objectListener = objectListener(log);
        Consumer<Session> newSessionListener = newSessionListener();
        connector = statusPanel().listenTo(new Connector
                (
                        receiver,
                        connectionStateListener,
                        handleConnection(objectListener, newSessionListener)
                ));

        Size.maximumWidth().maximum(this);
        Borders.insideMarginsOf(Margins.of(8)).apply(this);
        new VerticalBoxLayout(this)
                .add(new ToolPanel(this))
                .add(splitPanePanel())
                .add(statusPanel());

        // Load sessions and start trying to connect to the given port
        sessionPanel.loadSessions();
    }

    public void addAll(List<LogEntry> toAdd)
    {
        if (connector.isConnected())
        {
            sessionPanel().currentSession().addAll(toAdd);
            tablePanel().addAll(toAdd);
        }
    }

    public void clear()
    {
        tablePanel().clear();
        searchPanel().clear();
    }

    public ConnectionPanel connectionPanel()
    {
        if (connectionPanel == null)
        {
            connectionPanel = listenTo(new ConnectionPanel(connector));
        }
        return connectionPanel;
    }

    public Connector connector()
    {
        return connector;
    }

    public OutputPanel consolePanel()
    {
        if (consolePanel == null)
        {
            consolePanel = listenTo(new OutputPanel(FIXED_WIDTH));
        }
        return consolePanel;
    }

    @Override
    public Dimension getPreferredSize()
    {
        return new Dimension(1000, 500);
    }

    @Override
    public <T extends Broadcaster> T listenTo(T broadcaster)
    {
        return statusPanel().listenTo(broadcaster);
    }

    public ClientLog log()
    {
        return log;
    }

    public void say(String message, Object... arguments)
    {
        statusPanel.status(message, arguments);
    }

    public void say(Duration stayFor, String message, Object... arguments)
    {
        statusPanel().status(stayFor, message, arguments);
    }

    public SearchPanel searchPanel()
    {
        if (searchPanel == null)
        {
            searchPanel = listenTo(new SearchPanel(this));
        }
        return searchPanel;
    }

    public SessionPanel sessionPanel()
    {
        if (sessionPanel == null)
        {
            sessionPanel = listenTo(new SessionPanel(this));
            sessionPanel.loadSessions();
        }
        return sessionPanel;
    }

    public StatusPanel statusPanel()
    {
        if (statusPanel == null)
        {
            statusPanel = new StatusPanel(SHOW_HEALTH_PANEL);
        }
        return statusPanel;
    }

    public TablePanel tablePanel()
    {
        if (tablePanel == null)
        {
            tablePanel = listenTo(new TablePanel(this));
        }
        return tablePanel;
    }

    public void updateTitle()
    {
        var currentSession = sessionPanel().currentSession();
        var entries = currentSession != null ? currentSession.entries().size() : 0;
        frame.title(Strings.format("MesaKit Log Viewer ($) - Viewing $ Entries",
                connector().isConnected() ? connector().connectedPort() : "Disconnected", entries));
    }

    protected JSplitPane splitPane(JPanel one, JPanel two)
    {
        var splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, one, two);
        splitPane.setMaximumSize(new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE));
        splitPane.setOneTouchExpandable(false);
        splitPane.setResizeWeight(0.75);
        splitPane.setContinuousLayout(true);
        SwingUtilities.invokeLater(() ->
        {
            var screen = Toolkit.getDefaultToolkit().getScreenSize();
            splitPane.setDividerLocation((int) (screen.getHeight() * 0.45));
        });
        return splitPane;
    }

    protected JPanel splitPanePanel()
    {
        var splitPanePanel = new JPanel();
        var splitPane = splitPane(tablePanel(), consolePanel());
        splitPane.setDividerSize(12);
        splitPanePanel.setLayout(new BoxLayout(splitPanePanel, BoxLayout.X_AXIS));
        splitPanePanel.add(splitPane);
        return splitPanePanel;
    }

    @NotNull
    private Consumer<Connector.State> connectionStateListener()
    {
        return state ->
        {
            if (state == Connector.State.DISCONNECTED)
            {
                saveConnectedSession();
            }
            updateTitle();
        };
    }

    @NotNull
    private Consumer<Connection> handleConnection(Consumer<VersionedObject<?>> objectListener,
                                                  Consumer<Session> newSessionListener)
    {
        return connection ->
        {
            var reporter = BroadcastingProgressReporter.create();
            var progressiveInput = new ProgressiveInputStream(connection.input(), reporter);
            var progress = new ProgressPanel(reporter, 150, status ->
            {
                if (status == CANCELLED)
                {
                    receiver.stop();
                }
            });
            progress.setVisible(true);

            listenTo(KivaKitThread.run("LogReceiver", () -> receiver.receive(connection, newSessionListener, objectListener)));
        };
    }

    @NotNull
    private Consumer<Session> newSessionListener()
    {
        return session -> connectedSession = session;
    }

    @NotNull
    private Consumer<VersionedObject<?>> objectListener(ClientLog log)
    {
        return versionedObject ->
        {
            var object = versionedObject.object();
            if (object instanceof LogEntry)
            {
                log.log((LogEntry) object);
                tablePanel().update();
            }
            if (object instanceof JavaVirtualMachineHealth)
            {
                statusPanel().update((JavaVirtualMachineHealth) object);
            }
        };
    }

    private void saveConnectedSession()
    {
        SessionStore.get().save(connectedSession);
    }
}
