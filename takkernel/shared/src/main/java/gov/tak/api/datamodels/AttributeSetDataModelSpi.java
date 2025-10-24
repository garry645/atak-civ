package gov.tak.api.datamodels;

import java.util.Map;

import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.AttributeSetUtils;

/**
 * Primitive (except {@code boolean}) accepting {@code put()}s will remove KVPs if given their Object
 * type's {@code MIN_VALUE}. Object type-accepting {@code put()}s will remove KVPs is provided null.
 */
public final class AttributeSetDataModelSpi implements DataModelSpi<AttributeSet> {
    public static final Integer REMOVE_INT = Integer.MIN_VALUE;
    public static final Byte REMOVE_BYTE = Byte.MIN_VALUE;
    public static final Long REMOVE_LONG = Long.MIN_VALUE;
    public static final Double REMOVE_DOUBLE = Double.MIN_VALUE;

    @Override
    public void put(AttributeSet metadata, String key, int value) {
        if (value == REMOVE_INT) {
            metadata.removeAttribute(key);
            return;
        }
        metadata.setAttribute(key, value);
    }

    @Override
    public void put(AttributeSet metadata, String key, String value) {
        if (value == null) {
            metadata.removeAttribute(key);
            return;
        }
        metadata.setAttribute(key, value);
    }

    @Override
    public void put(AttributeSet metadata, String key, byte value) {
        if (value == REMOVE_BYTE) {
            metadata.removeAttribute(key);
            return;
        }
        metadata.setAttribute(key, value);
    }

    @Override
    public void put(AttributeSet metadata, String key, String[] value) {
        if (value == null) {
            metadata.removeAttribute(key);
            return;
        }
        metadata.setAttribute(key, value);
    }

    @Override
    public void put(AttributeSet metadata, String key, int[] value) {
        if (value == null) {
            metadata.removeAttribute(key);
            return;
        }
        metadata.setAttribute(key, value);
    }

    @Override
    public void put(AttributeSet metadata, String key, boolean value) {
        metadata.setAttribute(key, value);
    }

    @Override
    public void put(AttributeSet metadata, String key, double value) {
        if (value == REMOVE_DOUBLE) {
            metadata.removeAttribute(key);
            return;
        }
        metadata.setAttribute(key, value);
    }

    @Override
    public void put(AttributeSet metadata, String key, long value) {
        if (value == REMOVE_LONG) {
            metadata.removeAttribute(key);
            return;
        }
        metadata.setAttribute(key, value);
    }

    /**
     * Puts all KVPs in {@code value} into a new or existing {@link AttributeSet}, referenced
     * by the provided {@code key}, on the provided {@code metadata} {@link AttributeSet}.
     */
    @Override
    public void put(AttributeSet metadata, String key, Map<String, Object> value) {
        if (value == null) {
            metadata.removeAttribute(key);
            return;
        }
        AttributeSetUtils.putAll(metadata, key, value, true);
    }

    @Override
    public void put(AttributeSet metadata, String key, AttributeSet value) {
        if (value == null) {
            metadata.removeAttribute(key);
            return;
        }
        metadata.setAttribute(key, value);
    }

    @Override
    public int getInt(AttributeSet metadata, String key, int defValue) {
        try {
            return metadata.getIntAttribute(key, defValue);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    @Override
    public long getLong(AttributeSet metadata, String key, long defValue) {
        try {
            return metadata.getLongAttribute(key, defValue);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    @Override
    public double getDouble(AttributeSet metadata, String key, double defValue) {
        try {
            return metadata.getDoubleAttribute(key, defValue);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    @Override
    public String[] getStringArray(AttributeSet metadata, String key, String[] defValue) {
        try {
            return metadata.getStringArrayAttribute(key, defValue);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    @Override
    public int[] getIntArray(AttributeSet metadata, String key, int[] defValue) {
        try {
            return metadata.getIntArrayAttribute(key, defValue);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    @Override
    public double[] getDoubleArray(AttributeSet metadata, String key, double[] defValue) {
        try {
            return metadata.getDoubleArrayAttribute(key, defValue);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    @Override
    public long[] getLongArray(AttributeSet metadata, String key, long[] defValue) {
        try {
            return metadata.getLongArrayAttribute(key, defValue);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    /**
     * @param metadata
     * @param key
     * @param defValue
     * @return {@link AttributeSet}s don't natively support {@link Map}s; the returned {@link Map}
     * will be a new object/reference.
     */
    @Override
    public Map<String, Object> getMap(AttributeSet metadata, String key,
                                      Map<String, Object> defValue) {
        return AttributeSetUtils.getMapAttribute(metadata, key, true);
    }

    @Override
    public AttributeSet getAttributeSet(AttributeSet metadata, String key, AttributeSet defValue) {
        try {
            return metadata.getAttributeSetAttribute(key);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    @Override
    public String getString(AttributeSet metadata, String key, String defValue) {
        try {
            return metadata.getStringAttribute(key, defValue);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    @Override
    public boolean getBoolean(AttributeSet metadata, String key, boolean defValue) {
        try {
            return metadata.getBooleanAttribute(key, defValue);
        } catch (IllegalArgumentException e) {
            return defValue;
        }
    }

    @Override
    public boolean containsKey(AttributeSet metadata, String key) {
        return metadata.containsAttribute(key);
    }
}
