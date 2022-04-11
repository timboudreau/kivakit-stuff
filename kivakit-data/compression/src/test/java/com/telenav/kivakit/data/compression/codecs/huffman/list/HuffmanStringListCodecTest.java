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

package com.telenav.kivakit.data.compression.codecs.huffman.list;

import com.telenav.kivakit.core.collections.Lists;
import com.telenav.kivakit.core.collections.map.CountMap;
import com.telenav.kivakit.core.messaging.Listener;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.core.value.count.Count;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.core.value.count.Minimum;
import com.telenav.kivakit.data.compression.Codec;
import com.telenav.kivakit.data.compression.DataCompressionUnitTest;
import com.telenav.kivakit.data.compression.SymbolConsumer;
import com.telenav.kivakit.data.compression.codecs.huffman.character.HuffmanCharacterCodec;
import com.telenav.kivakit.data.compression.codecs.huffman.string.HuffmanStringCodec;
import com.telenav.kivakit.data.compression.codecs.huffman.tree.Symbols;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import static com.telenav.kivakit.core.value.count.Count._10;
import static com.telenav.kivakit.core.value.count.Count._100;
import static com.telenav.kivakit.interfaces.code.FilteredLoopBody.FilterAction.ACCEPT;

@SuppressWarnings("SpellCheckingInspection")
public class HuffmanStringListCodecTest extends DataCompressionUnitTest
{
    @Test
    public void testBug()
    {
        //    [HuffmanCodec size = 2, bits = 1]:
        //        1. 0 -> 'm' (8,524)
        //        2. 1 -> 'gxafxac' (9,202)
        //    [HuffmanCodec size = 7, bits = 5]:
        //        1. 11100 -> 0x00 (1,024)
        //        2. 11101 -> 0x01 (1,024)
        //        3. 1111 -> 'x' (5,566)
        //        4. 110 -> 'e' (6,379)
        //        5. 00 -> 'j' (8,098)
        //        6. 01 -> 'd' (8,154)
        //        7. 10 -> 'm' (10,826)
        //
        //    input: [ohkh, m, ohkh, m, gxafxac, ohkh, gxafxac, m, m, gxafxac, ohkh, m, gxafxac, gxafxac, gxafxac, ohkh, m, m, m]

        var stringSymbols = new Symbols<>(new CountMap<String>()
                .add("m", Objects.requireNonNull(Count.parseCount(this, "8,524")))
                .add("gxafxac", Objects.requireNonNull(Count.parseCount(this, "9,202"))));

        var string = HuffmanStringCodec.from(stringSymbols, Maximum._8);

        var characterSymbols = new Symbols<>(new CountMap<Character>()
                .add('m', Objects.requireNonNull(Count.parseCount(this, "10,826")))
                .add('d', Objects.requireNonNull(Count.parseCount(this, "8,154")))
                .add('j', Objects.requireNonNull(Count.parseCount(this, "8,098")))
                .add('e', Objects.requireNonNull(Count.parseCount(this, "6,379")))
                .add('x', Objects.requireNonNull(Count.parseCount(this, "5,566")))
                .add(HuffmanCharacterCodec.ESCAPE, Count._1024)
                .add(HuffmanCharacterCodec.END_OF_STRING, Count._1024), HuffmanCharacterCodec.ESCAPE, Minimum._1);

        var character = HuffmanCharacterCodec.from(characterSymbols, Maximum._8);

        var codec = new HuffmanStringListCodec(string, character);

        test(codec, Lists.arrayList("ohkh", "m", "ohkh", "m", "gxafxac", "ohkh", "gxafxac",
                "m", "m", "gxafxac", "ohkh", "m", "gxafxac", "gxafxac", "gxafxac", "ohkh", "m", "m", "m"));

        test(codec, Lists.arrayList("ohkh"));
    }

    @Test
    public void testDecode()
    {
        var string = HuffmanStringCodec.from(properties("string.codec"));
        var character = HuffmanCharacterCodec.from(this, properties("character.codec"), HuffmanCharacterCodec.ESCAPE);
        var codec = new HuffmanStringListCodec(string, character);

        test(codec, Lists.arrayList("bicycle", "barrier", "highway", "banana"));
        test(codec, Lists.arrayList("oneway", "turkey", "foot", "access", "footway"));
        test(codec, Lists.arrayList("gorilla", "amenity", "footway", "monkey", "maxspeed", "footway"));
    }

    @Test
    public void testRandom()
    {
        var progress = BroadcastingProgressReporter.create(Listener.deafListener(), "codec");
        _10.loop(codecNumber ->
        {
            var stringSymbols = randomStringSymbols(2, 16, 1, 32);
            var string = HuffmanStringCodec.from(stringSymbols, Maximum._8);

            var characterSymbols = randomCharacterSymbols(1, 25);
            var character = HuffmanCharacterCodec.from(characterSymbols, Maximum._8);

            var codec = new HuffmanStringListCodec(string, character);

            var choices = stringSymbols.symbols();
            choices.addAll(randomStringSymbols(2, 8, 1, 32).symbols());

            var test = BroadcastingProgressReporter.create(Listener.deafListener(), "test");
            _100.loop(testNumber ->
            {
                var input = new ArrayList<String>();
                random().rangeInclusive(1, 32).loop(() -> input.add(choices.get(random().randomIntExclusive(0, choices.size() - 1))));
                test(codec, input);
                test.next();
                return ACCEPT;
            });

            progress.next();
            return ACCEPT;
        });
    }

    @Override
    protected void test(Codec<String> codec, List<String> symbols)
    {
        var data = encode(codec, symbols);
        var decoded = new ArrayList<>();
        Count.count(symbols).loop(() -> decoded.add(null));
        data.reset();
        codec.decode(data, (index, value) ->
        {
            decoded.set(index, value);
            return SymbolConsumer.Directive.CONTINUE;
        });
        ensureEqual(decoded, symbols);
    }
}
