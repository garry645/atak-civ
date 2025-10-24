#include "com_atakmap_map_gdal_GdalLibrary.h"
#include "common.h"

#include <cmath>
#include <memory>
#include <vector>

#include <cpl_conv.h>
#include <cpl_vsi.h>
#include <ogr_srs_api.h>

#include <core/Projection2.h>
#include <core/ProjectionFactory3.h>
#include <util/Memory.h>

#include "interop/JNIByteArray.h"
#include "interop/JNIStringUTF.h"

using namespace TAK::Engine::Core;
using namespace TAK::Engine::Math;
using namespace TAK::Engine::Util;

using namespace TAKEngineJNI;

namespace
{
    typedef std::unique_ptr<void, void (*)(OGRCoordinateTransformationH)> OGRCoordinateTransformationPtr;

    class GdalProjection : public Projection2
    {
    public:
        GdalProjection(OGRSpatialReferenceH srs, const int srid) NOTHROWS;
        ~GdalProjection() NOTHROWS;
    public:
        int getSpatialReferenceID() const NOTHROWS;

        TAKErr forward(Point2<double> *proj, const GeoPoint2 &geo) const NOTHROWS;
        TAKErr inverse(GeoPoint2 *geo, const TAK::Engine::Math::Point2<double> &proj) const NOTHROWS;
        double getMinLatitude() const NOTHROWS;
        double getMaxLatitude() const NOTHROWS;
        double getMinLongitude() const NOTHROWS;
        double getMaxLongitude() const NOTHROWS;

        bool is3D() const NOTHROWS;
    private :
        std::unique_ptr<void, void(*)(OGRCoordinateTransformationH)> forwardImpl;
        std::unique_ptr<void, void(*)(OGRCoordinateTransformationH)> inverseImpl;
        int srid;
    }; // end class Projection

    void OGRCoordinateTransformationH_deleter(const OGRCoordinateTransformationH value)
    {
        OCTDestroyCoordinateTransformation(value);
    }
    void OGRSpatialReferenceH_deleter(const OGRSpatialReferenceH value)
    {
        OSRDestroySpatialReference(value);
    }
}

JNIEXPORT void JNICALL Java_com_atakmap_map_gdal_GdalLibrary_registerProjectionSpi
  (JNIEnv *env, jclass clazz)
{
    class SpiImpl : public ProjectionSpi3
    {
    public :
        TAKErr create(Projection2Ptr &value, const int srid) NOTHROWS
        {
            std::unique_ptr<void, void(*)(OGRSpatialReferenceH)> srs(OSRNewSpatialReference(NULL), OGRSpatialReferenceH_deleter);
            if(!srs.get())
                return TE_Err;
            if(OSRImportFromEPSG(srs.get(), srid) != OGRERR_NONE)
                return TE_InvalidArg;
            value = Projection2Ptr(new GdalProjection(srs.get(), srid), Memory_deleter_const<Projection2, GdalProjection>);
            return TE_Ok;
        }
    };

    ProjectionSpi3Ptr spi(new SpiImpl(), Memory_deleter_const<ProjectionSpi3, SpiImpl>);
    ProjectionFactory3_registerSpi(std::move(spi), 1);
}

JNIEXPORT void JNICALL Java_com_atakmap_map_gdal_GdalLibrary_setThreadLocalConfigOption
  (JNIEnv *env, jclass clazz, jstring mkey, jstring mvalue)
{
    TAK::Engine::Port::String ckey;
    TAKEngineJNI::Interop::JNIStringUTF_get(ckey, *env, mkey);
    TAK::Engine::Port::String cvalue;
    TAKEngineJNI::Interop::JNIStringUTF_get(cvalue, *env, mvalue);
    CPLSetThreadLocalConfigOption(ckey, cvalue);
}

JNIEXPORT jbyteArray JNICALL Java_com_atakmap_map_gdal_GdalLibrary_GetMemFileBuffer
  (JNIEnv *env, jclass clazz, jstring mpath)
{
    TAK::Engine::Port::String cpath;
    Interop::JNIStringUTF_get(cpath, *env, mpath);
    struct VSILFILE_ {
        VSILFILE_(const char *path, const char *mode) : f(VSIFOpenL(path, mode)) {}
        ~VSILFILE_() {VSIFCloseL(f);}
        VSILFILE *f;
    } mb(cpath, "rb");
    if (!mb.f) {
        Logger_log(TELL_Error, "Failed to open memfile %s", cpath.get());
        return nullptr;
    }

    if(VSIFSeekL(mb.f, 0, SEEK_END) != 0) {
        Logger_log(TELL_Error, "Failed to seek memfile to end");
        return nullptr;
    }

    const auto mbLength = VSIFTellL(mb.f);
    if(!mbLength) {
        Logger_log(TELL_Error, "memfile zero length");
        return Interop::JNIByteArray_newByteArray(env, nullptr, 0);
    }
    if(VSIFSeekL(mb.f, 0, SEEK_SET) != 0) {
        Logger_log(TELL_Error, "Failed to rewind memfile");
        return nullptr;
    }

    std::vector<uint8_t> data(mbLength);
    const auto read = VSIFReadL(data.data(), 1, mbLength, mb.f);
    if (read != mbLength) {
        Logger_log(TELL_Error, "Failed to read memfile  fully expected %d read %d", (int)mbLength, (int)read);
        return nullptr;
    }

    return Interop::JNIByteArray_newByteArray(env, reinterpret_cast<const jbyte *>(data.data()), (std::size_t)mbLength);
}

namespace
{
    GdalProjection::GdalProjection(OGRSpatialReferenceH srs, const int srid_) NOTHROWS :
        forwardImpl(NULL, NULL),
        inverseImpl(NULL, NULL),
        srid(srid_)
    {
        std::unique_ptr<void, void(*)(OGRSpatialReferenceH)> wgs84(OSRNewSpatialReference(NULL), OGRSpatialReferenceH_deleter);
        if(!wgs84.get())
            return;
        if(OSRImportFromEPSG(wgs84.get(), 4326) != OGRERR_NONE)
            return;

        forwardImpl = OGRCoordinateTransformationPtr(OCTNewCoordinateTransformation(wgs84.get(), srs), OGRCoordinateTransformationH_deleter);
        inverseImpl = OGRCoordinateTransformationPtr(OCTNewCoordinateTransformation(srs, wgs84.get()), OGRCoordinateTransformationH_deleter);
    }
    GdalProjection::~GdalProjection() NOTHROWS
    {}
    int GdalProjection::getSpatialReferenceID() const NOTHROWS
    {
        return srid;
    }
    TAKErr GdalProjection::forward(Point2<double> *proj, const GeoPoint2 &geo) const NOTHROWS
    {
        if(!forwardImpl.get())
            return TE_IllegalState;
        double x = geo.longitude;
        double y = geo.latitude;
        double z = ::isnan(geo.altitude) ? 0.0 : geo.altitude;
        if(!OCTTransform(forwardImpl.get(), 1, &x, &y, &z))
            return TE_Err;
        proj->x = x;
        proj->y = y;
        proj->z = z;
        return TE_Ok;
    }
    TAKErr GdalProjection::inverse(GeoPoint2 *geo, const TAK::Engine::Math::Point2<double> &proj) const NOTHROWS
    {
        if(!inverseImpl.get())
            return TE_IllegalState;
        double x = proj.x;
        double y = proj.y;
        double z = proj.z;
        if(!OCTTransform(inverseImpl.get(), 1, &x, &y, &z))
            return TE_Err;
        geo->longitude = x;
        geo->latitude = y;
        geo->altitude = z;
        geo->altitudeRef = AltitudeReference::HAE;
        return TE_Ok;
    }
    double GdalProjection::getMinLatitude() const NOTHROWS
    {
        return -90.0;
    }
    double GdalProjection::getMaxLatitude() const NOTHROWS
    {
        return 90.0;
    }
    double GdalProjection::getMinLongitude() const NOTHROWS
    {
        return -180.0;
    }
    double GdalProjection::getMaxLongitude() const NOTHROWS
    {
        return 180.0;
    }
    bool GdalProjection::is3D() const NOTHROWS
    {
        return false;
    }
}
