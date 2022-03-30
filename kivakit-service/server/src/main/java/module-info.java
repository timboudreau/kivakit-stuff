open module kivakit.service.server
{
    // KivaKit
    requires kivakit.application;
    requires kivakit.service.client;
    requires kivakit.web.jetty;
    requires kivakit.web.wicket;
    requires kivakit.web.jersey;
    requires kivakit.web.swagger;

    // Swagger annotations
    requires io.swagger.v3.oas.annotations;

    // Module exports
    exports com.telenav.kivakit.service.registry.server.lexakai;
    exports com.telenav.kivakit.service.registry.server.rest;
    exports com.telenav.kivakit.service.registry.server.webapp.pages.home;
    exports com.telenav.kivakit.service.registry.server.webapp;
    exports com.telenav.kivakit.service.registry.server;
}
