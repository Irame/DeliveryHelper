package de.vanselow.deliveryhelper.utils;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

public class Utils {
    public static void startNavigation(Context context, LatLng destination) {
        Uri gmmIntentUri = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", destination.latitude, destination.longitude));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mapIntent.setPackage("com.google.android.apps.maps");
        context.startActivity(mapIntent);
    }
}
