#ifndef TAKENGINEJNI_INTEROP_FORMATS_C3DT_MANAGEDASSETREQUEST_H
#define TAKENGINEJNI_INTEROP_FORMATS_C3DT_MANAGEDASSETREQUEST_H

#include "common.h"

#include "CesiumAsync/IAssetRequest.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Formats {
            namespace Cesium3DTiles {
                class ManagedAssetRequest : public CesiumAsync::IAssetRequest {
                public:
                    ManagedAssetRequest(JNIEnv &env_, jobject impl) NOTHROWS;
                    ~ManagedAssetRequest() NOTHROWS;

                    const std::string &method() const override;
                    const std::string &url() const override;
                    const CesiumAsync::HttpHeaders &headers() const override;
                    const CesiumAsync::IAssetResponse *response() const override;

                private:
                    jobject mimpl;
                    mutable std::string _method;
                    mutable std::string _url;
                    mutable CesiumAsync::HttpHeaders _headers;
                    mutable std::shared_ptr<CesiumAsync::IAssetResponse> _response;
                };
            }
        }
    }
}


#endif //TAKENGINEJNI_INTEROP_FORMATS_C3DT_MANAGEDASSETREQUEST_H
