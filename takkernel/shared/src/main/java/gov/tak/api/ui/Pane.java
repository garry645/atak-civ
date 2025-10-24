package gov.tak.api.ui;

import java.util.HashMap;
import java.util.Map;

import gov.tak.api.annotation.NonNull;

/**
 * Opaque representation for a _pane_ added to the UI via {@link IHostUIService}.
 *
 * <P>Original native representation may be retrieved via
 * {@link gov.tak.platform.marshal.MarshalManager} per the associated type used to construct with
 * the {@Link PaneBuilder}.
 *
 * <P>Each {@code IPane} instance may have associated metadata which provide hints to the host
 * application about how the pane should be displayed. All metadata values are optional, and default
 * values will be used if not supplied.
 */
public abstract class Pane {
    /**
     * The relative location of the pane on the host UI, with respect to the main view.
     */
    public enum Location {
        /**
         * The default location for user panes.
         *
         * <P>For Desktop applications, the default location is to the right of the main view.
         * <P>For Mobile applications, the default location is context specific based on the current
         * orientation of the application. When in landscape mode, the default location is to the
         * right of the main view. When in portrait mode, the default location is to the bottom of
         * the main view.
         */
        Default,
        /**
         * The pane should appear to the left of the main view.
         *
         * <P>For Mobile applications, use of this {@code Location} is currently interpreted in the
         * same manner as {@link #Default}.
         */
        Left,
        /**
         * The pane should appear to the right of the main view.
         *
         * <P>For Mobile applications, use of this {@code Location} is currently interpreted in the
         * same manner as {@link #Default}.
         */
        Right,
        /**
         * The pane should appear to the bottom of the main view.
         *
         * <P>For Mobile applications, use of this {@code Location} will place the pane at
         * the bottom of the main view, regardless of orientation.
         */
        Bottom
    }

    /**
     * Specifies the preferred width of the pane, as a ratio of the main view when no other panes
     * are showing.
     *
     * <P>If both {@code PREFERRED_WIDTH_RATIO} and {@link #PREFERRED_WIDTH_PIXELS} are specified,
     * {@link #PREFERRED_WIDTH_PIXELS} will take precedence.
     */
    public static String PREFERRED_WIDTH_RATIO = "PREFERRED_WIDTH_RATIO";
    /**
     * Specifies the preferred height of the pane, as a ratio of the main view when no other panes
     * are showing.
     *
     * <P>If both {@code PREFERRED_HEIGHT_RATIO} and {@link #PREFERRED_HEIGHT_PIXELS} are specified,
     * {@link #PREFERRED_HEIGHT_PIXELS} will take precedence.
     */
    public static String PREFERRED_HEIGHT_RATIO = "PREFERRED_HEIGHT_RATIO";
    /**
     * Specifies the preferred width of the pane, in pixels.
     *
     * <P>If both {@link #PREFERRED_WIDTH_RATIO} and {@code PREFERRED_WIDTH_PIXELS} are specified,
     * {@code PREFERRED_WIDTH_PIXELS} will take precedence.
     */
    public static String PREFERRED_WIDTH_PIXELS = "PREFERRED_WIDTH_PIXELS";
    /**
     * Specifies the preferred height of the pane, as a ratio of the main view when no other panes
     * are showing.
     *
     * <P>If both {@link #PREFERRED_HEIGHT_RATIO} and {@code PREFERRED_HEIGHT_PIXELS} are specified,
     * {@code PREFERRED_HEIGHT_PIXELS} will take precedence.
     */
    public static String PREFERRED_HEIGHT_PIXELS = "PREFERRED_HEIGHT_PIXELS";
    /**
     * Relative location of the pane on the host UI, with the respect to the main view. The corresponding value should
     * be one of the enums defined by {@link Location}
     */
    public static String RELATIVE_LOCATION = "RELATIVE_LOCATION";
    /**
     * The URI for the icon for the pane.
     */
    public static String ICON_URI = "ICON_URI";
    /**
     * The name of the pane, which may be displayed in the UI.
     */
    public static String PANE_NAME = "PANE_NAME";
    /**
     * A hint to _retain_ the pane if the host application's default behavior is to remove when
     * another pane is shown.
     *
     * <P>This hint is currently only applicable for Mobile applications.
     */
    public static String RETAIN = "RETAIN";

    protected Map<String, Object> metadata = new HashMap<>();

    /**
     * Returns <code>true</code> if the pane has the specified metadata value associated.
     *
     * @param key   The metadata key
     *
     * @return  <code>true</code> if the pane has a value for the specified metadata key, <code>false</code> otherwise.
     */
    public final boolean hasMetaValue(@NonNull String key)
    {
        return metadata.containsKey(key);
    }

    /**
     * Returns the metadata value associated with the specified key for this pane.
     *
     * @param key   The metadata key
     *
     * @return  The associated metadata value or <code>null</code> if the metadata value is not defined for this pane.
     */
    public final Object getMetaValue(@NonNull  String key)
    {
        return metadata.get(key);
    }

    /**
     * Returns the metadata value associated with the specified key for this pane as a
     * {@code double} if the associated value is an instance of {@link Number}. If the value is not
     * a {@link Number}, or if the value is not present, the specified default value is returned.
     *
     * @param key           The metadata key
     * @param defaultValue  The default value
     *
     * @return  The associated metadata value or {@code defaultValue} if the metadata value is
     *          either not defined or not a number.
     */
    public final double getMetaDouble(@NonNull String key, double defaultValue)
    {
        return getMetaValue(key, Number.class, Double.valueOf(defaultValue)).doubleValue();
    }

    /**
     * Returns the metadata value associated with the specified key for this pane as a
     * {@code float} if the associated value is an instance of {@link Number}. If the value is not
     * a {@link Number}, or if the value is not present, the specified default value is returned.
     *
     * @param key           The metadata key
     * @param defaultValue  The default value
     *
     * @return  The associated metadata value or {@code defaultValue} if the metadata value is
     *          either not defined or not a number.
     */
    public final float getMetaFloat(@NonNull String key, float defaultValue)
    {
        return getMetaValue(key, Number.class, Float.valueOf(defaultValue)).floatValue();
    }

    /**
     * Returns the metadata value associated with the specified key for this pane as a
     * {@code int} if the associated value is an instance of {@link Number}. If the value is not
     * a {@link Number}, or if the value is not present, the specified default value is returned.
     *
     * @param key           The metadata key
     * @param defaultValue  The default value
     *
     * @return  The associated metadata value or {@code defaultValue} if the metadata value is
     *          either not defined or not a number.
     */
    public final int getMetaInt(@NonNull String key, int defaultValue)
    {
        return getMetaValue(key, Number.class, Integer.valueOf(defaultValue)).intValue();
    }

    /**
     * Returns the metadata value associated with the specified key for this pane as a
     * {@code boolean} if the associated value is an instance of {@link Boolean}. If the value is
     * not a {@link Boolean}, or if the value is not present, the specified default value is
     * returned.
     *
     * @param key           The metadata key
     * @param defaultValue  The default value
     *
     * @return  The associated metadata value or {@code defaultValue} if the metadata value is
     *          either not defined or not a boolean.
     */
    public final boolean getMetaBoolean(@NonNull String key, boolean defaultValue)
    {
        return getMetaValue(key, Boolean.class, Boolean.valueOf(defaultValue)).booleanValue();
    }

    /**
     * Returns the metadata value associated with the specified key for this pane as a
     * {@link String}. If the value is not a {@link String}, or if the value is not present, the
     * specified default value is returned.
     *
     * @param key           The metadata key
     * @param defaultValue  The default value
     *
     * @return  The associated metadata value or {@code defaultValue} if the metadata value is
     *          either not defined or not a string.
     */
    public final String getMetaString(@NonNull String key, String defaultValue)
    {
        return getMetaValue(key, String.class, defaultValue);
    }

    /**
     * Returns the metadata value associated with the specified key for this pane as a value of the
     * specified type. If the value is not present or cannot be assigned from the specified type,
     * the specified default value is returned.
     *
     * @param key           The metadata key
     * @param type          The value type
     * @param defaultValue  The default value
     *
     * @return  The associated metadata value or {@code defaultValue} if the metadata value is
     *          either not defined or cannot be assigned to the specified type.
     */
    public <T> T getMetaValue(@NonNull String key, Class<T> type, T defaultValue)
    {
        final Object value = getMetaValue(key);
        if(value == null || !type.isAssignableFrom(value.getClass()))
        {
            return defaultValue;
        }
        return (T) value;
    }
}
