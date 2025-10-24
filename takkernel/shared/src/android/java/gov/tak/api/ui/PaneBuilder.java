package gov.tak.api.ui;

import android.view.View;

import java.util.Map;
import java.util.WeakHashMap;

import androidx.fragment.app.Fragment;
import gov.tak.api.marshal.IMarshal;
import gov.tak.platform.marshal.MarshalManager;

public final class PaneBuilder extends PaneBuilderBase
{
    static
    {
        MarshalManager.registerMarshal(new ViewMarshal(), Pane.class, View.class);
    }

    static Map<Fragment, View> _views = new WeakHashMap<>();

    public PaneBuilder(Fragment pane)
    {
        super(pane, Fragment.class);
    }

    public PaneBuilder(View pane)
    {
        this(new GenericFragmentAdapter(pane));

        synchronized(_views)
        {
            _views.put(MarshalManager.marshal(_pane, Pane.class, Fragment.class), pane);
        }
    }

    final static class ViewMarshal implements IMarshal
    {
        @Override
        public <T, V> T marshal(V in) {
            Fragment fragment = MarshalManager.marshal((Pane) in, Pane.class, Fragment.class);
            synchronized(_views)
            {
                return (T) _views.get(fragment);
            }
        }
    }
}
