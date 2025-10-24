package gov.tak.api.commons.graphics;

public final class ColorFilter
{
    final int color;
    final ColorBlendMode mode;

    public ColorFilter(int color, ColorBlendMode mode)
    {
        this.color = color;
        this.mode = mode;
    }

    public int getColor()
    {
        return this.color;
    }

    public ColorBlendMode getBlendMode()
    {
        return this.mode;
    }
}
