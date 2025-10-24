
#ifndef TAKENGINEJNI_INTEROP_RASTER_TILEMATRIX_MANAGEDTILECLIENT_H
#define TAKENGINEJNI_INTEROP_RASTER_TILEMATRIX_MANAGEDTILECLIENT_H

#include "common.h"
#include "interop/raster/tilematrix/ManagedTileMatrix.h"

#include <raster/tilematrix/TileClient.h>

namespace TAKEngineJNI {
    namespace Interop {
        namespace Raster {
            namespace TileMatrix {
                class ManagedTileClient : public ManagedTileMatrix, public TAK::Engine::Raster::TileMatrix::TileClient {
                public:
                    ManagedTileClient(JNIEnv &env, jobject impl) NOTHROWS;
                    virtual ~ManagedTileClient() NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr clearAuthFailed() NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr checkConnectivity() NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr cache(const TAK::Engine::Raster::TileMatrix::CacheRequest &request, std::shared_ptr<TAK::Engine::Raster::TileMatrix::CacheRequestListener> &listener) NOTHROWS;
                    virtual TAK::Engine::Util::TAKErr estimateTilecount(int *count, const TAK::Engine::Raster::TileMatrix::CacheRequest &request) NOTHROWS;

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
