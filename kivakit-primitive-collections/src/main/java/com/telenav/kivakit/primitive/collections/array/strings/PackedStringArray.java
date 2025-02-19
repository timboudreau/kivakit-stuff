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

package com.telenav.kivakit.primitive.collections.array.strings;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;
import com.telenav.kivakit.core.collections.map.CacheMap;
import com.telenav.kivakit.core.logging.Logger;
import com.telenav.kivakit.core.logging.LoggerFactory;
import com.telenav.kivakit.core.messaging.Debug;
import com.telenav.kivakit.core.string.AsciiArt;
import com.telenav.kivakit.core.string.Strings;
import com.telenav.kivakit.core.value.count.BitCount;
import com.telenav.kivakit.core.value.count.Estimate;
import com.telenav.kivakit.core.value.count.Maximum;
import com.telenav.kivakit.core.vm.JavaVirtualMachine;
import com.telenav.kivakit.primitive.collections.CompressibleCollection;
import com.telenav.kivakit.primitive.collections.array.PrimitiveArray;
import com.telenav.kivakit.primitive.collections.array.scalars.SplitByteArray;
import com.telenav.kivakit.primitive.collections.array.scalars.SplitCharArray;
import com.telenav.kivakit.primitive.collections.lexakai.DiagramPrimitiveArray;
import com.telenav.lexakai.annotations.UmlClassDiagram;

/**
 * A store of strings that can be added to that is packed according to information content, using 8 or 16 bit storage
 * for ASCII and Unicode characters, respectively. Strings are added with {@link #add(String)}, which returns an
 * identifier that can be used to retrieve the string again with {@link #get(int)} or {@link #safeGet(int)}.
 * <p>
 * In addition to reducing space by storing ASCII strings as bytes, this store performs string pooling. Adding a string
 * that has been added in the past N (65,536 by default) additions will return the identifier of the previously added
 * string instead of adding a new string.
 *
 * @author jonathanl (shibo)
 */
@UmlClassDiagram(diagram = DiagramPrimitiveArray.class)
public class PackedStringArray extends PrimitiveArray
{
    private static final Logger LOGGER = LoggerFactory.newLogger();

    private static final Debug DEBUG = new Debug(LOGGER);

    private static final int TYPE_MASK = (int) BitCount.bitCount(Type.UNICODE.ordinal()).mask();

    private static final int TYPE_SHIFT = 32 - 1 - BitCount.bitCount(Type.UNICODE.ordinal()).asInt();

    private enum Type
    {
        ASCII,
        UNICODE;

        public static Type forIdentifier(int identifier)
        {
            switch (identifier)
            {
                case 0:
                    return ASCII;

                case 1:
                    return UNICODE;

                default:
                    throw new IllegalArgumentException();
            }
        }
    }

    /**
     * All simple ASCII (7-bit character) strings laid out end-to-end (with no terminators)
     */
    private SplitByteArray asciiCharacters;

    /**
     * Insert index to the asciiCharacters array
     */
    private int asciiCharacterIndex = 1;

    /**
     * All character strings in this string array laid out end-to-end
     */
    private SplitCharArray unicodeCharacters;

    /**
     * Insert index to the characters array
     */
    private int characterIndex = 1;

    /**
     * The size of this array
     */
    private int size;

    /**
     * Maximum length of strings stored in this array
     */
    private int maximumStringLength = 64;

    /**
     * String pooling map used while loading to avoid duplicating frequently occurring strings. This effectively
     * compresses the input ala LZW.
     */
    @JavaVirtualMachine.KivaKitExcludeFromSizeOf
    private transient CacheMap<String, Integer> pool = new CacheMap<>(Maximum._65536);

    public PackedStringArray(String objectName)
    {
        super(objectName);
    }

    protected PackedStringArray()
    {
    }

    /**
     * @param string The string to add
     * @return An identifier for the string
     */
    public int add(String string)
    {
        assert isInitialized();
        assert string != null;

        if (string.length() > maximumStringLength)
        {
            DEBUG.warning("'$' exceeds maximum string length of $", string, maximumStringLength);
        }
        string = AsciiArt.clip(string, maximumStringLength);

        // Look in pool for an already-stored index
        var pooledIndex = pool.get(string);
        if (pooledIndex != null)
        {
            return pooledIndex;
        }

        var type = type(string);
        int arrayIndex;
        switch (type)
        {
            case ASCII:
                arrayIndex = asciiCharacterIndex;
                for (var i = 0; i < string.length(); i++)
                {
                    asciiCharacters.set(asciiCharacterIndex++, (byte) string.charAt(i));
                }
                asciiCharacters.set(asciiCharacterIndex++, (byte) 0);
                break;

            case UNICODE:
                arrayIndex = characterIndex;
                for (var i = 0; i < string.length(); i++)
                {
                    unicodeCharacters.set(characterIndex++, string.charAt(i));
                }
                unicodeCharacters.set(characterIndex++, (char) 0);
                break;

            default:
                throw new IllegalStateException();
        }
        size++;
        var index = index(type, arrayIndex);
        pool.put(string, index);
        return index;
    }

    /**
     * @return The string for the given identifier (returned by add)
     */
    public String get(int identifier)
    {
        var string = safeGet(identifier);
        if (string == null)
        {
            throw new IllegalStateException();
        }
        return string;
    }

    public void maximumStringLength(int maximumStringLength)
    {
        this.maximumStringLength = maximumStringLength;
    }

    @Override
    public CompressibleCollection.Method onCompress(CompressibleCollection.Method method)
    {
        pool = null;

        asciiCharacters.compress(method);
        unicodeCharacters.compress(method);

        return CompressibleCollection.Method.RESIZE;
    }

    @Override
    public void onInitialize()
    {
        super.onInitialize();
        asciiCharacters = new SplitByteArray(objectName() + ".ascii");
        asciiCharacters.initialSize(Estimate._65536).initialize();

        unicodeCharacters = new SplitCharArray(objectName() + ".unicode");
        unicodeCharacters.initialSize(1024).initialize();
    }

    @Override
    public void read(Kryo kryo, Input input)
    {
        super.read(kryo, input);
        asciiCharacters = kryo.readObject(input, SplitByteArray.class);
        unicodeCharacters = kryo.readObject(input, SplitCharArray.class);
        size = kryo.readObject(input, int.class);
    }

    /**
     * @return The string for the given identifier (returned by add), or null if the identifier was invalid
     */
    public String safeGet(int identifier)
    {
        var type = type(identifier);
        var start = index(identifier);
        switch (type)
        {
            case ASCII:
                return string(asciiCharacters, start);

            case UNICODE:
                return string(unicodeCharacters, start);

            default:
                return null;
        }
    }

    @Override
    public int size()
    {
        return size;
    }

    @Override
    public void write(Kryo kryo, Output output)
    {
        super.write(kryo, output);
        kryo.writeObject(output, asciiCharacters);
        kryo.writeObject(output, unicodeCharacters);
        kryo.writeObject(output, size);
    }

    private int index(int index)
    {
        return index & ~(TYPE_MASK << TYPE_SHIFT);
    }

    private int index(Type type, int index)
    {
        return (type.ordinal() << TYPE_SHIFT) | index;
    }

    private String string(SplitCharArray array, int index)
    {
        var builder = new StringBuilder();
        var i = index;
        while (true)
        {
            var next = array.get(i++);
            if (next == 0)
            {
                break;
            }
            builder.append(next);
        }
        return builder.toString();
    }

    private String string(SplitByteArray array, int index)
    {
        var builder = new StringBuilder();
        var i = index;
        while (true)
        {
            var next = (char) array.get(i++);
            if (next == 0)
            {
                break;
            }
            builder.append(next);
        }
        return builder.toString();
    }

    private Type type(int index)
    {
        return Type.forIdentifier(index >>> TYPE_SHIFT);
    }

    private Type type(String value)
    {
        if (Strings.isAscii(value))
        {
            return Type.ASCII;
        }
        return Type.UNICODE;
    }
}
