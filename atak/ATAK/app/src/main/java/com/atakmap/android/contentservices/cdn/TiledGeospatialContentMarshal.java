
package com.atakmap.android.contentservices.cdn;

import android.net.Uri;

import com.atakmap.android.importexport.AbstractMarshal;
import com.atakmap.android.importexport.Marshal;
import com.atakmap.lang.Objects;
import com.atakmap.map.formats.cdn.StreamingTiles;
import com.atakmap.map.layer.control.Controls;
import com.atakmap.map.layer.raster.controls.TilesMetadataControl;
import com.atakmap.map.layer.raster.tilematrix.TileClientFactory;
import com.atakmap.map.layer.raster.tilematrix.TileContainerFactory;
import com.atakmap.map.layer.raster.tilematrix.TileMatrix;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public final class TiledGeospatialContentMarshal extends AbstractMarshal {
    public final static Set<String> MIME_TYPES;
    static {
        MIME_TYPES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
                StreamingTilesMarshal.MIME_TYPE,
                MIME_APPLICATION_OCTET_STREAM)));
    }

    public final static String DEFAULT_MIME_TYPE_IMAGERY = MIME_APPLICATION_OCTET_STREAM
            + "+imagery";
    public final static Set<String> MIME_TYPES_IMAGERY;
    public final static String DEFAULT_MIME_TYPE_VECTOR = MIME_APPLICATION_OCTET_STREAM
            + "+vector";
    public final static Set<String> MIME_TYPES_VECTOR;
    public final static String DEFAULT_MIME_TYPE_TERRAIN = MIME_APPLICATION_OCTET_STREAM
            + "+terrain";
    public final static Set<String> MIME_TYPES_TERRAIN;
    static {
        Set<String> imageryMimeTypes = new HashSet<>();
        Set<String> vectorMimeTypes = new HashSet<>();
        Set<String> terrainMimeTypes = new HashSet<>();
        for (String baseMime : MIME_TYPES) {
            imageryMimeTypes.add(baseMime + "+imagery");
            vectorMimeTypes.add(baseMime + "+vector");
            terrainMimeTypes.add(baseMime + "+terrain");
        }
        MIME_TYPES_IMAGERY = Collections.unmodifiableSet(imageryMimeTypes);
        MIME_TYPES_VECTOR = Collections.unmodifiableSet(vectorMimeTypes);
        MIME_TYPES_TERRAIN = Collections.unmodifiableSet(terrainMimeTypes);
    }

    public final static Marshal INSTANCE_IMAGERY = new TiledGeospatialContentMarshal(
            "imagery");
    public final static Marshal INSTANCE_TERRAIN = new TiledGeospatialContentMarshal(
            "terrain");
    public final static Marshal INSTANCE_VECTOR = new TiledGeospatialContentMarshal(
            "vector");

    private final String _subtype;

    TiledGeospatialContentMarshal(String subtype) {
        super("Tiled Geospatial Content - " + subtype);
        _subtype = subtype;
    }

    @Override
    public String marshal(InputStream inputStream, int probeSize)
            throws IOException {
        do {
            final StreamingTiles tiles = StreamingTiles.parse(inputStream,
                    Math.min(probeSize, 8192));
            if (tiles == null)
                break;
            if (!tiles.content.equals(_subtype))
                break;
            return MIME_APPLICATION_OCTET_STREAM + "+" + _subtype;
        } while (false);

        return null;
    }

    @Override
    public String marshal(Uri uri) throws IOException {
        String mime = marshalUriAsStream(this, uri, 16 * 1024);
        if (mime != null)
            return mime;

        final String path = uri.getPath();
        TileMatrix tiles = null;
        try {
            do {
                tiles = TileClientFactory.create(
                        path,
                        null,
                        null);
                if (tiles != null)
                    break;
                tiles = TileContainerFactory.open(
                        path,
                        true,
                        null);
                if (tiles != null)
                    break;
            } while (false);
            if (tiles == null)
                return null;
            if (!(tiles instanceof Controls))
                return null;
            final Controls ctrls = (Controls) tiles;
            final TilesMetadataControl metadata = ctrls
                    .getControl(TilesMetadataControl.class);
            if (metadata == null)
                return null;
            Map<String, Object> m = metadata.getMetadata();
            if (m == null)
                return null;
            final Object content = m.get("content");
            if (!Objects.equals(content, _subtype))
                return null;
            return MIME_APPLICATION_OCTET_STREAM + "+" + _subtype;
        } finally {
            if (tiles != null)
                tiles.dispose();
        }
    }

    @Override
    public int getPriorityLevel() {
        return 1;
    }
}
