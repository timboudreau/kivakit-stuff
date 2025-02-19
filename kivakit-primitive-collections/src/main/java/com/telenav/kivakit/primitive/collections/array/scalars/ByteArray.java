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
import com.telenav.kivakit.core.string.StringTo;
import com.telenav.kivakit.primitive.collections.CompressibleCollection;
import com.telenav.kivakit.primitive.collections.PrimitiveCollection;
import com.telenav.kivakit.primitive.collections.array.PrimitiveArray;
import com.telenav.kivakit.primitive.collections.list.ByteList;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveArray;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import java.util.Arrays;

/**
 * A dynamic array of primitive byte values. Supports the indexing operations in {@link ByteList}. Expands the size of
 * the array if you call {@link #set(int, byte)} or {@link #add(byte)} and the array is not big enough.
 * <p>
 * Constructors take the same name, maximum size and estimated capacity that all {@link PrimitiveCollection}s take. In
 * addition, a {@link ByteArray} can construct from part or all of a primitive byte[].
 * <p>
 * A sub-array can be retrieved by specifying the starting index and the length with {@link #sublist(int, int)}. The
 * sub-array is read only and will share data with the underlying parent array for efficiency.
 *
 * @author jonathanl (shibo)
 * @see PrimitiveCollection
 * @see ByteList
 * @see KryoSerializable
 * @see CompressibleCollection
 */
@SuppressWarnings("DuplicatedCode") @UmlClassDiagram(diagram = DiagramPrimitiveArray.class)
public class ByteArray extends PrimitiveArray implements ByteList
{
    /** The underlying primitive data array */
    private byte[] data;

    /** The index where {@link #add(byte)} will add values and {@link #next()} will read values */
    private int cursor;

    /** True if this array is a read-only sub-array of a parent array */
    private boolean isSubArray;

    public ByteArray(String objectName)
    {
        super(objectName);
    }

    protected ByteArray()
    {
    }

    /**
     * Adds a value, advancing the add cursor
     */
    @Override
    public boolean add(byte value)
    {
        assert isWritable();

        if (ensureHasRoomFor(1))
        {
            set(cursor, value);
            return true;
        }
        return false;
    }

    /**
     * Adds a long value
     *
     * @return True if the value was added
     */
    public boolean add(long value)
    {
        assert isWritable();

        if (ensureHasRoomFor(8))
        {
            for (var shift = 7; shift >= 0; shift--)
            {
                add((byte) ((value >>> (shift * 8)) & 0xff));
            }
            return true;
        }
        return false;
    }

    /**
     * This dynamic array as a primitive array
     */
    @Override
    public byte[] asArray()
    {
        compress(Method.RESIZE);
        return Arrays.copyOfRange(data, 0, size());
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
        set(index, nullByte());
        cursor(index + 1);
    }

    /**
     * Positions the cursor
     */
    @Override
    public void cursor(int cursor)
    {
        this.cursor = cursor;
    }

    /**
     * @return The index of the cursor
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
        if (object instanceof ByteArray)
        {
            var that = (ByteArray) object;
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
    public byte get(int index)
    {
        assert index >= 0;
        assert index < size();

        return data[index];
    }

    /**
     * @return The value at the given index as an unsigned value
     */
    public int getUnsigned(int index)
    {
        return get(index) & 0xff;
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
    public byte next()
    {
        return get(cursor++);
    }

    @Override
    public Method onCompress(Method method)
    {
        if (size() < data.length)
        {
            resize(size());
        }

        return Method.RESIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInitialize()
    {
        data = newByteArray(this, "allocated");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(Kryo kryo, Input input)
    {
        super.read(kryo, input);

        isSubArray = kryo.readObject(input, boolean.class);
        data = kryo.readObject(input, byte[].class);
        cursor = kryo.readObject(input, int.class);
    }

    /**
     * @return The value at the given index or the null value if the index is out of bounds
     */
    @Override
    public byte safeGet(int index)
    {
        assert index >= 0;
        if (index < size())
        {
            return data[index];
        }
        return nullByte();
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
    public void set(int index, byte value)
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
        set(index, (byte) value);
    }

    /**
     * @return A read-only sub-array which shares underlying data with this array.
     */
    @Override
    public ByteArray sublist(int offset, int size)
    {
        var outer = this;
        var array = new ByteArray(objectName() + "[" + offset + " - " + (offset + size - 1) + "]")
        {
            @Override
            public byte[] asArray()
            {
                compress(Method.RESIZE);
                return Arrays.copyOfRange(outer.data, offset, size());
            }

            @Override
            public byte get(int index)
            {
                return outer.get(offset + index);
            }

            @Override
            public void onInitialize()
            {
            }

            @Override
            public byte safeGet(int index)
            {
                return outer.safeGet(offset + index);
            }

            @Override
            public void set(int index, byte value)
            {
                outer.set(offset + index, value);
            }

            @Override
            public int size()
            {
                return size;
            }
        };
        array.initialize();
        return array;
    }

    public String toBinaryString()
    {
        return toString(", ", 10, "\n", index -> StringTo.binary(getUnsigned(index), 8), 49);
    }

    public String toHexString()
    {
        return toString(", ", 10, "\n", (index) -> String.format("%02x", getUnsigned(index)), 49);
    }

    @Override
    public String toString()
    {
        return "[" + getClass().getSimpleName() + " name = " + objectName() + ", cursor = " + cursor() + ", size = " + size() + "]\n" +
                toHexString() + "\n" + toBinaryString();
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
        kryo.writeObject(output, cursor);
    }

    /** Returns true if this is not a read-only sub-array */
    private boolean isWritable()
    {
        return !isSubArray;
    }

    /** Resizes this dynamic array's capacity to the given size */
    private void resize(int size)
    {
        assert size >= size();

        // If we're writable and the size is increasing we can resize,
        if (isWritable())
        {
            // so create a new byte[] of the right size,
            var data = newByteArray(this, "resized", size);

            // copy the data from this array to the new array,
            System.arraycopy(this.data, 0, data, 0, size());

            // and assign the new byte[].
            this.data = data;
        }
    }
}
