#include "com_atakmap_map_layer_model_SceneInfoFactory.h"

#include <model/ASSIMPSceneSpi.h>
#include <model/Cesium3DTilesSceneInfoSpi.h>
#include <model/Cesium3DTilesSceneSpi.h>
#include <model/ContextCaptureSceneInfoSpi.h>
#include <model/ContextCaptureSceneSpi.h>
#include <model/DAESceneInfoSpi.h>
#include <model/KMZSceneInfoSpi.h>
#include <model/LASSceneInfoSpi.h>
#include <model/LASSceneSpi.h>
#include <model/OBJSceneInfoSpi.h>
#include <model/Pix4dGeoreferencer.h>
#include "port/STLVectorAdapter.h"
#include <renderer/model/GLSceneFactory.h>
#include <renderer/model/GLC3DTRenderer.h>

#include "common.h"
#include "interop/JNIStringUTF.h"
#include <interop/model/Interop.h>

using namespace TAK::Engine::Core;
using namespace TAK::Engine::Model;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Model;

// XXX - native C3DT implementation is deficient. When interop for `GLSceneFactory` is introduced,
//       native impl must be brought to parity or disabled
#ifdef __ANDROID__
#define C3DT_ENABLED 0
#else
#define C3DT_ENABLED 1
#endif

class ImplGlobals {
public:
    ImplGlobals();
    ~ImplGlobals();
    std::shared_ptr<TAK::Engine::Model::Georeferencer> *pix4dgeoref;
    std::shared_ptr<TAK::Engine::Model::SceneInfoSpi> *objinfospi;
    std::shared_ptr<TAK::Engine::Model::SceneInfoSpi> *daeinfospi;
    std::shared_ptr<TAK::Engine::Model::SceneInfoSpi> *kmzinfospi;
    std::shared_ptr<TAK::Engine::Model::SceneInfoSpi> *ccinfospi;
    std::shared_ptr<TAK::Engine::Model::SceneInfoSpi> *c3dtinfospi;
    std::shared_ptr<TAK::Engine::Model::SceneInfoSpi> *lasinfospi;
    std::shared_ptr<TAK::Engine::Model::SceneSpi> *assimpscenespi;
    std::shared_ptr<TAK::Engine::Model::SceneSpi> *ccscenespi;
    std::shared_ptr<TAK::Engine::Model::SceneSpi> *c3dtscenespi;
    std::shared_ptr<TAK::Engine::Model::SceneSpi> *lasscenespi;
};

ImplGlobals::ImplGlobals()
{
    pix4dgeoref = new std::shared_ptr<TAK::Engine::Model::Georeferencer>(new TAK::Engine::Model::Pix4dGeoreferencer());
    objinfospi = new std::shared_ptr<TAK::Engine::Model::SceneInfoSpi>(new TAK::Engine::Model::OBJSceneInfoSpi());
    daeinfospi = new std::shared_ptr<TAK::Engine::Model::SceneInfoSpi>(new TAK::Engine::Model::DAESceneInfoSpi());
    kmzinfospi = new std::shared_ptr<TAK::Engine::Model::SceneInfoSpi>(new TAK::Engine::Model::KMZSceneInfoSpi());
    ccinfospi = new std::shared_ptr<TAK::Engine::Model::SceneInfoSpi>(new TAK::Engine::Model::ContextCaptureSceneInfoSpi());
#if C3DT_ENABLED
    c3dtinfospi = new std::shared_ptr<TAK::Engine::Model::SceneInfoSpi>(new TAK::Engine::Model::Cesium3DTilesSceneInfoSpi());
#endif
    lasinfospi = new std::shared_ptr<TAK::Engine::Model::SceneInfoSpi>(new TAK::Engine::Model::LASSceneInfoSpi());
    assimpscenespi = new std::shared_ptr<TAK::Engine::Model::SceneSpi>(new TAK::Engine::Model::ASSIMPSceneSpi());
    ccscenespi = new std::shared_ptr<TAK::Engine::Model::SceneSpi>(new TAK::Engine::Model::ContextCaptureSceneSpi());
#if C3DT_ENABLED
    c3dtscenespi = new std::shared_ptr<TAK::Engine::Model::SceneSpi>(new TAK::Engine::Model::Cesium3DTilesSceneSpi());
#endif
    lasscenespi = new std::shared_ptr<TAK::Engine::Model::SceneSpi>(new TAK::Engine::Model::LASSceneSPI());
}

ImplGlobals::~ImplGlobals()
{
    TAK::Engine::Model::SceneInfoFactory_unregisterGeoreferencer(*ImplGlobals::pix4dgeoref);
    delete ImplGlobals::pix4dgeoref;
    TAK::Engine::Model::SceneInfoFactory_unregisterSpi(*ImplGlobals::objinfospi);
    delete ImplGlobals::objinfospi;
    TAK::Engine::Model::SceneInfoFactory_unregisterSpi(*ImplGlobals::daeinfospi);
    delete ImplGlobals::daeinfospi;
    TAK::Engine::Model::SceneInfoFactory_unregisterSpi(*ImplGlobals::kmzinfospi);
    delete ImplGlobals::kmzinfospi;
    TAK::Engine::Model::SceneInfoFactory_unregisterSpi(*ImplGlobals::ccinfospi);
    delete ImplGlobals::ccinfospi;
#if C3DT_ENABLED
    TAK::Engine::Model::SceneInfoFactory_unregisterSpi(*ImplGlobals::c3dtinfospi);
    delete ImplGlobals::c3dtinfospi;
#endif
    TAK::Engine::Model::SceneInfoFactory_unregisterSpi(*ImplGlobals::lasinfospi);
    delete ImplGlobals::lasinfospi;
    TAK::Engine::Model::SceneFactory_unregisterSpi(*ImplGlobals::assimpscenespi);
    delete ImplGlobals::assimpscenespi;
    TAK::Engine::Model::SceneFactory_unregisterSpi(*ImplGlobals::ccscenespi);
    delete ImplGlobals::ccscenespi;
#if C3DT_ENABLED
    TAK::Engine::Model::SceneFactory_unregisterSpi(*ImplGlobals::c3dtscenespi);
    delete ImplGlobals::c3dtscenespi;
#endif
    TAK::Engine::Model::SceneFactory_unregisterSpi(*ImplGlobals::lasscenespi);
    delete ImplGlobals::lasscenespi;
}

namespace {
    static std::unique_ptr<ImplGlobals> implGlobals;
    void ImplGlobals_cleanup()
    {
        implGlobals.reset();
    }
}

extern "C"
JNIEXPORT void JNICALL
Java_com_atakmap_map_layer_model_SceneInfoFactory_registerAllNative(JNIEnv *env, jclass clazz)
{
    if (implGlobals) return;

    implGlobals.reset(new ImplGlobals());
#if C3DT_ENABLED
    std::unique_ptr<TAK::Engine::Renderer::Model::GLSceneSpi> c3dtSceneSpi(new TAK::Engine::Renderer::Model::GLC3DTRenderer::Spi());
    TAK::Engine::Renderer::Model::GLSceneFactory_registerSpi(std::move(c3dtSceneSpi), 1);
#endif
    TAK::Engine::Model::SceneFactory_registerSpi(*implGlobals->assimpscenespi);
    TAK::Engine::Model::SceneFactory_registerSpi(*implGlobals->ccscenespi);
#if C3DT_ENABLED
    TAK::Engine::Model::SceneFactory_registerSpi(*implGlobals->c3dtscenespi);
#endif
    TAK::Engine::Model::SceneFactory_registerSpi(*implGlobals->lasscenespi);
    TAK::Engine::Model::SceneInfoFactory_registerSpi(*implGlobals->objinfospi);
    TAK::Engine::Model::SceneInfoFactory_registerSpi(*implGlobals->daeinfospi);
    TAK::Engine::Model::SceneInfoFactory_registerSpi(*implGlobals->kmzinfospi);
#if C3DT_ENABLED
    TAK::Engine::Model::SceneInfoFactory_registerSpi(*implGlobals->c3dtinfospi);
#endif
    TAK::Engine::Model::SceneInfoFactory_registerSpi(*implGlobals->ccinfospi);
    TAK::Engine::Model::SceneInfoFactory_registerSpi(*implGlobals->lasinfospi);
    TAK::Engine::Model::SceneInfoFactory_registerGeoreferencer(*implGlobals->pix4dgeoref);
//    TAK::Engine::Model::SceneInfoFactory_registerGeoreferencer(*implGlobals->contextCaptureGeoreferencer);
    atexit(ImplGlobals_cleanup);
}
extern "C"
JNIEXPORT void JNICALL
Java_com_atakmap_map_layer_model_SceneInfoFactory_unregisterAllNative(JNIEnv *env, jclass clazz)
{
    implGlobals.reset();
}
extern "C"
JNIEXPORT jboolean JNICALL
Java_com_atakmap_map_layer_model_SceneInfoFactory_isSupportedNative(JNIEnv *env, jclass clazz, jstring mpath, jstring mhint)
{
    TAKErr code(TE_Ok);

    TAK::Engine::Port::String cpath;
    code = JNIStringUTF_get(cpath, *env, mpath);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;
    TAK::Engine::Port::String chint;
    code = JNIStringUTF_get(chint, *env, mhint);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;
    return SceneInfoFactory_isSupported(cpath, chint);
}
extern "C"
JNIEXPORT jobjectArray JNICALL
Java_com_atakmap_map_layer_model_SceneInfoFactory_createNative(JNIEnv *env, jclass clazz, jstring mpath, jstring mhint) {
    TAKErr code(TE_Ok);

    TAK::Engine::Port::String cpath;
    code = JNIStringUTF_get(cpath, *env, mpath);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    TAK::Engine::Port::String chint;
    code = JNIStringUTF_get(chint, *env, mhint);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;

    std::vector<SceneInfoPtr> infos;
    TAK::Engine::Port::STLVectorAdapter<SceneInfoPtr> adapter(infos);
    code = SceneInfoFactory_create(adapter, cpath, chint);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;

    Java::JNILocalRef objectArrayRef(*env, env->NewObjectArray(infos.size(), ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/model/ModelInfo"), NULL));

    size_t i = 0;
    for (SceneInfoPtr info : infos) {
        Java::JNILocalRef minfo(*env, nullptr);
        Interop_create(minfo, env, *info);
        env->SetObjectArrayElement(static_cast<jobjectArray>(objectArrayRef.get()), i++, minfo);
    }
    return static_cast<jobjectArray>(objectArrayRef.release());
}