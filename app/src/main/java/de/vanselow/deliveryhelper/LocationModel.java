package de.vanselow.deliveryhelper;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.gms.location.places.Place;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.NumberFormat;
import java.text.ParseException;

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
    public Place place;
    public float price;
    public String notes;
    public State state;

    public LocationModel(long id, String name, String address, String placeId, double latitude, double longitude, float price, String notes, State state) {
        this.id = id;
        this.name = name;
        this.place = new Place(placeId, address, latitude, longitude);
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
        this.place.address = otherLocation.place.address;
        this.place.placeId = otherLocation.place.placeId;
        this.place.latitude = otherLocation.place.latitude;
        this.place.longitude = otherLocation.place.longitude;
        this.price = otherLocation.price;
        this.notes = otherLocation.notes;
        this.state = otherLocation.state;
        return true;
    }

    public boolean setPrice(String priceString) {
        price = 0;
        try {
            price = NumberFormat.getCurrencyInstance().parse(priceString).floatValue();
            return true;
        } catch (ParseException e) {}
        try {
            price = NumberFormat.getInstance().parse(priceString).floatValue();
            return true;
        } catch (ParseException e) {}
        return false;
    }

    // Json stuff
    public JSONObject toJson() throws JSONException {
        JSONObject result = new JSONObject();
        try {
            result.put("id", id);
            result.put("name", name);
            result.put("address", place.address);
            result.put("placeId", place.placeId);
            result.put("latitude", place.latitude);
            result.put("longitude", place.longitude);
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
            result.place = new Place(
                    json.getString("placeId"),
                    json.getString("address"),
                    json.getDouble("latitude"),
                    json.getDouble("longitude"));
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
        place = in.readParcelable(Place.class.getClassLoader());
        price = in.readFloat();
        notes = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeLong(id);
        dest.writeString(name);
        dest.writeParcelable(place, flags);
        dest.writeFloat(price);
        dest.writeString(notes);
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

    public static class Place implements Parcelable {
        public String placeId;
        public String address;
        public double latitude;
        public double longitude;

        public Place(String placeId, String address, double latitude, double longitude) {
            this.placeId = placeId;
            this.address = address;
            this.latitude = latitude;
            this.longitude = longitude;
        }

        public Place(com.google.android.gms.location.places.Place place) {
            this.address =  place.getAddress().toString();
            this.placeId = place.getId();
            this.latitude = place.getLatLng().latitude;
            this.longitude = place.getLatLng().longitude;
        }

        protected Place(Parcel in) {
            placeId = in.readString();
            address = in.readString();
            latitude = in.readDouble();
            longitude = in.readDouble();
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeString(placeId);
            dest.writeString(address);
            dest.writeDouble(latitude);
            dest.writeDouble(longitude);
        }

        @Override
        public int describeContents() {
            return 0;
        }

        public static final Creator<Place> CREATOR = new Creator<Place>() {
            @Override
            public Place createFromParcel(Parcel in) {
                return new Place(in);
            }

            @Override
            public Place[] newArray(int size) {
                return new Place[size];
            }
        };
    }
}
