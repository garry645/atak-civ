package gov.tak.api.contact;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.contact.Contacts;

import java.util.Objects;

/**
 * Represents a Contact that can be communicated with.
 *
 * @since 0.17.0
 */
public interface IContact
{
    String[] EMPTY_STRINGS = new String[0];

    /**
     * @return The unique ID of the contact
     */
    @NonNull
    String getUid();

    /**
     * @return The display name of the contact (e.g. call sign, group name, etc.)
     */
    @NonNull
    String getName();

    /**
     * @return The attributes of the contact, described by a set of key/value tuples
     */
    @NonNull
    AttributeSet getAttributes();

    /**
     * @return Array of protocol names supported by this contact, may be empty, never null
     */
    @NonNull
    default String[] getProtocols()
    {
        return getAttributes().getStringArrayAttribute(Contacts.PROTOCOL_ATTRIBUTE_KEY, EMPTY_STRINGS);
    }

    /**
     * Set the list of protocols supported by this contact.  This will completely replace any prior list of protocols.
     *
     * @param protocols New list of supported protocols
     */
    default void setProtocols(@NonNull String... protocols)
    {
        Objects.requireNonNull(protocols, "'protocols' must not be null");
        getAttributes().setAttribute(Contacts.PROTOCOL_ATTRIBUTE_KEY, protocols.clone());
    }

    /**
     * @param protocol Protocol to test for
     * @return {@code true} if this contact has the requested {@code protocol} in its list of supported protocols
     * @see #getProtocols()
     */
    default boolean supportsProtocol(@NonNull String protocol)
    {
        Objects.requireNonNull(protocol, "'protocol' must not be null");

        for (String p : getProtocols())
        {
            if (p.equals(protocol)) return true;
        }
        return false;
    }
}
