package de.gcffm.app;

import android.content.Context;
import android.os.Build;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Adapter for ListView of GcEvents
 */
public class CustomAdapter extends BaseAdapter implements Filterable {

    private final int resourceLayout;
    private final Object lock = new Object();
    private ArrayFilter filter;
    private List<GcEvent> allEvents;
    private List<GcEvent> events;
    private final Context context;

    public CustomAdapter(final Context context, final int resource, final List<GcEvent> events) {
        super();

        this.context = context;
        this.allEvents = new ArrayList<>(events);
        this.events = events;
        this.resourceLayout = resource;
    }

    @Override
    public int getCount() {
        return events != null ? events.size() : 0;
    }

    @Override
    public GcEvent getItem(final int position) {
        return events.get(position);
    }

    @Override
    public long getItemId(final int position) {
        return position;
    }

    @NonNull
    @Override
    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            final LayoutInflater vi;
            vi = LayoutInflater.from(context);
            view = vi.inflate(resourceLayout, null);
        }

        final GcEvent p = events.get(position);

        if (p != null) {
            final TextView date = view.findViewById(R.id.eventDate);
            final TextView coord = view.findViewById(R.id.eventCoord);
            final TextView name = view.findViewById(R.id.eventName);
            final TextView type = view.findViewById(R.id.eventType);
            final TextView owner = view.findViewById(R.id.eventOwner);
            final ImageView icon = view.findViewById(R.id.eventIcon);
            final float alpha = p.isPast() ? 0.5f : 1.0f;

            final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            date.setText(dateFormat.format(p.getDatumAsCalendar().getTime()));
            date.setAlpha(alpha);
            coord.setText(p.getCoords());
            coord.setAlpha(alpha);

            if (Build.VERSION.SDK_INT >= 24) {
                name.setText(Html.fromHtml(p.getName(), Html.FROM_HTML_MODE_LEGACY));
            } else {
                name.setText(Html.fromHtml(p.getName()));
            }
            name.setAlpha(alpha);

            if (p.isToday()) {

                if (p.getType().getDescription() == "news")
                {
                    view.setBackgroundResource(R.drawable.item_list_backgroundcolor_today_news);
                } else {
                    view.setBackgroundResource(R.drawable.item_list_backgroundcolor_today);
                }
            } else {
                if (position % 2 == 1) {
                    view.setBackgroundResource(R.drawable.item_list_backgroundcolor);
                } else {
                    view.setBackgroundResource(R.drawable.item_list_backgroundcolor2);
                }
            }
System.out.println(p.getType().getDescription());
            type.setText(p.getType().getDescription());
            type.setAlpha(alpha);
            owner.setText(p.getOwner());
            owner.setAlpha(alpha);

            icon.setImageDrawable(ResourcesCompat.getDrawable(context.getResources(), p.getType().getIconRessourceId(), null));
        }

        return view;
    }

    @Override
    public Filter getFilter() {
        if (filter == null) {
            filter = new ArrayFilter();
        }
        return filter;
    }

    public void replace(final List<GcEvent> events) {
        this.events = events;
        this.allEvents = new ArrayList<>(events);
        notifyDataSetInvalidated();
    }

    /**
     * An array filters constrains the content of the array adapter with a prefix. Each
     * item that does not start with the supplied prefix is removed from the list.
     */
    private class ArrayFilter extends Filter {
        @Override
        protected FilterResults performFiltering(final CharSequence search) {
            final FilterResults results = new FilterResults();

            if (search == null || search.length() == 0) {
                synchronized (lock) {
                    results.values = new ArrayList<>(allEvents);
                    results.count = allEvents.size();
                }
            } else {
                final String searchString = search.toString().toLowerCase(Locale.getDefault());
                final List<GcEvent> newValues = new ArrayList<>(allEvents.size());

                for (final GcEvent event : allEvents) {
                    if (event.getName().toLowerCase(Locale.ROOT).contains(searchString)
                            || event.getOwner().toLowerCase(Locale.ROOT).contains(searchString)) {
                        newValues.add(event);
                    }
                }

                results.values = newValues;
                results.count = newValues.size();
            }

            return results;
        }

        @Override
        @SuppressWarnings("unchecked")
        protected void publishResults(final CharSequence constraint, final FilterResults results) {
            events = (List<GcEvent>) results.values;
            if (results.count > 0) {
                notifyDataSetChanged();
            } else {
                notifyDataSetInvalidated();
            }
        }
    }

}
