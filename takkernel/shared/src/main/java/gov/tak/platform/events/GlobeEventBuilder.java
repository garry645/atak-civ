package gov.tak.platform.events;

import java.util.ArrayList;
import java.util.List;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.api.events.IGlobeEvent;
import gov.tak.platform.graphics.PointF;

/**
 * Construct an IGlobeEvent a property at a time
 */
public class GlobeEventBuilder
{

    private String type;
    private Object source;
    private int globeId;
    private int pointerId = IGlobeEvent.MAIN_POINTER_ID;
    private int buttons = 0;
    private IGeoPoint globeCoordinate;
    private PointF viewPosition;
    private final List<Object> targetObjects = new ArrayList<>();
    private final List<IGeoPoint> targetCoordinates = new ArrayList<>();

    /**
     * Create a new IGlobeEvent instance with the current property values
     *
     * @return a new IGlobeEvent instance
     */
    public IGlobeEvent build()
    {
        // will throw if type is null as expected
        return new GlobeEvent(type, source, globeId, pointerId, buttons, globeCoordinate, viewPosition,
                targetObjects.toArray(), targetCoordinates.toArray(new IGeoPoint[targetCoordinates.size()]));
    }

    /**
     * Set the event type
     *
     * @param type a standard constant globe event type or a custom string
     * @return this builder
     */
    public GlobeEventBuilder setType(@NonNull String type)
    {
        this.type = type;
        return this;
    }

    /**
     * Set the event source object
     *
     * @param source the source object
     * @return this builder
     */
    public GlobeEventBuilder setSource(@NonNull Object source)
    {
        this.source = source;
        return this;
    }

    /**
     * Set the pointer id
     *
     * @param pointerId
     * @return this builder
     */
    public GlobeEventBuilder setPointerId(int pointerId)
    {
        this.pointerId = pointerId;
        return this;
    }

    /**
     * Set the buttons bitfield
     *
     * @param buttons bitfield of pressed buttons
     * @return this builder
     */
    public GlobeEventBuilder setButtons(int buttons)
    {
        this.buttons = buttons;
        return this;
    }

    /**
     * Set the button state
     *
     * @param button button index 0-32
     * @param down   the down state
     * @return this builder
     */
    public GlobeEventBuilder setButton(int button, boolean down)
    {
        if (button < 0 || button > 31)
            throw new ArrayIndexOutOfBoundsException();
        this.buttons |= ((down ? 1 : 0) << button);
        return this;
    }

    /**
     * Set the unique globe identifier
     *
     * @param globeId unique globe identifier
     * @return this builder
     */
    public GlobeEventBuilder setGlobeId(int globeId)
    {
        this.globeId = globeId;
        return this;
    }

    /**
     * Set the globe coordinate
     *
     * @param globeCoordinate the globe coordinate (or null)
     * @return this builder
     */
    public GlobeEventBuilder setGlobeCoordinate(IGeoPoint globeCoordinate)
    {
        this.globeCoordinate = globeCoordinate;
        return this;
    }

    /**
     * Set the view position
     *
     * @param viewPosition view position (or null)
     * @return this builder
     */
    public GlobeEventBuilder setViewPosition(PointF viewPosition)
    {
        this.viewPosition = viewPosition;
        return this;
    }

    /**
     * Add a target and coordinate
     *
     * @param object           target object
     * @param targetCoordinate target coordinate
     * @return this builder
     */
    public GlobeEventBuilder addTarget(Object object, IGeoPoint targetCoordinate)
    {
        targetObjects.add(object);
        targetCoordinates.add(targetCoordinate);
        return this;
    }

    /**
     * Add multiple target objects and coordinates
     *
     * @param targetObjects     the target objects
     * @param targetCoordinates the target coordinates
     * @return this builder
     */
    public GlobeEventBuilder addTargets(Object[] targetObjects, IGeoPoint[] targetCoordinates)
    {
        if (targetObjects.length != targetCoordinates.length)
            throw new IllegalArgumentException();
        for (int i = 0; i < targetObjects.length; ++i)
        {
            this.targetObjects.add(targetObjects[i]);
            this.targetCoordinates.add(targetCoordinates[i]);
        }
        return this;
    }

    /**
     * Clear all targets
     *
     * @return this builder
     */
    public GlobeEventBuilder clearTargets()
    {
        targetObjects.clear();
        targetCoordinates.clear();
        return this;
    }

    /**
     * Set the target objects and coordinates
     *
     * @param targetObjects     target objects
     * @param targetCoordinates target coordinates
     * @return this builder
     */
    public GlobeEventBuilder setTargets(Object[] targetObjects, IGeoPoint[] targetCoordinates)
    {
        return clearTargets()
                .addTargets(targetObjects, targetCoordinates);
    }
}
