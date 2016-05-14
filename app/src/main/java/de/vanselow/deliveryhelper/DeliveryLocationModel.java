package de.vanselow.deliveryhelper;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.android.gms.location.places.Place;

/**
 * Created by Felix on 12.05.2016.
 */
public class DeliveryLocationModel implements Parcelable {
    public enum Status {
        OPEN("Open"), DELIVERED("Delivered");

        public final String sectionText;

        Status(String sectionText) {
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
    public Status status;

    DeliveryLocationModel(String name, Place place) {
        id = -1;
        this.name = name;
        address = place.getAddress().toString();
        placeid = place.getId();
        latitude = place.getLatLng().latitude;
        longitude = place.getLatLng().longitude;
        price = 0;
        status = Status.OPEN;
    }

    public DeliveryLocationModel(long id, String name, String address, String placeid, double latitude, double longitude, float price, Status status) {
        this.id = id;
        this.name = name;
        this.address = address;
        this.placeid = placeid;
        this.latitude = latitude;
        this.longitude = longitude;
        this.price = price;
        this.status = status;
    }

    protected DeliveryLocationModel(Parcel in) {
        id = in.readLong();
        name = in.readString();
        address = in.readString();
        placeid = in.readString();
        latitude = in.readDouble();
        longitude = in.readDouble();
        price = in.readFloat();
        status = Status.valueOf(in.readString());
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
        dest.writeString(status.name());
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<DeliveryLocationModel> CREATOR = new Creator<DeliveryLocationModel>() {
        @Override
        public DeliveryLocationModel createFromParcel(Parcel in) {
            return new DeliveryLocationModel(in);
        }

        @Override
        public DeliveryLocationModel[] newArray(int size) {
            return new DeliveryLocationModel[size];
        }
    };
}
