package com.atakmap.map.layer.feature.style;

import com.atakmap.interop.Pointer;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

/**
 * Level of detail style. Specifies a level of display thresholds for features.
 */
public final class LevelOfDetailStyle extends Style
{
    public static final int MIN_LOD = 0;
    public static final int MAX_LOD = 33;

    private WeakReference<Style> cachedStyle;

    /**
     * Creates a new instance of the specified minimum and maximum level of detail.
     *
     * @param style The underlying style
     * @param minLod The minimum level of detail (inclusive)
     * @param maxLod The maximum level of detail (exclusive)
     */
    public LevelOfDetailStyle(Style style, int minLod, int maxLod)
    {
        this(LevelOfDetailStyle_create(style.pointer.raw, minLod, maxLod), null);
    }

    LevelOfDetailStyle(Pointer pointer, Object owner)
    {
        super(pointer, owner);
    }

    /**
     * Returns the underlying style.
     *
     * @return The underlying style
     */
    public Style getStyle()
    {
        this.rwlock.acquireRead();
        try
        {
            synchronized (this)
            {
                if (cachedStyle != null)
                {
                    final Style retval = cachedStyle.get();
                    if (retval != null)
                        return retval;
                }
                final Pointer pointer = LevelOfDetailStyle_getStyle(this.pointer.raw);
                final Style retval = create(pointer, this);
                if (retval != null)
                {
                    cachedStyle = new WeakReference<>(retval);
                }
                return retval;
            }
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    /**
     * Returns the minimum level of detail.
     *
     * @return The minimum level of detail (inclusive)
     */
    public int getMinLevelOfDetail()
    {
        this.rwlock.acquireRead();
        try
        {
            return LevelOfDetailStyle_getMinLod(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    /**
     * Returns the maximum level of detail.
     *
     * @return The maximum level of detail (exclusive)
     */
    public int getMaxLevelOfDetail()
    {
        this.rwlock.acquireRead();
        try
        {
            return LevelOfDetailStyle_getMaxLod(this.pointer.raw);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }
}
