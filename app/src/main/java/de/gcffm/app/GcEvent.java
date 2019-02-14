package de.gcffm.app;

import android.support.annotation.NonNull;

import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

class GcEvent {
    public static final TimeZone GCFFM_TIMEZONE = TimeZone.getTimeZone("GMT+1");

    private String name;
    private String geocode;
    private long datum;
    private long endDatum;
    private double lat;
    private double lon;
    private String owner;
    private EventType type = EventType.EVENT;

    public long getDatum() {
        return datum;
    }

    public void setDatum(final long datum) {
        this.datum = datum;
    }

    public String getGeocode() {
        return geocode;
    }

    public void setGeocode(final String geocode) {
        this.geocode = geocode;
    }

    public String getName() {
        return name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public void setLat(final double lat) {
        this.lat = lat;
    }

    public double getLat() {
        return lat;
    }

    public void setLon(final double lon) {
        this.lon = lon;
    }

    public double getLon() {
        return lon;
    }

    public String getDecimalCoords() {
        return String.format("%s,%s", lat, lon);
    }

    public String getCoordInfoUrl() {
        return "https://coord.info/" + geocode;
    }

    public String getCoords() {
        return String.format(Locale.ENGLISH, "%c %02d° %06.3f %c %03d° %06.3f",
                getLatDir(), getLatDeg(), getLatMinRaw(),
                getLonDir(), getLonDeg(), getLonMinRaw());
    }

    private char getLonDir() {
        return lon >= 0 ? 'E' : 'W';
    }

    private char getLatDir() {
        return lat >= 0 ? 'N' : 'S';
    }

    private int getLatDeg() {
        return getDeg(getLatitudeE6());
    }

    private int getLatitudeE6() {
        return (int) Math.round(lat * 1e6);
    }

    private int getLonDeg() {
        return getDeg(getLongitudeE6());
    }

    private int getLongitudeE6() {
        return (int) Math.round(lon * 1e6);
    }

    private static int getDeg(final int degE6) {
        return Math.abs(degE6 / 1000000);
    }

    private double getLatMinRaw() {
        return getMinRaw(getLatitudeE6());
    }

    private double getLonMinRaw() {
        return getMinRaw(getLongitudeE6());
    }

    private static double getMinRaw(final int degE6) {
        return (Math.abs(degE6) * 60L % 60000000L) / 1000000d;
    }

    public boolean isToday() {
        final Calendar today = Calendar.getInstance();
        final Calendar eventCal = getDatumAsCalendar();

        return today.get(Calendar.DAY_OF_YEAR) == eventCal.get(Calendar.DAY_OF_YEAR)
                && today.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR);
    }

    public boolean isPast() {
        final Calendar today = Calendar.getInstance();
        final Calendar eventCal = getDatumAsCalendar();

        return today.get(Calendar.DAY_OF_YEAR) > eventCal.get(Calendar.DAY_OF_YEAR)
                && today.get(Calendar.YEAR) >= eventCal.get(Calendar.YEAR);
    }

    @NonNull
    public Calendar getDatumAsCalendar() {
        final Calendar eventCal = Calendar.getInstance();
        eventCal.setTimeInMillis(datum);
        eventCal.setTimeZone(GCFFM_TIMEZONE);
        return eventCal;
    }

    public void setOwner(final String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setType(final EventType type) {
        this.type = type;
    }

    public EventType getType() {
        return type;
    }

    public String getCalendarDescription() {
        return String.format("%s\n%s\nein Service von https://gcffm.de", getCoordInfoUrl(), owner);
    }

    public long getEndDatum() {
        return endDatum;
    }

    public void setEndDatum(final long endDatum) {
        this.endDatum = endDatum;
    }
}
