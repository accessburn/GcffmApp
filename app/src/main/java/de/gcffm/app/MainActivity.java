package de.gcffm.app;

import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.Toast;

//TRI
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
//TRI

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.navigation.NavigationView;

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
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final Pattern COORD_PATTERN = Pattern.compile("([\\d.]+)\\s+([\\d.]+)");

    private static final int MENU_CONTEXT_OPEN_ID = 1;
    private static final int MENU_CONTEXT_NAVIGATE_ID = 2;
    private static final int MENU_CONTEXT_COPY_GEOCODE_ID = 3;
    private static final int MENU_CONTEXT_CALENDAR_ID = 4;
    private static final int MENU_CONTEXT_COPY_COORDS_ID = 5;
    private static final int MENU_CONTEXT_SHARE_ID = 6;
    public static final int ONE_HOUR = 60 * 60 * 1000;
    public static final String TAG = "MainActivity";
    private ListView listView;
    private SwipeRefreshLayout swipeContainer;
    private CustomAdapter adapter;
    private String lastSearch;
    private Switch sw;

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

        adapter = new CustomAdapter(this, R.layout.item, new ArrayList<>());
        listView.setAdapter(adapter);

        swipeContainer = findViewById(R.id.swipeContainer);
        swipeContainer.setOnRefreshListener( refreshEvents(true) );

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        refreshEvents(true);
    }

    private void refreshEvents( boolean orderByDistance ) {
        swipeContainer.setRefreshing(true);
        System.out.println("++++" + orderByDistance);
        new Thread(new EventLoader(this)).start();
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

    @Override
    public boolean onNavigationItemSelected(@NonNull final MenuItem item) {
        // Handle navigation view item clicks here.
        final int id = item.getItemId();

        if (id == R.id.menuItemAbout) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.menuItemTwitter) {
            final Uri uri = Uri.parse("https://twitter.com/gcffm");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else if (id == R.id.menuItemTelegram) {
            final Uri uri = Uri.parse("https://t.me/joinchat/Q8l3UN0g5tFhYjZi");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else if (id == R.id.nav_email) {
            final Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@gcffm.de"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fab_subject));
            startActivity(Intent.createChooser(emailIntent, getString(R.string.fab_chooser_title)));
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
        final AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) menuInfo;
        final GcEvent event = (GcEvent) adapter.getItem(acmi.position);

        if (v.getId() == R.id.listView) {
            menu.add(Menu.NONE, MENU_CONTEXT_OPEN_ID, Menu.NONE, R.string.menu_event_open);
            if (!event.isPast()) {
                menu.add(Menu.NONE, MENU_CONTEXT_NAVIGATE_ID, Menu.NONE, R.string.menu_event_navigate);
            }
            menu.add(Menu.NONE, MENU_CONTEXT_COPY_GEOCODE_ID, Menu.NONE, R.string.menu_event_copy_geocode);
            menu.add(Menu.NONE, MENU_CONTEXT_COPY_COORDS_ID, Menu.NONE, R.string.menu_event_copy_coords);
            if (!event.isPast()) {
                menu.add(Menu.NONE, MENU_CONTEXT_CALENDAR_ID, Menu.NONE, R.string.menu_event_add_to_calendar);
                menu.add(Menu.NONE, MENU_CONTEXT_SHARE_ID, Menu.NONE, R.string.menu_event_share);
            }
        }
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull final Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        final MenuItem searchMenu = menu.findItem(R.id.action_search);
        searchMenu.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(final MenuItem arg0) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(final MenuItem arg0) {
                invalidateOptionsMenu();
                return true;
            }
        });
        // Switch sortierung Entfernung
        MenuItem itemswitch = menu.findItem(R.id.switch_action_bar);
        itemswitch.setActionView(R.layout.use_switch);
        sw = (Switch) menu.findItem(R.id.switch_action_bar).getActionView().findViewById(R.id.switch2);
        sw.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {

                if (isChecked) {
                    refreshEvents(isChecked);
                    Toast.makeText(MainActivity.this, R.string.menu_sort, Toast.LENGTH_SHORT).show();
                } else {
                    refreshEvents(isChecked);
                    Toast.makeText(MainActivity.this, R.string.menu_sort, Toast.LENGTH_SHORT).show();
                }
            }
        });



        final SearchView searchView = (SearchView) searchMenu.getActionView();
        searchView.setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(final String s) {
                Log.d(TAG, "onQueryTextSubmit: " + s);
                searchList(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(final String s) {
                Log.d(TAG, "onQueryTextChange: " + s);
                searchList(s);
                return false;
            }

        });

        return true;
    }

    private void searchList(final String search) {
        this.lastSearch = search;
        adapter.getFilter().filter(search);
    }

    @Override
    public boolean onContextItemSelected(final MenuItem item) {
        final AdapterView.AdapterContextMenuInfo acmi = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();
        final GcEvent event = (GcEvent) adapter.getItem(acmi.position);

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
                        .putExtra(CalendarContract.Events.DESCRIPTION, event.getCalendarDescription())
                        .putExtra(CalendarContract.Events.EVENT_TIMEZONE, GcEvent.GCFFM_TIMEZONE);
                intent.putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, event.getDatum());
                intent.putExtra(CalendarContract.EXTRA_EVENT_END_TIME, event.getEndDatum());
                intent.putExtra(CalendarContract.EXTRA_EVENT_ALL_DAY, false);
                intent.putExtra(CalendarContract.Events.EVENT_LOCATION, event.getCoords());
                startActivity(intent);
                return true;
            case MENU_CONTEXT_SHARE_ID:
                final Intent shareIntent = new Intent(Intent.ACTION_SEND);
                shareIntent.setType("text/plain");
                shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Geocaching Event");
                String shareMessage = "\nHallo, kommst du zu diesem Event?\n\n";
                final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

                shareMessage = shareMessage + "https://coord.info/" + event.getGeocode()
                        + "\n\nStart:\n" + dateFormat.format(event.getDatum()) + " Uhr"
                        + "\n\nKoordinaten:\nhttps://www.google.de/maps/search/" + event.getDecimalCoords() + "/"
                        + "\n\nOwner:\n" + event.getOwner()
                        + "\n\nEin Service von https://gcffm.de";
                shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
                startActivity(Intent.createChooser(shareIntent, "choose one"));


                return true;
            default:
                return super.onContextItemSelected(item);
        }
    }

    private void ShareTheEvent(final int label, final String text) {
        final ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        final ClipData clip = ClipData.newPlainText(getResources().getString(label), text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, getResources().getString(R.string.copied_to_clipboard, getResources().getString(label)), Toast.LENGTH_LONG).show();
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

    public static class EventLoader implements Runnable {

        private final WeakReference<MainActivity> mainActivityRef;

        protected EventLoader(final MainActivity mainActivity) {
            mainActivityRef = new WeakReference<>(mainActivity);
            enableHttpResponseCache(mainActivity);
        }

        private void enableHttpResponseCache(final MainActivity mainActivity) {
            try {
                final long httpCacheSize = 10 * 1024 * 1024; // 10 MiB
                final File httpCacheDir = new File(mainActivity.getCacheDir(), "http");
                Class.forName("android.net.http.HttpResponseCache")
                        .getMethod("install", File.class, long.class)
                        .invoke(null, httpCacheDir, httpCacheSize);
            } catch (final Exception httpResponseCacheNotAvailable) {
                Log.i(TAG, "HTTP response cache is unavailable.");
            }
        }

        @Override
        public void run() {
            HttpURLConnection connection = null;
            BufferedReader reader = null;
            int count = 0;
            final List<GcEvent> events = new ArrayList<>(count);

            try {

                final URL url = new URL(BuildConfig.GCFFM_API_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.connect();

                final InputStream stream = connection.getInputStream();
                //final InputStream stream = getResources().openRawResource(R.raw.gcffm); // for local testing
                reader = new BufferedReader(new InputStreamReader(stream));
                final StringBuilder buffer = new StringBuilder();
                String line;
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
                    event.setOwner(jsonObj.getString("owner"));
                    event.setType(EventType.byName(jsonObj.getString("type")));

                    final String coord = jsonObj.getString("coord");
                    final Matcher matcher = COORD_PATTERN.matcher(coord);
                    if (matcher.find()) {
                        event.setLat(Double.parseDouble(Objects.requireNonNull(matcher.group(1))));
                        event.setLon(Double.parseDouble(Objects.requireNonNull(matcher.group(2))));
                    }

                    event.setDatum(Long.parseLong(jsonObj.getString("datum")) * 1000);
                    final long enddatum = Long.parseLong(jsonObj.getString("enddatum"));
                    event.setEndDatum(enddatum > 0 ? enddatum * 1000 : event.getDatum() + ONE_HOUR);
                    events.add(event);
                }
                onFinished(events, null);
            } catch (final Exception e) {
                Log.e(TAG, "Error refreshing events", e);
                onFinished(events, e);
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
        }

        private void onFinished(final List<GcEvent> events, final Exception exception) {
            final MainActivity mainActivity = mainActivityRef.get();
            if (mainActivity != null) {
                mainActivity.runOnUiThread(() -> mainActivity.onNewEventsLoaded(events, exception));
            }
        }

    }

    private void onNewEventsLoaded(final List<GcEvent> events, final Exception exception) {
        if (isDestroyed()) {
            return;
        }

        swipeContainer.setRefreshing(false);
        if (exception != null) {
            Toast.makeText(getBaseContext(), getString(R.string.event_update_failed) + exception.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            adapter.replace(events);
            if (!TextUtils.isEmpty(lastSearch)) {
                searchList(lastSearch);
            }
        }
    }

}
