#include "ManagedAssetAccessor.h"

#include "ManagedAssetRequest.h"
#include "formats/c3dt/CesiumNative.h"

using namespace TAK::Engine::Formats::Cesium3DTiles;
using namespace TAKEngineJNI::Interop::Formats::Cesium3DTiles;

namespace {
    struct {
        jclass id;
        jmethodID get;
        jmethodID request;
    } AssetAccessor_class;

    struct
    {
        jclass id;
        jmethodID ctor;
        jmethodID put;
    } HashMap_class;

    bool AssetAccessor_init(JNIEnv &env) NOTHROWS;
}

ManagedAssetAccessor::ManagedAssetAccessor(JNIEnv &env_, jobject impl) NOTHROWS :
 mimpl (env_.NewGlobalRef(impl))
{
    static bool clinit = AssetAccessor_init(env_);
}

ManagedAssetAccessor::~ManagedAssetAccessor() NOTHROWS
{
    if(mimpl) {
        LocalJNIEnv env;
        env->DeleteGlobalRef(mimpl);
        mimpl = NULL;
    }
}

CesiumAsync::Future<std::shared_ptr<CesiumAsync::IAssetRequest>>
ManagedAssetAccessor::get(const CesiumAsync::AsyncSystem &asyncSystem, const std::string &url,
                          const std::vector<CesiumAsync::IAssetAccessor::THeader> &headers) {
    assert(!url.empty());

    return runInWorkerThread(asyncSystem , std::function(
            [url, headers, this]()
            {
                LocalJNIEnv env;
                Java::JNILocalRef murl(*env, env->NewStringUTF(url.c_str()));
                Java::JNILocalRef mheaders = Java::JNILocalRef(*env, env->NewObject(HashMap_class.id, HashMap_class.ctor));

                for (const auto &header : headers) {
                    Java::JNILocalRef mkey(*env, env->NewStringUTF(header.first.c_str()));
                    Java::JNILocalRef mvalue(*env, env->NewStringUTF(header.second.c_str()));
                    env->CallObjectMethod(mheaders, HashMap_class.put, mkey.get(), mvalue.get());
                }

                Java::JNILocalRef massetRequest = Java::JNILocalRef(*env, env->CallObjectMethod(mimpl, AssetAccessor_class.get, (jstring)murl, (jobject)mheaders));
                auto request = std::make_shared<ManagedAssetRequest>(*env, massetRequest);
                return std::dynamic_pointer_cast<CesiumAsync::IAssetRequest>(request);
            }));
}

CesiumAsync::Future<std::shared_ptr<CesiumAsync::IAssetRequest>>
ManagedAssetAccessor::request(const CesiumAsync::AsyncSystem &asyncSystem, const std::string &verb,
                              const std::string &url,
                              const std::vector<CesiumAsync::IAssetAccessor::THeader> &headers,
                              const gsl::span<const std::byte> &contentPayload) {
    assert(!url.empty());

    return runInWorkerThread(asyncSystem, std::function(
            [verb, url, headers, /*contentPayload,*/ this]()
            {
                LocalJNIEnv env;
                Java::JNILocalRef mverb(*env, env->NewStringUTF(verb.c_str()));
                Java::JNILocalRef murl(*env, env->NewStringUTF(url.c_str()));
//                Java::JNILocalRef mcontentPayload(*env, env->NewStringUTF(contentPayload.c_str()));
                Java::JNILocalRef mheaders = Java::JNILocalRef(*env, env->NewObject(HashMap_class.id, HashMap_class.ctor));

                for (const auto &header : headers) {
                    Java::JNILocalRef mkey(*env, env->NewStringUTF(header.first.c_str()));
                    Java::JNILocalRef mvalue(*env, env->NewStringUTF(header.second.c_str()));
                    env->CallObjectMethod(mheaders, HashMap_class.put, mkey.get(), mvalue.get());
                }

                Java::JNILocalRef massetRequest = Java::JNILocalRef(*env, env->CallObjectMethod(mimpl, AssetAccessor_class.request, (jstring)mverb, (jstring)murl, (jobject)mheaders, nullptr/*(jstring)mcontentPayload*/));
                auto request = std::make_shared<ManagedAssetRequest>(*env, massetRequest);
                return std::dynamic_pointer_cast<CesiumAsync::IAssetRequest>(request);
            }));
}

void ManagedAssetAccessor::tick() noexcept {
    // no-op
}

namespace
{
    bool AssetAccessor_init(JNIEnv &env) NOTHROWS
    {
        AssetAccessor_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/AssetAccessor");
        if (!AssetAccessor_class.id) return false;
        AssetAccessor_class.get = env.GetMethodID(AssetAccessor_class.id, "get", "(Ljava/lang/String;Ljava/util/Map;)Lcom/atakmap/map/formats/c3dt/AssetRequest;");
        if (!AssetAccessor_class.get) return false;
        AssetAccessor_class.request = env.GetMethodID(AssetAccessor_class.id, "request", "(Ljava/lang/String;Ljava/lang/String;Ljava/util/Map;Ljava/lang/String;)Lcom/atakmap/map/formats/c3dt/AssetRequest;");
        if (!AssetAccessor_class.request) return false;

        HashMap_class.id = ATAKMapEngineJNI_findClass(&env, "java/util/HashMap");
        if (!HashMap_class.id) return false;
        HashMap_class.ctor = env.GetMethodID(HashMap_class.id, "<init>", "()V");
        if (!HashMap_class.ctor) return false;
        HashMap_class.put = env.GetMethodID(HashMap_class.id, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        if (!HashMap_class.put) return false;

        return true;
    }
}