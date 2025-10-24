
package com.atakmap.android.contact;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import android.content.Context;

import com.atakmap.MapViewMocker;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapEvent;
import com.atakmap.android.maps.MapEventDispatcher;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.maps.MetaMapPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactService;

/**
 * Unit tests for the {@link Contacts} class.
 *
 * @since 0.17.0
 */
@RunWith(MockitoJUnitRunner.Silent.class)
public class ContactsTest {
    private static final int DEFAULT_VERIFY_TIMEOUT_MS = 250;
    private static final String DEFAULT_CONTACT_ID = "Goose123";
    private static final String ALTERNATE_CONTACT_ID1 = "Falcon456";
    private static final String ALTERNATE_CONTACT_ID2 = "Eagle789";

    MapViewMocker mapViewMocker;

    Contacts setupContactStore() {
        return setupContactStore(new MapEventDispatcher());
    }

    Contacts setupContactStore(MapEventDispatcher dispatcher) {
        mapViewMocker = new MapViewMocker();
        MapView mapView = mapViewMocker.getMapView();

        ExecutorService executor = Executors.newSingleThreadExecutor();
        // XXX - store is a singleton...
        IContactService contacts = gov.tak.platform.contact.Contacts
                .getDefaultContactStore();
        return new Contacts(null, dispatcher, "12345", executor, contacts);
    }

    @Before
    public void beforeTest() {
        IContactService contacts = gov.tak.platform.contact.Contacts
                .getDefaultContactStore();
        synchronized (contacts) {
            for (IContact contact : contacts.getAllContacts())
                contacts.removeContact(contact.getUid());
        }
    }

    @After
    public void afterTest() {
        IContactService contacts = gov.tak.platform.contact.Contacts
                .getDefaultContactStore();
        synchronized (contacts) {
            for (IContact contact : contacts.getAllContacts())
                contacts.removeContact(contact.getUid());
        }

    }

    Contact setupDefaultContact() {
        return setupContact(DEFAULT_CONTACT_ID);
    }

    Contact setupContact(String uid) {
        Contact contact = mock(Contact.class);
        when(contact.getUid()).thenReturn(uid);
        return contact;
    }

    @Test
    public void addContact_IgnoresContact_WhenContactHasAlreadyBeenAdded() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();

        contactsUnderTest.addContact(contact);
        contactsUnderTest.addContact(contact);

        assertThat("Contact list should only have a size of 1.",
                contactsUnderTest.getAllContacts().size(), is(1));
    }

    @Test
    public void removeContact_CorrectlyRemovesContact() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();

        contactsUnderTest.addContact(contact);
        contactsUnderTest.removeContactByUuid(contact.getUid());

        assertThat(
                "Expected Contact to be removed from store after calling removeContact().",
                contactsUnderTest.validContact(contact), is(false));
    }

    @Test
    public void removeContact_NotifiesContactListener() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();
        Contacts.OnContactsChangedListener legacyContactListener = mock(
                Contacts.OnContactsChangedListener.class);

        contactsUnderTest.addListener(legacyContactListener);
        contactsUnderTest.addContact(contact);
        contactsUnderTest.removeContactByUuid(contact.getUid());

        // once for Contacts internal legacy onContactChanged, and another for the one we are adding as part of the test for verification purposes
        verify(legacyContactListener,
                timeout(DEFAULT_VERIFY_TIMEOUT_MS).times(2))
                        .onContactChanged(null);
    }

    @Test
    public void removeContact_WhenUsingInstance() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();
        Contacts.OnContactsChangedListener legacyContactListener = mock(
                Contacts.OnContactsChangedListener.class);

        contactsUnderTest.addListener(legacyContactListener);
        contactsUnderTest.addContact(contact);
        contactsUnderTest.removeContact(contact);

        assertFalse(contactsUnderTest.validContact(contact));
    }

    @Test
    public void removeContact_DoesNotNotifyListeners_WhenRemovingContactThatHasNotBeenAdded() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();
        Contacts.OnContactsChangedListener legacyContactListener = mock(
                Contacts.OnContactsChangedListener.class);

        contactsUnderTest.addListener(legacyContactListener);
        contactsUnderTest.removeContactByUuid(contact.getUid());

        verifyNoInteractions(legacyContactListener);
    }

    @Test
    public void containsContact_ReturnsFalse_WhenNoContactsHaveBeenAdded() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();

        assertThat(
                "Expected containsContact() to return false when no Contacts have been added.",
                contactsUnderTest.validContact(contact), is(false));
    }

    @Test
    public void containsContact_ReturnsTrue_WhenContactHasBeenAdded() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();

        contactsUnderTest.addContact(contact);
        assertThat(
                "Expected containsContact() to return true for a Contact that has been added to the store.",
                contactsUnderTest.validContact(contact), is(true));
    }

    @Test
    public void containsContact_ReturnsFalse_WhenContactHasNotBeenAdded_WhenOtherContactsExistInStore() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();

        final Contact contactTwo = setupContact(ALTERNATE_CONTACT_ID1);

        contactsUnderTest.addContact(contact);
        assertThat(
                "Expected containsContact() to return false for a Contact that has not been added, while other Contacts exist in store.",
                contactsUnderTest.validContact(contactTwo), is(false));
    }

    @Test
    public void containsContact_ReturnsTrue_WhenCheckingDifferentContactInstanceWithSameUniqueId() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();

        final Contact duplicateContact = setupContact(DEFAULT_CONTACT_ID);

        contactsUnderTest.addContact(contact);
        assertThat(
                "Expected containsContact() to return true when Contact with same unique ID already in store.",
                contactsUnderTest.validContact(duplicateContact), is(true));
    }

    @Test
    public void getFirstContactWithCallsign_WhenContactExists() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();
        when(contact.getName()).thenReturn("abcdef");

        final Contact altContact1 = setupContact(ALTERNATE_CONTACT_ID1);
        when(altContact1.getName()).thenReturn("ghijkl");
        final Contact altContact2 = setupContact(ALTERNATE_CONTACT_ID2);
        when(altContact1.getName()).thenReturn("mnopqr");

        contactsUnderTest.addContact(contact);
        contactsUnderTest.addContact(altContact1);
        contactsUnderTest.addContact(altContact2);

        final Contact contactWithCallsign = contactsUnderTest
                .getFirstContactWithCallsign("abcdef");
        assertNotNull(contactWithCallsign);
        assertEquals(contact.getUid(), contactWithCallsign.getUid());
    }

    @Test
    public void getFirstContactWithCallsign_WhenContactDoesNotExist() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();
        when(contact.getName()).thenReturn("abcdef");

        final Contact altContact1 = setupContact(ALTERNATE_CONTACT_ID1);
        when(altContact1.getName()).thenReturn("ghijkl");
        final Contact altContact2 = setupContact(ALTERNATE_CONTACT_ID2);
        when(altContact1.getName()).thenReturn("mnopqr");

        contactsUnderTest.addContact(contact);
        contactsUnderTest.addContact(altContact1);
        contactsUnderTest.addContact(altContact2);

        final Contact contactWithCallsign = contactsUnderTest
                .getFirstContactWithCallsign("123456");
        assertNull(contactWithCallsign);
    }

    @Test
    public void getAllContactIds() {
        Contacts contactsUnderTest = setupContactStore();
        Contact contact = setupDefaultContact();
        when(contact.getUpdateStatus())
                .thenReturn(Contact.UpdateStatus.CURRENT);

        final Contact altContact1 = setupContact(ALTERNATE_CONTACT_ID1);
        when(altContact1.getUpdateStatus())
                .thenReturn(Contact.UpdateStatus.CURRENT);
        final Contact altContact2 = setupContact(ALTERNATE_CONTACT_ID2);
        when(altContact2.getUpdateStatus()).thenReturn(Contact.UpdateStatus.NA);

        contactsUnderTest.addContact(contact);
        contactsUnderTest.addContact(altContact1);
        contactsUnderTest.addContact(altContact2);

        List<String> uids = contactsUnderTest.getAllContactUuids();
        assertTrue(uids.contains(contact.getUid()));
        assertTrue(uids.contains(altContact1.getUid()));
        assertFalse(uids.contains(altContact2.getUid()));
        assertEquals(2, uids.size());
    }

    @Test
    public void containsContact_ReturnsFalse_WhenContactRemovedOnMapEvent() {
        MapEventDispatcher dispatcher = new MapEventDispatcher();
        Contacts contactsUnderTest = setupContactStore(dispatcher);
        Contact contact = setupDefaultContact();
        MetaMapPoint contactItem = new MetaMapPoint(new GeoPointMetaData(),
                contact.getUid());
        contactItem.setType("a-f");
        MapEvent event = new MapEvent.Builder(MapEvent.ITEM_REMOVED)
                .setItem(contactItem).build();

        contactsUnderTest.addContact(contact);
        assertTrue(contactsUnderTest.validContact(contact));

        dispatcher.dispatch(event);
        assertFalse(contactsUnderTest.validContact(contact));
    }
}
