package gov.tak.platform.contact;

import gov.tak.api.contact.IIndividualContact;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the {@link gov.tak.api.contact.IContact} classes.
 *
 * @since 3.3.7
 */
public class ContactTest
{
    @Test
    public void defaultContact_hasNoProtocols()
    {
        final IIndividualContact contact = new IndividualContactBuilder().build();
        assertThat(contact.getProtocols()).isEmpty();
    }

    @Test
    public void contact_withProtocolsSet_hasThoseProtocols()
    {
        final IIndividualContact contact = new IndividualContactBuilder().build();
        contact.setProtocols("p1", "p2");

        assertThat(contact.getProtocols()).containsOnly("p1", "p2");
    }

    @Test
    public void contact_withProtocolsSet_supportsThoseProtocols()
    {
        final IIndividualContact contact = new IndividualContactBuilder().build();
        contact.setProtocols("p1", "p2");

        assertThat(contact.supportsProtocol("p1")).isTrue();
        assertThat(contact.supportsProtocol("p2")).isTrue();
        assertThat(contact.supportsProtocol("p3")).isFalse();
    }

    @Test
    public void setProtocols_replacesPriorProtocols()
    {
        final IIndividualContact contact = new IndividualContactBuilder().build();
        contact.setProtocols("p1", "p2");
        assertThat(contact.getProtocols()).containsOnly("p1", "p2");

        contact.setProtocols("p3", "p4");
        assertThat(contact.getProtocols()).containsOnly("p3", "p4");
    }
}

