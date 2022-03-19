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

import com.telenav.kivakit.primitive.collections.CompressibleCollection;
import com.telenav.kivakit.primitive.collections.PrimitiveCollectionsUnitTest;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

public class LongToByteMapTest extends PrimitiveCollectionsUnitTest
{
    @FunctionalInterface
    interface MapTest
    {
        void test(LongToByteMap map, List<Long> keys, List<Byte> values);
    }

    @Test
    public void testClear()
    {
        withPopulatedMap((map, keys, values) ->
        {
            ensureFalse(map.isEmpty());
            map.clear();
            ensure(map.isEmpty());
        });
    }

    @Test
    public void testContainsKey()
    {
        withPopulatedMap((map, keys, values) -> keys.forEach(key -> ensure(map.containsKey(key))));
    }

    @Test
    public void testEqualsHashCode()
    {
        withPopulatedMap((a, keys, values) ->
        {
            var b = map();
            putAll(b, keys, values);
            ensureEqual(a, b);
            b.put(99, (byte) -1);
            ensureNotEqual(a, b);
        });
    }

    @Test
    public void testFreeze()
    {
        withPopulatedMap((a, keys, values) ->
        {
            var b = map();
            putAll(b, keys, values);
            ensureEqual(a, b);
            b.compress(CompressibleCollection.Method.FREEZE);
            ensureEqual(a, b);
        });
        withPopulatedMap((a, keys, values) ->
        {
            var b = map();
            putAll(b, keys, values);
            putAll(b, keys, values);
            ensureEqual(a, b);
            b.compress(CompressibleCollection.Method.FREEZE);
            ensureEqual(a, b);
        });
    }

    @Test
    public void testGetPut()
    {
        withPopulatedMap((map, keys, values) ->
        {
            index = 0;
            keys.forEach(key -> ensureEqual(map.get(key), values.get(index++)));
        });
    }

    @Test
    public void testKeys()
    {
        {
            var map = map();
            ensureEqual(0, map.size());
            ensureFalse(map.keys().hasNext());
        }
        {
            withPopulatedMap((map, keys, values) ->
            {
                var iterator = map.keys();
                int count = 0;
                while (iterator.hasNext())
                {
                    var key = iterator.next();
                    ensure((map.containsKey(key)));
                    count++;
                }
                ensureEqual(map.size(), count);
            });
        }
    }

    @Test
    public void testRemove()
    {
        withPopulatedMap((map, keys, values) ->
                keys.forEach(key ->
                {
                    int size = map.size();
                    boolean exists = map.containsKey(key);
                    map.remove(key);
                    ensure(map.isEmpty(map.get(key)));
                    if (exists)
                    {
                        ensureEqual(map.size(), size - 1);
                    }
                }));
    }

    @Test
    public void testSerialization()
    {
        withPopulatedMap((map, keys, values) -> testSerialization(map));
    }

    @Test
    public void testValues()
    {
        withPopulatedMap((map, keys, values) ->
        {
            var iterator = map.values();
            int count = 0;
            var valueSet = new HashSet<>(values);
            while (iterator.hasNext())
            {
                var value = iterator.next();
                ensure((valueSet.contains(value)));
                count++;
            }
            ensureEqual(map.size(), count);
        });
    }

    private LongToByteMap map()
    {
        var map = (LongToByteMap) new LongToByteMap("test")
                .nullLong(Long.MIN_VALUE)
                .nullByte(Byte.MIN_VALUE);
        map.initialize();
        return map;
    }

    private void putAll(LongToByteMap map, List<Long> keys, List<Byte> values)
    {
        index = 0;
        keys.forEach(key -> map.put(key, values.get(index++ % values.size())));
    }

    private void withPopulatedMap(MapTest test)
    {
        var map = map();
        var keys = random().list(Repeats.NO_REPEATS, Long.class);
        var values = random().list(Byte.class);
        putAll(map, keys, values);
        test.test(map, keys, values);
    }
}
