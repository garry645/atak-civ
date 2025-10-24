package com.atakmap.map.formats.cdn;

import android.util.Pair;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.io.SubInputStream;
import com.atakmap.map.layer.control.ColorControl2;
import com.atakmap.map.layer.raster.tilematrix.TileGrid;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import gov.tak.api.engine.map.features.Envelope;
import gov.tak.api.engine.math.PointD;

public final class StreamingTiles
{
    final static Map<Integer, TileGrid> tileGrids = new HashMap<>();
    static {
        tileGrids.put(TileGrid.WGS84.srid, TileGrid.WGS84);
        tileGrids.put(TileGrid.WGS84_3D.srid, TileGrid.WGS84_3D);
        tileGrids.put(TileGrid.WebMercator.srid, TileGrid.WebMercator);
        tileGrids.put(TileGrid.WorldMercator.srid, TileGrid.WorldMercator);
    }

    public static class Authorization
    {
        public String type;
        public String server;
        public String clientId;
    }

    public static class ColorFilter
    {
        public ColorControl2.Mode mode;
        public float red;
        public float green;
        public float blue;
        public float alpha;
    }

    public static class Defaults
    {
        public ColorFilter colorFilter;
        public boolean visible;
        public int priority;
    }

    public int schema;
    public String name;
    public String url;
    public String eula;
    public String attribution;
    public boolean downloadable;
    public boolean overlay;
    public String srs;
    public Envelope bounds;
    /** if {@code true} tile row {@code 0} is the bottom of the grid */
    public boolean invertYAxis;
    public Map<String, String> additionalParameters;
    public Set<String> serverParts;
    public int refreshInterval;
    public Authorization authorization;

    public PointD origin;
    public TileMatrix.ZoomLevel[] tileMatrix;
    public boolean isQuadtree;

    public String content;
    public String mimeType;

    public Map<String, String> metadata;

    public static StreamingTiles parse(File f, int limit) {
        try {
            try(FileInputStream is = new FileInputStream(f)) {
                return parse(is, limit);
            }
        } catch(IOException t) {
            return null;
        }
    }

    public static StreamingTiles parse(InputStream stream, int limit) throws IOException {
        try(InputStream is = new SubInputStream(stream, limit)) {
            return parse(FileSystemUtils.copyStreamToString(is, false, Charset.forName("UTF-8")));
        }
    }

    public static StreamingTiles parse(String s)
    {
        try {
            JSONObject json = new JSONObject(s);

            final String[] versionTokens = json.optString("schema", "1").split("\\.");
            final int[] version = new int[versionTokens.length];
            for(int i = 0; i < versionTokens.length; i++)
                version[i] = Integer.parseInt(versionTokens[i]);

            if(version[0] > 5)
                return null;
            // verify semantic version
            if(version[0] > 1 && version.length != 3)
                return null;

            StreamingTiles content = new StreamingTiles();
            content.schema = version[0]; // record the major version

            if(version[0] == 1)
                content.name = json.getString("name");
            else
                content.name = json.getString("title");

            content.url = json.getString("url");
            content.eula = json.optString("eula", null);
            content.attribution = json.optString("attribution", null);
            content.downloadable = json.optBoolean("downloadable", true);
            content.overlay = json.optBoolean("overlay", false);
            content.srs = json.optString("srs", "EPSG:3857");
            JSONObject bounds = json.optJSONObject("bounds");
            if(bounds != null)
                content.bounds = new Envelope(
                        bounds.getDouble("minX"),
                        bounds.getDouble("minY"),
                        0d,
                        bounds.getDouble("maxX"),
                        bounds.getDouble("maxY"),
                        0d);
            content.invertYAxis = json.optBoolean("invertYAxis", false);

            JSONArray additionalParameters = json.optJSONArray("additionalParameters");
            if(additionalParameters != null) {
                content.additionalParameters = new HashMap<>();
                for(int i = 0; i < additionalParameters.length(); i++) {
                    JSONObject kvp = additionalParameters.getJSONObject(i);
                    content.additionalParameters.put(kvp.getString("name"), kvp.getString("value"));
                }
            }

            JSONArray serverParts = json.optJSONArray("serverParts");
            if(serverParts != null) {
                content.serverParts = new HashSet<>();
                for(int i = 0; i < serverParts.length(); i++)
                    content.serverParts.add(serverParts.getString(i));
            }

            JSONObject authorization = json.optJSONObject("authorization");
            if(authorization != null) {
                content.authorization = new Authorization();
                content.authorization.type = authorization.optString("type", null);
                content.authorization.server = authorization.optString("server", null);
                content.authorization.clientId = authorization.optString("clientId", null);
            }

            JSONObject origin = json.optJSONObject("origin");
            if(origin != null)
                content.origin = new PointD(origin.getDouble("x"), origin.getDouble("y"), 0d);

            JSONArray tileMatrix = json.optJSONArray("tileMatrix");
            // quadtree is implicit with implicit tile grid
            content.isQuadtree = json.optBoolean("isQuadtree",
                    version[0] >= 4 &&
                            tileMatrix == null &&
                            json.optInt("numLevels", -1) != -1);
            if(tileMatrix != null) {
                if(tileMatrix.length() == 0)
                    return null;

                if(content.isQuadtree && tileMatrix.length() == 1) {
                    final TileMatrix.ZoomLevel z0 = parseTileMatrixLevel(tileMatrix.getJSONObject(0));
                    final int numLevels = json.optInt("numLevels", 0);
                    if(numLevels > 0)
                        content.tileMatrix = TileMatrix.Util.createQuadtree(z0, numLevels);
                    else
                        content.tileMatrix = new TileMatrix.ZoomLevel[] {z0};
                } else {
                    content.tileMatrix = new TileMatrix.ZoomLevel[tileMatrix.length()];
                    for (int i = 0; i < tileMatrix.length(); i++) {
                        content.tileMatrix[i] = parseTileMatrixLevel(tileMatrix.getJSONObject(i));
                    }
                }
            } else if(version[0] >= 4) {
                // implicit tile grid
                do {
                    if(!content.isQuadtree)
                        break;
                    final int numLevels = json.optInt("numLevels", -1);
                    if(numLevels <= 0)
                        break;
                    if(!content.srs.matches("EPSG:\\d+"))
                        break;
                    int srid = Integer.parseInt(content.srs.split(":")[1]);
                    final TileGrid z0 = tileGrids.get(srid);
                    if(z0 == null)
                        break;
                    if(content.origin == null)
                        content.origin = new PointD(z0.origin.x, z0.origin.y, z0.origin.z);
                    content.tileMatrix = TileMatrix.Util.createQuadtree(z0.zoomLevels[0], numLevels);
                } while(false);
            }

            if(version[0] == 1) {
                content.content = "imagery";
                content.mimeType = null;
            } else {
                content.content = json.optString("content", "imagery");
                content.mimeType = json.optString("mimeType", null);
            }

            if(version[0] > 2 || (version[0] == 2 && version[1] >= 1)) {
                JSONObject metadata = json.optJSONObject("metadata");
                if(metadata != null) {
                    content.metadata = new LinkedHashMap<>();
                    Iterator<String> keyIter = metadata.keys();
                    while(keyIter.hasNext()) {
                        final String key = keyIter.next();
                        final String value = metadata.optString(key, null);
                        if(value != null)
                            content.metadata.put(key, value);
                    }
                }
            }

            return content;
        } catch(Throwable t) {
            return null;
        }
    }

    static TileMatrix.ZoomLevel parseTileMatrixLevel(JSONObject zoomLevel) throws JSONException {
        return newTileMatrixLevel(
                zoomLevel.getInt("level"),
                zoomLevel.getDouble("resolution"),
                zoomLevel.getDouble("pixelSizeX"),
                zoomLevel.getDouble("pixelSizeY"),
                zoomLevel.getInt("tileWidth"),
                zoomLevel.getInt("tileHeight"));
    }
    static TileMatrix.ZoomLevel newTileMatrixLevel(int level, double resolution, double pixelSize, int tileSize) {
        return newTileMatrixLevel(level, resolution, pixelSize, pixelSize, tileSize, tileSize);
    }
    static TileMatrix.ZoomLevel newTileMatrixLevel(int level, double resolution, double pixelSizeX, double pixelSizeY, int tileWidth, int tileHeight) {
        TileMatrix.ZoomLevel tileMatrix = new TileMatrix.ZoomLevel();
        tileMatrix.level = level;
        tileMatrix.resolution = resolution;
        tileMatrix.pixelSizeX = pixelSizeX;
        tileMatrix.pixelSizeY = pixelSizeY;
        tileMatrix.tileWidth = tileWidth;
        tileMatrix.tileHeight = tileHeight;

        return tileMatrix;
    }

    public static JSONObject create(StreamingTiles content)
    {
        try {
            JSONObject json = new JSONObject();

            final int[] version = {3, 0, 0};
            if(content.invertYAxis)
                version[0] = 5;

            json.put("title", content.name);
            json.put("url", content.url);
            putOptional(json, "eula", content.eula);
            putOptional(json, "attribution", content.attribution);
            json.put("downloadable", content.downloadable);
            json.put("overlay", content.overlay);
            json.put("srs", content.srs);

            if(content.bounds != null) {
                JSONObject bounds = new JSONObject();
                bounds.put("minX", content.bounds.minX);
                bounds.put("minY", content.bounds.minY);
                bounds.put("maxX", content.bounds.maxX);
                bounds.put("maxY", content.bounds.maxY);
                json.put("bounds", bounds);
            }
            if(content.additionalParameters != null) {
                JSONArray additionalParameters = new JSONArray(content.additionalParameters.size());
                for(Map.Entry<String, String> entry : content.additionalParameters.entrySet()) {
                    JSONObject kvp = new JSONObject();
                    kvp.put("name", entry.getKey());
                    kvp.put("value", entry.getValue());
                    additionalParameters.put(kvp);
                }
                json.put("additionalParameters", additionalParameters);
            }

            if(content.serverParts != null) {
                JSONArray serverParts = new JSONArray(content.serverParts.size());
                for(String serverPart : content.serverParts)
                    serverParts.put(serverPart);
                json.put("serverParts", serverParts);
            }

            if(content.authorization != null) {
                JSONObject authorization = new JSONObject();
                putOptional(authorization, "type", content.authorization.type);
                putOptional(authorization, "server", content.authorization.server);
                putOptional(authorization, "clientId", content.authorization.clientId);
                json.put("authorization", authorization);
            }

            if(content.origin != null) {
                JSONObject origin = new JSONObject();
                origin.put("x", content.origin.x);
                origin.put("y", content.origin.y);
                json.put("origin", origin);
            }
            if(content.invertYAxis)
                json.put("invertYAxis", content.invertYAxis);

            json.put("isQuadtree", content.isQuadtree);
            if(content.tileMatrix != null && content.tileMatrix.length > 0) {
                JSONArray tileMatrix = new JSONArray();
                if(content.isQuadtree) {
                    json.put("numLevels", content.tileMatrix.length);
                    if(version[0] < 4)
                        putTileMatrixLevel(tileMatrix, content.tileMatrix[0]);
                } else {
                    for (int i = 0; i < content.tileMatrix.length; i++)
                        putTileMatrixLevel(tileMatrix, content.tileMatrix[i]);
                }
                if(version[0] < 4 || tileMatrix.length() > 0)
                    json.put("tileMatrix", tileMatrix);
            }

            json.put("content", content.content);
            putOptional(json, "mimeType", content.mimeType);

            if(content.metadata != null && !content.metadata.isEmpty()) {
                JSONObject metadata = new JSONObject();
                for(Map.Entry<String, String> entry : content.metadata.entrySet())
                    metadata.put(entry.getKey(), entry.getValue());
                json.put("metadata", metadata);
            }

            // seet schema version
            json.put("schema", String.format("%d.%d.%d", version[0], version[1], version[2]));

            return json;
        } catch(Throwable t) {
            return null;
        }
    }

    private static void putOptional(JSONObject obj, String key, String value) throws JSONException {
        if(value != null)
            obj.put(key, value);
    }

    static void putTileMatrixLevel(JSONArray json, TileMatrix.ZoomLevel tileMatrix) throws JSONException {
        JSONObject zoomLevel = new JSONObject();
        zoomLevel.put("level", tileMatrix.level);
        zoomLevel.put("resolution", tileMatrix.resolution);
        zoomLevel.put("pixelSizeX", tileMatrix.pixelSizeX);
        zoomLevel.put("pixelSizeY", tileMatrix.pixelSizeY);
        zoomLevel.put("tileWidth", tileMatrix.tileWidth);
        zoomLevel.put("tileHeight", tileMatrix.tileHeight);
        json.put(zoomLevel);
    }
}
