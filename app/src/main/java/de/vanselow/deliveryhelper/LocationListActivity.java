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
    public static final int ADD_LOCATION_REQUEST = 1;

    public static final String ROUTE_ID_KEY = "route_id";
    public static final String LOCATION_LIST_KEY = "locations";

    private GeoLocationCache geoLocationCache;
    private LocationListAdapter locationListAdapter;
    private long routeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_list);

        ArrayList<LocationModel> locationList = null;
        if (savedInstanceState != null) {
            routeId = savedInstanceState.getLong(ROUTE_ID_KEY);
            locationList = savedInstanceState.getParcelableArrayList(LOCATION_LIST_KEY);
        }
        if (locationList == null) {
            long rId = getIntent().getLongExtra(ROUTE_ID_KEY, -1);
            if (rId < 0) {
                finish();
                return;
            } else {
                routeId = rId;
                locationList = DatabaseHelper.getInstance(this).getAllRouteLocations(routeId);
            }
        }
        locationListAdapter = new LocationListAdapter(this, locationList);
        ListView locationListView = (ListView) findViewById(R.id.location_list);
        assert locationListView != null;
        locationListView.setAdapter(locationListAdapter);

        geoLocationCache = new GeoLocationCache(this);

        setResult(Activity.RESULT_OK, getIntent());
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putParcelableArrayList(LOCATION_LIST_KEY, locationListAdapter.getAllValues());
        outState.putLong(ROUTE_ID_KEY, routeId);
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
        if (requestCode == ADD_LOCATION_REQUEST && resultCode == Activity.RESULT_OK) {
            LocationModel newLocation = new LocationModel(
                    data.getStringExtra(AddLocationActivity.NAME_RESULT_KEY),
                    (Place) data.getParcelableExtra(AddLocationActivity.ADDRESS_RESULT_KEY)){{
                price = data.getFloatExtra(AddLocationActivity.PRICE_RESULT_KEY, 0);
                notes = data.getStringExtra(AddLocationActivity.NOTES_RESULT_KEY);
            }};
            DatabaseHelper.getInstance(this).addOrUpdateRouteLocation(newLocation, routeId);
            locationListAdapter.addItem(newLocation);
        }
    }

    public void addLocationOnClick(MenuItem item) {
        startActivityForResult(new Intent(getApplicationContext(), AddLocationActivity.class), ADD_LOCATION_REQUEST);
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
