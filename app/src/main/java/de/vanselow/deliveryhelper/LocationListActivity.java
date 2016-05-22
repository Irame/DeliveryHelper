package de.vanselow.deliveryhelper;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.Uri;
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

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;
import java.util.Map;

import de.vanselow.deliveryhelper.googleapi.RouteInfo;
import de.vanselow.deliveryhelper.googleapi.RouteInfoRequestClient;
import de.vanselow.deliveryhelper.utils.DatabaseHelper;
import de.vanselow.deliveryhelper.utils.GeoLocationCache;
import de.vanselow.deliveryhelper.utils.Utils;

public class LocationListActivity extends AppCompatActivity {
    public static final int ADD_LOCATION_REQUEST_CODE = 1;
    public static final int EDIT_LOCATION_REQUEST_CODE = 2;

    public static final String ROUTE_KEY = "route";

    private static final LatLng ROUTE_END = new LatLng(49.982545, 10.097857);

    private MapFragment mapFragment;
    private RouteInfoRequestClient<LocationModel> routeInfoRequestClient;
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

        routeInfoRequestClient = new RouteInfoRequestClient<LocationModel>(getApplicationContext()) {
            @Override
            public LatLng toLatLng(LocationModel item) {
                return new LatLng(item.latitude, item.longitude);
            }
        };
        routeInfoRequestClient.setDestination(ROUTE_END);

        setResult(Activity.RESULT_CANCELED, getIntent());
    }


    @Override
    public void onBackPressed() {
        View mapView = findViewById(R.id.location_list_map_wrapper);
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
                routeInfoRequestClient.invalidateLatestRoute();
            } else if (requestCode == EDIT_LOCATION_REQUEST_CODE) {
                LocationModel editedLocation = data.getParcelableExtra(LocationAddActivity.LOCATION_RESULT_KEY);
                LocationModel updatedLocation = locationListAdapter.updateItem(editedLocation);
                routeInfoRequestClient.invalidateLatestRoute();
                if (updatedLocation != null)
                    DatabaseHelper.getInstance(this).addOrUpdateRouteLocation(updatedLocation, routeModel.id);
            }
        }
    }

    public void addLocationOnClick(MenuItem item) {
        hideMap();
        startActivityForResult(new Intent(getApplicationContext(), LocationAddActivity.class), ADD_LOCATION_REQUEST_CODE);
    }

    public void sortLocationsOnClick(final MenuItem item) {
        hideMap();
        LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
        ImageView iv = (ImageView) inflater.inflate(R.layout.iv_sort_locations, null);
        Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_menu_item);
        rotation.setRepeatCount(Animation.INFINITE);
        iv.startAnimation(rotation);
        item.setActionView(iv);

        final ArrayList<LocationModel> locations = locationListAdapter.getValuesForSection(LocationModel.State.OPEN);
        routeInfoRequestClient.getRouteInfo(locations,
                new RouteInfoRequestClient.Callback<LocationModel>() {
                    @Override
                    public void onRouteInfoResult(RouteInfo<LocationModel> routeInfo) {
                        Collections.sort(locations, new LocationComparator(routeInfo.waypointOrder));
                        locationListAdapter.notifyDataSetChanged();
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
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                Location bestLoc = GeoLocationCache.getIncetance(getApplicationContext()).getBestLocation();
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(bestLoc.getLatitude(), bestLoc.getLongitude()), 13));
            }
        });
        fragmentTransaction.add(R.id.location_list_map_placeholder, mapFragment);
        fragmentTransaction.commit();
    }

    public void mapNavigationButtonOnClick(View view) {
        final ArrayList<LocationModel> locations = locationListAdapter.getValuesForSection(LocationModel.State.OPEN);
        if (locations.isEmpty()) {
            Utils.startNavigation(this, ROUTE_END);
        } else {
            LocationModel loc = locations.get(0);
            Utils.startNavigation(this, new LatLng(loc.latitude, loc.longitude));
        }
    }

    private class MapRouteData {
        GoogleMap googleMap = null;
        String polyline = null;
        LatLngBounds bounds = null;
    }

    private void showMap() {
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
                View mapView = findViewById(R.id.location_list_map_wrapper);
                assert mapView != null;
                mapView.setVisibility(View.VISIBLE);

                synchronized (mapRouteData) {
                    mapRouteData.googleMap = googleMap;
                    setMapRouteData(mapRouteData);
                }
            }
        });
        routeInfoRequestClient.getRouteInfo(
                locationListAdapter.getValuesForSection(LocationModel.State.OPEN),
                new RouteInfoRequestClient.Callback<LocationModel>() {
                    @Override
                    public void onRouteInfoResult(RouteInfo<LocationModel> routeInfo) {
                        Collections.sort(locationListAdapter.getValuesForSection(LocationModel.State.OPEN), new LocationComparator(routeInfo.waypointOrder));
                        locationListAdapter.notifyDataSetChanged();
                        synchronized (mapRouteData) {
                            mapRouteData.polyline = routeInfo.overviewPolyline;
                            mapRouteData.bounds = routeInfo.bounds;
                            setMapRouteData(mapRouteData);
                        }
                    }
                });
    }

    private void hideMap() {
        View mapView = findViewById(R.id.location_list_map_wrapper);
        assert mapView != null;
        mapView.setVisibility(View.INVISIBLE);
    }

    private void toggleMap() {
        View mapView = findViewById(R.id.location_list_map_wrapper);
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

        LocationComparator(Map<LocationModel, Integer> orderMap) {
            this.orderMap = orderMap;
        }

        @Override
        public int compare(LocationModel lhs, LocationModel rhs) {
            return Integer.compare(orderMap.get(lhs), orderMap.get(rhs));
        }
    }
}
