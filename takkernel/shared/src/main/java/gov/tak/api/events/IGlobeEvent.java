package gov.tak.api.events;

import gov.tak.api.engine.map.coords.IGeoPoint;
import gov.tak.platform.graphics.PointF;

/**
 * Interface for event capturing change to globe interaction.
 */
public interface IGlobeEvent
{
    /**
     * A pointer changed to down/pressed state on the globe/target
     */
    String PRESS = "gov.tak.events.globe.PRESS";

    /**
     * A pointer changed to a released/up state on the globe/target
     */
    String RELEASE = "gov.tak.events.globe.RELEASE";

    /**
     * A pointer performed a click gesture on the globe/target
     */
    String CLICK = "gov.tak.events.globe.CLICK";

    /**
     * A pointer performed a double-click gesture on the globe/target
     */
    String DOUBLE_CLICK = "gov.tak.events.globe.DOUBLE_CLICK";

    /**
     * A pointer performed a long-press gesture on the globe/target
     */
    String LONG_PRESS = "gov.tak.events.globe.LONG_PRESS";

    /**
     * A pointer entered region on the globe/target
     */
    String POINTER_ENTER = "gov.tak.events.globe.POINTER_ENTER";

    /**
     * A pointer left a region on the globe/target
     */
    String POINTER_EXIT = "gov.tak.events.globe.POINTER_EXIT";

    /**
     * A pointer, device permitting, changed position on the globe/target while in a released/up
     * state
     */
    String POINTER_HOVER = "gov.tak.events.globe.POINTER_HOVER";

    /**
     * A pointer changed position on the globe/target while in a pressed/down state
     */
    String DRAGGING = "gov.tak.events.globe.DRAGGING";

    /**
     * The position of the camera defining the globe view has changed
     */
    String VIEW_CHANGING = "gov.tak.events.globe.VIEW_CHANGING";

    /**
     * Bit signifying left mouse-button involvement
     */
    int BUTTON_LEFT = 1;

    /**
     * Bit signifying right mouse-button involvement
     */
    int BUTTON_RIGHT = 2;

    /**
     * Bit signifying middle mouse-button involvement
     */
    int BUTTON_MIDDLE = 4;

    /**
     * The ID of the main pointer for the platform. This is the primary touch or mouse pointer
     * on various platforms.
     */
    int MAIN_POINTER_ID = 0;

    /**
     * The type globe event
     *
     * @return unique event type String
     */
    String getType();

    /**
     * The source object the event originated
     *
     * @return the event source
     */
    Object getSource();

    /**
     * If the application has handled the globe event
     *
     * @return true if handled
     */
    boolean isHandled();

    /**
     * Set whether the application has handled the globe event
     *
     * @param handled true if handled
     */
    void setHandled(boolean handled);

    /**
     * Get a number that uniquely identifies the pointer involved in the globe interaction. This may
     * be a mouse cursor or touch depending on the platform. If the pointing device has buttons,
     * getButtons() returns a bitfield describing the press/release states of each button.
     *
     * @return unique pointer identifier int
     */
    int getPointerId();

    /**
     * Get a bitfield which signifies button involvement. For DRAG events, this is all pressed
     * buttons. For PRESS and RELEASE events this is just the button involved (if any).
     *
     * @return bitfield of involved buttons
     */
    int getButtons();

    /**
     * Get the globe coordinate of the globe interaction
     *
     * @return ILocation instance of globe coordinate
     */
    IGeoPoint getGlobeCoordinate();

    /**
     * Get the unique number that distinguishes which globe the interaction is associated
     *
     * @return unique globe ID
     */
    int getGlobeId();

    /**
     * Get the position in view space of the globe interaction
     *
     * @return PointF instance of view position
     */
    PointF getViewPosition();

    /**
     * Get the number of targets associated with the event. Targets are ordered descending as
     * defined by the implemented hit detection.
     *
     * @return number of targets
     */
    int getTargetCount();

    /**
     * Get the object at the target index. Targets are ordered descending as defined by the
     * implemented hit detection.
     *
     * @param targetIndex the index of the target
     * @return the object at the given target index
     * @throws IndexOutOfBoundsException if targetIndex index is outside the array index
     *                                   bounds with an array length of getTargetCount()
     */
    Object getTargetObject(int targetIndex);

    /**
     * Get the coordinate at the target index. Targets are ordered descending as defined by the
     * implemented hit detection.
     *
     * @param targetIndex the index of the target
     * @return ILocation instance at the given target index
     * @throws IndexOutOfBoundsException if targetIndex index is outside of the array index
     *                                   bounds with an array length of getTargetCount()
     */
    IGeoPoint getTargetCoordinate(int targetIndex);

    /**
     * Create a copy of this globe event that is guaranteed to be outside of any recycling
     * scheme and safe to cache.
     *
     * @return copy of the globe event
     */
    IGlobeEvent clone();
}
