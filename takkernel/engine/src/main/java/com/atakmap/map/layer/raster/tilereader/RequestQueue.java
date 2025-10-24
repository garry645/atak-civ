package com.atakmap.map.layer.raster.tilereader;

import java.util.ArrayList;

class RequestQueue
{
    double maxCost;
    boolean prefetch;
    ArrayList<TileReader.ReadRequest> queue;

    public RequestQueue(double v, boolean p)
    {
        this.maxCost = v;
        this.prefetch = p;
        this.queue = new ArrayList<>();
    }
}