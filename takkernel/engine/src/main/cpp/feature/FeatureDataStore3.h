#ifndef TAK_ENGINE_FEATURE_FEATUREDATASTORE3_H_INCLUDED
#define TAK_ENGINE_FEATURE_FEATUREDATASTORE3_H_INCLUDED

#include <memory>

#include "feature/FeatureDataStore2.h"
#include "port/Platform.h"
#include "util/Error.h"

namespace TAK {
    namespace Engine {
        namespace Feature {
            class ENGINE_API FeatureCursor3;

            typedef std::unique_ptr<FeatureCursor3, void(*)(const FeatureCursor3 *)> FeatureCursor3Ptr;

            class ENGINE_API FeatureDataStore3 : virtual public FeatureDataStore2
            {
            protected :
                virtual ~FeatureDataStore3() NOTHROWS = 0;
            public :
                virtual Util::TAKErr queryFeatures(FeatureCursor3Ptr &cursor) NOTHROWS = 0;
                virtual Util::TAKErr queryFeatures(FeatureCursor3Ptr &cursor, const FeatureQueryParameters &params) NOTHROWS = 0;
                virtual Util::TAKErr insertFeature(FeaturePtr_const *feature, const int64_t fsid, const char *name, const atakmap::feature::Geometry &geom, const atakmap::feature::Style *style, const atakmap::util::AttributeSet &attributes, const Traits &traits) NOTHROWS = 0;
                virtual Util::TAKErr updateFeature(const int64_t fid, const Traits &traits) NOTHROWS = 0;
            }; // FeatureDataStore3

            typedef std::unique_ptr<FeatureDataStore3, void(*)(const FeatureDataStore3 *)> FeatureDataStore3Ptr;
        }
    }
}

#endif
