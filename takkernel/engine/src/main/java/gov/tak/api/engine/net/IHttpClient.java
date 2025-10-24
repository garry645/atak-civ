package gov.tak.api.engine.net;

import java.io.IOException;
import java.lang.annotation.Native;

public interface IHttpClient {
    /** {@code true} to follow redirects */
    @Native
    int OPTION_FOLLOW_REDIRECTS = 1;
    /** read timeout, in milliseconds */
    @Native
    int OPTION_READ_TIMEOUT = 2;
    /** connect timeout, in milliseconds */
    @Native
    int OPTION_CONNECT_TIMEOUT = 3;
    /** {@code true} to disable SSL peer verification */
    @Native
    int OPTION_DISABLE_SSL_PEER_VERIFY = 4;

    /**
     * Executes the request configured for this client.
     *
     * @return  The response for the request
     * @throws IOException
     */
    IResponse execute() throws IOException;

    /**
     * Sets the value for the specified option on this client. If the client does not recognize the
     * option the method shall return without raising an exception. Implementors are encouraged to
     * log a warning.
     *
     * @param opt   The option name
     * @param value The value for the option, as an integer
     */
    void setOption(int opt, int value);

    /**
     * Sets the value for the specified option on this client. If the client does not recognize the
     * option the method shall return without raising an exception. Implementors are encouraged to
     * log a warning.
     *
     * @param opt   The option name
     * @param value The value for the option, as a boolean
     */
    void setOption(int opt, boolean value);

    /**
     * Sets the value for the specified option on this client. If the client does not recognize the
     * option the method shall return without raising an exception. Implementors are encouraged to
     * log a warning.
     *
     * @param opt   The option name
     * @param value The value for the option, as a {@code String}
     */
    void setOption(int opt, String value);
}
