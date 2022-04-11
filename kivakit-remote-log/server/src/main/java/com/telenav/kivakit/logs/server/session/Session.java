package com.telenav.kivakit.logs.server.session;

import com.telenav.kivakit.conversion.core.time.HumanizedLocalDateTimeConverter;
import com.telenav.kivakit.core.logging.LogEntry;
import com.telenav.kivakit.core.messaging.Listener;
import com.telenav.kivakit.core.time.Time;
import com.telenav.kivakit.core.value.count.Bytes;
import com.telenav.kivakit.resource.FileName;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Objects;

/**
 * @author jonathanl (shibo)
 */
public class Session implements Comparable<Session>
{
    private String name;

    private Bytes size;

    private Time started;

    public Session(String name, Time started, Bytes size)
    {
        this.name = name;
        this.started = started;
        this.size = size;
    }

    protected Session()
    {
    }

    public void addAll(List<LogEntry> entries)
    {
        SessionStore.get().addAll(this, entries);
    }

    @Override
    public int compareTo(@NotNull Session that)
    {
        return started.compareTo(that.started);
    }

    public List<LogEntry> entries()
    {
        return SessionStore.get().entries(this);
    }

    @Override
    public boolean equals(Object object)
    {
        if (object instanceof Session)
        {
            Session that = (Session) object;
            return name.equals(that.name) && started.equals(that.started);
        }
        return false;
    }

    public FileName fileName()
    {
        return FileName.dateTime(started.asLocalTime()).prefixedWith(name + "-");
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(name, started);
    }

    public String name()
    {
        return name;
    }

    public Session session(FileName name)
    {
        return null;
    }

    public Bytes size()
    {
        return size;
    }

    @Override
    public String toString()
    {
        return name + " - " + new HumanizedLocalDateTimeConverter(Listener.throwingListener())
                .unconvert(started.asLocalTime());
    }
}
