#ifndef TAKENGINEJNI_INTEROP_RASTER_TILEMATRIX_INTEROP_H_INCLUDED
#define TAKENGINEJNI_INTEROP_RASTER_TILEMATRIX_INTEROP_H_INCLUDED

#include <memory>

#include <jni.h>

#include <raster/tilematrix/TileMatrix.h>
#include <raster/tilematrix/TileClient.h>
#include <raster/tilematrix/TileClientFactory.h>
#include <raster/tilematrix/TileContainer.h>
#include <raster/tilematrix/TileContainerFactory.h>

#include "interop/java/JNILocalRef.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Raster {
            namespace TileMatrix {
                template <typename T> TAK::Engine::Util::TAKErr Interop_isWrapper(bool *value, JNIEnv &, jobject mlayer) NOTHROWS;

                template<> TAK::Engine::Util::TAKErr Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileClientSpi>(bool *value, JNIEnv &, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_isWrapper(bool *value, JNIEnv &, const TAK::Engine::Raster::TileMatrix::TileClientSpi &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClientSpi> &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Raster::TileMatrix::TileClientSpiPtr &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClientSpi> &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileClientSpi &cspi) NOTHROWS;

                template<> TAK::Engine::Util::TAKErr Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileContainerSpi>(bool *value, JNIEnv &, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_isWrapper(bool *value, JNIEnv &, const TAK::Engine::Raster::TileMatrix::TileContainerSpi &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainerSpi> &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Raster::TileMatrix::TileContainerSpiPtr &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainerSpi> &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileContainerSpi &cspi) NOTHROWS;

                // TileMatrix
                template<> TAK::Engine::Util::TAKErr Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileMatrix>(bool *value, JNIEnv &, jobject mtmatrix) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_isWrapper(bool *value, JNIEnv &, const TAK::Engine::Raster::TileMatrix::TileMatrix &ctmatrix) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileMatrix> &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Raster::TileMatrix::TileMatrixPtr &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileMatrix> &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileMatrix &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, TAK::Engine::Raster::TileMatrix::TileMatrixPtr &&tmptr) NOTHROWS;

                // TileClient
                template<> TAK::Engine::Util::TAKErr Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileClient>(bool *value, JNIEnv &, jobject mtmatrix) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_isWrapper(bool *value, JNIEnv &, const TAK::Engine::Raster::TileMatrix::TileClient &ctmatrix) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClient> &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Raster::TileMatrix::TileClientPtr &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileClient> &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileClient &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, TAK::Engine::Raster::TileMatrix::TileClientPtr &&tmptr) NOTHROWS;

                // TileContainer
                template<> TAK::Engine::Util::TAKErr Interop_isWrapper<TAK::Engine::Raster::TileMatrix::TileContainer>(bool *value, JNIEnv &, jobject mtmatrix) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_isWrapper(bool *value, JNIEnv &, const TAK::Engine::Raster::TileMatrix::TileContainer &ctmatrix) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainer> &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Raster::TileMatrix::TileContainerPtr &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::TileContainer> &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::TileContainer &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, TAK::Engine::Raster::TileMatrix::TileContainerPtr &&tmptr) NOTHROWS;

                // CacheRequestListener
                template<> TAK::Engine::Util::TAKErr Interop_isWrapper<TAK::Engine::Raster::TileMatrix::CacheRequestListener>(bool *value, JNIEnv &, jobject mtmatrix) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_isWrapper(bool *value, JNIEnv &, const TAK::Engine::Raster::TileMatrix::CacheRequestListener &ctmatrix) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(std::shared_ptr<TAK::Engine::Raster::TileMatrix::CacheRequestListener> &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(TAK::Engine::Raster::TileMatrix::CacheRequestListenerPtr &value, JNIEnv &env, jobject mspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const std::shared_ptr<TAK::Engine::Raster::TileMatrix::CacheRequestListener> &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, const TAK::Engine::Raster::TileMatrix::CacheRequestListener &cspi) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &value, JNIEnv &env, TAK::Engine::Raster::TileMatrix::CacheRequestListenerPtr &&tmptr) NOTHROWS;
            }
        }
    }
}

#endif
