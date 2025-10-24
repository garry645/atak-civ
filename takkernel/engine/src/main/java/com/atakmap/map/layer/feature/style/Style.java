package com.atakmap.map.layer.feature.style;

import com.atakmap.interop.InteropCleaner;
import com.atakmap.interop.NativePeerManager;
import com.atakmap.interop.Pointer;
import com.atakmap.lang.ref.Cleaner;
import gov.tak.api.util.Disposable;

import com.atakmap.map.layer.feature.geometry.Geometry;
import com.atakmap.math.MathUtils;
import com.atakmap.util.ReadWriteLock;

import java.util.EnumSet;

import gov.tak.api.annotation.DontObfuscate;

/**
 * Defines the style for a feature. Implementations will provide instruction on
 * how geometries are to be rendered.
 *
 * @author Developer
 */
@DontObfuscate
public abstract class Style implements Disposable
{
    final static NativePeerManager.Cleaner CLEANER = new InteropCleaner(Style.class);

    static StyleClasses styleClasses = null;

    final ReadWriteLock rwlock = new ReadWriteLock();
    private final Cleaner cleaner;
    Pointer pointer;
    Object owner;

    Style(Pointer pointer, Object owner)
    {
        cleaner = NativePeerManager.register(this, pointer, rwlock, null, CLEANER);
        this.pointer = pointer;
        this.owner = owner;
    }

    @Override
    public void dispose()
    {
        if (cleaner != null)
            cleaner.clean();
    }

    @Override
    public Style clone()
    {
        this.rwlock.acquireRead();
        try
        {
            return create(clone(this.pointer.raw), null);
        } finally
        {
            this.rwlock.releaseRead();
        }
    }

    @Override
    public final boolean equals(Object o)
    {
        if(!(o instanceof Style))
            return false;
        final Style other = (Style) o;
        this.rwlock.acquireRead();
        other.rwlock.acquireRead();
        try
        {
            return equals(this.pointer.raw, other.pointer.raw);
        } finally
        {
            other.rwlock.releaseRead();
            this.rwlock.releaseRead();
        }
    }

    @Override
    public final int hashCode()
    {
        return this.pointer.hashCode();
    }

    static Style create(Pointer pointer, Object owner)
    {
        if (pointer == null || pointer.raw == 0L)
            return null;

        if (styleClasses == null)
            styleClasses = new StyleClasses();

        final int styleClass = getClass(pointer.raw);
        if (styleClass == styleClasses.BasicStrokeStyle)
        {
            return new BasicStrokeStyle(pointer, owner);
        } else if (styleClass == styleClasses.BasicFillStyle)
        {
            return new BasicFillStyle(pointer, owner);
        } else if (styleClass == styleClasses.BasicPointStyle)
        {
            return new BasicPointStyle(pointer, owner);
        } else if (styleClass == styleClasses.IconPointStyle)
        {
            return new IconPointStyle(pointer, owner);
        } else if (styleClass == styleClasses.MeshPointStyle)
        {
            return new MeshPointStyle(pointer, owner);
        } else if (styleClass == styleClasses.LabelPointStyle)
        {
            return new LabelPointStyle(pointer, owner);
        } else if (styleClass == styleClasses.CompositeStyle)
        {
            return new CompositeStyle(pointer, owner);
        } else if (styleClass == styleClasses.PatternStrokeStyle)
        {
            return new PatternStrokeStyle(pointer, owner);
        } else if (styleClass == styleClasses.LevelOfDetailStyle)
        {
            return new LevelOfDetailStyle(pointer, owner);
        } else if (styleClass == styleClasses.ArrowStrokeStyle)
        {
            return new ArrowStrokeStyle(pointer, owner);
        } else
        {
            return null;
        }
    }

    static long getPointer(Style style)
    {
        if (style != null)
            return style.pointer.raw;
        else
            return 0L;
    }

    static class StyleClasses
    {
        public int BasicStrokeStyle = getTESC_BasicStrokeStyle();
        public int BasicFillStyle = getTESC_BasicFillStyle();
        public int BasicPointStyle = getTESC_BasicPointStyle();
        public int IconPointStyle = getTESC_IconPointStyle();
        public int MeshPointStyle = getTESC_MeshPointStyle();
        public int LabelPointStyle = getTESC_LabelPointStyle();
        public int CompositeStyle = getTESC_CompositeStyle();
        public int PatternStrokeStyle = getTESC_PatternStrokeStyle();
        public int LevelOfDetailStyle = getTESC_LevelOfDetailStyle();
        public int ArrowStrokeStyle = getTESC_ArrowStrokeStyle();
    }

    static native int getTESC_BasicStrokeStyle();

    static native int getTESC_BasicFillStyle();

    static native int getTESC_BasicPointStyle();

    static native int getTESC_IconPointStyle();

    static native int getTESC_MeshPointStyle();

    static native int getTESC_LabelPointStyle();

    static native int getTESC_CompositeStyle();

    static native int getTESC_PatternStrokeStyle();

    static native int getTESC_LevelOfDetailStyle();

    static native int getTESC_ArrowStrokeStyle();

    static native void destruct(Pointer pointer);

    static native Pointer clone(long ptr);

    static native int getClass(long ptr);

    static native boolean equals(long aPtr, long bPtr);

    static native Pointer BasicFillStyle_create(int color);

    static native int BasicFillStyle_getColor(long ptr);

    static native Pointer BasicStrokeStyle_create(int color, float strokeWidth, int extrudeMode);

    static native int BasicStrokeStyle_getColor(long ptr);

    static native float BasicStrokeStyle_getStrokeWidth(long ptr);

    static native int BasicStrokeStyle_getExtrudeMode(long ptr);

    static native Pointer BasicPointStyle_create(int color, float size);

    static native int BasicPointStyle_getColor(long ptr);

    static native float BasicPointStyle_getSize(long ptr);

    static native Pointer IconPointStyle_create(int color, String uri, float width, float height, float offsetX, float offsetY, int halign, int valign, float rotation, boolean isRotationAbsolute);

    static native Pointer IconPointStyle_create(int color, String uri, float scale, int halign, int valign, float rotation, boolean isRotationAbsolute);

    static native int IconPointStyle_getColor(long ptr);

    static native String IconPointStyle_getUri(long ptr);

    static native float IconPointStyle_getWidth(long ptr);

    static native float IconPointStyle_getHeight(long ptr);

    static native int IconPointStyle_getHorizontalAlignment(long ptr);

    static native int IconPointStyle_getVerticalAlignment(long ptr);

    static native float IconPointStyle_getScaling(long ptr);

    static native float IconPointStyle_getRotation(long ptr);

    static native boolean IconPointStyle_isRotationAbsolute(long ptr);

    static native float IconPointStyle_getIconOffsetX(long ptr);

    static native float IconPointStyle_getIconOffsetY(long ptr);

    static native int getIconPointStyle_HorizontalAlignment_LEFT();

    static native int getIconPointStyle_HorizontalAlignment_H_CENTER();

    static native int getIconPointStyle_HorizontalAlignment_RIGHT();

    static native int getIconPointStyle_VerticalAlignment_ABOVE();

    static native int getIconPointStyle_VerticalAlignment_V_CENTER();

    static native int getIconPointStyle_VerticalAlignment_BELOW();

    static native Pointer MeshPointStyle_create(String uri, int color, float[] transform);

    static native String MeshPointStyle_getUri(long ptr);

    static native int MeshPointStyle_getColor(long ptr);

    static native float[] MeshPointStyle_getTransform(long ptr);

    static native Pointer LabelPointStyle_create(String text, int textColor, int bgColor, int scrollMode, String fontFace, float textSize, int fontStyle, float offsetX, float offsetY, int halign, int valign, float rotation, boolean isRotationAbsolute, double labelMinRenderResolution, float scale);

    static native String LabelPointStyle_getText(long ptr);

    static native int LabelPointStyle_getTextColor(long ptr);

    static native int LabelPointStyle_getBackgroundColor(long ptr);

    static native int LabelPointStyle_getScrollMode(long ptr);

    static native float LabelPointStyle_getTextSize(long ptr);

    static native int LabelPointStyle_getHorizontalAlignment(long ptr);

    static native int LabelPointStyle_getVerticalAlignment(long ptr);

    static native float LabelPointStyle_getRotation(long ptr);

    static native boolean LabelPointStyle_isRotationAbsolute(long ptr);

    static native double LabelPointStyle_getLabelMinRenderResolution(long ptr);

    static native float LabelPointStyle_getLabelScale(long ptr);

    static native float LabelPointStyle_getOffsetX(long ptr);

    static native float LabelPointStyle_getOffsetY(long ptr);

    static native String LabelPointStyle_getFontFace(long ptr);

    static native int LabelPointStyle_getStyle(long ptr);

    static native int getLabelPointStyle_HorizontalAlignment_LEFT();

    static native int getLabelPointStyle_HorizontalAlignment_H_CENTER();

    static native int getLabelPointStyle_HorizontalAlignment_RIGHT();

    static native int getLabelPointStyle_VerticalAlignment_ABOVE();

    static native int getLabelPointStyle_VerticalAlignment_V_CENTER();

    static native int getLabelPointStyle_VerticalAlignment_BELOW();

    static native int getLabelPointStyle_ScrollMode_DEFAULT();

    static native int getLabelPointStyle_ScrollMode_ON();

    static native int getLabelPointStyle_ScrollMode_OFF();

    static LabelPointStyle.ScrollMode getScrollMode(int cmode)
    {
        if (cmode == getLabelPointStyle_ScrollMode_DEFAULT())
            return LabelPointStyle.ScrollMode.DEFAULT;
        else if (cmode == getLabelPointStyle_ScrollMode_OFF())
            return LabelPointStyle.ScrollMode.OFF;
        else if (cmode == getLabelPointStyle_ScrollMode_ON())
            return LabelPointStyle.ScrollMode.ON;
        else
            throw new IllegalArgumentException();
    }

    static int getScrollMode(LabelPointStyle.ScrollMode mmode)
    {
        switch (mmode)
        {
            case OFF:
                return getLabelPointStyle_ScrollMode_OFF();
            case ON:
                return getLabelPointStyle_ScrollMode_ON();
            case DEFAULT:
                return getLabelPointStyle_ScrollMode_DEFAULT();
            default:
                throw new IllegalArgumentException();
        }
    }

    static native int getLabelPointStyle_Style_BOLD();

    static native int getLabelPointStyle_Style_ITALIC();

    static native int getLabelPointStyle_Style_UNDERLINE();

    static native int getLabelPointStyle_Style_STRIKETHROUGH();

    static EnumSet<LabelPointStyle.Style> getFontStyle(int cstyle)
    {
        EnumSet<LabelPointStyle.Style> style = EnumSet.noneOf(LabelPointStyle.Style.class);
        if (MathUtils.hasBits(cstyle, getLabelPointStyle_Style_BOLD())) style.add(LabelPointStyle.Style.BOLD);
        if (MathUtils.hasBits(cstyle, getLabelPointStyle_Style_ITALIC())) style.add(LabelPointStyle.Style.ITALIC);
        if (MathUtils.hasBits(cstyle, getLabelPointStyle_Style_UNDERLINE())) style.add(LabelPointStyle.Style.UNDERLINE);
        if (MathUtils.hasBits(cstyle, getLabelPointStyle_Style_STRIKETHROUGH())) style.add(LabelPointStyle.Style.STRIKETHROUGH);
        return style;
    }

    static int getFontStyle(EnumSet<LabelPointStyle.Style> mstyle)
    {
        int style = 0;
        if (mstyle.contains(LabelPointStyle.Style.BOLD)) style |= getLabelPointStyle_Style_BOLD();
        if (mstyle.contains(LabelPointStyle.Style.ITALIC)) style |= getLabelPointStyle_Style_ITALIC();
        if (mstyle.contains(LabelPointStyle.Style.UNDERLINE)) style |= getLabelPointStyle_Style_UNDERLINE();
        if (mstyle.contains(LabelPointStyle.Style.STRIKETHROUGH)) style |= getLabelPointStyle_Style_STRIKETHROUGH();
        return style;
    }

    static native Pointer CompositeStyle_create(long[] stylePtrs);

    static native int CompositeStyle_getNumStyles(long ptr);

    static native Pointer CompositeStyle_getStyle(long ptr, int idx);

    static native Pointer PatternStrokeStyle_create(int factor, short pattern, int color, float width, int extrudeMode);

    static native short PatternStrokeStyle_getPattern(long pointer);

    static native int PatternStrokeStyle_getFactor(long pointer);

    static native int PatternStrokeStyle_getColor(long pointer);

    static native float PatternStrokeStyle_getStrokeWidth(long pointer);

    static native int PatternStrokeStyle_getExtrudeMode(long pointer);

    static native Pointer LevelOfDetailStyle_create(long stylePtr, int minLod, int maxLod);

    static native Pointer LevelOfDetailStyle_getStyle(long pointer);

    static native int LevelOfDetailStyle_getMinLod(long pointer);

    static native int LevelOfDetailStyle_getMaxLod(long pointer);

    static native Pointer ArrowStrokeStyle_create(float arrowRadius, int factor, short pattern, int color, float width, int extrudeMode, int arrowHeadMode);

    static native float ArrowStrokeStyle_getArrowRadius(long pointer);

    static native short ArrowStrokeStyle_getPattern(long pointer);

    static native int ArrowStrokeStyle_getFactor(long pointer);

    static native int ArrowStrokeStyle_getColor(long pointer);

    static native float ArrowStrokeStyle_getStrokeWidth(long pointer);

    static native int ArrowStrokeStyle_getExtrudeMode(long pointer);

    static native int ArrowStrokeStyle_getArrowHeadMode(long pointer);

    static native int getArrowStrokeStyle_ArrowHeadMode_ONLYLAST();
    static native int getArrowStrokeStyle_ArrowHeadMode_PERVERTEX();

    static ArrowStrokeStyle.ArrowHeadMode getArrowHeadMode(int cmode)
    {
        if (cmode == getArrowStrokeStyle_ArrowHeadMode_ONLYLAST())
            return ArrowStrokeStyle.ArrowHeadMode.OnlyLast;
        else if (cmode == getArrowStrokeStyle_ArrowHeadMode_PERVERTEX())
            return ArrowStrokeStyle.ArrowHeadMode.PerVertex;
        else
            throw new IllegalArgumentException();
    }

    static int getArrowHeadMode(ArrowStrokeStyle.ArrowHeadMode mmode)
    {
        switch (mmode)
        {
            case OnlyLast:
                return getArrowStrokeStyle_ArrowHeadMode_ONLYLAST();
            case PerVertex:
                return getArrowStrokeStyle_ArrowHeadMode_PERVERTEX();
            default:
                throw new IllegalArgumentException();
        }
    }
}
