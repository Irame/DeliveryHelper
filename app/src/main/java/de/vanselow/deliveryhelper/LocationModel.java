package de.vanselow.deliveryhelper;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.places.Place;

/**
 * Created by Felix on 12.05.2016.
 */
public class LocationModel implements Parcelable {
    public enum State {
        OPEN("Open"), DELIVERED("Delivered");

        public final String sectionText;

        State(String sectionText) {
            this.sectionText = sectionText;
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

    LocationModel(String name, Place place) {
        id = -1;
        this.name = name;
        address = place.getAddress().toString();
        placeid = place.getId();
        latitude = place.getLatLng().latitude;
        longitude = place.getLatLng().longitude;
        price = 0;
        notes = "";
        state = State.OPEN;
    }

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
}
