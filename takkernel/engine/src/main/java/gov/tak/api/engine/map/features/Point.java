package gov.tak.api.engine.map.features;

/**
 * An x,y,z coordinate geometry
 */
public final class Point extends Geometry
{
    Point(com.atakmap.map.layer.feature.geometry.Point implementation)
    {
        super(implementation);
    }

    com.atakmap.map.layer.feature.geometry.Point getImpl() {
        return (com.atakmap.map.layer.feature.geometry.Point)impl;
    }

    /**
     * Creates a new 2D point.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    public Point(double x, double y)
    {
        this(new com.atakmap.map.layer.feature.geometry.Point(x, y));
    }

    /**
     * Creates a new 3D point.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param z The z-coordinate
     */
    public Point(double x, double y, double z)
    {
        this(new com.atakmap.map.layer.feature.geometry.Point(x, y, z));
    }

    /**
     * The x-coordinate
     */
    public double getX()
    {
        return getImpl().getX();
    }

    /**
     * The y-coordinate
     */
    public double getY()
    {
        return getImpl().getY();
    }

    /**
     * The z-coordinate
     */
    public double getZ()
    {
        return getImpl().getZ();
    }

    /**
     * Set the x,y coordinates. The z-coordinate is unchanged.
     * @param x the x-coordinate
     * @param y the y-coordinate
     */
    public void set(double x, double y)
    {
        getImpl().set(x, y);
    }

    /**
     * Set the x,y,z coordinates.
     * @param x the x-coordinate
     * @param y the y-coordinate
     * @param z the z-coordinate
     */
    public void set(double x, double y, double z)
    {
        getImpl().set(x, y, z);
    }
}
