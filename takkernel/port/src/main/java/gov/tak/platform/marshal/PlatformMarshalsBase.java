package gov.tak.platform.marshal;

import android.graphics.PorterDuff;

import gov.tak.api.commons.graphics.ColorBlendMode;
import gov.tak.api.marshal.IMarshalService;

class PlatformMarshalsBase
{
    private static Class[] marshaledTypes = new Class[]
    {
        gov.tak.api.commons.graphics.Bitmap.class,
    };

    public static void registerAll(IMarshalService svc)
    {
        // for all registered marshaled types, force static initialization for marshal registration via static blocks
        for(Class<?> marshaledType : marshaledTypes)
        {
            implicitRegistration(marshaledType);
        }

        svc.registerMarshal(new ColorBlendModeMarshal.Portable(), PorterDuff.Mode.class, ColorBlendMode.class);
        svc.registerMarshal(new ColorBlendModeMarshal.Platform(), ColorBlendMode.class, PorterDuff.Mode.class);
    }

    static void implicitRegistration(Class<?> marshaledType)
    {
        try
        {
            Class.forName(marshaledType.getName(), true, marshaledType.getClassLoader());
        }
        catch(Throwable ignored)
        {}
    }
}
