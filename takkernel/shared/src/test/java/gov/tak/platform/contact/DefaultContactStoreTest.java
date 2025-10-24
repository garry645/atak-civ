package gov.tak.platform.contact;

import com.atakmap.util.DirectExecutorService;
import gov.tak.api.contact.IContact;
import gov.tak.api.contact.IContactListener;
import gov.tak.api.contact.IGroupContact;
import gov.tak.api.contact.IIndividualContact;
import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for the {@link DefaultContactStore} class.
 *
 * @since 0.17.0
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultContactStoreTest
{
    private static final String DEFAULT_CONTACT_ID = "Goose123";
    private static final String DEFAULT_CONTACT_DISPLAY_NAME = "GooseTheGreat";
    private static final String ALTERNATE_CONTACT_ID = "Falcon456";

    private DefaultContactStore contactStoreUnderTest;

    @Mock
    private IContactListener contactListener;

    @Mock
    private IIndividualContact contact;

    @Before
    public void setupContactStore()
    {
        contactStoreUnderTest = new DefaultContactStore(new DirectExecutorService());

        when(contact.getUid()).thenReturn(DEFAULT_CONTACT_ID);
        when(contact.getName()).thenReturn(DEFAULT_CONTACT_DISPLAY_NAME);
    }

    @After
    public void removeAllContacts()
    {
        for (IContact contact : contactStoreUnderTest.getAllContacts())
        {
            contactStoreUnderTest.removeContact(contact.getUid());
        }
    }

    @Test(expected = NullPointerException.class)
    public void addContact_ThrowsNullPointerException_WhenGivenNullContact()
    {
        contactStoreUnderTest.addContact(null);
    }

    @Test
    public void addContact_IgnoresContact_WhenContactHasAlreadyBeenAdded()
    {
        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.addContact(contact);
        contactStoreUnderTest.addContact(contact);

        verify(contactListener).contactAdded(contact);
    }

    @Test
    public void updateContact_CorrectlyUpdatesExistingContact()
    {
        final IIndividualContact updatedContact = mock(IIndividualContact.class);
        final String updatedContactDisplayName = contact.getName() + "-2";
        final ArgumentCaptor<IIndividualContact> contactCaptor = ArgumentCaptor.forClass(IIndividualContact.class);

        when(updatedContact.getUid()).thenReturn(DEFAULT_CONTACT_ID);
        when(updatedContact.getName()).thenReturn(updatedContactDisplayName);

        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.addContact(contact);
        contactStoreUnderTest.updateContact(updatedContact);

        verify(contactListener).contactUpdated(contactCaptor.capture());
        assertThat("Expected Contact listener to be notified of updated Contact.",
                updatedContactDisplayName, is(contactCaptor.getValue().getName()));
        assertThat("Expected getContact() to return the updated contact after contact was updated.",
                contactStoreUnderTest.getContact(updatedContact.getUid()), is(updatedContact));
    }

    @Test(expected = IllegalArgumentException.class)
    public void updateContact_ThrowsIllegalArgumentException_WhenContactHasNotPreviouslyBeenAdded()
    {
        contactStoreUnderTest.updateContact(contact);
    }

    @Test(expected = NullPointerException.class)
    public void updateContact_ThrowsNullPointerException_WhenGivenNullContact()
    {
        contactStoreUnderTest.updateContact(null);
    }

    @Test
    public void removeContact_CorrectlyRemovesContact()
    {
        contactStoreUnderTest.addContact(contact);
        contactStoreUnderTest.removeContact(contact.getUid());

        assertThat("Expected Contact to be removed from store after calling removeContact().",
                contactStoreUnderTest.getContact(contact.getUid()), is(nullValue()));
    }

    @Test
    public void removeContact_NotifiesContactListener()
    {
        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.addContact(contact);
        contactStoreUnderTest.removeContact(contact.getUid());

        verify(contactListener).contactRemoved(contact);
    }

    @Test(expected = NullPointerException.class)
    public void removeContact_ThrowsNullPointerException_WhenGivenNullContactId()
    {
        contactStoreUnderTest.removeContact(null);
    }

    @Test
    public void removeContact_DoesNotNotifyListeners_WhenRemovingContactThatHasNotBeenAdded()
    {
        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.removeContact(contact.getUid());

        verifyNoInteractions(contactListener);
    }

    @Test
    public void containsContact_ReturnsFalse_WhenNoContactsHaveBeenAdded()
    {
        assertThat("Expected containsContact() to return false when no Contacts have been added.",
                contactStoreUnderTest.containsContact(contact), is(false));
    }

    @Test
    public void containsContact_ReturnsTrue_WhenContactHasBeenAdded()
    {
        contactStoreUnderTest.addContact(contact);
        assertThat("Expected containsContact() to return true for a Contact that has been added to the store.",
                contactStoreUnderTest.containsContact(contact), is(true));
    }

    @Test
    public void containsContact_ReturnsFalse_WhenContactHasNotBeenAdded_WhenOtherContactsExistInStore()
    {
        final IIndividualContact contactTwo = mock(IIndividualContact.class);

        when(contactTwo.getUid()).thenReturn(ALTERNATE_CONTACT_ID);

        contactStoreUnderTest.addContact(contact);
        assertThat(
                "Expected containsContact() to return false for a Contact that has not been added, while other Contacts exist in store.",
                contactStoreUnderTest.containsContact(contactTwo), is(false));
    }

    @Test
    public void containsContact_ReturnsTrue_WhenCheckingDifferentContactInstanceWithSameUniqueId()
    {
        final IIndividualContact duplicateContact = mock(IIndividualContact.class);

        when(duplicateContact.getUid()).thenReturn(DEFAULT_CONTACT_ID);

        contactStoreUnderTest.addContact(contact);
        assertThat("Expected containsContact() to return true when Contact with same unique ID already in store.",
                contactStoreUnderTest.containsContact(duplicateContact), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void containsContact_ThrowsNullPointerException_WhenGivenNullContact()
    {
        contactStoreUnderTest.containsContact(null);
    }

    @Test
    public void getContact_ReturnsContact_WhenContactHasBeenAddedToStore()
    {
        contactStoreUnderTest.addContact(contact);

        assertThat("Expected added contact to be retrieved from store.",
                contactStoreUnderTest.getContact(contact.getUid()), is(contact));
    }

    @Test
    public void getContact_ReturnsNull_WhenContactHasNotBeenAddedToStore()
    {
        assertThat("Expected null to be returned while attempting to get non-existent contact from store.",
                contactStoreUnderTest.getContact(contact.getUid()), is(nullValue()));
    }

    @Test
    public void getContact_ReturnsContact_WhenMultipleContactsHaveBeenAddedToStore()
    {
        final IContact alternateContact = mock(IContact.class);

        when(alternateContact.getUid()).thenReturn(ALTERNATE_CONTACT_ID);

        contactStoreUnderTest.addContact(contact);
        contactStoreUnderTest.addContact(alternateContact);

        assertThat("Expected correct contact to be retrieved when multiple contacts have been added to store.",
                contactStoreUnderTest.getContact(contact.getUid()), is(contact));
        assertThat("Expected correct contact to be retrieved when multiple contacts have been added to store.",
                contactStoreUnderTest.getContact(alternateContact.getUid()), is(alternateContact));
    }

    @Test(expected = NullPointerException.class)
    public void getContact_ThrowsNullPointerException_WhenGivenNullUid()
    {
        contactStoreUnderTest.getContact(null);
    }

    @Test
    public void registerContactListener_CorrectlyRegistersListener_WhenNoContactsHaveBeenAdded()
    {
        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.addContact(contact);

        verify(contactListener).contactAdded(contact);
    }

    @Test
    public void registerContactListener_CorrectlyRegistersListener_WhenContactsWerePreviouslyAdded()
    {
        final IIndividualContact contactTwo = mock(IIndividualContact.class);

        when(contactTwo.getUid()).thenReturn(ALTERNATE_CONTACT_ID);

        contactStoreUnderTest.addContact(contact);
        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.addContact(contactTwo);

        verify(contactListener).contactAdded(contact);
        verify(contactListener).contactAdded(contactTwo);
    }

    @Test
    public void registerContactListener_BackfillsExistingContacts()
    {
        final IIndividualContact contactTwo = mock(IIndividualContact.class);
        final IIndividualContact contactThree = mock(IIndividualContact.class);

        when(contactTwo.getUid()).thenReturn(DEFAULT_CONTACT_ID + "-2");
        when(contactThree.getUid()).thenReturn(DEFAULT_CONTACT_ID + "-3");

        contactStoreUnderTest.addContact(contact);
        contactStoreUnderTest.addContact(contactTwo);
        contactStoreUnderTest.addContact(contactThree);

        contactStoreUnderTest.registerContactListener(contactListener);

        verify(contactListener).contactAdded(contact);
        verify(contactListener).contactAdded(contactTwo);
        verify(contactListener).contactAdded(contactThree);
    }

    @Test(expected = NullPointerException.class)
    public void registerContactListener_ThrowsNullPointerException_WhenGivenNullListener()
    {
        contactStoreUnderTest.registerContactListener(null);
    }

    @Test
    public void registerContactListener_HasNoEffect_WhenListenerHasAlreadyBeenRegistered()
    {
        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.registerContactListener(contactListener);

        contactStoreUnderTest.addContact(contact);

        verify(contactListener).contactAdded(contact);
    }

    @Test
    public void unregisterContactListener_CorrectlyRemovesListener_WhenNoContactsHaveBeenAdded()
    {
        final IIndividualContact contactTwo = mock(IIndividualContact.class);

        when(contactTwo.getUid()).thenReturn(ALTERNATE_CONTACT_ID);

        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.unregisterContactListener(contactListener);
        contactStoreUnderTest.addContact(contactTwo);

        verifyNoInteractions(contactListener);
    }

    @Test
    public void unregisterContactListener_CorrectlyRemovesListener_WhenContactsWerePreviouslyAdded()
    {
        final IIndividualContact contactTwo = mock(IIndividualContact.class);

        when(contactTwo.getUid()).thenReturn(ALTERNATE_CONTACT_ID);

        contactStoreUnderTest.addContact(contact);
        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.unregisterContactListener(contactListener);
        contactStoreUnderTest.addContact(contactTwo);

        verify(contactListener).contactAdded(contact);
        verifyNoMoreInteractions(contactListener);
    }

    @Test(expected = NullPointerException.class)
    public void unregisterContactListener_ThrowsNullPointerException_WhenGivenNullListener()
    {
        contactStoreUnderTest.registerContactListener(contactListener);
        contactStoreUnderTest.unregisterContactListener(null);
    }

    @Test
    public void unregisterContactListener_HasNoEffect_WhenListenerHasNotBeenRegistered()
    {
        contactStoreUnderTest.unregisterContactListener(contactListener);

        contactStoreUnderTest.addContact(contact);

        verifyNoInteractions(contactListener);
    }

    @Test
    public void containsListener_ReturnsFalse_WhenListenerHasNotBeenAdded()
    {
        assertThat("Expected containsListener() to return false when no listeners have been added.",
                contactStoreUnderTest.containsListener(contactListener), is(false));
    }

    @Test
    public void containsListener_ReturnsTrue_WhenListenerHasBeenAdded()
    {
        contactStoreUnderTest.registerContactListener(contactListener);
        assertThat("Expected containsListener() to return true when listeners have been added.",
                contactStoreUnderTest.containsListener(contactListener), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void containsListener_ThrowsNullPointerException_WhenGivenNullListener()
    {
        contactStoreUnderTest.containsListener(null);
    }

    @Test
    public void getAllContacts_ReturnsContacts_whenContactsHaveBeenAdded()
    {
        final IIndividualContact contact1 = new IndividualContactBuilder().build();
        final IIndividualContact contact2 = new IndividualContactBuilder().build();

        contactStoreUnderTest.addContact(contact1);
        contactStoreUnderTest.addContact(contact2);

        Assertions.assertThat(contactStoreUnderTest.getAllContacts()).containsOnly(contact1, contact2);
    }

    @Test
    public void getAllContactsOfType_ReturnsEmptyList_whenNoContactsOfTypeHaveBeenAdded()
    {
        contactStoreUnderTest.addContact(new IndividualContactBuilder().build());
        contactStoreUnderTest.addContact(new IndividualContactBuilder().build());

        Assertions.assertThat(contactStoreUnderTest.getAllContactsOfType(IGroupContact.class)).isEmpty();
    }

    @Test
    public void getAllContactsOfType_ReturnsCorrectContacts_whenContactsHaveBeenAdded()
    {
        final IIndividualContact ic1 = new IndividualContactBuilder().build();
        final IIndividualContact ic2 = new IndividualContactBuilder().build();

        contactStoreUnderTest.addContact(ic1);
        contactStoreUnderTest.addContact(ic2);

        final Set<IContact> members = new HashSet<>();

        members.add(ic1);
        final IGroupContact gc1 = new TestConcreteBuilder().withGroupMembers(members).build();

        members.clear();
        members.add(ic2);
        final IGroupContact gc2 = new TestConcreteBuilder().withGroupMembers(members).build();

        contactStoreUnderTest.addContact(gc1);
        contactStoreUnderTest.addContact(gc2);

        Assertions.assertThat(contactStoreUnderTest.getAllContactsOfType(IIndividualContact.class))
                .hasOnlyElementsOfType(IIndividualContact.class)
                .containsOnly(ic1, ic2);

        Assertions.assertThat(contactStoreUnderTest.getAllContactsOfType(IGroupContact.class))
                .hasOnlyElementsOfType(IGroupContact.class)
                .containsOnly(gc1, gc2);
    }

    // Concrete implementation of GroupContactBuilderBase2
    private static class TestConcreteBuilder extends GroupContactBuilderBase2<IGroupContact, TestConcreteBuilder>
    {
    }
}
