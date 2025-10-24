package gov.tak.api.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.atakmap.coremap.log.Log;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import androidx.fragment.app.Fragment;
import gov.tak.api.annotation.NonNull;

public final class GenericFragmentAdapter extends Fragment
{
    final static Map<String, ViewFactory> _savedState = new HashMap<>();

    public static final String TAG = "GenericFragmentAdapter";

    ViewFactory _viewFactory;

    GenericFragmentAdapter()
    {
        this((ViewFactory)null);
    }

    public GenericFragmentAdapter(final View view)
    {
        this(new ViewFactory() {
            @Override
            public View createView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState)
            {
                return view;
            }

            @Override
            public void destroyView(View view)
            {
                // no-op; view is owned by caller
            }
        });
    }

    GenericFragmentAdapter(ViewFactory view)
    {
        _viewFactory = view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            final String uid = savedInstanceState.getString("viewuid");
            _viewFactory = _savedState.remove(uid);
            Log.d(TAG, "onCreate: fragment restored: " + uid);
        } else {
            Log.d(TAG, "onCreate: new fragment wrapper");
        }
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        final String uid = UUID.randomUUID().toString();
        Log.d(TAG,
                "onSaveInstanceState: saving the uid in case the fragment has not been destroyed: "
                        + uid);
        outState.putString("viewuid", uid);
        _savedState.put(uid, _viewFactory);
    }

    @Override
    public View onCreateView(@NonNull
                             final LayoutInflater inflater,
                             final ViewGroup container,
                             final Bundle savedInstanceState) {
        Log.d(TAG, "onCreateView: getting a fragment view");
        return _viewFactory != null ?
                _viewFactory.createView(inflater, container, savedInstanceState) : null;
    }

    @Override
    public void onDestroyView() {
        Log.d(TAG, "onDestroyView: destroying the fragment view");
        View view = getView();
        //Remove the view from the parent group prior to destroying the fragment.
        if (view != null) {
            ViewGroup parentViewGroup = (ViewGroup) view.getParent();
            if (parentViewGroup != null) {
                parentViewGroup.removeView(view);
            }
        }
        super.onDestroyView();
        _viewFactory.destroyView(view);
    }

    @Override
    public void onDestroy() {
        onDestroyView();
        super.onDestroy();
    }

    interface ViewFactory
    {
        View createView(final LayoutInflater inflater,
                        final ViewGroup container,
                        final Bundle savedInstanceState);
        void destroyView(View view);
    }
}

