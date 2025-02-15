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
import com.esotericsoftware.kryo.KryoSerializable;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.primitive.collections.PrimitiveCollection;
import com.telenav.kivakit.primitive.collections.array.scalars.SplitIntArray;
import com.telenav.kivakit.primitive.collections.array.strings.PackedStringArray;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveList;
import com.telenav.lexakai.annotations.UmlClassDiagram;

/**
 * A store of string values that can be written with {@link #set(int, String)} and read with {@link #get(int)}.
 * <p>
 * This class is {@link KryoSerializable}.
 *
 * @author jonathanl (shibo)
 * @see PrimitiveCollection
 */
@UmlClassDiagram(diagram = DiagramPrimitiveList.class)
public class PackedStringStore extends PrimitiveCollection
{
    private PackedStringArray strings;

    private SplitIntArray indexes;

    public PackedStringStore(String objectName)
    {
        super(objectName);
    }

    protected PackedStringStore()
    {
    }

    @Override
    public Count capacity()
    {
        return strings.capacity().plus(indexes.capacity());
    }

    public String get(int index)
    {
        var list = indexes.safeGet(index);
        if (!indexes.isNull(list))
        {
            return strings.safeGet(list);
        }
        return null;
    }

    @Override
    public Method onCompress(Method method)
    {
        strings.compress(method);
        indexes.compress(method);
        return Method.RESIZE;
    }

    @Override
    public void onInitialize()
    {
        super.onInitialize();
        indexes = new SplitIntArray(objectName() + ".indexes");
        indexes.initialize();

        strings = new PackedStringArray(objectName() + ".strings");
        strings.initialize();
    }

    @Override
    public void read(Kryo kryo, Input input)
    {
        super.read(kryo, input);

        strings = kryo.readObject(input, PackedStringArray.class);
        indexes = kryo.readObject(input, SplitIntArray.class);
    }

    public void set(int index, String value)
    {
        assert index > 0;
        if (value != null)
        {
            indexes.set(index, strings.add(value));
        }
        else
        {
            indexes.set(index, nullInt());
        }
    }

    @Override
    public void write(Kryo kryo, Output output)
    {
        super.write(kryo, output);

        kryo.writeObject(output, strings);
        kryo.writeObject(output, indexes);
    }
}
