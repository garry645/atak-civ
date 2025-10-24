package gov.tak.api.cot.detail;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Contain and manage Roles, default and custom
 *
 */
public final class RolesManager {
    final static Role _defaultRoles = new Role.Builder("Default")
                .addChild("Team Member", "TM")
                .addChild("Team Lead","TL")
                .addChild("HQ", "HQ")
                .addChild("Sniper", "S")
                .addChild("Medic", "M")
                .addChild("Forward Observer", "FO")
                .addChild("RTO", "R")
                .addChild("K9", "K9")
            .build();

    private static final String TAG = "RolesManager";


    List<Role> roles = new ArrayList<>();
    Map<String, Map<String, Role>> rolesIndex = new HashMap<>();

    public RolesManager() {
        roles.add(_defaultRoles);
        index(_defaultRoles);
    }

    public synchronized boolean isDefault(Role role)
    {
        for(Role child : _defaultRoles.getChildren())
        {
            if(Objects.equals(child.getName(), role.getName()) && Objects.equals(child.getAbbreviation(), role.getAbbreviation()))
                return true;
        }
        return false;
    }

    /**
     * Return a list of all of the top level roles.
     * @return an unmodifiable list of roles
     */
    public synchronized List<Role> getRoles() {
        return Collections.unmodifiableList(roles);
    }

    /**
     * Remove a role from the manager by matching the name.
     * @param name the top level name of the roles
     * @return true if the remove was successful
     */
    public synchronized boolean removeRoles(String name) {

        if (_defaultRoles.name.equals(name))
            return false;

        Iterator<Role> iter  = roles.iterator();
        while(iter.hasNext()) {
            final Role r = iter.next();
            if (Objects.equals(r.name, name)) {
                deindex(r);
                iter.remove();
                return true;
            }
        }
        return false;
    }


    private void index(Role r) {
        if(r.children.isEmpty()) {
            Map<String, Role> abbr = rolesIndex.get(r.name);
            if(abbr == null)
                rolesIndex.put(r.name, abbr=new HashMap<>());
            abbr.put(r.abbreviation, r);
        } else {
            for(Role c : r.children)
                index(c);
        }
    }

    private void deindex(Role r) {
        if(r.children.isEmpty()) {
            Map<String, Role> abbr = rolesIndex.get(r.name);
            if(abbr == null)
                return;
            abbr.remove(r.abbreviation);
            if(abbr.isEmpty())
                rolesIndex.remove(r.name);
        } else {
            for(Role c : r.children)
                deindex(c);
        }
    }



    /**
     * Add a role from the manager by matching the name.
     * @param r the role to be added and indexed, if the role name previously exists
     *          it is removed before the addition.
     */
    public synchronized void addRoles(Role r) {
        if (_defaultRoles.name.equals(r.name))
            return;

        removeRoles(r.name);

        roles.add(r);
        index(r);
        Collections.sort(roles, Role.defaultComparator);
    }


    /**
     * Given a name and an abbreviation, return the role object to be used
     * @param name the name of the role
     * @param abbreviation the abbreviation
     * @return the matching role
     */
    public synchronized Role getRole(String name, String abbreviation) {
        Map<String, Role> abbr = rolesIndex.get(name);
        if(abbr == null)
            return null;
        return abbr.get(abbreviation);
    }

    /**
     * Given search criteria, return the list of matches.
     * @param search the plaintext string used in a simple string contains search
     * @return the list of matching roles
     */
    public synchronized List<Role> search(String search)
    {
        List<Role> results = new ArrayList<>();
        for (Role role : this.getRoles()) {
            search(search.toLowerCase(), role, results);
        }
        return results;
    }

    void search(String search, Role currentRole, List<Role> results)
    {
        if(currentRole.getName().toLowerCase().contains(search))
            results.add(currentRole);

        for (Role role: currentRole.getChildren()) {
            search(search, role, results);
        }
    }

    public synchronized boolean importRoles(InputStream stream) {
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }

            JSONObject jsonObject = new JSONObject(sb.toString());

            Role role = Role.fromJSON(jsonObject);

            if(role == null)
                return false;

            addRoles(role);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private class IconKey
    {
        public String role, baseIconURI;

        public IconKey(String role, String baseIconURI)
        {
            this.role = role;
            this.baseIconURI = baseIconURI;
        }

        @Override
        public int hashCode() {
            return role.hashCode()
                    ^ baseIconURI.hashCode();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            IconKey iconKey = (IconKey) o;
            return Objects.equals(role, iconKey.role) && Objects.equals(baseIconURI, iconKey.baseIconURI);
        }
    }

    Map<IconKey, Bitmap> iconMap = new HashMap<>();

    public Bitmap getIcon(String abbreviation, String baseIconURI, Context context)
    {
        IconKey key = new IconKey(abbreviation, baseIconURI);
        Bitmap ret = iconMap.get(key);
        if(ret == null)
        {
            ret = createIcon(abbreviation, baseIconURI, context);
            iconMap.put(key, ret);
        }
        return ret;
    }

    private Bitmap createIcon(String abbreviation, String baseIconURI, Context context)
    {
        Bitmap bitmap = null;

        try {

            InputStream in = context.getAssets().open(baseIconURI);
            bitmap = BitmapFactory.decodeStream(in);
            in.close();
        }
        catch (Exception e)
        {
            return null;
        }

        Bitmap mutableBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true);
        Canvas canvas = new Canvas(mutableBitmap);

        Paint paint = new Paint();

        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
        paint.setColor(Color.BLACK);
        paint.setTextSize(12);
        canvas.drawText(abbreviation, 0, 0, paint);

        // draw text to the Canvas center
        int width = (int)paint.measureText(abbreviation, 0, abbreviation.length());
        int x = (bitmap.getWidth() - width)/2;
        int y = (bitmap.getHeight() + 12/2)/2;

        canvas.drawText(abbreviation, x, y, paint);

        return mutableBitmap;
    }
}
