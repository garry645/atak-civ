package gov.tak.platform.client.eud;

public final class EndPoints {
    private final static String authhost = "https://auth.tak.gov";
    private final static String apihost = "https://tak.gov";
    private final static String realm = "TPC";

    public final static String AUTH_SERVER = authhost + "/auth/realms/" + realm + "/protocol/openid-connect";

    public final static String DEVICE = authhost + "/auth/realms/" + realm + "/protocol/openid-connect/auth/device";
    public final static String TOKEN = authhost + "/auth/realms/" + realm + "/protocol/openid-connect/token";
    public final static String MAP_SOURCES = apihost + "/eud_api/map_sources/v1/map_sources.json";
    public final static String PLUGINS = apihost + "/eud_api/software/v1/plugins";

    public final static String client_id = "tak-gov-eud";

    private EndPoints() {}
}
