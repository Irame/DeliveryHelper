package de.vanselow.deliveryhelper;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.hb.views.PinnedSectionListView;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;

/**
 * Created by Felix on 15.05.2016.
 */
public class RouteListAdapter extends BaseAdapter {
    private LayoutInflater layoutInflater;
    private Context context;

    private ArrayList<RouteModel> routes;

    public RouteListAdapter(Context context, ArrayList<RouteModel> routes) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        this.routes = routes;
    }

    public void addItem(RouteModel route) {
        routes.add(route);
        notifyDataSetChanged();
    }

    public ArrayList<RouteModel> getRoutes() {
        return routes;
    }

    @Override
    public int getCount() {
        return routes.size();
    }

    @Override
    public Object getItem(int position) {
        return routes.get(position);
    }

    @Override
    public long getItemId(int position) {
        return routes.get(position).id;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        View v = layoutInflater.inflate(R.layout.route_list_item, parent, false);

        TextView name = (TextView) v.findViewById(R.id.route_list_item_name_label);
        TextView date = (TextView) v.findViewById(R.id.route_list_item_date_label);
        TextView open = (TextView) v.findViewById(R.id.route_list_item_open_count);
        TextView delivered = (TextView) v.findViewById(R.id.route_list_item_delivered_count);
        TextView totalPrice = (TextView) v.findViewById(R.id.route_list_item_total_price_label);

        RouteModel route = (RouteModel) getItem(position);

        if (name != null) {
            name.setText(route.name);
        }

        if (date != null) {
            DateFormat dateFormat = DateFormat.getDateInstance();
            date.setText(dateFormat.format(route.date));
        }

        if (open != null) {
            open.setText(Integer.toString(route.getOpenLocations()));
        }

        if (delivered != null) {
            delivered.setText(Integer.toString(route.getDeliveredLocations()));
        }

        if (totalPrice != null) {
            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            totalPrice.setText(currencyFormat.format(route.getTotalPrice()));
        }

        return v;
    }
}
