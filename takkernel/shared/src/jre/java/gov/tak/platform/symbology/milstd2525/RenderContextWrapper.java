package gov.tak.platform.symbology.milstd2525;

import ArmyC2.C2SD.Rendering.JavaRenderer;
import gov.tak.api.annotation.NonNull;


// ArmyC2.C2SD.Utilities.RenderSettings is for 2525C
// armyc2.c5isr.renderer.utilities.RendererSettings is for 2525D

class RenderContextWrapper {

    static String prevFontNameC;
    static int prevFontSizeC;
    static int prevFontTypeC;

    static String prevFontNameD;
    static int prevFontSizeD;
    static int prevFontTypeD;


    public synchronized static void setLabelFont(@NonNull String name, int type, int size) {
        if (!name.equals(prevFontNameC) || type != prevFontTypeC || size != prevFontSizeC) {
            //calls both ArmyC2.C2SD.Utilities.RenderSettings.setLabelFont() and SinglePointRenderer.RefreshModifierFont()
            JavaRenderer.getInstance().setModifierFont(name, type, size);
            prevFontNameC = name;
            prevFontSizeC = size;
            prevFontTypeC = type;
        }
    }


    public synchronized static void setLabelFontD(@NonNull String name, int type, int size) {
        if (!name.equals(prevFontNameD) || type != prevFontTypeD || size != prevFontSizeD) {
            armyc2.c5isr.renderer.utilities.RendererSettings.getInstance().setLabelFont(name, type, size);
            prevFontNameD = name;
            prevFontSizeD = size;
            prevFontTypeD = type;

        }
    }
}
