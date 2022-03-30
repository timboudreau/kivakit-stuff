import com.telenav.kivakit.core.logging.Log;
import com.telenav.kivakit.logs.client.ClientLog;

open module kivakit.logs.client
{
    provides Log with ClientLog;

    // KivaKit
    requires kivakit.ui.desktop;
    requires kivakit.logs.server;
    requires kivakit.service.client;
    requires kivakit.network.core;

    // Java
    requires java.desktop;
    requires kivakit.serialization.core;

    // Module exports
    exports com.telenav.kivakit.logs.client;
}
