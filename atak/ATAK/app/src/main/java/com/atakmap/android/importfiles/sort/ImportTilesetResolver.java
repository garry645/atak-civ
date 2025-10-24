
package com.atakmap.android.importfiles.sort;

import android.content.Context;
import android.util.Pair;

import com.atakmap.android.maps.tilesets.TilesetInfo;
import com.atakmap.android.util.ResUtils;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;
import java.io.IOException;
import java.util.Set;

/**
 * Sorts ATAK Tile sets
 */
public class ImportTilesetResolver extends gov.tak.api.importfiles.ImportResolver {

    private static final String TAG = "ImportTilesetSort";

    public ImportTilesetResolver(Context context) {
        super(".zip", FileSystemUtils.getItem("layers"),
                context.getString(R.string.tileset),
                ResUtils.getDrawable(context, R.drawable.ic_menu_maps));
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
