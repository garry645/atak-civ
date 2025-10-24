package gov.tak.api.ui;

import gov.tak.platform.marshal.MarshalManager;
import gov.tak.test.KernelTest;

import org.junit.Assert;
import org.junit.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

public abstract class PaneBuilderBaseTest extends KernelTest
{
    protected abstract Class<?> getPaneSourceType();
    protected abstract PaneBuilder newInstance();

    @Test
    public void PaneBuilder_marshal_source()
    {
        PaneBuilder builder = newInstance();
        Pane pane = builder.build();
        Assert.assertNotNull(pane);

        Object paneSource = MarshalManager.marshal(pane, Pane.class, getPaneSourceType());
        Assert.assertNotNull(paneSource);
        Assert.assertTrue(getPaneSourceType().isAssignableFrom(paneSource.getClass()));
    }

    @Test
    public void PaneBuilder_metadata_roundtrip()
    {
        final String metaKey1 = "abc";
        final Object metaValue1 = "123";
        final String metaKey2 = "def";
        final Object metaValue2 = Integer.valueOf(456);

        PaneBuilder builder = newInstance();
        builder.setMetaValue(metaKey1, metaValue1);
        builder.setMetaValue(metaKey2, metaValue2);
        Pane pane = builder.build();
        Assert.assertNotNull(pane);

        Assert.assertTrue(pane.hasMetaValue(metaKey1));
        Assert.assertSame(metaValue1, pane.getMetaValue(metaKey1));

        Assert.assertTrue(pane.hasMetaValue(metaKey2));
        Assert.assertSame(metaValue2, pane.getMetaValue(metaKey2));
    }

    @Test(expected = RuntimeException.class)
    public void PaneBuilder_null_source_object_throws()
    {
        // NOTE: constructor may be overloaded; use reflection to try each matching constructor for
        // the associated source type
        Class<?> sourceType = getPaneSourceType();
        try {
            Constructor<PaneBuilder> ctor = PaneBuilder.class.getConstructor(sourceType);
            ctor.newInstance(null);
            Assert.fail();
        } catch (NoSuchMethodException | IllegalAccessException | InstantiationException | InvocationTargetException e) {
            Assert.fail();
        }
    }
}
