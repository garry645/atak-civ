package gov.tak.platform.symbology.milstd2525;

import gov.tak.test.KernelTest;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;

abstract public class MilStd2525TestBase extends KernelTest
{
    public MilStd2525TestBase()
    {
        
    }

    //some symbols aren't mapped 1 - 1
    ArrayList<String> getExceptions() {
        ArrayList<String> exceptions = new ArrayList<>();
        exceptions.add("WOS-HHDMDFP----");
        exceptions.add("S*UPWM----*****");
        exceptions.add("G*GPGPOP--****X");
        exceptions.add("S*GP------*****");
        return exceptions;
    }

    @Test
    public void testInexact() throws Throwable
    {
        String c = MilStd2525.get2525CFrom2525D("10030100001205110709", false);

        Assert.assertEquals(c, "SFAPC-----*****");

        c = MilStd2525.get2525CFrom2525D("10000100001205110709", false);

        Assert.assertEquals(c, "S*APC-----*****");

    }

    @Test
    public void roundTripCDC() throws Throwable
    {
        ArrayList<String> exceptions = getExceptions();

        for(String orig : MilStd2525.c2d.keySet()) {

            if(exceptions.contains(orig))
                continue;

            String d = MilStd2525.get2525DFrom2525C(orig);
            String c = MilStd2525.get2525CFrom2525D(d);

            org.junit.Assert.assertEquals(orig,c);
        }
    }
    @Test
    public void roundTripCDCFriendly() throws Throwable
    {

        MilStd2525cInterop interop = new MilStd2525cInterop();

        ArrayList<String> exceptions = getExceptions();

        for(String orig : MilStd2525.c2d.keySet()) {

            if(exceptions.contains(orig))
                continue;

            if(!orig.substring(1,2).equals("*"))
                continue;

            String affil = interop.setAffiliation(orig, "F");

            String d = MilStd2525.get2525DFrom2525C(affil);
            String c = MilStd2525.get2525CFrom2525D(d);

            org.junit.Assert.assertEquals(affil,c);
        }
    }

    @Test
    public void roundTripDED() throws Throwable
    {

        final String orig = MilStd2525dSymbologyTableTest.airCivilianFixedWingId;

        String e = MilStd2525.get2525EFrom2525D(orig);
        String d = MilStd2525.get2525DFrom2525E(e);

        org.junit.Assert.assertEquals(orig,d);
    }
}
