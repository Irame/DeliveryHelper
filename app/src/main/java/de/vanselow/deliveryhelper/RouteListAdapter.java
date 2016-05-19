package de.vanselow.deliveryhelper;

import android.content.Intent;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.BaseSwipeAdapter;

import java.text.DateFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Locale;

import de.vanselow.deliveryhelper.utils.DatabaseHelper;

public class RouteListAdapter extends BaseSwipeAdapter {
    private LayoutInflater layoutInflater;
    private FragmentActivity activity;

    private ArrayList<RouteModel> routes;

    public RouteListAdapter(FragmentActivity activity, ArrayList<RouteModel> routes) {
        this.activity = activity;
        layoutInflater = LayoutInflater.from(activity);
        this.routes = routes;
    }

    public void addItem(RouteModel route) {
        routes.add(route);
        DatabaseHelper.getInstance(activity).addOrUpdateRoute(route);
        notifyDataSetChanged();
    }

    public void updateItem(RouteModel otherRoute) {
        for (RouteModel route : routes) {
            if (route.id == otherRoute.id && route.update(otherRoute)) {
                DatabaseHelper.getInstance(activity).addOrUpdateRoute(route);
                notifyDataSetChanged();
                break;
            }
        }
    }

    public void removeItemById(long id) {
        for (int i = 0; i < routes.size(); i++) {
            if (routes.get(i).id == id) {
                routes.remove(i);
                DatabaseHelper.getInstance(activity).deleteRouteById(id);
                notifyDataSetChanged();
                break;
            }
        }
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
    public int getSwipeLayoutResourceId(int position) {
        return R.id.route_list_item_swipe;
    }

    @Override
    public View generateView(int position, ViewGroup parent) {
        SwipeLayout v = (SwipeLayout) layoutInflater.inflate(R.layout.route_list_item, parent, false);
        ViewHolder viewHolder = new ViewHolder(v);
        v.setTag(viewHolder);
        return v;
    }

    @Override
    public void fillValues(int position, View convertView) {
        ((ViewHolder)convertView.getTag()).setup(position);
    }

    private class ViewHolder implements View.OnClickListener {
        private SwipeLayout swipeLayout;

        private View surfaceView;

        private ImageButton deleteButton;
        private ImageButton editButton;

        private TextView name;
        private TextView date;
        private TextView open;
        private TextView delivered;
        private TextView totalPrice;

        private RouteModel route;

        ViewHolder(SwipeLayout v) {
            swipeLayout = v;
            surfaceView = v.getSurfaceView();

            deleteButton = (ImageButton) v.findViewById(R.id.route_list_item_delete_button);
            editButton = (ImageButton) v.findViewById(R.id.route_list_item_edit_button);

            name = (TextView) v.findViewById(R.id.route_list_item_name_label);
            date = (TextView) v.findViewById(R.id.route_list_item_date_label);
            open = (TextView) v.findViewById(R.id.route_list_item_open_count);
            delivered = (TextView) v.findViewById(R.id.route_list_item_delivered_count);
            totalPrice = (TextView) v.findViewById(R.id.route_list_item_total_price_label);

            surfaceView.setLongClickable(false);
            surfaceView.setOnClickListener(this);

            deleteButton.setOnClickListener(this);
            editButton.setOnClickListener(this);

            v.setClickToClose(true);
        }

        public void setup(int position) {
            RouteModel route = (RouteModel) getItem(position);
            this.route = route;

            name.setText(route.name);

            DateFormat dateFormat = DateFormat.getDateInstance();
            date.setText(dateFormat.format(route.date));

            open.setText(String.format(Locale.getDefault(), activity.getString(R.string.open_amount_format_template), route.getOpenLocations()));
            delivered.setText(String.format(Locale.getDefault(), activity.getString(R.string.delivered_amount_format_template), route.getDeliveredLocations()));

            NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
            totalPrice.setText(currencyFormat.format(route.getTotalPrice()));
        }

        @Override
        public void onClick(View v) {
            if (v.getId() == surfaceView.getId()) {
                Intent i = new Intent(activity.getApplicationContext(), LocationListActivity.class);
                i.putExtra(LocationListActivity.ROUTE_KEY, route);
                activity.startActivityForResult(i, RouteListActivity.EXIT_LOC_LIST_REQUEST_CODE);
            } else if (v.getId() == editButton.getId()) {
                Intent intent = new Intent(activity.getApplicationContext(), RouteAddActivity.class);
                intent.putExtra(RouteAddActivity.ROUTE_RESULT_KEY, route);
                activity.startActivityForResult(intent, RouteListActivity.EDIT_ROUTE_REQUEST_CODE);
                swipeLayout.close();
            } else if (v.getId() == deleteButton.getId()) {
                swipeLayout.close(false);
                removeItemById(route.id);
            }
        }
    }
}
