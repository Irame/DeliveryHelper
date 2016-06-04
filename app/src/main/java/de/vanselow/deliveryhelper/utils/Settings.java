package de.vanselow.deliveryhelper.utils;

import android.content.Context;
import android.content.SharedPreferences;

import de.vanselow.deliveryhelper.RemoteAccess;

import static de.vanselow.deliveryhelper.utils.Utils.isValidPort;

public abstract class Settings {
    public static final String SHARED_PREFERENCES_NAME = "DeliveryHelperGeneralSettings";

    public static final String AUTOSORT_OPTION_PREFKEY = "autosortOption";
    public static final boolean AUTOSORT_OPTION_DEFAULT = false;

    public static final String REMOTEACCESS_OPTION_PREFKEY = "remoteaccessOption";
    public static final boolean REMOTEACCESS_OPTION_DEFAULT = false;

    public static final String REMOTEACCESS_PORT_PREFKEY = "remoteaccessPort";
    public static final int REMOTEACCESS_PORT_DEFAULT = 5678;

    private static SharedPreferences getSharedPreferences(Context context) {
        return context.getSharedPreferences(SHARED_PREFERENCES_NAME, Context.MODE_PRIVATE);
    }


    public static boolean isAutosortForOpenLocationsEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(AUTOSORT_OPTION_PREFKEY, AUTOSORT_OPTION_DEFAULT);
    }

    public static void setAutosortForOpenLocations(Context context, boolean enabled) {
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(AUTOSORT_OPTION_PREFKEY, enabled);
        editor.apply();
    }


    public static boolean isRemoteAccessEnabled(Context context) {
        return getSharedPreferences(context).getBoolean(REMOTEACCESS_OPTION_PREFKEY, REMOTEACCESS_OPTION_DEFAULT);
    }

    public static void setRemoteAccess(Context context, boolean enabled) {
        if (enabled)
            RemoteAccess.start(context);
        else
            RemoteAccess.stop();
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putBoolean(REMOTEACCESS_OPTION_PREFKEY, enabled);
        editor.apply();
    }


    public static int getRemoteAccessPort(Context context) {
        return getSharedPreferences(context).getInt(REMOTEACCESS_PORT_PREFKEY, REMOTEACCESS_PORT_DEFAULT);
    }

    public static boolean setRemoteAccessPort(Context context, int port) {
        if (!isValidPort(port)) return false;
        SharedPreferences.Editor editor = getSharedPreferences(context).edit();
        editor.putInt(REMOTEACCESS_PORT_PREFKEY, port);
        editor.apply();
        if (isRemoteAccessEnabled(context)) RemoteAccess.start(context);
        return true;
    }
}
