package de.vanselow.deliveryhelper;

import android.content.Context;
import android.util.Log;

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

    private AsyncHttpServer server;

    private RemoteAccess(Context context) {
        this.context = context;
        server = new AsyncHttpServer();
        server.get("/get/routes", new GetRoutesCallback());
        server.post("/addupdate/routes", new AddUpdateRoutesCallback());

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
                for (int i = 0; i < routes.length(); i++) {
                    RouteModel route = RouteModel.fromJson(routes.getJSONObject(i));
                    dbHelper.addOrUpdateRoute(route);
                    for (LocationModel location : route.locations) {
                        dbHelper.addOrUpdateRouteLocation(location, route.id);
                    }
                }
                onRoutesDataReceived(requestJson);
            } catch (Exception e) {
                Log.e(TAG, ERROR_ADD_UPDATE_ROUTES_DESC);
                response.code(400);
                result = errorAddUpdateRoutesRespose;
            }
            response.send(result);
        }
    }



    public static void start(Context context, int port) {
        if (instance == null) instance = new RemoteAccess(context);
        instance.server.stop();
        instance.server.listen(port);
    }

    public static void stop() {
        if (instance == null) return;
        instance.server.stop();
    }

    public interface DataReceivedListener {
        void onDataReceived(JSONObject jsonObject);
    }

    private DataReceivedListener routesDataReceivedListener;
    public static void setRoutesDataReceivedListener(DataReceivedListener listener) {
        if (instance == null) return;
        instance.routesDataReceivedListener = listener;
    }
    private void onRoutesDataReceived(JSONObject jsonObject) {
        if (routesDataReceivedListener == null) return;
        routesDataReceivedListener.onDataReceived(jsonObject);
    }
}
