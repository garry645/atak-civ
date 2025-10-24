#include "enrollmentimpl.h"
#include "simplefileioimpl.h"

using namespace TAK::Commo;
using namespace System;


TAK::Commo::EnrollmentIOUpdate::
EnrollmentIOUpdate(EnrollmentStep step,
    int transferId, SimpleFileIOStatus status,
    System::String ^info,
    System::Int64 bytesTransferred,
    System::Int64 totalBytes,
    array<System::Byte> ^privResult,
    array<System::Byte> ^caResult) : 
        SimpleFileIOUpdate(transferId, status, info, 
                            bytesTransferred, totalBytes),
        step(step),
        privResult(privResult),
        caResult(caResult)
        
{
}

TAK::Commo::EnrollmentIOUpdate::~EnrollmentIOUpdate()
{

}

impl::EnrollmentIOImpl::EnrollmentIOImpl(IEnrollmentIO ^io) : atakmap::commoncommo::EnrollmentIO(),
ioCLI(io)
{
}
impl::EnrollmentIOImpl::~EnrollmentIOImpl() {}

void impl::EnrollmentIOImpl::enrollmentUpdate(const atakmap::commoncommo::EnrollmentIOUpdate *update)
{
    String ^infoString = nullptr;
    if (update->additionalInfo)
        infoString = gcnew String(update->additionalInfo, 0, (int)strlen(update->additionalInfo), System::Text::Encoding::UTF8);
    int64_t bt = (int64_t)update->bytesTransferred;
    if (update->bytesTransferred > INT64_MAX)
        bt = INT64_MAX;
    int64_t tbt = (int64_t)update->totalBytesToTransfer;
    if (update->totalBytesToTransfer > INT64_MAX)
        tbt = INT64_MAX;

    array<System::Byte> ^privResultCLI = nullptr;
    array<System::Byte> ^caResultCLI = nullptr;
    if (update->privResult) {
        privResultCLI = gcnew array<System::Byte>(update->privResultLen);
        pin_ptr<System::Byte> pinPriv = &privResultCLI[0];
        memcpy(pinPriv, update->privResult, update->privResultLen);
    }
    if (update->caResult) {
        caResultCLI = gcnew array<System::Byte>(update->caResultLen);
        pin_ptr<System::Byte> caPriv = &caResultCLI[0];
        memcpy(caPriv, update->caResult, update->caResultLen);
    }

    EnrollmentIOUpdate ^up = gcnew EnrollmentIOUpdate(
        nativeToCLI(update->step),
        update->xferid,
        SimpleFileIOImpl::nativeToCLI(update->status),
        infoString,
        bt, tbt, privResultCLI, caResultCLI);

    ioCLI->EnrollmentUpdate(up);
}

EnrollmentStep impl::EnrollmentIOImpl::nativeToCLI(atakmap::commoncommo::EnrollmentStep step)
{
    EnrollmentStep ret;
    switch (step) {
    case atakmap::commoncommo::EnrollmentStep::ENROLL_STEP_KEYGEN:
        ret = EnrollmentStep::Keygen;
        break;
    case atakmap::commoncommo::EnrollmentStep::ENROLL_STEP_CSR:
        ret = EnrollmentStep::Csr;
        break;
    case atakmap::commoncommo::EnrollmentStep::ENROLL_STEP_SIGN:
        ret = EnrollmentStep::Sign;
        break;
    }
    return ret;
}

