#include "com_atakmap_map_layer_feature_vectortiles_GLVectorTiles.h"

#include <fstream>
#include <vector>

#include <formats/mbtiles/MapBoxGLStyleSheet.h>
#include <port/STLVectorAdapter.h>
#include <raster/tilematrix/TileMatrix.h>
#include <renderer/feature/GLVectorTiles.h>
#include <util/ConfigOptions.h>
#include <util/DataInput2.h>
#include <util/ProtocolHandler.h>

#include "common.h"
#include "interop/JNILongArray.h"
#include "interop/core/Interop.h"
#include "interop/feature/Interop.h"
#include "interop/java/JNICollection.h"
#include "interop/raster/tilematrix/Interop.h"
#include "interop/renderer/core/Interop.h"

using namespace TAK::Engine::Core;
using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Formats::MBTiles;
using namespace TAK::Engine::Raster::TileMatrix;
using namespace TAK::Engine::Renderer::Core;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI;

namespace
{
    template<class T>
    jobject createImpl(JNIEnv *env, jclass clazz, jlong ctxptr, jobject mtiles, jboolean overlay, jboolean autostyle, jlongArray mpointer)
    {
        std::shared_ptr<T> ctiles;
        if(Interop::Raster::TileMatrix::Interop_marshal(ctiles, *env, mtiles) != TE_Ok)
            return nullptr;
        if(!ctxptr)
            return nullptr;
        std::shared_ptr<StyleSheet> ss;
        do {
            if (autostyle)
                break;

            std::string sspath;
            DataInput2Ptr assetStream(nullptr, nullptr);
            TAK::Engine::Port::String overrideStyle;
            std::vector<const char *> search;
            if(overlay)
                search.push_back("overlay");
            if(ConfigOptions_getIntOptionOrDefault("vector-tiles.dark-default", 0))
                search.push_back("dark");
            search.push_back("bright");
            for(const auto &sp : search) {
                std::ostringstream searchPath;
                searchPath << "asset:/style/omt/" << sp << "/style.json";
                sspath = searchPath.str();
                if (ProtocolHandler_handleURI(assetStream, sspath.c_str()) == TE_Ok)
                    break;
            }


            if(!assetStream)
                break;
            char buf[4096];
            std::size_t numRead;
            std::stringstream buffer;
            while (assetStream->read(reinterpret_cast<uint8_t *>(buf), &numRead, 512) == TE_Ok) {
                buffer.write(buf, numRead);
            }
            std::ifstream t(sspath);
            buffer << t.rdbuf();

            auto parsed = nlohmann::json::parse(buffer.str(), nullptr, false);
            if(!parsed.is_discarded())
                ss = MapBoxGLStyleSheet_parse(parsed, sspath.c_str());
            if (ss && ss->empty())
                ss.reset();
        } while(false);
        GLMapRenderable2Ptr cgltiles(new GLVectorTiles(*JLONG_TO_INTPTR(RenderContext, ctxptr), ctiles, overlay, ss), Memory_deleter_const<GLMapRenderable2, GLVectorTiles>);
        if(mpointer) {
            Interop::JNILongArray cpointer(*env, mpointer, 0);
            cpointer[0] = INTPTR_TO_JLONG(static_cast<GLVectorTiles *>(cgltiles.get()));
        }
        Interop::Java::JNILocalRef mgltiles(*env, nullptr);
        Interop::Renderer::Core::Interop_marshal(mgltiles, *env, std::move(cgltiles));
        return mgltiles.release();
    }
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_vectortiles_GLVectorTiles_createClientImpl
  (JNIEnv *env, jclass clazz, jlong mctx, jobject mtiles, jboolean overlay, jboolean autostyle, jlongArray mpointer)
{
    return createImpl<TileClient>(env, clazz, mctx, mtiles, overlay, autostyle, mpointer);
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_vectortiles_GLVectorTiles_createContainerImpl
  (JNIEnv *env, jclass clazz, jlong mctx, jobject mtiles, jboolean overlay, jboolean autostyle, jlongArray mpointer)
{
    return createImpl<TileContainer>(env, clazz, mctx, mtiles, overlay, autostyle, mpointer);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_vectortiles_GLVectorTiles_sourceContentUpdated__J
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    auto *tiles = JLONG_TO_INTPTR(GLVectorTiles, ptr);
    if(tiles)
        tiles->getTiles().sourceContentUpdated();
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_vectortiles_GLVectorTiles_sourceContentUpdated__JIII
  (JNIEnv *env, jclass clazz, jlong ptr, jint zoom, jint x, jint y)
{
    auto *tiles = JLONG_TO_INTPTR(GLVectorTiles, ptr);
    if(tiles)
        tiles->getTiles().sourceContentUpdated(zoom, x, y);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_vectortiles_GLVectorTiles_hitTest
  (JNIEnv * env, jclass clazz, jlong ptr, jobject mfids, jfloat screenX, jfloat screenY, jdouble latitude, jdouble longitude, jdouble gsd, jfloat radius, jint limit)
{
    auto renderable = JLONG_TO_INTPTR(GLVectorTiles, ptr);
    if(!renderable)
        return;

    std::vector<std::shared_ptr<const Feature2>> cfeatures;
    TAK::Engine::Port::STLVectorAdapter<std::shared_ptr<const Feature2>> cfeatures_v(cfeatures);
    renderable->hitTest(cfeatures_v, screenX, screenY, GeoPoint2(latitude, longitude), gsd, radius, (limit < 0) ? 0u : (std::size_t)limit);
    TAKEngineJNI::Interop::Java::JNICollection_addAll<std::shared_ptr<const Feature2>>(mfids, *env, cfeatures_v,
        [](Interop::Java::JNILocalRef &m, JNIEnv &envr, const std::shared_ptr<const Feature2> &c)
        {
            Interop::Feature::Interop_marshal(m, envr, *c);
            return TE_Ok;
        });
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_vectortiles_GLVectorTiles_getColor
  (JNIEnv * env, jclass clazz, jlong ptr)
{
    auto renderable = JLONG_TO_INTPTR(GLVectorTiles, ptr);
    if(!renderable)
        return -1;

    return (jint)renderable->getColor();
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_vectortiles_GLVectorTiles_setColor
  (JNIEnv * env, jclass clazz, jlong ptr, jint color)
{
    auto renderable = JLONG_TO_INTPTR(GLVectorTiles, ptr);
    if(!renderable)
        return;
    renderable->setColor((uint32_t)color);
}
