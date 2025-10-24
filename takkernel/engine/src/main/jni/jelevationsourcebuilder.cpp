#include "com_atakmap_map_elevation_NativeElevationSource.h"

#include <vector>

#include <elevation/CascadingElevationSource.h>
#include <elevation/MultiplexingElevationSource.h>
#include <elevation/ResolutionConstrainedElevationSource.h>
#include <elevation/VirtualCLODElevationSource.h>
#include <util/Memory.h>

#include "common.h"
#include "interop/JNIStringUTF.h"
#include "interop/elevation/Interop.h"
#include "interop/java/JNILocalRef.h"

using namespace TAK::Engine::Elevation;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

namespace
{
    template<class T>
    jobject specialize(JNIEnv *env, jclass clazz, jstring mname, jobjectArray msources)
    {
        const jsize limit = env->GetArrayLength(msources);
        std::vector<std::shared_ptr<ElevationSource>> csources;
        csources.reserve(limit);
        for(jsize i = 0; i < limit; i++) {
            Java::JNILocalRef msource(*env, env->GetObjectArrayElement(msources, i));
            std::shared_ptr<ElevationSource> csource;
            Elevation::Interop_adapt(csource, env, msource, false);
            if(csource)
                csources.push_back(csource);
        }
        JNIStringUTF cname(*env, mname);
        ElevationSourcePtr cretval(new T(cname.get(), csources.data(), csources.size()), Memory_deleter_const<ElevationSource, T>);
        Java::JNILocalRef mretval(*env, nullptr);
        const auto code = Elevation::Interop_marshal(mretval, *env, std::move(cretval), msources);
        return (code == TE_Ok) ? mretval.release() : nullptr;
    }
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_elevation_NativeElevationSource_multiplex
  (JNIEnv *env, jclass clazz, jstring mname, jobjectArray msources)
{
    return specialize<MultiplexingElevationSource>(env, clazz, mname, msources);
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_elevation_NativeElevationSource_cascade
  (JNIEnv *env, jclass clazz, jstring mname, jobjectArray msources)
{
    return specialize<CascadingElevationSource>(env, clazz, mname, msources);
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_elevation_NativeElevationSource_constrainResolution
  (JNIEnv *env, jclass clazz, jobject mimpl, jdouble minRes, jdouble maxRes)
{
    std::shared_ptr<ElevationSource> cimpl;
    Elevation::Interop_adapt(cimpl, env, mimpl, false);
    if(!cimpl) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }
    ElevationSourcePtr cretval(new ResolutionConstrainedElevationSource(cimpl, minRes, maxRes), Memory_deleter_const<ElevationSource, ResolutionConstrainedElevationSource>);
    Java::JNILocalRef mretval(*env, nullptr);
    const auto code = Elevation::Interop_marshal(mretval, *env, std::move(cretval), mimpl);
    return (code == TE_Ok) ? mretval.release() : nullptr;
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_elevation_NativeElevationSource_clod
  (JNIEnv *env, jclass clazz, jobject mimpl, jobject mstrategy, jboolean sampleOnly)
{
    TAKErr code(TE_Ok);
    std::shared_ptr<ElevationSource> cimpl;
    Elevation::Interop_adapt(cimpl, env, mimpl, false);
    if(!cimpl) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }
    HeightmapStrategy cstrategy;
    code = Elevation::Interop_marshal(cstrategy, *env, mstrategy);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;
    ElevationSourcePtr cretval(new VirtualCLODElevationSource(cimpl, cstrategy, sampleOnly), Memory_deleter_const<ElevationSource, ResolutionConstrainedElevationSource>);
    Java::JNILocalRef mretval(*env, nullptr);
    code = Elevation::Interop_marshal(mretval, *env, std::move(cretval), mimpl);
    return (code == TE_Ok) ? mretval.release() : nullptr;
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_elevation_NativeElevationSource_tile
  (JNIEnv *env, jclass clazz, jobject mimpl, jboolean pointSampler, jint srid, jobject mstrategy, jobject mparams, jint , jstring mcachePath, jint cacheLimit, jboolean async)
{
    TAKErr code(TE_Ok);
    std::shared_ptr<ElevationSource> cimpl;
    Elevation::Interop_adapt(cimpl, env, mimpl, false);
    if(!cimpl) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }
    HeightmapStrategy cstrategy;
    code = Elevation::Interop_marshal(cstrategy, *env, mstrategy);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    ElevationSourcePtr cretval(nullptr, nullptr);
    VirtualCLODElevationSource::Builder b;
    b.setSource(cimpl).setStrategy(cstrategy).setPointSampler(pointSampler);
    if(mcachePath) {
        JNIStringUTF ccachePath(*env, mcachePath);
        b.enableCaching(ccachePath, cacheLimit, async);
    }
    if(mparams) {
        HeightmapParams cparams;
        code = Elevation::Interop_marshal(&cparams, *env, mparams);
        if(ATAKMapEngineJNI_checkOrThrow(env, code))
            return nullptr;
        b.optimizeForTileGrid(srid, cparams);
    }
    code = b.build(cretval);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;
    Java::JNILocalRef mretval(*env, nullptr);
    code = Elevation::Interop_marshal(mretval, *env, std::move(cretval), mimpl);
    return (code == TE_Ok) ? mretval.release() : nullptr;
}

