#include "ManagedAssetResponse.h"

#include "util/Error.h"
#include "interop/java/JNICollection.h"
#include "interop/JNIStringUTF.h"

using namespace TAK::Engine::Util;
using namespace TAKEngineJNI::Interop::Java;
using namespace TAKEngineJNI::Interop::Formats::Cesium3DTiles;

namespace {
    struct {
        jclass id;
        jfieldID statuscode;
        jfieldID contenttype;
        jfieldID headers;
        jfieldID data;
    } AssetResponse_class;

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

    bool AssetResponse_init(JNIEnv &env) NOTHROWS;
}

ManagedAssetResponse::ManagedAssetResponse(JNIEnv &env_, jobject impl) NOTHROWS :
        mimpl (env_.NewGlobalRef(impl))
{
    static bool clinit = AssetResponse_init(env_);
}

ManagedAssetResponse::~ManagedAssetResponse() NOTHROWS
{
    if(mimpl) {
        LocalJNIEnv env;
        env->DeleteGlobalRef(mimpl);
        mimpl = nullptr;
    }
}

uint16_t ManagedAssetResponse::statusCode() const {
    LocalJNIEnv env;
    return env->GetIntField(mimpl, AssetResponse_class.statuscode);
}

std::string ManagedAssetResponse::contentType() const {
    LocalJNIEnv env;
    TAK::Engine::Port::String cmethod;
    TAKErr code = ATAKMapEngineJNI_GetStringField(cmethod, *env, mimpl, AssetResponse_class.contenttype);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return cmethod.get();
}

const CesiumAsync::HttpHeaders &ManagedAssetResponse::headers() const {
    LocalJNIEnv env;
    Java::JNILocalRef mheader(*env, env->GetObjectField(mimpl, AssetResponse_class.headers));

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

gsl::span<const std::byte> ManagedAssetResponse::data() const {
    LocalJNIEnv env;
    Java::JNILocalRef mdata(*env, env->GetObjectField(mimpl, AssetResponse_class.data));
    const std::byte* data = GET_BUFFER_POINTER(const std::byte, mdata);
    long size = env->GetDirectBufferCapacity(mdata);
    return gsl::make_span(data, size);
}

namespace
{
    bool AssetResponse_init(JNIEnv &env) NOTHROWS
    {
        AssetResponse_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/AssetResponse");
        if (!AssetResponse_class.id) return false;
        AssetResponse_class.statuscode = env.GetFieldID(AssetResponse_class.id, "statusCode", "I");
        if (!AssetResponse_class.statuscode) return false;
        AssetResponse_class.contenttype = env.GetFieldID(AssetResponse_class.id, "contentType", "Ljava/lang/String;");
        if (!AssetResponse_class.contenttype) return false;
        AssetResponse_class.headers = env.GetFieldID(AssetResponse_class.id, "headers", "Ljava/util/Map;");
        if (!AssetResponse_class.headers) return false;
        AssetResponse_class.data = env.GetFieldID(AssetResponse_class.id, "data", "Ljava/nio/ByteBuffer;");
        if (!AssetResponse_class.data) return false;

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