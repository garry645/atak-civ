#ifndef TAKENGINEJNI_INTEROP_JAVA_JNIPRIMITIVE_H_INCLUDED
#define TAKENGINEJNI_INTEROP_JAVA_JNIPRIMITIVE_H_INCLUDED

#include <jni.h>

#include <port/Platform.h>
#include <util/Error.h>

#include "interop/java/JNILocalRef.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Java {
            JNILocalRef Boolean_valueOf(JNIEnv &env, const bool value) NOTHROWS;
            JNILocalRef Integer_valueOf(JNIEnv &env, const int value) NOTHROWS;
            JNILocalRef Long_valueOf(JNIEnv &env, const int64_t value) NOTHROWS;
            JNILocalRef Double_valueOf(JNIEnv &env, const double value) NOTHROWS;

            jbyte Number_byteValue(JNIEnv &env, jobject mvalue) NOTHROWS;
            jshort Number_shortValue(JNIEnv &env, jobject mvalue) NOTHROWS;
            jint Number_intValue(JNIEnv &env, jobject mvalue) NOTHROWS;
            jlong Number_longValue(JNIEnv &env, jobject mvalue) NOTHROWS;
            jfloat Number_floatValue(JNIEnv &env, jobject mvalue) NOTHROWS;
            jdouble Number_doubleValue(JNIEnv &env, jobject mvalue) NOTHROWS;
        }
    }
}
#endif
