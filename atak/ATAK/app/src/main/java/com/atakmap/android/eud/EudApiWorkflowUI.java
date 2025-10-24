
package com.atakmap.android.eud;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.atakmap.android.dropdown.DropDownReceiver;
import com.atakmap.android.maps.MapView;
import com.atakmap.app.R;

abstract class EudApiWorkflowUI {
    final Context _context;
    final View dropdownView;
    final TextView instructionsTextView;
    final TextView registrationUrlTextView;
    final TextView deviceCodeTextView;
    final TextView expirationTextView;
    final LinearLayout eudActiveWorkflowLinearView;
    final ProgressBar eudConnectingProgressBar;
    final ProgressBar expirationProgressBar;

    EudApiWorkflowUI(Context context) {
        _context = context;

        final LayoutInflater inflater = LayoutInflater.from(context);
        dropdownView = inflater.inflate(R.layout.eud_main_layout, null);
        instructionsTextView = dropdownView
                .findViewById(R.id.eudInstructionsTextView);
        registrationUrlTextView = dropdownView
                .findViewById(R.id.eudRegistrationUrlTextView);
        deviceCodeTextView = dropdownView
                .findViewById(R.id.eudDeviceCodeTextView);
        expirationTextView = dropdownView
                .findViewById(R.id.eudExpirationTextView);
        expirationProgressBar = dropdownView
                .findViewById(R.id.eudExpirationProgressBar);
        eudConnectingProgressBar = dropdownView
                .findViewById(R.id.eudConnectingProgressBar);
        eudActiveWorkflowLinearView = dropdownView
                .findViewById(R.id.eudActiveWorkflowLinearView);
    }

    abstract void show();

    abstract void dismiss();

    abstract boolean isDismissed();

    final void setInstructionsText(String s) {
        instructionsTextView.setText(s);
    }

    public void setInstructionsText(int resid) {
        instructionsTextView.setText(resid);
    }

    final void setRegistrationUrlText(String s) {
        registrationUrlTextView.setText(s);
    }

    public void setRegistrationUrlText(int resid) {
        registrationUrlTextView.setText(resid);
    }

    public void setConnectingProgressBarVisibility(int visible) {
        eudConnectingProgressBar.setVisibility(visible);
    }

    public void setEudActiveWorkflowViewVisible(int visible) {
        eudActiveWorkflowLinearView.setVisibility(visible);
    }

    /**
     * Allows for the device code and the verification Uri to be set on the active drop down screen
     * @param deviceCode the device code
     * @param verificationUri the verification uri
     */
    public void setDeviceCodeText(String deviceCode, String verificationUri) {
        deviceCodeTextView.setText(deviceCode);
        deviceCodeTextView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                try {
                    if (verificationUri != null) {
                        final Uri uri = Uri.parse(verificationUri);
                        final Intent intent = new Intent(Intent.ACTION_VIEW,
                                uri);
                        view.getContext().startActivity(intent);
                    }
                } catch (Exception ignore) {
                    // browser launch fail
                }
            }
        });
    }

    public void setExpirationText(String s) {
        expirationTextView.setText(s);
    }

    public void setExpirationBarProgress(int i) {
        expirationProgressBar.setProgress(i);
    }

    final static class Dropdown extends EudApiWorkflowUI {

        DropDownReceiver _owner;
        boolean _dismissed;

        Dropdown(DropDownReceiver owner) {
            super(owner.getMapView().getContext());

            _owner = owner;
        }

        @Override
        void show() {
            _owner.showDropDown(dropdownView, 0.5, 1.0, 1.0, 0.5);
        }

        @Override
        void dismiss() {
            _owner.closeDropDown();
        }

        @Override
        boolean isDismissed() {
            return _owner.isClosed();
        }
    }

    final static class Dialog extends EudApiWorkflowUI {

        AlertDialog _dialog;
        boolean _dismissed;

        Dialog(Context context) {
            super(context);

            _dismissed = false;
        }

        @Override
        void show() {
            if (_dialog == null) {
                _dialog = new AlertDialog.Builder(_context)
                        .setTitle(R.string.eud_prefs_link_dialog_title)
                        .setView(dropdownView)
                        .setNegativeButton(R.string.dismiss,
                                new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(
                                            DialogInterface dialogInterface,
                                            int i) {
                                        _dialog.dismiss();
                                    }
                                })
                        .setOnDismissListener(
                                new DialogInterface.OnDismissListener() {
                                    @Override
                                    public void onDismiss(
                                            DialogInterface dialogInterface) {
                                        _dismissed = true;
                                    }
                                })
                        .create();
            }

            MapView.getMapView().postOnActive(new Runnable() {
                @Override
                public void run() {
                    _dialog.show();
                }
            });
        }

        @Override
        void dismiss() {
            if (_dialog != null)
                _dialog.dismiss();
        }

        @Override
        boolean isDismissed() {
            return _dismissed;
        }
    }
}
