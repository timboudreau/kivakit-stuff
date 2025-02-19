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

package com.telenav.kivakit.data.formats.csv;

import com.telenav.kivakit.conversion.core.language.primitive.DoubleConverter;
import com.telenav.kivakit.conversion.core.language.primitive.IntegerConverter;
import com.telenav.kivakit.core.progress.ProgressReporter;
import com.telenav.kivakit.test.UnitTest;
import com.telenav.kivakit.resource.packages.PackageTrait;
import org.junit.Test;

public class CsvReaderTest extends UnitTest implements PackageTrait
{
    @Test
    public void test()
    {
        var year = CsvColumn.of("year", new IntegerConverter(this));
        var make = CsvColumn.of("make");
        var model = CsvColumn.of("model");
        var description = CsvColumn.of("description");
        var price = CsvColumn.of("price", new DoubleConverter(this));
        var schema = new CsvSchema(year, make, model, description, price);

        var resource = packageResource("SampleCsv.csv");
        try (var myReader = new CsvReader(resource, schema, ',', ProgressReporter.none()))
        {
            myReader.skipLines(1);
            ensure(myReader.hasNext());

            // 1st line
            var firstDataLine = myReader.next();
            ensureEqual(firstDataLine.get(year), 1997);
            ensureEqual(firstDataLine.string(description), "ac, abs, moon");
            ensureEqual(firstDataLine.get(price), 3000.0);

            // 2nd line
            myReader.next();

            // 3rd line
            var thirdLine = myReader.next();
            ensureEqual(thirdLine.string(model), "Venture \"Extended Edition, Very Large\"");

            // 4th line
            var fourthLine = myReader.next();
            ensure(fourthLine.string(description).indexOf('\n') >= 0);

            // 5th line
            var fifthLine = myReader.next();
            ensureEqual(fifthLine.string(model), "\"Venture Extended Edition, Very Large\"");

            // 6th line
            var sixthLine = myReader.next();
            ensureEqual(sixthLine.string(model), "\"K\" Ct");
        }
    }
}
