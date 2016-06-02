package de.vanselow.deliveryhelper;

import android.content.Context;
import android.os.Handler;
import android.util.Log;

import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.places.AutocompletePredictionBuffer;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.Places;
import com.koushikdutta.async.http.server.AsyncHttpServer;
import com.koushikdutta.async.http.server.AsyncHttpServerRequest;
import com.koushikdutta.async.http.server.AsyncHttpServerResponse;
import com.koushikdutta.async.http.server.HttpServerRequestCallback;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

import de.vanselow.deliveryhelper.utils.DatabaseHelper;

public class RemoteAccess {
    private static final String TAG = RemoteAccess.class.getName();

    private final JSONObject errorGetAllRoutesRespose;
    private static final String ERROR_GET_ALL_ROUTES = "ERROR_GET_ALL_ROUTES";
    private static final String ERROR_GET_ALL_ROUTES_DESC = "Error while building result for all routes.";

    private final JSONObject errorAddUpdateRoutesRespose;
    private static final String ERROR_ADD_UPDATE_ROUTES = "ERROR_ADD_UPDATE_ROUTES";
    private static final String ERROR_ADD_UPDATE_ROUTES_DESC = "Error while adding/updating Routes.";

    private static RemoteAccess instance;
    private final Context context;
    private final GoogleApiClient googleApiClient;

    private AsyncHttpServer server;

    private RemoteAccess(Context context) {
        this.context = context;
        server = new AsyncHttpServer();
        server.get("/get/routes", new GetRoutesCallback());
        server.post("/addupdate/routes", new AddUpdateRoutesCallback());
        server.post("/csv/add/locations", new CsvAddLocationCallback());

        errorGetAllRoutesRespose = new JSONObject();
        errorAddUpdateRoutesRespose = new JSONObject();
        try {
            errorGetAllRoutesRespose.put("status", ERROR_GET_ALL_ROUTES);
            errorGetAllRoutesRespose.put("status_desc", ERROR_GET_ALL_ROUTES_DESC);
            errorAddUpdateRoutesRespose.put("status", ERROR_ADD_UPDATE_ROUTES);
            errorAddUpdateRoutesRespose.put("status_desc", ERROR_ADD_UPDATE_ROUTES_DESC);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        googleApiClient = new GoogleApiClient
                .Builder( context )
                .addApi( Places.GEO_DATA_API )
                .build();
    }

    private class GetRoutesCallback implements HttpServerRequestCallback {
        @Override
        public void onRequest(AsyncHttpServerRequest request, final AsyncHttpServerResponse response) {
            Log.i(TAG, "Requested all Routes.");
            ArrayList<RouteModel> routes = DatabaseHelper.getInstance(context).getAllRoutes();
            try {
                JSONObject result = new JSONObject();
                JSONArray routesArray = new JSONArray();
                for (RouteModel route : routes) {
                    routesArray.put(route.toJson());
                }
                result.put("routes", routesArray);
                response.send(result);
            } catch (JSONException e) {
                Log.e(TAG, ERROR_GET_ALL_ROUTES_DESC);
                response.code(500); // internal server error
                response.send(errorGetAllRoutesRespose);
            }
        }
    }

    private class AddUpdateRoutesCallback implements HttpServerRequestCallback {
        @Override
        public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
            JSONObject result = new JSONObject();
            try {
                JSONObject requestJson = (JSONObject) request.getBody().get();
                JSONArray routes = requestJson.getJSONArray("routes");
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(context);
                ArrayList<RouteModel> routeArrayList = new ArrayList<>();
                for (int i = 0; i < routes.length(); i++) {
                    RouteModel route = RouteModel.fromJson(routes.getJSONObject(i));
                    dbHelper.addOrUpdateRoute(route);
                    for (LocationModel location : route.locations) {
                        dbHelper.addOrUpdateRouteLocation(location, route.id);
                    }
                    routeArrayList.add(route);
                }
                onRoutesDataReceived(routeArrayList);
            } catch (Exception e) {
                Log.e(TAG, ERROR_ADD_UPDATE_ROUTES_DESC);
                response.code(400);
                result = errorAddUpdateRoutesRespose;
            }
            response.send(result);
        }
    }

    private class CsvAddLocationCallback implements HttpServerRequestCallback {
        @Override
        public void onRequest(AsyncHttpServerRequest request, AsyncHttpServerResponse response) {
            ArrayList<LocationModel> locationArrayList = new ArrayList<>();
            String requestString = (String) request.getBody().get();
            String[] rows = requestString.split("\\r?\\n");
            for (String row : rows) {
                final String[] cells = row.split(";");
                if (cells.length > 3 || cells.length < 2) continue;
                String address = cells[1];
                AutocompletePredictionBuffer buffer = Places.GeoDataApi.getAutocompletePredictions(googleApiClient, address, null, null).await();
                if (buffer.getCount() < 1) continue;
                final Place place = Places.GeoDataApi.getPlaceById(googleApiClient, buffer.get(0).getPlaceId()).await().get(0);
                LocationModel locationModel = new LocationModel() {{
                    name = cells[0];
                    setPlace(place);
                    try {
                        if (cells.length == 3) price = Float.parseFloat(cells[2]);
                    } catch (NumberFormatException e) {
                        Log.w(TAG, "Could not parse price for a location. (" + name + "; "  + address + ")");
                    }
                }};
                locationArrayList.add(locationModel);
            }
            onLocationsDataReceived(locationArrayList);
            response.send("");
        }
    }


    public static void start(Context context, int port) {
        if (instance == null) instance = new RemoteAccess(context);
        instance.server.stop();
        instance.server.listen(port);
        instance.googleApiClient.connect();
    }

    public static void stop() {
        if (instance == null) return;
        instance.server.stop();
        instance.googleApiClient.disconnect();
    }

    private static abstract class HandlerHolder {
        protected final Handler handler;

        protected HandlerHolder(Context context) {
            this.handler = new Handler(context.getApplicationContext().getMainLooper());
        }
    }

    public static abstract class RoutesReceivedListener extends HandlerHolder{
        protected RoutesReceivedListener(Context context) {
            super(context);
        }

        abstract void onRoutesReceived(ArrayList<RouteModel> routes);
    }

    private RoutesReceivedListener routesReceivedListener;
    public static void setRoutesDataReceivedListener(RoutesReceivedListener listener) {
        if (instance == null) return;
        instance.routesReceivedListener = listener;
    }
    private void onRoutesDataReceived(final ArrayList<RouteModel> routes) {
        if (routesReceivedListener == null) return;
        routesReceivedListener.handler.post(new Runnable() {
            @Override
            public void run() {
                routesReceivedListener.onRoutesReceived(routes);
            }
        });
    }

    public static abstract class LocationsReceivedListener extends HandlerHolder {
        protected LocationsReceivedListener(Context context) {
            super(context);
        }

        abstract void onLocationsReceived(ArrayList<LocationModel> locations);
    }

    private LocationsReceivedListener locationsReceivedListener;
    public static void setLocationsDataReceivedListener(LocationsReceivedListener listener) {
        if (instance == null) return;
        instance.locationsReceivedListener = listener;
    }
    private void onLocationsDataReceived(final ArrayList<LocationModel> locations) {
        if (locationsReceivedListener == null) return;
        locationsReceivedListener.handler.post(new Runnable() {
            @Override
            public void run() {
                locationsReceivedListener.onLocationsReceived(locations);
            }
        });
    }
}
