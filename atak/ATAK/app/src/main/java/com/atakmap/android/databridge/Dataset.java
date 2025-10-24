
package com.atakmap.android.databridge;

import androidx.annotation.NonNull;

import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.Arrays;

import gov.tak.api.util.AttributeSet;

public class Dataset {

    private final String uid;
    private boolean sealed = false;
    private final AttributeSet attributeSet = new AttributeSet();

    /**
     * Construct a metadata set to convey information to subscribers.
     * @param uid the uid of the dataset provider
     */
    public Dataset(String uid) {
        this.uid = uid;
    }

    /**
     * Seal the metadata so that subscribers cannot modify it.   This should be
     * called before any listeners are notified.
     */
    public void seal() {
        sealed = true;
    }

    /**
     * Return the uid of the dataset provider
     * @return the uid of the dataset provider
     */
    public String getUID() {
        return uid;
    }

    /**
     * Sets the boolean value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the boolean value
     */
    public void set(String key, boolean value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the int value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the int value
     */
    public void set(String key, int value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the long value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the long value
     */
    public void set(String key, long value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the double value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the double value
     */
    public void set(String key, double value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the String value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the String value
     */
    public void set(String key, String value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the byte[] value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the byte[] value
     */
    public void set(String key, byte[] value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the int[] value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the int[] value
     */
    public void set(String key, int[] value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the long[] value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the long[] value
     */
    public void set(String key, long[] value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the double[] value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the double[] value
     */
    public void set(String key, double[] value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the String[] value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the String[] value
     */
    public void set(String key, String[] value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the byte[][] value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the byte[][] value
     */
    public void set(String key, byte[][] value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Sets the GeoPoint value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the GeoPoint value
     */
    public void set(String key, GeoPoint value) {
        if (!sealed)
            attributeSet.setAttribute(key, value.toStringRepresentation());
    }

    /**
     * Sets the GeoBounds value for a specific key.  This call is ignored as soon as the
     * dataset is sealed
     * @param key the key
     * @param value the GeoBounds value
     */
    public void set(String key, GeoBounds value) {
        if (!sealed)
            attributeSet.setAttribute(key,
                    new double[] {
                            value.getSouth(), value.getWest(),
                            value.getMinAltitude(),
                            value.getNorth(), value.getEast(),
                            value.getMaxAltitude()
                    });
    }

    public void set(String key, AttributeSet value) {
        if (!sealed)
            attributeSet.setAttribute(key, value);
    }

    /**
     * Returns true if the dataset contains a specific key.
     * @param key the key
     * @return true if the key exists within the dataset
     */
    public boolean contains(String key) {
        return attributeSet.containsAttribute(key);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public boolean get(String key, boolean defaultValue) {
        return attributeSet.getBooleanAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public int get(String key, int defaultValue) {
        return attributeSet.getIntAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public long get(String key, long defaultValue) {
        return attributeSet.getLongAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public double get(String key, double defaultValue) {
        return attributeSet.getDoubleAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public String get(String key, String defaultValue) {
        return attributeSet.getStringAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public byte[] get(String key, byte[] defaultValue) {
        return attributeSet.getBinaryAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public int[] get(String key, int[] defaultValue) {
        return attributeSet.getIntArrayAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public long[] get(String key, long[] defaultValue) {
        return attributeSet.getLongArrayAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public double[] get(String key, double[] defaultValue) {
        return attributeSet.getDoubleArrayAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public String[] get(String key, String[] defaultValue) {
        return attributeSet.getStringArrayAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public byte[][] get(String key, byte[][] defaultValue) {
        return attributeSet.getBinaryArrayAttribute(key, defaultValue);
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public GeoPoint get(String key, GeoPoint defaultValue) {
        return GeoPoint.parseGeoPoint(attributeSet.getStringAttribute(key,
                defaultValue.toStringRepresentation()));
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @param defaultValue the default value
     * @return the value or the default value if no value exists
     */
    public GeoBounds get(String key, GeoBounds defaultValue) {
        double[] bounds = attributeSet.getDoubleArrayAttribute(key,
                new double[] {
                        defaultValue.getSouth(), defaultValue.getWest(),
                        defaultValue.getMinAltitude(),
                        defaultValue.getNorth(), defaultValue.getEast(),
                        defaultValue.getMaxAltitude()
                });
        if (bounds != null && bounds.length == 6) {
            return new GeoBounds(bounds[0], bounds[1], bounds[2], bounds[3],
                    bounds[4], bounds[5]);
        }
        return defaultValue;
    }

    /**
     * Gets a value for a given key or the default key if no value is set
     * @param key the key
     * @return the value or the default value if no value exists
     */
    public AttributeSet getAttributeSet(String key) {
        return attributeSet.getAttributeSetAttribute(key);
    }

    @Override
    @NonNull
    public String toString() {
        return "Dataset{" +
                "uid='" + uid + '\'' +
                ", attributes= {" + toString(attributeSet) + "}";

    }

    private String toString(AttributeSet attributeSet) {
        StringBuilder sb = new StringBuilder();
        for (String key : attributeSet.getAttributeNames()) {
            if (sb.length() > 0)
                sb.append(",");
            final AttributeSet.AttributeType type = attributeSet
                    .getAttributeType(key);
            if (type == AttributeSet.AttributeType.STRING)
                sb.append(key + "=" + attributeSet.getStringAttribute(key));
            else if (type == AttributeSet.AttributeType.STRING_ARRAY)
                sb.append(key + "=" + toTruncatedString(
                        attributeSet.getStringArrayAttribute(key), 6));
            else if (type == AttributeSet.AttributeType.DOUBLE)
                sb.append(key + "=" + attributeSet.getDoubleAttribute(key));
            else if (type == AttributeSet.AttributeType.DOUBLE_ARRAY)
                sb.append(key + "=" + toTruncatedString(
                        attributeSet.getDoubleArrayAttribute(key), 6));
            else if (type == AttributeSet.AttributeType.INTEGER)
                sb.append(key + "=" + attributeSet.getIntAttribute(key));
            else if (type == AttributeSet.AttributeType.INTEGER_ARRAY)
                sb.append(key + "=" + toTruncatedString(
                        attributeSet.getIntArrayAttribute(key), 6));
            else if (type == AttributeSet.AttributeType.LONG)
                sb.append(key + "=" + attributeSet.getLongAttribute(key));
            else if (type == AttributeSet.AttributeType.LONG_ARRAY)
                sb.append(key + "=" + toTruncatedString(
                        attributeSet.getLongArrayAttribute(key), 6));
            else if (type == AttributeSet.AttributeType.BLOB)
                sb.append(key + "= [binary data]");
            else if (type == AttributeSet.AttributeType.BLOB_ARRAY)
                sb.append(key + "= [array of binary data]");
        }

        return sb.toString();

    }

    private String toTruncatedString(Object o, int val) {
        String s = "[]";
        if (o == null) {
            return s;
        } else if (o instanceof int[]) {
            s = Arrays.toString(Arrays.copyOf((int[]) o, val));
        } else if (o instanceof long[]) {
            s = Arrays.toString(Arrays.copyOf((long[]) o, val));
        } else if (o instanceof double[]) {
            s = Arrays.toString(Arrays.copyOf((double[]) o, val));
        } else if (o instanceof String[]) {
            s = Arrays.toString(Arrays.copyOf((String[]) o, val));

        }

        return s.substring(0, s.length() - 1) + ", ...]";

    }

}
