//
// Created by Geo Dev on 5/2/23.
//

#include "interop/feature/ManagedFeatureDefinition.h"

#include <feature/Geometry2.h>
#include <feature/LegacyAdapters.h>

#include "com_atakmap_map_layer_feature_FeatureDefinition.h"
#include "interop/JNIByteArray.h"
#include "interop/JNIStringUTF.h"
#include "interop/feature/Interop.h"
#include "interop/java/JNILocalRef.h"

using namespace TAKEngineJNI::Interop::Feature;

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

namespace
{
    struct
    {
        jclass id;
        jmethodID getRawGeom;
        jmethodID getGeomCoding;
        jmethodID getName;
        jmethodID getStyleCoding;
        jmethodID getRawStyle;
        jmethodID getAttributes;
        struct {
            jclass id;
            jmethodID getTimestamp;
        } v2;
        struct {
            jclass id;
            jmethodID getAltitudeMode;
            jmethodID getExtrude;
        } v3;
        struct {
            jclass id;
            jmethodID getTraits;
        } v4;
    } FeatureDefinition_class;

    bool FeatureDefinition2_class_init(JNIEnv &env) NOTHROWS;
}

ManagedFeatureDefinition::ManagedFeatureDefinition(JNIEnv &env_, jobject impl_) NOTHROWS :
        impl(env_.NewGlobalRef(impl_)),
        feature(NULL, NULL),
        version(1u)
{
    static bool clinit = FeatureDefinition2_class_init(env_);
    if(env_.IsInstanceOf(impl, FeatureDefinition_class.v4.id))
        version = 4u;
    else if(env_.IsInstanceOf(impl, FeatureDefinition_class.v3.id))
        version = 3u;
    else if(env_.IsInstanceOf(impl, FeatureDefinition_class.v2.id))
        version = 2u;
}
ManagedFeatureDefinition::~ManagedFeatureDefinition() NOTHROWS
{
    LocalJNIEnv env;
    if(impl) {
        env->DeleteGlobalRef(impl);
        impl = NULL;
    }

    reset();
}

void ManagedFeatureDefinition::reset() NOTHROWS
{
    LocalJNIEnv env;
    rawGeom.valid = false;
    rawGeom.ref.geom.reset();
    if(rawGeom.ref.managed) {
        rawGeom.ref.text = JNIStringUTF();
        rawGeom.ref.binary = JNIByteArray();
        env->DeleteGlobalRef(rawGeom.ref.managed);
        rawGeom.ref.managed = nullptr;
    }
    rawGeom.value = FeatureDefinition2::RawData();
    rawStyle.valid = false;
    rawStyle.ref.style = nullptr;
    if(rawStyle.ref.managed) {
        rawStyle.ref.text = JNIStringUTF();
        env->DeleteGlobalRef(rawStyle.ref.managed);
        rawStyle.ref.managed = nullptr;
    }
    rawStyle.value = FeatureDefinition2::RawData();
    name.valid = false;
    if(name.ref.managed) {
        name.ref.text = JNIStringUTF();
        env->DeleteGlobalRef(name.ref.managed);
        name.ref.managed = nullptr;
    }
    attributes.valid = false;
    attributes.value = nullptr;
    if(attributes.managed) {
        env->DeleteGlobalRef(attributes.managed);
        attributes.managed = nullptr;
    }
    feature.reset();
}
TAKErr ManagedFeatureDefinition::getTimestamp(int64_t *value) NOTHROWS
{
    *value = TE_TIMESTAMP_NONE;
    if(version >= 2u) {
        LocalJNIEnv env;
        *value = env->CallLongMethod(impl, FeatureDefinition_class.v2.getTimestamp);
    }
    return TE_Ok;
}
TAKErr ManagedFeatureDefinition::getRawGeometry(RawData *value) NOTHROWS
{
    if(!rawGeom.valid) {
        LocalJNIEnv env;
        if(env->ExceptionCheck())
            return TE_Err;
        jint geomCoding = env->CallIntMethod(impl, FeatureDefinition_class.getGeomCoding);
        if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_GEOM_WKT) {
            Java::JNILocalRef result(*env, (jstring)env->CallObjectMethod(impl, FeatureDefinition_class.getRawGeom));
            if(env->ExceptionCheck())
                return TE_Err;
            if(result) {
                rawGeom.ref.managed = env->NewGlobalRef(result);
                rawGeom.ref.text = JNIStringUTF(*env, (jstring)rawGeom.ref.managed);
                rawGeom.value.text = rawGeom.ref.text;
            }
        } else if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_GEOM_WKB) {
            Java::JNILocalRef result(*env, (jbyteArray)env->CallObjectMethod(impl, FeatureDefinition_class.getRawGeom));
            if(env->ExceptionCheck())
                return TE_Err;
            if(result) {
                rawGeom.ref.managed = env->NewGlobalRef(result);
                rawGeom.ref.binary = JNIByteArray(*env, (jbyteArray)rawGeom.ref.managed, JNI_ABORT);
                rawGeom.value.binary.len = rawGeom.ref.binary.length();
                rawGeom.value.binary.value = reinterpret_cast<const uint8_t *>((const jbyte *)rawGeom.ref.binary);
            }
        } else if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_GEOM_SPATIALITE_BLOB) {
            Java::JNILocalRef result(*env, (jbyteArray)env->CallObjectMethod(impl, FeatureDefinition_class.getRawGeom));
            if(env->ExceptionCheck())
                return TE_Err;
            if(result) {
                rawGeom.ref.managed = env->NewGlobalRef(result);
                rawGeom.ref.binary = JNIByteArray(*env, (jbyteArray)rawGeom.ref.managed, JNI_ABORT);
                rawGeom.value.binary.len = rawGeom.ref.binary.length();
                rawGeom.value.binary.value = reinterpret_cast<const uint8_t *>((const jbyte *)rawGeom.ref.binary);
            }
        } else if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_GEOM_ATAK_GEOMETRY) {
            Java::JNILocalRef result(*env, env->CallObjectMethod(impl, FeatureDefinition_class.getRawGeom));
            if(env->ExceptionCheck())
                return TE_Err;
            if(result) {
                Geometry2 *cgeom;
                TAKErr code = Interop_get(&cgeom, env, result);
                TE_CHECKRETURN_CODE(code);
                code = LegacyAdapters_adapt(rawGeom.ref.geom, *cgeom);
                TE_CHECKRETURN_CODE(code);
                rawGeom.value.object = rawGeom.ref.geom.get();
            }
        } else {
            return TE_Err;
        }
        rawGeom.valid = true;
    }
    *value = rawGeom.value;
    return TE_Ok;
}
FeatureDefinition2::GeometryEncoding ManagedFeatureDefinition::getGeomCoding() NOTHROWS
{
    LocalJNIEnv env;
    jint geomCoding = env->CallIntMethod(impl, FeatureDefinition_class.getGeomCoding);
    if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_GEOM_WKT) {
        return FeatureDefinition2::GeomWkt;
    } else if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_GEOM_WKB) {
        return FeatureDefinition2::GeomWkb;
    } else if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_GEOM_SPATIALITE_BLOB) {
        return FeatureDefinition2::GeomBlob;
    } else if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_GEOM_ATAK_GEOMETRY) {
        return FeatureDefinition2::GeomGeometry;
    } else {
        return FeatureDefinition2::GeomGeometry;
    }
}
TAKErr ManagedFeatureDefinition::getName(const char **value) NOTHROWS
{
    if(!name.valid) {
        LocalJNIEnv env;
        Java::JNILocalRef result(*env, (jstring)env->CallObjectMethod(impl, FeatureDefinition_class.getName));
        if(env->ExceptionCheck())
            return TE_Err;
        if(result) {
            name.ref.managed = env->NewGlobalRef(result);
            name.ref.text = JNIStringUTF(*env, (jstring)name.ref.managed);
        }
        name.valid = true;
    }

    *value = name.ref.text;
    return TE_Ok;
}
TAK::Engine::Feature::AltitudeMode ManagedFeatureDefinition::getAltitudeMode() NOTHROWS
{
    TAK::Engine::Feature::AltitudeMode caltMode = TAK::Engine::Feature::AltitudeMode::TEAM_ClampToGround;
    if(version >= 3u) {
        LocalJNIEnv env;
        Java::JNILocalRef maltMode(*env, env->CallObjectMethod(impl, FeatureDefinition_class.v3.getAltitudeMode));
        Interop_marshal(caltMode, *env, maltMode);
    }
    return caltMode;
}
double ManagedFeatureDefinition::getExtrude() NOTHROWS
{
    double extrude = 0.0;
    if(version >= 3u) {
        LocalJNIEnv env;
        extrude = env->CallDoubleMethod(impl, FeatureDefinition_class.v3.getExtrude);
    }
    return extrude;
}

Traits ManagedFeatureDefinition::getTraits() NOTHROWS
{
    Traits traits;
    if (version == 3u)
    {
        traits.altitudeMode = getAltitudeMode();
        traits.extrude = getExtrude();
    } else if (version >= 4)
    {
        LocalJNIEnv env;
        Java::JNILocalRef mtraits(*env, env->CallObjectMethod(impl, FeatureDefinition_class.v4.getTraits));
        TAKEngineJNI::Interop::Feature::Interop_marshal(traits, *env, mtraits);
    }
    return traits;
}

FeatureDefinition2::StyleEncoding ManagedFeatureDefinition::getStyleCoding() NOTHROWS
{
    LocalJNIEnv env;
    jint geomCoding = env->CallIntMethod(impl, FeatureDefinition_class.getStyleCoding);
    if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_STYLE_OGR) {
        return FeatureDefinition2::StyleOgr;
    } else if(geomCoding == com_atakmap_map_layer_feature_FeatureDefinition_STYLE_ATAK_STYLE) {
        return FeatureDefinition2::StyleStyle;
    } else {
        return FeatureDefinition2::StyleStyle;
    }
}
TAKErr ManagedFeatureDefinition::getRawStyle(RawData *value) NOTHROWS
{
    TAKErr code(TE_Ok);
    if(!rawStyle.valid) {
        LocalJNIEnv env;
        if(env->ExceptionCheck())
            return TE_Err;
        jint styleCoding = env->CallIntMethod(impl, FeatureDefinition_class.getStyleCoding);
        if(styleCoding == com_atakmap_map_layer_feature_FeatureDefinition_STYLE_OGR) {
            Java::JNILocalRef result(*env,  (jstring)env->CallObjectMethod(impl, FeatureDefinition_class.getRawStyle));
            if(env->ExceptionCheck())
                return TE_Err;
            if(result) {
                rawStyle.ref.managed = env->NewGlobalRef(result);
                rawStyle.ref.text = JNIStringUTF(*env, (jstring)rawStyle.ref.managed);
                rawStyle.value.text = rawStyle.ref.text;
            }
        } else if(styleCoding == com_atakmap_map_layer_feature_FeatureDefinition_STYLE_ATAK_STYLE) {
            Java::JNILocalRef result(*env, env->CallObjectMethod(impl, FeatureDefinition_class.getRawStyle));
            if(env->ExceptionCheck())
                return TE_Err;
            if(result) {
                atakmap::feature::Style *cstyle;
                rawStyle.ref.managed = env->NewGlobalRef(result);
                code = Interop_get(&rawStyle.ref.style, env, rawStyle.ref.managed);
                TE_CHECKRETURN_CODE(code);
                rawStyle.value.object = rawStyle.ref.style;
            }
        } else {
            return TE_Err;
        }
        rawStyle.valid = true;
    }
    *value = rawStyle.value;
    return TE_Ok;
}
TAKErr ManagedFeatureDefinition::getAttributes(const atakmap::util::AttributeSet **value) NOTHROWS
{
    LocalJNIEnv env;
    if(env->ExceptionCheck())
        return TE_Err;

    if(!attributes.valid) {
        Java::JNILocalRef mattrs(*env, env->CallObjectMethod(impl, FeatureDefinition_class.getAttributes));
        if(mattrs) {
            attributes.managed = env->NewGlobalRef(mattrs);
            Interop_get(&attributes.value, env, attributes.managed);
        }
        attributes.valid = true;
    }

    *value = attributes.value;
    return TE_Ok;
}
TAKErr ManagedFeatureDefinition::get(const Feature2 **value) NOTHROWS
{
    TAKErr code(TE_Ok);
    if(!feature.get()) {
        code = Feature_create(feature, *this);
        TE_CHECKRETURN_CODE(code);
    }
    *value = feature.get();
    return code;
}

namespace
{
    bool FeatureDefinition2_class_init(JNIEnv &env) NOTHROWS
    {
        FeatureDefinition_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDefinition");
        FeatureDefinition_class.v2.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDefinition2");
        FeatureDefinition_class.v3.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDefinition3");
        FeatureDefinition_class.v4.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/feature/FeatureDefinition4");
        FeatureDefinition_class.getRawGeom = env.GetMethodID(FeatureDefinition_class.id, "getRawGeometry", "()Ljava/lang/Object;");
        FeatureDefinition_class.getGeomCoding = env.GetMethodID(FeatureDefinition_class.id, "getGeomCoding", "()I");
        FeatureDefinition_class.getName = env.GetMethodID(FeatureDefinition_class.id, "getName", "()Ljava/lang/String;");
        FeatureDefinition_class.getStyleCoding = env.GetMethodID(FeatureDefinition_class.id, "getStyleCoding", "()I");
        FeatureDefinition_class.getRawStyle = env.GetMethodID(FeatureDefinition_class.id, "getRawStyle", "()Ljava/lang/Object;");
        FeatureDefinition_class.getAttributes = env.GetMethodID(FeatureDefinition_class.id, "getAttributes", "()Lcom/atakmap/map/layer/feature/AttributeSet;");
        FeatureDefinition_class.v2.getTimestamp = env.GetMethodID(FeatureDefinition_class.v2.id, "getTimestamp", "()J");
        FeatureDefinition_class.v3.getAltitudeMode = env.GetMethodID(FeatureDefinition_class.v3.id, "getAltitudeMode", "()Lcom/atakmap/map/layer/feature/Feature$AltitudeMode;");
        FeatureDefinition_class.v3.getExtrude = env.GetMethodID(FeatureDefinition_class.v3.id, "getExtrude", "()D");
        FeatureDefinition_class.v4.getTraits = env.GetMethodID(FeatureDefinition_class.v4.id, "getTraits", "()Lcom/atakmap/map/layer/feature/Feature$Traits;");

        return true;
    }
}