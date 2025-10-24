#ifndef TAK_ENGINE_FEATURE_LINEMODE_H_INCLUDED
#define TAK_ENGINE_FEATURE_LINEMODE_H_INCLUDED

#include "port/Platform.h"

namespace TAK {
    namespace Engine {
        namespace Feature {
            enum LineMode
            {
                /** Rhumb lines */
                TELM_Rhumb,
                /** Great Circle lines */
                TELM_GreatCircle,
            };
        }
    }
}

#endif //TAK_ENGINE_FEATURE_LINEMODE_H_INCLUDED
