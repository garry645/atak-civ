package gov.tak.platform.commons.resources;

import gov.tak.api.commons.resources.IResourceManager;
import gov.tak.api.commons.resources.IResourceManagerTest;

public class JavaResourceManagerTest extends IResourceManagerTest
{

    @Override
    protected IResourceManager createResourceManager()
    {
        return new JavaResourceManager();
    }

}
