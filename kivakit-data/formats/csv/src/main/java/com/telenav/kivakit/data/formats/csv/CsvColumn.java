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

import com.telenav.kivakit.conversion.BaseStringConverter;
import com.telenav.kivakit.conversion.StringConverter;
import com.telenav.kivakit.core.collections.list.ObjectList;
import com.telenav.kivakit.core.value.name.Name;
import com.telenav.kivakit.data.formats.csv.lexakai.DiagramCsv;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import static com.telenav.kivakit.core.ensure.Ensure.ensureNotNull;

/**
 * A typesafe model of a CSV column that belongs to a {@link CsvSchema} and can convert values for the column between
 * strings and objects.
 *
 * <p>
 * {@link CsvColumn}s are added to a {@link CsvSchema} with {@link CsvSchema#CsvSchema(CsvColumn[])} and then used as a
 * key to retrieve values from {@link CsvLine}. This model includes the CSV schema that owns it, as well as the name and
 * index of the column in the data. A {@link StringConverter} converts values for the column to and from objects.
 * </p>
 *
 * @author jonathanl (shibo)
 */
@UmlClassDiagram(diagram = DiagramCsv.class)
@LexakaiJavadoc(complete = true)
public class CsvColumn<Type> extends Name
{
    public static <T> CsvColumn<T> of(String name)
    {
        return CsvColumn.of(name, null);
    }

    public static <T> CsvColumn<T> of(String name, StringConverter<T> converter)
    {
        return new CsvColumn<>(name, converter);
    }

    /** The zero-based index of the column in the CSV schema */
    private int index;

    /** The schema that this column has been added to */
    private CsvSchema schema;

    /** The converter to convert this column to and from its type */
    private final StringConverter<Type> converter;

    /**
     * Construct the named column
     */
    protected CsvColumn(String name, StringConverter<Type> converter)
    {
        super(name);
        this.converter = converter;
    }

    /**
     * @return The text for the given value in this column
     */
    public String asString(Type value, StringConverter<Type> converter)
    {
        ensureNotNull(converter);
        return converter.unconvert(value);
    }

    /**
     * @return The text for the given value in this column
     */
    public String asString(Type value)
    {
        return asString(value, converter);
    }

    /**
     * @return The value of the given text if it is in this column
     */
    public ObjectList<Type> asType(String text, BaseStringConverter<Type> converter)
    {
        ensureNotNull(converter);
        return converter.convertToList(text, ",");
    }

    /**
     * @return The value of the given text if it is in this column
     */
    public Type asType(String text, StringConverter<Type> converter)
    {
        ensureNotNull(converter);
        return converter.convert(text);
    }

    /**
     * @return The value of the given text if it is in this column
     */
    public Type asType(String text)
    {
        return asType(text, converter);
    }

    /**
     * @return The schema that defines this column
     */
    public CsvSchema schema()
    {
        return schema;
    }

    /**
     * @return The index of this column in the schema that references it
     */
    int index()
    {
        return index;
    }

    /**
     * Sets the index for this column
     */
    void index(int index)
    {
        this.index = index;
    }

    /**
     * Sets the schema for this column
     */
    void schema(CsvSchema schema)
    {
        if (this.schema != null)
        {
            throw new IllegalStateException("Column " + this + " has already been added to schema " + this.schema);
        }
        this.schema = schema;
    }
}
