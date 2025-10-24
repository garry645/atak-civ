package gov.tak.api.engine.map.cache;

import org.junit.Assert;
import org.junit.Test;
import org.mockito.Mockito;

import gov.tak.api.engine.map.features.Geometry;
import gov.tak.api.engine.map.features.LineString;
import gov.tak.test.KernelJniTest;

public class SmartCacheTriggerTest extends KernelJniTest {
    @Test
    public void test_missing_geom() {
        try {
            new SmartCacheTrigger(null, 1, 0.0, 0.0,
                    "cacheSrc", "cacheSink");
            Assert.fail("didn't throw");
        } catch (IllegalArgumentException e) {
            // pass
        } catch (Throwable th) {
            Assert.fail("wrong throw" + th);
        }
    }

    @Test
    public void test_roundtrip_values() {
        Geometry geom = new LineString(2);
        int priority = 1;
        double minRes = 0.0;
        double maxRes = 1.0;
        String cacheSrc = "cs";
        String cacheSink = "sink";

        SmartCacheTrigger event = new SmartCacheTrigger(geom, priority, minRes, maxRes,
                cacheSrc, cacheSink);
        Assert.assertNotSame(event.getGeometry(), geom);
        Assert.assertEquals(event.getGeometry(), geom);
        Assert.assertEquals(event.getPriority(), priority);
        Assert.assertEquals(event.getMinResolution(), minRes, minRes);
        Assert.assertEquals(event.getMaxResolution(), maxRes, maxRes);
        Assert.assertEquals(event.getCacheSource(), cacheSrc);
        Assert.assertEquals(event.getCacheSink(), cacheSink);
    }
}
