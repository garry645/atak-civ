#include "interop/formats/c3dt/Interop.h"

#include <util/Memory.h>

#include "glm/gtc/type_ptr.hpp"

#include "common.h"
#include "interop/Pointer.h"

using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Java;

namespace
{
    struct {
        jclass id;
        jmethodID ctor;
        jfieldID pointer;
    } Tileset_class;

    struct {
        jclass id;
        jfieldID maxScreenSpaceError;
    } TilesetOpenOptions_class;

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID pointer;
    } Tile_class;

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID tilesToRenderThisFrame;
        jfieldID tilesFadingOut;
        jfieldID workerThreadTileLoadQueueLength;
        jfieldID mainThreadTileLoadQueueLength;
        jfieldID tilesVisited;
        jfieldID culledTilesVisited;
        jfieldID tilesCulled;
        jfieldID tilesOccluded;
        jfieldID tilesWaitingForOcclusionResults;
        jfieldID tilesKicked;
        jfieldID maxDepthVisited;
        jfieldID frameNumber;
    } ViewUpdateResults_class;

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID pointer;
    } TileExternalContent_class;

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID pointer;
    } TileRenderContent_class;

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID pointer;
    } Model_class;

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID centerX;
        jfieldID centerY;
        jfieldID centerZ;
        jfieldID xDirHalfLen;
        jfieldID yDirHalfLen;
        jfieldID zDirHalfLen;
    } Volume_Box_class;

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID west;
        jfieldID south;
        jfieldID east;
        jfieldID north;
        jfieldID minimumHeight;
        jfieldID maximumHeight;
    } Volume_Region_class;

    struct {
        jclass id;
        jmethodID ctor;
        jfieldID centerX;
        jfieldID centerY;
        jfieldID centerZ;
        jfieldID radius;
    } Volume_Sphere_class;

    bool checkInit(JNIEnv &env) NOTHROWS;
    bool C3DT_interop_init(JNIEnv &env) NOTHROWS;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(JNILocalRef &mtileset, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::TilesetPtr &&ctileset) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    Java::JNILocalRef mtilesetPointer(env, NewPointer(&env, std::move(ctileset)));
    mtileset = JNILocalRef(env, env.NewObject(Tileset_class.id, Tileset_class.ctor, mtilesetPointer.get()));
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_copy(TAK::Engine::Formats::Cesium3DTiles::TilesetOpenOptions *copts, JNIEnv &env, jobject jopts) NOTHROWS
{
    if(!copts)
        return TE_InvalidArg;
    if(!jopts)
        return TE_InvalidArg;
    if(!checkInit(env))
        return TE_IllegalState;

    copts->maximumScreenSpaceError = env.GetDoubleField(jopts, TilesetOpenOptions_class.maxScreenSpaceError);

    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(JNILocalRef &mtile, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::TilePtr &&ctile) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    Java::JNILocalRef mtilePointer(env, NewPointer(&env, std::move(ctile)));
    mtile = JNILocalRef(env, env.NewObject(Tile_class.id, Tile_class.ctor, mtilePointer.get()));
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(JNILocalRef &mtileArray, JNIEnv &env, gsl::span<Cesium3DTilesSelection::Tile> &ctileSpan) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    mtileArray = Java::JNILocalRef(env, env.NewObjectArray((jsize)ctileSpan.size(), Tile_class.id, nullptr));
    for (int a=0; a<ctileSpan.size();a++) {
        Java::JNILocalRef mtile(env, nullptr);
        TAK::Engine::Formats::Cesium3DTiles::TilePtr tilePtr(&ctileSpan[a], NULL);
        TAKErr code = Interop_marshal(mtile, env, std::move(tilePtr));
        TE_CHECKRETURN_CODE(code);
        env.SetObjectArrayElement((jobjectArray)(mtileArray.get()), a, mtile);
    }
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(JNILocalRef &mviewUpdateResult, JNIEnv &env, const Cesium3DTilesSelection::ViewUpdateResult &cviewUpdateResult) NOTHROWS\
{
    if(!checkInit(env))
        return TE_IllegalState;

    TAKErr code;
    mviewUpdateResult = JNILocalRef(env, env.NewObject(ViewUpdateResults_class.id, ViewUpdateResults_class.ctor));
    std::vector<jobject> ctilesToRenderThisFrameArray;
    for (auto it : cviewUpdateResult.tilesToRenderThisFrame) {
        Java::JNILocalRef mtile(env, NULL);
        TAK::Engine::Formats::Cesium3DTiles::TilePtr tilePtr(it, NULL);
        code = Interop_marshal(mtile, env, std::move(tilePtr));
        TE_CHECKBREAK_CODE(code)
        ctilesToRenderThisFrameArray.push_back(mtile.release());
    }
    jobjectArray jtilesToRenderThisFrameArray = env.NewObjectArray(ctilesToRenderThisFrameArray.size(), Tile_class.id, NULL);
    if (jtilesToRenderThisFrameArray == NULL) return TE_OutOfMemory;
    for (int i = 0; i < ctilesToRenderThisFrameArray.size(); i++) {
        env.SetObjectArrayElement(jtilesToRenderThisFrameArray, i, ctilesToRenderThisFrameArray[i]);
    }
    env.SetObjectField(mviewUpdateResult, ViewUpdateResults_class.tilesToRenderThisFrame, jtilesToRenderThisFrameArray);

    std::vector<jobject> ctilesFadingOutArray;
    for (auto it : cviewUpdateResult.tilesFadingOut) {
        Java::JNILocalRef mtile(env, NULL);
        TAK::Engine::Formats::Cesium3DTiles::TilePtr tilePtr(it, NULL);
        code = Interop_marshal(mtile, env, std::move(tilePtr));
        TE_CHECKBREAK_CODE(code)
        ctilesFadingOutArray.push_back(mtile.release());
    }
    jobjectArray jtilesFadingOutArray = env.NewObjectArray(ctilesFadingOutArray.size(), Tile_class.id, NULL);
    if (jtilesFadingOutArray == NULL) return TE_OutOfMemory;
    for (int i = 0; i < ctilesFadingOutArray.size(); i++) {
        env.SetObjectArrayElement(jtilesFadingOutArray, i, ctilesFadingOutArray[i]);
    }
    env.SetObjectField(mviewUpdateResult, ViewUpdateResults_class.tilesFadingOut, jtilesFadingOutArray);

    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.workerThreadTileLoadQueueLength, cviewUpdateResult.workerThreadTileLoadQueueLength);
    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.mainThreadTileLoadQueueLength, cviewUpdateResult.mainThreadTileLoadQueueLength);
    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.tilesVisited, cviewUpdateResult.tilesVisited);
    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.culledTilesVisited, cviewUpdateResult.culledTilesVisited);
    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.tilesCulled, cviewUpdateResult.tilesCulled);
    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.tilesOccluded, cviewUpdateResult.tilesOccluded);
    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.tilesWaitingForOcclusionResults, cviewUpdateResult.tilesWaitingForOcclusionResults);
    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.tilesKicked, cviewUpdateResult.tilesKicked);
    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.maxDepthVisited, cviewUpdateResult.maxDepthVisited);
    env.SetIntField(mviewUpdateResult, ViewUpdateResults_class.frameNumber, cviewUpdateResult.frameNumber);
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(JNILocalRef &mtileExternalContent, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::TileExternalContentPtr &&ctileExternalContentPtr) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    Java::JNILocalRef mtileExternalContentPointer(env, NewPointer(&env, std::move(ctileExternalContentPtr)));
    mtileExternalContent = JNILocalRef(env, env.NewObject(TileExternalContent_class.id, TileExternalContent_class.ctor, mtileExternalContentPointer.get()));
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(JNILocalRef &mtileRenderContent, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::TileRenderContentPtr &&ctileRenderContentPtr) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    Java::JNILocalRef mtileRenderContentPointer(env, NewPointer(&env, std::move(ctileRenderContentPtr)));
    mtileRenderContent = JNILocalRef(env, env.NewObject(TileRenderContent_class.id, TileRenderContent_class.ctor, mtileRenderContentPointer.get()));
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(Java::JNILocalRef &mmodel, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::ModelPtr &&cmodelPtr) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    Java::JNILocalRef mmodelPointer(env, NewPointer(&env, std::move(cmodelPtr)));
    mmodel = JNILocalRef(env, env.NewObject(Model_class.id, Model_class.ctor, mmodelPointer.get()));
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(JNILocalRef &mvolume, JNIEnv &env, CesiumGeometry::OrientedBoundingBox &cvolume) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    mvolume = JNILocalRef(env, env.NewObject(Volume_Box_class.id, Volume_Box_class.ctor));
    env.SetDoubleField(mvolume, Volume_Box_class.centerX, cvolume.getCenter().x);
    env.SetDoubleField(mvolume, Volume_Box_class.centerY, cvolume.getCenter().y);
    env.SetDoubleField(mvolume, Volume_Box_class.centerZ, cvolume.getCenter().z);

    double row[3];
    row[0] = cvolume.getLengths()[0] / 2.0;
    row[1] = 0;
    row[2] = 0;
    Java::JNILocalRef mxarray(env, env.NewDoubleArray(3));
    env.SetDoubleArrayRegion((jdoubleArray)mxarray.get(), 0, 3, row);
    env.SetObjectField(mvolume, Volume_Box_class.xDirHalfLen, mxarray.get());

    row[0] = 0;
    row[1] = cvolume.getLengths()[1] / 2.0;
    row[2] = 0;
    Java::JNILocalRef myarray(env, env.NewDoubleArray(3));
    env.SetDoubleArrayRegion((jdoubleArray)myarray.get(), 0, 3, row);
    env.SetObjectField(mvolume, Volume_Box_class.yDirHalfLen, myarray.get());

    row[0] = 0;
    row[1] = 0;
    row[2] = cvolume.getLengths()[2] / 2.0;
    Java::JNILocalRef mzarray(env, env.NewDoubleArray(3));
    env.SetDoubleArrayRegion((jdoubleArray)mzarray.get(), 0, 3, row);
    env.SetObjectField(mvolume, Volume_Box_class.zDirHalfLen, mzarray.get());

    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(JNILocalRef &mvolume, JNIEnv &env, CesiumGeospatial::BoundingRegion &cvolume) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    mvolume = JNILocalRef(env, env.NewObject(Volume_Region_class.id, Volume_Region_class.ctor));
    env.SetDoubleField(mvolume, Volume_Region_class.west, cvolume.getRectangle().getWest());
    env.SetDoubleField(mvolume, Volume_Region_class.south, cvolume.getRectangle().getSouth());
    env.SetDoubleField(mvolume, Volume_Region_class.east, cvolume.getRectangle().getEast());
    env.SetDoubleField(mvolume, Volume_Region_class.north, cvolume.getRectangle().getNorth());
    env.SetDoubleField(mvolume, Volume_Region_class.minimumHeight, cvolume.getMinimumHeight());
    env.SetDoubleField(mvolume, Volume_Region_class.maximumHeight, cvolume.getMaximumHeight());
    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Formats::Cesium3DTiles::Interop_marshal(JNILocalRef &mvolume, JNIEnv &env, CesiumGeometry::BoundingSphere &cvolume) NOTHROWS
{
    if(!checkInit(env))
        return TE_IllegalState;

    mvolume = JNILocalRef(env, env.NewObject(Volume_Sphere_class.id, Volume_Sphere_class.ctor));
    env.SetDoubleField(mvolume, Volume_Sphere_class.centerX, cvolume.getCenter().x);
    env.SetDoubleField(mvolume, Volume_Sphere_class.centerY, cvolume.getCenter().y);
    env.SetDoubleField(mvolume, Volume_Sphere_class.centerZ, cvolume.getCenter().z);
    env.SetDoubleField(mvolume, Volume_Sphere_class.radius, cvolume.getRadius());
    return TE_Ok;
}

namespace
{
    bool checkInit(JNIEnv &env) NOTHROWS
    {
        static bool clinit = C3DT_interop_init(env);
        return clinit;
    }
    bool C3DT_interop_init(JNIEnv &env) NOTHROWS
    {
        Tileset_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/Tileset");
        if (!Tileset_class.id) return false;
        Tileset_class.ctor = env.GetMethodID(Tileset_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;)V");
        if (!Tileset_class.ctor) return false;
        Tileset_class.pointer = env.GetFieldID(Tileset_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        if (!Tileset_class.pointer) return false;

        TilesetOpenOptions_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/Tileset$OpenOptions");
        if (!TilesetOpenOptions_class.id) return false;
        TilesetOpenOptions_class.maxScreenSpaceError = env.GetFieldID(TilesetOpenOptions_class.id, "maxScreenSpaceError", "D");
        if (!TilesetOpenOptions_class.maxScreenSpaceError) return false;

        Tile_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/Tile");
        if (!Tile_class.id) return false;
        Tile_class.ctor = env.GetMethodID(Tile_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;)V");
        if (!Tile_class.ctor) return false;
        Tile_class.pointer = env.GetFieldID(Tile_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        if (!Tile_class.pointer) return false;

        ViewUpdateResults_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/ViewUpdateResults");
        if (!ViewUpdateResults_class.id) return false;
        ViewUpdateResults_class.ctor = env.GetMethodID(ViewUpdateResults_class.id, "<init>", "()V");
        if (!ViewUpdateResults_class.ctor) return false;
        ViewUpdateResults_class.tilesToRenderThisFrame = env.GetFieldID(ViewUpdateResults_class.id, "tilesToRenderThisFrame", "[Lcom/atakmap/map/formats/c3dt/Tile;");
        if (!ViewUpdateResults_class.tilesToRenderThisFrame) return false;
        ViewUpdateResults_class.tilesFadingOut = env.GetFieldID(ViewUpdateResults_class.id, "tilesFadingOut", "[Lcom/atakmap/map/formats/c3dt/Tile;");
        if (!ViewUpdateResults_class.tilesFadingOut) return false;
        ViewUpdateResults_class.workerThreadTileLoadQueueLength = env.GetFieldID(ViewUpdateResults_class.id, "workerThreadTileLoadQueueLength", "I");
        if (!ViewUpdateResults_class.workerThreadTileLoadQueueLength) return false;
        ViewUpdateResults_class.mainThreadTileLoadQueueLength = env.GetFieldID(ViewUpdateResults_class.id, "mainThreadTileLoadQueueLength", "I");
        if (!ViewUpdateResults_class.mainThreadTileLoadQueueLength) return false;
        ViewUpdateResults_class.tilesVisited = env.GetFieldID(ViewUpdateResults_class.id, "tilesVisited", "I");
        if (!ViewUpdateResults_class.tilesVisited) return false;
        ViewUpdateResults_class.culledTilesVisited = env.GetFieldID(ViewUpdateResults_class.id, "culledTilesVisited", "I");
        if (!ViewUpdateResults_class.culledTilesVisited) return false;
        ViewUpdateResults_class.tilesCulled = env.GetFieldID(ViewUpdateResults_class.id, "tilesCulled", "I");
        if (!ViewUpdateResults_class.tilesCulled) return false;
        ViewUpdateResults_class.tilesOccluded = env.GetFieldID(ViewUpdateResults_class.id, "tilesOccluded", "I");
        if (!ViewUpdateResults_class.tilesOccluded) return false;
        ViewUpdateResults_class.tilesWaitingForOcclusionResults = env.GetFieldID(ViewUpdateResults_class.id, "tilesWaitingForOcclusionResults", "I");
        if (!ViewUpdateResults_class.tilesWaitingForOcclusionResults) return false;
        ViewUpdateResults_class.tilesKicked = env.GetFieldID(ViewUpdateResults_class.id, "tilesKicked", "I");
        if (!ViewUpdateResults_class.tilesKicked) return false;
        ViewUpdateResults_class.maxDepthVisited = env.GetFieldID(ViewUpdateResults_class.id, "maxDepthVisited", "I");
        if (!ViewUpdateResults_class.maxDepthVisited) return false;
        ViewUpdateResults_class.frameNumber = env.GetFieldID(ViewUpdateResults_class.id, "frameNumber", "I");
        if (!ViewUpdateResults_class.frameNumber) return false;

        TileExternalContent_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/TileExternalContent");
        if (!TileExternalContent_class.id) return false;
        TileExternalContent_class.ctor = env.GetMethodID(TileExternalContent_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;)V");
        if (!TileExternalContent_class.ctor) return false;
        TileExternalContent_class.pointer = env.GetFieldID(TileExternalContent_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        if (!TileExternalContent_class.pointer) return false;

        TileRenderContent_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/TileRenderContent");
        if (!TileRenderContent_class.id) return false;
        TileRenderContent_class.ctor = env.GetMethodID(TileRenderContent_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;)V");
        if (!TileRenderContent_class.ctor) return false;
        TileRenderContent_class.pointer = env.GetFieldID(TileRenderContent_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        if (!TileRenderContent_class.pointer) return false;

        Model_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/Model");
        if (!Model_class.id) return false;
        Model_class.ctor = env.GetMethodID(Model_class.id, "<init>", "(Lcom/atakmap/interop/Pointer;)V");
        if (!Model_class.ctor) return false;
        Model_class.pointer = env.GetFieldID(Model_class.id, "pointer", "Lcom/atakmap/interop/Pointer;");
        if (!Model_class.pointer) return false;

        Volume_Box_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/Volume$Box");
        if (!Volume_Box_class.id) return false;
        Volume_Box_class.ctor = env.GetMethodID(Volume_Box_class.id, "<init>", "()V");
        if (!Volume_Box_class.ctor) return false;
        Volume_Box_class.centerX = env.GetFieldID(Volume_Box_class.id, "centerX", "D");
        if (!Volume_Box_class.centerX) return false;
        Volume_Box_class.centerY = env.GetFieldID(Volume_Box_class.id, "centerY", "D");
        if (!Volume_Box_class.centerY) return false;
        Volume_Box_class.centerZ = env.GetFieldID(Volume_Box_class.id, "centerZ", "D");
        if (!Volume_Box_class.centerZ) return false;
        Volume_Box_class.xDirHalfLen = env.GetFieldID(Volume_Box_class.id, "xDirHalfLen", "[D");
        if (!Volume_Box_class.xDirHalfLen) return false;
        Volume_Box_class.yDirHalfLen = env.GetFieldID(Volume_Box_class.id, "yDirHalfLen", "[D");
        if (!Volume_Box_class.yDirHalfLen) return false;
        Volume_Box_class.zDirHalfLen = env.GetFieldID(Volume_Box_class.id, "zDirHalfLen", "[D");
        if (!Volume_Box_class.zDirHalfLen) return false;

        Volume_Region_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/Volume$Region");
        if (!Volume_Region_class.id) return false;
        Volume_Region_class.ctor = env.GetMethodID(Volume_Region_class.id, "<init>", "()V");
        if (!Volume_Region_class.ctor) return false;
        Volume_Region_class.west = env.GetFieldID(Volume_Region_class.id, "west", "D");
        if (!Volume_Region_class.west) return false;
        Volume_Region_class.south = env.GetFieldID(Volume_Region_class.id, "south", "D");
        if (!Volume_Region_class.south) return false;
        Volume_Region_class.east = env.GetFieldID(Volume_Region_class.id, "east", "D");
        if (!Volume_Region_class.east) return false;
        Volume_Region_class.north = env.GetFieldID(Volume_Region_class.id, "north", "D");
        if (!Volume_Region_class.north) return false;
        Volume_Region_class.minimumHeight = env.GetFieldID(Volume_Region_class.id, "minimumHeight", "D");
        if (!Volume_Region_class.minimumHeight) return false;
        Volume_Region_class.maximumHeight = env.GetFieldID(Volume_Region_class.id, "maximumHeight", "D");
        if (!Volume_Region_class.maximumHeight) return false;

        Volume_Sphere_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/formats/c3dt/Volume$Sphere");
        if (!Volume_Sphere_class.id) return false;
        Volume_Sphere_class.ctor = env.GetMethodID(Volume_Sphere_class.id, "<init>", "()V");
        if (!Volume_Sphere_class.ctor) return false;
        Volume_Sphere_class.centerX = env.GetFieldID(Volume_Sphere_class.id, "centerX", "D");
        if (!Volume_Sphere_class.centerX) return false;
        Volume_Sphere_class.centerY = env.GetFieldID(Volume_Sphere_class.id, "centerY", "D");
        if (!Volume_Sphere_class.centerY) return false;
        Volume_Sphere_class.centerZ = env.GetFieldID(Volume_Sphere_class.id, "centerZ", "D");
        if (!Volume_Sphere_class.centerZ) return false;
        Volume_Sphere_class.radius = env.GetFieldID(Volume_Sphere_class.id, "radius", "D");
        if (!Volume_Sphere_class.radius) return false;

        return true;
    }
}
