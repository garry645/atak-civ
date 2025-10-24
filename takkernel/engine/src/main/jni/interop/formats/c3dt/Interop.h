#ifndef TAKENGINEJNI_INTEROP_FORMATS_C3DT_INTEROP_H_INCLUDED
#define TAKENGINEJNI_INTEROP_FORMATS_C3DT_INTEROP_H_INCLUDED

#include <jni.h>

#include <util/Error.h>

#include "interop/java/JNILocalRef.h"
#include "formats/c3dt/Model.h"
#include "formats/c3dt/Tile.h"
#include "formats/c3dt/TileExternalContent.h"
#include "formats/c3dt/TileRenderContent.h"
#include "formats/c3dt/Tileset.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Formats {
            namespace Cesium3DTiles {
                // Tileset interop
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mtileset, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::TilesetPtr &&ctileset) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_copy(TAK::Engine::Formats::Cesium3DTiles::TilesetOpenOptions *copts, JNIEnv &env, jobject jopts) NOTHROWS;

                // Tile interop
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mtile, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::TilePtr &&ctile) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mtileArray, JNIEnv &env, gsl::span<Cesium3DTilesSelection::Tile> &ctileSpan) NOTHROWS;

                // ViewUpdateResults interop
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mviewUpdateResult, JNIEnv &env, const Cesium3DTilesSelection::ViewUpdateResult &cviewUpdateResult) NOTHROWS;

                // TileExternalContent interop
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mtileExternalContent, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::TileExternalContentPtr &&ctileExternalContentPtr) NOTHROWS;

                // TileRenderContent interop
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mtileRenderContent, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::TileRenderContentPtr &&ctileRenderContentPtr) NOTHROWS;

                // Model interop
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mmodel, JNIEnv &env, TAK::Engine::Formats::Cesium3DTiles::ModelPtr &&cmodelPtr) NOTHROWS;

                // Volume interop
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mvolume, JNIEnv &env, CesiumGeometry::OrientedBoundingBox &cvolume) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mvolume, JNIEnv &env, CesiumGeospatial::BoundingRegion &cvolume) NOTHROWS;
                TAK::Engine::Util::TAKErr Interop_marshal(Java::JNILocalRef &mvolume, JNIEnv &env, CesiumGeometry::BoundingSphere &cvolume) NOTHROWS;
            }
        }
    }
}

#endif
