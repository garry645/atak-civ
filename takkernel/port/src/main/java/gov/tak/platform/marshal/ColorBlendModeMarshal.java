package gov.tak.platform.marshal;

import android.graphics.ColorFilter;
import android.graphics.PorterDuff;

import gov.tak.api.commons.graphics.ColorBlendMode;
import gov.tak.api.marshal.IMarshal;

final class ColorBlendModeMarshal {
    /** Marshals from portable to platform */
    static final class Platform implements IMarshal  {
        @Override
        public <T, V> T marshal(V in) {
            switch((ColorBlendMode) in) {
                case Add:       return (T) PorterDuff.Mode.ADD;
                case Clear:     return (T) PorterDuff.Mode.CLEAR;
                case Darken:    return (T) PorterDuff.Mode.DARKEN;
                case Dst:       return (T) PorterDuff.Mode.DST;
                case DstAtop:   return (T) PorterDuff.Mode.DST_ATOP;
                case DstIn:     return (T) PorterDuff.Mode.DST_IN;
                case DstOut:    return (T) PorterDuff.Mode.DST_OUT;
                case DstOver:   return (T) PorterDuff.Mode.DST_OVER;
                case Lighten:   return (T) PorterDuff.Mode.LIGHTEN;
                case Multiply:  return (T) PorterDuff.Mode.MULTIPLY;
                case Overlay:   return (T) PorterDuff.Mode.OVERLAY;
                case Screen:    return (T) PorterDuff.Mode.SCREEN;
                case Src:       return (T) PorterDuff.Mode.SRC;
                case SrcAtop:   return (T) PorterDuff.Mode.SRC_ATOP;
                case SrcIn:     return (T) PorterDuff.Mode.SRC_IN;
                case SrcOut:    return (T) PorterDuff.Mode.SRC_OUT;
                case SrcOver:   return (T) PorterDuff.Mode.SRC_OVER;
                case Xor:       return (T) PorterDuff.Mode.XOR;
                default:        return null;
            }
        }
    }

    /** Marshals from portable to platform */
    static final class Portable implements IMarshal  {

        @Override
        public <T, V> T marshal(V in) {
            switch((PorterDuff.Mode) in) {
                case ADD:       return (T) ColorBlendMode.Add;
                case CLEAR:     return (T) ColorBlendMode.Clear;
                case DARKEN:    return (T) ColorBlendMode.Darken;
                case DST:       return (T) ColorBlendMode.Dst;
                case DST_ATOP:  return (T) ColorBlendMode.DstAtop;
                case DST_IN:    return (T) ColorBlendMode.DstIn;
                case DST_OUT:   return (T) ColorBlendMode.DstOut;
                case DST_OVER:  return (T) ColorBlendMode.DstOver;
                case LIGHTEN:   return (T) ColorBlendMode.Lighten;
                case MULTIPLY:  return (T) ColorBlendMode.Multiply;
                case OVERLAY:   return (T) ColorBlendMode.Overlay;
                case SCREEN:    return (T) ColorBlendMode.Screen;
                case SRC:       return (T) ColorBlendMode.Src;
                case SRC_ATOP:  return (T) ColorBlendMode.SrcAtop;
                case SRC_IN:    return (T) ColorBlendMode.SrcIn;
                case SRC_OUT:   return (T) ColorBlendMode.SrcOut;
                case SRC_OVER:  return (T) ColorBlendMode.SrcOver;
                case XOR:       return (T) ColorBlendMode.Xor;
                default:        return null;
            }
        }
    }
}
