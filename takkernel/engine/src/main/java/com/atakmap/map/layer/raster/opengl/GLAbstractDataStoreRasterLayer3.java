package com.atakmap.map.layer.raster.opengl;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.MapRenderer;
import com.atakmap.map.MapRenderer3;
import com.atakmap.map.hittest.HitTestControl;
import com.atakmap.map.hittest.HitTestQueryParameters;
import com.atakmap.map.hittest.HitTestResult;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.control.SurfaceRendererControl;
import com.atakmap.map.layer.feature.geometry.Envelope;
import com.atakmap.map.layer.opengl.GLAsynchronousLayer2;
import com.atakmap.map.layer.raster.AbstractDataStoreRasterLayer2;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.RasterDataAccess2;
import com.atakmap.map.layer.raster.RasterDataStore;
import com.atakmap.map.layer.raster.RasterLayer2;
import com.atakmap.map.layer.raster.service.LayerAttributeExtension;
import com.atakmap.map.layer.raster.service.RasterDataAccessControl;
import com.atakmap.map.opengl.GLMapRenderable2;
import com.atakmap.map.opengl.GLMapView;
import com.atakmap.util.Collections2;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public abstract class GLAbstractDataStoreRasterLayer3  extends GLAsynchronousLayer2<LinkedList<DatasetDescriptor>> implements RasterLayer2.OnSelectionChangedListener, RasterDataStore.OnDataStoreContentChangedListener, RasterDataAccessControl, RasterLayer2.OnSelectionVisibleChangedListener, RasterLayer2.OnSelectionTransparencyChangedListener
{

    protected AbstractDataStoreRasterLayer2 subject;
    protected String selection;

    protected Map<DatasetDescriptor, GLMapRenderable2> renderable;

    protected int renderPass;

    private boolean isOverlay;

    private HitTestControlImpl hitTest;

    protected GLAbstractDataStoreRasterLayer3(MapRenderer surface, AbstractDataStoreRasterLayer2 subject)
    {
        super(surface, subject);

        this.subject = subject;

        // XXX - sort ???
        this.renderable = new LinkedHashMap<>();

        this.renderPass = GLMapView.RENDER_PASS_SURFACE|GLMapView.RENDER_PASS_SURFACE_PREFETCH;

        this.isOverlay = false;
        do {
            final LayerAttributeExtension attrs = subject.getExtension(LayerAttributeExtension.class);
            if(attrs == null)
                break;
            final Boolean overlay = attrs.getAttribute("overlay", Boolean.class);
            if(overlay == null)
                break;
            this.isOverlay = overlay.booleanValue();
        } while(false);

        this.hitTest = new HitTestControlImpl();
    }

    protected abstract void updateTransparency(GLMapRenderable2 layer);

    /**************************************************************************/
    // GL Asynchronous Map Renderable

    @Override
    public int getRenderPass()
    {
        return renderPass;
    }

    @Override
    public final void draw(GLMapView view, int renderPass)
    {
        // Check for surface bounds update when the map isn't moving
        // TODO: More efficient check for surface bounds change
        if (this.targetState != null && view.settled && !view.multiPartPass) {

            // Get the latest surface bounds
            Collection<Envelope> surfaceBounds = new ArrayList<>();
            SurfaceRendererControl ctrl = view.getControl(SurfaceRendererControl.class);
            if (ctrl != null)
                surfaceBounds = ctrl.getSurfaceBounds();

            // If the surface bounds have changed then invalidate
            if (!Collections2.equals(surfaceBounds, this.targetState.surfaceRegions))
                invalidateNoSync();
        }

        super.draw(view, renderPass);
    }

    @Override
    protected void releaseImpl()
    {
        super.releaseImpl();

        this.renderable.clear();
        this.hitTest.controls.clear();
    }

    @Override
    protected Collection<GLMapRenderable2> getRenderList()
    {
        return this.renderable.values();
    }

    @Override
    protected void resetPendingData(LinkedList<DatasetDescriptor> pendingData)
    {
        pendingData.clear();
    }

    @Override
    protected void releasePendingData(LinkedList<DatasetDescriptor> pendingData)
    {
        pendingData.clear();
    }

    @Override
    protected LinkedList<DatasetDescriptor> createPendingData()
    {
        // XXX - sorted lowest GSD to highest GSD (back to front render order)
        return new LinkedList<DatasetDescriptor>();
    }

    @Override
    protected boolean updateRenderList(ViewState state, LinkedList<DatasetDescriptor> pendingData)
    {
        Map<DatasetDescriptor, GLMapRenderable2> swap = new HashMap<>(this.renderable);
        this.renderable.clear();

        GLMapRenderable2 renderer;
        for (DatasetDescriptor info : pendingData)
        {
            renderer = swap.remove(info);
            if (renderer == null) {
                info.setLocalData("overlay", Boolean.valueOf(this.isOverlay));
                renderer = GLMapLayerFactory.create4(this.renderContext, info);
            }
            if (renderer != null)
                this.renderable.put(info, renderer);
        }

        // queue the release
        final Collection<GLMapRenderable2> released = swap.values();
        this.renderContext.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                for (GLMapRenderable2 renderable : released)
                    renderable.release();
            }
        });

        this.postProcessRenderables();

        return true;
    }

    protected void postProcessRenderables()
    {
        this.renderPass = GLMapView.RENDER_PASS_SURFACE|GLMapView.RENDER_PASS_SURFACE_PREFETCH;
        hitTest.controls.clear();
        for (GLMapRenderable2 renderer : this.renderable.values())
        {
            this.updateTransparency(renderer);
            this.renderPass |= renderer.getRenderPass();
            if(!(renderer instanceof Controls))
                continue;
            HitTestControl ctrl = ((Controls)renderer).getControl(HitTestControl.class);
            if(ctrl != null)
                hitTest.controls.add(ctrl);
        }
    }

    @Override
    protected ViewState newViewStateInstance()
    {
        return new State();
    }

    /**************************************************************************/
    // GL Layer
    @Override
    public synchronized void start()
    {
        super.start();

        this.subject.addOnSelectionChangedListener(this);
        this.selection = this.subject.getSelection();

        this.subject.getDataStore().addOnDataStoreContentChangedListener(this);
        this.subject.addOnSelectionVisibleChangedListener(this);
        this.subject.addOnSelectionTransparencyChangedListener(this);

        // raster data access
        this.renderContext.registerControl(this.subject, this);
        this.renderContext.registerControl(this.subject, this.hitTest);
    }

    @Override
    public synchronized void stop()
    {
        super.stop();

        this.subject.removeOnSelectionChangedListener(this);
        this.subject.getDataStore().removeOnDataStoreContentChangedListener(this);
        this.subject.removeOnSelectionVisibleChangedListener(this);
        this.subject.removeOnSelectionTransparencyChangedListener(this);

        // raster data access
        this.renderContext.unregisterControl(this.subject, this);
        this.renderContext.registerControl(this.subject, this.hitTest);
    }

    /**************************************************************************/
    // On Selection Changed Listener
    @Override
    public void onSelectionChanged(RasterLayer2 layer)
    {
        final String selection = layer.isAutoSelect() ? null : layer.getSelection();
        this.renderContext.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                GLAbstractDataStoreRasterLayer3.this.selection = selection;
                GLAbstractDataStoreRasterLayer3.this.invalidate();
            }
        });
    }

    /**************************************************************************/
    // On Data Store Content Changed Listener
    @Override
    public void onDataStoreContentChanged(RasterDataStore dataStore)
    {
        this.renderContext.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                GLAbstractDataStoreRasterLayer3.this.invalidate();
            }
        });
    }

    /**************************************************************************/
    // Raster Layer On Selection Visible Changed Listener
    @Override
    public void onSelectionVisibleChanged(RasterLayer2 layer)
    {
        this.renderContext.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                GLAbstractDataStoreRasterLayer3.this.invalidate();
            }
        });
    }

    /**************************************************************************/

    @Override
    public synchronized RasterDataAccess2 accessRasterData(GeoPoint point)
    {
        for (GLMapRenderable2 layer : this.renderable.values())
        {
            if(!(layer instanceof Controls))
                continue;
            final RasterDataAccessControl ctrl = ((Controls)layer).getControl(RasterDataAccessControl.class);
            if (ctrl == null)
                continue;
            final RasterDataAccess2 dataAccess = ctrl.accessRasterData(point);
            if (dataAccess != null)
                return dataAccess;
        }

        return null;
    }

    /**************************************************************************/

    @Override
    public void onTransparencyChanged(final RasterLayer2 ctrl)
    {
        this.renderContext.queueEvent(new Runnable()
        {
            @Override
            public void run()
            {
                synchronized (GLAbstractDataStoreRasterLayer3.this)
                {
                    for (GLMapRenderable2 r : GLAbstractDataStoreRasterLayer3.this.renderable.values())
                    {
                        updateTransparency(r);
                    }
                }
            }
        });
    }

    /**************************************************************************/

    protected class State extends ViewState
    {
        public String selection;
        public RasterDataStore.DatasetQueryParameters queryParams = new RasterDataStore.DatasetQueryParameters();

        @Override
        public void set(GLMapView view)
        {
            super.set(view);
            this.selection = GLAbstractDataStoreRasterLayer3.this.selection;
            this.upperLeft.set(this.northBound, this.westBound);
            this.upperRight.set(this.northBound, this.eastBound);
            this.lowerRight.set(this.southBound, this.eastBound);
            this.lowerLeft.set(this.southBound, this.westBound);
            this.updateQueryParams();
        }

        @Override
        public void copy(ViewState view)
        {
            super.copy(view);
            this.selection = ((State) view).selection;
            this.updateQueryParams();
        }

        /**
         * Updates the dataset query parameters to reflect the current state.
         */
        protected void updateQueryParams()
        {
            RasterDataStore.DatasetQueryParameters.clear(this.queryParams);
            if (this.upperLeft != null && this.lowerRight != null)
            {
                if (this.queryParams.spatialFilter instanceof RasterDataStore.DatasetQueryParameters.RegionSpatialFilter)
                {
                    RasterDataStore.DatasetQueryParameters.RegionSpatialFilter roi = (RasterDataStore.DatasetQueryParameters.RegionSpatialFilter) this.queryParams.spatialFilter;
                    roi.upperLeft = this.upperLeft;
                    roi.lowerRight = this.lowerRight;
                } else
                {
                    this.queryParams.spatialFilter = new RasterDataStore.DatasetQueryParameters.RegionSpatialFilter(this.upperLeft, this.lowerRight);
                }
            }

            // Utilize surface regions to calculate minimum GSD
            double minGsd = this.drawMapResolution;
            int width = Math.abs(_right - _left);
            int height = Math.abs(_top - _bottom);
            GeoPoint ul = GeoPoint.createMutable();
            GeoPoint ur = GeoPoint.createMutable();
            GeoPoint lr = GeoPoint.createMutable();
            GeoPoint ll = GeoPoint.createMutable();
            for (Envelope e : this.surfaceRegions)
            {
                ul.set(e.maxY, e.minX);
                ur.set(e.maxY, e.maxX);
                lr.set(e.minY, e.maxX);
                ll.set(e.minY, e.minX);
                double gsd = DatasetDescriptor.computeGSD(width, height, ul, ur, lr, ll);
                minGsd = Math.min(minGsd, gsd);
            }
            this.queryParams.minGsd = minGsd;
        }
    }

    static class HitTestControlImpl implements HitTestControl
    {
        Collection<HitTestControl> controls = Collections.newSetFromMap(new ConcurrentHashMap<>());

        @Override
        public void hitTest(MapRenderer3 renderer, HitTestQueryParameters params, Collection<HitTestResult> results)
        {
            for(HitTestControl ctrl : controls)
                ctrl.hitTest(renderer, params, results);
        }
    }
}
