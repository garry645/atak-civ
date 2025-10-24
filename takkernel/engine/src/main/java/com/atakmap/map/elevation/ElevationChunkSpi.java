package com.atakmap.map.elevation;

import com.atakmap.math.PointI;
import com.atakmap.spi.ServiceProvider;

import java.nio.ByteBuffer;
import java.util.Map;

import gov.tak.api.util.Disposable;

public interface ElevationChunkSpi
{
    final class Hints
    {
        public PointI tileIndex = null;
        public int srid = -1;
        public Map<String, Object> extras;
    }

    /**
     * @param data The input
     * @return
     */
    ElevationChunk create(ByteBuffer data, Hints hints);
    String getMimeType();
}
