#include "gov_tak_api_engine_net_LibcurlHttpClientBuilder.h"

#include <atomic>
#include <map>
#include <set>
#include <sstream>

#include <curl/curl.h>
#include <openssl/ssl.h>

#include <port/STLVectorAdapter.h>
#include <thread/Lock.h>
#include <thread/Monitor.h>
#include <thread/Mutex.h>
#include <thread/Thread.h>
#include <util/ConfigOptions.h>

#include "gov_tak_api_engine_net_IHttpClient.h"
#include "common.h"
#include "interop/Pointer.h"
#include "interop/JNIStringUTF.h"
#include "interop/java/JNILocalRef.h"
#include "interop/util/ManagedDataInput2.h"
#include "interop/util/ManagedDataOutput2.h"

using namespace TAK::Engine::Thread;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

extern int rehash(const char *sdirname, const char *ddirname);

namespace
{

    struct {
        Mutex mutex;
        std::vector<CURL *> value;
        const std::size_t capacity{32u};
    } connectionPool;

    CURL *curl_create(jint method)
    {
        CURL *curl;
        {
            Lock lock(connectionPool.mutex);
            if (connectionPool.value.empty()) {
                curl = curl_easy_init();
            } else {
                curl = connectionPool.value.back();
                connectionPool.value.pop_back();
            }
        }
        if(method == gov_tak_api_engine_net_LibcurlHttpClientBuilder_METHOD_GET) {
            curl_easy_setopt(curl, CURLOPT_HTTPGET, 1);
        } else if(method == gov_tak_api_engine_net_LibcurlHttpClientBuilder_METHOD_POST) {
            curl_easy_setopt(curl, CURLOPT_HTTPPOST, 1);
            curl_easy_setopt(curl, CURLOPT_POSTFIELDS, "");
            curl_easy_setopt(curl, CURLOPT_POSTFIELDSIZE, 0);
        }

        // configure default options
        curl_easy_setopt(curl, CURLOPT_FOLLOWLOCATION, 1);
        curl_easy_setopt(curl, CURLOPT_TIMEOUT_MS, (long)10000);
        curl_easy_setopt(curl, CURLOPT_CONNECTTIMEOUT_MS, (long)10000);
        curl_easy_setopt(curl, CURLOPT_SSL_VERIFYPEER, 1);
#ifdef _MSC_VER
        // on windows, use the native CA store for SSL peer verification
        curl_easy_setopt(curl, CURLOPT_SSL_OPTIONS, (long)CURLSSLOPT_NATIVE_CA);
#endif
        return curl;
    }
    void curl_destroy(CURL *handle)
    {
        Lock lock(connectionPool.mutex);
        connectionPool.value.reserve(connectionPool.capacity);
        if(connectionPool.value.size() == connectionPool.capacity) {
            curl_easy_cleanup(handle);
        } else {
            curl_easy_reset(handle);
            connectionPool.value.push_back(handle);
        }
    }

#ifdef __ANDROID__
    struct {
        /** `true` once the */
        std::atomic<bool> deployed {false};
        Mutex monitor;
        /** The local CA path; should be application private storage */
        std::string capath;
    } certManager;

    void installCACerts() NOTHROWS
    {
        {
            Lock lock(certManager.monitor);
            if(!certManager.deployed) {
                TAK::Engine::Port::String appFilesDir;
                if(ConfigOptions_getOption(appFilesDir, "app.dirs.files") == TE_Ok) {
                    // define the CA certs dir
                    std::ostringstream strm;
                    strm << appFilesDir.get() << "/cacerts";
                    certManager.capath = strm.str();
                    const char *capath = certManager.capath.c_str();
                    bool dirExists = false;
                    IO_exists(&dirExists, capath);
                    if(dirExists) {
                        // delete any previous contents
                        std::vector<TAK::Engine::Port::String> contents;
                        TAK::Engine::Port::STLVectorAdapter<TAK::Engine::Port::String> contents_a(contents);
                        IO_listFiles(contents_a, capath);
                        for(const auto &f : contents)
                            auto c = IO_delete(f);
                    } else {
                        // create the directory
                        IO_mkdirs(capath);
                    }
                    // rehash the system certs into the new dir
                    rehash("/system/etc/security/cacerts", capath);
                }
                // signal done
                certManager.deployed.exchange(true);
            }
        }
    }
#endif

    struct
    {
        jclass id;
        jfieldID _sink;
        jfieldID _contentLength;
        jfieldID _code;
        jfieldID _curle;
        jmethodID notify;
    } ResponseImpl_class;

    bool ResponseImpl_init(JNIEnv &env, jobject mresponse)
    {
        ResponseImpl_class.id = ATAKMapEngineJNI_findClass(&env, "gov/tak/api/engine/net/LibcurlHttpClientBuilder$ResponseImpl");
        ResponseImpl_class._sink = env.GetFieldID(ResponseImpl_class.id, "_sink", "Ljava/io/OutputStream;");
        ResponseImpl_class._contentLength = env.GetFieldID(ResponseImpl_class.id, "_contentLength", "J");
        ResponseImpl_class._code = env.GetFieldID(ResponseImpl_class.id, "_code", "I");
        ResponseImpl_class._curle = env.GetFieldID(ResponseImpl_class.id, "_curle", "I");

        ResponseImpl_class.notify = env.GetMethodID(ResponseImpl_class.id, "notify", "()V");
        return true;
    }

    struct CurlCallbackContext {
        CURL *self{nullptr};
        jobject mresponse;
        Util::ManagedDataInput2 input;
        Util::ManagedDataOutput2 output;
        struct {
            long responseCode = 0;
            curl_off_t contentLength = -1;
            bool set{false};
        } header;
    };

    struct RequestContext
    {
        jint method = 0;
        TAK::Engine::Port::String url;
        curl_slist *headers{nullptr};
        jobject mresponse{nullptr};
        jobject postData {nullptr};
        jobject responseData{nullptr};

        struct {
            std::map<int, int> intopts;
            std::map<int, bool> boolopts;
            std::map<int, TAK::Engine::Port::String> stringopts;
        } options;


        ~RequestContext()
        {
            if(headers) {
                curl_slist_free_all(headers);
                headers = nullptr;
            }
            if(mresponse || responseData || postData) {
                LocalJNIEnv env;
                if (responseData) {
                    env->DeleteGlobalRef(responseData);
                    responseData = nullptr;
                }
                if (mresponse) {
                    env->DeleteGlobalRef(mresponse);
                    mresponse = nullptr;
                }
               if (postData) {
                    env->DeleteGlobalRef(postData);
                    postData = nullptr;
                }
            }
        }
    };

    void setUserOptions(CURL *handle, const RequestContext &ctx) NOTHROWS;
    CURLcode configure(JNIEnv &env, CURL *handle, CurlCallbackContext &cb, const RequestContext &ctx) NOTHROWS;
    bool setHeaderInfo(JNIEnv *env, jobject mresponse, const CURLcode curlCode, const long responseCode, const curl_off_t contentLength) NOTHROWS
    {
        // make sure there is _something_ to set
        if(curlCode == CURLE_OK && !responseCode && contentLength == -1)
            return false;
        if(!env) {
            LocalJNIEnv lenv;
            return setHeaderInfo(lenv, mresponse, curlCode, responseCode, contentLength);
        } else {
            env->MonitorEnter(mresponse);
            if(responseCode)
                env->SetIntField(mresponse, ResponseImpl_class._code, (jint) responseCode);
            if(contentLength != -1)
                env->SetLongField(mresponse, ResponseImpl_class._contentLength, (jlong) contentLength);
            if (curlCode != CURLE_OK)
                env->SetIntField(mresponse, ResponseImpl_class._curle, curlCode);
            env->CallVoidMethod(mresponse, ResponseImpl_class.notify);
            env->MonitorExit(mresponse);
            return true;
        }
    }

    void *execute(void *opaque)
    {
        LocalJNIEnv env;
        std::unique_ptr<std::shared_ptr<RequestContext>> ptr(static_cast<std::shared_ptr<RequestContext> *>(opaque));
        auto &ctx = *(ptr->get());

        std::unique_ptr<CURL, void(*)(CURL *)> handle(curl_create(ctx.method), curl_destroy);
        setUserOptions(handle.get(), ctx);

        CURLcode code;
        CurlCallbackContext cb;
        do {
            code = configure(*env, handle.get(), cb, ctx);
            if(code != CURLE_OK)
                break;
            code = curl_easy_perform(handle.get());
            if(code != CURLE_OK)
                break;
        } while(false);

        if(code == CURLE_HTTP_RETURNED_ERROR) {
            long responseCode;
            if(curl_easy_getinfo(handle.get(), CURLINFO_RESPONSE_CODE, &responseCode) == CURLE_OK)
                cb.header.responseCode = responseCode;
        }
        // if header info wasn't set, propagate the CURL error code (and potentially any captured
        // HTTP response status code)
        if(!cb.header.set)
            setHeaderInfo(env, ctx.mresponse, code, cb.header.responseCode, cb.header.contentLength);

        return nullptr;
    }

#ifdef __ANDROID__
    void *executeInstallCACerts(void *opaque)
    {
        installCACerts();
        return execute(opaque);
    }
#endif

    bool globalInit() NOTHROWS
    {
        // IMPORTANT: the same initialization logic employed by `commoncommo` must be used here
        OPENSSL_init_ssl(0, NULL);
        SSL_load_error_strings();
        OpenSSL_add_ssl_algorithms();
        curl_global_init(CURL_GLOBAL_NOTHING);
        return true;
    }
}


JNIEXPORT jobject JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_create
        (JNIEnv *env, jclass clazz, jint method)
{
    static auto ginit = globalInit();

    std::shared_ptr<RequestContext> ctx(new RequestContext());
    ctx->method = method;
    return NewPointer(env, ctx);
}
JNIEXPORT void JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_destroy
        (JNIEnv *env, jclass clazz, jobject ptr)
{
    Pointer_destruct<RequestContext>(env, ptr);
}
JNIEXPORT jint JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_setUrl
        (JNIEnv *env, jclass clazz, jlong ptr, jstring murl)
{
    auto ctx = JLONG_TO_INTPTR(RequestContext, ptr);
    if(!ctx)
        return -1;
    JNIStringUTF url(*env, murl);
    ctx->url = url.get();
    return CURLE_OK;
}
JNIEXPORT jint JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_setHeaders
        (JNIEnv *env, jclass clazz, jlong ptr, jobjectArray mheaders)
{
    auto ctx = JLONG_TO_INTPTR(RequestContext, ptr);
    if(!ctx)
        return -1;

    const std::size_t numHeaders = env->GetArrayLength(mheaders);
    for(std::size_t i = 0u; i < numHeaders; i++) {
        Java::JNILocalRef melem(*env, env->GetObjectArrayElement(mheaders, i));
        JNIStringUTF mheader(*env, (jstring)melem.get());
        ctx->headers = curl_slist_append(ctx->headers, mheader.get());
    }
    return CURLE_OK;
}
JNIEXPORT jint JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_setBody
        (JNIEnv *env, jclass clazz, jlong ptr, jobject mdata, jstring mcontentType)
{
    auto ctx = JLONG_TO_INTPTR(RequestContext, ptr);
    ctx->postData = env->NewGlobalRef(mdata);
    if(mcontentType) {
        std::ostringstream strm;
        TAK::Engine::Port::String ccontentType;
        JNIStringUTF_get(ccontentType, *env, mcontentType);
        strm << "Content-Type: " << ccontentType;
        ctx->headers = curl_slist_append(ctx->headers, strm.str().c_str());
    }
    return CURLE_OK;
}
JNIEXPORT void JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_setResponseHandler
        (JNIEnv *env, jclass clazz, jlong ptr, jobject mresponseHandler)
{
    static bool clinit = ResponseImpl_init(*env, mresponseHandler);
    auto ctx = JLONG_TO_INTPTR(RequestContext, ptr);
    ctx->mresponse = env->NewGlobalRef(mresponseHandler);
    Java::JNILocalRef mresponseData(*env, env->GetObjectField(mresponseHandler, ResponseImpl_class._sink));
    ctx->responseData = env->NewGlobalRef(mresponseData.get());
}
JNIEXPORT jint JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_start
        (JNIEnv *env, jclass clazz, jobject ptr)
{
    std::shared_ptr<RequestContext> ctx;
    Pointer_get<RequestContext>(ctx, *env, ptr);

    TAK::Engine::Thread::ThreadPtr t(nullptr, nullptr);
#ifdef __ANDROID__
    if(!certManager.deployed)
        TAK::Engine::Thread::Thread_start(t, executeInstallCACerts, new std::shared_ptr<RequestContext>(ctx));
    else
#endif
        TAK::Engine::Thread::Thread_start(t, execute, new std::shared_ptr<RequestContext>(ctx));
    t->detach();

    return CURLE_OK;
}
JNIEXPORT void JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_setOptionInt
        (JNIEnv *env, jclass clazz, jlong ptr, jint opt, jint val)
{
    auto ctx = JLONG_TO_INTPTR(RequestContext, ptr);
    ctx->options.intopts[opt] = val;
}
JNIEXPORT void JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_setOptionBool
        (JNIEnv *env, jclass clazz, jlong ptr, jint opt, jboolean val)
{
    auto ctx = JLONG_TO_INTPTR(RequestContext, ptr);
    ctx->options.boolopts[opt] = val;
}
JNIEXPORT void JNICALL Java_gov_tak_api_engine_net_LibcurlHttpClientBuilder_setOptionString
        (JNIEnv *env, jclass clazz, jlong ptr, jint opt, jstring murl)
{
    auto ctx = JLONG_TO_INTPTR(RequestContext, ptr);
    // no string options currently supported
}

namespace {
    // callbacks
    size_t header_callback(void *buffer, size_t size, size_t nmemb, void *userdata)
    {
        auto &ctx = *static_cast<CurlCallbackContext *>(userdata);
        if(!ctx.header.responseCode || (ctx.header.responseCode/100) == 3)
            curl_easy_getinfo(ctx.self, CURLINFO_RESPONSE_CODE, &ctx.header.responseCode);
        if(ctx.header.contentLength == -1)
            curl_easy_getinfo(ctx.self, CURLINFO_CONTENT_LENGTH_DOWNLOAD_T, &ctx.header.contentLength);
        // set headers on the response object if we have both the code and content length OR we got
        // a 4xx code
        if(!ctx.header.set &&
           ctx.mresponse &&
           ctx.header.responseCode) {

            switch(ctx.header.responseCode/100) {
                case 1 : // 1xx informational
                case 3 : // 3xx redirect
                    break;
                case 2 : // 2xx success; try to wait for content length
                    if(ctx.header.contentLength == -1) break;

                case 4 : // 4xx client error
                case 5 : // 5xx server error
                    ctx.header.set = setHeaderInfo(nullptr, ctx.mresponse, CURLE_OK,
                                                   ctx.header.responseCode, ctx.header.contentLength);
                break;
            }
        }
        return size*nmemb;
    }
    size_t read_callback(void* buffer, size_t size, size_t nitems, void* userdata)
    {
        auto &ctx = *static_cast<CurlCallbackContext *>(userdata);
        size_t numRead = 0u;
        do {
            size_t n = 0u;
            if(ctx.input.read(static_cast<uint8_t *>(buffer), &numRead, (size*nitems)) != TE_Ok)
                break;
            if(n > 0)
                numRead += n;
        } while(false);
        return numRead;
    }
    size_t write_callback(void *buffer, size_t size, size_t nmemb, void *userdata)
    {
        auto &ctx = *static_cast<CurlCallbackContext *>(userdata);
        // set header info on the response object if it hasn't been set yet
        if(!ctx.header.set && ctx.mresponse) {
            ctx.header.set = setHeaderInfo(nullptr, ctx.mresponse, CURLE_OK, ctx.header.responseCode, ctx.header.contentLength);
        }
        if(ctx.output.write(static_cast<uint8_t *>(buffer), size*nmemb) != TE_Ok)
            return 0;
        return size*nmemb;
    }

    // handle configuration
    void setUserOptions(CURL *handle, const RequestContext &ctx) NOTHROWS
    {
        // int options
        for (auto &option : ctx.options.intopts) {
            const auto opt = option.first;
            const auto val = option.second;
            switch (opt) {
                case gov_tak_api_engine_net_IHttpClient_OPTION_CONNECT_TIMEOUT:
                    curl_easy_setopt(handle, CURLOPT_CONNECTTIMEOUT_MS, val);
                    break;
                case gov_tak_api_engine_net_IHttpClient_OPTION_READ_TIMEOUT:
                    curl_easy_setopt(handle, CURLOPT_TIMEOUT_MS, val);
                    break;
                default :
                    break;
            }
        }
        // bool options
        for (auto &option : ctx.options.boolopts) {
            const auto opt = option.first;
            const auto val = option.second;

            switch (opt) {
                case gov_tak_api_engine_net_IHttpClient_OPTION_FOLLOW_REDIRECTS:
                    curl_easy_setopt(handle, CURLOPT_FOLLOWLOCATION, val ? 1 : 0);
                    break;
                case gov_tak_api_engine_net_IHttpClient_OPTION_DISABLE_SSL_PEER_VERIFY:
                    curl_easy_setopt(handle, CURLOPT_SSL_VERIFYPEER, val ? 0 : 1);
                    break;
                default :
                    break;
            }
        }
        // string options
        // no string options currently supported
    }
    CURLcode configure(JNIEnv &env, CURL *handle, CurlCallbackContext &cb, const RequestContext &ctx) NOTHROWS
    {
        CURLcode code;
        do {
            cb.self = handle;
            cb.mresponse = ctx.mresponse;
            if (ctx.postData)
                cb.input.open(env, ctx.postData);
            if (ctx.responseData)
                cb.output.open(env, ctx.responseData);

            code = curl_easy_setopt(handle, CURLOPT_URL, ctx.url.get());
            if (ctx.headers) {
                code = curl_easy_setopt(handle, CURLOPT_HTTPHEADER, ctx.headers);
                if (code != CURLE_OK) break;
            }
#ifdef __ANDROID__
            // configure the system CAs path
            if (certManager.deployed) {
                curl_easy_setopt(handle, CURLOPT_CAPATH, certManager.capath.c_str());
                if (code != CURLE_OK) break;
            }
#endif
            code = curl_easy_setopt(handle, CURLOPT_HEADERFUNCTION, header_callback);
            if (code != CURLE_OK) break;
            code = curl_easy_setopt(handle, CURLOPT_HEADERDATA, &cb);
            if (code != CURLE_OK) break;
            code = curl_easy_setopt(handle, CURLOPT_WRITEFUNCTION, write_callback);
            if (code != CURLE_OK) break;
            code = curl_easy_setopt(handle, CURLOPT_WRITEDATA, &cb);
            if (code != CURLE_OK) break;
            if (ctx.postData) {
                code = curl_easy_setopt(handle, CURLOPT_POSTFIELDS, nullptr);
                if (code != CURLE_OK) break;
                code = curl_easy_setopt(handle, CURLOPT_POSTFIELDSIZE_LARGE, (curl_off_t) -1);
                if (code != CURLE_OK) break;
                code = curl_easy_setopt(handle, CURLOPT_READFUNCTION, read_callback);
                if (code != CURLE_OK) break;
                code = curl_easy_setopt(handle, CURLOPT_READDATA, &cb);
                if (code != CURLE_OK) break;
            }
        } while(false);
        return code;
    }
}
