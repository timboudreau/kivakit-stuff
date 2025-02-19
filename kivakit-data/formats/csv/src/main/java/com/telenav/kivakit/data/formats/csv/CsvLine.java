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
import com.telenav.kivakit.conversion.core.language.object.ObjectPopulator;
import com.telenav.kivakit.core.collections.list.ObjectList;
import com.telenav.kivakit.core.collections.list.StringList;
import com.telenav.kivakit.core.language.reflection.Type;
import com.telenav.kivakit.core.language.reflection.property.Property;
import com.telenav.kivakit.core.language.reflection.property.PropertyValues;
import com.telenav.kivakit.core.messaging.repeaters.RepeaterMixin;
import com.telenav.kivakit.core.string.Strings;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.data.formats.csv.lexakai.DiagramCsv;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.lexakai.annotations.UmlClassDiagram;

/**
 * A model of a line in a CSV (Comma Separated Variable) file. {@link CsvLine} objects are produced by {@link CsvReader}
 * and consumed by {@link CsvWriter}.
 *
 * <p><b>Properties</b></p>
 *
 * <p>
 * This model has a line number available with {@link #lineNumber()} and it conforms to a {@link CsvSchema}, available
 * with {@link #schema()}. Columns are separated in the CSV file with a delimiter character, normally a comma, that is
 * passed to the constructor {@link #CsvLine(CsvSchema, char)}. Values from the line can be retrieved by column with
 * {@link #get(CsvColumn)} and they can be provided with {@link #set(CsvColumn, Object)}.
 * </p>
 *
 * <p><b>Converting a Line to an Object</b></p>
 *
 * <p>
 * A {@link CsvLine} can be converted directly to an object with {@link #populatedObject(Class)}. A new instance of the
 * class is created and its properties are populated using the {@link CsvSchema} of this line. For details, see {@link
 * #populatedObject(Class)} and {@link #valueFor(Property)}.
 * </p>
 *
 * @author jonathanl (shibo)
 * @see CsvSchema
 * @see CsvColumn
 * @see CsvReader
 * @see CsvWriter
 */
@UmlClassDiagram(diagram = DiagramCsv.class)
@LexakaiJavadoc(complete = true)
public class CsvLine extends StringList implements PropertyValues, RepeaterMixin
{
    /** The schema that this line obeys */
    private final transient CsvSchema schema;

    /**
     * The line number of this CSV line in the file or input stream being read. If the object is being written, the
     * value will be -1.
     */
    private int lineNumber = -1;

    /** The value separator, by default a comma */
    private final char delimiter;

    /**
     * Construct with a given schema and delimiter
     */
    public CsvLine(CsvSchema schema, char delimiter)
    {
        super(Maximum._10_000);

        this.schema = schema;
        this.delimiter = delimiter;
    }

    /**
     * @return The value of the given column
     */
    public <T> T get(CsvColumn<T> column)
    {
        var text = string(column);
        return text == null ? null : column.asType(text);
    }

    /**
     * @return The value of the given column
     */
    public <T> T get(CsvColumn<T> column, StringConverter<T> converter)
    {
        var text = string(column);
        return text == null ? null : column.asType(text, converter);
    }

    /**
     * @return The value of the given column
     */
    public <T> ObjectList<T> get(CsvColumn<T> column, BaseStringConverter<T> converter)
    {
        var text = string(column);
        return text == null ? null : column.asType(text, converter);
    }

    /**
     * @return The line number of this CSV line in the input, or -1 if the line was not read from an input source (if it
     * was constructed to be written)
     */
    public int lineNumber()
    {
        return lineNumber;
    }

    /**
     * @return An object of the given type with its properties populated by {@link ObjectPopulator} using {@link
     * CsvPropertyFilter}. Properties of the object that correspond to {@link CsvColumn}s using KivaKit property naming
     * are retrieved with {@link PropertyValues#valueFor(Property)} (see below) and set on the new object by reflection.
     * The result is an object corresponding to this line.
     */
    public <T> T populatedObject(Class<T> type)
    {
        try
        {
            return new ObjectPopulator(new CsvPropertyFilter(schema()), () -> this)
                    .populate(Type.forClass(type).newInstance());
        }
        catch (Exception e)
        {
            problem(e, "Unable to create or populate ${debug}", type);
            return null;
        }
    }

    /**
     * @return The schema for this line
     */
    public CsvSchema schema()
    {
        return schema;
    }

    /**
     * Sets the given column to the given value
     */
    public <T> void set(CsvColumn<T> column, T value)
    {
        set(column, column.asString(value));
    }

    /**
     * Sets the given column to the given value
     */
    public void set(CsvColumn<?> column, String value)
    {
        if (column != null)
        {
            var index = column.index();
            while (index >= size())
            {
                add("");
            }
            set(index, value);
        }
    }

    /**
     * @return The unconverted string value for the given column
     */
    public String string(CsvColumn<?> column)
    {
        var index = column.index();
        return index >= size() ? null : get(column.index());
    }

    @Override
    public String toString()
    {
        return join(delimiter());
    }

    @Override
    public String toString(String value)
    {
        // Escape quotes
        if (value.contains("\""))
        {
            value = Strings.replaceAll(value, "\"", "\"\"");
        }

        // And quote values with separators in them
        if (value.indexOf(delimiter()) >= 0)
        {
            value = '"' + value + '"';
        }
        return value;
    }

    /**
     * Implementation of {@link PropertyValues} used by {@link ObjectPopulator} in {@link #populatedObject(Class)} to
     * get the value of the given property using the property name to find the {@link CsvColumn}.
     */
    @Override
    public Object valueFor(Property property)
    {
        var column = schema().columnForName(property.name());
        if (column != null)
        {
            return get(column);
        }
        return null;
    }

    /**
     * @return The separator used in this CSV line
     */
    protected char delimiter()
    {
        return delimiter;
    }

    @Override
    protected String separator()
    {
        return Character.toString(delimiter());
    }

    /**
     * Used by CSV reader to set the line number for this line
     */
    void lineNumber(int lineNumber)
    {
        this.lineNumber = lineNumber;
    }
}
