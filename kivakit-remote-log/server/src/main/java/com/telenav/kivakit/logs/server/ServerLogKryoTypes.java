package com.telenav.kivakit.logs.server;

import com.telenav.kivakit.logs.server.session.Session;
import com.telenav.kivakit.serialization.kryo.types.KryoTypes;

/**
 * @author jonathanl (shibo)
 */
public class ServerLogKryoTypes extends KryoTypes
{
    public ServerLogKryoTypes()
    {
        //----------------------------------------------------------------------------------------------
        // NOTE: To maintain backward compatibility, classes are assigned identifiers by KivaKitKryoSerializer.
        // If classes are appended to groups and no classes are removed, older data can always be read.
        //----------------------------------------------------------------------------------------------

        group("logs-server", () -> register(Session.class));
    }
}
