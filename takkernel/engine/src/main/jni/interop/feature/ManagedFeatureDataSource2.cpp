#include "ManagedFeatureDataSource2.h"

#include <list>

#include <feature/Feature2.h>
#include <feature/LegacyAdapters.h>
#include <util/Memory.h>

#include "common.h"
#include "com_atakmap_map_layer_feature_FeatureDefinition.h"
#include "interop/JNIByteArray.h"
#include "interop/JNIStringUTF.h"
#include "interop/java/JNILocalRef.h"
#include "interop/feature/Interop.h"
#include "interop/feature/ManagedFeatureDefinition.h"

using namespace TAKEngineJNI::Interop::Feature;

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Util;

using namespace atakmap::feature;
using namespace atakmap::util;

using namespace TAKEngineJNI::Interop;

namespace
{
    struct
    {
        jclass id;
        jmethodID getName;
        jmethodID parseVersion;
        jmethodID parse;
    } FeatureDataSource2_class;

    struct
    {
        jclass id;
        jmethodID getType;
        jmethodID getProvider;
        jmethodID moveToNext;
        jmethodID get;
        jmethodID getFeatureSetName;
        jmethodID getMinResolution;
        jmethodID getMaxResolution;
        jmethodID close;

        struct
        {
            jobject FEATURE;
            jobject FEATURE_SET;
        } ContentPointer_enum;

    } FeatureDataSource_Content_class;

    class ManagedContent : public FeatureDataSource2::Content
    {
    public :
        ManagedContent(JNIEnv *env, jobject impl) NOTHROWS;
        ~ManagedContent() NOTHROWS;
    public :
        const char *getType() const NOTHROWS;
        const char *getProvider() const NOTHROWS;
        TAKErr moveToNextFeature() NOTHROWS;
        TAKErr moveToNextFeatureSet() NOTHROWS;
        TAKErr get(FeatureDefinition2 **value) const NOTHROWS;
        TAKErr getFeatureSetName(TAK::Engine::Port::String &value) const NOTHROWS;
        TAKErr getMinResolution(double *value) const NOTHROWS;
        TAKErr getMaxResolution(double *value) const NOTHROWS;
        TAKErr getVisible(bool *value) const NOTHROWS;
        TAKErr getFeatureSetVisible(bool *value) const NOTHROWS;
    public :
        jobject impl;
        TAK::Engine::Port::String type;
        TAK::Engine::Port::String provider;
        TAK::Engine::Feature::FeatureDefinitionPtr feature;
    };

    bool FeatureDataSource2_class_init(JNIEnv *env) NOTHROWS;
}

ManagedFeatureDataSource2::ManagedFeatureDataSource2(JNIEnv *env_, jobject impl_) NOTHROWS :
    impl(env_->NewGlobalRef(impl_))
{
    static bool clinit = FeatureDataSource2_class_init(env_);

    Java::JNILocalRef mname(*env_, env_->CallObjectMethod(impl, FeatureDataSource2_class.getName));
    JNIStringUTF jname(*env_, mname);
    name = jname;
}
ManagedFeatureDataSource2::~ManagedFeatureDataSource2() NOTHROWS
{
    if(impl) {
        LocalJNIEnv env;
        env->DeleteGlobalRef(impl);
    }
}
TAKErr ManagedFeatureDataSource2::parse(ContentPtr &content, const char *file) NOTHROWS
{
    LocalJNIEnv env;
    jclass File_class = ATAKMapEngineJNI_findClass(env, "java/io/File");
    if(!File_class)
        return TE_Err;
    jmethodID File_ctor = env->GetMethodID(File_class, "<init>", "(Ljava/lang/String;)V");
    if(!File_ctor)
        return TE_Err;
    Java::JNILocalRef jpath(*env,  env->NewStringUTF(file));
    Java::JNILocalRef jfile(*env, env->NewObject(File_class, File_ctor, jpath.get()));
    if(!jfile)
        return TE_Err;
    Java::JNILocalRef result(*env, env->CallObjectMethod(impl, FeatureDataSource2_class.parse, jfile.get()));
    if(!result)
        return TE_InvalidArg;
    content = ContentPtr(new ManagedContent(env, result), Memory_deleter_const<FeatureDataSource2::Content, ManagedContent>);
    return TE_Ok;
}

TAK::Engine::Feature::AltitudeMode ManagedFeatureDataSource2::getAltitudeMode() const NOTHROWS
{
    return altMode;
}

double ManagedFeatureDataSource2::getExtrude() const NOTHROWS 
{
    return extrude;
}

const char *ManagedFeatureDataSource2::getName() const NOTHROWS
{
    return name;
}

int ManagedFeatureDataSource2::parseVersion() const NOTHROWS
{
    LocalJNIEnv env;
    return env->CallIntMethod(impl, FeatureDataSource2_class.parseVersion);
}

namespace
{
    ManagedContent::ManagedContent(JNIEnv *env_, jobject impl_) NOTHROWS :
        impl(env_->NewGlobalRef(impl_)),
        feature(NULL, NULL)
    {
        do {
            if(env_->ExceptionCheck())
                break;
            Java::JNILocalRef result(*env_, env_->CallObjectMethod(impl, FeatureDataSource_Content_class.getType));
            if(env_->ExceptionCheck())
                break;
            if(result) {
                JNIStringUTF jresult(*env_, result);
                type = jresult;
            }
        } while(false);
        do {
            if(env_->ExceptionCheck())
                break;
            Java::JNILocalRef result(*env_, env_->CallObjectMethod(impl, FeatureDataSource_Content_class.getProvider));
            if(env_->ExceptionCheck())
                break;
            if(result) {
                JNIStringUTF jresult(*env_, result);
                provider = jresult;
            }
        } while(false);
    }
    ManagedContent::~ManagedContent() NOTHROWS
    {
        if(impl) {
            LocalJNIEnv env;
            if(!env->ExceptionCheck())
                env->CallVoidMethod(impl, FeatureDataSource_Content_class.close);
            env->DeleteGlobalRef(impl);
            impl = NULL;
        }
    }
    const char *ManagedContent::getType() const NOTHROWS
    {
        return type;
    }
    const char *ManagedContent::getProvider() const NOTHROWS
    {
        return provider;
    }
    TAKErr ManagedContent::moveToNextFeature() NOTHROWS
    {
        feature.reset();

        LocalJNIEnv env;
        if(env->ExceptionCheck())
            return TE_Err;
        jboolean result = env->CallBooleanMethod(impl, FeatureDataSource_Content_class.moveToNext, FeatureDataSource_Content_class.ContentPointer_enum.FEATURE);
        if(env->ExceptionCheck())
            return TE_Err;
        if(!result)
            return TE_Done;

        Java::JNILocalRef jfdefn(*env, env->CallObjectMethod(impl, FeatureDataSource_Content_class.get));
        if(jfdefn)
            feature = FeatureDefinitionPtr(new ManagedFeatureDefinition(*env, jfdefn), Memory_deleter_const<FeatureDefinition2, ManagedFeatureDefinition>);

        return TE_Ok;
    }
    TAKErr ManagedContent::moveToNextFeatureSet() NOTHROWS
    {
        feature.reset();

        LocalJNIEnv env;
        if(env->ExceptionCheck())
            return TE_Err;
        jboolean result = env->CallBooleanMethod(impl, FeatureDataSource_Content_class.moveToNext, FeatureDataSource_Content_class.ContentPointer_enum.FEATURE_SET);
        if(env->ExceptionCheck())
            return TE_Err;
        return result ? TE_Ok : TE_Done;
    }
    TAKErr ManagedContent::get(FeatureDefinition2 **value) const NOTHROWS
    {
        *value = feature.get();
        return TE_Ok;
    }
    TAKErr ManagedContent::getFeatureSetName(TAK::Engine::Port::String &value) const NOTHROWS
    {
        LocalJNIEnv env;
        if(env->ExceptionCheck())
            return TE_Err;
        Java::JNILocalRef result(*env, env->CallObjectMethod(impl, FeatureDataSource_Content_class.getFeatureSetName));
        if(env->ExceptionCheck())
            return TE_Err;
        if(!result) {
            value = NULL;
            return TE_Ok;
        } else {
            JNIStringUTF jresult(*env, result);
            value = jresult;
            return TE_Ok;
        }
    }
    TAKErr ManagedContent::getMinResolution(double *value) const NOTHROWS
    {
        LocalJNIEnv env;
        if(env->ExceptionCheck())
            return TE_Err;
        jdouble result = env->CallDoubleMethod(impl, FeatureDataSource_Content_class.getMinResolution);
        if(env->ExceptionCheck())
            return TE_Err;
            *value = result;
        return TE_Ok;
    }
    TAKErr ManagedContent::getMaxResolution(double *value) const NOTHROWS
    {
        LocalJNIEnv env;
        jdouble result = env->CallDoubleMethod(impl, FeatureDataSource_Content_class.getMaxResolution);
        if(env->ExceptionCheck())
            return TE_Err;
            *value = result;
        return TE_Ok;
    }
    TAKErr ManagedContent::getVisible(bool *value) const NOTHROWS
    {
        *value = true;
        return TE_Ok;
    }
    TAKErr ManagedContent::getFeatureSetVisible(bool *value) const NOTHROWS
    {
        *value = true;
        return TE_Ok;
    }

    bool FeatureDataSource2_class_init(JNIEnv *env) NOTHROWS
    {
        FeatureDataSource2_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/feature/FeatureDataSource");
        FeatureDataSource2_class.getName = env->GetMethodID(FeatureDataSource2_class.id, "getName", "()Ljava/lang/String;");
        FeatureDataSource2_class.parseVersion = env->GetMethodID(FeatureDataSource2_class.id, "parseVersion", "()I");
        FeatureDataSource2_class.parse = env->GetMethodID(FeatureDataSource2_class.id, "parse", "(Ljava/io/File;)Lcom/atakmap/map/layer/feature/FeatureDataSource$Content;");

        FeatureDataSource_Content_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/feature/FeatureDataSource$Content");
        FeatureDataSource_Content_class.getType = env->GetMethodID(FeatureDataSource_Content_class.id, "getType", "()Ljava/lang/String;");
        FeatureDataSource_Content_class.getProvider = env->GetMethodID(FeatureDataSource_Content_class.id, "getProvider", "()Ljava/lang/String;");
        FeatureDataSource_Content_class.moveToNext = env->GetMethodID(FeatureDataSource_Content_class.id, "moveToNext", "(Lcom/atakmap/map/layer/feature/FeatureDataSource$Content$ContentPointer;)Z");
        FeatureDataSource_Content_class.get = env->GetMethodID(FeatureDataSource_Content_class.id, "get", "()Lcom/atakmap/map/layer/feature/FeatureDataSource$FeatureDefinition;");
        FeatureDataSource_Content_class.getFeatureSetName = env->GetMethodID(FeatureDataSource_Content_class.id, "getFeatureSetName", "()Ljava/lang/String;");
        FeatureDataSource_Content_class.getMinResolution = env->GetMethodID(FeatureDataSource_Content_class.id, "getMinResolution", "()D");
        FeatureDataSource_Content_class.getMaxResolution = env->GetMethodID(FeatureDataSource_Content_class.id, "getMaxResolution", "()D");
        FeatureDataSource_Content_class.close = env->GetMethodID(FeatureDataSource_Content_class.id, "close", "()V");

        jclass ContentPointer_enum = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/feature/FeatureDataSource$Content$ContentPointer");
        FeatureDataSource_Content_class.ContentPointer_enum.FEATURE = env->NewWeakGlobalRef(env->GetStaticObjectField(ContentPointer_enum, env->GetStaticFieldID(ContentPointer_enum, "FEATURE", "Lcom/atakmap/map/layer/feature/FeatureDataSource$Content$ContentPointer;")));
        FeatureDataSource_Content_class.ContentPointer_enum.FEATURE_SET = env->NewWeakGlobalRef(env->GetStaticObjectField(ContentPointer_enum, env->GetStaticFieldID(ContentPointer_enum, "FEATURE_SET", "Lcom/atakmap/map/layer/feature/FeatureDataSource$Content$ContentPointer;")));

        return true;
    }
}
