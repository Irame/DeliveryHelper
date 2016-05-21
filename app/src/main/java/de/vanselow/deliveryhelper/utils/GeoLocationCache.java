package de.vanselow.deliveryhelper.utils;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

import java.util.LinkedList;
import java.util.List;

public class GeoLocationCache {
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1; // 1 minute

    private static GeoLocationCache incetance;

    private List<Listener> geoLocationListenerList;

    private Location gpsLocation;
    private Location netLocation;

    private GeoLocationCache(Context context) {
        geoLocationListenerList = new LinkedList<>();
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        boolean isPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (isPermissionGranted) {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, new NetworkLocationListener());
                netLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, new GPSLocationListener());
                gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
    }

    public static GeoLocationCache getIncetance(Context context) {
        if (incetance == null)
            incetance = new GeoLocationCache(context);
        return incetance;
    }

    public Location getBestLocation() {
        if (gpsLocation == null && netLocation == null) return null;
        if (gpsLocation == null) return new Location(netLocation);
        if (netLocation == null) return new Location(gpsLocation);

        if (gpsLocation.getTime() < netLocation.getTime()) {
            return new Location(netLocation);
        } else {
            return new Location(gpsLocation);
        }
    }

    public void addGeoLocationListener(Listener listener) {
        geoLocationListenerList.add(listener);
    }

    public void removeGeoLocationListener(Listener listener) {
        geoLocationListenerList.remove(listener);
    }

    private void OnGeoLocationChanged(Location location) {
        for (Listener geoLocationListener : geoLocationListenerList) {
            geoLocationListener.onGeoLocationChanged(location);
        }
    }

    private class NetworkLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            netLocation = location;
            OnGeoLocationChanged(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    private class GPSLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            gpsLocation = location;
            OnGeoLocationChanged(location);
        }

        @Override
        public void onStatusChanged(String provider, int status, Bundle extras) {

        }

        @Override
        public void onProviderEnabled(String provider) {

        }

        @Override
        public void onProviderDisabled(String provider) {

        }
    }

    public interface Listener {
        void onGeoLocationChanged(Location location);
    }
}
