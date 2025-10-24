package gov.tak.platform.commons.resources;

import android.content.Context;
import android.content.res.Resources;

import gov.tak.api.commons.resources.IResourceManager;
import gov.tak.api.commons.resources.ResourceType;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

abstract class ResourceManagerBase implements IResourceManager
{
    final static Map<ResourceType, String> resourceTypeToAndroidName = new HashMap<>();

    static
    {
        resourceTypeToAndroidName.put(ResourceType.Array, "array");
        resourceTypeToAndroidName.put(ResourceType.Color, "color");
        resourceTypeToAndroidName.put(ResourceType.Drawable, "drawable");
        resourceTypeToAndroidName.put(ResourceType.Font, "font");
        resourceTypeToAndroidName.put(ResourceType.Raw, "raw");
        resourceTypeToAndroidName.put(ResourceType.String, "string");
        resourceTypeToAndroidName.put(ResourceType.Xml, "xml");
    }

    final Context context;
    final Resources resources;

    ResourceManagerBase(Context context)
    {
        this.context = context;
        this.resources = context.getResources();
    }

    @Override
    public int getResourceId(String resourceName, ResourceType resourceType)
    {
        return context.getResources().getIdentifier(resourceName, resourceTypeToAndroidName.get(resourceType), context.getPackageName());
    }

    @Override
    public InputStream openRawResource(int resourceId) throws IOException
    {
        return context.getResources().openRawResource(resourceId);
    }

    @Override
    public InputStream openRawResource(String resourceName, ResourceType resourceType) throws IOException
    {
        return openRawResource(getResourceId(resourceName, resourceType));
    }

    @Override
    public int getColor(String resourceName)
    {
        return getColor(getResourceId(resourceName, ResourceType.Color));
    }

    @Override
    public String getString(String resourceName)
    {
        return getString(getResourceId(resourceName, ResourceType.String));
    }

    @Override
    public String[] getStringArray(String resourceName)
    {
        return getStringArray(getResourceId(resourceName, ResourceType.Array));
    }

    @Override
    public int getColor(int resourceId)
    {
        return context.getResources().getColor(resourceId);
    }

    @Override
    public String getString(int resourceId)
    {
        return context.getResources().getString(resourceId);
    }

    @Override
    public String[] getStringArray(int resourceId)
    {
        return context.getResources().getStringArray(resourceId);
    }
}
