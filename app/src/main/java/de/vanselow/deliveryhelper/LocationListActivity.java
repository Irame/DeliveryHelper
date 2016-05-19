package de.vanselow.deliveryhelper;

import android.app.Activity;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.android.gms.location.places.Place;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

import de.vanselow.deliveryhelper.googleapi.RequestClient;
import de.vanselow.deliveryhelper.utils.DatabaseHelper;
import de.vanselow.deliveryhelper.utils.GeoLocationCache;

public class LocationListActivity extends AppCompatActivity {
    public static final int ADD_LOCATION_REQUEST_CODE = 1;

    public static final String ROUTE_KEY = "route";

    private GeoLocationCache geoLocationCache;
    private LocationListAdapter locationListAdapter;
    private RouteModel routeModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_list);

        if (savedInstanceState != null) {
            routeModel = savedInstanceState.getParcelable(ROUTE_KEY);
        }
        if (routeModel == null) {
            routeModel = getIntent().getParcelableExtra(ROUTE_KEY);
        }
        if (routeModel.id < 0) {
            finish();
            return;
        }
        locationListAdapter = new LocationListAdapter(this, routeModel.locations);
        ListView locationListView = (ListView) findViewById(R.id.location_list);
        assert locationListView != null;
        locationListView.setAdapter(locationListAdapter);

        geoLocationCache = new GeoLocationCache(this);

        setResult(Activity.RESULT_CANCELED, getIntent());
    }


    @Override
    public void onBackPressed() {
        Intent data = new Intent();
        data.putExtra(ROUTE_KEY, routeModel);
        setResult(Activity.RESULT_OK, data);
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelable(ROUTE_KEY, routeModel);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.location_list_menu, menu);
        return true;
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ADD_LOCATION_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            LocationModel newLocation = new LocationModel(
                    data.getStringExtra(LocationAddActivity.NAME_RESULT_KEY),
                    (Place) data.getParcelableExtra(LocationAddActivity.ADDRESS_RESULT_KEY)){{
                price = data.getFloatExtra(LocationAddActivity.PRICE_RESULT_KEY, 0);
                notes = data.getStringExtra(LocationAddActivity.NOTES_RESULT_KEY);
            }};
            DatabaseHelper.getInstance(this).addOrUpdateRouteLocation(newLocation, routeModel.id);
            routeModel.locations.add(newLocation);
            locationListAdapter.addItem(newLocation);
        }
    }

    public void addLocationOnClick(MenuItem item) {
        startActivityForResult(new Intent(getApplicationContext(), LocationAddActivity.class), ADD_LOCATION_REQUEST_CODE);
    }

    public void sortLocationsOnClick(final MenuItem item) {
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        ImageView iv = (ImageView)inflater.inflate(R.layout.iv_sort_locations, null);
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_menu_item);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
        item.setActionView(iv);

        Map<String, String> params = new HashMap<>();
        final Location location = geoLocationCache.getBestLocation();
        params.put("origin", location.getLatitude() + "," + location.getLongitude());
        params.put("destination", "Apotheke+Vanselow,Schönbornstraße+19,97440+Werneck");
        StringBuilder waypoints = new StringBuilder("optimize:true");
        for (LocationModel loc : locationListAdapter.getValuesForSection(LocationModel.State.OPEN)) {
            waypoints.append("|").append(loc.latitude).append(",").append(loc.longitude);
        }
        params.put("waypoints", waypoints.toString());
        new RequestClient(){
            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                try {
                    JSONArray order = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONArray("waypoint_order");
                    ArrayList<LocationModel> locationList = locationListAdapter.getValuesForSection(LocationModel.State.OPEN);

                    if (locationList.size() != order.length())
                        return;

                    int[] orderArray = new int[order.length()];
                    for (int i = 0; i < orderArray.length; i++) {
                        orderArray[i] = order.getInt(i);
                    }
                    Collections.sort(locationList, new LocationComparator(locationList, orderArray));
                    locationListAdapter.selectedItemPosition = -1;
                    locationListAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                item.getActionView().clearAnimation();
                item.setActionView(null);
            }
        }.execute("maps", "directions", params);
    }

    private class LocationComparator implements Comparator<LocationModel> {
        private Map<LocationModel, Integer> orderMap;

        LocationComparator(ArrayList<LocationModel> origList, int[] order) {
            orderMap = new HashMap<>();
            for (int i = 0; i < order.length; i++) {
                orderMap.put(origList.get(order[i]), i);
            }
        }

        @Override
        public int compare(LocationModel lhs, LocationModel rhs) {
            return Integer.compare(orderMap.get(lhs), orderMap.get(rhs));
        }
    }
}
