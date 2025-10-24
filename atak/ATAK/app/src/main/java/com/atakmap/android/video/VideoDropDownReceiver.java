
package com.atakmap.android.video;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.SystemClock;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.Surface;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.atakmap.android.dropdown.DropDown.OnStateListener;
import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.image.ExifHelper;
import com.atakmap.android.image.ImageGalleryReceiver;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.tools.ActionBarReceiver;
import com.atakmap.android.user.VolumeSwitchManager;
import com.atakmap.android.util.ATAKUtilities;
import com.atakmap.android.util.AltitudeUtilities;
import com.atakmap.android.util.NotificationUtil;
import com.atakmap.android.video.manager.VideoManager;
import com.atakmap.app.R;
import com.atakmap.coremap.conversions.CoordinateFormat;
import com.atakmap.coremap.conversions.CoordinateFormatUtilities;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.coremap.maps.time.CoordinatedTime;
import com.atakmap.coremap.maps.time.CoordinatedTime.SimpleDateFormatThread;
import com.atakmap.math.MathUtils;
import com.atakmap.util.zip.IoUtils;
import com.partech.mobilevid.GLVidRenderer;
import com.partech.mobilevid.SharedGLSurfaceView;
import com.partech.mobilevid.SurfaceVideoConsumer;
import com.partech.mobilevid.gl.GLMisc;
import com.partech.pgscmedia.AudioMediaFormat;
import com.partech.pgscmedia.MediaException;
import com.partech.pgscmedia.MediaFormat;
import com.partech.pgscmedia.MediaProcessor;
import com.partech.pgscmedia.VideoMediaFormat;
import com.partech.pgscmedia.consumers.AudioConsumer;
import com.partech.pgscmedia.consumers.KLVConsumer;
import com.partech.pgscmedia.consumers.MediaConsumer;
import com.partech.pgscmedia.consumers.StatusUpdateConsumer;
import com.partech.pgscmedia.frameaccess.AudioFrameConverter;
import com.partech.pgscmedia.frameaccess.AudioFrameData;
import com.partech.pgscmedia.frameaccess.DecodedMetadataItem;
import com.partech.pgscmedia.frameaccess.KLVData;
import com.partech.pgscmedia.frameaccess.MediaMetadataDecoder;
import com.partech.pgscmedia.frameaccess.NativeArray;

import org.apache.sanselan.formats.tiff.constants.TiffConstants;
import org.apache.sanselan.formats.tiff.write.TiffOutputSet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import gov.tak.platform.graphics.Color;

/**
 * Holder for the new Video Display code.
 */
public class VideoDropDownReceiver extends DropDownReceiver implements
        OnStateListener, MediaConsumer, StatusUpdateConsumer,
        BufferSeekBar.BufferSeekBarChangeListener, View.OnClickListener,
        KLVConsumer,
        View.OnLongClickListener, OnSharedPreferenceChangeListener {

    public static final String TAG = "VideoDropDownReceiver";

    private final AtakPreferences _prefs;

    private final Object surfaceLock = new Object();
    private final Object processorLock = new Object();

    public static final String DISPLAY = "com.atakmap.maps.video.DISPLAY";
    private static final int NOTIFICATION_ID = 13213;

    private enum Screen {
        CONNECTING,
        VIDEO,
        FAILED
    }

    public static final String VIDEO_DIRNAME = FileSystemUtils.TOOL_DATA_DIRECTORY
            + File.separatorChar + "videos";
    public static final String SNAPSHOT_DIRNAME = FileSystemUtils.TOOL_DATA_DIRECTORY
            + File.separatorChar + "videosnaps";

    private static final CoordinatedTime.SimpleDateFormatThread recdirDateFormatter = new SimpleDateFormatThread(
            "yyyyMMMdd", LocaleUtil.getCurrent());

    private static final SimpleDateFormatThread recDateFormatter = new SimpleDateFormatThread(
            "yyyyMMMdd_HHmmss", LocaleUtil.getCurrent());

    private static final SimpleDateFormatThread snapDateFormatter = new SimpleDateFormatThread(
            "yyyyMMMdd_HHmmss_SSS", LocaleUtil.getCurrent());

    private final Context context;
    private final View videoView;

    private double currWidth = HALF_WIDTH;
    private double currHeight = HALF_HEIGHT;

    private final ImageButton playPauseBtn, snapshotBtn, muteBtn, recordBtn, galleryBtn,
            centerBtn;

    private final Button autoRecordBtn;

    private final ImageButton extrudeBtn;
    private boolean extrudeButtonVis = false;

    private BufferSeekBar curVideoSeekBar;
    private final BufferSeekBar bufferSeekBar;
    private final TextView time, frameCenter, altitude;

    private final View video_player;
    private final View status_screen;
    private final View status_connecting;
    private final View status_failed;

    private final View metadataControls;

    private final ViewSwitcher video_switcher;
    private final ViewSwitcher status_switcher;

    private final SharedGLSurfaceView glView;

    private final Button cancelBtn;
    private final Button cancelDuringConnectBtn;
    private final TextView connectionText;

    // if null, do not record, otherwise record
    private OutputStream recordingStream = null;

    private final RelativeLayout overlays;

    private final static Map<String, VideoViewLayer> videoviewlayers = new HashMap<>();
    private final Set<VideoViewLayer> activeLayers = new HashSet<>();

    private final MediaMetadataDecoder metadataDecoder = new MediaMetadataDecoder();
    private final VideoMetadata vmd = new VideoMetadata();

    final VideoMapItemController vmic;

    // See processorState
    private MediaProcessor processor;

    private enum ProcessorState {
        NONE, // no processor active; this.processor == null 
        CONNECTING, // attempting a connection asynchronously; this.processor == null
        // but there is a processing being initialized in the background that may
        // become the active processor once connection completes
        ACTIVE // processor active; this.processor != null
    }

    // Current state of the processor member variable.  Can only be used whilst holding
    // processorLock.
    private ProcessorState processorState = ProcessorState.NONE;

    private final BitmapVideoConsumer videoConsumer;
    private Surface sourceSurface;
    private boolean dropDownVis = false;
    private boolean switcherOnVideo = true;

    private AudioRenderer audioConsumer;

    private boolean priorTrackingState = true;
    private long lastVideoTime;

    private final static String VIDEO_MUTE_WHEN_HIDDEN_PREF = "video_mute_when_hidden";
    private boolean currMuteState = false;

    /**
     * Temporary file directory to use. Temp directory in use - not used for file opens
     */
    private static final String TEMP_DIR = FileSystemUtils.TOOL_DATA_DIRECTORY
            + File.separatorChar + "videotmp";
    private File tmpDir;

    private final VideoOverlayLayer vo = new VideoOverlayLayer("videooverlay",
            vmd);

    private long timeSinceLastUpdate;
    private static final long METADATA_TIME_GUARD = 66;

    private static AlternativeVideoPlayer avp = new WaveRelayAlternativePlayer();

    public VideoDropDownReceiver(final MapView mapView, Context context) {
        super(mapView);
        this.context = context;

        _prefs = AtakPreferences.getInstance(mapView.getContext());
        _prefs.registerListener(this);

        LayoutInflater inflator = LayoutInflater.from(context);
        videoView = inflator.inflate(R.layout.video, null);

        centerBtn = videoView.findViewById(R.id.PanToButton);
        centerBtn.setOnClickListener(this);

        extrudeBtn = videoView.findViewById(R.id.extrude);
        extrudeBtn.setOnClickListener(this);

        playPauseBtn = videoView.findViewById(R.id.fmvPlayPause);
        playPauseBtn.setOnClickListener(this);
        playPauseBtn.setOnLongClickListener(this);

        snapshotBtn = videoView.findViewById(R.id.ssButton);
        snapshotBtn.setOnClickListener(this);

        autoRecordBtn = videoView
                .findViewById(R.id.AutoRecordButton);
        autoRecordBtn.setOnClickListener(this);

        recordBtn = videoView
                .findViewById(R.id.RecordVideoButton);
        recordBtn.setOnClickListener(this);

        muteBtn = videoView
                .findViewById(R.id.muteButton);
        muteBtn.setOnClickListener(this);

        galleryBtn = videoView.findViewById(R.id.galleryButton);
        galleryBtn.setOnClickListener(this);

        bufferSeekBar = videoView.findViewById(R.id.fmvSeekBar);
        bufferSeekBar.setSeekBarChangeListener(this);

        time = videoView.findViewById(R.id.TimeView);


        frameCenter = videoView.findViewById(R.id.framectrtext);
        altitude = videoView.findViewById(R.id.altitudetext);

        // different states of the view
        video_player = videoView.findViewById(R.id.video_player_screen);
        status_screen = videoView.findViewById(R.id.status_screen);
        status_connecting = videoView.findViewById(R.id.status_connecting);
        status_failed = videoView.findViewById(R.id.status_failed);

        scaleDetector = new ScaleGestureDetector(context,
                new ScaleGestureListener());
        gestureDetector = new GestureDetector(context, new GestureListener());

        videoConsumer = new BitmapVideoConsumer();
        videoConsumer.setCurrentTimeListener(
                new SurfaceVideoConsumer.CurrentTimeListener() {
                    private long lastTimeMs = 0;

                    @Override
                    public void videoCurrentTimeUpdate(final long curTimeMs) {
                        if (Math.abs(curTimeMs - lastTimeMs) > 1000) {
                            lastTimeMs = curTimeMs;
                            mapView.post(new Runnable() {
                                public void run() {
                                    lastVideoTime = curTimeMs;
                                    time.setText(formatTime(curTimeMs));
                                    if (curVideoSeekBar != null)
                                        curVideoSeekBar
                                                .setCurrent((int) curTimeMs);

                                }
                            });
                        }
                    }
                });
        videoConsumer.setSizeChangeListener(
                new SurfaceVideoConsumer.SizeChangeListener() {
                    @Override
                    public void videoSizeChanged(final int w, final int h) {
                        mapView.post(new Runnable() {
                            public void run() {
                                // Update video size for GL view
                                glView.sourceSizeUpdate(w, h);

                                // Notify overlays of video size too
                                for (VideoViewLayer vvl : activeLayers) {
                                    try {
                                        vvl.videoSizeChanged(w, h);
                                    } catch (Exception e) {
                                        Log.e(TAG, "error with a layer", e);
                                    }
                                }

                                // Update view matrix since it depends on the
                                // video size
                                updateViewMatrix();
                            }
                        });
                        vo.dispatchSizeChange(w, h);
                    }
                });
        glView = videoView
                .findViewById(R.id.video_glsurface);
        glView.initializeStandalone(new GLVidRenderer.RenderSurfaceListener() {

            @Override
            public void renderSurfaceInvalid() {
            }

            @Override
            public void renderSurfaceInitFailed(String arg0) {
            }

            @Override
            public void newRenderSurface(SurfaceTexture tex) {
                changeOutput(tex);
                videoConsumer.setOutputSurface(sourceSurface);
                if (processor != null && !processor.isProcessing())
                    processor.prefetch();
            }
        });
        glView.setYUV(true);
        glView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent e) {
                scaleDetector.onTouchEvent(e);
                gestureDetector.onTouchEvent(e);
                return true;
            }
        });

        metadataControls = videoView.findViewById(R.id.metadata_controls);

        video_switcher = videoView
                .findViewById(R.id.video_switcher);
        status_switcher = videoView
                .findViewById(R.id.status_switcher);

        vmic = new VideoMapItemController(mapView);

        cancelBtn = videoView.findViewById(R.id.connect_cancel);
        cancelBtn.setOnClickListener(this);

        cancelDuringConnectBtn = videoView
                .findViewById(R.id.cancel_during_connect);
        cancelDuringConnectBtn.setOnClickListener(this);

        connectionText = videoView.findViewById(R.id.progress);

        overlays = videoView.findViewById(R.id.overlays);
        overlays.setEnabled(false);

        vmd.useKlvElevation = _prefs.get("prefs_use_klv_elevation", false);

    }

    public void requestBitmapFrame(BitmapFrameRequestCallback callback) {
        videoConsumer.requestBitmapFrame(callback);
    }

    @Override
    public void onSharedPreferenceChanged(
            final SharedPreferences sp,
            final String key) {

        if (key == null)
            return;

        if (key.equals("prefs_use_klv_elevation")) {
            vmd.useKlvElevation = _prefs.get(key,  false);
        } else if (key.equals(VIDEO_MUTE_WHEN_HIDDEN_PREF)) {
            if (audioConsumer != null) {
                if (_prefs.get(key, true)) {
                    if (!currMuteState)
                        audioConsumer.toggleMuting();
                } else {
                    if (currMuteState != audioConsumer.muted)
                        audioConsumer.toggleMuting();
                }
            }
        }
    }

    private void changeOutput(SurfaceTexture tex) {
        synchronized (surfaceLock) {
            if (sourceSurface != null) {
                sourceSurface.release();
            }

            sourceSurface = null;

            if (tex != null)
                sourceSurface = new Surface(tex);
        }
    }

    /**
     * Registers an overlay view on top of the video window.    This will only be used when the 
     * VIDEO_DISPLAY intent has the "layers" list filled in with the lasters in bottom to top ordering.
     * @param v the videoviewlayer to register
     */
    public static void registerVideoViewLayer(final VideoViewLayer v) {
        videoviewlayers.put(v.id, v);

    }

    /**
     * Unregisters an overlay view previously registered.
     * @param v the videoviewlayer to unregister
     */
    public static void unregisterVideoViewLayer(final VideoViewLayer v) {
        videoviewlayers.remove(v.id);
    }

    void open() {
        if (!isVisible() && !isClosed()) {
            unhideDropDown();
        }
    }

    @Override
    public boolean onLongClick(View view) {
        Toast.makeText(MapView.getMapView().getContext(),
                "(" + glView.getSourceWidth() + "x" + glView.getSourceHeight()
                        + ")",
                Toast.LENGTH_SHORT)
                .show();
        Log.d(TAG, "information: " + glView.getSourceWidth() + " "
                + glView.getSourceHeight() + " "
                + glView.getComputedFrameRate());
        return true;
    }

    @Override
    public void onClick(View view) {
        final int id = view.getId();
        if (id == playPauseBtn.getId()) {
            if (processor != null && processor.isProcessing()) {
                videoConsumer.videoStopped();
                processor.stop();
                playPauseBtn.setImageResource(
                        R.drawable.playforeground);
                sendNotification(false);
            } else if (processor != null) {
                // if the file at the end, pressing play will restart it from the beginning.
                if (vmd.connectionEntry
                        .getProtocol() == ConnectionEntry.Protocol.FILE &&
                        processor.getDuration() - processor.getTime() < 500)
                    processor.setTime(0);

                processor.start();
                playPauseBtn.setImageResource(
                        R.drawable.pauseforeground);
                sendNotification(true);
            }

        } else if (id == snapshotBtn.getId()) {
            takeScreenshot();

        } else if (id == muteBtn.getId() && audioConsumer != null) {
            if (audioConsumer.toggleMuting())
                muteBtn.setImageResource(R.drawable.video_audio_muted);
            else
                muteBtn.setImageResource(R.drawable.video_audio);

            // if the video is muted, use the volume switcher to switch maps, otherwise
            // use the volume switcher to change the volume
            VolumeSwitchManager.getInstance(getMapView()).setEnabled(audioConsumer.muted);

            // record the current state of the user selected mute for use when muting and
            // unmuting based on visibility.
            currMuteState = audioConsumer.muted;



        } else if (id == recordBtn.getId()) {
            final boolean sel = recordBtn.isSelected();
            recordBtn.setSelected(!sel);
            record(!sel);

            if (!sel) {

                if (!_prefs.get("video.autorecordonstart." +
                        ConnectionEntry.getURL(vmd.connectionEntry, true),
                        false)) {
                    Log.d(TAG,
                            "video entry prompt for autorecord: "
                                    + ConnectionEntry
                                            .getURL(vmd.connectionEntry, true));

                    autoRecordBtn.setVisibility(View.VISIBLE);
                    getMapView().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            autoRecordBtn.setVisibility(View.GONE);
                        }
                    }, 5000);
                }
            } else {
                final VideoMetadata lvmd = vmd;
                if (lvmd != null && lvmd.connectionEntry != null) {
                    _prefs.remove("video.autorecordonstart." +
                            ConnectionEntry.getURL(vmd.connectionEntry, true));
                    Log.d(TAG,
                            "video entry unset autorecord: " + ConnectionEntry
                                    .getURL(vmd.connectionEntry, true));

                }
            }
        } else if (id == autoRecordBtn.getId()) {
            final VideoMetadata lvmd = vmd;
            if (lvmd != null && lvmd.connectionEntry != null) {
                _prefs.set("video.autorecordonstart." +
                        ConnectionEntry.getURL(vmd.connectionEntry, true),
                        true);
                Log.d(TAG, "video entry set autorecord: "
                        + ConnectionEntry.getURL(vmd.connectionEntry, true));

            }
            autoRecordBtn.setVisibility(View.GONE);
        } else if (id == extrudeBtn.getId()) {
            boolean sel = extrudeBtn.isSelected();
            extrudeBtn.setSelected(!sel);
            if (!sel) {
                hideDropDown();

                if (vmic != null)
                    vmic.zoomTo(getMapView(), vmd);
            }
        } else if (id == centerBtn.getId()) {
            if (vmic != null) {
                vmic.zoomTo(getMapView(), vmd);
            }
        } else if (id == galleryBtn.getId()) {
            Intent i = new Intent(ImageGalleryReceiver.IMAGE_GALLERY);
            i.putExtra("title", context.getString(R.string.video_snapshots));
            i.putExtra("directory",
                    FileSystemUtils.getItem(SNAPSHOT_DIRNAME).toString());
            AtakBroadcast.getInstance().sendBroadcast(i);

        } else if (id == cancelBtn.getId()) {
            if (!isClosed())
                closeDropDown();

        } else if (id == cancelDuringConnectBtn.getId()) {
            if (!isClosed())
                closeDropDown();
        }

    }

    @Override
    public void onProgressChanged(BufferSeekBar seekBar, int progress) {
        long posAct = processor.setTime(progress);
    }

    @Override
    public void onStartTrackingTouch(BufferSeekBar seekBar) {
        priorTrackingState = processor.isProcessing();
        processor.stop();
    }

    @Override
    public void onStopTrackingTouch(BufferSeekBar seekBar) {
        if (priorTrackingState) {
            processor.start();
        }
    }

    @Override
    public void mediaBytes(int i, byte[] bytes) {
        try {
            if (recordingStream != null && bytes != null)
                recordingStream.write(bytes, 0, i);
        } catch (IOException ie) {
            // on error stop
            Log.e(TAG, "Could not create new file for recording location: "
                    + ie);
            recordBtn.setSelected(false);
            recordingStream = null;
            toast("Error occurred Recording", Toast.LENGTH_SHORT);
        }

    }

    private void toast(final String message, final int length) {
        getMapView().post(new Runnable() {
            public void run() {
                Toast.makeText(context, message, length).show();
            }
        });
    }

    @Override
    public void mediaStreamExtentsUpdate(final long startMillis,
            final long endMillis) {

        bufferSeekBar.post(new Runnable() {
            public void run() {

                bufferSeekBar.setRange((int) startMillis, (int) endMillis);

                final ConnectionEntry ce = vmd.connectionEntry;

                if (ce != null && lastVideoTime < (endMillis
                        - ce.getBufferTime())
                        && processor != null && !processor.isProcessing()) {
                    // We're paused at the head of the buffer and our buffer is full
                    // Force play to keep the buffering at/near the desired amount
                    processor.start();
                    playPauseBtn.setImageResource(R.drawable.pauseforeground);
                }

            }
        });

    }

    @Override
    public void mediaEOF() {

        // make sure the connection entry is the latest
        vmd.connectionEntry = VideoManager.refreshConnectionEntry(vmd.connectionEntry);

        videoConsumer.videoStopped();

        final ConnectionEntry ce = vmd.connectionEntry;
        if (ce != null) {
            attemptReconnect(ce);
            if (ce.getProtocol() == ConnectionEntry.Protocol.FILE) {
                getMapView().post(new Runnable() {
                    @Override
                    public void run() {
                        playPauseBtn.setImageResource(
                                R.drawable.playforeground);
                    }
                });
            } else {
                for (VideoViewLayer vvl : activeLayers) {
                    try {
                        vvl.error(ce);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    @Override
    public void mediaFatalError(final String s) {

        // make sure the connection entry is the latest
        vmd.connectionEntry = VideoManager.refreshConnectionEntry(vmd.connectionEntry);

        final ConnectionEntry ce = vmd.connectionEntry;
        if (ce != null) {
            attemptReconnect(ce);

            if (ce.getProtocol() != ConnectionEntry.Protocol.FILE) {
                for (VideoViewLayer vvl : activeLayers) {
                    try {
                        vvl.error(ce);
                    } catch (Exception ignored) {
                    }
                }
            }
        }
    }

    private class ReconnectRunnable implements Runnable {
        private ConnectionEntry ce;
        // processor at the time the reconnect was initiated.
        // use in conjunction with outer class processorLock/State
        private MediaProcessor oldProcessor;

        ReconnectRunnable(MediaProcessor oldProcessor, ConnectionEntry ce) {
            this.ce = ce;
            this.oldProcessor = oldProcessor;
        }

        public void run() {
            MediaProcessor newProcessor = null;

            while ((newProcessor = startConnection()) == null) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignored) {
                }
                if (isClosed())
                    // old processor not our problem; closing will have destroyed it
                    return;

            }

            // Take processorLock; if old processor was still relevant/current, 
            // push in our new one and start destruction of the old one.  Otherwise destroy the new
            // one since it won't get used (something else is going on or was closed out) and be done.
            Log.e(TAG, "reconnected to: " + ce);
            MediaProcessor toBeCleaned = null;
            synchronized (processorLock) {
                if (processorState == ProcessorState.ACTIVE) {
                    toBeCleaned = oldProcessor;
                    VideoDropDownReceiver.this.processor = newProcessor;
                    showScreen(Screen.VIDEO);
                } else {
                    toBeCleaned = newProcessor;
                }
            }
            if (toBeCleaned != null) {
                toBeCleaned.destroy();
            }
        }
    }

    private void attemptReconnect(final ConnectionEntry ce) {
        Log.e(TAG, "attempting reconnection to: " + ce);
        if (ce.getProtocol() != ConnectionEntry.Protocol.FILE) {
            showScreen(Screen.CONNECTING);

            // Kick off runnable here
            Thread t = new Thread(new ReconnectRunnable(processor, ce),
                    "video-reconnect");
            t.setPriority(Thread.NORM_PRIORITY);
            t.start();
        }

    }

    @Override
    public void mediaKLVData(final KLVData klvData) {

        if (processor == null)
            return;

        if (klvData == null || !klvData.isValid())
            return;

        final Map<DecodedMetadataItem.MetadataItemIDs, DecodedMetadataItem> items;

        try {
            items = metadataDecoder.decode(klvData);
        } catch (Exception e) {
            Log.d(TAG, "error occurred during klv decoding", e);
            return;
        }

        if (vmd != null) {
            vmd.update(items);
            setExtrudeVisible(vmd.hasFourCorners());
        }

        long currentTime = SystemClock.elapsedRealtime();
        if (currentTime > (timeSinceLastUpdate + METADATA_TIME_GUARD)) {
            timeSinceLastUpdate = currentTime;
            // moving the markers at 1/15 a second produces fluid results unless you are 
            // for UAV's and will produce more decent results that previous versions of 
            // ATAK (1/5 a second).   This will guard against data streams that provide 
            // metadata more frequently than the video play rate. 

            if (vmd != null)
                vmic.update(vmd);

            final String frameCenterTxt;
            final String altitudeTxt;

            if (vmd != null && vmd.frameDTED != null
                    && vmd.frameDTED.get().isValid()) {
                frameCenterTxt = "TGT " +
                        CoordinateFormatUtilities.formatToString(
                                vmd.frameDTED.get(),
                                CoordinateFormat.MGRS);

                altitudeTxt = AltitudeUtilities.format(vmd.frameDTED.get(),
                        _prefs.getSharedPrefs(), false);
            } else {
                frameCenterTxt = getMapView().getContext()
                        .getString(R.string.video_meta_tgt);
                altitudeTxt = getMapView().getContext()
                        .getString(R.string.ft_msl4);
            }

            getMapView().post(new Runnable() {
                public void run() {
                    frameCenter.setText(frameCenterTxt);
                    altitude.setText(altitudeTxt);
                }
            });
        }

        vo.dispatch();

        try {
            for (VideoViewLayer vvl : activeLayers) {
                try {
                    vvl.metadataChanged(klvData, items);
                } catch (Exception e) {
                    Log.e(TAG, "error with a layer", e);
                }
            }
        } catch (Exception ignored) {
        }

    }

    /**
     * Responsible for setting the visibility of the extrude button.  This method depends on the
     * state and the actual visibility to match.   This is reset each time a new video is loaded.
     * @param vis the new state of the visibility.
     */
    private void setExtrudeVisible(final boolean vis) {
        if (vis != extrudeButtonVis) {
            getMapView().post(new Runnable() {
                public void run() {
                    extrudeBtn.setVisibility(vis ? View.VISIBLE : View.GONE);
                    extrudeButtonVis = vis;
                }
            });
        }
    }

    @Override
    public void disposeImpl() {
        _prefs.unregisterListener(this);
    }

    /**
     * Only returns a list of layers that do not have to be explicitly requested in order to show
     * up.   These layers will be considered always on.
     * @return the array of layers that do not have to be requested.
     */
    private String[] getGeneralLayers() {
        Set<String> set = videoviewlayers.keySet();
        List<String> list = new ArrayList<>();
        for (String s : set) {
            VideoViewLayer vvl = videoviewlayers.get(s);
            if (vvl != null && vvl.isAlwaysOn())
                list.add(s);
        }
        return list.toArray(new String[0]);
    }

    @Override
    public void onReceive(final Context c, final Intent intent) {

        final String action = intent.getAction();
        if (action == null)
            return;

        ConnectionEntry connectionEntry = (ConnectionEntry) intent
                .getSerializableExtra("CONNECTION_ENTRY");

        if (connectionEntry == null) {
            connectionEntry = VideoManager.parse(intent);
            if (connectionEntry == null) {
                // there is a video playing already if it is not closed
                if (!isClosed()) {
                    if (!isVisible())
                        unhideDropDown();
                    return;
                }
                vmd.connectionEntry = null;
                toast("invalid video information, cannot display",
                        Toast.LENGTH_SHORT);
                return;
            }
        } else {
            // caused when the user used the Video Drop Down to select a connection
            // entry that may not currently reflect the state of the marker
            connectionEntry = VideoManager.refreshConnectionEntry(connectionEntry);
        }

        connectionEntry = connectionEntry.copy();
        final AlternativeVideoPlayer a = getAlternativeVideoPlayer();
        if (a != null && a.launchOther(connectionEntry))
            return;

        Log.i(TAG, "call to show the video display for: " + connectionEntry);

        // the connection entry will be used to set up the video playback
        vmd.connectionEntry = connectionEntry;

        final String s = connectionEntry.getAddress();
        if ((connectionEntry.getProtocol() == ConnectionEntry.Protocol.RAW) &&
                s != null &&
                (s.startsWith("file://") || s.startsWith("/"))) {
            connectionEntry.setProtocol(ConnectionEntry.Protocol.FILE);
            connectionEntry.setPath(s);
        }

        setRetain(true);

        final String spi = intent.getStringExtra("spi_uid");
        final String sensor = intent.getStringExtra("sensor_uid");

        vmic.showSpiMarker(FileSystemUtils.isEmpty(spi));
        vmic.showSensorMarker(FileSystemUtils.isEmpty(sensor));

        showScreen(Screen.CONNECTING);

        if (!isVisible()) {
            showDropDown(videoView, HALF_WIDTH, FULL_HEIGHT, FULL_WIDTH,
                    HALF_HEIGHT, true, this);
        }

        final Thread t = new Thread("video-start-thread") {
            public void run() {

                // only enable structured decoding if a plugin wants structured decoding.

                // if the user has passed in an intent with specific layers specified use that
                final String[] intentLayers = intent
                        .getStringArrayExtra("layers");

                // otherwise go ahead and just display all of the layers registered
                final String[] layers = (intentLayers != null) ? intentLayers
                        : getGeneralLayers();

                startProcessor(layers);

            }
        };
        t.start();

    }

    /**
     * Start the processing of the video.   This will always ensure that all of the components are
     * properly initialized if the processor is created successfully.
     * This runs on a background thread
     * @param layers the list of layers to put over the processor
     */
    private void startProcessor(String[] layers) {

        stopProcessor();

        synchronized (processorLock) {

            switch (processorState) {
                case CONNECTING:
                case ACTIVE:
                    // A processor is already starting or active
                    return;
                case NONE:
                    break;
            }

            activeLayers.clear();
            if (layers != null) {
                for (String layer : layers) {
                    VideoViewLayer vvl = videoviewlayers.get(layer);
                    if (vvl != null) {
                        try {
                            activeLayers.add(vvl);
                        } catch (Exception e) {
                            Log.e(TAG, "error adding: " + layer, e);
                        }
                    }
                }
            }

            getMapView().post(new Runnable() {
                public void run() {
                    overlays.removeAllViews();
                    boolean enableStockMetadata = true;
                    for (VideoViewLayer vvl : activeLayers) {
                        overlays.addView(vvl.v, vvl.rlp);
                        enableStockMetadata &= vvl.enableStockMetadata;
                    }
                    frameCenter.setVisibility(
                            enableStockMetadata ? View.VISIBLE : View.GONE);
                    altitude.setVisibility(
                            enableStockMetadata ? View.VISIBLE : View.GONE);
                }
            });

            processorState = ProcessorState.CONNECTING;
        }

        final MediaProcessor processor = startConnection();

        synchronized (processorLock) {
            // Check if we got cancelled
            final boolean success = processor != null;
            if (processorState != ProcessorState.CONNECTING) {
                if (success)
                    stopProcessorImpl(processor);
                return;
            }

            if (success) {
                processorState = ProcessorState.ACTIVE;
                this.processor = processor;
                getMapView().addLayer(MapView.RenderStack.MAP_SURFACE_OVERLAYS,
                        vo);
                showScreen(Screen.VIDEO);

                getMapView().post(new Runnable() {
                    @Override
                    public void run() {

                        final ConnectionEntry entry = vmd.connectionEntry;

                        if (entry == null)
                            return;

                        final String url = ConnectionEntry.getURL(entry, true);

                        if (_prefs.get("video.autorecordonstart." + url,
                                false)) {
                            if (!recordBtn.isSelected()) {
                                Log.d(TAG, "video entry autorecord: " + url);
                                recordBtn.performClick();
                            }
                        }
                    }
                });
            } else {
                processorState = ProcessorState.NONE;
                final ConnectionEntry ce = vmd.connectionEntry;
                if (ce != null
                        && ce.getProtocol() == ConnectionEntry.Protocol.FILE)
                    closeDropDown();
                else {
                    showScreen(Screen.FAILED);

                }
            }
        }
    }

    private boolean shouldUseProgressBar() {
        final ConnectionEntry ce = vmd.connectionEntry;
        return ce != null &&
                (ce.getProtocol() == ConnectionEntry.Protocol.FILE ||
                        ((ce.getProtocol() == ConnectionEntry.Protocol.UDP
                                || ce.getProtocol() == ConnectionEntry.Protocol.SRT
                                ||
                                ce.getProtocol() == ConnectionEntry.Protocol.RTSP)
                                && ce.getBufferTime() > 10000));
    }

    private MediaProcessor startConnection() {
        MediaProcessor processor = null;
        try {
            vmd.connectionEntry = VideoManager.refreshConnectionEntry(vmd.connectionEntry);
            final ConnectionEntry ce = vmd.connectionEntry;
            // Create MediaProcessor using the various available
            // constructors, depending on the type of source media.

            getMapView().post(new Runnable() {
                public void run() {

                    // the extrude button will now only show if there is 4 corner metadata
                    // set to gone until the video is loaded.
                    extrudeButtonVis = false;
                    extrudeBtn.setVisibility(View.GONE);
                    extrudeBtn.setSelected(false);

                    if (shouldUseProgressBar()) {
                        playPauseBtn.setVisibility(View.VISIBLE);
                        bufferSeekBar.setVisibility(View.VISIBLE);
                        time.setTextColor(Color.BLACK);
                        time.setShadowLayer(5, 2,2, 0xAAFFFFFF);
                    } else {
                        bufferSeekBar.setVisibility(View.INVISIBLE);
                        playPauseBtn.setVisibility(View.INVISIBLE);
                        time.setTextColor(Color.WHITE);
                        time.setShadowLayer(5, 2,2, 0xAA000000);
                    }
                    recordBtn.setEnabled(false);
                }
            });

            switch (ce.getProtocol()) {
                case FILE:
                    try {
                        File f = new File(
                                FileSystemUtils.validityScan(ce.getPath()));
                        if (IOProviderFactory.exists(f)) {
                            processor = new MediaProcessor(f);
                        } else {
                            return null;
                        }
                    } catch (IOException ioe) {
                        Log.d(TAG, "invalid file", ioe);
                        return null;
                    }
                    break;

                case UDP: {
                    String host = ce.getAddress();
                    if (host != null) {
                        if (host.isEmpty() || host.equals("0.0.0.0")
                                || host.equals("127.0.0.1"))
                            host = null;
                    }
                    int port = ce.getPort();
                    setupTmpDir();

                    int buffer = ce.getBufferTime();
                    int timeout = ce.getNetworkTimeout();

                    String localAddr = null;
                    if (ce.getPreferredInterfaceAddress() != null) {
                        localAddr = ce.getPreferredInterfaceAddress();
                        Log.d(TAG,
                                "use local address for network traffic: "
                                        + localAddr);

                    }

                    if (buffer == 0)
                        buffer = -1;

                    Log.d(TAG,
                            "Create udp processor " + host + ":" + port
                                    + " timeout=" + timeout + " buffer="
                                    + buffer + " local=" + localAddr);

                    processor = new MediaProcessor(host, port, timeout, buffer,
                            0,
                            tmpDir, localAddr);
                    Log.d(TAG,
                            "Create udp processor DONE " + host + ":" + port
                                    + " timeout=" + timeout + " buffer="
                                    + buffer + " local=" + localAddr);

                }
                    break;
                case RAW:
                case RTP:
                case TCP:
                case RTMP:
                case RTMPS:
                case HTTPS:
                case HTTP:
                    setupTmpDir();
                    String addr = ConnectionEntry.getURL(ce, false);
                    Log.d(TAG, "connect to " + addr);
                    processor = new MediaProcessor(addr);
                    break;
                case SRT: {

                    setupTmpDir();
                    final String srtAddr = ce.getAddress();
                    int srtPort = ce.getPort();
                    String srtPass = ce.getPassphrase();
                    if (srtPass != null && srtPass.isEmpty())
                        srtPass = null;
                    Log.d(TAG,
                            "SRT connect to " + srtAddr + ":" + srtPort
                                    + " pass is { ********** } "
                                    + ce.getNetworkTimeout() + " "
                                    + ce.getBufferTime());
                    processor = new MediaProcessor(srtAddr, srtPort, srtPass,
                            ce.getNetworkTimeout(),
                            ce.getBufferTime(), 0, tmpDir);
                    break;
                }

                case RTSP: {

                    setupTmpDir();
                    final String rtspaddr = ConnectionEntry.getURL(ce, false);

                    int buffer = ce.getBufferTime();
                    if (buffer == 0)
                        buffer = -1;

                    Log.d(TAG,
                            "Create rtsp processor " + rtspaddr
                                    + " timeout=" + ce.getNetworkTimeout()
                                    + " buffer="
                                    + buffer);

                    processor = new MediaProcessor(rtspaddr,
                            ce.getNetworkTimeout(),
                            buffer, 0, tmpDir);
                    break;
                }
            }

            // could get really messy here because the procesor could be cancelled
            try {
                boolean supported = processor.setMediaConsumer(
                        MediaProcessor.MediaFileType.MEDIATYPE_MPEGTS,
                        this);
                if (supported)
                    getMapView().post(new Runnable() {
                        public void run() {
                            recordBtn.setEnabled(true);
                        }
                    });
            } catch (Exception e) {
                Log.d(TAG, "could not record from this stream", e);
            }

            processor.setStatusUpdateConsumer(this);

            // Look at the format of all tracks and grab the ones we are
            // interested in. Here, for sake of simplicity,
            // we take just the first video track
            // and first klv metadata track if it exists.
            final MediaFormat[] fmts = processor.getTrackInfo();
            boolean haveVid = false;
            // Skip looking for/setting up audio if disabled
            boolean haveAud = _prefs.get("video_disable_audio",false);
            vmd.hasMetadata = false;
            audioConsumer = null;
            for (MediaFormat fmt : fmts) {
                Log.d(TAG, "discovered track: " + fmt.type);
                if (!haveVid && fmt.type == MediaFormat.Type.FORMAT_VIDEO) {
                    setupVideo(processor, (VideoMediaFormat) fmt);
                    haveVid = true;
                }

                if (!haveAud && fmt.type == MediaFormat.Type.FORMAT_AUDIO) {
                    setupAudio(processor, (AudioMediaFormat)fmt);
                    haveAud = true;
                }

                if (!ce.getIgnoreEmbeddedKLV()) {
                    if ((!vmd.hasMetadata
                            && fmt.type == MediaFormat.Type.FORMAT_KLV)) {
                        setupKLV(processor, fmt);
                        vmd.hasMetadata = true;
                    }
                }
                if (vmd.hasMetadata && haveVid && haveAud)
                    break;
            }

            if (!haveVid) {
                // Not supporting videos without video
                processor.destroy();
                throw new Exception("No video track");
            }

            final int max = (int) (processor.getDuration());

            getMapView().post(new Runnable() {
                @Override
                public void run() {
                    int vis = View.GONE;
                    if (vmd.hasMetadata)
                        vis = View.VISIBLE;
                    altitude.setText(R.string.ft_msl4);
                    frameCenter.setText(R.string.video_meta_tgt);

                    metadataControls.setVisibility(vis);

                    if (shouldUseProgressBar()) {
                        bufferSeekBar.resetBufTime(0, max,
                                vmd.connectionEntry.getBufferTime());
                    }
                    playPauseBtn.setImageResource(
                            R.drawable.pauseforeground);

                    muteBtn.setVisibility(audioConsumer != null ? View.VISIBLE : View.GONE);
                    muteBtn.setImageResource(R.drawable.video_audio);

                    // if there is an audioConsumer, if muted use the standard map selector
                    // for the volume keys otherwise allow for volume changing
                    // Additionally record the state of the audio consumer
                    if (audioConsumer != null) {
                        VolumeSwitchManager.getInstance(getMapView()).setEnabled(audioConsumer.muted);
                        currMuteState = audioConsumer.muted;

                    }
                }
            });

            for (VideoViewLayer vvl : activeLayers) {
                if (vvl != null) {
                    try {
                        activeLayers.add(vvl);
                        vvl.init(processor, metadataDecoder,
                                vmd.connectionEntry,
                                VideoDropDownReceiver.this, vmd.hasMetadata);
                    } catch (Exception e) {
                        Log.e(TAG, "error starting: " + vvl.id, e);
                    }
                }
            }

            sendNotification(true);
            resetPanAndScale();
            processor.start();

        } catch (Exception me) {
            Log.e(TAG, "Error occurred loading video", me);
            for (VideoViewLayer vvl : activeLayers) {
                try {
                    vvl.error(vmd.connectionEntry);
                    vvl.dispose();
                } catch (Exception ignored) {
                }
            }
            return null;
        }
        return processor;
    }

    private void stopProcessorImpl(final MediaProcessor oldprocessor) {
        // processor stopping and destruction might take some time.
        Thread reaper = new Thread(new Runnable() {
            @Override
            public void run() {
                oldprocessor.stop();
                oldprocessor.destroy();
            }
        }, "video-resource-reaper");
        reaper.start();
    }

    /**
     * Clean up all resources associated with a current connection. Must be run prior opening a new
     * connection.
     * Called on random threads
     */
    private void stopProcessor() {

        synchronized (processorLock) {
            if (processorState == ProcessorState.NONE) {
                return;
            }
            processorState = ProcessorState.NONE;

            // stop recording
            record(false);

            videoConsumer.videoStopped();

            //remove all of the views
            getMapView().post(new Runnable() {
                public void run() {
                    overlays.removeAllViews();
                }
            });
            for (VideoViewLayer vvl : activeLayers) {
                try {
                    vvl.stop(vmd.connectionEntry);
                    vvl.dispose();
                } catch (Exception ignored) {
                }
            }
            activeLayers.clear();

            final MediaProcessor oldprocessor = processor;
            processor = null;
            if (oldprocessor != null) {
                stopProcessorImpl(oldprocessor);
            }

            getMapView().removeLayer(MapView.RenderStack.MAP_SURFACE_OVERLAYS,
                    vo);

            removeNotifcation();

            try {
                metadataDecoder.clear();
            } catch (Exception e) {
                Log.d(TAG, "clearing the decoder during exit", e);
            }

            vmic.dispose();
            vmd.dispose();

            // restore the state of the volume button to be used to switch the maps
            VolumeSwitchManager.getInstance(getMapView()).setEnabled(true);

        }

    }

    @Override
    protected void onStateRequested(int state) {
        if (state == DROPDOWN_STATE_FULLSCREEN) {
            Log.d(TAG, "onStateRequested: full screen close action bar");
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent(ActionBarReceiver.TOGGLE_ACTIONBAR)
                            .putExtra("show", false));

            if (!isPortrait()) {
                if (Double.compare(currWidth, HALF_WIDTH) == 0) {
                    resize(FULL_WIDTH, FULL_HEIGHT);
                }
            } else {
                if (Double.compare(currHeight, HALF_HEIGHT) == 0) {
                    resize(FULL_WIDTH, FULL_HEIGHT);
                }
            }
        } else if (state == DROPDOWN_STATE_NORMAL) {
            Log.d(TAG, "onStateRequested: normal screen open action bar");
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent(ActionBarReceiver.TOGGLE_ACTIONBAR)
                            .putExtra("show", true));

            if (!isPortrait()) {
                resize(HALF_WIDTH, FULL_HEIGHT);
            } else {
                resize(FULL_WIDTH, HALF_HEIGHT);
            }
        }
    }

    @Override
    protected boolean onBackButtonPressed() {
        // Need to override back button behavior so the contacts drop-down
        // passes the check in DropDownManager.isSpecialCase - See ATAK-10656

        if (!isVisible() && !isClosed()) {
            unhideDropDown();
            return true;
        }

        //Log.d(TAG, "onBackButtonPressed: isPortrait: " + isPortrait() + ", w=" + currWidth + ", h=" + currHeight);
        if (!isPortrait()
                && currWidth >= FULL_WIDTH - HANDLE_THICKNESS_LANDSCAPE) {
            Log.d(TAG,
                    "onBackButtonPressed: landscape full width, back to half width");
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent(ActionBarReceiver.TOGGLE_ACTIONBAR)
                            .putExtra("show", true));
            resize(HALF_WIDTH, FULL_HEIGHT);
            return true;
        } else if (isPortrait()
                && currHeight > FULL_HEIGHT - HANDLE_THICKNESS_PORTRAIT) {
            Log.d(TAG,
                    "onBackButtonPressed: portrait full height, back to half height");
            AtakBroadcast.getInstance().sendBroadcast(
                    new Intent(ActionBarReceiver.TOGGLE_ACTIONBAR)
                            .putExtra("show", true));
            resize(FULL_WIDTH, HALF_HEIGHT);
            return true;

        }

        closeDropDown();
        return true;
    }

    @Override
    public void onDropDownSelectionRemoved() {
    }

    private void resetGLViewVis() {
        if (dropDownVis && switcherOnVideo) {
            glView.onResume();
            glView.setVisibility(View.VISIBLE);
        } else {
            glView.onPause();
            // Need to set visible gone because otherwise the surface still 
            // "punches through" the map on some newer devices
            // and/or android versions
            glView.setVisibility(View.GONE);
        }

    }

    @Override
    public void onDropDownVisible(final boolean v) {
        dropDownVis = v;
        resetGLViewVis();

        boolean voEn = extrudeBtn.isSelected() && !v;
        vo.setEnabled(voEn);
        if (voEn) {
            changeOutput(vo.getDestinationTexture());
            videoConsumer.setOutputSurface(sourceSurface);
            if (processor != null && !processor.isProcessing())
                processor.prefetch();
        }

        setVisible(v);
    }

    void setVisible(boolean visible) {
        if (audioConsumer != null) { 
            if (_prefs.get(VIDEO_MUTE_WHEN_HIDDEN_PREF, true) && audioConsumer != null) {
                if (!visible && !audioConsumer.muted) {
                    // if either the application or the dropDown is not visible, make sure to mute the video
                    audioConsumer.toggleMuting();
                } else if (isVisible() && audioConsumer.muted != currMuteState) {
                    // if the drop down is visible restore the audio to the previous setting
                    audioConsumer.toggleMuting();
                }
                VolumeSwitchManager.getInstance(getMapView()).setEnabled(audioConsumer.muted);
            }
        }
    }

    @Override
    public void onDropDownSizeChanged(double width, double height) {
        //Log.d(TAG, "onDropDownSizeChanged: " + width + ", " + height);
        currWidth = width;
        currHeight = height;
        getMapView().post(new Runnable() {
            @Override
            public void run() {
                // View matrix depends on drop-down size
                updateViewMatrix();
            }
        });
    }

    @Override
    public void onDropDownClose() {
        Log.d(TAG, "starting video drop down pane closure");
        stopProcessor();
        vmd.connectionEntry = null;
        removeNotifcation();
        cleanTmpDirs();
        Log.d(TAG, "video drop down pane closed");
        glView.clearPreviousFrame();
        resetPanAndScale();
    }

    /**
     * Clean up any remaining contents in tool-specific temporary directory
     * @param context application context against which to find file locations
     */
    static void cleanupTmpDir(Context context) {
        File base = context.getFilesDir();
        File tmpDir = new File(base, TEMP_DIR);
        FileSystemUtils.deleteDirectory(tmpDir, false);
    }

    /**
     * Creates a temp directory that is unique
     */
    private void setupTmpDir() throws IOException {
        File base = context.getFilesDir();
        base = new File(base, TEMP_DIR);
        if (!IOProviderFactory.exists(base)) {
            if (!IOProviderFactory.mkdirs(base)) {
                Log.d(TAG, "could not wrap: " + base);
            }
        }
        tmpDir = IOProviderFactory.createTempFile("stream", null, base);
        FileSystemUtils.delete(tmpDir);
        if (!IOProviderFactory.mkdirs(tmpDir)) {
            Log.d(TAG, "could not wrap: " + tmpDir);
        }
    }

    /**
     * Recursively delete the temp directory if it exists.
     */
    private void cleanTmpDirs() {
        File base = context.getFilesDir();
        base = new File(base, TEMP_DIR);
        if (!IOProviderFactory.exists(base))
            return;
        FileSystemUtils.deleteDirectory(base, true);
    }

    /**
     * Sets up the track described by the argument.
     */
    private void setupVideo(MediaProcessor processor,
            final VideoMediaFormat fmt) {
        // Connect processor to output consumer
        if (shouldUseProgressBar())
            curVideoSeekBar = bufferSeekBar;
        else
            curVideoSeekBar = null;

        videoConsumer.setFormat(fmt);

        // Set our renderer as consumer for the video track.
        processor.setVideoConsumer(fmt.trackNum, videoConsumer);

    }

    /**
     * Sets up the track described by the argument.
     */
    private void setupAudio(MediaProcessor processor,
                            final AudioMediaFormat fmt) {
        try {
            audioConsumer = new AudioRenderer(fmt);

            // Set our renderer as consumer for the video track.
            processor.setAudioConsumer(fmt.trackNum, audioConsumer);
        } catch (MediaException ex) {
            Log.e(TAG, "Setting up audio track for format " + fmt + " failed", ex);
        }

    }

    /**
     * Sets up the metadata track decribed by the argument. Raw KLV is delivered to the callback.
     * The metadata decoder processes both 0104/0601 metadata. see:
     * MapVideoLibrary/libs/gv2fapidoc.zip For metadata elements parsed by the decoder and the
     * definitions normalized across the dictionaries see:
     * com.partech.pgscmedia.frameaccess.DecodedMetadataItem.MetadataItemIDs
     */
    private void setupKLV(MediaProcessor processor, MediaFormat fmt) {
        processor.setKLVConsumer(fmt.trackNum, this);
    }

    private void sendNotification(boolean isPlaying) {
        if (vmd.connectionEntry == null)
            return;

        Intent videoPlayer = new Intent();
        videoPlayer.setAction("com.atakmap.maps.video.DISPLAY");

        if (isPlaying) {
            NotificationUtil.getInstance().postNotification(NOTIFICATION_ID,
                    R.drawable.green_full,
                    NotificationUtil.GREEN,
                    context.getString(R.string.video_text56),
                    context.getString(R.string.playing), videoPlayer, false);
        } else {
            NotificationUtil.getInstance().postNotification(NOTIFICATION_ID,
                    R.drawable.red_full,
                    NotificationUtil.RED,
                    context.getString(R.string.video_text56),
                    context.getString(R.string.stopped), videoPlayer, false);
        }

    }

    private void removeNotifcation() {
        NotificationUtil.getInstance().clearNotification(NOTIFICATION_ID);
    }

    private void takeScreenshot() {

        final Bitmap bitmap = glView.getBitmap();

        // First attempt to read from KLV metadata
        final GeoPoint point = new GeoPoint(vmd.sensorLatitude,
                vmd.sensorLongitude, vmd.sensorAltitude);

        final GeoPoint framePoint = new GeoPoint(vmd.frameLatitude,
                vmd.frameLongitude, GeoPoint.UNKNOWN);

        double heading = Double.NaN, hFOV = Double.NaN;
        long time = new CoordinatedTime().getMilliseconds();
        if (point.isValid()) {
            heading = 0.0;
            hFOV = vmd.sensorHFOV;
            // XXX - KLV timestamp?
        }

        if (point.isValid() && !Double.isNaN(heading)) {
            heading = ATAKUtilities.convertFromTrueToMagnetic(point, heading);
            if (hFOV <= 0)
                hFOV = 30;
        }

        final double fHeading = heading;
        final double fFOV = hFOV;
        final String fTime = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss",
                LocaleUtil.getCurrent()).format(new Date(time));
        Log.i(TAG, "Taking Snapshot");
        File snapDir = FileSystemUtils.getItem(SNAPSHOT_DIRNAME);
        if (!IOProviderFactory.exists(snapDir)
                && !IOProviderFactory.mkdirs(snapDir))
            Log.e(TAG, "Failed to make dir at " + snapDir);
        final String pathToRecordLocation = SNAPSHOT_DIRNAME
                + File.separator
                + vmd.connectionEntry.getAlias()
                + "-"
                + snapDateFormatter.format(
                        new Date(new CoordinatedTime().getMilliseconds()))
                + ".jpg";

        final File file = FileSystemUtils.getItem(FileSystemUtils
                .sanitizeWithSpacesAndSlashes(pathToRecordLocation));
        try {
            //If file already exists, don't try to make a new snapshot
            if (!IOProviderFactory.createNewFile(file)) {
                toast("Snapshot already exists", Toast.LENGTH_SHORT);
            } else {
                try (OutputStream ostream = IOProviderFactory
                        .getOutputStream(file)) {
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, ostream);
                }
            }

        } catch (Exception e) {
            Log.e(TAG, "error: ", e);
        }

        // Add metadata if we have any
        if (point.isValid()) {
            TiffOutputSet tos = ExifHelper.getExifOutput(ExifHelper
                    .getExifMetadata(file));
            ExifHelper.updateField(tos,
                    TiffConstants.TIFF_TAG_DATE_TIME, fTime);
            ExifHelper.setPoint(tos, point);

            if (framePoint.isValid())
                ExifHelper.setFramePoint(tos, framePoint);

            if (!Double.isNaN(fHeading)) {
                ExifHelper.updateField(tos,
                        ExifHelper.GPS_IMG_DIRECTION, fHeading);
                ExifHelper.updateField(tos,
                        ExifHelper.GPS_IMG_DIRECTION_REF, "M");
            }
            if (!Double.isNaN(fFOV)) {
                Map<String, Object> extras = new HashMap<>();
                extras.put("HorizontalFOV", fFOV);
                ExifHelper.putExtras(extras, tos);
            }
            ExifHelper.saveExifOutput(tos, file);
        }

        for (VideoViewLayer vvl : activeLayers) {
            try {
                vvl.snapshot(vmd.connectionEntry, file);
            } catch (Exception ignored) {
            }
        }

        getMapView().post(new Runnable() {
            public void run() {
                Toast.makeText(context, R.string.video_text55,
                        Toast.LENGTH_SHORT)
                        .show();
            }
        });
    }

    private void record(final boolean state) {
        recordingStream = null;
        recordBtn.setSelected(state);

        if (state) {
            final long time = new CoordinatedTime().getMilliseconds();
            final File vidDir = FileSystemUtils
                    .getItem(VIDEO_DIRNAME + File.separator
                            + recdirDateFormatter.get().format(time));
            if (!IOProviderFactory.exists(vidDir)) {
                if (!IOProviderFactory.mkdirs(vidDir)) {
                    Log.e(TAG,
                            "Failed to make directory at "
                                    + vidDir.getAbsolutePath());
                }
            }

            String pathToRecordLocation = vidDir
                    + File.separator
                    + vmd.connectionEntry.getAlias()
                    + "_"
                    + recDateFormatter.get().format(time)
                    + ".mpg";
            final File recordLocation = new File(pathToRecordLocation);

            try {
                if (!IOProviderFactory.createNewFile(recordLocation)) {
                    Log.e(TAG, "Recording file could not be created");
                    return;
                } else {
                    Log.i(TAG, "Recording file created: "
                            + recordLocation.getAbsolutePath());

                }
                recordingStream = new BufferedOutputStream(
                        IOProviderFactory.getOutputStream(
                                recordLocation));
            } catch (Exception e) {
                Log.e(TAG, "error starting recording stream", e);
                recordingStream = null;
                recordBtn.setSelected(false);
                toast("Error Starting the Recording", Toast.LENGTH_SHORT);
            }

        } else {
            IoUtils.close(recordingStream, TAG,
                    "error closing out the recording stream");
            recordingStream = null;
        }

    }

    private void showScreen(final Screen s) {
        getMapView().post(new Runnable() {
            public void run() {
                final ConnectionEntry ce = vmd.connectionEntry;
                if (ce == null)
                    return;

                if (s == Screen.CONNECTING || s == Screen.FAILED) {
                    if (video_switcher.getNextView() == status_screen) {
                        video_switcher.showNext();
                        switcherOnVideo = false;
                    }

                } else {
                    if (video_switcher.getNextView() == video_player) {
                        video_switcher.showNext();
                        switcherOnVideo = true;
                    }

                }

                if (s == Screen.CONNECTING) {
                    connectionText.setText(context
                            .getString(R.string.connecting_to)
                            + ce.getAlias() + " at "
                            + ConnectionEntry.getURL(ce,
                                    true));

                    if (status_switcher.getNextView() == status_connecting) {
                        status_switcher.showNext();
                    }
                } else {
                    if (status_switcher.getNextView() == status_failed) {
                        status_switcher.showNext();
                    }
                }
                resetGLViewVis();
            }
        });

    }

    private static String formatTime(long currentTime) {
        int seconds = (int) (currentTime * div_1000d) % 60;
        int minutes = (int) ((currentTime * div_60000d) % 60); // 1000 * 60 =
        // 60000
        int hours = (int) ((currentTime * div_3600000d) % 24); // 1000 * 60 * 60
        // = 3600000
        return (hours < 10 ? "0" + hours : hours) + ":"
                + (minutes < 10 ? "0" + minutes : minutes) + ":"
                + (seconds < 10 ? "0" + seconds : seconds);
    }

    private static final double div_1000d = 1d / 1000d;
    private static final double div_60000d = 1d / 60000d;
    private static final double div_3600000d = 1d / 3600000d;

    private float startSpan;
    private float currentScale;
    private float currentPanX;
    private float currentPanY;
    private final GestureDetector gestureDetector;
    private final ScaleGestureDetector scaleDetector;

    private void resetPanAndScale() {
        currentScale = 1.0f;
        currentPanX = currentPanY = 0;
        updateViewMatrix();
    }

    void updateViewMatrix() {

        // Pass raw values to GL
        glView.setScale(currentScale);
        glView.setPanOffset((int) currentPanX, (int) currentPanY);

        // Need to transform the raw pan X/Y to a value that works for the overlays
        // Reference: GLVidRenderer.resizeVideo
        float surfaceW = glView.getWidth();
        float surfaceH = glView.getHeight();
        int sourceHeight = glView.getSourceHeight();
        int sourceWidth = glView.getSourceWidth();

        int clampSH = sourceHeight == 0 ? 1 : sourceHeight;
        int clampSW = sourceWidth == 0 ? 1 : sourceWidth;
        float ar = (float) sourceWidth / (float) clampSH;
        float destAR = surfaceW / surfaceH;
        float w;
        float h;
        if (destAR > ar) {
            h = surfaceH;
            w = h * ar;
        } else {
            w = surfaceW;
            h = w / ar;
        }

        int scaleW = (int) Math
                .floor((float) sourceWidth * currentScale);
        int scaleH = (int) Math
                .floor((float) sourceHeight * currentScale);
        int scaleDw = scaleW - sourceWidth;
        int scaleDh = scaleH - sourceHeight;
        float clampedPanX = MathUtils.clamp(currentPanX,
                -scaleDw / 2f, scaleDw / 2f);
        float clampedPanY = MathUtils.clamp(currentPanY,
                -scaleDh / 2f, scaleDh / 2f);

        // Perform the same matrix calculations that are in GLVidRenderer
        float[] fm = new float[16];
        System.arraycopy(GLMisc.IDENTITY, 0, fm, 0, 16);
        android.opengl.Matrix.scaleM(fm, 0, w / surfaceW, h / surfaceH, 1.0f);
        android.opengl.Matrix.translateM(fm, 0, -2 * clampedPanX / clampSW,
                -clampedPanY / clampSH, 0);
        android.opengl.Matrix.scaleM(fm, 0, currentScale, currentScale, 1.0f);
        android.opengl.Matrix.translateM(fm, 0, 2 * clampedPanX / clampSW,
                clampedPanY / clampSH, 0);

        // Convert the above 4x4 matrix to 3x3 and flip columns/rows
        // Also need to negate the y-translate since it's flipped in Android
        // view code
        Matrix m1 = new Matrix();
        m1.setValues(new float[] {
                fm[0], 0f, fm[12],
                0f, fm[5], -fm[13],
                0f, 0f, 1f
        });

        // Need half surface width and height for below matrix
        float halfW = surfaceW / 2f;
        float halfH = surfaceH / 2f;

        // Need to perform a translate and scale before applying the GL matrix above
        Matrix m2 = new Matrix();

        // Move surface so its bounds are [-w/2, -h/2] to [w/2, h/2]
        // This will make scale operations center-oriented instead of
        // top-left oriented
        m2.postTranslate(-halfW, -halfH);

        // Scale down dimensions so they're normalized [-1, -1] to [1, 1]
        m2.postScale(1f / halfW, 1f / halfH);

        // Now that the input dimensions are the same as what GL expects,
        // concat our converted 3x3 GL matrix
        m2.postConcat(m1);

        // Scale and translate back to original position
        m2.postScale(halfW, halfH);
        m2.postTranslate(halfW, halfH);

        // Set view matrix on layers
        try {
            for (VideoViewLayer vvl : activeLayers) {
                try {
                    vvl.setPan((int) currentPanX, (int) currentPanY);
                    vvl.setScale(currentScale);
                    vvl.setViewMatrix(m2);
                } catch (Exception e) {
                    Log.e(TAG, "error with a layer", e);
                }
            }
        } catch (Exception ignored) {
        }
    }

    private void setPan(float x, float y) {
        currentPanX -= x;
        currentPanY += y;

        currentPanY = Math.max(Math.min(currentPanY, glView.getWidth()),
                -glView.getWidth());
        currentPanX = Math.max(Math.min(currentPanX, glView.getHeight()),
                -glView.getHeight());

        updateViewMatrix();
    }

    private void setScale(float scale) {
        currentScale += scale;
        if (currentScale < 1f)
            currentScale = 1f;

        updateViewMatrix();
    }

    class ScaleGestureListener
            extends ScaleGestureDetector.SimpleOnScaleGestureListener {

        @Override
        public boolean onScaleBegin(ScaleGestureDetector detector) {
            startSpan = detector.getCurrentSpan();
            return true;
        }

        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float currentSpan = detector.getCurrentSpan();
            setScale((currentSpan - startSpan) / startSpan);
            startSpan = currentSpan;
            return true;
        }

        @Override
        public void onScaleEnd(ScaleGestureDetector detector) {
            setScale((detector.getCurrentSpan() - startSpan) / startSpan);
        }
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {

        @Override
        public boolean onDown(@NonNull MotionEvent e) {
            return true;
        }

        @Override
        public boolean onScroll(@Nullable MotionEvent e1,
                @NonNull MotionEvent e2, float distanceX,
                float distanceY) {
            setPan(distanceX, distanceY);
            return true;
        }

        @Override
        public boolean onDoubleTap(@NonNull MotionEvent e) {
            resetPanAndScale();
            return true;
        }

        @Override
        public void onLongPress(@NonNull MotionEvent e) {
            for (VideoViewLayer vvl : activeLayers) {
                try {
                    vvl.onLongPress(e);
                } catch (Exception ignored) {
                }
            }
        }

    }

    /**
     * Method for setting the alternative video player for TAK.
     * @param a the implementation of the alternative video player or null if no alternative should
     *          be used.   Make sure that during plugin unload the alternative video player is
     *          set to null.
     */
    public static void setAlternativeVideoPlayer(
            final AlternativeVideoPlayer a) {
        avp = a;
    }

    /**
     * Obtain the currently set Alternative video player.
     * @return the alternative video player.
     */
    public static AlternativeVideoPlayer getAlternativeVideoPlayer() {
        return avp;
    }

    public interface AlternativeVideoPlayer {
        /**
         * The action to take when the single click is performed.
         * @param ce the Connection Entry, can be null
         * @return true if the alternative player was successful.   If false, the default behavior is
         * performed.
         */
        boolean launchSingleClick(final ConnectionEntry ce);

        /**
         * The action to take when the long press is performed.
         * @param ce the Connection Entry, can be null
         * @return true if the alternative player was successful.   If false, the default behavior is
         * performed.
         */
        boolean launchLongPress(final ConnectionEntry ce);

        /**
         * The action to take when the video connection is requested from a
         * marker on the map.
         * @param ce the Connection Entry, can be null
         * @return true if the alternative player was successful.   If false, the default behavior is
         * performed.
         */
        boolean launchOther(final ConnectionEntry ce);
    }

    /**
     * Consolidates the original third party logic for Wave Relay devices.    This behavior
     * is completely plugable via setAlternativeVideoPlayer
     */
    public static class WaveRelayAlternativePlayer
            implements AlternativeVideoPlayer {
        @Override
        public boolean launchSingleClick(ConnectionEntry ce) {
            return false;
        }

        @Override
        public boolean launchLongPress(ConnectionEntry ce) {
            return launchOther(ce);
        }

        @Override
        public boolean launchOther(ConnectionEntry ce) {
            final Context context = MapView.getMapView().getContext();

            if (ce == null)
                return false;

            String uri = ConnectionEntry.getURL(ce, false);
            final ConnectionEntry.Protocol p = ce.getProtocol();
            if (p == ConnectionEntry.Protocol.UDP
                    || p == ConnectionEntry.Protocol.RTSP
                    || p == ConnectionEntry.Protocol.FILE)
                uri = p.toURL() + uri;
            else
                return false;

            final String pkg = "com.persistentsystems.waverelayplayer";
            final String act = pkg + ".activities.FullScreenVideoActivity";
            try {
                PackageManager pm = context.getPackageManager();
                pm.getPackageInfo(pkg, PackageManager.GET_META_DATA);
                Log.d(TAG, "found " + pkg + " on the device");
            } catch (PackageManager.NameNotFoundException e) {
                Log.d(TAG, "could not find " + pkg + " on the device");
                return false;
            }

            final Intent intent = new Intent(
                    "com.persistentsystems.waverelayplayer.VIEW_VIDEO");
            intent.setComponent(new ComponentName(pkg, act));
            intent.putExtra("uri", uri);
            context.startActivity(intent);
            return true;

        }
    }

    private static class AudioRenderer implements AudioConsumer {
        private static final AudioFormat trackFormat;
        private static final AudioAttributes trackAttribs;
        // Arbitrary, but common, choices: 44.1 khz, stereo, 16bit pcm
        private static final int SAMPLE_RATE = 44100;
        static {
            trackAttribs = new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build();
            trackFormat = new AudioFormat.Builder().setChannelMask(AudioFormat.CHANNEL_OUT_STEREO)
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .build();
        }

        private AudioTrack track;
        private ByteBuffer buffer;
        private int bufferOffset;
        private AudioFrameConverter converter;

        private boolean muted;

        AudioRenderer(AudioMediaFormat fmt) throws MediaException {
            converter = new AudioFrameConverter(fmt,
                    AudioMediaFormat.SampleType.A_TYPE_SHORT,
                    AudioMediaFormat.Layout.A_LAYOUT_STEREO,
                    SAMPLE_RATE);
            track = new AudioTrack(trackAttribs, trackFormat,
                    AudioTrack.getMinBufferSize(SAMPLE_RATE,
                            trackFormat.getChannelMask(),
                            trackFormat.getEncoding()),
                    AudioTrack.MODE_STREAM,
                    AudioManager.AUDIO_SESSION_ID_GENERATE);
            track.play();
            wrapConverterOutput();
        }

        synchronized boolean toggleMuting() {
            muted = !muted;
            if (muted) {
                track.pause();
                track.flush();
            } else {
                track.play();
            }
            return muted;
        }

        private void wrapConverterOutput() {
            NativeArray[] arrays = converter.getOutputArrays();
            // Only a single array, of byte type, due to our sample format
            byte[] bytes = (byte[])arrays[0].array;
            bufferOffset = arrays[0].offset;
            buffer = ByteBuffer.wrap(bytes, arrays[0].offset, arrays[0].length);
        }

        @Override
        public void mediaAudioFrame(AudioFrameData audioFrameData) {
            try {
                synchronized (this) {
                    if (muted)
                        return;

                    if (converter.convert(audioFrameData)) {
                        // change of output buffers, rewrap
                        wrapConverterOutput();
                    }
                    buffer.position(bufferOffset);
                    // *4 to convert num samples to bytes
                    buffer.limit(bufferOffset + converter.getOutputNumSamples() * 4);
                    track.write(buffer, buffer.remaining(), AudioTrack.WRITE_BLOCKING);
                }
            } catch (MediaException e) {
                Log.e(TAG, "Exception during audio conversion", e);
            }
        }
    }
}
