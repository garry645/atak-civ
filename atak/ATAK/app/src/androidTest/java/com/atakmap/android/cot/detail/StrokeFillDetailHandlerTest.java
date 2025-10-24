
package com.atakmap.android.cot.detail;

import android.graphics.Color;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.atakmap.android.androidtest.ATAKInstrumentedTest;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.cot.event.CotDetail;
import com.atakmap.coremap.cot.event.CotEvent;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.coords.GeoPointMetaData;
import com.atakmap.coremap.maps.coords.MutableGeoBounds;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(AndroidJUnit4.class)
public class StrokeFillDetailHandlerTest extends ATAKInstrumentedTest {

    private Shape createShape() {
        return new Shape("uid") {

            @Override
            public GeoPoint[] getPoints() {
                return new GeoPoint[0];
            }

            @Override
            public GeoPointMetaData[] getMetaDataPoints() {
                return new GeoPointMetaData[0];
            }

            @Override
            public GeoBounds getBounds(MutableGeoBounds bounds) {
                return null;
            }
        };
    }

    @Test
    public void strokefillDetailHandlerTest() {
        StrokeFillDetailHandler sfdh = new StrokeFillDetailHandler();
        CotEvent cotEvent = new CotEvent();
        CotDetail cd = new CotDetail("strokefill");
        cd.setAttribute("name", "strokeColor");
        cd.setAttribute("value", Integer.toString(Color.RED));
        Shape s = createShape();
        sfdh.toItemMetadata(s, cotEvent, cd);
        Assert.assertEquals(0xFF000000 & Color.RED, s.getStrokeColor());
    }
}
