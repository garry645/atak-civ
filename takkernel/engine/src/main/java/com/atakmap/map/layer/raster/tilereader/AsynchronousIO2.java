package com.atakmap.map.layer.raster.tilereader;

import com.atakmap.coremap.maps.coords.GeoPoint;
import com.atakmap.map.layer.feature.geometry.Envelope;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

final class AsynchronousIO2 implements Runnable
{
    private Collection<TileReader.ReadRequest> tasks;
    private List<RequestQueue> queues;

    private GeoPoint camera = GeoPoint.createMutable();

    private Collection<RequestServiceWorker> serviceWorkers;
    private Thread sortWorkerThread;

    AsynchronousIO2()
    {
        this.queues = new LinkedList<>();

        this.tasks = new ArrayList<>();

        double[] queueCosts = new double[] {2d, 17d, 257d, Double.MAX_VALUE};
        for (double maxCost : queueCosts)
            this.queues.add(new RequestQueue(maxCost, true));
        for (double maxCost : queueCosts)
            this.queues.add(new RequestQueue(maxCost, false));

        Collections.sort(this.queues, new Comparator<RequestQueue>()
        {
            public int compare(RequestQueue a, RequestQueue b)
            {
                if (a.maxCost < b.maxCost)
                    return -1;
                else if (a.maxCost > b.maxCost)
                    return 1;
                else
                    return a.hashCode() - b.hashCode();
            }
        });

        this.serviceWorkers = new ArrayList<>(queues.size());
        for (RequestQueue queue : this.queues)
        {
            RequestServiceWorker worker = new RequestServiceWorker();
            worker.queueLock = this.queues;
            // NOTE: as designed, the worker allows for multiple queues, allowing for work patterns
            // such that idle workers for high cost or prefetch queues may pick up lower cost or
            // non-prefetch jobs. This feature is not currently utilized as there is some concern
            // regarding the implications on concurrent memory consumption in the resource
            // constrained Android environment
            worker.queues = Collections.singleton(queue);
            serviceWorkers.add(worker);
        }
    }

    /**
     * Aborts all unserviced tasks and kills the thread. If a task is
     * currently being serviced, it will complete before the thread exits.
     * The thread may be restarted by queueing a new task.
     */
    void release()
    {
        this.abortRequests(null);

        synchronized (this.tasks)
        {
            if (this.sortWorkerThread != null)
                this.sortWorkerThread = null;
            this.tasks.notify();
        }
        synchronized (this.queues)
        {
            for (RequestServiceWorker serviceWorker : this.serviceWorkers)
                serviceWorker.shutdown = true;
            this.queues.notifyAll();
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
        synchronized (this.tasks)
        {
            for (TileReader.ReadRequest request : this.tasks)
                if (reader == null || request.owner == reader)
                    request.cancel();
        }
        synchronized (this.queues)
        {
            for (RequestQueue queue : this.queues)
            {
                for (TileReader.ReadRequest request : queue.queue)
                {
                    if (reader == null || request.owner == reader)
                        request.cancel();
                }
            }
            for (RequestServiceWorker serviceWorker : this.serviceWorkers)
            {
                if (serviceWorker.servicing != null && (reader == null || serviceWorker.servicing.owner == reader))
                    serviceWorker.servicing.cancel();
            }
        }
    }

    void setCameraLocation(final GeoPoint location)
    {
        synchronized (tasks)
        {
            this.camera.set(location);
            this.tasks.notify();
        }
    }

    void enqueue(TileReader.ReadRequest request, Envelope bounds)
    {
        synchronized (this.tasks)
        {
            request.bounds = new Envelope(bounds);
            tasks.add(request);

            if (this.sortWorkerThread == null)
            {
                this.sortWorkerThread = new Thread(this);
                this.sortWorkerThread.setPriority(Thread.MIN_PRIORITY);
                this.sortWorkerThread.setName("async-io-sort-worker@" + Integer.toString(this.hashCode(), 16));

                this.sortWorkerThread.start();

                // spawn the service workers
                for (RequestServiceWorker serviceWorker : serviceWorkers)
                {
                    Thread t = new Thread(serviceWorker);
                    t.setPriority(Thread.MIN_PRIORITY);
                    t.setName("async-io-service-worker@" + Integer.toString(this.hashCode(), 16));

                    t.start();
                }
            } else
            {
                this.tasks.notify();
            }
        }
    }

    public void run()
    {
        while (true)
        {
            ArrayList<TileReader.ReadRequest> requests;
            GeoPoint distanceFrom = GeoPoint.createMutable();
            synchronized (this.tasks)
            {
                // check for shutdown
                if (this.sortWorkerThread != Thread.currentThread())
                    break;

                if (this.tasks.isEmpty())
                {
                    try
                    {
                        this.tasks.wait();
                    } catch (InterruptedException ignored) {}
                    continue;
                }

                // transfer current requests queue via copy
                requests = new ArrayList<>(this.tasks.size());
                for (TileReader.ReadRequest request : this.tasks)
                    if (!request.canceled)
                        requests.add(request);
                this.tasks.clear();

                distanceFrom.set(camera);
            }

            synchronized (this.queues)
            {
                int cap = 0;
                for (RequestQueue queue : queues)
                    cap += queue.queue.size();
                requests.ensureCapacity(requests.size() + cap);
                for (RequestQueue queue : queues)
                {
                    for (TileReader.ReadRequest request : queue.queue)
                    {
                        if (!request.canceled)
                            requests.add(request);
                    }
                    queue.queue.clear();
                }

                // transfer the requests into the respective queues
                for (TileReader.ReadRequest request : requests)
                {
                    for (RequestQueue queue : queues)
                    {
                        if (queue.maxCost < request.cost)
                            continue;
                        else if (queue.prefetch != request.isPrefetch)
                            continue;
                        queue.queue.add(request);
                        break;
                    }
                }

                // sort
                final Comparator<TileReader.ReadRequest> cmp = new ReadRequestComparator(distanceFrom);
                for (RequestQueue queue : queues)
                    Collections.sort(queue.queue, cmp);

                this.queues.notifyAll();
            }
        }
    }
}
