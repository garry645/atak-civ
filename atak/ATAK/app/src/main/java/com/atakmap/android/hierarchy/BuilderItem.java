
package com.atakmap.android.hierarchy;

import android.view.View;

import com.atakmap.android.hierarchy.action.Action;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;

class BuilderItem implements HierarchyListItem {
    String _title;
    String _uid;
    int _preferredListIndex;
    boolean _childSupported;
    List<HierarchyListItem> _children;
    String _iconUri;
    int _iconColor;
    Collection<Action> _actions;
    Object _userObject;
    View _extraView;
    Builder.OnRefreshListener _onRefreshListener;

    Map<String, Object> _localData;

    BuilderItem(String title) {
        _title = title;
        _uid = UUID.randomUUID().toString();
        _preferredListIndex = -1;
        _children = new ArrayList<>();
        _iconColor = -1;
        _actions = new HashSet<>();
        _localData = new HashMap<>();
    }

    BuilderItem(BuilderItem other) {
        _title = other._title;
        _uid = other._uid;
        _preferredListIndex = other._preferredListIndex;
        _childSupported = other._childSupported;
        _children = new ArrayList<>(other._children);
        _iconUri = other._iconUri;
        _iconColor = other._iconColor;
        _actions = new HashSet<>(other._actions);
        _userObject = other._userObject;
        _extraView = other._extraView;
        _onRefreshListener = other._onRefreshListener;
    }

    @Override
    public final String getUID() {
        return _uid;
    }

    @Override
    public final String getTitle() {
        return _title;
    }

    @Override
    public int getPreferredListIndex() {
        return _preferredListIndex;
    }

    @Override
    public int getChildCount() {
        return _children.size();
    }

    @Override
    public int getDescendantCount() {
        return getChildCount();
    }

    @Override
    public HierarchyListItem getChildAt(int index) {
        if (index < 0 || index >= _children.size())
            return null;
        return _children.get(index);
    }

    @Override
    public boolean isChildSupported() {
        return _childSupported;
    }

    @Override
    public String getIconUri() {
        return _iconUri;
    }

    @Override
    public int getIconColor() {
        return _iconColor;
    }

    @Override
    public Object setLocalData(String s, Object o) {
        return _localData.put(s, o);
    }

    @Override
    public Object getLocalData(String s) {
        return _localData.get(s);
    }

    @Override
    public <T> T getLocalData(String s, Class<T> clazz) {
        return (T) _localData.get(s);
    }

    @Override
    public <T extends Action> T getAction(Class<T> clazz) {
        for (Action action : _actions) {
            if (clazz.isAssignableFrom(action.getClass()))
                return (T) action;
        }
        return null;
    }

    @Override
    public Object getUserObject() {
        return _userObject;
    }

    @Override
    public View getExtraView() {
        return _extraView;
    }

    @Override
    public HierarchyListItem.Sort refresh(HierarchyListItem.Sort sortHint) {
        return (_onRefreshListener) != null
                ? _onRefreshListener.onRefresh(sortHint)
                : null;
    }
}
