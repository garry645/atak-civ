package com.atakmap.coremap.maps.coords;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.AngleUtilities;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.platform.marshal.MarshalManager;

/**
 *
 */
public enum NorthReference
{

    TRUE(0, "T", "True North"),
    MAGNETIC(1, "M", "Magnetic North"),
    GRID(2, "G", "Grid North"),
    ;

    private final int _value;
    private final String _abbrev;
    private final String _name;

    NorthReference(final int value, final String abbrev,
                   final String name)
    {
        _value = value;
        _abbrev = abbrev;
        _name = name;
    }

    /**
     * Gets the integer representation of the NorthReference.
     */
    public int getValue()
    {
        return _value;
    }

    /**
     * Gets a human readable short string representation of the NorthReference
     */
    public String getAbbrev()
    {
        return _abbrev;
    }

    /**
     * Gets the human readable full string representation of the NorthReference
     */
    public String getName()
    {
        return _name;
    }

    @NonNull
    @Override
    public String toString()
    {
        return _name;
    }

    /**
     * @param value the integer representation of the north reference
     * @return the north reference
     */
    public static NorthReference findFromValue(final int value)
    {
        for (NorthReference nr : NorthReference.values())
        {
            if (nr._value == value)
            {
                return nr;
            }
        }
        return null;
    }

    /**
     * @param abbrev the abbreviation for the north reference
     * @return the north reference based on the abbreviation
     */
    public static NorthReference findFromAbbrev(final String abbrev)
    {
        for (NorthReference nr : NorthReference.values())
        {
            if (nr._abbrev.equalsIgnoreCase(abbrev))
            {
                return nr;
            }
        }
        return null;
    }

    public static double convert(double deg, GeoPoint point, double range,
                                 NorthReference from, NorthReference to)
    {
        gov.tak.api.engine.map.coords.IGeoPoint pointM = MarshalManager.marshal(point, GeoPoint.class, gov.tak.api.engine.map.coords.IGeoPoint.class);
        return convert(deg, pointM, range, from, to);
    }

    /**
     * Convert from degrees in one north reference to another
     *
     * @param deg   Degrees in input north reference
     * @param point Reference point (used in mag and grid calc)
     * @param range Range in meters (used in grid calc)
     * @param from  Input north reference
     * @param to    Output north reference
     * @return Degrees in output north reference
     */
    public static double convert(double deg, IGeoPoint point, double range,
                                 NorthReference from, NorthReference to)
    {

        // Nothing to do
        if (from == to)
            return deg;

        // First convert degrees to true north
        double trueDeg = deg;
        switch (from)
        {
            case MAGNETIC:
                trueDeg = GeoCalculations.convertFromMagneticToTrue(point, deg);
                break;
            case GRID:
                trueDeg += GeoCalculations.computeGridConvergence(point, trueDeg,
                        range);
                break;
        }

        // Then convert to output reference
        switch (to)
        {
            case MAGNETIC:
                trueDeg = GeoCalculations.convertFromTrueToMagnetic(point,
                        trueDeg);
                break;
            case GRID:
                trueDeg -= GeoCalculations.computeGridConvergence(point, trueDeg,
                        range);
                break;
        }
        return AngleUtilities.wrapDeg(trueDeg);
    }

    public static String format(double trueDeg, GeoPoint point, double range,
                                Angle units, NorthReference northRef, int decimalPoints)
    {
        gov.tak.api.engine.map.coords.IGeoPoint pointM = MarshalManager.marshal(point, GeoPoint.class, gov.tak.api.engine.map.coords.IGeoPoint.class);
        return format(trueDeg, pointM, range, units, northRef, decimalPoints);
    }

    /**
     * Format degrees to a specific north reference
     *
     * @param trueDeg       Degrees value (true north)
     * @param point         Reference point (used in mag and grid calc)
     * @param range         Range in meters (used in grid calc)
     * @param units         Units for output format
     * @param northRef      North reference
     * @param decimalPoints Number of decimal points in output format
     * @return Formatted angle string
     */
    public static String format(double trueDeg, IGeoPoint point, double range,
                                Angle units, NorthReference northRef, int decimalPoints)
    {
        double deg = convert(trueDeg, point, range, NorthReference.TRUE,
                northRef);
        return AngleUtilities.format(deg, units, decimalPoints)
                + northRef.getAbbrev();
    }

    public static String format(double trueDeg, GeoPoint point, Angle units,
                                NorthReference northRef, int decimalPoints)
    {
        gov.tak.api.engine.map.coords.IGeoPoint pointM = MarshalManager.marshal(point, GeoPoint.class, gov.tak.api.engine.map.coords.IGeoPoint.class);
        return format(trueDeg, pointM, units, northRef, decimalPoints);
    }

    /**
     * Format degrees to a specific north reference
     *
     * @param trueDeg       Degrees value (true north)
     * @param point         Reference point (used in mag and grid calc)
     * @param units         Units for output format
     * @param northRef      North reference
     * @param decimalPoints Number of decimal points in output format
     * @return Formatted angle string
     */
    public static String format(double trueDeg, IGeoPoint point, Angle units,
                                NorthReference northRef, int decimalPoints)
    {
        return format(trueDeg, point, 1, units, northRef, decimalPoints);
    }

    /**
     * Gets the enum literal corresponding to the provided ordinal.
     *
     * @param ordinal The ordinal of the enum
     * @return The enum literal for the ordinal, or null if not found.
     */
    @Nullable
    public static NorthReference fromOrdinal(int ordinal)
    {
        NorthReference[] values = values();
        if (ordinal < 0 || ordinal >= values.length)
            return null;

        return values[ordinal];
    }
}
