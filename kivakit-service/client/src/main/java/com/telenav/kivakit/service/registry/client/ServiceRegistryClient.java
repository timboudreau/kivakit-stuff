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

import com.google.gson.Gson;
import com.telenav.kivakit.application.Application;
import com.telenav.kivakit.application.Server;
import com.telenav.kivakit.component.BaseComponent;
import com.telenav.kivakit.core.function.Result;
import com.telenav.kivakit.core.language.reflection.Type;
import com.telenav.kivakit.core.os.OperatingSystem;
import com.telenav.kivakit.core.thread.KivaKitThread;
import com.telenav.kivakit.core.time.Duration;
import com.telenav.kivakit.core.version.Version;
import com.telenav.kivakit.core.vm.JavaVirtualMachine;
import com.telenav.kivakit.filesystem.Folder;
import com.telenav.kivakit.launcher.JarLauncher;
import com.telenav.kivakit.network.core.Port;
import com.telenav.kivakit.service.registry.Scope;
import com.telenav.kivakit.service.registry.Service;
import com.telenav.kivakit.service.registry.ServiceMetadata;
import com.telenav.kivakit.service.registry.ServiceRegistry;
import com.telenav.kivakit.service.registry.ServiceRegistrySettings;
import com.telenav.kivakit.service.registry.ServiceType;
import com.telenav.kivakit.service.registry.client.lexakai.DiagramClient;
import com.telenav.kivakit.service.registry.protocol.BaseRequest;
import com.telenav.kivakit.service.registry.protocol.BaseResponse;
import com.telenav.kivakit.service.registry.protocol.discover.DiscoverApplicationsRequest;
import com.telenav.kivakit.service.registry.protocol.discover.DiscoverApplicationsResponse;
import com.telenav.kivakit.service.registry.protocol.discover.DiscoverPortServiceRequest;
import com.telenav.kivakit.service.registry.protocol.discover.DiscoverPortServiceResponse;
import com.telenav.kivakit.service.registry.protocol.discover.DiscoverServicesRequest;
import com.telenav.kivakit.service.registry.protocol.discover.DiscoverServicesResponse;
import com.telenav.kivakit.service.registry.protocol.register.RegisterServiceRequest;
import com.telenav.kivakit.service.registry.protocol.register.RegisterServiceResponse;
import com.telenav.kivakit.service.registry.protocol.renew.RenewServiceRequest;
import com.telenav.kivakit.service.registry.protocol.renew.RenewServiceResponse;
import com.telenav.kivakit.service.registry.protocol.update.NetworkRegistryUpdateRequest;
import com.telenav.kivakit.service.registry.protocol.update.NetworkRegistryUpdateResponse;
import com.telenav.kivakit.service.registry.registries.LocalServiceRegistry;
import com.telenav.kivakit.service.registry.registries.NetworkServiceRegistry;
import com.telenav.kivakit.service.registry.serialization.ServiceRegistryGsonFactorySource;
import com.telenav.lexakai.annotations.UmlClassDiagram;
import com.telenav.lexakai.annotations.associations.UmlRelation;
import org.glassfish.jersey.client.JerseyClientBuilder;
import org.jetbrains.annotations.NotNull;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import java.net.ConnectException;
import java.security.Provider;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.telenav.kivakit.core.ensure.Ensure.unsupported;
import static com.telenav.kivakit.launcher.JarLauncher.ProcessType.DETACHED;
import static com.telenav.kivakit.service.registry.protocol.discover.DiscoverServicesRequest.SearchType.ALL_SERVICES;
import static com.telenav.kivakit.service.registry.protocol.discover.DiscoverServicesRequest.SearchType.APPLICATION_SERVICES;
import static com.telenav.kivakit.service.registry.protocol.discover.DiscoverServicesRequest.SearchType.SERVICES_OF_TYPE;

/**
 * A JSON + REST + JERSEY proxy to {@link ServiceRegistry} processes that register services on the local host and
 * discover services by {@link Scope}: locally, on remote hosts, in clusters and on the network at large.
 * <p>
 * Service registration makes application services discoverable and maps them to physical ports on the local host,
 * ensuring that port conflicts don't occur. These port mappings and other information are automatically propagated to
 * the network-wide registry where it can be queried with this client.
 * <p>
 * <i>Note: at this time, only {@link Scope#localhost()} and {@link Scope#network()} are supported.</i>
 *
 * </p>
 * <p>
 * <b>Registry Servers</b>
 * </p>
 *
 * ServiceRegistryServer is a command-line application can be run in two modes: as a registry on the local host (on port
 * 23,573) or as a network registry located on a well-known host (on port 23575 by default) that can be specified by the
 * KIVAKIT_NETWORK_SERVICE_REGISTRY_PORT environment variable (the default is kivakit-network-service-registry.mypna.com:23575).
 * When local service registries add, update and expire registration entries, they propagate this information upwards to
 * the network registry, allowing service lookups directed to the network registry to be performed against all services
 * on the network. A web view of current service registrations is available on the ports mentioned above.
 * <p>
 * <b>Registry Clients</b>
 * </p>
 * <p>
 * Registration and lookup are implemented under-the-hood as JSON REST services and non-Java applications can use this
 * interface directly (see ServiceRegistryRestResource). For KivaKit applications, the {@link ServiceRegistryClient}
 * class provides easy-to-use access to the network and local registries. {@link ServiceRegistryClient} is simply a Java
 * proxy that implements this {@link ServiceRegistry} interface.
 * </p>
 * <p>
 * <b>Only {@link ServiceRegistryClient} is Public API</b>
 * </p>
 * <p>
 * The service registry class has three subclasses: {@link LocalServiceRegistry}, {@link NetworkServiceRegistry} and
 * {@link ServiceRegistryClient}. Only the client (which interacts with the other two subclasses over REST) is intended
 * for use by end-users of the KivaKit.
 * </p>
 *
 * <p>
 * <b>Registration</b>
 * </p>
 *
 * <p>
 * Services can register themselves by calling {@link #register(Service)}. The application identifier uniquely
 * identifies the registering application on the local host. The {@link Application} and {@link Server} classes produce
 * such identifiers with the {@link Application#identifier()} method. In {@link Application} and {@link Server} classes,
 * the convenience method {@link #register(Scope, ServiceType, ServiceMetadata)} can be used. If the KivaKit application
 * framework is not being utilized, any unique string can be used to identify the registrant. The service type specifies
 * an identifier for the kind of service being registered, typically a value supplied as a service interface constant
 * value. When successful, the register() method will return a {@link Service} object that is bound to a free port in
 * the ephemeral port range.
 *
 * <p>
 * <b>Lookup</b>
 * <p>
 *
 * The following methods can be used to look up registered services within a given {@link Scope} (local host, network,
 * cluster or on a specific host):
 * <ul>
 *     <li>{@link #discoverApplications(Scope)} - A list of applications that have registered service(s) within the given scope</li>
 *     <li>{@link #discoverPortService(Port)} - Find the service running on the given port (and host since {@link Port} specifies host)</li>
 *     <li>{@link #discoverServices(Scope)} - All services within the given scope</li>
 *     <li>{@link #discoverServices(Scope, ServiceType)} - All services of the given type within the given scope</li>
 *     <li>{@link #discoverServices(Scope, Application.Identifier)} - All services belonging to the given application within the given scope</li>
 *     <li>{@link #discoverServices(Scope, Application.Identifier, ServiceType)} - A specific application service within the given scope</li>
 * </ul>
 *
 * <p>
 * <b>Example</b>
 * </p>
 *
 * A service can be bound by application A by calling register() on a {@link ServiceRegistryClient}:
 * <pre>
 *     public static final Service.SearchType SERVICE = new Service.SearchType("example-server");
 *     public static final Identifier APPLICATION_A = new Identifier("application-a");
 *
 *         [...]
 *
 *     var service = client.register(APPLICATION_A, SERVICE);
 *     var port = service.port();
 * </pre>
 * <p>
 * and application B can do the same thing, registering its own server on a different port:
 * </p>
 * <pre>
 *     public static final Identifier APPLICATION_B = new Identifier("application-b");
 *
 *         [...]
 *
 *     var service = client.register(APPLICATION_B, SERVICE);
 *     var port = service.port();
 * </pre>
 * <p>
 * In this way each logical application will be assigned a different physical port for its server.
 * </p>
 * <p>
 * A third application, can then look up either application's web server without knowing the port to connect to in
 * advance:
 * </p>
 * <pre>
 *     var serverA = client.lookup(APPLICATION_A, SERVICE);
 *     var serverB = client.lookup(APPLICATION_B, SERVICE);
 * </pre>
 *
 * <p>
 * <b>API Implementation Details</b>
 * </p>
 *
 * <p>
 * For details on the implementation service registration, including service expiration and port leases,
 * see {@link LocalServiceRegistry}.
 * </p>
 *
 * @author jonathanl (shibo)
 * @see LocalServiceRegistry
 * @see Service
 * @see ServiceType
 * @see Scope
 * @see Application.Identifier
 * @see Port
 * @see Result
 */
@SuppressWarnings({ "InfiniteLoopStatement", "SpellCheckingInspection", "unused", "UnusedReturnValue" })
@UmlClassDiagram(diagram = DiagramClient.class)
@UmlRelation(label = "returns", referent = Result.class)
@UmlRelation(label = "discovers applications", referent = Application.Identifier.class)
@UmlRelation(label = "discovers services", referent = Provider.Service.class)
@UmlRelation(label = "searches within", referent = Scope.class)
public class ServiceRegistryClient extends BaseComponent
{
    /** True if the client is connected */
    private boolean connected;

    /** Client settings */
    private final ServiceRegistryClientSettings settings;

    public ServiceRegistryClient()
    {
        settings = require(ServiceRegistryClientSettings.class);
    }

    /**
     * @return All applications that have registered a service within the given scope
     */
    public @NotNull
    Result<Set<Application.Identifier>> discoverApplications(Scope scope)
    {
        trace("Requesting $ applications from remote registry", scope);
        var request = new DiscoverApplicationsRequest().scope(scope);
        var result = request(scope, request, DiscoverApplicationsResponse.class).asResult();
        trace("Received from registry: $", result);
        return result;
    }

    /**
     * @return Any service running on the given port. Since a {@link Port} includes the host it is unique and only a
     * single service is returned since only one service can be running on a specific port on a specific host.
     */
    public @NotNull
    Result<Service> discoverPortService(Port port)
    {
        trace("Looking up service on port $ with remote registry", port);
        var request = new DiscoverPortServiceRequest().port(port);
        var service = request(port.host().isLocal()
                ? Scope.localhost()
                : Scope.network(), request, DiscoverPortServiceResponse.class).asResult();
        trace("Service on port $: ", port, service);
        return service;
    }

    /**
     * @return All services registered by the given application within the given scope
     */
    public @NotNull
    Result<Set<Service>> discoverServices(Scope scope, Application.Identifier application)
    {
        trace("Discovering $ services of $", scope, application);
        var request = new DiscoverServicesRequest()
                .scope(scope)
                .type(APPLICATION_SERVICES)
                .application(application);

        var result = request(scope, request, DiscoverServicesResponse.class).asResult();
        trace("Discovered services: $", result);
        return result;
    }

    /**
     * @return All services registered with this registry within the given scope
     */
    public @NotNull
    Result<Set<Service>> discoverServices(Scope scope)
    {
        trace("Discovering all $ services", scope);
        var request = new DiscoverServicesRequest()
                .scope(scope)
                .type(ALL_SERVICES);

        var result = request(scope, request, DiscoverServicesResponse.class).asResult();
        trace("Discovered services: $", result);
        return result;
    }

    /**
     * @return All application services of the given type registered within the given scope
     */
    public @NotNull
    Result<Set<Service>> discoverServices(
            Scope scope, Application.Identifier application, ServiceType type)
    {
        trace("Discovering $ $ services of $ in remote registry", scope, type, application);
        var request = new DiscoverServicesRequest()
                .scope(scope)
                .application(application)
                .serviceType(type);

        var result = request(scope, request, DiscoverServicesResponse.class).asResult();
        trace("Discovered services: $", result);
        return result;
    }

    /**
     * @return All services of the given type that have been registered within the given scope
     */
    public @NotNull
    Result<Set<Service>> discoverServices(Scope scope, ServiceType type)
    {
        trace("Discovering $ $ services in remote registry", scope, type);
        var request = new DiscoverServicesRequest()
                .scope(scope)
                .type(SERVICES_OF_TYPE)
                .serviceType(type);

        var result = request(scope, request, DiscoverServicesResponse.class).asResult();
        trace("Discovered services: $", result);
        return result;
    }

    /**
     * Registers a {@link Service}, returning an updated {@link Service} object that has been bound to a unique port on
     * the local host and which has a lease on that port for a few minutes. The resulting registration information will
     * be immediately propagated to the {@link NetworkServiceRegistry} allowing the given service to be located on the
     * network after calling this method.
     *
     * @param service The service to register
     * @return The registered service, bound to a port
     */
    @SuppressWarnings("InfiniteLoopStatement")
    public @NotNull Result<Service> register(Service service)
    {
        // Register the service
        trace("Registering with local registry: $", service);
        service.health(JavaVirtualMachine.local().health());
        var request = new RegisterServiceRequest(service);
        var result = request(Scope.localhost(), request, RegisterServiceResponse.class).asResult();
        if (result.succeeded())
        {
            var registered = result.get();
            if (registered != null)
            {
                // then update the registration (renewing the lease) regularly in the background.
                KivaKitThread.run(this, "ServiceRegistryLeaseUpdater", () ->
                {
                    while (true)
                    {
                        settings().serviceLeaseRenewalFrequency().cycleLength().sleep();
                        trace("Renewing lease on registered service: $", registered);
                        var health = JavaVirtualMachine.local().health();
                        if (health != null)
                        {
                            registered.health(health.update());
                        }
                        var renewal = new RenewServiceRequest(registered);
                        var renewed = request(Scope.localhost(), renewal, RenewServiceResponse.class).asResult();
                        trace("Remote registry renewed service: $", renewed);
                    }
                });
                trace("Local registry bound service: $", result);
            }
            else
            {
                warning("Unable to register service: $", service);
            }
        }

        return result;
    }

    /**
     * Convenience method that registers a service of the given type using the current KivaKit {@link Application} or
     * {@link Server} to fill in the service version and to create an application identifier and service description. If
     * this method is called from an application that is not using the kivakit-application base classes, the best effort
     * is made to fill in service registration details.
     *
     * @param scope The scope within which the service should be visible
     * @param serviceType A unique identifier for the type of service used in search/discovery
     * @param metadata Metadata for the service. If a description is not provided, one will be generated. The KivaKit
     * version and service version will be automatically filled in from the {@link Application} or {@link Server} that
     * is using this client.
     * @return The registered service, or null if it was not possible to do so due to network or server error.
     */
    public Result<Service> register
    (
            Scope scope,
            ServiceType serviceType,
            ServiceMetadata metadata
    )
    {
        // Get the current kivakit application, if any
        var application = Application.get();

        // and this process' id
        var pid = OperatingSystem.get().processIdentifier();

        // then compose an identifier for the service application
        var applicationIdentifier = application == null
                ? new Application.Identifier("Unknown (pid " + OperatingSystem.get().processIdentifier() + ")")
                : application.identifier();

        // and a name to use in the description
        var name = application == null ? "unknown process with pid " + pid : application.name();

        // and finally, add to the metadata
        if (metadata.description() == null)
        {
            metadata.description("Service '" + serviceType + "' for " + name);
        }
        metadata.kivakitVersion(kivakit().projectVersion());
        metadata.version(version());

        // and register the service
        return register(new Service()
                .application(applicationIdentifier)
                .scope(scope)
                .type(serviceType)
                .port(Service.UNBOUND)
                .metadata(metadata));
    }

    /**
     * <b>Not public API</b>
     * <p>
     * Updates the given service in a network-wide registry of KivaKit services. Local service registries running on
     * individual hosts push service registration information to the network service registry via this method when
     * initial service registration and lease renewals occur. Services are transparently looked up in the network
     * service registry client when a network {@link Scope} is searched.
     * </p>
     *
     * @return True if the service was added
     */
    @NotNull
    public Result<Boolean> sendNetworkRegistryUpdate(Service service)
    {
        // The client is used by the local service registry to update the network service registry.
        // This method should not be called by any end-users.
        try
        {
            if (service.isBound())
            {
                trace("Updating network service registry for: $", service);
                var request = new NetworkRegistryUpdateRequest().service(service);
                var result = request(Scope.network(), request, NetworkRegistryUpdateResponse.class).asResult();
                trace("Updated network service registry: $", result);
                return result;
            }
            else
            {
                warning("Cannot send update on unbound service: $", service);
                return success(false);
            }
        }
        catch (Exception | AssertionError e)
        {
            return failure("Unable to update service $ int network registry: $", service.descriptor(), e.getMessage());
        }
    }

    /**
     * @return The version of this service registry client
     */
    public Version version()
    {
        return settings().version();
    }

    /**
     * @return The connected client
     */
    private synchronized ServiceRegistryClient connectToLocalRegistry()
    {
        // If the client is not yet connected to the local registry,
        if (!connected)
        {
            // get the local port and if it is available, the registry is not running,
            var port = settings().local();
            if (port.isAvailable())
            {
                // so launch the service from remote storage
                trace("Connecting client to $", port);
                var local = Folder.kivakitExtensionsHome()
                        .folder("kivakit-service/server/target")
                        .file("kivakit-service-server-" + kivakit().kivakitVersion() + ".jar");
                var jar = settings.serverJar();
                trace("Launching $", jar);
                listenTo(new JarLauncher())
                        .processType(DETACHED)
                        .addJarSource(local)
                        .addJarSource(jar)
                        .run();

                // and wait until the port is no longer available, which means that the server is ready
                trace("Waiting for registry to start up");
                while (port.isAvailable())
                {
                    Duration.seconds(1).sleep();
                }
                trace("Registry is started");
            }
            connected = true;
        }
        return this;
    }

    private Gson gson()
    {
        return listenTo(new ServiceRegistryGsonFactorySource()).gsonFactory().gson();
    }

    /**
     * Connects to the appropriate server for the given request scope, sends a request object and returns the response.
     */
    private <T, Response extends BaseResponse<T>> Response request(
            Scope scope, BaseRequest request, Class<Response> responseType)
    {
        // If we're searching locally,
        if (scope.isLocal())
        {
            // connect to the local registry, possibly launching it if it isn't running,
            connectToLocalRegistry();
        }

        // then create a new Jersey client,
        var timeout = settings.accessTimeout();
        Client client = JerseyClientBuilder.newBuilder()
                .connectTimeout((long) timeout.asSeconds(), TimeUnit.SECONDS)
                .build();

        // turn the request into JSON,
        var requestJson = gson().toJson(request);
        trace("Sending JSON ${class} to $/$:\n$", request.getClass(), settings().restApiPath(),
                request.path(), requestJson);

        // get the appropriate server to contact based on the scope,
        Port server;
        switch (scope.type())
        {
            case LOCALHOST:
                server = settings().local();
                break;

            case NETWORK:
            case CLUSTER:
                server = settings().network();
                break;

            default:
                return unsupported("Scope type '$' is not supported", scope.type());
        }

        // If the server host can be resolved,
        if (server.host().isResolvable())
        {
            // compose and post a request to the server,
            try
            {
                var entity = Entity.entity(requestJson, "application/json");
                var path = server
                        .path(this, settings().restApiPath())
                        .withChild(request.path());
                trace("Posting $ to $", request.getClass().getSimpleName(), path);
                var jaxResponse = client
                        .target(path.toString())
                        .request("application/json")
                        .post(entity, javax.ws.rs.core.Response.class);

                // read the JSON response,
                var responseJson = jaxResponse.readEntity(String.class);

                // and convert the response to an object.
                var response = gson().fromJson(responseJson, responseType);
                trace("Received JSON ${class}:\n$", response.getClass(), responseJson);

                return response;
            }
            catch (Exception e)
            {
                if (e instanceof ProcessingException && e.getCause() instanceof ConnectException)
                {
                    warning(e, "Unable to connect to host $", server);
                }
                else
                {
                    problem(e, "Failure trying to connect to $", server);
                }
            }
        }

        // If the host is not resolvable or an exception occurred, so fail
        var response = (Response) Type.forClass(responseType).newInstance();
        response.problem("Unable to connect to $", server);
        problem("Could not resolve ${lower} registry on host $", scope, server.host().name());
        return response;
    }

    private ServiceRegistrySettings settings()
    {
        return require(ServiceRegistrySettings.class);
    }
}
