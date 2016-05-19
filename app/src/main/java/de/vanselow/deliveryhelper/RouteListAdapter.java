package de.vanselow.deliveryhelper;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

import de.vanselow.deliveryhelper.utils.DatabaseHelper;

public class RouteListAdapter extends BaseAdapter {
    private LayoutInflater layoutInflater;
    private FragmentActivity context;

    private ArrayList<RouteModel> routes;

    private boolean checkMode;
    private Set<Integer> checkedItems;

    public RouteListAdapter(FragmentActivity context, ArrayList<RouteModel> routes) {
        this.context = context;
        layoutInflater = LayoutInflater.from(context);
        this.routes = routes;
        this.checkedItems = new HashSet<>();
    }

    public void addItem(RouteModel route) {
        routes.add(route);
        notifyDataSetChanged();
    }

    public void removeSelectedItems() {
        checkMode = false;
        for (int i = routes.size() - 1; i >= 0; i--) {
            if (checkedItems.contains(i)) {
                RouteModel removedRoute = routes.remove(i);
                DatabaseHelper.getInstance(context).deleteRoute(removedRoute);
            }
        }
        checkedItems.clear();
        context.invalidateOptionsMenu();
        notifyDataSetChanged();
    }

    public RouteModel getItemById(long id) {
        for (int i = 0; i < routes.size(); i++) {
            RouteModel r = routes.get(i);
            if (r.id == id) {
                return r;
            }
        }
        return null;
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

    public boolean isCheckMode() {
        return checkMode;
    }

    public ArrayList<RouteModel> getSelectedRoutes() {
        ArrayList<RouteModel> selectedRoutes = new ArrayList<>();
        for (Integer checkedItem : checkedItems) {
            selectedRoutes.add(routes.get(checkedItem));
        }
        return selectedRoutes;
    }

    public int getSelectedCount() {
        return checkedItems.size();
    }

    public void clearSelection() {
        checkedItems.clear();
        checkMode = false;
        notifyDataSetChanged();
        context.invalidateOptionsMenu();
    }

    private class ViewHolder implements View.OnLongClickListener, View.OnClickListener {
        View view;

        private TextView name;
        private TextView date;
        private TextView open;
        private TextView delivered;
        private TextView totalPrice;

        private long id;
        private int position;

        ViewHolder(View v) {
            view = v;

            name = (TextView) v.findViewById(R.id.route_list_item_name_label);
            date = (TextView) v.findViewById(R.id.route_list_item_date_label);
            open = (TextView) v.findViewById(R.id.route_list_item_open_count);
            delivered = (TextView) v.findViewById(R.id.route_list_item_delivered_count);
            totalPrice = (TextView) v.findViewById(R.id.route_list_item_total_price_label);

            v.setOnLongClickListener(this);
            v.setOnClickListener(this);
        }

        public void setup(int position) {
            this.position = position;

            RouteModel route = (RouteModel) getItem(position);
            this.id = route.id;

            name.setText(route.name);

            DateFormat dateFormat = DateFormat.getDateInstance();
            date.setText(dateFormat.format(route.date));

            open.setText(String.format(Locale.getDefault(), context.getString(R.string.open_amount_format_template), route.getOpenLocations()));
            delivered.setText(String.format(Locale.getDefault(), context.getString(R.string.delivered_amount_format_template), route.getDeliveredLocations()));

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            totalPrice.setText(currencyFormat.format(route.getTotalPrice()));

            view.setSelected(checkedItems.contains(position));
        }

        @Override
        public boolean onLongClick(View v) {
            view.setSelected(!view.isSelected());
            if (view.isSelected())
                checkedItems.add(position);
            else
                checkedItems.remove(position);
            checkMode = !checkedItems.isEmpty();
            context.invalidateOptionsMenu();
            return true;
        }

        @Override
        public void onClick(View v) {
            if (isCheckMode()) {
                onLongClick(v);
            } else {
                Intent i = new Intent(context.getApplicationContext(), LocationListActivity.class);
                i.putExtra(LocationListActivity.ROUTE_ID_KEY, id);
                context.startActivityForResult(i, RouteListActivity.EXIT_LOC_LIST_REQUEST_CODE);
            }
        }
    }
}
