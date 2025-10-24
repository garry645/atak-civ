package com.atakmap.map.layer.raster.nativeimagery;

import android.util.Pair;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapControl;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.layer.Layer;
import com.atakmap.map.layer.opengl.GLAbstractLayer2;
import com.atakmap.map.layer.opengl.GLLayer2;
import com.atakmap.map.layer.opengl.GLLayerSpi2;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.MosaicDatasetDescriptor;
import com.atakmap.map.layer.raster.RasterDataAccess2;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.RasterLayer2;
import com.atakmap.map.layer.raster.controls.ImagerySelectionControl;
import com.atakmap.map.layer.raster.mosaic.MosaicDatabase2.Frame;
import com.atakmap.map.layer.raster.mosaic.MosaicFrameColorControl;
import com.atakmap.map.layer.raster.mosaic.opengl.GLMosaicMapLayer2;
import com.atakmap.map.layer.raster.service.RasterDataAccessControl;
import com.atakmap.map.layer.raster.service.SelectionOptionsCallbackExtension;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.map.projection.EquirectangularMapProjection;
import com.atakmap.opengl.GLResolvable;
import com.atakmap.util.Filter;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

final class GLNativeImageryRasterLayer3 extends GLAbstractLayer2
        implements GLResolvable,
        RasterLayer2.OnSelectionChangedListener,
        RasterDataStore.OnDataStoreContentChangedListener,
        RasterLayer2.OnSelectionVisibleChangedListener,
        RasterLayer2.OnSelectionTransparencyChangedListener,
        SelectionOptionsCallbackExtension.OnSelectionOptionsChangedListener
{
    private GLMosaic mosaic;
    private String layerKey;
    private MosaicDatasetDescriptor layerDesc;

    private Map<String, TransparencySetting> selectionTransparency;
    private Set<TransparencySetting> addFilters;
    private Set<Filter<Frame>> removeFilters;

    protected final RasterDataAccessControl rasterAccessControl;

    private final NativeImageryRasterLayer2 subject;

    GLNativeImageryRasterLayer3(MapRenderer surface, NativeImageryRasterLayer2 subject)
    {
        super(surface, subject, GLMapView.RENDER_PASS_SURFACE|GLMapView.RENDER_PASS_SURFACE_PREFETCH);

        this.subject = subject;

        this.mosaic = null;
        this.layerKey = null;
        this.layerDesc = null;

        this.selectionTransparency = new HashMap<String, TransparencySetting>();
        this.addFilters = Collections.newSetFromMap(new IdentityHashMap<TransparencySetting, Boolean>());
        this.removeFilters = Collections.newSetFromMap(new IdentityHashMap<Filter<Frame>, Boolean>());

        this.rasterAccessControl = new RasterDataAccessControlImpl();
    }

    /**************************************************************************/
    // GL Abstract Layer
    @Override
    public void start()
    {
        super.start();

        final NativeImageryRasterLayer2 nativeImageryLayer = this.subject;
        final RasterDataStore dataStore = nativeImageryLayer.getDataStore();

        synchronized (this)
        {
            this.layerKey = NativeImageryMosaicDatabase2.registerLayer(nativeImageryLayer);
            this.layerDesc = new MosaicDatasetDescriptor(this.subject.getName(),
                    "/dev/null",
                    null,
                    null,
                    new File(this.layerKey),
                    NativeImageryMosaicDatabase2.TYPE,
                    Collections.singleton("native"),
                    Collections.singletonMap("native", Pair.create(Double.valueOf(Double.MAX_VALUE), Double.valueOf(0.0d))),
                    Collections.singletonMap("native", DatasetDescriptor.createSimpleCoverage(new GeoPoint(90, -180), new GeoPoint(90, 180), new GeoPoint(-90, 180), new GeoPoint(-90, -180))),
                    4326,
                    false,
                    null,
                    Collections.<String, String>singletonMap("relativePaths", "false"));
        }

        dataStore.addOnDataStoreContentChangedListener(this);
        nativeImageryLayer.addOnSelectionChangedListener(this);
        nativeImageryLayer.addOnSelectionVisibleChangedListener(this);

        SelectionOptionsCallbackExtension optionsChangedEx = nativeImageryLayer.getExtension(SelectionOptionsCallbackExtension.class);
        if (optionsChangedEx != null)
            optionsChangedEx.addOnSelectionOptionsChangedListener(this);

        nativeImageryLayer.setPreferredProjection(EquirectangularMapProjection.INSTANCE);

        // obtain current transparency values
        this.onTransparencyChanged(this.subject);

        this.subject.addOnSelectionTransparencyChangedListener(this);

        // raster data access
        this.renderContext.registerControl(this.subject, this.rasterAccessControl);
    }

    @Override
    public void stop()
    {
        final NativeImageryRasterLayer2 nativeImageryLayer = this.subject;
        final RasterDataStore dataStore = nativeImageryLayer.getDataStore();

        SelectionOptionsCallbackExtension optionsChangedEx = nativeImageryLayer.getExtension(SelectionOptionsCallbackExtension.class);
        if (optionsChangedEx != null)
            optionsChangedEx.removeOnSelectionOptionsChangedListener(this);

        dataStore.removeOnDataStoreContentChangedListener(this);
        nativeImageryLayer.removeOnSelectionChangedListener(this);
        nativeImageryLayer.removeOnSelectionVisibleChangedListener(this);
        nativeImageryLayer.removeOnSelectionTransparencyChangedListener(this);

        // raster data access
        this.renderContext.unregisterControl(this.subject, this.rasterAccessControl);

        synchronized (this)
        {
            NativeImageryMosaicDatabase2.unregisterLayer(this.subject);
            this.layerKey = null;
            this.layerDesc = null;
        }

        super.stop();
    }

    @Override
    protected void init()
    {
        super.init();

        synchronized (this)
        {
            if (this.layerDesc != null)
                this.mosaic = new GLMosaic(this.renderContext, this.layerDesc);
        }
    }

    @Override
    protected void drawImpl(GLMapView view, int renderPass)
    {
        if((getRenderPass()&renderPass) == 0)
            return;
        if (this.mosaic != null)
        {
            final boolean transFilterAdd = !this.addFilters.isEmpty();
            final boolean transFilterRemove = !this.removeFilters.isEmpty();
            if (transFilterAdd || transFilterRemove)
            {
                MosaicFrameColorControl ctrl = this.mosaic.getControl(MosaicFrameColorControl.class);
                if (ctrl != null)
                {
                    if (transFilterRemove)
                    {
                        for (Filter<Frame> filter : this.removeFilters)
                            ctrl.removeFilter(filter);
                        this.removeFilters.clear();
                    }
                    if (transFilterAdd)
                    {
                        for (TransparencySetting s : this.addFilters)
                            ctrl.addFilter(s.filter, ((int) (s.value * 255) << 24) | 0x00FFFFFF);
                        this.addFilters.clear();
                    }
                }
            }
            this.mosaic.draw(view);

            // XXX - breaking contract of started/stopped state
            this.subject.setAutoSelectValue(this.mosaic.getAutoSelectValue());
        }
    }

    @Override
    public void release()
    {
        if (this.mosaic != null)
        {
            this.mosaic.release();
            this.mosaic = null;
        }

        // all transparency settings will need to be reset on init
        this.addFilters.addAll(this.selectionTransparency.values());

        super.release();
    }

    private void refreshImpl(RasterLayer2 layer)
    {
        final GLMosaic mosaic = this.mosaic;
        if (mosaic == null)
            return;

        Set<String> typeFilter = new HashSet<>();
        boolean allVisible = true;

        final Collection<String> opts = layer.getSelectionOptions();
        for (String opt : opts)
        {
            if (!layer.isVisible(opt))
            {
                allVisible = false;
                continue;
            }
            typeFilter.add(opt);
        }
        if (allVisible)
            typeFilter = null;
        final ImagerySelectionControl.Mode resSelectMode = !layer.isAutoSelect() ?
            ImagerySelectionControl.Mode.MinimumResolution :
            ImagerySelectionControl.Mode.MaximumResolution;

        final ImagerySelectionControl ctrl = mosaic.getControl(ImagerySelectionControl.class);
        if (ctrl == null)
            return;

        mosaic.setLockOnSelection(subject);

        ctrl.setFilter(typeFilter);
        ctrl.setResolutionSelectMode(resSelectMode);
    }

    /**************************************************************************/
    // Raster Layer On Selection Changed Listener
    @Override
    public void onSelectionChanged(RasterLayer2 layer)
    {
        renderContext.queueEvent(new Runnable()
        {
            public void run()
            {
                refreshImpl(subject);
            }
        });
    }

    /**************************************************************************/
    // Raster Layer On Selection Visible Changed Listener
    @Override
    public void onSelectionVisibleChanged(RasterLayer2 layer)
    {
        renderContext.queueEvent(new Runnable()
        {
            public void run()
            {
                refreshImpl(subject);
            }
        });
    }

    /**************************************************************************/
    // OnSelectionOptionsChangedListener
    @Override
    public void onSelectionOptionsChanged(RasterLayer2 layer)
    {
        renderContext.queueEvent(new Runnable()
        {
            public void run()
            {
                refreshImpl(subject);
            }
        });
    }

    /**************************************************************************/
    // SelectionTransparencyControl On Selection Transparency Changed Listener
    @Override
    public void onTransparencyChanged(RasterLayer2 control)
    {
        // XXX - obtain current transparency values
        final Map<String, TransparencySetting> transparencySettings = new HashMap<String, TransparencySetting>();
        Collection<String> selections = control.getSelectionOptions();
        for (final String selection : selections)
        {
            TransparencySetting s = new TransparencySetting();
            s.value = this.subject.getTransparency(selection);
            if (s.value < 1f)
            {
                s.filter = new Filter<Frame>()
                {
                    @Override
                    public boolean accept(Frame arg)
                    {
                        return selection.equals(arg.type);
                    }
                };
                transparencySettings.put(selection, s);
            }
        }

        this.renderContext.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                for (TransparencySetting s : selectionTransparency.values())
                    removeFilters.add(s.filter);
                selectionTransparency.clear();
                selectionTransparency.putAll(transparencySettings);
                addFilters.addAll(selectionTransparency.values());
            }
        });
    }

    /**************************************************************************/
    // Raster Data Store On Data Store Content Changed Listener
    @Override
    public void onDataStoreContentChanged(RasterDataStore dataStore)
    {
        renderContext.queueEvent(new Runnable()
        {
            public void run()
            {
                refreshImpl(subject);
            }
        });
    }

    /**************************************************************************/
    // GLResolvableMapRenderable
    @Override
    public State getState()
    {
        final GLMosaic mosaic = this.mosaic;
        if (mosaic == null)
            return State.UNRESOLVED;

        return mosaic.getState();
    }

    @Override
    public void suspend()
    {
        final GLMosaic mosaic = this.mosaic;
        if (mosaic != null)
            mosaic.suspend();
    }

    @Override
    public void resume()
    {
        final GLMosaic mosaic = this.mosaic;
        if (mosaic != null)
            mosaic.resume();
    }

    /**************************************************************************/

    // extend to provide public accessible invalidation mechanism
    private final static class GLMosaic extends GLMosaicMapLayer2
    {
        public GLMosaic(MapRenderer surface, MosaicDatasetDescriptor desc)
        {
            super(surface, desc);
        }

        public synchronized String getAutoSelectValue()
        {
            if (this.visibleFrames.isEmpty())
                return null;
            else
                return this.visibleFrames.lastKey().type;
        }

        void setLockOnSelection(RasterLayer2 subject)
        {
            for(MapControl ctrl : this.controls) {
                if(ctrl instanceof RasterLayer2.OnSelectionChangedListener) {
                    final RasterLayer2.OnSelectionChangedListener sl = (RasterLayer2.OnSelectionChangedListener)ctrl;
                    sl.onSelectionChanged(subject);
                    break;
                }
            }
        }
    }

    private final static class TransparencySetting
    {
        public float value;
        public Filter<Frame> filter;
    }

    /**************************************************************************/
    // RasterDataAccess2

    private final class RasterDataAccessControlImpl implements RasterDataAccessControl
    {
        @Override
        public RasterDataAccess2 accessRasterData(GeoPoint point)
        {
            final GLMosaic mosaic = GLNativeImageryRasterLayer3.this.mosaic;
            if (mosaic == null)
                return null;

            // XXX - CLEAN UP!!!
            final RasterDataAccessControl ctrl = mosaic.getControl(RasterDataAccessControl.class);
            if (ctrl == null)
                return null;
            return ctrl.accessRasterData(point);
        }
    }

    ;

} // GLNativeImageryRasterLayer
