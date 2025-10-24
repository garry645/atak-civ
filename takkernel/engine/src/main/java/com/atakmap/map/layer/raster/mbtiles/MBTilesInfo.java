package com.atakmap.map.layer.raster.mbtiles;

import java.io.File;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.atakmap.coremap.io.DatabaseInformation;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.database.CursorIface;
import com.atakmap.database.DatabaseIface;
import com.atakmap.database.Databases;
import com.atakmap.lang.Objects;
import com.atakmap.map.formats.mapbox.TerrainRGBChunkSpi;

public class MBTilesInfo
{
    private final static Set<String> TILES_TABLE_COLUMN_NAMES = new HashSet<String>();

    static
    {
        TILES_TABLE_COLUMN_NAMES.add("zoom_level");
        TILES_TABLE_COLUMN_NAMES.add("tile_column");
        TILES_TABLE_COLUMN_NAMES.add("tile_row");
        TILES_TABLE_COLUMN_NAMES.add("tile_data");
    }

    private final static Set<String> TILES_TABLE_COLUMN_NAMES2 = new HashSet<String>();

    static
    {
        TILES_TABLE_COLUMN_NAMES2.addAll(TILES_TABLE_COLUMN_NAMES);
        TILES_TABLE_COLUMN_NAMES.add("tile_alpha");
    }

    private final static String COLUMN_TILE_ALPHA = "tile_alpha";

    public int minLevel;
    public int maxLevel;
    public int minLevelGridMinX;
    public int minLevelGridMinY;
    public int minLevelGridMaxX;
    public int minLevelGridMaxY;
    public int maxLevelGridMinX;
    public int maxLevelGridMinY;
    public int maxLevelGridMaxX;
    public int maxLevelGridMaxY;
    public String name;
    public int tileWidth;
    public int tileHeight;
    public boolean hasTileAlpha;
    public String content;
    public String format;
    public int srid = 3857;

    /**************************************************************************/

    public static MBTilesInfo get(String path, DatabaseIface[] returnRef)
    {
        return get(path, false, returnRef);
    }

    public static MBTilesInfo get(String path, boolean maxZoomGridScan, DatabaseIface[] returnRef)
    {
        DatabaseIface database;

        // try spatialite first
        database = null;
        try
        {
            database = IOProviderFactory.createDatabase(new File(path), DatabaseInformation.OPTION_READONLY);
            final MBTilesInfo retval = get(database, maxZoomGridScan);
            if (retval != null)
            {
                if (returnRef != null)
                {
                    returnRef[0] = database;
                    database = null;
                }
                return retval;
            }
        } catch (Throwable ignored)
        {
        } finally
        {
            if (database != null)
                database.close();
        }

        return null;
    }

    public static MBTilesInfo get(DatabaseIface database)
    {
        return get(database, false);
    }

    public static MBTilesInfo get(DatabaseIface database, boolean maxZoomGridScan)
    {
        try
        {
            Map<String, Set<String>> databaseStructure;
            try
            {
                databaseStructure = Databases.getColumnNames(database);
            } catch (Exception e)
            {
                return null;
            }

            if (databaseStructure.get("tiles") == null)
                return null;

            Set<String> tilesTable = databaseStructure.get("tiles");
            if (!tilesTable.equals(TILES_TABLE_COLUMN_NAMES) && !tilesTable.equals(TILES_TABLE_COLUMN_NAMES2))
                return null;

            MBTilesInfo info = new MBTilesInfo();
            info.hasTileAlpha = tilesTable.contains(COLUMN_TILE_ALPHA);

            CursorIface result;

            info.minLevel = -1;
            info.maxLevel = -1;
            info.content = "imagery";
            result = null;
            try
            {
                result = database.query("SELECT name, value FROM metadata", null);
                String name;
                while (result.moveToNext())
                {
                    name = result.getString(0);
                    if(name == null)
                        continue;
                    //don't use min/maxZoomLevel since it isn't always reliable, calculate it below
                    switch(name) {
                        case "name" :
                            info.name = result.getString(1);
                            break;
                        case "format" :
                            info.format = result.getString(1);
                            if(Objects.equals(info.format, "pbf"))
                                info.content = "vector";
                            else if(Objects.equals(info.format, TerrainRGBChunkSpi.MIME_TYPE))
                                info.content = "terrain";
                            break;
                        case "json" :
                            // XXX - is this always the case
                            info.content = "vector";
                            break;
                        default :
                            break;
                    }
                }
            } finally
            {
                if (result != null)
                    result.close();
            }

            if (info.minLevel < 0)
            {
                result = null;
                try
                {
                    // XXX - switch to min(zoom_level)
                    result = database
                            .query(
                                    "SELECT zoom_level FROM tiles ORDER BY zoom_level ASC LIMIT 1",
                                    null);
                    if (result.moveToNext())
                        info.minLevel = result.getInt(0);
                } finally
                {
                    if (result != null)
                        result.close();
                }
            }
            if (info.maxLevel < 0)
            {
                result = null;
                try
                {
                    result = database
                            .query(
                                    "SELECT zoom_level FROM tiles ORDER BY zoom_level DESC LIMIT 1",
                                    null);
                    if (result.moveToNext())
                        info.maxLevel = result.getInt(0);
                } finally
                {
                    if (result != null)
                        result.close();
                }
            }
            // no tiles present in database
            if (info.minLevel < 0 || info.maxLevel < 0)
                return null;

            // bounds discovery
            int[] minLevelGridExtents = getTileGridExtent(database, info.minLevel);
            info.minLevelGridMinX = minLevelGridExtents[0];
            info.minLevelGridMinY = minLevelGridExtents[1];
            info.minLevelGridMaxX = minLevelGridExtents[2];
            info.minLevelGridMaxY = minLevelGridExtents[3];
            if(maxZoomGridScan) {
                int[] maxZoomGridExtents = getTileGridExtent(database, info.maxLevel);
                info.maxLevelGridMinX = maxZoomGridExtents[0];
                info.maxLevelGridMinY = maxZoomGridExtents[1];
                info.maxLevelGridMaxX = maxZoomGridExtents[2];
                info.maxLevelGridMaxY = maxZoomGridExtents[3];
            } else {
                info.maxLevelGridMinX = info.minLevelGridMinX << (info.maxLevel-info.minLevel);
                info.maxLevelGridMinY = info.minLevelGridMinY << (info.maxLevel-info.minLevel);
                info.maxLevelGridMaxX = info.minLevelGridMaxX << (info.maxLevel-info.minLevel);
                info.maxLevelGridMaxY = info.minLevelGridMaxY << (info.maxLevel-info.minLevel);
            }

            // obtain and tile dimensions
            info.tileWidth = 256;
            info.tileHeight = 256;

            result = null;
            try
            {
                result = database.query("SELECT tile_data FROM tiles LIMIT 1",
                        null);
                if (result.moveToNext())
                {
                    byte[] blob = result.getBlob(0);
                    BitmapFactory.Options opts = new BitmapFactory.Options();
                    opts.inJustDecodeBounds = true;
                    Bitmap tile = null;
                    try
                    {
                        tile = BitmapFactory.decodeByteArray(blob, 0, blob.length, opts);
                        if (opts.outWidth > 0 && opts.outHeight > 0)
                        {
                            info.tileWidth = opts.outWidth;
                            info.tileHeight = opts.outHeight;
                        }
                    } finally
                    {
                        if (tile != null)
                            tile.recycle();
                    }
                } else
                {
                    throw new IllegalStateException();
                }
            } finally
            {
                if (result != null)
                    result.close();
            }

            return info;
        } catch (Exception e)
        {
            Log.w("MBTilesInfo", "Unexpected error parsing info", e);
            return null;
        }
    }

    /** @return {@code minTileX,minTileY,maxTileX,maxTileY} */
    static int[] getTileGridExtent(DatabaseIface db, int zoomLevel) {
        int[] tileIndices = new int[4];

        String[] queries = new String[]
        {
            "SELECT tile_column FROM tiles WHERE zoom_level = ? ORDER BY tile_column ASC",
            "SELECT tile_row FROM tiles WHERE zoom_level = ? ORDER BY tile_row ASC",
            "SELECT tile_column FROM tiles WHERE zoom_level = ? ORDER BY tile_column DESC",
            "SELECT tile_row FROM tiles WHERE zoom_level = ? ORDER BY tile_row DESC",
        };
        for(int i = 0; i < 4; i++)
            try(CursorIface result = db.query(queries[i], new String[] {String.valueOf(zoomLevel)})) {
                if (result.moveToNext()) tileIndices[i] = result.getInt(0);
                else throw new RuntimeException();
            }
        return tileIndices;
    }
}