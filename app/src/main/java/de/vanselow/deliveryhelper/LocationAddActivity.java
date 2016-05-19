package de.vanselow.deliveryhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
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

public class LocationAddActivity extends AppCompatActivity {
    private static final String TAG = LocationAddActivity.class.getName();

    private static final int PLACE_AUTOCOMPLETE_REQUEST_CODE = 1;

    public static final String NAME_RESULT_KEY = "name";
    public static final String ADDRESS_RESULT_KEY = "address";
    public static final String PRICE_RESULT_KEY = "price";
    public static final String NOTES_RESULT_KEY = "notes";

    private Toast noNameOrAddressToast;
    private Place address;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_add);
        noNameOrAddressToast = Toast.makeText(getApplicationContext(), "No 'Name' or 'Address' given.", Toast.LENGTH_SHORT);
        address = null;

        EditText nameInput = (EditText) findViewById(R.id.location_add_name_input);
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

        EditText priceInput = (EditText) findViewById(R.id.location_add_price_input);
        assert priceInput != null;
        priceInput.setOnEditorActionListener(new EditText.OnEditorActionListener(){
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    Button confirmButton = (Button) findViewById(R.id.location_add_confirm_button);
                    assert confirmButton != null;
                    confirmButton.performClick();
                    return true;
                }
                return false;
            }
        });

        priceInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }
            }
        });
    }

    public void addLocationConfirm(View view) {
        EditText nameInput = ((EditText) findViewById(R.id.location_add_name_input));
        EditText priceInput = ((EditText) findViewById(R.id.location_add_price_input));
        EditText notesInput = ((EditText) findViewById(R.id.location_add_note_input));

        Intent result = new Intent();
        assert nameInput != null;
        String name = nameInput.getText().toString();
        float price;
        try {
            assert priceInput != null;
            price = Float.parseFloat(priceInput.getText().toString());
        } catch (NumberFormatException e) {
            price = 0;
        }
        assert notesInput != null;
        String notes = notesInput.getText().toString();

        if (name.isEmpty() || address == null) {
            noNameOrAddressToast.show();
            return;
        }

        result.putExtra(NAME_RESULT_KEY, name);
        result.putExtra(PRICE_RESULT_KEY, price);
        result.putExtra(ADDRESS_RESULT_KEY, (PlaceImpl) address);
        result.putExtra(NOTES_RESULT_KEY, notes);

        setResult(Activity.RESULT_OK, result);
        finish();
    }

    public void searchForAddress(View view) {
        try {
            Intent intent = new PlaceAutocomplete.IntentBuilder(PlaceAutocomplete.MODE_OVERLAY).build(this);
            EditText addressDisplay = (EditText) findViewById(R.id.location_add_address_display);
            assert addressDisplay != null;
            intent.putExtra("initial_query", addressDisplay.getText().toString());
            startActivityForResult(intent, PLACE_AUTOCOMPLETE_REQUEST_CODE);
        } catch (GooglePlayServicesRepairableException | GooglePlayServicesNotAvailableException e) {
            Log.e(TAG, "Error while setting up the search address intent.");
        }
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == PLACE_AUTOCOMPLETE_REQUEST_CODE) {
            if (resultCode == RESULT_OK) {
                address = PlaceAutocomplete.getPlace(this, data);
                EditText addressLabel = (EditText) findViewById(R.id.location_add_address_display);
                assert addressLabel != null;
                addressLabel.setText(address.getAddress());
            } else if (resultCode == PlaceAutocomplete.RESULT_ERROR) {
                Status status = PlaceAutocomplete.getStatus(this, data);
                Log.i(TAG, status.getStatusMessage());
            }
            EditText priceInput = (EditText) findViewById(R.id.location_add_price_input);
            assert priceInput != null;
            priceInput.requestFocus();
        }
    }

    public void clearAddressSearch(View view) {
        EditText addressDisplay = (EditText) findViewById(R.id.location_add_address_display);
        assert addressDisplay != null;
        addressDisplay.getText().clear();
    }
}
