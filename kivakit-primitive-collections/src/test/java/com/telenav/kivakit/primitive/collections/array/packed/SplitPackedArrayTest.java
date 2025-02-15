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

import com.telenav.kivakit.core.value.count.BitCount;
import com.telenav.kivakit.primitive.collections.PrimitiveCollectionsUnitTest;
import org.junit.Test;

import static com.telenav.kivakit.core.bits.Bits.oneBits;

public class SplitPackedArrayTest extends PrimitiveCollectionsUnitTest
{
    @Test
    public void accessTest()
    {
        var values = new PackedArray("test");
        values.bits(BitCount._3, PackedPrimitiveArray.OverflowHandling.NO_OVERFLOW);
        values.initialize();

        values.set(0, 3L);
        ensureEqual(1, values.size());
        ensureEqual(3L, values.get(0));

        values.set(1, 5L);
        ensureEqual(2, values.size());
        ensureEqual(5L, values.get(1));

        values.set(21, 7L);
        ensureEqual(22, values.size());
        ensureEqual(7L, values.get(21));

        values.set(22, 7L);
        ensureEqual(23, values.size());
        ensureEqual(7L, values.get(22));
    }

    @Test
    public void exhaustiveTest()
    {
        // Test each bit length
        for (var bits = 1; bits <= Long.SIZE; bits++)
        {
            // Create packed array with given bit-length
            var count = BitCount.bitCount(bits);
            var values = new PackedArray("test");
            values.initialSize(32);
            values.bits(count, PackedPrimitiveArray.OverflowHandling.NO_OVERFLOW);
            values.initialize();

            // Loop through all indexes
            var mask = oneBits(values.bits());
            for (var index = 0; index < values.initialSizeAsInt(); index++)
            {
                // and test storage of a range of values
                var maximum = 1 << bits;
                for (var i = 0; i < 32; i++)
                {
                    var value = Math.abs(random().randomIntExclusive(0, maximum - 1));
                    var left = values.safeGet(index - 1);
                    var right = values.safeGet(index + 1);
                    values.set(index, value);
                    ensureEqual((value & mask), values.get(index));
                    ensureEqual(left, values.safeGet(index - 1));
                    ensureEqual(right, values.safeGet(index + 1));
                }
            }
        }
    }

    @Test
    public void nullTest()
    {
        {
            var values = new SplitPackedArray("test");
            values.bits(BitCount.bitCount(3), PackedPrimitiveArray.OverflowHandling.NO_OVERFLOW);
            values.initialChildSize(100);
            values.nullLong(7);
            values.initialize();

            values.set(0, 3L);
            ensure(!values.isNull(values.get(0)));

            values.set(1, 5L);
            ensure(!values.isNull(values.get(1)));

            values.set(21, values.nullLong());
            ensure(values.isNull(values.get(21)));

            values.set(22, values.nullLong());
            ensure(values.isNull(values.get(22)));
        }
        {
            var values = new PackedArray("test");
            values.bits(BitCount._8, PackedPrimitiveArray.OverflowHandling.NO_OVERFLOW);
            values.nullLong(255);
            values.initialize();

            values.set(0, 0);
            ensure(values.get(0) == 0);
            values.set(1, 254);
            ensure(values.get(1) == 254);
            values.set(2, values.nullLong());
            ensure(values.isNull(values.get(2)));
        }
    }

    @Test
    public void testSerialization()
    {
        var values = new PackedArray("test");
        values.bits(BitCount._3, PackedPrimitiveArray.OverflowHandling.NO_OVERFLOW);
        values.initialize();

        for (var i = 0; i < 7; i++)
        {
            values.setInt(i, i);
        }
        testSerialization(values);
    }
}
