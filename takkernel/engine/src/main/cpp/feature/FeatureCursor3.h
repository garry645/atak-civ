#ifndef TAK_ENGINE_FEATURE_FEATURECURSOR3_H_INCLUDED
#define TAK_ENGINE_FEATURE_FEATURECURSOR3_H_INCLUDED

#include "FeatureCursor2.h"
#include "feature/FeatureDefinition3.h"

namespace TAK {
    namespace Engine {
        namespace Feature {
            class ENGINE_API FeatureCursor3 : public virtual FeatureCursor2,
                                   public virtual FeatureDefinition3
            {
            protected :
                virtual ~FeatureCursor3() NOTHROWS = 0;
            }; // FeatureCursor3
        }
    }
}

#endif // TAK_ENGINE_FEATURE_FEATURECURSOR3_H_INCLUDED
