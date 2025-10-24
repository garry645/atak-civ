
#ifndef TAKENGINEJNI_INTEROP_RASTER_TILEMATRIX_TILEMATRIX_H
#define TAKENGINEJNI_INTEROP_RASTER_TILEMATRIX_TILEMATRIX_H

#include "common.h"

#include <raster/tilematrix/TileMatrix.h>

namespace TAKEngineJNI {
    namespace Interop {
        namespace Raster {
            namespace TileMatrix {
                class ManagedTileMatrix : public TAK::Engine::Raster::TileMatrix::TileMatrix {
                public:
                    ManagedTileMatrix(JNIEnv &env, jobject impl) NOTHROWS;
                    virtual ~ManagedTileMatrix() NOTHROWS;
                    virtual const char* getName() const NOTHROWS;
                    virtual int getSRID() const NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr getZoomLevel(TAK::Engine::Port::Collection<ZoomLevel>& value) const NOTHROWS;
                    virtual double getOriginX() const NOTHROWS;
                    virtual double getOriginY() const NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr getTile(TAK::Engine::Renderer::BitmapPtr& result, const std::size_t zoom, const std::size_t x, const std::size_t y) NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr getTileData(std::unique_ptr<const uint8_t, void (*)(const uint8_t*)>& value, std::size_t* len,
                                                     const std::size_t zoom, const std::size_t x, const std::size_t y) NOTHROWS;

                    virtual TAK::Engine::Util::TAKErr getBounds(TAK::Engine::Feature::Envelope2 *value) const NOTHROWS;

                public:
                    jobject impl;
                private:
                    mutable TAK::Engine::Port::String cached_name_;
                };

                TAK::Engine::Util::TAKErr Interop_get(TAK::Engine::Raster::TileMatrix::TileMatrix::ZoomLevel& result, JNIEnv& env, jobject object) NOTHROWS;
            }
        }
    }
}
#endif
