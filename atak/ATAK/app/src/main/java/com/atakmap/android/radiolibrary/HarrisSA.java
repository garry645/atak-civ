
package com.atakmap.android.radiolibrary;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.maps.MapView;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.app.R;
import com.atakmap.comms.NetworkManagerLite;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;
import com.atakmap.coremap.log.Log;
import com.atakmap.coremap.maps.time.CoordinatedTime;

import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.TimeZone;

import gov.tak.api.util.Disposable;

public class HarrisSA implements Disposable {
    private final HarrisHelper falconSA;
    private final HarrisConfigurator harrisConfig;
    private final View _layout;
    private final String STATE_DISCONNECTING;
    private final String STATE_CONNECTING;
    private static final int POLL_TIME = 2500;

    private final Context con;
    private CheckThread checkThread;
    private final TextView radioState;
    private final Switch prcSwitch;
    private final MapView mapView;
    private final HarrisSaRadioManager harrisSaRadioManager;

    public static final String TAG = "HarrisSA";

    public HarrisSA(final MapView mapView) {
        this.mapView = mapView;
        con = mapView.getContext();

        harrisSaRadioManager = new HarrisSaRadioManager(con);
        harrisConfig = new HarrisConfigurator(con, harrisSaRadioManager);
        STATE_DISCONNECTING = con.getString(R.string.radio_state_disconnecting);
        STATE_CONNECTING = con.getString(R.string.radio_state_connecting);

        falconSA = new HarrisHelper(con, harrisSaRadioManager);
        LayoutInflater inf = LayoutInflater.from(con);
        _layout = inf.inflate(R.layout.radio_item_harris_sa, null);

        InetAddress address = falconSA.getAddress();
        boolean active = ((address != null) || falconSA.isStarted());
        Log.d(TAG, "ppp connection state:" + active);
        radioState = _layout.findViewById(R.id.prc152_state);
        if (address != null) {
            changeStateText(con.getString(R.string.radio_online)
                    + address.getHostAddress());
        } else if (falconSA.isStarted()) {
            changeStateText(STATE_CONNECTING);
        }

        prcSwitch = _layout.findViewById(R.id.prc152_switch);
        prcSwitch.setChecked(active);
        if (!active)
            initPRCLine();

        ImageButton prccfg = _layout
                .findViewById(R.id.prc152_cfg_btn);
        prccfg.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                showPRCCableConfig();
            }

        });

        prcSwitch
                .setOnCheckedChangeListener(new OnCheckedChangeListener() {
                    @Override
                    public void onCheckedChanged(
                            CompoundButton buttonView, boolean isChecked) {
                        if (isChecked) {
                            checkPPPThread(true);
                        } else {
                            checkPPPThread(false);
                            initPRCLine();
                        }

                    }
                });

    }

    private void initPRCLine() {
        if (!falconSA.isModelSupported()) {
            prcSwitch.setEnabled(false);
            if (!falconSA.isRooted()) {
                changeStateText(String.format(con.getString(
                        R.string.radio_device_not_rooted),
                        falconSA.getModelName()));
            } else {
                changeStateText(String.format(con.getString(
                        R.string.radio_device_unsupported),
                        falconSA.getModelName()));
            }
        } else {
            changeStateText(String.format(con.getString(
                    R.string.radio_device_might_be_supported),
                    falconSA.getModelName()));
        }

    }

    private void showPRCCableConfig() {

        try {
            harrisSaRadioManager.addListener(queryListener);
            harrisSaRadioManager.queryRadios();
        } catch (Exception ignored) {
        }
    }

    synchronized public void checkPPPThread(boolean enable) {
        if ((checkThread == null) && enable) {
            checkThread = new CheckThread();
            checkThread.start();
        } else if ((checkThread != null) && (!enable)) {
            checkThread.cancel();
        }

    }

    /**
     * Responsible for setting the radiostate text view to the msg specified.
     */
    void changeStateText(final int color, final String msg) {
        mapView.post(new Runnable() {
            @Override
            public void run() {
                radioState.setTextColor(color);
                radioState.setText(msg);
            }
        });

    }

    void changeStateText(final String msg) {
        changeStateText(Color.WHITE, msg);
    }

    class CheckThread extends Thread {
        final static int numRetries = 10;
        volatile boolean cancelled = false;
        boolean defRouteAlreadySet = false;

        public void cancel() {
            cancelled = true;
            falconSA.stop();
            interrupt();
            changeStateText(STATE_DISCONNECTING);
            synchronized (this) {
                checkThread = null;
            }
        }

        public void off() {
            // set the switch to false
            mapView.post(new Runnable() {
                @Override
                public void run() {
                    prcSwitch.setChecked(false);
                }
            });
            synchronized (this) {
                checkThread = null;
            }
        }

        @Override
        public void run() {
            falconSA.start();
            defRouteAlreadySet = false;

            if (!initialCheck() && !cancelled) {
                // number of retries exceeded
                changeStateText(con
                        .getString(R.string.radio_connection_attempt_exceeded));
                off();
                return;
            }

            while (!cancelled) {
                final InetAddress address = falconSA.getAddress();
                if (address == null) {
                    SimpleDateFormat dateFormatGmt = new SimpleDateFormat(
                            "HH:mm:ss", LocaleUtil.getCurrent());
                    dateFormatGmt.setTimeZone(TimeZone.getTimeZone("UTC"));
                    changeStateText(Color.RED, con
                            .getString(R.string.radio_loss_connection)
                            + dateFormatGmt.format(CoordinatedTime
                                    .currentDate()));
                } else {
                    changeStateText(Color.GREEN,
                            con.getString(R.string.radio_online)
                                    + address.getHostAddress());

                    AtakPreferences _controlPrefs = AtakPreferences
                            .getInstance(con);
                    boolean setDefRoute = _controlPrefs.get("prc_defroute",
                            false);
                    if (!defRouteAlreadySet && setDefRoute) {
                        defRouteAlreadySet = true;
                        NetworkManagerLite.configure("ppp0",
                                address.getHostAddress(), "255.255.255.0",
                                address.getHostAddress(), true, null);

                    }
                }

                // wait
                try {
                    Thread.sleep(POLL_TIME);
                } catch (InterruptedException ignored) {
                }

            }

        }

        boolean initialCheck() {
            for (int count = 0; count < numRetries * 2; ++count) {
                try {
                    Thread.sleep(POLL_TIME);
                } catch (InterruptedException ignored) {
                }
                if (cancelled) {
                    return false;
                }

                final InetAddress address = falconSA.getAddress();
                if (!FileSystemUtils.isEmpty(falconSA.getErrorReason())) {
                    changeStateText(Color.RED, falconSA.getErrorReason());
                } else if (address == null) {
                    // the below divisions were added because the polling time
                    // is twice as fast as it was in prior versions of ATAK
                    changeStateText(Color.YELLOW, STATE_CONNECTING
                            + con.getString(R.string.radio_retry_)
                            + (count / 2)
                            + "/"
                            + (numRetries));
                } else {
                    changeStateText(Color.GREEN,
                            con.getString(R.string.radio_online)
                                    + address.getHostAddress());
                    return true;
                }
            }
            return false;
        }
    }

    public View getView() {
        return _layout;
    }

    @Override
    public void dispose() {
        falconSA.stop();
        checkPPPThread(false);
        harrisSaRadioManager.dispose();
        falconSA.dispose();
        harrisConfig.dispose();
    }

    private final RadiosQueriedListener queryListener = new RadiosQueriedListener() {
        @Override
        public void radiosQueried(List<HarrisRadio> radios) {
            harrisSaRadioManager.removeListener(this);
            if (!radios.isEmpty()) {
                harrisSaRadioManager.setSelectedRadio(radios.get(0));
                launchConfigDialog();
            } else {
                Toast.makeText(con, con.getString(R.string.could_not_find_a,
                        "Harris PRC-152A"), Toast.LENGTH_SHORT).show();
            }
        }
    };

    private void launchConfigDialog() {

        final AtakPreferences _controlPrefs = AtakPreferences.getInstance(con);
        // inflate the layout
        LayoutInflater inflater = LayoutInflater.from(con);
        final View l = inflater.inflate(R.layout.prc_radio_cfg, null);

        final CheckBox cblr = l.findViewById(R.id.prc_localreport);
        cblr.setChecked(_controlPrefs.get("prc_localreport", false));
        cblr.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {

                _controlPrefs.set("prc_localreport", cblr.isChecked());
            }
        });

        final Button cfg_push = l.findViewById(R.id.prc_cfg_push);
        cfg_push.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                final String callsign = MapView.getMapView().getSelfMarker()
                        .getMetaString("callsign", "");
                harrisConfig.configure(callsign, cblr.isChecked());
            }
        });

        final TextView tv = l.findViewById(R.id.prc_cfg_info);
        tv.setText(con.getString(R.string.radio_prc_152a_details1));

        final CheckBox cb = l.findViewById(R.id.prc_defroute);
        cb.setChecked(_controlPrefs.get("prc_defroute", false));

        cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView,
                    boolean isChecked) {
                _controlPrefs.set("prc_defroute", cb.isChecked());
            }
        });

        AlertDialog.Builder ad = new AlertDialog.Builder(con);
        ad.setTitle(R.string.radio_cable_configuration);
        ad.setPositiveButton(R.string.dismiss, null);
        ad.setView(l);
        mapView.post(new Runnable() {
            @Override
            public void run() {
                final AlertDialog dialog = ad.create();
                dialog.show();
            }
        });
    }

}
