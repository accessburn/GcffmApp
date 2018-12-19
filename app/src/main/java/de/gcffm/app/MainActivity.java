package de.gcffm.app;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
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

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String API_START_URL = "https://gcffm.de/api.php?module=event&action=get&ver=2";
    private static final Pattern COORD_PATTERN = Pattern.compile("([\\d\\.]+)\\s+([\\d\\.]+)");

    private static final int MENU_CONTEXT_OPEN_ID = 1;
    private static final int MENU_CONTEXT_NAVIGATE_ID = 2;
    private static final int MENU_CONTEXT_COPY_GEOCODE_ID = 3;
    private static final int MENU_CONTEXT_CALENDAR_ID = 4;
    private static final int MENU_CONTEXT_COPY_COORDS_ID = 5;
    public static final int ONE_HOUR = 60 * 60 * 1000;
    public final String TAG = "MainActivity";
    private ListView listView;
    private SwipeRefreshLayout swipeContainer;
    private CustomAdapter adapter;

    @Override
    protected void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main2);
        final Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        final ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        final NavigationView navigationView = findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        listView = findViewById(R.id.listView);
        registerForContextMenu(listView);

        adapter = new CustomAdapter(this, R.layout.item, new ArrayList<GcEvent>());
        listView .setAdapter(adapter);

        swipeContainer = findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshEvents();
            }
        });

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        refreshEvents();
    }

    private void refreshEvents() {
        new JSONTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
    }

    @Override
    public void onBackPressed() {
        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();

        if (id == R.id.menuItemAbout) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.menuItemTwitter) {
            final Uri uri = Uri.parse("https://twitter.com/gcffm");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else if (id == R.id.menuItemFacebook) {
            final Uri uri = Uri.parse("https://www.facebook.com/gcffm/");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        }

        final DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
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
                        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, "GMT+1");
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getDatum().getTime());
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getDatum().getTime() + ONE_HOUR);
                intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false);
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

            final List<GcEvent> events = new ArrayList<>(count);
            try {
                final URL url = new URL(API_START_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                final InputStream stream = connection.getInputStream();
                reader = new BufferedReader(new InputStreamReader(stream));
                final StringBuilder buffer = new StringBuilder();
                String line = "";
                while ((line = reader.readLine()) != null) {
                    buffer.append(line);
                }
                final String finalJson = buffer.toString();

                final JSONArray eventList = new JSONObject(finalJson).getJSONArray("event");
                count = eventList.length();
                Log.i(TAG, "Parsed " + count + " events");

                for (int i = 0; i < eventList.length(); i++) {
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

            swipeContainer.setRefreshing(false);
            if (exception != null) {
                Toast.makeText(getBaseContext(), getString(R.string.event_update_failed) + exception.getMessage(), Toast.LENGTH_LONG).show();
            } else {
                adapter.clear();
                adapter.addAll(gcEvents);
            }

            unlockScreenOrientation();
        }

        @Override
        protected void onPreExecute() {
            lockScreenOrientation();
            swipeContainer.setRefreshing(true);
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
