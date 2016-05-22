package de.vanselow.deliveryhelper.googleapi;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import de.vanselow.deliveryhelper.utils.GeoLocationCache;

public abstract class RouteInfoRequestClient<T> {
    private static final String TAG = RouteInfoRequestClient.class.getName();
    private Context context;
    private RouteInfo<T> latestRouteInfo;
    private boolean validData;

    private LatLng origin;
    private LatLng destination;

    public RouteInfoRequestClient(Context context) {
        this.context = context;
        GeoLocationCache.getIncetance(context).addGeoLocationListener(
                new GeoLocationCache.Listener() {
                    @Override
                    public void onGeoLocationChanged(Location location) {
                        invalidateLatestRoute();
                    }
                });
        validData = false;
    }

    public void setOrigin(LatLng origin) {
        this.origin = origin;
        invalidateLatestRoute();
    }

    public void setDestination(LatLng destination) {
        this.destination = destination;
        invalidateLatestRoute();
    }

    private LatLng getOrigin() {
        if (origin == null) {
            final Location location = GeoLocationCache.getIncetance(context).getBestLocation();
            return new LatLng(location.getLatitude(), location.getLongitude());
        } else
            return origin;
    }

    private LatLng getDestination() {
        if (destination == null) {
            final Location location = GeoLocationCache.getIncetance(context).getBestLocation();
            return new LatLng(location.getLatitude(), location.getLongitude());
        } else
            return destination;
    }

    private void requestRouteInfo(final ArrayList<T> locations, final Callback<T> callback) {
        Map<String, String> params = new HashMap<>();
        LatLng o = getOrigin();
        params.put("origin", o.latitude + "," + o.longitude);

        LatLng d = getDestination();
        params.put("destination", d.latitude + "," + d.longitude);
        StringBuilder waypoints = new StringBuilder("optimize:true");
        for (T loc : locations) {
            LatLng latLng = toLatLng(loc);
            waypoints.append("|").append(latLng.latitude).append(",").append(latLng.longitude);
        }
        params.put("waypoints", waypoints.toString());
        new RequestClient() {
            @Override
            protected void onPostExecute(JSONObject jsonObject) {
                try {
                    latestRouteInfo = new RouteInfo<>();

                    JSONArray order = jsonObject.getJSONArray("routes").getJSONObject(0).getJSONArray("waypoint_order");
                    if (locations.size() == order.length()) {
                        for (int i = 0; i < locations.size(); i++) {
                            latestRouteInfo.waypointOrder.put(locations.get(order.getInt(i)), i);
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
                } catch (JSONException e) {
                    Log.e(TAG, "Could not parse JSON: " + jsonObject.toString() + " (Error: " + e.getMessage() + ")");
                    latestRouteInfo = null;
                }
                callback.onRouteInfoResult(latestRouteInfo);
            }
        }.execute("maps", "directions", params);
    }

    public void getRouteInfo(final ArrayList<T> locations, Callback<T> callback) {
        if (validData) {
            callback.onRouteInfoResult(latestRouteInfo);
        } else {
            requestRouteInfo(locations, callback);
        }
    }

    public void invalidateLatestRoute() {
        validData = false;
    }

    protected abstract LatLng toLatLng(T item);

    public interface Callback<T> {
        void onRouteInfoResult(@Nullable RouteInfo<T> routeInfo);
    }
}

