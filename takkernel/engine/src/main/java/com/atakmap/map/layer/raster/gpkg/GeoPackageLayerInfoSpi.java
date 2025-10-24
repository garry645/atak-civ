package com.atakmap.map.layer.raster.gpkg;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import android.graphics.Point;
import android.net.Uri;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.raster.tilematrix.TileContainer;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.map.projection.Projection;
import com.atakmap.map.projection.ProjectionFactory;
import com.atakmap.math.PointD;
import gov.tak.platform.system.SystemUtils;
import org.gdal.osr.SpatialReference;

import android.util.Pair;

import com.atakmap.map.gdal.GdalLibrary;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.gpkg.TileTable;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.raster.AbstractDatasetDescriptorSpi;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorSpi;
import com.atakmap.map.layer.raster.MosaicDatasetDescriptor;
import com.atakmap.map.layer.raster.mobileimagery.MobileImageryRasterLayer2;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;
import com.atakmap.spi.InteractiveServiceProvider;

public class GeoPackageLayerInfoSpi extends AbstractDatasetDescriptorSpi
{

    static
    {
        MobileImageryRasterLayer2.registerDatasetType("gpkg");
    }

    public final static DatasetDescriptorSpi INSTANCE = new GeoPackageLayerInfoSpi();

    private GeoPackageLayerInfoSpi()
    {
        super("gpkg", 0);
    }

    @Override
    protected Set<DatasetDescriptor> create(File file, File workingDir, InteractiveServiceProvider.Callback callback)
    {
        // Don't do anything with the callback in this method. This create method just
        // Reads data from a single table that contains bounds, and it will be fast
        // enough that it isn't worth reporting the completion of individual items.

        if (!GeoPackage.isGeoPackage(file))
            return null;

        GeoPackageMosaicDatabase gpkg = null;
        try
        {
            gpkg = new GeoPackageMosaicDatabase();
            gpkg.open(file);

            Map<String, Pair<Double, Double>> res = new HashMap<String, Pair<Double, Double>>();
            Map<String, Geometry> cov = new HashMap<String, Geometry>();
            Map<Integer, int[]> srids = new HashMap<Integer, int[]>();

            Map<String, MosaicDatabase2.Coverage> scratch = new HashMap<String, MosaicDatabase2.Coverage>();
            gpkg.getCoverages(scratch);
            if (scratch.isEmpty())
                return null;

            int srid = 4326;
            int maxSrids = 0;
            for (Map.Entry<String, MosaicDatabase2.Coverage> entry : scratch.entrySet())
            {
                res.put(entry.getKey(), Pair.<Double, Double>create(Double.valueOf(entry.getValue().minGSD), Double.valueOf(entry.getValue().maxGSD)));
                cov.put(entry.getKey(), entry.getValue().geometry);

                int[] count = srids.get(gpkg.getSrid(entry.getKey()));
                if (count == null)
                    srids.put(gpkg.getSrid(entry.getKey()), count = new int[] {0});
                count[0]++;
                if (count[0] > maxSrids)
                {
                    srid = gpkg.getSrid(entry.getKey());
                    maxSrids = count[0];
                }
            }

            Map<String, String> extraData = new HashMap<>();
            extraData.put("relativePaths", "false");
            // XXX - code in metadata for quadtree layers
            if(scratch.size() == 1)
            {
                TileContainer tiles = null;
                try {
                    do
                    {
                        tiles = GeoPackageTileContainer.SPI.open(file.getAbsolutePath(), null, true);
                        if (tiles == null)
                            break;
                        if(!TileMatrix.Util.isQuadtreeable(tiles))
                             break;
                        extraData.put("quadtree",  "1");

                        final TileMatrix.ZoomLevel[] zoomLevels = tiles.getZoomLevel();
                        final TileMatrix.ZoomLevel maxZoom = zoomLevels[zoomLevels.length-1];
                        final TileMatrix.ZoomLevel minZoom = zoomLevels[0];

                        Envelope bounds = tiles.getBounds();

                        Projection proj = ProjectionFactory.getProjection(tiles.getSRID());
                        GeoPoint imgUL = proj.inverse(new PointD(tiles.getOriginX(), tiles.getOriginY()), null);

                        Point minZoomGridLRTile = TileMatrix.Util.getTileIndex(tiles.getOriginX(), tiles.getOriginY(), minZoom, bounds.maxX-minZoom.pixelSizeX, bounds.minY+minZoom.pixelSizeY);
                        PointD minZoomGridLR = new PointD(
                                tiles.getOriginX()+(minZoomGridLRTile.x+1)*minZoom.tileWidth*minZoom.pixelSizeX,
                                tiles.getOriginY()-(minZoomGridLRTile.y+1)*minZoom.tileHeight*minZoom.pixelSizeY);
                        GeoPoint imgLR = proj.inverse(minZoomGridLR, null);

                        // image descriptor
                        extraData.put("image.numLevels", String.valueOf(maxZoom.level-minZoom.level));
                        extraData.put("image.width", String.valueOf((int)((minZoomGridLR.x-tiles.getOriginX())/maxZoom.pixelSizeX)));
                        extraData.put("image.height", String.valueOf((int)((tiles.getOriginY()-minZoomGridLR.y)/maxZoom.pixelSizeY)));
                        extraData.put("image.upperLeft.latitude", String.valueOf(imgUL.getLatitude()));
                        extraData.put("image.upperLeft.longitude", String.valueOf(imgUL.getLongitude()));
                        extraData.put("image.upperRight.latitude", String.valueOf(imgUL.getLatitude()));
                        extraData.put("image.upperRight.longitude", String.valueOf(imgLR.getLongitude()));
                        extraData.put("image.lowerRight.latitude", String.valueOf(imgLR.getLatitude()));
                        extraData.put("image.lowerRight.longitude", String.valueOf(imgLR.getLongitude()));
                        extraData.put("image.lowerLeft.latitude", String.valueOf(imgLR.getLatitude()));
                        extraData.put("image.lowerLeft.longitude", String.valueOf(imgUL.getLongitude()));
                    } while(false);
                } finally {
                    tiles.dispose();
                }

            }

            final String uri = SystemUtils.isOsWindows() ? Uri.fromFile(file).toString() : file.getAbsolutePath();
            return Collections.<DatasetDescriptor>singleton(
                    new MosaicDatasetDescriptor(file.getName(),
                            uri,
                            this.getType(),
                            "gpkg",
                            file,
                            GeoPackageMosaicDatabase.TYPE,
                            res.keySet(),
                            res,
                            cov,
                            srid,
                            false,
                            null,
                            extraData));
        } catch (Throwable t)
        {
            return null;
        } finally
        {
            if (gpkg != null)
                gpkg.close();
        }
    }

    @Override
    public boolean probe(File file, InteractiveServiceProvider.Callback callback)
    {
        return GeoPackage.isGeoPackage(file);
    }

    private static boolean isEPSG(String organization)
    {
        return (organization != null) && organization.equalsIgnoreCase("epsg");
    }

    public static int getSRID(GeoPackage geopackage, TileTable.TileMatrixSet matrix)
    {
        return getSRID(geopackage, matrix.srs_id);
    }

    public static int getSRID(GeoPackage geopackage, int srs_id)
    {
        // Geopackage Requirement 11
        if (srs_id == 4326)
            return srs_id;

        // look up the SRS based on the 'srs_id', returning the 
        final GeoPackage.SRS srs = geopackage.getSRSInfo(srs_id);
        if (srs != null)
        {
            // if the coordsys_id is an EPSG code, return it
            if (isEPSG(srs.organization) && srs.organization_coordsys_id > 0)
            {
                return srs.organization_coordsys_id;
            } else if (srs.definition != null)
            {
                // try to extract the SRID from the WKT representation
                try
                {
                    final SpatialReference ref = new SpatialReference(srs.definition);
                    final int srid = GdalLibrary.getSpatialReferenceID(ref);
                    if (srid > 0)
                        return srid;
                } catch (Exception ignored) {}
            }
        }

        // unhandled, default to legacy behavior
        return srs_id;
    }

    @Override
    public int parseVersion()
    {
        return 8;
    }
}
