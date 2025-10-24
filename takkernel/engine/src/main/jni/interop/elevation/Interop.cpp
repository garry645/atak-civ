#include "interop/elevation/Interop.h"

#include <thread/Lock.h>
#include <thread/Mutex.h>
#include <util/Memory.h>
#include <interop/Pointer.h>

#include "common.h"
#include "interop/Pointer.h"
#include "interop/InterfaceMarshalContext.h"
#include "interop/elevation/ManagedElevationSource.h"
#include "interop/feature/Interop.h"

using namespace TAK::Engine::Elevation;
using namespace TAK::Engine::Thread;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Elevation;

namespace {
    struct {
        jclass id;
        jfieldID pointer;
        jmethodID ctor;
    } NativeElevationSource_class;

    struct {
        /**
         * key is raw pointer to the
         */
        std::map<const ElevationSource *, jobject> nativeElevationSourceInstances;
        std::map<jobject, std::weak_ptr<ElevationSource>> managedElevationSourceInstances;
        Mutex mutex;
    } ElevationSourceInterop;

    struct
    {
        jobject HighestResolution;
        jobject Low;
        jobject LowFillHoles;
    } HeightmapStrategy_enum;

    struct
    {
        jclass id;
        jfieldID boundsID;
        jfieldID numPostsLatID;
        jfieldID numPostsLngID;
        jfieldID invertYAxisID;
        jfieldID srid;
    } HeightmapParams_class;

    typedef std::unique_ptr<ElevationSource, void(*)(const ElevationSource *)> ElevationSourcePtr;

    bool checkInit(JNIEnv *env) NOTHROWS;
    bool Interop_class_init(JNIEnv *env) NOTHROWS;
    TAKErr Interop_findInternedObjectNoSync(Java::JNILocalRef &value, JNIEnv &env, const ElevationSource &csource) NOTHROWS;
}


TAKErr TAKEngineJNI::Interop::Elevation::Interop_adapt(std::shared_ptr<TAK::Engine::Elevation::ElevationSource> &value, JNIEnv *env, jobject msource, const bool force) NOTHROWS
{
    TAKErr code(TE_Ok);
    if(!msource)
        return TE_InvalidArg;
    LockPtr lock(NULL, NULL);
    if(!force) {
        // check if it's a wrapper, and return
        if(Interop_isWrapper<ElevationSource>(env, msource)) {
            do {
                Java::JNILocalRef msourcePtr(*env, env->GetObjectField(msource, NativeElevationSource_class.pointer));
                if(!Pointer_makeShared<ElevationSource>(env, msourcePtr))
                    break;
                value = *JLONG_TO_INTPTR(std::shared_ptr<ElevationSource>, env->GetLongField(msourcePtr, Pointer_class.value));
                return TE_Ok;
            } while(false);
        }

        code = Lock_create(lock, ElevationSourceInterop.mutex);
        TE_CHECKRETURN_CODE(code);
        for(auto it = ElevationSourceInterop.managedElevationSourceInstances.begin(); it != ElevationSourceInterop.managedElevationSourceInstances.end(); ) {
            if(env->IsSameObject(msource, it->first)) {
                value = it->second.lock();
                if(value.get())
                    return TE_Ok;
                env->DeleteWeakGlobalRef(it->first);
                ElevationSourceInterop.managedElevationSourceInstances.erase(it);
                break;
            } else if(env->IsSameObject(NULL, it->first)) {
                // erase cleared references
                env->DeleteWeakGlobalRef(it->first);
                it = ElevationSourceInterop.managedElevationSourceInstances.erase(it);
            } else {
                it++;
            }
        }
    }

    value = ElevationSourcePtr(new ManagedElevationSource(env, msource), Memory_deleter_const<ElevationSource, ManagedElevationSource>);
    if(!force)
        ElevationSourceInterop.managedElevationSourceInstances[env->NewWeakGlobalRef(msource)] = value;

    return code;
}
jobject TAKEngineJNI::Interop::Elevation::Interop_adapt(JNIEnv *env, const std::shared_ptr<TAK::Engine::Elevation::ElevationSource> &csource, const bool force) NOTHROWS
{
    Java::JNILocalRef msource(*env, nullptr);
    const auto code = Interop_marshal(msource, *env, csource, nullptr, force);
    return (code == TE_Ok) ? msource.release() : nullptr;
}
TAKErr TAKEngineJNI::Interop::Elevation::Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Elevation::ElevationSource> &csource, jobject mowner, const bool force) NOTHROWS
{
    const ManagedElevationSource *impl = dynamic_cast<const ManagedElevationSource *>(csource.get());
    if(impl) {
        value = Java::JNILocalRef(env, env.NewLocalRef(impl->impl));
        return TE_Ok;
    }

    LockPtr lock(NULL, NULL);
    // XXX - no failover on lock acuisition failure...won't intern
    if(!force && (Lock_create(lock, ElevationSourceInterop.mutex) == TE_Ok)) {
        Java::JNILocalRef msource(env, nullptr);
        if (Interop_findInternedObjectNoSync(msource, env, *csource) == TE_Ok && msource) {
            value = std::move(msource);
            return TE_Ok;
        }
    }
    if(!checkInit(&env))
        return TE_IllegalState;
    Java::JNILocalRef mpointer(env, NewPointer(&env, csource));
    value = Java::JNILocalRef(env, env.NewObject(NativeElevationSource_class.id, NativeElevationSource_class.ctor, mpointer.get(), mowner));
    if(!force && lock.get())
        ElevationSourceInterop.nativeElevationSourceInstances[csource.get()] = env.NewWeakGlobalRef(value.get());
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Elevation::Interop_getObject(Java::JNILocalRef &value, JNIEnv &env, const ElevationSource &csource) NOTHROWS
{
    const ManagedElevationSource *impl = dynamic_cast<const ManagedElevationSource *>(&csource);
    if(impl) {
        value = Java::JNILocalRef(env, env.NewLocalRef(impl->impl));
        return TAK::Engine::Util::TE_Ok;
    }

    // look up in intern
    TAKErr code(TE_Ok);
    LockPtr lock(NULL, NULL);
    code = Lock_create(lock, ElevationSourceInterop.mutex);
    TE_CHECKRETURN_CODE(code);

    return Interop_findInternedObjectNoSync(value, env, csource);
}

template<>
bool TAKEngineJNI::Interop::Elevation::Interop_isWrapper<ElevationSource>(JNIEnv *env, jobject msource)
{
    if(!checkInit(env))
        return false;
    if(!msource)
        return false;
    Java::JNILocalRef mclazz(*env, env->GetObjectClass(msource));
    return ATAKMapEngineJNI_equals(env, mclazz.get(), NativeElevationSource_class.id);
}
bool TAKEngineJNI::Interop::Elevation::Interop_isWrapper(const TAK::Engine::Elevation::ElevationSource &csource)
{
    const ManagedElevationSource *impl = dynamic_cast<const ManagedElevationSource *>(&csource);
    return !!impl;
}

TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Elevation::Interop_marshal(Java::JNILocalRef &mstrategy, JNIEnv &env, const TAK::Engine::Elevation::HeightmapStrategy &cstrategy) NOTHROWS
{
    if(!checkInit(&env))
        return TE_IllegalState;
    if(cstrategy == TAK::Engine::Elevation::HeightmapStrategy::Low)
        mstrategy = Java::JNILocalRef(env, HeightmapStrategy_enum.Low);
    else if(cstrategy == TAK::Engine::Elevation::HeightmapStrategy::LowFillHoles)
        mstrategy = Java::JNILocalRef(env, HeightmapStrategy_enum.LowFillHoles);
    else if(cstrategy == TAK::Engine::Elevation::HeightmapStrategy::HighestResolution)
        mstrategy = Java::JNILocalRef(env, HeightmapStrategy_enum.HighestResolution);
    else
        return TE_InvalidArg;
    return TE_Ok;
}
TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Elevation::Interop_marshal(TAK::Engine::Elevation::HeightmapStrategy &cstrategy, JNIEnv &env, jobject mstrategy) NOTHROWS
{
    if(!checkInit(&env))
        return TE_IllegalState;
    if(mstrategy == nullptr)
        return TE_InvalidArg;
    else if(env.IsSameObject(mstrategy, HeightmapStrategy_enum.Low))
        cstrategy = TAK::Engine::Elevation::HeightmapStrategy::Low;
    else if(env.IsSameObject(mstrategy, HeightmapStrategy_enum.LowFillHoles))
        cstrategy = TAK::Engine::Elevation::HeightmapStrategy::LowFillHoles;
    else if(env.IsSameObject(mstrategy, HeightmapStrategy_enum.HighestResolution))
        cstrategy = TAK::Engine::Elevation::HeightmapStrategy::HighestResolution;
    else
        return TE_InvalidArg;
    return TE_Ok;
}

TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Elevation::Interop_marshal(Java::JNILocalRef &mparams, JNIEnv &env, const TAK::Engine::Elevation::HeightmapParams &cparams) NOTHROWS
{
    if(!checkInit(&env))
        return TE_IllegalState;
    return TE_NotImplemented;
}
TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Elevation::Interop_marshal(TAK::Engine::Elevation::HeightmapParams *cparams, JNIEnv &env, jobject mparams) NOTHROWS
{
    if(!checkInit(&env))
        return TE_IllegalState;
    Java::JNILocalRef mboundsRef(env, env.GetObjectField(mparams, HeightmapParams_class.boundsID));
    Feature::Interop_copy(&cparams->bounds, &env, mboundsRef.get());
    cparams->numPostsLat = env.GetIntField(mparams, HeightmapParams_class.numPostsLatID);
    cparams->numPostsLng = env.GetIntField(mparams, HeightmapParams_class.numPostsLngID);
    cparams->invertYAxis = env.GetBooleanField(mparams, HeightmapParams_class.invertYAxisID);
    cparams->srid = env.GetIntField(mparams, HeightmapParams_class.srid);
    return TE_Ok;
}

namespace
{
    bool checkInit(JNIEnv *env) NOTHROWS
    {
        static bool clinit = Interop_class_init(env);
        return clinit;
    }
    bool Interop_class_init(JNIEnv *env) NOTHROWS
    {
        NativeElevationSource_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/elevation/NativeElevationSource");
        NativeElevationSource_class.pointer = env->GetFieldID(NativeElevationSource_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        NativeElevationSource_class.ctor = env->GetMethodID(NativeElevationSource_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");

        jclass HeightmapStrategyId = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/elevation/ElevationManager$HeightmapStrategy");
        HeightmapStrategy_enum.HighestResolution = env->NewGlobalRef(env->GetStaticObjectField(HeightmapStrategyId, env->GetStaticFieldID(HeightmapStrategyId, "HighestResolution", "Lcom/atakmap/map/elevation/ElevationManager$HeightmapStrategy;")));
        HeightmapStrategy_enum.Low = env->NewGlobalRef(env->GetStaticObjectField(HeightmapStrategyId, env->GetStaticFieldID(HeightmapStrategyId, "Low", "Lcom/atakmap/map/elevation/ElevationManager$HeightmapStrategy;")));
        HeightmapStrategy_enum.LowFillHoles = env->NewGlobalRef(env->GetStaticObjectField(HeightmapStrategyId, env->GetStaticFieldID(HeightmapStrategyId, "LowFillHoles", "Lcom/atakmap/map/elevation/ElevationManager$HeightmapStrategy;")));

        HeightmapParams_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/elevation/ElevationManager$HeightmapParams");
        HeightmapParams_class.boundsID = env->GetFieldID(HeightmapParams_class.id, "bounds", "Lcom/atakmap/map/layer/feature/geometry/Envelope;");
        HeightmapParams_class.numPostsLatID = env->GetFieldID(HeightmapParams_class.id, "numPostsLat", "I");
        HeightmapParams_class.numPostsLngID = env->GetFieldID(HeightmapParams_class.id, "numPostsLng", "I");
        HeightmapParams_class.invertYAxisID = env->GetFieldID(HeightmapParams_class.id, "invertYAxis", "Z");
        HeightmapParams_class.srid = env->GetFieldID(HeightmapParams_class.id, "srid", "I");

        return true;
    }

    TAKErr Interop_findInternedObjectNoSync(Java::JNILocalRef &value, JNIEnv &env, const ElevationSource &csource) NOTHROWS
    {
        // look up in intern
        do {
            auto entry = ElevationSourceInterop.nativeElevationSourceInstances.find(&csource);
            if(entry == ElevationSourceInterop.nativeElevationSourceInstances.end())
                break;
            value = Java::JNILocalRef(env, env.NewLocalRef(entry->second));
            if(!value) { // weak ref was cleared
                env.DeleteWeakGlobalRef(entry->second);
                ElevationSourceInterop.nativeElevationSourceInstances.erase(entry);
            }
            return TAK::Engine::Util::TE_Ok;
        } while(false);
        value = Java::JNILocalRef(env, nullptr);
        return TAK::Engine::Util::TE_Ok;
    }
}
