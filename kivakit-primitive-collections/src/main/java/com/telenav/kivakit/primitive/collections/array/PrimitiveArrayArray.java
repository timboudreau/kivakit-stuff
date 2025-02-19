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

package com.telenav.kivakit.primitive.collections.array;

import com.telenav.kivakit.primitive.collections.array.arrays.ByteArrayArray;
import com.telenav.kivakit.primitive.collections.array.arrays.IntArrayArray;
import com.telenav.kivakit.primitive.collections.array.arrays.LongArrayArray;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveArrayArray;
import com.telenav.lexakai.annotations.UmlClassDiagram;

/**
 * Base class for two-dimensional primitive arrays.
 *
 * @author jonathanl (shibo)
 * @see ByteArrayArray
 * @see IntArrayArray
 * @see LongArrayArray
 */
@UmlClassDiagram(diagram = DiagramPrimitiveArrayArray.class)
public abstract class PrimitiveArrayArray extends PrimitiveArray
{
    protected PrimitiveArrayArray(String name)
    {
        super(name);
    }

    protected PrimitiveArrayArray()
    {
    }
}
