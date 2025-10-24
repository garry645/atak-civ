package com.atakmap.content;

import com.atakmap.coremap.filesystem.FileSystemUtils;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * A default implementation of {@link CatalogCurrency} that relies metadata about the file (last
 * modified, length) to determine if an associated record is up-to-date.
 */
public class FileCurrency implements CatalogCurrency
{
    public final static CatalogCurrency INSTANCE = new FileCurrency();

    @Override
    public byte[] getAppData(File file) {
        return ByteBuffer.wrap(new byte[2 * Long.SIZE / 8])
                .order(ByteOrder.BIG_ENDIAN)
                .putLong(FileSystemUtils.getFileSize(file))
                .putLong(FileSystemUtils.getLastModified(file))
                .array();
    }

    @Override
    public int getAppVersion() {
        return 0;
    }

    @Override
    public String getName() {
        return "File";
    }

    @Override
    public boolean isValidApp(File f,
                              int appVersion,
                              byte[] appData) {
        boolean valid = false;

        if (appVersion == getAppVersion()) {
            ByteBuffer buffer = ByteBuffer.wrap(appData)
                    .order(ByteOrder.BIG_ENDIAN);

            valid = buffer.getLong() == FileSystemUtils.getFileSize(f)
                    && buffer.getLong() == FileSystemUtils
                    .getLastModified(f);
        }

        return valid;
    }
}
