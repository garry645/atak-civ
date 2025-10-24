package com.atakmap.map.layer.raster.tilereader;

import android.os.SystemClock;

import com.atakmap.coremap.log.Log;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @deprecated use {@link com.atakmap.map.layer.raster.tilereader.AsynchronousIO2}
 */
@Deprecated final class LegacyAsyncIO
{
    private Map<TileReader, LegacyAsyncIO.RequestQueue> tasks;
    private LegacyAsyncIO.Task executing;
    private final Object syncOn;
    private boolean dead;
    private boolean started;
    private final long maxIdle;

    LegacyAsyncIO(Object syncOn, long maxIdle)
    {
        if (syncOn == null)
            syncOn = this;
        this.syncOn = syncOn;
        this.tasks = new HashMap<>();
        this.dead = true;
        this.started = false;
        this.maxIdle = maxIdle;
    }

    /**
     * Aborts all unserviced tasks and kills the thread. If a task is
     * currently being serviced, it will complete before the thread exits.
     * The thread may be restarted by queueing a new task.
     */
    void release()
    {
        synchronized (this.syncOn)
        {
            this.abortRequests(null);
            this.dead = true;
            this.syncOn.notify();
        }
    }

    /**
     * Aborts all unserviced tasks made by the specified reader. If
     * <code>null</code> tasks for all readers are aborted.
     *
     * @param reader
     */
    void abortRequests(TileReader reader)
    {
        synchronized (this.syncOn)
        {
            if (this.executing != null && executing.reader == reader)
            {
                if (this.executing.action instanceof TileReader.Cancelable)
                    ((TileReader.Cancelable) this.executing.action).cancel();
            }

            if (reader != null)
            {
                final LegacyAsyncIO.RequestQueue queue = this.tasks.remove(reader);
                while (queue != null)
                {
                    LegacyAsyncIO.Task t = queue.get();
                    if (t == null)
                        break;
                    if (t.action instanceof TileReader.Cancelable)
                        ((TileReader.Cancelable) t.action).cancel();
                }
            } else
            {
                for (Map.Entry<TileReader, LegacyAsyncIO.RequestQueue> entry : tasks.entrySet())
                {
                    while (true)
                    {
                        LegacyAsyncIO.Task t = entry.getValue().get();
                        if (t == null)
                            break;
                        if (t.action instanceof TileReader.Cancelable)
                            ((TileReader.Cancelable) t.action).cancel();
                    }
                }
                tasks.clear();
            }
        }
    }

    void setReadRequestPrioritizer(TileReader reader, Comparator<TileReader.ReadRequest> prioritizer)
    {
        synchronized (this.syncOn)
        {
            LegacyAsyncIO.RequestQueue queue = this.tasks.get(reader);
            if (queue == null)
                this.tasks.put(reader, queue = new LegacyAsyncIO.RequestQueue());
            queue.requestPrioritizer = prioritizer;
        }
    }

    void runLater(TileReader.ReadRequest request)
    {
        runLater(request.owner, new ReadRequestTask(request));
    }

    void runLater(TileReader reader, Runnable r)
    {
        synchronized (this.syncOn)
        {
            LegacyAsyncIO.RequestQueue queue = this.tasks.get(reader);
            if (queue == null)
                this.tasks.put(reader, queue = new LegacyAsyncIO.RequestQueue());
            queue.enqueue(new LegacyAsyncIO.Task(reader, r));
            this.syncOn.notify();

            this.dead = false;
            if (!this.started)
            {
                Thread t = new Thread()
                {
                    @Override
                    public void run()
                    {
                        LegacyAsyncIO.this.runImpl();
                    }
                };
                t.setPriority(Thread.MIN_PRIORITY);
                t.setName("tilereader-legacy-async-io-thread@" + Integer.toString(this.hashCode(), 16));

                this.started = true;
                t.start();
            }
        }
    }

    private void runImpl()
    {
        LegacyAsyncIO.Task task;
        while (true)
        {
            synchronized (this.syncOn)
            {
                this.executing = null;
                if (this.dead)
                {
                    this.started = false;
                    break;
                }

                // iterate all request queues and select the oldest task.
                // oldest task is used here to prevent starvation
                LegacyAsyncIO.RequestQueue rq = null;
                int candidateId = Integer.MAX_VALUE;
                for (LegacyAsyncIO.RequestQueue queue : tasks.values())
                {
                    final LegacyAsyncIO.Task t = queue.peek();
                    if (t == null)
                        continue;
                    if (t.id < candidateId)
                    {
                        rq = queue;
                        candidateId = t.id;
                    }
                }

                if (rq == null)
                {
                    final long startIdle = SystemClock.elapsedRealtime();
                    try
                    {
                        this.syncOn.wait(this.maxIdle);
                    } catch (InterruptedException ignored)
                    {
                    }
                    final long stopIdle = SystemClock.elapsedRealtime();
                    // check if the thread has idle'd out
                    if (this.maxIdle > 0L && (stopIdle - startIdle) >= this.maxIdle)
                        this.dead = true;
                    // wake up and re-run the sync block
                    continue;
                }

                task = rq.get();
                this.executing = task;
            }

            try
            {
                task.action.run();
            } catch (RuntimeException e)
            {
                Log.e(TileReader.TAG, "error: ", e);
            }
        }
    }

    final static class Task
    {
        final static AtomicInteger idGenerator = new AtomicInteger(0);
        public final TileReader reader;
        public final Runnable action;
        public final int id;

        public Task(TileReader reader, Runnable action)
        {
            this.reader = reader;
            this.action = action;
            this.id = idGenerator.getAndIncrement();
        }
    }

    final static class ReadRequestTask implements TileReader.Cancelable
    {
        final TileReader.ReadRequest request;

        ReadRequestTask(TileReader.ReadRequest request)
        {
            this.request = request;
        }

        @Override
        public void cancel()
        {
            request.cancel();
            request.callback.requestCanceled(request.id);
        }

        @Override
        public void run()
        {
            request.run();
        }
    }

    final static class RequestQueue implements Comparator<LegacyAsyncIO.Task>
    {
        // sort order for tasks is LO => HI priority
        ArrayList<LegacyAsyncIO.Task> tasks = new ArrayList<>(64);
        Comparator<TileReader.ReadRequest> requestPrioritizer;

        LegacyAsyncIO.Task peek()
        {
            if (tasks.isEmpty())
                return null;
            return tasks.get(tasks.size() - 1);
        }

        LegacyAsyncIO.Task get()
        {
            if (tasks.isEmpty())
                return null;
            return tasks.remove(tasks.size() - 1);
        }

        void enqueue(LegacyAsyncIO.Task task)
        {
            tasks.add(task);
            Collections.sort(tasks, this);
            while (!tasks.isEmpty())
            {
                final int idx = tasks.size() - 1;
                final LegacyAsyncIO.Task t = tasks.get(idx);
                if (t.action instanceof ReadRequestTask && ((ReadRequestTask) t.action).request.canceled)
                    tasks.remove(idx);
                else
                    break;
            }
        }

        @Override
        public int compare(LegacyAsyncIO.Task a, LegacyAsyncIO.Task b)
        {
            if (a == null && b == null)
                return 0;
            else if (a == null)
                return -1;
            else if (b == null)
                return 1;
            final boolean aIsRead = (a.action instanceof ReadRequestTask);
            final boolean bIsRead = (b.action instanceof ReadRequestTask);
            if (!aIsRead && !bIsRead)
                return b.id - a.id; // FIFO on non read requests
            else if (!aIsRead)
                return 1;
            else if (!bIsRead)
                return -1;

            final TileReader.ReadRequest aRequest = ((ReadRequestTask) a.action).request;
            final TileReader.ReadRequest bRequest = ((ReadRequestTask) b.action).request;
            if (aRequest.canceled && bRequest.canceled)
                return b.id - a.id;
            else if (aRequest.canceled)
                return 1;
            else if (bRequest.canceled)
                return -1;
            if (this.requestPrioritizer != null)
            {
                final int c = this.requestPrioritizer.compare(aRequest, bRequest);
                if (c != 0)
                    return c;
            }

            // prioritize high resolution tiles over low resolution tiles
            if (aRequest.level < bRequest.level)
                return 1;
            else if (aRequest.level > bRequest.level)
                return -1;
            else
                return a.id - b.id; // LIFO on read requests
        }
    }
}
