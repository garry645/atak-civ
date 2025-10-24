package com.atakmap.commoncommo;

/**
 * Interface that can be implemented and registered with a Commo instance
 * to provide an extension implementation to CoT Detail encoding.
 * Implementations can translate portions of CoT detail XMl to/from a binary
 * representation for bandwidth efficiency.  See TAK protocol detail extension
 * documentation for further details. Note specifically that extensions must
 * be registered to the central extension registry before being deployed!
 *
 * Encoding and decoding may be called from multiple threads simultaneously.
 */
public interface CoTDetailExtender {
    
    /**
     * Given an xml element node tree for the element that this extension
     * was registered to support, encodes the data to the binary equivalent
     * in compliance with the protocol extension.
     * @param cotDetailElement contains an XML document whose root element
     *                    is the node for which the Extender was registered
     *                    to support
     * @return byte array containing the newly encoded extension data, or null
     *         if the provided xml element cannot be encoded
     */
    public byte[] encode(String cotDetailElement);

    /**
     * Given an array of binary encoded extension data, decodes it to an xml
     * document containing the element node tree for the element that
     * this extension was registered to support. The decoding is performed
     * in compliance with the protocol extension which this Extender
     * is supporting. Implementations must perform validity checking on the
     * provided data!
     * @param encodedExtension the encoded extension data to decode
     * @return decoded xml equivalent of the provided
     *         binary extension data.  Must contain an XML document
     *         whose root element is the node for which the
     *         Extender was registered to support, or null if the 
     *         provided data cannot be decoded
     */
    public String decode(byte[] encodedExtension);
}
