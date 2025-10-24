#include "com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener.h"

#include <feature/FeatureDataStore2.h>

#include "common.h"
#include "interop/Pointer.h"
#include "interop/feature/Interop.h"

using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI;

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_destruct
  (JNIEnv *env, jclass clazz, jobject pointer)
{
    Interop::Pointer_destruct_iface<FeatureDataStore2::OnDataStoreContentChangedListener>(env, pointer);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_onDataStoreContentChanged
  (JNIEnv *env, jclass clazz, jlong ptr, jobject mdatastore, jlong datastorePtr)
{
    auto &clistener = *JLONG_TO_INTPTR(FeatureDataStore2::OnDataStoreContentChangedListener, ptr);
    FeatureDataStore2 *cdatastore = JLONG_TO_INTPTR(FeatureDataStore2, datastorePtr);
    std::shared_ptr<FeatureDataStore2> cdatastore_sp;
    // XXX - there is some danger here. if managed gets wrapped as a result of this call, it could
    //       disposed when scope exits
    if(!cdatastore) {
        Interop::Feature::Interop_marshal(cdatastore_sp, *env, mdatastore);
        cdatastore = cdatastore_sp.get();
    }
    clistener.onDataStoreContentChanged(*cdatastore);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_onFeatureInserted
  (JNIEnv *env, jclass clazz, jlong ptr, jobject mdatastore, jlong datastorePtr, jlong, jobject, jlong)
{
    Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_onDataStoreContentChanged(env, clazz, ptr, mdatastore, datastorePtr);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_onFeatureUpdated
  (JNIEnv *env, jclass clazz, jlong ptr, jobject mdatastore, jlong datastorePtr, jlong, jint, jstring, jobject, jobject, jobject, jint)
{
    Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_onDataStoreContentChanged(env, clazz, ptr, mdatastore, datastorePtr);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_onFeatureDeleted
  (JNIEnv *env, jclass clazz, jlong ptr, jobject mdatastore, jlong datastorePtr, jlong)
{
    Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_onDataStoreContentChanged(env, clazz, ptr, mdatastore, datastorePtr);
}
JNIEXPORT void JNICALL Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_onFeatureVisibilityChanged
  (JNIEnv *env, jclass clazz, jlong ptr, jobject mdatastore, jlong datastorePtr, jlong, jboolean)
{
    Java_com_atakmap_map_layer_feature_NativeDataStoreContentChangedListener_onDataStoreContentChanged(env, clazz, ptr, mdatastore, datastorePtr);
}
