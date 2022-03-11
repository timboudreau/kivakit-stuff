package com.telenav.kivakit.logs.server;

import com.telenav.kivakit.core.object.Lazy;
import com.telenav.kivakit.core.project.Project;
import com.telenav.kivakit.logs.server.project.LogsServerKryoTypes;
import com.telenav.kivakit.serialization.kryo.KryoSerializationSessionFactory;
import com.telenav.kivakit.serialization.kryo.types.CoreKryoTypes;
import com.telenav.kivakit.serialization.kryo.types.KryoTypes;

/**
 * @author jonathanl (shibo)
 */
public class ServerLogProject extends Project
{
    private static final KryoTypes KRYO_TYPES = new CoreKryoTypes()
            .mergedWith(new LogsServerKryoTypes());

    private static final Lazy<ServerLogProject> singleton = Lazy.of(ServerLogProject::new);

    public static ServerLogProject get()
    {
        return singleton.get();
    }

    private ServerLogProject()
    {
        register(new KryoSerializationSessionFactory(KRYO_TYPES));
    }
}
