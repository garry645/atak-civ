package com.atakmap.map.formats.c3dt;

import gov.tak.api.annotation.DontObfuscate;

@DontObfuscate
class ViewUpdateResults
{
    public Tile[] tilesToRenderThisFrame;
    public Tile[] tilesFadingOut;
    public int workerThreadTileLoadQueueLength;
    public int mainThreadTileLoadQueueLength;
    public int tilesVisited;
    public int culledTilesVisited;
    public int tilesCulled;
    public int tilesOccluded;
    public int tilesWaitingForOcclusionResults;
    public int tilesKicked;
    public int maxDepthVisited;
    public int frameNumber;
}
