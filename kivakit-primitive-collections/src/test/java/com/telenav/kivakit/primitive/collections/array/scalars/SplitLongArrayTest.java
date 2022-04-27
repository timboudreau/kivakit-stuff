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
import static com.telenav.kivakit.core.value.count.Count._10;

public class SplitLongArrayTest extends PrimitiveCollectionsUnitTest
{
    @Test
    public void testAdd()
    {
        var array = array();
        array.hasNullLong(false);

        var values = random().list(Long.class);
        values.forEach(array::add);

        index = 0;
        values.forEach(value -> ensureEqual(array.get(index++), value));
    }

    @Test
    public void testClear()
    {
        var array = array();
        array.hasNullLong(false);
        random().longSequence(array::add);
        random().indexes(array.size(), index ->
        {
            ensure(!array.isNull(array.get(index)));
            array.clear(index);
            ensure(!array.isNull(array.get(index)));
        });

        array.nullLong(-1);
        random().longSequence(NO_REPEATS, value ->
        {
            if (!array.isNull(value))
            {
                array.add(value);
            }
        });

        random().indexes(ALLOW_REPEATS, array.size(), index ->
        {
            array.set(index, 99);
            ensure(!array.isNull(array.get(index)));
            array.clear(index);
            ensure(array.isNull(array.get(index)));
        });
    }

    @Test
    public void testEqualsHashCode()
    {
        var map = new HashMap<SplitLongArray, Integer>();
        _10.loop(() ->
        {
            var array = array();
            random().longSequence(array::add);
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
        random().longSequence(value ->
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
        {
            var array = array();

            index = 0;
            random().longSequence(value ->
            {
                index++;
                array.set(index, value);
                ensureEqual(array.get(index), value);
            });

            index++;
            random().longSequence(value ->
            {
                index++;
                array.set(index, value);
                ensureEqual(array.get(index), value);
            });
        }
        {
            var array = array();
            array.nullLong(-1);

            random().longSequence(NO_REPEATS, value -> !value.equals(-1L), array::add);
            array.safeGet(0);
            random().loop(() ->
            {
                var index = random().randomIndex(array.size() * 2);
                var value = array.safeGet(index);
                ensureEqual(index >= array.size(), array.isNull(value));
            });
        }
    }

    @Test
    public void testIsNull()
    {
        var array = array();
        var nullValue = newRandomValueFactory().randomLong();
        array.nullLong(nullValue);
        ensure(array.hasNullLong());
        index = 0;
        random().longSequence(value -> value != array.nullLong(), value ->
        {
            index++;

            array.set(index, value);
            ensure(!array.isNull(array.get(index)));

            array.set(index, array.nullLong());
            ensure(array.isNull(array.get(index)));
        });
    }

    @Test
    public void testIteration()
    {
        var array = array();

        array.add(0);
        array.add(1);
        array.add(2);
        array.set(100, 100);

        var values = array.iterator();
        ensureEqual((long) 0, values.next());
        ensureEqual((long) 1, values.next());
        ensureEqual((long) 2, values.next());
        ensureEqual((long) 100, values.next());
        ensure(!values.hasNext());

        array.hasNullLong(false);

        values = array.iterator();
        ensureEqual((long) 0, values.next());
        ensureEqual((long) 1, values.next());
        ensureEqual((long) 2, values.next());
        ensureEqual(Long.MIN_VALUE, values.next());
        ensure(values.hasNext());
    }

    @Test
    public void testSerialization()
    {
        var array = array();
        random().longSequence(array::add);
        testSerialization(array);
    }

    @Test
    public void testSizeIsEmpty()
    {
        {
            var array = array();

            ensure(array.isEmpty());
            ensure(array.size() == 0);
            array.add(0);
            ensure(array.size() == 1);
            array.add(1);
            ensure(array.size() == 2);
            array.add(2);
            ensure(array.size() == 3);
            array.set(1000, 1000);
            ensure(array.size() == 1001);
            array.clear(2);
            ensure(array.size() == 1001);
        }
        {
            var array = array();
            var maximum = new MutableInteger(Integer.MIN_VALUE);
            index = 0;
            random().longSequence(value ->
            {
                index++;
                maximum.maximum(index);
                array.set(index, value);
                ensure(array.size() == maximum.get() + 1);
            });
        }
    }

    private SplitLongArray array()
    {
        var array = (SplitLongArray) new SplitLongArray("test")
                .nullLong(Long.MIN_VALUE)
                .initialChildSize(100);
        array.initialize();
        return array;
    }
}
