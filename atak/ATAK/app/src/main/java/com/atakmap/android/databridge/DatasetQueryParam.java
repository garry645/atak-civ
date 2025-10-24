
package com.atakmap.android.databridge;

import androidx.annotation.NonNull;

/**
 * Defines a very simplistic and'ing query syntax which can be used by the dataset listener.
 */
public class DatasetQueryParam {
    public enum Operation {
        EQUALS,
        CONTAINS,
        INTERSECTS,
        NOT_EQUALS,
        NOT_CONTAINS,
        NOT_INTERSECT,
        GREATER_THAN,
        LESS_THEN
    }

    public final String key;
    public final Operation operation;
    public final Object value;

    public DatasetQueryParam(@NonNull String key, @NonNull Operation operation,
            @NonNull Object value) {
        this.key = key;
        this.operation = operation;
        this.value = value;
    }

}
