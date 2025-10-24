package com.atakmap.map.gdal;

import android.content.Context;
import com.atakmap.map.elevation.ElevationChunk;
import com.atakmap.map.elevation.ElevationData;
import gov.tak.test.KernelJniTest;
import gov.tak.test.util.FileUtils;
import org.gdal.gdal.Dataset;
import org.gdal.gdal.Driver;
import org.gdal.gdal.gdal;
import org.gdal.gdalconst.gdalconstConstants;
import org.j3d.geom.terrain.FractalTerrainGenerator;
import org.junit.Assert;
import org.junit.Test;

public class GdalElevationChunkTest extends KernelJniTest {
    private static void createTestData(String filePath, float[][] grid, double[] geotransform, int srid, String driverName) {
        Driver driver = gdal.GetDriverByName(driverName);
        Dataset ds = driver.Create(filePath, grid.length, grid[0].length, 1, gdalconstConstants.GDT_Float32);
        if (ds == null) {
            return;
        }

        float[] data = new float[grid.length*grid[0].length];
        for(int i = 0; i < grid.length; i++)
            for(int j = 0; j < grid[i].length; j++)
                data[(i*grid[i].length)+j] = grid[i][j];

        ds.GetRasterBand(1).WriteRaster(0, 0, grid.length, grid[0].length, data);
        // https://gdal.org/tutorials/geotransforms_tut.html
        ds.SetGeoTransform(geotransform);
        ds.SetProjection(GdalLibrary.getWkt(srid));
        ds.delete();
    }

    //[479993.99996347405, 1.0, 0.0, 4040005.999987617, 0.0, -1.0]
    //26913
    @Test
    public void regression_check() throws Throwable {
        final Context c = getTestContext();
        try(FileUtils.AutoDeleteFile testFile = FileUtils.AutoDeleteFile.createTempFile(c)){
            GdalLibrary.init();

            FractalTerrainGenerator terrainGenerator = new FractalTerrainGenerator(3601, 3601, 1024, 9, 1.5f, 0x12345678);
            terrainGenerator.setSeaData(false, 0f);
            final float[][] heightmap = terrainGenerator.generate();
            createTestData(testFile.file.getAbsolutePath(), heightmap, new double[] {479993.99996347405, 1.0, 0.0, 4040005.999987617, 0.0, -1.0}, 26913, "GTiff");

            Dataset ds = GdalLibrary.openDatasetFromFile(testFile.file);
            ElevationChunk chunk = GdalElevationChunk.create(ds, true, "Geotiff", 1d, ElevationData.MODEL_TERRAIN, true);
            Assert.assertNotNull(chunk);

            double[] sampleLLA = new double[] {
                    -105.21979115301347, 36.50437884342893, Double.NaN,
                    -105.2193866111655, 36.50423915902729, Double.NaN,
                    -105.21784479222215, 36.504799674163955, Double.NaN,
                    -105.22219634774677, 36.503327849902476, Double.NaN,
                    -105.22201494439805, 36.504013684554614, Double.NaN,
                    -105.22304794149368, 36.5047951256795, Double.NaN,
                    -105.22230302726345, 36.50072624451322, Double.NaN,
                    -105.21864182866042, 36.50288507394384, Double.NaN,
                    -105.22219766236678, 36.50408825533591, Double.NaN,
                    -105.21875319038816, 36.505091273182046, Double.NaN,
                    -105.21931730599165, 36.50290999645572, Double.NaN,
                    -105.21863057663599, 36.50159066885666, Double.NaN,
                    -105.223026322793, 36.501765739454214, Double.NaN,
                    -105.22132523675732, 36.50282570263571, Double.NaN,
                    -105.22191943128016, 36.50466615009293, Double.NaN,
                    -105.21858384397592, 36.50184514791746, Double.NaN,
                    -105.2176583485432, 36.50077925366366, Double.NaN,
                    -105.21766834744788, 36.50498494277708, Double.NaN,
                    -105.21766644125682, 36.50141809167193, Double.NaN,
                    -105.21767311836955, 36.50467299164159, Double.NaN,
                    -105.2176650332056, 36.50130641581207, Double.NaN,
                    -105.21766171330418, 36.50221503429479, Double.NaN,
                    -105.21766455716774, 36.500954095645035, Double.NaN,
                    -105.21766408867673, 36.50269282237917, Double.NaN,
                    -105.2176586131201, 36.500585086730624, Double.NaN,
                    -105.21767165024872, 36.503103812124465, Double.NaN,
                    -105.21766130369393, 36.50289099119838, Double.NaN,
                    -105.21766859974215, 36.504775419696394, Double.NaN,
                    -105.21766919135112, 36.504165162062485, Double.NaN,
                    -105.21766205139053, 36.502213295152224, Double.NaN,
                    -105.21766645669226, 36.50458384096118, Double.NaN,
                    -105.2176669142706, 36.503442259699554, Double.NaN,
                    -105.2176702734895, 36.50239466533315, Double.NaN,
                    -105.21765815252216, 36.50128257951041, Double.NaN,
            };
            final boolean sampled = chunk.sample(sampleLLA, 0, sampleLLA.length/3);
            Assert.assertTrue(sampled);
            double[] heights = { -574.8398342695506, -790.420420709581, -200.75094387573503, -210.277093026209, -579.8479291863194, -262.7706171764427, 409.518502089477, 671.6215412335782, -285.0837311184961, -861.1932257261727, 238.3870226437875, -289.37086705404676, -307.8744195993134, -249.0223276387412, -453.20272663120727, -307.1962647815421, 286.5848337523639, -34.598383345874026, 165.12784885801375, -554.913905672729, -157.17805199697614, 189.58348323311657, 122.7397229699418, 435.58309925720096, 18.56972063705325, -76.18621545296628, 389.1992430537939, -352.23968747258186, -281.3020772607997, 191.47427896084264, -507.36815437767655, -209.40057726413943, 373.36903110332787, -101.08307260088623,};
            for(int i = 0; i < heights.length; i++) {
                Assert.assertEquals(heights[i], sampleLLA[(i*3)+2], 0.01d);
            }
        }
    }
}
