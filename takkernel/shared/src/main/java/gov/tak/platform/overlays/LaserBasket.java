package gov.tak.platform.overlays;

import com.atakmap.coremap.conversions.Angle;
import com.atakmap.coremap.conversions.AngleUtilities;
import com.atakmap.coremap.conversions.Span;
import com.atakmap.coremap.conversions.SpanUtilities;
import com.atakmap.coremap.maps.coords.NorthReference;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.map.layer.feature.FeatureDataStore2;
import com.atakmap.map.layer.feature.style.ArrowStrokeStyle;
import com.atakmap.map.layer.feature.style.BasicFillStyle;
import com.atakmap.map.layer.feature.style.BasicStrokeStyle;
import com.atakmap.map.layer.feature.style.CompositeStyle;
import com.atakmap.map.layer.feature.style.LabelPointStyle;
import com.atakmap.map.layer.feature.style.Style;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;

import gov.tak.api.engine.map.coords.GeoCalculations;
import gov.tak.api.engine.map.coords.GeoPoint;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.platform.graphics.Color;

import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Polygon;


public final class LaserBasket
{
    /** Number of segments for each arc */
    public static final int NUM_SEGMENTS = 5;

    private IGeoPoint target;

    private IGeoPoint designator;

    private boolean showAngleLabels;

    private double laserBasketRange;

    private LineString targetLine;

    private Polygon[] safetyZones;

    private ArrayList<Feature> basket;

    private ArrayList<String[]> zoneLabels;

    private LaserBasket(Builder builder)
    {
        basket = new ArrayList<>(6);
        zoneLabels = new ArrayList<>(5);
        this.target = builder.target;
        this.designator = builder.designator;
        this.showAngleLabels = builder.showAngleLabels;
        this.laserBasketRange = builder.laserBasketRange;

        if (builder.addTargetLine)
        {
            targetLine = createTargetLine();
        }

        if (builder.addSafetyZones)
        {
            safetyZones = createSafetyZones();
        }

        basket.add(targetLineFeature(targetLine, showAngleLabels));
        for(int n=0; n < safetyZones.length; n++)
        {
            Feature[] safetyZoneFeature = new Feature[3]; // polygon and optional label
            safetyZoneFeature(safetyZones[n], safetyZoneFeature, zoneLabels, n, showAngleLabels);
            for(Feature f : safetyZoneFeature) {
                if (f != null)
                    basket.add(f);
            }
        }
    }

    public Collection<Feature> getBasket()
    {
        return basket;
    }

    private LineString createTargetLine()
    {
        LineString rtnVal = new LineString(2);
        rtnVal.addPoint(target.getLongitude(), target.getLatitude());
        rtnVal.addPoint(designator.getLongitude(), designator.getLatitude());
        return rtnVal;
    }

    private Polygon[] createSafetyZones()
    {
        // compute the angle to the designator
        double rawBearing = GeoCalculations.bearing(designator, target);

        // use the bearing of the given designator
        int bearing = (int) Math.round(GeoCalculations
                .convertFromTrueToMagnetic(designator, rawBearing));


        IGeoPoint dPoint = designator;
        if (!Double.isNaN(laserBasketRange) &&  laserBasketRange > 0) {
            dPoint = GeoCalculations.pointAtDistance(target, rawBearing - 180, laserBasketRange);
        }

        Polygon[] rtnVal = new Polygon[5];
        rtnVal[0] = makeWedge(target, dPoint, 1.0, -10.0, 20.0);
        zoneLabels.add(0, new String[] { format(bearing + 10), format(bearing - 10) });
        rtnVal[1] = makeWedge(target, dPoint, 1.0, 10.0, 35.0);
        zoneLabels.add(1, new String[] { "" , format(bearing - 45) });
        rtnVal[2] = makeWedge(target, dPoint, 1.0, 315.0, 35.0);
        zoneLabels.add(2, new String[] { format(bearing + 45), "" });
        rtnVal[3] = makeWedge(target, dPoint, 1.0, 45.0, 15.0);
        zoneLabels.add(3, new String[] {"", format(bearing - 60) });
        rtnVal[4] = makeWedge(target, dPoint, 1.0, 300.0, 15.0);
        zoneLabels.add(4, new String[] {format(bearing + 60), "" });
        return rtnVal;
    }

    private String format(final double mag) {
        return AngleUtilities.format(mag) + "M";
    }

    public IGeoPoint getTarget()
    {
        return target;
    }

    public IGeoPoint getDesignator()
    {
        return designator;
    }

    public double getLaserBasketRange() { return laserBasketRange; }

    private static Feature targetLineFeature(LineString lineString, boolean label)
    {
        final float strokeWeight = 1.0f;
        final int renderColor = Color.RED;

        final GeoPoint designator = new GeoPoint(lineString.getY(1), lineString.getX(1));
        final GeoPoint target = new GeoPoint(lineString.getY(0), lineString.getX(0));

        Style[] s = new Style[label ? 3 : 2];
        s[0] = new ArrowStrokeStyle(16f, 1, (short)-1, 0xFF000000 & renderColor, strokeWeight + 2f, BasicStrokeStyle.EXTRUDE_NONE);
        s[1] = new ArrowStrokeStyle(16f, 1, (short)-1, renderColor, strokeWeight, BasicStrokeStyle.EXTRUDE_NONE);
        if(label) {
            s[2] = new LabelPointStyle(
                    createTargetLineLabel(
                            designator,
                            target,
                            Angle.DEGREE,
                            NorthReference.MAGNETIC,
                            Span.METRIC),
                    Color.WHITE,
                    Color.BLACK,
                    LabelPointStyle.ScrollMode.OFF/*,
                    0f,
                    0, 0,
                    (float)GeoCalculations.bearing(designator, target) + 90f,
                    true*/);
        }

        return new Feature(
                FeatureDataStore2.FEATURESET_ID_NONE,
                FeatureDataStore2.FEATURE_ID_NONE,
                null,
                lineString,
                new CompositeStyle(s),
                null,
                Feature.AltitudeMode.ClampToGround,
                0d,
                FeatureDataStore2.TIMESTAMP_NONE,
                FeatureDataStore2.FEATURE_ID_NONE);
    }

    private static Feature wedgeLabelFeature(LineString wedge, int segment, String text)
    {
        if(text == null || text.isEmpty())
            return null;
        final IGeoPoint p0 = new GeoPoint(wedge.getY(segment), wedge.getX(segment));
        final IGeoPoint p1 = new GeoPoint(wedge.getY(segment+1), wedge.getX(segment+1));

        LineString anchor = new LineString(2);
        anchor.addPoint(wedge.getX(segment), wedge.getY(segment));
        anchor.addPoint(wedge.getX(segment+1), wedge.getY(segment+1));

        return new Feature(
                FeatureDataStore2.FEATURESET_ID_NONE,
                FeatureDataStore2.FEATURESET_ID_NONE,
                null,
                anchor,
                new LabelPointStyle(
                        text,
                        Color.WHITE,
                        Color.BLACK,
                        LabelPointStyle.ScrollMode.OFF/*,
                        0f,
                        0, 0,
                        (float)GeoCalculations.bearing(p0, p1),
                        true*/),
                new AttributeSet(),
                Feature.AltitudeMode.ClampToGround,
                0d,
                FeatureDataStore2.TIMESTAMP_NONE,
                FeatureDataStore2.FEATURE_ID_NONE);
    }

    private static void safetyZoneFeature(Polygon polygon, Feature[] feature, ArrayList<String[]> zoneLabels, int wedgeNumber, boolean label)
    {
        final float strokeWeight = 1.0f;
        int renderColor;
        switch (wedgeNumber) {
            case 0:
                renderColor = 0x50FF0000;
                break;
            case 1:
            case 2:
                renderColor = 0x5000FF00;
                break;
            case 3:
            case 4:
                renderColor = 0x500000FF;
                break;
            default:
                renderColor = 0x50FFFFFF;
        }

        // XXX - s[1] maintains backwards compatibility with bad consumer implementation detail.
        //       remove at next major version bump.
        Style s = new CompositeStyle(new Style[] {
                new BasicFillStyle(renderColor),
                new BasicStrokeStyle(renderColor, strokeWeight, BasicStrokeStyle.EXTRUDE_NONE),
                new BasicStrokeStyle(Color.BLACK, strokeWeight, BasicStrokeStyle.EXTRUDE_NONE)
        });

        AttributeSet attributeSet = new AttributeSet();
        attributeSet.setAttribute("label", zoneLabels.get(wedgeNumber));

        feature[0] = new Feature(
                1L,
                FeatureDataStore2.FEATURESET_ID_NONE,
                null,
                polygon,
                s,
                attributeSet,
                Feature.AltitudeMode.ClampToGround,
                0d,
                FeatureDataStore2.TIMESTAMP_NONE,
                FeatureDataStore2.FEATURE_ID_NONE);

        if(label) {
            LineString wedge = polygon.getExteriorRing();
            String[] segmentLabels = zoneLabels.get(wedgeNumber);

            int labelFeature = 1;
            feature[labelFeature] = wedgeLabelFeature(wedge, 0, segmentLabels[0]);
            if(feature[labelFeature] != null) labelFeature++;
            feature[labelFeature] = wedgeLabelFeature(wedge, wedge.getNumPoints()-2, segmentLabels[1]);
            if(feature[labelFeature] != null) labelFeature++;
        }
    }

    private Polygon makeWedge(IGeoPoint pointX, IGeoPoint pointZ, final double multiplier, final double offsetAngle, final double angle)
    {
        Polygon rtnVal = new Polygon(2);
        double distanceMultiplier = multiplier;
        double angleX = offsetAngle;
        double angleXPrime = angle;

        if (pointX != null && pointZ != null && !Double.isNaN(angleX) && !Double.isNaN(angleXPrime))
        {
            // XXX We need to be using the glorthoview.scale and glorthoview.drawLat to generate angles
            // properly
            // otherwise the displayed angle will be skewed from what is represented on the map.

            // Since I don't want to make an entirely new GLMapItem for GLWedge, I'm just going to use
            // the DistanceCalculations to get the correct points given angleX and angleXprime which
            // will be
            // offset from the azimuth of Point X to Point Z

            // Initialize 3 points representing X, Y, and Y' respectively
            LineString corners = new LineString(2);

            // Set the known point, the source, X
            corners.addPoint(pointX.getLongitude(), pointX.getLatitude());

            // Get the true azimuth of point X to point Z
            // This will be used to apply the offset
            // Get the distance in meters so we can apply the multiplier for the
            // wedge's radius.
            double azimuth = GeoCalculations.bearing(pointX, pointZ);
            double distance = GeoCalculations.distance(pointX, pointZ) * distanceMultiplier;
            double fraction = 1.0 / NUM_SEGMENTS;

            // include the starting point and make the ending point the same as the starting point
            for (int i = 0; i <= NUM_SEGMENTS; i++) {
                IGeoPoint gp = GeoCalculations.pointAtDistance(pointX, azimuth - angleX - angleXPrime * i * fraction, distance);
                corners.addPoint(gp.getLongitude(), gp.getLatitude());
            }
            corners.addPoint(pointX.getLongitude(), pointX.getLatitude());
            rtnVal.addRing(corners);
        }

        return rtnVal;
    }

    private static String createTargetLineLabel(IGeoPoint p0, IGeoPoint p1,
                                                Angle bearingUnits,
                                                NorthReference northReference,
                                                int rangeUnits) {

        double bearing = GeoCalculations.bearing(p0, p1);
        double range = GeoCalculations.distance(p0, p1);

        String bs;
        if (northReference == NorthReference.GRID) {
            double gridConvergence = GeoCalculations
                    .computeGridConvergence(p0, p1);
            bs = AngleUtilities.format(AngleUtilities.wrapDeg(
                    bearing - gridConvergence), bearingUnits) + "G";
        } else if (northReference == NorthReference.MAGNETIC) {
            double bearingMag = GeoCalculations.convertFromTrueToMagnetic(
                    p0, bearing);
            bs = AngleUtilities.format(bearingMag, bearingUnits) + "M";
        } else {
            bs = AngleUtilities.format(bearing, bearingUnits) + "T";
        }

        String direction = "\u2192";
        if (bearing > 180 && bearing < 360)
            direction = "\u2190";

        //start out with direction
        String text = direction + " ";

        text += bs + "   "
                + SpanUtilities.formatType(rangeUnits, range, Span.METER);

        return text;
    }

    public static class Builder
    {
        private IGeoPoint target;

        private IGeoPoint designator;

        private boolean addTargetLine;

        private boolean addSafetyZones;

        private boolean showAngleLabels;

        private double laserBasketRange = Double.NaN;


        public Builder()
        {
        }

        public Builder setAddTargetLine(boolean addTargetLine)
        {
            this.addTargetLine = addTargetLine;
            return this;
        }

        public Builder setAddSafetyZones(boolean addSafetyZones)
        {
            this.addSafetyZones = addSafetyZones;
            return this;
        }

        public Builder setTarget(IGeoPoint target)
        {
            this.target = target;
            return this;
        }

        public Builder setDesignator(IGeoPoint designator)
        {
            this.designator = designator;
            return this;
        }

        public Builder setShowAngleLabels(boolean showAngleLabels)
        {
            this.showAngleLabels = showAngleLabels;
            return this;
        }

        public Builder setLaserBasketRange(double laserBasketRange)
        {
            this.laserBasketRange = laserBasketRange;
            return this;
        }

        public LaserBasket build()
        {
            Objects.requireNonNull(target, "A target must be provided");
            Objects.requireNonNull(designator, "A designator must be provided");

            return new LaserBasket(this);
        }

        public Builder buildFrom(LaserBasket laserBasket)
        {
            designator = laserBasket.getDesignator();
            target = laserBasket.getTarget();
            addTargetLine = laserBasket.targetLine != null;
            addSafetyZones = laserBasket.basket.size() == 6;
            showAngleLabels = laserBasket.showAngleLabels;
            laserBasketRange = laserBasket.laserBasketRange;
            return this;
        }
    }
}
