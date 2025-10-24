
package com.atakmap.comms;

/**
 * Exception class for issues encountered during CotDetailExtension encoding and decoding operations
 */
public class CotDetailExtensionException extends Exception {
    public CotDetailExtensionException(String message) {
        super(message);
    }

    public CotDetailExtensionException(String message, Throwable cause) {
        super(message, cause);
    }
}
