
package com.atakmap.android.drawing.details.msd;

public class ShapeMsdEntry {
    public final String title;
    public final double range;
    public final int color;
    public final String id;

    /**
     * The record for the Msd Entry
     * @param id the unique identifier for the msd entry
     * @param title the user title
     * @param range the range in meters
     * @param color the color as an integer 
     */
    public ShapeMsdEntry(String id, String title, double range, int color) {
        this.title = title;
        this.range = range;
        this.color = color;
        this.id = id;
    }
}
