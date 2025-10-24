#ifndef TAKENGINEJNI_INTEROP_FEATURE_INTEROP_H_INCLUDED
#define TAKENGINEJNI_INTEROP_FEATURE_INTEROP_H_INCLUDED

#include <jni.h>

#include <feature/Feature2.h>
#include <feature/FeatureDataStore2.h>
#include <feature/Geometry2.h>
#include <feature/Style.h>
#include <feature/Traits.h>
#include <port/Platform.h>
#include <renderer/feature/GLBatchGeometryFeatureDataStoreRenderer3.h>
#include <util/Error.h>
#include <interop/java/JNILocalRef.h>

namespace TAKEngineJNI {
    namespace Interop {
        namespace Feature {
            TAK::Engine::Util::TAKErr Interop_create(TAK::Engine::Feature::Geometry2Ptr &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_create(TAK::Engine::Feature::Geometry2Ptr_const &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            jobject Interop_create(JNIEnv *env, const TAK::Engine::Feature::Geometry2 &cgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(TAK::Engine::Feature::Geometry2 **value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(const TAK::Engine::Feature::Geometry2 **value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(std::shared_ptr<TAK::Engine::Feature::Geometry2> &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(std::shared_ptr<const TAK::Engine::Feature::Geometry2> &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mgeom, JNIEnv &env, TAK::Engine::Feature::Geometry2Ptr &&cgeom) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_create(TAK::Engine::Feature::StylePtr &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_create(TAK::Engine::Feature::StylePtr_const &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            jobject Interop_create(JNIEnv *env, const atakmap::feature::Style &cstyle) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(atakmap::feature::Style **value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(const atakmap::feature::Style **value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(std::shared_ptr<atakmap::feature::Style> &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(std::shared_ptr<const atakmap::feature::Style> &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mstyle, JNIEnv &env, const atakmap::feature::Style &cstyle) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_create(TAK::Engine::Feature::AttributeSetPtr &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_create(TAK::Engine::Feature::AttributeSetPtr_const &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            jobject Interop_create(JNIEnv *env, const atakmap::util::AttributeSet &cattr) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(atakmap::util::AttributeSet **value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(const atakmap::util::AttributeSet **value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(std::shared_ptr<atakmap::util::AttributeSet> &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_get(std::shared_ptr<const atakmap::util::AttributeSet> &value, JNIEnv *env, jobject jgeom) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mattrs, JNIEnv &env, const atakmap::util::AttributeSet &cattrs) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_copy(TAK::Engine::Feature::Envelope2 *value, JNIEnv *env, jobject jenvelope) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(jobject value, JNIEnv &env, const TAK::Engine::Feature::Envelope2 &cenvelope) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Feature::Envelope2 &cenvelope) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &maltMode, JNIEnv &env, const TAK::Engine::Feature::AltitudeMode &caltMode) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::AltitudeMode &caltMode, JNIEnv &env, jobject maltMode) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mlineMode, JNIEnv &env, const TAK::Engine::Feature::LineMode &clineMode) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::LineMode &clineMode, JNIEnv &env, jobject mlineMode) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mfeature, JNIEnv &env, const TAK::Engine::Feature::Feature2 &cfeature) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mfeature, JNIEnv &env, TAK::Engine::Feature::FeaturePtr &&cfeature) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::FeaturePtr &cfeature, JNIEnv &env, jobject mfeature) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::FeaturePtr_const &cfeature, JNIEnv &env, jobject mfeature) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mfeatureset, JNIEnv &env, const TAK::Engine::Feature::FeatureSet2 &cfeatureset) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mfeatureset, JNIEnv &env, TAK::Engine::Feature::FeatureSetPtr &&cfeatureset) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::FeatureSetPtr &cfeatureset, JNIEnv &env, jobject mfeatureset) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::FeatureSetPtr_const &cfeatureset, JNIEnv &env, jobject mfeatureset) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mparams, JNIEnv &env, const TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters &cparams) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::FeatureDataStore2::FeatureQueryParameters *cparams, JNIEnv &env, jobject mparams) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mparams, JNIEnv &env, const TAK::Engine::Feature::FeatureDataStore2::FeatureSetQueryParameters &cparams) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::FeatureDataStore2::FeatureSetQueryParameters *cparams, JNIEnv &env, jobject mparams) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::FeatureDataStore2 **cdatastore, JNIEnv &env, jobject mdatastore) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(std::shared_ptr<TAK::Engine::Feature::FeatureDataStore2> &cdatastore, JNIEnv &env, jobject mdatastore) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::FeatureDataStore2Ptr &cdatastore, JNIEnv &env, jobject mdatastore) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mdatastore, JNIEnv &env, const TAK::Engine::Feature::FeatureDataStore2 &cdatastore) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mdatastore, JNIEnv &env, const std::shared_ptr<TAK::Engine::Feature::FeatureDataStore2> &cdatastore) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mdatastore, JNIEnv &env, TAK::Engine::Feature::FeatureDataStore2Ptr &&cdatastore) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Feature::Traits &ctraits, JNIEnv &env, jobject mtraits) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mtraits, JNIEnv &env, const TAK::Engine::Feature::Traits &ctraits) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Renderer::Feature::GLBatchGeometryFeatureDataStoreRenderer3::Options &coptions, JNIEnv &env, jobject moptions) NOTHROWS;
        }
    }
}

#endif
