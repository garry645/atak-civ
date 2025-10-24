package gov.tak.platform.symbology.milstd2525;

import ArmyC2.C2SD.Utilities.RendererSettings;
import ArmyC2.C2SD.Utilities.SymbolDef;
import ArmyC2.C2SD.Utilities.SymbolDefTable;
import ArmyC2.C2SD.Utilities.UnitDef;
import ArmyC2.C2SD.Utilities.UnitDefTable;
import gov.tak.platform.commons.resources.JavaResourceManager;

import java.util.HashMap;
import java.util.Map;

final class MilStd2525cSymbolTable extends MilStd2525cSymbolTableBase<SymbolDef>
{
    @Override
    Map<String, SymbolDef> getAllSymbolDefs() {
        if(symbolSummary.isEmpty())
            parseDescriptions(new JavaResourceManager());

        Map<String, SymbolDef> symbols = SymbolDefTable.getInstance().GetAllSymbolDefs(RendererSettings.Symbology_2525C);
        Map<String, UnitDef> units = UnitDefTable.getInstance().GetAllUnitDefs(RendererSettings.Symbology_2525C);

        Map<String, SymbolDef> all = new HashMap<>(symbols.size()+units.size());
        for(Map.Entry<String, SymbolDef> symbol : symbols.entrySet()) {
            all.put(symbol.getKey(), MilStd2525cInterop.filter(symbol.getValue()));
        }
        for(Map.Entry<String, UnitDef> unit : units.entrySet()) {
            // NOTE: upstream contains some entries that are erroneously mapped to both signal and unit
            if(all.containsKey(unit.getKey())) continue;
            all.put(unit.getKey(), MilStd2525cInterop.toSymbolDef(unit.getValue()));
        }

        return all;
    }
}
