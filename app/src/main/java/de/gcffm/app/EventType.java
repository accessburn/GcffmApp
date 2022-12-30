package de.gcffm.app;

import java.util.HashSet;
import java.util.Set;

enum EventType {
    EVENT("Event", R.drawable.type_event),
    MEGA("Mega-Event", R.drawable.type_mega),
    GIGA("Giga-Event", R.drawable.type_giga),
    CITO("CiTo", R.drawable.type_cito),
    MAZE("Maze", R.drawable.type_maze),
    COMMUNITYCELEBRATION("Community Celebration", R.drawable.type_event);

    private final String description;

    private final int iconRessourceId;

    EventType(final String description, int iconRessourceId) {
        this.description = description;
        this.iconRessourceId = iconRessourceId;
    }

    public static EventType byName(final String name) {
        for (final EventType type : values()) {
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

    public String getDescription() {
        return description;
    }

    public int getIconRessourceId() {
        return iconRessourceId;
    }
}
