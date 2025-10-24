#include "common.h"

#include <algorithm>
#include <cstring>
#include <list>
#include <map>
#include <string>

#include <sqlite3.h>
#ifndef _MSC_VER
#include <spatialite.h>
#endif

#ifdef __ANDROID__
#include <android/log.h>
#endif
#ifdef _MSC_VER
// GDI+
#include <Windows.h>
#include <ObjIdl.h>

#pragma warning(push)
#pragma warning(disable : 4458)
#ifdef NOMINMAX
#define min(a, b) ((a)<(b) ? (a) : (b))
#define max(a, b) ((a)<(b) ? (a) : (b))
#include <gdiplus.h>
#pragma comment (lib, "Gdiplus.lib")
#include <gdiplusheaders.h>
#include <gdiplusfont.h>
#include <gdipluspixelformats.h>
#undef min
#undef max
#else
#include <gdiplus.h>
#pragma comment (lib, "Gdiplus.lib")
#include <gdiplusheaders.h>
#include <gdipluspixelformats.h>
#endif
#pragma warning(pop)
#endif
#include <interop/JNIStringUTF.h>
#include <interop/java/JNILocalRef.h>

#include "thread/Lock.h"
#include "thread/Mutex.h"
#include "util/Logging2.h"
#include "util/Memory.h"

#include "interop/Pointer.h"
#include "interop/java/JNILocalRef.h"

using namespace TAK::Engine::Thread;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

namespace {

    class ManagedLogger : public Logger2
    {
    public :
        ManagedLogger(JNIEnv &env) NOTHROWS;
        ~ManagedLogger() NOTHROWS;
    public :
        /** returns 0 on success, non-zero on error */
        virtual int print(const LogLevel lvl, const char *fmt, va_list arg) NOTHROWS;
    private :
        struct {
            jclass id;
            jmethodID v;
            jmethodID d;
            jmethodID i;
            jmethodID w;
            jmethodID e;
            jmethodID wtf;
        } Log_class;
        jobject mtag;
    };

    struct
    {
        jclass id;
        jmethodID loadClass;
        jobject runtime;
        jobject library;

        // XXX - need additional instances for Android and library?
    } ClassLoader_class;

    struct
    {
        jclass id;
        jmethodID ctor;
        jmethodID getContextClassLoader;
        jmethodID currentThread;
    } Thread_class;

    std::list<std::pair<std::unique_ptr<void, void(*)(const void *)>, void(*)(JNIEnv&, void *) NOTHROWS>> shutdownHooks;

#define PGSC_ENV_SET_METHOD_FN_DECL(type) \
        void envSetReturns ## type (JNIEnv *env, jobject object, jmethodID methodID, ...);

    PGSC_ENV_SET_METHOD_FN_DECL(Void);
    PGSC_ENV_SET_METHOD_FN_DECL(Byte);
    PGSC_ENV_SET_METHOD_FN_DECL(Boolean);
    PGSC_ENV_SET_METHOD_FN_DECL(Char);
    PGSC_ENV_SET_METHOD_FN_DECL(Short);
    PGSC_ENV_SET_METHOD_FN_DECL(Int);
    PGSC_ENV_SET_METHOD_FN_DECL(Long);
    PGSC_ENV_SET_METHOD_FN_DECL(Float);
    PGSC_ENV_SET_METHOD_FN_DECL(Double);
    PGSC_ENV_SET_METHOD_FN_DECL(Object);

#undef PGSC_ENV_SET_METHOD_FN_DECL

    int indexOf(const char *str, const char c, const int from);

    std::map<std::string, jclass> &classRefMap();
    Logger2 &getManagedLogger(JNIEnv &env) NOTHROWS;
    Mutex &mutex();

    void sqliteLogCallback(void *pArg, int iErrCode, const char *zMsg);

    JavaVM *vm;

#ifdef _MSC_VER
    ULONG_PTR gdiplusToken;
#endif

    jweak getJavaThreadClassLoader()
    {
        JNIEnv *env;
        int jnicode = vm->GetEnv((void **)&env, JNI_VERSION_1_6);

        // Create java thread
        Java::JNILocalRef mthread(*env, env->NewObject(Thread_class.id, Thread_class.ctor));
        // obtain a Java thread's _context classloader_
        return env->NewWeakGlobalRef(env->CallObjectMethod(mthread, Thread_class.getContextClassLoader));
    }
} // unnamed namespace

JavaIntFieldAccess *nioBuffer_position;
JavaIntFieldAccess *nioBuffer_limit;
jclass nioBufferClass;

jmethodID glLineBatch_flush;
jclass glLineBatchClass;

jclass pointDClass;
jmethodID pointD_ctor__DDD;
jfieldID pointD_x;
jfieldID pointD_y;
jfieldID pointD_z;

jclass ProgressCallback_class;
jmethodID ProgressCallback_progress;
jmethodID ProgressCallback_error;

jclass java_lang_RuntimeException_class;
jclass java_lang_IllegalArgumentException_class;
jclass java_lang_IndexOutOfBoundsException_class;
jclass java_lang_InterruptedException_class;
jclass java_lang_UnsupportedOperationException_class;
jclass java_io_IOException_class;
jclass java_lang_OutOfMemoryError_class;
jclass java_lang_IllegalStateException_class;
jclass java_util_ConcurrentModificationException_class;
jclass java_io_EOFException_class;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm_, void *reserved)
{
    vm = vm_;

    void *env_vp;
    JNIEnv *env;
    if (vm->GetEnv(&env_vp, JNI_VERSION_1_6))
        return JNI_ERR;

    env = (JNIEnv *)env_vp;

    // configure threads
    {
        Thread_class.id = ATAKMapEngineJNI_findClass(env, "java/lang/Thread");
        Thread_class.ctor = env->GetMethodID(Thread_class.id, "<init>", "()V");
        Thread_class.getContextClassLoader = env->GetMethodID(Thread_class.id, "getContextClassLoader", "()Ljava/lang/ClassLoader;");
        Thread_class.currentThread = env->GetStaticMethodID(Thread_class.id, "currentThread", "()Ljava/lang/Thread;");
    }

    // Configure classloader
    {
        ClassLoader_class.id = NULL;
        ClassLoader_class.loadClass = NULL;
        ClassLoader_class.runtime = NULL;

        ClassLoader_class.id = ATAKMapEngineJNI_findClass(env, "java/lang/ClassLoader");
        ClassLoader_class.loadClass = env->GetMethodID(ClassLoader_class.id, "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;");

        Java::JNILocalRef Class_class(*env, env->FindClass("java/lang/Class"));
        jmethodID Class_getClassLoader = env->GetMethodID(Class_class, "getClassLoader", "()Ljava/lang/ClassLoader;");

        ClassLoader_class.runtime = env->NewGlobalRef(env->CallObjectMethod(Class_class, Class_getClassLoader));

        Java::JNILocalRef EngineLibrary_class(*env, env->FindClass("com/atakmap/map/EngineLibrary"));
        ClassLoader_class.library = env->NewGlobalRef(env->CallObjectMethod(EngineLibrary_class, Class_getClassLoader));
    }

    // Load externed classes/methods
    nioBufferClass = ATAKMapEngineJNI_findClass(env, "java/nio/Buffer");
    if (!nioBufferClass)
        return JNI_ERR;

    nioBuffer_position = new JavaIntFieldAccess(env,
                                                nioBufferClass,
                                                "position",
                                                "position",
                                                "()I",
                                                "position",
                                                "(I)Ljava/nio/Buffer;");

    if (!nioBuffer_position->isValid(true, true))
        return JNI_ERR;

    nioBuffer_limit = new JavaIntFieldAccess(env,
                                             nioBufferClass,
                                             "limit",
                                             "limit",
                                             "()I",
                                             "limit",
                                             "(I)Ljava/nio/Buffer;");

    if (!nioBuffer_limit->isValid(true, true))
        return JNI_ERR;

    glLineBatchClass = ATAKMapEngineJNI_findClass(env, ATAKMAP_GL_PKG "/GLLineBatch");
    if (!glLineBatchClass)
        return JNI_ERR;

    glLineBatch_flush = env->GetMethodID(glLineBatchClass, "flush", "()V");
    if (!glLineBatch_flush)
        return JNI_ERR;

    pointDClass = ATAKMapEngineJNI_findClass(env, "com/atakmap/math/PointD");
    if(!pointDClass)
        return JNI_ERR;
    pointD_ctor__DDD = env->GetMethodID(pointDClass, "<init>", "(DDD)V");
    if(!pointD_ctor__DDD)
        return JNI_ERR;
    pointD_x = env->GetFieldID(pointDClass, "x", "D");
    if(!pointD_x)
        return JNI_ERR;
    pointD_y = env->GetFieldID(pointDClass, "y", "D");
    if(!pointD_y)
        return JNI_ERR;
    pointD_z = env->GetFieldID(pointDClass, "z", "D");
    if(!pointD_z)
        return JNI_ERR;

    Pointer_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/interop/Pointer");
    if(!Pointer_class.id)
        return JNI_ERR;
    Pointer_class.ctor__JJIJ = env->GetMethodID(Pointer_class.id, "<init>", "(JJIJ)V");
    if(!Pointer_class.ctor__JJIJ)
        return JNI_ERR;
    Pointer_class.value = env->GetFieldID(Pointer_class.id, "value", "J");
    if(!Pointer_class.value)
        return JNI_ERR;
    Pointer_class.raw = env->GetFieldID(Pointer_class.id, "raw", "J");
    if(!Pointer_class.raw)
        return JNI_ERR;
    Pointer_class.type = env->GetFieldID(Pointer_class.id, "type", "I");
    if(!Pointer_class.type)
        return JNI_ERR;
    Pointer_class.deleter = env->GetFieldID(Pointer_class.id, "deleter", "J");
    if(!Pointer_class.deleter)
        return JNI_ERR;

    ProgressCallback_class = ATAKMapEngineJNI_findClass(env, "com/atakmap/interop/ProgressCallback");
    if(!ProgressCallback_class)
        return JNI_ERR;
    ProgressCallback_progress = env->GetMethodID(ProgressCallback_class, "progress", "(I)V");
    if(!ProgressCallback_progress)
        return JNI_ERR;
    ProgressCallback_error = env->GetMethodID(ProgressCallback_class, "error", "(Ljava/lang/String;)V");
    if(!ProgressCallback_error)
        return JNI_ERR;

#define LOAD_EXCEPTION_CLASS(cn, p) \
    java_##p##_##cn##_class = (jclass)ATAKMapEngineJNI_findClass(env, "java/"#p"/"#cn); \
    if(!java_##p##_##cn##_class) \
        return JNI_ERR;

    LOAD_EXCEPTION_CLASS(RuntimeException, lang);
    LOAD_EXCEPTION_CLASS(IllegalArgumentException, lang);
    LOAD_EXCEPTION_CLASS(IndexOutOfBoundsException, lang);
    LOAD_EXCEPTION_CLASS(InterruptedException, lang);
    LOAD_EXCEPTION_CLASS(UnsupportedOperationException, lang);
    LOAD_EXCEPTION_CLASS(IOException, io);
    LOAD_EXCEPTION_CLASS(OutOfMemoryError, lang);
    LOAD_EXCEPTION_CLASS(IllegalStateException, lang);
    LOAD_EXCEPTION_CLASS(ConcurrentModificationException, util);
    LOAD_EXCEPTION_CLASS(EOFException, io);
#undef LOAD_EXCEPTION_CLASS
    LoggerPtr managedLogger(&getManagedLogger(*env), Memory_leaker_const<Logger2>);
    Logger_setLogger(std::move(managedLogger));
    sqlite3_config(SQLITE_CONFIG_LOG, sqliteLogCallback, NULL);
#ifdef __ANDROID__
    spatialite_init(1);
#endif

#ifdef _MSC_VER
    // GDI+ init for windows builds
    Gdiplus::GdiplusStartupInput input;
    Gdiplus::Status s = Gdiplus::GdiplusStartup(&gdiplusToken, &input, nullptr);
#endif
    return JNI_VERSION_1_6;
}

JNIEXPORT void JNICALL JNI_OnUnload(JavaVM *vm, void *reserved)
{
#ifdef _MSC_VER
    // GDI+ teardown for windows builds
    Gdiplus::GdiplusShutdown(gdiplusToken);
#endif

    void *env_vp;
    JNIEnv *env;
    if (vm->GetEnv(&env_vp, JNI_VERSION_1_6))
        return; // really shouldn't happen, if we got through JNI_OnLoad
    env = (JNIEnv *)env_vp;

    Lock lock(mutex());

    for (auto it = shutdownHooks.begin(); it != shutdownHooks.end(); it++)
        (*it).second(*env, (*it).first.get());
    shutdownHooks.clear();

    std::map<std::string, jclass>::iterator entry;
    for(entry = classRefMap().begin(); entry != classRefMap().end(); entry++)
        env->DeleteGlobalRef(entry->second);
    classRefMap().clear();

    Logger_setLogger(LoggerPtr(nullptr, nullptr));

    vm = NULL;
}


/******************************************************************************/

JavaIntFieldAccess::JavaIntFieldAccess(JNIEnv *env,
                       jclass clazz,
                       const char *fieldName,
                       const char *getMethodName,
                       const char *getMethodSig,
                       const char *setMethodName,
                       const char *setMethodSig) :

        fieldId(0),
        setMethodId(0),
        getMethodId(0),
        getImpl(0),
        setImpl(0),
        envSetMethodImpl(0)
{

    if(fieldName) {
        fieldId = env->GetFieldID(clazz, fieldName, "I");
        if(fieldId) {
            getImpl = &getDirect;

            // XXX - need to check final ???
            setImpl = &setDirect;
        } else {
            // fields are probably part of the private or protected API and may
            // be subject to change between versions -- try to handle this
            // gracefully by allow method lookup to occur
            if(env->ExceptionCheck())
                env->ExceptionClear();

            jclass langClassClass = env->FindClass("java/lang/Class");
            jmethodID langClass_getName = env->GetMethodID(langClassClass, "getName", "()Ljava/lang/String;");
            Java::JNILocalRef jclassName(*env, env->CallObjectMethod(clazz, langClass_getName));
            const char *className = env->GetStringUTFChars(jclassName, 0);
#ifdef __ANDROID__
            __android_log_print(ANDROID_LOG_ERROR, "ATAKMapEngineJNI", "Field lookup failed. Class: %s Field: %s\n", className, fieldName);
#endif
            env->ReleaseStringUTFChars(jclassName, className);
        }
    }

    if(!getImpl && getMethodName && getMethodSig) {
        getMethodId = env->GetMethodID(clazz, getMethodName, getMethodSig);
        if(getMethodId) {
            getImpl = &getMethod;
        } else {
            // log the method lookup failure -- don't clear the exception as
            // there is no appropriate error handling at this point
#ifdef __ANDROID__
            __android_log_print(ANDROID_LOG_ERROR, "ATAKMapEngineJNI", "Method lookup failed. Method: %s%s\n", getMethodName, getMethodSig);
#endif
        }
    }

    if(!setImpl && setMethodName && setMethodSig) {
        setMethodId = env->GetMethodID(clazz, setMethodName, setMethodSig);
        if(setMethodId) {
            int idx = indexOf(setMethodSig, ')', 0);
            if(idx > 0 && idx < (strlen(setMethodSig)-1)) {
                switch(setMethodSig[idx+1]) {
                    case 'Z' : // boolean
                        envSetMethodImpl = &envSetReturnsBoolean;
                        break;
                    case 'B' : // byte
                        envSetMethodImpl = &envSetReturnsByte;
                        break;
                    case 'C' : // char
                        envSetMethodImpl = &envSetReturnsChar;
                        break;
                    case 'S' : // short
                        envSetMethodImpl = &envSetReturnsShort;
                        break;
                    case 'I' : // int
                        envSetMethodImpl = &envSetReturnsInt;
                        break;
                    case 'J' : // long
                        envSetMethodImpl = &envSetReturnsLong;
                        break;
                    case 'F' : // float
                        envSetMethodImpl = &envSetReturnsDouble;
                        break;
                    case 'D' : // double
                        envSetMethodImpl = &envSetReturnsFloat;
                        break;
                    case 'L' : // object
                    case '[' : // array
                        envSetMethodImpl = &envSetReturnsObject;
                        break;
                    default :
                        break;
                }
            }

            if(envSetMethodImpl)
                setImpl = &setMethod;
        } else {
            // log the method lookup failure -- don't clear the exception as
            // there is no appropriate error handling at this point
#ifdef __ANDROID__
            __android_log_print(ANDROID_LOG_ERROR, "ATAKMapEngineJNI", "Method lookup failed. Method: %s%s\n", setMethodName, setMethodSig);
#endif
        }
    }
}

JavaIntFieldAccess::~JavaIntFieldAccess()
{
}

jint JavaIntFieldAccess::get(JNIEnv *env, jobject object)
{
    return getImpl(this, env, object);
}

void JavaIntFieldAccess::set(JNIEnv *env, jobject object, jint value)
{
    setImpl(this, env, object, value);
}

bool JavaIntFieldAccess::isValid(bool get, bool set)
{
    return (!get || getImpl) && (!set || setImpl);
}

jint JavaIntFieldAccess::getDirect(JavaIntFieldAccess *self, JNIEnv *env, jobject object)
{
    return env->GetIntField(object, self->fieldId);
}

void JavaIntFieldAccess::setDirect(JavaIntFieldAccess *self, JNIEnv *env, jobject object, jint value)
{
    env->SetIntField(object, self->fieldId, value);
}

jint JavaIntFieldAccess::getMethod(JavaIntFieldAccess *self, JNIEnv *env, jobject object)
{
    return env->CallIntMethod(object, self->getMethodId);
}

void JavaIntFieldAccess::setMethod(JavaIntFieldAccess *self, JNIEnv *env, jobject object, jint value)
{
    self->envSetMethodImpl(env, object, self->setMethodId, value);
}

namespace
{
    bool getJniEnv(JNIEnv **penv, bool *shouldDetach) NOTHROWS
    {
        *shouldDetach = false;
        *penv = nullptr;

        if(!vm)
            return false;

        JNIEnv *value = nullptr;
        int code = vm->GetEnv((void **)&value, JNI_VERSION_1_6);
        if(code == JNI_EDETACHED) {
            JavaVMAttachArgs args;
            args.version = JNI_VERSION_1_6;
            args.group = NULL;
            args.name = NULL;
#ifdef __ANDROID__
            if(vm->AttachCurrentThread(&value, &args) == 0) {
#else
            if(vm->AttachCurrentThread((void **)&value, &args) == 0) {
#endif
                // attached to thread, should detach
                *shouldDetach = true;
            } else {
                // failed to attach to thread
                return false;
            }
        } else if(code != JNI_OK) {
            // error
            return false;
        }
        *penv = value;
        return true;
    }
    class LocalJNIEnvImpl
    {
    public :
        LocalJNIEnvImpl() NOTHROWS :
                value(NULL),
                detach(false)
        {
            static jweak javaClassLoader = getJavaThreadClassLoader();
            getJniEnv(&value, &detach);
            if(value && !detach) {
                // obtain the current thread's _context classloader_
                Java::JNILocalRef mthread(*value, value->CallStaticObjectMethod(Thread_class.id, Thread_class.currentThread));
                Java::JNILocalRef mtcl(*value, value->CallObjectMethod(mthread, Thread_class.getContextClassLoader));
                // If they are not equal then assume this is a native thread
                if (!value->IsSameObject(mtcl, javaClassLoader)) {
                    value = nullptr;
                }
            }
        }
        ~LocalJNIEnvImpl() NOTHROWS
        {
            if(detach && vm)
                vm->DetachCurrentThread();
        }
    public :
        JNIEnv *value;
        bool detach;
    };
}

LocalJNIEnv::LocalJNIEnv() NOTHROWS :
  env(nullptr),
  detach(false)
{
    // desktop linux observed failing to exit during unit tests
#if defined(__ANDROID__) || !(defined(__LINUX__) || defined(__APPLE__))
    // associate `JNIEnv` using TLS to significantly reduce overhead with native initiated JNI
    // invocations
    thread_local LocalJNIEnvImpl impl;
    env = impl.value;

    // there may be no _thread local_ env if the current thread is a native thread originating from
    // another library
    if(!env)
#endif
    getJniEnv(&env, &detach);
}
LocalJNIEnv::~LocalJNIEnv() NOTHROWS
{
    if(detach && vm)
        vm->DetachCurrentThread();
}
bool LocalJNIEnv::valid() NOTHROWS
{
    return !!env;
}
JNIEnv *LocalJNIEnv::operator->() const NOTHROWS
{
    return env;
}
JNIEnv &LocalJNIEnv::operator*() const NOTHROWS
{
    return *env;
}
LocalJNIEnv::operator JNIEnv *() const NOTHROWS
{
    return env;
}

bool ATAKMapEngineJNI_checkOrThrow(JNIEnv *env, const TAKErr code) NOTHROWS
{
    switch(code) {
    case TE_Ok :
    case TE_Done : // XXX - throws?
    case TE_Canceled : // XXX - throws
    case TE_Busy : // XXX -
        return false;
    case TE_InvalidArg :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_IllegalArgumentException_class, "");
        return true;
    case TE_Interrupted :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_InterruptedException_class, "");
        return true;
    case TE_BadIndex :
        if(!env->ExceptionCheck())
             env->ThrowNew(java_lang_IndexOutOfBoundsException_class, "");
        return true;
    case TE_NotImplemented :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_UnsupportedOperationException_class, "");
        return true;
    case TE_IO :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_io_IOException_class, "");
        return true;
    case TE_OutOfMemory :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_OutOfMemoryError_class, "");
        return true;
    case TE_IllegalState :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_lang_IllegalStateException_class, "");
        return true;
    case TE_ConcurrentModification :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_util_ConcurrentModificationException_class, "");
        return true;
    case TE_EOF :
        if(!env->ExceptionCheck())
            env->ThrowNew(java_io_EOFException_class, "");
        return true;
    case TE_Err :
    default :
       if(!env->ExceptionCheck())
           env->ThrowNew(java_lang_RuntimeException_class, "An error occurred");
        return true;
    }
}

jclass ATAKMapEngineJNI_findClass(JNIEnv *env, const char *name) NOTHROWS
{
    if(!name)
        return NULL;

    Mutex &m = mutex();
    Lock lock(m);
    if(lock.status != TE_Ok)
        return NULL;

    std::map<std::string, jclass> &classMap = classRefMap();
    std::map<std::string, jclass>::iterator entry;
    entry = classMap.find(name);
    if(entry != classMap.end())
        return entry->second;
    if(!env)
        return NULL;
    Java::JNILocalRef localClassRef(*env, NULL);
    do {
        // load from JNIEnv
        localClassRef = Java::JNILocalRef(*env, env->FindClass(name));
        if(env->ExceptionCheck())
            env->ExceptionClear();
        if(localClassRef)
            break;

        // load from ClassLoader

        // the name needs to be massaged to replace slashes with dots
        std::string namex(name);
        std::replace(namex.begin(), namex.end(), '/', '.');
        Java::JNILocalRef mname(*env, env->NewStringUTF(namex.c_str()));
        if(ClassLoader_class.library) {
            localClassRef = Java::JNILocalRef(*env, env->CallObjectMethod(ClassLoader_class.library,
                                                                          ClassLoader_class.loadClass,
                                                                          (jstring) mname));
            if (env->ExceptionCheck())
                env->ExceptionClear();
            if (localClassRef)
                break;
        }

        if(ClassLoader_class.runtime) {
            localClassRef = Java::JNILocalRef(*env, env->CallObjectMethod(ClassLoader_class.runtime,
                                                                          ClassLoader_class.loadClass,
                                                                          (jstring) mname));
            if (env->ExceptionCheck())
                env->ExceptionClear();
            if (localClassRef)
                break;
        }
    } while(false);
    if(!localClassRef) {
        Logger_log(TELL_Warning, "ATAKMapEngineJNI_findClass: failed to find %s", name);
        return NULL;
    }
    jclass clazz = (jclass)env->NewGlobalRef(localClassRef);
    classMap[name] = clazz;
    return clazz;
}

bool ATAKMapEngineJNI_equals(JNIEnv *env, jobject a, jobject b) NOTHROWS
{
    if(!a && !b)
        return true;
    else if(!a)
        return false;
    else if(!b)
        return false;
    // both are non-null
    if(env->IsSameObject(a, b))
        return true;
    jclass Object_class = ATAKMapEngineJNI_findClass(env, "java/lang/Object");
    jmethodID Object_equals = env->GetMethodID(Object_class, "equals", "(Ljava/lang/Object;)Z");
    return env->CallBooleanMethod(a, Object_equals, b);
}

TAKErr ATAKMapEngineJNI_SetStringField(JNIEnv &env, jobject obj, jfieldID fieldid, const char *cstr) NOTHROWS
{
    Java::JNILocalRef mstr(env, nullptr);
    if(cstr) {
        mstr = Java::JNILocalRef(env, env.NewStringUTF(cstr));
        if(env.ExceptionCheck())
            return TE_Err;
    }
    env.SetObjectField(obj, fieldid, mstr.get());
    if(env.ExceptionCheck())
        return TE_Err;
    return TE_Ok;
}
TAKErr ATAKMapEngineJNI_GetStringField(TAK::Engine::Port::String &value, JNIEnv &env, jobject obj, jfieldID fieldid) NOTHROWS
{
    Java::JNILocalRef mstr(env, env.GetObjectField(obj, fieldid));
    if(env.ExceptionCheck())
        return TE_Err;
    return JNIStringUTF_get(value, env, (jstring)mstr);
}

void ATAKMapEngineJNI_registerShutdownHook(void(*hook)(JNIEnv &, void *) NOTHROWS, std::unique_ptr<void, void(*)(const void *)> &&opaque) NOTHROWS
{
    Lock lock(mutex());
    shutdownHooks.push_back(std::make_pair(std::move(opaque), hook));
}
void ATAKMapEngineJNI_unregisterShutdownHook(std::unique_ptr<void, void(*)(const void *)> &hook, const void *opaque) NOTHROWS
{
    Lock lock(mutex());
    for (auto it = shutdownHooks.begin(); it != shutdownHooks.end(); it++) {
        if ((*it).first.get() == opaque) {
            hook = std::move((*it).first);
            break;
        }
    }
}

TAKErr ProgressCallback_dispatchProgress(jobject jcallback, jint value) NOTHROWS
{
    LocalJNIEnv env;
    if(!env.valid())
        return TE_Err;
    if(env->ExceptionCheck())
        return TE_Err;
    env->CallVoidMethod(jcallback, ProgressCallback_progress, value);
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}
TAKErr ProgressCallback_dispatchError(jobject jcallback, const char *value) NOTHROWS
{
    LocalJNIEnv env;
    if(!env.valid())
        return TE_Err;
    if(env->ExceptionCheck())
        return TE_Err;
    Java::JNILocalRef mvalue(*env, value ? env->NewStringUTF(value) : NULL);
    env->CallVoidMethod(jcallback, ProgressCallback_error, mvalue.get());
    return env->ExceptionCheck() ? TE_Err : TE_Ok;
}

void Thread_dumpStack() NOTHROWS
{
    LocalJNIEnv env;
    jclass Thread_class = ATAKMapEngineJNI_findClass(env, "java/lang/Thread");
    jmethodID dumpStack = env->GetStaticMethodID(Thread_class, "dumpStack", "()V");
    env->CallStaticVoidMethod(Thread_class, dumpStack);
}
TAK::Engine::Port::String Object_toString(jobject obj) NOTHROWS
{
    if(!obj)
        return nullptr;
    LocalJNIEnv env;
    jclass Object_class = ATAKMapEngineJNI_findClass(env, "java/lang/Object");
    jmethodID toString = env->GetMethodID(Object_class, "toString", "()Ljava/lang/String;");
    Java::JNILocalRef mstr(*env, env->CallObjectMethod(obj, toString));
    TAK::Engine::Port::String cstr;
    JNIStringUTF_get(cstr, *env, mstr);
    return cstr;
}
bool ATAKMapEngineJNI_ExceptionCheck(JNIEnv &env, const bool clear) NOTHROWS
{
    const auto v = env.ExceptionCheck();
    if(v && clear)
        env.ExceptionClear();
    return v;
}

namespace {

        ManagedLogger::ManagedLogger(JNIEnv &env) NOTHROWS :
    mtag(nullptr)
{
#ifndef __ANDROID__
    Log_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/coremap/log/Log");
    Log_class.v = env.GetStaticMethodID(Log_class.id, "v", "(Ljava/lang/String;Ljava/lang/String;)I");
    Log_class.d = env.GetStaticMethodID(Log_class.id, "d", "(Ljava/lang/String;Ljava/lang/String;)I");
    Log_class.i = env.GetStaticMethodID(Log_class.id, "i", "(Ljava/lang/String;Ljava/lang/String;)I");
    Log_class.w = env.GetStaticMethodID(Log_class.id, "w", "(Ljava/lang/String;Ljava/lang/String;)I");
    Log_class.e = env.GetStaticMethodID(Log_class.id, "e", "(Ljava/lang/String;Ljava/lang/String;)I");
    Log_class.wtf = env.GetStaticMethodID(Log_class.id, "wtf", "(Ljava/lang/String;Ljava/lang/String;)I");

    Java::JNILocalRef mtag_local(env, env.NewStringUTF("TAKEngineJNI"));
    mtag = env.NewGlobalRef(mtag_local);
#endif
}
ManagedLogger::~ManagedLogger() NOTHROWS
{
#ifndef __ANDROID
    // NOTE: global ref to `mtag` leaks on JVM exit
#endif
}
#ifdef __ANDROID__
int ManagedLogger::print(const LogLevel lvl, const char *fmt, va_list arg) NOTHROWS
{
    android_LogPriority prio;
    switch(lvl) {
    case TELL_All :
        prio = ANDROID_LOG_VERBOSE;
        break;
    case TELL_Debug :
        prio =  ANDROID_LOG_DEBUG;
        break;
    case TELL_Info :
        prio = ANDROID_LOG_INFO;
        break;
    case TELL_Warning :
        prio = ANDROID_LOG_WARN;
        break;
    case TELL_Error :
        prio = ANDROID_LOG_ERROR;
        break;
    case TELL_Severe :
        prio = ANDROID_LOG_FATAL;
        break;
    case TELL_None :
        prio = ANDROID_LOG_SILENT;
        break;
    default :
        prio = ANDROID_LOG_DEFAULT;
        break;
    }
    __android_log_vprint(prio, "ATAKMapEngineJNI", fmt, arg);
    return 0;
}
#else
int ManagedLogger::print(const LogLevel lvl, const char *fmt, va_list arg) NOTHROWS
{
    jmethodID log = nullptr;
    switch(lvl) {
    case TELL_All :
        log = Log_class.v;
        break;
    case TELL_Debug :
        log = Log_class.d;
        break;
    case TELL_Info :
        log = Log_class.i;
        break;
    case TELL_Warning :
        log = Log_class.w;
        break;
    case TELL_Error :
        log = Log_class.e;
        break;
    case TELL_Severe :
        log = Log_class.wtf;
        break;
    case TELL_None :
        log = nullptr;
        break;
    default :
        log = Log_class.i;
        break;
    }
    if(log) {
        char msgbuf[256];
        vsnprintf(msgbuf, 255u, fmt, arg);
        LocalJNIEnv env;
        Java::JNILocalRef mmsg(*env, env->NewStringUTF(msgbuf));
        env->CallStaticIntMethod(Log_class.id, log, mtag, mmsg.get());
    }
    return 0;
}
#endif
#define PGSC_ENV_SET_METHOD_FN_DEFN(type)                                      \
        void envSetReturns ## type (JNIEnv *env,                               \
                                    jobject object,                            \
                                    jmethodID methodId,                        \
                                    ...)                                       \
        {                                                                      \
            va_list args;                                                      \
            va_start(args, methodId);                                          \
                                                                               \
            env->Call ## type ## MethodV(object, methodId, args);              \
        }                                                                      \

PGSC_ENV_SET_METHOD_FN_DEFN(Void)
PGSC_ENV_SET_METHOD_FN_DEFN(Byte)
PGSC_ENV_SET_METHOD_FN_DEFN(Boolean)
PGSC_ENV_SET_METHOD_FN_DEFN(Char)
PGSC_ENV_SET_METHOD_FN_DEFN(Short)
PGSC_ENV_SET_METHOD_FN_DEFN(Int)
PGSC_ENV_SET_METHOD_FN_DEFN(Long)
PGSC_ENV_SET_METHOD_FN_DEFN(Float)
PGSC_ENV_SET_METHOD_FN_DEFN(Double)
PGSC_ENV_SET_METHOD_FN_DEFN(Object)

#undef PGSC_ENV_SET_METHOD_FN_DEFN

int indexOf(const char *str, const char c, const int from)
{
    const int l = strlen(str);
    for(int i = from; i < l; i++)
        if(str[i] == c)
            return i;
    return -1;
}

Logger2 &getManagedLogger(JNIEnv &env) NOTHROWS
{
    static ManagedLogger mlogger(env);
    return mlogger;
}

std::map<std::string, jclass> &classRefMap()
{
    static std::map<std::string, jclass> m;
    return m;
}

Mutex &mutex()
{
    static Mutex m;
    return m;
}

void sqliteLogCallback(void *pArg, int iErrCode, const char *zMsg)
{
    const char *errStr = sqlite3_errstr(iErrCode);
    if(errStr)
        Logger_log(TELL_Debug, "SQLITE [%s] %s", errStr, zMsg ? zMsg : "");
    else
        Logger_log(TELL_Debug, "SQLITE [code=%d] %s", zMsg ? zMsg : "");
}

};
