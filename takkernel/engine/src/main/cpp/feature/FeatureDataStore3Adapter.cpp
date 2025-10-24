#include "feature/FeatureDataStore3Adapter.h"

#include "feature/LegacyAdapters.h"

using namespace TAK::Engine::Feature;

using namespace TAK::Engine::Util;

using namespace atakmap::util;

FeatureDataStore3Adapter::FeatureDataStore3Adapter(FeatureDataStore2Ptr&& impl_) :
       FilterFeatureDataStore2(std::move(impl_))
{}

FeatureDataStore3Adapter::~FeatureDataStore3Adapter() NOTHROWS
{}

TAKErr FeatureDataStore3Adapter::queryFeatures(FeatureCursor3Ptr &cursor) NOTHROWS
{
    FeatureCursorPtr legacy(nullptr, nullptr);
    TAKErr code = impl->queryFeatures(legacy);
    TE_CHECKRETURN_CODE(code)
    code = LegacyAdapters_adapt(cursor, legacy);
    TE_CHECKRETURN_CODE(code)
    return TE_Ok;
}

TAKErr FeatureDataStore3Adapter::queryFeatures(FeatureCursor3Ptr &cursor, const FeatureDataStore2::FeatureQueryParameters &params) NOTHROWS
{
    FeatureCursorPtr legacy(nullptr, nullptr);
    TAKErr code = impl->queryFeatures(legacy, params);
    TE_CHECKRETURN_CODE(code)
    code = LegacyAdapters_adapt(cursor, legacy);
    TE_CHECKRETURN_CODE(code)
    return TE_Ok;
}

TAKErr FeatureDataStore3Adapter::insertFeature(FeaturePtr_const *feature, const int64_t fsid,
                                        const char *name, const atakmap::feature::Geometry &geom,
                                        const atakmap::feature::Style *style,
                                        const AttributeSet &attributes,
                                        const Traits &traits) NOTHROWS
{
    return impl->insertFeature(feature, fsid, name, geom, traits.altitudeMode, traits.extrude, style, attributes);
}

TAKErr FeatureDataStore3Adapter::updateFeature(const int64_t fid, const Traits &traits) NOTHROWS
{
    return impl->updateFeature(fid, traits.altitudeMode, traits.extrude);
}
