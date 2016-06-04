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
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 60000; // 1 minute

    private static GeoLocationCache instance;

    private Context context;
    private List<Listener> geoLocationListenerList;

    private Location gpsLocation;
    private Location netLocation;

    private LocationManager locationManager;
    private LocationListener gpsLocationListener;
    private LocationListener networkLocationListener;

    private GeoLocationCache(Context context) {
        this.context = context.getApplicationContext();
        geoLocationListenerList = new LinkedList<>();

        gpsLocationListener = new GPSLocationListener();
        networkLocationListener = new NetworkLocationListener();
    }

    public static GeoLocationCache getInstance(Context context) {
        if (instance == null)
            instance = new GeoLocationCache(context);
        return instance;
    }

    public void start() {
        stop();
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        if (checkPermission()) {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        networkLocationListener);
                netLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        gpsLocationListener);
                gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
            Location bestLocation = getBestLocation();
            if (bestLocation != null) onGeoLocationChanged(bestLocation);
        }
    }

    public void stop() {
        if (locationManager != null && checkPermission()) {
            locationManager.removeUpdates(networkLocationListener);
            locationManager.removeUpdates(gpsLocationListener);
            locationManager = null;
        }
    }

    public Location getBestLocation() {
        if (locationManager == null)
            throw new IllegalStateException("GeoLocationCache has not been started yet.");
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

    private boolean checkPermission() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }

    private void onGeoLocationChanged(Location location) {
        for (Listener geoLocationListener : geoLocationListenerList) {
            geoLocationListener.onGeoLocationChanged(location);
        }
    }

    private class NetworkLocationListener implements LocationListener {
        @Override
        public void onLocationChanged(Location location) {
            netLocation = location;
            onGeoLocationChanged(location);
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
            onGeoLocationChanged(location);
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
