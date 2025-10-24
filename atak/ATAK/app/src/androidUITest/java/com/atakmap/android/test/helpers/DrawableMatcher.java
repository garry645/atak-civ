
package com.atakmap.android.test.helpers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.graphics.drawable.StateListDrawable;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.appcompat.view.menu.ActionMenuItemView;
import androidx.core.content.ContextCompat;
import androidx.test.espresso.matcher.BoundedMatcher;
import androidx.test.platform.app.InstrumentationRegistry;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Created by vmm0613 on 6/12/2017.
 */
public class DrawableMatcher extends TypeSafeMatcher<View> {

    private final int resourceId;

    public DrawableMatcher(int resourceId) {
        super(View.class);
        this.resourceId = resourceId;
    }

    private String resourceName = null;
    private Drawable expectedDrawable = null;

    @Override
    public boolean matchesSafely(View target) {
        if (expectedDrawable == null) {
            loadDrawableFromResources(target.getContext());
        }
        if (invalidExpectedDrawable()) {
            return false;
        }

        if (target instanceof ImageView) {
            return hasImage((ImageView) target) || hasBackground(target);
        }
        if (target instanceof TextView) {
            return hasCompoundDrawable((TextView) target)
                    || hasBackground(target);
        }
        return hasBackground(target);
    }

    private void loadDrawableFromResources(Context context) {
        try {
            expectedDrawable = context.getDrawable(resourceId);
            resourceName = context.getResources()
                    .getResourceEntryName(resourceId);
        } catch (Resources.NotFoundException ignored) {
            // view could be from a context unaware of the resource id.
        }
    }

    private boolean invalidExpectedDrawable() {
        return expectedDrawable == null;
    }

    private boolean hasImage(ImageView target) {
        return isSameDrawable(target.getDrawable());
    }

    private boolean hasCompoundDrawable(TextView target) {
        if (target.getCompoundDrawables() == null) {
            return false;
        }
        for (Drawable drawable : target.getCompoundDrawables()) {
            if (isSameDrawable(drawable)) {
                return true;
            }
        }
        return false;
    }

    private boolean hasBackground(View target) {
        return isSameDrawable(target.getBackground());
    }

    private boolean isSameDrawable(Drawable drawable) {
        if (drawable == null) {
            return false;
        }
        final Drawable.ConstantState state = expectedDrawable
                .getConstantState();
        if (state == null)
            return false;
        else
            return state.equals(drawable.getConstantState());
    }

    @Override
    public void describeTo(Description description) {
        description.appendText("with drawable from resource id: ");
        description.appendValue(resourceId);
        if (resourceName != null) {
            description.appendText("[");
            description.appendText(resourceName);
            description.appendText("]");
        }
    }

    public Drawable getSingleDrawable(LayerDrawable layerDrawable) {

        int resourceBitmapHeight = 136, resourceBitmapWidth = 153;

        float widthInInches = 0.9f;

        int widthInPixels = (int) (widthInInches * InstrumentationRegistry
                .getInstrumentation().getTargetContext().getResources()
                .getDisplayMetrics().densityDpi);
        int heightInPixels = widthInPixels * resourceBitmapHeight
                / resourceBitmapWidth;

        int insetLeft = 10, insetTop = 10, insetRight = 10, insetBottom = 10;

        layerDrawable.setLayerInset(1, insetLeft, insetTop, insetRight,
                insetBottom);

        Bitmap bitmap = Bitmap.createBitmap(widthInPixels, heightInPixels,
                Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(bitmap);
        layerDrawable.setBounds(0, 0, widthInPixels, heightInPixels);
        layerDrawable.draw(canvas);

        BitmapDrawable bitmapDrawable = new BitmapDrawable(
                InstrumentationRegistry.getInstrumentation().getTargetContext()
                        .getResources(),
                bitmap);
        bitmapDrawable.setBounds(0, 0, widthInPixels, heightInPixels);

        return bitmapDrawable;
    }

    public static Bitmap drawableToBitmap(Drawable drawable) {
        Bitmap bitmap;

        if (drawable instanceof BitmapDrawable) {
            BitmapDrawable bitmapDrawable = (BitmapDrawable) drawable;
            if (bitmapDrawable.getBitmap() != null) {
                return bitmapDrawable.getBitmap();
            }
        }

        if (drawable.getIntrinsicWidth() <= 0
                || drawable.getIntrinsicHeight() <= 0) {
            bitmap = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888); // Single color bitmap will be created of 1x1 pixel
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        }

        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

    public static Matcher<View> withActionIconDrawable(@DrawableRes
    final int resourceId) {
        return new BoundedMatcher<View, ActionMenuItemView>(
                ActionMenuItemView.class) {
            @Override
            public void describeTo(final Description description) {
                description.appendText(
                        "has image drawable resource " + resourceId);
            }

            @SuppressLint("RestrictedApi")
            @Override
            public boolean matchesSafely(
                    final ActionMenuItemView actionMenuItemView) {
                return sameBitmap(actionMenuItemView.getContext(),
                        actionMenuItemView.getItemData().getIcon(), resourceId);
            }
        };
    }

    private static boolean sameBitmap(Context context, Drawable drawable,
            int resourceId) {
        Drawable otherDrawable = ContextCompat.getDrawable(context, resourceId);
        if (drawable == null || otherDrawable == null) {
            return false;
        }
        if (drawable instanceof StateListDrawable
                && otherDrawable instanceof StateListDrawable) {
            drawable = drawable.getCurrent();
            otherDrawable = otherDrawable.getCurrent();
        }
        if (drawable instanceof BitmapDrawable) {
            Bitmap bitmap = ((BitmapDrawable) drawable).getBitmap();
            Bitmap otherBitmap = ((BitmapDrawable) otherDrawable).getBitmap();
            return bitmap.sameAs(otherBitmap);
        }
        return false;
    }

}
