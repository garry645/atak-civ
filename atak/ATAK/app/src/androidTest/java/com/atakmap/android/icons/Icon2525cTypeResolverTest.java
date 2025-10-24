
package com.atakmap.android.icons;

import static org.junit.Assert.assertEquals;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;

import org.junit.Test;
import org.junit.runner.RunWith;

import gov.tak.api.cot.CotUtils;

@RunWith(AndroidJUnit4.class)
public class Icon2525cTypeResolverTest extends ATAKInstrumentedTest {
    // see https://spatialillusions.com for the explanation of SIDC
    // also please see tak-assets/mil-std-2525c
    // and tak-assets/tools/assetbuilder/2525/
    // along with the human name tree in the ATAK tree
    // ATAK/app/src/main/assets/symbols.dat


    @Test
    public void friendlyTest() {
        assertEquals("sf-------------",
                CotUtils.mil2525cFromCotType("a-f"));
    }

    @Test
    public void unknownTest() {
        assertEquals("su-------------",
                CotUtils.mil2525cFromCotType("a-u"));
    }
    @Test
    public void neutralTest() {
        assertEquals("sn-------------",
                CotUtils.mil2525cFromCotType("a-n"));
    }
    @Test
    public void hostileTest() {
        assertEquals("sh-------------",
                CotUtils.mil2525cFromCotType("a-h"));
    }

    @Test
    public void friendlyTest1() {
        assertEquals("sfap-----------",
                CotUtils.mil2525cFromCotType("a-f-A"));
    }

    @Test
    public void unknownTest1() {
        assertEquals("suap-----------",
                CotUtils.mil2525cFromCotType("a-u-A"));
    }

    @Test
    public void neutralTest1() {
            assertEquals("snap-----------",
                    CotUtils.mil2525cFromCotType("a-n-A"));
    }

    @Test
    public void hostileTest1() {
        assertEquals("shap-----------",
                CotUtils.mil2525cFromCotType("a-h-A"));
    }

    @Test
    public void friendlyTest2() {
        assertEquals("sfgpi-----h----",
                CotUtils.mil2525cFromCotType("a-f-G-i"));
    }

    @Test
    public void hostileTest2() {
        // oddball not standard but will work
        assertEquals("sh-------------",
                CotUtils.mil2525cFromCotType("a,h"));

    }

}
