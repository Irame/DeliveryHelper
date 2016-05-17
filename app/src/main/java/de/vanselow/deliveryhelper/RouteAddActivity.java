package de.vanselow.deliveryhelper;

import android.app.Activity;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.TextView;

import java.text.DateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * Created by Felix on 15.05.2016.
 */
public class RouteAddActivity extends AppCompatActivity {
    public static final String ID_RESULT_KEY = "id";
    public static final String NAME_RESULT_KEY = "name";
    public static final String DATE_RESULT_KEY = "date";

    private static final String DATE_PICKER_TIME_KEY = "time";
    private static final int DATE_PICKER_REQUEST_CODE = 1;

    private long id;
    private long date;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_add);

        Intent data = getIntent();
        if (data != null) {
            id = data.getLongExtra(ID_RESULT_KEY, -1);

            String name = data.getStringExtra(NAME_RESULT_KEY);
            EditText nameLabel = ((EditText) findViewById(R.id.route_add_name_input));
            if (nameLabel != null) nameLabel.setText(name);

            long time = data.getLongExtra(DATE_RESULT_KEY, Calendar.getInstance().getTimeInMillis());
            updateDate(time);
        } else {
            id = -1;
            updateDate(Calendar.getInstance().getTimeInMillis());
        }
    }

    public void showDatePicker() {
        DialogFragment newFragment = DatePickerFragment.newInstance(date);
        newFragment.setTargetFragment(null, DATE_PICKER_REQUEST_CODE);
        newFragment.show(getSupportFragmentManager(), "datePicker");
    }

    public void dateOnClick(View view) {
        showDatePicker();
    }

    private void updateDate(long date) {
        this.date = date;
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

    private void addRouteConfirm() {
        Intent result = new Intent();

        String name = ((EditText) findViewById(R.id.route_add_name_input)).getText().toString();

        result.putExtra(ID_RESULT_KEY, id);
        result.putExtra(NAME_RESULT_KEY, name);
        result.putExtra(DATE_RESULT_KEY, date);

        setResult(Activity.RESULT_OK, result);
        finish();
    }

    public void addRouteConfirmOnClick(View view) {
        addRouteConfirm();
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
