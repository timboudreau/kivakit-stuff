package com.telenav.kivakit.logs.client.view.panels.table;

import com.telenav.kivakit.core.collections.list.StringList;
import com.telenav.kivakit.core.value.count.Maximum;

import javax.swing.RowFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import static com.telenav.kivakit.core.ensure.Ensure.ensure;
import static com.telenav.kivakit.logs.client.view.panels.search.SearchPanel.ALL_CONTEXTS;
import static com.telenav.kivakit.logs.client.view.panels.search.SearchPanel.ALL_THREADS;
import static com.telenav.kivakit.logs.client.view.panels.search.SearchPanel.ALL_TYPES;
import static com.telenav.kivakit.logs.client.view.panels.table.TableModel.COLUMNS;
import static com.telenav.kivakit.logs.client.view.panels.table.TableModel.CONTEXT;
import static com.telenav.kivakit.logs.client.view.panels.table.TableModel.MESSAGE_TYPE;
import static com.telenav.kivakit.logs.client.view.panels.table.TableModel.THREAD;

/**
 * @author jonathanl (shibo)
 */
class TableFilter extends RowFilter<Object, Object>
{
    private final List<Matcher> matchers = new ArrayList<>();

    private final List<String> strings = new ArrayList<>();

    private final List<Boolean> negate = new ArrayList<>();

    private final String thread;

    private final String context;

    private final String messageType;

    @SuppressWarnings("ConstantConditions")
    TableFilter(String search, String thread, String context, String messageType)
    {
        this.thread = thread;
        this.context = context;
        this.messageType = messageType;
        ensure(search != null);

        var quotedMatcher = Pattern.compile("\"(?<quoted>[^\"]+?)\"").matcher(search);
        var terms = new StringList(Maximum._100);
        while (quotedMatcher.find())
        {
            terms.add(quotedMatcher.group("quoted"));
        }
        quotedMatcher.reset();
        search = quotedMatcher.replaceAll("");
        terms.addAll(search.split("\\s+"));

        for (var term : terms)
        {
            // If the term is not a regular expression,
            if (term.matches("[\\w+._!]+"))
            {
                // just add it as a string
                strings.add(term.toLowerCase());
            }
            else
            {
                try
                {
                    // Look for negation
                    var negated = false;
                    if (term.startsWith("~"))
                    {
                        negated = true;
                        term = term.substring(1);
                    }
                    negate.add(negated);

                    // Build matcher
                    var matcher = Pattern.compile(term
                                    .replaceAll("\\.", "\\\\.")
                                    .replaceAll("\\[", "\\\\[")
                                    .replaceAll("]", "\\\\]")
                                    .replaceAll("!", "\\\\!")
                                    .replaceAll("\\*", ".*"), Pattern.CASE_INSENSITIVE)
                            .matcher("");
                    matchers.add(matcher);
                }
                catch (PatternSyntaxException ignored)
                {
                }
            }
        }
    }

    @Override
    public boolean include(Entry<?, ?> entry)
    {
        if (strings.isEmpty() && matchers.isEmpty())
        {
            return false;
        }

        if (thread != null && !ALL_THREADS.equals(thread) && !entry.getValue(THREAD).toString().equals(thread))
        {
            return false;
        }

        if (context != null && !ALL_CONTEXTS.equals(context) && !entry.getValue(CONTEXT).toString().equals(context))
        {
            return false;
        }

        if (!ALL_TYPES.equals(messageType))
        {
            var type = entry.getValue(MESSAGE_TYPE).toString();
            var include = false;
            for (var current : new String[] { "Problem", "Warning", "Glitch" })
            {
                if (messageType.contains(current) && type.equals(current))
                {
                    include = true;
                    break;
                }
            }
            if (!include && !type.equals(messageType))
            {
                return false;
            }
        }

        var found = false;

        for (var string : strings)
        {
            found = false;
            for (var column = 0; column < COLUMNS; column++)
            {
                var value = entry.getValue(column).toString().toLowerCase();
                if (value.contains(string))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                return false;
            }
        }

        var index = 0;
        for (var matcher : matchers)
        {
            found = false;
            for (var column = 0; column < COLUMNS; column++)
            {
                matcher.reset(entry.getValue(column).toString());
                if (matcher.find() != negate.get(index))
                {
                    found = true;
                    break;
                }
            }
            if (!found)
            {
                return false;
            }
            index++;
        }

        return found;
    }
}
