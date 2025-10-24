#ifndef TAKENGINEJNI_INTEROP_FORMATS_C3DT_MANAGEDASSETRESPONSE_H
#define TAKENGINEJNI_INTEROP_FORMATS_C3DT_MANAGEDASSETRESPONSE_H

#include "common.h"

#include "CesiumAsync/IAssetResponse.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Formats {
            namespace Cesium3DTiles {
                class ManagedAssetResponse : public CesiumAsync::IAssetResponse {
                public:
                    ManagedAssetResponse(JNIEnv &env_, jobject impl) NOTHROWS;
                    ~ManagedAssetResponse() NOTHROWS;

                    uint16_t statusCode() const override;
                    std::string contentType() const override;
                    const CesiumAsync::HttpHeaders &headers() const override;
                    gsl::span<const std::byte> data() const override;

                private:
                    jobject mimpl;
                    mutable CesiumAsync::HttpHeaders _headers;
//                    std::shared_ptr<CesiumAsync::IAssetResponse> _response;
                };
            }
        }
    }
}

#endif //TAKENGINEJNI_INTEROP_FORMATS_C3DT_MANAGEDASSETRESPONSE_H
