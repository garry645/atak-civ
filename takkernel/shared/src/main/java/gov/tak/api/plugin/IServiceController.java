package gov.tak.api.plugin;

public interface IServiceController
{
    /**
     * @param serviceType the type of the service to be retrieved
     * @return An instance of the specified service, or <code>null</code> if the application does not provide
     */
    <T> T getService(Class<T> serviceType);

    /**
     * @param type the type of the component to be registered with the application
     * @param obj  The component object
     * @return <code>true</code> if the component was successfully registered, <code>false</code> otherwise
     */
    <T> boolean registerComponent(Class<T> type, T obj);

    /**
     * @param type the type of the component to be unregistered from the application
     * @param obj  The component object
     * @return <code>true</code> if the component was successfully unregistered, <code>false</code> otherwise
     */
    <T> boolean unregisterComponent(Class<T> type, T obj);
}
