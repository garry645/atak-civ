package com.atakmap.android.radiolibrary;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Color;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.atakmap.android.network.TrafficRecorder;
import com.atakmap.android.preference.AtakPreferences;
import com.atakmap.android.util.SimpleItemSelectedListener;
import com.atakmap.android.video.AddEditAlias;
import com.atakmap.app.ATAKApplication;
import com.atakmap.app.R;
import com.atakmap.coremap.filesystem.FileSystemUtils;
import com.atakmap.coremap.locale.LocaleUtil;

import java.io.File;
import java.net.NetworkInterface;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.regex.Pattern;

import gov.tak.api.cot.CoordinatedTime;
import gov.tak.platform.lang.Parsers;

public class NetworkCaptureDialog {

    public static final Pattern PARTIAL_IP_ADDRESS = Pattern
            .compile(
                    "^((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9])\\.){0,3}"
                            + "((25[0-5]|2[0-4][0-9]|[0-1][0-9]{2}|[1-9][0-9]|[0-9]))?$");


    private static final String NETWORK_CAPTURE_DIALOG_ADDRESS_PREF = "network_capture_dialog_address_pref";
    private static final String NETWORK_CAPTURE_DIALOG_PORT_PREF = "network_capture_dialog_port_pref";

    private static final String TAG = "NetworkCaptureDialog";

    private final Context context;
    private final AtakPreferences prefs;

    public NetworkCaptureDialog(Context context) {
        this.context = context;
        prefs = AtakPreferences.getInstance(context);
    }

    private View createView() {
        final View view = LayoutInflater.from(context).inflate(R.layout.network_capture_layout, null);
        Spinner s = view.findViewById(R.id.networkSelector);
        // mirror the same list as is found in the Alias editor
        s.setAdapter(AddEditAlias.getNetworkDeviceAdapter(context));

        s.setOnItemSelectedListener(new SimpleItemSelectedListener() {

            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int pos, long id) {
                if (view instanceof TextView)
                    ((TextView) view).setTextColor(Color.WHITE);
            }

        });

        final Button record = view.findViewById(R.id.record_button);
        record.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View buttonView) {
                doRecord(view);
            }
        });

        final EditText address = view.findViewById(R.id.address);
        address.setText(prefs.get(NETWORK_CAPTURE_DIALOG_ADDRESS_PREF, ""));
        address.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i,
                    int i1, int i2) {
            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1,
                    int i2) {
            }

            private String previousText = "";

            @Override
            public void afterTextChanged(Editable s) {
                if (PARTIAL_IP_ADDRESS.matcher(s).matches())
                    previousText = s.toString();
                else
                    s.replace(0, s.length(), previousText);
            }
        });

        final EditText port = view.findViewById(R.id.port);
        port.setText(prefs.get(NETWORK_CAPTURE_DIALOG_PORT_PREF, ""));

        return view;
    }

   public void show() {
        final AlertDialog.Builder adb = new AlertDialog.Builder(context);
        adb.setTitle(R.string.record_udp_tool_title);
        adb.setView(createView());
        adb.show();
    }


    private void doRecord(final View view) {

        final EditText portView = view.findViewById(R.id.port);
        final EditText addressView = view.findViewById(R.id.address);

        final SimpleDateFormat recDateFormatter = new SimpleDateFormat(
                "yyyyMMMdd_HHmmss", Locale.US);

        final File f = FileSystemUtils.getItem("tools/network_recording");

        if (!f.mkdir()) {
            Log.e(TAG, "error creating a directory");
        }

        final String basename = "rec_"
                + recDateFormatter.format(CoordinatedTime.currentDate());
        final File raw = new File(f, basename + ".raw");

        final int port = Parsers.parseInt(portView.getText().toString(), -1);

        if (port > 0) {
            prefs.set(NETWORK_CAPTURE_DIALOG_PORT_PREF, Integer.toString(port));
        } else  {
            displayError(String.format(LocaleUtil.getCurrent(),
                    context.getString(R.string.failed_to_use_port_s), portView.getText()));
            return;
        }

        String address = addressView.getText().toString().trim();
        prefs.set(NETWORK_CAPTURE_DIALOG_ADDRESS_PREF, address);
        if (address.isEmpty())
            address = "239.255.0.1";

        Spinner s = view.findViewById(R.id.networkSelector);
        AddEditAlias.NetworkDeviceStringWrapper name =
                (AddEditAlias.NetworkDeviceStringWrapper)s.getSelectedItem();

        NetworkInterface ni = null;
        try {
            if (name.nd != null) {
                if ("-1".equals(name.nd.getInterfaceName())) {
                    ni = NetworkInterface.getByName("wlan0");
                } else {
                    ni = NetworkInterface.getByName(name.nd.getInterfaceName());
                }
            }
        } catch (Exception e) {
            displayError(String.format(LocaleUtil.getCurrent(),
                    context.getString(R.string.error_attach_network_iface), name));
        }

        TrafficRecorder tf = new TrafficRecorder(address,
                port, ni, raw, context);
        Thread t = new Thread(tf);
        t.start();
    }

    private void displayError(String message) {
        Log.e(TAG, message);
        Toast.makeText(ATAKApplication.getCurrentActivity(),
                message, Toast.LENGTH_SHORT).show();
    }


}
