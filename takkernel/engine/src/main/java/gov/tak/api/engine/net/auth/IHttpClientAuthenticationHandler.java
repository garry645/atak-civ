package gov.tak.api.engine.net.auth;

import gov.tak.api.engine.net.IHttpClientBuilder;

public interface IHttpClientAuthenticationHandler
{
    IHttpClientBuilder configureClient(IHttpClientBuilder builder);
}
