package com.atakmap.map.formats.c3dt;

import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.WeakHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ContentProxy implements ContentSource, Runnable
{
    enum FetchMode
    {
        /** Fetches from the source on cache miss, blocking until result */
        Blocking,
        /** Queues an asynchronous fetch from source on cache miss */
        Async,
        BlockingRefresh,
        AsyncRefresh,
    }
    final ContentContainer cache;
    final ContentSource source;

    final LinkedList<String> queue = new LinkedList<>();
    final Set<String> queuedUris = new HashSet<>();

    final Set<OnContentChangedListener> listeners = Collections.newSetFromMap(new WeakHashMap<>());

    ExecutorService executor;

    boolean connected;

    public ContentProxy(ContentSource source, ContentContainer cache)
    {
        this.source = source;
        this.cache = cache;
        this.connected = false;
    }

    @Override
    public byte[] getData(String uri, long[] version)
    {
        return this.getData(uri, version, FetchMode.Async);
    }

    public byte[] getData(String uri, long[] version, boolean async)
    {
        return this.getData(uri, version, async ? FetchMode.Async : FetchMode.Blocking);
    }

    byte[] getData(String uri, long[] version, FetchMode fetchMode)
    {
        // check cache
        final byte[] cached = this.cache.getData(uri, version);

        if(fetchMode == FetchMode.Async || fetchMode == FetchMode.AsyncRefresh) {
            // queue fetch if async refresh or cache miss
            if(fetchMode == FetchMode.AsyncRefresh || cached == null)
                enqueue(uri);
            return cached;
        } else { // fetchMode == FetchMode.Blocking || fetchMode == FetchMode.BlockingRefresh
            byte[] retval = cached;
            //  fetch immediately if blocking refresh or cache miss
            if(fetchMode == FetchMode.BlockingRefresh || cached == null) {
                final byte[] data = this.source.getData(uri, version);
                if (data != null && cache(uri, data, version))
                    retval = data;
            }
            return retval;
        }
    }

    private synchronized void enqueue(String uri)
    {
        if (this.connected && this.queuedUris.add(uri))
        {
            this.queue.add(uri);
            this.notifyAll();
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
    public synchronized void connect()
    {
        if (this.connected)
            return;
        this.source.connect();
        this.cache.connect();
        this.connected = true;
        final int numWorkers = 8;
        this.executor = Executors.newFixedThreadPool(numWorkers);
        for (int i = 0; i < numWorkers; i++)
            this.executor.submit(this);
    }

    @Override
    public synchronized void disconnect()
    {
        if (!this.connected)
            return;
        this.source.disconnect();
        this.cache.disconnect();
        this.connected = false;
        this.notifyAll();

        this.executor.shutdown();
        this.executor = null;
    }

    @Override
    public void run()
    {
        long[] version = new long[1];
        boolean notify = false;
        String fetchUri = null;
        while (true)
        {
            synchronized (this)
            {
                if(fetchUri != null) {
                    this.queuedUris.remove(fetchUri);
                    fetchUri = null;
                }
                if (notify)
                {
                    for (OnContentChangedListener l : this.listeners)
                    {
                        l.onContentChanged(ContentProxy.this);
                    }
                }
                if (!this.connected)
                    break;
                if (this.queue.isEmpty())
                {
                    try
                    {
                        this.wait();
                    } catch (InterruptedException ignored) {}
                    continue;
                }

                // popping the last element (FILO)
                fetchUri = this.queue.removeLast();
            }

            final byte[] fetched = this.source.getData(fetchUri, version);
            if (fetched != null)
            {
                notify = this.cache(fetchUri, fetched, version);
            }
        }
    }

    boolean cache(String uri, byte[] blob, long[] version) {
        // if refreshing a tileset JSON, verify it can be parsed
        String contentName = uri;
        final int queryIdx = contentName.indexOf('?');
        if(queryIdx >  0)
            contentName = contentName.substring(0, queryIdx);
        if(contentName.toLowerCase(LocaleUtil.getCurrent()).endsWith(".json")) {
            try {
                final String json = new String(blob, FileSystemUtils.UTF8_CHARSET);
                // Try to parse JSON
                new JSONObject(json);
            } catch(Throwable t) {
                return false;
            }
        }

        this.cache.put(uri, blob, (version != null) ? version[0] : System.currentTimeMillis());
        return true;
    }
}
