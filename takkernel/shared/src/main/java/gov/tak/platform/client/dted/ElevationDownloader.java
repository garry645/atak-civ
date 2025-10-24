
package gov.tak.platform.client.dted;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoBounds;
import com.atakmap.math.MathUtils;

import gov.tak.api.commons.resources.IResourceManager;
import gov.tak.api.commons.resources.ResourceType;
import gov.tak.platform.system.SystemUtils;
import gov.tak.platform.util.LimitingThread;
import gov.tak.platform.util.SuppressibleErrorTracker;
import gov.tak.platform.util.SuppressibleErrorTracker.ErrorType;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.BitSet;
import java.util.Collections;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;



/**
 * Downloads DTED elevation data in the background for areas of interest. Downloads are triggered
 * by calling {@link #scanCurrentAreas} and happen in a background thread.
 */
public class ElevationDownloader
{
    // Default server to download tiles from
    private static final String DEFAULT_DOWNLOAD_SERVER = "tak.gov";

    private static final String TAG = "ElevationDownloader";

    // Number of milliseconds between bulk download attempts
    private static final long DOWNLOAD_INTERVAL = 1000; // 1 second

    // Number of milliseconds to wait before retrying download for failed tiles
    private static final long RETRY_TIMEOUT = 5 * 60 * 1000; // 5 minutes

    private static final int COVERAGE_SIZE = 360 * 180;
    private static final int COVERAGE_SIZE_BYTES = 8098;

    public interface OnContentChangedListener
    {
        void onContentChanged();
    }

    private final ConcurrentLinkedQueue<OnContentChangedListener> listeners = new ConcurrentLinkedQueue<>();

    /**
     * Code coverage file generator is as follows against the unzipped dtedlevel0.zip file
     * <pre>{@code
     *          BitSet bs = new BitSet();
     *          FileOutputStream fos = new FileOutputStream("remote_elevation.cov");
     *
     *          int idx = 0;
     *          for (int lng = -180; lng < 180; ++lng) {
     *              for (int lat = -90; lat < 90; ++lat) {
     *                 File f = new File(getRelativePath(0, lat, lng));
     *                 bs.set(idx, f.exists());
     *                 idx++;
     *              }
     *          }
     *          fos.write(bs.toByteArray());
     *          fos.close();
     *}</pre>
     */
    private final BitSet remote_coverage;

    private final Dt2FileWatcher dt2FileWatcher;
    private final BitSet downloadQueued, downloadFailed;
    private final ExecutorService downloadPool = Executors.newFixedThreadPool(5);

    private boolean running;
    private long retryTime = -1;
    private final File baseDir;
    private volatile String downloadServer = DEFAULT_DOWNLOAD_SERVER;
    private volatile boolean enabled = true;
    private final AtomicReference<GeoBounds[]> currentAreasOfInterest = new AtomicReference<>(new GeoBounds[0]);
    private final SuppressibleErrorTracker suppressibleErrorTracker = new SuppressibleErrorTracker();

    public ElevationDownloader(IResourceManager resourceManager, File baseDir)
    {
        this(resourceManager, new Dt2FileWatcher(Collections.singletonList(baseDir)));
    }

    protected ElevationDownloader(IResourceManager resourceManager, Dt2FileWatcher dt2FileWatcher)
    {
        baseDir = dt2FileWatcher.getRootDirs().isEmpty() ? getTempDir() : dt2FileWatcher.getRootDirs().get(0);
        this.dt2FileWatcher = dt2FileWatcher;
        downloadQueued = new BitSet(COVERAGE_SIZE);
        downloadFailed = new BitSet(COVERAGE_SIZE);
        running = true;

        // Read remote coverage bitset file
        BitSet remoteCoverage;
        try (InputStream is = resourceManager.openRawResource("remote_elevation_cov", ResourceType.Raw))
        {
            byte[] cov = new byte[COVERAGE_SIZE_BYTES];
            int read = is.read(cov);

            // Ensure correct file size
            if (read < COVERAGE_SIZE_BYTES)
                throw new Exception("Unexpected file size (expected "
                        + COVERAGE_SIZE_BYTES + ", got " + read + ")");

            remoteCoverage = BitSet.valueOf(cov);

            // Ensure correct bit set size
            if (remoteCoverage.size() != downloadQueued.size())
                throw new Exception("Unexpected coverage size (expected "
                        + downloadQueued.size() + ", got "
                        + remoteCoverage.size() + ")");
        } catch (Exception e)
        {
            Log.e(TAG, "error loading code coverage", e);
            remoteCoverage = null;
        }
        remote_coverage = remoteCoverage;
        this.dt2FileWatcher.addListener(this::onDtedFilesUpdated);
    }

    public void dispose()
    {
        dt2FileWatcher.removeListener(this::onDtedFilesUpdated);
        running = false;
        downloadPool.shutdown();
        downloadScanner.dispose(false);
    }

    /**
     * Gets the download server for DTED data.
     * @return the download server
     */
    public String getDownloadServer()
    {
        return downloadServer;
    }

    public void setDownloadServer(String downloadServer)
    {
        this.downloadServer = downloadServer;
    }

    /**
     * Determines if tile downloading is enabled.
     * @return true if enabled, otherwise false
     */
    public boolean isEnabled()
    {
        return enabled;
    }

    public void setEnabled(boolean enabled)
    {
        this.enabled = enabled;
    }

    /**
     * Get the current areas of interest for DTED data. Note that this call may come from a background thread.
     * @return an array of the current areas of interest
     */
    protected GeoBounds[] getCurrentAreasOfInterest()
    {
        return currentAreasOfInterest.get().clone();
    }

    public void setCurrentAreasOfInterest(GeoBounds[] currentAreasOfInterest)
    {
        this.currentAreasOfInterest.set(currentAreasOfInterest.clone());
    }

    /**
     * Determines if the tile downloader is running.
     * @return true if running, otherwise false
     */
    protected boolean isRunning()
    {
        return running;
    }

    /**
     * Scan the current areas returned by {@link #getCurrentAreasOfInterest()} and download any
     * missing DTED data for the areas.
     */
    public void scanCurrentAreas()
    {
        downloadScanner.exec();
    }

    /**
     * Add a {@link OnContentChangedListener} for events.
     * @param listener the listener
     */
    public void addListener(OnContentChangedListener listener)
    {
        listeners.add(listener);
    }

    /**
     * Remove a {@link OnContentChangedListener} from events.
     * @param listener the listener
     */
    public void removeListener(OnContentChangedListener listener)
    {
        listeners.remove(listener);
    }

    private void onDtedFilesUpdated()
    {
        for (OnContentChangedListener listener : listeners) {
            listener.onContentChanged();
        }
    }

    /**
     * Check if we need to download DTED 0 tiles over a given region
     * @param bounds Geo bounds to check
     */
    private void checkDownloadTiles(GeoBounds bounds)
    {
        // Get server host and whether this feature is enabled
        final String server = getDownloadServer();
        final boolean isEnabled = isEnabled();

        // Feature is disabled
        if (!isEnabled || remote_coverage == null)
            return;

        // Get current DTED map bounds
        int[] b = getDownloadBounds(bounds);
        if (b == null)
            return;
        int minLng = b[0], minLat = b[1], maxLng = b[2], maxLat = b[3];

        // Skip request if the user is zoomed out and attempting to download
        // way too many tiles at once
        int latSpan = (maxLat - minLat) + 1;
        int lngSpan = (maxLng - minLng) + 1;
        int totalTiles = latSpan * lngSpan;
        if (totalTiles <= 0 || totalTiles > 100)
            return;

        // Clear download failure cache if enough time has passed
        synchronized (this)
        {
            if (retryTime >= 0 && System.currentTimeMillis() >= retryTime)
            {
                retryTime = -1;
                downloadFailed.clear();
            }
        }

        BitSet coverage = dt2FileWatcher.getCoverage(0);

        for (int lat = minLat; lat <= maxLat; lat++)
        {
            for (int lng = minLng; lng <= maxLng; lng++)
            {
                if (!running)
                    return;

                // IDL wrap
                int lngWrapped = lng;
                if (lngWrapped >= 180)
                    lngWrapped -= 360;
                else if (lngWrapped < -180)
                    lngWrapped += 360;

                int idx = Dt2FileWatcher.getCoverageIndex(lat, lngWrapped);

                // Make sure the server has coverage for this tile
                if (!remote_coverage.get(idx))
                    continue;

                // Check if the current tile is already downloaded,
                // being downloaded, or failed to download
                // Reads are intentionally un-synchronized for best performance
                if (coverage.get(idx) || downloadQueued.get(idx)
                        || downloadFailed.get(idx))
                    continue;

                // Kick off download
                downloadTile(server, lat, lngWrapped, lng);
            }
        }
    }

    /**
     * Downloads a specific tile from the defined server for the first cache
     * miss. Try again after 5 minutes if failed.
     * @param server Stream server hostname
     * @param lat the latitude
     * @param lng the longitude
     * @param lngUnwrapped Unwrapped longitude
     */
    private void downloadTile(String server, final int lat, final int lng,
                              final int lngUnwrapped)
    {
        final String file = Dt2FileWatcher.getRelativePath(0, lat, lng);
        final File tileFile = new File(baseDir, file);
        final File zipFile = new File(tileFile.getAbsolutePath() + ".zip");
        final String urlPath = SystemUtils.isOsWindows() ? file.replace('\\', '/') : file;
        final String url = "https://" + server + "/elevation/DTED/" + urlPath + ".zip";
        final int tileIdx = Dt2FileWatcher.getCoverageIndex(lat, lng);

        // Flag tile as being downloaded so it isn't requested again
        // until finished or failed
        synchronized (this)
        {
            downloadQueued.set(tileIdx, true);
        }

        // Begin download
        downloadPool.execute(new Runnable()
        {
            @Override
            public void run()
            {
                if (!running)
                    return;

                // Check if the user is still focused over the AOI
                // and the downloader is still active
                boolean inAOI = false;
                for (GeoBounds bounds : getCurrentAreasOfInterest())
                {
                    int[] b = getDownloadBounds(bounds);
                    inAOI |= b != null && lngUnwrapped >= b[0]
                            && lngUnwrapped <= b[2]
                            && lat >= b[1] && lat <= b[3];
                }
                if (!running || !inAOI)
                {
                    // Cancel download request
                    synchronized (ElevationDownloader.this)
                    {
                        downloadQueued.set(tileIdx, false);
                    }
                    return;
                }

                try
                {
                    File parent = zipFile.getParentFile();
                    synchronized (ElevationDownloader.this)
                    {
                        // This is sync'd so we don't attempt (and fail) to mkdirs
                        // when the directory was just created by another thread
                        if (!IOProviderFactory.exists(parent)
                                && !IOProviderFactory.mkdirs(parent))
                            throw new FileNotFoundException(
                                    "Failed to make directory: " + parent);
                    }
                    //Log.d(TAG, "Downloading " + url);
                    final URL u = new URL(url);
                    URLConnection conn = u.openConnection();
                    try (InputStream is = new BufferedInputStream(conn.getInputStream());
                         OutputStream os = new FileOutputStream(zipFile))
                    {
                        FileSystemUtils.copy(is, os);
                        FileSystemUtils.unzip(zipFile, parent, true);
                    }
                } catch (Exception e)
                {
                    final ErrorType errorType = suppressibleErrorTracker.getTypeForException(e);
                    final boolean suppressible = errorType != SuppressibleErrorTracker.UnsuppressibleError;
                    final boolean suppressLog = suppressibleErrorTracker.record(errorType);

                    if (!suppressLog)
                    {
                        Log.w(TAG, "Failed to download tile " + url, suppressible ? null : e);
                        if (SuppressibleErrorTracker.suppressDuplicateLogs() && suppressible)
                            Log.e(TAG, "Suppressing additional " + errorType.name() + " error logs for " + url);
                    }
                } finally
                {
                    IOProviderFactory.delete(zipFile);
                }

                // Update state
                boolean success = FileSystemUtils.isFile(tileFile);
                if (success)
                {
                    dt2FileWatcher.refreshCache(tileFile);
                    //Log.d(TAG, "Download finished for " + url);
                }
                synchronized (ElevationDownloader.this)
                {
                    if (!success)
                    {
                        downloadFailed.set(tileIdx, true);
                        if (retryTime < 0)
                            retryTime = System.currentTimeMillis()
                                    + RETRY_TIMEOUT;
                    }
                    downloadQueued.set(tileIdx, false);
                }
            }
        });
    }

    // Checks for tiles to download once per second
    private final LimitingThread downloadScanner = new LimitingThread(
            TAG + "-Scanner", new Runnable()
            {
                @Override
                public void run()
                {
                    if (running)
                    {
                        for (GeoBounds geoBounds : getCurrentAreasOfInterest()) {
                            ElevationDownloader.this.checkDownloadTiles(geoBounds);
                        }
                        try
                        {
                            Thread.sleep(DOWNLOAD_INTERVAL);
                        } catch (InterruptedException ignored)
                        {
                        }
                    }
                }
            });

    /**
     * Get the tile boundaries for download
     * @param bounds Geo bounds
     * @return [min lng, min lat, max lng, max lat]
     */
    private static int[] getDownloadBounds(GeoBounds bounds)
    {
        double w = bounds.getWest();
        double e = bounds.getEast();
        double n = bounds.getNorth();
        double s = bounds.getSouth();

        // Invalid bounds
        if (Double.isNaN(w) || Double.isNaN(e) || Double.isNaN(n)
                || Double.isNaN(s))
            return null;

        // Unwrap longitude west of IDL
        if (bounds.crossesIDL())
        {
            e = w + 360;
            w = bounds.getEast();
        }

        int minLng = (int) Math.floor(w);
        int minLat = (int) Math.floor(s);
        int maxLng = (int) Math.floor(e);
        int maxLat = (int) Math.floor(n);

        // Clamp latitude
        minLat = MathUtils.clamp(minLat, -90, 90);
        maxLat = MathUtils.clamp(maxLat, -90, 90);

        return new int[]{
                minLng, minLat, maxLng, maxLat
        };
    }

    private static File getTempDir()
    {
        try
        {
            return FileSystemUtils.createTempDir("eldownloader", null, null);
        } catch (IOException e)
        {
            Log.e(TAG, "Could not get temporary directory for elevation data.", e);
            return null;
        }
    }
}
