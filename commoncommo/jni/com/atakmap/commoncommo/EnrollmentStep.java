package com.atakmap.commoncommo;

/**
 * Enum representing the various steps/stages of
 * a certificate enrollment operation.  See EnrollmentIOUpdate
 * and Commo.enrollmentInit()/enrollmentStart()
 */
public enum EnrollmentStep {
    /** Test server authorization, paths, and connectivity */
    KEYGEN(0),
    /** Generating Certificate request */
    CSR(1),
    /** Requesting signed certificate */
    SIGN(2);
    
    private final int id;
    
    private EnrollmentStep(int id) {
        this.id = id;
    }
    
    int getNativeVal() {
        return id;
    }
}
