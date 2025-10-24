
package com.atakmap.android.devtools;

import com.atakmap.util.ConfigOptions;

final class DebugHitBoxes extends DevToolToggle {
    DebugHitBoxes() {
        super("Debug Hit Boxes", "debughitboxes");
    }

    @Override
    protected boolean isEnabled() {
        return ConfigOptions.getOption("debug-hit-boxes", 0) != 0;
    }

    @Override
    protected void setEnabled(boolean v) {
        ConfigOptions.setOption("debug-hit-boxes", v ? 1 : 0);
    }
}
