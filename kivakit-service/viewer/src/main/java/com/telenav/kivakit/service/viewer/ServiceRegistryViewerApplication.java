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

package com.telenav.kivakit.service.viewer;

import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.commandline.SwitchParser;
import com.telenav.kivakit.core.KivaKit;
import com.telenav.kivakit.core.collections.list.ObjectList;
import com.telenav.kivakit.core.collections.list.StringList;
import com.telenav.kivakit.core.collections.set.ObjectSet;
import com.telenav.kivakit.core.messaging.messages.status.Problem;
import com.telenav.kivakit.core.os.Console;
import com.telenav.kivakit.core.string.AsciiArt;
import com.telenav.kivakit.service.registry.Scope;
import com.telenav.kivakit.service.registry.Scope.Type;
import com.telenav.kivakit.service.registry.ServiceMetadata;
import com.telenav.kivakit.service.registry.ServiceType;
import com.telenav.kivakit.service.registry.client.ServiceRegistryClient;

import static com.telenav.kivakit.core.string.Formatter.Format.WITH_EXCEPTION;
import static com.telenav.kivakit.service.registry.Scope.Type.scopeTypeSwitchParser;

/**
 * Identifier to view the KivaKit services on a machine.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings("UseOfSystemOutOrSystemErr")
public class ServiceRegistryViewerApplication extends Application
{
    public static void main(String[] arguments)
    {
        new ServiceRegistryViewerApplication().run(arguments);
    }

    private final SwitchParser<Type> SCOPE_TYPE = scopeTypeSwitchParser(this).build();

    @Override
    protected void onRun()
    {
        showCommandLine();

        // Connect to service registry server
        var client = listenTo(new ServiceRegistryClient());

        // register a server log
        if (isDebugOn())
        {
            var metadata = new ServiceMetadata()
                    .description("Server log for " + identifier())
                    .kivakitVersion(KivaKit.get().kivakitVersion())
                    .version(KivaKit.get().projectVersion());
            var serverLog = client.register(Scope.localhost(), new ServiceType("kivakit-server-log"), metadata);
            System.out.println("Registered service " + serverLog);
        }

        // and show available services
        var services = client.discoverServices(Scope.scope(get(SCOPE_TYPE)));
        if (services.failed())
        {
            Console.println("\nUnable to find services: $\n", services.messages().find(Problem.class).formatted(WITH_EXCEPTION));
        }
        else
        {
            var lines = new StringList();
            var format = "%-24s %-8s %-32s %-48s %s";
            lines.add("");
            lines.add(String.format(format, "renewed", "port", "service", "application", "description"));
            lines.add(AsciiArt.line(200));
            var sorted = ObjectList.objectList(services.get()).sorted();
            for (var service : sorted)
            {
                lines.add(String.format(format, service.renewedAt().elapsedSince() + " ago", service.port().number(), service.type(), service.application(), service.metadata().description()));
            }
            lines.add("");
            System.out.println(lines.join("\n"));
        }
        System.out.flush();
    }

    @Override
    protected ObjectSet<SwitchParser<?>> switchParsers()
    {
        return ObjectSet.objectSet(SCOPE_TYPE);
    }
}
