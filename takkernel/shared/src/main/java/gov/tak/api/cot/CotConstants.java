package gov.tak.api.cot;

/**
 * Constants used in CoT message handling.
 *
 * @since 6.0.0
 */
public class CotConstants
{
    /**
     * Value indicating an "unknown" numeric value in a CoT message.
     */
    public static final double UNKNOWN_VALUE = 9999999;

    /**
     * HOW: value for machine generated
     */
    public static final String HOW_MACHINE_GENERATED_PREFIX = "m-g";

    /**
     * HOW: value for machine generated (GPS) lost
     */
    public static final String HOW_GPS_LOST_PREFIX = "m-g-l";

    /**
     * HOW: value for Machine generated garbage
     */
    public static final String HOW_MACHINE_GENERATED_GARBAGE = "m-g-g";

    /**
     * HOW: value for Human Entered
     */
    public static final String HOW_HUMAN_ENTERED = "h-e";

    /**
     * HOW: Prefix for any HOW that is Human created
     */
    public static final String HOW_HUMAN_PREFIX = "h-";

    /**
     * HOW: value for human generated garbage
     */
    public static final String HOW_HUMAN_GARBAGE_IN_GARBAGE_OUT = "h-g-i-g-o";

    /**
     * MIME type indicating a CoT message.
     */
    public static final String MIME_TYPE = "application/cot+xml";

    private CotConstants()
    {
        /* no instances */
    }
}
