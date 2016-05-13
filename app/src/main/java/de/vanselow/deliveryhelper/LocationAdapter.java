package de.vanselow.deliveryhelper;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Created by Felix on 12.05.2016.
 */
public class LocationAdapter extends ArrayAdapter<DeliveryLocationModel> {
    private ArrayList<DeliveryLocationModel> values;

    public int selectedItemPosition = -1;

    public LocationAdapter(Context context) {
        super(context, 0);
    }

    public LocationAdapter(Context context, ArrayList<DeliveryLocationModel> items) {
        super(context, 0, items);
        values = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        View v = convertView;

        if (v == null) {
            LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            v = vi.inflate(R.layout.location_list_row, null);

            v.findViewById(R.id.location_list_item_delete_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeliveryLocationModel loc = values.remove(selectedItemPosition);
                    LocationsDatabaseHelper.getInstance(getContext()).deleteLocation(loc);
                    selectedItemPosition = -1;
                    notifyDataSetChanged();
                }
            });

            v.findViewById(R.id.location_list_item_nav_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeliveryLocationModel loc = values.get(selectedItemPosition);
                    Uri gmmIntentUri = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", loc.latitude, loc.longitude));
                    Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
                    mapIntent.setPackage("com.google.android.apps.maps");
                    getContext().startActivity(mapIntent);
                }
            });

            v.findViewById(R.id.location_list_item_done_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    DeliveryLocationModel loc = values.get(selectedItemPosition);
                    loc.status = DeliveryLocationModel.Status.DELIVERED;
                    LocationsDatabaseHelper.getInstance(getContext()).addOrUpdateLocation(loc);
                }
            });
        }

        View buttonBar = v.findViewById(R.id.location_list_item_button_bar);
        if (selectedItemPosition == position)
            buttonBar.setVisibility(View.VISIBLE);
        else
            buttonBar.setVisibility(View.GONE);

        DeliveryLocationModel p = getItem(position);

        if (p != null) {
            TextView name = (TextView) v.findViewById(R.id.nameLabel);
            TextView address = (TextView) v.findViewById(R.id.adressLabel);
            TextView price = (TextView) v.findViewById(R.id.priceLabel);

            if (name != null) {
                name.setText(p.name);
            }

            if (address != null) {
                address.setText(p.address);
            }

            if (price != null) {
                price.setText(String.format(Locale.GERMANY, "%.2f€", p.price));
            }
        }

        return v;
    }

    public ArrayList<DeliveryLocationModel> getValues() {
        return values;
    }
}
