
#include "com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix.h"
#include "com_atakmap_map_layer_raster_tilematrix_NativeTileClient.h"
#include "com_atakmap_map_layer_raster_tilematrix_NativeTileContainer.h"
#include "com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi.h"
#include "com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi.h"
#include "com_atakmap_map_layer_raster_tilematrix_TileContainerFactory.h"
#include "com_atakmap_map_layer_raster_tilematrix_TileClientFactory.h"
#include "com_atakmap_map_contentservices_NativeCacheRequestListener.h"

#include "common.h"
#include "interop/Pointer.h"
#include "interop/java/JNILocalRef.h"
#include "interop/core/Interop.h"
#include "interop/feature/Interop.h"
#include "interop/renderer/core/Interop.h"
#include "interop/renderer/Interop.h"
#include "interop/JNIStringUTF.h"
#include "interop/java/JNIEnum.h"
#include "interop/raster/tilematrix/ManagedTileMatrix.h"
#include "interop/raster/tilematrix/Interop.h"
#include "interop/JNIByteArray.h"

#include <renderer/Bitmap2.h>
#include <raster/tilematrix/TileMatrix.h>
#include <raster/tilematrix/TileClient.h>
#include <raster/tilematrix/TileClientFactory.h>
#include <raster/tilematrix/TileContainer.h>
#include <raster/tilematrix/TileContainerFactory.h>
#include <raster/tilematrix/TileClientFactory.h>
#include <port/Collection.h>
#include <port/STLVectorAdapter.h>

#include <limits>

using namespace TAKEngineJNI::Interop;
using namespace TAK::Engine::Util;
using namespace TAK::Engine::Raster::TileMatrix;

namespace {
    struct {
        jclass id;
        jmethodID ctor;
        jfieldID levelField;
        jfieldID tileWidthField;
        jfieldID tileHeightField;
        jfieldID resolutionField;
        jfieldID pixelSizeXField;
        jfieldID pixelSizeYField;
    } ZoomLevel_class;

    bool ZoomLevel_class_init(JNIEnv *env) {
        ZoomLevel_class.id = ATAKMapEngineJNI_findClass(env,
                                                        "com/atakmap/map/layer/raster/tilematrix/TileMatrix$ZoomLevel");
        ZoomLevel_class.ctor = env->GetMethodID(ZoomLevel_class.id, "<init>", "()V");
        ZoomLevel_class.levelField = env->GetFieldID(ZoomLevel_class.id, "level", "I");
        ZoomLevel_class.tileWidthField = env->GetFieldID(ZoomLevel_class.id, "tileWidth", "I");
        ZoomLevel_class.tileHeightField = env->GetFieldID(ZoomLevel_class.id, "tileHeight", "I");
        ZoomLevel_class.resolutionField = env->GetFieldID(ZoomLevel_class.id, "resolution", "D");
        ZoomLevel_class.pixelSizeXField = env->GetFieldID(ZoomLevel_class.id, "pixelSizeX", "D");
        ZoomLevel_class.pixelSizeYField = env->GetFieldID(ZoomLevel_class.id, "pixelSizeY", "D");
        return true;
    }

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID minResID;
        jfieldID maxResID;
        jfieldID regionID;
        jfieldID timespanStartID;
        jfieldID timespanEndID;
        jfieldID cacheFileID;
        jfieldID modeID;
        jfieldID canceledID;
        jfieldID countOnlyID;
        jfieldID maxThreadsID;
        jfieldID expireOffsetID;
        jfieldID prefContainerProvID;
        jmethodID File_getPathID;
    } CacheRequest_class;

    bool CacheRequest_class_init(JNIEnv* env) {
        CacheRequest_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/contentservices/CacheRequest");
        CacheRequest_class.ctor = env->GetMethodID(CacheRequest_class.id, "<init>", "()V");
        CacheRequest_class.minResID = env->GetFieldID(CacheRequest_class.id, "minResolution", "D");
        CacheRequest_class.maxResID = env->GetFieldID(CacheRequest_class.id, "maxResolution", "D");
        CacheRequest_class.regionID = env->GetFieldID(CacheRequest_class.id, "region", "Lcom/atakmap/map/layer/feature/geometry/Geometry;");
        CacheRequest_class.timespanStartID = env->GetFieldID(CacheRequest_class.id, "timespanStart", "J");
        CacheRequest_class.timespanEndID = env->GetFieldID(CacheRequest_class.id, "timespanEnd", "J");
        CacheRequest_class.cacheFileID = env->GetFieldID(CacheRequest_class.id, "cacheFile", "Ljava/io/File;");
        CacheRequest_class.modeID = env->GetFieldID(CacheRequest_class.id, "mode", "Lcom/atakmap/map/contentservices/CacheRequest$CacheMode;");
        CacheRequest_class.canceledID = env->GetFieldID(CacheRequest_class.id, "canceled", "Z");
        CacheRequest_class.countOnlyID = env->GetFieldID(CacheRequest_class.id, "countOnly", "Z");
        CacheRequest_class.maxThreadsID = env->GetFieldID(CacheRequest_class.id, "maxThreads", "I");
        CacheRequest_class.expireOffsetID = env->GetFieldID(CacheRequest_class.id, "expirationOffset", "J");
        CacheRequest_class.prefContainerProvID = env->GetFieldID(CacheRequest_class.id, "preferredContainerProvider", "Ljava/lang/String;");
        jclass fileClass = ATAKMapEngineJNI_findClass(env, "java/io/File");
        CacheRequest_class.File_getPathID = env->GetMethodID(fileClass, "getPath", "()Ljava/lang/String;");
        return true;
    }


    struct {
        jclass options_id;
        jclass id;
        jfieldID options_dnsLookupTimeoutID;
        jfieldID options_connectTimeoutID;
        jfieldID options_proxyID;

    } TileClientSpi_class;

    struct {
        jclass id;
        jmethodID ctor;

        jclass native_tile_client_id;
        jmethodID native_tile_client_ctor;
        jfieldID native_tile_client_pointer;
    } NativeTileClientSpi_class;

    bool TileClientSpi_class_init(JNIEnv* env) {
        TileClientSpi_class.options_id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/TileClientSpi$Options");
        TileClientSpi_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/TileClientSpi");
        TileClientSpi_class.options_dnsLookupTimeoutID = env->GetFieldID(TileClientSpi_class.options_id, "dnsLookupTimeout", "J");
        TileClientSpi_class.options_connectTimeoutID = env->GetFieldID(TileClientSpi_class.options_id, "connectTimeout", "J");
        TileClientSpi_class.options_proxyID = env->GetFieldID(TileClientSpi_class.options_id, "proxy", "Z");

        NativeTileClientSpi_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/NativeTileClientSpi");
        NativeTileClientSpi_class.ctor = env->GetMethodID(NativeTileClientSpi_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");

        NativeTileClientSpi_class.native_tile_client_id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/NativeTileClient");
        NativeTileClientSpi_class.native_tile_client_ctor = env->GetMethodID(NativeTileClientSpi_class.native_tile_client_id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");
        NativeTileClientSpi_class.native_tile_client_pointer = env->GetFieldID(NativeTileClientSpi_class.native_tile_client_id, "pointer", "Lcom/atakmap/interop/Pointer;");

        return true;
    }

    struct {
        jclass iface_id;

        jclass id;
        jmethodID ctor;

        jclass native_tile_container_id;
        jmethodID native_tile_container_ctor;
        jfieldID native_tile_container_pointer;
    } NativeTileContainerSpi_class;

    bool NativeTileContainerSpi_class_init(JNIEnv* env) {
        NativeTileContainerSpi_class.iface_id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/TileContainerSpi");
        NativeTileContainerSpi_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/NativeTileContainerSpi");
        NativeTileContainerSpi_class.ctor = env->GetMethodID(NativeTileContainerSpi_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");

        NativeTileContainerSpi_class.native_tile_container_id = ATAKMapEngineJNI_findClass(env, "com/atakmap/map/layer/raster/tilematrix/NativeTileContainer");
        NativeTileContainerSpi_class.native_tile_container_ctor = env->GetMethodID(NativeTileContainerSpi_class.native_tile_container_id, "<init>", "(Lcom/atakmap/interop/Pointer;Ljava/lang/Object;)V");
        NativeTileContainerSpi_class.native_tile_container_pointer = env->GetFieldID(NativeTileContainerSpi_class.native_tile_container_id, "pointer", "Lcom/atakmap/interop/Pointer;");

        return true;
    }

    struct {
        jclass id;
        jmethodID visitID;
    } Visitor_class;

    bool Visitor_class_init(JNIEnv* env) {
        Visitor_class.id = ATAKMapEngineJNI_findClass(env, "com/atakmap/util/Visitor");
        Visitor_class.visitID = env->GetMethodID(Visitor_class.id, "visit", "(Ljava/lang/Object;)V");
        return true;
    }

    bool checkInit(JNIEnv *env) {
        static bool v = ZoomLevel_class_init(env) &&
                CacheRequest_class_init(env) &&
                TileClientSpi_class_init(env) &&
                NativeTileContainerSpi_class_init(env) &&
                Visitor_class_init(env);
        return v;
    }
}

//
// com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix
//

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix_destruct
    (JNIEnv *env, jclass, jobject mpointer) {
    Pointer_destruct_iface<TileMatrix>(env, mpointer);
}

JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix_getName
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TileMatrix *cobj = JLONG_TO_INTPTR(TileMatrix, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }
    return env->NewStringUTF(cobj->getName());
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix_getSRID
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return 0;
    }

    TileMatrix *cobj = JLONG_TO_INTPTR(TileMatrix, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }
    return cobj->getSRID();
}

JNIEXPORT jobjectArray JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix_getZoomLevel
    (JNIEnv *env, jclass, jlong ptr) {

    TileMatrix *cobj = JLONG_TO_INTPTR(TileMatrix, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    std::vector<TileMatrix::ZoomLevel> levels;
    TAK::Engine::Port::STLVectorAdapter<TileMatrix::ZoomLevel> adapter(levels);
    TAKErr code = cobj->getZoomLevel(adapter);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return nullptr;
    }

    Java::JNILocalRef result(*env, env->NewObjectArray(levels.size(), ZoomLevel_class.id, nullptr));
    size_t i = 0;
    for (TileMatrix::ZoomLevel level : levels) {
        Java::JNILocalRef zoomLevelInst(*env, env->NewObject(ZoomLevel_class.id, ZoomLevel_class.ctor));
        env->SetIntField(zoomLevelInst, ZoomLevel_class.levelField, level.level);
        env->SetIntField(zoomLevelInst, ZoomLevel_class.tileWidthField, level.tileWidth);
        env->SetIntField(zoomLevelInst, ZoomLevel_class.tileHeightField, level.tileHeight);
        env->SetDoubleField(zoomLevelInst, ZoomLevel_class.resolutionField, level.resolution);
        env->SetDoubleField(zoomLevelInst, ZoomLevel_class.pixelSizeXField, level.pixelSizeX);
        env->SetDoubleField(zoomLevelInst, ZoomLevel_class.pixelSizeYField, level.pixelSizeY);
        env->SetObjectArrayElement(static_cast<jobjectArray>(result.get()), i++, zoomLevelInst.get());
    }

    return static_cast<jobjectArray>(result.release());
}

JNIEXPORT jdouble JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix_getOriginX
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return 0.0;
    }

    TileMatrix *cobj = JLONG_TO_INTPTR(TileMatrix, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0.0;
    }

    return cobj->getOriginX();
}

JNIEXPORT jdouble JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix_getOriginY
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return 0.0;
    }

    TileMatrix *cobj = JLONG_TO_INTPTR(TileMatrix, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0.0;
    }

    return cobj->getOriginY();
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix_getTile
    (JNIEnv *env, jclass, jlong ptr, jint zoom, jint x, jint y, jobjectArray error) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    if (zoom < 0 || x < 0 || y < 0) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    TileMatrix *cobj = JLONG_TO_INTPTR(TileMatrix, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    TAK::Engine::Renderer::BitmapPtr tilePtr(nullptr, nullptr);
    if(cobj->getTile(tilePtr, zoom, x, y) != TE_Ok)
        return nullptr;

    Java::JNILocalRef result(*env, nullptr);
    TAKErr code = TAKEngineJNI::Interop::Renderer::Interop_marshal(result, env, *tilePtr);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    return result.release();
}

JNIEXPORT jbyteArray JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix_getTileData
    (JNIEnv *env, jclass, jlong ptr, jint zoom, jint x, jint y, jobjectArray error) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    if (zoom < 0 || x < 0 || y < 0) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    TileMatrix *cobj = JLONG_TO_INTPTR(TileMatrix, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    std::unique_ptr<const uint8_t, void (*)(const uint8_t*)> data(nullptr, nullptr);
    size_t len = 0;
    if(cobj->getTileData(data, &len, zoom, x, y) != TE_Ok)
        return nullptr;

    Java::JNILocalRef result(*env, env->NewByteArray(len));
    JNIByteArray arr(*env, static_cast<jbyteArray>(result.get()), 0);
    memcpy(arr.get<uint8_t>(), data.get(), len);

    return static_cast<jbyteArray>(result.release());
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileMatrix_getBounds
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TileMatrix *cobj = JLONG_TO_INTPTR(TileMatrix, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    Java::JNILocalRef result(*env, nullptr);
    TAK::Engine::Feature::Envelope2 bounds;

    TAKErr code = cobj->getBounds(&bounds);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    code = TAKEngineJNI::Interop::Feature::Interop_marshal(result, *env, bounds);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    return result.release();
}

//
// com_atakmap_map_layer_raster_tilematrix_NativeTileClient
//

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClient_wrap
        (JNIEnv *env, jclass, jobject mspi) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TAKErr code(TE_Ok);
    TileClientPtr cspi(NULL, NULL);
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(cspi, *env, mspi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(cspi));
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClient_getControl
    (JNIEnv *env, jclass, jlong, jclass) {

    // intentional nop
    return nullptr;
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClient_getControls
    (JNIEnv *env, jclass, jlong, jobject) {

    // intentional nop
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClient_clearAuthFailed
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    TileClient *cobj = JLONG_TO_INTPTR(TileClient, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    TAKErr code = cobj->clearAuthFailed();
    ATAKMapEngineJNI_checkOrThrow(env, code);
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClient_checkConnectivity
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    TileClient *cobj = JLONG_TO_INTPTR(TileClient, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    TAKErr code = cobj->checkConnectivity();
    ATAKMapEngineJNI_checkOrThrow(env, code);
}

namespace {
    TAKErr CacheRequest_get(CacheRequest& creq, JNIEnv* env, jobject request) {
        creq.minResolution = env->GetDoubleField(request, CacheRequest_class.minResID);
        creq.maxResolution = env->GetDoubleField(request, CacheRequest_class.maxResID);

        TAKErr code = TE_Ok;
        Java::JNILocalRef geomRef(*env, env->GetObjectField(request, CacheRequest_class.regionID));
        if (geomRef.get()) {
            code = Feature::Interop_create(creq.region, env, geomRef.get());
            if (ATAKMapEngineJNI_checkOrThrow(env, code))
                return code;
        }

        creq.timeSpanStart = env->GetLongField(request, CacheRequest_class.timespanStartID);
        creq.timeSpanEnd = env->GetLongField(request, CacheRequest_class.timespanEndID);
        creq.canceled = env->GetBooleanField(request, CacheRequest_class.canceledID);
        creq.countOnly = env->GetBooleanField(request, CacheRequest_class.countOnlyID);
        creq.maxThreads = env->GetIntField(request, CacheRequest_class.maxThreadsID);
        creq.expirationOffset = env->GetLongField(request, CacheRequest_class.expireOffsetID);
        creq.skipUnexpiredTiles = false;

        Java::JNILocalRef cacheFileRef(*env, env->GetObjectField(request, CacheRequest_class.cacheFileID));
        Java::JNILocalRef cacheFilePathRef(*env, env->CallObjectMethod(cacheFileRef.get(), CacheRequest_class.File_getPathID));
        code = JNIStringUTF_get(creq.cacheFilePath, *env, static_cast<jstring>(cacheFilePathRef.get()));
        if (ATAKMapEngineJNI_checkOrThrow(env, code))
            return code;

        Java::JNILocalRef pcpRef(*env, env->GetObjectField(request, CacheRequest_class.prefContainerProvID));
        if (pcpRef.get()) {
            code = JNIStringUTF_get(creq.preferredContainerProvider, *env, static_cast<jstring>(pcpRef.get()));
            if (ATAKMapEngineJNI_checkOrThrow(env, code))
                return code;
        }

        Java::JNILocalRef enumValue(*env, nullptr);
        const char enumClass[] = "com/atakmap/map/contentservices/CacheRequest$CacheMode";
        if (TAKEngineJNI::Interop::Java::JNIEnum_value(enumValue, *env, enumClass, "Create") == TE_Ok) {
            creq.mode = TECM_Create;
        } else if (TAKEngineJNI::Interop::Java::JNIEnum_value(enumValue, *env, enumClass, "Append") == TE_Ok) {
            creq.mode = TECM_Append;
        } else {
            ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
            return TE_InvalidArg;
        }

        return TE_Ok;
    }
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClient_estimateTileCount
    (JNIEnv *env, jclass, jlong ptr, jobject request) {

    TileClient *cobj = JLONG_TO_INTPTR(TileClient, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return 0;
    }

    CacheRequest creq;
    if (CacheRequest_get(creq, env, request) != TE_Ok)
        return 0;

    int count = 0;
    TAKErr code = cobj->estimateTilecount(&count, creq);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return 0;

    return count;
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClient_cache
    (JNIEnv *env, jclass, jlong ptr, jobject request, jobject listener) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    TileClient *cobj = JLONG_TO_INTPTR(TileClient, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    CacheRequest creq;
    if (CacheRequest_get(creq, env, request) != TE_Ok)
        return;

    std::shared_ptr<CacheRequestListener> clistener;
    TAKErr code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(clistener, *env, listener);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    code = cobj->cache(creq, clistener);
    ATAKMapEngineJNI_checkOrThrow(env, code);
}

//
// com_atakmap_map_layer_raster_tilematrix_NativeTileContainer
//

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainer_wrap
        (JNIEnv *env, jclass, jobject mspi) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TAKErr code(TE_Ok);
    TileContainerPtr cspi(NULL, NULL);
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(cspi, *env, mspi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(cspi));
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainer_isReadOnly
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return false;
    }

    TileContainer *cobj = JLONG_TO_INTPTR(TileContainer, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }

    bool value = false;
    TAKErr code = cobj->isReadOnly(&value);
    ATAKMapEngineJNI_checkOrThrow(env, code);

    return value;
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainer_setTile
    (JNIEnv *env, jclass, jlong ptr, jint zoom, jint x, jint y, jobject mbitmap, jlong expiration) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    if (zoom < 0 || x < 0 || y < 0) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    TileContainer *cobj = JLONG_TO_INTPTR(TileContainer, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    TAK::Engine::Renderer::BitmapPtr bmp(nullptr, nullptr);
    TAKErr code = TAKEngineJNI::Interop::Renderer::Interop_marshal(bmp, env, mbitmap);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    code = cobj->setTile(zoom, x, y, bmp.get(), expiration);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainer_setTileData
    (JNIEnv *env, jclass, jlong ptr, jint zoom, jint x, jint y, jbyteArray data, jlong expiration) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    if (zoom < 0 || x < 0 || y < 0) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    TileContainer *cobj = JLONG_TO_INTPTR(TileContainer, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    JNIByteArray arr(*env, data, JNI_ABORT);
    TAKErr code = cobj->setTile(zoom, x, y, arr.get<const uint8_t>(), arr.length(), expiration);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainer_hasTileExpirationMetadata
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return false;
    }

    TileContainer *cobj = JLONG_TO_INTPTR(TileContainer, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }

    return cobj->hasTileExpirationMetadata();
}

JNIEXPORT jlong JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainer_getTileExpiration
    (JNIEnv *env, jclass, jlong ptr, jint level, jint x, jint y) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return 0;
    }

    // don't overflow the std::size_t
    if (level < 0 || x < 0 || y < 0) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    TileContainer *cobj = JLONG_TO_INTPTR(TileContainer, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    return cobj->getTileExpiration(static_cast<size_t>(level), static_cast<size_t>(x), static_cast<size_t>(y));
}

//
// com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi
//

JNIEXPORT jlong JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_getPointer
        (JNIEnv *env, jclass, jobject mspi) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return 0;
    }

    TAKErr code(TE_Ok);
    bool isWrapper = false;
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileContainerSpi>(&isWrapper, *env, mspi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return 0LL;
    if(!isWrapper)
        return 0LL;
    std::shared_ptr<TileContainerSpi> cspi;
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(cspi, *env, mspi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return 0LL;
    return INTPTR_TO_JLONG(cspi.get());
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_wrap
        (JNIEnv *env, jclass, jobject mspi) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TAKErr code(TE_Ok);
    TileContainerSpiPtr cspi(NULL, NULL);
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(cspi, *env, mspi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(cspi));
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_hasPointer
        (JNIEnv *env, jclass, jobject mspi) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return false;
    }

    if(!mspi) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }
    bool retval = false;
    TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TileContainerSpi>(&retval, *env, mspi);
    return retval;
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_create__Lcom_atakmap_interop_Pointer_2Ljava_lang_Object_2
        (JNIEnv *env, jclass, jobject mpointer, jobject mowner) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    return env->NewObject(NativeTileContainerSpi_class.id, NativeTileContainerSpi_class.ctor, mpointer, mowner);
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_hasObject
        (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return false;
    }

    TileContainerSpi *cspi = JLONG_TO_INTPTR(TileContainerSpi, ptr);
    if(!cspi) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }
    bool retval = false;
    TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper(&retval, *env, *cspi);
    return retval;
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_getObject
        (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TileContainerSpi *cspi = JLONG_TO_INTPTR(TileContainerSpi, ptr);
    if(!cspi) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    Java::JNILocalRef mspi(*env, NULL);
    TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(mspi, *env, *cspi);
    return mspi.release();
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_destruct
    (JNIEnv *env, jclass, jobject mpointer) {
    Pointer_destruct_iface<TileContainerSpi>(env, mpointer);
}

JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_getName
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TileContainerSpi *cobj = JLONG_TO_INTPTR(TileContainerSpi, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    return env->NewStringUTF(cobj->getName());
}

JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_getDefaultExtension
    (JNIEnv *env, jclass, jlong ptr) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TileContainerSpi *cobj = JLONG_TO_INTPTR(TileContainerSpi, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    return env->NewStringUTF(cobj->getDefaultExtension());
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_create__JLjava_lang_String_2Ljava_lang_String_2Lcom_atakmap_map_layer_raster_tilematrix_TileMatrix_2
    (JNIEnv *env, jclass, jlong ptr, jstring name, jstring path, jobject spec) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TileContainerSpi *cobj = JLONG_TO_INTPTR(TileContainerSpi, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    TAK::Engine::Port::String nameStr;
    TAK::Engine::Port::String pathStr;
    TAKErr code = JNIStringUTF_get(nameStr, *env, name);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    code = JNIStringUTF_get(pathStr, *env, path);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TileContainerPtr result(nullptr, nullptr);
    code = cobj->create(result, nameStr, pathStr, nullptr);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    Java::JNILocalRef ptrRes(*env, TAKEngineJNI::Interop::NewPointer(env, std::move(result)));
    return env->NewObject(NativeTileContainerSpi_class.native_tile_container_id, NativeTileContainerSpi_class.native_tile_container_ctor,
                          ptrRes.release(),
                          nullptr);
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_open
    (JNIEnv *env, jclass, jlong ptr, jstring name, jobject spec, jboolean readOnly) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TileContainerSpi *cobj = JLONG_TO_INTPTR(TileContainerSpi, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    TAK::Engine::Port::String nameStr;
    TAKErr code = JNIStringUTF_get(nameStr, *env, name);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TileContainerPtr result(nullptr, nullptr);
    code = cobj->open(result, nameStr, nullptr, readOnly);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    Java::JNILocalRef ptrRes(*env, TAKEngineJNI::Interop::NewPointer(env, std::move(result)));
    return env->NewObject(NativeTileContainerSpi_class.native_tile_container_id, NativeTileContainerSpi_class.native_tile_container_ctor,
            ptrRes.release(),
            nullptr);
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileContainerSpi_isCompatible
    (JNIEnv *env, jclass, jlong ptr, jobject spec) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return false;
    }

    TileContainerSpi *cobj = JLONG_TO_INTPTR(TileContainerSpi, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    TAKErr code(TE_Ok);
    TileMatrixPtr ctm(nullptr, nullptr);
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(ctm, *env, spec);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;

    bool result = false;
    cobj->isCompatible(&result, ctm.get());
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;

    return result;
}

//
// com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi
//

JNIEXPORT jlong JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_getPointer
        (JNIEnv* env, jclass clazz, jobject mspi) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return 0;
    }

    TAKErr code(TE_Ok);
    bool isWrapper = false;
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileClientSpi>(&isWrapper, *env, mspi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return 0LL;
    if(!isWrapper)
        return 0LL;
    std::shared_ptr<TileClientSpi> cspi;
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(cspi, *env, mspi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return 0LL;
    return INTPTR_TO_JLONG(cspi.get());
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_wrap
        (JNIEnv* env, jclass, jobject mspi) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    TAKErr code(TE_Ok);
    TileClientSpiPtr cspi(NULL, NULL);
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(cspi, *env, mspi);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return NewPointer(env, std::move(cspi));
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_hasPointer
        (JNIEnv *env, jclass, jobject mspi) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return false;
    }

    if(!mspi) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }
    bool retval = false;
    TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper<TileClientSpi>(&retval, *env, mspi);
    return retval;
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_create__Lcom_atakmap_interop_Pointer_2Ljava_lang_Object_2
        (JNIEnv *env, jclass, jobject mpointer, jobject mowner) {

    if(!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return NULL;
    }

    return env->NewObject(NativeTileClientSpi_class.id, NativeTileClientSpi_class.ctor, mpointer, mowner);
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_hasObject
        (JNIEnv *env, jclass, jlong ptr) {
    TileClientSpi *cspi = JLONG_TO_INTPTR(TileClientSpi, ptr);
    if(!cspi) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }
    bool retval = false;
    TAKEngineJNI::Interop::Raster::TileMatrix::Interop_isWrapper(&retval, *env, *cspi);
    return retval;
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_getObject
        (JNIEnv *env, jclass, jlong ptr) {
    TileClientSpi *cspi = JLONG_TO_INTPTR(TileClientSpi, ptr);
    if(!cspi) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return NULL;
    }
    Java::JNILocalRef mspi(*env, NULL);
    TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(mspi, *env, *cspi);
    return mspi.release();
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_destruct
    (JNIEnv *env, jclass, jobject mpointer) {
    Pointer_destruct_iface<TileClientSpi>(env, mpointer);
}

JNIEXPORT jint JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_getPriority
    (JNIEnv *env, jclass, jlong ptr) {

    TileClientSpi *cobj = JLONG_TO_INTPTR(TileClientSpi, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return 0;
    }

    return cobj->getPriority();
}

JNIEXPORT jstring JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_getName
    (JNIEnv *env, jclass, jlong ptr) {

    TileClientSpi *cobj = JLONG_TO_INTPTR(TileClientSpi, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    return env->NewStringUTF(cobj->getName());
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_NativeTileClientSpi_create__JLjava_lang_String_2Ljava_lang_String_2Lcom_atakmap_map_layer_raster_tilematrix_TileClientSpi_Options_2
    (JNIEnv *env, jclass, jlong ptr, jstring path, jstring offlineCachePath, jobject options) {

    TileClientSpi *cobj = JLONG_TO_INTPTR(TileClientSpi, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return nullptr;
    }

    TileClientSpi::Options opts;
    if (options) {
        opts.dnsLookupTimeout = env->GetLongField(options, TileClientSpi_class.options_dnsLookupTimeoutID);
        if(env->ExceptionCheck()) {
            return nullptr;
        }

        opts.connectTimeout = env->GetLongField(options, TileClientSpi_class.options_connectTimeoutID);
        if(env->ExceptionCheck()) {
            return nullptr;
        }
    }

    TAK::Engine::Port::String cpath;
    TAKErr code = JNIStringUTF_get(cpath, *env, path);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TAK::Engine::Port::String cocPath;
    code = JNIStringUTF_get(cocPath, *env, offlineCachePath);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TileClientPtr result(nullptr, nullptr);
    code = cobj->create(result, cpath, cocPath, &opts);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    Java::JNILocalRef ptrRes(*env, TAKEngineJNI::Interop::NewPointer(env, std::move(result)));
    return env->NewObject(NativeTileClientSpi_class.native_tile_client_id, NativeTileClientSpi_class.native_tile_client_ctor,
                   ptrRes.release(),
                   nullptr);
}

//
// TileContainerFactory
//

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_TileContainerFactory_registerNative
    (JNIEnv *env, jclass, jobject mpointer) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    TAKErr code(TE_Ok);
    std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainerSpi> cspi;
    code = Pointer_get<TAK::Engine::Raster::TileMatrix::TileContainerSpi>(cspi, *env, mpointer);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
    TAK::Engine::Raster::TileMatrix::TileContainerFactory_registerSpi(cspi);
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_TileContainerFactory_unregisterNative
    (JNIEnv *env, jclass, jlong ptr) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    TAK::Engine::Raster::TileMatrix::TileContainerSpi *cspi = JLONG_TO_INTPTR(TAK::Engine::Raster::TileMatrix::TileContainerSpi, ptr);
    TAK::Engine::Raster::TileMatrix::TileContainerFactory_unregisterSpi(cspi);
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_TileContainerFactory_openOrCreateCompatibleContainerNative
        (JNIEnv *env, jclass, jstring path, jobject spec, jstring hint) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return nullptr;
    }

    TAK::Engine::Port::String cpath;
    TAKErr code = JNIStringUTF_get(cpath, *env, path);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TileMatrixPtr cspec(nullptr, nullptr);
    code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(cspec, *env, spec);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TAK::Engine::Port::String chint;
    code = JNIStringUTF_get(chint, *env, hint);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TAK::Engine::Raster::TileMatrix::TileContainerPtr cretval(nullptr, nullptr);
    if(TAK::Engine::Raster::TileMatrix::TileContainerFactory_openOrCreateCompatibleContainer(cretval, cpath, cspec.get(), chint) != TE_Ok)
        return nullptr;

    Java::JNILocalRef mretval(*env, nullptr);
    std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainer> cretval_shared(std::move(cretval));
    if(Raster::TileMatrix::Interop_marshal(mretval, *env, cretval_shared) != TE_Ok)
        return NULL;
    return mretval.release();
}

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_TileContainerFactory_openNative
        (JNIEnv *env, jclass, jstring path, jboolean readOnly, jstring hint) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return nullptr;
    }

    TAK::Engine::Port::String cpath;
    TAKErr code = JNIStringUTF_get(cpath, *env, path);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TAK::Engine::Port::String chint;
    code = JNIStringUTF_get(chint, *env, hint);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TAK::Engine::Raster::TileMatrix::TileContainerPtr cretval(nullptr, nullptr);
    if(TAK::Engine::Raster::TileMatrix::TileContainerFactory_open(cretval, cpath, readOnly, chint) != TE_Ok)
        return nullptr;

    Java::JNILocalRef mretval(*env, nullptr);
    std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainer> cretval_shared(std::move(cretval));
    if(Raster::TileMatrix::Interop_marshal(mretval, *env, cretval_shared) != TE_Ok)
        return NULL;
    return mretval.release();
}

struct VisitorImplArgs {
    JNIEnv *env;
    jobject visitor;
};

static TAKErr visitorImpl(void* opaque, TAK::Engine::Port::Collection<std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainerSpi>>& spis) {

    TAK::Engine::Port::Collection<std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainerSpi>>::IteratorPtr iter(nullptr, nullptr);
    spis.iterator(iter);

    VisitorImplArgs *args = static_cast<VisitorImplArgs*>(opaque);
    Java::JNILocalRef objArr(*args->env, args->env->NewObjectArray(spis.size(), NativeTileContainerSpi_class.iface_id, nullptr));

    jint i = 0;
    do {
        std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainerSpi> item;
        TAKErr code = iter->get(item);
        if (code != TE_Ok)
            break;
        Java::JNILocalRef itemRef(*args->env, nullptr);
        TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(itemRef, *args->env, item);
        args->env->SetObjectArrayElement((jobjectArray)objArr.get(), i, itemRef.release());
        ++i;
    } while (iter->next() == TE_Ok);

    args->env->CallVoidMethod(args->visitor, Visitor_class.visitID, objArr.get());

    return TE_Ok;
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_TileContainerFactory_visitSpisNative
    (JNIEnv *env, jclass, jobject visitor) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    VisitorImplArgs args {
        env,
        visitor
    };
    TAKErr code = TAK::Engine::Raster::TileMatrix::TileContainerFactory_visitSpis(visitorImpl, &args);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_TileContainerFactory_visitCompatibleSpisNative
    (JNIEnv *env, jclass, jobject visitor, jobject spec) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    TileMatrixPtr cspec(nullptr, nullptr);
    TAKErr code = TAKEngineJNI::Interop::Raster::TileMatrix::Interop_marshal(cspec, *env, spec);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;

    VisitorImplArgs args {
        env,
        visitor
    };

    code = TAK::Engine::Raster::TileMatrix::TileContainerFactory_visitCompatibleSpis(visitorImpl, &args, cspec.get());
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
}

//
// com_atakmap_map_layer_raster_tilematrix_TileClientFactory
//

JNIEXPORT jobject JNICALL Java_com_atakmap_map_layer_raster_tilematrix_TileClientFactory_createNative
        (JNIEnv *env, jclass, jstring path, jstring offlineCache, jobject options) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return nullptr;
    }

    TileClientSpi::Options opts;
    if(options) {
        opts.dnsLookupTimeout = env->GetLongField(options,
                                                  TileClientSpi_class.options_dnsLookupTimeoutID);
        if (env->ExceptionCheck()) {
            return nullptr;
        }

        opts.connectTimeout = env->GetLongField(options,
                                                TileClientSpi_class.options_connectTimeoutID);
        if (env->ExceptionCheck()) {
            return nullptr;
        }
    }

    TAK::Engine::Port::String cpath;
    TAKErr code = JNIStringUTF_get(cpath, *env, path);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TAK::Engine::Port::String cocPath;
    code = JNIStringUTF_get(cocPath, *env, offlineCache);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;

    TileClientPtr result(nullptr, nullptr);
    if(TAK::Engine::Raster::TileMatrix::TileClientFactory_create(result, cpath, cocPath, &opts) != TE_Ok)
        return nullptr;

    Java::JNILocalRef mretval(*env, nullptr);
    std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClient> cretval_shared(std::move(result));
    if(Raster::TileMatrix::Interop_marshal(mretval, *env, cretval_shared) != TE_Ok)
        return NULL;

    return mretval.release();
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_TileClientFactory_registerNative
    (JNIEnv *env, jclass, jobject mpointer) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    TAKErr code(TE_Ok);
    std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClientSpi> cspi;
    code = Pointer_get<TAK::Engine::Raster::TileMatrix::TileClientSpi>(cspi, *env, mpointer);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return;
    TAK::Engine::Raster::TileMatrix::TileClientFactory_registerSpi(cspi);
}

JNIEXPORT void JNICALL Java_com_atakmap_map_layer_raster_tilematrix_TileClientFactory_unregisterNative
    (JNIEnv *env, jclass, jlong ptr) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    TAK::Engine::Raster::TileMatrix::TileClientSpi *cspi = JLONG_TO_INTPTR(TAK::Engine::Raster::TileMatrix::TileClientSpi, ptr);
    TAK::Engine::Raster::TileMatrix::TileClientFactory_unregisterSpi(cspi);
}

//
// com_atakmap_map_layer_raster_tilematrix_NativeCacheRequestListener
//

JNIEXPORT void JNICALL Java_com_atakmap_map_contentservices_NativeCacheRequestListener_destruct
    (JNIEnv *env, jclass, jobject mpointer) {
    Pointer_destruct_iface<CacheRequestListener>(env, mpointer);
}

JNIEXPORT void JNICALL Java_com_atakmap_map_contentservices_NativeCacheRequestListener_onRequestCompleteNative
    (JNIEnv *env, jclass, jlong ptr) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    CacheRequestListener *cobj = JLONG_TO_INTPTR(CacheRequestListener, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    cobj->onRequestComplete();
}

JNIEXPORT void JNICALL Java_com_atakmap_map_contentservices_NativeCacheRequestListener_onRequestStartedNative
    (JNIEnv *env, jclass, jlong ptr) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    CacheRequestListener *cobj = JLONG_TO_INTPTR(CacheRequestListener, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    cobj->onRequestStarted();
}

JNIEXPORT void JNICALL Java_com_atakmap_map_contentservices_NativeCacheRequestListener_onRequestProgressNative
    (JNIEnv *env, jclass, jlong ptr, jint taskNum, jint numTasks, jint taskProgress, jint maxTaskProgress, jint totalProgress, jint maxTaskProgress1) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    CacheRequestListener *cobj = JLONG_TO_INTPTR(CacheRequestListener, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    cobj->onRequestProgress(taskNum, numTasks, taskProgress, maxTaskProgress, totalProgress, maxTaskProgress1);
}

JNIEXPORT void JNICALL Java_com_atakmap_map_contentservices_NativeCacheRequestListener_onRequestCanceledNative
    (JNIEnv *env, jclass, jlong ptr) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return;
    }

    CacheRequestListener *cobj = JLONG_TO_INTPTR(CacheRequestListener, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return;
    }

    cobj->onRequestCanceled();
}

JNIEXPORT jboolean JNICALL Java_com_atakmap_map_contentservices_NativeCacheRequestListener_onRequestErrorNative
        (JNIEnv *env, jclass, jlong ptr, jthrowable, jstring message, jboolean fatal) {

    if (!checkInit(env)) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_IllegalState);
        return false;
    }

    CacheRequestListener *cobj = JLONG_TO_INTPTR(CacheRequestListener, ptr);
    if(!cobj) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return false;
    }

    TAK::Engine::Port::String cmsg;
    TAKErr code = JNIStringUTF_get(cmsg, *env, message);
    if (ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;

    return cobj->onRequestError(cmsg, fatal) == TE_Ok;
}