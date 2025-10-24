
package com.atakmap.android.hierarchy.action;

/**
 * Interface for deleting an item
 */
public interface Delete2 extends Delete {

    /**
     * Perform deletion on this item
     * @param bulkTransactionId provided in the case where delete is being
     *                          called as part of a built transaction.
     *                          If a user prompt is performed, the delete
     *                          call must wait until the user prompt is
     *                          completed before returning true or false.
     * @return True if deletion executed successfully
     */
    boolean delete(String bulkTransactionId);
}
