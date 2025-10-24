package com.atakmap.map.layer.feature.style;

import com.atakmap.interop.Pointer;

import java.lang.annotation.Native;

/**
 * Basic style for linestring geometry. Defines stroke width and color.
 *
 * @author Developer
 */
public final class BasicStrokeStyle extends Style
{
    @Native
    public static final int EXTRUDE_NONE = 0;
    @Native
    public static final int EXTRUDE_VERTEX = 1;
    @Native
    public static final int EXTRUDE_ENDPOINT = 2;

    /**
     * Creates a new instance.
     *
     * @param color       The stroke color
     * @param strokeWidth The stroke width
     */
    public BasicStrokeStyle(int color, float strokeWidth)
    {
        this(color, strokeWidth, EXTRUDE_VERTEX);
    }

    public BasicStrokeStyle(int color, float strokeWidth, int extrudeMode)
    {
        this(BasicStrokeStyle_create(color, strokeWidth, extrudeMode), null);
    }

    BasicStrokeStyle(Pointer pointer, Object owner)
    {
        super(pointer, owner);
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
            return BasicStrokeStyle_getColor(this.pointer.raw);
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
            return BasicStrokeStyle_getStrokeWidth(this.pointer.raw);
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
            return BasicStrokeStyle_getExtrudeMode(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }
} // BasicStrokeStyle
