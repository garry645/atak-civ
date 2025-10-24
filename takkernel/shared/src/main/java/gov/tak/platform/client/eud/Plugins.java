package gov.tak.platform.client.eud;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import gov.tak.api.engine.net.HttpClientBuilder;
import gov.tak.api.engine.net.IHttpClient;
import gov.tak.api.engine.net.IResponse;
import gov.tak.api.engine.net.auth.OAuthAccessToken;

public final class Plugins {
    public final static class Element {
        public String identifier;
        public String platform;
        public String apk_type;
        public String package_name;
        public String display_name;
        public String version;
        public int revision_code;
        public String apk_url;
        public String icon_url;
        public String description;
        public String apk_hash;
        public int os_requirement;
        public String tak_prerequisite;
        public int apk_size_bytes;

        public static Element parse(JSONObject obj) {
            try {
                Element element = new Element();
                element.identifier = obj.optString("identifier", null);
                element.platform = obj.optString("platform", null);
                element.apk_type = obj.optString("apk_type", null);
                element.package_name = obj.optString("package_name", null);
                element.display_name = obj.optString("display_name", null);
                element.version = obj.optString("version", null);
                element.revision_code = obj.optInt("revision_code", 0);
                element.apk_url = obj.optString("apk_url", null);
                element.icon_url = obj.optString("icon_url", null);
                element.description = obj.optString("description", null);
                element.apk_hash = obj.optString("apk_hash", null);
                element.os_requirement = obj.optInt("os_requirement", -1);
                element.tak_prerequisite = obj.optString("tak_prerequisite", null);
                element.apk_size_bytes = obj.optInt("apk_size_in_bytes", -1);
                return element;
            } catch(Throwable t) {
                return null;
            }
        }
    }

    public Element[] elements;
    
    public static Plugins get(String u, String product, String version, OAuthAccessToken.Data token) throws IOException {
        IHttpClient client = HttpClientBuilder.newBuilder(u)
                .addQueryParameter("product", product)
                .addQueryParameter("product_version", version)
                .addHeader("Authorization", "Bearer " + token.access_token)
                .get();
        try (IResponse response = client.execute()) {
            String s = IResponse.getString(response);
            return parse(s);
        }
    }

    public static Plugins get(String u, String product, String version, OAuthAccessToken token) throws IOException {
        IHttpClient client = HttpClientBuilder.newBuilder(u)
                .addQueryParameter("product", product)
                .addQueryParameter("product_version", version)
                .addHeader("Authorization", "Bearer " + token.accessToken())
                .get();
        try (IResponse response = client.execute()) {
            String s = IResponse.getString(response);
            return parse(s);
        }
    }

    public static Plugins parse(String response) {
        try {
            Plugins plugins = new Plugins();
            JSONArray json = new JSONArray(response);
            plugins.elements = new Element[json.length()];
            for(int i = 0; i < json.length(); i++) {
                plugins.elements[i] = Element.parse(json.optJSONObject(i));
            }
            return plugins;
        } catch(Throwable t) {
            return null;
        }
    }
}
