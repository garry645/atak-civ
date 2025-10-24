#ifndef TAK_ENGINE_FEATURE_FEATUREDEFINITION3_H_INCLUDED
#define TAK_ENGINE_FEATURE_FEATUREDEFINITION3_H_INCLUDED

#include "FeatureDefinition2.h"
#include "feature/Traits.h"

namespace TAK {
    namespace Engine {
        namespace Feature {
            /**
            * The definition of a feature. Feature properties may be recorded as raw,
            * unprocessed data of several well-defined types. Utilization of
            * unprocessed data may yield significant a performance advantage depending
            * on the intended storage.
            */
            class FeatureDefinition3 : public virtual FeatureDefinition2
            {
            public :
                virtual Traits getTraits() NOTHROWS = 0;

            }; // FeatureDefinition3

            typedef std::unique_ptr<FeatureDefinition3, void(*)(const FeatureDefinition3 *)> FeatureDefinition3Ptr;
        }
    }
}

#endif
