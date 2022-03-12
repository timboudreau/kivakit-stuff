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

package com.telenav.kivakit.service.registry;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.core.messaging.messages.status.Problem;
import com.telenav.kivakit.core.project.Project;
import com.telenav.kivakit.core.project.ProjectTrait;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.network.core.Port;
import com.telenav.kivakit.serialization.gson.factory.GsonFactory;
import com.telenav.kivakit.serialization.gson.serializers.ProblemGsonSerializer;
import com.telenav.kivakit.serialization.gson.serializers.TimeInMillisecondsGsonSerializer;
import com.telenav.kivakit.serialization.kryo.KryoSerializationSessionFactory;
import com.telenav.kivakit.serialization.kryo.types.CoreKryoTypes;
import com.telenav.kivakit.service.registry.project.ServiceRegistryKryoTypes;
import com.telenav.kivakit.service.registry.serialization.serializers.ApplicationIdentifierGsonSerializer;
import com.telenav.kivakit.service.registry.serialization.serializers.ServiceTypeGsonSerializer;

import static com.telenav.kivakit.core.string.Formatter.Format.WITH_EXCEPTION;

/**
 * This class defines a KivaKit {@link Project}. It cannot be constructed with the new operator since it has a private
 * constructor. To access the singleton instance of this class, call {@link Project#resolveProject(Class)}, or use
 * {@link ProjectTrait#project(Class)}.
 *
 * @author jonathanl (shibo)
 */
public class ServiceRegistryProject extends Project
{
    private ServiceRegistryProject()
    {
        register(new KryoSerializationSessionFactory(new ServiceRegistryKryoTypes()
                .mergedWith(new CoreKryoTypes())));
    }

    @Override
    public void onInitialize()
    {
        require(GsonFactory.class)
                .addConvertingSerializer(Port.class, new Port.Converter(this))
                .addJsonSerializerDeserializer(Application.Identifier.class, new ApplicationIdentifierGsonSerializer())
                .addJsonSerializerDeserializer(ServiceType.class, new ServiceTypeGsonSerializer())
                .addJsonSerializerDeserializer(Problem.class, new ProblemGsonSerializer(WITH_EXCEPTION))
                .addJsonSerializerDeserializer(Time.class, new TimeInMillisecondsGsonSerializer());
    }
}
