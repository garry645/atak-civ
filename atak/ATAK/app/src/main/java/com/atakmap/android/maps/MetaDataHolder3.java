
package com.atakmap.android.maps;

import java.util.Set;

import gov.tak.api.util.AttributeSet;

public interface MetaDataHolder3 extends MetaDataHolder2 {

    /**
     * Returns the list of attribute names within the MetaDataHolder
     *
     * @return the set of string values in the attribute set
     */
    Set<String> getAttributeNames();

    /**
     * Copy all the attributes from the specified AttributeSet into this one.  Existing attributes with the
     * same name will be overwritten.
     *
     * @param source Source of attributes to copy
     */
    void putAll(AttributeSet source);

    /**
     * Clears all key value pairs within this container.
     */
    void clear();

    /**
     * Returns a copy of the current AttributeSet that defines all of the attributes for the holder.
     * @return the copy of attributes from this metadata holder
     */

    AttributeSet getAttributes();

}
