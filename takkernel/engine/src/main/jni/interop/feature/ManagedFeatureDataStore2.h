//
// Created by Geo Dev on 5/2/23.
//

#ifndef TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATUREDDDATASTORE2_H_INCLUDED
#define TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATUREDDDATASTORE2_H_INCLUDED

#include <set>

#include <feature/FeatureDataStore3.h>
#include <thread/RWMutex.h>

#include "common.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Feature {
            class ManagedFeatureDataStore2 : public TAK::Engine::Feature::FeatureDataStore3
            {
            public :
                ManagedFeatureDataStore2(JNIEnv &env, jobject impl) NOTHROWS;
                ~ManagedFeatureDataStore2() NOTHROWS;
            public :
                virtual TAK::Engine::Util::TAKErr addOnDataStoreContentChangedListener(OnDataStoreContentChangedListener *l) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr removeOnDataStoreContentChangedListener(OnDataStoreContentChangedListener *l) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr getFeature(TAK::Engine::Feature::FeaturePtr_const &feature, const int64_t fid) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr queryFeatures(TAK::Engine::Feature::FeatureCursorPtr &cursor) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr queryFeatures(TAK::Engine::Feature::FeatureCursorPtr &cursor, const FeatureQueryParameters &params) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr queryFeaturesCount(int *value) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr queryFeaturesCount(int *value, const FeatureQueryParameters &params) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr getFeatureSet(TAK::Engine::Feature::FeatureSetPtr_const &featureSet, const int64_t featureSetId) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr queryFeatureSets(TAK::Engine::Feature::FeatureSetCursorPtr &cursor) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr queryFeatureSets(TAK::Engine::Feature::FeatureSetCursorPtr &cursor, const FeatureSetQueryParameters &params) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr queryFeatureSetsCount(int *value) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr queryFeatureSetsCount(int *value, const FeatureSetQueryParameters &params) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr getModificationFlags(int *value) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr beginBulkModification() NOTHROWS;
                virtual TAK::Engine::Util::TAKErr endBulkModification(const bool successful) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr isInBulkModification(bool *value) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr insertFeatureSet(TAK::Engine::Feature::FeatureSetPtr_const *featureSet, const char *provider, const char *type, const char *name, const double minResolution, const double maxResolution) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeatureSet(const int64_t fsid, const char *name) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeatureSet(const int64_t fsid, const double minResolution, const double maxResolution) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeatureSet(const int64_t fsid, const char *name, const double minResolution, const double maxResolution) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr deleteFeatureSet(const int64_t fsid) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr deleteAllFeatureSets() NOTHROWS;
                virtual TAK::Engine::Util::TAKErr insertFeature(TAK::Engine::Feature::FeaturePtr_const *feature, const int64_t fsid, const char *name, const atakmap::feature::Geometry &geom, const TAK::Engine::Feature::AltitudeMode altitudeMode, const double extrude, const atakmap::feature::Style *style, const atakmap::util::AttributeSet &attributes) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeature(const int64_t fid, const char *name) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeature(const int64_t fid, const atakmap::feature::Geometry &geom) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeature(const int64_t fid, const atakmap::feature::Geometry &geom, const TAK::Engine::Feature::AltitudeMode altitudeMode, const double extrude) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeature(const int64_t fid, const TAK::Engine::Feature::AltitudeMode altitudeMode, const double extrude) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeature(const int64_t fid, const atakmap::feature::Style *style) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeature(const int64_t fid, const atakmap::util::AttributeSet &attributes) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeature(const int64_t fid, const char *name, const atakmap::feature::Geometry &geom, const atakmap::feature::Style *style, const atakmap::util::AttributeSet &attributes) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr deleteFeature(const int64_t fid) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr deleteAllFeatures(const int64_t fsid) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr getVisibilitySettingsFlags(int *value) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr setFeatureVisible(const int64_t fid, const bool visible) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr setFeaturesVisible(const FeatureQueryParameters &params, const bool visible) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr isFeatureVisible(bool *value, const int64_t fid) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr setFeatureSetVisible(const int64_t setId, const bool visible) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr setFeatureSetsVisible(const FeatureSetQueryParameters &params, const bool visible) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr setFeatureSetsReadOnly(const FeatureSetQueryParameters &paramsRef, const bool readOnly) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr isFeatureSetVisible(bool *value, const int64_t setId) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr setFeatureSetReadOnly(const int64_t fsid, const bool readOnly) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr isFeatureSetReadOnly(bool *value, const int64_t fsid) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr isFeatureReadOnly(bool *value, const int64_t fsid) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr isAvailable(bool *value) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr refresh() NOTHROWS;
                virtual TAK::Engine::Util::TAKErr getUri(TAK::Engine::Port::String &value) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr close() NOTHROWS;

                virtual TAK::Engine::Util::TAKErr queryFeatures(TAK::Engine::Feature::FeatureCursor3Ptr &cursor) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr queryFeatures(TAK::Engine::Feature::FeatureCursor3Ptr &cursor, const FeatureQueryParameters &params) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr
                insertFeature(TAK::Engine::Feature::FeaturePtr_const *feature, const int64_t fsid,
                              const char *name, const atakmap::feature::Geometry &geom,
                              const atakmap::feature::Style *style,
                              const atakmap::util::AttributeSet &attributes,
                              const TAK::Engine::Feature::Traits &traits) NOTHROWS;
                virtual TAK::Engine::Util::TAKErr updateFeature(const int64_t fid, const TAK::Engine::Feature::Traits &traits) NOTHROWS;

            public :
                jobject impl;
            private :
                struct {
                    std::set<TAK::Engine::Feature::FeatureDataStore2::OnDataStoreContentChangedListener *> listeners;
                    TAK::Engine::Thread::RWMutex mutex;
                    jobject managed { nullptr };
                } callbackForwarder;
                /** interface version */
                std::size_t version;
            }; // ManagedFeatureDataStore2
        }
    }
}

#endif //TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATUREDDDATASTORE2_H_INCLUDED
