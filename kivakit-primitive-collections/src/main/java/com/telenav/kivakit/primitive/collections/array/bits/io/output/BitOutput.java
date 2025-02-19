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

package com.telenav.kivakit.primitive.collections.array.bits.io.output;

import com.telenav.kivakit.core.io.IO;
import com.telenav.kivakit.primitive.collections.array.bits.io.BitWriter;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveArrayBitIo;
import com.telenav.lexakai.annotations.LexakaiJavadoc;
import com.telenav.lexakai.annotations.UmlClassDiagram;

import java.io.IOException;
import java.io.OutputStream;

import static com.telenav.kivakit.core.ensure.Ensure.unsupported;

/**
 * A {@link BitWriter} that outputs bytes to an {@link OutputStream}.
 *
 * @author jonathanl (shibo)
 * @see BaseBitWriter
 * @see BitWriter
 */
@UmlClassDiagram(diagram = DiagramPrimitiveArrayBitIo.class)
@LexakaiJavadoc(complete = true)
public class BitOutput extends BaseBitWriter
{
    private final OutputStream out;

    /**
     * Construct from an output stream
     */
    public BitOutput(OutputStream out)
    {
        this.out = out;
    }

    /**
     * Close the stream
     */
    @Override
    public void onClose()
    {
        IO.close(out);
    }

    @Override
    protected void onFlush(byte value)
    {
        unsupported("Cannot flush bits on an output stream");
    }

    /**
     * Writes a byte to the output stream
     */
    @Override
    protected void onWrite(byte value)
    {
        try
        {
            out.write(value);
        }
        catch (IOException e)
        {
            throw new IllegalStateException("Unable to write " + value, e);
        }
    }
}
