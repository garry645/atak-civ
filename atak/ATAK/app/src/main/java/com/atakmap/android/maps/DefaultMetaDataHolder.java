
package com.atakmap.android.maps;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.SparseArray;

import com.atakmap.coremap.log.Log;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.AttributeSetUtils;
import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;

public class DefaultMetaDataHolder implements MetaDataHolder3 {

    private static final String TAG = "DefaultMetaDataHolder";

    public DefaultMetaDataHolder() {
        this(new ConcurrentHashMap<>());
    }

    public DefaultMetaDataHolder(final Map<String, Object> bundle) {
        _serializableData = bundle;
        _attrs = new AttributeSet();
        AttributeSetUtils.putAll(_attrs, bundle, true);
    }

    static {
        MarshalManager.registerMarshal(
                new AbstractMarshal(MetaDataHolder2.class, AttributeSet.class) {
                    @Override
                    protected <T, V> T marshalImpl(V in) {
                        DefaultMetaDataHolder defaultMetaDataHolder;
                        if (in instanceof FilterMetaDataHolder) {
                            FilterMetaDataHolder filterMetaDataHolder = (FilterMetaDataHolder) in;
                            if (filterMetaDataHolder.metadata instanceof DefaultMetaDataHolder) {
                                defaultMetaDataHolder = (DefaultMetaDataHolder) filterMetaDataHolder.metadata;
                                return (T) defaultMetaDataHolder._attrs;
                            }
                        } else if (in instanceof DefaultMetaDataHolder) {
                            defaultMetaDataHolder = (DefaultMetaDataHolder) in;
                            return (T) defaultMetaDataHolder._attrs;
                        }

                        AttributeSet attrs = new AttributeSet();
                        return (T) attrs;
                    }
                }, MetaDataHolder2.class, AttributeSet.class);
    }

    @Override
    public final String getMetaString(final String key,
            final String fallbackValue) {
        return typedGet(_attrs, key, String.class, fallbackValue);
    }

    @Override
    public final void setMetaString(final String key, final String value) {
        if (value == null) {
            _attrs.removeAttribute(key);
        } else {
            _attrs.setAttribute(key, value);
        }
    }

    @Override
    public final int getMetaInteger(final String key, final int fallbackValue) {
        Integer r = typedGet(_attrs, key, Integer.class);
        return (r != null) ? r : fallbackValue;
    }

    @Override
    public final void setMetaInteger(final String key, final int value) {
        _attrs.setAttribute(key, value);
    }

    @Override
    public final double getMetaDouble(final String key,
            final double fallbackValue) {
        Double r = typedGet(_attrs, key, Double.class);
        return (r != null) ? r : fallbackValue;
    }

    @Override
    public final void setMetaDouble(final String key, final double value) {
        _attrs.setAttribute(key, value);
    }

    @Override
    public final boolean getMetaBoolean(final String key,
            final boolean fallbackValue) {
        Boolean r = typedGet(_attrs, key, Boolean.class);
        return (r != null) ? r : fallbackValue;
    }

    @Override
    public final <T extends Object> T get(final String key) {
        return (T) typedGet(_attrs, key, Object.class, null);
    }

    @Override
    public final void setMetaBoolean(final String key, final boolean value) {
        _attrs.setAttribute(key, value);
    }

    @Override
    public final boolean hasMetaValue(final String key) {
        return (_attrs != null && _attrs.containsAttribute(key)) ||
                (_serializableData != null
                        && _serializableData.containsKey(key));
    }

    @Override
    public final long getMetaLong(final String key, final long fallbackValue) {
        Long r = typedGet(_attrs, key, Long.class);
        return (r != null) ? r : fallbackValue;
    }

    @Override
    public final void setMetaLong(final String key, final long value) {
        _attrs.setAttribute(key, value);
    }

    @Override
    public final void removeMetaData(final String key) {
        _serializableData.remove(key);
        _attrs.removeAttribute(key);
    }

    @Override
    public final ArrayList<String> getMetaStringArrayList(final String key) {
        String[] strArr = typedGet(_attrs, key, String[].class,
                null);
        return strArr != null
                ? new PropogatingArrayList<>(key, _attrs,
                        new ArrayList<>(Arrays.asList(strArr)))
                : null;
    }

    @Override
    public final void setMetaStringArrayList(final String key,
            final ArrayList<String> value) {
        _attrs.setAttribute(key, value.toArray(new String[0]));
    }

    @Override
    public final int[] getMetaIntArray(final String key) {
        return typedGet(_attrs, key, int[].class, null);
    }

    @Override
    public final void setMetaIntArray(final String key, final int[] value) {
        _attrs.setAttribute(key, value);
    }

    private static <T> T typedGet(final Map<String, Object> map,
            final String key,
            final Class<T> type, final T defValue) {
        Object o = map.get(key);
        if (o == null)
            return defValue;
        try {
            return (T) o;
        } catch (ClassCastException e) {
            Log.w(TAG, "Wrong type, "
                    + o.getClass().getName() + ", for \""
                    + key + "\".");
            return defValue;
        }
    }

    private static <T> T typedGet(final Map<String, Object> map,
            final String key,
            final Class<T> type) {
        Object o = map.get(key);
        if (o == null)
            return null;
        try {
            return (T) o;
        } catch (ClassCastException e) {
            Log.w(TAG, "Wrong type, "
                    + o.getClass().getName() + ", for \""
                    + key + "\".");
            return null;
        }
    }

    private static <T> T typedGet(final AttributeSet attrs, final String key,
            final Class<T> type) {
        return typedGet(attrs, key, type, null);
    }

    private static <T> T typedGet(final AttributeSet attrs, final String key,
            final Class<T> type,
            final T defValue) {
        // explicit check if caller is requesting `AttributeSet` to short-circuit marshaling
        if (type.equals(AttributeSet.class)
                && type.equals(attrs.getAttributeValueType(key)))
            return (T) attrs.getAttributeSetAttribute(key);

        Object o = null;
        try {
            o = AttributeSetUtils.get(attrs, key);

            if (o == null)
                return defValue;

            return (T) o;

        } catch (IllegalArgumentException ie) {
            return defValue;
        } catch (ClassCastException e) {
            if (o != null) {
                Log.w(TAG, "wrong type, "
                        + o.getClass().getName() + ", for \""
                        + key + "\".", e);
            }
            return defValue;
        }
    }

    private final Map<String, Object> _serializableData;
    private final AttributeSet _attrs;

    public static void metaMapToBundle(final Map<String, Object> map,
            final Bundle bundle,
            final boolean deep) {
        String key;
        Object value;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            key = entry.getKey();
            value = entry.getValue();
            // XXX - implementation of Bundle has untyped values for backing map
            // so *ANY* putXXX method will store the value. check against
            // Serializable first because it's going to cover most of the
            // cases
            if (deep && (value instanceof Map) && checkBundleMap((Map) value)) {
                Bundle b = new Bundle();
                metaMapToBundle((Map<String, Object>) value, b, deep);
                bundle.putBundle(key, b);
            } else if (value instanceof Serializable) {
                bundle.putSerializable(key, (Serializable) value);
            } else if (value instanceof CharSequence) {
                bundle.putCharSequence(key, (CharSequence) value);
            } else if (value instanceof CharSequence[]) {
                bundle.putCharSequenceArray(key, (CharSequence[]) value);
            } else if (value instanceof Parcelable) {
                bundle.putParcelable(key, (Parcelable) value);
            } else if (value instanceof Parcelable[]) {
                bundle.putParcelableArray(key, (Parcelable[]) value);
            } else if (value instanceof SparseArray) {
                bundle.putSparseParcelableArray(key,
                        (SparseArray<? extends Parcelable>) value);
            } else if (value != null) {
                Log.w(TAG, "Failed to transfer \"" + key
                        + "\" to bundle, type: "
                        + value.getClass());
            }
        }
    }

    private static boolean checkBundleMap(final Map<Object, Object> m) {
        Object key;
        Object value;
        for (Map.Entry entry : m.entrySet()) {
            key = entry.getKey();
            if (!(key instanceof String))
                return false;

            value = entry.getValue();
            // XXX - implementation of Bundle has untyped values for backing map
            // so *ANY* putXXX method will store the value.

            if (!((value instanceof Serializable) ||
                    (value instanceof CharSequence) ||
                    (value instanceof CharSequence[]) ||
                    (value instanceof Parcelable) ||
                    (value instanceof Parcelable[]) ||
                    (value instanceof SparseArray)
                    || (value instanceof Map && checkBundleMap((Map) value)))) {

                return false;
            }
        }
        return true;
    }

    public static void bundleToMetaMap(final Bundle bundle,
            final Map<String, Object> map) {
        Object value;
        for (String key : bundle.keySet()) {
            value = bundle.get(key);

            // XXX - implementation of Bundle has untyped values for backing map
            // so *ANY* putXXX method will store the value. check against
            // Serializable first because it's going to cover most of the
            // cases
            if (value instanceof Bundle) {
                Map<String, Object> m = new HashMap<>();
                bundleToMetaMap((Bundle) value, m);
                map.put(key, m);
            } else if (value instanceof Serializable) {
                bundle.putSerializable(key, (Serializable) value);
            } else if (value != null) {
                Log.w("bundleToMetaMap", "Failed to transfer \"" + key
                        + "\" to map, type: "
                        + value.getClass());
            }
        }
    }

    @Override
    public AttributeSet getMetaAttributeSet(String key) {
        return typedGet(_attrs, key, AttributeSet.class, null);
    }

    @Override
    public void setMetaAttributeSet(String key, AttributeSet value) {
        _attrs.setAttribute(key, value);
    }

    @Override
    public Set<String> getAttributeNames() {
        return _attrs.getAttributeNames();
    }

    @Override
    public void putAll(AttributeSet source) {
        _attrs.putAll(source);
    }

    @Override
    public void clear() {
        _attrs.clear();
    }

    @Override
    public AttributeSet getAttributes() {
        return new AttributeSet(_attrs);
    }
}
