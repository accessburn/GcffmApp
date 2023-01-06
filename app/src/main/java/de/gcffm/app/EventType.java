package de.gcffm.app;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

enum EventType {
    EVENT("Event", R.drawable.type_event, true),
    MEGA("Mega-Event", R.drawable.type_mega, true),
    GIGA("Giga-Event", R.drawable.type_giga, true),
    CITO("CiTo", R.drawable.type_cito, true),
    MAZE("Maze", R.drawable.type_maze, true),
    NEWS("news", R.drawable.type_news, false),
    COMMUNITYCELEBRATION("Community Celebration", R.drawable.type_event, true);

    private final String description;

    private final int iconRessourceId;

    private final boolean filter;

    EventType(String description, int iconRessourceId, boolean filter) {
        this.description = description;
        this.iconRessourceId = iconRessourceId;
        this.filter = filter;
    }

    public static EventType byName(String name) {
        for (EventType type : values()) {
            if (type.name().toLowerCase().equals(name)) {
                return type;
            }
        }
        return EVENT;
    }

    public static Set<String> allAsSet() {
        Set<String> all = new HashSet<>();
        for (EventType eventType : values()) {
            all.add(eventType.name());
        }
        return all;
    }

    public static EventType[] allFilterAsArray() {
        List<EventType> filters = new ArrayList<>();
        for (EventType eventType : values()) {
            if (eventType.filter) {
                filters.add(eventType);
            }
        }
        return filters.toArray(new EventType[]{});
    }

    public String getDescription() {
        return description;
    }

    public int getIconRessourceId() {
        return iconRessourceId;
    }
}
