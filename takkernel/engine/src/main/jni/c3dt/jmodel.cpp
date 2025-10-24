#include <memory>

#ifdef __ANDROID__
#include <android/log.h>
#endif

#include <math/Point2.h>
#include "formats/c3dt/GLTFRenderer.h"
#include "formats/c3dt/Model.h"
#include "common.h"
#include "interop/java/JNILocalRef.h"

using namespace TAK::Engine;
using namespace TAK::Engine::Util;
using namespace TAKEngineJNI;
using namespace Interop;

extern "C" JNIEXPORT void JNICALL Java_com_atakmap_map_formats_c3dt_Model_destroy
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    CesiumGltf::Model *model = JLONG_TO_INTPTR(CesiumGltf::Model, ptr);
    delete model;
}

extern "C" JNIEXPORT jlong JNICALL Java_com_atakmap_map_formats_c3dt_Model_bindModel
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    CesiumGltf::Model *model = JLONG_TO_INTPTR(CesiumGltf::Model, ptr);
    if(!model) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "Cannot bind NULL model");
#endif
        return 0LL;
    }

    std::unique_ptr<GLRendererState> retval(new GLRendererState());
    bool bound = TAK::Engine::Formats::Cesium3DTiles::Renderer_bindModel(retval.get(), *model);

    if(!bound)
        return 0LL;
    return (jlong)(intptr_t)retval.release();
}
extern "C" JNIEXPORT void JNICALL Java_com_atakmap_map_formats_c3dt_Model_releaseModel
  (JNIEnv *env, jclass clazz, jlong vao)
{
    std::unique_ptr<GLRendererState> renderer((GLRendererState *)(intptr_t)vao);
    if(renderer)
        TAK::Engine::Formats::Cesium3DTiles::Renderer_release(*renderer);
}

extern "C" JNIEXPORT jdoubleArray JNICALL Java_com_atakmap_map_formats_c3dt_Model_getRtc
        (JNIEnv *env, jclass, jlong ptr)
{
    CesiumGltf::Model *model = JLONG_TO_INTPTR(CesiumGltf::Model, ptr);
    if(!model) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL model");
#endif
        return NULL;
    }
    Math::Point2<double> center;
    TAKErr code = TAK::Engine::Formats::Cesium3DTiles::Model_getRtc(center, *model);
    if (code != TE_Ok) return NULL;
    Java::JNILocalRef mresult(*env, env->NewDoubleArray(3));
    env->SetDoubleArrayRegion((jdoubleArray)mresult.get(), 0, 1, &center.x);
    env->SetDoubleArrayRegion((jdoubleArray)mresult.get(), 1, 1, &center.y);
    env->SetDoubleArrayRegion((jdoubleArray)mresult.get(), 2, 1, &center.z);
    return (jdoubleArray)mresult.release();
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_atakmap_map_formats_c3dt_Model_hittest
        (JNIEnv *env, jclass, jlong ptr, jdouble originx, jdouble originy, jdouble originz, jdouble directionx, jdouble directiony, jdouble directionz, jobject mhitpoint)
{
    CesiumGltf::Model *model = JLONG_TO_INTPTR(CesiumGltf::Model, ptr);
    if(!model) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL model");
#endif
        return false;
    }
    CesiumGltfContent::GltfUtilities::IntersectResult intersectResult;
    TAKErr code = TAK::Engine::Formats::Cesium3DTiles::Model_intersectRayGltfModel(&intersectResult, originx, originy, originz, directionx, directiony, directionz, *model);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;
    if (intersectResult.hit.has_value() && mhitpoint) {
        auto hitResult = intersectResult.hit.value();
        env->SetDoubleField(mhitpoint, pointD_x, hitResult.worldPoint.x);
        env->SetDoubleField(mhitpoint, pointD_y, hitResult.worldPoint.y);
        env->SetDoubleField(mhitpoint, pointD_z, hitResult.worldPoint.z);
    }
    return intersectResult.hit.has_value();
}
