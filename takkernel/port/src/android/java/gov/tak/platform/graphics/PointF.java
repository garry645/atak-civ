
package gov.tak.platform.graphics;

public class PointF extends android.graphics.PointF
{
    public PointF()
    {
        super();
    }

    public PointF(float x, float y)
    {
        super(x, y);
    }

    public PointF(Point p)
    {
        super(p);
    }

    @Override
    public boolean equals(Object o) {
        if(!(o instanceof PointF))
            return false;
        final PointF other = (PointF)o;
        return (this.x == other.x) && (this.y == other.y);
    }
}
