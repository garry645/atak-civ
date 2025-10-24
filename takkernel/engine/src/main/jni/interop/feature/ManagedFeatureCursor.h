//
// Created by Geo Dev on 5/4/23.
//

#ifndef TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATURECURSOR_H_INCLUDED
#define TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATURECURSOR_H_INCLUDED

#include <feature/FeatureCursor3.h>

#include "common.h"
#include "interop/feature/ManagedFeatureDefinition.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Feature {
            class ManagedFeatureCursor : public virtual TAK::Engine::Feature::FeatureCursor3
            {
            public :
                ManagedFeatureCursor(JNIEnv &env, jobject impl) NOTHROWS;
                ~ManagedFeatureCursor() NOTHROWS;
            public : // Java API
                TAK::Engine::Util::TAKErr getTimestamp(int64_t *value) NOTHROWS;
            public : // FeatureDefinition2
                TAK::Engine::Util::TAKErr getRawGeometry(TAK::Engine::Feature::FeatureDefinition2::RawData *value) NOTHROWS override;
                TAK::Engine::Feature::FeatureDefinition2::GeometryEncoding getGeomCoding() NOTHROWS override;
                TAK::Engine::Util::TAKErr getName(const char **value) NOTHROWS override;
                TAK::Engine::Feature::AltitudeMode getAltitudeMode() NOTHROWS override;
                double getExtrude() NOTHROWS override;
                TAK::Engine::Feature::Traits getTraits() NOTHROWS override;
                TAK::Engine::Feature::FeatureDefinition2::StyleEncoding getStyleCoding() NOTHROWS override;
                TAK::Engine::Util::TAKErr getRawStyle(TAK::Engine::Feature::FeatureDefinition2::RawData *value) NOTHROWS override;
                TAK::Engine::Util::TAKErr getAttributes(const atakmap::util::AttributeSet **value) NOTHROWS override;
                TAK::Engine::Util::TAKErr get(const TAK::Engine::Feature::Feature2 **values) NOTHROWS override;
            public : // FeatureCursor2
                TAK::Engine::Util::TAKErr getId(int64_t *value) NOTHROWS override;
                TAK::Engine::Util::TAKErr getFeatureSetId(int64_t *value) NOTHROWS override;
                TAK::Engine::Util::TAKErr getVersion(int64_t *value) NOTHROWS override;
            public :
                TAK::Engine::Util::TAKErr moveToNext() NOTHROWS override;
            public :
                ManagedFeatureDefinition cimpl;
                TAK::Engine::Feature::FeaturePtr_const row;
            };
        }
    }
}

#endif //ATAK_MANAGEDFEATURECURSOR_H
