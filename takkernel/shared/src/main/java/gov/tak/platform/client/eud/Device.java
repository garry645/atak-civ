package gov.tak.platform.client.eud;

import org.json.JSONObject;

import java.io.IOException;

import gov.tak.api.engine.net.HttpClientBuilder;
import gov.tak.api.engine.net.IHttpClient;
import gov.tak.api.engine.net.IResponse;

public final class Device {
    public String device_code;
    public String user_code;
    public String verification_uri;
    public String verification_uri_complete;
    public int expires_in;
    public int interval;

    public static Device register(String url) throws IOException {
        String content = "client_id=" + EndPoints.client_id + "&scope=openid offline_access email profile";
        IHttpClient client = HttpClientBuilder.newBuilder(url)
                .setBody(content, "application/x-www-form-urlencoded")
                .post();
        try (IResponse response = client.execute()) {
            String s = IResponse.getString(response);
            return Device.parse(s);
        }
    }

    public static Device parse(String response) {
        try {
            JSONObject json = new JSONObject(response);

            Device struct = new Device();
            struct.device_code = json.getString("device_code");
            struct.user_code = json.getString("user_code");
            struct.verification_uri = json.getString("verification_uri");
            struct.verification_uri_complete = json.getString("verification_uri_complete");
            struct.expires_in = json.getInt("expires_in");
            struct.interval = json.getInt("interval");

            return struct;
        } catch(Throwable t) {
            return null;
        }
    }
}
