
package com.atakmap.android.gui;

import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.atakmap.android.cot.detail.UrlLinkDetailEntry;
import com.atakmap.android.geofence.data.ShapeUtils;
import com.atakmap.android.maps.MapItem;
import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.log.Log;

import java.util.List;

import gov.tak.api.util.Disposable;

public class UrlUiHandler implements
        SharedPreferences.OnSharedPreferenceChangeListener, Disposable {

    private final String DISABLE_AUTOLINK_PREF_KEY = "disable_autolink_mapitem_links";
    private final View parentView;
    private final View cotURLLayout;
    private final LinearLayout entries;
    private final AtakPreferences atakPreferences;
    private final static String TAG = "UrlUiHandler";

    private boolean disable_auto_links = false;

    /**
     * Handle the Range Range and Bearing table included in the v provided.
     * The range and bearing table is defined in rab_layout.xml.
     * @param parentView the parent view to use to find the url information
     */
    public UrlUiHandler(@NonNull
    final View parentView) {
        this.parentView = parentView;
        cotURLLayout = parentView.findViewById(R.id.cotURLLayout);
        if (cotURLLayout != null) {
            entries = cotURLLayout.findViewById(R.id.cotInfoURLEntries);
        } else {
            entries = null;
            Log.e(TAG,
                    "view makes use of UrlUiHandler but does not include the right layout.  this is an error",
                    new Exception());
        }
        atakPreferences = AtakPreferences.getInstance(
                MapView.getMapView().getContext());
        atakPreferences.registerListener(this);
    }

    /**
     * Must be called on the UI thread and updates the current display with information
     * from the marker.
     * @param marker the marker to be used during the update
     */
    public void update(final MapItem marker) {
        if (marker == null || cotURLLayout == null || entries == null) {
            return;
        }

        disable_auto_links = atakPreferences.get(DISABLE_AUTOLINK_PREF_KEY,
                false);

        List<String> urls = marker
                .getMetaStringArrayList(UrlLinkDetailEntry.ASSOCIATED_URLS_KEY);

        if (urls == null) {
            MapItem s = ShapeUtils.resolveShape(marker);
            if (s != null)
                urls = s.getMetaStringArrayList(
                        UrlLinkDetailEntry.ASSOCIATED_URLS_KEY);
        }

        // allocate the number of views required to display the urls passed in
        // this can grow to the maximum number of urls seen over session but the
        // views are recycled.
        if (urls != null) {
            while (entries.getChildCount() < urls.size()) {
                final View entry = LayoutInflater.from(parentView.getContext())
                        .inflate(R.layout.url_layout_entry, null);
                entries.addView(entry);
            }
        }

        boolean titleVisible = false;
        for (int i = 0; i < entries.getChildCount(); ++i) {
            final View entry = entries.getChildAt(i);
            final TextView cotInfoURLtext = entry
                    .findViewById(R.id.cotInfoURLtext);
            final TextView cotInfoURLtextNoAutoLink = entry
                    .findViewById(R.id.cotInfoURLtextNoAutoLink);
            final TextView cotInfoURLdesc = entry
                    .findViewById(R.id.cotInfoURLDescription);

            if (urls != null && i < urls.size()) {
                UrlLinkDetailEntry ulde = null;
                if (!FileSystemUtils.isEmpty(urls)) {
                    ulde = UrlLinkDetailEntry
                            .fromStringRepresentation(urls.get(i));
                }

                if (ulde == null || ulde.getUrl() == null) {
                    //Log.d(TAG, marker.getUID() + " Hiding linkUrl");
                    cotURLLayout.setVisibility(View.INVISIBLE);
                    cotInfoURLtext.setText("");
                    cotInfoURLtextNoAutoLink.setText("");

                } else {
                    //Log.d(TAG, marker.getUID() + " Displaying linkUrl: " + linkUrl);
                    titleVisible = true;

                    cotURLLayout.setVisibility(View.VISIBLE);
                    cotInfoURLtext.setText(ulde.getUrl());
                    cotInfoURLtextNoAutoLink.setText(ulde.getUrl());

                    entry.setVisibility(View.VISIBLE);
                    // Note: Setting the mask did not work //
                    if (disable_auto_links) {
                        cotInfoURLtext.setVisibility(View.GONE);
                        cotInfoURLtextNoAutoLink.setVisibility(View.VISIBLE);
                    } else {
                        cotInfoURLtext.setVisibility(View.VISIBLE);
                        cotInfoURLtextNoAutoLink.setVisibility(View.GONE);
                    }

                    if (FileSystemUtils.isEmpty(ulde.getRemark())) {
                        cotInfoURLdesc.setVisibility(View.GONE);
                        cotInfoURLdesc.setText("");
                    } else {
                        //Log.d(TAG, "Displaying linkRemarks: " + linkRemarks);
                        cotInfoURLdesc.setVisibility(View.VISIBLE);
                        cotInfoURLdesc.setText(ulde.getRemark());
                    }
                }

            } else {
                entry.setVisibility(View.GONE);
                cotInfoURLdesc.setText("");
                cotInfoURLtext.setText("");
                cotInfoURLtextNoAutoLink.setText("");
            }
        }
        cotURLLayout.setVisibility(titleVisible ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
            String key) {
        if (key == null)
            return;

        if (key.equals(DISABLE_AUTOLINK_PREF_KEY)) {
            disable_auto_links = sharedPreferences
                    .getBoolean(DISABLE_AUTOLINK_PREF_KEY, false);
            entries.post(new Runnable() {
                @Override
                public void run() {
                    for (int i = 0; i < entries.getChildCount(); ++i) {
                        final View entry = entries.getChildAt(i);
                        final TextView cotInfoURLtext = entry
                                .findViewById(R.id.cotInfoURLtext);
                        final TextView cotInfoURLtextNoAutoLink = entry
                                .findViewById(R.id.cotInfoURLtextNoAutoLink);

                        if (disable_auto_links) {
                            cotInfoURLtext.setVisibility(View.GONE);
                            cotInfoURLtextNoAutoLink
                                    .setVisibility(View.VISIBLE);
                        } else {
                            cotInfoURLtext.setVisibility(View.VISIBLE);
                            cotInfoURLtextNoAutoLink.setVisibility(View.GONE);
                        }
                    }

                }
            });

        }
    }

    @Override
    public void dispose() {
        atakPreferences.unregisterListener(this);
    }
}
