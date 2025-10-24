package gov.tak.api.cot;

import com.atakmap.coremap.log.Log;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;

/**
 * Utility methods for parsing CoT XML content.
 *
 * @since 6.0.0
 */
public class CotParseUtils
{
    private static final String TAG = "CotParseUtils";

    /**
     * Get a {@link CoordinatedTime} value from the given element attributes.  If the value is missing/{@code null} or fails to
     * parse as a {@link CoordinatedTime}, then return a new, {@link CoordinatedTime}.
     *
     * @param attrs Attributes from which to obtain the value
     * @param name  Name of attribute to get
     * @param msg   Message to use in log if the attribute is missing/{@code null} or fails to parse
     * @return attribute value as a CoordinatedTime
     * @see CoordinatedTime#CoordinatedTime()
     * @see CotFormatting#cotTimeToCoordinatedTime(java.lang.String)
     */
    @NonNull
    public static CoordinatedTime timeOrDefault(@NonNull Attributes attrs, @NonNull String name, @NonNull String msg)
    {
        try
        {
            return CotFormatting.cotTimeToCoordinatedTime(attrs.getValue(name));
        } catch (Exception e)
        {
            Log.e(TAG, msg);
            return new CoordinatedTime();
        }
    }

    /**
     * Get a String value from the given element attributes.  If the value is {@code null} (absent), throw an exception.
     *
     * @param attrs Attributes from which to obtain the value
     * @param name  Name of attribute to get
     * @param msg   Message to use in exception if the attribute is missing/{@code null}
     * @return attribute value
     * @throws SAXException If the attribute is missing/{@code null}
     */
    public static String getStringOrThrow(@NonNull Attributes attrs, @NonNull String name, @NonNull String msg)
            throws SAXException
    {
        final String value = attrs.getValue(name);
        if (value == null) throw new SAXException(msg);
        return value;
    }

    /**
     * Get a String value from the given element attributes.  If the value is missing/{@code null},
     * then return the given fallback value.
     *
     * @param attrs    Attributes from which to obtain the value
     * @param name     Name of attribute to get
     * @param fallback Value to return if the attribute is missing
     * @return attribute value
     */
    public static String getString(@NonNull Attributes attrs, @NonNull String name, @Nullable String fallback)
    {
        final String value = attrs.getValue(name);
        return value == null ? fallback : value;
    }

    /**
     * Get a double value from the given element attributes.  If the value is {@code null} (absent), throw an exception.
     *
     * @param attrs Attributes from which to obtain the value
     * @param name  Name of attribute to get
     * @param msg   Message to use in exception if the attribute is missing/{@code null}
     * @return attribute value as a double
     * @throws SAXException If the attribute is missing/{@code null}
     */
    public static double getDoubleOrThrow(@NonNull Attributes attrs, @NonNull String name, @NonNull String msg)
            throws SAXException
    {
        try
        {
            return parseDouble(attrs.getValue(name));
        } catch (Exception e)
        {
            throw new SAXException(msg);
        }
    }

    /**
     * Get a double value from the given element attributes.  If the value is missing/{@code null} or fails to parse as a double,
     * then return the given fallback value.
     *
     * @param attrs    Attributes from which to obtain the value
     * @param name     Name of attribute to get
     * @param fallback Value to return if the attribute is missing or fails to parse
     * @return attribute value
     */
    public static double getDouble(@NonNull Attributes attrs, @NonNull String name, double fallback)
    {
        String value = attrs.getValue(name);
        return parseDoubleWithFallback(value, fallback);
    }

    /**
     * Attempts to get a double representation of a "value" attribute for the provided detail name.
     *
     * @param event      The CoT event to search
     * @param detailName The name of the detail for which to get the "value" attribute
     * @param fallback   The default value to return if a valid value is not present
     * @return The double value or {@code fallback} if a valid value was not present.
     */
    public static double getDetailValueAsDouble(CotEvent event, String detailName, double fallback)
    {
        final String doubleString = getDetailValue(event, detailName, null);
        return parseDoubleWithFallback(doubleString, fallback);
    }

    /**
     * Attempts to get an int representation of a "value" attribute for the provided detail name.
     *
     * @param event      The CoT event to search
     * @param detailName The name of the detail for which to get the "value" attribute
     * @param fallback   The default value to return if a valid value is not present
     * @return The int value or {@code fallback} if a valid value was not present.
     */
    public static int getDetailValueAsInt(CotEvent event, String detailName, int fallback)
    {
        final String intString = getDetailValue(event, detailName, null);
        return parseIntWithFallback(intString, fallback);
    }

    /**
     * Attempts to get a string representation of a "value" attribute for the provided detail name.
     *
     * @param event      The CoT event to search
     * @param detailName The name of the detail for which to get the "value" attribute
     * @param fallback   The default value to return if a valid value is not present
     * @return The value or {@code fallback} if detail or value attribute wasn't present
     */
    public static String getDetailValue(CotEvent event, String detailName, @Nullable String fallback)
    {
        CotDetail detail = event.findDetail(detailName);
        if (detail == null) return fallback;

        final String attribute = detail.getAttribute(DetailConstants.ATTR_VALUE);
        return attribute != null ? attribute : fallback;
    }

    /**
     * Parse an {@code int} value from the given {@code String}, using a fallback value
     * if the value cannot be interpreted.
     *
     * @param value    String representing some {@code int} value.
     * @param fallback Value to use if the string representation is not a valid {@code int}.
     * @return {@code double} value from the string representation.
     */
    private static int parseIntWithFallback(@Nullable String value, int fallback)
    {
        if (value == null) return fallback;
        try
        {
            return Integer.parseInt(value.trim());
        } catch (Exception e)
        {
            return fallback;
        }
    }

    /**
     * Parse a {@code double} value from the given {@code String}, using a fallback value
     * if the value cannot be interpreted.
     *
     * @param value    String representing some {@code double} value.
     * @param fallback Value to use if the string representation is not a valid {@code double}.
     * @return {@code double} value from the string representation.
     */
    private static double parseDoubleWithFallback(@Nullable String value, double fallback)
    {
        try
        {
            if (value == null) return fallback;
            return parseDouble(value.trim());
        } catch (Exception e)
        {
            return fallback;
        }
    }

    /**
     * Parse a {@code double} value from the given {@code String}.
     *
     * @param value String representing some {@code double} value.
     * @return {@code double} value from the string representation.
     * @throws NumberFormatException If the {@code double} string is malformed.
     */
    private static double parseDouble(@NonNull String value)
    {
        // Strip leading redundant '+' if it exists.
        if (value.length() > 1 && value.charAt(0) == '+') value = value.substring(1);
        
        // The standard 'parseDouble' implementation has edge-case support, but only if they
        // match the exact case such as 'NaN' and will not match lower-cased 'nan' or 'infinity'.
        if (value.equalsIgnoreCase("nan")) return Double.NaN;
        if (value.equalsIgnoreCase("infinity")) return Double.POSITIVE_INFINITY;
        if (value.equalsIgnoreCase("-infinity")) return Double.NEGATIVE_INFINITY;
        return Double.parseDouble(value);
    }

    private CotParseUtils()
    {
        /* no instances */
    }
}
