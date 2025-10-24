package gov.tak.api.symbology;

import static gov.tak.api.cot.detail.DetailConstants.ATTR_CODE;

import com.atakmap.coremap.cot.event.CotPoint;
import com.atakmap.coremap.maps.coords.GeoPoint;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.cot.detail.DetailConstants;
import gov.tak.api.cot.detail.ICotDetailHandler;
import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.symbology.SymbologyProvider;
import gov.tak.platform.symbology.milstd2525.MilSymStandardAttributes;

/**
 * Bi-directional "__milsym" detail handling, to include unit modifiers and a child "link"
 * {@code CotDetail}.
 *
 * @since 6.10.0
 */
public final class MilSymDetailHandler implements ICotDetailHandler {
    public static final String DETAIL_MULTIPOINT = "__milsym";
    public static final String DETAIL_SINGLEPOINT = "__milicon";

    /** @deprecated use {@link #DETAIL_MULTIPOINT} for multi-point, {@link #DETAIL_SINGLEPOINT} for single-point */
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public static final String DETAIL = DETAIL_MULTIPOINT;
    public static final String MILSYM_ATTR = "milsym";

    final static String ATTR_MILSYM_SOURCE = "milsym.sourcetag";

    private final Set<String> _detailNames;

    public MilSymDetailHandler() {
        _detailNames = new HashSet<>();
        _detailNames.add(DETAIL_MULTIPOINT);
        _detailNames.add(DETAIL_SINGLEPOINT);
    }

    @Override
    public Set<String> getDetailNames() {
        return _detailNames;
    }

    @Override
    public ImportResult toItemMetadata(AttributeSet attrs, CotEvent event, CotDetail detail) {
        final String id = detail.getAttribute(DetailConstants.ATTR_ID);
        if (id != null) {
            for (CotDetail unitModifier : detail.getChildrenByName(DetailConstants.UNIT_MODIFIER)) {
                final String code = unitModifier.getAttribute(DetailConstants.ATTR_CODE);
                final String value = unitModifier.getInnerText();
                if (value == null) {
                    attrs.removeAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + code);
                } else {
                    attrs.setAttribute(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX + code, value);
                }
            }

            final CotDetail link = detail.getFirstChildByName(DetailConstants.LINK);
            if (link != null) {
                GeoPoint point = GeoPoint.parseGeoPoint(link.getAttribute(DetailConstants.ATTR_POINT));

                if (point != null) {
                    attrs.setAttribute(MilSymStandardAttributes.MILSYM_CP_LAT, point.getLatitude());
                    attrs.setAttribute(MilSymStandardAttributes.MILSYM_CP_LON, point.getLongitude());
                    attrs.setAttribute(MilSymStandardAttributes.MILSYM_CP_ALT, point.getAltitude());
                }
            }
            attrs.setAttribute(MILSYM_ATTR, id);
        } else
            clearMilsymMetadata(attrs);

        return ImportResult.SUCCESS;
    }

    @Override
    public boolean toCotDetail(AttributeSet attrs, CotEvent event, CotDetail root) {
        if (!attrs.containsAttribute(MILSYM_ATTR))
            return false;

        final String symbolCode = attrs.getStringAttribute(MILSYM_ATTR, null);

        // use __milsym
        //  - if 2525C
        //  - if multi-point
        //  - if not supported and source __milsym
        // use __milicon
        //  - if single-point
        //  - if not supported and source __milicon

        final ISymbologyProvider provider = SymbologyProvider.getProviderFromSymbol(symbolCode);
        if(provider == null) {
            // the SIDC is not supported, pass-through
            toCotDetail(
                    attrs.getStringAttribute(ATTR_MILSYM_SOURCE, DETAIL_SINGLEPOINT),
                    symbolCode,
                    attrs,
                    event,
                    root);
        } else {
            final boolean isSinglePoint = provider.getDefaultSourceShape(symbolCode) == ShapeType.Point;
            // write __milicon for single-point
            if(isSinglePoint)
                toCotDetail(DETAIL_SINGLEPOINT, symbolCode, attrs, event, root);
            // write __milsym for multi-point AND single-point 2525C for most robust backwards
            // interoperability
            if(!isSinglePoint || provider.getName().equals("2525C"))
                toCotDetail(DETAIL_MULTIPOINT, symbolCode, attrs, event, root);
        }
        return true;
    }

    private static void toCotDetail(String tag, String symbolCode, AttributeSet attrs, CotEvent event, CotDetail root) {
        CotDetail symbolDetail = root.getFirstChildByName(tag);
        if (symbolDetail == null)
            root.addChild(symbolDetail = new CotDetail(tag));

        symbolDetail.setAttribute(DetailConstants.ATTR_ID, symbolCode);

        if (symbolCode != null) {
            // clear any existing unit modifiers
            for (CotDetail unitModifier : symbolDetail.getChildrenByName(DetailConstants.UNIT_MODIFIER)) {
                symbolDetail.removeChild(unitModifier);
            }
            // add unit modifiers
            for (String attributeName : attrs.getAttributeNames())
            {
                if (attributeName.startsWith(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX))
                {
                    final int lastDot = attributeName.lastIndexOf('.');
                    if (lastDot > 0 && lastDot < attributeName.length() - 1)
                    {
                        final CotDetail unitModifierDetail = new CotDetail(DetailConstants.UNIT_MODIFIER);

                        unitModifierDetail.setAttribute(ATTR_CODE, attributeName.substring(lastDot + 1));
                        unitModifierDetail.setInnerText(attrs.getStringAttribute(attributeName));

                        symbolDetail.addChild(unitModifierDetail);
                    }
                }
            }
        }

        if (attrs.containsAttribute(MilSymStandardAttributes.MILSYM_CP_LAT)
                && attrs.containsAttribute(MilSymStandardAttributes.MILSYM_CP_LON)
                && attrs.containsAttribute(MilSymStandardAttributes.MILSYM_CP_ALT)) {

            GeoPoint point = new GeoPoint(
                    attrs.getDoubleAttribute(MilSymStandardAttributes.MILSYM_CP_LAT, Double.NaN),
                    attrs.getDoubleAttribute(MilSymStandardAttributes.MILSYM_CP_LON, Double.NaN),
                    attrs.getDoubleAttribute(MilSymStandardAttributes.MILSYM_CP_ALT, Double.NaN));

            CotDetail link = symbolDetail.getFirstChildByName(DetailConstants.LINK);
            if (link == null)
                symbolDetail.addChild(link = new CotDetail(DetailConstants.LINK));
            link.setAttribute(DetailConstants.ATTR_POINT, CotPoint.decimate(point));
            link.setAttribute(DetailConstants.ATTR_UID, UUID.randomUUID().toString());
            link.setAttribute(DetailConstants.ATTR_TYPE, "b-m-p-c");
            link.setAttribute(DetailConstants.ATTR_RELATION, "c-p");
        }
    }

    private void clearMilsymMetadata(AttributeSet attrs) {
        attrs.removeAttribute(MILSYM_ATTR);
        attrs.removeAttribute(MilSymStandardAttributes.MILSYM_CP_LAT);
        attrs.removeAttribute(MilSymStandardAttributes.MILSYM_CP_LON);
        attrs.removeAttribute(MilSymStandardAttributes.MILSYM_CP_ALT);

        for (String attributeName : attrs.getAttributeNames())
        {
            if (attributeName.startsWith(MilSymStandardAttributes.MILSYM_MODIFIER_PREFIX))
                attrs.removeAttribute(attributeName);
        }
    }

    @Override
    public boolean isSupported(AttributeSet attrs, CotEvent event, CotDetail detail) {
        return attrs.containsAttribute(MILSYM_ATTR) || _detailNames.contains(detail.getElementName());
    }
}
