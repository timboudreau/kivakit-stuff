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

package com.telenav.kivakit.primitive.collections.map.scalars;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.primitive.collections.CompressibleCollection;
import com.telenav.kivakit.primitive.collections.iteration.IntIterator;
import com.telenav.kivakit.primitive.collections.iteration.LongIterator;
import com.telenav.kivakit.primitive.collections.map.PrimitiveMap;
import com.telenav.kivakit.primitive.collections.map.PrimitiveScalarMap;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveMap;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import java.util.Arrays;

/**
 * A map from long keys to int values. Supports typical map functions:
 * <p>
 * <b>Access</b>
 * <ul>
 *     <li>{@link #get(long)} </li>
 *     <li>{@link #put(long, int)}</li>
 *     <li>{@link #remove(long)}</li>
 *     <li>{@link #increment(long)}</li>
 *     <li>{@link #clear()}</li>
 * </ul>
 * <p>
 * <b>Keys and Values</b>
 * <ul>
 *     <li>{@link #keys()}</li>
 *     <li>{@link #values()}</li>
 *     <li>{@link #entries(EntryVisitor)} </li>
 *     <li>{@link #containsKey(long)}</li>
 * </ul>
 * <p>
 * This class supports the {@link #hashCode()} / {@link #equals(Object)} contract and is {@link KryoSerializable}.
 *
 * @author jonathanl (shibo)
 * @see PrimitiveMap
 * @see KryoSerializable
 */
@SuppressWarnings("DuplicatedCode") @UmlClassDiagram(diagram = DiagramPrimitiveMap.class)
public final class LongToIntMap extends PrimitiveMap implements PrimitiveScalarMap
{
    /**
     * Interface for visiting map entries with {@link #entries(EntryVisitor)}
     */
    @LexakaiJavadoc(complete = true)
    public interface EntryVisitor
    {
        void onEntry(long key, int value);
    }

    /** The keys */
    private long[] keys;

    /** The values */
    private int[] values;

    public LongToIntMap(String objectName)
    {
        super(objectName);
    }

    private LongToIntMap()
    {
    }

    @Override
    public Count capacity()
    {
        return Count.count(keys.length);
    }

    /**
     * Clears all key/value pairs from this map
     */
    @Override
    public void clear()
    {
        super.clear();
        clear(keys);
    }

    /**
     * @return True if this map contains the given key
     */
    public boolean containsKey(long key)
    {
        return contains(keys, key);
    }

    /**
     * Calls the visitor with each key / value pair in the map
     */
    public void entries(EntryVisitor visitor)
    {
        var indexes = nonEmptyIndexes(keys);
        while (indexes.hasNext())
        {
            var index = indexes.next();
            visitor.onEntry(keys[index], values[index]);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object)
    {
        if (object instanceof LongToIntMap)
        {
            var that = (LongToIntMap) object;
            if (this == that)
            {
                return true;
            }
            if (size() != that.size())
            {
                return false;
            }
            var keys = keys();
            while (keys.hasNext())
            {
                var key = keys.next();
                var value = get(key);
                if (value != that.get(key))
                {
                    return false;
                }
            }
            return true;
        }
        return false;
    }

    /**
     * @return The value for the given key. The returned value should be checked with {@link #isNull(int)} to determine
     * if it represents null.
     */
    public int get(long key)
    {
        if (compressionMethod() == CompressibleCollection.Method.FREEZE)
        {
            var index = Arrays.binarySearch(keys, key);
            return index < 0 ? nullInt() : values[index];
        }
        else
        {
            return values[index(keys, key)];
        }
    }

    @Override
    public long getScalar(long key)
    {
        return get(key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return keys().hash() ^ values().hash();
    }

    /**
     * Increments the value for the given key
     */
    public void increment(long key)
    {
        assert compressionMethod() != CompressibleCollection.Method.FREEZE;

        put(key, get(key) + 1);
    }

    @Override
    public boolean isScalarKeyNull(long key)
    {
        return isNull(key);
    }

    @Override
    public boolean isScalarValueNull(long value)
    {
        return isNull((int) value);
    }

    /**
     * @return The keys in this map in an undefined order
     */
    public LongIterator keys()
    {
        return nonEmptyValues(keys);
    }

    /**
     * Freezes this primitive collection into an optimal memory representation. Once frozen, the collection can no
     * longer be modified.
     */
    @Override
    public CompressibleCollection.Method onCompress(CompressibleCollection.Method method)
    {
        if (method == CompressibleCollection.Method.RESIZE)
        {
            return super.onCompress(method);
        }
        else
        {
            var frozenKeys = newLongArray(this, "froze", size());
            var frozenValues = newIntArray(this, "froze", size());
            var keys = keys();
            for (var i = 0; keys.hasNext(); i++)
            {
                frozenKeys[i] = keys.next();
            }
            Arrays.sort(frozenKeys);
            for (var i = 0; i < frozenValues.length; i++)
            {
                frozenValues[i] = get(frozenKeys[i]);
            }
            this.keys = frozenKeys;
            values = frozenValues;
            return method;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInitialize()
    {
        super.onInitialize();

        keys = newLongArray(this, "allocated");
        values = newIntArray(this, "allocated");
    }

    /**
     * Stores the given value under the given key. The value may not be null. To remove a value, call {@link
     * #remove(long)}.
     *
     * @return True if a new value was added, false if an existing value was overwritten
     */
    public boolean put(long key, int value)
    {
        assert !isNull(value) : "Value " + value + " for key " + key + " is null";
        assert compressionMethod() != CompressibleCollection.Method.FREEZE;

        var keys = this.keys;
        var values = this.values;

        // Get the index to put at
        var index = index(keys, key);

        // If the slot at the given index is empty
        if (isEmpty(keys[index]))
        {
            // then we're adding a new key/value pair
            keys[index] = key;
            values[index] = value;
            increaseSize();
            return true;
        }
        else
        {
            // otherwise, we're just changing the value
            values[index] = value;
            return false;
        }
    }

    @Override
    public void putScalar(long key, long value)
    {
        put(key, (int) value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(Kryo kryo, Input input)
    {
        super.read(kryo, input);
        keys = kryo.readObject(input, long[].class);
        values = kryo.readObject(input, int[].class);
    }

    /**
     * Removes the given key from the map along with its value
     *
     * @return True if the key was removed, false if it was not found
     */
    public boolean remove(long key)
    {
        assert compressionMethod() != CompressibleCollection.Method.FREEZE;

        // Get index of key
        var index = index(keys, key);

        // If the key was found,
        if (!isEmpty(keys[index]))
        {
            // remove it
            keys[index] = TOMBSTONE_INT;
            values[index] = nullInt();
            decreaseSize(1);
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "[" + getClass().getSimpleName() + " name = " + objectName() + ", size = " + size() + "]\n" +
                toString(keys(), values(), (key, value) -> key + " -> " + value);
    }

    /**
     * @return The values in this map in an undefined order
     */
    public IntIterator values()
    {
        return nonEmptyValues(values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(Kryo kryo, Output output)
    {
        super.write(kryo, output);

        kryo.writeObject(output, keys);
        kryo.writeObject(output, values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copy(PrimitiveMap uncast)
    {
        super.copy(uncast);

        var that = (LongToIntMap) uncast;
        keys = that.keys;
        values = that.values;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void copyEntries(PrimitiveMap uncast, ProgressReporter reporter)
    {
        var that = (LongToIntMap) uncast;
        var indexes = nonEmptyIndexes(that.keys);
        while (indexes.hasNext())
        {
            var index = indexes.next();
            var key = that.keys[index];
            var value = that.values[index];
            if (!isNull(key) && !isNull(value))
            {
                put(key, value);
            }
            reporter.next();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected LongToIntMap newMap()
    {
        return new LongToIntMap(objectName());
    }

    @Override
    protected int slots()
    {
        return keys.length;
    }
}
