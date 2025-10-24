package gov.tak.api.cot.detail;

import com.atakmap.util.Collections2;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import gov.tak.api.annotation.NonNull;


/**
 * Hold roles, default and custom
 *
 */
public final class Role {

    final static Comparator<Role> defaultComparator = new Comparator<Role>() {
        @Override
        public int compare(Role a, Role b) {
            int cmp;

            final boolean aDefault = a.name.equals("Default");
            final boolean bDefault = b.name.equals("Default");
            if(aDefault == bDefault)
                cmp = 0;
            else if(aDefault)
                cmp = -1;
            else // bDefault
                return 1;

            cmp = a.name.compareToIgnoreCase(b.name);
            if(cmp != 0)
                return cmp;

            if(a.abbreviation != null && b.abbreviation != null)
                cmp = a.abbreviation.compareToIgnoreCase(b.abbreviation);
            else if(a.abbreviation != null && b.abbreviation == null)
                cmp = -1;
            else if(a.abbreviation == null && b.abbreviation != null)
                cmp = 1;
            else
                cmp = 0;
            if(cmp != 0)
                return cmp;

            return Collections2.compare(a.children, b.children, defaultComparator);
        }
    };

    private static final String TAG = "Role";

    List<Role> children = new ArrayList<>();
    String name;
    String abbreviation = null;
    String fallback = "Team Member";
    boolean useTypeIcon = false;

    Role parent = null;


    private Role() {}

    public static Role fromJSON(JSONObject object, Role parent) {
        final Role.Builder role = builderFromJSON(object);
        if(role == null)
            return null;
        role.setParent(parent);
        return role.build();
    }

    public static Role fromJSON(JSONObject object) {
        final Role.Builder role = builderFromJSON(object);
        return (role != null) ? role.build() : null;
    }

    static Builder builderFromJSON(JSONObject object) {
        final String name = object.optString("name", null);
        if(name == null)
            //doesn't have required attributes
            return null;
        final String abbreviation = object.optString("abbreviation", null);
        Builder role = new Builder(name, abbreviation);

        final String fallback = object.optString("fallback", null);
        if(fallback != null)
            role.setFallback(fallback);
        final boolean useTypeIcon = object.optBoolean("useTypeIcon", false);
        if(useTypeIcon)
            role.useTypeIcon();

        try {
            JSONArray children = object.getJSONArray("roles");

            for(int i=0;i < children.length();++i)
            {
                JSONObject child = children.getJSONObject(i);
                Builder childRole = builderFromJSON(child);
                if(childRole == null)
                    return null;
                role.addChild(childRole);
            }
        }
        catch(Exception e)
        {
            //no children
        }
        return role;
    }

    public String getName() { return name;}
    public String getAbbreviation() { return abbreviation;}
    public String getFallback() { return fallback; }
    public boolean useTypeIcon() { return useTypeIcon; }
    public List<Role> getChildren() {return children;}

    public Role getParent() {
        return parent;
    }

    public final static class Builder {
        Role impl;

        public Builder(@NonNull String name) {
            this(name, null);
        }
        public Builder(@NonNull String name, String abbrev) {
            Objects.nonNull(name);
            impl = new Role();
            impl.name = name;
            impl.abbreviation = abbrev;
        }

        public Builder setFallback(String fallback) {
            impl.fallback = fallback;
            return this;
        }
        public Builder useTypeIcon() {
            impl.useTypeIcon = true;
            return this;
        }
        public Builder setParent(Role parent) {
            impl.parent = parent;
            return this;
        }
        public Builder addChild(@NonNull String name, String abbrev) {
            return addChild(new Builder(name, abbrev));
        }
        public Builder addChild(@NonNull Builder childBuilder) {
            impl.children.add(childBuilder.setParent(impl).build());
            return this;
        }
        public Role build() {
            if(!impl.children.isEmpty())
                Collections.sort(impl.children, defaultComparator);
            return impl;
        }
    }
}
