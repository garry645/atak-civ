
package com.atakmap.android.maps;

import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.Feature;
import com.atakmap.util.Diagnostic;

import org.junit.Test;

public class MarkerBasicListenerBenchmarkTest {

    private static final String TAG = "MarkerBasicListenerBenchmarkTest";
    private static final boolean ENABLED = false;

    @Test
    public void benchmark_1000_unique_listeners() {
        if (ENABLED) {
            doBench("1000 unique", new Runnable() {
                @Override
                public void run() {
                    Marker marker = new Marker("uid");
                    for (int i = 0; i < 1000; ++i) {
                        marker.addOnPointChangedListener(
                                new PointMapItem.OnPointChangedListener() {
                                    @Override
                                    public void onPointChanged(
                                            PointMapItem item) {
                                    }
                                });
                    }

                    marker.setPoint(new GeoPoint(1, 2));
                }
            });
        }
    }

    @Test
    public void benchmark_1000_same_instance() {
        if (ENABLED) {
            doBench("1000 same", new Runnable() {
                @Override
                public void run() {
                    Marker marker = new Marker("uid");
                    Marker.OnTitleChangedListener l = new Marker.OnTitleChangedListener() {
                        @Override
                        public void onTitleChanged(Marker marker) {
                        }
                    };

                    for (int i = 0; i < 1000; ++i) {
                        marker.addOnTitleChangedListener(l);
                    }

                    marker.setTitle("not_default_title");
                }
            });
        }
    }

    @Test
    public void benchmark_32_unique_listeners() {
        if (ENABLED) {
            doBench("32 unique", new Runnable() {
                @Override
                public void run() {
                    Marker marker = new Marker("uid");
                    for (int i = 0; i < 32; ++i) {
                        marker.addOnPointChangedListener(
                                new PointMapItem.OnPointChangedListener() {
                                    @Override
                                    public void onPointChanged(
                                            PointMapItem item) {
                                    }
                                });
                    }

                    marker.setPoint(new GeoPoint(1, 2));
                }
            });
        }
    }

    @Test
    public void benchmark_32_same_instance() {
        if (ENABLED) {
            doBench("32 same", new Runnable() {
                @Override
                public void run() {
                    Marker marker = new Marker("uid");
                    Marker.OnTitleChangedListener l = new Marker.OnTitleChangedListener() {
                        @Override
                        public void onTitleChanged(Marker marker) {
                        }
                    };

                    for (int i = 0; i < 32; ++i) {
                        marker.addOnTitleChangedListener(l);
                    }

                    marker.setTitle("not_default_title");
                }
            });
        }
    }

    private static class RealWorldMapItemListener
            implements MapItem.OnAltitudeModeChangedListener,
            MapItem.OnClickableChangedListener,
            MapItem.OnGroupChangedListener,
            MapItem.OnHeightChangedListener,
            MapItem.OnMetadataChangedListener,
            MapItem.OnTypeChangedListener,
            MapItem.OnVisibleChangedListener,
            MapItem.OnZOrderChangedListener {

        @Override
        public void onVisibleChanged(MapItem item) {
        }

        @Override
        public void onTypeChanged(MapItem item) {
        }

        @Override
        public void onMetadataChanged(MapItem item, String field) {
        }

        @Override
        public void onClickableChanged(MapItem item) {
        }

        @Override
        public void onZOrderChanged(MapItem item) {
        }

        @Override
        public void onHeightChanged(MapItem item) {
        }

        @Override
        public void onAltitudeModeChanged(Feature.AltitudeMode altitudeMode) {
        }

        @Override
        public void onItemAdded(MapItem item, MapGroup group) {
        }

        @Override
        public void onItemRemoved(MapItem item, MapGroup group) {
        }

        public void addTo(MapItem item) {
            item.addOnAltitudeModeChangedListener(this);
            item.addOnClickableChangedListener(this);
            item.addOnGroupChangedListener(this);
            item.addOnMetadataChangedListener("test", this);
            item.addOnHeightChangedListener(this);
            item.addOnVisibleChangedListener(this);
            item.addOnTypeChangedListener(this);
            item.addOnZOrderChangedListener(this);
        }
    }

    private static class RealWorldPointMapItemListener
            extends RealWorldMapItemListener implements
            PointMapItem.OnPointChangedListener {

        @Override
        public void onPointChanged(PointMapItem item) {
        }

        public void addTo(PointMapItem pointItem) {
            super.addTo(pointItem);
            pointItem.addOnPointChangedListener(this);
        }
    }

    private static class RealWorldMarkerListener
            extends RealWorldPointMapItemListener implements
            Marker.OnMarkerHitBoundsChangedListener,
            Marker.OnTrackChangedListener,
            Marker.OnTitleChangedListener,
            Marker.OnSummaryChangedListener,
            Marker.OnLabelTextSizeChangedListener,
            Marker.OnLabelPriorityChangedListener,
            Marker.OnIconChangedListener,
            Marker.OnStateChangedListener,
            Marker.OnStyleChangedListener {

        @Override
        public void onIconChanged(Marker marker) {
        }

        @Override
        public void onTitleChanged(Marker marker) {
        }

        @Override
        public void onLabelPriorityChanged(Marker marker) {
        }

        @Override
        public void onLabelSizeChanged(Marker marker) {
        }

        @Override
        public void onSummaryChanged(Marker marker) {
        }

        @Override
        public void onStateChanged(Marker marker) {
        }

        @Override
        public void onMarkerHitBoundsChanged(Marker marker) {
        }

        @Override
        public void onTrackChanged(Marker marker) {
        }

        @Override
        public void onStyleChanged(Marker marker) {
        }

        public void addTo(Marker marker) {
            super.addTo(marker);
            marker.addOnMarkerHitBoundsChangedListener(this);
            marker.addOnStyleChangedListener(this);
            marker.addOnIconChangedListener(this);
            marker.addOnStateChangedListener(this);
            marker.addOnSummaryChangedListener(this);
            marker.addOnTitleChangedListener(this);
            marker.addOnTrackChangedListener(this);
            marker.addOnLabelPriorityChangedListener(this);
            marker.addOnLabelSizeChangedListener(this);
        }
    }

    @Test
    public void benchmark_real_world() {
        if (ENABLED) {
            doBench("real-world", new Runnable() {
                @Override
                public void run() {
                    Marker marker = new Marker("uid");

                    // load up generally with what a "real-world" average marker might have
                    // A large all listener single instance and a handful of anonymous
                    // listeners

                    RealWorldMarkerListener l = new RealWorldMarkerListener();
                    l.addTo(marker);

                    marker.addOnTitleChangedListener(
                            new Marker.OnTitleChangedListener() {
                                @Override
                                public void onTitleChanged(Marker marker) {
                                }
                            });
                    marker.addOnPointChangedListener(
                            new PointMapItem.OnPointChangedListener() {
                                @Override
                                public void onPointChanged(PointMapItem item) {
                                }
                            });

                    marker.setTitle("not_default_title");
                    marker.setPoint(new GeoPoint(1, 2));
                    marker.setLabelTextSize(23);
                    marker.setIcon(new Icon());
                    marker.setSummary("summary");
                }
            });
        }
    }

    private void doBench(String name, Runnable runnable) {
        Diagnostic diag = new Diagnostic();
        for (int i = 0; i < 1000; ++i) {
            diag.start();
            runnable.run();
            diag.stop();
        }
        long duration = (diag.getDuration() / diag.getCount());
        Log.i(TAG, name + " duration= " + duration + "ns");
    }

}
