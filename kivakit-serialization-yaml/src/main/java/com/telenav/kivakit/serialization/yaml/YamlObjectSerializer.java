package com.telenav.kivakit.serialization.yaml;

import com.esotericsoftware.yamlbeans.YamlConfig;
import com.esotericsoftware.yamlbeans.YamlReader;
import com.telenav.kivakit.core.io.IO;
import com.telenav.kivakit.core.language.Classes;
import com.telenav.kivakit.core.language.trait.TryTrait;
import com.telenav.kivakit.core.path.StringPath;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.core.registry.InstanceIdentifier;
import com.telenav.kivakit.core.registry.RegistryTrait;
import com.telenav.kivakit.core.version.Version;
import com.telenav.kivakit.resource.serialization.ObjectMetadata;
import com.telenav.kivakit.resource.serialization.ObjectSerializer;
import com.telenav.kivakit.resource.serialization.SerializableObject;
import org.jetbrains.annotations.Nullable;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.regex.Pattern;

import static com.telenav.kivakit.core.ensure.Ensure.ensureNotNull;
import static com.telenav.kivakit.core.ensure.Ensure.unsupported;
import static com.telenav.kivakit.resource.serialization.ObjectMetadata.INSTANCE;
import static com.telenav.kivakit.resource.serialization.ObjectMetadata.TYPE;
import static com.telenav.kivakit.resource.serialization.ObjectMetadata.VERSION;

/**
 * JSON {@link ObjectSerializer} using Google Gson library.
 *
 * @author jonathanl (shibo)
 */
public class YamlObjectSerializer implements
        ObjectSerializer,
        RegistryTrait,
        TryTrait
{
    private static final Pattern TYPE_PATTERN = Pattern.compile("(class|type)\\s*:\\s*(?<type>.+)\\n*");

    private static final Pattern INSTANCE_PATTERN = Pattern.compile("(instance)\\s*:\\s*(?<instance>.+)\\n*");

    private static final Pattern VERSION_PATTERN = Pattern.compile("(version)\\s*:\\s*(?<version>.+)\\n*");

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> SerializableObject<T> read(InputStream input,
                                          StringPath path,
                                          Class<T> type,
                                          ObjectMetadata... metadata)
    {
        // Read .yaml text,
        var text = IO.string(input);

        return tryCatchThrow(() ->
        {
            var yaml = text;

            // and resolve the type, version and instance we want to read,
            var resolvedType = resolvedType(yaml, metadata, type);
            var version = version(yaml, metadata);
            var instance = instance(yaml, metadata);

            // remove these fields,
            yaml = TYPE_PATTERN.matcher(yaml).replaceAll("");
            yaml = INSTANCE_PATTERN.matcher(yaml).replaceAll("");
            yaml = VERSION_PATTERN.matcher(yaml).replaceAll("");

            // and deserialize the YAML,
            var settings = new YamlConfig();
            settings.setBeanProperties(true);
            settings.setPrivateFields(true);
            var reader = new YamlReader(yaml);
            var object = tryFinally(() -> reader.read(resolvedType), reader::close);

            // and returning the deserialized object.
            return new SerializableObject<>(object, version, instance);
        }, "Unable to read $ from $", resolvedType(text, metadata, type), path);
    }

    @Override
    public ProgressReporter reporter()
    {
        return ProgressReporter.none();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> void write(OutputStream output,
                          StringPath path,
                          SerializableObject<T> object,
                          ObjectMetadata... metadata)
    {
        unsupported("Writing YAML is not supported at this time");
    }

    private InstanceIdentifier instance(String text, ObjectMetadata[] metadata)
    {
        var instance = InstanceIdentifier.SINGLETON;
        var instanceMatcher = INSTANCE_PATTERN.matcher(text);
        if (INSTANCE.containedIn(metadata) && instanceMatcher.find())
        {
            instance = InstanceIdentifier.of(instanceMatcher.group("instance"));
        }
        return instance;
    }

    private <T> Class<T> resolvedType(String text, ObjectMetadata[] metadata,
                                      Class<T> typeToRead) throws RuntimeException
    {
        Class<T> type = typeToRead;
        if (type == null && TYPE.containedIn(metadata))
        {
            var typeMatcher = TYPE_PATTERN.matcher(text);
            if (typeMatcher.find())
            {
                type = Classes.forName(typeMatcher.group("type"));
            }
        }

        return ensureNotNull(type, "Could not resolve type: Must supply an explicit type or ObjectMetadata.TYPE to read the type from input");
    }

    @Nullable
    private Version version(String text, ObjectMetadata[] metadata)
    {
        Version version = null;
        if (VERSION.containedIn(metadata))
        {
            var versionMatcher = VERSION_PATTERN.matcher(text);
            if (versionMatcher.find())
            {
                version = Version.parseVersion(versionMatcher.group("version"));
            }
        }
        return version;
    }
}
