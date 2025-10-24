package gov.tak.platform.lang;

import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.text.ParseException;
import java.util.Locale;

public final class Parsers {
    private Parsers() {}

    /**
     * Convert a string to a boolean (exceptions caught)
     *
     * @param value String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default value if conversion failed
     */
    public static boolean parseBoolean(String value, boolean defaultVal) {
        if (value == null)
            return defaultVal;

        try {
            return Boolean.parseBoolean(value.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /**
     * Convert a string to a double (exceptions caught)
     *
     * @param value String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default value if conversion failed
     */
    public static double parseDouble(String value, double defaultVal) {
        if (value == null)
            return defaultVal;

        try {
            return Double.parseDouble(value.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /**
     * Convert a string to an integer (exceptions caught)
     * @param value String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default if failed
     */
    public static int parseInt(String value, int defaultVal) {
        if (value == null)
            return defaultVal;

        try {
            return Integer.parseInt(value.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /**
     * Convert a string to a long (exceptions caught)
     *
     * @param value String value
     * @param defaultVal Default value if conversion fails
     * @return Converted value or default value if conversion failed
     */
    public static long parseLong(String value, long defaultVal) {
        if (value == null)
            return defaultVal;

        try {
            return Long.parseLong(value.trim());
        } catch (Exception e) {
            return defaultVal;
        }
    }

    /**
     * Parse a locale specific representation double. This method is useful when human entered
     * data is being collected from the keyboard where the locale might use different interpretation
     * of periods and commas.
     * @param d the locale specific string representation of a number
     * @param fallback the value to fallback to if parsing fails
     * @return the double
     */
    public static double parseDouble(String d, Locale locale, double fallback) {
        final Number result = parseLocale(d, locale);
        return (result != null) ? result.doubleValue() : fallback;
    }

    /**
     * Parse a locale specific representation integer.  This method is useful when human entered
     * data is being collected from the keyboard where the locale might use different interpretation
     * of periods and commas.
     * @param d the locale specific string representation of a number
     * @param fallback the value to fallback to if parsing fails
     * @return the integer
     */
    public static int parseInt(String d, Locale locale, int fallback) {
        final Number result = parseLocale(d, locale);
        return (result != null) ? result.intValue() : fallback;
    }

    /**
     * Parse a locale specific representation long.  This method is useful when human entered
     * data is being collected from the keyboard where the locale might use different interpretation
     * of periods and commas.
     * @param d the locale specific string representation of a number
     * @param fallback the value to fallback to if parsing fails
     * @return the long
     */
    public static long parseLong(String d, Locale locale, long fallback) {
        final Number result = parseLocale(d, locale);
        return (result != null) ? result.longValue() : fallback;
    }

    public static Number parseLocale(String d, Locale locale) {
        final NumberFormat f = NumberFormat.getInstance(locale);
        if (f instanceof DecimalFormat) {
            try {
                if (d != null) {
                    return f.parse(d.trim());
                }
            } catch (ParseException ignored) {}
        }
        return null;
    }
}
