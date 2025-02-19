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

package com.telenav.kivakit.service.registry.protocol.discover;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.core.language.object.ObjectFormatter;
import com.telenav.kivakit.core.language.reflection.property.KivaKitIncludeProperty;
import com.telenav.kivakit.microservice.protocols.rest.openapi.OpenApiIncludeMember;
import com.telenav.kivakit.microservice.protocols.rest.openapi.OpenApiIncludeType;
import com.telenav.kivakit.service.registry.lexakai.DiagramRest;
import com.telenav.kivakit.service.registry.protocol.BaseResponse;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import java.util.HashSet;
import java.util.Set;

import static com.telenav.kivakit.core.language.object.ObjectFormatter.Format.MULTILINE;

/**
 * The set of applications found for an {@link DiscoverApplicationsRequest}.
 *
 * @author jonathanl (shibo)
 */
@OpenApiIncludeType(description = "The set of application identifiers discovered")
@UmlClassDiagram(diagram = DiagramRest.class)
@LexakaiJavadoc(complete = true)
public class DiscoverApplicationsResponse extends BaseResponse<Set<Application.Identifier>>
{
    @JsonProperty
    @OpenApiIncludeMember(description = "The applications that were found")
    private Set<Application.Identifier> applications = new HashSet<>();

    @KivaKitIncludeProperty
    public Set<Application.Identifier> applications()
    {
        return applications;
    }

    public DiscoverApplicationsResponse applications(Set<Application.Identifier> applications)
    {
        this.applications = applications;
        return this;
    }

    @Override
    public String toString()
    {
        return new ObjectFormatter(this).toString(MULTILINE);
    }

    @Override
    protected void value(Set<Application.Identifier> value)
    {
        applications = value;
    }

    @Override
    protected Set<Application.Identifier> value()
    {
        return applications();
    }
}
