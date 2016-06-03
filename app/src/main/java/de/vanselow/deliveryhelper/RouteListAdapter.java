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
import java.util.Iterator;
import java.util.Locale;

import de.vanselow.deliveryhelper.utils.DatabaseAsync;
import de.vanselow.deliveryhelper.utils.DatabaseHelper;
import de.vanselow.deliveryhelper.utils.Utils;

public class RouteListAdapter extends BaseSwipeAdapter {
    private static final int TYPE_ITEM = 0;
    private static final int TYPE_EPTY_NOTE = 1;

    private final LayoutInflater layoutInflater;
    private final FragmentActivity activity;
    private final ArrayList<RouteModel> routes;

    public RouteListAdapter(FragmentActivity activity, ArrayList<RouteModel> routes) {
        this.activity = activity;
        this.routes = routes;
        this.layoutInflater = LayoutInflater.from(activity);
    }

    public void updateRouteFromDatabase(final long routeId) {
        if (routeId < 0) return;
        DatabaseAsync.getInstance(activity).getRouteById(routeId, new DatabaseAsync.Callback<RouteModel>() {
            @Override
            public void onPostExecute(RouteModel routeModel) {
                boolean found = false;
                for (Iterator<RouteModel> iterator = routes.iterator(); iterator.hasNext(); ) {
                    RouteModel route = iterator.next();
                    if (route.id == routeId) {
                        found = true;
                        if (routeModel == null)
                            iterator.remove();
                        else
                            route.update(routeModel);
                        notifyDataSetChanged();
                        break;
                    }
                }
                if (!found) {
                    routes.add(routeModel);
                    notifyDataSetChanged();
                }
            }
        });
    }

    public void updateAllRoutesFromDatabase() {
        DatabaseAsync.getInstance(activity).getAllRoutes(new DatabaseAsync.Callback<ArrayList<RouteModel>>() {
            @Override
            public void onPostExecute(ArrayList<RouteModel> routeModels) {
                routes.clear();
                routes.addAll(routeModels);
                notifyDataSetChanged();
            }
        });
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
            return activity.getString(R.string.no_routes_existing);
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
                i.putExtra(LocationListActivity.ROUTE_ID_KEY, route.id);
                activity.startActivityForResult(i, RouteListActivity.EXIT_LOC_LIST_REQUEST_CODE);
            } else if (v.getId() == editButton.getId()) {
                Intent intent = new Intent(activity.getApplicationContext(), RouteAddActivity.class);
                intent.putExtra(RouteAddActivity.ROUTE_ID_KEY, route.id);
                activity.startActivityForResult(intent, RouteListActivity.EDIT_ROUTE_REQUEST_CODE);
                swipeLayout.close();
            } else if (v.getId() == deleteButton.getId()) {
                swipeLayout.close();
                Utils.deleteAlert(activity, route.name, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        DatabaseHelper.getInstance(activity).deleteRoute(route);
                        updateRouteFromDatabase(route.id);
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
            emptynoteLabel.setText(activity.getString(R.string.no_routes_existing));
        }
    }
}
