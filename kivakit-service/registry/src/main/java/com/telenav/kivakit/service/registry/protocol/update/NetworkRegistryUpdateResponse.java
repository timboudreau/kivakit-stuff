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
import com.telenav.kivakit.service.registry.lexakai.DiagramRest;
import com.telenav.kivakit.service.registry.protocol.BaseResponse;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import static com.telenav.kivakit.core.language.object.ObjectFormatter.Format.MULTILINE;

/**
 * The response to a network registry update request. {@link #added()} returns true if the service was added to the
 * registry.
 *
 * @author jonathanl (shibo)
 */
@OpenApiIncludeType(description = "Response to a registry update request")
@UmlClassDiagram(diagram = DiagramRest.class)
@LexakaiJavadoc(complete = true)
public class NetworkRegistryUpdateResponse extends BaseResponse<Boolean>
{
    @JsonProperty
    @OpenApiIncludeMember(description = "True if the caller was added to the network registry")
    private boolean added;

    public NetworkRegistryUpdateResponse added(boolean added)
    {
        this.added = added;
        return this;
    }

    @KivaKitIncludeProperty
    public boolean added()
    {
        return added;
    }

    @Override
    public String toString()
    {
        return new ObjectFormatter(this).toString(MULTILINE);
    }

    @Override
    protected void value(Boolean value)
    {
        added = value;
    }

    @Override
    protected Boolean value()
    {
        return added;
    }
}
