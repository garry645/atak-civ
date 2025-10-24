#include "interop/java/JNICollection.h"

#include "common.h"
#include "interop/JNIStringUTF.h"
#include "interop/java/JNIPrimitive.h"

using namespace TAKEngineJNI::Interop::Java;

using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

namespace
{
    struct
    {
        jclass id;
        jmethodID add;
        jmethodID remove;
        jmethodID clear;
        jmethodID iterator;
        jmethodID size;
    } Collection_class;

    struct
    {
        jclass id;
        jmethodID ctor;
    } ArrayList_class;

    struct
    {
        jclass id;
        jmethodID ctor;
    } LinkedList_class;

    struct
    {
        jclass id;
        jmethodID ctor;
    } HashSet_class;

    bool checkInit(JNIEnv &env) NOTHROWS;
    bool JNICollection_class_init(JNIEnv &env) NOTHROWS;
}

JNILocalRef TAKEngineJNI::Interop::Java::JNICollection_create(JNIEnv &env, const JNICollectionClass type)
{
    if(!checkInit(env))
        return JNILocalRef(env, NULL);
    jclass clazz;
    jmethodID ctor;
    switch(type) {
        case ArrayList :
            clazz = ArrayList_class.id;
            ctor = ArrayList_class.ctor;
            break;
        case LinkedList :
            clazz = LinkedList_class.id;
            ctor = LinkedList_class.ctor;
            break;
        case HashSet :
            clazz = HashSet_class.id;
            ctor = HashSet_class.ctor;
            break;
        default :
            return JNILocalRef(env, NULL);
    }

    return JNILocalRef(env, env.NewObject(clazz, ctor));
}
TAKErr TAKEngineJNI::Interop::Java::JNICollection_add(JNIEnv &env, jobject collection, jobject element) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;
    if(!collection)
        return TE_InvalidArg;
    if(env.ExceptionCheck())
        return TE_Err;
    env.CallBooleanMethod(collection, Collection_class.add, element);
    if(env.ExceptionCheck())
        return TE_Err;
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Java::JNICollection_remove(JNIEnv &env, jobject collection, jobject element) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;
    if(!collection)
        return TE_InvalidArg;
    if(env.ExceptionCheck())
        return TE_Err;
    env.CallBooleanMethod(collection, Collection_class.remove, element);
    if(env.ExceptionCheck())
        return TE_Err;
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Java::JNICollection_size(std::size_t &size, JNIEnv &env, jobject collection) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;
    if(!collection)
        return TE_InvalidArg;
    if(env.ExceptionCheck())
        return TE_Err;
    size = (std::size_t)env.CallIntMethod(collection, Collection_class.size);
    if(env.ExceptionCheck())
        return TE_Err;
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Java::JNICollection_clear(JNIEnv &env, jobject collection) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;
    if(!collection)
        return TE_InvalidArg;
    if(env.ExceptionCheck())
        return TE_Err;
    env.CallVoidMethod(collection, Collection_class.clear);
    if(env.ExceptionCheck())
        return TE_Err;
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Java::JNICollection_iterator(JNILocalRef &iterator, JNIEnv &env, jobject collection) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;
    if(!collection)
        return TE_InvalidArg;
    if(env.ExceptionCheck())
        return TE_Err;
    iterator = JNILocalRef(env, env.CallObjectMethod(collection, Collection_class.iterator));
    if(env.ExceptionCheck())
        return TE_Err;
    return TE_Ok;
}
TAKErr TAKEngineJNI::Interop::Java::JNICollection_addAll(TAK::Engine::Port::Collection<TAK::Engine::Port::String> *cvalues, JNIEnv &env, jobject mvalues) NOTHROWS
{
    return JNICollection_addAll<TAK::Engine::Port::String>(cvalues, env, mvalues,
    [&](JNIEnv &env, jobject melem)
    {
        TAK::Engine::Port::String celem;
        JNIStringUTF_get(celem, env, (jstring)melem);
        return celem;
    });
}
TAKErr TAKEngineJNI::Interop::Java::JNICollection_addAll(jobject mvalues, JNIEnv &env, const TAK::Engine::Port::Collection<TAK::Engine::Port::String> &cvalues) NOTHROWS
{
    return JNICollection_addAll<TAK::Engine::Port::String>(mvalues, env, cvalues,
    [&](JNILocalRef &melem, JNIEnv &env, const TAK::Engine::Port::String &celem)
    {
        melem = JNILocalRef(env, env.NewStringUTF(celem));
        return TE_Ok;
    });
}
TAKErr TAKEngineJNI::Interop::Java::JNICollection_addAll(TAK::Engine::Port::Collection<int64_t> *cvalues, JNIEnv &env, jobject mvalues) NOTHROWS
{
    return JNICollection_addAll<int64_t>(cvalues, env, mvalues,
    [&](JNIEnv &env, jobject melem)
    {
        return Number_longValue(env, melem);
    });
}
TAKErr TAKEngineJNI::Interop::Java::JNICollection_addAll(jobject mvalues, JNIEnv &env, const TAK::Engine::Port::Collection<int64_t> &cvalues) NOTHROWS
{
    return JNICollection_addAll<int64_t>(mvalues, env, cvalues,
    [](JNILocalRef &melem, JNIEnv &env, const int64_t &celem)
    {
        melem = Long_valueOf(env, celem);
        return TE_Ok;
    });
}


namespace
{
    bool checkInit(JNIEnv &env) NOTHROWS
    {
        static bool clinit = JNICollection_class_init(env);
        return clinit;
    }
    bool JNICollection_class_init(JNIEnv &env) NOTHROWS
    {
        Collection_class.id = ATAKMapEngineJNI_findClass(&env, "java/util/Collection");
        Collection_class.add = env.GetMethodID(Collection_class.id, "add", "(Ljava/lang/Object;)Z");
        Collection_class.remove = env.GetMethodID(Collection_class.id, "remove", "(Ljava/lang/Object;)Z");
        Collection_class.clear = env.GetMethodID(Collection_class.id, "clear", "()V");
        Collection_class.iterator = env.GetMethodID(Collection_class.id, "iterator", "()Ljava/util/Iterator;");
        Collection_class.size = env.GetMethodID(Collection_class.id, "size", "()I");

        ArrayList_class.id = ATAKMapEngineJNI_findClass(&env, "java/util/ArrayList");
        ArrayList_class.ctor = env.GetMethodID(ArrayList_class.id, "<init>", "()V");

        LinkedList_class.id = ATAKMapEngineJNI_findClass(&env, "java/util/LinkedList");
        LinkedList_class.ctor = env.GetMethodID(LinkedList_class.id, "<init>", "()V");

        HashSet_class.id = ATAKMapEngineJNI_findClass(&env, "java/util/HashSet");
        HashSet_class.ctor = env.GetMethodID(HashSet_class.id, "<init>", "()V");

        return true;
    }
}
