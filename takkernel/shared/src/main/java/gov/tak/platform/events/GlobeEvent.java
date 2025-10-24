package gov.tak.platform.events;

import java.util.Arrays;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.events.IGlobeEvent;
import gov.tak.platform.graphics.PointF;

public class GlobeEvent implements IGlobeEvent
{

    private final String type;
    private final int globeId;
    private final Object source;
    private final int pointerId;
    private final int buttons;
    private final IGeoPoint globeCoordinate;
    private final PointF viewPosition;
    private final Object[] targetObjects;
    private final IGeoPoint[] targetCoordinates;
    private boolean handled;

    public GlobeEvent(@NonNull String eventType, @NonNull Object eventSource, int eventGlobeId, int eventPointerId,
                      int eventButtons, IGeoPoint eventGlobeCoordinate, PointF eventViewPosition,
                      Object[] eventTargetObjects, IGeoPoint[] eventTargetCoordinates)
    {

        type = eventType;
        source = eventSource;
        globeId = eventGlobeId;
        pointerId = eventPointerId;
        buttons = eventButtons;
        globeCoordinate = eventGlobeCoordinate;
        viewPosition = eventViewPosition;
        targetObjects = Arrays.copyOf(eventTargetObjects, eventTargetObjects.length);
        targetCoordinates = Arrays.copyOf(eventTargetCoordinates, eventTargetCoordinates.length);
    }

    @Override
    public String getType()
    {
        return type;
    }

    @Override
    public Object getSource()
    {
        return source;
    }

    @Override
    public boolean isHandled()
    {
        return handled;
    }

    @Override
    public void setHandled(boolean handled)
    {
        this.handled = handled;
    }

    @Override
    public int getGlobeId()
    {
        return globeId;
    }

    @Override
    public int getPointerId()
    {
        return pointerId;
    }

    @Override
    public int getButtons()
    {
        return buttons;
    }

    @Override
    public IGeoPoint getGlobeCoordinate()
    {
        return globeCoordinate;
    }

    @Override
    public PointF getViewPosition()
    {
        return viewPosition;
    }

    @Override
    public int getTargetCount()
    {
        return targetObjects.length;
    }

    @Override
    public Object getTargetObject(int targetIndex)
    {
        return targetObjects[targetIndex];
    }

    @Override
    public IGeoPoint getTargetCoordinate(int targetIndex)
    {
        IGeoPoint gp = targetCoordinates[targetIndex];
        if (gp == null)
            gp = globeCoordinate;
        return gp;
    }

    @Override
    public IGlobeEvent clone()
    {
        return new GlobeEvent(type, source, globeId, pointerId, buttons, globeCoordinate, viewPosition,
                targetObjects, targetCoordinates);
    }
}
