package gov.tak.platform.symbology.milstd2525;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ShapeType;
import gov.tak.platform.marshal.MarshalManager;
import net.bytebuddy.implementation.bytecode.Throw;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.EnumSet;

public class MilStd2525dSymbologyTableTest extends MilStd2525dSymbologyTableTestBase
{
    final static String forwardLineOfTroopsId = "10002500001401000000";
    final static String airCivilianFixedWingId = "10000100001201000000";

    @Override
    ISymbologyProvider newInstance() {
        return new MilStd2525dSymbologyProvider();
    }

    @Override
    String getForwardLineOfTroopsId() {
        return forwardLineOfTroopsId;
    }

    @Override
    String getAirCivilianFixedWingId() {
        return airCivilianFixedWingId;
    }
}
