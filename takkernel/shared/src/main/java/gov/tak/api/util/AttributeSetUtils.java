package gov.tak.api.util;

import com.atakmap.coremap.log.Log;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.util.function.Function;
import gov.tak.platform.marshal.MarshalManager;

public final class AttributeSetUtils {
    /**
     * Added to {@link AttributeSet}s during {@link #putAll(AttributeSet, Map, boolean)} and
     * {@link #putAll(AttributeSet, String, Map, boolean)} calls when there are types provided
     * that aren't natively supported by the {@link AttributeSet}. This is used to deserialize to
     * the proper type. If {@link #toMap(AttributeSet, boolean)} or {@link #get(AttributeSet, String)}
     * returns a {@link Map} with an unwanted MAPPED_TYPE KVP, {@link #recursiveRemoveMappedType(Map)}
     * can be used to remove it.
     */
    public static final String MAPPED_TYPE = "mapped_type";

    private static final String TAG = "AttributeSetUtils";

    /**
     * @param marshal if {@code true} attempts to marshal values that are not natively supported
     * @return A subset of {@code mapToAdd} for KVPs that could not be added due to value types
     */
    public static Map<String, Object> putAll(AttributeSet attrs, Map<String, Object> mapToAdd,
                                             boolean marshal)
    {
        Map<String, Object> unsupported = new HashMap<>();
        for (Map.Entry<String, Object> mapEntry : mapToAdd.entrySet()) {
            String key = mapEntry.getKey();
            Object val = mapEntry.getValue();
            if (!setNativeType(attrs, key, val)) {
                if (val instanceof Map) {
                    attrs.setAttribute(key, fromMap((Map<String, Object>)val, marshal));
                } else {
                    if (marshal && val != null) {
                        Class valClass = val.getClass();
                        AttributeSet marshalledAttrSet = MarshalManager.marshal(val, valClass,
                                AttributeSet.class);
                        if (marshalledAttrSet != null) {
                            marshalledAttrSet.setAttribute(MAPPED_TYPE, valClass.getName());
                            attrs.setAttribute(key, marshalledAttrSet);
                        }
                        else
                            unsupported.put(key, val);
                    }
                    else
                        unsupported.put(key, val);
                }
            }
        }
        return unsupported;
    }

    /**
     * Performs a {@link #putAll(AttributeSet, Map, boolean)} on the {@link AttributeSet} value
     * mapped to by the provided {@code attrs}'s {@code key}
     * @param marshal if {@code true} attempts to marshal values that are not natively supported
     * @return A subset of {@code mapToAdd} for KVPs that could not be added due to value types
     */
    public static Map<String, Object> putAll(AttributeSet attrs, String key, Map<String, Object>
                                             mapToAdd, boolean marshal)
    {
        AttributeSet nestedAttrs;
        if (attrs.containsAttribute(key))
            nestedAttrs = attrs.getAttributeSetAttribute(key);
        else
            nestedAttrs = new AttributeSet();
        if (nestedAttrs != null) {
            nestedAttrs.setAttribute(MAPPED_TYPE, Map.class.getName());
            attrs.setAttribute(key, nestedAttrs);
            return putAll(nestedAttrs, mapToAdd, marshal);
        }
        return new HashMap<>();
    }

    /**
     * @param attrs an {@code AttributeSet} with a nested, {@code Map}-representing
     * {@code AttributeSet} attribute
     * @param key key for the {@code attrs} KVP whose value is a {@code Map}-representing
     * {@code AttributeSet}
     * @param marshal if {@code true} attempts to marshal values that are not natively supported
     * @return a {@code Map} representation of a nested, {@code Map}-representing
     * {@code AttributeSet} on the {@code attrs}
     */
    public static Map<String, Object> getMapAttribute(AttributeSet attrs, String key,
                                                      boolean marshal) {
        final AttributeSetBase.AttributeValue v = attrs.attributeMap.get(key);
        if(v == null)
            return null;
        AttributeSet mapAttrs = null;
        try {
            mapAttrs = attrs.getAttributeSetAttribute(key);
        } catch (IllegalArgumentException e) {
            Log.w(TAG, "Unable to find or cast AttributeSet attribute with key: " + key);
        }
        if(mapAttrs == null)
            return null;
        return toMap(mapAttrs, marshal);
    }

    /**
     * @param attrs an {@code AttributeSet} that will be parsed into a {@code Map}
     * @param marshal if {@code true} attempts to marshal values that are not natively supported
     * during the {@code Map} conversion. {@code AttributeSet}s with a Map.class.getName()
     * string attribute will never be marshalled.
     * @return a {@code Map} representation of an {@code AttributeSet}
     */
    public static Map<String, Object> toMap(AttributeSet attrs, boolean marshal) {
        return marshal ?
                Transmute.map(attrs.attributeMap, marshalForwardTransmute, marshallInverseTransmute) :
                Transmute.map(attrs.attributeMap, mapForwardTransmute, mapInverseTransmute);
    }

    public static AttributeSet fromMap(Map<String, Object> map, boolean marshal) {
        return new AttributeSet(marshal ?
                Transmute.map(map, marshallInverseTransmute, marshalForwardTransmute) :
                Transmute.map(map, mapInverseTransmute, mapForwardTransmute));
    }

    /**
     *
     * @param attrs the {@code AttributeSet} to search by the given {@code attrName}
     * @param attrName the key to use for lookup on the given {@code attrs}
     * @return the attribute mapped to by the {@code attrName}. Requires a registered
     * {@code IMarshal} implementation for any type natively supported by the {@code AttributeSet},
     * or a {@code Map}.
     */
    public static Object get(AttributeSet attrs, String attrName) {
        Object nativeType = getNativeType(attrs, attrName);
        if (nativeType == null) {
            Class<?> attrType = attrs.getAttributeValueType(attrName);
            if (attrType == AttributeSet.class) {
                AttributeSet nestedAttrs = attrs.getAttributeSetAttribute(attrName);
                String mappedType = nestedAttrs.getStringAttribute(MAPPED_TYPE);
                if (mappedType.equals(Map.class.getName())) {
                    return toMap(nestedAttrs, true);
                } else {
                    try {
                        return MarshalManager.marshal(nestedAttrs,
                                AttributeSet.class, Class.forName(mappedType));
                    } catch (ClassNotFoundException e) {
                        Log.w(TAG, "No Marshal found for in: " +
                                AttributeSet.class.getName() + ", out: " + mappedType);
                    }
                }
            }
        }
        return nativeType;
    }

    /**
     * Removes the {@link #MAPPED_TYPE} KVP from the input {@link Map} and all nested {@link Map}s.
     */
    public static void recursiveRemoveMappedType(Map<String, Object> map) {
        Iterator<Map.Entry<String, Object>> it = map.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Object> entry = it.next();
            if (entry.getKey().equals(AttributeSetUtils.MAPPED_TYPE)) {
                it.remove();
            } else if (entry.getValue() instanceof Map) {
                recursiveRemoveMappedType((Map) entry.getValue());
            }
        }
    }

    /**
     * Performs a thread-safe {@code containsAttribute()} and {@code getIntAttribute()} call on
     * the provided {@link AttributeSet}. Use case: dependency on the value mapped to by the given
     * key, only if a mapping exists.
     * @return {@code defValue} if there's no attribute mapping with the provided {@code key}
     */
    public static Integer getIntAttribute(@NonNull AttributeSet attrs, String key,
                                          Integer defValue) {
        attrs.rwLock.acquireRead();
        try
        {
            if (!attrs.containsAttribute(key)) {
                return defValue;
            }
            if (defValue == null) { // guard against unboxing NPE
                return attrs.getIntAttribute(key);
            }
            return attrs.getIntAttribute(key, defValue);
        }
        finally
        {
            attrs.rwLock.releaseRead();
        }
    }

    /**
     * Performs a thread-safe {@code containsAttribute()} and {@code getDoubleAttribute()} call on
     * the provided {@link AttributeSet}. Use case: dependency on the value mapped to by the given
     * key, only if a mapping exists (ascertained by both supplying and checking the return for a
     * {@code null} default value).
     * @return {@code defValue} if there's no attribute mapping with the provided {@code key}
     */
    public static Double getDoubleAttribute(@NonNull AttributeSet attrs, String key,
                                         Double defValue) {
        attrs.rwLock.acquireRead();
        try
        {
            if (!attrs.containsAttribute(key)) {
                return defValue;
            }
            if (defValue == null) { // guard against unboxing NPE
                return attrs.getDoubleAttribute(key);
            }
            return attrs.getDoubleAttribute(key, defValue);
        }
        finally
        {
            attrs.rwLock.releaseRead();
        }
    }

    /**
     * Performs a thread-safe {@code containsAttribute()} and {@code getLongAttribute()} call on
     * the provided {@link AttributeSet}. Use case: dependency on the value mapped to by the given
     * key, only if a mapping exists (ascertained by both supplying and checking the return for a
     * {@code null} default value).
     * @return {@code defValue} if there's no attribute mapping with the provided {@code key}
     */
    public static Long getLongAttribute(@NonNull AttributeSet attrs, String key,
                                       Long defValue) {
        attrs.rwLock.acquireRead();
        try
        {
            if (!attrs.containsAttribute(key)) {
                return defValue;
            }
            if (defValue == null) { // guard against unboxing NPE
                return attrs.getLongAttribute(key);
            }
            return attrs.getLongAttribute(key, defValue);
        }
        finally
        {
            attrs.rwLock.releaseRead();
        }
    }

    /**
     * Performs a thread-safe {@code containsAttribute()} and {@code getBooleanAttribute()} call on
     * the provided {@link AttributeSet}. Use case: dependency on the value mapped to by the given
     * key, only if a mapping exists(ascertained by both supplying and checking the return for a
     * {@code null} default value).
     * @return {@code defValue} if there's no attribute mapping with the provided {@code key}
     */
    public static Boolean getBooleanAttribute(@NonNull AttributeSet attrs, String key,
                                          Boolean defValue) {
        attrs.rwLock.acquireRead();
        try
        {
            if (!attrs.containsAttribute(key)) {
                return defValue;
            }
            if (defValue == null) { // guard against unboxing NPE
                return attrs.getBooleanAttribute(key);
            }
            return attrs.getBooleanAttribute(key, defValue);
        }
        finally
        {
            attrs.rwLock.releaseRead();
        }
    }

    /**
     * Performs a thread-safe {@code containsAttribute()} and {@code getAttributeSetAttribute()} call on
     * the provided {@link AttributeSet}.
     * @return {@code null} if there's no attribute mapping with the provided {@code key}
     */
    public static AttributeSet getAttributeSetAttribute(@NonNull AttributeSet attrs, String key) {
        attrs.rwLock.acquireRead();
        try
        {
            if (!attrs.containsAttribute(key)) {
                return null;
            }
            return attrs.getAttributeSetAttribute(key);
        }
        finally
        {
            attrs.rwLock.releaseRead();
        }
    }

    private static String getMappedType(AttributeSet attrs) {
        String mappedType = "";
        try {
            mappedType = attrs.getStringAttribute(MAPPED_TYPE);
        } catch (IllegalArgumentException e) {
            // ignore
        }
        return mappedType;
    }

    private static Object getNativeType(AttributeSet fromAttrs, String attrName) {
        Class<?> attrType = fromAttrs.getAttributeValueType(attrName);
        if (attrType == String.class) {
            return fromAttrs.getStringAttribute(attrName);
        } else if (attrType == Double.class) {
            return fromAttrs.getDoubleAttribute(attrName);
        } else if (attrType == Boolean.class) {
            return fromAttrs.getBooleanAttribute(attrName);
        } else if (attrType == Integer.class) {
            return fromAttrs.getIntAttribute(attrName);
        } else if (attrType == Long.class) {
            return fromAttrs.getLongAttribute(attrName);
        } else if (attrType == byte[].class) {
            return fromAttrs.getBinaryAttribute(attrName);
        } else if (attrType == int[].class) {
            return fromAttrs.getIntArrayAttribute(attrName);
        } else if (attrType == long[].class) {
            return fromAttrs.getLongArrayAttribute(attrName);
        } else if (attrType == double[].class) {
            return fromAttrs.getDoubleArrayAttribute(attrName);
        } else if (attrType == byte[][].class) {
            return fromAttrs.getBinaryArrayAttribute(attrName);
        } else if (attrType == String[].class) {
            return fromAttrs.getStringArrayAttribute(attrName);
        }
        return null;
    }

    private static boolean setNativeType(AttributeSet attrs, String key, Object value) {
        if (value instanceof String) {
            attrs.setAttribute(key, (String)value);
            return true;
        } else if (value instanceof Integer) {
            attrs.setAttribute(key, (int)value);
            return true;
        } else if (value instanceof Long) {
            attrs.setAttribute(key, (long)value);
            return true;
        } else if (value instanceof Double) {
            attrs.setAttribute(key, (double)value);
            return true;
        } else if (value instanceof Boolean) {
            attrs.setAttribute(key, (Boolean) value);
            return true;
        } else if (value instanceof String[]) {
            attrs.setAttribute(key, (String[])value);
            return true;
        } else if (value instanceof int[]) {
            attrs.setAttribute(key, (int[])value);
            return true;
        } else if (value instanceof long[]) {
            attrs.setAttribute(key, (long[])value);
            return true;
        } else if (value instanceof double[]) {
            attrs.setAttribute(key, (double[])value);
            return true;
        } else if (value instanceof byte[]) {
            attrs.setAttribute(key, (byte[])value);
            return true;
        } else if (value instanceof byte[][]) {
            attrs.setAttribute(key, (byte[][])value);
            return true;
        }
        return false;
    }

    static final Function<AttributeSet.AttributeValue, Object> mapForwardTransmute = new Function<AttributeSet.AttributeValue, Object>() {

        @Override
        public Object apply(AttributeSetBase.AttributeValue arg) {
            if(arg.type == AttributeSetBase.AttributeType.ATTRIBUTE_SET) {
                final AttributeSet nested = (AttributeSet) arg.value;
                return Transmute.map(nested.attributeMap, this);
            } else {
                return arg.value;
            }
        }
    };

    static final Function<Object, AttributeSet.AttributeValue> mapInverseTransmute = new Function<Object, AttributeSet.AttributeValue>() {

        @Override
        public AttributeSet.AttributeValue apply(Object arg) {
            if(arg instanceof Map) {
                final Map m = (Map)arg;
                // verify all keys are strings
                for(Object k : m.keySet())
                    if(!(k instanceof String))
                        return null;
                return new AttributeSetBase.AttributeValue(AttributeSet.AttributeType.ATTRIBUTE_SET, new AttributeSet(Transmute.map((Map<String, Object>)m, mapInverseTransmute, mapForwardTransmute)));
            } else if(arg instanceof Integer) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.INTEGER, arg);
            } else if(arg instanceof Long) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.LONG, arg);
            } else if(arg instanceof Double) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.DOUBLE, arg);
            } else if(arg instanceof String) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.STRING, arg);
            } else if(arg instanceof byte[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.BLOB, arg);
            } else if(arg instanceof int[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.INTEGER_ARRAY, arg);
            } else if(arg instanceof long[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.LONG_ARRAY, arg);
            } else if(arg instanceof double[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.DOUBLE_ARRAY, arg);
            } else if(arg instanceof String[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.STRING_ARRAY, arg);
            } else if(arg instanceof byte[][]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.BLOB_ARRAY, arg);
            } else {
                // XXX - not sure about failover here
                return null;
            }
        }
    };

    static final Function<AttributeSet.AttributeValue, Object> marshalForwardTransmute = new Function<AttributeSet.AttributeValue, Object>() {

        @Override
        public Object apply(AttributeSetBase.AttributeValue arg) {
            if(arg.type == AttributeSetBase.AttributeType.ATTRIBUTE_SET) {
                final AttributeSet nested = (AttributeSet) arg.value;
                do {
                    String mappedType = getMappedType(nested);
                    if(mappedType == null)
                        break;
                    if (mappedType.equals(Map.class.getName()))
                        break;
                    try {
                        return MarshalManager.marshal(nested,
                                AttributeSet.class, Class.forName(mappedType));
                    } catch (ClassNotFoundException e) {
                        Log.w(TAG, "No Marshal found for (type) in: " +
                                AttributeSet.class.getName() + ", out: " + mappedType);
                    }
                } while(false);
                return Transmute.map(nested.attributeMap, this);
            } else {
                return arg.value;
            }
        }
    };

    static final Function<Object, AttributeSet.AttributeValue> marshallInverseTransmute = new Function<Object, AttributeSet.AttributeValue>() {

        @Override
        public AttributeSet.AttributeValue apply(Object arg) {
            if(arg instanceof Map) {
                final Map m = (Map)arg;
                // verify all keys are strings
                for(Object k : m.keySet())
                    if(!(k instanceof String))
                        return null;
                return new AttributeSetBase.AttributeValue(AttributeSet.AttributeType.ATTRIBUTE_SET, new AttributeSet(Transmute.map((Map<String, Object>)m, marshallInverseTransmute, marshalForwardTransmute)));
            } else if(arg instanceof Integer) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.INTEGER, arg);
            } else if(arg instanceof Long) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.LONG, arg);
            } else if(arg instanceof Double) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.DOUBLE, arg);
            } else if(arg instanceof String) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.STRING, arg);
            } else if(arg instanceof byte[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.BLOB, arg);
            } else if(arg instanceof int[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.INTEGER_ARRAY, arg);
            } else if(arg instanceof long[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.LONG_ARRAY, arg);
            } else if(arg instanceof double[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.DOUBLE_ARRAY, arg);
            } else if(arg instanceof String[]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.STRING_ARRAY, arg);
            } else if(arg instanceof byte[][]) {
                return new AttributeSet.AttributeValue(AttributeSet.AttributeType.BLOB_ARRAY, arg);
            } else if(arg != null) {
                final AttributeSet marshaled = MarshalManager.marshal(arg,
                        (Class)arg.getClass(), AttributeSet.class);
                if(marshaled != null) {
                    return new AttributeSet.AttributeValue(AttributeSet.AttributeType.ATTRIBUTE_SET, marshaled);
                }
                // XXX - not sure about failover here
                return null;
            } else {
                return null;
            }
        }
    };
}
