
package com.atakmap.android.icons;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;

import androidx.annotation.NonNull;

import com.atakmap.app.R;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.util.MultiCollection;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import gov.tak.api.symbology.Affiliation;
import gov.tak.api.symbology.Amplifier;
import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.ISymbologyProvider;
import gov.tak.api.symbology.Status;
import gov.tak.api.util.AttributeSet;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.symbology.SymbologyProvider;

public class CotDescriptions {
    public static final String TAG = "CotDescriptions";

    private static volatile boolean loaded = false;

    private static final HashMap<String, Node> descriptions =
            new HashMap<>();


    private CotDescriptions() {}


    private static String lookup(Context context, Node key) {
        Class<?> c = com.atakmap.app.R.string.class;

        try {
            Field f = c.getField(key.resourceName);
            int i = f.getInt(null);
            return context.getString(i);
        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Return a human readable description of the cot type associated with the icon.
     * @param ctx the context to use
     * @param sidc the symbol id code (uppercase), but will also accept the legacy iconfile format
     *             lower case with underscores and the .jpg extension.
     * @return the human readable String
     */
    public static String getDescription(Context ctx, String sidc) {
        synchronized (CotDescriptions.class) {
            if (!loaded) {
                load();
                loaded = true;
            }
        }

        if (!Character.isDigit(sidc.charAt(0))) {
            sidc = sidc.replaceAll(".png", "")
                    .replaceAll("_", "-")
                    .toUpperCase(Locale.US);
        }
        sidc = normalize(sidc);

        String tmp = ctx.getString(com.atakmap.app.R.string.not_recognized);
        final Node n = descriptions.get(sidc);
        if (n != null && n.entry != null) {
            tmp = lookup(ctx, n);
            if (tmp == null)
                tmp = n.entry.getName();
        }
        return tmp;
    }

    /**
     * Returns the human readable path that describes the icon file.
     * @param ctx the context
     * @param sidc the symbol id code (uppercase), but will also accept the legacy iconfile format
     *             lower case with underscores and the .jpg extension.
     * @param level, the number of parents to go back, if -1 go back until the end.
     * @return an list of values used to describe the current sub type.
     */
    public static List<String> getDescriptionPath(Context ctx,
            String sidc, int level) {

        synchronized (CotDescriptions.class) {
            if (!loaded) {
                load();
                loaded = true;
            }
        }

        if (level < 0)
            level = 256;


        final List<String> retval = new ArrayList<>();

        final String UNKNOWN_TYPE = ctx.getString(R.string.not_recognized);

        if (!Character.isDigit(sidc.charAt(0))) {
            // address legacy usage
            sidc = sidc.replaceAll(".png", "")
                    .replaceAll("_", "-")
                    .toUpperCase(Locale.US);
        }
        sidc = normalize(sidc);

        Node n = descriptions.get(sidc);
        if (n == null)
            retval.add(UNKNOWN_TYPE);
        else {
            while (level >= 0 && n != null) {
                String tmp = lookup(ctx, n);
                if (tmp == null)
                    tmp = n.entry.getName();
                retval.add(0, tmp);
                n = n.parent;
                level--;
            }
        }
        return retval;
    }

    /**
     * Returns the human readable name for the provided type
     * @param context the context
     * @param type the 2525 type to get the human readable name
     * @return the human readable name
     */
    public static String getHumanName(Context context, String type) {

        List<String> path = CotDescriptions.getDescriptionPath(context,
                type, 1);

        if (path.size() > 1) {
            return path.get(0) + " > " + path.get(1);
        } else
            return path.get(0);
    }

    private static void load() {
        for (ISymbologyProvider sp: SymbologyProvider.getProviders()) {
            ISymbolTable.Folder folder = sp.getSymbolTable().getRoot();
            Node node = new Node(folder, null);
            load(folder, node);
        }
    }

    private static void load(@NonNull ISymbolTable.Folder folder, @NonNull Node parentNode) {

        MultiCollection<ISymbolTable.Entry> mc = new MultiCollection<>(Arrays.asList(folder.getChildren(), folder.getSymbols()));
        for (ISymbolTable.Entry entry: mc) {
            Node childNode = new Node(entry, parentNode);
            if (entry instanceof ISymbolTable.Symbol) {
                descriptions.put(normalize(((ISymbolTable.Symbol) entry).getCode()),
                        childNode);
            }

            if (entry instanceof ISymbolTable.Folder) {
                load((ISymbolTable.Folder) entry, childNode);
            }
        }
    }



    /**
     * Returns the human identifiable symbol for the provided CoT message.
     * @param context the context
     * @param sidc the symbol id code (uppercase), but will also accept the legacy iconfile format
     *             lower case with underscores and the .jpg extension.
     * @param selectedAffil the selected affiliation
     * @return an list of values used to describe the current sub type.
     */
    public static BitmapDrawable getIcon(Context context, String sidc, Affiliation selectedAffil) {

        String code = sidc;
        int index = code.indexOf(".png");
        if(index != -1)  {
            code = code.substring(0, index);
        }

        if(selectedAffil != null)
            code = SymbologyProvider.setAffiliation(code, selectedAffil);

        if(code != null)
            sidc = code + ".png";

        return loadIcon(context, sidc);
    }

    private static BitmapDrawable loadIcon(Context context, @NonNull String fn) {
        final String milsym = fn.replace(".png", "").toUpperCase(LocaleUtil.US);
        try {
            gov.tak.api.commons.graphics.Bitmap bmp = SymbologyProvider
                    .renderSinglePointIcon(milsym,
                            new AttributeSet(), null);
            Bitmap abmp = MarshalManager.marshal(bmp,
                    gov.tak.api.commons.graphics.Bitmap.class, Bitmap.class);
            return new BitmapDrawable(context.getResources(), abmp);
        } catch (Exception e) {
            Log.e(TAG,
                    "error occurred obtaining " + milsym + " from the provider",
                    e);
            return null;
        }
    }



    private static class Node {
        final ISymbolTable.Entry entry;
        final Node parent;
        final String resourceName;

        Node(ISymbolTable.Entry entry, Node parent) {
            this.entry = entry;
            this.parent = parent;

            if (entry instanceof ISymbolTable.Symbol) {
                String code = ((ISymbolTable.Symbol) entry).getCode();
                String resourceName = code.toLowerCase(Locale.US);

                // legacy naming for client translation - only set up for 2525c
                resourceName = resourceName
                        .replaceAll("\\*", "-")
                        .replaceAll("-", "_");

                if (resourceName.charAt(0) == 's')
                    resourceName = resourceName.substring(0, 3) + "p" + resourceName.substring(4);
                this.resourceName = resourceName;
            } else
                this.resourceName = null;
        }
    }

    /**
     * Normalizes the basic symbol for lookup / retrieval
     * @param code the 2525 symbol
     * @return a normalized version of that symbol used during lookups
     */
    public static String normalize(String code) {
        if (code == null)
            return null;

        if (code.length() < 5)
            return code;

        code = code.toUpperCase(Locale.US);
        code = code.replaceAll("\\*", "-");
        if (!Character.isDigit(code.charAt(0))) {
            code = code.substring(0, 3) + "-" + code.substring(4);
            // TODO: Cannot remove space 2 using the methods when not weather
            if (code.charAt(0) != 'W')
                code = code.charAt(0) + "-" + code.substring(2);
        } else {
            // 2525d try to normalize?
            code = SymbologyProvider.setStatus(code, Status.Present);
            code = SymbologyProvider.setAmplifier(code, Amplifier.Army);
            code = SymbologyProvider.setHeadquartersTaskForceDummyMask(code, 0);
            code = SymbologyProvider.setAffiliation(code, Affiliation.Pending);
        }
        return code;
    }

}
