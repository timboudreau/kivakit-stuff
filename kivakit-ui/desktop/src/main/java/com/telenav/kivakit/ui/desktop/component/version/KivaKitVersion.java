package com.telenav.kivakit.ui.desktop.component.version;

import com.telenav.kivakit.core.KivaKit;
import com.telenav.kivakit.core.version.Version;
import com.telenav.kivakit.ui.desktop.theme.KivaKitTheme;

import javax.swing.JLabel;

import static com.telenav.kivakit.core.project.Project.resolveProject;

/**
 * @author jonathanl (shibo)
 */
public class KivaKitVersion extends JLabel
{
    public KivaKitVersion()
    {
        this(resolveProject(KivaKit.class).projectVersion());
    }

    public KivaKitVersion(Version version)
    {
        super("KivaKit " + version);

        KivaKitTheme.get().applyToComponentLabel(this);
    }
}
