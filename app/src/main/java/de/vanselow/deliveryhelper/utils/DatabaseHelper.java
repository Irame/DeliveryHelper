package de.vanselow.deliveryhelper.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import java.util.ArrayList;

import de.vanselow.deliveryhelper.LocationModel;
import de.vanselow.deliveryhelper.RouteModel;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = DatabaseHelper.class.getName();
    private static DatabaseHelper instance;

    private static final int DATABASE_VERSION = 4;
    private static final String DATABASE_NAME = "vanselow_delivery_helper";

    private static final String ROUTES_TABLE_NAME = "routes";
    private static final String ROUTES_ID = "id";
    private static final String ROUTES_NAME = "name";
    private static final String ROUTES_DATE = "date";

    private static final String LOCATIONS_TABLE_NAME = "locations";
    private static final String LOCATIONS_ID = "id";
    private static final String LOCATIONS_NAME = "name";
    private static final String LOCATIONS_ADDRESS = "address";
    private static final String LOCATIONS_PLACEID = "placeid";
    private static final String LOCATIONS_LATITUDE = "latitude";
    private static final String LOCATIONS_LONGITUDE = "longitude";
    private static final String LOCATIONS_PRICE = "price";
    private static final String LOCATIONS_NOTES = "notes";
    private static final String LOCATIONS_STATE = "state";
    private static final String LOCATIONS_ROUTE_ID = "route_id";

    private DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ROUTES_TABLE_NAME + " ( " +
                ROUTES_ID + " INTEGER PRIMARY KEY, " +
                ROUTES_NAME + " TEXT NOT NULL, " +
                ROUTES_DATE + " INTEGER NOT NULL )");

        db.execSQL("CREATE TABLE " + LOCATIONS_TABLE_NAME + " ( " +
                LOCATIONS_ID + " INTEGER PRIMARY KEY, " +
                LOCATIONS_NAME + " TEXT NOT NULL, " +
                LOCATIONS_ADDRESS + " TEXT NOT NULL, " +
                LOCATIONS_PLACEID + " TEXT NOT NULL, " +
                LOCATIONS_LATITUDE + " REAL NOT NULL, " +
                LOCATIONS_LONGITUDE + " REAL NOT NULL, " +
                LOCATIONS_PRICE + " REAL NOT NULL, " +
                LOCATIONS_NOTES + " TEXT NOT NULL, " +
                LOCATIONS_STATE + " TEXT NOT NULL, " +
                LOCATIONS_ROUTE_ID + " INTEGER NOT NULL, " +
                "FOREIGN KEY(" + LOCATIONS_ROUTE_ID + ") REFERENCES " + ROUTES_TABLE_NAME + "(" + ROUTES_ID + ") ON DELETE CASCADE )");
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + ROUTES_TABLE_NAME);
            db.execSQL("DROP TABLE IF EXISTS " + LOCATIONS_TABLE_NAME);
            onCreate(db);
        }
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public long addOrUpdateRoute(RouteModel route) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(ROUTES_NAME, route.name);
            values.put(ROUTES_DATE, route.date);

            if (route.id >= 0) {
                int rows = db.update(ROUTES_TABLE_NAME, values, ROUTES_ID + " = ?", new String[]{Long.toString(route.id)});
                if (rows == 0) route.id = -1;
            }
            if (route.id < 0) {
                route.id = db.insertOrThrow(ROUTES_TABLE_NAME, null, values);
            }

            for (LocationModel location : route.locations) {
                addOrUpdateRouteLocation(location, route.id);
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to add or update a route to the database.");
        } finally {
            db.endTransaction();
        }

        return route.id;
    }

    public RouteModel getRouteById(long routeId, boolean withLocations) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(ROUTES_TABLE_NAME,
                null,   // all columns (*)
                ROUTES_ID + " = ?",
                new String[]{String.valueOf(routeId)},
                null,   // no grouping
                null,   // no having (group selection)
                null,   // no ordering
                null);  // no limit

        RouteModel routeModel = null;
        try {
            if (cursor.moveToFirst()) {
                routeModel = new RouteModel(
                        cursor.getLong(cursor.getColumnIndex(ROUTES_ID)),
                        cursor.getString(cursor.getColumnIndex(ROUTES_NAME)),
                        cursor.getLong(cursor.getColumnIndex(ROUTES_DATE))
                );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get route from the database. (id = " + routeId + ")");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        if (withLocations && routeModel != null) routeModel.locations = getAllRouteLocations(routeId);
        return routeModel;
    }

    public ArrayList<RouteModel> getAllRoutes() {
        ArrayList<RouteModel> result = new ArrayList<>();
        getAllRoutes(result);
        return result;
    }

    private void getAllRoutes(ArrayList<RouteModel> routeList) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(ROUTES_TABLE_NAME,
                null,   // all columns (*)
                null,   // all rows (no WHERE)
                null,   // no args for the WHERE
                null,   // no grouping
                null,   // no having (group selection)
                null,   // no ordering
                null);  // no limit

        try {
            if (cursor.moveToFirst()) {
                do {
                    routeList.add(new RouteModel(
                            cursor.getLong(cursor.getColumnIndex(ROUTES_ID)),
                            cursor.getString(cursor.getColumnIndex(ROUTES_NAME)),
                            cursor.getLong(cursor.getColumnIndex(ROUTES_DATE))
                    ));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get all routes from the database.");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }

        for (RouteModel route : routeList) {
            route.locations = getAllRouteLocations(route.id);
        }
    }

    public void deleteRoute(RouteModel route) {
        deleteRouteById(route.id);
    }

    public void deleteRouteById(long routeId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(ROUTES_TABLE_NAME, ROUTES_ID + " = ?", new String[]{Long.toString(routeId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to delete a route from the database.");
        } finally {
            db.endTransaction();
        }

    }

    public long updateRouteLocation(LocationModel loc) {
        return addOrUpdateRouteLocation(loc, -1);
    }

    public long addOrUpdateRouteLocation(LocationModel dl, long routeId) {
        if (dl.id < 0 && routeId < 0) return -1;

        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(LOCATIONS_NAME, dl.name);
            values.put(LOCATIONS_ADDRESS, dl.place.address);
            values.put(LOCATIONS_PLACEID, dl.place.placeId);
            values.put(LOCATIONS_LATITUDE, dl.place.latitude);
            values.put(LOCATIONS_LONGITUDE, dl.place.longitude);
            values.put(LOCATIONS_PRICE, dl.price);
            values.put(LOCATIONS_NOTES, dl.notes);
            values.put(LOCATIONS_STATE, dl.state.name());
            if (routeId >= 0)
                values.put(LOCATIONS_ROUTE_ID, routeId);

            if (dl.id >= 0) {
                int rows = db.update(LOCATIONS_TABLE_NAME, values, LOCATIONS_ID + " = ?", new String[]{Long.toString(dl.id)});
                if (rows == 0) dl.id = -1;
            }
            if (dl.id < 0) {
                dl.id = db.insertOrThrow(LOCATIONS_TABLE_NAME, null, values);
            }

            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to add or update a location to the database.");
        } finally {
            db.endTransaction();
        }

        return dl.id;
    }

    public LocationModel getRouteLocationById(long locationId) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(LOCATIONS_TABLE_NAME,
                new String[]{LOCATIONS_ID, LOCATIONS_NAME, LOCATIONS_ADDRESS, LOCATIONS_PLACEID,
                        LOCATIONS_LATITUDE, LOCATIONS_LONGITUDE, LOCATIONS_PRICE,
                        LOCATIONS_NOTES, LOCATIONS_STATE},   // all columns (*)
                LOCATIONS_ID + " = ?",   // all rows (no WHERE)
                new String[]{Long.toString(locationId)},   // no args for the WHERE
                null,   // no grouping
                null,   // no having (group selection)
                null,   // no ordering
                null);  // no limit

        try {
            if (cursor.moveToFirst()) {
                    return new LocationModel(
                            cursor.getLong(cursor.getColumnIndex(LOCATIONS_ID)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_NAME)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_ADDRESS)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_PLACEID)),
                            cursor.getDouble(cursor.getColumnIndex(LOCATIONS_LATITUDE)),
                            cursor.getDouble(cursor.getColumnIndex(LOCATIONS_LONGITUDE)),
                            cursor.getFloat(cursor.getColumnIndex(LOCATIONS_PRICE)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_NOTES)),
                            LocationModel.State.valueOf(cursor.getString(cursor.getColumnIndex(LOCATIONS_STATE)))
                    );
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get locations from the database.");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
        return null;
    }

    public ArrayList<LocationModel> getAllRouteLocations(long routeId) {
        ArrayList<LocationModel> result = new ArrayList<>();
        getAllRouteLocations(routeId, result);
        return result;
    }

    public void getAllRouteLocations(long routeId, ArrayList<LocationModel> locationList) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(LOCATIONS_TABLE_NAME,
                new String[]{LOCATIONS_ID, LOCATIONS_NAME, LOCATIONS_ADDRESS, LOCATIONS_PLACEID,
                        LOCATIONS_LATITUDE, LOCATIONS_LONGITUDE, LOCATIONS_PRICE,
                        LOCATIONS_NOTES, LOCATIONS_STATE},   // all columns (*)
                LOCATIONS_ROUTE_ID + " = ?",   // all rows (no WHERE)
                new String[]{Long.toString(routeId)},   // no args for the WHERE
                null,   // no grouping
                null,   // no having (group selection)
                null,   // no ordering
                null);  // no limit

        try {
            if (cursor.moveToFirst()) {
                do {
                    locationList.add(new LocationModel(
                            cursor.getLong(cursor.getColumnIndex(LOCATIONS_ID)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_NAME)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_ADDRESS)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_PLACEID)),
                            cursor.getDouble(cursor.getColumnIndex(LOCATIONS_LATITUDE)),
                            cursor.getDouble(cursor.getColumnIndex(LOCATIONS_LONGITUDE)),
                            cursor.getFloat(cursor.getColumnIndex(LOCATIONS_PRICE)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_NOTES)),
                            LocationModel.State.valueOf(cursor.getString(cursor.getColumnIndex(LOCATIONS_STATE)))
                    ));
                } while (cursor.moveToNext());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to get locations from the database.");
        } finally {
            if (cursor != null && !cursor.isClosed()) {
                cursor.close();
            }
        }
    }

    public void deleteRouteLocation(LocationModel dl) {
        deleteRouteLocationById(dl.id);
    }

    public void deleteRouteLocationById(long locationId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(LOCATIONS_TABLE_NAME, LOCATIONS_ID + " = ?", new String[]{Long.toString(locationId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to delete a location from the database.");
        } finally {
            db.endTransaction();
        }
    }

    public void deleteAllRouteLocations(long routeId) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(LOCATIONS_TABLE_NAME, LOCATIONS_ROUTE_ID + " = ?", new String[]{Long.toString(routeId)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to delete all locations from the database.");
        } finally {
            db.endTransaction();
        }
    }
}
