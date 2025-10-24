package com.atakmap.map.gpkg.extensions;

import com.atakmap.coremap.conversions.ConversionFactors;

/**
 * Source: https://unitsofmeasure.org/ucum
 */
final class UnitsOfMeasure {
    enum Length {
        Foot_BR("[ft_br]", ConversionFactors.FEET_TO_METERS),
        Foot_INT("[ft_i]", ConversionFactors.FEET_TO_METERS),
        Foot_US("[ft_us]", ConversionFactors.FEET_TO_METERS),
        Inch_BR("[in_br]", ConversionFactors.FEET_TO_METERS/12d),
        Inch_INT("[in_i]", ConversionFactors.FEET_TO_METERS/12d),
        Inch_US("[in_us]", ConversionFactors.FEET_TO_METERS/12d),
        Yard_BR("[yd_br]", ConversionFactors.FEET_TO_METERS*3d),
        Yard_INT("[yd_i]", ConversionFactors.FEET_TO_METERS*3d),
        Yard_US("[yd_us]", ConversionFactors.FEET_TO_METERS*3d),
        Centimeter("cm", 1d/100d),
        Meter("m", 1d),
        Kilometer("m", 1000d),
        ;

        Length(String code, double toMeters) {
            this.code = code;
            this.toMeters = toMeters;
        }
        public String code;
        public double toMeters;

        public static Length fromCode(String code) {
            if(code != null) {
                for (Length length : values())
                    if (length.code.equals(code))
                        return length;
            }
            return null;
        }

    }
}
