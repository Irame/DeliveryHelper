package de.vanselow.deliveryhelper;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.SwitchCompat;
import android.widget.CompoundButton;

public class SettingsActivity extends AppCompatActivity implements CompoundButton.OnCheckedChangeListener {
    public static final String SHARED_PREFERENCES_NAME = "DeliveryHelperGeneralSettings";
    public static final String AUTOSORT_OPTION_PREFKEY = "autosortOption";

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        SwitchCompat autosortSwitch = (SwitchCompat) findViewById(R.id.settings_autosort_open_switch);
        if (autosortSwitch != null) {
            autosortSwitch.setChecked(getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).getBoolean(AUTOSORT_OPTION_PREFKEY, false));
            autosortSwitch.setOnCheckedChangeListener(this);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.settings_autosort_open_switch:
                SharedPreferences.Editor editor = getSharedPreferences(SHARED_PREFERENCES_NAME, MODE_PRIVATE).edit();
                editor.putBoolean(AUTOSORT_OPTION_PREFKEY, isChecked);
                editor.apply();
                break;
        }
    }
}
