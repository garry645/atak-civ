package gov.tak.api.ui;

import gov.tak.platform.ui.MotionEvent;

public class ToolbarItemAdapter implements IToolbarItemListener {
    final static long LONG_PRESS_TIMEOUT = 500L;

    @Override
    public void onItemEvent(ToolbarItem item, MotionEvent event)
    {
        switch(event.getActionMasked())
        {
            case MotionEvent.ACTION_HOVER_MOVE:
                onHover(item);
                break;
            case MotionEvent.ACTION_UP:
                if((event.getEventTime()-event.getDownTime()) > LONG_PRESS_TIMEOUT)
                    onLongClick(item);
                else
                    onClick(item);
                break;
            default :
                return;
        }
    }

    public void onClick(ToolbarItem item)
    { }

    public void onLongClick(ToolbarItem item)
    { }

    public void onHover(ToolbarItem item)
    { }
}
