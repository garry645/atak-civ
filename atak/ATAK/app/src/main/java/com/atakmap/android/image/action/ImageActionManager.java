
package com.atakmap.android.image.action;

import java.util.concurrent.ConcurrentLinkedQueue;

import gov.tak.api.annotation.NonNull;

public class ImageActionManager {

    public interface ImageActionCallback {
        int ACTION_SUCCESS = 0;
        int ACTION_FAILURE = -1;
        int ACTION_CANCELLED = 1;

        /**
         * Callback method that is always called when the image action is completed.
         * @param originalFile the path to the original file
         * @param newFile the path to the file that has been actioned or null if
         *                    cancelled or failure has occurred.
         * @param status one of ACTION_SUCCESS, ACTION_FAILURE, ACTION_CANCELLED
         * @param reason any human readable text that helps the user understand the reason
         *               for a failure
         */
        void onComplete(@NonNull String originalFile, String newFile,
                int status, String reason);
    }

    public interface ImageActionProvider {

        /**
         * Returns the unique identifier for the provider
         * @return the string unique identifier
         */
        @NonNull
        String getUID();

        /**
         * The human readable name of the provider
         * @return the provider
         */
        @NonNull
        String getName();

        /**
         * A description of the provider for human consumption
         * @return the description
         */
        @NonNull
        String getDescription();

        /**
         * Given a valid file path, perform the action on and when completed fire the callback.
         * @param file the complete filename and path
         * @param callback the callback to use when completed
         */
        void performAction(@NonNull String file,
                @NonNull ImageActionCallback callback);

    }

    private static ImageActionManager instance;
    private final ConcurrentLinkedQueue<ImageActionProvider> providers = new ConcurrentLinkedQueue<>();

    private ImageActionManager() {
    }

    @NonNull
    public static ImageActionManager getInstance() {
        if (instance == null)
            instance = new ImageActionManager();
        return instance;
    }

    /**
     * Register a image action provider such as an image compression or image resize capability.
     * @param provider the provider to be registered
     */
    public void registerImageActionProvider(
            @NonNull ImageActionProvider provider) {
        if (!providers.contains(provider))
            providers.add(provider);
    }

    /**
     * Unregister a image action provider.
     * @param provider the provider to be registered
     */
    public void unregisterImageActionProvider(
            @NonNull ImageActionProvider provider) {
        providers.remove(provider);
    }

    /**
     * Return a list of currently registered providers.  If a provider is unregistered after
     * this list is obtained, then it will remain in the returned list.
     * @return the array of providers
     */
    @NonNull
    public ImageActionProvider[] getProviders() {
        return providers.toArray(new ImageActionProvider[0]);
    }

}
