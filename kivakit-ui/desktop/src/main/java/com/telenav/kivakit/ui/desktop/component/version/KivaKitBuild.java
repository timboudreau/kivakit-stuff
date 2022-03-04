package com.telenav.kivakit.ui.desktop.component.version;

import com.telenav.kivakit.core.KivaKit;
import com.telenav.kivakit.ui.desktop.component.KivaKitPanel;

import java.awt.BorderLayout;

/**
 * @author jonathanl (shibo)
 */
public class KivaKitBuild extends KivaKitPanel
{
    public KivaKitBuild()
    {
        setOpaque(false);
        setLayout(new BorderLayout());

        var kivakit = KivaKit.get();
        var text = "KivaKit " + kivakit.projectVersion().withoutRelease() + " " + kivakit.build().name();
        add(newSmallFadedLabel(text), BorderLayout.EAST);
    }
}
