
package com.atakmap.android.maps;

import org.junit.Assert;

import gov.tak.api.events.IGlobeEvent;
import gov.tak.api.events.IGlobeEventService;

public class ExpectsGlobeEventListener implements IGlobeEventService.Listener {

    private final String expectedType;
    private int triggered;
    private IGlobeEvent lastGlobeEvent;

    public ExpectsGlobeEventListener(String type) {
        expectedType = type;
    }

    @Override
    public void onGlobeEvent(IGlobeEvent globeEvent) {
        ++triggered;
        lastGlobeEvent = globeEvent;
        Assert.assertEquals(expectedType, globeEvent.getType());
    }

    public int getTriggered() {
        return triggered;
    }

    public IGlobeEvent getLastGlobeEvent() {
        return lastGlobeEvent;
    }
}
