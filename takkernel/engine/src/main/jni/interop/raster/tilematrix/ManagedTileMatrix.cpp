
#include "util/Memory.h"

#include "interop/raster/tilematrix/ManagedTileMatrix.h"
#include "interop/java/JNILocalRef.h"
#include "interop/JNIStringUTF.h"
#include "interop/renderer/Interop.h"
#include "interop/feature/Interop.h"
#include "interop/JNIByteArray.h"

#include <limits> // numeric_limits
#include <cstring> // memcpy
#include <cmath> // NAN

using namespace TAK::Engine::Util;
using namespace TAK::Engine::Raster::TileMatrix;
using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Renderer;
using namespace TAKEngineJNI::Interop::Raster::TileMatrix;

namespace {
    struct {
        jclass id;
        jmethodID getNameID;
        jmethodID getSRIDID;
        jmethodID getZoomLevelID;
        jmethodID getOriginXID;
        jmethodID getOriginYID;
        jmethodID getTileID;
        jmethodID getTileDataID;
        jmethodID getBoundsID;
    } TileMatrix_class;

    bool TileMatrix_class_init(JNIEnv& env) {
        TileMatrix_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/raster/tilematrix/TileMatrix");
        TileMatrix_class.getNameID = env.GetMethodID(TileMatrix_class.id, "getName", "()Ljava/lang/String;");
        TileMatrix_class.getSRIDID = env.GetMethodID(TileMatrix_class.id, "getSRID", "()I");
        TileMatrix_class.getZoomLevelID = env.GetMethodID(TileMatrix_class.id, "getZoomLevel", "()[Lcom/atakmap/map/layer/raster/tilematrix/TileMatrix$ZoomLevel;");
        TileMatrix_class.getOriginXID = env.GetMethodID(TileMatrix_class.id, "getOriginX", "()D");
        TileMatrix_class.getOriginYID = env.GetMethodID(TileMatrix_class.id, "getOriginY", "()D");
        TileMatrix_class.getTileID = env.GetMethodID(TileMatrix_class.id, "getTile", "(III[Ljava/lang/Throwable;)Landroid/graphics/Bitmap;");
        TileMatrix_class.getTileDataID = env.GetMethodID(TileMatrix_class.id, "getTileData", "(III[Ljava/lang/Throwable;)[B");
        TileMatrix_class.getBoundsID = env.GetMethodID(TileMatrix_class.id, "getBounds", "()Lcom/atakmap/map/layer/feature/geometry/Envelope;");
        return true;
    }

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

    bool ZoomLevel_class_init(JNIEnv& env) {
        ZoomLevel_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/raster/tilematrix/TileMatrix$ZoomLevel");
        ZoomLevel_class.ctor = env.GetMethodID(ZoomLevel_class.id, "<init>", "()V");
        ZoomLevel_class.levelField = env.GetFieldID(ZoomLevel_class.id, "level", "I");
        ZoomLevel_class.tileWidthField = env.GetFieldID(ZoomLevel_class.id, "tileWidth", "I");
        ZoomLevel_class.tileHeightField = env.GetFieldID(ZoomLevel_class.id, "tileHeight", "I");
        ZoomLevel_class.resolutionField = env.GetFieldID(ZoomLevel_class.id, "resolution", "D");
        ZoomLevel_class.pixelSizeXField = env.GetFieldID(ZoomLevel_class.id, "pixelSizeX", "D");
        ZoomLevel_class.pixelSizeYField = env.GetFieldID(ZoomLevel_class.id, "pixelSizeY", "D");
        return true;
    }

    bool checkInit(JNIEnv& env) {
        static bool v = TileMatrix_class_init(env) &&
                ZoomLevel_class_init(env);
        return v;
    }
}

ManagedTileMatrix::ManagedTileMatrix(JNIEnv &env, jobject impl) NOTHROWS : impl(env.NewGlobalRef(impl)) {
    checkInit(env);
}

ManagedTileMatrix::~ManagedTileMatrix() NOTHROWS {
    LocalJNIEnv env;
    if (impl)
        env->DeleteGlobalRef(impl);
}

const char* ManagedTileMatrix::getName() const NOTHROWS {
    if (!impl)
        return nullptr;

    LocalJNIEnv env;
    Java::JNILocalRef nameStr(*env, env->CallObjectMethod(impl, TileMatrix_class.getNameID));
    cached_name_ = nullptr;
    JNIStringUTF_get(cached_name_, *env, static_cast<jstring>(nameStr.get()));
    return cached_name_;
}

int ManagedTileMatrix::getSRID() const NOTHROWS {

    if (!impl)
        return 0;

    LocalJNIEnv env;
    return env->CallIntMethod(impl, TileMatrix_class.getSRIDID);
}

TAK::Engine::Util::TAKErr ManagedTileMatrix::getZoomLevel(TAK::Engine::Port::Collection<ZoomLevel>& value) const NOTHROWS {

    LocalJNIEnv env;

    Java::JNILocalRef arrRef(*env, env->CallObjectMethod(impl, TileMatrix_class.getZoomLevelID));
    jsize len = env->GetArrayLength(static_cast<jobjectArray>(arrRef.get()));
    for (jsize i = 0; i < len; ++i) {
        TileMatrix::ZoomLevel zl;
        Java::JNILocalRef elemRef(*env, env->GetObjectArrayElement(static_cast<jobjectArray>(arrRef.get()), i));
        Interop_get(zl, *env, elemRef.get());
        value.add(zl);
    }

    return TE_Ok;
}

double ManagedTileMatrix::getOriginX() const NOTHROWS {

    if (!impl)
        return NAN;

    LocalJNIEnv env;
    return env->CallDoubleMethod(impl, TileMatrix_class.getOriginXID);
}

double ManagedTileMatrix::getOriginY() const NOTHROWS {

    if (!impl)
        return NAN;

    LocalJNIEnv env;
    return env->CallDoubleMethod(impl, TileMatrix_class.getOriginYID);
}

TAK::Engine::Util::TAKErr ManagedTileMatrix::getTile(TAK::Engine::Renderer::BitmapPtr& result, const std::size_t zoom, const std::size_t x, const std::size_t y) NOTHROWS {

    if (!impl)
        return TE_IllegalState;

    // unlikely, but safer
    if (zoom > std::numeric_limits<jint>::max() || x > std::numeric_limits<jint>::max() || y > std::numeric_limits<jint>::max())
        return TE_InvalidArg;

    LocalJNIEnv env;
    Java::JNILocalRef bitmapRef(*env, env->CallObjectMethod(impl, TileMatrix_class.getTileID, (jint)zoom, (jint)x, (jint)y, nullptr));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    return Interop_marshal(result, env, bitmapRef.get());
}

TAK::Engine::Util::TAKErr ManagedTileMatrix::getTileData(std::unique_ptr<const uint8_t, void (*)(const uint8_t*)>& value, std::size_t* len,
                                              const std::size_t zoom, const std::size_t x, const std::size_t y) NOTHROWS {
    if (!impl)
        return TE_IllegalState;
    if (!len)
        return TE_InvalidArg;

    // unlikely, but safer
    if (zoom > std::numeric_limits<jint>::max() || x > std::numeric_limits<jint>::max() || y > std::numeric_limits<jint>::max())
        return TE_InvalidArg;

    LocalJNIEnv env;
    Java::JNILocalRef objRef(*env, env->CallObjectMethod(impl, TileMatrix_class.getTileDataID,
                                                         static_cast<jint>(zoom),
                                                         static_cast<jint>(x),
                                                         static_cast<jint>(y),
                                                         nullptr));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    if (objRef) {
        JNIByteArray arr(*env, (jbyteArray)objRef.get(), JNI_ABORT);
        if (arr.length() > 0) {
            auto buffer = std::unique_ptr<uint8_t>(new (std::nothrow) uint8_t[arr.length()]);
            if (!buffer)
                return TE_OutOfMemory;

            memcpy(buffer.get(), arr.get<const uint8_t>(), arr.length());
            value = std::unique_ptr<const uint8_t, void (*)(const uint8_t*)>(buffer.release(), Memory_array_deleter_const<uint8_t>);
        }
        *len = arr.length();
        return TE_Ok;
    } else {
        value = std::unique_ptr<const uint8_t, void (*)(const uint8_t*)>(nullptr, nullptr);
        *len = 0;
        return TE_Done;
    }
}

TAK::Engine::Util::TAKErr ManagedTileMatrix::getBounds(TAK::Engine::Feature::Envelope2 *value) const NOTHROWS {

    if (!impl)
        return TE_IllegalState;
    if (!value)
        return TE_InvalidArg;

    LocalJNIEnv env;

    Java::JNILocalRef objRef(*env, env->CallObjectMethod(impl, TileMatrix_class.getBoundsID));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    return Feature::Interop_copy(value, env, objRef.get());
}

TAK::Engine::Util::TAKErr TAKEngineJNI::Interop::Raster::TileMatrix::Interop_get(TAK::Engine::Raster::TileMatrix::TileMatrix::ZoomLevel& result,
                                                                                 JNIEnv& env, jobject object) NOTHROWS {

    if (!checkInit(env))
        return TE_IllegalState;

    result.level = env.GetIntField(object, ZoomLevel_class.levelField);
    result.tileHeight = env.GetIntField(object, ZoomLevel_class.tileHeightField);
    result.tileWidth = env.GetIntField(object, ZoomLevel_class.tileWidthField);
    result.resolution = env.GetDoubleField(object, ZoomLevel_class.resolutionField);
    result.pixelSizeX = env.GetDoubleField(object, ZoomLevel_class.pixelSizeXField);
    result.pixelSizeY = env.GetDoubleField(object, ZoomLevel_class.pixelSizeYField);
    return TE_Ok;
}
