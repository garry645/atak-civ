//
// Created by Geo Dev on 5/4/23.
//

#include "interop/feature/ManagedFeatureCursor.h"

#include <feature/FeatureDataStore2.h>

#include "interop/db/Interop.h"

using namespace TAKEngineJNI::Interop::Feature;

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Util;

namespace
{
    struct {
        jclass id;
        jmethodID getId;
        jmethodID getFeatureSetId;
        jmethodID getVersion;
    } FeatureCursor_class;

    bool FeatureCursor_init(JNIEnv &env) NOTHROWS;
}

ManagedFeatureCursor::ManagedFeatureCursor(JNIEnv &env, jobject impl) NOTHROWS :
    cimpl(env, impl),
    row(nullptr, nullptr)
{
    static bool clinit = FeatureCursor_init(env);
}
ManagedFeatureCursor::~ManagedFeatureCursor() NOTHROWS
{
    LocalJNIEnv env;
    TAKEngineJNI::Interop::DB::RowIterator_close(env, cimpl.impl);
}

TAKErr ManagedFeatureCursor::getTimestamp(int64_t *value) NOTHROWS
{
    return cimpl.getTimestamp(value);
}
TAKErr ManagedFeatureCursor::getRawGeometry(FeatureDefinition2::RawData *value) NOTHROWS
{
    return cimpl.getRawGeometry(value);
}
FeatureDefinition2::GeometryEncoding ManagedFeatureCursor::getGeomCoding() NOTHROWS
{
    return cimpl.getGeomCoding();
}
TAKErr ManagedFeatureCursor::getName(const char **value) NOTHROWS
{
    return cimpl.getName(value);
}
AltitudeMode ManagedFeatureCursor::getAltitudeMode() NOTHROWS
{
    return cimpl.getAltitudeMode();
}
double ManagedFeatureCursor::getExtrude() NOTHROWS
{
    return cimpl.getExtrude();
}
Traits ManagedFeatureCursor::getTraits() NOTHROWS
{
    return cimpl.getTraits();
}
FeatureDefinition2::StyleEncoding ManagedFeatureCursor::getStyleCoding() NOTHROWS
{
    return cimpl.getStyleCoding();
}
TAKErr ManagedFeatureCursor::getRawStyle(TAK::Engine::Feature::FeatureDefinition2::RawData *value) NOTHROWS
{
    return cimpl.getRawStyle(value);
}
TAKErr ManagedFeatureCursor::getAttributes(const atakmap::util::AttributeSet **value) NOTHROWS
{
    return cimpl.getAttributes(value);
}
TAKErr ManagedFeatureCursor::get(const Feature2 **value) NOTHROWS
{
    if(!row.get()) {
        int64_t fid = FeatureDataStore2::FEATURE_ID_NONE;
        getId(&fid);
        int64_t fsid = FeatureDataStore2::FEATURESET_ID_NONE;
        getFeatureSetId(&fsid);
        int64_t version = FeatureDataStore2::FEATURE_VERSION_NONE;
        getVersion(&version);
        Feature_create(row, fid, fsid, *this, version);
    }
    *value = row.get();
    return TE_Ok;
}
TAKErr ManagedFeatureCursor::getId(int64_t *value) NOTHROWS
{
    LocalJNIEnv env;
    *value = env->CallLongMethod(cimpl.impl, FeatureCursor_class.getId);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureCursor::getFeatureSetId(int64_t *value) NOTHROWS
{
    LocalJNIEnv env;
    *value = env->CallLongMethod(cimpl.impl, FeatureCursor_class.getFeatureSetId);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureCursor::getVersion(int64_t *value) NOTHROWS
{
    LocalJNIEnv env;
    *value = env->CallLongMethod(cimpl.impl, FeatureCursor_class.getVersion);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ManagedFeatureCursor::moveToNext() NOTHROWS
{
    LocalJNIEnv env;
    cimpl.reset();
    row.reset();
    return TAKEngineJNI::Interop::DB::RowIterator_moveToNext(env, cimpl.impl);
}

namespace
{
    bool FeatureCursor_init(JNIEnv &env) NOTHROWS
    {
        FeatureCursor_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureCursor");
        FeatureCursor_class.getId = env.GetMethodID(FeatureCursor_class.id, "getId", "()J");
        FeatureCursor_class.getFeatureSetId = env.GetMethodID(FeatureCursor_class.id, "getFsid", "()J");
        FeatureCursor_class.getVersion = env.GetMethodID(FeatureCursor_class.id, "getVersion", "()J");

        return true;
    }
}
