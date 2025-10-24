package gov.tak.api.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class AttributeSetTestBase {
    protected static final String BOOL = "boolean";
    protected static final String INT = "int";
    protected static final String LONG = "long";
    protected static final String DOUBLE = "double";
    protected static final String STRING = "string";
    protected static final String BLOB = "blob";
    protected static final String INT_ARRAY = "int_array";
    protected static final String LONG_ARRAY = "long_array";
    protected static final String DOUBLE_ARRAY = "double_array";
    protected static final String STRING_ARRAY = "string_array";
    protected static final String BLOB_ARRAY = "blob_array";
    protected static final String NESTED = "nested";

    protected static final boolean BOOL_VALUE = true;
    protected static final int INT_VALUE = 42;
    protected static final long LONG_VALUE = Long.MAX_VALUE - 1;
    protected static final long[] LONG_ARRAY_VALUE = {LONG_VALUE, Long.MAX_VALUE - 2, Long.MAX_VALUE - 3};
    protected static final double DOUBLE_VALUE = 42.0001d;
    protected static final String STRING_VALUE = "I'm a teapot";
    protected static final byte[] BLOB_VALUE = {1, 2, 3, 4};
    protected static final int[] INT_ARRAY_VALUE = {1, 2, 3, 4};
    protected static final double[] DOUBLE_ARRAY_VALUE = {1, 2, 3, 4};
    protected static final String[] STRING_ARRAY_VALUE = {"fetch", "fido", "fetch"};
    protected static final byte[][] BLOB_ARRAY_VALUE = {{1, 2, 3}, {4, 5, 6}};

    /**
     * Validate the attribute values against what is set in makeAttributeSet.
     */
    protected void checkBasicTypes(AttributeSet as)
    {
        assertThat(as.getBooleanAttribute(BOOL)).isEqualTo(BOOL_VALUE);
        assertThat(as.getIntAttribute(INT)).isEqualTo(INT_VALUE);
        assertThat(as.getLongAttribute(LONG)).isEqualTo(LONG_VALUE);
        assertThat(as.getDoubleAttribute(DOUBLE)).isEqualTo(DOUBLE_VALUE);
        assertThat(as.getStringAttribute(STRING)).isEqualTo(STRING_VALUE);
        assertThat(as.getBinaryAttribute(BLOB)).isEqualTo(BLOB_VALUE);

        assertThat(as.getIntArrayAttribute(INT_ARRAY)).isEqualTo(INT_ARRAY_VALUE);
        assertThat(as.getLongArrayAttribute(LONG_ARRAY)).isEqualTo(LONG_ARRAY_VALUE);
        assertThat(as.getDoubleArrayAttribute(DOUBLE_ARRAY)).isEqualTo(DOUBLE_ARRAY_VALUE);
        assertThat(as.getStringArrayAttribute(STRING_ARRAY)).isEqualTo(STRING_ARRAY_VALUE);
        assertThat(as.getBinaryArrayAttribute(BLOB_ARRAY)).isEqualTo(BLOB_ARRAY_VALUE);
    }

    /**
     * Fill the attribute set with standard values.
     */
    protected AttributeSet fillAttributeSet(AttributeSet as)
    {
        as.setAttribute(BOOL, BOOL_VALUE);
        as.setAttribute(INT, INT_VALUE);
        as.setAttribute(LONG, LONG_VALUE);
        as.setAttribute(DOUBLE, DOUBLE_VALUE);
        as.setAttribute(STRING, STRING_VALUE);
        as.setAttribute(BLOB, BLOB_VALUE);
        as.setAttribute(INT_ARRAY, INT_ARRAY_VALUE);
        as.setAttribute(LONG_ARRAY, LONG_ARRAY_VALUE);
        as.setAttribute(DOUBLE_ARRAY, DOUBLE_ARRAY_VALUE);
        as.setAttribute(STRING_ARRAY, STRING_ARRAY_VALUE);
        as.setAttribute(BLOB_ARRAY, BLOB_ARRAY_VALUE);

        return as;
    }
}
