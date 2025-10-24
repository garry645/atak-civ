package com.atakmap.map.formats.c3dt;

import java.nio.ByteBuffer;
import java.util.Map;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
class AssetResponse
{
    public AssetResponse(int statusCode, String contentType, Map<String, String> headers, ByteBuffer data)
    {
        this.statusCode = statusCode;
        this.contentType = contentType;
        this.headers = headers;
        this.data = data;
    }

    int statusCode;
    String contentType;
    Map<String, String> headers;
    ByteBuffer data;
}
