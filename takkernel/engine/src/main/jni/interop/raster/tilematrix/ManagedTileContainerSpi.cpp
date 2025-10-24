
#include "common.h"
#include "interop/raster/tilematrix/ManagedTileContainerSpi.h"
#include "interop/raster/tilematrix/Interop.h"
#include "interop/JNIStringUTF.h"
#include "interop/java/JNILocalRef.h"

using namespace TAKEngineJNI::Interop::Raster::TileMatrix;
using namespace TAK::Engine::Raster::TileMatrix;
using namespace TAK::Engine::Util;
using namespace TAKEngineJNI::Interop;

namespace {
    struct {
        jclass id;
        jmethodID getNameID;
        jmethodID getExtID;
        jmethodID createID;
        jmethodID openID;
        jmethodID getPriorityID;
        jmethodID isCompatID;

    } TileContainerSpi_class;

    bool TileContainerSpi_class_init(JNIEnv* env) {
        TileContainerSpi_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/TileContainerSpi");
        TileContainerSpi_class.getNameID = env->GetMethodID(TileContainerSpi_class.id, "getName", "()Ljava/lang/String;");
        TileContainerSpi_class.getExtID = env->GetMethodID(TileContainerSpi_class.id, "getDefaultExtension", "()Ljava/lang/String;");
        TileContainerSpi_class.openID = env->GetMethodID(TileContainerSpi_class.id, "open", "(Ljava/lang/String;Lcom/atakmap/map/layer/raster/tilematrix/TileMatrix;Z)Lcom/atakmap/map/layer/raster/tilematrix/TileContainer;");
        TileContainerSpi_class.createID = env->GetMethodID(TileContainerSpi_class.id, "create", "(Ljava/lang/String;Ljava/lang/String;Lcom/atakmap/map/layer/raster/tilematrix/TileMatrix;)Lcom/atakmap/map/layer/raster/tilematrix/TileContainer;");
        TileContainerSpi_class.isCompatID = env->GetMethodID(TileContainerSpi_class.id, "isCompatible", "(Lcom/atakmap/map/layer/raster/tilematrix/TileMatrix;)Z");

        return true;
    }

    bool checkInit(JNIEnv *env) {
        static bool v = TileContainerSpi_class_init(env);
        return v;
    }
}

ManagedTileContainerSpi::ManagedTileContainerSpi(JNIEnv& env, jobject impl) NOTHROWS : impl(env.NewGlobalRef(impl)) {
    checkInit(&env);
}

ManagedTileContainerSpi::~ManagedTileContainerSpi() NOTHROWS {
    LocalJNIEnv env;
    if (impl)
        env->DeleteGlobalRef(impl);
}

const char *ManagedTileContainerSpi::getName() const NOTHROWS {
    if (!impl)
        return nullptr;

    LocalJNIEnv env;
    Java::JNILocalRef nameStr(*env, env->CallObjectMethod(impl, TileContainerSpi_class.getNameID));
    cached_name_ = nullptr;
    JNIStringUTF_get(cached_name_, *env, static_cast<jstring>(nameStr.get()));
    return cached_name_;
}

const char *ManagedTileContainerSpi::getDefaultExtension() const NOTHROWS {
    if (!impl)
        return nullptr;

    LocalJNIEnv env;
    Java::JNILocalRef nameStr(*env, env->CallObjectMethod(impl, TileContainerSpi_class.getExtID));
    cached_ext_ = nullptr;
    JNIStringUTF_get(cached_ext_, *env, static_cast<jstring>(nameStr.get()));
    return cached_ext_;
}

TAKErr ManagedTileContainerSpi::create(TileContainerPtr &result, const char *name, const char *path, const class TileMatrix *spec) const NOTHROWS {
    if (!impl)
        return TE_InvalidArg;

    LocalJNIEnv env;
    Java::JNILocalRef nRef(*env, env->NewStringUTF(name));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    Java::JNILocalRef pRef(*env, env->NewStringUTF(path));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    Java::JNILocalRef specRef(*env, nullptr);
    TAKErr code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(specRef, *env, *spec);
    if (code != TE_Ok)
        return code;

    Java::JNILocalRef resRef(*env, env->CallObjectMethod(impl, TileContainerSpi_class.createID, nRef.get(), pRef.get(), specRef.get()));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    return TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(result, *env, resRef);
}

TAKErr ManagedTileContainerSpi::open(TileContainerPtr &result, const char *path, const class TileMatrix *spec, bool readOnly) const NOTHROWS {

    if (!impl)
        return TE_InvalidArg;

    LocalJNIEnv env;
    Java::JNILocalRef pRef(*env, env->NewStringUTF(path));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    jboolean jreadOnly = readOnly;

    TAKErr code = TE_Ok;
    Java::JNILocalRef specRef(*env, nullptr);
    if (spec) {
        code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(specRef, *env, *spec);
        if (code != TE_Ok)
            return code;
    }
    Java::JNILocalRef resRef(*env, env->CallObjectMethod(impl, TileContainerSpi_class.openID, pRef.get(), specRef.get(), jreadOnly));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    return TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(result, *env, resRef);
}

TAKErr ManagedTileContainerSpi::isCompatible(bool *result, const class TileMatrix *spec) const NOTHROWS {

    if (!impl)
        return TE_InvalidArg;

    LocalJNIEnv env;
    TAKErr code = TE_Ok;
    Java::JNILocalRef specRef(*env, nullptr);
    if (spec) {
        code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(specRef, *env, *spec);
        if (code != TE_Ok)
            return code;
    }
    *result = env->CallBooleanMethod(impl, TileContainerSpi_class.isCompatID, specRef.get());
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    return TE_Ok;
}
