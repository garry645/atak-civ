package com.atakmap.map.cache;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicBoolean;

import gov.tak.api.engine.map.cache.AbstractCacheTrigger;
import gov.tak.api.engine.map.cache.CacheTriggerCancel;
import gov.tak.api.engine.map.cache.CachingService;
import gov.tak.api.engine.map.cache.DownloadCacheTrigger;
import gov.tak.api.engine.map.cache.DownloadStatus;
import gov.tak.api.engine.map.cache.DownloadTriggerStatus;
import gov.tak.api.engine.map.cache.SmartCacheTrigger;
import gov.tak.api.engine.map.cache.UpdateCacheSource;
import gov.tak.platform.marshal.MarshalManager;

public class CacheEventsService {

    private final EventBus eventBus;
    private final StatusListenerImpl statusListener;
    private final AtomicBoolean running = new AtomicBoolean(true);

    public  CacheEventsService()
    {
        this(EventBus.getDefault());
    }

    public CacheEventsService(EventBus operationalEventBus)
    {
        if (operationalEventBus == null)
            throw new IllegalArgumentException("null event bus");

        statusListener = new StatusListenerImpl();
        eventBus = operationalEventBus;
        eventBus.register(this);
    }

    public void shutdown()
    {
        if (running.getAndSet(false))
        {
            eventBus.unregister(this);
        }
    }

    // XXX: seems to be a technical limitation of greenrobot EventBus that pathway to
    //      subscribe methods must be fully public

    @Subscribe
    public void onSmartCacheTrigger(SmartCacheTrigger trigger)
    {
        CachingService.smartRequest(triggerToRequest(trigger), statusListener);
    }

    @Subscribe
    public void onDownloadCacheTrigger(DownloadCacheTrigger trigger)
    {
        CachingService.downloadRequest(triggerToRequest(trigger), statusListener);
    }

    @Subscribe
    public void onCacheTriggerCancel(CacheTriggerCancel cancel)
    {
        CachingService.cancelRequest(cancel.getTriggerId());
    }

    @Subscribe
    public void onUpdateCacheSource(UpdateCacheSource update)
    {
        Collection<String> sources = update.getCacheSources();
        for (String source : update.getCacheSources()) // should always be the case
        {
            CachingService.updateSource(source, update.getDefaultSink(source));
        }
    }

    private static DownloadStatus statusTypeToDlStatus(CachingService.StatusType status) {
        switch (status)
        {
            case Started: return DownloadStatus.STARTED;
            case Complete: return DownloadStatus.COMPLETED;
            case Canceled: return DownloadStatus.CANCELED;
            case Error: return DownloadStatus.FAILED;
        }
        throw new IllegalStateException("undefined CachingService.StatusType -> DownloadStatus");
    }

    private void fillCommonRequestDetails(CachingService.Request request, AbstractCacheTrigger trigger)
    {
        request.id = trigger.getTriggerId();
        gov.tak.api.engine.map.features.Geometry apiGeom = trigger.getGeometry();
        request.geom = trigger.getGeometry();
        request.priority = trigger.getPriority();
        request.minRes = trigger.getMinResolution();
        request.maxRes = trigger.getMaxResolution();
        request.sourcePaths = trigger.getCacheSource() == null ? null :  Collections.singleton(trigger.getCacheSource());
        request.sinkPaths = trigger.getCacheSink() == null ? null : Collections.singleton(trigger.getCacheSink());
    }

    private CachingService.Request triggerToRequest(AbstractCacheTrigger trigger)
    {
        CachingService.Request request = new CachingService.Request();
        fillCommonRequestDetails(request, trigger);
        return request;
    }

    private class StatusListenerImpl implements CachingService.IStatusListener
    {
        @Override
        public void statusUpdate(CachingService.Status status) {
            eventBus.post(new DownloadTriggerStatus(status.id, statusTypeToDlStatus(status.status),
                    status.completed, status.total, status.startTime, status.estimateDownloadSize,
                    status.estimateCompleteTime, // TODO: Assuming this is seconds
                    status.downloadRate));
        }
    }

}
