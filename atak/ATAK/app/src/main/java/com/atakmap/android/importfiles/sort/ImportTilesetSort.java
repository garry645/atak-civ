
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.android.maps.tilesets.TilesetInfo;
import com.atakmap.app.R;

import java.io.File;
import java.io.IOException;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * Sorts ATAK Tile sets
 * @deprecated use {@link ImportTilesetResolver}
 */
@Deprecated
@DeprecatedApi(since = "5.5", forRemoval = true, removeAt = "5.8")
public class ImportTilesetSort extends ImportResolver {

    private static final String TAG = "ImportTilesetSort";

    public ImportTilesetSort(Context context, boolean validateExt,
            boolean copyFile) {
        super(".zip", "layers",
                context.getString(R.string.tileset),
                context.getDrawable(R.drawable.ic_menu_maps));
    }

    @Override
    public boolean match(File file) {
        if (!super.match(file))
            return false;

        // it is a .zip, now lets see if it is a tile set
        try {
            return TilesetInfo.parse(file) != null;
        } catch (IOException | IllegalStateException e) {
            return false;
        }
    }

    @Override
    public Pair<String, String> getContentMIME() {
        return new Pair<>("Tileset", "application/zip");
    }
}
