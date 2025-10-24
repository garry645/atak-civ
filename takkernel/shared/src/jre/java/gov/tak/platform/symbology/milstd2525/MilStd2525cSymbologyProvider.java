package gov.tak.platform.symbology.milstd2525;

import ArmyC2.C2SD.Utilities.MilStdSymbol;
import ArmyC2.C2SD.Utilities.ShapeInfo;
import ArmyC2.C2SD.Utilities.SymbolDef;

public final class MilStd2525cSymbologyProvider extends MilStd2525cSymbologyProviderBase<SymbolDef, MilStdSymbol, ShapeInfo>
{

    public MilStd2525cSymbologyProvider()
    {
        super(new MilStd2525cInterop());
    }
}
