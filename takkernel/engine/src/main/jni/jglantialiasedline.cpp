#include "com_atakmap_map_opengl_GLAntiAliasedLine.h"

#include <core/RenderContext.h>
#include <renderer/feature/GLBatchGeometryShaders.h>

#include "common.h"

using namespace TAK::Engine::Core;
using namespace TAK::Engine::Renderer::Feature;

JNIEXPORT jint JNICALL Java_com_atakmap_map_opengl_GLAntiAliasedLine_getAntiAliasedLinesShader
  (JNIEnv *env, jclass clazz, jlong ctxptr)
{
    auto ctx = JLONG_TO_INTPTR(RenderContext, ctxptr);
    return GLBatchGeometryShaders_getAntiAliasedLinesShader(*ctx).base.handle;
}
