package com.atakmap.commoncommo;

/**
 * Generic exception derivative used for all Commo library-specific
 * exceptions.
 */
public class CommoException extends Exception {

    public CommoException() {
    }
    
    public CommoException(String msg) {
        super(msg);
    }
    
}
