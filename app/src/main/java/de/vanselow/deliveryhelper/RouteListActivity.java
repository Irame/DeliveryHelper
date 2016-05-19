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
import java.util.Calendar;

import de.vanselow.deliveryhelper.utils.DatabaseHelper;
import pl.com.salsoft.sqlitestudioremote.SQLiteStudioService;

public class RouteListActivity extends AppCompatActivity {
    public static final int ADD_ROUTE_REQUEST_CODE = 1;
    public static final int EDIT_ROUTE_REQUEST_CODE = 2;
    public static final int EXIT_LOC_LIST_REQUEST_CODE = 3;

    private static final String ROUTE_LIST_KEY = "routes";
    private static final String CHECK_MODE_KEY = "checkMode";
    private static final String CHECKED_LIST_KEY = "checkedList";

    private RouteListAdapter routeListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list);

        //deleteDatabase("vanselow_delivery_helper");

        // for external sqlite browser
        SQLiteStudioService.instance().start(this);

        ArrayList<RouteModel> routeList = null;
        if (savedInstanceState != null) {
            routeList = savedInstanceState.getParcelableArrayList(ROUTE_LIST_KEY);
        }
        if (routeList == null) {
            routeList = DatabaseHelper.getInstance(this).getAllRoutes();
        }
        routeListAdapter = new RouteListAdapter(this, routeList);
        ListView routeListView = (ListView) findViewById(R.id.route_list);
        assert routeListView != null;
        routeListView.setAdapter(routeListAdapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(ROUTE_LIST_KEY, routeListAdapter.getRoutes());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // for external sqlite browser
        SQLiteStudioService.instance().stop();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.route_list_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (routeListAdapter.isCheckMode()) {
            menu.findItem(R.id.route_list_menu_item_add).setVisible(false);
            menu.findItem(R.id.route_list_menu_item_remove).setVisible(true);
        } else {
            menu.findItem(R.id.route_list_menu_item_add).setVisible(true);
            menu.findItem(R.id.route_list_menu_item_remove).setVisible(false);
        }
        if (routeListAdapter.getSelectedCount() == 1) {
            menu.findItem(R.id.route_list_menu_item_edit).setVisible(true);
        } else {
            menu.findItem(R.id.route_list_menu_item_edit).setVisible(false);
        }
        return true;
    }

    public void addRouteOnClick(MenuItem item) {
        startActivityForResult(new Intent(getApplicationContext(), RouteAddActivity.class), ADD_ROUTE_REQUEST_CODE);
    }

    public void editRouteOnClick(MenuItem item) {
        RouteModel selectedRoute = routeListAdapter.getSelectedRoutes().get(0);
        routeListAdapter.clearSelection();
        Intent intent = new Intent(getApplicationContext(), RouteAddActivity.class);
        intent.putExtra(RouteAddActivity.ROUTE_RESULT_KEY, selectedRoute);
        startActivityForResult(intent, EDIT_ROUTE_REQUEST_CODE);
    }

    public void removeRouteOnClick(MenuItem item) {
        routeListAdapter.removeSelectedItems();
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
            RouteModel route = data.getParcelableExtra(LocationListActivity.ROUTE_KEY);
            route.locations = DatabaseHelper.getInstance(this).getAllRouteLocations(route.id);
            routeListAdapter.updateItem(route);
        }
    }
}
