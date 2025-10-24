package gov.tak.api.engine.map.features;

/**
 * An open or closed string of connected points geometry
 */
public final class LineString extends Geometry
{
    LineString(com.atakmap.map.layer.feature.geometry.LineString implementation)
    {
        super((com.atakmap.map.layer.feature.geometry.LineString)implementation);
    }

    /**
     * Create a line string with a dimension of 2D or 3D
     *
     * @param dimension the demension 2 or 3
     */
    public LineString(int dimension)
    {
        this(new com.atakmap.map.layer.feature.geometry.LineString(dimension));
    }

    com.atakmap.map.layer.feature.geometry.LineString getImpl() {
        return (com.atakmap.map.layer.feature.geometry.LineString)impl;
    }

    /**
     * Adds the specified point to the linestring. If the linestring is 3D, a z-coordinate of '0'
     * will be assumed.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     */
    public void addPoint(double x, double y)
    {
        getImpl().addPoint(x, y);
    }

    /**
     * Adds the specified point to the linestring. If the linestring is 2D, TE_IllegalState is
     * returned.
     *
     * @param x The x-coordinate
     * @param y The y-coordinate
     * @param z The z-coordinate
     */
    public void addPoint(double x, double y, double z)
    {
        getImpl().addPoint(x, y, z);
    }

    /**
     * Adds the specified points to the linestring. If 'ptsDim' is '2' and the linestring is 2D,
     * the z-coordinate for all points will be assumed to be '0'.
     *
     * @param pts       The points, interleaved by component
     * @param off       The offset into pts to start at
     * @param numPts    The number of points
     * @param ptsDim    The dimension of the points
     */
    public void addPoints(double[] pts, int off, int numPts, int ptsDim)
    {
        getImpl().addPoints(pts, off, numPts, ptsDim);
    }

    /**
     * Returns the number of points in the linestring.
     */
    public int getNumPoints()
    {
        return getImpl().getNumPoints();
    }

    /**
     * Returns the x-coordinate of the specified point.
     *
     * @param i     The point index
     */
    public double getX(int i)
    {
        return getImpl().getX(i);
    }

    /**
     * Returns the y-coordinate of the specified point.
     *
     * @param i     The point index
     */
    public double getY(int i)
    {
        return getImpl().getY(i);
    }

    /**
     * Returns the z-coordinate of the specified point.
     *
     * @param i     The point index
     */
    public double getZ(int i)
    {
        return getImpl().getZ(i);
    }

    /**
     * Returns the specified point. The supplied Point2 will have its dimension reset to the
     * dimension of this linestring.
     *
     * @param point Returns the point
     * @param i     The point index
     */
    public void get(Point point, int i)
    {
        point.setDimension(this.getDimension());
        if (this.getDimension() == 3)
            point.set(this.getX(i), this.getY(i), this.getZ(i));
        else
            point.set(this.getX(i), this.getY(i));
    }

    /**
     * Sets the x-coordinate of the specified point.
     *
     * @param i The point index
     * @param x The x-coordinate
     */
    public void setX(int i, double x)
    {
        getImpl().setX(i, x);
    }

    /**
     * Sets the y-coordinate of the specified point.
     *
     * @param i The point index
     * @param y The y-coordinate
     */
    public void setY(int i, double y)
    {
        getImpl().setY(i, y);
    }

    /**
     * Sets the z-coordinate of the specified point.
     *
     * @param i The point index
     * @param z The z-coordinate
     *
     * @return  TE_Ok on success; various codes on failure.
     */
    public void setZ(int i, double z)
    {
        getImpl().setZ(i, z);
    }

    /**
     * Returns 'true' if the linestring is closed (first point equals end point), 'false' otherwise.
     */
    public boolean isClosed()
    {
        return getImpl().isClosed();
    }
}
