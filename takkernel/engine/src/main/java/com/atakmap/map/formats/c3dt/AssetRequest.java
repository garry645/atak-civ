package com.atakmap.map.formats.c3dt;

import java.util.Map;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
class AssetRequest
{
    public AssetRequest(String method, String url, Map<String, String> headers, AssetResponse response)
    {
        this.method = method;
        this.url = url;
        this.headers = headers;
        this.response = response;
    }

    public String method;
    public String url;
    public Map<String, String> headers;
    public AssetResponse response;
}
