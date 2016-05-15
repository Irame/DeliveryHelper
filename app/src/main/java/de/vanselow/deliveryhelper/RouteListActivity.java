package de.vanselow.deliveryhelper;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import java.util.ArrayList;

import de.vanselow.deliveryhelper.utils.DatabaseHelper;
import pl.com.salsoft.sqlitestudioremote.SQLiteStudioService;

public class RouteListActivity extends AppCompatActivity {
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
}
