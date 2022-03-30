open module kivakit.service.client
{
    // KivaKit
    requires transitive kivakit.application;
    requires transitive kivakit.service.registry;
    requires transitive kivakit.network.http;

    // Jersey and XML binding
    requires jersey.client;
    requires java.ws.rs;
    requires gson;

    // Module exports
    exports com.telenav.kivakit.service.registry.client;
    exports com.telenav.kivakit.service.registry.client.lexakai;
}
