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

package com.telenav.kivakit.primitive.collections.map.scalars.fixed;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.telenav.kivakit.interfaces.numeric.Quantizable;
import com.telenav.kivakit.primitive.collections.CompressibleCollection;
import com.telenav.kivakit.primitive.collections.array.scalars.IntArray;
import com.telenav.kivakit.primitive.collections.iteration.LongIterator;
import com.telenav.kivakit.primitive.collections.list.PrimitiveList;
import com.telenav.kivakit.primitive.collections.map.multi.PrimitiveMultiMap;
import com.telenav.kivakit.primitive.collections.map.multi.PrimitiveScalarMultiMap;
import com.telenav.kivakit.primitive.collections.map.scalars.LongToIntMap;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveMultiMap;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import java.util.List;

/**
 * A compact multi-map which allows one-time put of a fixed list of values. Adding more values is not supported.
 *
 * @author jonathanl (shibo)
 * @see IntArray
 * @see PrimitiveMultiMap
 * @see KryoSerializable
 */
@SuppressWarnings({ "ConstantConditions", "DuplicatedCode" })
@UmlClassDiagram(diagram = DiagramPrimitiveMultiMap.class)
public final class LongToIntFixedMultiMap extends PrimitiveMultiMap implements PrimitiveScalarMultiMap
{
    private static final int TERMINATOR = Integer.MAX_VALUE - 1;

    /** Null terminated lists of values */
    private IntArray values;

    /** Map from key to values index */
    private LongToIntMap indexes;

    public LongToIntFixedMultiMap(String objectName)
    {
        super(objectName);
    }

    private LongToIntFixedMultiMap()
    {
    }

    /**
     * @return True if this map contains the given key
     */
    public boolean containsKey(long key)
    {
        return !indexes.isNull(indexes.get(key));
    }

    /**
     * @return An int array for the given key
     */
    public IntArray get(long key)
    {
        var index = indexes.get(key);
        if (!indexes.isNull(index))
        {
            var values = new IntArray("get");
            values.initialSize(initialChildSizeAsInt());
            values.initialize();

            for (var i = index; i < this.values.size(); i++)
            {
                var value = this.values.get(i);
                if (value == TERMINATOR)
                {
                    return values;
                }
                values.add(value);
            }
            return values;
        }
        return null;
    }

    @Override
    public PrimitiveList getPrimitiveList(long key)
    {
        return get(key);
    }

    @Override
    public boolean isScalarKeyNull(long key)
    {
        return isNull(key);
    }

    /**
     * @return An iterator over the keys in this map
     */
    public LongIterator keys()
    {
        return indexes.keys();
    }

    @Override
    public CompressibleCollection.Method onCompress(CompressibleCollection.Method method)
    {
        if (method == CompressibleCollection.Method.RESIZE)
        {
            return super.onCompress(method);
        }
        else
        {
            indexes.compress(method);
            values.compress(method);
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

        indexes = new LongToIntMap(objectName() + ".indexes");
        indexes.initialSize(initialSize());
        indexes.initialize();

        values = new IntArray(objectName() + ".values");
        indexes.initialSize(initialSize());
        values.initialize();

        // Add a value in the first index spot because index 0 is invalid
        values.add(nullInt());
    }

    public void putAll(long key, List<? extends Quantizable> values)
    {
        // If we haven't already put a value for this key
        assert isNull(indexes.get((int) key));

        // and we have room to put a value
        if (ensureHasRoomFor(1))
        {
            // get the next index in the values array
            var index = this.values.size();

            // add a mapping from the key to the index
            indexes.put((int) key, index);

            // then add all the values and a terminator
            this.values.addAll(values);
            this.values.add(TERMINATOR);
        }
    }

    /**
     * Puts the given values under the given key
     */
    public void putAll(long key, int[] values)
    {
        // If we haven't already put a value for this key
        assert isNull(indexes.get(key));

        // and we have room to put a value
        if (ensureHasRoomFor(1))
        {
            // get the next index in the values array
            var index = this.values.size();

            // add a mapping from the key to the index
            indexes.put(key, index);

            // then add all the values and a terminator
            this.values.addAll(values);
            this.values.add(TERMINATOR);
        }
    }

    /**
     * Puts the given values under the given key
     */
    public void putAll(long key, IntArray values)
    {
        putAll(key, values.asArray());
    }

    @Override
    public void putPrimitiveList(long key, PrimitiveList values)
    {
        putAll(key, (IntArray) values);
    }

    @Override
    public void putPrimitiveList(long key, List<? extends Quantizable> values)
    {
        putAll(key, values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(Kryo kryo, Input input)
    {
        super.read(kryo, input);

        values = kryo.readObject(input, IntArray.class);
        indexes = kryo.readObject(input, LongToIntMap.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return indexes.size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "[" + getClass().getSimpleName() + " name = " + objectName() + ", size = " + size() + "]\n" +
                toString(keys(), key -> get(key) == null ? null : get(key).iterator(),
                        (key, values) -> values == null ? "null" : key + " -> " + values);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(Kryo kryo, Output output)
    {
        super.write(kryo, output);

        kryo.writeObject(output, values);
        kryo.writeObject(output, indexes);
    }
}
