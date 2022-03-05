package com.telenav.kivakit.ui.desktop.component.panel.stack;

import com.telenav.kivakit.core.time.Duration;

import javax.swing.JPanel;
import java.awt.CardLayout;

/**
 * @author jonathanl (shibo)
 */
public class FlashCardPanel extends JPanel
{
    private final CardLayout cardLayout;

    public FlashCardPanel()
    {
        cardLayout = new CardLayout();
        setLayout(cardLayout);
    }

    public void start(Duration delay)
    {
        delay.repeat(timer -> cardLayout.next(this));
    }
}
