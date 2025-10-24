#include "com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl.h"

#include <map>

#include <thread/Lock.h>
#include <thread/Mutex.h>
#include <util/Memory.h>
#include "renderer/core/controls/FeaturesLabelControl.h"
#include "renderer/feature/GLBatchGeometryFeatureDataStoreRenderer3.h"

#include "common.h"
#include "interop/JNIIntArray.h"
#include "interop/JNIStringUTF.h"
#include "interop/Pointer.h"

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Port;
using namespace TAK::Engine::Renderer::Core::Controls;
using namespace TAK::Engine::Thread;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_setShapeCenterMarkersVisible
  (JNIEnv *env, jclass clazz, jlong pointer, jboolean vis)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    control->setShapeCenterMarkersVisible(vis);
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_getShapeCenterMarkersVisible
 (JNIEnv *env, jclass clazz, jlong pointer)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    return control->getShapeCenterMarkersVisible();
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_setShapeLabelVisible
  (JNIEnv *env, jclass clazz, jlong pointer, jlong fid, jboolean vis)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    control->setShapeLabelVisible(fid, vis);
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_getShapeLabelVisible
  (JNIEnv *env, jclass clazz, jlong pointer, jlong fid)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    return control->getShapeLabelVisible(fid);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_setDefaultLabelBackground
  (JNIEnv *env, jclass clazz, jlong pointer, jint color, jboolean override)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    control->setDefaultLabelBackground((uint32_t)color, override);
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_getDefaultLabelBackgroundColor
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    uint32_t color;
    bool override;
    control->getDefaultLabelBackground(&color, &override);
    return (jint)color;
}
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_isDefaultLabelBackgroundOverride
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    uint32_t color;
    bool override;
    control->getDefaultLabelBackground(&color, &override);
    return override;
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_setDefaultLabelLevelOfDetail
  (JNIEnv *env, jclass clazz, jlong pointer, jint minLod, jint maxLod)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    if(minLod < atakmap::feature::LevelOfDetailStyle::MIN_LOD)
        minLod = atakmap::feature::LevelOfDetailStyle::MIN_LOD;
    if(maxLod > atakmap::feature::LevelOfDetailStyle::MAX_LOD)
        maxLod = atakmap::feature::LevelOfDetailStyle::MAX_LOD;
    control->setDefaultLabelLevelOfDetail((std::size_t)minLod, (std::size_t)maxLod);
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_getDefaultLabelMinLevelOfDetail
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    std::size_t minLod;
    std::size_t maxLod;
    control->getDefaultLabelLevelOfDetail(&minLod, &maxLod);
    return (jint)minLod;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_opengl_NativeFeaturesLabelControl_getDefaultLabelMaxLevelOfDetail
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    auto control = JLONG_TO_INTPTR(FeaturesLabelControl, pointer);
    std::size_t minLod;
    std::size_t maxLod;
    control->getDefaultLabelLevelOfDetail(&minLod, &maxLod);
    return (jint)maxLod;
}
