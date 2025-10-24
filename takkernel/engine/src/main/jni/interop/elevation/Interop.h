#ifndef TAKENGINEJNI_INTEROP_ELEVATION_INTEROP_H_INCLUDED
#define TAKENGINEJNI_INTEROP_ELEVATION_INTEROP_H_INCLUDED

#include <jni.h>

#include <elevation/ElevationManager.h>
#include <elevation/ElevationSource.h>
#include <port/Platform.h>
#include <util/Error.h>

#include "interop/java/JNILocalRef.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Elevation {
            // general template declaration for Java-wraps-native
            template<class T>
            bool Interop_isWrapper(JNIEnv *env, jobject msource);

            template<class T>
            TAK::Engine::Util::TAKErr Interop_isWrapper(bool *value, JNIEnv &, jobject ml) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_adapt(std::shared_ptr<TAK::Engine::Elevation::ElevationSource> &value, JNIEnv *env, jobject msource, const bool forceWrap) NOTHROWS;
            jobject Interop_adapt(JNIEnv *env, const std::shared_ptr<TAK::Engine::Elevation::ElevationSource> &csource, const bool forceWrap) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_getObject(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Elevation::ElevationSource &source) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Elevation::ElevationSource> &csource, jobject mowner = nullptr, const bool forceWrap = false) NOTHROWS;

            // template specialization for Java-wraps-native
            template<>
            bool Interop_isWrapper<TAK::Engine::Elevation::ElevationSource>(JNIEnv *env, jobject msource);

            bool Interop_isWrapper(const TAK::Engine::Elevation::ElevationSource &csource);

            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &maltMode, JNIEnv &env, const TAK::Engine::Elevation::HeightmapStrategy &cstrategy) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Elevation::HeightmapStrategy &caltMode, JNIEnv &env, jobject mstrategy) NOTHROWS;

            TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Elevation::HeightmapParams &cparams) NOTHROWS;
            TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Elevation::HeightmapParams *value, JNIEnv &env, jobject mparams) NOTHROWS;
        }
    }
}

#endif
