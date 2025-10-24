
#ifndef TAKENGINEJNI_INTEROP_RASTER_MANAGEDTILECLIENTSPI_TILEMATRIX_H
#define TAKENGINEJNI_INTEROP_RASTER_MANAGEDTILECLIENTSPI_TILEMATRIX_H

#include "common.h"

#include <raster/tilematrix/TileClientFactory.h>
#include <raster/tilematrix/TileClient.h>
#include <port/String.h>

namespace TAKEngineJNI {
    namespace Interop {
        namespace Raster {
            namespace TileMatrix {
                class ManagedTileClientSpi : public TAK::Engine::Raster::TileMatrix::TileClientSpi {
                public:
                    ManagedTileClientSpi(JNIEnv& env, jobject impl) NOTHROWS;
                    virtual ~ManagedTileClientSpi() NOTHROWS;

                    virtual const char *getName() const NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr create(TAK::Engine::Raster::TileMatrix::TileClientPtr &result, const char *path, const char *offlineCachePath, const Options *opts) const NOTHROWS;
                    virtual int getPriority() const NOTHROWS;
                public:
                    jobject impl;
                private:
                    mutable TAK::Engine::Port::String cached_name_;
                };
            }
        }
    }
}

#endif