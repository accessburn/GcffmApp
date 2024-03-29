package de.gcffm.app;

import androidx.annotation.NonNull;

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
    private double distanz;
    private String owner;
    private EventType type = EventType.EVENT;
    private String ort;
    private String bundesland;

    public long getDatum() {
        return datum;
    }

    public void setDatum(long datum) {
        this.datum = datum;
    }

    public String getGeocode() {
        return geocode;
    }

    public void setGeocode(String geocode) {
        this.geocode = geocode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLat() {
        return lat;
    }

    public void setLon(double lon) {
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

    private static int getDeg(int degE6) {
        return Math.abs(degE6 / 1000000);
    }

    private double getLatMinRaw() {
        return getMinRaw(getLatitudeE6());
    }

    private double getLonMinRaw() {
        return getMinRaw(getLongitudeE6());
    }

    private static double getMinRaw(int degE6) {
        return (Math.abs(degE6) * 60L % 60000000L) / 1000000d;
    }

    public boolean isToday() {
        Calendar today = Calendar.getInstance();
        Calendar eventCal = getDatumAsCalendar();

        return today.get(Calendar.DAY_OF_YEAR) == eventCal.get(Calendar.DAY_OF_YEAR)
                && today.get(Calendar.YEAR) == eventCal.get(Calendar.YEAR);
    }

    public boolean isPast() {
        Calendar today = Calendar.getInstance();
        Calendar eventCal = getDatumAsCalendar();

        return today.get(Calendar.DAY_OF_YEAR) > eventCal.get(Calendar.DAY_OF_YEAR)
                && today.get(Calendar.YEAR) >= eventCal.get(Calendar.YEAR);
    }

    @NonNull
    public Calendar getDatumAsCalendar() {
        Calendar eventCal = Calendar.getInstance();
        eventCal.setTimeInMillis(datum);
        eventCal.setTimeZone(GCFFM_TIMEZONE);
        return eventCal;
    }

    public void setOwner(String owner) {
        this.owner = owner;
    }

    public String getOwner() {
        return owner;
    }

    public void setType(EventType type) {
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

    public void setEndDatum(long endDatum) {
        this.endDatum = endDatum;
    }

    public double getDistanz() {
        return distanz;
    }

    public void setDistanz(double distanz) {
        this.distanz = distanz;
    }

    public void setOrt(final String ort) {
        this.ort = ort;
    }

    public String getOrt() {
        return ort;
    }

    public void setBundesland(final String bundesland) {
        this.bundesland = bundesland;
    }

    public String getBundesland() {
        return bundesland;
    }
}
