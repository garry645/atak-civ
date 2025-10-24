
#ifndef TAKENGINEJNI_INTEROP_RENDERER_INTEROP_H_INCLUDED
#define TAKENGINEJNI_INTEROP_RENDERER_INTEROP_H_INCLUDED

#include <jni.h>
#include "interop/java/JNILocalRef.h"

#include <renderer/Bitmap2.h>
#include <util/Error.h>

namespace TAKEngineJNI {
    namespace Interop {
        namespace Renderer {
			TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Renderer::BitmapPtr& result, JNIEnv* env, jobject android_bitmap) NOTHROWS;
			TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef& result, JNIEnv* env, const TAK::Engine::Renderer::Bitmap2& bitmap) NOTHROWS;
        }
	}
}



#endif