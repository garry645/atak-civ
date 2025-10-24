package com.atakmap.commoncommo;

/**
 * A bundle of information updating status of an ongoing enrollment
 * IO operation.
 * See the EnrollmentIO interface.
 * 
 * The info in the superclass applies here-in as well, of course.
 * Particularly, the notion of progress updates and final status delivery
 * carries through here as well.
 * See in particular the documentation of super.status!
 *
 * Note, however, that Enrollment is a multi-step process, so the status
 * firings of PROGRESS and SUCCESS apply to the current enrollment step only!
 * Firings for each step progress sequentially - that is, KEYGEN progress
 * firings come until KEYGEN SUCCESS (or fail). Then, and only then, will
 * status updates for step CSR arrive, and similarly for the SIGN step.
 * 
 * The entire enrollment process is completed when any error status is received,
 * regardless of enrollment step, or when a SUCCESS status is received for
 * the SIGN step.
 */
public class EnrollmentIOUpdate extends SimpleFileIOUpdate {
    /**
     * The current step of the overall enrollment process that
     * this update applies to. 
     */
    public final EnrollmentStep step;
    
    /**
     * Private result buffer.  Only valid on status of SUCCESS
     * for the following steps:
     * KEYGEN: the generated private key in PEM format, encrypted using
     *         the password provided during initialization of the
     *         enrollment operation
     * SIGN: signed client certificate resulting from enrollment
     *       and associated CAs in PKCS#12 binary format, encrypted
     *       using the password provided during intialization of the
     *       enrollment operation
     */
    public final byte[] privResult;
    
    /**
     * CA result buffer.  Only valid on status of SUCCESS
     * for the SIGN step, where it will hold the CAs returned from
     * the enrollment operation in PKCS#12 binary format, encrypted using
     * the password provided during initialization of the enrollment operation.
     * Even for SIGN/SUCCESS updates this may still be null if the
     * enrollment process did not generate any CAs in the reply.
     */
    public final byte[] caResult;
    

    EnrollmentIOUpdate(EnrollmentStep step, 
                  int transferId, SimpleFileIOStatus status,
                  String info,
                  long bytesTransferred,
                  long totalBytes,
                  byte[] privResult,
                  byte[] caResult)
    {
        super(transferId, status, info, bytesTransferred, totalBytes);
        this.step = step;
        this.privResult = privResult; 
        this.caResult = caResult; 
    }
    
}
