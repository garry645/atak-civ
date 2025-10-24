package gov.tak.api.engine.map.cache;

import com.atakmap.interop.Interop;
import com.atakmap.map.EngineLibrary;

import gov.tak.api.annotation.DontObfuscate;
import gov.tak.api.annotation.NonNull;
import gov.tak.api.engine.map.features.Geometry;
import gov.tak.platform.marshal.MarshalManager;
//import com.atakmap.map.layer.feature.geometry.Geometry;

import java.util.Collection;
import java.util.Objects;

@DontObfuscate
public final class CachingService {
    static {
        EngineLibrary.initialize();
    }

    static Interop<com.atakmap.map.layer.feature.geometry.Geometry> Geometry_interop = Interop.findInterop(com.atakmap.map.layer.feature.geometry.Geometry.class);

    @DontObfuscate
    public enum StatusType
    {
        Started,
        Complete,
        Canceled,
        Error,
        Progress
    }

    @DontObfuscate
    public static class Status
    {
        public int id;
        public StatusType status;
        public int completed;
        public int total;
        public long startTime;
        public long estimateDownloadSize;
        public long estimateCompleteTime;
        public double downloadRate;
    }

    @DontObfuscate
    public interface IStatusListener
    {
        void statusUpdate(Status status);
    };

    private CachingService() {
    }

    @DontObfuscate
    public static class Request
    {
        public int id;
        public Geometry geom;
        public int priority;
        public double minRes;
        public double maxRes;
        public Collection<String> sourcePaths;
        public Collection<String> sinkPaths;
    }


    public static native void updateSource(String source, String defaultSink);
    public static void smartRequest(@NonNull Request request, IStatusListener callback)
    {
        Objects.requireNonNull(request);
        com.atakmap.map.layer.feature.geometry.Geometry geom = MarshalManager.marshal(request.geom, gov.tak.api.engine.map.features.Geometry.class, com.atakmap.map.layer.feature.geometry.Geometry.class);
        if(geom == null)
            throw new IllegalArgumentException();
        smartRequest(request.id, Geometry_interop.getPointer(geom), request.priority, request.minRes, request.maxRes, request.sourcePaths, request.sinkPaths, callback);
    }
    public static void downloadRequest(@NonNull Request request, IStatusListener callback)
    {
        Objects.requireNonNull(request);
        com.atakmap.map.layer.feature.geometry.Geometry geom = MarshalManager.marshal(request.geom, gov.tak.api.engine.map.features.Geometry.class, com.atakmap.map.layer.feature.geometry.Geometry.class);
        if(geom == null)
            throw new IllegalArgumentException();
        downloadRequest(request.id, Geometry_interop.getPointer(geom), request.priority, request.minRes, request.maxRes, request.sourcePaths, request.sinkPaths, callback);
    }

    public static native void cancelRequest(int id);
    public static native void smartRequest(int id, long geomptr, int priority, double minRes, double maxRes, Collection<String> sources, Collection<String> sinks, IStatusListener callback);
    public static native void downloadRequest(int id, long geomptr, int priority, double minRes, double maxRes, Collection<String> sources, Collection<String> sinks, IStatusListener callback);

    public static native void setSmartCacheDownloadLimit(long size);
    public static native long getSmartCacheDownloadLimit();
    public static native void setSmartCacheEnabled(boolean enabled);
    public static native boolean getSmartCacheEnabled();


}