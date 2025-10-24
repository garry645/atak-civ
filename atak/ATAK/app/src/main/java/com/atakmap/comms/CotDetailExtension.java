
package com.atakmap.comms;

/**
 * Interface that can be implemented and registered with CommsMapComponent
 * to provide an extension implementation for communications-level encoding of various
 * CoT detail elements with the aim of optimizing bandwidth utilization.
 * See TAK protocol detail extension documentation and
 * CommsMapComponent.registerCotDetailExtension() for further details.
 * Note specifically that extensions must
 * be registered to the central TPC extension registry before being deployed!
 * <p>
 * Implementations should be aware that encoding and decoding may be called
 * from multiple threads simultaneously.
 */
public interface CotDetailExtension {
    /**
     * Given an xml element node tree for the element that this extension
     * was registered to support, encodes the data to the binary equivalent
     * in compliance with the protocol extension.
     * An extension implementation must completely and fully translate/encode all data
     * represented in the xml equivalent.
     * 
     * @param detailXml contains an XML document whose root element
     *                    is the node for which the Extender was registered
     *                    to support
     * @return byte array containing the newly encoded extension data
     * @throws CotDetailExtensionException if there is an error parsing the given detailXml 
     *                                     or encoding the result
     */
    byte[] encode(String detailXml) throws CotDetailExtensionException;

    /**
     * Given an array of binary encoded extension data, decodes it to an xml
     * document containing the element node tree for the element that
     * this extension was registered to support. The decoding is performed
     * in compliance with the protocol extension which this Extender
     * is supporting. Implementations are responsible for performing any and all validity checking
     * on the provided encoded data!
     * @param encodedExtension the encoded extension data to decode
     * @return decoded xml equivalent of the provided
     *         binary extension data.  Must contain an XML document
     *         whose root element is the node for which the
     *         Extender was registered to support (element name matching that which the extension
     *         was registered as supporting)
     * @throws CotDetailExtensionException if unexpected/invalid data is encountered 
     *                                     in encodedExtension or there is an error producing the
     *                                     xml result
     */
    String decode(byte[] encodedExtension)
            throws CotDetailExtensionException;
}
