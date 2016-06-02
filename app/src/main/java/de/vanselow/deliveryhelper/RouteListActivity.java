package de.vanselow.deliveryhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.ListView;

import java.util.ArrayList;

import de.vanselow.deliveryhelper.utils.DatabaseAsync;
import pl.com.salsoft.sqlitestudioremote.SQLiteStudioService;

public class RouteListActivity extends AppCompatActivity {
    public static final int ADD_ROUTE_REQUEST_CODE = 1;
    public static final int EDIT_ROUTE_REQUEST_CODE = 2;
    public static final int EXIT_LOC_LIST_REQUEST_CODE = 3;

    private RouteListAdapter routeListAdapter;
    private boolean activityVisible;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list);

        activityVisible = false;

        RemoteAccess.start(this, 1337);
        RemoteAccess.setRoutesDataReceivedListener(new RemoteAccess.RoutesReceivedListener(this) {
            @Override
            public void onRoutesReceived(ArrayList<RouteModel> routes) {
                if (activityVisible) updateRouteListData();
            }
        });

        //deleteDatabase("vanselow_delivery_helper");

        // for external sqlite browser
        SQLiteStudioService.instance().start(this);

        routeListAdapter = new RouteListAdapter(this, new ArrayList<RouteModel>());
        updateRouteListData();
        ListView routeListView = (ListView) findViewById(R.id.route_list);
        assert routeListView != null;
        routeListView.setAdapter(routeListAdapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        activityVisible = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        activityVisible = false;
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        updateRouteListData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // for external sqlite browser
        SQLiteStudioService.instance().stop();
        RemoteAccess.stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.route_list_menu, menu);
        return true;
    }

    public void addRouteOnClick(MenuItem item) {
        startActivityForResult(new Intent(getApplicationContext(), RouteAddActivity.class), ADD_ROUTE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( resultCode == Activity.RESULT_OK) {
            if (requestCode == ADD_ROUTE_REQUEST_CODE) {
                RouteModel newRoute = data.getParcelableExtra(RouteAddActivity.ROUTE_RESULT_KEY);
                routeListAdapter.addItem(newRoute);
            } else if (requestCode == EXIT_LOC_LIST_REQUEST_CODE) {
                RouteModel route = data.getParcelableExtra(LocationListActivity.ROUTE_KEY);
                routeListAdapter.updateItem(route);
            } else if (requestCode == EDIT_ROUTE_REQUEST_CODE) {
                RouteModel updatedRoute = data.getParcelableExtra(RouteAddActivity.ROUTE_RESULT_KEY);
                routeListAdapter.updateItem(updatedRoute);
            }
        } else if (data != null) {
            final RouteModel route = data.getParcelableExtra(LocationListActivity.ROUTE_KEY);
            DatabaseAsync.getInstance(this).getAllRouteLocations(route.id, new DatabaseAsync.Callback<ArrayList<LocationModel>>() {
                @Override
                public void onPostExecute(ArrayList<LocationModel> locationModels) {
                    route.locations = locationModels;
                    routeListAdapter.updateItem(route);
                }
            });
        }
    }

    private void updateRouteListData() {
        routeListAdapter.getRoutes().clear();
        DatabaseAsync.getInstance(this).getAllRoutes(new DatabaseAsync.Callback<ArrayList<RouteModel>>() {
            @Override
            public void onPostExecute(ArrayList<RouteModel> routeModels) {
                routeListAdapter.getRoutes().clear();
                routeListAdapter.getRoutes().addAll(routeModels);
                routeListAdapter.notifyDataSetChanged();
            }
        });
    }
}
