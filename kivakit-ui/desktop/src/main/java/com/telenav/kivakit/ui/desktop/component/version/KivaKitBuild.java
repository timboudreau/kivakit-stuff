package com.telenav.kivakit.ui.desktop.component.version;

import com.telenav.kivakit.core.KivaKit;
import com.telenav.kivakit.ui.desktop.component.KivaKitPanel;

import java.awt.BorderLayout;

import static com.telenav.kivakit.core.project.Project.resolveProject;

/**
 * @author jonathanl (shibo)
 */
public class KivaKitBuild extends KivaKitPanel
{
    public KivaKitBuild()
    {
        setOpaque(false);
        setLayout(new BorderLayout());

        var kivakit = resolveProject(KivaKit.class);
        var text = "KivaKit " + kivakit.projectVersion().withoutRelease() + " " + kivakit.build().name();
        add(newSmallFadedLabel(text), BorderLayout.EAST);
    }
}
