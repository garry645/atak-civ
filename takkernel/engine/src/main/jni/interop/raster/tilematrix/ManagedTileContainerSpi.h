
#ifndef TAKENGINEJNI_INTEROP_RASTER_MANAGEDTILECONTAINERSPI_TILEMATRIX_H
#define TAKENGINEJNI_INTEROP_RASTER_MANAGEDTILECONTAINERSPI_TILEMATRIX_H

#include "common.h"

#include <raster/tilematrix/TileContainerFactory.h>
#include <raster/tilematrix/TileContainer.h>
#include <port/String.h>

namespace TAKEngineJNI {
    namespace Interop {
        namespace Raster {
            namespace TileMatrix {
                class ManagedTileContainerSpi : public TAK::Engine::Raster::TileMatrix::TileContainerSpi {
                public:
                    ManagedTileContainerSpi(JNIEnv& env, jobject impl) NOTHROWS;
                    virtual ~ManagedTileContainerSpi() NOTHROWS;
                    virtual const char *getName() const NOTHROWS;
                    virtual const char *getDefaultExtension() const NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr create(TAK::Engine::Raster::TileMatrix::TileContainerPtr &result, const char *name, const char *path, const TAK::Engine::Raster::TileMatrix::TileMatrix *spec) const NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr open(TAK::Engine::Raster::TileMatrix::TileContainerPtr &result, const char *path, const TAK::Engine::Raster::TileMatrix::TileMatrix *spec, bool readOnly) const NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr isCompatible(bool *result, const TAK::Engine::Raster::TileMatrix::TileMatrix *spec) const NOTHROWS;
                public:
                    jobject impl;
                private:
                    mutable TAK::Engine::Port::String cached_name_;
                    mutable TAK::Engine::Port::String cached_ext_;
                };
            }
        }
    }
}

#endif