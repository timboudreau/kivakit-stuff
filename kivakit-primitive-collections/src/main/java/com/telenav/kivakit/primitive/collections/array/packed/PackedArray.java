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

package com.telenav.kivakit.primitive.collections.array.packed;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.telenav.kivakit.core.language.Hash;
import com.telenav.kivakit.core.language.Objects;
import com.telenav.kivakit.core.value.count.BitCount;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Estimate;
import com.telenav.kivakit.primitive.collections.CompressibleCollection;
import com.telenav.kivakit.primitive.collections.PrimitiveCollection;
import com.telenav.kivakit.primitive.collections.array.PrimitiveArray;
import com.telenav.kivakit.primitive.collections.array.scalars.LongArray;
import com.telenav.kivakit.primitive.collections.list.LongList;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveArray;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import static com.telenav.kivakit.core.ensure.Ensure.ensure;
import static com.telenav.kivakit.primitive.collections.array.packed.PackedPrimitiveArray.OverflowHandling.ALLOW_OVERFLOW;
import static com.telenav.kivakit.primitive.collections.array.packed.PackedPrimitiveArray.OverflowHandling.NO_OVERFLOW;

/**
 * A packed array packs arbitrary bit-length integral values into an underlying long array.
 * <p>
 * For example, if you create a packed array of 3-bit values, the first three bits of the first long (at index 0) in the
 * long array would store the first 3-bit value, the next 3 bits would store the second 3-bit value, and so on... until
 * we get to value 21, which would span from the first long to the second one and be stored in the last bit of the first
 * long and the first two bits of the second long (because 64 / 3 = 21 and 21 * 3 = 63, leaving only one bit left in the
 * first long). In diagram form, the long array will be laid out like this:
 *
 * <pre>
 * long array: [----------- index 0 -------------------]   [---------- index 1 ---------]
 *     values: [ value 0 ]   [ value 1 ]  ... [value 20]   [value 21]
 *       bit: 0    1    2   3    4    5  ... 60  61  62   63   0   1
 * </pre>
 * <p>
 * A special null value can be specified by passing a nullValue to the constructor. When this particular numeric value
 * is read by {@link #get(int)} and {@link #safeGet(int)}, they will return null instead of that value. When a null
 * reference is passed to {@link #set(int, long)}, it will be stored as the given null numeric value.
 *
 * @author jonathanl (shibo)
 */
@SuppressWarnings({ "ConstantConditions" })
@UmlClassDiagram(diagram = DiagramPrimitiveArray.class)
public final class PackedArray extends PrimitiveArray implements LongList, PackedPrimitiveArray
{
    /** Data storage */
    private LongArray data;

    /** Number of bits per packed element */
    private int bits;

    // Shifts and masks for first and second long
    private int[] firstShift = new int[Long.SIZE];

    private long[] firstMask = new long[Long.SIZE];

    private int[] secondShift = new int[Long.SIZE];

    private long[] secondMask = new long[Long.SIZE];

    /** The largest value in this packed array */
    private long largestValue;

    /** The smallest value in this packed array */
    private long smallestValue;

    /** The maximum allowed value based on how overflow is handled */
    private long maximumAllowedValue;

    /** The index at which adding takes place */
    private int cursor;

    public PackedArray(String objectName)
    {
        super(objectName);
    }

    private PackedArray()
    {
    }

    /**
     * Adds a value, advancing the add cursor
     */
    @Override
    public boolean add(long value)
    {
        assert ensureHasRoomFor(1);
        set(cursor++, value);
        return true;
    }

    /**
     * @return The bit width of this array
     */
    @Override
    public BitCount bits()
    {
        return BitCount.bitCount(bits);
    }

    /**
     * Sets the bit width of this array
     */
    @Override
    public PackedArray bits(BitCount bits, OverflowHandling overflow)
    {
        ensure(bits != null);
        ensure(!bits.isZero());
        ensure(!bits.isGreaterThan(Count._64));
        this.bits = bits.asInt();
        maximumAllowedValue = maximumAllowedValue(bits, overflow);
        return this;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void clear()
    {
        super.clear();

        for (var i = 0; i < size(); i++)
        {
            set(i, nullLong());
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void copySettings(PrimitiveCollection uncast)
    {
        super.copySettings(uncast);

        var that = (PackedPrimitiveArray) uncast;
        bits = that.bits().asInt();
        maximumAllowedValue = maximumAllowedValue(that.bits(), that.overflow());
    }

    @Override
    public int cursor()
    {
        return cursor;
    }

    @Override
    public void cursor(int cursor)
    {
        this.cursor = cursor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals(Object object)
    {
        if (object instanceof PackedArray)
        {
            var that = (PackedArray) object;
            return Objects.equalPairs(bits, that.bits, data, that.data);
        }
        return false;
    }

    /**
     * @return The value at the given index
     */
    @Override
    public long get(int index)
    {
        assert index >= 0 : "Index " + index + " must be >= 0";
        assert index < size() : "Index " + index + " must be less than " + size();

        long value;

        var data = this.data;
        switch (bits)
        {
            case 32:
                value = data.get(index / 2);
                if (index % 2 == 0)
                {
                    value >>>= 32;
                }
                value &= 0xffff_ffffL;
                break;

            case 64:
                value = data.get(index);
                break;

            default:
                var bitIndex = index * bits;
                var dataIndex = bitIndex / Long.SIZE;
                var bitOffset = bitIndex % Long.SIZE;
                var firstShift = this.firstShift[bitOffset];
                if (firstShift > 0)
                {
                    value = (data.get(dataIndex) & firstMask[bitOffset]) >>> firstShift;
                }
                else
                {
                    value = (data.get(dataIndex) & firstMask[bitOffset]) << -firstShift;
                    value |= ((data.get(dataIndex + 1) & secondMask[bitOffset]) >>> secondShift[bitOffset]);
                }
        }

        return value;
    }

    public long getSigned(int index)
    {
        // Get the value stored at the given index
        var storedValue = get(index);

        int signBits = 64 - bits;
        return storedValue << signBits >> signBits;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode()
    {
        return Hash.many(bits, data);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Estimate initialSize()
    {
        return Estimate.estimate(super.initialSize().times(bits()).dividedBy(Long.SIZE).plus(Count._1).asInt());
    }

    @Override
    public CompressibleCollection.Method onCompress(CompressibleCollection.Method method)
    {
        // Copy the values into an array with the lower bit count
        var requiredBits = requiredBits();
        if (requiredBits.isLessThan(bits()))
        {
            var array = new PackedArray(objectName());
            array.copySettings(this);
            array.bits(requiredBits, overflow());
            array.initialize();

            for (var i = 0; i < size(); i++)
            {
                var value = safeGet(i);
                if (!isNull(value))
                {
                    array.set(i, value);
                }
            }

            // copy the fields of the smaller array into this object
            copy(array);

            // Trim the underlying primitive array
            data.compress(CompressibleCollection.Method.RESIZE);

            return CompressibleCollection.Method.RESIZE;
        }
        return CompressibleCollection.Method.NONE;
    }

    @Override
    public void onInitialize()
    {
        super.onInitialize();

        data = new LongArray(objectName() + ".data");
        data.initialSize(Estimate._65536);
        data.initialize();

        computeShiftsAndMasks();
    }

    @Override
    public OverflowHandling overflow()
    {
        return maximumAllowedValue == Long.MAX_VALUE ? ALLOW_OVERFLOW : NO_OVERFLOW;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void read(Kryo kryo, Input input)
    {
        super.read(kryo, input);
        data = kryo.readObject(input, LongArray.class);
        bits = kryo.readObject(input, int.class);
        firstShift = kryo.readObject(input, int[].class);
        firstMask = kryo.readObject(input, long[].class);
        secondShift = kryo.readObject(input, int[].class);
        secondMask = kryo.readObject(input, long[].class);
        largestValue = kryo.readObject(input, long.class);
        smallestValue = kryo.readObject(input, long.class);
        maximumAllowedValue = kryo.readObject(input, long.class);
    }

    /**
     * @return The value at the given index. Null if the index is out of bounds or the value at the given index is the
     * null value
     */
    @Override
    public long safeGet(int index)
    {
        if (index >= 0 && index < size())
        {
            return get(index);
        }
        return nullLong();
    }

    @Override
    public long safeGetPrimitive(int index)
    {
        return safeGet(index);
    }

    /**
     * Sets the value at the given index
     */
    @Override
    public void set(int index, long value)
    {
        // Find the new largest and smallest values
        largestValue = Long.max(largestValue, value);
        smallestValue = Long.min(smallestValue, value);

        value = Math.min(maximumAllowedValue, value);

        // Normally we wouldn't want to check method parameters like this in Java, but this method
        // is a hot spot, so we want to be able to lift the assertion check completely when -ea is
        // not specified
        ensureIndexInRange(index);

        // Store bits in underlying long array
        var bitIndex = index * bits;
        var dataIndex = bitIndex / Long.SIZE;
        var bitOffset = bitIndex % Long.SIZE;
        var firstShift = this.firstShift[bitOffset];
        if (firstShift > 0)
        {
            data.setBits(dataIndex, firstMask[bitOffset], value << firstShift);
        }
        else
        {
            data.setBits(dataIndex, firstMask[bitOffset], value >>> -firstShift);
            data.setBits(dataIndex + 1, secondMask[bitOffset], value << secondShift[bitOffset]);
        }

        // If we've written past the end
        if (index >= size())
        {
            // initialize elements between the end of previously stored values and the new value
            for (var i = size(); i < index; i++)
            {
                set(i, nullLong());
            }

            // and increase the size
            size(index + 1);
        }

        assert index < size();
        assert overflow() != NO_OVERFLOW || isValueStoredAtIndex(index, value) :
                "Unable to store " + value + " in " + bits + " bit array at index " + index;
    }

    public void setInt(int index, int value)
    {
        set(index, value);
    }

    @Override
    public void setPrimitive(int index, long value)
    {
        set(index, value);
    }

    @Override
    public String toString()
    {
        return "[" + getClass().getSimpleName() + " name = " + objectName() + ", size = " + size() + "]\n" +
                toString(index -> Long.toString(get(index)));
    }

    @Override
    public void write(Kryo kryo, Output output)
    {
        super.write(kryo, output);

        kryo.writeObject(output, data);
        kryo.writeObject(output, bits);
        kryo.writeObject(output, firstShift);
        kryo.writeObject(output, firstMask);
        kryo.writeObject(output, secondShift);
        kryo.writeObject(output, secondMask);
        kryo.writeObject(output, largestValue);
        kryo.writeObject(output, smallestValue);
        kryo.writeObject(output, maximumAllowedValue);
    }

    private void computeShiftsAndMasks()
    {
        ensure(bits > 0);

        for (var bitOffset = 0; bitOffset < Long.SIZE; bitOffset++)
        {
            // Number of bits available in the first long at the given bit offset
            var firstAvailableBits = Long.SIZE - bitOffset;

            // If there's room for all the bits in the first long
            if (firstAvailableBits >= bits)
            {
                // create mask for the given number of bits at the given offset
                firstMask[bitOffset] = mask(bitOffset, bits);
                firstShift[bitOffset] = Long.SIZE - bitOffset - bits;
            }
            else
            {
                // Compute number of bits to store in second long
                var secondBits = bits - firstAvailableBits;

                firstMask[bitOffset] = mask(bitOffset, bits);
                firstShift[bitOffset] = -secondBits;

                secondMask[bitOffset] = mask(0, secondBits);
                secondShift[bitOffset] = Long.SIZE - secondBits;
            }
        }
    }

    private void copy(PackedArray that)
    {
        super.copy(that);
        copySettings(that);
        data = that.data;
        firstShift = that.firstShift;
        firstMask = that.firstMask;
        secondShift = that.secondShift;
        secondMask = that.secondMask;
        largestValue = that.largestValue;
        smallestValue = that.smallestValue;
        maximumAllowedValue = that.maximumAllowedValue;
        cursor = that.cursor;
    }

    private boolean isValueStoredAtIndex(int index, long value)
    {
        // If the value is a negative number
        if (value < 0)
        {
            // then sign extend the stored value before comparing
            return value == getSigned(index);
        }

        return value == get(index);
    }

    private long mask(int bitOffset, int count)
    {
        var mask = 0L;
        var bit = 1L << (Long.SIZE - bitOffset - 1);
        for (var i = 0; i < count; i++)
        {
            mask |= bit;
            bit >>= 1;
        }
        return mask;
    }

    private long maximumAllowedValue(BitCount bits, OverflowHandling overflow)
    {
        return overflow == ALLOW_OVERFLOW ? Long.MAX_VALUE : bits.maximumUnsigned();
    }

    private BitCount requiredBits()
    {
        return Count.count(largestValue - smallestValue).bitsToRepresent();
    }
}
