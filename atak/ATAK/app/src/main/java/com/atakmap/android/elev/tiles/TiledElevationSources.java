
package com.atakmap.android.elev.tiles;

import com.atakmap.content.CatalogDatabase;
import com.atakmap.content.FileCurrency;
import com.atakmap.coremap.log.Log;
import com.atakmap.database.Databases;
import com.atakmap.map.elevation.ElevationSource;
import com.atakmap.map.elevation.ElevationSourceManager;
import com.atakmap.map.elevation.TileMatrixElevationSource;
import com.atakmap.map.formats.cdn.StreamingTiles;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.raster.tilematrix.TileClientFactory;
import com.atakmap.map.layer.raster.tilematrix.TileContainerFactory;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;
import com.atakmap.util.Collections2;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import gov.tak.api.util.Disposable;

public final class TiledElevationSources implements Disposable {

    public interface OnContentChangedListener {
        void onContentChanged(TiledElevationSources sources);
    }

    private final Map<File, ElevationSource> _sources = new HashMap<>();
    private final CatalogDatabase _db;
    private final String _cacheDir;

    private static final String TAG = "TiledElevationSources";

    private final Set<OnContentChangedListener> _listeners = Collections2
            .newIdentityHashSet();

    public TiledElevationSources(String dbfile, String cacheDir) {
        _db = new CatalogDatabase(Databases.openOrCreateDatabase(dbfile),
                FileCurrency.INSTANCE);
        _cacheDir = cacheDir;

        _db.validateCatalog();
        try (CatalogDatabase.CatalogCursor result = _db.queryCatalog()) {
            while (result.moveToNext()) {
                final File f = new File(result.getPath());
                ElevationSource src = createSource(f, cacheDir);
                if(src != null)
                    _sources.put(f, src);
                else
                    Log.w(TAG, "Failed to create source from: " + f.getAbsolutePath());
            }
        }
    }

    private ElevationSource createSource(File file, String cacheDir) {
        TileMatrix tiles = null;
        do {
            tiles = TileClientFactory.create(
                    file.getAbsolutePath(),
                    null,
                    null);
            if (tiles != null) {
                final String name = tiles.getName();
                tiles.dispose();
                tiles = TileClientFactory.create(
                        file.getAbsolutePath(),
                        new File(cacheDir, name + ".sqlite").getAbsolutePath(),
                        null);
                break;
            }
            tiles = TileContainerFactory.open(
                    file.getAbsolutePath(),
                    true,
                    null);
            if (tiles != null)
                break;

        } while (false);
        if (tiles == null)
            return null;
        String mimeType = null;
        if (tiles instanceof Controls) {
            Controls ctrls = (Controls) tiles;
            final StreamingTiles config = ctrls
                    .getControl(StreamingTiles.class);
            if (config != null) {
                if (!config.content.equals("terrain"))
                    return null;
                mimeType = config.mimeType;
            }
        }
        return new TileMatrixElevationSource(
                tiles,
                mimeType,
                false);
    }

    public synchronized ElevationSource add(File file) {
        byte[] fileAppData = FileCurrency.INSTANCE.getAppData(file);
        ElevationSource src = _sources.get(file);
        if (src != null) {
            // Source already loaded;  see if it matches the given file
            try (CatalogDatabase.CatalogCursor c = _db.queryCatalog(file)) {
                while (c.moveToNext()) {
                    byte[] current = c.getAppData();
                    if (Arrays.equals(current, fileAppData)) {
                        // already in db and equal so use existing entry
                        return src;
                    }
                }
            }

            // at this point, our source is missing from the catalog (weird?) or outdated.
            // Remove and will be replaced below
            removeImplNoSync(file);
        }

        final ElevationSource elevationSource = createSource(file, _cacheDir);
        if (elevationSource != null) {
            _sources.put(file, elevationSource);
            _db.addCatalogEntry(file, FileCurrency.INSTANCE);
            ElevationSourceManager.attach(elevationSource);
        }
        for (OnContentChangedListener l : _listeners)
            l.onContentChanged(this);
        return elevationSource;
    }

    public synchronized ElevationSource getSource(File file) {
        return _sources.get(file);
    }

    public synchronized File getFile(ElevationSource source) {
        for (Map.Entry<File, ElevationSource> entry : _sources.entrySet()) {
            if (entry.getValue() == source)
                return entry.getKey();
        }
        return null;
    }

    public synchronized void remove(File file) {
        removeImplNoSync(file);
        for (OnContentChangedListener l : _listeners)
            l.onContentChanged(this);
    }

    private void removeImplNoSync(File file) {
        _db.deleteCatalog(file);
        _sources.remove(file);
    }

    public synchronized void removeAll() {
        for (File f : _sources.keySet())
            _db.deleteCatalog(f);
        _sources.clear();
        for (OnContentChangedListener l : _listeners)
            l.onContentChanged(this);
    }

    public synchronized Set<ElevationSource> get() {
        return new HashSet<>(_sources.values());
    }

    public synchronized void addOnContentChangedListener(
            OnContentChangedListener l) {
        _listeners.add(l);
    }

    public synchronized void removeOnContentChangedListener(
            OnContentChangedListener l) {
        _listeners.remove(l);
    }

    @Override
    public void dispose() {
        _db.close();
    }
}
