
package com.atakmap.android.test.helpers;

import android.view.View;
import android.view.ViewGroup;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class CustomViewMatchers {
    public static Matcher<View> nthChild(final int childPosition) {
        return new TypeSafeMatcher<View>() {
            @Override
            public void describeTo(Description description) {
                description.appendText("with " + childPosition
                        + " child view of type parentMatcher");
            }

            @Override
            public boolean matchesSafely(View view) {
                if (!(view.getParent() instanceof ViewGroup)) {
                    return false;
                }

                ViewGroup group = (ViewGroup) view.getParent();

                if (group.getChildCount() <= childPosition)
                    return false;

                return group.getChildAt(childPosition).equals(view);
            }
        };
    }
}
