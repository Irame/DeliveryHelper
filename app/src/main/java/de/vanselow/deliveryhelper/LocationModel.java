package de.vanselow.deliveryhelper;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.places.Place;

public class LocationModel implements Parcelable {
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
