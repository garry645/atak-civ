
package com.atakmap.android.cot;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.maps.Marker;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.AccessUtils;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class MarkerMarkingTest extends ATAKInstrumentedTest {

    @Test
    public void markingTest() {

        AtakPreferences.getInstance(null)
                .set(AccessUtils.MAPITEM_ACCESS_PREF_KEY, "Unclassified");
        AtakPreferences.getInstance(null)
                .set(AccessUtils.MAPITEM_CAVEAT_PREF_KEY, "XYZ");

        Marker m = new Marker("test");
        String access = m.getMetaString("access", "");
        Assert.assertEquals(access, "Unclassified");

        String caveat = m.getMetaString("caveat", "");
        Assert.assertEquals(caveat, "XYZ");

        AtakPreferences.getInstance(null)
                .remove(AccessUtils.MAPITEM_ACCESS_PREF_KEY);
        AtakPreferences.getInstance(null)
                .remove(AccessUtils.MAPITEM_CAVEAT_PREF_KEY);
        m = new Marker("test");
        access = m.getMetaString("access", "");
        Assert.assertEquals(access, "Undefined");

        caveat = m.getMetaString("caveat", "");
        Assert.assertEquals(caveat, "");

    }
}
