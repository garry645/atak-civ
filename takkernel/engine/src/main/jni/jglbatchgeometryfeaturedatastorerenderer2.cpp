#include "com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2.h"

#include <core/RenderContext.h>
#include <feature/LegacyAdapters.h>
#include <port/STLVectorAdapter.h>
#include <renderer/feature/GLBatchGeometryFeatureDataStoreRenderer3.h>

#include "com_atakmap_map_layer_feature_FeatureDataStore2.h"
#include "common.h"
#include "interop/Pointer.h"
#include "interop/JNIStringUTF.h"
#include "interop/feature/Interop.h"
#include "interop/java/JNICollection.h"
#include "interop/renderer/Interop.h"
#include "interop/renderer/core/Interop.h"

using namespace TAK::Engine::Core;
using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Renderer::Core;
using namespace TAK::Engine::Renderer::Core::Controls;
using namespace TAK::Engine::Renderer::Feature;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_create
    (JNIEnv *env, jclass clazz, jlong ctxptr, jlong dsptr, jobject moptions)
{
    auto &surface = *JLONG_TO_INTPTR(RenderContext, ctxptr);
    auto &subject = *JLONG_TO_INTPTR(FeatureDataStore3, dsptr);
    GLBatchGeometryFeatureDataStoreRenderer3::Options opts;
    if (moptions) {
        TAKEngineJNI::Interop::Feature::Interop_marshal(opts, *env, moptions);
    }

    std::unique_ptr<GLBatchGeometryFeatureDataStoreRenderer3, void(*)(const GLBatchGeometryFeatureDataStoreRenderer3 *)> retval(new GLBatchGeometryFeatureDataStoreRenderer3(surface, subject, opts), Memory_deleter_const<GLBatchGeometryFeatureDataStoreRenderer3>);
    return TAKEngineJNI::Interop::NewPointer(env, std::move(retval));
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_destruct
        (JNIEnv *env, jclass jclazz, jobject mpointer)
{
    Pointer_destruct<GLBatchGeometryFeatureDataStoreRenderer3>(env, mpointer);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_draw
        (JNIEnv *env, jclass clazz, jlong ptr, jobject mview, jint mrenderPass)
{
    TAKErr code(TE_Ok);
    auto layer = JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, ptr);
    if(!layer) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }
    std::shared_ptr<GLGlobeBase> cview;
    code = Renderer::Core::Interop_marshal(cview, *env, mview);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
    GLMapView2::RenderPass crenderPass;
    Renderer::Core::Interop_marshal(&crenderPass, mrenderPass);
    layer->draw(*cview, crenderPass);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_release
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    auto layer = JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, ptr);
    if(!layer) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }
    layer->release();
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_getRenderPass
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    auto layer = JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, ptr);
    if(!layer) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }
    const int crenderPass = layer->getRenderPass();
    jint mrenderPass = 0;
    Renderer::Core::Interop_marshal(&mrenderPass, (GLMapView2::RenderPass)crenderPass);
    return mrenderPass;
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_start
    (JNIEnv *env, jclass clazz, jlong renderablePtr)
{
    auto &renderable = *JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, renderablePtr);
    renderable.start();
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_stop
    (JNIEnv *env, jclass clazz, jlong renderablePtr)
{
    auto &renderable = *JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, renderablePtr);
    renderable.stop();
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_hitTest
  (JNIEnv * env, jclass clazz, jlong ptr, jobject mfids, jfloat screenX, jfloat screenY, jdouble latitude, jdouble longitude, jdouble gsd, jfloat radius, jint limit)
{
    auto renderable = JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, ptr);
    if(!renderable)
        return;

    std::vector<int64_t> cfids;
    TAK::Engine::Port::STLVectorAdapter<int64_t> cfids_v(cfids);
    renderable->hitTest2(cfids_v, screenX, screenY, atakmap::core::GeoPoint(latitude, longitude), gsd, radius, (limit < 0) ? 0u : (std::size_t)limit);
    TAKEngineJNI::Interop::Java::JNICollection_addAll(mfids, *env, cfids_v);
}
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_startEditing
  (JNIEnv *env, jclass clazz, jlong ptr, jlong fid)
{
    auto renderable = JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, ptr);
    if(!renderable)
        return false;
    return renderable->startEditing(fid) == TE_Ok;
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_stopEditing
  (JNIEnv *env, jclass clazz, jlong ptr, jlong fid)
{
    auto renderable = JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, ptr);
    if(!renderable)
        return;
    renderable->stopEditing(fid);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_updateFeature
  (JNIEnv *env, jclass clazz, jlong ptr, jlong fid, jint mpropFields, jstring mname, jlong geomptr, jlong styleptr, jobject mtraits)
{
    auto renderable = JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, ptr);
    if(!renderable)
        return;
    unsigned cpropFields = 0u;
    if(mpropFields&com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_GEOMETRY)
        cpropFields |= FeatureDataStore2::FeatureQueryParameters::GeometryField;
    if(mpropFields&com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_ATTRIBUTES)
        cpropFields |= FeatureDataStore2::FeatureQueryParameters::AttributesField;
    if(mpropFields&com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_STYLE)
        cpropFields |= FeatureDataStore2::FeatureQueryParameters::StyleField;
    if(mpropFields&com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_NAME)
        cpropFields |= FeatureDataStore2::FeatureQueryParameters::NameField;

    Traits ctraits;
    Feature::Interop_marshal(ctraits, *env, mtraits);

    TAK::Engine::Port::String cname;
    if(cpropFields&FeatureDataStore2::FeatureQueryParameters::NameField)
        JNIStringUTF_get(cname, *env, mname);
    GeometryPtr cgeom(nullptr, nullptr);
    if(cpropFields&FeatureDataStore2::FeatureQueryParameters::GeometryField)
        LegacyAdapters_adapt(cgeom, *JLONG_TO_INTPTR(const Geometry2, geomptr));
    renderable->updateFeature(fid, cpropFields, cname, cgeom.get(), JLONG_TO_INTPTR(const atakmap::feature::Style, styleptr), ctraits);
}

JNIEXPORT jlong JNICALL Java_com_atakmap_map_layer_feature_opengl_GLBatchGeometryFeatureDataStoreRenderer2_getFeaturesLabelControl
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    auto renderable = JLONG_TO_INTPTR(GLBatchGeometryFeatureDataStoreRenderer3, ptr);
    if(!renderable)
        return 0LL;
    void *ctrl = nullptr;
    renderable->getControl(&ctrl, FeaturesLabelControl_getType());
    return INTPTR_TO_JLONG(static_cast<FeaturesLabelControl *>(ctrl));
}
