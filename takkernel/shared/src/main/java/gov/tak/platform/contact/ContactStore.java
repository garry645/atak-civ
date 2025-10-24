package gov.tak.platform.contact;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactListener;
import gov.tak.api.contact.IContactService;

/**
 * The Contact Store allows core applications and plugins to manage {@link IContact}s through the API, as well as
 * register/unregister {@link IContactListener}s so that listeners can receive notifications when Contacts are added,
 * removed, or updated.
 * <p>
 * Aside from takkernel-implemented components, it is typically expected that plugins will be responsible for managing
 * Contacts as sourced through a specific transport protocol (e.g. XMPP).
 * <p>
 * On the other hand, it is typically expected that core applications will register as Contact Listeners to be notified
 * of changes in Contacts, thereby informing the user of all available Contacts at any given time.
 * <p>
 * An instance of this class can be accessed through the Contacts class, and immutable {@link IContact} instances
 * can be built through the available ContactBuilder classes. For example:
 * <pre>
 *     IContactStore contactStore = Contacts.getDefaultContactStore();
 *     IIndividualContact contact = new IndividualContactBuilder().withUniqueId("uniqueNewYork").build();
 *     contactStore.registerContactListener(contactListener);
 *     contactStore.addContact(contact);
 *     contactStore.unregisterContactListener(contactListener);
 * </pre>
 *
 * @see IContact
 * @see IContactListener
 * @since 0.32.0
 */
public abstract class ContactStore implements IContactService
{
    ContactStore()
    {
        // Prevent API consumers from extending this class
    }
}

