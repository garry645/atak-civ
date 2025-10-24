package gov.tak.api.engine.map.cache;

import com.atakmap.util.Collections2;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;

import gov.tak.test.KernelJniTest;

public class UpdateCacheSourceTest extends KernelJniTest {
    @Test
    public void test_missing_cacheSrc() {
        try {
            new UpdateCacheSource(null, new String[] {"bathroom"});
            Assert.fail("didn't throw");
        } catch (IllegalArgumentException e) {
            // pass
        } catch (Throwable th) {
            Assert.fail("wrong throw" + th);
        }
    }

    @Test
    public void test_missing_defSink() {
        try {
            String[] cacheSrcs = { "one", "two" };
            new UpdateCacheSource(cacheSrcs, null);
            Assert.fail("didn't throw");
        } catch (IllegalArgumentException e) {
            // pass
        } catch (Throwable th) {
            Assert.fail("wrong throw" + th);
        }
    }

    @Test
    public void test_missing_cacheSrc_elem() {
        try {
            String[] cacheSrcs = { null, "two" };
            new UpdateCacheSource(cacheSrcs, new String[] {"foo","bar"});
            Assert.fail("didn't throw");
        } catch (IllegalArgumentException e) {
            // pass
        } catch (Throwable th) {
            Assert.fail("wrong throw" + th);
        }
    }

    @Test
    public void test_routrip_values() {
        String[] cacheSrcs = { "one", "two" };
        String[] defSnk = { "kitchen", "bathroom" };
        UpdateCacheSource event = new UpdateCacheSource(cacheSrcs, defSnk);
        Assert.assertEquals(cacheSrcs.length, event.getCacheSources().size());
        for(int i = 0; i < cacheSrcs.length; i++)
            Assert.assertTrue(event.getCacheSources().contains(cacheSrcs[i]));
        for(int i = 0; i < cacheSrcs.length; i++)
            Assert.assertEquals(defSnk[i], event.getDefaultSink(cacheSrcs[i]));
    }
}
