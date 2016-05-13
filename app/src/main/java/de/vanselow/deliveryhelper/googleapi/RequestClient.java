package de.vanselow.deliveryhelper.googleapi;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import de.vanselow.deliveryhelper.BuildConfig;

/**
 * Created by Felix on 13.05.2016.
 */
public class RequestClient extends AsyncTask<URL, Void, JSONObject> {
    private static final String APIKEY = BuildConfig.GOOGLE_MAPS_API_KEY;

    public AsyncTask<URL, Void, JSONObject> execute(String apiType, String apiSubtype, Map<String, String> paramsMap) {
        StringBuilder params = new StringBuilder();
        for (Map.Entry<String, String> param : paramsMap.entrySet()) {
            if (params.length() != 0)
                params.append("&");
            params.append(param.getKey());
            params.append("=");
            params.append(param.getValue());
        }
        String urlString = String.format(Locale.ENGLISH, "https://%1$s.googleapis.com/%1$s/api/%2$s/json?%3$s&key=%4$s", apiType, apiSubtype, params.toString(), APIKEY);
        try {
            URL url = new URL(urlString);
            return execute(url);
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected JSONObject doInBackground(@NonNull URL... params) {
        URL url = params[0];
        HttpURLConnection connection;
        JSONObject result = null;
        StringBuilder total;
        try {
            connection = (HttpURLConnection) url.openConnection();

            BufferedReader r = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            total = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) {
                total.append(line).append('\n');
            }
            result = new JSONObject(total.toString());
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }
}
