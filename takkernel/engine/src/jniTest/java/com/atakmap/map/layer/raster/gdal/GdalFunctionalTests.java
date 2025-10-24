package com.atakmap.map.layer.raster.gdal;

import android.content.Context;

import com.atakmap.coremap.io.DefaultIOProvider;
import com.atakmap.coremap.io.IOProviderFactoryHelper;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.gdal.GdalLibrary;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;
import com.atakmap.map.layer.raster.MosaicDatasetDescriptor;
import com.atakmap.map.layer.raster.mosaic.ATAKMosaicDatabase3;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2;
import com.atakmap.map.layer.raster.tilereader.TileReader;
import com.atakmap.map.layer.raster.tilereader.TileReaderFactory;

import com.atakmap.math.PointD;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;

import gov.tak.test.KernelJniTest;
import gov.tak.test.util.FileUtils;

import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.gdal.osr.SpatialReference;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.Set;

public class GdalFunctionalTests extends KernelJniTest {
    @Before
    public void beforeTests() {
        IOProviderFactoryHelper.registerProvider(new DefaultIOProvider(), true);
    }

    private static byte[] createTestDataSamples(int width, int height) {
        byte[] data = new byte[width*height];
        for (int i = 0; i < data.length; i++) {
            data[i] = (byte)(i%100);
        }
        return data;
    }

    private static void createTestData(String filePath, byte[] data, int width, int height, String driverName) {
        createTestData(filePath, data, width, height, new double[] {
                -100, 0.07, 0,
                39, 0, -0.07,
        }, 4326, driverName);
    }
    private static void createTestData(String filePath, byte[] data, int width, int height, double[] geotransform, int srid, String driverName) {
        Driver driver = gdal.GetDriverByName(driverName);
        Dataset ds = driver.Create(filePath, width, height, 1, gdalconstConstants.GDT_Byte);
        if (ds == null) {
            return;
        }

        ds.GetRasterBand(1).WriteRaster(0, 0, width, height, data);
        // https://gdal.org/tutorials/geotransforms_tut.html
        ds.SetGeoTransform(geotransform);
        ds.SetProjection(GdalLibrary.getWkt(srid));
        ds.delete();
    }

    //[479993.99996347405, 1.0, 0.0, 4040005.999987617, 0.0, -1.0]
    //26913
    @Test
    public void GDALDatasetProjection2_projected_regression_check() throws Throwable {
        final Context c = getTestContext();
        try(FileUtils.AutoDeleteFile testFile = FileUtils.AutoDeleteFile.createTempFile(c)){
            GdalLibrary.init();

            final byte[] testDataSamples = createTestDataSamples(519, 512);
            createTestData(testFile.file.getAbsolutePath(), testDataSamples, 519, 512, new double[] {479993.99996347405, 1.0, 0.0, 4040005.999987617, 0.0, -1.0}, 26913, "GTiff");

            Dataset ds = GdalLibrary.openDatasetFromFile(testFile.file);
            Assert.assertNotNull(ds);

            GdalDatasetProjection2 proj = GdalDatasetProjection2.getInstance(ds);
            Assert.assertNotNull(ds);

            double[][] imgPointWeights = new double[][]
                    {
                            {0d, 0d},
                            {1d, 0d},
                            {1d, 1d},
                            {0d, 1d},
                            {0.5d, 0.5d},
                            {0.25d, 0.25d},
                            {0.75d, 0.75d},
                            {0.333d, 0.667d},
                            {0.667d, 0.333d},
                    };
            PointD[] imgPoints = new PointD[imgPointWeights.length];
            GeoPoint[] llaPoints = new GeoPoint[imgPoints.length];
            for(int i = 0; i < imgPointWeights.length; i++) {
                imgPoints[i] = new PointD((ds.GetRasterXSize()-1)*imgPointWeights[i][0], (ds.GetRasterYSize()-1)*imgPointWeights[i][1]);
                llaPoints[i] = GeoPoint.createMutable();

                Assert.assertTrue(proj.imageToGround(imgPoints[i], llaPoints[i]));

                // verify round-trip
                PointD rt_img = new PointD();
                Assert.assertTrue(proj.groundToImage(llaPoints[i], rt_img));
                Assert.assertEquals(imgPoints[i].x, rt_img.x, 0.000001d);
                Assert.assertEquals(imgPoints[i].y, rt_img.y, 0.000001d);
            }

            // regression check
            Assert.assertEquals(36.50518115624388, llaPoints[0].getLatitude(), 0.000001d);
            Assert.assertEquals(-105.22340750506233, llaPoints[0].getLongitude(), 0.000001d);
            Assert.assertEquals(36.505191868637006, llaPoints[1].getLatitude(), 0.000001d);
            Assert.assertEquals(-105.21761185733453, llaPoints[1].getLongitude(), 0.000001d);
            Assert.assertEquals(36.50057612749218, llaPoints[2].getLatitude(), 0.000001d);
            Assert.assertEquals(-105.21759894044084, llaPoints[2].getLongitude(), 0.000001d);
            Assert.assertEquals(36.50056541689261, llaPoints[3].getLatitude(), 0.000001d);
            Assert.assertEquals(-105.2233942441604, llaPoints[3].getLongitude(), 0.000001d);
            Assert.assertEquals(36.50287867795506, llaPoints[4].getLatitude(), 0.000001d);
            Assert.assertEquals(-105.22050313675977, llaPoints[4].getLongitude(), 0.000001d);
            Assert.assertEquals(36.50402992612234, llaPoints[5].getLatitude(), 0.000001d);
            Assert.assertEquals(-105.22195529941192, llaPoints[5].getLongitude(), 0.000001d);
            Assert.assertEquals(36.50172741174428, llaPoints[6].getLatitude(), 0.000001d);
            Assert.assertEquals(-105.21905101710355, llaPoints[6].getLongitude(), 0.000001d);
            Assert.assertEquals(36.502106056587195, llaPoints[7].getLatitude(), 0.000001d);
            Assert.assertEquals(-105.22146878576355, llaPoints[7].getLongitude(), 0.000001d);
            Assert.assertEquals(36.50365129147156, llaPoints[8].getLatitude(), 0.000001d);
            Assert.assertEquals(-105.21953746856563, llaPoints[8].getLongitude(), 0.000001d);
        }
    }

    @Test
    public void GDALDatasetProjection2_4326_regression_check() throws Throwable {
        final Context c = getTestContext();
        try(FileUtils.AutoDeleteFile testFile = FileUtils.AutoDeleteFile.createTempFile(c)){
            GdalLibrary.init();

            final byte[] testDataSamples = createTestDataSamples(519, 512);
            createTestData(testFile.file.getAbsolutePath(), testDataSamples, 519, 512, "GTiff");

            Dataset ds = GdalLibrary.openDatasetFromFile(testFile.file);
            Assert.assertNotNull(ds);

            GdalDatasetProjection2 proj = GdalDatasetProjection2.getInstance(ds);
            Assert.assertNotNull(ds);

            double[][] imgPointWeights = new double[][]
            {
                {0d, 0d},
                {1d, 0d},
                {1d, 1d},
                {0d, 1d},
                {0.5d, 0.5d},
                {0.25d, 0.25d},
                {0.75d, 0.75d},
                {0.333d, 0.667d},
                {0.667d, 0.333d},
            };
            PointD[] imgPoints = new PointD[imgPointWeights.length];
            GeoPoint[] llaPoints = new GeoPoint[imgPoints.length];
            for(int i = 0; i < imgPointWeights.length; i++) {
                imgPoints[i] = new PointD((ds.GetRasterXSize()-1)*imgPointWeights[i][0], (ds.GetRasterYSize()-1)*imgPointWeights[i][1]);
                llaPoints[i] = GeoPoint.createMutable();

                Assert.assertTrue(proj.imageToGround(imgPoints[i], llaPoints[i]));

                // verify round-trip
                PointD rt_img = new PointD();
                Assert.assertTrue(proj.groundToImage(llaPoints[i], rt_img));
                Assert.assertEquals(imgPoints[i].x, rt_img.x, 0.000001d);
                Assert.assertEquals(imgPoints[i].y, rt_img.y, 0.000001d);
            }

            // regression check
            Assert.assertEquals(39.0, llaPoints[0].getLatitude(), 0.000001d);
            Assert.assertEquals(-100.0, llaPoints[0].getLongitude(), 0.000001d);
            Assert.assertEquals(39.0, llaPoints[1].getLatitude(), 0.000001d);
            Assert.assertEquals(-63.669999999999995, llaPoints[1].getLongitude(), 0.000001d);
            Assert.assertEquals(3.1599999999999966, llaPoints[2].getLatitude(), 0.000001d);
            Assert.assertEquals(-63.669999999999995, llaPoints[2].getLongitude(), 0.000001d);
            Assert.assertEquals(3.1599999999999966, llaPoints[3].getLatitude(), 0.000001d);
            Assert.assertEquals(-100.0, llaPoints[3].getLongitude(), 0.000001d);
            Assert.assertEquals(21.08, llaPoints[4].getLatitude(), 0.000001d);
            Assert.assertEquals(-81.835, llaPoints[4].getLongitude(), 0.000001d);
            Assert.assertEquals(30.04, llaPoints[5].getLatitude(), 0.000001d);
            Assert.assertEquals(-90.9175, llaPoints[5].getLongitude(), 0.000001d);
            Assert.assertEquals(12.119999999999997, llaPoints[6].getLatitude(), 0.000001d);
            Assert.assertEquals(-72.7525, llaPoints[6].getLongitude(), 0.000001d);
            Assert.assertEquals(15.094719999999995, llaPoints[7].getLatitude(), 0.000001d);
            Assert.assertEquals(-87.90211, llaPoints[7].getLongitude(), 0.000001d);
            Assert.assertEquals(27.065279999999998, llaPoints[8].getLatitude(), 0.000001d);
            Assert.assertEquals(-75.76789, llaPoints[8].getLongitude(), 0.000001d);
        }
    }

    @Test
    public void parse_dataset_and_read_tile_data() throws Throwable {
        parse_dataset_and_read_tile_data("dirname", "filename.tif", "GeoTiff 7km");
    }

    @Test
    public void parse_dataset_and_read_tile_data_dirname_with_spaces() throws Throwable {
        parse_dataset_and_read_tile_data("dir name with spaces", "filename.tif", "GeoTiff 7km");
    }

    @Test
    public void parse_dataset_and_read_tile_data_filename_with_spaces() throws Throwable {
        parse_dataset_and_read_tile_data("dirname", "filename with spaces.tif", "GeoTiff 7km");
    }

    @Test
    public void parse_dataset_and_read_tile_data_dirnam_and_filename_with_spaces() throws Throwable {
        parse_dataset_and_read_tile_data("dir name with spaces", "filename with spaces.tif", "GeoTiff 7km");
    }

    @Test
    public void parse_rpf_dataset() throws Throwable {
        parse_dataset_and_read_tile_data("ctlm50", "0h4gp013.tl1", "TLM50");
    }

    private void parse_dataset_and_read_tile_data(String dirname, String filename, String expectedType) throws Throwable {
        final Context c = getTestContext();
        try(FileUtils.AutoDeleteFile testDir = FileUtils.AutoDeleteFile.createTempDir(c)){
            File dir = new File(testDir.file, dirname);
            Assert.assertTrue(dir.mkdir());

            File img = new File(dir, filename);
            File workingDir = new File(dir, "work");
            Assert.assertTrue(workingDir.mkdir());

            GdalLibrary.init();

            final byte[] testDataSamples = createTestDataSamples(519, 512);
            createTestData(img.getAbsolutePath(), testDataSamples, 519, 512, "GTiff");

            final Set<DatasetDescriptor> descs = GdalLayerInfo.INSTANCE.create(new DatasetDescriptorSpiArgs(img, workingDir));
            Assert.assertNotNull(descs);
            Assert.assertEquals(1, descs.size());

            final DatasetDescriptor desc = descs.iterator().next();

            final String imageryType = desc.getImageryTypes().iterator().next();
            Assert.assertEquals(expectedType, imageryType);

            TileReader reader = GdalTileReader.SPI.create(GdalLayerInfo.getGdalFriendlyUri(desc), new TileReaderFactory.Options());
            Assert.assertNotNull(reader);

            Assert.assertEquals(519, reader.getWidth());
            Assert.assertEquals(512, reader.getHeight());

            byte[] data = new byte[(int)(reader.getWidth()*reader.getHeight()*reader.getPixelSize())];
            reader.read(0, 0, reader.getWidth(), reader.getHeight(), (int)reader.getWidth(), (int)reader.getHeight(), data);

            Assert.assertArrayEquals(testDataSamples, data);
        }
    }

    @Test
    public void parse_generic_mosaic_dataset_and_read_tile_data() throws Throwable {
        String[] chipNames = new String[10];
        for(int i = 0; i < chipNames.length; i++)
            chipNames[i] = "chip" + i + ".tif";
        parse_mosaic_dataset_and_read_tile_data(chipNames);
    }

    @Test
    public void parse_rpf_mosaic_dataset_and_read_tile_data() throws Throwable {
        String[] chipNames = new String[10];
        for(int i = 0; i < chipNames.length; i++)
            chipNames[i] = "chip" + i + ".tif";
        parse_mosaic_dataset_and_read_tile_data(new String[]
        {
            "00000023.gn2",
            "00000033.GN2",
            "00001023.gn2",
            "00001033.GN2",
            "00002023.gn2",
            "00002033.GN2",
            "00003023.gn2",
            "00003043.GN2",
            "00003053.GN2",
            "00004000.GN2",
        });
    }

    private void parse_mosaic_dataset_and_read_tile_data(String[] chipNames) throws Throwable {
        final String dirname = "mosaic";
        final Context c = getTestContext();
        try(FileUtils.AutoDeleteFile testDir = FileUtils.AutoDeleteFile.createTempDir(c)){
            File dir = new File(testDir.file, dirname);
            Assert.assertTrue(dir.mkdir());

            File workingDir = new File(testDir.file, "work");
            Assert.assertTrue(workingDir.mkdir());

            GdalLibrary.init();

            final byte[] testDataSamples = createTestDataSamples(519, 512);
            for(String chip : chipNames)
            {
                createTestData(new File(dir, chip).getAbsolutePath(), testDataSamples, 519, 512, "GTiff");
            }

            final Set<DatasetDescriptor> descs = GdalLayerInfo.INSTANCE.create(new DatasetDescriptorSpiArgs(dir, workingDir));
            Assert.assertNotNull(descs);
            Assert.assertEquals(1, descs.size());

            final DatasetDescriptor desc = descs.iterator().next();
            Assert.assertTrue(desc instanceof MosaicDatasetDescriptor);

            MosaicDatasetDescriptor mosaic = (MosaicDatasetDescriptor)desc;
            MosaicDatabase2 db = new ATAKMosaicDatabase3();
            db.open(mosaic.getMosaicDatabaseFile());

            try(MosaicDatabase2.Cursor result = db.query(null))
            {
                while(result.moveToNext())
                {
                    // XXX - logic lifted from `GLMosaicMapLayer` -- should be well defined APIs for testing purposes
                    TileReaderFactory.Options opts = new TileReaderFactory.Options();
                    opts.asyncIO = null;
                    opts.cacheUri = null;
                    opts.preferredTileWidth = 256;
                    opts.preferredTileHeight = 256;

                    TileReader reader;
                    do
                    {
                        reader = GdalTileReader.SPI.create(result.getPath(), opts);
                        if(reader != null)
                            break;

                        String path = result.getPath();
                        if (path.startsWith("file:///"))
                            path = path.substring(7);
                        else if (path.startsWith("file://"))
                            path = path.substring(6);
                        else if (path.startsWith("file:/"))
                            path = path.substring(5);
                        else if (path.startsWith("zip://"))
                            path = path.replace("zip://", "/vsizip/");

                        Dataset dataset = GdalLibrary.openDatasetFromPath(path);
                        Assert.assertNotNull(dataset);
                        reader = new GdalTileReader(dataset,
                                dataset.GetDescription(),
                                opts.preferredTileWidth,
                                opts.preferredTileHeight,
                                opts.cacheUri,
                                opts.asyncIO);
                    } while(false);
                    Assert.assertNotNull(reader);

                    Assert.assertEquals(519, reader.getWidth());
                    Assert.assertEquals(512, reader.getHeight());

                    byte[] data = new byte[(int) (reader.getWidth() * reader.getHeight() * reader.getPixelSize())];
                    reader.read(0, 0, reader.getWidth(), reader.getHeight(), (int) reader.getWidth(), (int) reader.getHeight(), data);

                    Assert.assertArrayEquals(testDataSamples, data);
                }
            }
        }
    }
}
