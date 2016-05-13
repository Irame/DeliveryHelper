package de.vanselow.deliveryhelper;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;

/**
 * Created by Felix on 13.05.2016.
 */
public class LocationCache {
    // The minimum distance to change Updates in meters
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 1; // 10 meters

    // The minimum time between updates in milliseconds
    private static final long MIN_TIME_BW_UPDATES = 1; // 1 minute

    private Location gpsLocation;
    private Location netLocation;

    LocationCache(Context context) {
        LocationManager locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        boolean isPermissionGranted = ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                || ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;

        if (isPermissionGranted) {
            if (locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.NETWORK_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, new NetworkLocationListener(this));
                netLocation = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            }
            if (locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                locationManager.requestLocationUpdates(
                        LocationManager.GPS_PROVIDER,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES, new GPSLocationLitener(this));
                gpsLocation = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            }
        }
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


    private class NetworkLocationListener implements LocationListener {
        private LocationCache locationCache;

        NetworkLocationListener(LocationCache locationCache){
            this.locationCache = locationCache;
        }

        @Override
        public void onLocationChanged(Location location) {
            locationCache.netLocation = location;
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

    private class GPSLocationLitener implements LocationListener {
        private LocationCache locationCache;

        GPSLocationLitener(LocationCache locationCache){
            this.locationCache = locationCache;
        }

        @Override
        public void onLocationChanged(Location location) {
            locationCache.gpsLocation = location;
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
}
