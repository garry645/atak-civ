
package com.atakmap.map.layer.raster.tilereader.opengl;

import java.nio.ByteBuffer;
import java.util.Comparator;

import com.atakmap.map.layer.raster.gdal.GdalTileReader;
import com.atakmap.map.projection.PointL;
import com.atakmap.util.ResourcePool;

final class GLQuadTileNode3 {
    final static int STATE_RESOLVED = 0x01;
    final static int STATE_RESOLVING = 0x02;
    final static int STATE_UNRESOLVED = 0x04;
    final static int STATE_UNRESOLVABLE = 0x08;
    final static int STATE_SUSPENDED = 0x10;

    final static int[] POI_ITERATION_BIAS =
            {
                    1, 3, 0, 2, // 0
                    1, 0, 2, 3, // 1
                    0, 1, 2, 3, // 2
                    0, 2, 1, 3, // 3
                    2, 0, 3, 1, // 4
                    2, 3, 0, 1, // 5
                    3, 2, 1, 0, // 6
                    3, 1, 2, 0, // 7
            };

    static boolean offscreenFboFailed = false;

    final static double POLE_LATITUDE_LIMIT_EPISLON = 0.00001d;

    final static Comparator<PointL> POINT_COMPARATOR = new Comparator<PointL>()
    {
        @Override
        public int compare(PointL p0, PointL p1)
        {
            long retval = p0.y - p1.y;
            if (retval == 0L)
                retval = p0.x - p1.x;
            if (retval > 0L)
                return 1;
            else if (retval < 0L)
                return -1;
            else
                return 0;
        }
    };

    static boolean mipmapEnabled = false;

    /*************************************************************************/

    final static boolean DEBUG = false;

    static
    {
        GdalTileReader.setPaletteRgbaFormat(GdalTileReader.Format.RGBA);
    }

    final static int TEXTURE_CACHE_HINT_RESOLVED = 0x00000001;

    final static int MAX_GRID_SIZE = 32;

    // 8MB reserved transfer buffer
    final static ResourcePool<ByteBuffer> transferBuffers = new ResourcePool<>(32);
}
