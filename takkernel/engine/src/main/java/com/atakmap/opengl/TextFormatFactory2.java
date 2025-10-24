package com.atakmap.opengl;

import com.atakmap.android.maps.MapTextFormat;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.DontObfuscate;

/** @deprecated removed without replacement; the library fully implements */
@Deprecated
@DeprecatedApi(since = "5.4", forRemoval = true, removeAt = "5.7")
@DontObfuscate
public interface TextFormatFactory2 extends TextFormatFactory
{
    /**
     * @param fontFace Font face name.
     * @param isBold   Flag indicating if the typeface is bold.
     * @param isItalic Flag indicating if the typeface is italic.
     * @param fontSize Font size.
     * @return MapTextFormat object with default typeface and characteristics determined by input parameters.
     */
    MapTextFormat createTextFormat(String fontFace, boolean isBold, boolean isItalic, int fontSize, boolean isUnderline, boolean isStrikethrough);
}
