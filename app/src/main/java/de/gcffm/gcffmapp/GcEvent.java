package de.gcffm.gcffmapp;

import java.util.Date;
import java.util.Locale;

class GcEvent {
    private String name;
    private String geocode;
    private Date datum;
    private double lat;
    private double lon;

    public Date getDatum() {
        return datum;
    }

    public void setDatum(final Date datum) {
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

}
