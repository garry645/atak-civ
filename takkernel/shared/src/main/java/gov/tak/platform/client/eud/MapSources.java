package gov.tak.platform.client.eud;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;

import gov.tak.api.engine.net.HttpClientBuilder;
import gov.tak.api.engine.net.IHttpClient;
import gov.tak.api.engine.net.IHttpClientBuilder;
import gov.tak.api.engine.net.IResponse;
import gov.tak.api.engine.net.auth.OAuthAccessToken;

public final class MapSources {
    public final static class Element {
        public int id;
        public String name;
        public String short_description;
        public String image_src;
        public String file_src;

        public static Element parse(JSONObject obj) {
            try {
                Element element = new Element();
                element.id = obj.optInt("id");
                element.name = obj.optString("name", null);
                element.short_description = obj.optString("short_description", null);
                JSONObject image = obj.optJSONObject("image");
                if(image != null)
                    element.image_src = image.optString("src", null);
                JSONObject file = obj.optJSONObject("file");
                if(file != null)
                    element.file_src = file.optString("src", null);
                return element;
            } catch(Throwable t) {
                return null;
            }
        }
    }

    public Element[] elements;

    public static MapSources get(String url, OAuthAccessToken.Data token) throws IOException {
        IHttpClient client = HttpClientBuilder.newBuilder(url)
                .addHeader("Authorization", "Bearer " + token.access_token)
                .get();
        try (IResponse response = client.execute()) {
            String s = IResponse.getString(response);
            return parse(s);
        }
    }

    public static MapSources get(String url, OAuthAccessToken token) throws IOException {
        IHttpClientBuilder client = HttpClientBuilder.newBuilder(url);
        if(token != null && token.isValid())
            client.addHeader("Authorization", "Bearer " + token.accessToken());
        try (IResponse response = client.get().execute()) {
            String s = IResponse.getString(response);
            return parse(s);
        }
    }

    public static byte[] getFile(OAuthAccessToken.Data token, Element element) throws IOException {
        IHttpClient client = HttpClientBuilder.newBuilder(element.file_src)
                .addHeader("Authorization", "Bearer " + token.access_token)
                .get();
        try (IResponse response = client.execute()) {
            return IResponse.getBytes(response);
        }
    }

    public static byte[] getFile(OAuthAccessToken token, Element element) throws IOException {
        IHttpClientBuilder client = HttpClientBuilder.newBuilder(element.file_src);
        if(token != null && token.isValid())
            client.addHeader("Authorization", "Bearer " + token.accessToken());
        try (IResponse response = client.get().execute()) {
            return IResponse.getBytes(response);
        }
    }

    public static MapSources parse(String response) {
        try {
            MapSources mapSources = new MapSources();
            JSONArray json = new JSONArray(response);
            mapSources.elements = new Element[json.length()];
            for(int i = 0; i < json.length(); i++) {
                mapSources.elements[i] = Element.parse(json.optJSONObject(i));
            }
            return mapSources;
        } catch(Throwable t) {
            return null;
        }
    }
}
