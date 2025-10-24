package gov.tak.api.engine.net;

import org.junit.Assert;
import org.junit.Test;

import gov.tak.test.KernelJniTest;

public class HttpClientBuilderTest extends KernelJniTest {

    @Test
    public void eud_mapsources_endpoint() throws Throwable {
        IHttpClientBuilder client = HttpClientBuilder.newBuilder("https://tak.gov/eud_api/map_sources/v1/map_sources.json");
        try (IResponse response = client.get().execute()) {
            String s = IResponse.getString(response);
            Assert.assertNotNull(s);
        }
    }
}
