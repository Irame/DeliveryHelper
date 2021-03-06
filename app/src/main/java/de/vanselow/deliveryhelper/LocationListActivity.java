package de.vanselow.deliveryhelper;

import android.Manifest;
import android.app.Activity;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.Nullable;
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

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapFragment;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.PolyUtil;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import de.vanselow.deliveryhelper.googleapi.RouteInfo;
import de.vanselow.deliveryhelper.googleapi.RouteInfoRequestClient;
import de.vanselow.deliveryhelper.utils.DatabaseAsync;
import de.vanselow.deliveryhelper.utils.GeoLocationCache;
import de.vanselow.deliveryhelper.utils.Settings;
import de.vanselow.deliveryhelper.utils.Utils;
import se.emilsjolander.stickylistheaders.StickyListHeadersListView;

public class LocationListActivity extends AppCompatActivity {
    public static final int ADD_LOCATION_REQUEST_CODE = 1;
    public static final int EDIT_LOCATION_REQUEST_CODE = 2;
    public static final int EXIT_SETTINGS_REQUEST_CODE = 3;

    public static final String ROUTE_ID_KEY = "routeId";
    public static final String IS_SORTING_KEY = "isSoring";
    public static final String LOCATION_LIST_ADAPTER_KEY = "locationListAdapter";

    private static final LatLng ROUTE_END = new LatLng(49.982545, 10.097857);

    private MapFragment mapFragment;
    private GeoLocationCache geoLocationCache;
    private RouteInfoRequestClient<LocationModel> routeInfoRequestClient;
    private LocationListAdapter locationListAdapter;
    private long routeId = -1;
    private boolean isSorting = false;

    private BitmapDescriptor openDeliveryMarkerIcon;
    private BitmapDescriptor deliveredDeliveryMarkerIcon;

    private Menu optionsMenu;

    private RouteInfo<LocationModel> routeInfo;
    private GoogleMap googleMap;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_location_list);

        boolean needToUpdateListViewAdapter;
        if (savedInstanceState != null) {
            routeId = savedInstanceState.getLong(ROUTE_ID_KEY, -1);
            isSorting = savedInstanceState.getBoolean(IS_SORTING_KEY);
            locationListAdapter = savedInstanceState.getParcelable(LOCATION_LIST_ADAPTER_KEY);
            needToUpdateListViewAdapter = false;
            findViewById(R.id.location_list_loading_panel).setVisibility(View.GONE);
        } else {
            if (getIntent() == null || (routeId = getIntent().getLongExtra(ROUTE_ID_KEY, -1)) < 0) {
                finish();
                return;
            } else {
                locationListAdapter = new LocationListAdapter(this, routeId);
                needToUpdateListViewAdapter = true;
            }
        }

        locationListAdapter.setItemCollectionChangedListener(new LocationListAdapter.ItemCollectionChangedListener() {
            @Override
            public void onChanged() {
                routeInfoRequestClient.invalidateLatestRoute(true);
                autosortOpenLocationsByRouteIfOptionSelected();
            }
        });

        StickyListHeadersListView locationListView = (StickyListHeadersListView) findViewById(R.id.location_list);
        assert locationListView != null;
        locationListView.setAdapter(locationListAdapter);

        if (needToUpdateListViewAdapter)
            locationListAdapter.updateAllLocationsFromDatabase();

        geoLocationCache = new GeoLocationCache(this);
        routeInfoRequestClient = new RouteInfoRequestClient<LocationModel>(getApplicationContext(), geoLocationCache) {
            @Override
            public LatLng toLatLng(LocationModel item) {
                return new LatLng(item.place.latitude, item.place.longitude);
            }
        };
        routeInfoRequestClient.setDestination(ROUTE_END);
        routeInfoRequestClient.startInvalidateOnGeoLocationChanges();

        MapsInitializer.initialize(getApplicationContext());
        openDeliveryMarkerIcon = Utils.getBitmapDescriptor(getDrawable(R.drawable.ic_open_delivery));
        deliveredDeliveryMarkerIcon = Utils.getBitmapDescriptor(getDrawable(R.drawable.ic_delivered_delivery));

        final Context context = this;
        RemoteAccess.setLocationsDataReceivedListener(this, new RemoteAccess.LocationsReceivedListener(this) {
            @Override
            public void onLocationsReceived(ArrayList<LocationModel> locations) {
                for (LocationModel location : locations) {
                    if (location.id < 0) {
                        DatabaseAsync.getInstance(context).addOrUpdateRouteLocation(location, routeId, new DatabaseAsync.Callback<Long>() {
                            @Override
                            public void onPostExecute(Long locationId) {
                                locationListAdapter.updateLocationFromDatabase(locationId);
                            }
                        });
                    }
                }
            }
        });

        setResult(Activity.RESULT_CANCELED, getIntent());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        routeInfoRequestClient.stopInvalidateOnGeoLocationChanges();
        routeInfoRequestClient.cancelRequest();
        RemoteAccess.setLocationsDataReceivedListener(this, null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        geoLocationCache.start();
    }

    @Override
    protected void onPause() {
        super.onPause();
        geoLocationCache.stop();
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
        data.putExtra(ROUTE_ID_KEY, routeId);
        setResult(Activity.RESULT_OK, data);
        super.onBackPressed();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putLong(ROUTE_ID_KEY, routeId);
        outState.putParcelable(LOCATION_LIST_ADAPTER_KEY, locationListAdapter);
        outState.putBoolean(IS_SORTING_KEY, isSorting);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.location_list_menu, menu);
        optionsMenu = menu;
        if (isSorting) startSortIconAnimation();
        return true;
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == Activity.RESULT_OK) {
            if (requestCode == ADD_LOCATION_REQUEST_CODE || requestCode == EDIT_LOCATION_REQUEST_CODE) {
                long locationId = data.getLongExtra(LocationAddActivity.LOCATION_ID_KEY, -1);
                if (locationId > 0) locationListAdapter.updateLocationFromDatabase(locationId);
            }
        }
        if (requestCode == EXIT_SETTINGS_REQUEST_CODE) {
            autosortOpenLocationsByRouteIfOptionSelected();
        }
    }

    public void addLocationOnClick(MenuItem item) {
        hideMap();
        Intent intent = new Intent(getApplicationContext(), LocationAddActivity.class);
        intent.putExtra(LocationAddActivity.ROUTE_ID_KEY, routeId);
        startActivityForResult(intent, ADD_LOCATION_REQUEST_CODE);
    }

    public void sortLocationsByRouteOnClick(final MenuItem item) {
        hideMap();
        updateRouteInfo();
    }

    public void sortLocationsAlphabeticalOnClick(MenuItem item) {
        hideMap();
        locationListAdapter.resetCustomSort(LocationModel.State.OPEN);
    }

    public void openSettingsOnClick(MenuItem item) {
        hideMap();
        startActivityForResult(new Intent(getApplicationContext(), SettingsActivity.class), EXIT_SETTINGS_REQUEST_CODE);
    }

    private void autosortOpenLocationsByRouteIfOptionSelected() {
        if (Settings.isAutosortByRouteForOpenLocationsEnabled(this)) {
            updateRouteInfo();
        }
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
                Location bestLoc = geoLocationCache.getBestLocation();
                if (bestLoc != null)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(bestLoc.getLatitude(), bestLoc.getLongitude()), 13));
            }
        });
        fragmentTransaction.add(R.id.location_list_map_placeholder, mapFragment);
        fragmentTransaction.commit();
    }

    public void mapNavigationButtonOnClick(View view) {
        final List<LocationModel> locations = locationListAdapter.getValuesForSection(LocationModel.State.OPEN);
        if (locations.isEmpty()) {
            Utils.startNavigation(this, ROUTE_END);
        } else {
            LocationModel loc = locations.get(0);
            Utils.startNavigation(this, new LatLng(loc.place.latitude, loc.place.longitude));
        }
    }

    private void showMap() {
        ensureMapFragment();

        mapFragment.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap gMap) {
                googleMap = gMap;
                gMap.clear();

                List<LocationModel> locations = locationListAdapter.getAllValues();
                if (!locations.isEmpty()) {
                    NumberFormat currencyFormat = NumberFormat.getCurrencyInstance();
                    LatLngBounds.Builder boundsBuilder = new LatLngBounds.Builder();
                    for (LocationModel location : locations) {
                        LatLng loc = new LatLng(location.place.latitude, location.place.longitude);
                        gMap.addMarker(new MarkerOptions()
                                .position(new LatLng(location.place.latitude, location.place.longitude))
                                .title(location.name + " - " + currencyFormat.format(location.price))
                                .snippet(location.place.address)
                                .icon(location.state == LocationModel.State.OPEN
                                        ? openDeliveryMarkerIcon : deliveredDeliveryMarkerIcon));
                        boundsBuilder.include(loc);
                    }
                    gMap.moveCamera(CameraUpdateFactory.newLatLngBounds(boundsBuilder.build(), 50));
                }
                View mapView = findViewById(R.id.location_list_map_wrapper);
                assert mapView != null;
                mapView.setVisibility(View.VISIBLE);
                updateMapRouteData();
            }
        });
        updateRouteInfo();
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

    private void updateMapRouteData() {
        if (googleMap == null
                || routeInfo == null
                || routeInfo.overviewPolyline == null
                || routeInfo.bounds == null)
            return;

        PolylineOptions polylineOptions = new PolylineOptions().addAll(PolyUtil.decode(routeInfo.overviewPolyline));
        polylineOptions.color(getResources().getColor(R.color.colorGoogleMapsPolyline));
        googleMap.addPolyline(polylineOptions);

        googleMap.animateCamera(CameraUpdateFactory.newLatLngBounds(routeInfo.bounds, 50));
    }


    private void startSortIconAnimation() {
        stopSortIconAnimation();
        isSorting = true;
        if (optionsMenu == null) return;
        MenuItem item = optionsMenu.findItem(R.id.action_locations_sort_route);
        if (item != null) {
            LayoutInflater inflater = LayoutInflater.from(getApplicationContext());
            ImageView iv = (ImageView) inflater.inflate(R.layout.iv_sort_locations, null);
            Animation rotation = AnimationUtils.loadAnimation(this, R.anim.rotate_menu_item);
            rotation.setRepeatCount(Animation.INFINITE);
            iv.startAnimation(rotation);
            item.setActionView(iv);
        }
    }

    private void stopSortIconAnimation() {
        isSorting = false;
        if (optionsMenu == null) return;
        MenuItem item = optionsMenu.findItem(R.id.action_locations_sort_route);
        if (item != null && item.getActionView() != null) {
            item.getActionView().clearAnimation();
            item.setActionView(null);
        }
    }

    private void updateRouteInfo() {
        startSortIconAnimation();
        routeInfoRequestClient.getRouteInfo(
                locationListAdapter.getValuesForSection(LocationModel.State.OPEN),
                new RouteInfoRequestClient.Callback<LocationModel>() {
                    @Override
                    public void onRouteInfoResult(@Nullable RouteInfo<LocationModel> requestedRouteInfo) {
                        if (requestedRouteInfo != null) {
                            routeInfo = requestedRouteInfo;
                            locationListAdapter.customSort(LocationModel.State.OPEN, new LocationComparator(routeInfo.waypointOrder));
                            updateMapRouteData();
                        }
                        stopSortIconAnimation();
                    }
                });
    }

    private class LocationComparator implements Comparator<LocationModel> {
        private Map<LocationModel, Integer> orderMap;

        LocationComparator(Map<LocationModel, Integer> orderMap) {
            this.orderMap = orderMap;
        }

        @Override
        public int compare(LocationModel lhs, LocationModel rhs) {
            if (lhs.state == LocationModel.State.OPEN && rhs.state == LocationModel.State.OPEN)
                return Integer.compare(orderMap.get(lhs), orderMap.get(rhs));
            else if (lhs.state == LocationModel.State.OPEN)
                return -1;
            else if (rhs.state == LocationModel.State.OPEN)
                return 1;
            else
                return lhs.name.compareTo(rhs.name);
        }
    }
}
