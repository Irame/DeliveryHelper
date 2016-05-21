package de.vanselow.deliveryhelper;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageView;
import android.widget.ListView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
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
    public static final int EDIT_LOCATION_REQUEST_CODE = 2;

    public static final String ROUTE_KEY = "route";

    private MapFragment mapFragment;
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
        locationListAdapter = new LocationListAdapter(this, routeModel);
        ListView locationListView = (ListView) findViewById(R.id.location_list);
        assert locationListView != null;
        locationListView.setAdapter(locationListAdapter);

        geoLocationCache = new GeoLocationCache(this);

        setResult(Activity.RESULT_CANCELED, getIntent());
    }


    @Override
    public void onBackPressed() {
        View mapView = findViewById(R.id.location_list_map_placeholder);
        assert mapView != null;
        if (mapView.getVisibility() == View.VISIBLE) {
            hideMap();
            return;
        }

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
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ADD_LOCATION_REQUEST_CODE) {
                LocationModel newLocation = data.getParcelableExtra(LocationAddActivity.LOCATION_RESULT_KEY);
                DatabaseHelper.getInstance(this).addOrUpdateRouteLocation(newLocation, routeModel.id);
                routeModel.locations.add(newLocation);
                locationListAdapter.addItem(newLocation);
            } else if (requestCode == EDIT_LOCATION_REQUEST_CODE) {
                LocationModel editedLocation = data.getParcelableExtra(LocationAddActivity.LOCATION_RESULT_KEY);
                LocationModel updatedLocation = locationListAdapter.updateItem(editedLocation);
                if (updatedLocation != null)
                    DatabaseHelper.getInstance(this).addOrUpdateRouteLocation(updatedLocation, routeModel.id);
            }
        }
    }

    public void addLocationOnClick(MenuItem item) {
        hideMap();
        startActivityForResult(new Intent(getApplicationContext(), LocationAddActivity.class), ADD_LOCATION_REQUEST_CODE);
    }

    private void routeRequest(RequestClient requestClient) {
        Map<String, String> params = new HashMap<>();
        final Location location = geoLocationCache.getBestLocation();
        params.put("origin", location.getLatitude() + "," + location.getLongitude());
        params.put("destination", "49.982545,10.097857");
        StringBuilder waypoints = new StringBuilder("optimize:true");
        for (LocationModel loc : locationListAdapter.getValuesForSection(LocationModel.State.OPEN)) {
            waypoints.append("|").append(loc.latitude).append(",").append(loc.longitude);
        }
        params.put("waypoints", waypoints.toString());
        requestClient.execute("maps", "directions", params);
    }

    public void sortLocationsOnClick(final MenuItem item) {
        hideMap();
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        ImageView iv = (ImageView) inflater.inflate(R.layout.iv_sort_locations, null);
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_menu_item);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
        item.setActionView(iv);

        routeRequest(new RequestClient() {
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
                    locationListAdapter.notifyDataSetChanged();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
                item.getActionView().clearAnimation();
                item.setActionView(null);
            }
        });
    }

    public void mapLocationsOnClick(MenuItem item) {
        toggleMap();
    }

    private void ensureMapFragment() {
        if (mapFragment != null) return;
        mapFragment = MapFragment.newInstance();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                        && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                googleMap.setMyLocationEnabled(true);

                Location bestLoc = geoLocationCache.getBestLocation();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(bestLoc.getLatitude(), bestLoc.getLongitude()), 13));
            }
        });
        fragmentTransaction.add(R.id.location_list_map_placeholder, mapFragment);
        fragmentTransaction.commit();
    }

    private class MapRouteData {
        GoogleMap googleMap = null;
        String polyline = null;
        LatLngBounds bounds = null;
    }

    private void showMap() {
        View mapView = findViewById(R.id.location_list_map_placeholder);
        assert mapView != null;
        mapView.setVisibility(View.VISIBLE);

        ensureMapFragment();

        final MapRouteData mapRouteData = new MapRouteData();
        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                googleMap.clear();

                ArrayList<LocationModel> locations = locationListAdapter.getValuesForSection(LocationModel.State.OPEN);
                NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
                for (LocationModel location : locations) {
                    googleMap.addMarker(new MarkerOptions()
                            .position(new LatLng(location.latitude, location.longitude))
                            .title(location.name + " - " + currencyFormat.format(location.price))
                            .snippet(location.address));
                }

                synchronized (mapRouteData) {
                    mapRouteData.googleMap = googleMap;
                    setMapRouteData(mapRouteData);
                }
            }
        });
        routeRequest(new RequestClient(){
            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                try {
                    JSONObject route = jsonObject.getJSONArray("routes").getJSONObject(0);

                    String polyline = route.getJSONObject("overview_polyline").getString("points");

                    JSONObject bounds = route.getJSONObject("bounds");
                    JSONObject northeast = bounds.getJSONObject("northeast");
                    LatLng northeastBound = new LatLng(northeast.getDouble("lat"), northeast.getDouble("lng"));
                    JSONObject southwest = bounds.getJSONObject("southwest");
                    LatLng southwestBound = new LatLng(southwest.getDouble("lat"), southwest.getDouble("lng"));

                    synchronized (mapRouteData) {
                        mapRouteData.polyline = polyline;
                        mapRouteData.bounds = new LatLngBounds(southwestBound, northeastBound);
                        setMapRouteData(mapRouteData);
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void hideMap() {
        View mapView = findViewById(R.id.location_list_map_placeholder);
        assert mapView != null;
        mapView.setVisibility(View.GONE);
    }

    private void toggleMap() {
        View mapView = findViewById(R.id.location_list_map_placeholder);
        assert mapView != null;

        if (mapView.getVisibility() == View.VISIBLE)
            hideMap();
        else
            showMap();
    }

    private void setMapRouteData(MapRouteData mapRouteData) {
        if (mapRouteData.googleMap == null
                || mapRouteData.polyline == null
                || mapRouteData.bounds == null)
            return;

        PolylineOptions polylineOptions = new PolylineOptions().addAll(PolyUtil.decode(mapRouteData.polyline));
        polylineOptions.color(getResources().getColor(R.color.colorGoogleMapsPolyline));
        mapRouteData.googleMap.addPolyline(polylineOptions);

        mapRouteData.googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(mapRouteData.bounds, 50));
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
