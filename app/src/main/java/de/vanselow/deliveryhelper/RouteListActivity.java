package de.vanselow.deliveryhelper;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;
import java.util.Calendar;

import de.vanselow.deliveryhelper.utils.DatabaseHelper;
import pl.com.salsoft.sqlitestudioremote.SQLiteStudioService;

public class RouteListActivity extends AppCompatActivity {
    private static final int ADD_ROUTE_REQUEST_CODE = 1;
    private static final int EXIT_LOC_LIST_REQUEST_CODE = 2;

    private static final String ROUTE_LIST_KEY = "routes";

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

        routeListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Intent i = new Intent(getApplicationContext(), LocationListActivity.class);
                i.putExtra(LocationListActivity.ROUTE_ID_KEY, id);
                startActivityForResult(i, EXIT_LOC_LIST_REQUEST_CODE);
            }
        });
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

    public void addRouteOnClick(MenuItem item) {
        startActivityForResult(new Intent(getApplicationContext(), RouteAddActivity.class), ADD_ROUTE_REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ( resultCode == Activity.RESULT_OK) {
            if (requestCode == ADD_ROUTE_REQUEST_CODE) {
                RouteModel newRoute = new RouteModel(
                        data.getStringExtra(RouteAddActivity.NAME_RESULT_KEY),
                        data.getLongExtra(RouteAddActivity.DATE_RESULT_KEY, Calendar.getInstance().getTimeInMillis())
                );
                DatabaseHelper.getInstance(this).addOrUpdateRoute(newRoute);
                routeListAdapter.addItem(newRoute);
            } else if (requestCode == EXIT_LOC_LIST_REQUEST_CODE) {
                long routeId = data.getLongExtra(LocationListActivity.ROUTE_ID_KEY, -1);
                for (RouteModel routeModel : routeListAdapter.getRoutes()) {
                    if (routeModel.id == routeId) {
                        routeModel.locations = DatabaseHelper.getInstance(this).getAllRouteLocations(routeId);
                        routeListAdapter.notifyDataSetChanged();
                        break;
                    }
                }
            }
        }
    }
}
