#include "com_atakmap_android_maps_graphics_GLBitmapLoader.h"

#include <renderer/AsyncBitmapLoader2.h>
#include <util/ProtocolHandler.h>

#include "common.h"
#include "interop/JNIStringUTF.h"
#include "interop/java/JNILocalRef.h"
#include "interop/util/ManagedDataInput2.h"

using namespace TAK::Engine::Renderer;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI::Interop;

namespace
{
    struct
    {
        jclass id;
        jmethodID openStream;
    } AbstractInputStreamLoaderSpi_class;

    class LoaderSpiProtocolHandler : public ProtocolHandler
    {
    public :
        LoaderSpiProtocolHandler(JNIEnv &env, jobject mloaderSpi) NOTHROWS;
        ~LoaderSpiProtocolHandler() NOTHROWS;
    public :
        TAKErr handleURI(DataInput2Ptr &ctx, const char * uri) NOTHROWS override;
    public :
        jobject impl;
    };
    bool AbstractInputStreamLoaderSpi_init(JNIEnv &env) NOTHROWS;
}

JNIEXPORT jlong JNICALL Java_com_atakmap_android_maps_graphics_GLBitmapLoader_registerProtocolHandler
  (JNIEnv *env, jclass clazz, jstring mscheme, jobject mloaderSpi, jstring mqueueHint)
{
    std::unique_ptr<LoaderSpiProtocolHandler> chandler(new LoaderSpiProtocolHandler(*env, mloaderSpi));

    JNIStringUTF cscheme(*env, mscheme);
    JNIStringUTF cqueueHint(*env, mqueueHint);

    AsyncBitmapLoader2::registerProtocolHandler(cscheme, chandler.get(), cqueueHint);
    return INTPTR_TO_JLONG(chandler.release());
}
JNIEXPORT void JNICALL Java_com_atakmap_android_maps_graphics_GLBitmapLoader_unregisterProtocolHandler
  (JNIEnv *env, jclass clazz, jlong ptr)
{
    std::unique_ptr<LoaderSpiProtocolHandler> chandler(JLONG_TO_INTPTR(LoaderSpiProtocolHandler, ptr));
    AsyncBitmapLoader2::unregisterProtocolHandler(*chandler);
}

namespace
{

    LoaderSpiProtocolHandler::LoaderSpiProtocolHandler(JNIEnv &env_, jobject mloaderSpi_) NOTHROWS :
        impl(env_.NewGlobalRef(mloaderSpi_))
    {
        static bool clinit = AbstractInputStreamLoaderSpi_init(env_);
    }
    LoaderSpiProtocolHandler::~LoaderSpiProtocolHandler() NOTHROWS
    {
        if(impl) {
            LocalJNIEnv env;
            env->DeleteGlobalRef(impl);
            impl = nullptr;
        }
    }
    TAKErr LoaderSpiProtocolHandler::handleURI(DataInput2Ptr &ctx, const char * uri) NOTHROWS
    {
        LocalJNIEnv env;
        Java::JNILocalRef muri(*env, env->NewStringUTF(uri));
        Java::JNILocalRef mstream(*env, env->CallObjectMethod(impl, AbstractInputStreamLoaderSpi_class.openStream, muri.get()));
        if(ATAKMapEngineJNI_ExceptionCheck(*env, true))
            return TE_Err;
        std::unique_ptr<Util::ManagedDataInput2> cstream(new Util::ManagedDataInput2());
        const auto code = cstream->open(*env, mstream);
        if(code != TE_Ok)
            return code;
        ctx = DataInput2Ptr(cstream.release(), Memory_deleter_const<DataInput2, Util::ManagedDataInput2>);
        return TE_Ok;
    }

    bool AbstractInputStreamLoaderSpi_init(JNIEnv &env) NOTHROWS
    {
        AbstractInputStreamLoaderSpi_class.id = ATAKMapEngineJNI_findClass(&env, "com/atakmap/android/maps/graphics/GLBitmapLoader$AbstractInputStreamLoaderSpi");
        AbstractInputStreamLoaderSpi_class.openStream = env.GetMethodID(AbstractInputStreamLoaderSpi_class.id, "openStream", "(Ljava/lang/String;)Ljava/io/InputStream;");
        return true;
    }
}
