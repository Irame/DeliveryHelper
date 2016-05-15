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

import pl.com.salsoft.sqlitestudioremote.SQLiteStudioService;

public class RouteListActivity extends AppCompatActivity {
    private static final int ADD_ROUTE_REQUEST_CODE = 1;

    private RouteListAdapter routeListAdapter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_route_list);

        // for external sqlite browser
        SQLiteStudioService.instance().start(this);


        ArrayList<RouteModel> routeList = null;
        if (savedInstanceState != null) {
            routeList = savedInstanceState.getParcelableArrayList("routes");
        }
        if (routeList == null) {
            routeList = new ArrayList<>();
        }
        routeListAdapter = new RouteListAdapter(this, routeList);
        ListView routeListView = (ListView) findViewById(R.id.route_list);
        assert routeListView != null;
        routeListView.setAdapter(routeListAdapter);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList("locations", routeListAdapter.getRoutes());
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
        if (requestCode == ADD_ROUTE_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            RouteModel newRoute = new RouteModel(
                    data.getStringExtra(RouteAddActivity.NAME_RESULT_KEY),
                    data.getLongExtra(RouteAddActivity.DATE_RESULT_KEY, Calendar.getInstance().getTimeInMillis())
            );
            routeListAdapter.addItem(newRoute);
        }
    }
}
