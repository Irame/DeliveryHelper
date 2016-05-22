package de.vanselow.deliveryhelper;

import android.content.DialogInterface;
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
import de.vanselow.deliveryhelper.utils.Utils;

public class RouteListAdapter extends BaseSwipeAdapter {
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_EPTY_NOTE = 1;

    private LayoutInflater layoutInflater;
    private FragmentActivity activity;

    private ArrayList<RouteModel> routes;
    private String emptynoteText;

    public RouteListAdapter(FragmentActivity activity, ArrayList<RouteModel> routes) {
        this.activity = activity;
        layoutInflater = LayoutInflater.from(activity);
        this.routes = routes;
        emptynoteText = activity.getString(R.string.no_routes_existing);
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

    public void removeItem(int position) {
        RouteModel route = routes.remove(position);
        DatabaseHelper.getInstance(activity).deleteRouteById(route.id);
        notifyDataSetChanged();
    }

    public ArrayList<RouteModel> getRoutes() {
        return routes;
    }

    @Override
    public int getItemViewType(int position) {
        if (routes.isEmpty())
            return TYPE_EPTY_NOTE;
        else
            return TYPE_ITEM;
    }

    @Override
    public int getViewTypeCount() {
        return 2;
    }

    @Override
    public int getCount() {
        return Math.max(routes.size(), 1);
    }

    @Override
    public Object getItem(int position) {
        if (routes.isEmpty())
            return emptynoteText;
        else
            return routes.get(position);
    }

    @Override
    public long getItemId(int position) {
        if (routes.isEmpty())
            return -1;
        else
            return routes.get(position).id;
    }

    @Override
    public int getSwipeLayoutResourceId(int position) {
        switch (getItemViewType(position)) {
            case TYPE_ITEM: return R.id.route_list_item_swipe;
            case TYPE_EPTY_NOTE: return R.id.list_emptynote_swipe;
            default: return -1;
        }
    }

    @Override
    public View generateView(int position, ViewGroup parent) {
        View v;
        ViewHolder viewHolder;
        if (routes.isEmpty()) {
            v = layoutInflater.inflate(R.layout.list_emptynote, parent, false);
            viewHolder = new EmptyNoteViewHolder(v);
        } else {
            v = layoutInflater.inflate(R.layout.route_list_item, parent, false);
            viewHolder = new ItemViewHolder(v);
        }
        v.setTag(viewHolder);
        return v;
    }

    @Override
    public void fillValues(int position, View convertView) {
        ((ViewHolder)convertView.getTag()).setup(position);
    }

    private interface ViewHolder {
        void setup(int position);
    }

    private class ItemViewHolder implements View.OnClickListener, ViewHolder {
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
        private int position;

        ItemViewHolder(View v) {
            swipeLayout = (SwipeLayout) v;
            surfaceView = swipeLayout.getSurfaceView();

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

            swipeLayout.setClickToClose(true);
        }

        public void setup(int position) {
            RouteModel route = (RouteModel) getItem(position);
            this.route = route;
            this.position = position;

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
                swipeLayout.close();
                RouteModel route = routes.get(position);
                Utils.deleteAlert(activity, route.name, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        removeItem(position);
                        dialog.dismiss();
                    }
                }).show();
            }
        }
    }


    private class EmptyNoteViewHolder implements ViewHolder {
        private TextView emptynoteLabel;

        public EmptyNoteViewHolder(View view) {
            emptynoteLabel = (TextView) view.findViewById(R.id.list_emptynote_text);
        }

        @Override
        public void setup(int position) {
            emptynoteLabel.setText(emptynoteText);
        }
    }
}
