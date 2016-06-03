package de.vanselow.deliveryhelper;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import java.text.DateFormat;
import java.util.Calendar;

import de.vanselow.deliveryhelper.utils.DatabaseHelper;

public class RouteAddActivity extends AppCompatActivity {
    public static final String ROUTE_ID_KEY = "routeId";
    public static final String ROUTE_KEY = "route";

    private static final String DATE_PICKER_TIME_KEY = "time";
    private static final int DATE_PICKER_REQUEST_CODE = 1;

    private Toast noNameToast;
    private RouteModel route;
    private long initDate = 0;
    private boolean doubleBackToExitPressedOnce = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_add);
        noNameToast = Toast.makeText(this, R.string.no_name_toast, Toast.LENGTH_SHORT);

        if (savedInstanceState != null) {
            route = savedInstanceState.getParcelable(ROUTE_KEY);
            updateDate(route.date);
        } else {
            Intent data = getIntent();
            long routeId = -1;
            if (data != null) routeId = data.getLongExtra(ROUTE_ID_KEY, -1);
            if (routeId >= 0) {
                route = DatabaseHelper.getInstance(this).getRouteById(routeId);
                if (route == null) {
                    finish();
                    return;
                }
                // Edit Route
                EditText nameLabel = ((EditText) findViewById(R.id.route_add_name_input));
                if (nameLabel != null) {
                    nameLabel.setText(route.name);
                    nameLabel.setSelection(route.name.length());
                }

                updateDate(route.date);

                setTitle(R.string.edit_route);
            } else {
                // Add Route
                route = new RouteModel();
                initDate = Calendar.getInstance().getTimeInMillis();
                updateDate(initDate);
            }
        }
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ROUTE_KEY, route);
    }

    private void showDatePicker() {
        DialogFragment newFragment = DatePickerFragment.newInstance(route.date);
        newFragment.setTargetFragment(null, DATE_PICKER_REQUEST_CODE);
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void dateOnClick(View view) {
        showDatePicker();
    }

    private void updateDate(long date) {
        route.date = date;
        TextView dateLabel = (TextView) findViewById(R.id.route_add_date_label);
        if (dateLabel != null) {
            DateFormat dateFormat = DateFormat.getDateInstance();
            dateLabel.setText(dateFormat.format(date));
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && requestCode == DATE_PICKER_REQUEST_CODE) {
            updateDate(data.getLongExtra(DATE_PICKER_TIME_KEY, Calendar.getInstance().getTimeInMillis()));
        }
    }

    @Override
    public void onBackPressed() {
        EditText nameInput = ((EditText) findViewById(R.id.route_add_name_input));

        assert nameInput != null;
        route.name = nameInput.getText().toString();

        if (route.name.isEmpty() && route.date == initDate) {
            super.onBackPressed();
        } else if (route.name.isEmpty()) {
            if (doubleBackToExitPressedOnce) {
                super.onBackPressed();
            } else {
                this.doubleBackToExitPressedOnce = true;
                noNameToast.show();

                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        doubleBackToExitPressedOnce = false;
                    }
                }, 2000);
            }
        } else {
            DatabaseHelper.getInstance(this).addOrUpdateRoute(route);
            Intent result = new Intent();
            result.putExtra(ROUTE_ID_KEY, route.id);
            setResult(Activity.RESULT_OK, result);
            super.onBackPressed();
        }
    }

    public static class DatePickerFragment extends DialogFragment
            implements DatePickerDialog.OnDateSetListener {

        private long time;

        public static DatePickerFragment newInstance(long t) {
            DatePickerFragment f = new DatePickerFragment();

            // Supply num input as an argument.
            Bundle args = new Bundle();
            args.putLong(DATE_PICKER_TIME_KEY, t);
            f.setArguments(args);

            return f;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (savedInstanceState != null) {
                time = savedInstanceState.getLong(DATE_PICKER_TIME_KEY);
            } else {
                time = Calendar.getInstance().getTimeInMillis();
            }
        }

        @NonNull @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Calendar c = Calendar.getInstance();
            c.setTimeInMillis(time);
            return new DatePickerDialog(getActivity(), this,
                    c.get(Calendar.YEAR),
                    c.get(Calendar.MONTH),
                    c.get(Calendar.DAY_OF_MONTH));
        }

        @Override
        public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
            Calendar c = Calendar.getInstance();
            c.set(year, monthOfYear, dayOfMonth);
            Intent intent = new Intent();
            intent.putExtra(DATE_PICKER_TIME_KEY, c.getTimeInMillis());
            // hack because its called from an activity and not from a fragment
            ((RouteAddActivity) getActivity()).onActivityResult(getTargetRequestCode(), RESULT_OK, intent);
        }
    }
}
