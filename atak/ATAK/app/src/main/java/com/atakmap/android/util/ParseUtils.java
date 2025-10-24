
package com.atakmap.android.util;

import com.atakmap.coremap.locale.LocaleUtil;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.platform.lang.Parsers;

/** @deprecated use {@link Parsers} */
@Deprecated
@DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
public class ParseUtils {

    /**
     * Convert a string to a boolean (exceptions caught)
     *
     * @param value String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default value if conversion failed
     */
    public static boolean parseBoolean(String value, boolean defaultVal) {
        return Parsers.parseBoolean(value, defaultVal);
    }

    /**
     * Convert a string to a double (exceptions caught)
     *
     * @param value String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default value if conversion failed
     */
    public static double parseDouble(String value, double defaultVal) {
        return Parsers.parseDouble(value, defaultVal);
    }

    /**
     * Convert a string to an integer (exceptions caught)
     * @param value String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default if failed
     */
    public static int parseInt(String value, int defaultVal) {
        return Parsers.parseInt(value, defaultVal);
    }

    /**
     * Convert a string to a long (exceptions caught)
     *
     * @param value String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default value if conversion failed
     */
    public static long parseLong(String value, long defaultVal) {
        return Parsers.parseLong(value, defaultVal);
    }

    /**
     * Parse a locale specific representation double. This method is useful when human entered
     * data is being collected from the keyboard where the locale might use different interpretation
     * of periods and commas.
     * @param d the locale specific string representation of a number
     * @param fallback the value to fallback to if parsing fails
     * @return the double
     */
    public static double parseLocale(String d, double fallback) {
        return Parsers.parseDouble(d, LocaleUtil.getCurrent(), fallback);
    }

    /**
     * Parse a locale specific representation integer.  This method is useful when human entered
     * data is being collected from the keyboard where the locale might use different interpretation
     * of periods and commas.
     * @param d the locale specific string representation of a number
     * @param fallback the value to fallback to if parsing fails
     * @return the integer
     */
    public static int parseLocale(String d, int fallback) {
        return Parsers.parseInt(d, LocaleUtil.getCurrent(), fallback);
    }

    /**
     * Parse a locale specific representation long.  This method is useful when human entered
     * data is being collected from the keyboard where the locale might use different interpretation
     * of periods and commas.
     * @param d the locale specific string representation of a number
     * @param fallback the value to fallback to if parsing fails
     * @return the long
     */
    public static long parseLocale(String d, long fallback) {
        return Parsers.parseLong(d, LocaleUtil.getCurrent(), fallback);
    }

    /**
     * Call the object toString() or if the object is null return the fallback.
     * @param o the object to stringify
     * @param fallback the value to fallback to if the object is null
     * @return the corresponding string
     */
    public static String toString(Object o, String fallback) {
        if (o == null)
            return fallback;
        return o.toString();
    }

}
