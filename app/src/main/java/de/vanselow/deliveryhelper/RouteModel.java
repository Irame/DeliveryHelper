package de.vanselow.deliveryhelper;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;

/**
 * Created by Felix on 15.05.2016.
 */
public class RouteModel implements Parcelable {
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

    public RouteModel(String name, long date) {
        this.id = -1;
        this.name = name;
        this.date = date;
        this.locations = new ArrayList<>();
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
