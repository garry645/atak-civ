package gov.tak.api.ui;

import gov.tak.api.annotation.NonNull;
import gov.tak.api.marshal.IMarshal;
import gov.tak.platform.marshal.MarshalManager;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

abstract class PaneBuilderBase
{
    static Set<Class<?>> marshals = new HashSet<>();

    PaneImpl _pane;
    Map<String, Object> _hints = new HashMap<>();

    PaneBuilderBase(@NonNull Object impl, @NonNull Class<?> iface)
    {
        Objects.requireNonNull(impl, "Pane source object may not be null");
        Objects.requireNonNull(iface, "Pane source object type may not be null");

        _pane = new PaneImpl(impl);
        synchronized(marshals) {
            if(!marshals.contains(iface)) {
                final IMarshal marshal = new PaneMarshal(iface);
                marshals.add(iface);
                MarshalManager.registerMarshal(marshal, Pane.class, iface);
            }
        }
    }

    public final PaneBuilder setMetaValue(@NonNull String key, @NonNull Object value)
    {
        _pane.setMetaValue(key, value);
        return (PaneBuilder)this;
    }

    public final Pane build()
    {
        return _pane;
    }

    final class PaneImpl extends Pane
    {
        final Object _impl;

        PaneImpl(Object impl)
        {
            _impl = impl;
        }

        protected void setMetaValue(String key, Object value)
        {
            metadata.put(key, value);
        }
    }

    final class PaneMarshal implements IMarshal
    {

        final Class<?> _iface;

        PaneMarshal(Class<?> iface)
        {
            _iface = iface;
        }

        @Override
        public <T, V> T marshal(V in)
        {
            if(!(in instanceof PaneImpl))
                return null;

            PaneImpl pane = (PaneImpl) in;
            if(_iface.isAssignableFrom(pane._impl.getClass())) return (T)_iface.cast(pane._impl);
            else return null;
        }
    }
}
