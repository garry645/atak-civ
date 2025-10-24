
package com.atakmap.android.databridge;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DatasetDefinition {

    private final String uid;
    private final String name;
    private final List<DatasetElement> dataElementList = new ArrayList<>();
    private boolean sealed = false;

    /**
     * Provides a definition for a dataset from a provider
     * @param uid the uid for the dataset definition.    This uid is used to uniquely identify a
     *            dataset independent of the provider.   This would be used to identify the dataset
     *            definition in a global registry.
     */
    public DatasetDefinition(@NonNull String uid, @NonNull String name) {
        this.uid = uid;
        this.name = name;
    }

    /**
     * Seal the dataset so that subscribers cannot modify it.   This should be
     * called before any listeners are notified.
     */
    public void seal() {
        sealed = true;
    }

    /**
     * Gets the human readable name for this specific dataset definition which should
     * match the global registry for the uid.   All dataset definitions with the same
     * uid would have the same name.
     */
    public String getName() {
        return name;
    }

    /**
     * Obtain the UID that matches the provider that the data came from
     * @return the uid for the provider
     */
    public String getUID() {
        return uid;
    }

    /**
     * The elements in the dataset definition
     * @return the data elements that make up the dataset
     */
    public List<DatasetElement> getDataElementList() {
        return Collections.unmodifiableList(dataElementList);
    }

    /**
     * Add a data element to the current dataset definition
     * @param element the data element to add
     */
    public void addDataElement(@NonNull DatasetElement element) {
        if (!sealed)
            dataElementList.add(element);
    }

    @NonNull
    @Override
    public String toString() {
        return "DatasetDefinition{" +
                "name='" + name + '\'' +
                "uid='" + uid + '\'' +
                ", dataElementList= {" + toString(dataElementList) + "}";
    }

    private static String toString(List<DatasetElement> list) {
        StringBuilder sb = new StringBuilder();
        for (DatasetElement element : list) {
            if (sb.length() > 0)
                sb.append(",");
            sb.append(element.toString());
        }
        return sb.toString();
    }
}
