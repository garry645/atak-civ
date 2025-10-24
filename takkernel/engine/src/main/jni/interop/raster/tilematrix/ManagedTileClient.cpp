
#include "interop/raster/tilematrix/ManagedTileClient.h"
#include "interop/feature/Interop.h"
#include "interop/java/JNIEnum.h"
#include "interop/raster/tilematrix/Interop.h"

#include "interop/InterfaceMarshalContext.h"

#include <limits>
#include <cstring> // memcpy

using namespace TAK::Engine::Util;
using namespace TAKEngineJNI::Interop::Raster::TileMatrix;
using namespace TAKEngineJNI::Interop;

namespace {
    struct {
        jclass id;
        jmethodID clearAuthFailedID;
        jmethodID checkConnectivityID;
        jmethodID cacheID;
        jmethodID estimateTileCountID;
    } TileClient_class;

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID minResID;
        jfieldID maxResID;
        jfieldID regionID;
        jfieldID timespanStartID;
        jfieldID timespanEndID;
        jfieldID cacheFileID;
        jfieldID modeID;
        jfieldID canceledID;
        jfieldID countOnlyID;
        jfieldID maxThreadsID;
        jfieldID expireOffsetID;
        jfieldID prefContainerProvID;
        jclass File_id;
        jmethodID File_ctor;
    } CacheRequest_class;

    bool CacheRequest_class_init(JNIEnv* env) {
        CacheRequest_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/contentservices/CacheRequest");
        CacheRequest_class.ctor = env->GetMethodID(CacheRequest_class.id, "<init>", "()V");
        CacheRequest_class.minResID = env->GetFieldID(CacheRequest_class.id, "minResolution", "D");
        CacheRequest_class.maxResID = env->GetFieldID(CacheRequest_class.id, "maxResolution", "D");
        CacheRequest_class.regionID = env->GetFieldID(CacheRequest_class.id, "region", "Lcom/atakmap/map/layer/feature/geometry/Geometry;");
        CacheRequest_class.timespanStartID = env->GetFieldID(CacheRequest_class.id, "timespanStart", "J");
        CacheRequest_class.timespanEndID = env->GetFieldID(CacheRequest_class.id, "timespanEnd", "J");
        CacheRequest_class.cacheFileID = env->GetFieldID(CacheRequest_class.id, "cacheFile", "Ljava/io/File;");
        CacheRequest_class.modeID = env->GetFieldID(CacheRequest_class.id, "mode", "Lcom/atakmap/map/contentservices/CacheRequest$CacheMode;");
        CacheRequest_class.canceledID = env->GetFieldID(CacheRequest_class.id, "canceled", "Z");
        CacheRequest_class.countOnlyID = env->GetFieldID(CacheRequest_class.id, "countOnly", "Z");
        CacheRequest_class.maxThreadsID = env->GetFieldID(CacheRequest_class.id, "maxThreads", "I");
        CacheRequest_class.expireOffsetID = env->GetFieldID(CacheRequest_class.id, "expirationOffset", "J");
        CacheRequest_class.prefContainerProvID = env->GetFieldID(CacheRequest_class.id, "preferredContainerProvider", "Ljava/lang/String;");
        CacheRequest_class.File_id = ATAKMapEngineJNI_findClass(env, "java/io/File");
        CacheRequest_class.File_ctor = env->GetMethodID(CacheRequest_class.File_id, "<init>", "(Ljava/lang/String;)V");
        return true;
    }

    bool Init_impl(JNIEnv& env) {
        TileClient_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/raster/tilematrix/TileClient");
        TileClient_class.clearAuthFailedID = env.GetMethodID(TileClient_class.id, "clearAuthFailed", "()V");
        TileClient_class.checkConnectivityID = env.GetMethodID(TileClient_class.id, "checkConnectivity", "()V");
        TileClient_class.cacheID = env.GetMethodID(TileClient_class.id, "cache", "(Lcom/atakmap/map/contentservices/CacheRequest;Lcom/atakmap/map/contentservices/CacheRequestListener;)V");
        TileClient_class.estimateTileCountID = env.GetMethodID(TileClient_class.id, "estimateTileCount", "(Lcom/atakmap/map/contentservices/CacheRequest;)I");
        return true;
    }

    bool checkInit(JNIEnv& env) {
        static bool v = Init_impl(env) &&
                CacheRequest_class_init(&env);
        return v;
    }

    TAKErr CacheRequest_get(Java::JNILocalRef& jout, JNIEnv* env, const TAK::Engine::Raster::TileMatrix::CacheRequest& creq) {

        jout = Java::JNILocalRef(*env, env->NewObject(CacheRequest_class.id, CacheRequest_class.ctor));
        env->SetDoubleField(jout, CacheRequest_class.minResID, creq.minResolution);
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        env->SetDoubleField(jout, CacheRequest_class.maxResID, creq.maxResolution);
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        if (creq.region) {
            env->SetObjectField(jout, CacheRequest_class.regionID, TAKEngineJNI::Interop::Feature::Interop_create(env, *creq.region));
            if (env->ExceptionCheck()) {
                env->ExceptionClear();
                return TE_Err;
            }
        }

        env->SetLongField(jout, CacheRequest_class.timespanStartID, creq.timeSpanStart);
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        env->SetLongField(jout, CacheRequest_class.timespanEndID, creq.timeSpanEnd);
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        env->SetBooleanField(jout, CacheRequest_class.canceledID, creq.canceled);
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        env->SetBooleanField(jout, CacheRequest_class.countOnlyID, creq.countOnly);
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        env->SetIntField(jout, CacheRequest_class.maxThreadsID, creq.maxThreads);
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        env->SetLongField(jout, CacheRequest_class.expireOffsetID, creq.expirationOffset);
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        Java::JNILocalRef mcachePath(*env, env->NewStringUTF(creq.cacheFilePath));
        Java::JNILocalRef cacheFilePathRef(*env, env->NewObject(CacheRequest_class.File_id, CacheRequest_class.File_ctor, mcachePath.get()));
        env->SetObjectField(jout, CacheRequest_class.cacheFileID, cacheFilePathRef.get());
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        Java::JNILocalRef mpreferredContainerProvider(*env, env->NewStringUTF(creq.preferredContainerProvider));
        env->SetObjectField(jout, CacheRequest_class.prefContainerProvID, mpreferredContainerProvider.get());
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        Java::JNILocalRef enumValue(*env, nullptr);
        const char enumClass[] = "com/atakmap/map/contentservices/CacheRequest$CacheMode";
        switch (creq.mode) {
        case TAK::Engine::Raster::TileMatrix::TECM_Create: TAKEngineJNI::Interop::Java::JNIEnum_value(enumValue, *env, enumClass, "Create"); break;
        case TAK::Engine::Raster::TileMatrix::TECM_Append: TAKEngineJNI::Interop::Java::JNIEnum_value(enumValue, *env, enumClass, "Append"); break;
        }

        env->SetObjectField(jout, CacheRequest_class.modeID, enumValue.get());
        if(env->ExceptionCheck()) {
            env->ExceptionClear();
            return TE_Err;
        }

        return TE_Ok;
    }
}

ManagedTileClient::ManagedTileClient(JNIEnv &env, jobject impl) NOTHROWS
    : ManagedTileMatrix(env, impl) {
    checkInit(env);
}

ManagedTileClient::~ManagedTileClient() NOTHROWS {}

TAKErr ManagedTileClient::clearAuthFailed() NOTHROWS {
    LocalJNIEnv env;
    env->CallVoidMethod(impl, TileClient_class.clearAuthFailedID);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }
    return TE_Ok;
}

TAKErr ManagedTileClient::checkConnectivity() NOTHROWS {
    LocalJNIEnv env;
    env->CallVoidMethod(impl, TileClient_class.checkConnectivityID);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }
    return TE_Ok;
}

TAKErr ManagedTileClient::cache(const TAK::Engine::Raster::TileMatrix::CacheRequest &request, std::shared_ptr<TAK::Engine::Raster::TileMatrix::CacheRequestListener> &listener) NOTHROWS {

    LocalJNIEnv env;

    Java::JNILocalRef reqRef(*env, nullptr);
    TAKErr code = CacheRequest_get(reqRef, env, request);
    if (code != TE_Ok)
        return code;

    Java::JNILocalRef listenerRef(*env, nullptr);
    code = Interop_marshal(listenerRef, *env, listener);
    if (code != TE_Ok)
        return code;

    env->CallVoidMethod(impl, TileClient_class.cacheID, reqRef.get(), listenerRef.get());
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    return TE_Ok;
}

TAKErr ManagedTileClient::estimateTilecount(int *count, const TAK::Engine::Raster::TileMatrix::CacheRequest &request) NOTHROWS {
    LocalJNIEnv env;

    Java::JNILocalRef reqRef(*env, nullptr);
    TAKErr code = CacheRequest_get(reqRef, env, request);
    if (code != TE_Ok)
        return code;

    *count = env->CallIntMethod(impl, TileClient_class.estimateTileCountID, reqRef.get());
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    return TE_Ok;
}
