package gov.tak.api.engine.net;

import java.io.InputStream;

import gov.tak.api.annotation.NonNull;

public interface IHttpClientBuilder
{
    IHttpClientBuilder addQueryParameter(@NonNull String key, @NonNull String value);
    IHttpClientBuilder addHeader(String key, String value);
    IHttpClientBuilder setBody(String body, String mime);
    IHttpClientBuilder setBody(byte[] body, String mime);
    IHttpClientBuilder setBody(InputStream body, String mime);

    IHttpClient post();
    IHttpClient get();
}
