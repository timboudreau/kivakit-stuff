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

import com.telenav.kivakit.core.value.mutable.MutableInteger;
import com.telenav.kivakit.primitive.collections.PrimitiveCollectionsUnitTest;
import org.junit.Test;

import java.util.HashMap;

import static com.telenav.kivakit.core.test.Repeats.ALLOW_REPEATS;
import static com.telenav.kivakit.core.test.Repeats.NO_REPEATS;
import static com.telenav.kivakit.core.value.count.Count._20;

public class ByteArrayTest extends PrimitiveCollectionsUnitTest
{
    @Test
    public void testAdd()
    {
        var array = array();
        array.hasNullByte(false);

        var values = random().list(Byte.class);
        values.forEach(array::add);

        index = 0;
        values.forEach(value -> ensureEqual(array.get(index++), value));
    }

    @Test
    public void testClear()
    {
        var array = array();
        array.hasNullByte(false);
        random().byteSequence(array::add);

        random().indexes(NO_REPEATS, array.size(), index ->
        {
            ensure(!array.isNull(array.get(index)));
            array.clear(index);
            ensure(!array.isNull(array.get(index)));
        });

        array.nullByte((byte) -1);
        random().byteSequence(NO_REPEATS, value ->
        {
            if (!array.isNull(value))
            {
                array.add(value);
            }
        });

        random().indexes(ALLOW_REPEATS, array.size(), index ->
        {
            array.set(index, (byte) 99);
            ensure(!array.isNull(array.get(index)));
            array.clear(index);
            ensure(array.isNull(array.get(index)));
        });
    }

    @Test
    public void testEqualsHashCode()
    {
        var map = new HashMap<ByteArray, Integer>();

        _20.loop(() ->
        {
            var array = array();
            random().byteSequence(array::add);
            map.put(array, 99);
            ensureEqual(99, map.get(array));
        });
    }

    @Test
    public void testFirstLast()
    {
        var array = array();

        ensureThrows(array::first);
        ensureThrows(array::last);

        var last = new MutableInteger(Integer.MIN_VALUE);

        index = 0;
        random().byteSequence(value ->
        {
            index++;
            array.set(index, value);
            last.maximum(index);
            ensureEqual(array.get(0), array.first());
            ensureEqual(array.get(last.get()), array.last());
        });
    }

    @Test
    public void testGetSet()
    {
        var array = array();

        index = 0;
        random().byteSequence(value ->
        {
            array.set(index, value);
            ensureEqual(array.get(index), value);
            index++;
        });

        index = 0;
        random().byteSequence(value ->
        {
            array.set(index, value);
            ensureEqual(array.get(index), value);
            index++;
        });

        array.clear();
        array.nullByte((byte) -1);
        random().byteSequence(value -> !value.equals((byte)-1), array::add);
        random().loop(() ->
        {
            var index = random().randomIndex(array.size() * 2);
            var value = array.safeGet(index);
            ensureEqual(index >= array.size(), array.isNull(value), "index = $, size = $, value = $", index, array.size(), value);
        });
    }

    @Test
    public void testIsNull()
    {
        var array = array();
        var nullValue = random().randomByte();
        array.nullByte(nullValue);
        ensure(array.hasNullByte());

        index = 0;
        random().byteSequence(value -> value != array.nullByte(), value ->
        {
            index++;

            array.set(index, value);
            ensure(!array.isNull(array.get(index)));

            array.set(index, array.nullByte());
            ensure(array.isNull(array.get(index)));
        });
    }

    @Test
    public void testIteration()
    {
        var array = array();
        array.hasNullByte(false);

        array.add((byte) 0);
        array.add((byte) 1);
        array.add((byte) 2);
        array.set(32, (byte) 100);

        var values = array.iterator();
        ensureEqual((byte)0, values.next());
        ensureEqual((byte)1, values.next());
        ensureEqual((byte)2, values.next());
        ensureEqual(array.nullByte(), values.next());
        ensure(values.hasNext());

        array.hasNullByte(true);

        values = array.iterator();
        ensureEqual((byte)1, values.next());
        ensureEqual((byte)2, values.next());
        ensureEqual((byte)100, values.next());
        ensureFalse(values.hasNext());
    }

    @Test
    public void testSerialization()
    {
        if (!isQuickTest())
        {
            var array = array();
            random().byteSequence(array::add);
            testSerialization(array);
        }
    }

    @Test
    public void testSizeIsEmpty()
    {
        var array = array();

        ensure(array.isEmpty());
        ensure(array.size() == 0);
        array.add((byte) 0);
        ensure(array.size() == 1);
        array.add((byte) 1);
        ensure(array.size() == 2);
        array.add((byte) 2);
        ensure(array.size() == 3);
        array.set(1000, (byte) 1000);
        ensure(array.size() == 1001);
        array.clear(2);
        ensure(array.size() == 1001);
        array.clear();
        ensure(array.isEmpty());
        ensure(array.size() == 0);

        index = 0;
        var maximum = new MutableInteger(Integer.MIN_VALUE);
        random().byteSequence(value ->
        {
            index++;
            maximum.maximum(index);
            array.set(index, value);
            ensure(array.size() == maximum.get() + 1);
        });
    }

    @Test
    public void testSubArray()
    {
        var array = array();
        random().byteSequence(array::add);

        var last = array.size() - 1;
        var offset = Math.abs(random().randomIntExclusive(0, last));
        var length = Math.abs(random().randomIntExclusive(0, last - offset));

        ensure(offset < array.size());
        ensure(length >= 0);
        ensure(offset + length < array.size());

        var subArray = array.sublist(offset, length);

        for (var i = 0; i < length; i++)
        {
            ensureEqual(array.get(offset + i), subArray.get(i));
        }
    }

    private ByteArray array()
    {
        var array = new ByteArray("test");
        array.initialize();
        return array;
    }
}
