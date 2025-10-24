
package com.atakmap.android.toolbar;

import android.graphics.drawable.Drawable;

import com.atakmap.android.tools.ActionBarView;

import java.util.List;

import gov.tak.api.annotation.DeprecatedApi;
import gov.tak.api.annotation.DontObfuscate;

public interface IToolbarExtension2 {

    /***
     * Implement this method show the appropriate active tool icon in the toolbar.
     * @return the drawable associated with the toolbar icon.
     */
    Drawable getToolbarIcon();

    /***
     * Implement this method to return a set of tools to be managed by ToolbarLibrary.
     *
     * @return A list of tools
     * @deprecated Use getToolbarView() instead.
     */
    @Deprecated
    @DeprecatedApi(since = "4.1", forRemoval = true, removeAt = "4.4")
    @DontObfuscate
    List<Tool> getTools();

    /***
     * Implement this method to return the view that should be placed in the toolbar drawer when
     * your component has control of it.
     *
     * @return the action bar view for the tool
     */
    @DontObfuscate
    ActionBarView getToolbarView();

    /***
     * Implement this method to tell ToolbarLibrary whether your component will sometimes take
     * control of the toolbar's contents or whether it only implements tools without implementing a
     * toolbar.
     *
     * @return if it has a corresponding tool bar
     */
    @DontObfuscate
    boolean hasToolbar();

    @DontObfuscate
    void onToolbarVisible(final boolean vis);
}
