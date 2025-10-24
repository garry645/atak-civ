
package com.atakmap.android.rubbersheet.data.export;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.AttributeSet;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.model.ModelInfo;
import com.atakmap.math.Matrix;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.TimeZone;

import com.atakmap.coremap.locale.LocaleUtil;

// TODO - refactor as `takkernel` JNI interface to C++ impl

final class ZipCommentInfo {
    final static int schemaVersionCurrent = 1;
    final static int schemaVersion1 = 1;
    final String schemaVersionKey = "schemaVersion";

    final static int maxZipCommentSize = (1 << 16) - 1;

    final static Map<GeoPoint.AltitudeReference, String> _altRefAliases = new HashMap<>();
    static {
        _altRefAliases.put(GeoPoint.AltitudeReference.AGL, "AGL");
        _altRefAliases.put(GeoPoint.AltitudeReference.HAE, "HAE");
    }

    static JSONObject to_json(GeoPoint point) throws JSONException {
        JSONObject j = new JSONObject();
        put(j, "longitude", point.getLongitude());
        put(j, "latitude", point.getLatitude());
        put(j, "altitude", point.getAltitude());
        j.put("altitudeRef", _altRefAliases.get(point.getAltitudeReference()));
        put(j, "ce90", point.getCE());
        put(j, "le90", point.getLE());
        return j;
    }

    static final Map<ModelInfo.AltitudeMode, String> _altModeAliases = new HashMap<>();
    static {
        _altModeAliases.put(ModelInfo.AltitudeMode.ClampToGround,
                "ClampToGround");
        _altModeAliases.put(ModelInfo.AltitudeMode.Relative, "Relative");
        _altModeAliases.put(ModelInfo.AltitudeMode.Absolute, "Absolute");
    }

    static JSONObject to_json(Envelope envelope) throws JSONException {
        JSONObject j = new JSONObject();
        j.put("maxX", envelope.maxX);
        j.put("maxY", envelope.maxY);
        j.put("maxZ", envelope.maxZ);
        j.put("minX", envelope.minX);
        j.put("minY", envelope.minY);
        j.put("minZ", envelope.minZ);
        return j;
    }

    JSONObject to_json(Matrix matrix) throws JSONException {
        JSONObject j = new JSONObject();
        double[] mx = new double[16];
        matrix.get(mx);
        JSONArray arr = new JSONArray();
        for (double v : mx)
            arr.put(v);
        j.put("matrix", arr);
        return j;
    }

    // XXX - ignoring metadata

    Envelope envelope = new Envelope();
    GeoPoint location = GeoPoint.createMutable();
    ModelInfo.AltitudeMode altitudeMode;
    int projection_srid;
    String projection_wkt;
    Matrix localFrame = Matrix.getIdentity();
    double displayResolution_min;
    double displayResolution_max;
    double resolution;
    String date; // YYYYMMDDhhmmss UTC
    AttributeSet metadata;

    ZipCommentInfo() {
        altitudeMode = ModelInfo.AltitudeMode.ClampToGround;
        projection_srid = -1;
        displayResolution_max = displayResolution_min = Double.NaN;
        resolution = Double.NaN;
    }

    public String toString() {
        try {
            JSONObject j = new JSONObject();
            j.put(schemaVersionKey, schemaVersion1);
            j.put("envelope", to_json(envelope));
            j.put("location", to_json(location));
            j.put("altitudeMode", _altModeAliases.get(altitudeMode));
            JSONObject projection = new JSONObject();
            projection.put("srid", projection_srid);
            projection.put("wkt", projection_wkt);
            j.put("projection", projection);
            j.put("localFrame", to_json(localFrame));
            JSONObject displayResolution = new JSONObject();
            put(displayResolution, "max", displayResolution_max);
            put(displayResolution, "min", displayResolution_min);
            if (displayResolution.length() > 0)
                j.put("displayResolution", displayResolution);
            put(j, "resolution", resolution);
            j.put("metadata", new JSONArray());

            final SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMddHHmmss",
                    LocaleUtil.US);
            fmt.setTimeZone(TimeZone.getTimeZone("UTC"));
            j.put("date", fmt.format(new Date()) + " UTC");

            return j.toString(4).replace("\"NaN\"", "NaN");
        } catch (JSONException e) {
            return null;
        }
    }

    static void put(JSONObject json, String k, double v) throws JSONException {
        if (Double.isNaN(v))
            json.put(k, 0d);
        else
            json.put(k, v);
    }
}
