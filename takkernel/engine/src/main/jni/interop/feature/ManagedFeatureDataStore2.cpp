//
// Created by Geo Dev on 5/2/23.
//

#include "interop/feature/ManagedFeatureDataStore2.h"

#include <feature/LegacyAdapters.h>
#include <thread/Lock.h>
#include <thread/Mutex.h>

#include "com_atakmap_map_layer_feature_FeatureDataStore2.h"
#include "com_atakmap_map_layer_feature_FeatureDataStore3.h"
#include "com_atakmap_map_layer_feature_FeatureDataStore4.h"

#include "interop/JNIStringUTF.h"
#include "interop/Pointer.h"
#include "interop/feature/Interop.h"
#include "interop/feature/ManagedFeatureCursor.h"
#include "interop/feature/ManagedFeatureSetCursor.h"
#include "interop/java/JNILocalRef.h"

using namespace TAKEngineJNI::Interop::Feature;

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Thread;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI;

namespace
{
    struct {
        jclass id;
        jmethodID queryFeatures;
        jmethodID queryFeaturesCount;
        jmethodID queryFeatureSets;
        jmethodID queryFeatureSetsCount;
        jmethodID insertFeature__JJLFeatureDefinition2J;
        jmethodID insertFeature__LFeature;
        jmethodID insertFeatures;
        jmethodID insertFeatureSet;
        jmethodID insertFeatureSets;
        jmethodID updateFeature;
        jmethodID updateFeatureSet__JLStringDD;
        jmethodID updateFeatureSet__JLString;
        jmethodID updateFeatureSet__JDD;
        jmethodID deleteFeature;
        jmethodID deleteFeatures;
        jmethodID deleteFeatureSet;
        jmethodID deleteFeatureSets;
        jmethodID setFeatureVisible;
        jmethodID setFeaturesVisible;
        jmethodID setFeatureSetVisible;
        jmethodID setFeatureSetsVisible;
        jmethodID hasTimeReference;
        jmethodID getMinimumTimestamp;
        jmethodID getMaximumTimestamp;
        jmethodID getUri;
        jmethodID supportsExplicitIDs;
        jmethodID getModificationFlags;
        jmethodID getVisibilityFlags;
        jmethodID hasCache;
        jmethodID clearCache;
        jmethodID getCacheSize;
        jmethodID acquireModifyLock;
        jmethodID releaseModifyLock;
        jmethodID addOnDataStoreContentChangedListener;
        jmethodID removeOnDataStoreContentChangedListener;
        jmethodID dispose;
        struct {
            jclass id;
            jmethodID updateFeature;
        } v4;
    } FeatureDataStore2_class;

    struct
    {
        jclass id;
        jmethodID ctor;
    } NativeDataStoreContentChangedListener_class;

    class CallbackForwarder : public FeatureDataStore2::OnDataStoreContentChangedListener
    {
    public :
        CallbackForwarder(std::set<FeatureDataStore2::OnDataStoreContentChangedListener *> &listeners, RWMutex &mutex) NOTHROWS;
        ~CallbackForwarder() NOTHROWS;
    public :
        void onDataStoreContentChanged(FeatureDataStore2 &dataStore) NOTHROWS override;
    private :
        std::set<FeatureDataStore2::OnDataStoreContentChangedListener *> &listeners;
        RWMutex &mutex;
    };

    bool FeatureDataStore2_init(JNIEnv &env) NOTHROWS;
}

ManagedFeatureDataStore2::ManagedFeatureDataStore2(JNIEnv &env_, jobject impl_) NOTHROWS :
    impl(env_.NewGlobalRef(impl_)),
    version(2u)
{
    static bool clinit = FeatureDataStore2_init(env_);
    if(env_.IsInstanceOf(impl, FeatureDataStore2_class.v4.id))
        version = 4u;
    std::unique_ptr<FeatureDataStore2::OnDataStoreContentChangedListener, void(*)(const FeatureDataStore2::OnDataStoreContentChangedListener *)> ccb(new CallbackForwarder(callbackForwarder.listeners, callbackForwarder.mutex), Memory_deleter_const<FeatureDataStore2::OnDataStoreContentChangedListener, CallbackForwarder>);
    Interop::Java::JNILocalRef mccbpointer(env_, Interop::NewPointer(&env_, std::move(ccb)));
    Interop::Java::JNILocalRef mcb(
            env_,
            env_.NewObject(
                    NativeDataStoreContentChangedListener_class.id,
                    NativeDataStoreContentChangedListener_class.ctor,
                                mccbpointer.get(),
                                nullptr,
                                INTPTR_TO_JLONG(static_cast<FeatureDataStore2 *>(this))));
    env_.CallVoidMethod(impl, FeatureDataStore2_class.addOnDataStoreContentChangedListener, mcb.get());
    callbackForwarder.managed = env_.NewWeakGlobalRef(mcb);
}
ManagedFeatureDataStore2::~ManagedFeatureDataStore2() NOTHROWS
{
    if(impl) {
        LocalJNIEnv env;
        if(callbackForwarder.managed) {
            Interop::Java::JNILocalRef mcb(*env, env->NewLocalRef(callbackForwarder.managed));
            env->CallVoidMethod(impl, FeatureDataStore2_class.removeOnDataStoreContentChangedListener, mcb.get());
            env->DeleteWeakGlobalRef(callbackForwarder.managed);
        }
        {
            WriteLock wlock(callbackForwarder.mutex);
            callbackForwarder.listeners.clear();
        }
        // XXX - disposal is not performed here as ownership is not well-defined
        //env->CallVoidMethod(impl, FeatureDataStore2_class.dispose);
        env->DeleteGlobalRef(impl);
        impl = nullptr;
    }
}
TAKErr ManagedFeatureDataStore2::addOnDataStoreContentChangedListener(OnDataStoreContentChangedListener *l) NOTHROWS
{
    WriteLock wlock(callbackForwarder.mutex);
    callbackForwarder.listeners.insert(l);
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::removeOnDataStoreContentChangedListener(OnDataStoreContentChangedListener *l) NOTHROWS
{
    WriteLock wlock(callbackForwarder.mutex);
    callbackForwarder.listeners.erase(l);
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::getFeature(TAK::Engine::Feature::FeaturePtr_const &cfeature, const int64_t fid) NOTHROWS
{
    TAKErr code(TE_Ok);
    FeatureDataStore2::FeatureQueryParameters cparams;
    cparams.featureIds->add(fid);
    cparams.limit = 1u;
    FeatureCursorPtr result(nullptr, nullptr);
    code = queryFeatures(result, cparams);
    TE_CHECKRETURN_CODE(code);
    code = result->moveToNext();
    if(code == TE_Done)
        return TE_InvalidArg;
    TE_CHECKRETURN_CODE(code);
    const Feature2 *f;
    code = result->get(&f);
    TE_CHECKRETURN_CODE(code);
    cfeature = FeaturePtr_const(new Feature2(*f), Memory_deleter_const<Feature2>);
    return code;
}
TAKErr ManagedFeatureDataStore2::queryFeatures(TAK::Engine::Feature::FeatureCursorPtr &ccursor) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mcursor(*env, env->CallObjectMethod(impl, FeatureDataStore2_class.queryFeatures, nullptr));
    if(env->ExceptionCheck())
        return TE_Err;
    ccursor = FeatureCursorPtr(new Interop::Feature::ManagedFeatureCursor(*env, mcursor), Memory_deleter_const<FeatureCursor2, Interop::Feature::ManagedFeatureCursor>);
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::queryFeatures(TAK::Engine::Feature::FeatureCursorPtr &ccursor, const FeatureQueryParameters &cparams) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, cparams);
    Interop::Java::JNILocalRef mcursor(*env, env->CallObjectMethod(impl, FeatureDataStore2_class.queryFeatures, mparams.get()));
    if(env->ExceptionCheck())
        return TE_Err;
    ccursor = FeatureCursorPtr(new Interop::Feature::ManagedFeatureCursor(*env, mcursor), Memory_deleter_const<FeatureCursor2, Interop::Feature::ManagedFeatureCursor>);
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::queryFeatures(FeatureCursor3Ptr &cursor) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mcursor(*env, env->CallObjectMethod(impl, FeatureDataStore2_class.queryFeatures, nullptr));
    if(env->ExceptionCheck())
        return TE_Err;
    cursor = FeatureCursor3Ptr(new Interop::Feature::ManagedFeatureCursor(*env, mcursor), Memory_deleter_const<FeatureCursor3, Interop::Feature::ManagedFeatureCursor>);
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::queryFeatures(FeatureCursor3Ptr &cursor, const FeatureDataStore2::FeatureQueryParameters &params) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, params);
    Interop::Java::JNILocalRef mcursor(*env, env->CallObjectMethod(impl, FeatureDataStore2_class.queryFeatures, mparams.get()));
    if(env->ExceptionCheck())
        return TE_Err;
    cursor = FeatureCursor3Ptr(new Interop::Feature::ManagedFeatureCursor(*env, mcursor), Memory_deleter_const<FeatureCursor3, Interop::Feature::ManagedFeatureCursor>);
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::queryFeaturesCount(int *value) NOTHROWS
{
    LocalJNIEnv env;
    *value = env->CallIntMethod(impl, FeatureDataStore2_class.queryFeatureSetsCount, nullptr);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::queryFeaturesCount(int *value, const FeatureQueryParameters &cparams) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, cparams);
    *value = env->CallIntMethod(impl, FeatureDataStore2_class.queryFeaturesCount, mparams.get());
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::getFeatureSet(TAK::Engine::Feature::FeatureSetPtr_const &cfeatureSet, const int64_t fsid) NOTHROWS
{
    TAKErr code(TE_Ok);
    FeatureDataStore2::FeatureSetQueryParameters cparams;
    cparams.ids->add(fsid);
    cparams.limit = 1u;
    FeatureSetCursorPtr result(nullptr, nullptr);
    code = queryFeatureSets(result, cparams);
    TE_CHECKRETURN_CODE(code);
    code = result->moveToNext();
    if(code == TE_Done)
        return TE_InvalidArg;
    TE_CHECKRETURN_CODE(code);
    const FeatureSet2 *f;
    code = result->get(&f);
    TE_CHECKRETURN_CODE(code);
    cfeatureSet = FeatureSetPtr_const(new FeatureSet2(*f), Memory_deleter_const<FeatureSet2>);
    return code;
}
TAKErr ManagedFeatureDataStore2::queryFeatureSets(TAK::Engine::Feature::FeatureSetCursorPtr &ccursor) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mcursor(*env, env->CallObjectMethod(impl, FeatureDataStore2_class.queryFeatureSets, nullptr));
    if(env->ExceptionCheck())
        return TE_Err;
    ccursor = FeatureSetCursorPtr(new Interop::Feature::ManagedFeatureSetCursor(*env, mcursor), Memory_deleter_const<FeatureSetCursor2, Interop::Feature::ManagedFeatureSetCursor>);
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::queryFeatureSets(TAK::Engine::Feature::FeatureSetCursorPtr &ccursor, const FeatureSetQueryParameters &cparams) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, cparams);
    Interop::Java::JNILocalRef mcursor(*env, env->CallObjectMethod(impl, FeatureDataStore2_class.queryFeatureSets, mparams.get()));
    if(env->ExceptionCheck())
        return TE_Err;
    ccursor = FeatureSetCursorPtr(new Interop::Feature::ManagedFeatureSetCursor(*env, mcursor), Memory_deleter_const<FeatureSetCursor2, Interop::Feature::ManagedFeatureSetCursor>);
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::queryFeatureSetsCount(int *value) NOTHROWS
{
    LocalJNIEnv env;
    *value = env->CallIntMethod(impl, FeatureDataStore2_class.queryFeatureSetsCount, nullptr);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::queryFeatureSetsCount(int *value, const FeatureSetQueryParameters &cparams) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, cparams);
    *value = env->CallIntMethod(impl, FeatureDataStore2_class.queryFeatureSetsCount, mparams.get());
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::getModificationFlags(int *value) NOTHROWS
{
    LocalJNIEnv env;
    const auto mflags = env->CallIntMethod(impl, FeatureDataStore2_class.getModificationFlags);
    if(env->ExceptionCheck())
        return TE_Err;
    *value = 0;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURESET_DELETE)
        (*value) |= FeatureDataStore2::MODIFY_FEATURESET_DELETE;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURESET_DISPLAY_THRESHOLDS)
        (*value) |= FeatureDataStore2::MODIFY_FEATURESET_DISPLAY_THRESHOLDS;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURESET_FEATURE_DELETE)
        (*value) |= FeatureDataStore2::MODIFY_FEATURESET_FEATURE_DELETE;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURESET_FEATURE_INSERT)
        (*value) |= FeatureDataStore2::MODIFY_FEATURESET_FEATURE_INSERT;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURESET_FEATURE_UPDATE)
        (*value) |= FeatureDataStore2::MODIFY_FEATURESET_FEATURE_UPDATE;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURESET_INSERT)
        (*value) |= FeatureDataStore2::MODIFY_FEATURESET_INSERT;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURESET_NAME)
        (*value) |= FeatureDataStore2::MODIFY_FEATURESET_NAME;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURESET_UPDATE)
        (*value) |= FeatureDataStore2::MODIFY_FEATURESET_UPDATE;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURE_ATTRIBUTES)
        (*value) |= FeatureDataStore2::MODIFY_FEATURE_ATTRIBUTES;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURE_GEOMETRY)
        (*value) |= FeatureDataStore2::MODIFY_FEATURE_GEOMETRY;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURE_NAME)
        (*value) |= FeatureDataStore2::MODIFY_FEATURE_NAME;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_MODIFY_FEATURE_STYLE)
        (*value) |= FeatureDataStore2::MODIFY_FEATURE_STYLE;
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::beginBulkModification() NOTHROWS
{
    // no-op, interface for Java is sufficiently different
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::endBulkModification(const bool successful) NOTHROWS
{
    // no-op, interface for Java is sufficiently different
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::isInBulkModification(bool *value) NOTHROWS
{
    // no-op, interface for Java is sufficiently different
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::insertFeatureSet(TAK::Engine::Feature::FeatureSetPtr_const *featureSet, const char *provider, const char *type, const char *name, const double minResolution, const double maxResolution) NOTHROWS
{
    LocalJNIEnv env;
    FeatureSet2 cfeatureset(FeatureDataStore2::FEATURESET_ID_NONE, provider, type, name, minResolution, maxResolution, FeatureDataStore2::FEATURESET_VERSION_NONE);
    Interop::Java::JNILocalRef mfeatureset(*env, nullptr);
    Interop::Feature::Interop_marshal(mfeatureset, *env, cfeatureset);
    const auto fsid = env->CallLongMethod(impl, FeatureDataStore2_class.insertFeatureSet, mfeatureset.get());
    if(env->ExceptionCheck())
        return TE_Err;
    return !!featureSet ? getFeatureSet(*featureSet, fsid) : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::updateFeatureSet(const int64_t fsid, const char *name) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mname(*env, env->NewStringUTF(name));
    env->CallVoidMethod(impl, FeatureDataStore2_class.updateFeatureSet__JLString, (jlong)fsid, mname.get());
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::updateFeatureSet(const int64_t fsid, const double minResolution, const double maxResolution) NOTHROWS
{
    LocalJNIEnv env;
    env->CallVoidMethod(impl, FeatureDataStore2_class.updateFeatureSet__JDD, (jlong)fsid, minResolution, maxResolution);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::updateFeatureSet(const int64_t fsid, const char *name, const double minResolution, const double maxResolution) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mname(*env, env->NewStringUTF(name));
    env->CallVoidMethod(impl, FeatureDataStore2_class.updateFeatureSet__JLStringDD, (jlong)fsid, mname.get(), minResolution, maxResolution);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::deleteFeatureSet(const int64_t fsid) NOTHROWS
{
    LocalJNIEnv env;
    env->CallVoidMethod(impl, FeatureDataStore2_class.deleteFeatureSet, (jlong)fsid);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    } else {
        return TE_Ok;
    }
}
TAKErr ManagedFeatureDataStore2::deleteAllFeatureSets() NOTHROWS
{
    LocalJNIEnv env;
    env->CallVoidMethod(impl, FeatureDataStore2_class.deleteFeatureSets, nullptr);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    } else {
        return TE_Ok;
    }
}
TAKErr ManagedFeatureDataStore2::insertFeature(TAK::Engine::Feature::FeaturePtr_const *feature, const int64_t fsid, const char *name, const atakmap::feature::Geometry &geom, const TAK::Engine::Feature::AltitudeMode altitudeMode, const double extrude, const atakmap::feature::Style *style, const atakmap::util::AttributeSet &attributes) NOTHROWS
{
    Traits traits;
    traits.altitudeMode = altitudeMode;
    traits.extrude = extrude;
    return insertFeature(feature, fsid, name, geom, style, attributes, traits);
}
TAKErr ManagedFeatureDataStore2::insertFeature(FeaturePtr_const *feature, const int64_t fsid,
                                               const char *name,
                                               const atakmap::feature::Geometry &geom,
                                               const atakmap::feature::Style *style,
                                               const atakmap::util::AttributeSet &attributes,
                                               const Traits &traits) NOTHROWS
{
    LocalJNIEnv env;
    Feature2 cfeature(FeatureDataStore2::FEATURE_ID_NONE, fsid, name, geom, style, attributes, traits, 0, FeatureDataStore2::FEATURE_VERSION_NONE);
    Interop::Java::JNILocalRef mfeature(*env, nullptr);
    Interop::Feature::Interop_marshal(mfeature, *env, cfeature);
    const auto fid = env->CallLongMethod(impl, FeatureDataStore2_class.insertFeature__LFeature, mfeature.get());
    if(env->ExceptionCheck())
        return TE_Err;
    return !!feature ? getFeature(*feature, fid) : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::updateFeature(const int64_t fid, const Traits &traits) NOTHROWS
{
    if (version >= 4) {
        LocalJNIEnv env;
        Java::JNILocalRef mtraits(*env, nullptr);
        TAKErr code = Interop_marshal(mtraits, *env, traits);
        TE_CHECKRETURN_CODE(code)

        env->CallVoidMethod(impl, FeatureDataStore2_class.v4.updateFeature,
                            (jlong)fid,
                            com_atakmap_map_layer_feature_FeatureDataStore4_PROPERTY_FEATURE_TRAITS,
                            nullptr, //name
                            nullptr, //geometry
                            nullptr, //style
                            nullptr, //attributes
                            com_atakmap_map_layer_feature_FeatureDataStore2_UPDATE_ATTRIBUTES_ADD_OR_REPLACE,
                            mtraits.get());
        return ATAKMapEngineJNI_ExceptionCheck(*env, true) ? TE_Err : TE_Ok;
    } else {
        return ManagedFeatureDataStore2::updateFeature(fid, traits.altitudeMode, traits.extrude);
    }
}
TAKErr ManagedFeatureDataStore2::updateFeature(const int64_t fid, const char *name) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mname(*env, env->NewStringUTF(name));
    env->CallVoidMethod(impl, FeatureDataStore2_class.updateFeature,
                        (jlong)fid,
                        com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_NAME,
                        mname.get(),
                        nullptr,
                        nullptr,
                        nullptr,
                        com_atakmap_map_layer_feature_FeatureDataStore2_UPDATE_ATTRIBUTES_ADD_OR_REPLACE);
    return ATAKMapEngineJNI_ExceptionCheck(*env, true) ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::updateFeature(const int64_t fid, const atakmap::feature::Geometry &geom) NOTHROWS
{
    LocalJNIEnv env;
    Geometry2Ptr geom2(nullptr, nullptr);
    LegacyAdapters_adapt(geom2, geom);
    Interop::Java::JNILocalRef mgeom(*env, nullptr);
    Interop::Feature::Interop_marshal(mgeom, *env, std::move(geom2));
    env->CallVoidMethod(impl, FeatureDataStore2_class.updateFeature,
                        (jlong)fid,
                        com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_GEOMETRY,
                        nullptr,
                        mgeom.get(),
                        nullptr,
                        nullptr,
                        com_atakmap_map_layer_feature_FeatureDataStore2_UPDATE_ATTRIBUTES_ADD_OR_REPLACE);
    return ATAKMapEngineJNI_ExceptionCheck(*env, true) ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::updateFeature(const int64_t fid, const atakmap::feature::Geometry &geom, const TAK::Engine::Feature::AltitudeMode altitudeMode, const double extrude) NOTHROWS
{
    return TE_NotImplemented;
}
TAKErr ManagedFeatureDataStore2::updateFeature(const int64_t fid, const TAK::Engine::Feature::AltitudeMode altitudeMode, const double extrude) NOTHROWS
{
    return TE_NotImplemented;
}
TAKErr ManagedFeatureDataStore2::updateFeature(const int64_t fid, const atakmap::feature::Style *style) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mstyle(*env, nullptr);
    if(style)
        Interop::Feature::Interop_marshal(mstyle, *env, *style);
    env->CallVoidMethod(impl, FeatureDataStore2_class.updateFeature,
                        (jlong)fid,
                        com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_STYLE,
                        nullptr,
                        nullptr,
                        mstyle.get(),
                        nullptr,
                        com_atakmap_map_layer_feature_FeatureDataStore2_UPDATE_ATTRIBUTES_ADD_OR_REPLACE);
    return ATAKMapEngineJNI_ExceptionCheck(*env, true) ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::updateFeature(const int64_t fid, const atakmap::util::AttributeSet &attributes) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mattrs(*env, nullptr);
    Interop::Feature::Interop_marshal(mattrs, *env, attributes);
    env->CallVoidMethod(impl, FeatureDataStore2_class.updateFeature,
                        (jlong)fid,
                        com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_ATTRIBUTES,
                        nullptr,
                        nullptr,
                        nullptr,
                        mattrs.get(),
                        com_atakmap_map_layer_feature_FeatureDataStore2_UPDATE_ATTRIBUTES_SET);
    return ATAKMapEngineJNI_ExceptionCheck(*env, true) ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::updateFeature(const int64_t fid, const char *name, const atakmap::feature::Geometry &geom, const atakmap::feature::Style *style, const atakmap::util::AttributeSet &attributes) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mname(*env, env->NewStringUTF(name));
    Geometry2Ptr geom2(nullptr, nullptr);
    LegacyAdapters_adapt(geom2, geom);
    Interop::Java::JNILocalRef mgeom(*env, nullptr);
    Interop::Feature::Interop_marshal(mgeom, *env, std::move(geom2));
    Interop::Java::JNILocalRef mstyle(*env, nullptr);
    if(style)
        Interop::Feature::Interop_marshal(mstyle, *env, *style);
    Interop::Java::JNILocalRef mattrs(*env, nullptr);
    Interop::Feature::Interop_marshal(mattrs, *env, attributes);
    env->CallVoidMethod(impl, FeatureDataStore2_class.updateFeature,
                        (jlong)fid,
                        com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_NAME|
                        com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_GEOMETRY|
                        com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_STYLE|
                        com_atakmap_map_layer_feature_FeatureDataStore2_PROPERTY_FEATURE_ATTRIBUTES,
                        mname.get(),
                        mgeom.get(),
                        mstyle.get(),
                        mattrs.get(),
                        com_atakmap_map_layer_feature_FeatureDataStore2_UPDATE_ATTRIBUTES_SET);
    return ATAKMapEngineJNI_ExceptionCheck(*env, true) ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::deleteFeature(const int64_t fid) NOTHROWS
{
    LocalJNIEnv env;
    env->CallVoidMethod(impl, FeatureDataStore2_class.deleteFeature, (jlong)fid);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    } else {
        return TE_Ok;
    }
}
TAKErr ManagedFeatureDataStore2::deleteAllFeatures(const int64_t fsid) NOTHROWS
{
    LocalJNIEnv env;
    FeatureDataStore2::FeatureQueryParameters cparams;
    cparams.featureSetIds->add(fsid);
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, cparams);
    env->CallVoidMethod(impl, FeatureDataStore2_class.deleteFeatures, mparams.get());
    return env->ExceptionCheck() ?  TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::getVisibilitySettingsFlags(int *value) NOTHROWS
{
    LocalJNIEnv env;
    const auto mflags = env->CallIntMethod(impl, FeatureDataStore2_class.getVisibilityFlags);
    if(env->ExceptionCheck())
        return TE_Err;
    *value = 0;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_VISIBILITY_SETTINGS_FEATURE)
        (*value) |= FeatureDataStore2::VISIBILITY_SETTINGS_FEATURE;
    if(mflags&com_atakmap_map_layer_feature_FeatureDataStore2_VISIBILITY_SETTINGS_FEATURESET)
        (*value) |= FeatureDataStore2::VISIBILITY_SETTINGS_FEATURESET;
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::setFeatureVisible(const int64_t fid, const bool visible) NOTHROWS
{
    LocalJNIEnv env;
    env->CallVoidMethod(impl, FeatureDataStore2_class.setFeatureVisible, fid, visible);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::setFeaturesVisible(const FeatureQueryParameters &cparams, const bool visible) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, cparams);
    env->CallVoidMethod(impl, FeatureDataStore2_class.setFeaturesVisible, mparams.get());
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::isFeatureVisible(bool *value, const int64_t fid) NOTHROWS
{
    LocalJNIEnv env;
    FeatureDataStore2::FeatureQueryParameters cparams;
    cparams.featureIds->add(fid);
    cparams.limit = 1u;
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, cparams);
    *value = env->CallIntMethod(impl, FeatureDataStore2_class.queryFeaturesCount, mparams.get());
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::setFeatureSetVisible(const int64_t setId, const bool visible) NOTHROWS
{
    LocalJNIEnv env;
    env->CallVoidMethod(impl, FeatureDataStore2_class.setFeatureSetVisible, setId, visible);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::setFeatureSetsVisible(const FeatureSetQueryParameters &cparams, const bool visible) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, cparams);
    env->CallVoidMethod(impl, FeatureDataStore2_class.setFeatureSetsVisible, mparams.get(), visible);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::setFeatureSetsReadOnly(const FeatureSetQueryParameters &paramsRef, const bool readOnly) NOTHROWS
{
    // no comparable Java API
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::isFeatureSetVisible(bool *value, const int64_t setId) NOTHROWS
{
    LocalJNIEnv env;
    FeatureDataStore2::FeatureSetQueryParameters cparams;
    cparams.ids->add(setId);
    cparams.limit = 1u;
    Interop::Java::JNILocalRef mparams(*env, nullptr);
    Interop::Feature::Interop_marshal(mparams, *env, cparams);
    *value = env->CallIntMethod(impl, FeatureDataStore2_class.queryFeatureSetsCount, mparams.get());
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureDataStore2::setFeatureSetReadOnly(const int64_t fsid, const bool readOnly) NOTHROWS
{
    // no comparable Java API
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::isFeatureSetReadOnly(bool *value, const int64_t fsid) NOTHROWS
{
    // no comparable Java API
    *value = false;
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::isFeatureReadOnly(bool *value, const int64_t fsid) NOTHROWS
{
    // no comparable Java API
    *value = false;
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::isAvailable(bool *value) NOTHROWS
{
    // no comparable Java API
    *value = true;
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::refresh() NOTHROWS
{
    // no comparable Java API
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::getUri(TAK::Engine::Port::String &value) NOTHROWS
{
    LocalJNIEnv env;
    Interop::Java::JNILocalRef muri(*env, env->CallObjectMethod(impl, FeatureDataStore2_class.getUri));
    Interop::JNIStringUTF_get(value, *env, muri);
    return TE_Ok;
}
TAKErr ManagedFeatureDataStore2::close() NOTHROWS
{
    LocalJNIEnv env;
    env->CallVoidMethod(impl, FeatureDataStore2_class.dispose);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    } else {
        return TE_Ok;
    }
}

namespace
{
    CallbackForwarder::CallbackForwarder(std::set<FeatureDataStore2::OnDataStoreContentChangedListener *> &listeners_, RWMutex &mutex_) NOTHROWS :
        listeners(listeners_),
        mutex(mutex_)
    {}
    CallbackForwarder::~CallbackForwarder() NOTHROWS
    {}
    void CallbackForwarder::onDataStoreContentChanged(FeatureDataStore2 &dataStore) NOTHROWS
    {
        ReadLock rlock(mutex);
        for(auto &l : listeners)
            l->onDataStoreContentChanged(dataStore);
    }

    bool FeatureDataStore2_init(JNIEnv &env) NOTHROWS
    {
        FeatureDataStore2_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore2");
        FeatureDataStore2_class.queryFeatures = env.GetMethodID(FeatureDataStore2_class.id, "queryFeatures", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters;)Lcom/atakmap/map/layer/feature/FeatureCursor;");
        FeatureDataStore2_class.queryFeaturesCount = env.GetMethodID(FeatureDataStore2_class.id, "queryFeaturesCount", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters;)I");
        FeatureDataStore2_class.queryFeatureSets = env.GetMethodID(FeatureDataStore2_class.id, "queryFeatureSets", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureSetQueryParameters;)Lcom/atakmap/map/layer/feature/FeatureSetCursor;");
        FeatureDataStore2_class.queryFeatureSetsCount = env.GetMethodID(FeatureDataStore2_class.id, "queryFeatureSetsCount", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureSetQueryParameters;)I");
        FeatureDataStore2_class.insertFeature__JJLFeatureDefinition2J = env.GetMethodID(FeatureDataStore2_class.id, "insertFeature", "(JJLcom/atakmap/map/layer/feature/FeatureDefinition2;J)J");
        FeatureDataStore2_class.insertFeature__LFeature = env.GetMethodID(FeatureDataStore2_class.id, "insertFeature", "(Lcom/atakmap/map/layer/feature/Feature;)J");
        FeatureDataStore2_class.insertFeatures = env.GetMethodID(FeatureDataStore2_class.id, "insertFeatures", "(Lcom/atakmap/map/layer/feature/FeatureCursor;)V");
        FeatureDataStore2_class.insertFeatureSet = env.GetMethodID(FeatureDataStore2_class.id, "insertFeatureSet", "(Lcom/atakmap/map/layer/feature/FeatureSet;)J");
        FeatureDataStore2_class.insertFeatureSets = env.GetMethodID(FeatureDataStore2_class.id, "insertFeatureSets", "(Lcom/atakmap/map/layer/feature/FeatureSetCursor;)V");
        FeatureDataStore2_class.updateFeature = env.GetMethodID(FeatureDataStore2_class.id, "updateFeature", "(JILjava/lang/String;Lcom/atakmap/map/layer/feature/geometry/Geometry;Lcom/atakmap/map/layer/feature/style/Style;Lcom/atakmap/map/layer/feature/AttributeSet;I)V");
        FeatureDataStore2_class.updateFeatureSet__JLStringDD = env.GetMethodID(FeatureDataStore2_class.id, "updateFeatureSet", "(JLjava/lang/String;DD)V");
        FeatureDataStore2_class.updateFeatureSet__JLString = env.GetMethodID(FeatureDataStore2_class.id, "updateFeatureSet", "(JLjava/lang/String;)V");
        FeatureDataStore2_class.updateFeatureSet__JDD = env.GetMethodID(FeatureDataStore2_class.id, "updateFeatureSet", "(JDD)V");
        FeatureDataStore2_class.deleteFeature = env.GetMethodID(FeatureDataStore2_class.id, "deleteFeature", "(J)V");
        FeatureDataStore2_class.deleteFeatures = env.GetMethodID(FeatureDataStore2_class.id, "deleteFeatures", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters;)V");
        FeatureDataStore2_class.deleteFeatureSet = env.GetMethodID(FeatureDataStore2_class.id, "deleteFeatureSet", "(J)V");
        FeatureDataStore2_class.deleteFeatureSets = env.GetMethodID(FeatureDataStore2_class.id, "deleteFeatureSets", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureSetQueryParameters;)V");
        FeatureDataStore2_class.setFeatureVisible = env.GetMethodID(FeatureDataStore2_class.id, "setFeatureVisible", "(JZ)V");
        FeatureDataStore2_class.setFeaturesVisible = env.GetMethodID(FeatureDataStore2_class.id, "setFeaturesVisible", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureQueryParameters;Z)V");
        FeatureDataStore2_class.setFeatureSetVisible = env.GetMethodID(FeatureDataStore2_class.id, "setFeatureSetVisible", "(JZ)V");
        FeatureDataStore2_class.setFeatureSetVisible = env.GetMethodID(FeatureDataStore2_class.id, "setFeatureSetsVisible", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$FeatureSetQueryParameters;Z)V");
        FeatureDataStore2_class.hasTimeReference = env.GetMethodID(FeatureDataStore2_class.id, "hasTimeReference", "()Z");
        FeatureDataStore2_class.getMinimumTimestamp = env.GetMethodID(FeatureDataStore2_class.id, "getMinimumTimestamp", "()J");
        FeatureDataStore2_class.getMaximumTimestamp = env.GetMethodID(FeatureDataStore2_class.id, "getMaximumTimestamp", "()J");
        FeatureDataStore2_class.getUri = env.GetMethodID(FeatureDataStore2_class.id, "getUri", "()Ljava/lang/String;");
        FeatureDataStore2_class.supportsExplicitIDs = env.GetMethodID(FeatureDataStore2_class.id, "supportsExplicitIDs", "()Z");
        FeatureDataStore2_class.getModificationFlags = env.GetMethodID(FeatureDataStore2_class.id, "getModificationFlags", "()I");
        FeatureDataStore2_class.getVisibilityFlags = env.GetMethodID(FeatureDataStore2_class.id, "getVisibilityFlags", "()I");
        FeatureDataStore2_class.hasCache = env.GetMethodID(FeatureDataStore2_class.id, "hasCache", "()Z");
        FeatureDataStore2_class.clearCache = env.GetMethodID(FeatureDataStore2_class.id, "clearCache", "()V");
        FeatureDataStore2_class.getCacheSize = env.GetMethodID(FeatureDataStore2_class.id, "getCacheSize", "()J");
        FeatureDataStore2_class.acquireModifyLock = env.GetMethodID(FeatureDataStore2_class.id, "acquireModifyLock", "(Z)V");
        FeatureDataStore2_class.releaseModifyLock = env.GetMethodID(FeatureDataStore2_class.id, "releaseModifyLock", "()V");
        FeatureDataStore2_class.addOnDataStoreContentChangedListener = env.GetMethodID(FeatureDataStore2_class.id, "addOnDataStoreContentChangedListener", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$OnDataStoreContentChangedListener;)V");
        FeatureDataStore2_class.removeOnDataStoreContentChangedListener = env.GetMethodID(FeatureDataStore2_class.id, "removeOnDataStoreContentChangedListener", "(Lcom/atakmap/map/layer/feature/FeatureDataStore2$OnDataStoreContentChangedListener;)V");
        FeatureDataStore2_class.dispose = env.GetMethodID(FeatureDataStore2_class.id, "dispose", "()V");

        FeatureDataStore2_class.v4.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDataStore4");
        FeatureDataStore2_class.v4.updateFeature = env.GetMethodID(FeatureDataStore2_class.v4.id, "updateFeature", "(JILjava/lang/String;Lcom/atakmap/map/layer/feature/geometry/Geometry;Lcom/atakmap/map/layer/feature/style/Style;Lcom/atakmap/map/layer/feature/AttributeSet;ILcom/atakmap/map/layer/feature/Feature$Traits;)V");

        NativeDataStoreContentChangedListener_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/NativeDataStoreContentChangedListener");
        NativeDataStoreContentChangedListener_class.ctor = env.GetMethodID(NativeDataStoreContentChangedListener_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;J)V");

        return true;
    }
}
