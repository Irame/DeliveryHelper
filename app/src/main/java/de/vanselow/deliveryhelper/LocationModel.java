package de.vanselow.deliveryhelper;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.places.Place;

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

    public void setPlace(Place place) {
        this.address =  place.getAddress().toString();
        this.placeid = place.getId();
        this.latitude = place.getLatLng().latitude;
        this.longitude = place.getLatLng().longitude;
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
