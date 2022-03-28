package com.telenav.kivakit.ui.desktop.graphics.image;

import com.telenav.kivakit.resource.Resource;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.IOException;

/**
 * @author jonathanl (shibo)
 */
public class ImageResource
{
    public static ImageResource of(Resource resource)
    {
        return new ImageResource(resource);
    }

    private final Resource resource;

    public ImageResource(Resource resource)
    {
        this.resource = resource;
    }

    public Image image()
    {
        try (var input = resource.openForReading())
        {
            return ImageIO.read(input);
        }
        catch (IOException ignored)
        {
            return null;
        }
    }

    public Image image(int width, int height)
    {
        return image().getScaledInstance(width, height, Image.SCALE_SMOOTH);
    }
}
