package com.atakmap.map.formats.mapbox;

import android.graphics.Bitmap;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationChunkSpi;
import com.atakmap.map.elevation.ElevationData;
import com.atakmap.map.layer.feature.geometry.GeometryFactory;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.raster.osm.OSMUtils;
import com.atakmap.util.ResourcePool;

import org.json.JSONObject;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

public final class TerrainRGBChunkSpi implements ElevationChunkSpi
{
    public final static String MIME_TYPE = "application/vnd.mapbox-terrain-rgb";

    final static ResourcePool<Bitmap> chunkBitmapPool = new ResourcePool<>(4);

    @Override
    public ElevationChunk create(ByteBuffer data, Hints hints) {
        if(data == null)
            return null;

        byte[] dataArray;
        int off;
        int len;
        if (data.hasArray()) {
            dataArray = data.array();
            off = data.position();
            len = data.remaining();
        } else {
            dataArray = new byte[data.remaining()];
            data.duplicate().get(dataArray);
            off = 0;
            len = data.remaining();

        }
        Metadata metadata = new Metadata();
        defaultMetadata(hints, metadata);
        blobMetadata(dataArray, off, len, metadata);

        // require a valid tile index
        if(metadata.tx < 0 || metadata.ty < 0 || metadata.tz < 0)
            return null;

        final double uly;
        final double ulx;
        final double lry;
        final double lrx;
        if(metadata.srid == 3857) {
            uly = OSMUtils.mapnikTileLat(metadata.tz, metadata.ty);
            ulx = OSMUtils.mapnikTileLng(metadata.tz, metadata.tx);
            lry = OSMUtils.mapnikTileLat(metadata.tz, metadata.ty + 1);
            lrx = OSMUtils.mapnikTileLng(metadata.tz, metadata.tx + 1);
        } else if(metadata.srid == 4326) {
            final double tileSizeDegrees = (180d/(double)(1<<metadata.tz));
            uly = 90d - (metadata.ty*tileSizeDegrees);
            ulx = -180d + (metadata.tx*tileSizeDegrees);
            lry = uly - tileSizeDegrees;
            lrx = ulx + tileSizeDegrees;
        } else if(metadata.srid == 3395) {
            uly = WorldMercator.tileLat(metadata.tz, metadata.ty);
            ulx = WorldMercator.tileLng(metadata.tz, metadata.tx);
            lry = WorldMercator.tileLat(metadata.tz, metadata.ty + 1);
            lrx = WorldMercator.tileLng(metadata.tz, metadata.tx + 1);
        } else {
            // XXX - add support for other well-known tile grids
            return null;
        }

        final ElevationChunk chunk = ElevationChunk.Factory.create(
                metadata.type,
                null,
                ElevationData.MODEL_TERRAIN,
                OSMUtils.mapnikTileResolution(metadata.tz),
                (Polygon) GeometryFactory.polygonFromQuad(ulx, uly, lrx, uly, lrx, lry, ulx, lry),
                Double.NaN,
                Double.NaN,
                false,
                new TerrainRGB(dataArray, off, len, metadata.srid, metadata.orthometric, metadata.tx, metadata.ty, metadata.tz, chunkBitmapPool));
        return chunk;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    static class Metadata {
        int srid = 3857;
        int tx = -1;
        int ty = -1;
        int tz = -1;
        String type = "Mapbox";
        boolean orthometric = true;
    }

    static void defaultMetadata(Hints hints, Metadata metadata) {
        if(hints == null)
            return;
        // tile index
        if(hints.tileIndex != null) {
            metadata.tx = hints.tileIndex.x;
            metadata.ty = hints.tileIndex.y;
            metadata.tz = hints.tileIndex.z;
        }
        // SRID
        metadata.srid = (hints.srid == 4326) ? hints.srid : 3857;
        // MSL vs HAE
        do {
            if(hints.extras == null)
                break;
            final Object heights = hints.extras.get("heights");
            if(!(heights instanceof String))
                break;
            metadata.orthometric = !"hae".equals(heights);
        } while(false);
        // type
        do {
            if(hints.extras == null)
                break;
            final Object dataSource = hints.extras.get("dataSource");
            if(!(dataSource instanceof String))
                break;
            metadata.type = (String)dataSource;
        } while(false);
    }

    public static JSONObject getMetadata(byte[] blob, int off, int len) {
        try {
            // check for metadata
            final boolean[] done = {false};
            final JSONObject[] metadata = new JSONObject[1];
            final PNGChunkReader.Callback cb = new PNGChunkReader.Callback() {
                @Override
                public void chunk(int length, int type, InputStream data) {
                    if(type == 0x74414b61) { // tAKa -- TAK JSON metadata
                        done[0] = true;
                        byte[] buf = new byte[length];
                        try {
                            data.read(buf);
                        } catch(IOException e) {
                            return;
                        }
                        try {
                            metadata[0] = new JSONObject(new String(buf, 0, buf.length, FileSystemUtils.UTF8_CHARSET));
                        } catch(Throwable t) {}
                    }
                }

                @Override
                public void crc32(int expected) {}
            };
            PNGChunkReader chunks = new PNGChunkReader(new ByteArrayInputStream(blob, off, len));
            do {
                if(!chunks.nextChunk(cb))
                    break;
                if(done[0])
                    break;
            } while(true);
            return metadata[0];
        } catch(IOException ignored) {
            return null;
        }
    }

    static void blobMetadata(byte[] blob, int off, int len, final Metadata metadata) {
        try {
            JSONObject json = getMetadata(blob, off, len);
            metadata.srid = json.optInt("srid", metadata.srid);
            metadata.type = json.optString("dataSource", metadata.type);
            metadata.orthometric = !json.optString("heights", metadata.orthometric ? "orthometric" : "ellipsoidal").equals("ellipsoidal");
            JSONObject tileIndex = json.optJSONObject("tileIndex");
            if(tileIndex != null) {
                metadata.tx = tileIndex.optInt("x", metadata.tx);
                metadata.ty = tileIndex.optInt("y", metadata.ty);
                metadata.tz = tileIndex.optInt("z", metadata.tz);
            }
        } catch(Throwable ignored) {}
    }
}
