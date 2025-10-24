package com.atakmap.map.layer.feature.style;

import com.atakmap.interop.Pointer;

public class ArrowStrokeStyle extends Style
{
    public enum ArrowHeadMode
    {
        OnlyLast,
        PerVertex,
    }

    public ArrowStrokeStyle(float arrowRadius, int factor, short pattern, int color, float strokeWidth, int extrudeMode)
    {
        this(arrowRadius, factor, pattern, color, strokeWidth, extrudeMode, ArrowHeadMode.OnlyLast);
    }

    public ArrowStrokeStyle(float arrowRadius, int factor, short pattern, int color, float strokeWidth, int extrudeMode, ArrowHeadMode arrowHeadMode)
    {
        this(ArrowStrokeStyle_create(arrowRadius, factor, pattern, color, strokeWidth, extrudeMode, getArrowHeadMode(arrowHeadMode)), null);
    }

    ArrowStrokeStyle(Pointer pointer, Object owner)
    {
        super(pointer, owner);
    }

    public float getArrowRadius()
    {
        this.rwlock.acquireRead();
        try {
            return ArrowStrokeStyle_getArrowRadius(this.pointer.raw);
        } finally {
            this.rwlock.releaseRead();
        }
    }

    /**
     * Returns the pattern. The low-order 16-bits compose the pattern; other
     * bits are ignored.
     *
     * @return
     */
    public long getPattern()
    {
        this.rwlock.acquireRead();
        try
        {
            return ArrowStrokeStyle_getPattern(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    public int getFactor()
    {
        this.rwlock.acquireRead();
        try
        {
            return ArrowStrokeStyle_getFactor(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    /**
     * Returns the stroke color.
     *
     * @return The stroke color.
     */
    public int getColor()
    {
        this.rwlock.acquireRead();
        try
        {
            return ArrowStrokeStyle_getColor(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    /**
     * Returns the stroke width, in pixels.
     *
     * @return The stroke width, in pixels.
     */
    public float getStrokeWidth()
    {
        this.rwlock.acquireRead();
        try
        {
            return ArrowStrokeStyle_getStrokeWidth(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    public int getExtrudeMode()
    {
        this.rwlock.acquireRead();
        try
        {
            return ArrowStrokeStyle_getExtrudeMode(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    /**
     * Returns the arrow head mode.
     *
     * @return The arrow head mode
     */
    public ArrowHeadMode getArrowHeadMode()
    {
        this.rwlock.acquireRead();
        try
        {
            return getArrowHeadMode(ArrowStrokeStyle_getArrowHeadMode(this.pointer.raw));
        } finally
        {
            this.rwlock.releaseRead();
        }
    }
}
