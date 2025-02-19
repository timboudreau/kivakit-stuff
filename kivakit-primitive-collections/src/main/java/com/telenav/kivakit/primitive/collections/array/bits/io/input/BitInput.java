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

package com.telenav.kivakit.primitive.collections.array.bits.io.input;

import com.telenav.kivakit.core.io.IO;
import com.telenav.kivakit.primitive.collections.array.bits.io.BitReader;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveArrayBitIo;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import java.io.InputStream;

import static com.telenav.kivakit.core.ensure.Ensure.unsupported;

/**
 * A {@link BitReader} that gets input from an underlying {@link InputStream}.
 *
 * @author jonathanl (shibo)
 * @see BitReader
 * @see BaseBitReader
 */
@UmlClassDiagram(diagram = DiagramPrimitiveArrayBitIo.class)
@LexakaiJavadoc(complete = true)
public class BitInput extends BaseBitReader
{
    /** The input stream to read */
    private final InputStream in;

    /** The next byte, or a value < 0 if there is no next byte */
    private int next;

    /** The cursor in the input stream */
    private int cursor;

    /**
     * Construct from an input stream
     */
    public BitInput(InputStream in)
    {
        this.in = in;
        next = IO.readByte(in);
        if (next >= 0)
        {
            cursor++;
        }
    }

    /**
     * Close the stream
     */
    @Override
    public void onClose()
    {
        IO.close(in);
    }

    @Override
    protected long byteCursor()
    {
        return cursor;
    }

    @Override
    protected void byteCursor(long position)
    {
        unsupported();
    }

    @Override
    protected boolean hasNextByte()
    {
        return next >= 0;
    }

    @Override
    protected byte nextByte()
    {
        var current = next;
        next = IO.readByte(in);
        cursor++;
        return (byte) current;
    }
}
