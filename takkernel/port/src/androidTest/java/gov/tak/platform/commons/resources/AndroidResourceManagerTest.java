package gov.tak.platform.commons.resources;

import android.content.Context;
import android.content.res.Resources;
import android.os.SystemClock;

import gov.tak.api.commons.resources.IResourceManager;
import gov.tak.api.commons.resources.IResourceManagerTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class AndroidResourceManagerTest extends IResourceManagerTest
{

    @Override
    protected IResourceManager createResourceManager()
    {
        return new AndroidResourceManager(getTestContext());
    }

}