
package com.atakmap.map.layer.feature.ogr.style;

import android.util.LruCache;

import com.atakmap.coremap.log.Log;
import com.atakmap.interop.Interop;
import com.atakmap.interop.Pointer;
import com.atakmap.map.layer.feature.style.Style;

public class FeatureStyleParser
{
    private final static Interop<Style> Style_interop = Interop.findInterop(Style.class);

    private static final String TAG = "FeatureStyleParser";

    private static int STATE_TOOL_NAME = 0;
    private static int STATE_PARAM_NAME = 1;
    private static int STATE_PARAM_VALUE = 2;
    private static int STATE_QUOTED_VALUE = 3;
    private static int STATE_TOOL_END = 4;

    private static LruCache<String, Style> styleCache = new LruCache<String, Style>(100);

    public static void parse(String style, FeatureStyle retval)
    {
        int[] pidx = new int[1];

        DrawingTool tool;

        do
        {
            tool = parseTool(style, pidx);
            if (tool == null)
                break;
            retval.pushDrawingTool(tool);
        } while (true);
    }

    public static Style parse2(String ogrStyle)
    {
        Style style = styleCache.get(ogrStyle);
        if (style != null)
            return style;

        final Pointer p = parseStyle(ogrStyle);
        if(p == null)
            return null;
        style = Style_interop.create(p);
        styleCache.put(ogrStyle, style);
        return style;
    }

    private static DrawingTool parseTool(String style, int[] pidx)
    {
        StringBuilder s = new StringBuilder();
        int idx = pidx[0];

        int state = STATE_TOOL_NAME;

        DrawingTool tool = null;

        String paramKey = null;
        String paramVal = null;

        char c;
        while (idx < style.length())
        {
            c = style.charAt(idx++);
            if (state == STATE_TOOL_NAME && c == '(')
            {
                // end tool name
                final String toolName = s.toString();
                s.delete(0, s.length());

                if (toolName.equals("PEN"))
                    tool = new Pen();
                else if (toolName.equals("BRUSH"))
                    tool = new Brush();
                else if (toolName.equals("LABEL"))
                    tool = new Label();
                else if (toolName.equals("SYMBOL"))
                    tool = new Symbol();
                else
                    ; // XXX -

                state = STATE_PARAM_NAME;
            } else if (state == STATE_PARAM_NAME && c == ':')
            {
                // end param key
                paramKey = s.toString();
                s.delete(0, s.length());

                state = STATE_PARAM_VALUE;
            } else if (state == STATE_PARAM_VALUE && (c == ',' || c == ')'))
            {
                // end param value
                paramVal = s.toString();
                s.delete(0, s.length());

                if (tool != null && paramKey != null)
                    tool.pushParam(paramKey.trim(), paramVal.trim());

                if (c == ',')
                    state = STATE_PARAM_NAME;
                else if (c == ')')
                    state = STATE_TOOL_END;
                else
                    throw new IllegalStateException();
            } else if (state == STATE_PARAM_VALUE && c == '"')
            {
                if (s.length() != 0)
                    throw new IllegalStateException();
                state = STATE_QUOTED_VALUE;
            } else if (state == STATE_QUOTED_VALUE && c == '"')
            {
                if (s.length() > 0 && s.charAt(s.length() - 1) == '\\')
                    s.delete(s.length() - 1, s.length());
                else
                    state = STATE_PARAM_VALUE;
            } else if (state == STATE_TOOL_END && c != ' ')
            {
                if (c != ';')
                    throw new IllegalStateException();
                break;
            } else
            {
                s.append(c);
            }
        }

        pidx[0] = idx;

        return tool;
    }

    public final static int parseOgrColor(String paramValue)
    {
        final int len = paramValue.length();
        if ((len != 7 && len != 9)
                || (paramValue.charAt(0) != '#') || !isHex(paramValue, 1, len - 1))
        {
            Log.w("FeatureStyleParser", "Bad color value: " + paramValue + ", default to 0xFFFFFFFF");
            return 0xFFFFFFFF;
        }

        if (len == 7)
        {
            return 0xFF000000 | Integer.parseInt(paramValue.substring(1), 16);
        } else if (len == 9)
        {
            final int alpha = Integer.parseInt(paramValue.substring(7), 16);
            return (int) (((long) alpha << 24L) | Long.parseLong(paramValue.substring(1, 7), 16));
        } else
        {
            throw new IllegalStateException();
        }
    }

    private static boolean isHex(String s, int off, int len)
    {
        char c;
        for (int i = 0; i < len; i++)
        {
            c = s.charAt(i + off);
            if (c < 48 || (c & ~0x20) > 70)
                return false;
            if (c > 57 && (c & ~0x20) < 65)
                return false;
        }
        return true;
    }

    public static String pack(Style style)
    {
        if (style == null)
            return null;
        return packStyle(Style_interop.getPointer(style));
    }

    private static int argb2rgba(int argb)
    {
        return ((argb << 8) | ((argb >> 24) & 0xFF));
    }

    static native String packStyle(long styleptr);
    static native Pointer parseStyle(String ogrstyle);
}
