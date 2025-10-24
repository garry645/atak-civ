package gov.tak.platform.symbology.milstd2525;

import android.content.Context;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;

import armyc2.c2sd.renderer.utilities.SymbolDefTable;
import armyc2.c2sd.renderer.utilities.UnitDefTable;
import gov.tak.platform.commons.resources.AndroidResourceManager;

public final class MilStd2525 extends MilStd2525Base
{
    private MilStd2525(){}

    static public void init(Context context) {
        if(init(new AndroidResourceManager(context))) {
            long s = System.currentTimeMillis();
            // 2525C init
            SymbolDefTable.getInstance().init(context);
            UnitDefTable.getInstance().init(context);
            final File cacheDir = FileSystemUtils.getItem("tools/milsym/cache");
            if(!cacheDir.exists())
                cacheDir.mkdirs();
            armyc2.c2sd.renderer.MilStdIconRenderer.getInstance().init(context, cacheDir.getAbsolutePath());
            // XXX - R8 re-packaging is preventing `MilStd2525` from invoking
            //       `MilStd2525cSymbolTable`. We'll do a recursive invocation here which will skip
            //       the primary invocation and give us access to initialize the table
            //MilStd2525cSymbolTable.init(context);
            MilStd2525cSymbologyProvider.init(context);
            // 2525D+ init
            armyc2.c5isr.renderer.MilStdIconRenderer.getInstance().init(context);
            MilStd2525dSymbolTable.init(context);
            long e = System.currentTimeMillis();

//            Exception stack = null;
//            try {
//                throw new RuntimeException();
//            } catch(Exception t) {
//                stack = t;
//            }
//            android.util.Log.i("MilStd2525", "providers init in " + (e-s) + "ms");
        }
    }

    static void init() {
        // no-op -- explicit Context based initialization required for Andorid runtime
    }
}
