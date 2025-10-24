package com.atakmap.map.layer.raster.gpkg.opengl;

import android.util.Pair;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.gpkg.GeoPackage;
import com.atakmap.map.layer.control.ColorControl;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.ImageDatasetDescriptor;
import com.atakmap.map.layer.raster.MosaicDatasetDescriptor;
import com.atakmap.map.layer.raster.mosaic.opengl.GLMosaicMapLayer;
import com.atakmap.map.layer.raster.opengl.GLMapLayer3;
import com.atakmap.map.layer.raster.opengl.GLMapLayerSpi3;
import com.atakmap.map.layer.raster.tilematrix.TileMatrixReader;
import com.atakmap.map.layer.raster.tilereader.TileReaderFactory;
import com.atakmap.map.layer.raster.tilereader.TileReaderSpi2;
import com.atakmap.map.layer.raster.tilereader.opengl.GLTiledMapLayer2;
import com.atakmap.util.Collections2;

import java.util.Map;

public final class GLGeoPackageTilesLayer2
{

    final static TileReaderSpi2 TILEREADER_SPI = new TileMatrixReader.ContainerSpi("gpkg", -1);
    static {
        TileReaderFactory.registerSpi(TILEREADER_SPI);
    }

    public final static GLMapLayerSpi3 SPI = new GLMapLayerSpi3()
    {
        @Override
        public int getPriority()
        {
            return 1;
        }

        @Override
        public GLMapLayer3 create(Pair<MapRenderer, DatasetDescriptor> arg)
        {
            final MapRenderer surface = arg.first;
            final DatasetDescriptor info = arg.second;
            if (!info.getDatasetType().equals("gpkg"))
                return null;
            if (!(info instanceof MosaicDatasetDescriptor))
                return null;

            if(!DatasetDescriptor.getExtraData(info, "quadtree", "0").equals("0") &&
                    (info.getSpatialReferenceID() == 4326 ||
                     info.getSpatialReferenceID() == 3857)) {

                final Map<String, String> extraData = info.getExtraData();
                ImageDatasetDescriptor img = new ImageDatasetDescriptor(
                        info.getName(),
                        info.getUri(),
                        info.getProvider(),
                        info.getDatasetType(),
                        Collections2.first(info.getImageryTypes()),
                        Integer.parseInt(extraData.get("image.width")),
                        Integer.parseInt(extraData.get("image.height")),
                        Integer.parseInt(extraData.get("image.numLevels")),
                        new GeoPoint(Double.parseDouble(extraData.get("image.upperLeft.latitude")),
                                Double.parseDouble(extraData.get("image.upperLeft.longitude"))),
                        new GeoPoint(Double.parseDouble(extraData.get("image.upperRight.latitude")),
                                Double.parseDouble(extraData.get("image.upperRight.longitude"))),
                        new GeoPoint(Double.parseDouble(extraData.get("image.lowerRight.latitude")),
                                Double.parseDouble(extraData.get("image.lowerRight.longitude"))),
                        new GeoPoint(Double.parseDouble(extraData.get("image.lowerLeft.latitude")),
                                Double.parseDouble(extraData.get("image.lowerLeft.longitude"))),
                        info.getSpatialReferenceID(),
                        info.isRemote(),
                        info.getWorkingDirectory(),
                        info.getExtraData());
                GLMapLayer3 glml = GLTiledMapLayer2.SPI.create(new Pair<>(surface, img));
                if(glml != null)
                    return glml;
            }

            return new GLMosaicMapLayer(surface, (MosaicDatasetDescriptor) info);
        }
    };

    private GLGeoPackageTilesLayer2()
    {
    }
}
