package gov.tak.api.cot;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import gov.tak.api.cot.event.CotDetail;

/**
 * Validate {@link CotDetail} functionality.
 *
 * @since 6.0.0
 */
public class CotDetailTest
{
    private static final String ATTR_KEY_1 = "key1";
    private static final String ATTR_VALUE_1 = "value1";
    private static final String ATTR_KEY_2 = "key2";
    private static final String ATTR_VALUE_2 = "value2";
    private static final String INNER_TEXT = "some text";
    private static final String BOGUS = "bogus";
    private static final String CHILD_1 = "child1";
    private static final String CHILD_2 = "child2";
    private static final String CHILD = "child";

    @Test
    public void test_default_state()
    {
        final CotDetail detail = new CotDetail();

        assertThat(detail.getElementName()).isEqualTo(CotDetail.DETAIL);
        assertThat(detail.getInnerText()).isNull();

        assertThat(detail.getAttributeCount()).isEqualTo(0);
        assertThat(detail.getAttributes()).isEmpty();
        assertThat(detail.getAttribute(BOGUS)).isNull();

        assertThat(detail.childCount()).isEqualTo(0);
        assertThat(detail.getChildren()).isEmpty();
        assertThat(detail.getChild(0)).isNull();
        assertThat(detail.getChildrenByName(BOGUS)).isEmpty();
    }

    @Test
    public void test_legal_attributes()
    {
        final CotDetail detail = new CotDetail();

        detail.setAttribute(ATTR_KEY_1, ATTR_VALUE_1);

        assertThat(detail.getAttributeCount()).isEqualTo(1);
        assertThat(detail.getAttributes()).isNotEmpty();
        assertThat(detail.getAttribute(ATTR_KEY_1)).isEqualTo(ATTR_VALUE_1);
        assertThat(detail.getAttribute(BOGUS)).isNull();
        assertThat(detail.getAttributes()[0].getName()).isEqualTo(ATTR_KEY_1);
        assertThat(detail.getAttributes()[0].getValue()).isEqualTo(ATTR_VALUE_1);

        detail.setAttribute(ATTR_KEY_2, ATTR_VALUE_2);

        assertThat(detail.getAttributeCount()).isEqualTo(2);
        assertThat(detail.getAttributes()).isNotEmpty();
        assertThat(detail.getAttributes()[0].getName()).isEqualTo(ATTR_KEY_1);
        assertThat(detail.getAttributes()[0].getValue()).isEqualTo(ATTR_VALUE_1);
        assertThat(detail.getAttributes()[1].getName()).isEqualTo(ATTR_KEY_2);
        assertThat(detail.getAttributes()[1].getValue()).isEqualTo(ATTR_VALUE_2);

        detail.clearAttributes();
        assertThat(detail.getAttributeCount()).isEqualTo(0);
        assertThat(detail.getAttributes()).isEmpty();
    }

    @Test
    public void test_setting_attribute_twice_does_not_change_count()
    {
        final CotDetail detail = new CotDetail();

        detail.setAttribute(ATTR_KEY_1, ATTR_VALUE_1);
        assertThat(detail.getAttributeCount()).isEqualTo(1);

        detail.setAttribute(ATTR_KEY_1, ATTR_VALUE_1);
        assertThat(detail.getAttributeCount()).isEqualTo(1);
    }

    @Test
    public void test_overwrite_attribute()
    {
        final CotDetail detail = new CotDetail();

        detail.setAttribute(ATTR_KEY_1, ATTR_VALUE_1);
        assertThat(detail.getAttributeCount()).isEqualTo(1);

        detail.setAttribute(ATTR_KEY_1, ATTR_VALUE_2);
        assertThat(detail.getAttributeCount()).isEqualTo(1);
        assertThat(detail.getAttributes()[0].getName()).isEqualTo(ATTR_KEY_1);
        assertThat(detail.getAttributes()[0].getValue()).isEqualTo(ATTR_VALUE_2);
    }

    @Test
    public void test_add_and_remove_child()
    {
        final CotDetail detail = new CotDetail();
        final CotDetail child = new CotDetail(CHILD);

        detail.addChild(child);

        assertThat(detail.getInnerText()).isNull();

        assertThat(detail.childCount()).isEqualTo(1);
        assertThat(detail.getChildren()).isNotEmpty();
        assertThat(detail.getChild(0)).isSameAs(child);
        assertThat(detail.getChild(1)).isNull();

        detail.removeChild(child);
        assertThat(detail.childCount()).isEqualTo(0);
        assertThat(detail.getChildren()).isEmpty();
        assertThat(detail.getChild(0)).isNull();
    }

    @SuppressWarnings("LocalVariableNamingConvention")
    @Test
    public void test_multiple_children()
    {
        final CotDetail detail = new CotDetail();
        final CotDetail child_1_1 = new CotDetail(CHILD_1);
        final CotDetail child_1_2 = new CotDetail(CHILD_1);
        final CotDetail child_2 = new CotDetail(CHILD_2);

        detail.addChild(child_1_1);
        detail.addChild(child_2);
        detail.addChild(child_1_2);

        assertThat(detail.childCount()).isEqualTo(3);

        assertThat(detail.getChild(0)).isSameAs(child_1_1);
        assertThat(detail.getChild(1)).isSameAs(child_2);
        assertThat(detail.getChild(2)).isSameAs(child_1_2);
        assertThat(detail.getChild(3)).isNull();

        assertThat(detail.getChildren()).hasSize(3).containsExactly(child_1_1, child_2, child_1_2);

        assertThat(detail.getChildrenByName(CHILD_1)).hasSize(2).containsExactly(child_1_1, child_1_2);
        assertThat(detail.getChildrenByName(CHILD_2)).hasSize(1).containsExactly(child_2);
        assertThat(detail.getChildrenByName(BOGUS)).isEmpty();

        assertThat(detail.getFirstChildByName(CHILD_1)).isSameAs(child_1_1);
        assertThat(detail.getFirstChildByName(0, CHILD_1)).isSameAs(child_1_1);
        assertThat(detail.getFirstChildByName(1, CHILD_1)).isSameAs(child_1_2);
        assertThat(detail.getFirstChildByName(3, CHILD_1)).isNull();

        assertThat(detail.getFirstChildByName(CHILD_2)).isSameAs(child_2);
        assertThat(detail.getFirstChildByName(0, CHILD_2)).isSameAs(child_2);
        assertThat(detail.getFirstChildByName(1, CHILD_2)).isSameAs(child_2);
        assertThat(detail.getFirstChildByName(2, CHILD_2)).isNull();

        assertThat(detail.getFirstChildByName(BOGUS)).isNull();
        assertThat(detail.getFirstChildByName(0, BOGUS)).isNull();
    }

    @SuppressWarnings("LocalVariableNamingConvention")
    @Test
    public void test_remove_one_of_many_children()
    {
        final CotDetail detail = new CotDetail();
        final CotDetail child_1_1 = new CotDetail(CHILD_1);
        final CotDetail child_1_2 = new CotDetail(CHILD_1);
        final CotDetail child_2 = new CotDetail(CHILD_2);

        detail.addChild(child_1_1);
        detail.addChild(child_2);
        detail.addChild(child_1_2);

        // Remove child_2 and validate new state
        detail.removeChild(child_2);

        assertThat(detail.childCount()).isEqualTo(2);

        assertThat(detail.getChild(0)).isSameAs(child_1_1);
        assertThat(detail.getChild(1)).isSameAs(child_1_2);
        assertThat(detail.getChild(2)).isNull();

        assertThat(detail.getChildren()).hasSize(2).containsExactly(child_1_1, child_1_2);

        assertThat(detail.getChildrenByName(CHILD_1)).hasSize(2).containsExactly(child_1_1, child_1_2);
        assertThat(detail.getChildrenByName(CHILD_2)).isEmpty();

        assertThat(detail.getFirstChildByName(CHILD_1)).isSameAs(child_1_1);
        assertThat(detail.getFirstChildByName(0, CHILD_1)).isSameAs(child_1_1);
        assertThat(detail.getFirstChildByName(1, CHILD_1)).isSameAs(child_1_2);
        assertThat(detail.getFirstChildByName(2, CHILD_1)).isNull();

        assertThat(detail.getFirstChildByName(CHILD_2)).isNull();
        assertThat(detail.getFirstChildByName(0, CHILD_2)).isNull();

        // Test removing the first node named CHILD_1 and ensure that getFirstChildByName then returns the second one
        detail.removeChild(child_1_1);

        assertThat(detail.getFirstChildByName(CHILD_1)).isSameAs(child_1_2);
        assertThat(detail.getFirstChildByName(0, CHILD_1)).isSameAs(child_1_2);
        assertThat(detail.getFirstChildByName(1, CHILD_1)).isNull();
    }

    @Test
    public void test_add_child_clears_inner_text()
    {
        final CotDetail detail = new CotDetail();
        final CotDetail child = new CotDetail(CHILD);

        detail.setInnerText(INNER_TEXT);
        detail.addChild(child);

        assertThat(detail.getInnerText()).isNull();
        assertThat(detail.childCount()).isEqualTo(1);
    }

    @Test
    public void test_inner_text()
    {
        final CotDetail detail = new CotDetail();
        detail.setInnerText(INNER_TEXT);

        assertThat(detail.getInnerText()).isEqualTo(INNER_TEXT);
    }

    @Test
    public void test_set_inner_text_clears_children()
    {
        final CotDetail detail = new CotDetail();
        final CotDetail child = new CotDetail(CHILD);

        detail.addChild(child);
        assertThat(detail.childCount()).isEqualTo(1);

        detail.setInnerText(INNER_TEXT);

        assertThat(detail.childCount()).isEqualTo(0);
        assertThat(detail.getInnerText()).isEqualTo(INNER_TEXT);
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_attribute_name_throws()
    {
        new CotDetail().setAttribute("illegal 'quote'", "value");
    }

    @Test(expected = IllegalArgumentException.class)
    public void test_illegal_detail_name_throws()
    {
        //noinspection ResultOfObjectAllocationIgnored
        new CotDetail("illegal 'quote'");
    }
}
