#include "interop/java/JNIPrimitive.h"

#include "common.h"

using namespace TAKEngineJNI::Interop::Java;

namespace
{
    struct
    {
        jclass id;
        jmethodID valueOf;
    } Boolean_class;

    struct
    {
        jclass id;
        jmethodID valueOf;
    } Integer_class;

    struct
    {
        jclass id;
        jmethodID valueOf;
    } Long_class;

    struct
    {
        jclass id;
        jmethodID valueOf;
    } Double_class;

    struct
    {
        jclass id;
        jmethodID byteValue;
        jmethodID shortValue;
        jmethodID intValue;
        jmethodID longValue;
        jmethodID floatValue;
        jmethodID doubleValue;
    } Number_class;

    bool checkInit(JNIEnv &env) NOTHROWS;
    bool JNIPrimitive_class_init(JNIEnv &env) NOTHROWS;
}

JNILocalRef TAKEngineJNI::Interop::Java::Boolean_valueOf(JNIEnv &env, const bool value) NOTHROWS
{
    if(!checkInit(env))
        return JNILocalRef(env, NULL);
    return JNILocalRef(env, env.CallStaticObjectMethod(Boolean_class.id, Boolean_class.valueOf, value));
}
JNILocalRef TAKEngineJNI::Interop::Java::Integer_valueOf(JNIEnv &env, const int value) NOTHROWS
{
    if(!checkInit(env))
        return JNILocalRef(env, NULL);
    return JNILocalRef(env, env.CallStaticObjectMethod(Integer_class.id, Integer_class.valueOf, value));
}
JNILocalRef TAKEngineJNI::Interop::Java::Long_valueOf(JNIEnv &env, const int64_t value) NOTHROWS
{
    if(!checkInit(env))
        return JNILocalRef(env, NULL);
    return JNILocalRef(env, env.CallStaticObjectMethod(Long_class.id, Long_class.valueOf, value));
}
JNILocalRef TAKEngineJNI::Interop::Java::Double_valueOf(JNIEnv &env, const double value) NOTHROWS
{
    if(!checkInit(env))
        return JNILocalRef(env, NULL);
    return JNILocalRef(env, env.CallStaticObjectMethod(Double_class.id, Double_class.valueOf, value));
}

jbyte TAKEngineJNI::Interop::Java::Number_byteValue(JNIEnv &env, jobject mvalue) NOTHROWS
{
    checkInit(env);
    return env.CallByteMethod(mvalue, Number_class.byteValue);
}
jshort TAKEngineJNI::Interop::Java::Number_shortValue(JNIEnv &env, jobject mvalue) NOTHROWS
{
    checkInit(env);
    return env.CallShortMethod(mvalue, Number_class.shortValue);
}
jint TAKEngineJNI::Interop::Java::Number_intValue(JNIEnv &env, jobject mvalue) NOTHROWS
{
    checkInit(env);
    return env.CallIntMethod(mvalue, Number_class.intValue);
}
jlong TAKEngineJNI::Interop::Java::Number_longValue(JNIEnv &env, jobject mvalue) NOTHROWS
{
    checkInit(env);
    return env.CallLongMethod(mvalue, Number_class.longValue);
}
jfloat TAKEngineJNI::Interop::Java::Number_floatValue(JNIEnv &env, jobject mvalue) NOTHROWS
{
    checkInit(env);
    return env.CallFloatMethod(mvalue, Number_class.floatValue);
}
jdouble TAKEngineJNI::Interop::Java::Number_doubleValue(JNIEnv &env, jobject mvalue) NOTHROWS
{
    checkInit(env);
    return env.CallDoubleMethod(mvalue, Number_class.doubleValue);
}

namespace
{
    bool checkInit(JNIEnv &env) NOTHROWS
    {
        static bool clinit = JNIPrimitive_class_init(env);
        return clinit;
    }
    bool JNIPrimitive_class_init(JNIEnv &env) NOTHROWS
    {
        Boolean_class.id = ATAKMapEngineJNI_findClass(&env, "java/lang/Boolean");
        Boolean_class.valueOf = env.GetStaticMethodID(Boolean_class.id, "valueOf", "(Z)Ljava/lang/Boolean;");

        Integer_class.id = ATAKMapEngineJNI_findClass(&env, "java/lang/Integer");
        Integer_class.valueOf = env.GetStaticMethodID(Integer_class.id, "valueOf", "(I)Ljava/lang/Integer;");

        Long_class.id = ATAKMapEngineJNI_findClass(&env, "java/lang/Long");
        Long_class.valueOf = env.GetStaticMethodID(Long_class.id, "valueOf", "(J)Ljava/lang/Long;");

        Double_class.id = ATAKMapEngineJNI_findClass(&env, "java/lang/Double");
        Double_class.valueOf = env.GetStaticMethodID(Double_class.id, "valueOf", "(D)Ljava/lang/Double;");

        Number_class.id = ATAKMapEngineJNI_findClass(&env, "java/lang/Number");
        Number_class.byteValue = env.GetMethodID(Number_class.id, "byteValue", "()B");
        Number_class.shortValue = env.GetMethodID(Number_class.id, "shortValue", "()S");
        Number_class.intValue = env.GetMethodID(Number_class.id, "intValue", "()I");
        Number_class.longValue = env.GetMethodID(Number_class.id, "longValue", "()J");
        Number_class.floatValue = env.GetMethodID(Number_class.id, "floatValue", "()F");
        Number_class.doubleValue = env.GetMethodID(Number_class.id, "doubleValue", "()D");

        return true;
    }
}
