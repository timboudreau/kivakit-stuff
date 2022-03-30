open module kivakit.service.registry
{
    // KivaKit
    requires transitive kivakit.microservice;
    requires transitive kivakit.network.core;
    requires transitive kivakit.serialization.core;
    requires transitive kivakit.serialization.kryo;
    requires transitive kivakit.serialization.gson;
    requires transitive kivakit.web.jersey;

    // Jackson
    requires com.fasterxml.jackson.annotation;

    // Module exports
    exports com.telenav.kivakit.service.registry;
    exports com.telenav.kivakit.service.registry.lexakai;
    exports com.telenav.kivakit.service.registry.protocol;
    exports com.telenav.kivakit.service.registry.registries;
    exports com.telenav.kivakit.service.registry.store;
    exports com.telenav.kivakit.service.registry.protocol.discover;
    exports com.telenav.kivakit.service.registry.protocol.update;
    exports com.telenav.kivakit.service.registry.protocol.register;
    exports com.telenav.kivakit.service.registry.protocol.renew;
    exports com.telenav.kivakit.service.registry.serialization;
}
