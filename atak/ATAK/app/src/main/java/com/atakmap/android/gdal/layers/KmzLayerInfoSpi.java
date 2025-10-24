
package com.atakmap.android.gdal.layers;

import com.atakmap.io.ZipVirtualFile;
import com.atakmap.map.layer.raster.AbstractDatasetDescriptorSpi;
import com.atakmap.map.layer.raster.DatasetDescriptor;
import com.atakmap.map.layer.raster.DatasetDescriptorSpi;
import com.atakmap.map.layer.raster.DatasetDescriptorSpiArgs;
import com.atakmap.spi.InteractiveServiceProvider;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.FileFilter;
import java.io.InputStream;
import java.util.Set;

import gov.tak.api.annotation.DeprecatedApi;

/**
 * @deprecated use {@link com.atakmap.map.formats.kmz.KmzLayerInfoSpi}
 */
@Deprecated
@DeprecatedApi(since = "5.6", forRemoval = true, removeAt = "5.9")
public class KmzLayerInfoSpi extends AbstractDatasetDescriptorSpi {

    public static final String GROUND_OVERLAY = com.atakmap.map.formats.kmz.KmzLayerInfoSpi.GROUND_OVERLAY;

    public static final String TAG = com.atakmap.map.formats.kmz.KmzLayerInfoSpi.TAG;

    public static final FileFilter KML_FILTER = com.atakmap.map.formats.kmz.KmzLayerInfoSpi.KML_FILTER;

    public final static DatasetDescriptorSpi INSTANCE = new KmzLayerInfoSpi();

    private final static com.atakmap.map.formats.kmz.KmzLayerInfoSpi _impl = (com.atakmap.map.formats.kmz.KmzLayerInfoSpi)com.atakmap.map.formats.kmz.KmzLayerInfoSpi.INSTANCE;

    private KmzLayerInfoSpi() {
        super("kmz", 3);
    }

    @Override
    protected Set<DatasetDescriptor> create(File file, File workingDir,
            InteractiveServiceProvider.Callback callback) {
        return _impl.create(new DatasetDescriptorSpiArgs(file, workingDir), callback);
    }

    @Override
    protected boolean probe(File file,
            final InteractiveServiceProvider.Callback callback) {

        final boolean[] matches = {false};
        _impl.create(new DatasetDescriptorSpiArgs(file, null), new InteractiveServiceProvider.Callback() {
            public boolean isCanceled() { return callback.isCanceled(); }
            public boolean isProbeOnly() { return callback.isProbeOnly(); }
            public int getProbeLimit() { return callback.getProbeLimit(); }
            public void setProbeMatch(boolean match) { matches[0] = match; }
            public void errorOccurred(String msg, Throwable t) { callback.errorOccurred(msg, t); }
            public void progress(int progress) { callback.progress(progress); }
        });
        return matches[0];
    }

    public static boolean containsTag(File kmzFile, String tagName) {
        return com.atakmap.map.formats.kmz.KmzLayerInfoSpi.containsTag(kmzFile, tagName);
    }

    public static boolean containsTag(InputStream kmlStream, String tagName) {
        return com.atakmap.map.formats.kmz.KmzLayerInfoSpi.containsTag(kmlStream, tagName);
    }

    @Override
    public int parseVersion() {
        return _impl.parseVersion();
    }

    /**
     * Determines if the parser name matches the tag name provided
     * @param parser the parser
     * @param tagName the tag name to match against
     * @throws XmlPullParserException occurs if there is an exception with the parsing
     * @throws IllegalStateException if the tag encountered is not the correct tag
     */
    public static void checkAtTag(XmlPullParser parser, String tagName)
            throws XmlPullParserException {
        com.atakmap.map.formats.kmz.KmzLayerInfoSpi.checkAtTag(parser, tagName);
    }

    /**
     * Given a KMZ file, find the main document KML (doc.kml)
     * @param kmzFile KMZ file
     * @return ZIP file pointer to the document KML or null if not found
     */
    public static ZipVirtualFile findDocumentKML(File kmzFile) {
        return com.atakmap.map.formats.kmz.KmzLayerInfoSpi.findDocumentKML(kmzFile);
    }
}
