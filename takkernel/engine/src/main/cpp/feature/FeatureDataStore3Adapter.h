#ifndef TAK_ENGINE_FEATURE_FEATUREDATASTORE3ADAPTER_H_INCLUDED
#define TAK_ENGINE_FEATURE_FEATUREDATASTORE3ADAPTER_H_INCLUDED

#include "feature/FeatureDataStore3.h"
#include "feature/FilterFeatureDataStore2.h"

#ifdef _MSC_VER
#pragma warning(push)
#pragma warning(disable : 4250)
#endif

namespace TAK {
    namespace Engine {
        namespace Feature {
            class ENGINE_API FeatureDataStore3Adapter : public FilterFeatureDataStore2, public FeatureDataStore3
            {
            public :
                FeatureDataStore3Adapter(FeatureDataStore2Ptr&& impl);
                ~FeatureDataStore3Adapter() NOTHROWS override;
            public:
                Util::TAKErr queryFeatures(FeatureCursor3Ptr &cursor) NOTHROWS override;
                Util::TAKErr queryFeatures(FeatureCursor3Ptr &cursor, const FeatureQueryParameters &params) NOTHROWS override;
                Util::TAKErr insertFeature(FeaturePtr_const *feature, const int64_t fsid, const char *name,
                              const atakmap::feature::Geometry &geom,
                              const atakmap::feature::Style *style,
                              const atakmap::util::AttributeSet &attributes,
                              const Traits &traits) NOTHROWS override;
                Util::TAKErr updateFeature(const int64_t fid, const Traits &traits) NOTHROWS override;
            };
        }
    }
}

#ifdef _MSC_VER
#pragma warning(pop)
#endif

#endif //TAK_ENGINE_FEATURE_FEATUREDATASTORE3ADAPTER_H_INCLUDED
