
#include "com_atakmap_map_layer_elevation_NativeGradientControl.h"

#include <elevation/GradientControl.h>
#include <util/Logging2.h>
#include <port/STLVectorAdapter.h>
#include <port/String.h>

#include "common.h"
#include "interop/Pointer.h"
#include "interop/java/JNILocalRef.h"

#include <limits.h>

using namespace TAK::Engine;
using namespace TAK::Engine::Util;
using namespace TAK::Engine::Port;

using namespace TAKEngineJNI::Interop;

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_elevation_NativeGradientControl_destruct
        (JNIEnv *env, jclass, jobject mpointer) {

    Pointer_destruct_iface<TAK::Engine::Elevation::GradientControl>(env, mpointer);
}

JNIEXPORT jintArray JNICALL Java_com_atakmap_map_layer_elevation_NativeGradientControl_getGradientColors
        (JNIEnv *env, jclass, jlong ptr) {


    TAK::Engine::Elevation::GradientControl *control = JLONG_TO_INTPTR(TAK::Engine::Elevation::GradientControl, ptr);
    if(!control) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    std::vector<uint32_t> gradientColorsVec;
    STLVectorAdapter<uint32_t> gradientColors(gradientColorsVec);
    TAKErr code = control->getGradientColors(gradientColors);
    ATAKMapEngineJNI_checkOrThrow(env, code);

    Collection<uint32_t>::IteratorPtr intIter(nullptr, nullptr);
    auto intCount = static_cast<jsize>(gradientColors.size() & INT_MAX);
    jintArray colorArray = nullptr;
    Java::JNILocalRef colorArrayRef(*env, colorArray = env->NewIntArray(intCount));

    code = gradientColors.iterator(intIter);
    ATAKMapEngineJNI_checkOrThrow(env, code);

    if (intIter) {
        uint32_t color;
        for (jsize i = 0; intIter->get(color) != TE_Done && i < intCount; ++i, intIter->next()) {
            jint jintColor = static_cast<jint>(color);
            env->SetIntArrayRegion(colorArray, i, 1, &jintColor);
        }
    }

    colorArrayRef.release();
    return colorArray;
}

JNIEXPORT jobjectArray JNICALL Java_com_atakmap_map_layer_elevation_NativeGradientControl_getLineItemString
        (JNIEnv *env, jclass, jlong ptr) {


    TAK::Engine::Elevation::GradientControl *control = JLONG_TO_INTPTR(TAK::Engine::Elevation::GradientControl, ptr);
    if(!control) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    std::vector<TAK::Engine::Port::String> itemNamesVec;
    STLVectorAdapter<String> itemNames(itemNamesVec);
    TAKErr code = control->getLineItemStrings(itemNames);
    ATAKMapEngineJNI_checkOrThrow(env, code);

    Collection<String>::IteratorPtr stringIter(nullptr, nullptr);
    auto stringCount = static_cast<jsize>(itemNames.size() & INT_MAX);
    jobjectArray stringArray = nullptr;
    Java::JNILocalRef stringArrayRef(*env, stringArray = env->NewObjectArray(stringCount, ATAKMapEngineJNI_findClass(env, "java/lang/String"), NULL));

    code = itemNames.iterator(stringIter);
    ATAKMapEngineJNI_checkOrThrow(env, code);

    if (stringIter) {
        String str;
        for (jsize i = 0; stringIter->get(str) != TE_Done && i < stringCount; ++i, stringIter->next()) {
            env->SetObjectArrayElement(stringArray, i, env->NewStringUTF(str));
        }
    }

    stringArrayRef.release();
    return stringArray;
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_elevation_NativeGradientControl_getMode
        (JNIEnv *env, jclass, jlong ptr) {


    TAK::Engine::Elevation::GradientControl *control = JLONG_TO_INTPTR(TAK::Engine::Elevation::GradientControl, ptr);
    if(!control) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return -1;
    }

    int mode = -1;
    TAKErr code = control->getMode(&mode);
    ATAKMapEngineJNI_checkOrThrow(env, code);
    return mode;
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_elevation_NativeGradientControl_setMode
        (JNIEnv *env, jclass, jlong ptr, jint mmode) {


    TAK::Engine::Elevation::GradientControl *control = JLONG_TO_INTPTR(TAK::Engine::Elevation::GradientControl, ptr);
    if(!control) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    TAKErr code = control->setMode(mmode);
    ATAKMapEngineJNI_checkOrThrow(env, code);
}
