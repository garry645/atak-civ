#ifndef ENROLLMENT_CLI_H_
#define ENROLLMENT_CLI_H_

#include "simplefileio_cli.h"
#include "commoresult_cli.h"
#include "cloudio.h"


namespace TAK {
    namespace Commo {


        /**
        * <summary>
        * Enum representing the various steps/stages of
        * a certificate enrollment operation.  See EnrollmentIOUpdate
        * and Commo.EnrollmentInit()/EnrollmentStart()
        * </summary>
        */
        public enum class EnrollmentStep
        {
            /** Test server authorization, paths, and connectivity */
            Keygen,
            /** Generating Certificate request */
            Csr,
            /** Requesting signed certificate */
            Sign
        };


        /**
        * <summary>
        * A bundle of information updating status of an ongoing enrollment
        * IO operation.
        * See the EnrollmentIO interface.
        *
        * The info in the superclass applies here-in as well, of course.
        * Particularly, the notion of progress updates and final status delivery
        * carries through here as well.
        * See in particular the documentation of SimpleFileIOUpdate.status!
        *
        * Note, however, that Enrollment is a multi-step process, so the status
        * firings of PROGRESS and Success apply to the current enrollment step only!
        * Firings for each step progress sequentially - that is, Keygen progress
        * firings come until Keygen Success (or fail). Then, and only then, will
        * status updates for step Csr arrive, and similarly for the Sign step.
        *
        * The entire enrollment process is completed when any error status is received,
        * regardless of enrollment step, or when a Success status is received for
        * the Sign step.
        * </summary>
        */
        public ref class EnrollmentIOUpdate : public SimpleFileIOUpdate
        {
        public:
            /**
            * <summary>
            * The current step of the overall enrollment process that
            * this update applies to. 
            * </summary>
            */
            initonly EnrollmentStep step;

            /**
            * <summary>
            * Private result buffer.  Only valid on status of Success
            * for the following steps:
            * Keygen: the generated private key in PEM format, encrypted using
            *         the password provided during initialization of the
            *         enrollment operation
            * Sign: signed client certificate resulting from enrollment
            *       and associated CAs in PKCS#12 binary format, encrypted
            *       using the password provided during intialization of the
            *       enrollment operation
            * </summary>
            */
            initonly array<System::Byte> ^privResult;

            /**
            * <summary>
            * CA result buffer.  Only valid on status of Success
            * for the Sign step, where it will hold the CAs returned from
            * the enrollment operation in PKCS#12 binary format, encrypted using
            * the password provided during initialization of the enrollment operation.
            * Even for Sign/Success updates this may still be null if the
            * enrollment process did not generate any CAs in the reply.
            * </summary>
            */
            initonly array<System::Byte> ^caResult;

        internal:
            EnrollmentIOUpdate(EnrollmentStep step,
                int transferId, SimpleFileIOStatus status,
                System::String ^info,
                System::Int64 bytesTransferred,
                System::Int64 totalBytes,
                array<System::Byte> ^privResult,
                array<System::Byte> ^caResult);

            virtual ~EnrollmentIOUpdate();

        };

        /**
        * <summary>
        * Implemented by the application using Commo's Enrollment API
        * to receive status and completion notifications of enrollment operations.
        * After implementing this interface, register it with the commo instance
        * using Commo.EnableEnrollment()
        * </summary>
        */
        public interface class IEnrollmentIO
        {
        public:
            /**
            * <summary>
            * Provides an update on an enrollment operation that was
            * initiated via call to Commo.EnrollmentInit()/EnrollmentStart().
            * The provided update includes the integer id to uniquely
            * identify the operation as well as the status, the current step
            * of the enrollment process and various statistics.
            * </summary>
            *
            * <param name="update">
            * the update on the enrollment process
            * </param>
            */
            virtual void EnrollmentUpdate(EnrollmentIOUpdate ^update);
        };
    }
}


#endif
