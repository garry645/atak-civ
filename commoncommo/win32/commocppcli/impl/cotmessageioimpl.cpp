#include "cotmessageioimpl.h"

using namespace TAK::Commo::impl;

CoTMessageListenerImpl::CoTMessageListenerImpl(TAK::Commo::ICoTMessageListener ^listener) : CoTMessageListener()
{
    cotlistenerCLI = listener;
}

CoTMessageListenerImpl::~CoTMessageListenerImpl()
{

}

void CoTMessageListenerImpl::cotMessageReceived(const char *cotMessage, const char *rxEndpointId)
{
    cotlistenerCLI->CotMessageReceived(gcnew System::String(cotMessage, 0, strlen(cotMessage), System::Text::Encoding::UTF8), rxEndpointId ? gcnew System::String(rxEndpointId) : nullptr);
}

GenericDataListenerImpl::GenericDataListenerImpl(TAK::Commo::IGenericDataListener ^listener) : GenericDataListener()
{
    genericListenerCLI = listener;
}

GenericDataListenerImpl::~GenericDataListenerImpl()
{

}

void GenericDataListenerImpl::genericDataReceived(const uint8_t *data, size_t dataLen, const char *rxEndpointId)
{
    array<System::Byte> ^cliData = gcnew array<System::Byte>(dataLen);
    {
        pin_ptr<System::Byte> pinData = &cliData[0];
        uint8_t *nativeData = (uint8_t *)pinData;
        memcpy(pinData, data, dataLen);
    }

    genericListenerCLI->GenericDataReceived(cliData, rxEndpointId ? gcnew System::String(rxEndpointId) : nullptr);
}

CoTSendFailureListenerImpl::CoTSendFailureListenerImpl(TAK::Commo::ICoTSendFailureListener ^listener) : CoTSendFailureListener()
{
    cotlistenerCLI = listener;
}

CoTSendFailureListenerImpl::~CoTSendFailureListenerImpl()
{

}

void CoTSendFailureListenerImpl::sendCoTFailure(const char *host, int port, const char *errorReason)
{
    cotlistenerCLI->SendCoTFailure(gcnew System::String(host), port, gcnew System::String(errorReason));
}



CoTDetailExtenderImpl::CoTDetailExtenderImpl(TAK::Commo::ICoTDetailExtender ^extender) : CoTDetailExtender()
{
    extenderCLI = extender;
}

CoTDetailExtenderImpl::~CoTDetailExtenderImpl()
{

}

atakmap::commoncommo::CommoResult CoTDetailExtenderImpl::encode(uint8_t **encodedExtension, size_t *encodedSize, const char *cotDetailElement)
{
    array<System::Byte> ^encodedCLI = nullptr;
    try {
        System::String ^detailCLI = gcnew System::String(cotDetailElement, 0, strlen(cotDetailElement), System::Text::Encoding::UTF8);
        encodedCLI = extenderCLI->Encode(detailCLI);
    } catch (...) {
    }
    if (encodedCLI == nullptr)
        return atakmap::commoncommo::COMMO_ILLEGAL_ARGUMENT;
    uint8_t *encoded = new uint8_t[encodedCLI->Length];
    pin_ptr<System::Byte> pinEncodedCLI = &encodedCLI[0];
    memcpy(encoded, (uint8_t *)pinEncodedCLI, encodedCLI->Length);

    *encodedExtension = encoded;
    *encodedSize = encodedCLI->Length;
    return atakmap::commoncommo::COMMO_SUCCESS;
}

atakmap::commoncommo::CommoResult CoTDetailExtenderImpl::decode(const char **cotDetailElement, const uint8_t *encodedExtension, size_t encodedSize)
{
    char *detail = NULL;
    try {
        array<System::Byte> ^cliEncoded = gcnew array<System::Byte>(encodedSize);
        {
            pin_ptr<System::Byte> pinData = &cliEncoded[0];
            memcpy(pinData, encodedExtension, encodedSize);
        }
        System::String ^detailCLI = extenderCLI->Decode(cliEncoded);
        if (detailCLI == nullptr)
            return atakmap::commoncommo::COMMO_ILLEGAL_ARGUMENT;

        array<System::Byte> ^detailBytes = System::Text::Encoding::UTF8->GetBytes(detailCLI);
        detail = new char[detailBytes->Length + 1];
        System::Runtime::InteropServices::Marshal::Copy(detailBytes, 0, System::IntPtr(detail), detailBytes->Length);
        detail[detailBytes->Length] = 0;
    } catch (...) {
        delete[] detail;
        return atakmap::commoncommo::COMMO_ILLEGAL_ARGUMENT;
    }

    *cotDetailElement = detail;
    return atakmap::commoncommo::COMMO_SUCCESS;
}

void CoTDetailExtenderImpl::releaseDecodeResult(const char *resultBuffer)
{
    delete[] resultBuffer;
}

void CoTDetailExtenderImpl::releaseEncodeResult(const uint8_t *resultBuffer)
{
    delete[] resultBuffer;
}