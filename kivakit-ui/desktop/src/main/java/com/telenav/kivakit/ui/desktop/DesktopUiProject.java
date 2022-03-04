package com.telenav.kivakit.ui.desktop;

import com.telenav.kivakit.core.object.Lazy;
import com.telenav.kivakit.core.project.Project;

/**
 * @author jonathanl (shibo)
 */
public class DesktopUiProject extends Project
{
    private static final Lazy<DesktopUiProject> project = Lazy.of(DesktopUiProject::new);

    public static DesktopUiProject get()
    {
        return project.get();
    }

    protected DesktopUiProject()
    {
    }
}
