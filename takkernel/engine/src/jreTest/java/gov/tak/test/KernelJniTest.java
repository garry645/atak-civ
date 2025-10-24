package gov.tak.test;

import android.content.Context;

import com.atakmap.coremap.io.DefaultIOProvider;
import com.atakmap.coremap.io.IOProviderFactoryHelper;
import com.atakmap.map.EngineLibrary;
import org.junit.Before;

public abstract class KernelJniTest extends KernelTest
{
    protected KernelJniTest()
    {
        EngineLibrary.initialize();
    }

    @Before
    public final void clearIoProvider() {
        IOProviderFactoryHelper.registerProvider(new DefaultIOProvider(), true);
    }
}
