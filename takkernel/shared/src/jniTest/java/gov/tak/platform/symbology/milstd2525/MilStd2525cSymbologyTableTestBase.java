package gov.tak.platform.symbology.milstd2525;

import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ShapeType;
import gov.tak.test.KernelTest;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.EnumSet;

abstract class MilStd2525cSymbologyTableTestBase extends KernelTest
{
    final static String forwardLineOfTroopsId = "G*G*GLF---****X";
    final static String airCivilianFixedWingId = "S*A*C-----*****";

    @Test
    public void getSymbol_valid_code() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        ISymbolTable.Symbol sym = milsym.getSymbolTable().getSymbol(forwardLineOfTroopsId);
        Assert.assertNotNull(sym);
    }

    @Test
    public void getSymbol_invalid_code() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        ISymbolTable.Symbol sym = milsym.getSymbolTable().getSymbol("xxxx");
        Assert.assertNull(sym);
    }

    @Test
    public void getSymbol_null_code() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        ISymbolTable.Symbol sym = milsym.getSymbolTable().getSymbol(null);
        Assert.assertNull(sym);
    }

    @Test
    public void getPreviewDrawable_unit() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        ISymbolTable.Symbol sym = milsym.getSymbolTable().getSymbol(airCivilianFixedWingId);
        Assert.assertNotNull(sym);
        Drawable preview = sym.getPreviewDrawable(0);
        Assert.assertNotNull(preview);
    }

    @Test
    public void getPreviewDrawable_multipoint() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        ISymbolTable.Symbol sym = milsym.getSymbolTable().getSymbol(forwardLineOfTroopsId);
        Assert.assertNotNull(sym);
        Drawable preview = sym.getPreviewDrawable(0);
        Assert.assertNotNull(preview);
    }

    @Test
    public void find_null_filter() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        Collection<ISymbolTable.Symbol> syms = milsym.getSymbolTable().find("forward line", null);
        Assert.assertNotNull(syms);
        Assert.assertFalse(syms.isEmpty());

        boolean found = false;
        for(ISymbolTable.Symbol sym : syms) {
            found |= sym.getCode().equals(forwardLineOfTroopsId);
        }
        Assert.assertTrue(found);
    }

    @Test
    public void find_matching_filter() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        Collection<ISymbolTable.Symbol> syms = milsym.getSymbolTable().find("forward line", EnumSet.of(ShapeType.LineString));
        Assert.assertNotNull(syms);
        Assert.assertFalse(syms.isEmpty());

        boolean found = false;
        for(ISymbolTable.Symbol sym : syms) {
            found |= sym.getCode().equals(forwardLineOfTroopsId);
        }
        Assert.assertTrue(found);
    }

    @Test
    public void find_non_matching_filter() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        Collection<ISymbolTable.Symbol> syms = milsym.getSymbolTable().find("forward line", EnumSet.of(ShapeType.Point));
        Assert.assertNotNull(syms);
        Assert.assertTrue(syms.isEmpty());
    }

    @Test
    public void getSummary() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        ISymbolTable.Symbol sym = milsym.getSymbolTable().getSymbol(forwardLineOfTroopsId);
        Assert.assertNotNull(sym);
        Assert.assertNotNull(sym.getSummary());
    }


    @Test
    public void getRoot() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        ISymbolTable.Folder root = milsym.getSymbolTable().getRoot();
        Assert.assertNotNull(root);
        Assert.assertFalse(root.getChildren().isEmpty());
    }

    private static boolean recurseUntilSymbol(ISymbolTable.Folder f) {
        if(!f.getSymbols().isEmpty())
            return true;
        for(ISymbolTable.Folder c : f.getChildren())
            if(recurseUntilSymbol(c))
                return true;
        return false;
    }

    @Test
    public void getRoot_recurse_has_symbols() {
        MilStd2525cSymbologyProvider milsym = new MilStd2525cSymbologyProvider();
        ISymbolTable.Folder root = milsym.getSymbolTable().getRoot();
        Assert.assertTrue(recurseUntilSymbol(root));
    }
}
