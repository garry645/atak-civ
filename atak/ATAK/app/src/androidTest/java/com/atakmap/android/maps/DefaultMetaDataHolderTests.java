
package com.atakmap.android.maps;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.AttributeSetUtils;
import gov.tak.platform.marshal.AbstractMarshal;
import gov.tak.platform.marshal.MarshalManager;

@RunWith(AndroidJUnit4.class)
public class DefaultMetaDataHolderTests {
    private DefaultMetaDataHolder _defaultMetaDataHolder;

    private static final String BOOL = "boolean";
    private static final String INT = "int";
    private static final String LONG = "long";
    private static final String DOUBLE = "double";
    private static final String STRING = "string";
    private static final String INT_ARRAY = "int_array";
    private static final String STRING_ARRAY = "string_array";
    private static final String ATTRIBUTE_SET = "attribute_set";
    private static final String MARSHAL_TEST_CLASS = "marshalTestClass";
    private static final boolean BOOL_VALUE = true;
    private static final int INT_VALUE = 42;
    private static final long LONG_VALUE = Long.MAX_VALUE - 1;
    private static final double DOUBLE_VALUE = 42.0001d;
    private static final String STRING_VALUE = "I'm a teapot";
    private static final int[] INT_ARRAY_VALUE = {
            1, 2, 3, 4
    };
    private static final String[] STRING_ARRAY_VALUE = {
            "fetch", "fido", "fetch"
    };

    private static final MetaDataHolderMarshalTestClass MARSHAL_TEST_CLASS_VALUE = new MetaDataHolderMarshalTestClass();

    private static final AttributeSet ATTRIBUTE_SET_VALUE = new AttributeSet() {
        {
            setAttribute(STRING, STRING_VALUE);
            setAttribute(INT, INT_VALUE);
        }
    };

    @Before
    public void beforeTest() {
        _defaultMetaDataHolder = new DefaultMetaDataHolder();
    }

    //region (get) missing keys - expect defaults
    @Test
    public void getMetaString_missingKey_returnsDefault() {
        String metaStr = _defaultMetaDataHolder.getMetaString(STRING,
                STRING_VALUE);
        Assert.assertEquals(STRING_VALUE, metaStr);
    }

    @Test
    public void getMetaInt_missingKey_returnsDefault() {
        int metaInt = _defaultMetaDataHolder.getMetaInteger(INT, INT_VALUE);
        Assert.assertEquals(INT_VALUE, metaInt);
    }

    @Test
    public void getMetaDouble_missingKey_returnsDefault() {
        double metaDouble = _defaultMetaDataHolder.getMetaDouble(DOUBLE,
                DOUBLE_VALUE);
        Assert.assertEquals(DOUBLE_VALUE, metaDouble, 0);
    }

    @Test
    public void getMetaLong_missingKey_returnsDefault() {
        long metaLong = _defaultMetaDataHolder.getMetaLong(LONG, LONG_VALUE);
        Assert.assertEquals(LONG_VALUE, metaLong);
    }

    @Test
    public void getMetaBool_missingKey_returnsDefault() {
        boolean metaBool = _defaultMetaDataHolder.getMetaBoolean(BOOL,
                BOOL_VALUE);
        Assert.assertEquals(BOOL_VALUE, metaBool);
    }

    @Test
    public void getMetaAttributeSet_missingKey_returnsNull() {
        AttributeSet metaAttributeSet = _defaultMetaDataHolder
                .getMetaAttributeSet(ATTRIBUTE_SET);
        Assert.assertNull(metaAttributeSet);
    }
    //endregion

    //region (get) KVP type mismatches - expect ClassCastExceptions
    @Test(expected = ClassCastException.class)
    public void getMetaString_keyMapsToWrongType_throwsClassCastException() {
        _defaultMetaDataHolder.setMetaInteger(STRING, INT_VALUE);
        _defaultMetaDataHolder.getMetaString(STRING, STRING_VALUE);
    }

    @Test(expected = ClassCastException.class)
    public void getMetaInt_keyMapsToWrongType_throwsClassCastException() {
        _defaultMetaDataHolder.setMetaString(INT, STRING_VALUE);
        _defaultMetaDataHolder.getMetaInteger(INT, INT_VALUE);
    }

    @Test(expected = ClassCastException.class)
    public void getMetaDouble_keyMapsToWrongType_throwsClassCastException() {
        _defaultMetaDataHolder.setMetaInteger(DOUBLE, INT_VALUE);
        _defaultMetaDataHolder.getMetaDouble(DOUBLE, DOUBLE_VALUE);
    }

    @Test(expected = ClassCastException.class)
    public void getMetaLong_keyMapsToWrongType_throwsClassCastException() {
        _defaultMetaDataHolder.setMetaInteger(LONG, INT_VALUE);
        _defaultMetaDataHolder.getMetaLong(LONG, LONG_VALUE);
    }

    @Test(expected = ClassCastException.class)
    public void getMetaBoolean_keyMapsToWrongType_throwsClassCastException() {
        _defaultMetaDataHolder.setMetaInteger(BOOL, INT_VALUE);
        _defaultMetaDataHolder.getMetaBoolean(BOOL, BOOL_VALUE);
    }

    @Test(expected = ClassCastException.class)
    public void getMetaStringArrayList_keyMapsToWrongType_throwsClassCastException() {
        _defaultMetaDataHolder.setMetaInteger(STRING_ARRAY, INT_VALUE);
        _defaultMetaDataHolder.getMetaStringArrayList(STRING_ARRAY);
    }

    @Test(expected = ClassCastException.class)
    public void getMetaIntArray_keyMapsToWrongType_throwsClassCastException() {
        _defaultMetaDataHolder.setMetaInteger(INT_ARRAY, INT_VALUE);
        _defaultMetaDataHolder.getMetaIntArray(INT_ARRAY);
    }

    @Test(expected = ClassCastException.class)
    public void getMetaMap_keyMapsToWrongType_throwsClassCastException() {
        _defaultMetaDataHolder.setMetaInteger(BOOL, INT_VALUE);
        _defaultMetaDataHolder.getMetaAttributeSet(BOOL);
    }
    //endregion

    //region (get, set covered implicitly) Standard Case - has key, returns val
    @Test
    public void getMetaString_hasStrKey_returnsStr() {
        _defaultMetaDataHolder.setMetaString(STRING, STRING_VALUE);
        String metaStr = _defaultMetaDataHolder.getMetaString(STRING,
                STRING_VALUE);
        Assert.assertEquals(STRING_VALUE, metaStr);
    }

    @Test
    public void getMetaInt_hasIntKey_returnsInt() {
        _defaultMetaDataHolder.setMetaInteger(INT, INT_VALUE);
        int metaInt = _defaultMetaDataHolder.getMetaInteger(INT, INT_VALUE);
        Assert.assertEquals(INT_VALUE, metaInt);
    }

    @Test
    public void getMetaDouble_hasStrKey_returnsStr() {
        _defaultMetaDataHolder.setMetaDouble(DOUBLE, DOUBLE_VALUE);
        double metaDouble = _defaultMetaDataHolder.getMetaDouble(DOUBLE,
                DOUBLE_VALUE);
        Assert.assertEquals(DOUBLE_VALUE, metaDouble, 0);
    }

    @Test
    public void getMetaLong_hasStrKey_returnsStr() {
        _defaultMetaDataHolder.setMetaLong(LONG, LONG_VALUE);
        long metaLong = _defaultMetaDataHolder.getMetaLong(LONG, LONG_VALUE);
        Assert.assertEquals(LONG_VALUE, metaLong);
    }

    @Test
    public void getMetaBool_hasStrKey_returnsStr() {
        _defaultMetaDataHolder.setMetaBoolean(BOOL, BOOL_VALUE);
        boolean metaBoolean = _defaultMetaDataHolder.getMetaBoolean(BOOL,
                BOOL_VALUE);
        Assert.assertEquals(BOOL_VALUE, metaBoolean);
    }

    @Test
    public void getMetaStringArrayList_hasStrArrayListKey_returnsStrArrayList() {
        ArrayList<String> setMetaStrArrayList = new ArrayList<>(
                Arrays.asList(STRING_ARRAY_VALUE));
        _defaultMetaDataHolder.setMetaStringArrayList(STRING_ARRAY,
                setMetaStrArrayList);
        ArrayList<String> getMetaStrArrayList = _defaultMetaDataHolder
                .getMetaStringArrayList(STRING_ARRAY);
        Assert.assertEquals(setMetaStrArrayList, getMetaStrArrayList);
    }

    @Test
    public void getMetaStringArrayList_listChanges_returnedListReflectsChanges() {
        ArrayList<String> setMetaStrArrayList = new ArrayList<>(
                Arrays.asList(STRING_ARRAY_VALUE));
        _defaultMetaDataHolder.setMetaStringArrayList(STRING_ARRAY,
                setMetaStrArrayList);
        ArrayList<String> getMetaStrArrList = _defaultMetaDataHolder
                .getMetaStringArrayList(STRING_ARRAY);
        String addedString = "ATAK-18798 f6116a6f new String test";
        getMetaStrArrList.add(addedString);
        ArrayList<String> verificationGetMetaStrArrList = _defaultMetaDataHolder
                .getMetaStringArrayList(STRING_ARRAY);
        Assert.assertTrue(verificationGetMetaStrArrList.contains(addedString));
    }

    @Test
    public void getMetaIntArrayList_hasIntArray_returnsDefault() {
        _defaultMetaDataHolder.setMetaIntArray(INT_ARRAY, INT_ARRAY_VALUE);
        int[] metaIntArrayList = _defaultMetaDataHolder
                .getMetaIntArray(INT_ARRAY);
        Assert.assertEquals(INT_ARRAY_VALUE, metaIntArrayList);
    }

    @Test
    public void get_marshalledTypeWithMarshalImpl_returnsMarshalledType() {
        AttributeSet setMarshalTestClassAttrs = new AttributeSet();
        setMarshalTestClassAttrs.setAttribute("marshalTestClassStringField",
                STRING_VALUE);
        setMarshalTestClassAttrs.setAttribute(AttributeSetUtils.MAPPED_TYPE,
                MetaDataHolderMarshalTestClass.class.getName());
        MetaDataHolderMarshalTestClass.registerMarshals();

        _defaultMetaDataHolder.setMetaAttributeSet("marshalTestClass",
                setMarshalTestClassAttrs);
        MetaDataHolderMarshalTestClass marshalTestClass = _defaultMetaDataHolder
                .get("marshalTestClass");
        Assert.assertNotNull(marshalTestClass);
        Assert.assertEquals(STRING_VALUE, marshalTestClass.stringField);
    }
    //endregion

    @Test
    public void getMetaAttributeSet_withSeveralEntries_returnsAttributeSetWithMatchingEntryCount() {
        MetaDataHolderMarshalTestClass.registerMarshals();
        ATTRIBUTE_SET_VALUE.setAttribute(MARSHAL_TEST_CLASS,
                MarshalManager.marshal(MARSHAL_TEST_CLASS_VALUE,
                        MetaDataHolderMarshalTestClass.class,
                        AttributeSet.class));
        _defaultMetaDataHolder.setMetaAttributeSet(ATTRIBUTE_SET,
                ATTRIBUTE_SET_VALUE);
        AttributeSet attributeSet = _defaultMetaDataHolder
                .getMetaAttributeSet(ATTRIBUTE_SET);
        Assert.assertEquals(ATTRIBUTE_SET_VALUE.getAttributeNames().size(),
                attributeSet.getAttributeNames().size());
    }

    @Test
    public void getMetaAttributeSet_withSeveralEntries_returnsAttributeSetWithAllMatchingEntryValues() {
        MetaDataHolderMarshalTestClass.registerMarshals();
        ATTRIBUTE_SET_VALUE.setAttribute(MARSHAL_TEST_CLASS,
                MarshalManager.marshal(MARSHAL_TEST_CLASS_VALUE,
                        MetaDataHolderMarshalTestClass.class,
                        AttributeSet.class));
        _defaultMetaDataHolder.setMetaAttributeSet(ATTRIBUTE_SET,
                ATTRIBUTE_SET_VALUE);
        AttributeSet attributeSet = _defaultMetaDataHolder
                .getMetaAttributeSet(ATTRIBUTE_SET);
        Assert.assertEquals(STRING_VALUE,
                attributeSet.getStringAttribute(STRING));
        Assert.assertEquals(INT_VALUE, attributeSet.getIntAttribute(INT));
        Assert.assertEquals(MARSHAL_TEST_CLASS_VALUE,
                MarshalManager.marshal(
                        attributeSet.getAttributeSetAttribute(
                                MARSHAL_TEST_CLASS),
                        AttributeSet.class,
                        MetaDataHolderMarshalTestClass.class));
    }
}

class MetaDataHolderMarshalTestClass {
    String stringField;

    MetaDataHolderMarshalTestClass() {
        stringField = "";
    }

    static void registerMarshals() {
        MarshalManager.registerMarshal(
                new AbstractMarshal(MetaDataHolderMarshalTestClass.class,
                        AttributeSet.class) {
                    @Override
                    protected <T, V> T marshalImpl(V in) {
                        if (in instanceof MetaDataHolderMarshalTestClass) {
                            MetaDataHolderMarshalTestClass marshalTestClass = (MetaDataHolderMarshalTestClass) in;
                            AttributeSet attrs = new AttributeSet();
                            if (marshalTestClass.stringField != null)
                                attrs.setAttribute(
                                        "marshalTestClassStringField",
                                        marshalTestClass.stringField);
                            return (T) attrs;
                        }
                        return null;
                    }
                }, MetaDataHolderMarshalTestClass.class, AttributeSet.class);

        MarshalManager.registerMarshal(new AbstractMarshal(AttributeSet.class,
                MetaDataHolderMarshalTestClass.class) {
            @Override
            protected <T, V> T marshalImpl(V in) {
                if (in instanceof AttributeSet) {
                    AttributeSet attrs = (AttributeSet) in;
                    MetaDataHolderMarshalTestClass marshalTestClass = new MetaDataHolderMarshalTestClass();
                    if (attrs.containsAttribute("marshalTestClassStringField"))
                        marshalTestClass.stringField = attrs.getStringAttribute(
                                "marshalTestClassStringField");
                    return (T) marshalTestClass;
                }
                return null;
            }
        }, AttributeSet.class, MetaDataHolderMarshalTestClass.class);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof MetaDataHolderMarshalTestClass &&
                ((MetaDataHolderMarshalTestClass) o).stringField
                        .equals(stringField);
    }
}
