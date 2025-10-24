
package com.atakmap.android.toolbar.menu;

import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import java.util.concurrent.ConcurrentLinkedQueue;

public class MenuButton {
    private final ConcurrentLinkedQueue<OnClickListener> listeners = new ConcurrentLinkedQueue<>();
    private ToolbarMenuManager manager = ToolbarMenuManager.getInstance();
    private Button _button;

    public MenuButton(IMenu parent, View view) {
        if (view instanceof Button) {
            this.initialize(parent, (Button) view);
        } else {
            throw new IllegalArgumentException();
        }
    }

    public Button getButton() {
        return _button;
    }

    public void dispose() {
        listeners.clear();
        manager = null;
        _button.setOnClickListener(null);
        _button = null;
    }

    private void initialize(final IMenu parent, final Button button) {
        _button = button;
        button.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                onClickListener(v);
            }
        });

        // register with mediator
        manager.register(parent, this);
    }

    /**
     * Allows for multiple listeners to be fired when a menu button is clicked.
     * @param listener the listener to be called.
     */
    public void addOnClickListener(final OnClickListener listener) {
        listeners.add(listener);
    }

    public void removeOnClickListener(final OnClickListener listener) {
        listeners.remove(listener);
    }

    private void onClickListener(View v) {
        // Since users get confused when they first tap the button and nothing happens,
        // let's keep it consistent and always just open the menu when you tap a button with a
        // menu.

        manager.closeMenu();
        // manager.setActiveToolbarButton(null);

        // relay onClick event
        for (OnClickListener l : listeners) {
            l.onClick(v);
        }

        // enable this button to show that is has been selected
        // this.setEnabled(false);
    }
}
