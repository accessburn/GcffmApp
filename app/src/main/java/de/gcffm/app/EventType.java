package de.gcffm.app;

enum EventType {
    EVENT("Event"),
    MEGA("Mega-Event"),
    GIGA("Giga-Event"),
    CITO("CiTo"),
    MAZE("Maze");

    private final String description;

    EventType(final String description) {
        this.description = description;
    }

    public static EventType byName(final String name) {
        for (final EventType type : values()) {
            if (type.name().toLowerCase().equals(name)) {
                return type;
            }
        }
        return EVENT;
    }

    public String getDescription() {
        return description;
    }
}
