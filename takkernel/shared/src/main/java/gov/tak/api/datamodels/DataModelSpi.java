package gov.tak.api.datamodels;

import com.atakmap.annotations.DeprecatedApi;

import java.util.Map;

import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.AttributeSetUtils;

public interface DataModelSpi<T> {
    void put(T metadata, String key, boolean value);
    void put(T metadata, String key, int value);
    void put(T metadata, String key, long value);
    void put(T metadata, String key, double value);
    void put(T metadata, String key, String value);
    void put(T metadata, String key, byte value);
    void put(T metadata, String key, String[] value);
    void put(T metadata, String key, int[] value);
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    void put(T metadata, String key, Map<String, Object> value);

    /**
     * A non-default implementation will need to be provided starting at 5.7 with the deprecation
     * removal of {@link DataModelSpi#put(Object, String, Map)}.
     */
    default void put(T metadata, String key, AttributeSet value) {
        Map<String, Object> mapVal = value != null ? AttributeSetUtils.toMap(value, true) : null;
        put(metadata, key, mapVal);
    }

    String getString(T metadata, String key, String defValue);
    boolean getBoolean(T metadata, String key, boolean defValue);
    int getInt(T metadata, String key, int defValue);
    long getLong(T metadata, String key, long defValue);
    double getDouble(T metadata, String key, double defValue);
    String[] getStringArray(T metadata, String key, String[] defValue);
    int[] getIntArray(T metadata, String key, int[] defValue);
    double[] getDoubleArray(T metadata, String key, double[] defValue);
    long[] getLongArray(T metadata, String key, long[] defValue);
    @Deprecated
    @DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
    Map<String, Object> getMap(T metadata, String key, Map<String, Object> defValue);

    /**
     * A non-default implementation will need to be provided starting at 5.7 with the deprecation
     * removal of {@link DataModelSpi#getMap(Object, String, Map)}.
     * <p/>
     * If not overridden, the {@link AttributeSet} referenced by the key, as well as any of it's
     * nested {@link AttributeSet}s representing a {@link Map} must have a {@link AttributeSetUtils#MAPPED_TYPE}
     * key with a value of "Map".
     * @return defValue if no {@link AttributeSet} can be found
     */
    default AttributeSet getAttributeSet(T metadata, String key, AttributeSet defValue) {
        if (!containsKey(metadata, key))
            return defValue;

        Map<String, Object> metaMap = getMap(metadata, key, null);
        return (metaMap == null) ? defValue : AttributeSetUtils.fromMap(metaMap, true);
    }

    boolean containsKey(T metadata, String key);
}
