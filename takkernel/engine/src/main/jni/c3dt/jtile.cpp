#include <string>

#ifdef __ANDROID__
#include <android/log.h>
#endif

#include "common.h"
#include "memory.h"

#include <sstream>

#include "glm/gtc/type_ptr.hpp"

#include "formats/c3dt/Tile.h"
#include "interop/formats/c3dt/Interop.h"
#include "interop/java/JNILocalRef.h"
#include "port/String.h"

using namespace TAK::Engine::Util;
using namespace TAK::Engine::Formats::Cesium3DTiles;
using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Formats::Cesium3DTiles;

extern "C" JNIEXPORT jint JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getTransform
        (JNIEnv *env, jclass, jlong ptr, jdoubleArray mresult)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return TE_Err;
    }
    auto transform = tile->getTransform();
    auto transformPtr = glm::value_ptr(transform);
    env->SetDoubleArrayRegion(mresult, 0, 16, transformPtr);
    return TE_Ok;
}

extern "C" JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getParent
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return NULL;
    }
    auto parent = tile->getParent();
    if (!parent) return NULL;
    Java::JNILocalRef mparent(*env, nullptr);
    TilePtr parentPtr(parent, NULL);
    TAKErr code = Interop_marshal(mparent, *env, std::move(parentPtr));
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return (jobject) mparent.release();
}

extern "C" JNIEXPORT jobjectArray JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getChildren
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return NULL;
    }
    auto children = tile->getChildren();
    Java::JNILocalRef mtileArray(*env, nullptr);
    TAKErr code = Interop_marshal(mtileArray, *env, children);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return (jobjectArray) mtileArray.release();
}

extern "C" JNIEXPORT jint JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getBoundingVolumeType
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return 0;
    }
    auto boundingVolume = tile->getBoundingVolume();
    Java::JNILocalRef mvolume(*env, NULL);
    if (std::holds_alternative<CesiumGeometry::BoundingSphere>(boundingVolume)) {
        return 2;
    } else if (std::holds_alternative<CesiumGeometry::OrientedBoundingBox>(boundingVolume)) {
        return 0;
    } else if (std::holds_alternative<CesiumGeospatial::BoundingRegion>(boundingVolume) ||
               std::holds_alternative<CesiumGeospatial::BoundingRegionWithLooseFittingHeights>(boundingVolume) ||
               std::holds_alternative<CesiumGeospatial::S2CellBoundingVolume>(boundingVolume)) {
        return 1;
    }
    return 0;
}

extern "C" JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getBoundingVolume
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return NULL;
    }
    auto boundingVolume = tile->getBoundingVolume();
    Java::JNILocalRef mvolume(*env, NULL);
    if (std::holds_alternative<CesiumGeometry::BoundingSphere>(boundingVolume)) {
        CesiumGeometry::BoundingSphere boundingSphere = std::get<CesiumGeometry::BoundingSphere>(boundingVolume);
        TAKErr code = Interop_marshal(mvolume, *env, boundingSphere);
        if(ATAKMapEngineJNI_checkOrThrow(env, code))
            return NULL;
    } else if (std::holds_alternative<CesiumGeometry::OrientedBoundingBox>(boundingVolume)) {
        CesiumGeometry::OrientedBoundingBox orientedBoundingBox = std::get<CesiumGeometry::OrientedBoundingBox>(boundingVolume);
        TAKErr code = Interop_marshal(mvolume, *env, orientedBoundingBox);
        if(ATAKMapEngineJNI_checkOrThrow(env, code))
            return NULL;
    } else if (std::holds_alternative<CesiumGeospatial::BoundingRegion>(boundingVolume)) {
        CesiumGeospatial::BoundingRegion boundingRegion = std::get<CesiumGeospatial::BoundingRegion>(boundingVolume);
        TAKErr code = Interop_marshal(mvolume, *env, boundingRegion);
        if(ATAKMapEngineJNI_checkOrThrow(env, code))
            return NULL;
    } else if (std::holds_alternative<CesiumGeospatial::BoundingRegionWithLooseFittingHeights>(boundingVolume)) {
        CesiumGeospatial::BoundingRegionWithLooseFittingHeights boundingRegionWithLooseFittingHeights = std::get<CesiumGeospatial::BoundingRegionWithLooseFittingHeights>(boundingVolume);
        CesiumGeospatial::BoundingRegion boundingRegion = boundingRegionWithLooseFittingHeights.getBoundingRegion();
        TAKErr code = Interop_marshal(mvolume, *env, boundingRegion);
        if(ATAKMapEngineJNI_checkOrThrow(env, code))
            return NULL;
    } else if (std::holds_alternative<CesiumGeospatial::S2CellBoundingVolume>(boundingVolume)) {
        CesiumGeospatial::S2CellBoundingVolume s2CellBoundingVolume = std::get<CesiumGeospatial::S2CellBoundingVolume>(boundingVolume);
        std::unique_ptr<CesiumGeospatial::BoundingRegion> boundingRegion;
        TAKErr code = Tile_computeBoundingRegion(boundingRegion, s2CellBoundingVolume);
        if(ATAKMapEngineJNI_checkOrThrow(env, code))
            return nullptr;
        code = Interop_marshal(mvolume, *env, *boundingRegion);
        if(ATAKMapEngineJNI_checkOrThrow(env, code))
            return NULL;
    }
    return mvolume.release();
}

extern "C" JNIEXPORT jdoubleArray JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getGlobeRectangle
        (JNIEnv *env, jclass, jlong ptr) {
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return NULL;
    }
    auto boundingVolume = tile->getBoundingVolume();
    std::optional<CesiumGeospatial::GlobeRectangle> globeRectangle;
    TAKErr code = Tile_estimateGlobeRectangle(&globeRectangle, boundingVolume);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    if (!globeRectangle.has_value()) return nullptr;
    Java::JNILocalRef mglobeRectangleArray(*env, env->NewDoubleArray(4));
    jdouble *aryPtr = env->GetDoubleArrayElements((jdoubleArray) mglobeRectangleArray.get(),
                                                  JNI_FALSE);
    aryPtr[0] = globeRectangle->getWest();
    aryPtr[1] = globeRectangle->getSouth();
    aryPtr[2] = globeRectangle->getEast();
    aryPtr[3] = globeRectangle->getNorth();
    env->ReleaseDoubleArrayElements((jdoubleArray) mglobeRectangleArray.get(), aryPtr, 0);
    return (jdoubleArray) mglobeRectangleArray.release();
}

extern "C" JNIEXPORT jint JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getContentType
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return 0;
    }
    int contentType;
    TAKErr code = Tile_getContentType(&contentType, *tile);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return 0;
    return TE_Err;
}

extern "C" JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getExternalContent
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return NULL;
    }
    const Cesium3DTilesSelection::TileExternalContent* externalContent;
    TAKErr code = Tile_getExternalContent(&externalContent, *tile);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    if (!externalContent) return NULL;
    TileExternalContentPtr externalContentPtr(const_cast<Cesium3DTilesSelection::TileExternalContent*>(externalContent), NULL);
    Java::JNILocalRef mtileExternalContent(*env, NULL);
    code = Formats::Cesium3DTiles::Interop_marshal(mtileExternalContent, *env, std::move(externalContentPtr));
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return mtileExternalContent.release();
}

extern "C" JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getRenderContent
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return NULL;
    }
    const Cesium3DTilesSelection::TileRenderContent* renderContent;
    TAKErr code = Tile_getRenderContent(&renderContent, *tile);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    if (!renderContent) return NULL;
    TileRenderContentPtr renderContentPtr(const_cast<Cesium3DTilesSelection::TileRenderContent*>(renderContent), NULL);
    Java::JNILocalRef mtileRenderContent(*env, NULL);
    code = Formats::Cesium3DTiles::Interop_marshal(mtileRenderContent, *env, std::move(renderContentPtr));
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return NULL;
    return mtileRenderContent.release();
}

extern "C" JNIEXPORT jstring JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getTileId
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return NULL;
    }
    TAK::Engine::Port::String tileIdStr;
    TAK::Engine::Formats::Cesium3DTiles::Tile_createTileIdString(&tileIdStr, *tile);
    return env->NewStringUTF(tileIdStr.get());
}

extern "C" JNIEXPORT jint JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getRefine
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return -1;
    }
    int refine;
    TAKErr code = Tile_getRefine(&refine, *tile);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return -1;
    return refine;
}

extern "C" JNIEXPORT jboolean JNICALL Java_com_atakmap_map_formats_c3dt_Tile_isRenderable
        (JNIEnv *env, jclass, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return false;
    }
    bool isRenderable;
    TAKErr code = Tile_isRenderable(&isRenderable, *tile);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return false;
    return isRenderable;
}

extern "C" JNIEXPORT jdouble JNICALL Java_com_atakmap_map_formats_c3dt_Tile_getGeometricError
        (JNIEnv *env, jclass clazz, jlong ptr)
{
    Cesium3DTilesSelection::Tile *tile = JLONG_TO_INTPTR(Cesium3DTilesSelection::Tile, ptr);
    if(!tile) {
#ifdef __ANDROID__
        __android_log_print(ANDROID_LOG_ERROR, "Cesium3DTiles", "NULL tile");
#endif
        return 0;
    }
    return tile->getGeometricError();
}