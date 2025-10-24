#include "com_atakmap_map_layer_feature_NativeFeatureDefinition3.h"
#include "com_atakmap_map_layer_feature_NativeFeatureDefinition4.h"

#include <feature/FeatureDefinition2.h>
#include <feature/Geometry2.h>
#include <feature/LegacyAdapters.h>
#include <feature/Style.h>
#include <util/AttributeSet.h>
#include <util/Memory.h>
#include "common.h"

#include "interop/JNIByteArray.h"
#include "interop/Pointer.h"
#include <interop/feature/Interop.h>
#include "interop/feature/ManagedFeatureDefinition.h"
#include "interop/java/JNILocalRef.h"

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Util;

using namespace atakmap::feature;
using namespace atakmap::util;

using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Feature;

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_wrap
    (JNIEnv *env, jclass clazz, jobject mdefinition)
{
    FeatureDefinitionPtr retval(new ManagedFeatureDefinition(*env, mdefinition), Memory_deleter_const<FeatureDefinition2, ManagedFeatureDefinition>);
    return NewPointer(env, std::move(retval));
}
JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_hasObject
    (JNIEnv *env, jclass clazz, jlong ptr)
{
    auto cdefinition = JLONG_TO_INTPTR(const FeatureDefinition2, ptr);
    return !!dynamic_cast<const ManagedFeatureDefinition *>(cdefinition);
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getObject
    (JNIEnv *env, jclass clazz, jlong ptr)
{
    auto cdefinition = JLONG_TO_INTPTR(const FeatureDefinition2, ptr);
    auto cimpl = dynamic_cast<const ManagedFeatureDefinition *>(cdefinition);
    return !!cimpl ? cimpl->impl : nullptr;
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_destruct
    (JNIEnv *env, jclass clazz, jobject mpointer)
{
    Pointer_destruct_iface<FeatureDefinition2>(env, mpointer);
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getRawGeometry
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    FeatureDefinition2 *result = JLONG_TO_INTPTR(FeatureDefinition2, pointer);
    if(!result) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }

    TAKErr code(TE_Ok);
    FeatureDefinition2::RawData rawGeom;
    code = result->getRawGeometry(&rawGeom);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;

    const FeatureDefinition2::GeometryEncoding geomCoding = result->getGeomCoding();
    switch(geomCoding) {
        case FeatureDefinition2::GeomWkt :
            if(!rawGeom.text)
                return NULL;
            return env->NewStringUTF(rawGeom.text);
        case FeatureDefinition2::GeomWkb :
        case FeatureDefinition2::GeomBlob :
            if(!rawGeom.binary.value)
                return NULL;
            return JNIByteArray_newByteArray(env, reinterpret_cast<const jbyte *>(rawGeom.binary.value), rawGeom.binary.len);
        case FeatureDefinition2::GeomGeometry :
            if(!rawGeom.object)
                return NULL;
            Geometry2Ptr cgeom(NULL, NULL);
            LegacyAdapters_adapt(cgeom, *static_cast<const atakmap::feature::Geometry *>(rawGeom.object));
            if(ATAKMapEngineJNI_checkOrThrow(env, code))
                return NULL;
            return NewPointer(env, std::move(cgeom));
    }

    ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
    return NULL;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getGeomCoding
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    FeatureDefinition2 *result = JLONG_TO_INTPTR(FeatureDefinition2, pointer);
    if(!result) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    return (jint)result->getGeomCoding();
}
JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getName
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    FeatureDefinition2 *result = JLONG_TO_INTPTR(FeatureDefinition2, pointer);
    if(!result) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }

    TAKErr code(TE_Ok);
    const char *cname;
    code = result->getName(&cname);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    if(!cname)
        return NULL;
    return env->NewStringUTF(cname);
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getStyleCoding
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    FeatureDefinition2 *result = JLONG_TO_INTPTR(FeatureDefinition2, pointer);
    if(!result) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    return (jint)result->getStyleCoding();
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getRawStyle
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    FeatureDefinition2 *result = JLONG_TO_INTPTR(FeatureDefinition2, pointer);
    if(!result) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }

    TAKErr code(TE_Ok);
    FeatureDefinition2::RawData rawStyle;
    code = result->getRawStyle(&rawStyle);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;

    const FeatureDefinition2::StyleEncoding styleCoding = result->getStyleCoding();
    switch(styleCoding) {
        case FeatureDefinition2::StyleOgr :
            if(!rawStyle.text)
                return NULL;
            return env->NewStringUTF(rawStyle.text);
        case FeatureDefinition2::StyleStyle :
            if(!rawStyle.object)
                return NULL;
            TAK::Engine::Feature::StylePtr cstyle(static_cast<const Style *>(rawStyle.object)->clone(), Style::destructStyle);
            return NewPointer(env, std::move(cstyle));
    }

    ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
    return NULL;
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getAttributes
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    FeatureDefinition2 *result = JLONG_TO_INTPTR(FeatureDefinition2, pointer);
    if(!result) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }

    TAKErr code(TE_Ok);
    const AttributeSet *cattrs;
    code = result->getAttributes(&cattrs);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    if(!cattrs)
        return NULL;
    AttributeSetPtr cattrsPtr(new AttributeSet(*cattrs), Memory_deleter_const<AttributeSet>);
    return NewPointer(env, std::move(cattrsPtr));
}
JNIEXPORT jlong JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getTimestamp
  (JNIEnv *env, jclass clazz, jlong pointer)
{
    auto cdefinition = JLONG_TO_INTPTR(FeatureDefinition2, pointer);
    auto cimpl = dynamic_cast<ManagedFeatureDefinition *>(cdefinition);
    int64_t ctimestamp = TE_TIMESTAMP_NONE;
    if(cimpl)
        cimpl->getTimestamp(&ctimestamp);
    return ctimestamp;
}
JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getAltitudeMode
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    FeatureDefinition2 *result = JLONG_TO_INTPTR(FeatureDefinition2, ptr);
    if(!result) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0LL;
    }

    AltitudeMode altMode =  result->getAltitudeMode();
    switch(altMode) {
        case AltitudeMode::TEAM_Relative:
            return 1;
        case AltitudeMode::TEAM_Absolute:
            return 2;
        default:
            return 0;
    }
}
JNIEXPORT jdouble JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition3_getExtrude
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    FeatureDefinition2 *result = JLONG_TO_INTPTR(FeatureDefinition2, ptr);
    if(!result) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0LL;
    }

    return result->getExtrude();
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_feature_NativeFeatureDefinition4_getTraits
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    FeatureDefinition2 *result = JLONG_TO_INTPTR(FeatureDefinition2, ptr);
    if(!result) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0LL;
    }

    auto result3 = dynamic_cast<FeatureDefinition3 *>(result);
    Traits traits;
    if (result3)
    {
        traits = result3->getTraits();
    } else {
        traits.altitudeMode = result->getAltitudeMode();
        traits.extrude = result->getExtrude();
    }

    Java::JNILocalRef mtraits(*env, nullptr);
    TAKErr code = TAKEngineJNI::Interop::Feature::Interop_marshal(mtraits, *env, traits);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;

    return mtraits.release();
}
