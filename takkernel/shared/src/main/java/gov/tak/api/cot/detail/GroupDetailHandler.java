package gov.tak.api.cot.detail;

import java.util.HashSet;
import java.util.Set;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;

import gov.tak.api.cot.event.CotDetail;
import gov.tak.api.cot.event.CotEvent;
import gov.tak.api.util.AttributeSet;
import gov.tak.api.util.AttributeSetUtils;

import static gov.tak.api.cot.detail.DetailConstants.*;

/**
 * Team markers - group
 */
public final class GroupDetailHandler implements ICotDetailHandler2 {
    private final boolean _importOnly;
    private final Set<String> _detailNames;

    public GroupDetailHandler() {
        this(true);
    }

    public GroupDetailHandler(boolean importOnly) {
        _importOnly = importOnly;
        _detailNames = new HashSet<>();
        _detailNames.add(GROUP);
    }

    @Override
    public boolean isSupported(Object processItem, AttributeSet attrs, CotEvent event, CotDetail detail) {
        return AttributeSetUtils.getAttributeSetAttribute(attrs, GROUP) != null ||
            _detailNames.contains(detail.getElementName());
    }

    @Override
    public Set<String> getDetailNames() {
        return _detailNames;
    }

    @Override
    public boolean toCotDetail(Object processItem, AttributeSet attrs, CotEvent event,
                        CotDetail root)
    {
        if(_importOnly)
            return true;
        final CotDetail __group = toCotDetail(attrs);
        root.addChild(__group);
        return true;
    }


    @Override
    public ICotDetailHandler.ImportResult toItemMetadata(Object processItem, AttributeSet attrs, CotEvent event,
                                                  CotDetail detail)
    {
        final String name = detail.getAttribute(ATTR_NAME);
        if (!FileSystemUtils.isEmpty(name))  {
            attrs.setAttribute("team", ensureValidGroup(name));
        } else {
            attrs.removeAttribute("team");
        }

        final String role = detail.getAttribute(ATTR_ROLE);
        if (!FileSystemUtils.isEmpty(role)) attrs.setAttribute("atakRoleType", role);

        final String customRole = detail.getAttribute(ATTR_EXTENDED_ROLE);
        if (!FileSystemUtils.isEmpty(customRole)) 
               attrs.setAttribute("customRole", customRole);
        else 
               attrs.removeAttribute("customRole");

        final String customRoleAbbreviation = detail.getAttribute(ATTR_EXTENDED_ROLE_ABBREVIATION);
        if (!FileSystemUtils.isEmpty(customRoleAbbreviation))
               attrs.setAttribute("customRoleAbbreviation", customRoleAbbreviation);
        else 
               attrs.removeAttribute("customRoleAbbreviation");

        final String displayAsSymbologyGraphic = detail.getAttribute(ATTR_USE_TYPE_ICON);
        if (!FileSystemUtils.isEmpty(displayAsSymbologyGraphic))
            attrs.setAttribute("displayAsSymbologyGraphic", Boolean.parseBoolean(displayAsSymbologyGraphic));
        else
            attrs.removeAttribute("displayAsSymbologyGraphic");

        return ICotDetailHandler.ImportResult.SUCCESS;

    }

    public static CotDetail toCotDetail(AttributeSet attrs) {
        CotDetail __group = new CotDetail(GROUP);
        __group.setAttribute(ATTR_NAME, attrs.getStringAttribute("team", null));
        __group.setAttribute(ATTR_ROLE,
                attrs.getStringAttribute("atakRoleType", "Team Member"));
        __group.setAttribute(ATTR_EXTENDED_ROLE,
                attrs.getStringAttribute("customRole", null));
        __group.setAttribute(ATTR_EXTENDED_ROLE_ABBREVIATION,
                attrs.getStringAttribute("customRoleAbbreviation", null));
        if(attrs.getBooleanAttribute("displayAsSymbologyGraphic", false))
            __group.setAttribute(ATTR_USE_TYPE_ICON, "true");
        return __group;
    }

    /**
     * This ensures that the valid group colors are being used - otherwise
     * it will default to a high contrast non-user settable group "Pink".
     * @param groupName the group name as received in the CoT message
     * @return the normalized group name
     */
    public static String ensureValidGroup(final String groupName) {
        // a pink group does not exist and indicates an error with the group name
        String retval = "Pink";
        switch (groupName.toLowerCase(LocaleUtil.getCurrent())) {
            case "white":
                retval = "White";
                break;
            case "orange":
                retval = "Orange";
                break;
            case "maroon":
                retval = "Maroon";
                break;
            case "purple":
                retval = "Purple";
                break;
            case "dark blue":
                retval = "Dark Blue";
                break;
            case "teal":
                retval = "Teal";
                break;
            case "dark green":
                retval = "Dark Green";
                break;
            case "brown":
                retval = "Brown";
                break;
            case "cyan":
                retval = "Cyan";
                break;
            case "blue":
                retval = "Blue";
                break;
            case "green":
                retval = "Green";
                break;
            case "red":
                retval = "Red";
                break;
            case "magenta":
                retval = "Magenta";
                break;
            case "yellow":
            case "rad sensor":
                retval = "Yellow";
                break;
        }
        return retval;
    }
}
