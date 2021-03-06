package de.vanselow.deliveryhelper.googleapi;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.support.annotation.Nullable;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.vanselow.deliveryhelper.R;
import de.vanselow.deliveryhelper.utils.GeoLocationCache;

public abstract class RouteInfoRequestClient<T> {
    private static final String TAG = RouteInfoRequestClient.class.getName();
    private GeoLocationCache geoLocationCache;
    private RouteInfo<T> latestRouteInfo;
    private boolean validData;
    private boolean shouldRequest;

    private Toast failedToConnectToast;
    private Toast noRouteFoundToast;
    private Toast errorGettingRouteToast;

    private LatLng origin;
    private LatLng destination;

    private RequestClient requestClient;
    private GeoLocationCache.Listener geoLocationChangedListener;

    public RouteInfoRequestClient(Context context, GeoLocationCache geoLocationCache) {
        this.geoLocationCache = geoLocationCache;
        geoLocationChangedListener = new GeoLocationCache.Listener() {
            @Override
            public void onGeoLocationChanged(Location location) {
                invalidateLatestRoute(false);
            }
        };
        validData = false;
        shouldRequest = true;

        failedToConnectToast = Toast.makeText(context, R.string.failed_to_connect, Toast.LENGTH_SHORT);
        noRouteFoundToast = Toast.makeText(context, R.string.no_route_found, Toast.LENGTH_SHORT);
        errorGettingRouteToast = Toast.makeText(context, R.string.failed_retrieve_route, Toast.LENGTH_SHORT);
    }

    public void startInvalidateOnGeoLocationChanges() {
        geoLocationCache.addGeoLocationListener(geoLocationChangedListener);
    }

    public void stopInvalidateOnGeoLocationChanges() {
        geoLocationCache.removeGeoLocationListener(geoLocationChangedListener);
    }

    public void setOrigin(LatLng origin) {
        this.origin = origin;
        invalidateLatestRoute(true);
    }

    public void setDestination(LatLng destination) {
        this.destination = destination;
        invalidateLatestRoute(true);
    }

    private LatLng getOrigin() {
        if (origin == null) {
            Location location = geoLocationCache.getBestLocation();
            if (location == null) return null;
            return new LatLng(location.getLatitude(), location.getLongitude());
        } else
            return origin;
    }

    private LatLng getDestination() {
        if (destination == null) {
            Location location = geoLocationCache.getBestLocation();
            if (location == null) return null;
            return new LatLng(location.getLatitude(), location.getLongitude());
        } else
            return destination;
    }

    private void requestRouteInfo(List<T> locations, final Callback<T> callback) {
        final List<T> locationCopy = new ArrayList<>(locations);
        Map<String, String> params = new HashMap<>();
        LatLng o = getOrigin();
        LatLng d = getDestination();
        if (o == null || d == null) {
            errorGettingRouteToast.show();
            callback.onRouteInfoResult(null);
            return;
        }

        params.put("origin", o.latitude + "," + o.longitude);
        params.put("destination", d.latitude + "," + d.longitude);
        StringBuilder waypoints = new StringBuilder("optimize:true");
        for (T loc : locationCopy) {
            LatLng latLng = toLatLng(loc);
            waypoints.append("|").append(latLng.latitude).append(",").append(latLng.longitude);
        }
        params.put("waypoints", waypoints.toString());
        requestClient = new RequestClient() {
            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                latestRouteInfo = null;
                if (jsonObject == null) {
                    failedToConnectToast.show();
                } else {
                    try {
                        String status = jsonObject.getString("status");

                        if (status.equals("OK")) {
                            latestRouteInfo = new RouteInfo<>();
                            JSONArray order = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONArray("waypoint_order");
                            if (locationCopy.size() == order.length()) {
                                for (int i = 0; i < locationCopy.size(); i++) {
                                    latestRouteInfo.waypointOrder.put(locationCopy.get(order.getInt(i)), i);
                                }
                            }

                            JSONObject route = jsonObject.getJSONArray("routes").getJSONObject(0);
                            latestRouteInfo.overviewPolyline = route.getJSONObject("overview_polyline").getString("points");

                            JSONObject bounds = route.getJSONObject("bounds");
                            JSONObject northeast = bounds.getJSONObject("northeast");
                            LatLng northeastBound = new LatLng(northeast.getDouble("lat"), northeast.getDouble("lng"));
                            JSONObject southwest = bounds.getJSONObject("southwest");
                            LatLng southwestBound = new LatLng(southwest.getDouble("lat"), southwest.getDouble("lng"));
                            latestRouteInfo.bounds = new LatLngBounds(southwestBound, northeastBound);

                            validData = true;
                        } else if (status.equals("ZERO_RESULTS")) {
                            noRouteFoundToast.show();
                        } else {
                            errorGettingRouteToast.show();
                        }
                    } catch (JSONException e) {
                        Log.e(TAG, "Could not parse JSON: " + jsonObject.toString() + " (Error: " + e.getMessage() + ")");
                        errorGettingRouteToast.show();
                    }
                }
                callback.onRouteInfoResult(latestRouteInfo);
                shouldRequest = true;
            }
        };
        shouldRequest = false;
        requestClient.execute("maps", "directions", params);
    }

    public void getRouteInfo(final List<T> locations, Callback<T> callback) {
        if (validData) {
            callback.onRouteInfoResult(latestRouteInfo);
        } else if (shouldRequest) {
            cancelRequest();
            requestRouteInfo(locations, callback);
        }
    }

    public void invalidateLatestRoute(boolean cancelRequest) {
        if (cancelRequest) cancelRequest();
        else shouldRequest = true;    // to restart request at next request
        validData = false;
    }

    public void cancelRequest() {
        if (requestClient != null && requestClient.getStatus() != AsyncTask.Status.FINISHED) {
            requestClient.cancel(true);
        }
        shouldRequest = true;
    }

    protected abstract LatLng toLatLng(T item);

    public interface Callback<T> {
        void onRouteInfoResult(@Nullable RouteInfo<T> routeInfo);
    }
}

