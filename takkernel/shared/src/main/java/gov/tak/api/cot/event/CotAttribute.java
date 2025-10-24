package gov.tak.api.cot.event;

import gov.tak.api.annotation.NonNull;

/**
 * Simple name/value pair to hold attributes of a CoT detail element.
 *
 * @since 6.0.0
 */
public class CotAttribute
{
    private final String name;
    private final String value;

    /**
     * Create a new attribute
     *
     * @param name  the name of the attribute
     * @param value the value of the attribute, if {@code null} will be converted to an empty string
     */
    CotAttribute(@NonNull final String name, final String value)
    {
        this.name = name;
        this.value = value == null ? "" : value;
    }

    /**
     * @return the attribute name
     */
    @NonNull
    public String getName()
    {
        return name;
    }

    /**
     * @return the attribute value as a string
     */
    @NonNull
    public String getValue()
    {
        return value;
    }
}
