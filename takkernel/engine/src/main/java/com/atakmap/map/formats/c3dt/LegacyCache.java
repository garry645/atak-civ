package com.atakmap.map.formats.c3dt;

import com.atakmap.coremap.io.IOProviderFactory;
import com.atakmap.util.Collections2;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Set;

final class LegacyCache implements ContentContainer
{
    final Set<OnContentChangedListener> listeners = Collections2.newIdentityHashSet();

    final File cacheDir;
    final String relativeUri;

    LegacyCache(File cacheDir, String relativeUri) {
        this.cacheDir = cacheDir;
        this.relativeUri = relativeUri;
    }

    @Override
    public void put(String uri, byte[] data, long version)
    {
        final File cacheFile = getFile(uri);
        IOProviderFactory.mkdirs(cacheFile.getParentFile());
        try {
            try (FileOutputStream fos = new FileOutputStream(cacheFile)) {
                fos.write(data);
            }
            cacheFile.setLastModified(version);
        } catch (IOException ignored) {
            ignored.printStackTrace();
        }

        synchronized (listeners)
        {
            for (OnContentChangedListener l : this.listeners)
                l.onContentChanged(this);
        }
    }

    @Override
    public byte[] getData(String uri, long[] version)
    {
        final File cacheFile = getFile(uri);
        if (cacheFile == null)
            return null;
        try
        {
            byte[] data = new byte[(int) IOProviderFactory.length(cacheFile)];
            try (FileInputStream fis = IOProviderFactory.getInputStream(cacheFile))
            {
                int off = 0;
                while (off < data.length)
                {
                    final int r = fis.read(data, off, (data.length - off));
                    if (r < 0) // unexpected EOF
                        return null;
                    off += r;
                }
            }
            if (version != null)
                version[0] = IOProviderFactory.lastModified(cacheFile);
            return data;
        } catch (IOException ignored)
        {
            return null;
        }
    }

    @Override
    public synchronized void addOnContentChangedListener(OnContentChangedListener l)
    {
        this.listeners.add(l);
    }

    @Override
    public synchronized void removeOnContentChangedListener(OnContentChangedListener l)
    {
        this.listeners.remove(l);
    }

    @Override
    public void connect()
    {

    }

    @Override
    public void disconnect()
    {

    }

    private File getFile(String uriStr)
    {
        return new File(cacheDir, ContentSources.getCachePath(relativeUri, uriStr));
    }
}
