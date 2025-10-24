
package com.atakmap.android.contentservices.cdn;

import android.net.Uri;

import com.atakmap.android.importexport.AbstractMarshal;
import com.atakmap.android.importexport.Marshal;
import com.atakmap.map.formats.cdn.StreamingTiles;

import java.io.IOException;
import java.io.InputStream;

import gov.tak.api.annotation.DeprecatedApi;

/** @deprecated use {@link TiledGeospatialContentMarshal} */
@Deprecated
@DeprecatedApi(since = "5.2", forRemoval = true, removeAt = "5.5")
public final class StreamingTilesMarshal extends AbstractMarshal {
    public final static String MIME_TYPE = "application/vnd.tak-streaming-tiles";
    public final static String MIME_TYPE_IMAGERY = MIME_TYPE + "+imagery";
    public final static String MIME_TYPE_TERRAIN = MIME_TYPE + "+terrain";
    public final static String MIME_TYPE_VECTOR = MIME_TYPE + "+vector";

    public final static Marshal INSTANCE_IMAGERY = new StreamingTilesMarshal(
            "imagery");
    public final static Marshal INSTANCE_TERRAIN = new StreamingTilesMarshal(
            "terrain");
    public final static Marshal INSTANCE_VECTOR = new StreamingTilesMarshal(
            "vector");

    private final String _subtype;
    private final String _mime;

    StreamingTilesMarshal(String subtype) {
        super("Streaming Tiles - " + subtype);
        _subtype = subtype;
        _mime = MIME_TYPE + "+" + _subtype;
    }

    @Override
    public String marshal(InputStream inputStream, int probeSize)
            throws IOException {
        final StreamingTiles tiles = StreamingTiles.parse(inputStream,
                Math.min(probeSize, 8192));
        if (tiles == null)
            return null;
        return tiles.content.equals(_subtype) ? _mime : null;
    }

    @Override
    public String marshal(Uri uri) throws IOException {
        return marshalUriAsStream(this, uri);
    }

    @Override
    public int getPriorityLevel() {
        return 1;
    }
}
