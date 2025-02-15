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

package com.telenav.kivakit.primitive.collections;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.telenav.kivakit.collections.map.ConcurrentCountMap;
import com.telenav.kivakit.core.KivaKit;
import com.telenav.kivakit.core.collections.list.ObjectList;
import com.telenav.kivakit.core.language.Classes;
import com.telenav.kivakit.core.language.primitive.Booleans;
import com.telenav.kivakit.core.logging.Logger;
import com.telenav.kivakit.core.logging.LoggerFactory;
import com.telenav.kivakit.core.logging.logs.BaseLog;
import com.telenav.kivakit.core.messaging.Debug;
import com.telenav.kivakit.core.messaging.context.CallStack;
import com.telenav.kivakit.core.messaging.messages.status.activity.Activity;
import com.telenav.kivakit.core.string.Indent;
import com.telenav.kivakit.core.string.Strings;
import com.telenav.kivakit.core.time.Duration;
import com.telenav.kivakit.core.time.Frequency;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.core.value.count.BaseCount;
import com.telenav.kivakit.core.value.count.Bytes;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Countable;
import com.telenav.kivakit.core.value.count.Estimate;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.core.version.Version;
import com.telenav.kivakit.core.vm.JavaVirtualMachine;
import com.telenav.kivakit.core.vm.Properties;
import com.telenav.kivakit.core.vm.ShutdownHook;
import com.telenav.kivakit.interfaces.code.TripwireTrait;
import com.telenav.kivakit.interfaces.collection.Sized;
import com.telenav.kivakit.interfaces.lifecycle.Initializable;
import com.telenav.kivakit.interfaces.naming.Named;
import com.telenav.kivakit.interfaces.naming.NamedObject;
import com.telenav.kivakit.primitive.collections.array.PrimitiveArray;
import com.telenav.kivakit.primitive.collections.array.PrimitiveArrayArray;
import com.telenav.kivakit.primitive.collections.array.PrimitiveSplitArray;
import com.telenav.kivakit.primitive.collections.array.arrays.ByteArrayArray;
import com.telenav.kivakit.primitive.collections.array.scalars.ByteArray;
import com.telenav.kivakit.primitive.collections.array.scalars.IntArray;
import com.telenav.kivakit.primitive.collections.array.scalars.SplitIntArray;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveCollection;
import com.telenav.kivakit.primitive.collections.list.store.PackedStringStore;
import com.telenav.kivakit.primitive.collections.list.store.PrimitiveListStore;
import com.telenav.kivakit.primitive.collections.map.PrimitiveMap;
import com.telenav.kivakit.primitive.collections.map.multi.PrimitiveMultiMap;
import com.telenav.kivakit.primitive.collections.map.multi.dynamic.LongToLongMultiMap;
import com.telenav.kivakit.primitive.collections.map.scalars.LongToIntMap;
import com.telenav.kivakit.primitive.collections.map.scalars.LongToLongMap;
import com.telenav.kivakit.primitive.collections.map.split.SplitLongToIntMap;
import com.telenav.kivakit.primitive.collections.set.PrimitiveSet;
import com.telenav.lexakai.annotations.UmlClassDiagram;
import org.jetbrains.annotations.MustBeInvokedByOverriders;

import java.util.Arrays;
import java.util.Collections;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicLong;

import static com.telenav.kivakit.core.messaging.context.CallStack.Matching.SUBCLASS;
import static com.telenav.kivakit.core.messaging.context.CallStack.Proximity.IMMEDIATE;
import static com.telenav.kivakit.core.project.Project.resolveProject;
import static com.telenav.kivakit.core.vm.ShutdownHook.Order.FIRST;

/**
 * Base class for all primitive collections, which come in two types:
 * <p>
 * <b>One-Dimensional Collections</b>
 * <p>
 * One-dimensional collections have only one key (or index) and their size is configured with {@link
 * #initialSize(Estimate)} and {@link #maximumSize(Maximum)}. Examples of one-dimensional collections include {@link
 * ByteArray}, {@link IntArray} and {@link LongToLongMap}), since only one key is needed to access data in each case.
 * Note that "split" versions of arrays and maps (to avoid allocating large contiguous blocks of memory that make the
 * garbage collector's job harder) like {@link SplitIntArray} and {@link SplitLongToIntMap} are also one-dimensional
 * even though they have child collections internally because they still only require a single key.
 * <p>
 * <b>Two-Dimensional Collections</b>
 * <p>
 * Two-dimensional collections have two keys (or indexes) and the second dimension can be configured with {@link
 * #initialChildSize(BaseCount)} and {@link #maximumChildSize(Maximum)}. Examples of two-dimensional collections include
 * {@link ByteArrayArray} and {@link LongToLongMultiMap}. The first key gets access to a child collection (usually an
 * array or list) and the second key gets access to the data.
 * <p>
 * <b>Default Sizes</b>
 * <ul>
 *     <li>maximumSize = Integer.MAXIMUM</li>
 *     <li>initialSize = 256</li>
 *     <li>childSize = 65,536</li>
 *     <li>maximumChildSize = 65,536</li>
 * </ul>
 * <p>
 * The current size of the collection can be retrieved with {@link Sized#size()} or as a {@link Count} object with
 * {@link #count()}. For convenience, {@link #isEmpty()} returns true when {@link #size()} is zero.
 * <p>
 * Primitive collections can optionally have a value selected to represent no value or 'null'. The methods nullX(value)
 * can be used to specify this null value for type X and nullX() can be used to retrieve it. In cases where a null value
 * is not desired, for example in a collection of bytes that includes the value Byte.MIN_VALUE (-128), the
 * hasNullX(boolean) methods can be used to specify this and the hasNullX() method can be used to determine if there is
 * a null value for type X.
 * <p>
 * <b>Default Null Values</b>
 * <ul>
 *     <li>hasNullLong = true</li>
 *     <li>hasNullInt = true</li>
 *     <li>hasNullShort = true</li>
 *     <li>hasNullChar = true</li>
 *     <li>hasNullChar = true</li>
 *     <li>nullLong = 0</li>
 *     <li>nullInt = 0</li>
 *     <li>nullShort = 0</li>
 *     <li>nullChar = 0</li>
 *     <li>nullByte = 0</li>
 * </ul>
 * <p>
 * The default value for null is always zero, as shown above. This allows for better performance since Java always zeroes
 * out new data structures like arrays. However, if zero is a desired element value, it will be necessary to pick a different
 * null value, or to specify that there is none. Note that collections may specify more than one null value, for example
 * a {@link LongToIntMap} can specify a null long value and a null int value.
 * <p>
 * {@link PrimitiveCollection}s are {@link KryoSerializable} and {@link CompressibleCollection}. Freezing objects can reduce space to
 * save memory, although this will typically slow access a small amount.
 * <p>
 * Tracing of allocations can be enabled through {@link Debug} using -DKIVAKIT_DEBUG=PrimitiveCollection and details on
 * allocation-related activities can be increased with:
 * <p>
 * <b>Java Properties</b>
 * <ul>
 *     <li>KIVAKIT_LOG_ALLOCATIONS - log all allocations greater than the minimum size</li>
 *     <li>KIVAKIT_LOG_ALLOCATIONS_MINIMUM_SIZE - the minimum size to log (see {@link Bytes}, default is 64k)</li>
 *     <li>KIVAKIT_LOG_ALLOCATION_STACK_TRACES - log stack traces for allocations</li>
 * </ul>
 * <i>All of these debugging options are expensive and should only be used during debugging.</i>
 *
 * @author jonathanl (shibo)
 * @see CompressibleCollection
 * @see KryoSerializable
 * @see Bytes
 * @see Debug
 * @see Sized
 */
@SuppressWarnings({ "unused", "UnusedReturnValue" }) @UmlClassDiagram(diagram = DiagramPrimitiveCollection.class)
public abstract class PrimitiveCollection implements
        KryoSerializable,
        NamedObject,
        Initializable,
        Countable,
        CompressibleCollection,
        TripwireTrait
{
    /** The number of elements to show when converting a collection to a String */
    protected static final int TO_STRING_MAXIMUM_ELEMENTS = 20;

    private static final Logger LOGGER = LoggerFactory.newLogger();

    private static final Debug DEBUG = new Debug(LOGGER);

    /** Total number of bytes of primitive arrays allocated */
    private static final AtomicLong totalAllocated = new AtomicLong();

    /** True if allocations should be logged */
    private static Boolean logAllocations;

    /** The bytes allocated by an individual allocator */
    private static final ConcurrentCountMap<String> totalAllocatedByAllocator = new ConcurrentCountMap<>();

    /** The number of allocations of collections with a given {@link NamedObject#objectName()} */
    private static final ConcurrentCountMap<String> allocations = new ConcurrentCountMap<>();

    /** The minimum size of a collection considered "big" */
    private static final int LARGE_ALLOCATION = 5_000_000;

    /** A record of significant compression events */
    private static ObjectList<CompressionRecord> compressionRecords = new ObjectList<>();

    static
    {
        ShutdownHook.register(FIRST, () ->
        {
            compressionRecords = compressionRecords.uniqued();
            Collections.sort(compressionRecords);
            var totalDelta = 0L;
            for (var trim : compressionRecords)
            {
                totalDelta += Math.abs(trim.delta());
            }

            DEBUG.trace("Compressed collections by $:\n$", Bytes.bytes(totalDelta), compressionRecords.bulleted());
            LOGGER.flush();
        });
    }

    /**
     * @return The given size, increased (used only by collection implementations)
     */
    public static int increasedCapacity(int size)
    {
        if (size < 128)
        {
            return 512;
        }
        else if (size < 1_048_576)
        {
            return size * 2;
        }
        else
        {
            return Math.min(Integer.MAX_VALUE - 8, size * 3 / 2);
        }
    }

    /** Used when converting an indexed collection to a String */
    protected interface IndexedToString
    {
        String toString(int index);
    }

    /** An allocation stack trace */
    public static class AllocationStackTrace extends RuntimeException
    {
    }

    /**
     * Holds a record of significant collection size changes due to calls to {@link #compress(Method)} when debug is
     * enabled for this class. A sorted list of all compression events is presented on program termination using a
     * shutdown-hook.
     */
    private static class CompressionRecord implements Comparable<CompressionRecord>
    {
        final Class<?> type;

        final String objectName;

        final int before;

        final int after;

        CompressionRecord(Class<?> type, String objectName, int before, int after)
        {
            this.type = type;
            this.objectName = objectName;
            this.before = before;
            this.after = after;
        }

        @Override
        public int compareTo(PrimitiveCollection.CompressionRecord that)
        {
            int result = Double.compare(percentage(), that.percentage());
            if (result == 0)
            {
                result = objectName.compareTo(that.objectName);
            }
            return result;
        }

        @Override
        public boolean equals(Object object)
        {
            if (object instanceof CompressionRecord)
            {
                CompressionRecord that = (CompressionRecord) object;
                return name().equals(that.name());
            }
            return false;
        }

        @Override
        public int hashCode()
        {
            return Objects.hash(name());
        }

        @Override
        public String toString()
        {
            return Strings.format("${double}% ($) - ${class} $ $ -> $",
                    percentage(), delta(), type, objectName, before, after);
        }

        int delta()
        {
            return after - before;
        }

        double percentage()
        {
            return 100.0 * delta() / before;
        }

        private String name()
        {
            return type + "-" + objectName;
        }
    }

    /** Any compression method that has been applied to this collection (see {@link CompressibleCollection}) */
    private Method compressionMethod = Method.NONE;

    /** True if this collection defines a null byte value */
    private boolean hasNullByte = true;

    /** True if this collection defines a null char value */
    private boolean hasNullChar = true;

    /** True if this collection defines a null int value */
    private boolean hasNullInt = true;

    /** True if this collection defines a null long value */
    private boolean hasNullLong = true;

    /** True if this collection defines a null short value */
    private boolean hasNullShort = true;

    /** The size of any child collections */
    private int initialChildSize = 65_536;

    /** The initial size of this collection */
    private int initialSize = 2_048;

    /**
     * True if the collection is initialized
     */
    private boolean initialized;

    /** Minimum size of allocations which should be logged */
    private int logAllocationsMinimumSize;

    /** The maximum size of any child collections */
    private int maximumChildSize = 262_144;

    /** The maximum size of this collection */
    private int maximumSize = Integer.MAX_VALUE;

    /** Null byte value for this collection if hasNullByte is true */
    private byte nullByte;

    /** Null char value for this collection if hasNullChar is true */
    private char nullChar;

    /** Null int value for this collection if hasNullInt is true */
    private int nullInt;

    /** Null long value for this collection if hasNullLong is true */
    private long nullLong;

    /** Null short value for this collection if hasNullShort is true */
    private short nullShort;

    /** The name of this collection for debugging purposes */
    private transient String objectName;

    /** The size of this collection */
    private int size;

    /**
     * A {@link PrimitiveCollection} is assigned an object name, retrievable through {@link NamedObject#objectName()},
     * and it is initialized with an initializer that permits configuration of the collections minimum and estimated
     * sizes and what values should be used to represent null. If no initializer is provided all defaults will be used.
     *
     * @param objectName The name of this collection object
     */
    protected PrimitiveCollection(String objectName)
    {
        assert objectName != null;

        this.objectName = objectName;

        // If we are logging allocations
        if (logAllocations())
        {
            // and the size is "big"
            if (initialSize > LARGE_ALLOCATION)
            {
                // then show just how big.
                if (this instanceof PrimitiveMultiMap || this instanceof PrimitiveArrayArray)
                {
                    if (DEBUG.isDebugOn())
                    {
                        LOGGER.log(new Activity("Collection '$' ($ x $ = $) is big",
                                objectName, initialSize(), initialChildSize(), initialSize().times(initialChildSize())));
                    }
                }
                else
                {
                    if (DEBUG.isDebugOn())
                    {
                        LOGGER.log(new Activity("Collection '$' ($) is big", objectName, initialSize()));
                    }
                }
            }

            // If an object with a given name has been allocated "a lot"
            if (allocations.count(objectName).isGreaterThan(Count._1024))
            {
                // then report this, but only once
                if (DEBUG.isDebugOn())
                {
                    LOGGER.log(new Activity("Collection '$' has been allocated a lot", objectName).maximumFrequency(Frequency.ONCE));
                }
            }
        }

        if (logAllocations())
        {
            allocated(objectName, "created", this, initialSize, initialChildSize);
        }
    }

    protected PrimitiveCollection()
    {
    }

    public abstract Count capacity();

    @MustBeInvokedByOverriders
    public void clear()
    {
        assert compressionMethod != Method.FREEZE;
        size(0);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Method compress(Method method)
    {
        assert initialized : "Collection " + objectName() + " not initialized";

        // If the collection has been initialized, and it has not already been compressed,
        if (isInitialized() && !isCompressed())
        {
            // get the capacity in elements before compressing
            var before = capacity().asInt();

            // then compress the collection
            var start = Time.now();
            var big = before > 1_000_000;
            if (big)
            {
                DEBUG.trace("Compressing $", objectName());
            }

            compressionMethod = onCompress(method);

            var elapsed = start.elapsedSince();
            if (big && elapsed.isGreaterThan(Duration.ONE_SECOND))
            {
                DEBUG.trace("Compressed $ in $", objectName(), start.elapsedSince());
            }

            // and ensure that some kind of compression was declared (even NONE)
            assert compressionMethod != null;

            // and then if debug is enabled
            if (DEBUG.isDebugOn())
            {
                // and the collection was at least 64K
                var after = capacity().asInt();
                if (before > 65_536 && before != after)
                {
                    // then add a compression record to be dumped out on exit
                    var record = new CompressionRecord(getClass(), objectName(), before, after);
                    compressionRecords.add(record);

                    // and write trace the record as well.
                    DEBUG.trace(record.toString());
                }
            }
        }

        return compressionMethod;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public final Method compressionMethod()
    {
        return compressionMethod;
    }

    /**
     * Copies the sizes and null values from the given collection
     */
    @MustBeInvokedByOverriders
    public void copySettings(PrimitiveCollection that)
    {
        initialSize = that.initialSize;
        maximumSize = that.maximumSize;

        initialChildSize = that.initialChildSize;
        maximumChildSize = that.maximumChildSize;

        copyNullValues(that);
    }

    public void copyNullValues(PrimitiveCollection that)
    {
        hasNullLong = that.hasNullLong;
        hasNullInt = that.hasNullInt;
        hasNullShort = that.hasNullShort;
        hasNullChar = that.hasNullChar;
        hasNullByte = that.hasNullByte;

        nullLong = that.nullLong;
        nullInt = that.nullInt;
        nullShort = that.nullShort;
        nullChar = that.nullChar;
        nullByte = that.nullByte;
    }

    /**
     * @return Size of collection as a {@link Count} object
     */
    @Override
    public final Count count()
    {
        return Count.count(size());
    }

    public final PrimitiveCollection hasNullByte(boolean value)
    {
        hasNullByte = value;
        return this;
    }

    public final boolean hasNullByte()
    {
        return hasNullByte;
    }

    public final PrimitiveCollection hasNullChar(boolean value)
    {
        hasNullChar = value;
        return this;
    }

    public final boolean hasNullChar()
    {
        return hasNullChar;
    }

    public final PrimitiveCollection hasNullInt(boolean value)
    {
        hasNullInt = value;
        return this;
    }

    public final boolean hasNullInt()
    {
        return hasNullInt;
    }

    public final PrimitiveCollection hasNullLong(boolean value)
    {
        hasNullLong = value;
        return this;
    }

    public final boolean hasNullLong()
    {
        return hasNullLong;
    }

    public final PrimitiveCollection hasNullShort(boolean value)
    {
        hasNullShort = value;
        return this;
    }

    public final boolean hasNullShort()
    {
        return hasNullShort;
    }

    public Estimate initialChildCount()
    {
        return Estimate.estimate(initialChildCountAsInt());
    }

    public int initialChildCountAsInt()
    {
        return Math.max(initialSize / initialChildSize, 1);
    }

    public final Estimate initialChildSize()
    {
        return Estimate.estimate(initialChildSize);
    }

    public PrimitiveCollection initialChildSize(BaseCount<?> childSize)
    {
        initialChildSize(childSize.asInt());
        return this;
    }

    public PrimitiveCollection initialChildSize(int childSize)
    {
        initialChildSize = childSize;
        return this;
    }

    public final int initialChildSizeAsInt()
    {
        return initialChildSize;
    }

    public Estimate initialSize()
    {
        return Estimate.estimate(initialSize);
    }

    public final PrimitiveCollection initialSize(Estimate initialSize)
    {
        initialSize(initialSize.asInt());
        return this;
    }

    public final PrimitiveCollection initialSize(int initialSize)
    {
        this.initialSize = initialSize;
        return this;
    }

    public final int initialSizeAsInt()
    {
        return initialSize;
    }

    @Override
    public final void initialize()
    {
        if (!initialized)
        {
            onInitialize();
            initialized = true;
        }
    }

    /**
     * @return True if this set is empty
     */
    @Override
    public boolean isEmpty()
    {
        assert initialized : "Collection " + objectName() + " not initialized";
        return size() == 0;
    }

    public boolean isInitialized()
    {
        return initialized;
    }

    /**
     * @return True if the value is the "null" byte value
     */
    public final boolean isNull(byte value)
    {
        assert initialized : "Collection " + objectName() + " not initialized";
        return hasNullByte && nullByte == value;
    }

    /**
     * @return True if the value is the "null" integer value
     */
    public final boolean isNull(int value)
    {
        assert initialized : "Collection " + objectName() + " not initialized";
        return hasNullInt && nullInt == value;
    }

    /**
     * @return True if the value is the "null" long value
     */
    public final boolean isNull(long value)
    {
        assert initialized : "Collection " + objectName() + " not initialized";
        return hasNullLong && nullLong == value;
    }

    /**
     * @return True if the value is the "null" short value
     */
    public final boolean isNull(short value)
    {
        assert initialized : "Collection " + objectName() + " not initialized";
        return hasNullShort && nullShort == value;
    }

    /**
     * @return True if the value is the "null" char value
     */
    public final boolean isNull(char value)
    {
        assert initialized : "Collection " + objectName() + " not initialized";
        return hasNullChar && nullChar == value;
    }

    public final Maximum maximumChildSize()
    {
        return Maximum.maximum(maximumChildSize);
    }

    public PrimitiveCollection maximumChildSize(Maximum maximumChildSize)
    {
        maximumChildSize(maximumChildSize.asInt());
        return this;
    }

    public PrimitiveCollection maximumChildSize(int maximumChildSize)
    {
        this.maximumChildSize = maximumChildSize;
        return this;
    }

    public final int maximumChildSizeAsInt()
    {
        return maximumChildSize;
    }

    public PrimitiveCollection maximumSize(Maximum maximumSize)
    {
        maximumSize(maximumSize.asInt());
        return this;
    }

    public final Maximum maximumSize()
    {
        return Maximum.maximum(maximumSize);
    }

    public PrimitiveCollection maximumSize(int maximumSize)
    {
        this.maximumSize = maximumSize;
        return this;
    }

    public final int maximumSizeAsInt()
    {
        return maximumSize;
    }

    public final byte nullByte()
    {
        return nullByte;
    }

    public final PrimitiveCollection nullByte(byte value)
    {
        hasNullByte = true;
        nullByte = value;
        return this;
    }

    public final char nullChar()
    {
        return nullChar;
    }

    public final PrimitiveCollection nullChar(char value)
    {
        hasNullChar = true;
        nullChar = value;
        return this;
    }

    public final int nullIndex()
    {
        return -1;
    }


    public final int nullInt()
    {
        return nullInt;
    }

    public final PrimitiveCollection nullInt(int value)
    {
        hasNullInt = true;
        nullInt = value;
        return this;
    }

    public final long nullLong()
    {
        return nullLong;
    }

    public PrimitiveCollection nullLong(long value)
    {
        hasNullLong = true;
        nullLong = value;
        return this;
    }

    public final short nullShort()
    {
        return nullShort;
    }

    public final PrimitiveCollection nullShort(short value)
    {
        hasNullShort = true;
        nullShort = value;
        return this;
    }

    @Override
    public String objectName()
    {
        return objectName;
    }

    @Override
    public void objectName(String objectName)
    {
        this.objectName = objectName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @MustBeInvokedByOverriders
    public void read(Kryo kryo, Input input)
    {
        // NOTE: In the future, this version may be used.
        kryo.readObject(input, Version.class);

        objectName = kryo.readObject(input, String.class);
        size = kryo.readObject(input, int.class);
        compressionMethod = kryo.readObject(input, Method.class);
        initialized = kryo.readObject(input, boolean.class);

        maximumSize = kryo.readObject(input, int.class);
        initialSize = kryo.readObject(input, int.class);
        maximumChildSize = kryo.readObject(input, int.class);
        initialChildSize = kryo.readObject(input, int.class);

        hasNullLong = kryo.readObject(input, boolean.class);
        hasNullInt = kryo.readObject(input, boolean.class);
        hasNullShort = kryo.readObject(input, boolean.class);
        hasNullChar = kryo.readObject(input, boolean.class);
        hasNullByte = kryo.readObject(input, boolean.class);

        nullLong = kryo.readObject(input, Long.class);
        nullInt = kryo.readObject(input, Integer.class);
        nullShort = kryo.readObject(input, Short.class);
        nullChar = kryo.readObject(input, Character.class);
        nullByte = kryo.readObject(input, Byte.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        assert initialized : "Collection " + objectName() + " not initialized";
        return size;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @MustBeInvokedByOverriders
    @SuppressWarnings("DuplicatedCode")
    public void write(Kryo kryo, Output output)
    {
        kryo.writeObject(output, resolveProject(KivaKit.class).projectVersion());
        kryo.writeObject(output, objectName);
        kryo.writeObject(output, size);
        kryo.writeObject(output, compressionMethod);
        kryo.writeObject(output, initialized);

        kryo.writeObject(output, maximumSize);
        kryo.writeObject(output, initialSize);
        kryo.writeObject(output, maximumChildSize);
        kryo.writeObject(output, initialChildSize);

        kryo.writeObject(output, hasNullLong());
        kryo.writeObject(output, hasNullInt());
        kryo.writeObject(output, hasNullShort());
        kryo.writeObject(output, hasNullChar());
        kryo.writeObject(output, hasNullByte());

        kryo.writeObject(output, nullLong);
        kryo.writeObject(output, nullInt);
        kryo.writeObject(output, nullShort);
        kryo.writeObject(output, nullChar);
        kryo.writeObject(output, nullByte);
    }

    protected final <T> T allocated(Object who, String why, T what, int initialSize)
    {
        return allocated(who, why, what, initialSize, -1);
    }

    protected final <T> T allocated(Object allocator,
                                    String why,
                                    T allocated,
                                    int initialSize,
                                    int estimatedChildSize)
    {
        // If we want to log allocations and the size of the objects is big enough,
        if (logAllocations() && initialSize >= logAllocationsMinimumSize())
        {
            // Get the name of who did the allocation
            String who;
            if (allocator instanceof String)
            {
                who = allocator.toString();
            }
            else
            {
                if (allocator != null)
                {
                    if (allocator instanceof Named)
                    {
                        who = ((Named) allocator).name();
                    }
                    else
                    {
                        who = Classes.simpleName(allocator.getClass());
                    }
                }
                else
                {
                    // Get the caller who is constructing this collection
                    var method = CallStack.callerOf(IMMEDIATE, SUBCLASS, PrimitiveCollection.class,
                            SUBCLASS, PrimitiveArray.class, PrimitiveSplitArray.class, PrimitiveArrayArray.class,
                            PrimitiveMap.class, PrimitiveSet.class, PrimitiveListStore.class, PackedStringStore.class);
                    if (method != null)
                    {
                        var caller = method.type();
                        who = caller.simpleName();
                    }
                    else
                    {
                        who = null;
                    }
                }
            }

            assert who != null;

            // Get the name of what was allocated
            String what;
            var type = allocated.getClass();
            if (type.isArray())
            {
                what = type.getComponentType().getSimpleName();
            }
            else
            {
                what = type.getSimpleName();
            }

            // If we want to record stack traces, get a stack trace by creating a throwable (AllocationStackTrace)
            AllocationStackTrace stack = null;
            if (Booleans.isTrue(Properties.property("KIVAKIT_LOG_ALLOCATION_STACK_TRACES", "false")))
            {
                stack = new AllocationStackTrace();
            }

            // If there is a child (as in a multi-map),
            if (estimatedChildSize != -1)
            {
                // show both dimensions of the primitive collection
                LOGGER.log(new Activity(stack, "$ $ $($ x $ = $)", who, why, what, Count.count(initialSize),
                        Count.count(estimatedChildSize), Count.count(initialSize / estimatedChildSize).minimum(Count._16)));
            }
            else
            {
                // If the collection is a primitive array and not a primitive collection,
                if (type.isArray())
                {
                    // get the component type of the collection,
                    var componentType = type.getComponentType();

                    // compute the size of this object from the estimated size (initial capacity)
                    // and the size of the primitive type in bytes,
                    var size = JavaVirtualMachine.local().sizeOfPrimitiveType(componentType).times(initialSize);

                    // add to the total
                    var total = totalAllocated.addAndGet(size.asBytes());
                    totalAllocatedByAllocator.add(who, size);

                    // and then show what exactly was allocated
                    if (DEBUG.isDebugOn())
                    {
                        LOGGER.log(new Activity(stack, "$ $ $[$] $ (total $, $)", who, why, what, initialSize,
                                size, Bytes.bytes(totalAllocatedByAllocator.count(who)), Bytes.bytes(total)));
                    }
                }
                else
                {
                    // show the number of non-primitives allocated
                    if (DEBUG.isDebugOn())
                    {
                        LOGGER.log(new Activity(stack, "$ $ $ $\n$", who, why, what, initialSize, allocated));
                    }
                }
            }

            // If our object is "big",
            if (initialSize > LARGE_ALLOCATION)
            {
                // show an allocation trace
                if (estimatedChildSize != -1)
                {
                    if (DEBUG.isDebugOn())
                    {
                        LOGGER.log(new Activity("$ allocated large primitive collection $[$][$] (total $)", who, what, initialSize,
                                estimatedChildSize, Bytes.bytes(totalAllocated.get())));
                    }
                }
                else
                {
                    if (DEBUG.isDebugOn())
                    {
                        LOGGER.log(new Activity("$ allocated large primitive collection $[$] (total $)", who, what, initialSize,
                                Bytes.bytes(totalAllocated.get())));
                    }
                }
            }
        }
        return allocated;
    }

    protected final void clear(byte[] values)
    {
        if (hasNullByte())
        {
            Arrays.fill(values, nullByte());
        }
    }

    protected final void clear(int[] values)
    {
        if (hasNullInt())
        {
            Arrays.fill(values, nullInt());
        }
    }

    protected final void clear(char[] values)
    {
        if (hasNullInt())
        {
            Arrays.fill(values, nullChar());
        }
    }

    protected final void clear(long[] values)
    {
        if (hasNullLong())
        {
            Arrays.fill(values, nullLong());
        }
    }

    protected final void clear(Object[] values)
    {
        Arrays.fill(values, null);
    }

    protected final void clear(short[] values)
    {
        if (hasNullShort())
        {
            Arrays.fill(values, nullShort());
        }
    }

    protected final void clear(String[] values)
    {
        Arrays.fill(values, null);
    }

    /**
     * Copies the contents of another collection into this collection
     */
    @MustBeInvokedByOverriders
    protected final void copy(PrimitiveCollection that)
    {
        size = that.size;
    }

    protected final void decreaseSize(int count)
    {
        assert initialized : "Collection " + objectName() + " not initialized";

        size -= count;
    }

    protected final boolean ensureHasRoomFor(int increase)
    {
        assert initialized : "Collection " + objectName() + " not initialized";

        if (size + increase > maximumSize)
        {
            LOGGER.warning(Frequency.EVERY_5_SECONDS, new AllocationStackTrace(), "Maximum size of ${debug} elements would have been exceeded. " +
                    "Ignoring operation (this is not an exception, just a warning)", maximumSize);
            return false;
        }

        return true;
    }

    protected final void ensureIndexInRange(int index)
    {
        assert initialized : "Collection " + objectName() + " not initialized";

        assert index >= 0 : "Index " + index + " must be >= 0";
        assert index < maximumSizeAsInt() : "Index " + index + " must be < " + maximumSizeAsInt();
    }

    protected final int increaseSize(int count)
    {
        assert initialized : "Collection " + objectName() + " not initialized";

        size += count;
        return size;
    }

    protected final int incrementSize()
    {
        assert initialized : "Collection " + objectName() + " not initialized";

        return ++size;
    }

    @SuppressWarnings("SameParameterValue")
    protected byte[] newByteArray(Object who, String why)
    {
        return newByteArray(who, why, initialSize());
    }

    protected byte[] newByteArray(Object who, String why, BaseCount<?> size)
    {
        return newByteArray(who, why, size.asInt());
    }

    protected byte[] newByteArray(Object who, String why, int size)
    {
        tracePrimitiveAllocation(size);
        var values = new byte[size];
        if (nullByte() != 0)
        {
            clear(values);
        }
        return allocated(who, why, values, size);
    }

    @SuppressWarnings("SameParameterValue")
    protected char[] newCharArray(Object who, String why)
    {
        return newCharArray(who, why, initialSize());
    }

    protected char[] newCharArray(Object who, String why, BaseCount<?> size)
    {
        return newCharArray(who, why, size.asInt());
    }

    protected char[] newCharArray(Object who, String why, int size)
    {
        tracePrimitiveAllocation(size);
        var values = new char[size];
        if (nullChar() != 0)
        {
            clear(values);
        }
        return allocated(who, why, values, size);
    }

    @SuppressWarnings("SameParameterValue")
    protected int[] newIntArray(Object who, String why)
    {
        return newIntArray(who, why, initialSize());
    }

    protected int[] newIntArray(Object who, String why, BaseCount<?> size)
    {
        return newIntArray(who, why, size.asInt());
    }

    protected int[] newIntArray(Object who, String why, int size)
    {
        tracePrimitiveAllocation(size);
        var values = new int[size];
        if (nullInt() != 0)
        {
            clear(values);
        }
        return allocated(who, why, values, size);
    }

    @SuppressWarnings("SameParameterValue")
    protected long[] newLongArray(Object who, String why)
    {
        return newLongArray(who, why, initialSize());
    }

    protected long[] newLongArray(Object who, String why, BaseCount<?> size)
    {
        return newLongArray(who, why, size.asInt());
    }

    protected long[] newLongArray(Object who, String why, int size)
    {
        tracePrimitiveAllocation(size);
        var values = new long[size];
        if (nullLong() != 0)
        {
            clear(values);
        }
        return allocated(who, why, values, size);
    }

    @SuppressWarnings("SameParameterValue")
    protected <T> T[] newObjectArray(Object who, String why)
    {
        return newObjectArray(who, why, initialSize());
    }

    protected <T> T[] newObjectArray(Object who, String why, BaseCount<?> size)
    {
        return newObjectArray(who, why, size.asInt());
    }

    @SuppressWarnings("unchecked")
    protected <T> T[] newObjectArray(Object who, String why, int size)
    {
        tracePrimitiveAllocation(size);
        return allocated(who, why, (T[]) new Object[size], size);
    }

    @SuppressWarnings("SameParameterValue")
    protected short[] newShortArray(Object who, String why)
    {
        return newShortArray(who, why, initialSize());
    }

    protected short[] newShortArray(Object who, String why, BaseCount<?> size)
    {
        return newShortArray(who, why, size.asInt());
    }

    protected short[] newShortArray(Object who, String why, int size)
    {
        tracePrimitiveAllocation(size);
        var values = new short[size];
        if (nullShort() != 0)
        {
            clear(values);
        }
        return allocated(who, why, values, size);
    }

    @SuppressWarnings("SameParameterValue")
    protected String[] newStringArray(Object who, String why)
    {
        return newStringArray(who, why, initialSize());
    }

    protected String[] newStringArray(Object who, String why, BaseCount<?> size)
    {
        return newStringArray(who, why, size.asInt());
    }

    protected String[] newStringArray(Object who, String why, int size)
    {
        tracePrimitiveAllocation(size);
        return allocated(who, why, new String[size], size);
    }

    /**
     * @param method The suggested compression method
     * @return The method that should be used
     */
    protected Method onCompress(Method method)
    {
        return Method.NONE;
    }

    protected final void size(int size)
    {
        this.size = size;
    }

    protected String toString(IndexedToString toStringer)
    {
        return toString(", ", 10, "\n", toStringer);
    }

    protected String toString(String separator, int every, String section,
                              IndexedToString toStringer)
    {
        return toString(separator, every, section, toStringer, TO_STRING_MAXIMUM_ELEMENTS);
    }

    protected String toString(String separator, int every, String section,
                              IndexedToString toStringer, int maximumElements)
    {
        var count = Math.min(size(), maximumElements);
        var builder = new StringBuilder();
        for (var i = 0; i < count; i++)
        {
            if (i > 0)
            {
                if (every > 0 && i % every == 0)
                {
                    builder.append(section);
                }
                else
                {
                    builder.append(separator);
                }
            }
            builder.append(toStringer.toString(i));
        }
        if (size() > maximumElements)
        {
            builder.append(separator);
            builder.append("[...]");
        }
        return Indent.by(4, builder.toString());
    }

    private Boolean logAllocations()
    {
        if (logAllocations == null)
        {
            var property = System.getProperty("KIVAKIT_LOG_ALLOCATIONS");
            if (property != null)
            {
                logAllocations = Boolean.valueOf(property);

                // We cannot do asynchronous logging since primitive collections are mutable
                BaseLog.asynchronous(false);
            }
            else
            {
                logAllocations = false;
            }
        }
        return logAllocations;
    }

    private int logAllocationsMinimumSize()
    {
        if (logAllocationsMinimumSize == 0)
        {
            var property = System.getProperty("KIVAKIT_LOG_ALLOCATIONS_MINIMUM_SIZE");
            if (property != null)
            {
                logAllocationsMinimumSize = Objects.requireNonNull(Bytes.parseBytes(LOGGER, property)).asInt();
            }
            else
            {
                logAllocationsMinimumSize = 65_536;
            }
        }
        return logAllocationsMinimumSize;
    }

    private void tracePrimitiveAllocation(int size)
    {
        if (DEBUG.isDebugOn())
        {
            if (size > LARGE_ALLOCATION)
            {
                LOGGER.log(new Activity("Allocated large array for ${class} ($) with $ elements",
                        getClass(), objectName(), size));
            }
        }
    }
}
