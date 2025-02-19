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

package com.telenav.kivakit.data.compression;

import com.telenav.kivakit.core.collections.map.CountMap;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Minimum;
import com.telenav.kivakit.core.value.count.MutableCount;
import com.telenav.kivakit.core.value.mutable.MutableValue;
import com.telenav.kivakit.data.compression.codecs.huffman.character.HuffmanCharacterCodec;
import com.telenav.kivakit.data.compression.codecs.huffman.tree.Symbols;
import com.telenav.kivakit.primitive.collections.array.scalars.ByteArray;
import com.telenav.kivakit.primitive.collections.list.ByteList;
import com.telenav.kivakit.properties.PropertyMap;
import com.telenav.kivakit.serialization.kryo.KryoUnitTest;
import com.telenav.kivakit.serialization.kryo.types.CoreKryoTypes;
import com.telenav.kivakit.serialization.kryo.types.KryoTypes;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static com.telenav.kivakit.core.value.count.Count._10;

/**
 * @author jonathanl (shibo)
 */
@SuppressWarnings("SameParameterValue")
public class DataCompressionUnitTest extends KryoUnitTest
{
    protected ByteList encode(Codec<String> codec, List<String> values)
    {
        var data = new ByteArray("data");
        data.initialize();
        return codec.encode(data, SymbolProducer.fromList(values));
    }

    @NotNull
    protected Symbols<String> fixedSymbolSet()
    {
        return new Symbols<>(new CountMap<String>()
                .add("abc", Count._1_000)
                .add("def", Count._100)
                .add("ghi", _10)
                .add("jkl", Count._1));
    }

    @Override
    protected KryoTypes kryoTypes()
    {
        return new CoreKryoTypes().mergedWith(new DataCompressionKryoTypes());
    }

    protected PropertyMap properties(String name)
    {
        return PropertyMap.load(packageResource(name));
    }

    @NotNull
    protected Symbols<Character> randomCharacterSymbols(int minimum, int maximum)
    {
        var frequencies = new CountMap<Character>();
        random().rangeInclusive(minimum, maximum).loop(() ->
                frequencies.add(random().letter(), Count.count(random().randomIntExclusive(1, 10_000))));
        frequencies.add(HuffmanCharacterCodec.ESCAPE, Count._1024);
        frequencies.add(HuffmanCharacterCodec.END_OF_STRING, Count._1024);
        return new Symbols<>(frequencies, HuffmanCharacterCodec.ESCAPE, Minimum._1);
    }

    protected Symbols<String> randomStringSymbols(int minimum,
                                                  int maximum,
                                                  int minimumLength,
                                                  int maximumLength)
    {
        var frequencies = new CountMap<String>();
        var range = random().rangeInclusive(minimum, maximum, minimumLength);
        range.loop(() ->
        {
            while (true)
            {
                var value = random().letters(minimumLength, maximumLength);
                if (!frequencies.contains(value))
                {
                    frequencies.add(value, Count.count(random().randomIntExclusive(1, 10_000)));
                    break;
                }
            }
        });
        ensure(!frequencies.isEmpty());
        return new Symbols<>(frequencies);
    }

    protected void test(Codec<String> codec, List<String> symbols)
    {
        var data = encode(codec, symbols);
        testDecode(codec, data, symbols);
    }

    protected void testDecode(Codec<String> codec, ByteList data, List<String> expected)
    {
        data.reset();
        var count = new MutableCount();
        var indexValue = new MutableValue<Integer>();
        codec.decode(data, (index, next) ->
        {
            ensureEqual(index, (int) count.asLong());
            ensureEqual(next, expected.get(index));
            indexValue.set(index);
            count.increment();
            return index < expected.size() - 1 ? SymbolConsumer.Directive.CONTINUE : SymbolConsumer.Directive.STOP;
        });
        ensureEqual((int) count.asLong(), expected.size());
        ensureEqual(indexValue.get(), expected.size() - 1);
    }
}
