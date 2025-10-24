package gov.tak.api.cot.detail;

/**
 * Common CoT detail names for XML elements and attributes.
 * @since 6.0
 */
public class DetailConstants
{
    public static final String ANCHOR_UID = "anchorUID";
    public static final String ARCHIVE = "archive";
    public static final String BEARING = "bearing";
    public static final String BEARING_UNITS = "bearingUnits";
    public static final String BPHA = "BPHA"; // Battle Position Holding Area
    public static final String CHAT = "__chat";
    public static final String CHAT_GROUP = "chatgrp";
    public static final String CHAT_RECEIPT = "__chatreceipt";
    public static final String COLOR = "color";
    public static final String CONTACT = "contact";
    public static final String ELLIPSE = "ellipse";
    /** @since 5.2.0 */
    public static final String EXTRUDE_MODE = "extrudeMode";
    public static final String FILL_COLOR = "fillColor";

    public static final String FORCE_DELETE = "__forcedelete";
    public static final String GROUP = "__group";

    public static final String GEOFENCE = "__geofence";
    public static final String HEIGHT = "height";
    public static final String HIERARCHY = "hierarchy";
    public static final String HIERARCHY_GROUP = "group";
    public static final String INCLINATION = "inclination";
    public static final String INITIAL_BEARING = "initialBearing";
    public static final String LINE_STYLE = "LineStyle";
    public static final String LINK = "link";
    public static final String LINK_ATTR = "link_attr";
    public static final String MED_EVAC = "_medevac_";

    /**
     * detail containing MIL-2525 symbology code.
     */
    public static final String MILSYM = "__milsym";
    public static final String MISSION = "mission";
    public static final String NORTH_REF = "northRef";
    public static final String POLY_STYLE = "PolyStyle";
    public static final String PRECISION_LOCATION = "precisionlocation";
    public static final String RANGE = "range";
    public static final String RANGE_UID = "rangeUID";
    public static final String RANGE_UNITS = "rangeUnits";
    public static final String REMARKS = "remarks";
    public static final String SERVER_DESTINATION = "__serverdestination";
    public static final String SHAPE = "shape";
    public static final String STATUS = "status";
    public static final String STROKE_COLOR = "strokeColor";
    public static final String STROKE_WEIGHT = "strokeWeight";
    public static final String STYLE = "Style";
    public static final String SYMBOL_STANDARD = "__symbolStandard";

    public static final String TAKV = "takv";
    public static final String TRACK = "track";
    public static final String UID = "uid";
    /**
     * Modifier codes for MIL-2525 symbols.
     */
    public static final String UNIT_MODIFIER = "unitmodifier";
    public static final String USER_ICON = "usericon";
    public static final String WIDTH = "width";

    public static final String ATTR_ANGLE = "angle";
    public static final String ATTR_ARGB = "argb";

    public static final String ATTR_AUTHOR_UID = "authorUid";
    public static final String ATTR_BATTERY = "battery";
    public static final String ATTR_CALLSIGN = "callsign";
    public static final String ATTR_CHATROOM = "chatroom";
    public static final String ATTR_CODE = "code";
    public static final String ATTR_COLOR = COLOR;
    public static final String ATTR_COURSE = "course";
    public static final String ATTR_DELETE_CHILD = "deleteChild";
    public static final String ATTR_DESTINATIONS = "destinations";

    public static final String ATTR_DEVICE = "device";
    public static final String ATTR_DIRECTION = "direction";
    public static final String ATTR_DROID = "Droid";
    public static final String ATTR_ENDPOINT = "endpoint";
    public static final String ATTR_GROUP_OWNER = "groupOwner";
    public static final String ATTR_ICONSET_PATH = "iconsetpath";
    public static final String ATTR_ID = "id";
    public static final String ATTR_LINE = "line";
    public static final String ATTR_MAJOR = "major";
    public static final String ATTR_MESSAGE_ID = "messageId";
    public static final String ATTR_METHOD = "method";
    public static final String ATTR_MINOR = "minor";
    public static final String ATTR_NAME = "name";
    public static final String ATTR_ORDER = "order";

    public static final String ATTR_OS = "os";
    public static final String ATTR_PARENT = "parent";
    public static final String ATTR_PLANNING_METHOD = "planningmethod";

    public static final String ATTR_PLATFORM = "platform";
    public static final String ATTR_POINT = "point";
    public static final String ATTR_PREFIX = "prefix";

    public static final String ATTR_PARENT_CALLSIGN = "parent_callsign";

    public static final String ATTR_PRODUCTION_TIME = "production_time";
    public static final String ATTR_READINESS = "readiness";
    public static final String ATTR_RELATION = "relation";
    public static final String ATTR_REMARKS = REMARKS;
    public static final String ATTR_ROLE = "role";
    public static final String ATTR_ROUTE_TYPE = "routetype";
    public static final String ATTR_EXTENDED_ROLE = "exrole";
    public static final String ATTR_EXTENDED_ROLE_ABBREVIATION = "abbr";
    public static final String ATTR_USE_TYPE_ICON = "typeicon";
    public static final String ATTR_SENDER_CALLSIGN = "senderCallsign";

    public static final String ATTR_SERVER = "server";
    public static final String ATTR_SOURCE = "source";
    public static final String ATTR_SPEED = "speed";
    public static final String ATTR_STROKE = "stroke";
    public static final String ATTR_TIME = "time";
    public static final String ATTR_TITLE = "title";
    public static final String ATTR_TO = "to";

    public static final String ATTR_TOOL = "tool";

    public static final String ATTR_TRACKING = "tracking";
    public static final String ATTR_TYPE = "type";
    public static final String ATTR_UID = "uid";
    public static final String ATTR_UID_0 = "uid0";
    public static final String ATTR_UID_1 = "uid1";
    public static final String ATTR_VALUE = "value";

    public static final String ATTR_VERSION = "version";


    public static final String VALUE_TYPE_INVITE = "INVITE";

    /**
     *
     */
    public static final String CHAT_ROOT_CONTACT_GROUP = "RootContactGroup";

    /**
     * The "type" attribute value from a link detail indicating it holds style sub-details.
     */
    public static final String LINK_TYPE_KML_STYLE = "b-x-KmlStyle";

    /**
     * The "relation" attribute value from a link detail indicating a parent-producer relationship.
     */
    public static final String LINK_RELATION_PARENT_PRODUCER = "p-p";

    /**
     * The "relation" attribute value from a link detail indicating it specifies another map item as the center point.
     */
    public static final String LINK_RELATION_CENTER_ANCHOR = "p-p-CenterAnchor";

    /**
     * The "relation" attribute value from a link detail indicating it specifies another map item as the radius
     */
    public static final String LINK_RELATION_RADIUS_ANCHOR = "p-p-RadiusAnchor";

    private DetailConstants()
    {
        // Prevent construction
    }
}
