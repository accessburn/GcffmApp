package de.gcffm.app;

import android.location.Location;
import android.util.Log;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EventLoader implements Runnable {

    private final WeakReference<MainActivity> mainActivityRef;
    private final boolean orderByDistance;
    private final int maxKm;
    private final Set<String> eventFilter;
    private final Location location;

    protected EventLoader(MainActivity mainActivity, boolean orderByDistance, int maxKm, Location location, Set<String> eventFilter) {
        this.mainActivityRef = new WeakReference<>(mainActivity);
        this.orderByDistance = orderByDistance;
        this.maxKm = maxKm;
        this.eventFilter = eventFilter;
        this.location = location;
        enableHttpResponseCache(mainActivity);
    }

    private void enableHttpResponseCache(MainActivity mainActivity) {
        try {
            long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
            File httpCacheDir = new File(mainActivity.getCacheDir(), "http");
            Class.forName("android.net.http.HttpResponseCache")
                    .getMethod("install", File.class, long.class)
                    .invoke(null, httpCacheDir, httpCacheSize);
        } catch (Exception httpResponseCacheNotAvailable) {
            Log.i(MainActivity.TAG, "HTTP response cache is unavailable.");
        }
    }

    @Override
    public void run() {
        HttpURLConnection connection = null;
        BufferedReader reader = null;
        int count = 0;
        List<GcEvent> events = new ArrayList<>(count);
        MainActivity mainActivity = mainActivityRef.get();

        try {
            String urlParameter;
            if (orderByDistance && location != null) {
                mainActivity.runOnUiThread(() -> Toast.makeText(mainActivity, R.string.menu_sort_distanz, Toast.LENGTH_SHORT).show());
                urlParameter = "&order=distanz&lat=" + location.getLatitude() + "&lon=" + location.getLongitude();
                if (maxKm < MainActivity.MAX_KM_UNLIMITED) {
                    urlParameter += "&km=" + maxKm;
                }
            } else {
                mainActivity.runOnUiThread(() -> Toast.makeText(mainActivity, R.string.menu_sort_time, Toast.LENGTH_SHORT).show());
                urlParameter = "&order=time";
            }

            StringBuilder event = new StringBuilder();
            for (String eventType : eventFilter) {
                if (event.length() > 0) {
                    event.append("|");
                }
                event.append(eventType.toLowerCase(Locale.ROOT));
            }
            urlParameter += "&event=" + event;

            URL url = new URL(BuildConfig.GCFFM_API_URL + urlParameter);
            System.out.println(BuildConfig.GCFFM_API_URL + urlParameter);
            System.out.println(url);
            connection = (HttpURLConnection) url.openConnection();
            connection.connect();

            InputStream stream = connection.getInputStream();
            reader = new BufferedReader(new InputStreamReader(stream));
            StringBuilder buffer = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                buffer.append(line);
            }
            String finalJson = buffer.toString();

            JSONArray eventList = new JSONObject(finalJson).getJSONArray("event");
            count = eventList.length();
            Log.i(MainActivity.TAG, "Parsed " + count + " events");

            for (int i = 0; i < eventList.length(); i++) {
                JSONObject jsonObj = (JSONObject) eventList.get(i);

                GcEvent gcEvent = new GcEvent();
                gcEvent.setName(jsonObj.getString("name"));
                gcEvent.setGeocode(jsonObj.getString("geocode"));
                gcEvent.setOwner(jsonObj.getString("owner"));
                gcEvent.setType(EventType.byName(jsonObj.getString("type")));
                gcEvent.setLat(jsonObj.getDouble("lat"));
                gcEvent.setLon(jsonObj.getDouble("lon"));
                gcEvent.setDistanz(jsonObj.getDouble("distanz"));
                gcEvent.setDatum(Long.parseLong(jsonObj.getString("datum")) * 1000);
                gcEvent.setOrt(jsonObj.getString("ort"));
                gcEvent.setBundesland(jsonObj.getString("bl"));
                long enddatum = Long.parseLong(jsonObj.getString("enddatum"));
                gcEvent.setEndDatum(enddatum > 0 ? enddatum * 1000 : gcEvent.getDatum() + MainActivity.ONE_HOUR);
                events.add(gcEvent);
            }
            onFinished(events, null);
        } catch (Exception e) {
            Log.e(MainActivity.TAG, "Error refreshing events", e);
            onFinished(events, e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
            try {
                if (reader != null) {
                    reader.close();
                }
            } catch (IOException e) {
                Log.e(MainActivity.TAG, "Cannot close reader", e);
            }
        }
    }

    private void onFinished(List<GcEvent> events, Exception exception) {
        MainActivity mainActivity = mainActivityRef.get();
        if (mainActivity != null) {
            mainActivity.runOnUiThread(() -> mainActivity.onNewEventsLoaded(events, exception));
        }
    }

}
