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

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.interfaces.numeric.Quantizable;
import com.telenav.kivakit.primitive.collections.array.scalars.IntArray;
import com.telenav.kivakit.primitive.collections.iteration.IntIterator;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveList;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import java.util.List;

/**
 * Stores linked lists of int values in a space efficient way.
 * <p>
 * The {@link #add(int, int)} method takes a list identifier and adds the given value to the head of the list, returning
 * an identifier to the new list. To start a new list, pass {@link #NEW_LIST} as the list identifier. To retrieve the
 * list as an {@link IntIterator}, call {@link #list(int)} with the list identifier.
 *
 * @author jonathanl (shibo)
 * @see IntIterator
 * @see Quantizable
 */
@SuppressWarnings("DuplicatedCode") @UmlClassDiagram(diagram = DiagramPrimitiveList.class)
public class IntLinkedListStore extends PrimitiveListStore
{
    /** List identifier to start a new list */
    public static final int NEW_LIST = 0;

    /** Pointer value for end of list */
    private static final int END_OF_LIST = 0;

    /** Array of list values that correspond with next pointers */
    private IntArray values;

    /** List next pointers */
    private IntArray next;

    /** The next available spot to add to in the arrays */
    private int addAt = 1;

    public IntLinkedListStore(String objectName)
    {
        super(objectName);
    }

    protected IntLinkedListStore()
    {
    }

    /**
     * Adds the given value to the given list by head insertion.
     * <p>
     * To start a new list, pass IntLinkedListStore.NEW_LIST as the list identifier.
     */
    public int add(int list, int value)
    {
        // Get a new list index,
        var newList = addAt++;

        // store the value at that index,
        values.set(newList, value);

        // then set the next item for the new list to point to the old list
        next.set(newList, list);

        // and return the new list.
        return newList;
    }

    /**
     * Adds each of the given values to the identified list
     */
    public int addAll(int list, IntArray values)
    {
        for (var index = 0; index < values.size(); index++)
        {
            list = add(list, values.get(index));
        }
        return list;
    }

    /**
     * Adds each of the given values to the identified list
     */
    public int addAll(int list, int[] values)
    {
        for (var value : values)
        {
            list = add(list, value);
        }
        return list;
    }

    /**
     * Adds the quantum of each value to the identified list
     */
    public int addAll(int list, List<? extends Quantizable> values)
    {
        for (var value : values)
        {
            list = add(list, (int) value.quantum());
        }
        return list;
    }

    @Override
    public Count capacity()
    {
        return values.capacity().plus(next.capacity());
    }

    /**
     * @return An iterator over the values in the identifier list
     */
    public IntIterator list(int list)
    {
        var outer = this;
        return new IntIterator()
        {
            private int index = list;

            @Override
            public boolean hasNext()
            {
                return index != END_OF_LIST;
            }

            @Override
            public int next()
            {
                var value = outer.values.get(index);
                index = outer.next.get(index);
                return value;
            }
        };
    }

    @Override
    public Method onCompress(Method method)
    {
        values.compress(method);
        next.compress(method);

        return Method.RESIZE;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onInitialize()
    {
        super.onInitialize();

        values = new IntArray(objectName() + ".values");
        values.initialSize(initialSize());
        values.initialize();

        next = new IntArray(objectName() + ".next");
        next.initialSize(initialSize());
        next.nullInt(END_OF_LIST);
        next.initialize();
    }

    @Override
    public void read(Kryo kryo, Input input)
    {
        super.read(kryo, input);
        values = kryo.readObject(input, IntArray.class);
        next = kryo.readObject(input, IntArray.class);
    }

    /**
     * Removes the given value from the identified list
     */
    public int remove(int list, int value)
    {
        var at = list;
        var previous = NEW_LIST;
        while (!next.isNull(at))
        {
            var next = this.next.get(at);
            if (values.get(at) == value)
            {
                if (at == list)
                {
                    return next;
                }
                else
                {
                    this.next.set(previous, next);
                    break;
                }
            }
            previous = at;
            at = next;
        }
        return list;
    }

    @Override
    public void write(Kryo kryo, Output output)
    {
        super.write(kryo, output);
        kryo.writeObject(output, values);
        kryo.writeObject(output, next);
    }
}
