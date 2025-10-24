
package com.atakmap.lang;

/**
 * Static utility functions for general operations on objects.
 *
 * @author Developer
 */
public final class Objects
{
    private Objects()
    {
    }

    /**
     * Checks the two specified objects for equality. The objects are considered
     * equal if they are both <code>null</code> or if <code>o1.equals(o2)</code>
     * returns <code>true</code>.
     *
     * @param o1 An object (may be <code>null</code>)
     * @param o2 An object (may be <code>null</code>)
     * @return <code>true</code> if the specified objects are equal,
     * <code>false</code> otherwise.
     */
    public static <T> boolean equals(T o1, T o2)
    {
        return (o1 == null && o2 == null) ||
                (o1 != null && o2 != null && ((o1 == o2) || o1.equals(o2)));
    }

    public static boolean equals(double o1, double o2)
    {
        final boolean nan1 = Double.isNaN(o1);
        final boolean nan2 = Double.isNaN(o2);
        return (nan1 && nan2) || (o1 == o2);
    }
} // Objects
