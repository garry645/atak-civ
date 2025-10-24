
package com.atakmap.android.test.helpers;

import androidx.test.espresso.ViewAction;
import androidx.test.espresso.action.CoordinatesProvider;
import androidx.test.espresso.action.GeneralClickAction;
import androidx.test.espresso.action.Press;
import androidx.test.espresso.action.Tap;
import android.view.View;

/**
 * Allows us to click on specific coordinates in a
 * view using percentages (so same for all screen sizes).
 * <p>
 * Created by vmm0613 on 5/31/2017.
 */
public class ClickXYPercent {

    public static CoordinatesProvider coords(final float pctX,
            final float pctY) {
        return new CoordinatesProvider() {
            @Override
            public float[] calculateCoordinates(View view) {

                final int[] screenPos = new int[2];
                view.getLocationOnScreen(screenPos);
                int w = view.getWidth();
                int h = view.getHeight();

                float x = w * pctX;
                float y = h * pctY;

                final float screenX = screenPos[0] + x;
                final float screenY = screenPos[1] + y;

                return new float[] {
                        screenX, screenY
                };
            }
        };

    }

    public static ViewAction clickPercent(final float pctX, final float pctY) {
        return new GeneralClickAction(
                Tap.SINGLE,
                coords(pctX, pctY),
                Press.FINGER);
    }

    public static ViewAction clickPercentLong(final float pctX,
            final float pctY) {
        return new GeneralClickAction(
                Tap.LONG,
                coords(pctX, pctY),
                Press.FINGER);
    }
}
