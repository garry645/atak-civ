package gov.tak.platform.symbology.milstd2525;

import android.content.Context;

import java.util.HashMap;
import java.util.Map;

import armyc2.c2sd.renderer.utilities.RendererSettings;
import armyc2.c2sd.renderer.utilities.SymbolDef;
import armyc2.c2sd.renderer.utilities.SymbolDefTable;
import armyc2.c2sd.renderer.utilities.UnitDef;
import armyc2.c2sd.renderer.utilities.UnitDefTable;
import gov.tak.platform.commons.resources.AndroidResourceManager;

final class MilStd2525cSymbolTable extends MilStd2525cSymbolTableBase<SymbolDef>
{
    static void init(Context context)
    {
        parseDescriptions(new AndroidResourceManager(context));
    }

    @Override
    Map<String, SymbolDef> getAllSymbolDefs()
    {
        Map<String, SymbolDef> symbols = SymbolDefTable.getInstance().GetAllSymbolDefs(RendererSettings.Symbology_2525C);
        Map<String, UnitDef> units = UnitDefTable.getInstance().getAllUnitDefs(RendererSettings.Symbology_2525C);

        Map<String, SymbolDef> all = new HashMap<>(symbols.size()+units.size());
        all.putAll(symbols);
        for(Map.Entry<String, UnitDef> unit : units.entrySet()) {
            // NOTE: upstream contains some entries that are erroneously mapped to both signal and unit
            if(all.containsKey(unit.getKey())) continue;
            all.put(unit.getKey(), MilStd2525cInterop.toSymbolDef(unit.getValue()));
        }
        return all;
    }
}
