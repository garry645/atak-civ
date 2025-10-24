#include "gov_tak_api_engine_map_CurrentRenderContext.h"

#include <core/RenderContext.h>

#include "common.h"
#include "interop/JNIFloatArray.h"
#include "interop/JNIIntArray.h"
#include "interop/JNIStringUTF.h"
#include "interop/Pointer.h"
#include "interop/core/Interop.h"

JNIEXPORT void JNICALL Java_gov_tak_api_engine_map_CurrentRenderContext_setCurrentNative
  (JNIEnv *, jclass, jlong ctxPtr)
{
	TAK::Engine::Core::RenderContext *cctx = JLONG_TO_INTPTR(TAK::Engine::Core::RenderContext, ctxPtr);
    TAK::Engine::Core::RenderContext_setCurrent(cctx);
}