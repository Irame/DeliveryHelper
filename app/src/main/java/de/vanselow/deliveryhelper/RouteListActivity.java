package de.vanselow.deliveryhelper;

import android.app.Activity;
import android.content.Context;
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

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list);

        final Context context = this;
        RemoteAccess.start(this, 1337);
        RemoteAccess.setRoutesDataReceivedListener(new RemoteAccess.RoutesReceivedListener(this) {
            @Override
            public void onRoutesReceived(ArrayList<RouteModel> routes) {
                DatabaseAsync dbHelper = DatabaseAsync.getInstance(context);
                for (RouteModel route : routes) {
                    dbHelper.addOrUpdateRoute(route, new DatabaseAsync.Callback<Long>() {
                        @Override
                        public void onPostExecute(Long routeId) {
                            routeListAdapter.updateRouteFromDatabase(routeId);
                        }
                    });
                }
            }
        });

        //deleteDatabase("vanselow_delivery_helper");

        // for external sqlite browser
        SQLiteStudioService.instance().start(this);

        routeListAdapter = new RouteListAdapter(this, new ArrayList<RouteModel>());
        routeListAdapter.updateAllRoutesFromDatabase();
        ListView routeListView = (ListView) findViewById(R.id.route_list);
        assert routeListView != null;
        routeListView.setAdapter(routeListAdapter);
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        routeListAdapter.updateAllRoutesFromDatabase();
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
            if (requestCode == ADD_ROUTE_REQUEST_CODE || requestCode == EDIT_ROUTE_REQUEST_CODE) {
                long routeId = data.getLongExtra(RouteAddActivity.ROUTE_ID_KEY, -1);
                if (routeId >= 0) routeListAdapter.updateRouteFromDatabase(routeId);
            } else if (requestCode == EXIT_LOC_LIST_REQUEST_CODE) {
                long routeId = data.getLongExtra(LocationListActivity.ROUTE_ID_KEY, -1);
                if (routeId >= 0) routeListAdapter.updateRouteFromDatabase(routeId);
            }
        } else if (data != null) {
            routeListAdapter.updateAllRoutesFromDatabase();
        }
    }
}
