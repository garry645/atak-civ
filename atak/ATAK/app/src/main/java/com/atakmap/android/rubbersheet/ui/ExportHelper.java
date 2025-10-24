
package com.atakmap.android.rubbersheet.ui;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.rubbersheet.maps.AbstractSheet;
import com.atakmap.android.rubbersheet.maps.RubberImage;
import com.atakmap.android.rubbersheet.maps.RubberModel;

public class ExportHelper {

    /**
     * Given a abstract sheet, invoke the export workflow without having to copy and paste or
     * refactor existing code.
     * @param mapView the map view
     * @param abstractSheet the abstract sheet
     */
    public static void export(MapView mapView, AbstractSheet abstractSheet) {
        if (abstractSheet instanceof RubberImage) {
            RubberImageHierarchyListItem rihl = new RubberImageHierarchyListItem(
                    mapView,
                    (RubberImage) abstractSheet);
            rihl.promptExport();
        } else if (abstractSheet instanceof RubberModel) {
            RubberModelHierarchyListItem rihl = new RubberModelHierarchyListItem(
                    mapView,
                    (RubberModel) abstractSheet);
            rihl.promptExport();
        }
    }
}
