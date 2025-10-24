package gov.tak.api.symbology;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import gov.tak.api.cot.CotEventTest;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;

public class MilSymDetailHandlerTests {
    private static final String ID = "INSPSCP-----**-";
    private static final String UNIT_MODIFIER_CODE_1 = "UNIT_MODIFIER_CODE_1";
    private static final String UNIT_MODIFIER_CODE_2 = "UNIT_MODIFIER_CODE_2";
    private static final String UNIT_MODIFIER_CODE_1_VALUE = "UNIT_MODIFIER_CODE_1_VALUE";
    private static final String UNIT_MODIFIER_CODE_2_VALUE = "UNIT_MODIFIER_CODE_2_VALUE";

    private static final String VALID_WITH_DETAIL_INNER_TEXT = CotEventTest.START_XML + CotEventTest.START_EVENT +
            CotEventTest.VALID_POINT +
            "<detail>" +
            "<" + MilSymDetailHandler.DETAIL + " " + DetailConstants.ATTR_ID + "=\"" + ID + "\">\n" +
            "	<" + DetailConstants.UNIT_MODIFIER + " " + DetailConstants.ATTR_CODE + "=\"" + UNIT_MODIFIER_CODE_1 + "\">" + UNIT_MODIFIER_CODE_1_VALUE + "</" + DetailConstants.UNIT_MODIFIER + ">\n" +
            "	<" + DetailConstants.UNIT_MODIFIER + " " + DetailConstants.ATTR_CODE + "=\"" + UNIT_MODIFIER_CODE_2 + "\">" + UNIT_MODIFIER_CODE_2_VALUE + "</" + DetailConstants.UNIT_MODIFIER + ">\n" +
            "</" + MilSymDetailHandler.DETAIL + ">\n" +
            "</detail>" +
            CotEventTest.END_EVENT;

    private static final String INVALID_MISSING_ID = CotEventTest.START_XML + CotEventTest.START_EVENT +
            CotEventTest.VALID_POINT +
            "<detail>" +
            "<" + MilSymDetailHandler.DETAIL + ">\n" +
            "	<" + DetailConstants.UNIT_MODIFIER + " " + DetailConstants.ATTR_CODE + "=\"" + UNIT_MODIFIER_CODE_1 + "\">" + UNIT_MODIFIER_CODE_1_VALUE + "</" + DetailConstants.UNIT_MODIFIER + ">\n" +
            "	<" + DetailConstants.UNIT_MODIFIER + " " + DetailConstants.ATTR_CODE + "=\"" + UNIT_MODIFIER_CODE_2 + "\">" + UNIT_MODIFIER_CODE_2_VALUE + "</" + DetailConstants.UNIT_MODIFIER + ">\n" +
            "</" + MilSymDetailHandler.DETAIL + ">\n" +
            "</detail>" +
            CotEventTest.END_EVENT;

    private MilSymDetailHandler _handler;
    private CotEvent _event;
    private CotDetail _detail;
    private AttributeSet _attrs;

    @Before
    public void beforeTests() {
        _handler = new MilSymDetailHandler();
        _event = CotEvent.parse(VALID_WITH_DETAIL_INNER_TEXT);
        _detail = _event.findDetail(MilSymDetailHandler.DETAIL);
        _attrs = new AttributeSet();
    }

    @Test
    public void toItemMetaData_cotHasIdAndCodes_modelMatchesCot() {
        _handler.toItemMetadata(_attrs, _event, _detail);

        String id = _attrs.getStringAttribute(MilSymDetailHandler.MILSYM_ATTR);
        String firstCodeVal = _attrs.getStringAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + UNIT_MODIFIER_CODE_1);
        String secondCodeVal = _attrs.getStringAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + UNIT_MODIFIER_CODE_2);
        Assert.assertEquals(id, ID);
        Assert.assertEquals(firstCodeVal, UNIT_MODIFIER_CODE_1_VALUE);
        Assert.assertEquals(secondCodeVal, UNIT_MODIFIER_CODE_2_VALUE);
    }

    @Test
    public void toItemMetaData_cotMissingId_milSymAttributesRemoved() {
        _attrs.setAttribute(MilSymDetailHandler.MILSYM_ATTR, ID);
        _attrs.setAttribute(MilSymStandardAttributes.MILSYM_CP_LAT, ID);
        _attrs.setAttribute(MilSymStandardAttributes.MILSYM_CP_LON, ID);
        _attrs.setAttribute(MilSymStandardAttributes.MILSYM_CP_ALT, ID);
        _attrs.setAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + UNIT_MODIFIER_CODE_1, UNIT_MODIFIER_CODE_1_VALUE);
        _attrs.setAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + UNIT_MODIFIER_CODE_2, UNIT_MODIFIER_CODE_2_VALUE);

        CotEvent event = CotEvent.parse(INVALID_MISSING_ID);
        CotDetail detail = event.findDetail(MilSymDetailHandler.DETAIL);
        _handler.toItemMetadata(_attrs, event, detail);

        Assert.assertFalse(_attrs.containsAttribute(MilSymDetailHandler.MILSYM_ATTR));
        Assert.assertFalse(_attrs.containsAttribute(MilSymStandardAttributes.MILSYM_CP_LAT));
        Assert.assertFalse(_attrs.containsAttribute(MilSymStandardAttributes.MILSYM_CP_LON));
        Assert.assertFalse(_attrs.containsAttribute(MilSymStandardAttributes.MILSYM_CP_ALT));
        Assert.assertFalse(_attrs.containsAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + UNIT_MODIFIER_CODE_1));
        Assert.assertFalse(_attrs.containsAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + UNIT_MODIFIER_CODE_2));
    }

    @Test
    public void toCotDetail_attrsHasIdAndCodes_modelMatchesCot() {
        _attrs.setAttribute(MilSymDetailHandler.MILSYM_ATTR, ID);
        _attrs.setAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + UNIT_MODIFIER_CODE_1, UNIT_MODIFIER_CODE_1_VALUE);
        _attrs.setAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + UNIT_MODIFIER_CODE_2, UNIT_MODIFIER_CODE_2_VALUE);

        _handler.toCotDetail(_attrs, _event, _detail);

        // ID match
        String id = _detail.getAttribute(DetailConstants.ATTR_ID);
        Assert.assertEquals(id, ID);

        // Unit modifier match
        List<CotDetail> unitModifierDetails = _detail.getChildrenByName(DetailConstants.UNIT_MODIFIER);
        Assert.assertEquals(unitModifierDetails.size(), 2);

        CotDetail firstUnitModDetail = unitModifierDetails.get(0);
        String firstCodeVal = firstUnitModDetail.getInnerText();
        CotDetail secondUnitModDetail = unitModifierDetails.get(1);
        String secondCodeVal = secondUnitModDetail.getInnerText();
        Assert.assertEquals(firstCodeVal, UNIT_MODIFIER_CODE_1_VALUE);
        Assert.assertEquals(secondCodeVal, UNIT_MODIFIER_CODE_2_VALUE);
    }
}
