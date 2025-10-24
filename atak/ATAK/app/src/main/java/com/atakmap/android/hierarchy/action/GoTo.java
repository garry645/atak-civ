
package com.atakmap.android.hierarchy.action;

public interface GoTo extends Action {
    /**
     * Interface for a hierarchy item that defines the implementation of the goto action
     * @param select if the hierarchy item is selected
     * @return true if the goto was handled successfully.
     */
    boolean goTo(boolean select);
}
