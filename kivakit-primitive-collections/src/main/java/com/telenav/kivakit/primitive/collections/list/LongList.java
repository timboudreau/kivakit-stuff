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

package com.telenav.kivakit.primitive.collections.list;

import com.telenav.kivakit.primitive.collections.LongCollection;
import com.telenav.kivakit.primitive.collections.iteration.LongIterator;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveList;
import com.telenav.lexakai.annotations.UmlClassDiagram;

/**
 * A long collection supporting indexed operations. The first and last values in the list can be retrieved with {@link
 * #first()} and {@link #last()}. Values at a given index can be altered and retrieved with {@link #clear(int)}, {@link
 * #get(int)}, {@link #safeGet(int)} and {@link #set(int, long)}.
 * <p>
 * If the list is sorted, it can be searched with {@link #binarySearch(long)}, which returns the index of the value if
 * it is found and a value less than zero if it is not.
 * <p>
 * A default iterator implementation is provided by {@link #iterator()}.
 *
 * @author jonathanl (shibo)
 * @see LongCollection
 */
@SuppressWarnings("DuplicatedCode") @UmlClassDiagram(diagram = DiagramPrimitiveList.class)
public interface LongList extends LongCollection, PrimitiveList
{
    /**
     * Binary search adapted from Java Arrays#binarySearch
     *
     * @param target The target to search for
     * @return The index of the given target. The return value will be &lt; 0 if the target was not found.
     */
    default int binarySearch(long target)
    {
        var low = 0;
        var high = size() - 1;

        while (low <= high)
        {
            var middle = (low + high) / 2;
            var value = get(middle);

            if (value < target)
            {
                low = middle + 1;
            }
            else if (value > target)
            {
                high = middle - 1;
            }
            else
            {
                return middle; // target found
            }
        }
        return -(low + 1);  // target not found.
    }

    /**
     * Sets the value at the given index to the {@link #nullLong()} value
     */
    default void clear(int index)
    {
        set(index, nullLong());
    }

    /**
     * @return Location of the read / write cursor
     */
    int cursor();

    /**
     * Sets the position of the read / write cursor
     */
    void cursor(int position);

    /**
     * @return The first element in this list
     */
    default long first()
    {
        return get(0);
    }

    /**
     * @return The long at the given index
     */
    long get(int index);

    /**
     * {@inheritDoc}
     */
    @Override
    default long getPrimitive(int index)
    {
        return get(index);
    }

    @Override
    default boolean isPrimitiveNull(long value)
    {
        return isNull(value);
    }

    /***
     * @return Default iterator implementation
     */
    @Override
    default LongIterator iterator()
    {
        return new LongIterator()
        {
            int index;

            @Override
            public boolean hasNext()
            {
                return index < size();
            }

            @Override
            public long next()
            {
                long next;
                do
                {
                    next = safeGet(index++);
                }
                while (isNull(next) && index < size());
                return next;
            }
        };
    }

    /**
     * @return The last element in this list
     */
    default long last()
    {
        return get(size() - 1);
    }

    /**
     * @return The value at the given index, but if the index is out of range, null is returned.
     */
    long safeGet(int index);

    /**
     * Sets the list entry at the given index to the given value
     */
    void set(int index, long value);
}
