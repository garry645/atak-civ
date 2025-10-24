package gov.tak.api.ui;

import gov.tak.api.commons.graphics.Bitmap;
import gov.tak.api.commons.graphics.BitmapDrawable;
import gov.tak.platform.marshal.MarshalManager;
import gov.tak.platform.ui.MotionEvent;
import org.junit.Assert;
import org.junit.Test;

public class ToolbarItemTest
{
    @Test
    public void ToolbarItem_title_roundtrip()
    {
        final String title = "title";
        final Bitmap icon = new Bitmap(1, 1);
        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        final ToolbarItem item = builder.build();
        Assert.assertNotNull(item);

        Assert.assertEquals(title, item.getTitle());
    }

    @Test
    public void ToolbarItem_icon_roundtrip()
    {
        final String title = "title";
        final BitmapDrawable icon = new BitmapDrawable(new Bitmap(1, 1));
        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        final ToolbarItem item = builder.build();
        Assert.assertNotNull(item);

        Assert.assertSame(icon, item.getIcon());
    }

    @Test
    public void ToolbarItem_identifier_roundtrip()
    {
        final String title = "title";
        final Bitmap icon = new Bitmap(1, 1);
        final String identifier = "identifier";
        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        builder.setIdentifier(identifier);
        final ToolbarItem item = builder.build();
        Assert.assertNotNull(item);

        Assert.assertEquals(identifier, item.getIdentifier());
    }

    @Test
    public void ToolbarItem_category_roundtrip()
    {
        final String title = "title";
        final Bitmap icon = new Bitmap(1, 1);
        final String category = "category";

        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        builder.setCategory(category);
        final ToolbarItem item = builder.build();
        Assert.assertNotNull(item);

        Assert.assertEquals(category, item.getCategory());
    }

    @Test
    public void ToolbarItem_fullbutton_roundtrip()
    {
        final String title = "title";
        final Bitmap icon = new Bitmap(1, 1);

        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        builder.setFullButtonIcon(true);
        final ToolbarItem item = builder.build();
        Assert.assertNotNull(item);

        Assert.assertTrue(item.isFullButtonIcon());
    }

    @Test
    public void ToolbarItem_listener_roundtrip()
    {
        final String title = "title";
        final Bitmap icon = new Bitmap(1, 1);
        final IToolbarItemListener listener = new IToolbarItemListener() {
            @Override
            public void onItemEvent(ToolbarItem item, MotionEvent event)
            { }
        };

        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        builder.setListener(listener);
        final ToolbarItem item = builder.build();
        Assert.assertNotNull(item);

        Assert.assertSame(listener, item.getListener());
    }

    @Test
    public void ToolbarItem_default_values_set()
    {
        final String title = "title";
        final Bitmap icon = new Bitmap(1, 1);

        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        final ToolbarItem item = builder.build();
        Assert.assertNotNull(item);

        Assert.assertNull(item.getCategory());
        Assert.assertNotNull(item.getIdentifier());
        Assert.assertNull(item.getListener());
        Assert.assertFalse(item.isFullButtonIcon());
    }

    @Test(expected = NullPointerException.class)
    public void ToolbarItem_null_title_throws()
    {
        final String title = null;
        final Bitmap icon = new Bitmap(1, 1);
        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        final ToolbarItem item = builder.build();
        Assert.fail();
    }

    @Test(expected = NullPointerException.class)
    public void ToolbarItem_null_icon_throws()
    {
        final String title = "title";
        final Bitmap icon = null;
        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        final ToolbarItem item = builder.build();
        Assert.fail();
    }

    @Test(expected = NullPointerException.class)
    public void ToolbarItem_null_identifier_throws()
    {
        final String title = "title";
        final Bitmap icon = new Bitmap(1, 1);
        ToolbarItem.Builder builder = new ToolbarItem.Builder(title, icon);
        builder.setIdentifier(null);
        Assert.fail();
    }
}
