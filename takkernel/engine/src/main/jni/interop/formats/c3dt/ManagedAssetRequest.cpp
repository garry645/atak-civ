#include "ManagedAssetRequest.h"

#include "util/Error.h"
#include "interop/java/JNICollection.h"
#include "interop/JNIStringUTF.h"
#include "ManagedAssetResponse.h"

using namespace TAK::Engine::Util;
using namespace TAKEngineJNI::Interop::Java;
using namespace TAKEngineJNI::Interop::Formats::Cesium3DTiles;

namespace CesiumAsync {
    struct NocaseCompare {
        bool operator()(const unsigned char& c1, const unsigned char& c2) const {
            return tolower(c1) < tolower(c2);
        }
    };

    bool CaseInsensitiveCompare::operator()(
            const std::string& s1,
            const std::string& s2) const {
        return std::lexicographical_compare(
                s1.begin(),
                s1.end(),
                s2.begin(),
                s2.end(),
                NocaseCompare());
    }
}

namespace {
    struct {
        jclass id;
        jfieldID method;
        jfieldID url;
        jfieldID headers;
        jfieldID response;
    } AssetRequest_class;

    struct
    {
        jclass id;
        jmethodID size;
        jmethodID keySet;
        jmethodID get;
        jmethodID put;
    } Map_class;

    struct
    {
        jclass id;
        jmethodID ctor;
    } HashMap_class;

    bool AssetRequest_init(JNIEnv &env) NOTHROWS;
}

ManagedAssetRequest::ManagedAssetRequest(JNIEnv &env_, jobject impl) NOTHROWS :
        mimpl (env_.NewGlobalRef(impl))
{
    static bool clinit = AssetRequest_init(env_);
}

ManagedAssetRequest::~ManagedAssetRequest() NOTHROWS
{
    if(mimpl) {
        LocalJNIEnv env;
        env->DeleteGlobalRef(mimpl);
        mimpl = nullptr;
    }
}

const std::string & TAKEngineJNI::Interop::Formats::Cesium3DTiles::ManagedAssetRequest::method() const {
    LocalJNIEnv env;
    TAK::Engine::Port::String cmethod;
    TAKErr code = ATAKMapEngineJNI_GetStringField(cmethod, *env, mimpl, AssetRequest_class.method);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return _method;
    _method = cmethod.get();
    return _method;
}

const std::string & TAKEngineJNI::Interop::Formats::Cesium3DTiles::ManagedAssetRequest::url() const {
    LocalJNIEnv env;
    TAK::Engine::Port::String curl;
    TAKErr code = ATAKMapEngineJNI_GetStringField(curl, *env, mimpl, AssetRequest_class.url);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return _url;
    _url = curl.get();
    return _url;
}

 const CesiumAsync::HttpHeaders & TAKEngineJNI::Interop::Formats::Cesium3DTiles::ManagedAssetRequest::headers() const {
    LocalJNIEnv env;
    Java::JNILocalRef mheader(*env, env->GetObjectField(mimpl, AssetRequest_class.headers));

    JNILocalRef keySet(*env, env->CallObjectMethod(mheader, Map_class.keySet));

    TAKErr code(TE_Ok);
    JNILocalRef iterator(*env, nullptr);
    code = JNICollection_iterator(iterator, *env, keySet);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return _headers;

    bool hasNext;
    code = JNIIterator_hasNext(hasNext, *env, iterator);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return _headers;

    _headers.clear();
    while (hasNext)
    {
        JNILocalRef key(*env, nullptr);
        code = JNIIterator_next(key, *env, iterator);
        TE_CHECKBREAK_CODE(code)

        JNILocalRef value(*env, env->CallObjectMethod(mheader, Map_class.get, key.get()));

        TAK::Engine::Port::String keyStr;
        TAK::Engine::Port::String valueStr;
        JNIStringUTF_get(keyStr, *env, (jstring)key);
        JNIStringUTF_get(valueStr, *env, (jstring)value);
        _headers.insert({keyStr.get(), valueStr.get()});

        code = JNIIterator_hasNext(hasNext, *env, iterator);
        TE_CHECKBREAK_CODE(code)
    }

    return _headers;
}

const CesiumAsync::IAssetResponse * TAKEngineJNI::Interop::Formats::Cesium3DTiles::ManagedAssetRequest::response() const {
    LocalJNIEnv env;
    TAK::Engine::Port::String curl;
    Java::JNILocalRef mresponse = Java::JNILocalRef(*env, env->GetObjectField(mimpl, AssetRequest_class.response));
    std::shared_ptr<CesiumAsync::IAssetResponse> response = std::make_shared<ManagedAssetResponse>(*env, mresponse);
    _response = response;
    return _response.get();
}

namespace
{
    bool AssetRequest_init(JNIEnv &env) NOTHROWS
    {
        AssetRequest_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/AssetRequest");
        if (!AssetRequest_class.id) return false;
        AssetRequest_class.method = env.GetFieldID(AssetRequest_class.id, "method", "Ljava/lang/String;");
        if (!AssetRequest_class.method) return false;
        AssetRequest_class.url = env.GetFieldID(AssetRequest_class.id, "url", "Ljava/lang/String;");
        if (!AssetRequest_class.url) return false;
        AssetRequest_class.headers = env.GetFieldID(AssetRequest_class.id, "headers", "Ljava/util/Map;");
        if (!AssetRequest_class.headers) return false;
        AssetRequest_class.response = env.GetFieldID(AssetRequest_class.id, "response", "Lcom/atakmap/map/formats/c3dt/AssetResponse;");
        if (!AssetRequest_class.response) return false;

        HashMap_class.id = ATAKMapEngineJNI_findClass(&env, "java/util/HashMap");
        if (!HashMap_class.id) return false;
        HashMap_class.ctor = env.GetMethodID(HashMap_class.id, "<init>", "()V");
        if (!HashMap_class.ctor) return false;

        Map_class.id = ATAKMapEngineJNI_findClass(&env, "java/util/Map");
        if (!Map_class.id) return false;
        Map_class.keySet = env.GetMethodID(Map_class.id, "keySet", "()Ljava/util/Set;");
        if (!Map_class.keySet) return false;
        Map_class.get = env.GetMethodID(Map_class.id, "get", "(Ljava/lang/Object;)Ljava/lang/Object;");
        if (!Map_class.get) return false;
        Map_class.put = env.GetMethodID(Map_class.id, "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;");
        if (!Map_class.put) return false;

        return true;
    }
}