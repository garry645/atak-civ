
package com.atakmap.android.location.framework;

import java.util.UUID;

public class TestLocationProvider extends LocationProvider {
    private final String UID = UUID.randomUUID().toString();
    private boolean isEnabled = false;
    private boolean isDisposed = false;

    @Override
    public String getUniqueIdentifier() {
        return UID;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public String getSourceCategory() {
        return null;
    }

    @Override
    public String getSource() {
        return null;
    }

    @Override
    public int getSourceColor() {
        return 0;
    }

    @Override
    public void setEnabled(boolean enabled) {
        isEnabled = enabled;
    }

    @Override
    public boolean getEnabled() {
        return isEnabled;
    }

    @Override
    public void dispose() {
        isDisposed = true;
    }

    public boolean isDisposed() {
        return isDisposed;
    }

    public void setLastLocation(Location location) {
        fireLocationChanged(location);
    }
}
