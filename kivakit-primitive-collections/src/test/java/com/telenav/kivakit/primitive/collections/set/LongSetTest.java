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

package com.telenav.kivakit.primitive.collections.set;

import com.telenav.kivakit.primitive.collections.CompressibleCollection;
import com.telenav.kivakit.primitive.collections.PrimitiveCollectionsUnitTest;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;

public class LongSetTest extends PrimitiveCollectionsUnitTest
{
    @FunctionalInterface
    interface MapTest
    {
        void test(LongSet set, List<Long> values);
    }

    @Test
    public void testClear()
    {
        withPopulatedSet((set, values) ->
        {
            ensureFalse(set.isEmpty());
            set.clear();
            ensure(set.isEmpty());
        });
    }

    @Test
    public void testContains()
    {
        withPopulatedSet((set, values) -> values.forEach(value -> ensure(set.contains(value))));
    }

    @Test
    public void testEqualsHashCode()
    {
        withPopulatedSet((a, values) ->
        {
            var b = set();
            addAll(b, values);
            ensureEqual(a, b);
            b.add(-1);
            ensureNotEqual(a, b);
        });
    }

    @Test
    public void testFreeze()
    {
        withPopulatedSet((a, values) ->
        {
            var b = set();
            addAll(b, values);
            ensureEqual(a, b);
            b.compress(CompressibleCollection.Method.FREEZE);
            ensureEqual(a, b);
        });
        withPopulatedSet((a, values) ->
        {
            var b = set();
            addAll(b, values);
            addAll(b, values);
            ensureEqual(a, b);
            b.compress(CompressibleCollection.Method.FREEZE);
            ensureEqual(a, b);
        });
    }

    @Test
    public void testRemove()
    {
        withPopulatedSet((set, values) ->
                values.forEach(value ->
                {
                    var size = set.size();
                    var exists = set.contains(value);
                    set.remove(value);
                    ensure(!set.contains(value));
                    if (exists)
                    {
                        ensureEqual(set.size(), size - 1);
                    }
                }));
    }

    @Test
    public void testSerialization()
    {
        withPopulatedSet((set, values) -> testSerialization(set));
    }

    @Test
    public void testValues()
    {
        withPopulatedSet((set, values) ->
        {
            var iterator = set.values();
            var count = 0;
            var valueSet = new HashSet<>(values);
            while (iterator.hasNext())
            {
                var value = iterator.next();
                ensure(valueSet.contains(value));
                count++;
            }
            ensureEqual(count, set.size());
        });
    }

    private void addAll(LongSet set, List<Long> values)
    {
        values.forEach(set::add);
    }

    private LongSet set()
    {
        var set = new LongSet("test");
        set.initialize();
        return set;
    }

    private void withPopulatedSet(MapTest test)
    {
        var set = set();
        var values = random().list(Repeats.NO_REPEATS, Long.class, value -> !value.equals(set.nullLong()));
        addAll(set, values);
        test.test(set, values);
    }
}
