package de.gcffm.app;

import static android.Manifest.permission.ACCESS_COARSE_LOCATION;

import android.Manifest;
import android.app.SearchManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.CalendarContract;
import android.text.Html;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SeekBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.navigation.NavigationView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import de.gcffm.app.databinding.ActivityMain2Binding;
import de.gcffm.app.databinding.MaxKmDialogBinding;


public class MainActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    public static final int ONE_HOUR = 60 * 60 * 1000;
    public static final String TAG = "MainActivity";
    public static final int MAX_KM_UNLIMITED = 200;
    private SwipeRefreshLayout swipeContainer;
    private CustomAdapter adapter;
    private String lastSearch;
    private SwitchCompat sw;
    private Location lastKnownLocation;
    private de.gcffm.app.databinding.MaxKmDialogBinding binding;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        ActivityMain2Binding binding = ActivityMain2Binding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.appBarMain2.toolbar);

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, binding.drawerLayout, binding.appBarMain2.toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        binding.drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        binding.navView.setNavigationItemSelectedListener(this);

        ListView listView = binding.appBarMain2.contentMain2.listView;
        registerForContextMenu(listView);

        adapter = new CustomAdapter(this, R.layout.item, new ArrayList<>());
        listView.setAdapter(adapter);
        listView.setOnItemClickListener((parent, view, position, id) -> {
            GcEvent event = adapter.getItem(position);
            if (event.getType() != EventType.NEWS) {
                showPopupMenu(event);
            }
        });

        swipeContainer = binding.appBarMain2.contentMain2.swipeContainer;
        swipeContainer.setOnRefreshListener(this::refreshEvents);

        // Configure the refreshing colors
        swipeContainer.setColorSchemeResources(android.R.color.holo_blue_bright,
                android.R.color.holo_green_light,
                android.R.color.holo_orange_light,
                android.R.color.holo_red_light);

        refreshEvents();
    }


    ActivityResultLauncher<String[]> locationPermissionRequest =
            registerForActivityResult(new ActivityResultContracts
                            .RequestMultiplePermissions(), result -> {
                        Boolean coarseLocationGranted = result.get(ACCESS_COARSE_LOCATION);
                        if (coarseLocationGranted != null && coarseLocationGranted) {
                            refreshEvents();
                        } else if (sw != null) {
                            Toast.makeText(MainActivity.this, R.string.no_location_permission_granted, Toast.LENGTH_SHORT).show();
                            setSortByTime();
                        }
                    }
            );

    private void refreshEvents() {
        swipeContainer.setRefreshing(true);

        boolean orderByDistance = sw != null && sw.isChecked();

        if (orderByDistance) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                locationPermissionRequest.launch(new String[]{
                        Manifest.permission.ACCESS_COARSE_LOCATION
                });
                return;
            }

            LocationManager locationManager = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
            String bestProvider = locationManager.getBestProvider(new Criteria(), true);
            if (bestProvider != null) {
                lastKnownLocation = locationManager.getLastKnownLocation(bestProvider);
            }
            if (lastKnownLocation == null) {
                Toast.makeText(MainActivity.this, R.string.no_location, Toast.LENGTH_SHORT).show();
                setSortByTime();
                Toast.makeText(MainActivity.this, R.string.menu_sort_time, Toast.LENGTH_SHORT).show();
            }
        }

        new Thread(new EventLoader(this, orderByDistance, PreferencesUtils.getMaxKm(this), lastKnownLocation, PreferencesUtils.getEventFilter(this))).start();
    }

    private void setSortByTime() {
        sw.setChecked(false);
        PreferencesUtils.setSortByDistance(this, false);
    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onNavigationItemSelected(@NonNull MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.menuItemAbout) {
            startActivity(new Intent(this, AboutActivity.class));
        } else if (id == R.id.menuItemTwitter) {
            Uri uri = Uri.parse("https://twitter.com/gcffm");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else if (id == R.id.menuItemTelegram) {
            Uri uri = Uri.parse("https://t.me/joinchat/Q8l3UN0g5tFhYjZi");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else if (id == R.id.nav_email) {
            Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:info@gcffm.de"));
            emailIntent.putExtra(Intent.EXTRA_SUBJECT, getString(R.string.fab_subject));
            startActivity(Intent.createChooser(emailIntent, getString(R.string.fab_chooser_title)));
        } else if (id == R.id.menuItemFacebook) {
            Uri uri = Uri.parse("https://www.facebook.com/gcffm/");
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else if (id == R.id.menuItemMaxKm) {
            showMaxKmDialog();
        } else if (id == R.id.menuItemEventFilter) {
            showEventFilterDialog();
        }

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(@NonNull Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);

        MenuItem searchMenu = menu.findItem(R.id.action_search);
        searchMenu.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(@NonNull MenuItem arg0) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(@NonNull MenuItem arg0) {
                invalidateOptionsMenu();
                return true;
            }
        });
        // Switch sortierung Entfernung
        MenuItem itemswitch = menu.findItem(R.id.switch_action_bar);
        itemswitch.setActionView(R.layout.use_switch);
        sw = menu.findItem(R.id.switch_action_bar).getActionView().findViewById(R.id.switch2);
        sw.setChecked(PreferencesUtils.getSortByDistance(this));
        sw.setOnCheckedChangeListener((buttonView, isChecked) -> {
            PreferencesUtils.setSortByDistance(this, isChecked);
            refreshEvents();
        });

        SearchView searchView = (SearchView) searchMenu.getActionView();
        searchView.setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE)).getSearchableInfo(getComponentName()));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {

            @Override
            public boolean onQueryTextSubmit(String s) {
                Log.d(TAG, "onQueryTextSubmit: " + s);
                searchList(s);
                return false;
            }

            @Override
            public boolean onQueryTextChange(String s) {
                Log.d(TAG, "onQueryTextChange: " + s);
                searchList(s);
                return false;
            }

        });

        return true;
    }

    private void searchList(String search) {
        this.lastSearch = search;
        adapter.getFilter().filter(search);
    }

    private void showPopupMenu(final GcEvent event) {
        List<String> menuList = new ArrayList<>();
        menuList.add(getString(R.string.menu_event_open));
        if (!event.isPast()) {
            menuList.add(getString(R.string.menu_event_navigate));
        }
        menuList.add(getString(R.string.menu_event_copy_geocode));
        menuList.add(getString(R.string.menu_event_copy_coords));
        if (!event.isPast()) {
            menuList.add(getString(R.string.menu_event_add_to_calendar));
            menuList.add(getString(R.string.menu_event_share));
        }
        String[] menuEntries = menuList.toArray(new String[]{});

        AlertDialog alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(event.getCoords() + "\n" + event.getGeocode())
                .setItems(menuEntries,
                        (dialog, which) -> onPopupMenuItemSelected(menuEntries[which], event))
                .setPositiveButton(android.R.string.ok, null)
                .create();
        alertDialog.show();
    }

    private void onPopupMenuItemSelected(String menuName, GcEvent event) {
        if (menuName.equals(getString(R.string.menu_event_open))) {
            Uri uri = Uri.parse(event.getCoordInfoUrl());
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } else if (menuName.equals(getString(R.string.menu_event_navigate))) {
            String location = String.format("geo:0,0?q=%s(%s)", event.getDecimalCoords(), event.getName());
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(location)));
        } else if (menuName.equals(getString(R.string.menu_event_copy_geocode))) {
            copyToClipboard(R.string.geocode, event.getGeocode());
        } else if (menuName.equals(getString(R.string.menu_event_copy_coords))) {
            copyToClipboard(R.string.coords, event.getCoords());
        } else if (menuName.equals(getString(R.string.menu_event_add_to_calendar))) {
            Intent intent = new Intent(Intent.ACTION_INSERT)
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
        } else if (menuName.equals(getString(R.string.menu_event_share))) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND);
            shareIntent.setType("text/plain");
            shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Geocaching Event");
            String shareMessage = "\nHallo, kommst du zu diesem Event?\n\n";
            DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);

            shareMessage = shareMessage + "https://coord.info/" + event.getGeocode()
                    + "\n\nStart:\n" + dateFormat.format(event.getDatum()) + " Uhr"
                    + "\n\nKoordinaten:\nhttps://www.google.de/maps/search/" + event.getDecimalCoords() + "/"
                    + "\n\nOwner:\n" + event.getOwner()
                    + "\n\nEin Service von https://gcffm.de";
            shareIntent.putExtra(Intent.EXTRA_TEXT, shareMessage);
            startActivity(Intent.createChooser(shareIntent, "choose one"));
        }
    }

    private void copyToClipboard(int label, String text) {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText(getResources().getString(label), text);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(this, getResources().getString(R.string.copied_to_clipboard, getResources().getString(label)), Toast.LENGTH_LONG).show();
    }

    public String stripHtml(String html) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
            return Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString();
        } else {
            return Html.fromHtml(html).toString();
        }
    }

    public void onNewEventsLoaded(List<GcEvent> events, Exception exception) {
        if (isDestroyed()) {
            return;
        }

        swipeContainer.setRefreshing(false);
        if (exception != null) {
            Toast.makeText(getBaseContext(), getString(R.string.event_update_failed) + exception.getMessage(), Toast.LENGTH_LONG).show();
        } else {
            if (!PreferencesUtils.getShowOldEvents(this)) {
                List<GcEvent> oldEvents = new ArrayList<>();
                for (GcEvent event : events) {
                    if (event.isPast()) {
                        oldEvents.add(event);
                    }
                }
                events.removeAll(oldEvents);
            }
            adapter.replace(events);
            if (!TextUtils.isEmpty(lastSearch)) {
                searchList(lastSearch);
            }
        }
    }

    private void showMaxKmDialog() {
        binding = MaxKmDialogBinding.inflate(LayoutInflater.from(this));
        binding.sbMaxKm.setProgress(PreferencesUtils.getMaxKm(this));
        setMaxKmText();
        binding.sbMaxKm.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int i, boolean b) {
                setMaxKmText();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });

        AlertDialog alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
                .setView(binding.getRoot())
                .setIcon(R.drawable.icon_1)
                .setTitle(R.string.app_name)
                .setCancelable(false)
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            PreferencesUtils.setMaxKm(this, getSelectedMaxKm());
            alertDialog.dismiss();
            if (sw.isChecked()) {
                refreshEvents();
            }
        });
    }

    private int getSelectedMaxKm() {
        return Math.max(binding.sbMaxKm.getProgress(), 1);
    }

    private void setMaxKmText() {
        int maxKm = getSelectedMaxKm();
        if (maxKm < MAX_KM_UNLIMITED) {
            binding.maxKm.setText(getString(R.string.max_km_info, String.valueOf(maxKm)));
        } else {
            binding.maxKm.setText(getString(R.string.max_km_info, getString(R.string.max_km_unlimited)));
        }
    }

    private void showEventFilterDialog() {
        Set<String> selectedEventFilter = PreferencesUtils.getEventFilter(this);
        EventType[] allEventFilter = EventType.allFilterAsArray();
        String[] eventNames = new String[allEventFilter.length + 1];
        boolean[] selected = new boolean[allEventFilter.length + 1];

        eventNames[0] = getString(R.string.outdated_events);
        selected[0] = PreferencesUtils.getShowOldEvents(this);

        for (int i = 0; i < allEventFilter.length; i++) {
            EventType eventTypeFilter = allEventFilter[i];
            eventNames[i + 1] = eventTypeFilter.getDescription();
            selected[i + 1] = selectedEventFilter.contains(eventTypeFilter.name());
        }

        AlertDialog alertDialog = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.AlertDialogCustom))
                .setIcon(R.mipmap.ic_launcher)
                .setTitle(R.string.event_filter)
                .setMultiChoiceItems(eventNames, selected,
                        (dialog, which, isChecked) -> {
                            if (which == 0) {
                                PreferencesUtils.setShowOldEvents(this, isChecked);
                            } else {
                                String name = allEventFilter[which - 1].name();
                                if (isChecked) {
                                    selectedEventFilter.add(name);
                                } else {
                                    selectedEventFilter.remove(name);
                                }
                            }
                        })
                .setPositiveButton(android.R.string.ok, null)
                .setNegativeButton(android.R.string.cancel, null)
                .create();
        alertDialog.show();

        alertDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            PreferencesUtils.setEventFilter(MainActivity.this, selectedEventFilter);
            alertDialog.dismiss();
            refreshEvents();
        });
    }

}
