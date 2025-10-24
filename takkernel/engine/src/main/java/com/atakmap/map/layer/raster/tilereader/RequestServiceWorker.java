package com.atakmap.map.layer.raster.tilereader;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

final class RequestServiceWorker implements Runnable
{
    Collection<RequestQueue> queues;
    TileReader.ReadRequest servicing;
    boolean shutdown;
    Object queueLock;

    public void run()
    {
        while (true)
        {
            synchronized (queueLock)
            {
                servicing = null;
                if (this.shutdown)
                    break;
                ArrayList<TileReader.ReadRequest> dispatchFrom = null;
                for (RequestQueue queue : queues)
                {
                    if (!queue.queue.isEmpty())
                    {
                        dispatchFrom = queue.queue;
                    }
                }
                if (dispatchFrom == null)
                {
                    try
                    {
                        queueLock.wait();
                    } catch (InterruptedException ignored) {}
                    continue;
                }

                final TileReader.ReadRequest request = dispatchFrom.remove(dispatchFrom.size() - 1);
                if (request.canceled)
                    continue;
                servicing = request;
            }

            // execute read
            servicing.run();
        }
    }
}
