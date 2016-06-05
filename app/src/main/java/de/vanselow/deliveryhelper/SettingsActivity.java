package de.vanselow.deliveryhelper;

import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

import de.vanselow.deliveryhelper.utils.Settings;

import static de.vanselow.deliveryhelper.utils.Utils.isValidPort;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SwitchCompat autosortSwitch = (SwitchCompat) findViewById(R.id.settings_autosort_open_switch);
        if (autosortSwitch != null) {
            autosortSwitch.setChecked(Settings.isAutosortForOpenLocationsEnabled(this));
            autosortSwitch.setOnCheckedChangeListener(this);
        }

        SwitchCompat remoteaccessSwitch = (SwitchCompat) findViewById(R.id.settings_remoteaccess_enabled_switch);
        if (remoteaccessSwitch != null) {
            remoteaccessSwitch.setChecked(Settings.isRemoteAccessEnabled(this));
            remoteaccessSwitch.setOnCheckedChangeListener(this);
        }

        updateHostPortLabel();
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.settings_autosort_open_switch:
                Settings.setAutosortForOpenLocations(this, isChecked);
                break;
            case R.id.settings_remoteaccess_enabled_switch:
                Settings.setRemoteAccess(this, isChecked);
        }
    }

    private void updateHostPortLabel() {
        TextView hostPortLabel = (TextView) findViewById(R.id.settings_host_port_label);
        if (hostPortLabel == null) return;

        WifiManager wifiManager = (WifiManager) getSystemService(WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();
        int ipAddress = wifiInfo.getIpAddress();
        String host = String.format(Locale.ENGLISH, "%d.%d.%d.%d",
                (ipAddress & 0xff), (ipAddress >> 8 & 0xff),
                (ipAddress >> 16 & 0xff), (ipAddress >> 24 & 0xff));
        int port = Settings.getRemoteAccessPort(this);
        hostPortLabel.setText(String.format("%s:%d", host, port));
    }

    public void remoteAccessPortOnClick(View view) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.remote_access_port);

        View dialogView = getLayoutInflater().inflate(R.layout.dialog_settings_port, null);

        final EditText input = (EditText) dialogView.findViewById(R.id.dialog_settings_port_input);
        input.setHint(String.valueOf(Settings.REMOTEACCESS_PORT_DEFAULT));
        String portString = String.valueOf(Settings.getRemoteAccessPort(this));
        input.setText(portString);
        input.setSelection(portString.length());

        builder.setView(dialogView);

        builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int port = parsePort(input.getText().toString());
                if (Settings.setRemoteAccessPort(getApplicationContext(), port))
                    updateHostPortLabel();
                dialog.dismiss();
            }
        });
        builder.setNegativeButton(R.string.cancel_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });

        final AlertDialog dialog = builder.create();

        input.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(isValidPort(parsePort(s.toString())));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        dialog.show();
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    private int parsePort(String portString) {
        int port;
        if (portString.isEmpty())
            port = Settings.REMOTEACCESS_PORT_DEFAULT;
        else {
            try {
                port = Integer.parseInt(portString);
            } catch (NumberFormatException e) {
                port = -1;
            }
        }
        return port;
    }
}
