
package com.atakmap.android.maps;

import android.graphics.Rect;

import com.atakmap.coremap.maps.assets.Icon;
import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.Feature;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Captures the behavior of Marker, PointMapItem, and MapItem listener callbacks under
 * various conditions: simple add + invoke, double add + invoke, self remove, etc. This is
 * intended to catch regressions due to any changes to the listener callback implementations.
 */
public class MarkerBasicListenerInvokeTest {

    private static final String META_DATA_TEST_KEY = "test_test_key";
    private static final int GROUP_ADD_VAR = 1;
    private static final int GROUP_REMOVE_VAR = 2;

    private static class MapItemListener
            implements MapItem.OnAltitudeModeChangedListener,
            MapItem.OnClickableChangedListener,
            MapItem.OnGroupChangedListener,
            MapItem.OnHeightChangedListener,
            MapItem.OnMetadataChangedListener,
            MapItem.OnTypeChangedListener,
            MapItem.OnVisibleChangedListener,
            MapItem.OnZOrderChangedListener {

        private List<Class<?>> calls = new ArrayList<>();

        private List<Integer> variant = new ArrayList<>();

        private List<Object[]> callParams = new ArrayList<>();

        MapItem selfRemoveMapItem;

        protected void doAnySelfRemove() {
            if (selfRemoveMapItem != null) {
                removeFrom(selfRemoveMapItem);
                selfRemoveMapItem = null;
            }
        }

        protected void handle(Class<?> cls, Object param) {
            handle(cls, 0, param);
        }

        protected void handle(Class<?> cls, int v, Object param) {
            calls.add(cls);
            variant.add(v);
            Object[] params = new Object[1];
            params[0] = param;
            callParams.add(params);
            doAnySelfRemove();
        }

        protected void handle(Class<?> cls, Object param1, Object param2) {
            handle(cls, 0, param1, param2);
        }

        protected void handle(Class<?> cls, int v, Object param1,
                Object param2) {
            calls.add(cls);
            variant.add(v);
            Object[] params = new Object[2];
            params[0] = param1;
            params[1] = param2;
            callParams.add(params);
            doAnySelfRemove();
        }

        public void clear() {
            calls.clear();
            variant.clear();
            callParams.clear();
        }

        public void assertJust(Class<?> l, Object param) {
            Class<?>[] ls = {
                    l
            };
            int[] vars = {
                    0
            };
            Object[][] params = {
                    {
                            param
                    }
            };
            assertJust(ls, vars, params);
        }

        public void assertDouble(Class<?> l, Object param) {
            Class<?>[] ls = {
                    l
            };
            int[] vars = {
                    0
            };
            Object[][] params = {
                    {
                            param
                    }
            };
            assertDouble(ls, vars, params);
        }

        public void assertJust(Class<?>[] ls, Object[][] params) {
            int[] vars = new int[ls.length];
            Arrays.fill(vars, 0);
            assertJust(ls, vars, params);
        }

        public void assertDouble(Class<?>[] ls, Object[][] params) {
            int[] vars = new int[ls.length];
            Arrays.fill(vars, 0);
            assertDouble(ls, vars, params);
        }

        public void assertJust(Class<?>[] ls, int[] vars, Object[][] params) {
            Assert.assertEquals(calls.size(), ls.length);
            Assert.assertEquals(variant.size(), vars.length);
            Assert.assertEquals(ls.length, vars.length);
            for (int i = 0; i < calls.size(); ++i) {
                Assert.assertSame(ls[i], calls.get(i));
                Assert.assertEquals(variant.get(i).intValue(), vars[i]);
            }
            Assert.assertEquals(callParams.size(), params.length);
            for (int i = 0; i < calls.size(); ++i) {
                Object[] expectParams = callParams.get(0);
                for (int j = 0; j < expectParams.length; ++j) {
                    Assert.assertSame(params[i][j], expectParams[j]);
                }
            }
        }

        public void assertDouble(Class<?>[] ls, int[] vars, Object[][] params) {
            Class<?>[] ls2 = new Class<?>[ls.length * 2];
            int[] vars2 = new int[vars.length * 2];
            Object[][] params2 = new Object[params.length * 2][];
            for (int i = 0; i < ls2.length; ++i) {
                ls2[i] = ls[i / 2];
            }
            for (int i = 0; i < vars2.length; ++i) {
                vars2[i] = vars[i / 2];
            }
            for (int i = 0; i < params2.length; ++i) {
                params2[i] = params[i / 2];
            }
            assertJust(ls2, vars2, params2);
        }

        public void assertNothing() {
            Assert.assertEquals(calls.size(), 0);
            Assert.assertEquals(callParams.size(), 0);
        }

        @Override
        public void onVisibleChanged(MapItem item) {
            handle(MapItem.OnVisibleChangedListener.class, item);
        }

        @Override
        public void onTypeChanged(MapItem item) {
            handle(MapItem.OnTypeChangedListener.class, item);
        }

        @Override
        public void onMetadataChanged(MapItem item, String field) {
            handle(MapItem.OnMetadataChangedListener.class, item);
        }

        @Override
        public void onClickableChanged(MapItem item) {
            handle(MapItem.OnClickableChangedListener.class, item);
        }

        @Override
        public void onZOrderChanged(MapItem item) {
            handle(MapItem.OnZOrderChangedListener.class, item);
        }

        @Override
        public void onHeightChanged(MapItem item) {
            handle(MapItem.OnHeightChangedListener.class, item);
        }

        @Override
        public void onAltitudeModeChanged(Feature.AltitudeMode altitudeMode) {
            handle(MapItem.OnAltitudeModeChangedListener.class, altitudeMode);
        }

        @Override
        public void onItemAdded(MapItem item, MapGroup group) {
            handle(MapItem.OnGroupChangedListener.class, GROUP_ADD_VAR, item,
                    group);
        }

        @Override
        public void onItemRemoved(MapItem item, MapGroup group) {
            handle(MapItem.OnGroupChangedListener.class, GROUP_REMOVE_VAR, item,
                    group);
        }

        public void addTo(MapItem item) {
            item.addOnAltitudeModeChangedListener(this);
            item.addOnClickableChangedListener(this);
            item.addOnGroupChangedListener(this);
            item.addOnMetadataChangedListener(META_DATA_TEST_KEY, this);
            item.addOnHeightChangedListener(this);
            item.addOnVisibleChangedListener(this);
            item.addOnTypeChangedListener(this);
            item.addOnZOrderChangedListener(this);
        }

        public void removeFrom(MapItem item) {
            item.removeOnAltitudeModeChangedListener(this);
            item.removeOnClickableChangedListener(this);
            item.removeOnGroupChangedListener(this);
            item.removeOnMetadataChangedListener(META_DATA_TEST_KEY, this);
            item.removeOnHeightChangedListener(this);
            item.removeOnVisibleChangedListener(this);
            item.removeOnTypeChangedListener(this);
            item.removeOnZOrderChangedListener(this);
        }
    }

    private static class PointMapItemListener extends MapItemListener
            implements PointMapItem.OnPointChangedListener {

        PointMapItem selfRemovePointMapItem;

        @Override
        protected void doAnySelfRemove() {
            super.doAnySelfRemove();
            if (selfRemovePointMapItem != null) {
                removeFrom(selfRemovePointMapItem);
                selfRemovePointMapItem = null;
            }
        }

        @Override
        public void onPointChanged(PointMapItem item) {
            handle(PointMapItem.OnPointChangedListener.class, item);
        }

        public void addTo(PointMapItem pointItem) {
            super.addTo(pointItem);
            pointItem.addOnPointChangedListener(this);
        }

        public void removeFrom(PointMapItem pointItem) {
            super.removeFrom(pointItem);
            pointItem.removeOnPointChangedListener(this);
        }
    }

    private static class MarkerListener extends PointMapItemListener
            implements Marker.OnMarkerHitBoundsChangedListener,
            Marker.OnTrackChangedListener,
            Marker.OnTitleChangedListener,
            Marker.OnSummaryChangedListener,
            Marker.OnLabelTextSizeChangedListener,
            Marker.OnLabelPriorityChangedListener,
            Marker.OnIconChangedListener,
            Marker.OnStateChangedListener,
            Marker.OnStyleChangedListener {

        Marker selfRemoveMarker;

        @Override
        protected void doAnySelfRemove() {
            super.doAnySelfRemove();
            if (selfRemoveMarker != null) {
                removeFrom(selfRemoveMarker);
                selfRemoveMarker = null;
            }
        }

        @Override
        public void onIconChanged(Marker marker) {
            handle(Marker.OnIconChangedListener.class, marker);
        }

        @Override
        public void onTitleChanged(Marker marker) {
            handle(Marker.OnTitleChangedListener.class, marker);
        }

        @Override
        public void onLabelPriorityChanged(Marker marker) {
            handle(Marker.OnLabelPriorityChangedListener.class, marker);
        }

        @Override
        public void onLabelSizeChanged(Marker marker) {
            handle(Marker.OnLabelTextSizeChangedListener.class, marker);
        }

        @Override
        public void onSummaryChanged(Marker marker) {
            handle(Marker.OnSummaryChangedListener.class, marker);
        }

        @Override
        public void onStateChanged(Marker marker) {
            handle(Marker.OnStateChangedListener.class, marker);
        }

        @Override
        public void onMarkerHitBoundsChanged(Marker marker) {
            handle(Marker.OnMarkerHitBoundsChangedListener.class, marker);
        }

        @Override
        public void onTrackChanged(Marker marker) {
            handle(Marker.OnTrackChangedListener.class, marker);
        }

        @Override
        public void onStyleChanged(Marker marker) {
            handle(Marker.OnStyleChangedListener.class, marker);
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

        public void removeFrom(Marker marker) {
            marker.removeOnMarkerHitBoundsChangedListener(this);
            marker.removeOnStyleChangedListener(this);
            marker.removeOnIconChangedListener(this);
            marker.removeOnStateChangedListener(this);
            marker.removeOnSummaryChangedListener(this);
            marker.removeOnTitleChangedListener(this);
            marker.removeOnTrackChangedListener(this);
            marker.removeOnLabelPriorityChangedListener(this);
            marker.removeOnLabelSizeChangedListner(this);
        }
    }

    //
    // simple_invoke_
    //

    @Test
    public void simple_invoke_MapItem_OnAltitudeModeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        item.setAltitudeMode(Feature.AltitudeMode.ClampToGround);
        l.assertJust(MapItem.OnAltitudeModeChangedListener.class,
                Feature.AltitudeMode.ClampToGround);
    }

    @Test
    public void simple_invoke_MapItem_OnClickableChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        item.setClickable(!MapItem.CLICKABLE_DEFAULT);
        l.assertJust(MapItem.OnClickableChangedListener.class, item);
    }

    @Test
    public void simple_invoke_MapItem_OnGroupChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        RootMapGroup mapGroup = new RootMapGroup();
        mapGroup.addItem(item);
        Class<?>[] ls = {
                MapItem.OnGroupChangedListener.class,
        };
        Object[][] params = {
                {
                        item, mapGroup
                },
        };
        int[] addVar = {
                GROUP_ADD_VAR
        };
        l.assertJust(ls, addVar, params);
        l.clear();
        mapGroup.removeItem(item);
        int[] removeVar = {
                GROUP_REMOVE_VAR
        };
        l.assertJust(ls, removeVar, params);
    }

    @Test
    public void simple_invoke_MapItem_OnHeightChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        item.setHeight(12344.0);
        l.assertJust(MapItem.OnHeightChangedListener.class, item);
    }

    @Test
    public void simple_invoke_MapItem_OnMetadataChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        item.setMetaBoolean(META_DATA_TEST_KEY, true);
        Object[][] params = {
                {
                        item, META_DATA_TEST_KEY
                }
        };
        Class<?>[] calls = {
                MapItem.OnMetadataChangedListener.class
        };
        l.assertJust(calls, params);
    }

    @Test
    public void simple_invoke_MapItem_OnTypeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        item.setType("not_default_type");
        l.assertJust(MapItem.OnTypeChangedListener.class, item);
    }

    @Test
    public void simple_invoke_MapItem_OnVisibleChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        item.setVisible(!MapItem.VISIBLE_DEFAULT);
        l.assertJust(MapItem.OnVisibleChangedListener.class, item);
    }

    @Test
    public void simple_invoke_MapItem_OnZOrderChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        item.setZOrder(MapItem.ZORDER_DEFAULT + 1);
        l.assertJust(MapItem.OnZOrderChangedListener.class, item);
    }

    @Test
    public void simple_invoke_PointMapItem_OnPointChangedListener() {
        Marker marker = new Marker("uid");
        PointMapItemListener l = new PointMapItemListener();
        l.addTo(marker);
        marker.setPoint(new GeoPoint(1, 1));
        l.assertJust(PointMapItem.OnPointChangedListener.class, marker);
    }

    @Test
    public void simple_invoke_Marker_OnMarkerHitBoundsChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        marker.setMarkerHitBounds(new Rect());
        l.assertJust(Marker.OnMarkerHitBoundsChangedListener.class, marker);
    }

    @Test
    public void simple_invoke_Marker_OnStyleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        marker.setStyle(1);
        l.assertJust(Marker.OnStyleChangedListener.class, marker);
    }

    @Test
    public void simple_invoke_Marker_OnIconChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        marker.setIcon(new Icon());
        l.assertJust(Marker.OnIconChangedListener.class, marker);
    }

    @Test
    public void simple_invoke_Marker_OnStateChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        marker.setState(1);
        l.assertJust(Marker.OnStateChangedListener.class, marker);
    }

    @Test
    public void simple_invoke_Marker_OnSummaryChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        marker.setSummary("foo");
        Class<?>[] ls = {
                Marker.OnLabelPriorityChangedListener.class,
                Marker.OnSummaryChangedListener.class
        };
        Object[][] params = {
                {
                        marker
                },
                {
                        marker
                }
        };
        l.assertJust(ls, params);
    }

    @Test
    public void simple_invoke_Marker_OnTitleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        marker.setTitle("foo");
        l.assertJust(Marker.OnTitleChangedListener.class, marker);
    }

    @Test
    public void simple_invoke_Marker_OnTrackChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        marker.setTrack(1, 2);
        l.assertJust(Marker.OnTrackChangedListener.class, marker);
    }

    @Test
    public void simple_invoke_Marker_OnLabelPriorityChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        marker.setLabelPriority(Marker.LabelPriority.High);
        l.assertJust(Marker.OnLabelPriorityChangedListener.class, marker);
    }

    @Test
    public void simple_invoke_Marker_OnLabelTextSizeChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        marker.setLabelTextSize(100);
        l.assertJust(Marker.OnLabelTextSizeChangedListener.class, marker);
    }

    //
    // no_invoke_after_remove_
    //

    @Test
    public void no_invoke_after_remove_MapItem_OnAltitudeModeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.removeFrom(item);
        item.setAltitudeMode(Feature.AltitudeMode.ClampToGround);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_MapItem_OnClickableChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.removeFrom(item);
        item.setClickable(!MapItem.CLICKABLE_DEFAULT);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_MapItem_OnGroupChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.removeFrom(item);
        RootMapGroup mapGroup = new RootMapGroup();
        mapGroup.addItem(item);
        l.assertNothing();
        l.clear();
        mapGroup.removeItem(item);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_MapItem_OnHeightChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.removeFrom(item);
        item.setHeight(12344.0);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_MapItem_OnMetadataChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.removeFrom(item);
        item.setMetaBoolean(META_DATA_TEST_KEY, true);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_MapItem_OnTypeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.removeFrom(item);
        item.setType("not_default_type");
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_MapItem_OnVisibleChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.removeFrom(item);
        item.setVisible(!MapItem.VISIBLE_DEFAULT);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_MapItem_OnZOrderChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.removeFrom(item);
        item.setZOrder(MapItem.ZORDER_DEFAULT + 1);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_PointMapItem_OnPointChangedListener() {
        Marker marker = new Marker("uid");
        PointMapItemListener l = new PointMapItemListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setPoint(new GeoPoint(1, 1));
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_Marker_OnMarkerHitBoundsChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setMarkerHitBounds(new Rect());
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_Marker_OnStyleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setStyle(1);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_Marker_OnIconChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setIcon(new Icon());
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_Marker_OnStateChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setState(1);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_Marker_OnSummaryChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setSummary("foo");
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_Marker_OnTitleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setTitle("foo");
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_Marker_OnTrackChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setTrack(1, 2);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_Marker_OnLabelPriorityChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setLabelPriority(Marker.LabelPriority.High);
        l.assertNothing();
    }

    @Test
    public void no_invoke_after_remove_Marker_OnLabelTextSizeChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setLabelTextSize(100);
        l.assertNothing();
    }

    //
    // double_add_invoke_
    //

    @Test
    public void double_add_invoke_MapItem_OnAltitudeModeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        item.setAltitudeMode(Feature.AltitudeMode.ClampToGround);
        l.assertDouble(MapItem.OnAltitudeModeChangedListener.class,
                Feature.AltitudeMode.ClampToGround);
    }

    @Test
    public void double_add_invoke_MapItem_OnClickableChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        item.setClickable(!MapItem.CLICKABLE_DEFAULT);
        l.assertDouble(MapItem.OnClickableChangedListener.class, item);
    }

    @Test
    public void double_add_invoke_MapItem_OnGroupChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        RootMapGroup mapGroup = new RootMapGroup();
        mapGroup.addItem(item);
        Class<?>[] ls = {
                MapItem.OnGroupChangedListener.class,
        };
        Object[][] params = {
                {
                        item, mapGroup
                },
        };
        int[] addVar = {
                GROUP_ADD_VAR
        };
        l.assertDouble(ls, addVar, params);
        l.clear();
        mapGroup.removeItem(item);
        int[] removeVar = {
                GROUP_REMOVE_VAR
        };
        l.assertDouble(ls, removeVar, params);
    }

    @Test
    public void double_add_invoke_MapItem_OnHeightChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        item.setHeight(12344.0);
        l.assertDouble(MapItem.OnHeightChangedListener.class, item);
    }

    @Test
    public void double_add_invoke_MapItem_OnMetadataChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        item.setMetaBoolean(META_DATA_TEST_KEY, true);
        Object[][] params = {
                {
                        item, META_DATA_TEST_KEY
                }
        };
        Class<?>[] calls = {
                MapItem.OnMetadataChangedListener.class
        };
        l.assertDouble(calls, params);
    }

    @Test
    public void double_add_invoke_MapItem_OnTypeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        item.setType("not_default_type");
        l.assertDouble(MapItem.OnTypeChangedListener.class, item);
    }

    @Test
    public void double_add_invoke_MapItem_OnVisibleChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        item.setVisible(!MapItem.VISIBLE_DEFAULT);
        l.assertDouble(MapItem.OnVisibleChangedListener.class, item);
    }

    @Test
    public void double_add_invoke_MapItem_OnZOrderChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        item.setZOrder(MapItem.ZORDER_DEFAULT + 1);
        l.assertDouble(MapItem.OnZOrderChangedListener.class, item);
    }

    @Test
    public void double_add_invoke_PointMapItem_OnPointChangedListener() {
        Marker marker = new Marker("uid");
        PointMapItemListener l = new PointMapItemListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setPoint(new GeoPoint(1, 1));
        l.assertJust(PointMapItem.OnPointChangedListener.class, marker);
    }

    @Test
    public void double_add_invoke_Marker_OnMarkerHitBoundsChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setMarkerHitBounds(new Rect());
        l.assertDouble(Marker.OnMarkerHitBoundsChangedListener.class, marker);
    }

    @Test
    public void double_add_invoke_Marker_OnStyleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setStyle(1);
        l.assertDouble(Marker.OnStyleChangedListener.class, marker);
    }

    @Test
    public void double_add_invoke_Marker_OnIconChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setIcon(new Icon());
        l.assertDouble(Marker.OnIconChangedListener.class, marker);
    }

    @Test
    public void double_add_invoke_Marker_OnStateChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setState(1);
        l.assertDouble(Marker.OnStateChangedListener.class, marker);
    }

    @Test
    public void double_add_invoke_Marker_OnSummaryChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setSummary("foo");
        Class<?>[] ls = {
                Marker.OnLabelPriorityChangedListener.class,
                Marker.OnSummaryChangedListener.class
        };
        Object[][] params = {
                {
                        marker
                },
                {
                        marker
                }
        };
        l.assertDouble(ls, params);
    }

    @Test
    public void double_add_invoke_Marker_OnTitleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setTitle("foo");
        l.assertDouble(Marker.OnTitleChangedListener.class, marker);
    }

    @Test
    public void double_add_invoke_Marker_OnTrackChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setTrack(1, 2);
        l.assertDouble(Marker.OnTrackChangedListener.class, marker);
    }

    @Test
    public void double_add_invoke_Marker_OnLabelPriorityChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setLabelPriority(Marker.LabelPriority.High);
        l.assertDouble(Marker.OnLabelPriorityChangedListener.class, marker);
    }

    @Test
    public void double_add_invoke_Marker_OnLabelTextSizeChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        marker.setLabelTextSize(100);
        l.assertDouble(Marker.OnLabelTextSizeChangedListener.class, marker);
    }

    //
    // double_add_remove_invoke_
    //

    @Test
    public void double_add_remove_invoke_MapItem_OnAltitudeModeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        l.removeFrom(item);
        item.setAltitudeMode(Feature.AltitudeMode.ClampToGround);
        l.assertJust(MapItem.OnAltitudeModeChangedListener.class,
                Feature.AltitudeMode.ClampToGround);
    }

    @Test
    public void double_add_remove_invoke_MapItem_OnClickableChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        l.removeFrom(item);
        item.setClickable(!MapItem.CLICKABLE_DEFAULT);
        l.assertJust(MapItem.OnClickableChangedListener.class, item);
    }

    @Test
    public void double_add_remove_invoke_MapItem_OnGroupChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        l.removeFrom(item);
        RootMapGroup mapGroup = new RootMapGroup();
        mapGroup.addItem(item);
        Class<?>[] ls = {
                MapItem.OnGroupChangedListener.class,
        };
        Object[][] params = {
                {
                        item, mapGroup
                },
        };
        int[] addVar = {
                GROUP_ADD_VAR
        };
        l.assertJust(ls, addVar, params);
        l.clear();
        mapGroup.removeItem(item);
        int[] removeVar = {
                GROUP_REMOVE_VAR
        };
        l.assertJust(ls, removeVar, params);
    }

    @Test
    public void double_add_remove_invoke_MapItem_OnHeightChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        l.removeFrom(item);
        item.setHeight(12344.0);
        l.assertJust(MapItem.OnHeightChangedListener.class, item);
    }

    @Test
    public void double_add_remove_invoke_MapItem_OnMetadataChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        l.removeFrom(item);
        item.setMetaBoolean(META_DATA_TEST_KEY, true);
        Object[][] params = {
                {
                        item, META_DATA_TEST_KEY
                }
        };
        Class<?>[] calls = {
                MapItem.OnMetadataChangedListener.class
        };
        l.assertJust(calls, params);
    }

    @Test
    public void double_add_remove_invoke_MapItem_OnTypeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        l.removeFrom(item);
        item.setType("not_default_type");
        l.assertJust(MapItem.OnTypeChangedListener.class, item);
    }

    @Test
    public void double_add_remove_invoke_MapItem_OnVisibleChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        l.removeFrom(item);
        item.setVisible(!MapItem.VISIBLE_DEFAULT);
        l.assertJust(MapItem.OnVisibleChangedListener.class, item);
    }

    @Test
    public void double_add_remove_invoke_MapItem_OnZOrderChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.addTo(item);
        l.addTo(item);
        l.removeFrom(item);
        item.setZOrder(MapItem.ZORDER_DEFAULT + 1);
        l.assertJust(MapItem.OnZOrderChangedListener.class, item);
    }

    @Test
    public void double_add_remove_invoke_PointMapItem_OnPointChangedListener() {
        Marker marker = new Marker("uid");
        PointMapItemListener l = new PointMapItemListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setPoint(new GeoPoint(1, 1));
        l.assertNothing();
    }

    @Test
    public void double_add_remove_invoke_Marker_OnMarkerHitBoundsChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setMarkerHitBounds(new Rect());
        l.assertJust(Marker.OnMarkerHitBoundsChangedListener.class, marker);
    }

    @Test
    public void double_add_remove_invoke_Marker_OnStyleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setStyle(1);
        l.assertJust(Marker.OnStyleChangedListener.class, marker);
    }

    @Test
    public void double_add_remove_invoke_Marker_OnIconChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setIcon(new Icon());
        l.assertJust(Marker.OnIconChangedListener.class, marker);
    }

    @Test
    public void double_add_remove_invoke_Marker_OnStateChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setState(1);
        l.assertJust(Marker.OnStateChangedListener.class, marker);
    }

    @Test
    public void double_add_remove_invoke_Marker_OnSummaryChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setSummary("foo");
        Class<?>[] ls = {
                Marker.OnLabelPriorityChangedListener.class,
                Marker.OnSummaryChangedListener.class
        };
        Object[][] params = {
                {
                        marker
                },
                {
                        marker
                }
        };
        l.assertJust(ls, params);
    }

    @Test
    public void double_add_remove_invoke_Marker_OnTitleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setTitle("foo");
        l.assertJust(Marker.OnTitleChangedListener.class, marker);
    }

    @Test
    public void double_add_remove_invoke_Marker_OnTrackChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setTrack(1, 2);
        l.assertJust(Marker.OnTrackChangedListener.class, marker);
    }

    @Test
    public void double_add_remove_invoke_Marker_OnLabelPriorityChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setLabelPriority(Marker.LabelPriority.High);
        l.assertJust(Marker.OnLabelPriorityChangedListener.class, marker);
    }

    @Test
    public void double_add_remove_invoke_Marker_OnLabelTextSizeChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.addTo(marker);
        l.addTo(marker);
        l.removeFrom(marker);
        marker.setLabelTextSize(100);
        l.assertJust(Marker.OnLabelTextSizeChangedListener.class, marker);
    }

    //
    // add_self_remove_invoke_
    //

    @Test
    public void add_self_remove_invoke_MapItem_OnAltitudeModeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.selfRemoveMapItem = item;
        l.addTo(item);
        item.setAltitudeMode(Feature.AltitudeMode.ClampToGround);
        l.assertJust(MapItem.OnAltitudeModeChangedListener.class,
                Feature.AltitudeMode.ClampToGround);
        l.clear();
        item.setAltitudeMode(Feature.AltitudeMode.Absolute);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_MapItem_OnClickableChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.selfRemoveMapItem = item;
        l.addTo(item);
        item.setClickable(!MapItem.CLICKABLE_DEFAULT);
        l.assertJust(MapItem.OnClickableChangedListener.class, item);
        l.clear();
        item.setClickable(MapItem.CLICKABLE_DEFAULT);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_MapItem_OnGroupChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.selfRemoveMapItem = item;
        l.addTo(item);
        RootMapGroup mapGroup = new RootMapGroup();
        mapGroup.addItem(item);
        Class<?>[] ls = {
                MapItem.OnGroupChangedListener.class,
        };
        Object[][] params = {
                {
                        item, mapGroup
                },
        };
        int[] addVar = {
                GROUP_ADD_VAR
        };
        l.assertJust(ls, addVar, params);
        l.clear();
        mapGroup.removeItem(item);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_MapItem_OnHeightChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.selfRemoveMapItem = item;
        l.addTo(item);
        item.setHeight(12344.0);
        l.assertJust(MapItem.OnHeightChangedListener.class, item);
        l.clear();
        item.setHeight(0.0);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_MapItem_OnMetadataChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.selfRemoveMapItem = item;
        l.addTo(item);
        item.setMetaBoolean(META_DATA_TEST_KEY, true);
        Object[][] params = {
                {
                        item, META_DATA_TEST_KEY
                }
        };
        Class<?>[] calls = {
                MapItem.OnMetadataChangedListener.class
        };
        l.assertJust(calls, params);
        l.clear();
        item.setMetaBoolean(META_DATA_TEST_KEY, false);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_MapItem_OnTypeChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.selfRemoveMapItem = item;
        l.addTo(item);
        item.setType("not_default_type");
        l.assertJust(MapItem.OnTypeChangedListener.class, item);
        l.clear();
        item.setType("not_default_type2");
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_MapItem_OnVisibleChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.selfRemoveMapItem = item;
        l.addTo(item);
        item.setVisible(!MapItem.VISIBLE_DEFAULT);
        l.assertJust(MapItem.OnVisibleChangedListener.class, item);
        l.clear();
        item.setVisible(MapItem.VISIBLE_DEFAULT);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_MapItem_OnZOrderChangedListener() {
        MapItem item = new Marker("uid");
        MapItemListener l = new MapItemListener();
        l.selfRemoveMapItem = item;
        l.addTo(item);
        item.setZOrder(MapItem.ZORDER_DEFAULT + 1);
        l.assertJust(MapItem.OnZOrderChangedListener.class, item);
        l.clear();
        item.setZOrder(MapItem.ZORDER_DEFAULT);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_PointMapItem_OnPointChangedListener() {
        Marker marker = new Marker("uid");
        PointMapItemListener l = new PointMapItemListener();
        l.selfRemovePointMapItem = marker;
        l.addTo(marker);
        marker.setPoint(new GeoPoint(1, 1));
        l.assertJust(PointMapItem.OnPointChangedListener.class, marker);
        l.clear();
        marker.setPoint(new GeoPoint(0, 0));
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_Marker_OnMarkerHitBoundsChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.selfRemoveMarker = marker;
        l.addTo(marker);
        marker.setMarkerHitBounds(new Rect());
        l.assertJust(Marker.OnMarkerHitBoundsChangedListener.class, marker);
        l.clear();
        marker.setMarkerHitBounds(new Rect(1, 1, 1, 1));
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_Marker_OnStyleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.selfRemoveMarker = marker;
        l.addTo(marker);
        marker.setStyle(1);
        l.assertJust(Marker.OnStyleChangedListener.class, marker);
        l.clear();
        marker.setStyle(0);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_Marker_OnIconChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.selfRemoveMarker = marker;
        l.addTo(marker);
        marker.setIcon(new Icon());
        l.assertJust(Marker.OnIconChangedListener.class, marker);
        l.clear();
        marker.setIcon(new Icon());
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_Marker_OnStateChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.selfRemoveMarker = marker;
        l.addTo(marker);
        marker.setState(1);
        l.assertJust(Marker.OnStateChangedListener.class, marker);
        l.clear();
        marker.setState(0);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_Marker_OnSummaryChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.selfRemoveMarker = marker;
        l.addTo(marker);
        marker.setSummary("foo");
        Class<?>[] ls = {
                Marker.OnLabelPriorityChangedListener.class,
        };
        Object[][] params = {
                {
                        marker
                }
        };
        l.assertJust(ls, params);
        l.clear();
        marker.setSummary("foo2");
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_Marker_OnTitleChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.selfRemoveMarker = marker;
        l.addTo(marker);
        marker.setTitle("foo");
        l.assertJust(Marker.OnTitleChangedListener.class, marker);
        l.clear();
        marker.setTitle("foo2");
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_Marker_OnTrackChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.selfRemoveMarker = marker;
        l.addTo(marker);
        marker.setTrack(1, 2);
        l.assertJust(Marker.OnTrackChangedListener.class, marker);
        l.clear();
        marker.setTrack(3, 4);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_Marker_OnLabelPriorityChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.selfRemoveMarker = marker;
        l.addTo(marker);
        marker.setLabelPriority(Marker.LabelPriority.High);
        l.assertJust(Marker.OnLabelPriorityChangedListener.class, marker);
        l.clear();
        marker.setLabelPriority(Marker.LabelPriority.Low);
        l.assertNothing();
    }

    @Test
    public void add_self_remove_invoke_Marker_OnLabelTextSizeChangedListener() {
        Marker marker = new Marker("uid");
        MarkerListener l = new MarkerListener();
        l.selfRemoveMarker = marker;
        l.addTo(marker);
        marker.setLabelTextSize(100);
        l.assertJust(Marker.OnLabelTextSizeChangedListener.class, marker);
        l.clear();
        marker.setLabelTextSize(101);
        l.assertNothing();
    }
}
