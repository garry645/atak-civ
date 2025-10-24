
package com.atakmap.map.gdal;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataSource;
import com.atakmap.map.layer.feature.FeatureDataSourceContentFactory;
import com.atakmap.map.layer.feature.FeatureDefinition;
import com.atakmap.map.layer.feature.ogr.OgrFeatureDataSource;

import gov.tak.test.KernelJniTest;
import org.gdal.gdal.gdal;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.LinkedList;

public class KMLStyleTest extends KernelJniTest
{
    private static final String kml = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n" +
            "<kml xmlns=\"http://www.opengis.net/kml/2.2\">\n" +
            "<Document>\n" +
            "\t<Style id=\"defaultStyle\">\n" +
            "\t\t<LineStyle>\n" +
            "\t\t\t<width>1.5</width>\n" +
            "\t\t</LineStyle>\n" +
            "\t</Style>\n" +
            "  <Placemark>\n" +
            "    <name>Simple placemark</name>\n" +
            "    <description>Can use HTML here.</description>\n" +
            "    <Point>\n" +
            "      <coordinates>-122.08,37.42,0</coordinates>\n" +
            "    </Point>\n" +
            "  </Placemark>\n" +
            "</Document>\n" +
            "</kml>";


    @BeforeClass
    public static void setup() {
        GdalLibrary.init();
        gdal.SetConfigOption("LIBKML_RESOLVE_STYLE", "yes");
        org.gdal.ogr.ogr.RegisterAll();
        FeatureDataSourceContentFactory.register(new OgrFeatureDataSource());

    }
    //ensure kml style contains a Point Style even if not specified
    @Test
    public void containsDefaultPointStyle() {
        String f = "/vsimem/mil-sym-plugin/temp.kml";
        try {
            org.gdal.gdal.gdal.FileFromMemBuffer(f, kml.getBytes(FileSystemUtils.UTF8_CHARSET));
            FeatureDataSource.Content features = FeatureDataSourceContentFactory.parse(new File(f), "ogr");
            try {
                if (features == null) {
                    Assert.fail();
                }

                int count = 0;
                Collection<Feature> retval = new LinkedList<>();
                while (features.moveToNext(FeatureDataSource.Content.ContentPointer.FEATURE_SET)) {
                    while (features.moveToNext(FeatureDataSource.Content.ContentPointer.FEATURE)) {
                        ++count;
                        FeatureDataSource.FeatureDefinition def = features.get();

                        Assert.assertEquals(def.styleCoding, FeatureDefinition.STYLE_OGR);
                        String rawStyle = (String)def.rawStyle;

                        Assert.assertTrue(rawStyle.contains("SYMBOL("));

                    }
                }
                Assert.assertNotEquals(count, 0);
            }
            finally {
            }
        }
        finally {
            org.gdal.gdal.gdal.Unlink(f);
        }
    }

    @Before
    public void beforeTest() {
    }

}
