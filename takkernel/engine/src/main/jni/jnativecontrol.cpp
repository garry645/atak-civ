
#include "com_atakmap_map_opengl_NativeControl.h"

#include <core/MapRenderer3.h>
#include <core/Layer2.h>
#include <util/Logging2.h>
#include <core/LegacyAdapters.h>

#include "common.h"
#include "interop/Pointer.h"
#include "interop/core/Interop.h"
#include "interop/JNIStringUTF.h"

using namespace TAK::Engine::Util;
using namespace TAK::Engine::Core;

namespace {
    struct {
        jclass id;
        jclass Visitor_id;
        jmethodID Visitor_onVisitNativeControl;
        jmethodID ctor;
    } NativeControl_class;

    bool checkInit(JNIEnv &env) NOTHROWS;
    bool NativeControl_init(JNIEnv &env) NOTHROWS;

    struct VisitorContext {
        JNIEnv *env;
        jobject visitor;
    };
    TAKErr visitor(void *opaque, const Layer2 &layer, const Control &ctrl);
}

void commonVisitCall(JNIEnv *env, jlong rptr, jstring typeName, jobject visitorObject, TAK::Engine::Core::Layer2& layer) {

    TAK::Engine::Core::MapRenderer3 *renderer = JLONG_TO_INTPTR(TAK::Engine::Core::MapRenderer3, rptr);
    if(!renderer) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    TAK::Engine::Port::String typeCStr;
    TAKErr code = TAKEngineJNI::Interop::JNIStringUTF_get(typeCStr, *env, typeName);
    ATAKMapEngineJNI_checkOrThrow(env, code);

    bool visited = false;
    VisitorContext context { env, visitorObject };
    if (!typeCStr)
        code = renderer->visitControls(&visited, &context, visitor, layer);
    else
        code = renderer->visitControls(&visited, &context, visitor, layer, typeCStr);
    ATAKMapEngineJNI_checkOrThrow(env, code);
}

/*
 * Class:     com_atakmap_map_NativeControl
 * Method:    getNativeControls
 * Signature: (JJ)[Lcom/atakmap/map/NativeControl;
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_opengl_NativeControl_visitNativeControls
        (JNIEnv *env, jclass, jlong rptr, jlong lptr, jstring typeName, jobject visitorObject) {

    if (!visitorObject)
        return;

    if(!checkInit(*env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    TAK::Engine::Core::Layer2* layer = JLONG_TO_INTPTR(TAK::Engine::Core::Layer2, lptr);
    if (!layer) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    commonVisitCall(env, rptr, typeName, visitorObject, *layer);
}

JNIEXPORT void JNICALL Java_com_atakmap_map_opengl_NativeControl_visitNativeControlsLegacy
        (JNIEnv *env, jclass, jlong rptr, jlong lptr, jstring typeName, jobject visitorObject) {

    if (!visitorObject)
        return;

    if(!checkInit(*env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    atakmap::core::Layer* layer = JLONG_TO_INTPTR(atakmap::core::Layer, lptr);
    if (!layer) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    std::shared_ptr<Layer2> layer2;
    TAKErr code = LegacyAdapters_find(layer2, *layer);

    ATAKMapEngineJNI_checkOrThrow(env, code);

    commonVisitCall(env, rptr, typeName, visitorObject, *layer2);
}

/*
 * Class:     com_atakmap_map_NativeControl
 * Method:    destruct
 * Signature: (Lcom/atakmap/interop/Pointer;)V
 */
JNIEXPORT void JNICALL Java_com_atakmap_map_opengl_NativeControl_destruct
    (JNIEnv *env, jclass, jobject pointerObject) {

    // The pointers are created with Memory_leaker_const, so this doesn't
    // delete anything, but it does clear out the pointer object as expected
    TAKEngineJNI::Interop::Pointer_destruct_iface<TAK::Engine::Core::Control>(env, pointerObject);
}

/*
 * Class:     com_atakmap_map_NativeControl
 * Method:    getName
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_atakmap_map_opengl_NativeControl_getName
    (JNIEnv *env, jclass, jlong pointer) {

    if(!checkInit(*env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return nullptr;
    }

    TAK::Engine::Core::Control* control = JLONG_TO_INTPTR(TAK::Engine::Core::Control, pointer);
    if (!control || !control->type)
        return nullptr;
    return env->NewStringUTF(control->type);
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_opengl_NativeControl_getControlPointer
        (JNIEnv *env, jclass, jlong pointer) {

    if(!checkInit(*env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return nullptr;
    }

    TAK::Engine::Core::Control* control = JLONG_TO_INTPTR(TAK::Engine::Core::Control, pointer);
    if (!control) {
        ATAKMapEngineJNI_checkOrThrow(env, TAK::Engine::Util::TE_InvalidArg);
        return nullptr;
    }

    std::unique_ptr<void, void (*)(const void *)> cptr(control->value, Memory_leaker_const<void>);
    return TAKEngineJNI::Interop::NewPointer(env, std::move(cptr));
}

namespace {
    bool checkInit(JNIEnv &env) NOTHROWS
    {
        static bool clinit = NativeControl_init(env);
        return clinit;
    }
    bool NativeControl_init(JNIEnv &env) NOTHROWS
    {
        NativeControl_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/opengl/NativeControl");
        NativeControl_class.Visitor_id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/opengl/NativeControl$Visitor");
        NativeControl_class.ctor = env.GetMethodID(NativeControl_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");
        NativeControl_class.Visitor_onVisitNativeControl = env.GetMethodID(NativeControl_class.Visitor_id, "onVisitNativeControl",
                                                   "(Ljava/lang/Object;)V");
        return true;
    }

    TAKErr visitor(void *opaque, const Layer2 &layer, const Control &ctrl) {

        auto* context = static_cast<VisitorContext*>(opaque);
        JNIEnv *env = context->env;

        std::unique_ptr<Control, void (*)(const Control *)> cptr(const_cast<Control*>(&ctrl), Memory_leaker_const<Control>);
        TAKEngineJNI::Interop::Java::JNILocalRef cptrObj(*env, TAKEngineJNI::Interop::NewPointer(env, std::move(cptr)));

        TAKEngineJNI::Interop::Java::JNILocalRef nativeCtrlObj(*env, env->NewObject(NativeControl_class.id,
                                                                                    NativeControl_class.ctor,
                                                                                    cptrObj.get(),
                       nullptr));

        env->CallVoidMethod(context->visitor, NativeControl_class.Visitor_onVisitNativeControl,
                            nativeCtrlObj.get());

        return TE_Ok;
    }
}
