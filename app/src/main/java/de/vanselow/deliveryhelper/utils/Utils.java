package de.vanselow.deliveryhelper.utils;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.support.v7.app.AlertDialog;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;

import java.util.Locale;

import de.vanselow.deliveryhelper.R;

public abstract class Utils {
    public static void startNavigation(Context context, LatLng destination) {
        Uri gmmIntentUri = Uri.parse(String.format(Locale.ENGLISH, "google.navigation:q=%f,%f", destination.latitude, destination.longitude));
        Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
        //mapIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        mapIntent.setPackage("com.google.android.apps.maps");
        context.startActivity(mapIntent);
    }

    public static BitmapDescriptor getBitmapDescriptor(Drawable vectorDrawable) {
        Rect bounds = new Rect(0, 0, vectorDrawable.getIntrinsicWidth(), vectorDrawable.getIntrinsicHeight());
        vectorDrawable.setBounds(bounds);
        Bitmap bm = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bm);
        vectorDrawable.draw(canvas);
        return BitmapDescriptorFactory.fromBitmap(bm);
    }

    public static AlertDialog deleteAlert(Context context, String item, DialogInterface.OnClickListener deleteButtonListener)
    {
        return new AlertDialog.Builder(context)
                //set message, title, and icon
                .setTitle(context.getString(R.string.delete_q))
                .setMessage(context.getString(R.string.do_you_want_to_delete) + (item == null ? "?" : ":\n'" + item + "' ?"))
                .setIcon(R.drawable.ic_list_item_remove)

                .setPositiveButton(context.getString(R.string.delete_button), deleteButtonListener)
                .setNegativeButton(context.getString(R.string.cancel_button), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                .create();

    }
}
