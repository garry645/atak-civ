#include "gov_tak_api_engine_map_cache_CachingService.h"

#include <memory>

#include "common.h"

#include "util/CachingService.h"

#include "feature/Feature2.h"
#include "feature/Geometry2.h"
#include "feature/LegacyAdapters.h"

#include "interop/JNIStringUTF.h"
#include "interop/Pointer.h"
#include "interop/feature/Interop.h"
#include "interop/model/Interop.h"
#include "interop/java/JNICollection.h"
#include "interop/java/JNIEnum.h"
#include "interop/java/JNIIterator.h"
#include "interop/java/JNILocalRef.h"

using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

namespace
{
    struct
    {
        jclass id;
        jmethodID statusUpdate;
    } CachingService_OnSourcesChangedListener_class;

    struct
    {
        jclass id;
        jobject Started, Complete, Canceled, Error, Progress;
    } StatusType_enum;

    struct
    {
        jclass id;
        jfieldID pointer;
    } Geometry_class;

    struct
    {
        jclass clazz;
        jfieldID id;
        jfieldID geom;
        jfieldID priority;
        jfieldID minRes;
        jfieldID maxRes;
        jfieldID sourcePaths;
        jfieldID sinkPaths;
    } Request_class;

    struct
    {
        jclass clazz;
        jfieldID id;
        jfieldID status;
        jfieldID completed;
        jfieldID total;
        jfieldID startTime;
        jfieldID estimateDownloadSize;
        jfieldID estimateCompleteTime;
        jfieldID downloadRate;
        jmethodID ctor;
    } Status_class;

    bool CachingService_class_init(JNIEnv *env) NOTHROWS
    {
        CachingService_OnSourcesChangedListener_class.id = ATAKMapEngineJNI_findClass(env, "gov/tak/api/engine/map/cache/CachingService$IStatusListener");
        CachingService_OnSourcesChangedListener_class.statusUpdate = env->GetMethodID(CachingService_OnSourcesChangedListener_class.id, "statusUpdate", "(Lgov/tak/api/engine/map/cache/CachingService$Status;)V");

        Request_class.clazz = ATAKMapEngineJNI_findClass(env, "gov/tak/api/engine/map/cache/CachingService$Request");
        Request_class.id = env->GetFieldID(Request_class.clazz, "id", "I");
        Request_class.geom = env->GetFieldID(Request_class.clazz, "geom", "Lgov/tak/api/engine/map/features/Geometry;");
        Request_class.priority = env->GetFieldID(Request_class.clazz, "priority", "I");
        Request_class.minRes = env->GetFieldID(Request_class.clazz, "minRes", "D");
        Request_class.maxRes = env->GetFieldID(Request_class.clazz, "maxRes", "D");
        Request_class.sourcePaths = env->GetFieldID(Request_class.clazz, "sourcePaths", "Ljava/util/Collection;");
        Request_class.sinkPaths = env->GetFieldID(Request_class.clazz, "sinkPaths", "Ljava/util/Collection;");

        Status_class.clazz = ATAKMapEngineJNI_findClass(env, "gov/tak/api/engine/map/cache/CachingService$Status");
        Status_class.id = env->GetFieldID(Status_class.clazz, "id", "I");
        Status_class.status = env->GetFieldID(Status_class.clazz, "status", "Lgov/tak/api/engine/map/cache/CachingService$StatusType;");
        Status_class.completed = env->GetFieldID(Status_class.clazz, "completed", "I");
        Status_class.total = env->GetFieldID(Status_class.clazz, "total", "I");
        Status_class.startTime = env->GetFieldID(Status_class.clazz, "startTime", "J");
        Status_class.estimateDownloadSize = env->GetFieldID(Status_class.clazz, "estimateDownloadSize", "J");
        Status_class.estimateCompleteTime = env->GetFieldID(Status_class.clazz, "estimateCompleteTime", "J");
        Status_class.downloadRate = env->GetFieldID(Status_class.clazz, "downloadRate", "D");
        Status_class.ctor = env->GetMethodID(Status_class.clazz, "<init>", "()V");

        Java::JNILocalRef enumValue(*env, nullptr);
        const char enumClass[] = "gov/tak/api/engine/map/cache/CachingService$StatusType";
        if(TAKEngineJNI::Interop::Java::JNIEnum_value(enumValue, *env, enumClass, "Started") != TE_Ok)
            return false;
        StatusType_enum.Started = env->NewWeakGlobalRef(enumValue);
        if(TAKEngineJNI::Interop::Java::JNIEnum_value(enumValue, *env, enumClass, "Complete") != TE_Ok)
            return false;
        StatusType_enum.Complete = env->NewWeakGlobalRef(enumValue);
        if(TAKEngineJNI::Interop::Java::JNIEnum_value(enumValue, *env, enumClass, "Canceled") != TE_Ok)
            return false;
        StatusType_enum.Canceled = env->NewWeakGlobalRef(enumValue);
        if(TAKEngineJNI::Interop::Java::JNIEnum_value(enumValue, *env, enumClass, "Progress") != TE_Ok)
            return false;
        StatusType_enum.Progress = env->NewWeakGlobalRef(enumValue);
        if(TAKEngineJNI::Interop::Java::JNIEnum_value(enumValue, *env, enumClass, "Error") != TE_Ok)
            return false;
        StatusType_enum.Error = env->NewWeakGlobalRef(enumValue);

        Geometry_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/feature/geometry/Geometry");
        Geometry_class.pointer = env->GetFieldID(Geometry_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");

        return true;
    }
    bool checkInit(JNIEnv *env) NOTHROWS
    {
        static bool clinit = CachingService_class_init(env);
        return clinit;
    }
    TAKErr Interop_marshal(std::unique_ptr<TAK::Engine::Port::String[]> &strings, std::size_t&count, JNIEnv *env, jobject stringList) NOTHROWS
    {
        if(!env)
            return TE_InvalidArg;
        if(env->ExceptionCheck())
            return TE_Err;
        if(!checkInit(env))
            return TE_IllegalState;
        if(!stringList)
        {
            count = 0;
            return TE_Ok;
        }

        TAKErr code(TE_Ok);

        code = Java::JNICollection_size(count, *env, stringList);
        TE_CHECKRETURN_CODE(code);

        strings.reset(new TAK::Engine::Port::String[count]);

        Java::JNILocalRef iterator(*env, nullptr);
        code = JNICollection_iterator(iterator, *env, stringList);
        TE_CHECKRETURN_CODE(code);

        bool hasNext;
        code = Java::JNIIterator_hasNext(hasNext, *env, iterator);
        TE_CHECKRETURN_CODE(code);

        std::size_t i = 0;
        while (hasNext)
        {
            Java::JNILocalRef str(*env, nullptr);
            code = Java::JNIIterator_next(str, *env, iterator);
            TE_CHECKRETURN_CODE(code);

            TAK::Engine::Port::String cStr;
            JNIStringUTF_get(cStr, *env, (jstring)str);

            strings.get()[i++] = cStr;

            code = Java::JNIIterator_hasNext(hasNext, *env, iterator);
            TE_CHECKRETURN_CODE(code);
        }

        return TE_Ok;
    }

    class NativeStatusListener : public TAK::Engine::Util::CachingServiceStatusListener
    {
    public:

        jobject toManaged(TAK::Engine::Util::StatusType status)
        {
            switch(status)
            {
            case TAK::Engine::Util::StatusType::Complete:
                return StatusType_enum.Complete;
            case TAK::Engine::Util::StatusType::Canceled:
                return StatusType_enum.Canceled;
            case TAK::Engine::Util::StatusType::Progress:
                return StatusType_enum.Progress;
            case TAK::Engine::Util::StatusType::Error:
                return StatusType_enum.Error;
            case TAK::Engine::Util::StatusType::Started:
                return StatusType_enum.Started;

            }
        }
    public:
        NativeStatusListener(JNIEnv *env, jobject impl) :
            jimpl(env->NewGlobalRef(impl))
        {
            checkInit(env);
        }

        virtual TAK::Engine::Util::TAKErr statusUpdate(const TAK::Engine::Util::CachingServiceStatus& status) override
        {
            LocalJNIEnv env;
            Java::JNILocalRef jstatus(*env, env->NewObject(Status_class.clazz, Status_class.ctor));
            env->SetIntField(jstatus, Status_class.id, status.id);
            env->SetObjectField(jstatus, Status_class.status, toManaged(status.status));
            env->SetIntField(jstatus, Status_class.completed, status.completed);
            env->SetIntField(jstatus, Status_class.total, status.total);
            env->SetLongField(jstatus, Status_class.startTime, status.startTime);
            env->SetLongField(jstatus, Status_class.estimateDownloadSize, status.estimateDownloadSize);
            env->SetLongField(jstatus, Status_class.estimateCompleteTime, status.estimateCompleteTime);
            env->SetDoubleField(jstatus, Status_class.downloadRate, status.downloadRate);

            env->CallVoidMethod(jimpl, CachingService_OnSourcesChangedListener_class.statusUpdate, jstatus.get());
            return TE_Ok;
        }

        ~NativeStatusListener()
        {
            if(jimpl)
            {
                LocalJNIEnv env;
                env->DeleteGlobalRef(jimpl);
                jimpl = nullptr;
            }
        }
    private :
        jobject jimpl;
    };
}

JNIEXPORT void JNICALL Java_gov_tak_api_engine_map_cache_CachingService_cancelRequest
  (JNIEnv *env, jclass clazz, int id)
{
    CachingService_cancelRequest(id);
}


JNIEXPORT void JNICALL Java_gov_tak_api_engine_map_cache_CachingService_updateSource
  (JNIEnv *env, jclass clazz, jstring source, jstring sink)
{
    TAKErr code;
    TAK::Engine::Port::String cSource, cSink;
    code = JNIStringUTF_get(cSource, *env, source);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    code = JNIStringUTF_get(cSink, *env, sink);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    code = CachingService_updateSource(cSource, cSink);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
}

JNIEXPORT void JNICALL Java_gov_tak_api_engine_map_cache_CachingService_smartRequest
  (JNIEnv *env, jclass, jint reqid, jlong geomptr, jint reqpri, jdouble minRes, jdouble maxRes, jobject jsources, jobject jsinks, jobject listener)
{
    checkInit(env);

    TAKErr code = TE_Ok;
    std::shared_ptr<NativeStatusListener> statusListener;
    if(listener)
        statusListener.reset(new NativeStatusListener(env, listener));

    TAK::Engine::Feature::Geometry2Ptr clone(nullptr, nullptr);
    code = TAK::Engine::Feature::Geometry_clone(clone, *JLONG_TO_INTPTR(const TAK::Engine::Feature::Geometry2, geomptr));
    std::shared_ptr<TAK::Engine::Feature::Geometry2> sharedGeom(clone.release(), clone.get_deleter());

    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    std::size_t count;
    std::unique_ptr<TAK::Engine::Port::String[]> cSources, cSinks;
    Interop_marshal(cSources, count, env, jsources);
    Interop_marshal(cSinks, count, env, jsinks);

    int id = reqid;
    int priority = reqpri;

    TAK::Engine::Util::CachingServiceRequest creq
    {
        id, sharedGeom, priority, minRes, maxRes, std::move(cSources), count, std::move(cSinks), count
    };

    code = CachingService_smartRequest(creq, statusListener);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
}


JNIEXPORT void JNICALL Java_gov_tak_api_engine_map_cache_CachingService_downloadRequest
    (JNIEnv *env, jclass, jint reqid, jlong geomptr, jint reqpri, jdouble minRes, jdouble maxRes, jobject jsources, jobject jsinks, jobject listener)
{
    checkInit(env);

    TAKErr code = TE_Ok;
    std::shared_ptr<NativeStatusListener> statusListener;
    if(listener)
        statusListener.reset(new NativeStatusListener(env, listener));

    TAK::Engine::Feature::Geometry2Ptr clone(nullptr, nullptr);
    code = TAK::Engine::Feature::Geometry_clone(clone, *JLONG_TO_INTPTR(const TAK::Engine::Feature::Geometry2, geomptr));
    std::shared_ptr<TAK::Engine::Feature::Geometry2> sharedGeom(clone.release(), clone.get_deleter());

    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    std::size_t count;
    std::unique_ptr<TAK::Engine::Port::String[]> cSources, cSinks;
    Interop_marshal(cSources, count, env, jsources);
    Interop_marshal(cSinks, count, env, jsinks);

    int id = reqid;
    int priority = reqpri;

    TAK::Engine::Util::CachingServiceRequest creq
    {
            id, sharedGeom, priority, minRes, maxRes, std::move(cSources), count, std::move(cSinks), count
    };

    code = CachingService_downloadRequest(creq, statusListener);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
}

JNIEXPORT void JNICALL Java_gov_tak_api_engine_map_cache_CachingService_setSmartCacheDownloadLimit(JNIEnv *env, jclass, jlong size)
{
    TAKErr code = CachingService_setSmartCacheDownloadLimit((std::size_t)size);
    ATAKMapEngineJNI_checkOrThrow(env, code);
}
JNIEXPORT jlong JNICALL Java_gov_tak_api_engine_map_cache_CachingService_getSmartCacheDownloadLimit(JNIEnv *env, jclass)
{
    std::size_t limit;
    TAKErr code = CachingService_getSmartCacheDownloadLimit(limit);
    ATAKMapEngineJNI_checkOrThrow(env, code);
    return limit;
}
JNIEXPORT void JNICALL Java_gov_tak_api_engine_map_cache_CachingService_setSmartCacheEnabled(JNIEnv *env, jclass, jboolean enabled)
{
    TAKErr code = CachingService_setSmartCacheEnabled(enabled);
    ATAKMapEngineJNI_checkOrThrow(env, code);
}
JNIEXPORT jboolean JNICALL Java_gov_tak_api_engine_map_cache_CachingService_getSmartCacheEnabled(JNIEnv *env, jclass)
{
    bool enabled;
    TAKErr code = CachingService_getSmartCacheEnabled(enabled);
    ATAKMapEngineJNI_checkOrThrow(env, code);
    return enabled;
}
