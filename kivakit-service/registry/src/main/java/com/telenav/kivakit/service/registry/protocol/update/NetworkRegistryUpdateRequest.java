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

package com.telenav.kivakit.service.registry.protocol.update;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.telenav.kivakit.core.language.object.ObjectFormatter;
import com.telenav.kivakit.core.language.reflection.property.KivaKitIncludeProperty;
import com.telenav.kivakit.microservice.protocols.rest.openapi.OpenApiIncludeMember;
import com.telenav.kivakit.microservice.protocols.rest.openapi.OpenApiIncludeType;
import com.telenav.kivakit.service.registry.Service;
import com.telenav.kivakit.service.registry.protocol.BaseRequest;
import com.telenav.lexakai.annotations.LexakaiJavadoc;

import static com.telenav.kivakit.core.language.object.ObjectFormatter.Format.MULTILINE;
import static com.telenav.kivakit.service.registry.protocol.ServiceRegistryProtocol.NETWORK_REGISTRY_UPDATE;

/**
 * A request from a local service registry to the network service registry propagating initial registration information
 * for a new service or renewing the lease of an existing service.
 *
 * @author jonathanl (shibo)
 */
@OpenApiIncludeType(description = "Request to update a service entry in the network registry")
@LexakaiJavadoc(complete = true)
public class NetworkRegistryUpdateRequest extends BaseRequest
{
    @JsonProperty
    @OpenApiIncludeMember(description = "The service to update")
    private Service service;

    @Override
    public String path()
    {
        return NETWORK_REGISTRY_UPDATE;
    }

    public NetworkRegistryUpdateRequest service(Service service)
    {
        this.service = service;
        return this;
    }

    @KivaKitIncludeProperty
    public Service service()
    {
        return service;
    }

    @Override
    public String toString()
    {
        return new ObjectFormatter(this).toString(MULTILINE);
    }
}
