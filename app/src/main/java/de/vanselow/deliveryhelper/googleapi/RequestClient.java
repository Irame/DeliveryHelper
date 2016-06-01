package de.vanselow.deliveryhelper.googleapi;

import android.os.AsyncTask;
import android.support.annotation.NonNull;
import android.util.Log;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Map;

import de.vanselow.deliveryhelper.BuildConfig;

public class RequestClient extends AsyncTask<URL, Void, JSONObject> {
    private static final String TAG = RequestClient.class.getName();

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
            Log.e(TAG, "Malformed URL: " + urlString);
        }
        return null;
    }

    @Override
    protected JSONObject doInBackground(@NonNull URL... params) {
        URL url = params[0];
        HttpURLConnection connection = null;
        InputStream inputStream = null;
        JSONObject result = null;
        StringBuilder total;
        try {
            try {
                if (isCancelled()) return null;
                connection = (HttpURLConnection) url.openConnection();
                inputStream = connection.getInputStream();
                BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));
                total = new StringBuilder();
                String line;
                while (!isCancelled() && (line = r.readLine()) != null) {
                    total.append(line).append('\n');
                }
                if (isCancelled()) return null;
                result = new JSONObject(total.toString());
            } finally {
                if (inputStream != null) inputStream.close();
            }
        } catch (IOException | JSONException e) {
            Log.e(TAG, "Error while retrieving the json object from: " + url.toString());
        } finally {
            if (connection != null) connection.disconnect();
        }
        return result;
    }
}
