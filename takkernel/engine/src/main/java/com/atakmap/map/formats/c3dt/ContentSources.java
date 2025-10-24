package com.atakmap.map.formats.c3dt;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.io.ProtocolHandler;
import com.atakmap.io.UriFactory;

import java.io.File;

public final class ContentSources
{
    private ContentSources()
    {
    }

    public static ContentSource create(final ProtocolHandler handler)
    {
        return new ContentSource()
        {
            @Override
            public byte[] getData(String uri, long[] version)
            {
                try
                {
                    if (version != null)
                        version[0] = System.currentTimeMillis();
                    try (UriFactory.OpenResult result = handler.handleURI(uri))
                    {
                        if (result == null)
                            return null;
                        else if (result.contentLength > 0L)
                            return FileSystemUtils.read(result.inputStream, (int) result.contentLength, false);
                        else
                            return FileSystemUtils.read(result.inputStream);
                    }
                } catch (Throwable t)
                {
                    return null;
                }
            }

            @Override
            public void addOnContentChangedListener(OnContentChangedListener l)
            {
            }

            @Override
            public void removeOnContentChangedListener(OnContentChangedListener l)
            {
            }

            @Override
            public void connect()
            {
            }

            @Override
            public void disconnect()
            {
            }
        };
    }

    public static ContentSource createDefault()
    {
        return createDefault(false);
    }

    public static ContentSource createDefault(final boolean preferLast)
    {
        return new ContentSource()
        {
            ProtocolHandler preferred = null;

            @Override
            public byte[] getData(String uri, long[] version)
            {
                try
                {
                    if (version != null)
                        version[0] = System.currentTimeMillis();
                    try (UriFactory.OpenResult result = open(preferred, uri))
                    {
                        if (result == null)
                            return null;
                        if (preferLast && result.handler != null)
                            preferred = result.handler;
                        if (result.contentLength > 0L)
                            return FileSystemUtils.read(result.inputStream, (int) result.contentLength, false);
                        else
                            return FileSystemUtils.read(result.inputStream);
                    }
                } catch (Throwable t)
                {
                    return null;
                }
            }

            @Override
            public void addOnContentChangedListener(OnContentChangedListener l)
            {
            }

            @Override
            public void removeOnContentChangedListener(OnContentChangedListener l)
            {
            }

            @Override
            public void connect()
            {
            }

            @Override
            public void disconnect()
            {
            }
        };
    }

    static ContentContainer createCache(String name, final File cacheDir, final String relativeUri) {
        if(name == null)
            return createCache(cacheDir, relativeUri);

        final ContentContainer impl =  new TilesPackage(new File(cacheDir, name + ".3dtiles").getAbsolutePath());
        return new ContentContainer()
        {
            @Override
            public void put(String uri, byte[] data, long version)
            {
                final String cacheFile = getCachePath(relativeUri, uri);
                if(cacheFile != null)
                    impl.put(cacheFile, data, version);
            }

            @Override
            public byte[] getData(String uri, long[] version)
            {
                final String cacheFile = getCachePath(relativeUri, uri);
                if (cacheFile == null)
                    return null;
                return impl.getData(cacheFile, version);
            }

            @Override
            public void addOnContentChangedListener(OnContentChangedListener l)
            {
                impl.addOnContentChangedListener(l);
            }

            @Override
            public void removeOnContentChangedListener(OnContentChangedListener l)
            {
                impl.removeOnContentChangedListener(l);
            }

            @Override
            public void connect()
            {
                impl.connect();
            }

            @Override
            public void disconnect()
            {
                impl.disconnect();
            }
        };
    }

    public static ContentContainer createCache(final File cacheDir, final String relativeUri)
    {
        return new LegacyCache(cacheDir, relativeUri);
    }

    public static byte[] getData(ContentSource source, String uri, long[] version, boolean async)
    {
        if (source instanceof ContentProxy)
            return ((ContentProxy) source).getData(uri, version, async);
        else
            return source.getData(uri, version);
    }

    static String getCachePath(String relativeUri, String uriStr)
    {
        if (relativeUri != null && uriStr.startsWith(relativeUri))
            uriStr = uriStr.replace(relativeUri, "");
        uriStr = uriStr.substring(uriStr.indexOf(':') + 1);
        while (uriStr.length() > 0 && uriStr.charAt(0) == '/')
            uriStr = uriStr.substring(1);
        // strip off any query
        final int queryIdx = uriStr.indexOf('?');
        if(queryIdx >  0)
            uriStr = uriStr.substring(0, queryIdx);

        return uriStr;
    }

    static UriFactory.OpenResult open(ProtocolHandler preferred, String uri)
    {
        if (preferred != null)
        {
            UriFactory.OpenResult result = preferred.handleURI(uri);
            if (result != null)
            {
                result.handler = preferred;
                return result;
            }
        }
        return UriFactory.open(uri);
    }
}
