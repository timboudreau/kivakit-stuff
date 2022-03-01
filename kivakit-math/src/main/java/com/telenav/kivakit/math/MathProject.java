package com.telenav.kivakit.math;

import com.telenav.kivakit.core.language.object.Lazy;
import com.telenav.kivakit.coreproject.Project;

/**
 * @author jonathanl (shibo)
 */
public class MathProject extends Project
{
    private static final Lazy<MathProject> project = Lazy.of(MathProject::new);

    public static MathProject get()
    {
        return project.get();
    }

    protected MathProject()
    {
    }
}
