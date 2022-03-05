package com.telenav.kivakit.logs.server;

import com.telenav.kivakit.core.object.Lazy;
import com.telenav.kivakit.core.project.Project;
import com.telenav.kivakit.logs.server.project.LogsServerKryoTypes;
import com.telenav.kivakit.serialization.core.SerializationSessionFactory;
import com.telenav.kivakit.serialization.kryo.CoreKernelKryoTypes;
import com.telenav.kivakit.serialization.kryo.KryoTypes;

/**
 * @author jonathanl (shibo)
 */
public class ServerLogProject extends Project
{
    private static final KryoTypes KRYO_TYPES = new CoreKernelKryoTypes()
            .mergedWith(new LogsServerKryoTypes());

    private static final Lazy<ServerLogProject> singleton = Lazy.of(ServerLogProject::new);

    public static ServerLogProject get()
    {
        return singleton.get();
    }

    private ServerLogProject()
    {
        SerializationSessionFactory.threadLocal(KRYO_TYPES.sessionFactory());
    }
}
