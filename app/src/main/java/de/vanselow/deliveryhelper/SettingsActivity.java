package de.vanselow.deliveryhelper;

import android.content.DialogInterface;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.text.InputType;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

import java.util.Locale;

import de.vanselow.deliveryhelper.utils.Settings;

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
        builder.setTitle("RempteAccess Port");

        // Set up the input
        final EditText input = new EditText(this);
        // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        int port = Settings.getRemoteAccessPort(this);
        String portString = String.valueOf(port);
        input.setText(portString);
        input.setSelection(portString.length());
        builder.setView(input);

        // Set up the buttons
        builder.setPositiveButton(R.string.ok_button, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                int port;
                try {
                    port = Integer.parseInt(input.getText().toString());
                } catch (NumberFormatException e) {
                    port = Settings.REMOTEACCESS_PORT_DEFAULT;
                }
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

        builder.show();
    }
}
