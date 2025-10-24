
package com.atakmap.android.location.framework;

import gov.tak.api.annotation.NonNull;

public interface LocationDerivation {

    /**
     * A short user defined name that would be useful for the user describing the
     * horizontal position source.  One such example would be the short phrase "GPS".
     * It is recommend that this be no longer than 4 or 5 characters.
     * @return the short phrase describing the horizontal position status derivation.
     */
    @NonNull
    String getHorizontalSource();

    /**
     * A short user defined name that would be useful for the user describing the
     * vertical position source.  One such example would be the short phrase "GPS".
     * It is recommend that this be no longer than 4 or 5 characters.
     * @return the short phrase describing the vertical position source.
     */
    @NonNull
    String getVerticalSource();

}
