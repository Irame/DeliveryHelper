package de.vanselow.deliveryhelper;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class RouteModel implements Parcelable {
    private static final String TAG = RouteModel.class.getName();

    public long id;
    public String name;
    public long date;
    public ArrayList<LocationModel> locations;

    public RouteModel(long id, String name, long date) {
        this.id = id;
        this.name = name;
        this.date = date;
        this.locations = new ArrayList<>();
    }

    public RouteModel() {
        this(-1, null, 0);
    }

    public boolean hasValidId() {
        return id >= 0;
    }

    public boolean update(RouteModel route) {
        if (id != route.id) return false;
        name = route.name;
        date = route.date;
        locations = route.locations;
        return true;
    }

    public float getTotalPrice() {
        float sum = 0;
        for (LocationModel location : locations) {
            sum += location.price;
        }
        return sum;
    }

    public int getOpenLocations() {
        int count = 0;
        for (LocationModel location : locations) {
            if (location.state == LocationModel.State.OPEN)
                count++;
        }
        return count;
    }

    public int getDeliveredLocations() {
        int count = 0;
        for (LocationModel location : locations) {
            if (location.state == LocationModel.State.DELIVERED)
                count++;
        }
        return count;
    }

    //JSON stuff
    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        try {
            result.put("id", id);
            result.put("name", name);
            result.put("date", date);
            JSONArray locationArray = new JSONArray();
            for (LocationModel location : locations) {
                locationArray.put(location.toJson());
            }
            result.put("locations", locationArray);
        } catch (JSONException e) {
            Log.e(TAG, "Error while creating JSONObject from RouteModel with: id=" + id);
            throw e;
        }
        return result;
    }

    public static RouteModel fromJson(JSONObject json) throws JSONException {
        RouteModel result = new RouteModel();
        try {
            if (json.has("id")) result.id = json.getLong("id");
            result.name = json.getString("name");
            result.date = json.getLong("date");
            result.locations = new ArrayList<>();
            if (json.has("locations")) {
                JSONArray locationsArray = json.getJSONArray("locations");
                for (int i = 0; i < locationsArray.length(); i++) {
                    result.locations.add(LocationModel.fromJson(locationsArray.getJSONObject(i)));
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error while creating RouteModel from JSONObject with: id=" + result.id);
            throw e;
        }
        return result;
    }

    // Parcelable stuff
    protected RouteModel(Parcel in) {
        id = in.readLong();
        name = in.readString();
        date = in.readLong();
        locations = in.createTypedArrayList(LocationModel.CREATOR);
    }

    public static final Creator<RouteModel> CREATOR = new Creator<RouteModel>() {
        @Override
        public RouteModel createFromParcel(Parcel in) {
            return new RouteModel(in);
        }

        @Override
        public RouteModel[] newArray(int size) {
            return new RouteModel[size];
        }
    };

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeLong(date);
        dest.writeTypedList(locations);
    }
}
