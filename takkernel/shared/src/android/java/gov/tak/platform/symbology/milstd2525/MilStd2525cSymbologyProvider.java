package gov.tak.platform.symbology.milstd2525;

import android.content.Context;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;

import armyc2.c2sd.renderer.MilStdIconRenderer;
import armyc2.c2sd.renderer.utilities.MilStdSymbol;
import armyc2.c2sd.renderer.utilities.ShapeInfo;
import armyc2.c2sd.renderer.utilities.SymbolDef;
import armyc2.c2sd.renderer.utilities.SymbolDefTable;
import armyc2.c2sd.renderer.utilities.UnitDefTable;
import gov.tak.api.annotation.DeprecatedApi;

public final class MilStd2525cSymbologyProvider extends MilStd2525cSymbologyProviderBase<SymbolDef, MilStdSymbol, ShapeInfo>
{
    /** @deprecated use {@link MilStd2525#init(Context)} */
    @Deprecated
    @DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
    public static void init(Context context) {
        MilStd2525.init(context);

        // XXX - R8 re-packaging is preventing `MilStd2525` from invoking `MilStd2525cSymbolTable`
        MilStd2525cSymbolTable.init(context);
    }

    public MilStd2525cSymbologyProvider()
    {
        super(new MilStd2525cInterop());
    }
}