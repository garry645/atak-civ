
#include "interop/renderer/Interop.h"
#include "interop/java/JNIEnum.h"
#include "interop/JNIIntArray.h"
#include "common.h"

#include <util/IO2.h> // endian
#include <limits> // numeric_limits
#include <cstring> // memcpy

using namespace TAK::Engine::Util;
using namespace TAKEngineJNI::Interop::Renderer;
using namespace TAK::Engine::Renderer;
using namespace TAKEngineJNI::Interop;

namespace {
    struct {
        jclass id;
        jmethodID getPixels;
        jmethodID setPixels;
        jmethodID recycle;
        jmethodID createBitmap;
        jmethodID getWidth;
        jmethodID getHeight;

        struct {
            jobject ARGB_8888;
            jobject RGB_565;
            jobject ALPHA_8;
        } Config_enum;
    } Bitmap_class;

    bool Bitmap_class_init(JNIEnv &env) NOTHROWS {

        Bitmap_class.id = ATAKMapEngineJNI_findClass(&env, "android/graphics/Bitmap");
        Bitmap_class.getPixels = env.GetMethodID(Bitmap_class.id, "getPixels", "([IIIIIII)V");
        Bitmap_class.setPixels = env.GetMethodID(Bitmap_class.id, "setPixels", "([IIIIIII)V");
        Bitmap_class.recycle = env.GetMethodID(Bitmap_class.id, "recycle", "()V");
        Bitmap_class.createBitmap = env.GetStaticMethodID(Bitmap_class.id, "createBitmap", "(IILandroid/graphics/Bitmap$Config;)Landroid/graphics/Bitmap;");
        Bitmap_class.getWidth = env.GetMethodID(Bitmap_class.id, "getWidth", "()I");
        Bitmap_class.getHeight = env.GetMethodID(Bitmap_class.id, "getHeight", "()I");

        {
            Java::JNILocalRef e(env, nullptr);
            if(Java::JNIEnum_value(e, env, "android/graphics/Bitmap$Config", "ARGB_8888") != TE_Ok)
                return false;
            Bitmap_class.Config_enum.ARGB_8888 = env.NewWeakGlobalRef(e);
            if(Java::JNIEnum_value(e, env, "android/graphics/Bitmap$Config", "RGB_565") != TE_Ok)
                return false;
            Bitmap_class.Config_enum.RGB_565 = env.NewWeakGlobalRef(e);
            if(Java::JNIEnum_value(e, env, "android/graphics/Bitmap$Config", "ALPHA_8") != TE_Ok)
                return false;
            Bitmap_class.Config_enum.ALPHA_8 = env.NewWeakGlobalRef(e);
        }
        return true;
    }

    bool checkInit(JNIEnv& env) {
        static bool v = Bitmap_class_init(env);
        return v;
    }
}

TAKErr TAKEngineJNI::Interop::Renderer::Interop_marshal(TAK::Engine::Renderer::BitmapPtr& result, JNIEnv* env, jobject android_bitmap) NOTHROWS {

    if (!checkInit(*env))
        return TE_Err;

    // allow for null bitmap as empty native pointer
    if (!android_bitmap) {
        result = TAK::Engine::Renderer::BitmapPtr(nullptr, nullptr);
        return TE_Ok;
    }

    jint width = env->CallIntMethod(android_bitmap, Bitmap_class.getWidth);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    jint height = env->CallIntMethod(android_bitmap, Bitmap_class.getHeight);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    Java::JNILocalRef margbref(*env, env->NewIntArray(width*height));
    env->CallVoidMethod(android_bitmap, Bitmap_class.getPixels, margbref.get(), 0, width, 0, 0, width, height);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    // create native bitmap
    if(TE_PlatformEndian == TE_LittleEndian)
        result = BitmapPtr(new(std::nothrow) Bitmap2(width, height, Bitmap2::BGRA32), Memory_deleter_const<Bitmap2>);
    else
        result = BitmapPtr(new(std::nothrow) Bitmap2(width, height, Bitmap2::ARGB32), Memory_deleter_const<Bitmap2>);

    if(!result)
        return TE_OutOfMemory;

    JNIIntArray margb(*env, (jintArray)margbref.get(), JNI_ABORT);
    memcpy(result->getData(), margb.get<const uint8_t>(), (width * height) * sizeof(jint));

    return TE_Ok;
}

TAKErr TAKEngineJNI::Interop::Renderer::Interop_marshal(Java::JNILocalRef& resultOut, JNIEnv* env, const TAK::Engine::Renderer::Bitmap2& bitmap) NOTHROWS {
    if (!checkInit(*env))
        return TE_Err;

    Bitmap2::Format convertFormat = TE_PlatformEndian == TE_LittleEndian ?
                                    Bitmap2::BGRA32 : Bitmap2::ARGB32;

    if (bitmap.getWidth() > std::numeric_limits<jint>::max() ||
        bitmap.getHeight() > std::numeric_limits<jint>::max()) {
        ATAKMapEngineJNI_checkOrThrow(env, TE_InvalidArg);
        return TE_InvalidArg;
    }

    jint width = static_cast<jint>(bitmap.getWidth());
    jint height = static_cast<jint>(bitmap.getHeight());

    Java::JNILocalRef result(*env, env->CallStaticObjectMethod(Bitmap_class.id, Bitmap_class.createBitmap,
                                                               width,
                                                               height,
                                                               Bitmap_class.Config_enum.ARGB_8888));

    Java::JNILocalRef pixels(*env, env->NewIntArray(width * height));

    {
        JNIIntArray margb(*env, (jintArray)pixels.get(), 0);
        if (bitmap.getFormat() != convertFormat || bitmap.getStride() != bitmap.getWidth()) {
            Bitmap2 tmp(bitmap, bitmap.getWidth(), bitmap.getHeight(), convertFormat);
            memcpy(margb.get<uint8_t>(), tmp.getData(), (width * height) * sizeof(jint));
        } else {
            memcpy(margb.get<uint8_t>(), bitmap.getData(), (width * height) * sizeof(jint));
        }
    }

    jint offset = 0;
    jint stride = width;
    jint x = 0;
    jint y = 0;
    env->CallVoidMethod(result.get(), Bitmap_class.setPixels, pixels.get(), offset, stride, x, y, width, height);
    if(env->ExceptionCheck()) {
        env->ExceptionClear();
        return TE_Err;
    }

    resultOut = std::move(result);

    return TE_Ok;
}