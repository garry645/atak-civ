package com.atakmap.map.layer.raster.tilematrix;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public interface TileClientSpi
{
    @DontObfuscate
    public static class Options
    {
        public final static Options defaults = new Options(1L, 3000L);

        public long dnsLookupTimeout;
        public long connectTimeout;
        public boolean proxy;

        public Options()
        {
            this(defaults.dnsLookupTimeout, defaults.connectTimeout);
        }

        private Options(long dnsLookupTimeout, long connectTimeout)
        {
            this.dnsLookupTimeout = dnsLookupTimeout;
            this.connectTimeout = connectTimeout;
            this.proxy = true;
        }
    }

    public String getName();

    public TileClient create(String path, String offlineCachePath, Options opts);

    public int getPriority();
}
