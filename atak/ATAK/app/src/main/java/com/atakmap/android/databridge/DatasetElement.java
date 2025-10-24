
package com.atakmap.android.databridge;

import androidx.annotation.NonNull;

public class DatasetElement {
    public enum Type {
        Boolean,
        Integer,
        Long,
        Double,
        String,
        Binary,
        GeoPoint,
        GeoBounds,
        BooleanArray,
        IntegerArray,
        LongArray,
        DoubleArray,
        StringArray,
        BinaryArray,
        AttributeSet
    }

    public final String name;
    public final String description;
    public final Type type;
    public final boolean queryable;
    public final DatasetElement attributeSet;

    public DatasetElement(@NonNull String name, @NonNull String description,
            @NonNull Type type, boolean queryable) {
        this.name = name;
        this.description = description;
        this.type = type;
        this.queryable = queryable;
        attributeSet = null;
    }

    public DatasetElement(@NonNull String name, @NonNull String description,
            boolean queryable, @NonNull DatasetElement attributeSet) {
        this.name = name;
        this.description = description;
        this.type = Type.AttributeSet;
        this.queryable = queryable;
        this.attributeSet = attributeSet;
    }

    @NonNull
    @Override
    public String toString() {
        return "DataElement{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", type=" + type +
                ", queryable=" + queryable +
                ((attributeSet == null) ? "" : " {" + attributeSet + "} ") +
                '}';
    }
}
