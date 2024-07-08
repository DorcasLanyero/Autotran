package com.sdgsystems.util;

import java.text.Format;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

// TODO: As part of Android 9/10 upgrade, convert this to use java.time (requires API 26+)
public class SimpleTimeStamp {
    private Date date;
    private SimpleDateFormat df;
    private boolean defaultToUtc;

    public static final long SECONDS = 1000;
    public static final long MINUTES = 60 * SECONDS;
    public static final long HOURS = 60 * MINUTES;
    public static final long DAYS = 24 * HOURS;

    public SimpleTimeStamp() {
        this(false, new Date());
    }

    public SimpleTimeStamp(boolean defaultToUtc) {
        this(defaultToUtc, new Date());
    }

    public SimpleTimeStamp (boolean defaultToUtc, Date date) {
        this.defaultToUtc = defaultToUtc;
        this.date = date;
        this.df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
    }

    public void setTime() {
        setTime(new Date());
    }

    public void setTime (Date date) {
        this.date = date;
    }

    private String getTimeStamp(boolean utc) {
        String dateTime = getDateTime(utc);
        String tz;
        if (utc) {
            tz = getUtcTimeZoneCode();
        }
        else {
            tz = getLocalTimeZone();
        }
        return dateTime + " " + tz;
    }

    public String getTimeStamp() {
        return getTimeStamp(defaultToUtc);
    }

    public String getUtcTimeStamp() {
        return getTimeStamp(true);
    }

    public String getLocalTimeStamp() {
        return getTimeStamp(false);
    }

    private String getDateTime(boolean utc) {
        if (utc) {
            df.setTimeZone(TimeZone.getTimeZone("utc")); // GMT (UTC) time
        }
        else {
            df.setTimeZone(TimeZone.getDefault());
        }
        df.applyPattern("yyyy-MM-dd HH:mm:ss");
        return df.format(date);
    }

    public static String convertUTCTimestampToLocal(String utcTimestamp) {
        Date localTime;
        try {
            if (!utcTimestamp.endsWith(" UTC")) {
                utcTimestamp += " UTC";
            }
            localTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz").parse(utcTimestamp);
        } catch (ParseException | NullPointerException e) {
           return("");
        }
        Format formatter = new SimpleDateFormat("yyyy-MM-dd h:mm:ss a zzz");
        String s = formatter.format(localTime);
        return s;
    }

    public String getDateTime() {
        return getDateTime(defaultToUtc);
    }

    public String getLocalDateTime() {
        return getDateTime(false);
    }

    public String getUtcDateTime() {
        return getDateTime(true);
    }

    public String getLocalTimeZone() {
        getDateTime(false); // To set timezone to non-UTC
        return df.getTimeZone().getDisplayName(false, TimeZone.SHORT);
    }

    public String getUtcTimeZone() {
        return getUtcTimeZoneCode();
    }

    public boolean olderThan(long num, long unit) {
        return (System.currentTimeMillis() - date.getTime()) > num * unit;
    }

    //public static SimpleTimeStamp getPersistentTimestamp() {};

    public static String getUtcTimeZoneCode() {
        return "UTC";
    }

    public static long timeStampStringToTimeInMillis(String timeStampString) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss zzz");
        try {
            Date mDate = sdf.parse(timeStampString + " UTC");
            return mDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public String addDays(int days) {
        long millisecondsPerDay = 24 * 60 * 60 * 1000;
        long thresholdTime = date.getTime() + ((long)days * millisecondsPerDay);
        Date thresholdDate = new Date(thresholdTime);

        return df.format(thresholdDate);
    }
}
