package de.vanselow.deliveryhelper;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.location.places.Place;

import org.json.JSONException;
import org.json.JSONObject;

public class LocationModel implements Parcelable {
    private static final String TAG = LocationModel.class.getName();

    public enum State {
        OPEN(R.string.open, R.string.no_open_locations),
        DELIVERED(R.string.delivered, R.string.no_delivered_locations);

        public final int sectionStringId;
        public final int emptyListStringId;

        State(int sectionStringId, int emptyListStringId) {
            this.sectionStringId = sectionStringId;
            this.emptyListStringId = emptyListStringId;
        }
    }

    public long id;
    public String name;
    public String address;
    public String placeid;
    public double latitude;
    public double longitude;
    public float price;
    public String notes;
    public State state;

    public LocationModel(long id, String name, String address, String placeid, double latitude, double longitude, float price, String notes, State state) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.placeid = placeid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.price = price;
        this.notes = notes;
        this.state = state;
    }

    LocationModel() {
        this(-1, null, null, null, 0, 0, 0, "", State.OPEN);
    }

    public boolean hasValidId() {
        return id >= 0;
    }

    public boolean update(LocationModel otherLocation) {
        if (otherLocation.id != this.id) return false;
        this.id = otherLocation.id;
        this.name = otherLocation.name;
        this.address = otherLocation.address;
        this.placeid = otherLocation.placeid;
        this.latitude = otherLocation.latitude;
        this.longitude = otherLocation.longitude;
        this.price = otherLocation.price;
        this.notes = otherLocation.notes;
        this.state = otherLocation.state;
        return true;
    }


    public void setPlace(Place place) {
        this.address =  place.getAddress().toString();
        this.placeid = place.getId();
        this.latitude = place.getLatLng().latitude;
        this.longitude = place.getLatLng().longitude;
    }

    // Json stuff
    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        try {
            result.put("id", id);
            result.put("name", name);
            result.put("address", address);
            result.put("placeid", placeid);
            result.put("latitude", latitude);
            result.put("longitude", longitude);
            result.put("price", price);
            result.put("notes", notes);
            result.put("state", state.name());
        } catch (JSONException e) {
            Log.e(TAG, "Error while creating JSONObject from LocationsModel with: id=" + id);
            throw e;
        }
        return result;
    }

    public static LocationModel fromJson(JSONObject json) throws JSONException {
        LocationModel result = new LocationModel();
        try {
            if (json.has("id")) result.id = json.getLong("id");
            result.name = json.getString("name");
            result.address = json.getString("address");
            result.placeid = json.getString("placeid");
            result.latitude = json.getDouble("latitude");
            result.longitude = json.getDouble("longitude");
            if (json.has("price")) result.price = (float) json.getDouble("price");
            if (json.has("notes")) result.notes = json.getString("notes");
            if (json.has("state")) result.state = State.valueOf(json.getString("state"));
        } catch (JSONException e) {
            Log.e(TAG, "Error while creating LocationsModel from JSONObject with: id=" + result.id);
            throw e;
        }
        return result;
    }

    // Parcelable stuff
    protected LocationModel(Parcel in) {
        id = in.readLong();
        name = in.readString();
        address = in.readString();
        placeid = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        price = in.readFloat();
        notes = in.readString();
        state = State.valueOf(in.readString());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeString(address);
        dest.writeString(placeid);
        dest.writeDouble(latitude);
        dest.writeDouble(longitude);
        dest.writeFloat(price);
        dest.writeString(notes);
        dest.writeString(state.name());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<LocationModel> CREATOR = new Creator<LocationModel>() {
        @Override
        public LocationModel createFromParcel(Parcel in) {
            return new LocationModel(in);
        }

        @Override
        public LocationModel[] newArray(int size) {
            return new LocationModel[size];
        }
    };

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        LocationModel that = (LocationModel) o;

        if (id != that.id) return false;
        if (Double.compare(that.latitude, latitude) != 0) return false;
        if (Double.compare(that.longitude, longitude) != 0) return false;
        if (Float.compare(that.price, price) != 0) return false;
        if (!name.equals(that.name)) return false;
        if (!address.equals(that.address)) return false;
        if (!placeid.equals(that.placeid)) return false;
        if (!notes.equals(that.notes)) return false;
        return state == that.state;

    }

    @Override
    public int hashCode() {
        return (int) (id ^ (id >>> 32));
    }
}
