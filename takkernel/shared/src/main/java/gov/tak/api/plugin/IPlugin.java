package gov.tak.api.plugin;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
public interface IPlugin
{

    /** 
     * Called when a plugin is starting and expected to begin performing and receiving events 
     * from the application. It can be called multiple times during the lifetime of the app, 
     * either upon first load or after being shutdown. 
     */
    void onStart();

    /**
     * Called when a plugin is stopping. The plugin is expected to clean up any resources. 
     * The app will cease to send events to the plugin. It can be called multiple times during 
     * the lifetime of the app, but only after the plugin has been started.
     */
    void onStop();
}
