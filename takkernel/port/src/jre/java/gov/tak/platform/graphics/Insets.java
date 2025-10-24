
package gov.tak.platform.graphics;

import gov.tak.api.annotation.ModifierApi;

/** @deprecated will no longer derive from {@link java.awt.Insets} */
@ModifierApi(since = "4.9", modifiers = {}, target = "4.12")
public class Insets extends java.awt.Insets
{
    public Insets(int top, int left, int bottom, int right)
    {
        super(top, left, bottom, right);
    }
}
