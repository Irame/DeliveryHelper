package de.vanselow.deliveryhelper.utils;

import android.content.Context;
import android.os.AsyncTask;

import java.util.ArrayList;

import de.vanselow.deliveryhelper.LocationModel;
import de.vanselow.deliveryhelper.RouteModel;

public class DatabaseAsync {
    private static DatabaseAsync instance;

    private DatabaseHelper db;

    private DatabaseAsync(Context context) {
        db = DatabaseHelper.getInstance(context);
    }

    public static DatabaseAsync getInstance(Context context) {
        if (instance == null) instance = new DatabaseAsync(context);
        return instance;
    }

    public void addOrUpdateRoute(final RouteModel route) {
        addOrUpdateRoute(route, null);
    }

    public void addOrUpdateRoute(final RouteModel route, Callback<Long> callback) {
        new Task<Long>(callback) {
            @Override
            protected Long doInBackground(Void... params) {
                return db.addOrUpdateRoute(route);
            }
        }.execute();
    }

    public void getRouteById(final long routeId, Callback<RouteModel> callback) {
        new Task<RouteModel>(callback) {
            @Override
            protected RouteModel doInBackground(Void... params) {
                return db.getRouteById(routeId);
            }
        }.execute();
    }

    public void getAllRoutes(Callback<ArrayList<RouteModel>> callback) {
        new Task<ArrayList<RouteModel>>(callback) {
            @Override
            protected ArrayList<RouteModel> doInBackground(Void... params) {
                return db.getAllRoutes();
            }
        }.execute();
    }

    public void deleteRoute(RouteModel route) {
        deleteRouteById(route.id);
    }

    public void deleteRouteById(final long id) {
        new Task<Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                db.deleteRouteById(id);
                return null;
            }
        }.execute();
    }

    public void updateRouteLocation(final LocationModel dl) {
        addOrUpdateRouteLocation(dl, -1, null);
    }

    public void updateRouteLocation(final LocationModel dl, Callback<Long> callback) {
        addOrUpdateRouteLocation(dl, -1, callback);
    }

    public void addOrUpdateRouteLocation(final LocationModel dl, final long routeId) {
        addOrUpdateRouteLocation(dl, routeId, null);
    }

    public void addOrUpdateRouteLocation(final LocationModel dl, final long routeId, Callback<Long> callback) {
        new Task<Long>(callback) {
            @Override
            protected Long doInBackground(Void... params) {
                return db.addOrUpdateRouteLocation(dl, routeId);
            }
        }.execute();
    }

    public void getRouteLocationById(final long locationId, Callback<LocationModel> callback) {
        new Task<LocationModel>(callback) {
            @Override
            protected LocationModel doInBackground(Void... params) {
                return db.getRouteLocationById(locationId);
            }
        }.execute();
    }

    public void getAllRouteLocations(final long routeId, Callback<ArrayList<LocationModel>> callback) {
        new Task<ArrayList<LocationModel>>(callback) {
            @Override
            protected ArrayList<LocationModel> doInBackground(Void... params) {
                return db.getAllRouteLocations(routeId);
            }
        }.execute();
    }

    public void deleteRouteLocation(LocationModel dl) {
        deleteRouteLocationById(dl.id);
    }

    public void deleteRouteLocationById(final long routeId) {
        new Task<Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                db.deleteRouteLocationById(routeId);
                return null;
            }
        }.execute();
    }

    public void deleteAllRouteLocations(final long routeId) {
        new Task<Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                db.deleteAllRouteLocations(routeId);
                return null;
            }
        }.execute();
    }

    public static abstract class Callback<Result> {
        public abstract void onPostExecute(Result result);

        public void onCancelled(Result result) {}
        public void onCancelled() {}
        public void onPreExecute() {}
        public void onProgressUpdate(Double... values) {}
    }

    private abstract class Task<Result> extends AsyncTask<Void, Double, Result> {
        private Callback<Result> callback;

        public Task(Callback<Result> callback) {
            this.callback = callback;
        }

        public Task() {
            this(null);
        }

        @Override
        protected abstract Result doInBackground(Void... params);

        @Override
        protected void onCancelled() {
            if (callback == null) return;
            callback.onCancelled();
        }

        @Override
        protected void onCancelled(Result result) {
            if (callback == null) return;
            callback.onCancelled(result);
        }

        @Override
        protected void onProgressUpdate(Double... values) {
            if (callback == null) return;
            callback.onProgressUpdate(values);
        }

        @Override
        protected void onPostExecute(Result result) {
            if (callback == null) return;
            callback.onPostExecute(result);
        }

        @Override
        protected void onPreExecute() {
            if (callback == null) return;
            callback.onPreExecute();
        }
    }
}
