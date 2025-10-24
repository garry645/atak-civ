#include <string>

#ifdef __ANDROID__
#include <android/log.h>
#endif

#include "common.h"
#include "memory.h"

#include "formats/c3dt/TileRenderContent.h"
#include "interop/formats/c3dt/Interop.h"
#include "interop/java/JNILocalRef.h"

using namespace TAK::Engine::Formats::Cesium3DTiles;
using namespace TAK::Engine::Util;
using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Formats::Cesium3DTiles;

extern "C" JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_c3dt_TileRenderContent_getModel
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::TileRenderContent *tileRenderContent = JLONG_TO_INTPTR(Cesium3DTilesSelection::TileRenderContent, ptr);
    if(!tileRenderContent) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL TileRenderContent");
#endif
        return NULL;
    }
    auto model = TileRenderContent_getModel(tileRenderContent);
    if (!model) return NULL;
    ModelPtr modelPtr(model, Memory_leaker_const<CesiumGltf::Model>);
    Java::JNILocalRef mmodel(*env, NULL);
    TAKErr code = Formats::Cesium3DTiles::Interop_marshal(mmodel, *env, std::move(modelPtr));
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return mmodel.release();
}