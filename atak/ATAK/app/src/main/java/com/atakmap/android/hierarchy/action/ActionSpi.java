
package com.atakmap.android.hierarchy.action;

public interface ActionSpi {
    /**
     * Given a command and a token create a concrete Action
     * @param command the command
     * @param token the token
     * @return the concrete action
     */
    Action create(String command, String token);
}
