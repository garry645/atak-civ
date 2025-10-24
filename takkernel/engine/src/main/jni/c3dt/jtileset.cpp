#include <string>

#ifdef __ANDROID__
#include <android/log.h>
#endif

#include "common.h"

#include "c3dt/shaders.h"
#include "core/RenderContext.h"
#include "formats/c3dt/GLTFRenderer.h"
#include "formats/c3dt/Tileset.h"
#include "interop/JNIDoubleArray.h"
#include "interop/JNILongArray.h"
#include "interop/JNIStringUTF.h"
#include "interop/formats/c3dt/Interop.h"
#include "interop/formats/c3dt/ManagedAssetAccessor.h"
#include "renderer/RenderContextAssociatedCache.h"

using namespace TAK::Engine::Util;
using namespace TAKEngineJNI;
using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Formats::Cesium3DTiles;

extern "C" JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_c3dt_Tileset_create
        (JNIEnv *env, jclass, jstring muri, jobject massetaccessor, jobject mopts)
{
    TAKErr code;
    if (!muri) return NULL;
    TAK::Engine::Port::String curi;
    if(muri) {
        Interop::JNIStringUTF_get(curi, *env, muri);
    }
    std::shared_ptr<CesiumAsync::IAssetAccessor> assetAccessor = std::make_shared<ManagedAssetAccessor>(*env, massetaccessor);
    TAK::Engine::Formats::Cesium3DTiles::TilesetOpenOptions opts;
    if (mopts != NULL) {
        code = Interop_copy(&opts, *env, mopts);
        if(ATAKMapEngineJNI_checkOrThrow(env, code))
            return NULL;
    }
    TAK::Engine::Formats::Cesium3DTiles::TilesetPtr tilesetPtr(nullptr, nullptr);
    code = TAK::Engine::Formats::Cesium3DTiles::Tileset_create(tilesetPtr, curi,
                                                                      assetAccessor, opts);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;

    Java::JNILocalRef mtileset(*env, NULL);
    code = Formats::Cesium3DTiles::Interop_marshal(mtileset, *env, std::move(tilesetPtr));
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return mtileset.release();
}

extern "C" JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_c3dt_Tileset_updateView
  (JNIEnv *env, jclass, jlong ptr, jdouble positionx, jdouble positiony, jdouble positionz,
   jdouble directionx, jdouble directiony, jdouble directionz,
   jdouble upx, jdouble upy, jdouble upz,
   jdouble viewportSizex, jdouble viewportSizey,
   jdouble hfov, jdouble vfov)
{
    Cesium3DTilesSelection::Tileset *ts = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tileset, ptr);
    if(!ts) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL Tileset");
#endif
        return 0;
    }
    std::unique_ptr<const Cesium3DTilesSelection::ViewUpdateResult> viewUpdateResult;
    TAKErr code = TAK::Engine::Formats::Cesium3DTiles::Tileset_updateView(viewUpdateResult, *ts, positionx,
                                                            positiony, positionz,
                                                            directionx, directiony, directionz,
                                                            upx, upy, upz,
                                                            viewportSizex, viewportSizey,
                                                            hfov, vfov);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    Java::JNILocalRef result(*env, NULL);
    code = Formats::Cesium3DTiles::Interop_marshal(result, *env, *viewUpdateResult);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return result.release();
}

extern "C" JNIEXPORT void JNICALL Java_com_atakmap_map_formats_c3dt_Tileset_loadRootTileSync
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    Cesium3DTilesSelection::Tileset *ts = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tileset, ptr);
    if(!ts) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL Tileset");
#endif
        return;
    }
    TAKErr code = TAK::Engine::Formats::Cesium3DTiles::Tileset_loadRootTileSync(*ts);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
}

extern "C" JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_c3dt_Tileset_getRootTile
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tileset *ts = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tileset, ptr);
    if(!ts) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL Tileset");
#endif
        return NULL;
    }
    const Cesium3DTilesSelection::Tile* tile;
    TAKErr code = TAK::Engine::Formats::Cesium3DTiles::Tileset_getRootTile(&tile, *ts);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    if (!tile) return NULL;
    TAK::Engine::Formats::Cesium3DTiles::TilePtr rootPtr(const_cast<Cesium3DTilesSelection::Tile*>(tile), NULL);
    Java::JNILocalRef mroot(*env, nullptr);
    code = Interop_marshal(mroot, *env, std::move(rootPtr));
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return (jobject) mroot.release();
}

extern "C" JNIEXPORT void JNICALL Java_com_atakmap_map_formats_c3dt_Tileset_destroy
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    Cesium3DTilesSelection::Tileset *ts = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tileset, ptr);
    if(!ts) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL Tileset");
#endif
        return;
    }
    TAK::Engine::Formats::Cesium3DTiles::Tileset_destroy(ts);
}

extern "C" JNIEXPORT void JNICALL Java_com_atakmap_map_formats_c3dt_Tileset_renderimpl
        (JNIEnv * env, jclass clazz, jlongArray mvaos, jobjectArray mMVPs, jboolean useShader, jint MVP_u)
{
    static TAK::Engine::Renderer::RenderContextAssociatedCache<Shaders> shaderMap;
    TAK::Engine::Core::RenderContext* renderContext = nullptr;
    TAK::Engine::Core::RenderContext_getCurrent(&renderContext);
    Shaders shader = shaderMap.get(renderContext);

    if(useShader) {
        glUseProgram(shader.pid);

        // grab uniforms to modify
        MVP_u = glGetUniformLocation(shader.pid, "MVP");
        GLuint sun_position_u = glGetUniformLocation(shader.pid, "sun_position");
        GLuint sun_color_u = glGetUniformLocation(shader.pid, "sun_color");
        GLuint tex_u = glGetUniformLocation(shader.pid, "tex");

        float sun_position[3] =  { 3.0, 10.0, -5.0 };
        float sun_color[3] = { 1.0, 1.0, 1.0 };

        GLint activeTex;
        glGetIntegerv(GL_ACTIVE_TEXTURE, &activeTex);

        glUniform3fv(sun_position_u, 1, sun_position);
        glUniform3fv(sun_color_u, 1, sun_color);
        glUniform1i(tex_u, activeTex - GL_TEXTURE0);
    }

    Interop::JNILongArray vaosArray(*env, mvaos, JNI_ABORT);
    for (int a = 0; a < vaosArray.length(); a++)
    {
        Interop::JNIDoubleArray mvpArray(*env, (jdoubleArray)env->GetObjectArrayElement(mMVPs, a), JNI_ABORT);
        float cmvp[16];
        for(int i = 0; i < 16; i++)
            cmvp[i] = (float)mvpArray[i];
        glUniformMatrix4fv(MVP_u, 1, GL_FALSE, cmvp);

        auto *renderer = JLONG_TO_INTPTR(GLRendererState, vaosArray[a]);
        if(renderer)
            TAK::Engine::Formats::Cesium3DTiles::Renderer_draw(*renderer, MVP_u, mvpArray);
    }
}
