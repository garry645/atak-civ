package gov.tak.api.commons.resources;

import java.io.IOException;
import java.io.InputStream;

public interface IResourceManager
{
    /**
     * Retrieves the ID for the specified resource.
     *
     * <P>Resource IDs may not be constant between runtimes in all environments and should not be persisted.
     *
     * @param resourceName The resource name. If the resource file has an extension, it MUST be omitted.
     * @param resourceType The resource type
     * @return The ID of the resource, or <code>-1</code> if no resource with the given name/type is found.
     */
    int getResourceId(String resourceName, ResourceType resourceType);

    /**
     * Opens a stream for reading the raw bytes of the specified resource. The named resource must have a type of one of
     * the following:
     * <UL>
     * <LI>{@link ResourceType#Drawable}</LI>
     * <LI>{@link ResourceType#Font}</LI>
     * <LI>{@link ResourceType#Raw}</LI>
     * <LI>{@link ResourceType#Xml}</LI>
     * </UL>
     *
     * @param resourceId The resource ID
     * @return An {@link InputStream} providing access to the resource data or <code>null</code> if no such named
     * resource exists or the resource type is not interpretable as a byte stream.
     * @throws IOException
     */
    InputStream openRawResource(int resourceId) throws IOException;

    /**
     * Opens a stream for reading the raw bytes of the specified resource.
     *
     * @param resourceName The name of the resource
     * @param resourceType <UL>
     *                     <LI>{@link ResourceType#Drawable}</LI>
     *                     <LI>{@link ResourceType#Font}</LI>
     *                     <LI>{@link ResourceType#Raw}</LI>
     *                     <LI>{@link ResourceType#Xml}</LI>
     *                     </UL>
     * @return An {@link InputStream} providing access to the resource data or <code>null</code> if no such named
     * resource exists or the resource type is not interpretable as a byte stream.
     * @throws IOException
     */
    InputStream openRawResource(String resourceName, ResourceType resourceType) throws IOException;

    /**
     * Returns the named color resource as a 32-bit integer packed ARGB value.
     *
     * @param resourceName The resource name
     * @return The color resource as a 32-bit integer packed ARGB value
     * @throws RuntimeException if the specified resource does not exist or is not a color resource
     */
    int getColor(String resourceName);

    /**
     * Returns the named string resource.
     *
     * @param resourceName The resource name
     * @return The string resource
     * @throws RuntimeException if the specified resource does not exist or is not a string resource
     */
    String getString(String resourceName);

    /**
     * Returns the named array resource.
     *
     * @param resourceName The resource name
     * @return The array resource
     * @throws RuntimeException if the specified resource does not exist or is not a array resource
     */
    String[] getStringArray(String resourceName);

    /**
     * Returns the named color resource as a 32-bit integer packed ARGB value.
     *
     * @param resourceId The resource ID
     * @return The color resource as a 32-bit integer packed ARGB value
     * @throws RuntimeException if the specified resource does not exist or is not a color resource
     */
    int getColor(int resourceId);

    /**
     * Returns the named string resource.
     *
     * @param resourceId The resource ID
     * @return The string resource
     * @throws RuntimeException if the specified resource does not exist or is not a string resource
     */
    String getString(int resourceId);

    /**
     * Returns the named array resource.
     *
     * @param resourceId The resource ID
     * @return The array resource
     * @throws RuntimeException if the specified resource does not exist or is not a array resource
     */
    String[] getStringArray(int resourceId);
}
