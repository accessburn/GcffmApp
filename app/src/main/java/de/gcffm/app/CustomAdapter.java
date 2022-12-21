package de.gcffm.app;

import android.content.Context;
import android.os.Build;
import androidx.annotation.NonNull;
import android.text.Html;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Adapter for ListView of GcEvents
 */
public class CustomAdapter extends ArrayAdapter<GcEvent> {

    private final int resourceLayout;

    public CustomAdapter(final Context context, final int resource, final List<GcEvent> events) {
        super(context, R.layout.item, events);

        this.resourceLayout = resource;
    }

    @NonNull
    @Override
    public View getView(final int position, final View convertView, @NonNull final ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            final LayoutInflater vi;
            vi = LayoutInflater.from(getContext());
            view = vi.inflate(resourceLayout, null);
        }

        final GcEvent p = getItem(position);

        if (p != null) {
            final TextView date = view.findViewById(R.id.eventDate);
            final TextView coord = view.findViewById(R.id.eventCoord);
            final TextView name = view.findViewById(R.id.eventName);
            final TextView type = view.findViewById(R.id.eventType);
            final TextView owner = view.findViewById(R.id.eventOwner);
            final float alpha = p.isPast() ? 0.5f : 1.0f;

            final DateFormat dateFormat = SimpleDateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
            date.setText(dateFormat.format(p.getDatumAsCalendar().getTime()));
            date.setAlpha(alpha);
            coord.setText(p.getCoords());
            coord.setAlpha(alpha);

            if (Build.VERSION.SDK_INT >= 24) {
                name.setText(Html.fromHtml(p.getName() , Html.FROM_HTML_MODE_LEGACY));
            } else {
                name.setText(Html.fromHtml(p.getName()));
            }
            name.setAlpha(alpha);

            if (p.isToday()) {
                view.setBackgroundResource(R.drawable.item_list_backgroundcolor_today);
            } else {
                if (position % 2 == 1) {
                    view.setBackgroundResource(R.drawable.item_list_backgroundcolor);
                } else {
                    view.setBackgroundResource(R.drawable.item_list_backgroundcolor2);
                }
            }

            type.setText(p.getType().getDescription());
            type.setAlpha(alpha);
            owner.setText(p.getOwner());
            owner.setAlpha(alpha);
        }

        return view;
    }

}
