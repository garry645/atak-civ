package com.atakmap.android.maps;

import android.graphics.Typeface;
import gov.tak.test.KernelJniTest;
import org.junit.Assert;
import org.junit.Test;

public class MapTextFormatTest extends KernelJniTest
{
    private void constructor_typeface_roundtrip(Typeface typeface, int size)
    {
        MapTextFormat f = new MapTextFormat(typeface, size);
        Assert.assertEquals(typeface, f.getTypeface());
        Assert.assertEquals(size, f.getFontSize());
    }
    @Test
    public void constructor_typeface_roundtrip()
    {
        constructor_typeface_roundtrip(Typeface.DEFAULT, 16);
        constructor_typeface_roundtrip(Typeface.DEFAULT_BOLD, 16);
        constructor_typeface_roundtrip(Typeface.defaultFromStyle(Typeface.BOLD), 16);
        constructor_typeface_roundtrip(Typeface.defaultFromStyle(Typeface.ITALIC), 16);
        constructor_typeface_roundtrip(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC), 16);
        constructor_typeface_roundtrip(Typeface.defaultFromStyle(Typeface.NORMAL), 16);
    }
}
