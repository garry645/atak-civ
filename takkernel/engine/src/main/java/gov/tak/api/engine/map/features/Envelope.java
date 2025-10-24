package gov.tak.api.engine.map.features;

import com.atakmap.math.MathUtils;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public final class Envelope
{

    public double minX;
    public double minY;
    public double minZ;

    public double maxX;
    public double maxY;
    public double maxZ;

    public Envelope()
    {
        this(Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN, Double.NaN);
    }

    public Envelope(double minX, double minY, double minZ, double maxX, double maxY, double maxZ)
    {
        this.minX = minX;
        this.minY = minY;
        this.minZ = minZ;

        this.maxX = maxX;
        this.maxY = maxY;
        this.maxZ = maxZ;
    }

    public Envelope(Envelope other)
    {
        this(other.minX, other.minY, other.minZ, other.maxX, other.maxY, other.maxZ);
    }

    @Override
    public boolean equals(Object o)
    {
        if (!(o instanceof Envelope))
            return false;
        final Envelope other = (Envelope) o;
        return MathUtils.equals(this.minX, other.minX) &&
                MathUtils.equals(this.minY, other.minY) &&
                MathUtils.equals(this.minZ, other.minZ) &&
                MathUtils.equals(this.maxX, other.maxX) &&
                MathUtils.equals(this.maxY, other.maxY) &&
                MathUtils.equals(this.maxZ, other.maxZ);

    }

    @Override
    public String toString()
    {
        return "Envelope {minX=" + minX + ",minY=" + minY + ",minZ=" + minZ + ",maxX=" + maxX + ",maxY=" + maxY + ",maxZ=" + maxZ + "}";
    }
}
