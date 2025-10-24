package gov.tak.platform.marshal;

import gov.tak.api.commons.graphics.Drawable;
import gov.tak.api.marshal.IMarshalService;

final class PlatformMarshals extends PlatformMarshalsBase
{

    private PlatformMarshals()
    {
    }

    public static void registerAll(IMarshalService svc)
    {
        PlatformMarshalsBase.registerAll(svc);
        implicitRegistration(Drawable.class);
        svc.registerMarshal(new MotionEventMarshal.Portable(), android.view.MotionEvent.class, gov.tak.platform.ui.MotionEvent.class);
        svc.registerMarshal(new MotionEventMarshal.Platform(), gov.tak.platform.ui.MotionEvent.class, android.view.MotionEvent.class);
    }
}
