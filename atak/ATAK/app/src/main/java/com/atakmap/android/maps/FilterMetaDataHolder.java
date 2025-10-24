
package com.atakmap.android.maps;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import gov.tak.api.util.AttributeSet;

public class FilterMetaDataHolder implements MetaDataHolder3 {

    protected MetaDataHolder2 metadata;

    public FilterMetaDataHolder(MetaDataHolder2 metadata) {
        this.metadata = metadata;
    }

    @Override
    public String getMetaString(String key, String fallbackValue) {
        return this.metadata.getMetaString(key, fallbackValue);
    }

    @Override
    public void setMetaString(String key, String value) {
        this.metadata.setMetaString(key, value);
    }

    @Override
    public int getMetaInteger(String key, int fallbackValue) {
        return this.metadata.getMetaInteger(key, fallbackValue);
    }

    @Override
    public void setMetaInteger(String key, int value) {
        this.metadata.setMetaInteger(key, value);
    }

    @Override
    public double getMetaDouble(String key, double fallbackValue) {
        return this.metadata.getMetaDouble(key, fallbackValue);
    }

    @Override
    public void setMetaDouble(String key, double value) {
        this.metadata.setMetaDouble(key, value);
    }

    @Override
    public boolean getMetaBoolean(String key, boolean fallbackValue) {
        return this.metadata.getMetaBoolean(key, fallbackValue);
    }

    @Override
    public void setMetaBoolean(String key, boolean value) {
        this.metadata.setMetaBoolean(key, value);
    }

    @Override
    public boolean hasMetaValue(String key) {
        return this.metadata.hasMetaValue(key);
    }

    @Override
    public void setMetaLong(String key, long value) {
        this.metadata.setMetaLong(key, value);
    }

    @Override
    public long getMetaLong(String key, long fallbackValue) {
        return this.metadata.getMetaLong(key, fallbackValue);
    }

    @Override
    public void removeMetaData(String key) {
        this.metadata.removeMetaData(key);
    }

    @Override
    public ArrayList<String> getMetaStringArrayList(String key) {
        return this.metadata.getMetaStringArrayList(key);
    }

    @Override
    public void setMetaStringArrayList(String key, ArrayList<String> value) {
        this.metadata.setMetaStringArrayList(key, value);
    }

    @Override
    public int[] getMetaIntArray(String key) {
        return this.metadata.getMetaIntArray(key);
    }

    @Override
    public void setMetaIntArray(String key, int[] value) {
        this.metadata.setMetaIntArray(key, value);
    }

    @Override
    public <T extends Object> T get(String key) {
        return this.metadata.get(key);
    }

    public void toggleMetaData(String k, boolean on) {
        if (on)
            setMetaBoolean(k, true);
        else
            removeMetaData(k);
    }

    @Override
    public AttributeSet getMetaAttributeSet(String key) {
        return ((MetaDataHolder2) this.metadata).getMetaAttributeSet(key);
    }

    @Override
    public void setMetaAttributeSet(String key, AttributeSet attributeSet) {
        ((MetaDataHolder2) this.metadata).setMetaAttributeSet(key,
                attributeSet);
    }

    @Override
    public Set<String> getAttributeNames() {
        if (this.metadata instanceof MetaDataHolder3)
            return ((MetaDataHolder3) this.metadata).getAttributeNames();
        else {
            // broken capability
            return new HashSet<>();
        }
    }

    @Override
    public void putAll(AttributeSet source) {
        if (this.metadata instanceof MetaDataHolder3)
            ((MetaDataHolder3) this.metadata).putAll(source);
        else {
            // broken capability
        }
    }

    @Override
    public void clear() {
        if (this.metadata instanceof MetaDataHolder3)
            ((MetaDataHolder3) this.metadata).clear();
        else {
            // broken capability
        }
    }

    public AttributeSet getAttributes() {
        if (this.metadata instanceof MetaDataHolder3)
            return ((MetaDataHolder3) this.metadata).getAttributes();
        else {
            // broken capability
            return new AttributeSet();
        }
    }

}
