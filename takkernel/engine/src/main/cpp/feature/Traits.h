#ifndef TAK_ENGINE_FEATURE_TRAITS_H_INCLUDED
#define TAK_ENGINE_FEATURE_TRAITS_H_INCLUDED

#include "feature/AltitudeMode.h"
#include "feature/LineMode.h"

namespace TAK {
    namespace Engine {
        namespace Feature {
            struct Traits
            {
                /**
                 * The altitude mode of the feature.
                 */
                AltitudeMode altitudeMode;
                /**
                 * The extrusion value of the feature. If 0.0, no extrusion occurs. If less than one, the
                 * geometry is extruded down to the terrain surface. If greater than one, the value
                 * is interpreted as the height of the geometry, in meters, and the geometry is
                 * extruded away from the surface of the earth by the specified number of meters.
                 */
                double extrude;
                /**
                 * The line mode of the feature
                 */
                LineMode lineMode = LineMode::TELM_Rhumb;
            };
        }
    }
}

#endif //TAK_ENGINE_FEATURE_TRAITS_H_INCLUDED