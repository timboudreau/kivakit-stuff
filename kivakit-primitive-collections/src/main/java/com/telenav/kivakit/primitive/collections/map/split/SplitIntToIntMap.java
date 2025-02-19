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

package com.telenav.kivakit.primitive.collections.map.split;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.primitive.collections.CompressibleCollection;
import com.telenav.kivakit.primitive.collections.iteration.IntIterator;
import com.telenav.kivakit.primitive.collections.map.PrimitiveScalarMap;
import com.telenav.kivakit.primitive.collections.map.SplitPrimitiveMap;
import com.telenav.kivakit.primitive.collections.map.scalars.IntToIntMap;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveMap;
import com.telenav.lexakai.annotations.UmlClassDiagram;

/**
 * A map from int keys to int values. Supports typical map functions:
 * <p>
 * <b>Access</b>
 * <ul>
 *     <li>{@link #get(int)} </li>
 *     <li>{@link #put(int, int)}</li>
 *     <li>{@link #remove(int)}</li>
 *     <li>{@link #clear()}</li>
 * </ul>
 * <p>
 * <b>Keys and Values</b>
 * <ul>
 *     <li>{@link #keys()}</li>
 *     <li>{@link #values()}</li>
 *     <li>{@link #entries(IntToIntMap.EntryVisitor)} </li>
 *     <li>{@link #containsKey(int)}</li>
 * </ul>
 * <p>
 * This class supports the {@link #hashCode()} / {@link #equals(Object)} contract and is {@link KryoSerializable}.
 *
 * @author jonathanl (shibo)
 * @see SplitPrimitiveMap
 * @see KryoSerializable
 */
@SuppressWarnings("DuplicatedCode") @UmlClassDiagram(diagram = DiagramPrimitiveMap.class)
public final class SplitIntToIntMap extends SplitPrimitiveMap implements PrimitiveScalarMap
{
    private IntToIntMap[] children;

    private int size;

    public SplitIntToIntMap(String objectName)
    {
        super(objectName);
    }

    private SplitIntToIntMap()
    {
    }

    @Override
    public Count capacity()
    {
        var capacity = 0;
        for (var child : children)
        {
            if (child != null)
            {
                capacity += child.capacity().asInt();
            }
        }
        return Count.count(capacity);
    }

    /**
     * @return True if this map contains the given key
     */
    public boolean containsKey(int key)
    {
        var child = child(key, false);
        return child != null && child.containsKey(key);
    }

    /**
     * Calls the visitor with each key / value pair in the map
     */
    public void entries(IntToIntMap.EntryVisitor visitor)
    {
        for (var child : children)
        {
            if (child != null)
            {
                child.entries(visitor);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object)
    {
        if (object instanceof SplitIntToIntMap)
        {
            var that = (SplitIntToIntMap) object;
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
     * @return The value for the given key
     */
    public int get(int key)
    {
        var child = child(key, false);
        return child == null ? nullInt() : child.get(key);
    }

    @Override
    public long getScalar(long key)
    {
        return get((int) key);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return keys().hash() ^ values().hash();
    }

    @Override
    public boolean isScalarKeyNull(long key)
    {
        return isNull((int) key);
    }

    @Override
    public boolean isScalarValueNull(long value)
    {
        return isNull((int) value);
    }

    /**
     * @return The keys in this map in an undefined order
     */
    public IntIterator keys()
    {
        var outer = this;
        return new IntIterator()
        {
            private int childIndex;

            private IntIterator keys;

            @Override
            public boolean hasNext()
            {
                if (keys != null && keys.hasNext())
                {
                    return true;
                }
                keys = null;
                while (keys == null && childIndex < outer.children.length)
                {
                    var next = outer.children[childIndex++];
                    if (next != null && !next.isEmpty())
                    {
                        keys = next.keys();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public int next()
            {
                return keys.next();
            }
        };
    }

    @Override
    public CompressibleCollection.Method onCompress(CompressibleCollection.Method method)
    {
        for (var child : children)
        {
            if (child != null)
            {
                child.compress(method);
            }
        }
        return method;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInitialize()
    {
        super.onInitialize();
        children = new IntToIntMap[initialChildCountAsInt()];
    }

    /**
     * Stores the given value under the given key
     */
    public void put(int key, int value)
    {
        assert compressionMethod() != CompressibleCollection.Method.FREEZE;

        if (child(key, true).put(key, value))
        {
            size++;
        }
    }

    @Override
    public void putScalar(long key, long value)
    {
        put((int) key, (int) value);
    }

    /**
     * @see KryoSerializable
     */
    @Override
    public void read(Kryo kryo, Input input)
    {
        super.read(kryo, input);

        children = kryo.readObject(input, IntToIntMap[].class);
        size = kryo.readObject(input, int.class);
    }

    /**
     * Removes the given key from the map along with its value
     *
     * @return True if the key was removed, false if it was not found
     */
    public boolean remove(int key)
    {
        assert compressionMethod() != CompressibleCollection.Method.FREEZE;
        var child = child(key, false);
        if (child != null)
        {
            if (child.remove(key))
            {
                size--;
                return true;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int size()
    {
        return size;
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
        var outer = this;
        return new IntIterator()
        {
            private int childIndex;

            private IntIterator values;

            @Override
            public boolean hasNext()
            {
                if (values != null && values.hasNext())
                {
                    return true;
                }
                values = null;
                while (values == null && childIndex < outer.children.length)
                {
                    var next = outer.children[childIndex++];
                    if (next != null && !next.isEmpty())
                    {
                        values = next.values();
                        return true;
                    }
                }
                return false;
            }

            @Override
            public int next()
            {
                return values.next();
            }
        };
    }

    /**
     * @see KryoSerializable
     */
    @Override
    public void write(Kryo kryo, Output output)
    {
        super.write(kryo, output);

        kryo.writeObject(output, children);
        kryo.writeObject(output, size);
    }

    /**
     * @return Gets the child map for the given key. If there is no map, one is created if create is true.
     */
    private IntToIntMap child(int key, boolean create)
    {
        // Get the child index from the key
        var childIndex = hash(key) % children.length;

        // and if the child index is null, and we should create a new child,
        var child = children[childIndex];
        if (child == null && create)
        {
            // then allocate and configure the child
            child = new IntToIntMap(objectName() + ".child[" + childIndex + "]");
            child.copySettings(this);
            child.initialSize(initialChildSize());
            child.maximumSize(Integer.MAX_VALUE);
            child.initialize();

            // and assign it to the children array.
            children[childIndex] = child;
        }

        return child;
    }
}
