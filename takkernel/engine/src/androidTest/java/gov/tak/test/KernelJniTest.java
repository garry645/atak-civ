package gov.tak.test;

import android.Manifest;
import android.content.Context;

import com.atakmap.coremap.io.DefaultIOProvider;
import com.atakmap.coremap.io.IOProviderFactoryHelper;
import com.atakmap.coremap.loader.NativeLoader;
import com.atakmap.map.EngineLibrary;

import org.junit.Rule;
import org.junit.Before;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.rule.GrantPermissionRule;

public abstract class KernelJniTest extends KernelTest
{
    protected KernelJniTest()
    {
        Context appContext = ApplicationProvider.getApplicationContext();

        NativeLoader.init(appContext);
        EngineLibrary.initialize();
    }

    @Before
    public final void clearIoProvider() {
        IOProviderFactoryHelper.registerProvider(new DefaultIOProvider(), true);
    }
}
