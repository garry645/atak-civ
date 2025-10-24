package com.atakmap.map.formats.quantizedmesh;

import com.atakmap.interop.DataType;
import com.atakmap.interop.Interop;
import com.atakmap.lang.Unsafe;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationChunkSpi;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.Mesh;
import com.atakmap.map.layer.model.ModelInfo;
import com.atakmap.map.layer.model.Models;
import com.atakmap.map.layer.model.VertexDataLayout;
import com.atakmap.math.Matrix;
import com.atakmap.math.NoninvertibleTransformException;
import com.atakmap.math.PointD;
import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.platform.io.DataOutputStream2;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Map;

public final class QMEChunkSpi implements ElevationChunkSpi
{
    final static Interop<ElevationChunk> ElevationChunk_interop = Interop.findInterop(ElevationChunk.class);

    public final static String MIME_TYPE = "application/vnd.quantized-mesh";

    @Override
    public ElevationChunk create(ByteBuffer data, Hints hints) {
        if(hints == null)
            return null;
        final int srid = hints.srid;
        final String type = opt(hints.extras, "dataSource", "QME");

        if(data.isDirect()) {
            return ElevationChunk_interop.create(QMENative.createFromPointer(Unsafe.getBufferPointer(data), data.position(), data.remaining(), hints.tileIndex.z, srid, null, type));
        } else if(data.hasArray()) {
            return ElevationChunk_interop.create(QMENative.createFromByteArray(data.array(), data.position(), data.remaining(), hints.tileIndex.z, srid, null, type));
        }
        // XXX - copy into array???
        return null;
    }

    @Override
    public String getMimeType() {
        return MIME_TYPE;
    }

    static String opt(Map<String, Object> extras, String key, String def) {
        do {
            if(extras == null)
                break;
            final Object v = extras.get(key);
            if(v == null)
                break;
            else if(v instanceof String)
                return (String)v;
        } while(false);
        return def;
    }

    static Matrix getHeightmapLocalTransform(int srid, Envelope tileBounds) {
        if(srid != 4326)
            return null;
        final double rtcX = (tileBounds.minX+tileBounds.maxX)/2d;
        final double rtcY = (tileBounds.minY+tileBounds.maxY)/2d;
        final double rtcZ = 0d;
        Matrix localFrame = Matrix.getIdentity();
        localFrame.translate(rtcX, rtcY, rtcZ);
        localFrame.scale(
                1d / GeoCalculations.approximateMetersPerDegreeLongitude(rtcY),
                1d / GeoCalculations.approximateMetersPerDegreeLatitude(rtcY),
                1d);
        return localFrame;
    }
    static Envelope getHeightmapLocalTileBounds(Matrix localFrame, Envelope tileBounds) {
        if(localFrame == null)
            return tileBounds;
        try {
            Matrix invLocalFrame = localFrame.createInverse();
            PointD p = new PointD();

            tileBounds = new Envelope(tileBounds);
            p.x = tileBounds.minX;
            p.y = tileBounds.minY;
            p.z = tileBounds.minZ;
            invLocalFrame.transform(p, p);
            tileBounds.minX = p.x;
            tileBounds.minY = p.y;
            tileBounds.minZ = p.z;

            p.x = tileBounds.maxX;
            p.y = tileBounds.maxY;
            p.z = tileBounds.maxZ;
            invLocalFrame.transform(p, p);
            tileBounds.maxX = p.x;
            tileBounds.maxY = p.y;
            tileBounds.maxZ = p.z;
        } catch(NoninvertibleTransformException e) {
            throw new RuntimeException(e);
        }
        return tileBounds;
    }

    public static byte[] encode(float[] heightmap, int srid, int tileWidth, int tileHeight, Envelope tileBounds, boolean discardNoDataValue, double simplifyMaxError) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        return encode(heightmap, srid, tileWidth, tileHeight, tileBounds, discardNoDataValue, simplifyMaxError, bos) ? bos.toByteArray() : null;
    }

    public static boolean encode(float[] heightmap, int srid, int tileWidth, int tileHeight, Envelope tileBounds, boolean discardNoDataValue, double simplifyMaxError, OutputStream sink) {
        Matrix localFrame = getHeightmapLocalTransform(srid, tileBounds);
        tileBounds = getHeightmapLocalTileBounds(localFrame, tileBounds);
        return encodeImpl(
                ElevationChunk.Factory.meshFromHeightmap(heightmap, tileWidth, tileHeight, tileBounds, discardNoDataValue, Double.TYPE),
                srid,
                localFrame,
                simplifyMaxError,
                sink);
    }

    public static byte[] encode(double[] heightmap, int srid, int tileWidth, int tileHeight, Envelope tileBounds, boolean discardNoDataValue, double simplifyMaxError) {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        return encode(heightmap, srid, tileWidth, tileHeight, tileBounds, discardNoDataValue, simplifyMaxError, bos) ? bos.toByteArray() : null;
    }
    public static boolean encode(double[] heightmap, int srid, int tileWidth, int tileHeight, Envelope tileBounds, boolean discardNoDataValue, double simplifyMaxError, OutputStream sink) {
        Matrix localFrame = getHeightmapLocalTransform(srid, tileBounds);
        tileBounds = getHeightmapLocalTileBounds(localFrame, tileBounds);
        return encodeImpl(
                ElevationChunk.Factory.meshFromHeightmap(heightmap, tileWidth, tileHeight, tileBounds, discardNoDataValue, Double.TYPE),
                srid,
                localFrame,
                simplifyMaxError,
                sink);
    }

    private static boolean encodeImpl(Mesh heightmap, int srid, Matrix localFrame, double simplifyMaxError, OutputStream sink) {
        ElevationChunk.Data chunk = new ElevationChunk.Data();
        chunk.srid = srid;
        chunk.localFrame = localFrame;
        chunk.value = heightmap;

        if(simplifyMaxError > 0d)
            // simplify
            chunk.value = Models.simplify(chunk.value, 0.05f);

        return encode(chunk, sink);
    }

    public static byte[] encode(ElevationChunk.Data chunk) {
        final ByteArrayOutputStream bos = new ByteArrayOutputStream();
        return encode(chunk, bos) ? bos.toByteArray() : null;
    }

    public static boolean encode(ElevationChunk.Data chunk, java.io.OutputStream sink) {
        Mesh m = chunk.value;
        try {
            if (chunk.srid != 4326 || chunk.localFrame != null) {
                ModelInfo srcOpts = new ModelInfo();
                srcOpts.srid = chunk.srid;
                srcOpts.localFrame = chunk.localFrame;

                ModelInfo dstOpts = new ModelInfo();
                dstOpts.srid = 4326;
                dstOpts.localFrame = Matrix.getIdentity();

                VertexDataLayout dstLayout = new VertexDataLayout();
                dstLayout.attributes = Mesh.VERTEX_ATTR_POSITION;
                dstLayout.interleaved = false;
                dstLayout.position.dataType = Double.TYPE;
                dstLayout.position.stride = DataType.sizeof(dstLayout.position.dataType)*3;

                m = Models.transform(srcOpts, m, dstOpts, dstLayout, null);
            }
            DataOutputStream2 dos = new DataOutputStream2(sink, ByteOrder.LITTLE_ENDIAN);
            new MeshTile(m).writeFile(dos);
            dos.flush();
            return true;
        } catch(IOException e) {
            return false;
        } finally {
            if(m != chunk.value)
                m.dispose();
        }
    }
}
