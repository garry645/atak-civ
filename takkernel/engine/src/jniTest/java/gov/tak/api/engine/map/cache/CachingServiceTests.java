
package gov.tak.api.engine.map.cache;

import gov.tak.api.engine.map.features.LineString;
import com.atakmap.map.layer.raster.tilematrix.*;

import com.atakmap.map.layer.feature.geometry.Envelope;
import gov.tak.api.engine.map.features.Point;
//import com.atakmap.map.layer.feature.geometry.Envelope;
//import com.atakmap.map.layer.feature.geometry.Point;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import gov.tak.api.engine.map.features.Polygon;
import gov.tak.test.KernelJniTest;

import static org.junit.Assert.assertEquals;

public class CachingServiceTests extends KernelJniTest {
    public class StatusListener implements CachingService.IStatusListener
    {
        boolean[] complete;
        public StatusListener(boolean[] c)
        {
            complete = c;
        }

        @Override
        public void statusUpdate(CachingService.Status status) {
            if(status.status != CachingService.StatusType.Complete)
                return;

            synchronized (this) {
                complete[0] = true;
                this.notifyAll();
            }
        }
    }

    class MockTileClientSpi implements TileClientSpi
    {
        TileMatrix.ZoomLevel[] _zooms;
        public MockTileClientSpi()
        {
            _zooms = new TileMatrix.ZoomLevel[4];
            for (int i = 0; i < 4; i++)
            {
                _zooms[i] = new TileMatrix.ZoomLevel();
                _zooms[i].level = i;
                _zooms[i].pixelSizeX = 4.0 / (i + 1);
                _zooms[i].pixelSizeY = 4.0 / (i + 1);
                _zooms[i].resolution = 4.0 / (i + 1);
                _zooms[i].tileWidth = 10 * (i + 1);
                _zooms[i].tileHeight = 10 * (i + 1);
            }
        }
        @Override
        public int getPriority() {
            return 1;
        }

        @Override
        public String getName() {
            return "CustomClient";
        }

        @Override
        public TileClient create(String path, String offlineCachePath, Options opts) {
            return new MockTileClientJNI(getName(), 4326, _zooms, 0, 0,
                    new Envelope(0, 0, 0, 20, 20, 20));
        }
    }

    class MockTileContainerSpi implements TileContainerSpi
    {
        TileMatrix.ZoomLevel[] _zooms;
        public MockTileContainerSpi()
        {
            _zooms = new TileMatrix.ZoomLevel[4];
            for (int i = 0; i < 4; i++)
            {
                _zooms[i] = new TileMatrix.ZoomLevel();
                _zooms[i].level = i;
                _zooms[i].pixelSizeX = 4.0 / (i + 1);
                _zooms[i].pixelSizeY = 4.0 / (i + 1);
                _zooms[i].resolution = 4.0 / (i + 1);
                _zooms[i].tileWidth = 10 * (i + 1);
                _zooms[i].tileHeight = 10 * (i + 1);
            }
        }

        @Override
        public String getName() {
            return "MockTileContainer";
        }

        @Override
        public String getDefaultExtension() {
            return "mtc";
        }

        @Override
        public TileContainer create(String name, String path, TileMatrix spec) {
            return new MockTileContainer(name, 4326, _zooms, 0, 0,
                    new Envelope(0, 0, 0, 20, 20, 20), false);
        }

        @Override
        public TileContainer open(String path, TileMatrix spec, boolean readOnly) {
            return create("MockedOpen", path, spec);
        }

        @Override
        public boolean isCompatible(TileMatrix spec) {
            return true;
        }
    }
    @Test
    //sanity test jni round trip
    public void test()
    {
        TileClientSpi spi = new MockTileClientSpi();
        TileClientFactory.registerSpi(spi);
        TileContainerFactory.registerSpi(new MockTileContainerSpi());
        boolean[] complete = { false };
        StatusListener sl = new StatusListener(complete);
        List<String> source = new ArrayList<>();
        source.add("CustomClient");
        List<String> sink = new ArrayList<>();
        sink.add(System.getenv("TEMP") + "\\tcf-test-create-cache.mtc");

        TileClient result = TileClientFactory.create("CustomClient", null, new TileClientSpi.Options());

        CachingService.Request req = new CachingService.Request();

        Envelope e = result.getBounds();

        LineString mbr = new LineString(2);
        mbr.addPoint(e.minX, e.minY);
        mbr.addPoint(e.minX, e.maxY);
        mbr.addPoint(e.maxX, e.maxY);
        mbr.addPoint(e.maxX, e.minY);
        mbr.addPoint(e.minX, e.minY);

        req.geom = new Polygon(mbr);
        req.id = 1;
        req.sourcePaths = source;
        req.sinkPaths = sink;
        req.minRes = 0;
        req.maxRes = 0;
        req.priority = 1;

        CachingService.downloadRequest(req, sl);
        synchronized (sl) {
            try {
                sl.wait(5000);
            }
            catch(Exception exception) {
            }
        }
        assertEquals(complete[0], true);
    }
    @Test
    public void testEmptyDefaults()
    {
        TileClientSpi spi = new MockTileClientSpi();
        TileClientFactory.registerSpi(spi);
        boolean[] complete = { false };
        StatusListener sl = new StatusListener(complete);

        TileClient result = TileClientFactory.create("CustomClient", null, new TileClientSpi.Options());

        CachingService.Request req = new CachingService.Request();

        Envelope e = result.getBounds();

        LineString mbr = new LineString(2);
        mbr.addPoint(e.minX, e.minY);
        mbr.addPoint(e.minX, e.maxY);
        mbr.addPoint(e.maxX, e.maxY);
        mbr.addPoint(e.maxX, e.minY);
        mbr.addPoint(e.minX, e.minY);

        req.geom = new Polygon(mbr);
        req.id = 1;
        req.sourcePaths = null;
        req.sinkPaths = null;
        req.minRes = 0;
        req.maxRes = 0;
        req.priority = 1;

        try {
            CachingService.downloadRequest(req, sl);
            Assert.fail("Didn't throw");
        }
        catch(IllegalArgumentException argumentException)
        {

        }
    }
}
