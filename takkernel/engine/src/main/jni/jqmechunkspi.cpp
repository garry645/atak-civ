#include "com_atakmap_map_formats_quantizedmesh_QMENative.h"

#include <elevation/ElevationData.h>
#include <feature/Polygon2.h>
#include <formats/quantizedmesh/impl/QMElevationSampler.h>
#include <formats/quantizedmesh/impl/TerrainData.h>
#include <raster/osm/OSMUtils.h>

#include "common.h"
#include "interop/JNIByteArray.h"
#include "interop/JNIStringUTF.h"
#include "interop/Pointer.h"

using namespace TAK::Engine::Elevation;
using namespace TAK::Engine::Feature;
using namespace TAK::Engine::Formats::QuantizedMesh::Impl;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI;

JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_quantizedmesh_QMENative_createFromByteArray
  (JNIEnv *env, jclass clazz, jbyteArray marr, jint off, jint len, jint level, jint srid, jstring muri, jstring mtype)
{
    Interop::JNIByteArray arr(*env, marr, 0);
    return Java_com_atakmap_map_formats_quantizedmesh_QMENative_createFromPointer(
            env,
            clazz,
            INTPTR_TO_JLONG(arr.get<uint8_t>()),
            off,
            len,
            level,
            srid,
            muri,
            mtype);
}
JNIEXPORT jobject JNICALL Java_com_atakmap_map_formats_quantizedmesh_QMENative_createFromPointer
  (JNIEnv *env, jclass clazz, jlong bufptr, jint off, jint len, jint level, jint srid, jstring muri, jstring mtype)
{
    const auto buf = JLONG_TO_INTPTR(uint8_t, bufptr);
    TerrainDataPtr data(nullptr, nullptr);
    if(TerrainData_deserialize(data, buf+off, len, level, srid) != TE_Ok)
        return nullptr;

    Geometry2Ptr_const bounds(nullptr, nullptr);
    if(Polygon2_fromEnvelope(bounds, data->getBounds()) != TE_Ok)
        return nullptr;

    TAK::Engine::Port::String curi;
    Interop::JNIStringUTF_get(curi, *env, muri);
    TAK::Engine::Port::String ctype;
    Interop::JNIStringUTF_get(ctype, *env, mtype);

    SamplerPtr sampler(new QMElevationSampler(std::move(data)), Memory_deleter_const<Sampler, QMElevationSampler>);
    ElevationChunkPtr chunk(nullptr, nullptr);
    if(ElevationChunkFactory_create(
            chunk,
            ctype,
            curi,
            ElevationData::MODEL_TERRAIN,
            atakmap::raster::osm::OSMUtils::mapnikTileResolution(level),
            static_cast<const Polygon2 &>(*bounds),
            NAN, NAN,
            false,
            std::move(sampler)) != TE_Ok) {

        return nullptr;
    }
    return Interop::NewPointer(env, std::move(chunk));
}
