#include "com_atakmap_map_layer_model_SceneLayer.h"

#include <core/LegacyAdapters.h>
#include <feature/FeatureDataStore2.h>
#include <model/SceneLayer.h>
#include <renderer/core/GLLayerFactory2.h>
#include <renderer/core/GLLayerSpi2.h>
#include <renderer/model/GLSceneLayer.h>
#include <util/Error.h>

#include "common.h"
#include "interop/JNIStringUTF.h"
#include "interop/core/Interop.h"
#include "interop/java/JNILocalRef.h"

using namespace TAK::Engine::Core;
using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Model;
using namespace TAK::Engine::Renderer::Core;
using namespace TAK::Engine::Renderer::Model;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

namespace
{
    bool registerSpi() NOTHROWS;
    SceneLayer *getLayer(const atakmap::core::Layer &layer) NOTHROWS;
}

extern "C"
JNIEXPORT jobject JNICALL
Java_com_atakmap_map_layer_model_SceneLayer_newInstance(JNIEnv *env, jclass clazz, jstring mname, jstring mcacheDir)
{
    static bool clinit = registerSpi();

    TAKErr code(TE_Ok);

    TAK::Engine::Port::String cname;
    code = JNIStringUTF_get(cname, *env, mname);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;

    TAK::Engine::Port::String ccacheDir;
    code = JNIStringUTF_get(ccacheDir, *env, mcacheDir);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;

    Layer2Ptr cretval(new(std::nothrow) SceneLayer(cname, ccacheDir), Memory_deleter_const<Layer2, SceneLayer>);
    if(!cretval) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_OutOfMemory);
        return NULL;
    }

    std::shared_ptr<atakmap::core::Layer> clayer;
    code = LegacyAdapters_adapt(clayer, std::move(cretval));

    Java::JNILocalRef mretval(*env, NULL);
    code = Core::Interop_marshal(mretval, *env, clayer);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;

    return mretval.release();
}
extern "C"
JNIEXPORT void JNICALL
Java_com_atakmap_map_layer_model_SceneLayer_add__JLjava_lang_String_2Ljava_lang_String_2(JNIEnv *env, jclass clazz, jlong ptr, jstring muri, jstring mhint)
{
    TAKErr code(TE_Ok);

    TAK::Engine::Port::String curi;
    code = JNIStringUTF_get(curi, *env, muri);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    TAK::Engine::Port::String chint;
    code = JNIStringUTF_get(chint, *env, mhint);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    auto impl = getLayer(*JLONG_TO_INTPTR(atakmap::core::Layer, ptr));
    if(!impl) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }
    code = impl->add(nullptr, curi, chint);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_atakmap_map_layer_model_SceneLayer_remove(JNIEnv *env, jclass clazz, jlong ptr, jstring muri)
{
    TAKErr code(TE_Ok);

    TAK::Engine::Port::String curi;
    code = JNIStringUTF_get(curi, *env, muri);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;

    auto impl = getLayer(*JLONG_TO_INTPTR(atakmap::core::Layer, ptr));
    if(!impl) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }
    code = impl->remove(curi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;
    return true;
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_atakmap_map_layer_model_SceneLayer_contains(JNIEnv *env, jclass clazz, jlong ptr, jstring muri)
{
    TAKErr code(TE_Ok);

    TAK::Engine::Port::String curi;
    code = JNIStringUTF_get(curi, *env, muri);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;

    auto impl = getLayer(*JLONG_TO_INTPTR(atakmap::core::Layer, ptr));
    if(!impl) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }
    bool contains;
    code = impl->contains(&contains, curi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;
    return contains;
}
extern "C"
JNIEXPORT void JNICALL
Java_com_atakmap_map_layer_model_SceneLayer_setVisible(JNIEnv *env, jclass clazz, jlong ptr, jstring muri, jboolean visible)
{
    TAKErr code(TE_Ok);

    TAK::Engine::Port::String curi;
    code = JNIStringUTF_get(curi, *env, muri);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    auto impl = getLayer(*JLONG_TO_INTPTR(atakmap::core::Layer, ptr));
    if(!impl) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    FeatureDataStore2::FeatureSetQueryParameters fsParams;
    code = fsParams.names->add(curi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
    FeatureSetCursorPtr fsCursorPtr(nullptr, nullptr);
    code = impl->query(fsCursorPtr, fsParams);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    do {
        code = fsCursorPtr->moveToNext();
        TE_CHECKBREAK_CODE(code)
        const FeatureSet2 *fs;
        code = fsCursorPtr->get(&fs);
        TE_CHECKBREAK_CODE(code)
        impl->setVisible(fs->getId(), visible);
    } while(true);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_atakmap_map_layer_model_SceneLayer_shutdown(JNIEnv *env, jclass clazz, jlong ptr)
{
    TAKErr code(TE_Ok);

    auto impl = getLayer(*JLONG_TO_INTPTR(atakmap::core::Layer, ptr));
    if(!impl) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }
    code = impl->shutdown();
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
}
namespace
{
    bool registerSpi() NOTHROWS
    {
        // GLSceneLayer_spi returns a static instance.
        GLLayerSpi2Ptr spi(&GLSceneLayer_spi(), Memory_leaker_const<GLLayerSpi2>);
        GLLayerFactory2_registerSpi(std::move(spi), 1);
        return true;
    }

    SceneLayer *getLayer(const atakmap::core::Layer &layer) NOTHROWS
    {
        std::shared_ptr<Layer2> impl;
        LegacyAdapters_find(impl, layer);
        return impl ? static_cast<SceneLayer *>(impl.get()) : nullptr;
    }
}