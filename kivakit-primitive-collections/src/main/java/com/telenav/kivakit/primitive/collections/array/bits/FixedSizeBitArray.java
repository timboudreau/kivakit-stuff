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

package com.telenav.kivakit.primitive.collections.array.bits;

import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveArray;
import com.telenav.lexakai.annotations.UmlClassDiagram;

/**
 * A really simple, fixed-length bit array.
 *
 * @author jonathanl (shibo)
 */
@UmlClassDiagram(diagram = DiagramPrimitiveArray.class)
public class FixedSizeBitArray
{
    private long[] words;

    public FixedSizeBitArray(int bits)
    {
        words = new long[((bits - 1) / 64) + 1];
    }

    private FixedSizeBitArray()
    {
    }

    public void clear(int bitIndex)
    {
        words[bitIndex / 64] &= ~(1L << (bitIndex % 64));
    }

    public boolean get(int bitIndex)
    {
        return (words[bitIndex / 64] & (1L << (bitIndex % 64))) != 0;
    }

    public void set(int bitIndex)
    {
        words[bitIndex / 64] |= (1L << (bitIndex % 64));
    }

    public void set(int bitIndex, boolean value)
    {
        if (value)
        {
            set(bitIndex);
        }
        else
        {
            clear(bitIndex);
        }
    }
}
