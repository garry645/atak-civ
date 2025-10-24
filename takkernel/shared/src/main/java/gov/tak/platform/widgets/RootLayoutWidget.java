
package gov.tak.platform.widgets;

import com.atakmap.math.MathUtils;
import gov.tak.api.widgets.IMapWidget;
import gov.tak.api.widgets.IRootLayoutWidget;
import gov.tak.api.widgets.IScaleWidget2;
import gov.tak.platform.graphics.Rect;
import gov.tak.platform.graphics.RectF;
import gov.tak.platform.ui.UIEventQueue;
import gov.tak.platform.view.Gravity;
import gov.tak.platform.graphics.Insets;
import gov.tak.platform.ui.MotionEvent;
import gov.tak.platform.widget.LinearLayout;

import java.util.ArrayList;
import java.util.Collection;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

public class RootLayoutWidget extends LayoutWidget implements IRootLayoutWidget
{

    private static final String TAG = "RootLayoutWidget";

    private final static Insets EMPTY_INSETS = new Insets(0, 0, 0, 0);

    public static final int TOP_LEFT = 0;
    public static final int TOP_EDGE = 1;
    public static final int TOP_RIGHT = 2;
    public static final int LEFT_EDGE = 3;
    public static final int RIGHT_EDGE = 4;
    public static final int BOTTOM_LEFT = 5;
    public static final int BOTTOM_RIGHT = 6;
    public static final int BOTTOM_EDGE = 7;
    private static final String[] LAYOUT_NAMES = new String[] {
            "Top Left", "Top Edge", "Top Right", "Left Edge", "Right Edge",
            "Bottom Left", "Bottom Right", "Bottom Edge"
    };

    // Background colors used for debugging purposes
    private static final int[] LAYOUT_COLORS = new int[] {
            0x40FF0000, 0x4000FF00, 0x40FFFF00, 0x400000FF, 0x40FF00FF,
            0x4000FFFF, 0x40FFFFFF, 0x4000FF00
    };

    private IMapWidget _pressedWidget;
    private IMapWidget _hoveredWidget;
    private Timer widTimer;
    private WidTimerTask widTask;

    private final Insets _insets = new Insets(0, 0, 0, 0);
    private RectF _content;
    private final RectF _usableArea = new RectF();
    private IMapWidget _ignore;
    private final Map<Object, Rect> _occupiedRegions = new IdentityHashMap<>();

    private final LinearLayoutWidget[] _layouts = new LinearLayoutWidget[BOTTOM_EDGE + 1];

    private class WidTimerTask extends TimerTask
    {
        @Override
        public void run()
        {
            UIEventQueue.invokeLater(new Runnable()
            {
                @Override
                public void run()
                {
                    if (_pressedWidget != null)
                        _pressedWidget.onLongPress();
                }
            });
        }
    }

    public RootLayoutWidget()
    {
        final OnWidgetSizeChangedListener widgetSizeChangedListener = new OnWidgetSizeChangedListener()
        {
            @Override
            public void onWidgetSizeChanged(IMapWidget widget)
            {
                RootLayoutWidget.this.onWidgetSizeChanged(widget);
            }
        };

        // Initialize corner and side widgets
        for (int i = TOP_LEFT; i <= BOTTOM_EDGE; i++)
        {
            _layouts[i] = new LinearLayoutWidget();
            _layouts[i].setAlpha(255);
            _layouts[i].setName(LAYOUT_NAMES[i]);
            int gravity = 0;
            gravity |= (i == TOP_RIGHT || i == BOTTOM_RIGHT || i == RIGHT_EDGE)
                    ? Gravity.END
                    : (i == TOP_EDGE || i == BOTTOM_EDGE ? Gravity.CENTER_HORIZONTAL
                    : Gravity.START);
            gravity |= (i == BOTTOM_LEFT || i == BOTTOM_RIGHT || i == BOTTOM_EDGE)
                    ? Gravity.BOTTOM
                    : (i == LEFT_EDGE || i == RIGHT_EDGE
                    ? Gravity.CENTER_VERTICAL
                    : Gravity.TOP);
            _layouts[i].setGravity(gravity);
            //_layouts[i].setBackingColor(LAYOUT_COLORS[i]);
            _layouts[i].addOnWidgetSizeChangedListener(widgetSizeChangedListener);
            addChildWidget(_layouts[i]);
        }
    }

    /**
     * Get a layout given its corner index
     *
     * @param corner Corner index (TOP_LEFT, TOP_RIGHT, etc.)
     * @return Corner layout
     */
    public LinearLayoutWidget getLayout(int corner)
    {
        return (corner >= TOP_LEFT && corner <= BOTTOM_RIGHT)
                || corner == BOTTOM_EDGE ? _layouts[corner]
                : null;
    }

    public void setInsets(Insets insets)
    {
        if(insets == null)
            insets = EMPTY_INSETS;
        _insets.left = insets.left;
        _insets.top = insets.top;
        _insets.right = insets.right;
        _insets.bottom = insets.bottom;
    }

    @Override
    public void onSizeChanged()
    {
        super.onSizeChanged();
        for (LinearLayoutWidget l : _layouts)
        {
            onWidgetSizeChanged(l);
        }
    }

    public float[] getFullSize(IMapWidget w)
    {
        float[] p = w.getPadding();
        float[] m = w.getMargins();
        return new float[] {
                w.getWidth() + p[LEFT] + p[RIGHT] + m[LEFT] + m[RIGHT],
                w.getHeight() + p[TOP] + p[BOTTOM] + m[TOP] + m[BOTTOM]
        };
    }

    @Override
    public boolean handleMotionEvent(MotionEvent event)
    {

        IMapWidget hit = seekWidgetHit(event, event.getX(), event.getY());
        if (hit == null && _pressedWidget != null
                && event.getAction() == MotionEvent.ACTION_MOVE)
        {
            boolean used = _pressedWidget.onMove(event);
            // If this widget isn't handling move events then get rid of it
            if (!used)
            {
                _pressedWidget.onUnpress(event);
                _pressedWidget = null;
                return false;
            } else
            {
                return true;
            }
        }

        if (hit != _pressedWidget)
        {
            if (_pressedWidget != null)
            {
                _pressedWidget.onUnpress(event);
                _pressedWidget = null;
            }
        }

        // check for hover exit
        if (hit != _hoveredWidget)
        {
            if (_hoveredWidget != null)
            {
                MotionEvent hoverExit = MotionEvent.obtain(event.getDownTime(),
                        event.getEventTime(),
                        MotionEvent.ACTION_HOVER_EXIT,
                        event.getX(),
                        event.getY(),
                        event.getPressure(),
                        event.getSize(),
                        event.getMetaState(),
                        event.getXPrecision(),
                        event.getYPrecision(),
                        event.getDeviceId(),
                        event.getEdgeFlags());
                _hoveredWidget.onHover(hoverExit);
                _hoveredWidget = null;
            }
        }

        if (hit != null)
        {
            switch (event.getAction())
            {
                case MotionEvent.ACTION_DOWN:
                    if (_pressedWidget != hit)
                    {
                        hit.onPress(event);
                        _pressedWidget = hit;

                        //start long press countdown 1 sec
                        widTimer = new Timer("GLWidgetsMapComponent");
                        widTimer.schedule(widTask = new WidTimerTask(),
                                1000);
                    }
                    break;
                case MotionEvent.ACTION_MOVE:
                    if (_pressedWidget == hit)
                    {
                        hit.onMove(event);
                    } else
                    {
                        hit.onPress(event);
                        _pressedWidget = hit;
                    }
                    break;
                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    hit.onUnpress(event);
                    if (_pressedWidget == hit)
                    {
                        if (widTask != null)
                            widTask.cancel();
                        if (widTimer != null && widTimer.purge() > 0)
                        {
                            //the long press task was canceled, so onClick
                            hit.onClick(event);
                        } //otherwise, ignore
                    }
                    _pressedWidget = null;
                    break;

                case MotionEvent.ACTION_HOVER_ENTER:
                case MotionEvent.ACTION_HOVER_MOVE:
                    if (_hoveredWidget == null)
                    {
                        _hoveredWidget = hit;
                        MotionEvent hoverEnter = MotionEvent.obtain(event.getDownTime(),
                                event.getEventTime(),
                                MotionEvent.ACTION_HOVER_ENTER,
                                event.getX(),
                                event.getY(),
                                event.getPressure(),
                                event.getSize(),
                                event.getMetaState(),
                                event.getXPrecision(),
                                event.getYPrecision(),
                                event.getDeviceId(),
                                event.getEdgeFlags());
                        hit.onHover(hoverEnter);
                    } else
                    {
                        // pass along the move event
                        hit.onHover(event);
                    }
                    break;
            }
        }

        return hit != null;
    }

    /**
     * Sets an <I>occupied region</I> in the root layout widget. Border layouts will automatically
     * attempt to deconflict with any occupied regions when the root layout is revalidated.
     *
     * <P>This method may be used to either add or update a region associated with the specified
     * object.
     *
     * @param obj           The object that occupies the corresponding bounds
     * @param region        The occupied region
     * @param revalidate    If {@code true}, the layout will be revalidated before this method returns
     */
    protected void setOccupiedRegion(Object obj, Rect region, boolean revalidate)
    {
        _occupiedRegions.put(obj, region);
        if(revalidate)
            onSizeChanged();
    }

    /**
     * Removes an <I>occupied region</I> on the root layout widget.
     *
     * @param obj           The object that occupied a region in the layout
     * @param revalidate    If {@code true}, the layout will be revalidated before this method returns
     */
    protected void removeOccupiedRegion(Object obj, boolean revalidate)
    {
        _occupiedRegions.remove(obj);
        if(revalidate)
            onSizeChanged();
    }

    private void onWidgetSizeChanged(IMapWidget widget) {
        if (widget == _ignore)
            return;

        if (_content != null) {
            _usableArea.set(_insets.left, _insets.top,
                    _content.width() - _insets.right,
                    _content.height() - _insets.bottom);
        } else {
            _usableArea.set(_insets.left, _insets.top,
                    _insets.left + getWidth() - _insets.right,
                    _insets.top + getHeight() - _insets.bottom);
        }

        Rect maxBounds = LayoutHelper.getBounds(this);
        List<Rect> bounds = getOccupiedBounds(false);
        LayoutHelper layoutHelper = new LayoutHelper(maxBounds, bounds);

        LinearLayoutWidget w = (LinearLayoutWidget) widget;
        float[] wMargin = w.getMargins();
        float[] wSize = w.getSize(true, false);

        float left = _padding[LEFT] + wMargin[LEFT];
        float top = _padding[TOP] + wMargin[TOP];
        float right = _width - _padding[RIGHT] - wMargin[RIGHT] - wSize[0];
        float bottom = _height - _padding[BOTTOM] - wMargin[BOTTOM] - wSize[1];
        float parentWidth = (parent != null) ? parent.getWidth() : getWidth();
        float parentHeight = (parent != null) ? parent.getHeight() : getHeight();

        // Align layouts while avoiding overlap

        // Top-left corner
        if (w == _layouts[TOP_LEFT]) {

            Rect tlRect = getChildrenBounds(w);
            layoutHelper.add(_layouts[BOTTOM_LEFT]);
            tlRect = layoutHelper.findBestPosition(tlRect, TOP_LEFT);

            w.setPoint(tlRect.left, tlRect.top);
            onWidgetSizeChanged(_layouts[TOP_EDGE]);
            onWidgetSizeChanged(_layouts[LEFT_EDGE]);
        }

        // Top-right corner
        else if (w == _layouts[TOP_RIGHT]) {

            Rect trRect = getChildrenBounds(w);
            layoutHelper.add(_layouts[BOTTOM_RIGHT]);
            trRect = layoutHelper.findBestPosition(trRect, TOP_RIGHT);

            // Update widget position and padding
            w.setPoint(trRect.left, trRect.top);
            onWidgetSizeChanged(_layouts[TOP_EDGE]);
            onWidgetSizeChanged(_layouts[RIGHT_EDGE]);
        }

        // Bottom left corner
        else if (w == _layouts[BOTTOM_LEFT]) {
            Rect blRect = getChildrenBounds(w);
            layoutHelper.add(_layouts[BOTTOM_RIGHT]);
            layoutHelper.add(_layouts[TOP_LEFT]);
            layoutHelper.add(_layouts[BOTTOM_EDGE]);
            blRect = layoutHelper.findBestPosition(blRect, BOTTOM_LEFT);

            w.setPoint(blRect.left, blRect.top);
            onWidgetSizeChanged(_layouts[LEFT_EDGE]);
        }

        // Bottom right corner
        else if (w == _layouts[BOTTOM_RIGHT]) {
            // Adjust padding based on screen curve (if any)
            float pad = MathUtils.clamp(Math.min(
                    (right + wSize[0]) - _usableArea.right,
                    (bottom + wSize[1]) - _usableArea.bottom),
                    0, _insets.bottom);

            if (w.setPadding(0f, 0f, pad, pad))
                return;

            w.setPoint(right, bottom);
            onWidgetSizeChanged(_layouts[RIGHT_EDGE]);
            onWidgetSizeChanged(_layouts[BOTTOM_EDGE]);
        }

        // Top edge - fill area between top-left and top-right layouts
        else if (w == _layouts[TOP_EDGE]) {

            // Get the size of the content we need to fit
            float[] childrenSize = getChildrenSize(w);
            int width = (int) childrenSize[0];
            int height = (int) childrenSize[1];

            // If the children are set to MATCH_PARENT or have no defined size
            // then default to a decent percentage of the screen
            int defSize = (int) (Math.min(parentWidth, parentHeight) / 1.75f);
            if (width <= 0 || width >= parentWidth)
                width = defSize;
            if (height <= 0 || height >= parentHeight)
                height = defSize / 2;

            // Get all possible top-aligned boundaries and sort from top-to-bottom
            layoutHelper.add(_layouts[TOP_RIGHT]);
            layoutHelper.add(_layouts[TOP_LEFT]);

            // Find the best placement with bias toward the top-right corner
            Rect wr = new Rect(0, 0, width, height);
            wr = layoutHelper.findBestPosition(wr, TOP_EDGE);
            // Calculate the max available width
            wr = layoutHelper.findMaxWidth(wr);

            // Update position and size while preventing recursion
            _ignore = w;
            w.setPoint(wr.left, wr.top);
            w.setLayoutParams(wr.width(), LinearLayout.LayoutParams.WRAP_CONTENT);
            _ignore = null;
        }

        // Left edge - fill area between top-left and bottom-left layouts
        else if (w == _layouts[LEFT_EDGE]) {
            Rect leRect = getChildrenBounds(w);

            layoutHelper.add(_layouts[TOP_LEFT]);
            layoutHelper.add(_layouts[BOTTOM_LEFT]);
            layoutHelper.add(_layouts[BOTTOM_EDGE]);

            leRect = layoutHelper.findBestPosition(leRect, LEFT_EDGE);
            leRect = layoutHelper.findMaxHeight(leRect);

            w.setPoint(leRect.left, leRect.top);
            w.setLayoutParams(LinearLayoutWidget.WRAP_CONTENT, leRect.height());
        }

        // Right edge - fill area between top-right and bottom-right layouts
        else if (w == _layouts[RIGHT_EDGE]) {
            Rect reRect = getChildrenBounds(w);

            layoutHelper.add(_layouts[TOP_RIGHT]);
            layoutHelper.add(_layouts[BOTTOM_RIGHT]);

            reRect = layoutHelper.findBestPosition(reRect, RIGHT_EDGE);
            reRect = layoutHelper.findMaxHeight(reRect);

            w.setPoint(reRect.left, reRect.top);
            w.setLayoutParams(LinearLayoutWidget.WRAP_CONTENT, reRect.height());
        }

        //Bottom edge - fill full bottom area
        else if (w == _layouts[BOTTOM_EDGE]) {
            float bPad = (bottom + wSize[1]) - _usableArea.bottom;
            float lPad = MathUtils.clamp(Math.min(
                    _usableArea.left - left, bPad),
                    0, _insets.left);
            float rPad = MathUtils.clamp(Math.min(
                    (right + wSize[0]) - _usableArea.right, bPad),
                    0, _insets.right);

            Rect beRect = getChildrenBounds(w);

            layoutHelper.add(_layouts[BOTTOM_RIGHT]);

            beRect = layoutHelper.findBestPosition(beRect, BOTTOM_EDGE);
            beRect = layoutHelper.findMaxWidth(beRect);

            _ignore = w;
            w.setPadding(lPad, 0f, rPad, 0f);
            w.setPoint(beRect.left, beRect.top);
            w.setLayoutParams(beRect.width(), LinearLayoutWidget.WRAP_CONTENT);
            _ignore = null;
            onWidgetSizeChanged(_layouts[BOTTOM_LEFT]);
        }
    }

    /**
     * Get the occupied boundaries taken up by views (and optionally widgets)
     * in the root layout
     * @param includeWidgets True to include widgets in the result
     * @return Occupied boundary rectangles
     */
    private List<Rect> getOccupiedBounds(boolean includeWidgets) {
        List<Rect> ret = new ArrayList<>(_occupiedRegions.size() + _layouts.length);
        for (Rect v : _occupiedRegions.values()) {
            ret.add(v);
        }
        if (includeWidgets) {
            for (LinearLayoutWidget w : _layouts) {
                if (w.isVisible())
                    ret.add(LayoutHelper.getBounds(w));
            }
        }
        return ret;
    }

    /**
     * Get the occupied boundaries taken up by views and widgets
     * in the root layout
     * @return Occupied boundary rectangles
     */
    private List<Rect> getOccupiedBounds() {
        return getOccupiedBounds(true);
    }


    private static float[] getChildrenSize(LinearLayoutWidget w) {
        float maxW = 0, maxH = 0, totalW = 0, totalH = 0;
        Collection<IMapWidget> children = w.getChildren();
        for (IMapWidget c : children) {
            float[] size = getSize(c, true, true);
            if (c instanceof IScaleWidget2) {
                // Scale bar can dynamically resize based on available width
                // Use the minimum width instead of current width
                IScaleWidget2 sw = (IScaleWidget2) c;
                size[0] = sw.getMinWidth();
            }
            if (c instanceof LinearLayoutWidget) {
                LinearLayoutWidget llw = (LinearLayoutWidget) c;
                if (llw._paramWidth == LinearLayout.LayoutParams.MATCH_PARENT)
                    size[0] = 0;
                if (llw._paramHeight == LinearLayout.LayoutParams.MATCH_PARENT)
                    size[1] = 0;
            }
            maxW = Math.max(maxW, size[0]);
            maxH = Math.max(maxH, size[1]);
            totalW += size[0];
            totalH += size[1];
        }
        int orientation = w.getOrientation();
        float cWidth = orientation == LinearLayout.HORIZONTAL ? totalW : maxW;
        float cHeight = orientation == LinearLayout.VERTICAL ? totalH : maxH;
        return new float[] {
                cWidth, cHeight
        };
    }

    private static Rect getChildrenBounds(LinearLayoutWidget w) {
        float[] childrenSize = getChildrenSize(w);
        int width = (int) Math.max(1, childrenSize[0]);
        int height = (int) Math.max(1, childrenSize[1]);
        return new Rect(0, 0, width, height);
    }

}
