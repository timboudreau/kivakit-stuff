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

package com.telenav.kivakit.primitive.collections.map.multi;

import com.telenav.kivakit.interfaces.collection.Sized;
import com.telenav.kivakit.interfaces.numeric.Quantizable;
import com.telenav.kivakit.primitive.collections.array.scalars.IntArray;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveMultiMap;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import java.util.List;

@UmlClassDiagram(diagram = DiagramPrimitiveMultiMap.class)
public interface IntMultiMap extends Sized
{
    IntArray get(long key);

    void putAll(long key, IntArray values);

    void putAll(long key, List<? extends Quantizable> values);
}
