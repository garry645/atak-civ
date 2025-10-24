package gov.tak.platform.symbology.milstd2525;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.ISymbologyProvider2;
import gov.tak.test.KernelTest;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;

import java.util.EnumSet;

public abstract class MilStd2525SymbologyProviderTestBase extends KernelTest
{
    abstract ISymbologyProvider newInstance();
    abstract boolean expectedHasSinglePoint();
    abstract boolean expectedHasMultiPoint();

    abstract String expectedProviderName();

    abstract EnumSet<Affiliation> supportedAffiliations();

    EnumSet<Amplifier> supportedAmplifiers() {
        return EnumSet.allOf(Amplifier.class);
    }

    int[] supportedHqTfDummyMasks() {
        return new int[]
                {
                        ISymbologyProvider2.MASK_HEADQUARTERS,
                        ISymbologyProvider2.MASK_TASKFORCE,
                        ISymbologyProvider2.MASK_TASKFORCE|ISymbologyProvider2.MASK_HEADQUARTERS,
                        ISymbologyProvider2.MASK_DUMMY_FEINT,
                        ISymbologyProvider2.MASK_DUMMY_FEINT|ISymbologyProvider2.MASK_HEADQUARTERS,
                        ISymbologyProvider2.MASK_DUMMY_FEINT|ISymbologyProvider2.MASK_TASKFORCE,
                        ISymbologyProvider2.MASK_DUMMY_FEINT|ISymbologyProvider2.MASK_TASKFORCE|ISymbologyProvider2.MASK_HEADQUARTERS,
                };
    }
    
    @Test
    public void hasSinglePoint() {
        ISymbologyProvider milsym = newInstance();
        Assert.assertEquals(expectedHasSinglePoint(), milsym.hasSinglePoint());
    }

    @Test
    public void hasMultiPoint() {
        ISymbologyProvider milsym = newInstance();
        Assert.assertEquals(expectedHasMultiPoint(), milsym.hasMultiPoint());
    }

    @Test
    public void getName() {
        ISymbologyProvider milsym = newInstance();
        Assert.assertNotNull(milsym.getName());
        Assert.assertEquals(expectedProviderName(), milsym.getName());
    }

    protected void affiliationRoundtrip(String code) {
        ISymbologyProvider milsym = newInstance();
        for(Affiliation affiliation : supportedAffiliations()) {
            final String affiliated = milsym.setAffiliation(code, affiliation);
            final Affiliation derived = milsym.getAffiliation(affiliated);
            Assert.assertEquals(affiliated + " Expected: " + affiliation + " Actual: " + derived, affiliation, derived);
        }
    }

    protected void amplifierRoundtrip(String code) {
        ISymbologyProvider milsym = newInstance();
        Assume.assumeTrue(milsym instanceof ISymbologyProvider2);
        for(Amplifier amplifier : supportedAmplifiers()) {
            final String amplified = ((ISymbologyProvider2)milsym).setAmplifier(code, amplifier);
            final Amplifier derived = ((ISymbologyProvider2)milsym).getAmplifier(amplified);
            Assert.assertEquals(amplified + " Expected: " + amplifier + " Actual: " + derived, amplifier, derived);
        }
    }

    protected void hqTfDummyMaskRoundtrip(String code) {
        ISymbologyProvider milsym = newInstance();
        Assume.assumeTrue(milsym instanceof ISymbologyProvider2);
        for(int hqTfDummy : supportedHqTfDummyMasks()) {
            final String updated = ((ISymbologyProvider2)milsym).setHeadquartersTaskForceDummyMask(code, hqTfDummy);
            final int derived = ((ISymbologyProvider2)milsym).getHeadquartersTaskForceDummyMask(updated);
            Assert.assertEquals(updated + " Expected: " + hqTfDummy + " Actual: " + derived, hqTfDummy, derived);
        }
    }
}
