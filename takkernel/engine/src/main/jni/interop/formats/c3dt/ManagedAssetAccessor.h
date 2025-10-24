#ifndef TAKENGINEJNI_INTEROP_FORMATS_C3DT_MANAGEDASSETACCESSOR_H
#define TAKENGINEJNI_INTEROP_FORMATS_C3DT_MANAGEDASSETACCESSOR_H

#include "common.h"
#include "interop/java/JNILocalRef.h"

#include "util/Error.h"

#include "CesiumAsync/IAssetAccessor.h"

namespace TAKEngineJNI {
    namespace Interop {
        namespace Formats {
            namespace Cesium3DTiles {
                class ManagedAssetAccessor : public CesiumAsync::IAssetAccessor {
                public:
                    ManagedAssetAccessor(JNIEnv &env_, jobject impl) NOTHROWS;
                    ~ManagedAssetAccessor() NOTHROWS;
                    virtual CesiumAsync::Future<std::shared_ptr<CesiumAsync::IAssetRequest>>
                    get(const CesiumAsync::AsyncSystem& asyncSystem,
                        const std::string& url,
                        const std::vector<CesiumAsync::IAssetAccessor::THeader>& headers)
                    override;

                    virtual CesiumAsync::Future<std::shared_ptr<CesiumAsync::IAssetRequest>>
                    request(
                            const CesiumAsync::AsyncSystem& asyncSystem,
                            const std::string& verb,
                            const std::string& url,
                            const std::vector<CesiumAsync::IAssetAccessor::THeader>& headers,
                            const gsl::span<const std::byte>& contentPayload) override;

                    virtual void tick() noexcept override;

                private:
                    jobject mimpl;
                };
            }
        }
    }
}

#endif //TAKENGINEJNI_INTEROP_FORMATS_C3DT_MANAGEDASSETACCESSOR_H
