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

package com.telenav.kivakit.data.compression.codecs.huffman.string;

import com.telenav.kivakit.core.messaging.Listener;
import com.telenav.kivakit.core.progress.reporters.BroadcastingProgressReporter;
import com.telenav.kivakit.data.compression.DataCompressionUnitTest;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static com.telenav.kivakit.core.value.count.Count._10;
import static com.telenav.kivakit.core.value.count.Count._100;
import static com.telenav.kivakit.interfaces.code.FilteredLoopBody.FilterAction.ACCEPT;

public class HuffmanStringCodecTest extends DataCompressionUnitTest
{
    @SuppressWarnings("SpellCheckingInspection")
    @Test
    public void testDecode()
    {
        var codec = HuffmanStringCodec.from(properties("string.codec"));

        test(codec, List.of("bicycle", "barrier", "highway"));
        test(codec, List.of("oneway", "foot", "access", "footway"));
        test(codec, List.of("amenity", "footway", "maxspeed", "footway"));
    }

    @Test
    public void testRandom()
    {
        var progress = BroadcastingProgressReporter.create(Listener.deafListener(), "codec");
        _10.loop(codecNumber ->
        {
            var symbols = randomStringSymbols(2, 100, 1, 100);
            var codec = HuffmanStringCodec.from(symbols);
            var choices = symbols.symbols();

            var test = BroadcastingProgressReporter.create(Listener.deafListener(), "test");
            _100.loop(testNumber ->
            {
                var input = new ArrayList<String>();
                random().rangeInclusive(1, 200).loop(() -> input.add(choices.get(random().randomIntExclusive(0, choices.size() - 1))));
                test(codec, input);
                test.next();
                return ACCEPT;
            });
            progress.next();
            return ACCEPT;
        });
    }
}
