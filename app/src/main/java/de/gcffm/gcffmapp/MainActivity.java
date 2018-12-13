package de.gcffm.gcffmapp;

import android.app.ProgressDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONObject;

public class MainActivity extends AppCompatActivity {

    private static final String API_START_URL = "https://gcffm.de/api.php?module=event&action=get";
    private static final Pattern COORD_PATTERN = Pattern.compile("([\\d\\.]+)\\s+([\\d\\.]+)");

    private static final int MENU_CONTEXT_OPEN_ID = 1;
    private static final int MENU_CONTEXT_NAVIGATE_ID = 2;
    private static final int MENU_CONTEXT_COPY_GEOCODE_ID = 3;
    private static final int MENU_CONTEXT_CALENDAR_ID = 4;
    private static final int MENU_CONTEXT_COPY_COORDS_ID = 5;
    public final String TAG = "MainActivity";
    private ListView listView;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        listView = findViewById(R.id.listView);
        registerForContextMenu(listView);

        new JSONTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    public void onCreateContextMenu(final ContextMenu menu, final View v, final ContextMenu.ContextMenuInfo menuInfo) {
        if (v.getId() == R.id.listView) {
            menu.add(Menu.NONE, MENU_CONTEXT_OPEN_ID, Menu.NONE, R.string.menu_event_open);
            menu.add(Menu.NONE, MENU_CONTEXT_NAVIGATE_ID, Menu.NONE, R.string.menu_event_navigate);
            menu.add(Menu.NONE, MENU_CONTEXT_COPY_GEOCODE_ID, Menu.NONE, R.string.menu_event_copy_geocode);
            menu.add(Menu.NONE, MENU_CONTEXT_COPY_COORDS_ID, Menu.NONE, R.string.menu_event_copy_coords);
            menu.add(Menu.NONE, MENU_CONTEXT_CALENDAR_ID, Menu.NONE, R.string.menu_event_add_to_calendar);
        }
    }

    public void launchAbout(final MenuItem menuItem) {
        startActivity(new Intent(MainActivity.this, AboutActivity.class));
    }

    @Override
    public boolean onCreateOptionsMenu(final Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final GcEvent event = (GcEvent) listView.getItemAtPosition(acmi.position);

        switch (item.getItemId()) {
            case MENU_CONTEXT_OPEN_ID:
                final Uri uri = Uri.parse(event.getCoordInfoUrl());
                startActivity(new Intent(Intent.ACTION_VIEW, uri));
                return true;
            case MENU_CONTEXT_NAVIGATE_ID:
                final String location = String.format("geo:0,0?q=%s(%s)", event.getDecimalCoords(), event.getName());
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(location)));
                return true;
            case MENU_CONTEXT_COPY_GEOCODE_ID:
                copyToClipboard(R.string.geocode, event.getGeocode());
                return true;
            case MENU_CONTEXT_COPY_COORDS_ID:
                copyToClipboard(R.string.coords, event.getCoords());
                return true;
            case MENU_CONTEXT_CALENDAR_ID:
                final Intent intent = new Intent(Intent.ACTION_INSERT)
                        .setData(Uri.parse("content://com.android.calendar/events"))
                        .putExtra(CalendarContract.Events.TITLE, stripHtml(event.getName()))
                        .putExtra(CalendarContract.Events.HAS_ALARM, false)
                        .putExtra(CalendarContract.Events.DESCRIPTION, event.getCoordInfoUrl())
                        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "UTC");
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getDatum().getTime());
                intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, true);
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.getCoords());
                startActivity(intent);
                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void copyToClipboard(final int label, final String text) {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText(getResources().getString(label), text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, getResources().getString(R.string.copied_to_clipboard, getResources().getString(label)), Toast.LENGTH_LONG).show();
    }

    public String stripHtml(final String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(html).toString();
        }
    }

    public class JSONTask extends AsyncTask<Void, String, List<GcEvent>> {

        private ProgressDialog progressDialog;
        private Exception exception;

        private void enableHttpResponseCache() {
            try {
                final long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
                final File httpCacheDir = new File(getCacheDir(), "http");
                Class.forName("android.net.http.HttpResponseCache")
                        .getMethod("install", File.class, long.class)
                        .invoke(null, httpCacheDir, httpCacheSize);
            } catch (final Exception httpResponseCacheNotAvailable) {
                Log.i(TAG, "HTTP response cache is unavailable.");
            }
        }

        protected JSONTask() {
            enableHttpResponseCache();
        }

        @Override
        protected List<GcEvent> doInBackground(final Void... params) {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            int count = 0;

            publishProgress(getResources().getString(R.string.connecting));
            final List<GcEvent> events = new ArrayList<>(count);
            try {
                final URL url = new URL(API_START_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                publishProgress(getString(R.string.reading_data));

                final InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                final StringBuilder buffer = new StringBuilder();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                final String finalJson = buffer.toString();

                publishProgress(getString(R.string.processing_data));
                final JSONArray eventList = new JSONObject(finalJson).getJSONArray("event");
                count = eventList.length();
                Log.i(TAG, "Parsed " + count + " events");

                for (int i = 0; i < eventList.length(); i++) {
                    publishProgress(getResources().getString(R.string.processing_item_of, i, count));
                    final JSONObject jsonObj = (JSONObject) eventList.get(i);

                    final GcEvent event = new GcEvent();
                    event.setName(jsonObj.getString("name"));
                    event.setGeocode(jsonObj.getString("geocode"));

                    final String coord = jsonObj.getString("coord");
                    final Matcher matcher = COORD_PATTERN.matcher(coord);
                    if (matcher.find()) {
                        event.setLat(Double.parseDouble(matcher.group(1)));
                        event.setLon(Double.parseDouble(matcher.group(2)));
                    }

                    event.setDatum(new Date(Long.parseLong(jsonObj.getString("datum")) * 1000));

                    events.add(event);
                }

                publishProgress(getString(R.string.events_loaded));
            } catch (final Exception e) {
                Log.e(TAG, "Error refreshing events", e);
                exception = e;
            } finally {
                if (connection != null) {
                    connection.disconnect();
                }
                try {
                    if (reader != null) {
                        reader.close();
                    }
                } catch (final IOException e) {
                    Log.e(TAG, "Cannot close reader", e);
                }
            }
            return events;
        }

        @Override
        protected void onPostExecute(final List<GcEvent> gcEvents) {
            if (MainActivity.this.isDestroyed()) {
                return;
            }
            if (progressDialog != null && progressDialog.isShowing()) {
                progressDialog.dismiss();
            }

            if (exception != null) {
                Toast.makeText(getBaseContext(), getString(R.string.event_update_failed) + exception.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                final CustomAdapter customAdapter = new CustomAdapter(getApplicationContext(), R.layout.item, gcEvents);
                listView .setAdapter(customAdapter);
            }

            unlockScreenOrientation();
        }

        @Override
        protected void onPreExecute() {
            lockScreenOrientation();
            progressDialog = new ProgressDialog(MainActivity.this);
            progressDialog.setIndeterminate(false);

            // show it
            progressDialog.show();
        }

        @Override
        protected void onProgressUpdate(final String... values) {
            super.onProgressUpdate(values);
            progressDialog.setMessage(values[0]);

        }

        private void lockScreenOrientation() {
            final int currentOrientation = getResources().getConfiguration().orientation;
            if (currentOrientation == Configuration.ORIENTATION_PORTRAIT) {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
            } else {
                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
            }
        }

        private void unlockScreenOrientation() {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR);
        }
    }

}
