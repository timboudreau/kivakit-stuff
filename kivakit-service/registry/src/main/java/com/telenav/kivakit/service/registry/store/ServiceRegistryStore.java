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

package com.telenav.kivakit.service.registry.store;

import com.telenav.kivakit.component.BaseComponent;
import com.telenav.kivakit.core.language.primitive.Booleans;
import com.telenav.kivakit.core.messaging.Debug;
import com.telenav.kivakit.core.string.CaseFormat;
import com.telenav.kivakit.core.vm.Properties;
import com.telenav.kivakit.filesystem.File;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.resource.serialization.SerializableObject;
import com.telenav.kivakit.serialization.kryo.KryoSerializationSession;
import com.telenav.kivakit.serialization.kryo.types.CoreKryoTypes;
import com.telenav.kivakit.serialization.kryo.types.KryoTypes;
import com.telenav.kivakit.service.registry.ServiceRegistry;
import com.telenav.kivakit.service.registry.ServiceRegistrySettings;
import com.telenav.kivakit.service.registry.lexakai.DiagramRegistry;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.lexakai.annotations.UmlClassDiagram;
import com.telenav.lexakai.annotations.associations.UmlRelation;
import com.telenav.lexakai.annotations.visibility.UmlNotPublicApi;

import static com.telenav.kivakit.resource.Extension.TMP;
import static com.telenav.kivakit.serialization.core.SerializationSession.SessionType.RESOURCE;

/**
 * <b>Not public API</b>
 * <p>
 * This store saves registry information with {@link #save(ServiceRegistry)}, ensuring that port assignments and other
 * information can be reloaded with {@link #load(Class)} when a service registry is down for a short time such as during
 * and upgrade or reboot. Note that service registration is only preserved for {@link
 * ServiceRegistrySettings#serviceRegistryStoreExpirationTime()}. If a reboot takes longer than this, the serialized
 * registry data is assumed to be too out-of-date to be useful.
 * </p>
 *
 * @author jonathanl (shibo)
 */
@UmlClassDiagram(diagram = DiagramRegistry.class)
@UmlNotPublicApi
@LexakaiJavadoc(complete = true)
public class ServiceRegistryStore extends BaseComponent
{
    /**
     * Loads the service registry of the given type from the cache folder where that type of registry is saved by {@link
     * #save(ServiceRegistry)}
     */
    @UmlRelation(label = "loads")
    public synchronized ServiceRegistry load(Class<? extends ServiceRegistry> type)
    {
        // Get the serialization file for the service registry type (this allows us to run both
        // local and network registries on the same machine)
        var file = file(type);
        if (file.exists())
        {
            // and if the data is not too old
            var lastModified = file.modifiedAt();
            var expirationTime = settings().serviceRegistryStoreExpirationTime();
            if (lastModified.elapsedSince().isLessThan(expirationTime))
            {
                // then open the file
                trace("Loading service registry from $", file);
                try (var input = file.openForReading())
                {
                    // create a serialization object and read the serialized registry
                    try (var session = new KryoSerializationSession(new CoreKryoTypes()))
                    {
                        session.open(input);
                        var object = session.read();
                        if (object != null)
                        {
                            // then unregister the loaded class with the Debug class so the debug flag
                            // is re-considered for the newly loaded instance
                            Debug.unregister(object.object().getClass());

                            // and add the listener to the registry.
                            trace("Loaded service registry");
                            return listenTo((ServiceRegistry) object.object());
                        }
                    }
                }
                catch (Exception e)
                {
                    // We are unable to load the service registry, so remove the file.
                    file.delete();
                }
            }
            else
            {
                // The file is too old so remove it.
                file.delete();
            }
        }
        return null;
    }

    /**
     * Saves the given registry to a cache folder
     */
    public synchronized void save(ServiceRegistry registry)
    {
        if (Booleans.isTrue(Properties.property("KIVAKIT_SAVE_REGISTRY", "true")))
        {
            var file = file(registry.getClass()).withExtension(TMP);
            trace("Saving service registry to $", file.messageSource());
            if (file.delete())
            {
                try (var output = file.openForWriting())
                {
                    var session = new KryoSerializationSession(new KryoTypes());
                    session.open(output, RESOURCE, settings().version());
                    session.write(new SerializableObject<>(registry, settings().version()));
                    session.close();
                }
                catch (Exception e)
                {
                    problem(e, "Unable to save service registry to $", file);
                }
                file.renameTo(file(registry.getClass()));
            }
            trace("Service registry saved");
        }
    }

    private File file(Class<? extends ServiceRegistry> type)
    {
        return Folder.kivakitCache()
                .folder("service-registry")
                .mkdirs()
                .file(CaseFormat.camelCaseToHyphenated(type.getSimpleName()) + ".kryo");
    }

    private ServiceRegistrySettings settings()
    {
        return require(ServiceRegistrySettings.class);
    }
}
