package de.vanselow.deliveryhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.GooglePlayServicesNotAvailableException;
import com.google.android.gms.common.GooglePlayServicesRepairableException;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.internal.PlaceImpl;
import com.google.android.gms.location.places.ui.PlaceAutocomplete;

import java.util.Calendar;

public class LocationAddActivity extends AppCompatActivity {
    private static final String TAG = LocationAddActivity.class.getName();

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    public static final String LOCATION_RESULT_KEY = "location";

    private Toast noNameOrAddressToast;
    private LocationModel location;
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_add);
        noNameOrAddressToast = Toast.makeText(getApplicationContext(), R.string.no_name_or_addess_toast, Toast.LENGTH_SHORT);

        EditText nameInput = ((EditText) findViewById(R.id.location_add_name_input));
        EditText addressLabel = ((EditText) findViewById(R.id.location_add_address_display));
        EditText priceInput = ((EditText) findViewById(R.id.location_add_price_input));
        EditText notesInput = ((EditText) findViewById(R.id.location_add_note_input));

        Intent data = getIntent();
        if (data != null) location = data.getParcelableExtra(LOCATION_RESULT_KEY);
        if (location != null && location.hasValidId()) {
            // Edit Location
            if (nameInput != null) {
                nameInput.setText(location.name);
                nameInput.setSelection(location.name.length());
            }
            if (addressLabel != null) addressLabel.setText(location.address);
            if (priceInput != null) priceInput.setText(Float.toString(location.price));
            if (notesInput != null) notesInput.setText(location.notes);

            setTitle(R.string.edit_location);
        } else {
            // Add Location
            location = new LocationModel();
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

        assert nameInput != null;
        nameInput.setOnEditorActionListener(new EditText.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_NEXT) {
                    EditText addressDisplay = (EditText) findViewById(R.id.location_add_address_display);
                    assert addressDisplay != null;
                    addressDisplay.performClick();
                    return true;
                }
                return false;
            }
        });

        assert priceInput != null;
        priceInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    @Override
    public void onBackPressed() {
        EditText nameInput = ((EditText) findViewById(R.id.location_add_name_input));
        EditText priceInput = ((EditText) findViewById(R.id.location_add_price_input));
        EditText notesInput = ((EditText) findViewById(R.id.location_add_note_input));

        assert nameInput != null;
        location.name = nameInput.getText().toString();
        try {
            assert priceInput != null;
            location.price = Float.parseFloat(priceInput.getText().toString());
        } catch (NumberFormatException e) {
            location.price = 0;
        }
        assert notesInput != null;
        location.notes = notesInput.getText().toString();

        if (location.name.isEmpty() || location.address == null) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
            } else {
                this.doubleBackToExitPressedOnce = true;
                noNameOrAddressToast.show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce=false;
                    }
                }, 2000);
            }
        } else {
            Intent result = new Intent();
            result.putExtra(LOCATION_RESULT_KEY, location);
            setResult(Activity.RESULT_OK, result);
            super.onBackPressed();
        }
    }

    public void searchForAddress(View view) {
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(this);
            intent.putExtra("initial_query", location.address);
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, "Error while setting up the search address intent.");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                Place place = PlaceAutocomplete.getPlace(this, data);
                location.setPlace(place);
                EditText addressLabel = (EditText) findViewById(R.id.location_add_address_display);
                assert addressLabel != null;
                addressLabel.setText(location.address);
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.i(TAG, status.getStatusMessage());
            }
            EditText priceInput = (EditText) findViewById(R.id.location_add_price_input);
            assert priceInput != null;
            priceInput.requestFocus();
        }
    }
}
