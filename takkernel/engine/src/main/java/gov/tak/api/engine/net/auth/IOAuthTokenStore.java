package gov.tak.api.engine.net.auth;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.annotation.Nullable;

public interface IOAuthTokenStore
{
    /**
     * Retrieves stored token data, or {@code null} it not available
     *
     * @param server
     * @param clientId
     * @return
     */
    OAuthAccessToken.Data getTokenData(@NonNull String server, @Nullable String clientId);
    void setTokenData(@NonNull String server, @Nullable String clientId, @NonNull OAuthAccessToken.Data tokenData);
    void deleteTokenData(@NonNull String server, @Nullable String clientId);
}
