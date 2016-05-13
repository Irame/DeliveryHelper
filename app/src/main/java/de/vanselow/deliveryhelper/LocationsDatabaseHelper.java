package de.vanselow.deliveryhelper;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.android.gms.location.places.Place;

import java.util.ArrayList;

/**
 * Created by Felix on 13.05.2016.
 */
public class LocationsDatabaseHelper extends SQLiteOpenHelper {
    private static final String TAG = LocationsDatabaseHelper.class.getName();
    private static LocationsDatabaseHelper instance;

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "vanselow_delivery_helper";

    private static final String LOCATIONS_TABLE_NAME = "locations";
    private static final String LOCATIONS_ID = "id";
    private static final String LOCATIONS_NAME = "name";
    private static final String LOCATIONS_ADDRESS = "address";
    private static final String LOCATIONS_PLACEID = "placeid";
    private static final String LOCATIONS_LATITUDE = "latitude";
    private static final String LOCATIONS_LONGITUDE = "longitude";
    private static final String LOCATIONS_PRICE = "price";
    private static final String LOCATIONS_STATUS = "status";

    private LocationsDatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + LOCATIONS_TABLE_NAME + " (" +
                LOCATIONS_ID + " INTEGER PRIMARY KEY, " +
                LOCATIONS_NAME + " TEXT NOT NULL, " +
                LOCATIONS_ADDRESS + " TEXT NOT NULL, " +
                LOCATIONS_PLACEID + " TEXT NOT NULL, " +
                LOCATIONS_LATITUDE + " REAL NOT NULL, " +
                LOCATIONS_LONGITUDE + " REAL NOT NULL, " +
                LOCATIONS_PRICE + " REAL NOT NULL, " +
                LOCATIONS_STATUS + " TEXT NOT NULL )");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion != newVersion) {
            db.execSQL("DROP TABLE IF EXISTS " + LOCATIONS_TABLE_NAME);
            onCreate(db);
        }
    }

    public static synchronized LocationsDatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new LocationsDatabaseHelper(context.getApplicationContext());
        }
        return instance;
    }

    public long addOrUpdateLocation(DeliveryLocationModel dl) {
        SQLiteDatabase db = getWritableDatabase();

        db.beginTransaction();
        try {
            ContentValues values = new ContentValues();
            values.put(LOCATIONS_NAME, dl.name);
            values.put(LOCATIONS_ADDRESS, dl.address);
            values.put(LOCATIONS_PLACEID, dl.placeid);
            values.put(LOCATIONS_LATITUDE, dl.latitude);
            values.put(LOCATIONS_LONGITUDE, dl.longitude);
            values.put(LOCATIONS_PRICE, dl.price);
            values.put(LOCATIONS_STATUS, dl.status.name());

            if (dl.id >= 0) {
                int rows = db.update(LOCATIONS_TABLE_NAME, values, LOCATIONS_ID + " = ?", new String[]{Long.toString(dl.id)});
                if (rows == 0) dl.id = -1;
            }
            if (dl.id < 0){
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

    public ArrayList<DeliveryLocationModel> getAllLocations() {
        ArrayList<DeliveryLocationModel> result = new ArrayList<>();
        getAllLocations(result);
        return result;
    }

    public void getAllLocations(ArrayList<DeliveryLocationModel> locationList) {
        SQLiteDatabase db = getReadableDatabase();

        Cursor cursor = db.query(LOCATIONS_TABLE_NAME,
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
                    locationList.add(new DeliveryLocationModel(
                            cursor.getLong(cursor.getColumnIndex(LOCATIONS_ID)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_NAME)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_ADDRESS)),
                            cursor.getString(cursor.getColumnIndex(LOCATIONS_PLACEID)),
                            cursor.getDouble(cursor.getColumnIndex(LOCATIONS_LATITUDE)),
                            cursor.getDouble(cursor.getColumnIndex(LOCATIONS_LONGITUDE)),
                            cursor.getFloat(cursor.getColumnIndex(LOCATIONS_PRICE)),
                            DeliveryLocationModel.Status.valueOf(cursor.getString(cursor.getColumnIndex(LOCATIONS_STATUS)))
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

    public void deleteLocation(DeliveryLocationModel dl) {
        deleteLocationById(dl.id);
    }

    public void deleteLocationById(long id) {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(LOCATIONS_TABLE_NAME, LOCATIONS_ID + " = ?", new String[]{Long.toString(id)});
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to delete a location from the database.");
        } finally {
            db.endTransaction();
        }
    }

    public void deleteAllLocations() {
        SQLiteDatabase db = getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete(LOCATIONS_TABLE_NAME, null, null);
            db.setTransactionSuccessful();
        } catch (Exception e) {
            Log.e(TAG, "Error while trying to delete all locations from the database.");
        } finally {
            db.endTransaction();
        }
    }
}
