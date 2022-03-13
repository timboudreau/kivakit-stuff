open module kivakit.serialization.yaml
{
    // KivaKit
    requires transitive kivakit.conversion;
    requires transitive kivakit.resource;

    // JSON
    requires yamlbeans;

    // Module exports
    exports com.telenav.kivakit.serialization.yaml;
}
