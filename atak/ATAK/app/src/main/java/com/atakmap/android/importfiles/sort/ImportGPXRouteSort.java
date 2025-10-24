
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;

import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.routes.RouteMapReceiver;
import com.atakmap.app.R;

import java.io.File;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.importfiles.ImportGPXRouteResolver;

/**
 * Imports GPX Route Files
 *
 * @deprecated use {@link ImportGPXRouteResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportGPXRouteSort extends ImportGPXSort {

    private static final String TAG = "ImportGPXRouteSort";

    private final Context _context;

    public ImportGPXRouteSort(Context context, boolean validateExt,
            boolean copyFile, boolean importInPlace) {
        super(validateExt, copyFile, importInPlace, context
                .getString(R.string.gpx_route_file));
        _context = context;
    }

    @Override
    public Drawable getIcon() {
        return _context.getDrawable(R.drawable.ic_route);
    }

    @Override
    protected void onFileSorted(File src, File dst, Set<SortFlags> flags) {
        Intent i = new Intent(RouteMapReceiver.ROUTE_IMPORT);
        i.putExtra("filename", src.toString());
        AtakBroadcast.getInstance().sendBroadcast(i);
    }
}
