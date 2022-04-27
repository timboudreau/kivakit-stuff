open module kivakit.primitive.collections
{
    // KivaKit
    requires transitive kivakit.serialization.core;
    requires transitive kivakit.serialization.kryo;
    requires transitive kivakit.collections;
    requires transitive kivakit.conversion;
    requires transitive kivakit.test;
    requires transitive kivakit.test.internal;

    // Module exports
    exports com.telenav.kivakit.primitive.collections;
    exports com.telenav.kivakit.primitive.collections.array;
    exports com.telenav.kivakit.primitive.collections.array.arrays;
    exports com.telenav.kivakit.primitive.collections.array.bits.io.input;
    exports com.telenav.kivakit.primitive.collections.array.bits.io.output;
    exports com.telenav.kivakit.primitive.collections.array.bits.io;
    exports com.telenav.kivakit.primitive.collections.array.bits;
    exports com.telenav.kivakit.primitive.collections.array.packed;
    exports com.telenav.kivakit.primitive.collections.array.scalars;
    exports com.telenav.kivakit.primitive.collections.array.strings;
    exports com.telenav.kivakit.primitive.collections.iteration;
    exports com.telenav.kivakit.primitive.collections.list;
    exports com.telenav.kivakit.primitive.collections.list.adapters;
    exports com.telenav.kivakit.primitive.collections.list.store;
    exports com.telenav.kivakit.primitive.collections.map;
    exports com.telenav.kivakit.primitive.collections.map.multi.dynamic;
    exports com.telenav.kivakit.primitive.collections.map.scalars.fixed;
    exports com.telenav.kivakit.primitive.collections.map.objects;
    exports com.telenav.kivakit.primitive.collections.map.scalars;
    exports com.telenav.kivakit.primitive.collections.map.split;
    exports com.telenav.kivakit.primitive.collections.set;
    exports com.telenav.kivakit.primitive.collections.map.multi;
    exports com.telenav.kivakit.primitive.collections.lexakai;
}
