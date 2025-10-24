//
// Created by Geo Dev on 5/2/23.
//

#ifndef TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATUREDEFINITION2_H
#define TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATUREDEFINITION2_H

#include <list>

#include <feature/Geometry2.h>
#include <feature/Feature2.h>
#include <feature/FeatureDefinition3.h>

#include "common.h"
#include "interop/JNIStringUTF.h"
#include "interop/JNIByteArray.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Feature {
            class ManagedFeatureDefinition : public TAK::Engine::Feature::FeatureDefinition3
            {
            public :
                ManagedFeatureDefinition(JNIEnv &env, jobject impl) NOTHROWS;
                ~ManagedFeatureDefinition() NOTHROWS;
            public :
                /** clears any cached values */
                void reset() NOTHROWS;
            public : // Java API
                TAK::Engine::Util::TAKErr getTimestamp(int64_t *value) NOTHROWS;
            public : // FeatureDefinition2
                TAK::Engine::Util::TAKErr getRawGeometry(TAK::Engine::Feature::FeatureDefinition2::RawData *value) NOTHROWS;
                TAK::Engine::Feature::FeatureDefinition2::GeometryEncoding getGeomCoding() NOTHROWS;
                TAK::Engine::Util::TAKErr getName(const char **value) NOTHROWS;
                TAK::Engine::Feature::AltitudeMode getAltitudeMode() NOTHROWS;
                double getExtrude() NOTHROWS;
                TAK::Engine::Feature::Traits getTraits() NOTHROWS;
                TAK::Engine::Feature::FeatureDefinition2::StyleEncoding getStyleCoding() NOTHROWS;
                TAK::Engine::Util::TAKErr getRawStyle(TAK::Engine::Feature::FeatureDefinition2::RawData *value) NOTHROWS;
                TAK::Engine::Util::TAKErr getAttributes(const atakmap::util::AttributeSet **value) NOTHROWS;
                TAK::Engine::Util::TAKErr get(const TAK::Engine::Feature::Feature2 **values) NOTHROWS;
            public :
                jobject impl;
            private :
                struct {
                    bool valid {false};
                    struct {
                        TAKEngineJNI::Interop::JNIStringUTF text;
                        jobject managed{nullptr};
                    } ref;
                } name;
                TAK::Engine::Feature::FeaturePtr_const feature;
                struct {
                    bool valid {false};
                    struct {
                        TAKEngineJNI::Interop::JNIByteArray binary;
                        TAKEngineJNI::Interop::JNIStringUTF text;
                        TAK::Engine::Feature::GeometryPtr geom {TAK::Engine::Feature::GeometryPtr(nullptr, nullptr)};
                        jobject managed{nullptr};
                    } ref;
                    TAK::Engine::Feature::FeatureDefinition2::RawData value;
                } rawGeom;
                struct {
                    bool valid {false};
                    struct {
                        TAKEngineJNI::Interop::JNIStringUTF text;
                        atakmap::feature::Style *style{nullptr};
                        jobject managed{nullptr};
                    } ref;
                    TAK::Engine::Feature::FeatureDefinition2::RawData value;
                } rawStyle;
                struct {
                    bool valid {false};
                    atakmap::util::AttributeSet *value {nullptr};
                    jobject managed{nullptr};
                } attributes;
                /** interface version */
                std::size_t version;
            };
        }
    }
}

#endif //TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATUREDEFINITION2_H
