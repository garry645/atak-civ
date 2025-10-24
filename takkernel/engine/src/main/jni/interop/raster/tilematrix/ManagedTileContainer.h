
#ifndef TAKENGINEJNI_INTEROP_RASTER_TILEMATRIX_MANAGEDTILECONTAINER_H
#define TAKENGINEJNI_INTEROP_RASTER_TILEMATRIX_MANAGEDTILECONTAINER_H

#include "common.h"
#include "interop/raster/tilematrix/ManagedTileMatrix.h"

#include <raster/tilematrix/TileContainer.h>

namespace TAKEngineJNI {
    namespace Interop {
        namespace Raster {
            namespace TileMatrix {
                class ManagedTileContainer : public ManagedTileMatrix, public TAK::Engine::Raster::TileMatrix::TileContainer {
                public:
                    ManagedTileContainer(JNIEnv &env, jobject impl) NOTHROWS;
                    virtual ~ManagedTileContainer() NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr isReadOnly(bool* value) NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr setTile(const std::size_t level, const std::size_t x, const std::size_t y, const uint8_t* value, const std::size_t len, const int64_t expiration) NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr setTile(const std::size_t level, const std::size_t x, const std::size_t y, const TAK::Engine::Renderer::Bitmap2* data, const int64_t expiration) NOTHROWS;

                    virtual bool hasTileExpirationMetadata() NOTHROWS;
                    virtual int64_t getTileExpiration(const std::size_t level, const std::size_t x, const std::size_t y) NOTHROWS;

                    virtual const char* getName() const NOTHROWS { return ManagedTileMatrix::getName(); }
                    virtual int getSRID() const NOTHROWS { return ManagedTileMatrix::getSRID(); }
                    virtual TAK::Engine::Util::TAKErr getZoomLevel(TAK::Engine::Port::Collection<ZoomLevel>& value) const NOTHROWS { return ManagedTileMatrix::getZoomLevel(value); }
                    virtual double getOriginX() const NOTHROWS { return ManagedTileMatrix::getOriginX(); }
                    virtual double getOriginY() const NOTHROWS { return ManagedTileMatrix::getOriginY(); }
                    virtual TAK::Engine::Util::TAKErr getTile(TAK::Engine::Renderer::BitmapPtr& result, const std::size_t zoom, const std::size_t x, const std::size_t y) NOTHROWS {
                        return ManagedTileMatrix::getTile(result, zoom, x, y);
                    }
                    virtual TAK::Engine::Util::TAKErr getTileData(std::unique_ptr<const uint8_t, void (*)(const uint8_t*)>& value, std::size_t* len,
                                                                  const std::size_t zoom, const std::size_t x, const std::size_t y) NOTHROWS {
                        return ManagedTileMatrix::getTileData(value, len, zoom, x, y);
                    }

                    virtual TAK::Engine::Util::TAKErr getBounds(TAK::Engine::Feature::Envelope2 *value) const NOTHROWS {
                        return ManagedTileMatrix::getBounds(value);
                    }
                };
            }
        }
    }
}

#endif

