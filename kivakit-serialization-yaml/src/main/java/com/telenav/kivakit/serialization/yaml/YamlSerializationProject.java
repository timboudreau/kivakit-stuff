package com.telenav.kivakit.serialization.yaml;

import com.telenav.kivakit.core.project.Project;
import com.telenav.kivakit.resource.path.Extension;
import com.telenav.kivakit.resource.serialization.ObjectSerializers;

/**
 * This class defines a KivaKit {@link Project}. It cannot be constructed with the new operator since it has a private
 * constructor. To access the singleton instance of this class, call {@link Project#resolveProject(Class)}, or use
 * {@link com.telenav.kivakit.core.project.ProjectTrait#project(Class)}.
 *
 * <p>
 * Project initialization associates a {@link YamlObjectSerializer} with the <i>.yaml</i> extension in {@link
 * ObjectSerializers}.
 * </p>
 *
 * @author jonathanl (shibo)
 * @see YamlObjectSerializer
 * @see ObjectSerializers
 */
public class YamlSerializationProject extends Project
{
    @Override
    public void onInitialize()
    {
        // Associate YAML object serializer with .yaml resources
        require(ObjectSerializers.class, ObjectSerializers::new)
                .add(Extension.YAML, listenTo(new YamlObjectSerializer()))
                .add(Extension.YML, listenTo(new YamlObjectSerializer()));
    }
}
