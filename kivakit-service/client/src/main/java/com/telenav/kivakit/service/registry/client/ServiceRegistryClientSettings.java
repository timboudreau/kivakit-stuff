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

package com.telenav.kivakit.service.registry.client;

import com.telenav.kivakit.conversion.core.language.object.KivaKitConverted;
import com.telenav.kivakit.conversion.core.time.DurationConverter;
import com.telenav.kivakit.core.KivaKit;
import com.telenav.kivakit.core.language.object.ObjectFormatter;
import com.telenav.kivakit.core.language.reflection.property.KivaKitIncludeProperty;
import com.telenav.kivakit.core.time.Duration;
import com.telenav.kivakit.network.core.NetworkLocation;
import com.telenav.kivakit.network.http.HttpNetworkLocation;
import com.telenav.kivakit.resource.Resource;
import com.telenav.kivakit.resource.Resourceful;
import com.telenav.lexakai.annotations.LexakaiJavadoc;

/**
 * {@link ServiceRegistryClient} settings, including an {@link #accessTimeout(Duration)} and the location of the
 * executable server jar, provided with {@link #serverJar(Resourceful)}. Note that all {@link Resource}s are {@link
 * Resourceful}, but {@link NetworkLocation} is also resourceful and can be used directly with {@link
 * #serverJar(Resourceful)} to launch a JAR from a network location. The default settings for {@link
 * ServiceRegistryClient} will launch the server directly from GitHub with a timeout of one minute. It will thereafter
 * be cached in the KivaKit cache folder, as provided by {@link KivaKit#cacheFolderPath()}. See the
 * ServiceRegistryClientSettings.properties file in this package.
 *
 * @author jonathanl (shibo)
 */
@LexakaiJavadoc(complete = true)
public class ServiceRegistryClientSettings
{
    private Duration accessTimeout;

    private Resourceful serverJar;

    @KivaKitConverted(DurationConverter.class)
    public ServiceRegistryClientSettings accessTimeout(Duration timeout)
    {
        accessTimeout = timeout;
        return this;
    }

    @KivaKitIncludeProperty
    public Duration accessTimeout()
    {
        return accessTimeout;
    }

    @KivaKitConverted(HttpNetworkLocation.Converter.class)
    public ServiceRegistryClientSettings serverJar(Resourceful location)
    {
        serverJar = location;
        return this;
    }

    @KivaKitIncludeProperty
    public Resourceful serverJar()
    {
        return serverJar;
    }

    @Override
    public String toString()
    {
        return new ObjectFormatter(this).toString();
    }
}
