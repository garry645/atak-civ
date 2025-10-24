
package com.atakmap.android.drawing.milsym;

import android.content.Context;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.atakmap.android.cotdetails.ModifierInfoView;
import com.atakmap.android.cotdetails.extras.ExtraDetailsProvider;
import com.atakmap.android.drawing.mapItems.DrawingCircle;
import com.atakmap.android.drawing.mapItems.DrawingEllipse;
import com.atakmap.android.drawing.mapItems.DrawingRectangle;
import com.atakmap.android.drawing.mapItems.DrawingShape;
import com.atakmap.android.ipc.AtakBroadcast;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;

import com.atakmap.app.R;

import java.util.Collection;

import gov.tak.api.symbology.ISymbolTable;
import gov.tak.api.symbology.Modifier;
import gov.tak.platform.symbology.SymbologyProvider;

final class MilSymExtraDetails implements ExtraDetailsProvider {

    private final MapView mapView;
    private final Context context;

    private final static int buttonStyle = R.style.darkButton;

    public MilSymExtraDetails(MapView mapView, Context context) {
        this.mapView = mapView;
        this.context = context;
    }

    public View getExtraView(MapItem mapItem, View existing) {
        if (existing == null
                && TacticalGraphicMenuFactory.isObjectSupported(mapItem)) {

            TextView header = new TextView(context);
            header.setTextColor(
                    context.getResources().getColor(R.color.heading_yellow));
            header.setTextSize(context.getResources()
                    .getDimension(R.dimen.draper_font)
                    / context.getResources().getDisplayMetrics().density);
            header.setText(R.string.milsym_details);

            Button button = new Button(context, null, 0, buttonStyle);
            button.setPadding(16, 16, 16, 16);

            String code = mapItem.getMetaString("milsym", null);

            String fullName = SymbologyProvider.getFullName(code);
            if(code != null) {
                ISymbolTable table = SymbologyProvider.getSymbolTable(code);
                if(table != null) {
                    fullName = MilSym.getTranslatedFullName(table.getSymbol(code));
                }
            }

            if (fullName == null) {
                if (mapItem instanceof DrawingCircle)
                    button.setText(R.string.drawing_circle);
                else if (mapItem instanceof DrawingRectangle)
                    button.setText(R.string.drawing_rectangle);
                else if (mapItem instanceof DrawingEllipse)
                    button.setText(R.string.drawing_ellipse);
                else if (mapItem instanceof DrawingShape)
                    button.setText(R.string.drawing_shape);

            } else {
                button.setText(fullName);
            }

            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    Intent selectType = new Intent(
                            MilSymReceiver.ACTION_SELECT_TYPE);
                    MapItem m = TacticalGraphicMenuFactory
                            .getSubjectMapItem(mapItem);
                    if (m == null)
                        return;

                    selectType.putExtra(MilSymReceiver.EXTRA_UID, m.getUID());
                    AtakBroadcast.getInstance().sendBroadcast(selectType);

                    Intent i = new Intent();
                    i.setAction("com.atakmap.android.maps.HIDE_MENU");
                    AtakBroadcast.getInstance().sendBroadcast(i);

                }
            });

            LinearLayout linearLayout = new LinearLayout(context);
            linearLayout.setOrientation(LinearLayout.VERTICAL);
            linearLayout.setLayoutParams(new ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT));

            linearLayout.addView(header);
            linearLayout.addView(button);
            existing = linearLayout;

            do {
                if (code == null)
                    break;
                Collection<Modifier> modifiers = SymbologyProvider.getModifiers(code);
                if(modifiers == null)
                    break;
                ModifierInfoView.Builder builder = new ModifierInfoView.Builder(mapView,
                        linearLayout, mapItem);
                for (Modifier mod : modifiers)
                    builder.addModifierView(mod);
                builder.build();
            } while(false);
        }

        return existing;
    }
}
