//
// Created by Geo Dev on 5/6/23.
//

#ifndef TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATURESETCURSOR_H_INCLUDED
#define TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATURESETCURSOR_H_INCLUDED

#include <feature/FeatureSetCursor2.h>

#include "common.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Feature {
        class ManagedFeatureSetCursor : public TAK::Engine::Feature::FeatureSetCursor2
            {
            public :
                ManagedFeatureSetCursor(JNIEnv &env, jobject impl) NOTHROWS;
                ~ManagedFeatureSetCursor() NOTHROWS;
            public :
                TAK::Engine::Util::TAKErr get(const TAK::Engine::Feature::FeatureSet2 **featureSet) NOTHROWS;
            public :
                TAK::Engine::Util::TAKErr moveToNext() NOTHROWS;
            public :
                jobject impl;
            private :
                TAK::Engine::Feature::FeatureSetPtr row;
            }; // ManagedFeatureSetCursor
        }
    }
}

#endif //TAKENGINEJNI_INTEROP_FEATURE_MANAGEDFEATURESETCURSOR_H_INCLUDED
