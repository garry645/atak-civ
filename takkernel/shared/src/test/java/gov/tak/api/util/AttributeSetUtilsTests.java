package gov.tak.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.BeforeClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;

public class AttributeSetUtilsTests extends AttributeSetTestBase {
    private static Map<String, Object> _nativeAttrsTestMap;
    private static AttributeSet _attrs;
    private static final String MAP = "map";

    @BeforeClass
    public static void init()
    {
        _attrs = new AttributeSet();

        _nativeAttrsTestMap = new HashMap<>();
        _nativeAttrsTestMap.put(STRING, STRING_VALUE);
        _nativeAttrsTestMap.put(BOOL, BOOL_VALUE);
        _nativeAttrsTestMap.put(INT, INT_VALUE);
        _nativeAttrsTestMap.put(LONG, LONG_VALUE);
        _nativeAttrsTestMap.put(DOUBLE, DOUBLE_VALUE);
        _nativeAttrsTestMap.put(BLOB, BLOB_VALUE);
        _nativeAttrsTestMap.put(INT_ARRAY, INT_ARRAY_VALUE);
        _nativeAttrsTestMap.put(LONG_ARRAY, LONG_ARRAY_VALUE);
        _nativeAttrsTestMap.put(DOUBLE_ARRAY, DOUBLE_ARRAY_VALUE);
        _nativeAttrsTestMap.put(BLOB_ARRAY, BLOB_ARRAY_VALUE);
        _nativeAttrsTestMap.put(STRING_ARRAY, STRING_ARRAY_VALUE);
    }

    @Test
    public void putAll_nativeAttrsTypes_roundTrip()
    {
        AttributeSetUtils.putAll(_attrs, _nativeAttrsTestMap, false);
        checkBasicTypes(_attrs);
    }

    @Test
    public void putAll_multipleNestedMaps_roundTrip()
    {
        Map<String, Object> firstMap = new HashMap<>();
        Map<String, Object> secondMap = new HashMap<>();
        Map<String, Object> thirdMap = new HashMap<>();
        String firstStringKey = "first string key";
        String firstStringVal = "first string val";
        String secondStringKey = "second string key";
        String secondStringVal = "second string val";
        String firstMapKey = "first map key";
        String secondMapKey = "second map key";
        firstMap.put(firstStringKey, firstStringVal);
        firstMap.put(firstMapKey, secondMap);
        secondMap.put(secondStringKey, secondStringVal);
        secondMap.put(secondMapKey, thirdMap);
        _nativeAttrsTestMap.put(NESTED, firstMap);
        AttributeSetUtils.putAll(_attrs, _nativeAttrsTestMap, false);

        AttributeSet firstAttrs = _attrs.getAttributeSetAttribute(NESTED);
        AttributeSet secondAttrs = firstAttrs.getAttributeSetAttribute(firstMapKey);
        AttributeSet thirdAttrs = secondAttrs.getAttributeSetAttribute(secondMapKey);

        assertThat(firstAttrs.getStringAttribute(firstStringKey)).isEqualTo(firstStringVal);
        assertThat(secondAttrs.getStringAttribute(secondStringKey)).isEqualTo(secondStringVal);
        assertThat(thirdAttrs).isNotNull();
    }

    @Test
    public void getMapAttribute_nonMapAttrSet_returnsNull()
    {
        AttributeSet mapAttrSet = new AttributeSet();
        _attrs.setAttribute(MAP, mapAttrSet);

        Map<String, Object> map = AttributeSetUtils.getMapAttribute(_attrs, MAP, false);

        assertThat(map).isNullOrEmpty();
    }

    @Test
    public void getMapAttribute_mapAttrSet_returnsMap()
    {
        AttributeSet mapAttrSet = new AttributeSet();
        mapAttrSet.setAttribute(AttributeSetUtils.MAPPED_TYPE, Map.class.getName());
        _attrs.setAttribute(MAP, mapAttrSet);

        Map<String, Object> map = AttributeSetUtils.getMapAttribute(_attrs, MAP, false);

        assertThat(map).isNotNull();
    }

    @Test
    public void getMapAttribute_mapAttrSet_withAttrsKeyValuePairs()
    {
        AttributeSet mapAttrSet = new AttributeSet();
        mapAttrSet.setAttribute(AttributeSetUtils.MAPPED_TYPE, Map.class.getName());
        _attrs.setAttribute(MAP, mapAttrSet);
        fillAttributeSet(mapAttrSet);

        Map<String, Object> map = AttributeSetUtils.getMapAttribute(_attrs, MAP, false);

        assertThat(map).isNotNull();
        checkBasicTypes(map);
    }

    @Test
    public void toMap_nativeAttrsTypes_mapAttrsParity()
    {
        fillAttributeSet(_attrs);
        Map<String, Object> map = AttributeSetUtils.toMap(_attrs, false);

        checkBasicTypes(map);
    }

    @Test
    public void get_nonNativeAttrsTypeWithMarshal_returnsMarshalledClass()
    {
        AttributeSet marshTestClassAttrs = new AttributeSet();
        marshTestClassAttrs.setAttribute("marshalTestClassStringField", STRING_VALUE);
        marshTestClassAttrs.setAttribute(AttributeSetUtils.MAPPED_TYPE,
                MarshalTestClass.class.getName());
        _attrs.setAttribute("marshalTestClass", marshTestClassAttrs);
        MarshalTestClass.registerMarshals();

        Object marshalTestObj = AttributeSetUtils.get(_attrs, "marshalTestClass");

        assertThat(marshalTestObj).isNotNull();
        assertThat(marshalTestObj).isInstanceOf(MarshalTestClass.class);
    }

    @Test
    public void get_nonNativeAttrsTypeWithoutMarshal_returnsNull()
    {
        AttributeSet marshTestClassAttrs = new AttributeSet();
        marshTestClassAttrs.setAttribute("marshalTestClassStringField", STRING_VALUE);
        marshTestClassAttrs.setAttribute(AttributeSetUtils.MAPPED_TYPE,
                MarshalTestClass.class.getName());
        _attrs.setAttribute("marshalTestClass", marshTestClassAttrs);

        Object marshalTestObj = AttributeSetUtils.get(_attrs, "marshalTestClass");

        assertThat(marshalTestObj).isNull();
    }


    private void checkBasicTypes(Map<String, Object> map) {
        assertThat(map.get(BOOL)).isEqualTo(BOOL_VALUE);
        assertThat(map.get(INT)).isEqualTo(INT_VALUE);
        assertThat(map.get(LONG)).isEqualTo(LONG_VALUE);
        assertThat(map.get(DOUBLE)).isEqualTo(DOUBLE_VALUE);
        assertThat(map.get(STRING)).isEqualTo(STRING_VALUE);
        assertThat(map.get(BLOB)).isEqualTo(BLOB_VALUE);

        assertThat(map.get(INT_ARRAY)).isEqualTo(INT_ARRAY_VALUE);
        assertThat(map.get(LONG_ARRAY)).isEqualTo(LONG_ARRAY_VALUE);
        assertThat(map.get(DOUBLE_ARRAY)).isEqualTo(DOUBLE_ARRAY_VALUE);
        assertThat(map.get(STRING_ARRAY)).isEqualTo(STRING_ARRAY_VALUE);
        assertThat(map.get(BLOB_ARRAY)).isEqualTo(BLOB_ARRAY_VALUE);
    }
}

class MarshalTestClass {
    public String stringField;

    static void registerMarshals() {
        MarshalManager.registerMarshal(new AbstractMarshal(MarshalTestClass.class,
                AttributeSet.class) {
            @Override
            protected <T, V> T marshalImpl(V in) {
                if (in instanceof MarshalTestClass) {
                    MarshalTestClass marshalTestClass = (MarshalTestClass)in;
                    AttributeSet attrs = new AttributeSet();
                    attrs.setAttribute("marshalTestClassStringField",
                            marshalTestClass.stringField);
                    return (T) attrs;
                }
                return null;
            }
        }, MarshalTestClass.class, AttributeSet.class);

        MarshalManager.registerMarshal(new AbstractMarshal(AttributeSet.class,
                MarshalTestClass.class) {
            @Override
            protected <T, V> T marshalImpl(V in) {
                if (in instanceof AttributeSet) {
                    AttributeSet attrs = (AttributeSet)in;
                    MarshalTestClass marshalTestClass = new MarshalTestClass();
                    marshalTestClass.stringField =
                            attrs.getStringAttribute("marshalTestClassStringField");
                    return (T) marshalTestClass;
                }
                return null;
            }
        }, AttributeSet.class, MarshalTestClass.class);
    }
}
