
#include "common.h"
#include "interop/raster/tilematrix/ManagedTileClientSpi.h"
#include "interop/JNIStringUTF.h"
#include "interop/java/JNILocalRef.h"
#include "interop/raster/tilematrix/Interop.h"

using namespace TAKEngineJNI::Interop::Raster::TileMatrix;
using namespace TAK::Engine::Util;
using namespace TAKEngineJNI::Interop;

namespace {
    struct {
        jclass id;
        jclass options_id;
        jfieldID options_dnsLookupTimeoutID;
        jfieldID options_connectTimeoutID;
        jfieldID options_proxyID;
        jmethodID options_ctor;
        jmethodID getNameID;
        jmethodID createID;
        jmethodID getPriorityID;

    } TileClientSpi_class;

    bool TileClientSpi_class_init(JNIEnv* env) {
        TileClientSpi_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/TileClientSpi");
        TileClientSpi_class.options_id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/TileClientSpi$Options");
        TileClientSpi_class.options_ctor = env->GetMethodID(TileClientSpi_class.options_id, "<init>", "()V");
        TileClientSpi_class.options_dnsLookupTimeoutID = env->GetFieldID(TileClientSpi_class.options_id, "dnsLookupTimeout", "J");
        TileClientSpi_class.options_connectTimeoutID = env->GetFieldID(TileClientSpi_class.options_id, "connectTimeout", "J");
        TileClientSpi_class.options_proxyID = env->GetFieldID(TileClientSpi_class.options_id, "proxy", "Z");
        TileClientSpi_class.getNameID = env->GetMethodID(TileClientSpi_class.id, "getName", "()Ljava/lang/String;");
        TileClientSpi_class.createID = env->GetMethodID(TileClientSpi_class.id, "create", "(Ljava/lang/String;Ljava/lang/String;Lcom/atakmap/map/layer/raster/tilematrix/TileClientSpi$Options;)Lcom/atakmap/map/layer/raster/tilematrix/TileClient;");
        TileClientSpi_class.getPriorityID = env->GetMethodID(TileClientSpi_class.id, "getPriority", "()I");
        return true;
    }

    bool checkInit(JNIEnv *env) {
        static bool v = TileClientSpi_class_init(env);
        return v;
    }
}

ManagedTileClientSpi::ManagedTileClientSpi(JNIEnv& env, jobject impl) NOTHROWS : impl(env.NewGlobalRef(impl)) {
    checkInit(&env);
}

ManagedTileClientSpi::~ManagedTileClientSpi() NOTHROWS {
    LocalJNIEnv env;
    if (impl)
        env->DeleteGlobalRef(impl);
}

const char *ManagedTileClientSpi::getName() const NOTHROWS {
    if (!impl)
        return nullptr;

    LocalJNIEnv env;
    Java::JNILocalRef nameStr(*env, env->CallObjectMethod(impl, TileClientSpi_class.getNameID));
    cached_name_ = nullptr;
    JNIStringUTF_get(cached_name_, *env, static_cast<jstring>(nameStr.get()));
    return cached_name_;
}

TAKErr ManagedTileClientSpi::create(TAK::Engine::Raster::TileMatrix::TileClientPtr &result, const char *path, const char *offlineCachePath, const Options *opts) const NOTHROWS {

    if (!impl)
        return TE_InvalidArg;

    LocalJNIEnv env;
    Java::JNILocalRef pRef(*env, env->NewStringUTF(path));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    Java::JNILocalRef ocpRef(*env, env->NewStringUTF(offlineCachePath));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    Java::JNILocalRef optsRef(*env, nullptr);
    if(opts) {
        optsRef = Java::JNILocalRef(*env, env->NewObject(TileClientSpi_class.options_id, TileClientSpi_class.options_ctor));
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        env->SetLongField(optsRef.get(), TileClientSpi_class.options_dnsLookupTimeoutID, opts->dnsLookupTimeout);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        env->SetLongField(optsRef.get(), TileClientSpi_class.options_connectTimeoutID, opts->connectTimeout);
        if (env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }
    }

    Java::JNILocalRef resultRef(*env, env->CallObjectMethod(impl, TileClientSpi_class.createID, pRef.get(), ocpRef.get(), optsRef.get()));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    return TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(result, *env, resultRef);
}

int ManagedTileClientSpi::getPriority() const NOTHROWS {

    if (!impl)
        return TE_InvalidArg;

    LocalJNIEnv env;
    jint result = env->CallIntMethod(impl, TileClientSpi_class.getPriorityID);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return 0;
    }
    return result;
}