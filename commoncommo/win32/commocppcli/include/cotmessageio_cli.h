#ifndef COTMESSAGEIO_CLI_H_
#define COTMESSAGEIO_CLI_H_

namespace TAK {
        namespace Commo {

        public enum class CoTSendMethod {
            SendTAKServer,
            SendPointToPoint,
            SendAny
        };


        public value class CoTPointData {
        public:
            // Can be used for hae, ce, or le to indicate a lack of value
            literal double NO_VALUE = 9999999.0;

            CoTPointData(double lat, double lon, double hae,
                            double ce, double le) : lat(lat), lon(lon),
                                                    hae(hae), ce(ce), le(le)
            {
            };

            initonly double lat;
            initonly double lon;
            initonly double hae;
            initonly double ce;
            initonly double le;
        };


        public enum class CoTMessageType {
            SituationalAwareness,
            Chat,
        };


        public interface class ICoTMessageListener
        {
        public:
            // rxEndpointId is identifier of NetworkInterface upon which
            // the message was received, if known, or nullptr
            // if not known.
            virtual void CotMessageReceived(System::String ^cotMessage, System::String ^rxEndpointId);
        };

        public interface class IGenericDataListener
        {
        public:
            // rxEndpointId is identifier of NetworkInterface upon which
            // the message was received, if known, or nullptr
            // if not known.
            virtual void GenericDataReceived(array<System::Byte> ^data, System::String ^rxEndpointId);
        };

        public interface class ICoTSendFailureListener
        {
        public:
            virtual void SendCoTFailure(System::String ^host, int port, System::String ^errorReason);
        };


        /**
         * <summary>Interface that can be implemented and registered with a Commo instance
         * to provide an extension implementation to CoT Detail encoding.
         * Implementations can translate portions of CoT detail XMl to/from a binary
         * representation for bandwidth efficiency.  See TAK protocol detail extension
         * documentation for further details. Note specifically that extensions must
         * be registered to the central extension registry before being deployed!
         *
         * Encoding and decoding may be called from multiple threads simultaneously.
         * </summary>
         */
        public interface class ICoTDetailExtender
        {
        public:
            /**
             * <summary>Given an xml element node tree for the element that this extension
             * was registered to support, encodes the data to the binary equivalent
             * in compliance with the protocol extension.
             * Returns encoded data as an array if encoding completed successfully,
             * or null if the element cannot be encoded.
             * </summary>
             * <param name="cotDetailElement">
             * contains an XML document whose root element
             *                    is the node for which the Extender was registered
             *                    to support.  NULL terminated.
             * </param>
             */
            virtual array<System::Byte> ^Encode(System::String ^cotDetailElement);

            /**
             * <summary>Given a buffer of binary encoded extension data, decodes to an xml
             * document containing the element node tree for the element that
             * this extension was registered to support. The decoding is performed
             * in compliance with the protocol extension which this Extender
             * is supporting. Implementations must perform validity checking on the
             * provided data!
             * On success, returns the newly decoded xml equivalent of the provided
             * binary extension data.  Must be an XML document
             * whose root element is the node for which the
             * Extender was registered to support, or null if decoding fails.
             * </summary>
             * <param name="encodedExtension">
             * the encoded extension data to decode
             * </param>
             */
            virtual System::String ^Decode(array<System::Byte> ^encodedExtension);
        };

    }
}


#endif /* COTMESSAGE_H_ */
