package gov.tak.platform.widgets;

import gov.tak.api.widgets.IMapWidget;
import gov.tak.platform.graphics.Point;
import gov.tak.platform.graphics.Rect;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

final class LayoutHelper
{
    private static final Comparator<Rect> SORT_TOP = new Comparator<Rect>() {
        @Override
        public int compare(Rect r1, Rect r2) {
            int topComp = Integer.compare(r1.top, r2.top);
            if (topComp != 0)
                return topComp;
            return Integer.compare(r1.left, r2.left);
        }
    };

    private static final Comparator<Rect> SORT_BOTTOM = new Comparator<Rect>() {
        @Override
        public int compare(Rect r1, Rect r2) {
            int botComp = Integer.compare(r1.bottom, r2.bottom);
            if (botComp != 0)
                return botComp;
            return Integer.compare(r1.left, r2.left);
        }
    };

    // Sort modes - used for faster intersection calculations
    public static final int UNSORTED = -1;
    public static final int SORT_ORDER_TOP = 0;

    // Distance computation methods
    public static final int DIST_X = 0;
    public static final int DIST_Y = 1;
    public static final int DIST_HYPOT = 2;

    private final Rect _maxBounds;
    private final List<Rect> _occupied;

    private int _sortOrder = UNSORTED;

    public LayoutHelper(Rect maxBounds, List<Rect> otherBounds) {
        _maxBounds = maxBounds;
        _occupied = new ArrayList<>(otherBounds);
    }

    /**
     * Add occupied boundary for a given widget
     * @param widget Widget
     */
    public void add(IMapWidget widget) {
        _occupied.add(getBounds(widget));
        _sortOrder = UNSORTED;
    }

    /**
     * Add an arbitrary occupied boundary
     * @param region the occupied boundary
     */
    public void add(Rect region) {
        _occupied.add(new Rect(region));
        _sortOrder = UNSORTED;
    }

    /**
     * Get the list of occupied boundaries
     * @return List of boundaries
     */
    public List<Rect> getOccupiedBounds() {
        return new ArrayList<>(_occupied);
    }

    /**
     * Sort the other bounds by the specified sort order
     * This is used to make intersection testing more efficient
     * @param sortOrder Sort order (either {@link #SORT_ORDER_TOP}
     *                 or {@link #UNSORTED})
     */
    public void sort(int sortOrder) {
        if (_sortOrder != sortOrder) {
            if (sortOrder == SORT_ORDER_TOP)
                Collections.sort(_occupied, SORT_TOP);
            _sortOrder = sortOrder;
        }
    }

    /**
     * Find the best position for the given bounds
     * @param bounds Widget/view bounds
     * @param desiredPt Desired position
     * @param distMethod Distance calculation method
     * @return Rectangle closest to desired position
     */
    public Rect findBestPosition(Rect bounds, Point desiredPt, int distMethod) {
        Rect ret = new Rect(bounds);
        Point bestPt = new Point(desiredPt.x, desiredPt.y);
        ret.offsetTo(bestPt.x, bestPt.y);
        return ret;
    }

    /**
     * Find the best position for the given bounds
     * @param bounds Widget/view bounds
     * @param desiredPt Desired position
     * @return Rectangle closest to desired position
     */
    public Rect findBestPosition(Rect bounds, Point desiredPt) {
        return findBestPosition(bounds, desiredPt, DIST_HYPOT);
    }

    /**
     * Find the best position for the given bounds
     * @param bounds Widget/view bounds
     * @param position Desired {@link RootLayoutWidget} position
     * @return Rectangle closest to desired position
     */
    public Rect findBestPosition(Rect bounds, int position) {

        int desiredX = 0, desiredY = 0;

        // Get the desired X coordinate
        switch (position) {
            case RootLayoutWidget.TOP_LEFT:
            case RootLayoutWidget.LEFT_EDGE:
            case RootLayoutWidget.BOTTOM_LEFT:
                desiredX = _maxBounds.left;
                break;
            case RootLayoutWidget.TOP_EDGE:
            case RootLayoutWidget.BOTTOM_EDGE:
                desiredX = _maxBounds.centerX() - (bounds.width() / 2);
                break;
            case RootLayoutWidget.TOP_RIGHT:
            case RootLayoutWidget.RIGHT_EDGE:
            case RootLayoutWidget.BOTTOM_RIGHT:
                desiredX = _maxBounds.right - bounds.width();
                break;
        }

        // Get the desired Y coordinate
        switch (position) {
            case RootLayoutWidget.TOP_LEFT:
            case RootLayoutWidget.TOP_EDGE:
            case RootLayoutWidget.TOP_RIGHT:
                desiredY = _maxBounds.top;
                break;
            case RootLayoutWidget.LEFT_EDGE:
            case RootLayoutWidget.RIGHT_EDGE:
                desiredY = _maxBounds.centerY() - (bounds.height() / 2);
                break;
            case RootLayoutWidget.BOTTOM_LEFT:
            case RootLayoutWidget.BOTTOM_EDGE:
            case RootLayoutWidget.BOTTOM_RIGHT:
                desiredY = _maxBounds.bottom - bounds.height();
                break;
        }

        // Distance calculation method
        int distMethod;
        switch (position) {
            case RootLayoutWidget.TOP_EDGE:
            case RootLayoutWidget.BOTTOM_EDGE:
                distMethod = DIST_Y;
                break;
            case RootLayoutWidget.LEFT_EDGE:
            case RootLayoutWidget.RIGHT_EDGE:
                distMethod = DIST_X;
                break;
            default:
                distMethod = DIST_HYPOT;
                break;
        }

        // Sort by top values to slightly speed up intersect calculations
        sort(SORT_ORDER_TOP);

        return findBestPosition(bounds, new Point(desiredX, desiredY),
                distMethod);
    }

    /**
     * Find the maximum possible width for a given boundary
     * @param bounds Bounds
     * @return Bounds containing max width/position
     */
    public Rect findMaxWidth(Rect bounds) {
        List<Rect> inBounds = new ArrayList<>(_occupied.size());
        for (Rect r : _occupied) {
            if (r.top > bounds.bottom || r.bottom < bounds.top)
                continue;
            inBounds.add(r);
        }

        Rect ret = new Rect(bounds);
        Rect test = new Rect(bounds);
        for (int i = 0; i < inBounds.size(); i++) {
            Rect r1 = inBounds.get(i);
            if (r1.right < _maxBounds.right)
                findMaxWidthTest(ret, test, inBounds, r1.right,
                        _maxBounds.right);
            if (r1.left > _maxBounds.left)
                findMaxWidthTest(ret, test, inBounds, _maxBounds.left, r1.left);
            for (int j = 0; j < inBounds.size(); j++) {
                Rect r2 = inBounds.get(j);
                if (r1 == r2)
                    continue;
                if (r1.right < r2.left)
                    findMaxWidthTest(ret, test, inBounds, r1.right, r2.left);
                else if (r2.right < r1.left)
                    findMaxWidthTest(ret, test, inBounds, r2.right, r1.left);
            }
        }

        return ret;
    }

    /**
     * Find the maximum possible height for a given boundary
     * @param bounds Bounds
     * @return Bounds containing max height/position
     */
    public Rect findMaxHeight(Rect bounds) {
        List<Rect> inBounds = new ArrayList<>(_occupied.size());
        for (Rect r : _occupied) {
            if (r.right < bounds.left || r.left > bounds.right)
                continue;
            inBounds.add(r);
        }

        Rect ret = new Rect(bounds);
        Rect test = new Rect(bounds);
        for (int i = 0; i < inBounds.size(); i++) {
            Rect r1 = inBounds.get(i);
            if (r1.bottom < _maxBounds.bottom)
                findMaxHeightTest(ret, test, inBounds, r1.bottom,
                        _maxBounds.bottom);
            if (r1.top > _maxBounds.top)
                findMaxHeightTest(ret, test, inBounds, _maxBounds.top, r1.top);
            for (int j = 0; j < inBounds.size(); j++) {
                Rect r2 = inBounds.get(j);
                if (r1 == r2)
                    continue;
                if (r1.bottom < r2.top)
                    findMaxHeightTest(ret, test, inBounds, r1.bottom, r2.top);
                else if (r2.bottom < r1.top)
                    findMaxHeightTest(ret, test, inBounds, r2.bottom, r1.top);
            }
        }

        return ret;
    }

    /**
     * Check if the given bounds intersects any of the occupied boundaries
     * @param bounds Bounds to check
     * @return True if intersects
     */
    public boolean intersects(Rect bounds) {
        return intersects(bounds, _occupied);
    }

    /**
     * Get the bounds of a widget
     * @param w Widget
     * @return Bounds rectangle
     */
    public static Rect getBounds(IMapWidget w) {
        float[] size = w.getSize(true, false);
        return new Rect(
                (int) w.getPointX(),
                (int) w.getPointY(),
                (int) (w.getPointX() + size[0]),
                (int) (w.getPointY() + size[1]));
    }

    private void findMaxWidthTest(Rect rect, Rect test, List<Rect> otherBounds,
                                  int left, int right) {
        test.set(left, rect.top, right, rect.bottom);
        if (test.width() > rect.width() && !intersects(test, otherBounds))
            rect.set(test);
    }

    private void findMaxHeightTest(Rect rect, Rect test, List<Rect> otherBounds,
                                   int top, int bottom) {
        test.set(rect.left, top, rect.right, bottom);
        if (test.height() > rect.height() && !intersects(test, otherBounds))
            rect.set(test);
    }

    private boolean intersects(Rect bounds, List<Rect> otherBounds) {
        if (bounds.left < _maxBounds.left
                || bounds.right > _maxBounds.right
                || bounds.top < _maxBounds.top
                || bounds.bottom > _maxBounds.bottom)
            return true;
        for (int i = 0; i < otherBounds.size(); i++) {
            Rect r = otherBounds.get(i);
            if (breakLoop(bounds, r))
                break;
            if (Rect.intersects(bounds, r))
                return true;
        }
        return false;
    }

    private boolean breakLoop(Rect bounds, Rect other) {
        return _sortOrder != UNSORTED && other.top >= bounds.bottom;
    }

    private double getDistance(int x1, int y1, int x2, int y2, int method) {
        switch (method) {
            case DIST_X:
                return Math.abs(x1 - x2);
            case DIST_Y:
                return Math.abs(y1 - y2);
            case DIST_HYPOT:
                return Math.hypot(x1 - x2, y1 - y2);
        }
        return 0;
    }
}
