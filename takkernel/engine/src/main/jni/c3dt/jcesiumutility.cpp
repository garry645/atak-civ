#ifdef __ANDROID__
#include <android/log.h>
#endif

#include "common.h"

#include "formats/c3dt/CesiumUtility.h"
#include "interop/JNIStringUTF.h"
#include "port/String.h"
#include "util/Error.h"

using namespace TAK::Engine;
using namespace TAK::Engine::Formats::Cesium3DTiles;
using namespace TAK::Engine::Util;
using namespace TAKEngineJNI;

extern "C" JNIEXPORT jstring JNICALL Java_com_atakmap_map_formats_c3dt_CesiumUtility_nativePathToUriPathImpl
        (JNIEnv *env, jclass, jstring mnativepath)
{
    if (!mnativepath) return NULL;
    TAK::Engine::Port::String cnativepath;
    Interop::JNIStringUTF_get(cnativepath, *env, mnativepath);
    Port::String nativePath = cnativepath.get();
    Port::String uriPath;
    TAKErr code = CesiumUtility_nativePathToUriPath(&uriPath, nativePath);
    if(ATAKMapEngineJNI_checkOrThrow(env, code))
        return nullptr;
    return env->NewStringUTF(uriPath.get());
}