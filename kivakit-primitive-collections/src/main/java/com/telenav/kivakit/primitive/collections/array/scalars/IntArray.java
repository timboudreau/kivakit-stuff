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

package com.telenav.kivakit.primitive.collections.array.scalars;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.telenav.kivakit.conversion.BaseStringConverter;
import com.telenav.kivakit.core.collections.list.StringList;
import com.telenav.kivakit.core.messaging.Listener;
import com.telenav.kivakit.core.string.Separators;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.primitive.collections.CompressibleCollection;
import com.telenav.kivakit.primitive.collections.PrimitiveCollection;
import com.telenav.kivakit.primitive.collections.array.PrimitiveArray;
import com.telenav.kivakit.primitive.collections.list.IntList;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveArray;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import java.util.Arrays;

/**
 * A dynamic array of primitive int values. Supports the indexing operations in {@link IntList}. Expands the size of the
 * array if you call {@link #set(int, int)} or {@link #add(int)} and the array is not big enough.
 * <p>
 * Constructors take the same name, maximum size and estimated capacity that all {@link PrimitiveCollection}s take. In
 * addition a {@link IntArray} can construct from part or all of a primitive int[].
 * <p>
 * A sub-array can be retrieved by specifying the starting index and the length with {@link #subArray(int, int)}. The
 * sub-array is read only and will share data with the underlying parent array for efficiency.
 *
 * @author jonathanl (shibo)
 * @see PrimitiveCollection
 * @see IntList
 * @see KryoSerializable
 * @see CompressibleCollection
 */
@SuppressWarnings("DuplicatedCode") @UmlClassDiagram(diagram = DiagramPrimitiveArray.class)
public final class IntArray extends PrimitiveArray implements IntList
{
    /**
     * Converts to and from an {@link IntArray}
     *
     * @author jonathanl (shibo)
     */
    @LexakaiJavadoc(complete = true)
    public static class Converter extends BaseStringConverter<IntArray>
    {
        private final Separators separators;

        public Converter(Listener listener, Separators separators)
        {
            super(listener);
            this.separators = separators;
        }

        @Override
        protected String onToString(IntArray array)
        {
            var strings = new StringList(Maximum.maximum(array.size()));
            var values = array.iterator();
            while (values.hasNext())
            {
                strings.add(Integer.toString(values.next()));
            }
            return strings.join(separators.current());
        }

        @Override
        protected IntArray onToValue(String value)
        {
            var elements = StringList.split(value, separators.current());
            var array = new IntArray("converted");
            array.initialize();
            for (var element : elements)
            {
                array.add(Integer.parseInt(element));
            }
            return array;
        }
    }

    /** The underlying primitive data array */
    private int[] data;

    /**
     * The index of the first element in the data array. Normally this will be zero, but for read-only sub-arrays, it
     * will be some offset into the parent array's data (which is shared).
     */
    private int offset;

    /** The index where {@link #add(int)} will add values */
    private int cursor;

    /** True if this array is a read-only sub-array of some parent array */
    private boolean isSubArray;

    public IntArray(String objectName)
    {
        super(objectName);
    }

    private IntArray()
    {
    }

    /**
     * Constructor for constructing read-only sub-arrays that share data with their parent.
     */
    private IntArray(String name, int[] data, int offset, int size)
    {
        this(name);

        isSubArray = true;

        this.data = data;
        this.offset = offset;
        size(size);
    }

    /**
     * Adds a value, advancing the add cursor
     */
    @Override
    public boolean add(int value)
    {
        assert isWritable();

        if (ensureHasRoomFor(1))
        {
            set(cursor++, value);
            return true;
        }
        return false;
    }

    /**
     * This dynamic array as a primitive array
     */
    public int[] asArray()
    {
        compress(CompressibleCollection.Method.RESIZE);
        return Arrays.copyOfRange(data, offset, offset + size());
    }

    /**
     * Clears this array
     */
    @Override
    public void clear()
    {
        assert isWritable();
        super.clear();
        cursor = 0;
    }

    /**
     * Sets the element at the given index to the current null value
     */
    @Override
    public void clear(int index)
    {
        set(index, nullInt());
    }

    /**
     * Positions the add cursor
     */
    @Override
    public void cursor(int cursor)
    {
        this.cursor = cursor;
    }

    /**
     * @return The index of the add cursor
     */
    @Override
    public int cursor()
    {
        return cursor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object)
    {
        if (object instanceof IntArray)
        {
            var that = (IntArray) object;
            if (size() == that.size())
            {
                return iterator().identical(that.iterator());
            }
        }
        return false;
    }

    /**
     * @return The value at the given logical index.
     */
    @Override
    public int get(int index)
    {
        assert index >= 0;
        assert index < size();

        return data[offset + index];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return iterator().hashValue();
    }

    @Override
    public CompressibleCollection.Method onCompress(CompressibleCollection.Method method)
    {
        if (size() < data.length)
        {
            resize(size());
        }

        return CompressibleCollection.Method.RESIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInitialize()
    {
        super.onInitialize();
        data = newIntArray(this, "allocated");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(Kryo kryo, Input input)
    {
        super.read(kryo, input);

        isSubArray = kryo.readObject(input, boolean.class);
        data = kryo.readObject(input, int[].class);
        offset = kryo.readObject(input, int.class);
        cursor = kryo.readObject(input, int.class);
    }

    /**
     * @return The value at the given index or the null value if the index is out of bounds
     */
    @Override
    public int safeGet(int index)
    {
        assert index >= 0;
        if (index < size())
        {
            return data[offset + index];
        }
        return nullInt();
    }

    @Override
    public long safeGetPrimitive(int index)
    {
        return safeGet(index);
    }

    /**
     * Sets a value at the given index, possibly extending the array size.
     */
    @Override
    public void set(int index, int value)
    {
        assert isWritable();

        var newSize = index + 1;
        var size = size();

        // If the given index is past the end of storage,
        if (newSize > data.length)
        {
            // resize the array,
            resize(PrimitiveCollection.increasedCapacity(newSize));
        }

        // then store the value at the given index,
        data[index] = value;

        // and possibly increase the size if we've written past the end of the previous size.
        if (newSize > size)
        {
            size(newSize);
        }

        cursor(newSize);
    }

    @Override
    public void setPrimitive(int index, long value)
    {
        set(index, (int) value);
    }

    /**
     * @return A read-only sub-array which shares underlying data with this array.
     */
    public IntArray subArray(int index, int size)
    {
        var array = new IntArray(objectName(), data, offset + index, size);
        array.initialSize(0);
        array.initialize();
        array.data = data;
        return array;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString()
    {
        return "[" + getClass().getSimpleName() + " name = " + objectName() + ", size = " + size() + "]\n" +
                toString(index -> Long.toString(get(index)));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void write(Kryo kryo, Output output)
    {
        super.write(kryo, output);

        kryo.writeObject(output, isSubArray);
        kryo.writeObject(output, data);
        kryo.writeObject(output, offset);
        kryo.writeObject(output, cursor);
    }

    /** Returns true if this is not a read-only sub-array */
    private boolean isWritable()
    {
        return !isSubArray;
    }

    /** Resizes this dynamic array to the given size */
    private void resize(int size)
    {
        // If we're writable and the size is increasing we can resize,
        if (isWritable())
        {
            // so create a new int[] of the right size,
            var data = newIntArray(this, "resized", size);

            // copy the data from this array to the new array,
            System.arraycopy(this.data, 0, data, 0, size());

            // and assign the new int[].
            this.data = data;
        }
    }
}
