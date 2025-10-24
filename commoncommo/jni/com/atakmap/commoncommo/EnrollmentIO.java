package com.atakmap.commoncommo;

/**
 * Implemented by the application using Commo's Enrollment API
 * to receive status and completion notifications of enrollment operations.
 * After implementing this interface, register it with the commo instance
 * using Commo.enableEnrollment()
 */
public interface EnrollmentIO {

    /**
     * Provides an update on an enrollment operation that was
     * initiated via call to Commo.enrollmentInit()/enrollmentStart().
     * The provided update includes the integer id to uniquely
     * identify the operation as well as the status, the current step
     * of the enrollment process and various statistics.
     */
    public void enrollmentUpdate(EnrollmentIOUpdate update);

}
