
#include "interop/raster/tilematrix/ManagedTileContainer.h"
#include "interop/JNIByteArray.h"
#include "interop/java/JNILocalRef.h"
#include "interop/renderer/Interop.h"

#include <limits> // numeric_limits
#include <cstring> // memcpy

using namespace TAK::Engine::Util;
using namespace TAKEngineJNI::Interop::Raster::TileMatrix;
using namespace TAK::Engine::Renderer;
using namespace TAKEngineJNI::Interop;
using namespace TAKEngineJNI::Interop::Renderer;

namespace {
    struct {
        jclass id;
        jmethodID isReadOnlyID;
        jmethodID setTileID;
        jmethodID setTileDataID;
        jmethodID hasTileExpirationMetadataID;
        jmethodID getTileExpirationID;
    } TileContainer_class;

    bool Init_impl(JNIEnv& env) {
        TileContainer_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/map/layer/raster/tilematrix/TileContainer");
        TileContainer_class.isReadOnlyID = env.GetMethodID(TileContainer_class.id, "isReadOnly", "()Z");
        TileContainer_class.setTileID = env.GetMethodID(TileContainer_class.id, "setTile", "(IIILandroid/graphics/Bitmap;J)V");
        TileContainer_class.setTileDataID = env.GetMethodID(TileContainer_class.id, "setTile", "(III[BJ)V");
        TileContainer_class.hasTileExpirationMetadataID = env.GetMethodID(TileContainer_class.id, "hasTileExpirationMetadata", "()Z");
        TileContainer_class.getTileExpirationID = env.GetMethodID(TileContainer_class.id, "getTileExpiration", "(III)J");
        return true;
    }

    bool checkInit(JNIEnv& env) {
        static bool v = Init_impl(env);
        return v;
    }
}

ManagedTileContainer::ManagedTileContainer(JNIEnv &env, jobject impl) NOTHROWS
    : ManagedTileMatrix(env, impl) {
    checkInit(env);
}

ManagedTileContainer::~ManagedTileContainer() NOTHROWS {}

TAKErr ManagedTileContainer::isReadOnly(bool* value) NOTHROWS {
    LocalJNIEnv env;
    *value = env->CallBooleanMethod(impl, TileContainer_class.isReadOnlyID);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }
    return TE_Ok;
}

TAKErr ManagedTileContainer::setTile(const std::size_t level, const std::size_t x, const std::size_t y, const uint8_t* value, const std::size_t len, const int64_t expiration) NOTHROWS {

    if (level > std::numeric_limits<jint>::max() ||
        x > std::numeric_limits<jint>::max() ||
        y > std::numeric_limits<jint>::max())
        return TE_InvalidArg;

    LocalJNIEnv env;

    Java::JNILocalRef arrRef(*env, env->NewByteArray(len));
    JNIByteArray arr(*env, static_cast<jbyteArray>(arrRef.get()), 0);
    memcpy(arr.get<uint8_t>(), value, len);

    env->CallVoidMethod(impl, TileContainer_class.setTileDataID, static_cast<jint>(level), static_cast<jint>(x), static_cast<jint>(y),
                        static_cast<jbyteArray>(arrRef.get()), expiration);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }
    return TE_Ok;
}

TAKErr ManagedTileContainer::setTile(const std::size_t level, const std::size_t x, const std::size_t y, const Bitmap2* data, const int64_t expiration) NOTHROWS {

    if (level > std::numeric_limits<jint>::max() ||
        x > std::numeric_limits<jint>::max() ||
        y > std::numeric_limits<jint>::max() ||
        !data)
        return TE_InvalidArg;

    LocalJNIEnv env;

    Java::JNILocalRef bitmapRef(*env, nullptr);
    if (TAKEngineJNI::Interop::Renderer::Interop_marshal(bitmapRef, env, *data) != TE_Ok)
        return TE_Err;

    env->CallVoidMethod(impl, TileContainer_class.setTileID, static_cast<jint>(level), static_cast<jint>(x), static_cast<jint>(y),
        bitmapRef.get(), expiration);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    return TE_Ok;
}

bool ManagedTileContainer::hasTileExpirationMetadata() NOTHROWS {
    LocalJNIEnv env;
    jboolean value = env->CallBooleanMethod(impl, TileContainer_class.hasTileExpirationMetadataID);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
    }
    return value;
}

int64_t ManagedTileContainer::getTileExpiration(const std::size_t level, const std::size_t x, const std::size_t y) NOTHROWS {

    if (level > std::numeric_limits<jint>::max() ||
        x > std::numeric_limits<jint>::max() ||
        y > std::numeric_limits<jint>::max())
        return 0;

    LocalJNIEnv env;
    jlong value = env->CallLongMethod(impl, TileContainer_class.getTileExpirationID, static_cast<jint>(level), static_cast<jint>(x), static_cast<jint>(y));
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
    }
    return value;
}