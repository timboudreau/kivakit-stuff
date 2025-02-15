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

package com.telenav.kivakit.primitive.collections.list.store;

import com.telenav.kivakit.core.value.mutable.MutableValue;
import com.telenav.kivakit.primitive.collections.PrimitiveCollectionsUnitTest;
import org.junit.Test;

import java.util.Collections;

import static com.telenav.kivakit.core.test.CoreUnitTest.Repeats.ALLOW_REPEATS;

public class IntLinkedListStoreTest extends PrimitiveCollectionsUnitTest
{
    @Test
    public void test()
    {
        var store = new IntLinkedListStore("test");
        store.initialize();

        var list = IntLinkedListStore.NEW_LIST;
        list = store.add(list, 1);
        list = store.add(list, 2);
        list = store.add(list, 3);
        {
            var iterator = store.list(list);
            var value = 3;
            while (iterator.hasNext())
            {
                var next = iterator.next();
                ensureEqual(value--, next);
            }
        }
        list = store.remove(list, 2);
        {
            var iterator = store.list(list);
            ensure(iterator.hasNext());
            ensureEqual(3, iterator.next());
            ensure(iterator.hasNext());
            ensureEqual(1, iterator.next());
            ensureFalse(iterator.hasNext());
        }
        list = store.remove(list, 3);
        {
            var iterator = store.list(list);
            ensure(iterator.hasNext());
            ensureEqual(1, iterator.next());
            ensureFalse(iterator.hasNext());
        }
        list = store.remove(list, 1);
        {
            var iterator = store.list(list);
            ensureFalse(iterator.hasNext());
        }
    }

    @Test
    public void test2()
    {
        var store = new IntLinkedListStore("test");
        store.initialize();

        var list = IntLinkedListStore.NEW_LIST;
        list = store.add(list, 1);
        list = store.add(list, 2);
        list = store.add(list, 3);
        {
            var iterator = store.list(list);
            var value = 3;
            while (iterator.hasNext())
            {
                var next = iterator.next();
                ensureEqual(value--, next);
            }
        }
        list = store.remove(list, 1);
        {
            var iterator = store.list(list);
            ensure(iterator.hasNext());
            ensureEqual(3, iterator.next());
            ensure(iterator.hasNext());
            ensureEqual(2, iterator.next());
            ensureFalse(iterator.hasNext());
        }
        list = store.remove(list, 3);
        {
            var iterator = store.list(list);
            ensure(iterator.hasNext());
            ensureEqual(2, iterator.next());
            ensureFalse(iterator.hasNext());
        }
        list = store.remove(list, 2);
        {
            var iterator = store.list(list);
            ensureFalse(iterator.hasNext());
        }
    }

    @Test
    public void testRandom()
    {
        var store = new IntLinkedListStore("test");
        store.initialize();

        var list1 = new MutableValue<>(IntLinkedListStore.NEW_LIST);
        var list2 = new MutableValue<>(IntLinkedListStore.NEW_LIST);

        var values = random().list(ALLOW_REPEATS, Integer.class);
        values.forEach(value ->
        {
            list1.set(store.add(list1.get(), value));
            list2.set(store.add(list2.get(), value));
        });

        // The list will be backwards because of head insertion
        Collections.reverse(values);

        // Ensure list 1
        var list1Values = store.list(list1.get());
        var iterator1 = values.iterator();
        while (list1Values.hasNext())
        {
            var value = list1Values.next();
            ensureEqual(iterator1.next(), value);
        }
        ensure(!iterator1.hasNext());

        // Ensure list 2
        var list2Values = store.list(list1.get());
        var iterator2 = values.iterator();
        while (list2Values.hasNext())
        {
            var value = list2Values.next();
            ensureEqual(iterator2.next(), value);
        }
        ensure(!iterator2.hasNext());
    }
}
