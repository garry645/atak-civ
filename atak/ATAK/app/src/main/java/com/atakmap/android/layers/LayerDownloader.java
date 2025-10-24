
package com.atakmap.android.layers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Pair;

import com.atakmap.android.drawing.mapItems.DrawingRectangle;
import com.atakmap.android.editableShapes.Rectangle;
import com.atakmap.android.importexport.ImportExportMapComponent;
import com.atakmap.android.importexport.ImportReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.ipc.AtakBroadcast.DocumentedIntentFilter;
import com.atakmap.android.layers.wms.DownloadAndCacheService;
import com.atakmap.android.maps.Polyline;
import com.atakmap.android.maps.Shape;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.coremap.maps.coords.GeoCalculations;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.contentservices.CacheRequest;
import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.map.layer.feature.geometry.LineString;
import com.atakmap.map.layer.feature.geometry.Polygon;
import com.atakmap.map.layer.raster.tilematrix.TileClient;
import com.atakmap.map.layer.raster.tilematrix.TileClientFactory;
import com.atakmap.math.MathUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import gov.tak.api.util.Disposable;

/**
 * Helper class for downloading tile from map layers
 */
public class LayerDownloader extends BroadcastReceiver implements Disposable {

    private static final String TAG = "LayerDownloader";

    private static final String DEFAULT_CALLBACK_ALIAS = "__default__";

    static LayerDownloader _instance = null;

    private final Context _context;

    private RequestBuilder _defaultRequest;
    private Map<String, Callback> _callback = new ConcurrentHashMap<>();


    public LayerDownloader(Context context) {
        _context = context;

        _defaultRequest = new RequestBuilder();
    }

    public static LayerDownloader getInstance(Context context) {
        if (_instance == null)
            _instance = new LayerDownloader(context);
        return _instance;
    }

    /**
     * Dispose the downloader by resetting the shape and unregistering receiver
     */
    @Override
    public void dispose() {
        reset();
        _callback.clear();
        setCallback("__null__", null);
    }


    /**
     * Set the event callback for the downloader
     * @param callback Callback
     */
    public void setCallback(String alias, Callback callback) {
        if (_callback.isEmpty() && callback != null) {
            AtakBroadcast.getInstance().registerReceiver(this,
                    new DocumentedIntentFilter(
                            DownloadAndCacheService.BROADCAST_ACTION,
                            "Tracks layer download events"));
            AtakBroadcast.getInstance().registerReceiver(this,
                    new DocumentedIntentFilter(
                            LayersManagerBroadcastReceiver.ACTION_TOOL_FINISHED,
                            ""));
        }
        if (callback != null)
            _callback.put(alias, callback);
        else
            _callback.remove(alias);
        if (_callback.isEmpty())
            AtakBroadcast.getInstance().unregisterReceiver(this);
    }


    /**
     * Start the download service for a tile set
     * The shape, title, and source URI must be set prior to calling this
     * @return True if download started
     */
    public boolean startDownload(RequestBuilder request) {
        if (request._shape == null) {
            Log.e(TAG, "Failed to start download - shape is null");
            return false;
        }

        if (request._title == null) {
            Log.e(TAG, "Failed to start download - title is null");
            return false;
        }

        if (request._sourceURI == null) {
            Log.e(TAG, "Failed to start download - layer source URI is null");
            return false;
        }

        GeoPoint[] points = request.getPoints();
        if (points.length == 0) {
            Log.e(TAG, "Failed to start download - shape has no points");
            return false;
        }

        Intent i = new Intent();
        i.setClass(_context, DownloadAndCacheService.class);
        i.putExtra(DownloadAndCacheService.QUEUE_DOWNLOAD, "");
        i.putExtra(DownloadAndCacheService.TITLE, request._title);
        if (request._cacheURI != null)
            i.putExtra(DownloadAndCacheService.CACHE_URI, request._cacheURI);
        if (request._defaultDownloadDirectory != null)
            i.putExtra(DownloadAndCacheService.CACHE_DIR,
                    request._defaultDownloadDirectory.getAbsolutePath());
        i.putExtra(DownloadAndCacheService.SOURCE_URI, request._sourceURI);
        i.putExtra(DownloadAndCacheService.UPPERLEFT, request.getUpperLeft());
        i.putExtra(DownloadAndCacheService.LOWERRIGHT, request.getLowerRight());
        i.putExtra(DownloadAndCacheService.GEOMETRY, points);
        i.putExtra(DownloadAndCacheService.MIN_RESOLUTION, request._minRes);
        i.putExtra(DownloadAndCacheService.MAX_RESOLUTION, request._maxRes);
        if (request._callbackAlias != null)
            i.putExtra(DownloadAndCacheService.JOB_UID, request._callbackAlias);
        if (request._importMimeAndContent != null) {
            Intent importCallback = new Intent(
                    ImportExportMapComponent.ACTION_IMPORT_DATA);
            if (request._cacheURI != null)
                importCallback.putExtra(ImportReceiver.EXTRA_URI,
                        request._cacheURI);
            else
                importCallback.putExtra(ImportReceiver.EXTRA_URI,
                        "${cacheFile}");
            if (request._importMimeAndContent.first != null)
                importCallback.putExtra(ImportReceiver.EXTRA_MIME_TYPE,
                        request._importMimeAndContent.first);
            if (request._importMimeAndContent.second != null)
                importCallback.putExtra(ImportReceiver.EXTRA_CONTENT,
                        request._importMimeAndContent.second);
            i.putExtra(DownloadAndCacheService.ON_COMPLETE_CALLBACK,
                    importCallback);
        }
        DownloadAndCacheService.getInstance().startService(i);
        return true;
    }

    /**
     * Stop any active downloads
     */
    public void stopDownload() {
        Intent i = new Intent(_context, DownloadAndCacheService.class);
        i.putExtra(DownloadAndCacheService.CANCEL_DOWNLOAD, "");
        DownloadAndCacheService.getInstance().startService(i);
    }

    /**
     * Reset the download shape
     */
    public void reset() {
        _defaultRequest._shape = null;
        _defaultRequest._geometry = null;
        _defaultRequest._points = null;
        _defaultRequest._bounds = null;
        _defaultRequest._ulPoint = _defaultRequest._lrPoint = null;
    }


    /* Callback event handling */

    public interface Callback {

        /**
         * Download progress update
         * @param status Download status and progress stats
         */
        void onDownloadStatus(DownloadStatus status);

        /**
         * Job status update
         * @param status Job status
         */
        void onJobStatus(JobStatus status);

        /**
         * The max overall progress value has been changed
         * @param title Layer title
         * @param progress Progress
         */
        void onMaxProgressUpdate(String title, int progress);

        /**
         * The max progress up to the current level has been changed
         * Usually this means the next level has begun downloading
         * @param title Layer title
         * @param progress Progress
         */
        void onLevelProgressUpdate(String title, int progress);

        void onRegionSelectFinished(Intent intent);
    }

    public static class DownloadStatus {

        // The title of the downloaded layer
        public String title;

        // The number of queued downloads
        public int queuedDownloads;

        // Number of tiles downloaded for the current level / total tiles for current level
        public String tileStatus;

        // Number of levels / total levels
        public String layerStatus;

        // The total number of tiles downloaded so far
        public int tilesDownloaded;

        // The total number of tiles to download
        public int totalTiles;

        // The total number of tiles to download up to the current level
        public int levelTotalTiles;

        // The estimated time left for the download to complete
        public long timeLeft;
    }

    public static class JobStatus {

        // Layer title
        public String title;

        // Job status code (see DownloadJob)
        // Possible values:
        // DownloadJob.CONNECTING - Attempting to initiate connection
        // DownloadJob.DOWNLOADING - Tiles are being downloaded
        // DownloadJob.COMPLETE - Tile download is complete
        // DownloadJob.CANCELLED - Download has been canceled
        // DownloadJob.ERROR - Download error occurred
        public int code;

        // The number of queued layer downloads
        public int queuedDownloads;
    }

    public final static class RequestBuilder {
        private String _title;
        private double _expandDist;
        private double _minRes, _maxRes;
        private Shape _shape;
        private String _cacheURI, _sourceURI;
        private GeoPoint[] _points;
        private GeoBounds _bounds;
        private GeoPoint _ulPoint, _lrPoint;
        private Geometry _geometry;
        private String _callbackAlias;

        private Pair<String, String> _importMimeAndContent;
        private File _defaultDownloadDirectory;

        /**
         * Set the title of the layer download
         * @param title Title string
         */
        public RequestBuilder setTitle(String title) {
            _title = title;
            return this;
        }

        /**
         * Set the download shape
         * @param shape Shape
         */
        public RequestBuilder setShape(Shape shape) {
            _shape = shape;
            return this;
        }

        /**
         * Set the distance to expand the download area (for open polylines)
         * @param dist Distance in meters
         */
        public RequestBuilder setExpandDistance(double dist) {
            _expandDist = dist;
            return this;
        }

        /**
         * Set the resolution range to download
         * @param minRes Minimum map resolution
         * @param maxRes Maximum map resolution
         */
        public RequestBuilder setResolution(double minRes, double maxRes) {
            _minRes = minRes;
            _maxRes = maxRes;
            return this;
        }

        /**
         * Set the URI of the tile cache
         * @param uri Cache URI
         */
        public RequestBuilder setCacheURI(String uri) {
            _cacheURI = uri;
            return this;
        }

        /**
         * Set the map layer source URI
         * @param uri Source URI
         */
        public RequestBuilder setSourceURI(String uri) {
            _sourceURI = uri;
            return this;
        }

        public RequestBuilder setCallback(String alias) {
            _callbackAlias = alias;
            return this;
        }

        public RequestBuilder setImportParams(String mime, String content) {
            if (mime == null && content == null)
                _importMimeAndContent = null;
            else
                _importMimeAndContent = Pair.create(mime, content);
            return this;
        }

        public RequestBuilder setDefaultDownloadDirectory(File dir) {
            _defaultDownloadDirectory = dir;
            return this;
        }

        /**
         * Calculate the number of tiles that will be downloaded
         * @return Tile count
         */
        int calculateTileCount() {
            // validate levels and rect bounds
            if (_shape == null || _minRes < _maxRes || _sourceURI == null)
                return 0;

            CacheRequest request = new CacheRequest();
            request.maxResolution = _maxRes;
            request.minResolution = _minRes;
            if (_geometry == null) {
                LineString ls = new LineString(2);
                GeoPoint[] geometry = getPoints();
                if (FileSystemUtils.isEmpty(geometry))
                    return 0;
                for (GeoPoint gp : geometry)
                    ls.addPoint(gp.getLongitude(), gp.getLatitude());
                _geometry = new Polygon(ls);
            }
            request.region = _geometry;
            request.countOnly = true;

            int numTiles = 0;
            TileClient client = null;
            try {
                client = TileClientFactory.create(_sourceURI, null, null);
                if (client != null)
                    numTiles = client.estimateTileCount(request);
            } finally {
                if (client != null)
                    client.dispose();
            }

            // if all of the layers were too zoomed out, then it can be covered in one tile
            return numTiles == 0 ? 1 : numTiles;
        }

        /**
         * Get the download bounds
         * @return Bounds
         */
        GeoBounds getBounds() {
            if (_bounds == null)
                _bounds = GeoBounds.createFromPoints(getPoints(),
                        true);
            return _bounds;
        }

        /**
         * Get the upper-left bounds point
         * @return Upper-left point
         */
        GeoPoint getUpperLeft() {
            if (_ulPoint != null)
                return _ulPoint;
            GeoBounds b = getBounds();
            if (b.crossesIDL())
                _ulPoint = new GeoPoint(b.getNorth(), b.getEast());
            else
                _ulPoint = new GeoPoint(b.getNorth(), b.getWest());
            return _ulPoint;
        }

        /**
         * Get the lower-right bounds point
         * @return Lower-right point
         */
        GeoPoint getLowerRight() {
            if (_lrPoint != null)
                return _lrPoint;
            GeoBounds b = getBounds();
            if (b.crossesIDL())
                _lrPoint = new GeoPoint(_bounds.getSouth(),
                        _bounds.getWest() + 360);
            else
                _lrPoint = new GeoPoint(_bounds.getSouth(), _bounds.getEast());
            return _lrPoint;
        }

        /**
         * Get the list of points that determine which tiles to download
         * @return Points array
         */
        GeoPoint[] getPoints() {
            if (_points != null)
                return _points;

            if (_shape == null)
                return new GeoPoint[0];

            GeoPoint[] points = _shape.getPoints();
            if (_shape instanceof Rectangle)
                points = Arrays.copyOf(points, 4);

            List<GeoPoint> pointList = new ArrayList<>(points.length);
            if (GeoCalculations.crossesIDL(points, 0, points.length)) {
                for (GeoPoint p : points) {
                    if (p.getLongitude() < 0)
                        pointList.add(new GeoPoint(p.getLatitude(),
                                p.getLongitude() + 360));
                    else
                        pointList.add(p);
                }
            } else
                pointList.addAll(Arrays.asList(points));

            if (pointList.isEmpty())
                return new GeoPoint[0];

            boolean closed = _shape instanceof DrawingRectangle
                    || MathUtils.hasBits(_shape.getStyle(),
                            Polyline.STYLE_CLOSED_MASK)
                    || points[0].equals(points[points.length - 1]);

            // Extrude route by a certain meter distance
            if (!closed && _expandDist > 0) {
                double d = _expandDist;
                double lastDist = 0;
                double lb = 0;
                List<GeoPoint> leftLine = new ArrayList<>();
                List<GeoPoint> rightLine = new ArrayList<>();
                int size = pointList.size();
                for (int i = 0; i < size; i++) {
                    GeoPoint c = pointList.get(i);
                    GeoPoint n = pointList.get(i == size - 1 ? i - 1 : i + 1);
                    double r = GeoCalculations.distanceTo(c, n);
                    double a = GeoCalculations.bearingTo(c, n);
                    lastDist += r;
                    if (i > 1 && i <= size - 2 && lastDist < 1)
                        continue;
                    lastDist = 0;
                    double b = a;
                    if (i > 0 && i < size - 1) {
                        if (Math.abs(lb - b) > 180)
                            b = ((Math.min(b, lb) + 360) + Math.max(b, lb)) / 2;
                        else
                            b = (b + lb) / 2;
                    } else if (i == size - 1)
                        b += 180;
                    lb = a;
                    GeoPoint left = GeoCalculations.pointAtDistance(c, b - 90,
                            d);
                    GeoPoint right = GeoCalculations.pointAtDistance(c, b + 90,
                            d);
                    if (i == 0 || i == size - 1) {
                        b += i == 0 ? 180 : 0;
                        left = GeoCalculations.pointAtDistance(left, b, d);
                        right = GeoCalculations.pointAtDistance(right, b, d);
                    }
                    leftLine.add(left);
                    rightLine.add(right);
                }
                pointList.clear();
                pointList.addAll(leftLine);
                Collections.reverse(rightLine);
                pointList.addAll(rightLine);
                closed = true;
            }

            GeoPoint start = pointList.get(0);
            GeoPoint end = pointList.get(pointList.size() - 1);
            if (closed && !start.equals(end))
                pointList.add(start);

            return _points = pointList.toArray(new GeoPoint[0]);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (_callback.isEmpty()) {
            AtakBroadcast.getInstance().unregisterReceiver(this);
            return;
        }

        if (FileSystemUtils.isEquals(intent.getAction(),
                LayersManagerBroadcastReceiver.ACTION_TOOL_FINISHED)) {

            String alias = intent
                    .getStringExtra(DownloadAndCacheService.JOB_UID);
            if (alias == null)
                alias = DEFAULT_CALLBACK_ALIAS;
            final Callback cb = _callback.get(alias);
            if (cb != null)
                cb.onRegionSelectFinished(intent);

            return;
        }

        if (!FileSystemUtils.isEquals(intent.getAction(),
                DownloadAndCacheService.BROADCAST_ACTION))
            return;

        // Current download progress
        if (intent.hasExtra(DownloadAndCacheService.DOWNLOAD_STATUS)) {
            DownloadStatus status = new DownloadStatus();
            status.timeLeft = intent.getLongExtra(
                    DownloadAndCacheService.TIME_STATUS, 0L);
            status.tileStatus = intent.getStringExtra(
                    DownloadAndCacheService.TILE_STATUS);
            status.layerStatus = intent.getStringExtra(
                    DownloadAndCacheService.LAYER_STATUS);
            status.queuedDownloads = intent.getIntExtra(
                    DownloadAndCacheService.QUEUE_SIZE, 0);
            status.tilesDownloaded = intent.getIntExtra(
                    DownloadAndCacheService.PROGRESS_BAR_PROGRESS, 0);
            status.levelTotalTiles = intent.getIntExtra(
                    DownloadAndCacheService.PROGRESS_BAR_ADJUST_SECONDARY, 0);
            status.totalTiles = intent.getIntExtra(
                    DownloadAndCacheService.PROGRESS_BAR_SET_MAX, 0);
            status.title = intent.getStringExtra(DownloadAndCacheService.TITLE);
            String alias = intent
                    .getStringExtra(DownloadAndCacheService.JOB_UID);
            if (alias == null)
                alias = DEFAULT_CALLBACK_ALIAS;
            final Callback cb = _callback.get(alias);
            if (cb != null)
                cb.onDownloadStatus(status);
        }

        // Current job status
        else if (intent.hasExtra(DownloadAndCacheService.JOB_STATUS)) {
            JobStatus status = new JobStatus();
            status.code = intent.getIntExtra(
                    DownloadAndCacheService.JOB_STATUS, 0);
            status.title = intent.getStringExtra(
                    DownloadAndCacheService.TITLE);
            status.queuedDownloads = intent.getIntExtra(
                    DownloadAndCacheService.QUEUE_SIZE, 0);
            String alias = intent
                    .getStringExtra(DownloadAndCacheService.JOB_UID);
            if (alias == null)
                alias = DEFAULT_CALLBACK_ALIAS;
            final Callback cb = _callback.get(alias);
            if (cb != null)
                cb.onJobStatus(status);
        }

        // Individual progress update
        else if (intent.hasExtra(DownloadAndCacheService.PROGRESS_BAR_STATUS)) {
            String title = intent.getStringExtra(DownloadAndCacheService.TITLE);
            String alias = intent
                    .getStringExtra(DownloadAndCacheService.JOB_UID);
            if (alias == null)
                alias = DEFAULT_CALLBACK_ALIAS;
            final Callback cb = _callback.get(alias);
            if (cb == null)
                return;
            ;
            if (intent.hasExtra(DownloadAndCacheService.PROGRESS_BAR_SET_MAX)) {
                cb.onMaxProgressUpdate(title, intent.getIntExtra(
                        DownloadAndCacheService.PROGRESS_BAR_SET_MAX, 0));
            } else if (intent.hasExtra(
                    DownloadAndCacheService.PROGRESS_BAR_ADJUST_SECONDARY)) {
                cb.onLevelProgressUpdate(title, intent.getIntExtra(
                        DownloadAndCacheService.PROGRESS_BAR_ADJUST_SECONDARY,
                        0));
            }
        }
    }
}
