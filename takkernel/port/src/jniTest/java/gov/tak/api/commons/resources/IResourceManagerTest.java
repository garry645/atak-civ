package gov.tak.api.commons.resources;

import gov.tak.test.KernelTest;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.io.InputStream;
import java.util.Locale;

public abstract class IResourceManagerTest extends KernelTest
{

    IResourceManager resmgr;

    protected abstract IResourceManager createResourceManager();

    @Before
    public void setupResourceManager()
    {
        resmgr = createResourceManager();
    }

    @Test
    public void getResourceId_WithValidResource()
    {
        String[] drawableResources =
                {
                        "alpha_sort",
                        "bloodhound_nav_lit",
                        "bloodhound_nav_unlit",
                        "attachment",
                        "radial_delete",
                        "radial_fine",
                        "obj_c_130",
                };
        for (String drawableResource : drawableResources)
        {
            int id = resmgr.getResourceId(drawableResource, ResourceType.Drawable);
            Assert.assertTrue("Failed to obtain ID for resource: " + drawableResource, id > 0);
        }

        String[] fontResources =
                {
                        "nunito_bold",
                        "nunito_regular",
                };
        for (String fontResource : fontResources)
        {
            int id = resmgr.getResourceId(fontResource, ResourceType.Font);
            Assert.assertTrue("Failed to obtain ID for resource: " + fontResource, id > 0);
        }

        String[] rawResources =
                {
                        "textfile",
                };
        for (String rawResource : rawResources)
        {
            int id = resmgr.getResourceId(rawResource, ResourceType.Raw);
            Assert.assertTrue("Failed to obtain ID for resource: " + rawResource, id > 0);
        }

        String[] stringResources =
                {
                        "favorites",
                        "reset_hint",
                };
        for (String stringResource : stringResources)
        {
            int id = resmgr.getResourceId(stringResource, ResourceType.String);
            Assert.assertTrue("Failed to obtain ID for resource: " + stringResource, id > 0);
        }

        String[] stringArrayResources =
                {
                        "brightness_names",
                };
        for (String stringArrayResource : stringArrayResources)
        {
            int id = resmgr.getResourceId(stringArrayResource, ResourceType.Array);
            Assert.assertTrue("Failed to obtain ID for resource: " + stringArrayResource, id > 0);
        }

        String[] xmlResources =
                {
                        "accounts",
                        "wms_preferences",
                };
        for (String xmlResource : xmlResources)
        {
            int id = resmgr.getResourceId(xmlResource, ResourceType.Xml);
            Assert.assertTrue("Failed to obtain ID for resource: " + xmlResource, id > 0);
        }
    }

    @Test
    public void getResourceId_WithInvalidResource()
    {
        int id = resmgr.getResourceId("invalidresource", ResourceType.Drawable);
        Assert.assertFalse(id > 0);
    }

    @Test
    public void getResourceId_WithResourceTypeMismatch()
    {
        int id = resmgr.getResourceId("alpha_sort", ResourceType.Xml);
        Assert.assertFalse(id > 0);
    }

    @Test
    public void openRawResource_FromId_WithValidResource() throws IOException
    {
        {
            int id = resmgr.getResourceId("alpha_sort", ResourceType.Drawable);
            try (InputStream stream = resmgr.openRawResource(id))
            {
                Assert.assertNotNull("Failed to obtain stream for drawable resource", stream);
            }
        }
        {
            int id = resmgr.getResourceId("nunito_bold", ResourceType.Font);
            try (InputStream stream = resmgr.openRawResource(id))
            {
                Assert.assertNotNull("Failed to obtain stream for xml resource", stream);
            }
        }
        {
            int id = resmgr.getResourceId("textfile", ResourceType.Raw);
            try (InputStream stream = resmgr.openRawResource(id))
            {
                Assert.assertNotNull("Failed to obtain stream for xml resource", stream);
            }
        }
        {
            int id = resmgr.getResourceId("accounts", ResourceType.Xml);
            try (InputStream stream = resmgr.openRawResource(id))
            {
                Assert.assertNotNull("Failed to obtain stream for xml resource", stream);
            }
        }
    }

    @Test
    public void openRawResource_FromNameAndType_WithValidResource() throws IOException
    {
        try (InputStream stream = resmgr.openRawResource("alpha_sort", ResourceType.Drawable))
        {
            Assert.assertNotNull("Failed to obtain stream for drawable resource", stream);
        }

        try (InputStream stream = resmgr.openRawResource("accounts", ResourceType.Xml))
        {
            Assert.assertNotNull("Failed to obtain stream for xml resource", stream);
        }
    }

    @Test
    public void getColor_FromName_WithValidResource() throws IOException
    {
        final int color = resmgr.getColor("color_group");
        Assert.assertEquals(0x30808080, color);
    }

    @Test
    public void getString_FromName_WithValidResource() throws IOException
    {
        final String string = resmgr.getString("are_you_sure");
        Assert.assertEquals("Are You Sure?", string);
    }

    @Test
    public void getStringArray_FromName_WithValidResource() throws IOException
    {
        final String[] strings = resmgr.getStringArray("self_coord_display_names");
        Assert.assertNotNull(strings);
        Assert.assertEquals(3, strings.length);
        Assert.assertEquals("Bottom-Right Box", strings[0]);
        Assert.assertEquals("Bottom Bar", strings[1]);
        Assert.assertEquals("Do Not Display", strings[2]);
    }

    @Test
    public void getColor_FromId_WithValidResource() throws IOException
    {
        {
            final int id = resmgr.getResourceId("heading_yellow", ResourceType.Color);
            Assert.assertTrue(id > 0);
            final int color = resmgr.getColor(id);
            Assert.assertEquals(0xffdfb228, color);
        }

        {
            final int id = resmgr.getResourceId("alert", ResourceType.Color);
            Assert.assertTrue(id > 0);
            final int color = resmgr.getColor(id);
            Assert.assertEquals(-709331, color);
        }
    }

    @Test(expected = RuntimeException.class)
    public void getColor_FromId_WithInvalidResource() throws IOException
    {
        resmgr.getColor(-1);
        Assert.fail();
    }

    @Test(expected = RuntimeException.class)
    public void getColor_FromId_WithMismatchedResourceType() throws IOException
    {
        int id = resmgr.getResourceId("textfile", ResourceType.Raw);
        Assert.assertTrue(id > 0);
        resmgr.getColor(id);
        Assert.fail();
    }

    @Test
    public void getString_FromId_WithValidResource() throws IOException
    {
        {
            final int id = resmgr.getResourceId("preferences_text2031", ResourceType.String);
            Assert.assertTrue(id > 0);
            final String string = resmgr.getString(id);
            Assert.assertEquals("Enable to display all preferences when opening Settings.", string);
        }
        {
            final int id = resmgr.getResourceId("bloodhoundRerouteTimerDescription", ResourceType.String);
            Assert.assertTrue(id > 0);
            final String string = resmgr.getString(id);
            Assert.assertEquals("Specify how frequently bloodhound will check to see if a reroute needs to be calculated.", string);
        }
    }

    @Test
    public void getString_FromId_WithValidResourceAndDifferentLocale() throws Exception
    {

        try (AutoCloseable localeHandler = new AutoCloseable()
        {
            @Override
            public void close()
            {
                setLocale(Locale.US);
            }
        })
        {
            setLocale(Locale.FRENCH);
            {
                final int id = resmgr.getResourceId("layer_outline_color_title", ResourceType.String);
                Assert.assertTrue(id > 0);
                final String string = resmgr.getString(id);
                Assert.assertEquals("Couleur de contour du calque", string);
            }
            {
                final int id = resmgr.getResourceId("play_pause", ResourceType.String);
                Assert.assertTrue(id > 0);
                final String string = resmgr.getString(id);
                Assert.assertEquals("Lecture ou pause", string);
            }
        }
    }

    @Test(expected = RuntimeException.class)
    public void getString_FromId_WithInvalidResource() throws IOException
    {
        resmgr.getString(-1);
        Assert.fail();
    }

    @Test(expected = RuntimeException.class)
    public void getString_FromId_WithMismatchedResourceType() throws IOException
    {
        // NOTE: Android will return the resource path for _raw_ type resources
        int id = resmgr.getResourceId("tap_selfoverlay_names", ResourceType.Array);
        Assert.assertTrue(id > 0);
        resmgr.getString(id);
        Assert.fail();
    }

    @Test
    public void getStringArray_FromId_WithValidResource() throws IOException
    {
        {
            final int id = resmgr.getResourceId("tap_selfoverlay_names", ResourceType.Array);
            Assert.assertTrue(id > 0);
            final String[] strings = resmgr.getStringArray(id);
            Assert.assertNotNull(strings);
            Assert.assertEquals(3, strings.length);
            Assert.assertEquals("Do Nothing", strings[0]);
            Assert.assertEquals("Change Coordinates", strings[1]);
            Assert.assertEquals("Pan to Self", strings[2]);
        }
        {
            final int id = resmgr.getResourceId("overlays_scaling_names", ResourceType.Array);
            Assert.assertTrue(id > 0);
            final String[] strings = resmgr.getStringArray(id);
            Assert.assertNotNull(strings);
            Assert.assertEquals(5, strings.length);
            Assert.assertEquals("Normal", strings[0]);
            Assert.assertEquals("1.25x", strings[1]);
            Assert.assertEquals("1.5x", strings[2]);
            Assert.assertEquals("1.75x", strings[3]);
            Assert.assertEquals("2x", strings[4]);
        }
    }

    @Test(expected = RuntimeException.class)
    public void getStringArray_FromId_InvalidResource() throws IOException
    {
        resmgr.getStringArray(-1);
        Assert.fail();
    }
}