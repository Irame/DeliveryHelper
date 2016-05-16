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
        View v = convertView;

        ViewHolder viewHolder;
        if (v == null) {
            v = layoutInflater.inflate(R.layout.route_list_item, parent, false);
            viewHolder = new ViewHolder(v);
            v.setTag(viewHolder);
        } else {
            viewHolder = (ViewHolder) v.getTag();
        }
        viewHolder.setup(position);

        return v;
    }

    private class ViewHolder {
        private TextView name;
        private TextView date;
        private TextView open;
        private TextView delivered;
        private TextView totalPrice;

        ViewHolder(View v) {
            name = (TextView) v.findViewById(R.id.route_list_item_name_label);
            date = (TextView) v.findViewById(R.id.route_list_item_date_label);
            open = (TextView) v.findViewById(R.id.route_list_item_open_count);
            delivered = (TextView) v.findViewById(R.id.route_list_item_delivered_count);
            totalPrice = (TextView) v.findViewById(R.id.route_list_item_total_price_label);
        }

        public void setup(int position) {
            RouteModel route = (RouteModel) getItem(position);

            name.setText(route.name);

            DateFormat dateFormat = DateFormat.getDateInstance();
            date.setText(dateFormat.format(route.date));

            open.setText(Integer.toString(route.getOpenLocations()));
            delivered.setText(Integer.toString(route.getDeliveredLocations()));

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            totalPrice.setText(currencyFormat.format(route.getTotalPrice()));
        }
    }
}
