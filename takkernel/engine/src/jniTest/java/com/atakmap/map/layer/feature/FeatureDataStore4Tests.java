
package com.atakmap.map.layer.feature;

import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Point;
import org.junit.Assert;
import org.junit.Test;

public abstract class FeatureDataStore4Tests extends FeatureDataStore3Tests
{
    @Override
    abstract protected FeatureDataStore4 createDataStore();

    @Test
    public void testAltitudeModeTraitsLineString() throws DataStoreException {

        FeatureDataStore4 retval = createDataStore();

        long fsid = retval.insertFeatureSet(new FeatureSet(
                "Hello3D", // what generated the features
                "Buildings", // the type of the featuers
                "Buildings", // the feature set name
                Double.MAX_VALUE, // min resolution threshold
                0d));

        retval.setFeatureSetVisible(fsid, true);

        LineString ls = new LineString(3);
        ls.addPoint(-78.78944, 35.77100, 201);
        ls.addPoint(-78.78900, 35.77120, 260);
        ls.addPoint(-78.78877, 35.77000, 101);

        long id_test_relative = retval.insertFeature(new Feature(fsid, FeatureDataStore2.FEATURE_ID_NONE,
                "route", ls, null, null, traits(Feature.AltitudeMode.Relative, 0, Feature.LineMode.Rhumb), FeatureDataStore2.TIMESTAMP_NONE, FeatureDataStore2.FEATURE_VERSION_NONE));

        long id_test_clamp = retval.insertFeature(new Feature(fsid, FeatureDataStore2.FEATURE_ID_NONE,
                "route", ls, null, null, traits(Feature.AltitudeMode.ClampToGround, 0, Feature.LineMode.Rhumb), FeatureDataStore2.TIMESTAMP_NONE, FeatureDataStore2.FEATURE_VERSION_NONE));

        long id_test_absolute = retval.insertFeature(new Feature(fsid, FeatureDataStore2.FEATURE_ID_NONE,
                "route", ls, null, null, traits(Feature.AltitudeMode.Absolute, 0, Feature.LineMode.Rhumb), FeatureDataStore2.TIMESTAMP_NONE, FeatureDataStore2.FEATURE_VERSION_NONE));

        Assert.assertEquals("relative", Feature.AltitudeMode.Relative,
                Utils.getFeature(retval, id_test_relative).getTraits().altitudeMode);
        Assert.assertEquals("clamp", Feature.AltitudeMode.ClampToGround,
                Utils.getFeature(retval, id_test_clamp).getTraits().altitudeMode);
        Assert.assertEquals("absolute", Feature.AltitudeMode.Absolute,
                Utils.getFeature(retval, id_test_absolute).getTraits().altitudeMode);
    }

    @Test
    public void testAltitudeModeTraitsPoint() throws DataStoreException {
        FeatureDataStore4 retval = createDataStore();

        long fsid = retval.insertFeatureSet(new FeatureSet(
                "Hello3D", // what generated the features
                "Buildings", // the type of the featuers
                "Buildings", // the feature set name
                Double.MAX_VALUE, // min resolution threshold
                0d));

        retval.setFeatureSetVisible(fsid, true);

        Point point = new Point(1, 1);

        long id_test_relative = retval.insertFeature(new Feature(fsid, FeatureDataStore2.FEATURE_ID_NONE,
                "route", point, null, null, traits(Feature.AltitudeMode.Relative, 0, Feature.LineMode.Rhumb), FeatureDataStore2.TIMESTAMP_NONE, FeatureDataStore2.FEATURE_VERSION_NONE));

        long id_test_clamp = retval.insertFeature(new Feature(fsid, FeatureDataStore2.FEATURE_ID_NONE,
                "route", point, null, null, traits(Feature.AltitudeMode.ClampToGround, 0, Feature.LineMode.Rhumb), FeatureDataStore2.TIMESTAMP_NONE, FeatureDataStore2.FEATURE_VERSION_NONE));

        long id_test_absolute = retval.insertFeature(new Feature(fsid, FeatureDataStore2.FEATURE_ID_NONE,
                "route", point, null, null, traits(Feature.AltitudeMode.Absolute, 0, Feature.LineMode.Rhumb), FeatureDataStore2.TIMESTAMP_NONE, FeatureDataStore2.FEATURE_VERSION_NONE));

        Assert.assertEquals("relative", Feature.AltitudeMode.Relative,
                Utils.getFeature(retval, id_test_relative).getTraits().altitudeMode);
        Assert.assertEquals("clamp", Feature.AltitudeMode.ClampToGround,
                Utils.getFeature(retval, id_test_clamp).getTraits().altitudeMode);
        Assert.assertEquals("absolute", Feature.AltitudeMode.Absolute,
                Utils.getFeature(retval, id_test_absolute).getTraits().altitudeMode);

        retval.updateFeature(id_test_absolute, FeatureDataStore4.PROPERTY_FEATURE_TRAITS, null, null,
                null, null, 0, traits(Feature.AltitudeMode.ClampToGround, 0, Feature.LineMode.Rhumb));
        Assert.assertEquals("update_absolute_to_clamp", Feature.AltitudeMode.ClampToGround,
                Utils.getFeature(retval, id_test_absolute).getTraits().altitudeMode);

        retval.updateFeature(id_test_absolute, FeatureDataStore4.PROPERTY_FEATURE_TRAITS, null, null,
                null, null, 0, traits(Feature.AltitudeMode.Relative, 0, Feature.LineMode.Rhumb));
        Assert.assertEquals("update_clamp_to_relative", Feature.AltitudeMode.Relative,
                Utils.getFeature(retval, id_test_absolute).getTraits().altitudeMode);

        retval.updateFeature(id_test_absolute, FeatureDataStore4.PROPERTY_FEATURE_TRAITS, null, null,
                null, null, 0, traits(Feature.AltitudeMode.Absolute, 0, Feature.LineMode.Rhumb));
        Assert.assertEquals("update_relative_to_absolute", Feature.AltitudeMode.Absolute,
                Utils.getFeature(retval, id_test_absolute).getTraits().altitudeMode);

        retval.updateFeature(id_test_absolute, FeatureDataStore4.PROPERTY_FEATURE_TRAITS, null, null, null,
                null, 0, traits(Feature.AltitudeMode.Absolute, -1, Feature.LineMode.GreatCircle));
        Assert.assertEquals("extrude", -1d,
                Utils.getFeature(retval, id_test_absolute).getTraits().extrude, 0.001d);
        Assert.assertEquals("linemode", Feature.LineMode.GreatCircle,
                Utils.getFeature(retval, id_test_absolute).getTraits().lineMode);

        retval.updateFeature(id_test_absolute, FeatureDataStore4.PROPERTY_FEATURE_TRAITS, null, null, null,
                null, 0, traits(Feature.AltitudeMode.Absolute, 1, Feature.LineMode.Rhumb));
        Assert.assertEquals("extrude", 1d,
                Utils.getFeature(retval, id_test_absolute).getTraits().extrude, 0.001d);
        Assert.assertEquals("linemode", Feature.LineMode.Rhumb,
                Utils.getFeature(retval, id_test_absolute).getTraits().lineMode);

    }

    @Test
    public void testAltitudeModeTraitsPoint2() throws DataStoreException {
        FeatureDataStore4 retval = createDataStore();

        long fsid = retval.insertFeatureSet(new FeatureSet(
                "Hello3D", // what generated the features
                "Buildings", // the type of the featuers
                "Buildings", // the feature set name
                Double.MAX_VALUE, // min resolution threshold
                0d));

        retval.setFeatureSetVisible(fsid, true);

        Point point = new Point(1, 1);

        long id_test_absolute = retval.insertFeature(new Feature(fsid, FeatureDataStore2.FEATURE_ID_NONE,
                "route", point, null, null, traits(Feature.AltitudeMode.Absolute, 0.0, Feature.LineMode.Rhumb), FeatureDataStore2.TIMESTAMP_NONE, FeatureDataStore2.FEATURE_VERSION_NONE));

        Assert.assertEquals("absolute", Feature.AltitudeMode.Absolute,
                Utils.getFeature(retval, id_test_absolute).getTraits().altitudeMode);
        retval.updateFeature(id_test_absolute, FeatureDataStore3.PROPERTY_FEATURE_NAME, "point", null, null,
                null, 0, traits(Feature.AltitudeMode.ClampToGround, -1.0, Feature.LineMode.Rhumb));

        Assert.assertEquals("update_no_change_altitudeMode", Feature.AltitudeMode.Absolute,
                Utils.getFeature(retval, id_test_absolute).getTraits().altitudeMode);
        Assert.assertEquals("update_no_change_extrude", 0d,
                Utils.getFeature(retval, id_test_absolute).getTraits().extrude, 0.001d);
        Assert.assertEquals("update_no_change_linemode", Feature.LineMode.Rhumb,
                Utils.getFeature(retval, id_test_absolute).getTraits().lineMode);
        Assert.assertEquals("update_nname", "point",
                Utils.getFeature(retval, id_test_absolute).getName());
    }

    private static Feature.Traits traits(Feature.AltitudeMode altitudeMode, double extrude, Feature.LineMode lineMode)
    {
        Feature.Traits traits = new Feature.Traits();
        traits.altitudeMode = altitudeMode;
        traits.extrude = extrude;
        traits.lineMode = lineMode;
        return traits;
    }
}
