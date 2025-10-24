
package com.atakmap.comms;

/**
 * The Factory to obtain a Comms Provider
 */
public class CommsProviderFactory {
    /**
     * The Class name
     */
    private static final String TAG = "CommsProviderFactory";

    /**
     * The default Comms Provider
     */
    private static final CommsProvider DEFAULT = new DefaultCommsProvider();

    /**
     * State of the current CommsProviderFactory
     */
    private static CommsProvider commsProvider = DEFAULT;
    private static boolean markedAsDefault = true;
    private static boolean registered = false;

    /**
     * Registers a new CommsProvider with the system.
     *
     * @param provider the file io provider to register
     */
    public synchronized static void registerProvider(
            final CommsProvider provider) {
        registerProvider(provider, false);
    }

    /**
     * Registers a new CommsProvider with the system.
     *
     * @param provider the file io provider to register
     */
    public synchronized static void registerProvider(
            final CommsProvider provider, boolean def) {
        if (registered)
            return;

        registered = true;
        commsProvider = provider;
        markedAsDefault = def;
    }

    /**
     * Returns <code>true</code> if the current provider is considered
     * default, <code>false</code> otherwise.
     *
     * @return true if the current provider is marked as default.
     */
    public synchronized static boolean isDefault() {
        return markedAsDefault;
    }

    /**
     * Returns the current Comms Provider instance.
     *
     * @return the provider that is currently registered.
     */
    public synchronized static CommsProvider getProvider() {
        return commsProvider;
    }
}
