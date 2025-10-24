#include "com_atakmap_map_elevation_ElevationManager.h"

#include <vector>

#include <cmath>

#include <core/GeoPoint2.h>
#include <elevation/ElevationManager.h>
#include <elevation/HeightmapParams.h>
#include <port/STLVectorAdapter.h>
#include <util/Memory.h>

#include "common.h"
#include "interop/Pointer.h"
#include "interop/JNIDoubleArray.h"
#include "interop/elevation/Interop.h"
#include "interop/feature/Interop.h"

using namespace TAK::Engine::Core;
using namespace TAK::Engine::Elevation;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

JNIEXPORT jint JNICALL Java_com_atakmap_map_elevation_ElevationManager_queryElevationSourcesCount
  (JNIEnv *env, jclass clazz, jlong cparamsPtr)
{
    TAKErr code(TE_Ok);
    ElevationSource::QueryParameters *cparams = JLONG_TO_INTPTR(ElevationSource::QueryParameters, cparamsPtr);
    std::size_t retval;
    if(cparams)
        code = ElevationManager_queryElevationSourcesCount(&retval, *cparams);
    else
        code = ElevationManager_queryElevationSourcesCount(&retval, ElevationSource::QueryParameters());
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return 0;
    return retval;
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_elevation_ElevationManager_queryElevationSources
  (JNIEnv *env, jclass clazz, jlong cparamsPtr)
{
    TAKErr code(TE_Ok);
    ElevationChunkCursorPtr result(NULL, NULL);
    if(cparamsPtr)
        code = ElevationManager_queryElevationSources(result, *JLONG_TO_INTPTR(ElevationSource::QueryParameters, cparamsPtr));
    else
        code = ElevationManager_queryElevationSources(result, ElevationSource::QueryParameters());
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(result));
}
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_elevation_ElevationManager_getElevation__DDJ_3Ljava_lang_String_2
  (JNIEnv *env, jclass clazz, jdouble latitude, jdouble longitude, jlong cparamsPtr, jobjectArray mresultArr)
{
    TAKErr code(TE_Ok);
    double hae;
    TAK::Engine::Port::String cresultType;
    if(cparamsPtr)
        code = ElevationManager_getElevation(&hae, mresultArr ? &cresultType : NULL, latitude, longitude, *JLONG_TO_INTPTR(ElevationSource::QueryParameters, cparamsPtr));
    else
        code = ElevationManager_getElevation(&hae, mresultArr ? &cresultType : NULL, latitude, longitude, ElevationSource::QueryParameters());
    if((code == TE_InvalidArg) || ATAKMapEngineJNI_checkOrThrow(env, code))
        return NAN;
    if(mresultArr) {
        Java::JNILocalRef mresultType(*env, env->NewStringUTF(cresultType));
        env->SetObjectArrayElement(mresultArr, 0, mresultType);
    }
    return hae;
}
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_elevation_ElevationManager_getElevation___3DIJ
  (JNIEnv *env, jclass clazz, jdoubleArray mllaArr, jint count, jlong cparamsPtr)
{
    TAKErr code(TE_Ok);
    JNIDoubleArray mlla(*env, mllaArr, 0);
    if(cparamsPtr)
        code = ElevationManager_getElevation(mlla.get<double>()+2u, count, mlla.get<const double>()+1u, mlla.get<const double>(), 3u, 3u, 3u, *JLONG_TO_INTPTR(ElevationSource::QueryParameters, cparamsPtr));
    else
        code = ElevationManager_getElevation(mlla.get<double>()+2u, count, mlla.get<const double>()+1u, mlla.get<const double>(), 3u, 3u, 3u, ElevationSource::QueryParameters());
    const bool done = (code == TE_Ok);
    if(code == TE_Done)
        code = TE_Ok;
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;
    return done;
}
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_elevation_ElevationManager_getGeoidHeight
  (JNIEnv *env, jclass clazz, jdouble lat, jdouble lng)
{
    TAKErr code(TE_Ok);
    double retval;
    code = ElevationManager_getGeoidHeight(&retval, lat, lng);
    return (code == TE_Ok) ? retval : NAN;
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_elevation_ElevationManager_createHeightmapNative
  (JNIEnv *env, jclass clazz, jdoubleArray mvalueArr, jobject msource, jdouble gsd, jobject mparams, jobject mstrategy)
{
    TAKErr code(TE_Ok);

    if(!msource) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }
    std::shared_ptr<ElevationSource> csource;
    code = Elevation::Interop_adapt(csource, env, msource, false);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;
    JNIDoubleArray mvalue(*env, mvalueArr, 0);

    HeightmapParams params;
    code = Elevation::Interop_marshal(&params, *env, mparams);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;

    HeightmapStrategy strategy;
    code = Elevation::Interop_marshal(strategy, *env, mstrategy);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;

    code = ElevationManager_createHeightmap(mvalue.get<double>(), *csource, (double)gsd, params, strategy);
    const bool result = (code == TE_Ok);
    if(code == TE_Done)
        code = TE_Ok;
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;
    return result;
}
