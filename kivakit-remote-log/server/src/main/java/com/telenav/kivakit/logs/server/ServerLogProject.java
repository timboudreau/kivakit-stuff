package com.telenav.kivakit.logs.server;

import com.telenav.kivakit.core.project.Project;
import com.telenav.kivakit.core.project.ProjectTrait;
import com.telenav.kivakit.logs.server.project.LogsServerKryoTypes;
import com.telenav.kivakit.serialization.kryo.KryoSerializationSessionFactory;
import com.telenav.kivakit.serialization.kryo.types.CoreKryoTypes;

/**
 * This class defines a KivaKit {@link Project}. It cannot be constructed with the new operator since it has a private
 * constructor. To access the singleton instance of this class, call {@link Project#resolveProject(Class)}, or use
 * {@link ProjectTrait#project(Class)}.
 *
 * @author jonathanl (shibo)
 */
public class ServerLogProject extends Project
{
    public ServerLogProject()
    {
        register(new KryoSerializationSessionFactory(new CoreKryoTypes()
                .mergedWith(new LogsServerKryoTypes())));
    }
}
