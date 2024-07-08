package com.cassens.autotran.handlers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.hardware.PiccoloManager;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;
import java.util.Date;

public class TruckNumberHandler {
    private static final Logger log = LoggerFactory.getLogger(TruckNumberHandler.class.getSimpleName());
    /**
     * Preferences & values used to control source of truck number: manual or Piccolo
     */
    private static final String TRUCK_NUM_PREF = "cassens.truck.num";
    private static final String PICCOLO_TRUCK_NUM_PREF = "cassens.truck.num.piccolo";
    private static final String PICCOLO_PREV_TRUCK_NUM_PREF = "cassens.truck.num.piccolo.prev";
    private static final String PICCOLO_TRUCK_NUM_TIME = "cassens.truck.num.piccolo.time";
    private static final String PICCOLO_TRUCK_NUM_RECEIVED = "cassens.truck.num.piccolo.changed";

    public static final String ACTION_PICCOLO_TRUCK_NUM_RECEIVED = "com.cassens.autotran.PICCOLO_TRUCK_NUM_RECEIVED";

    private static final String HACKED_TRUCK_NUM_PREF = "HACKED_TRUCK_NUM";
    private static final int HACKED_TRUCK_NUM_DEFAULT = 21501;

    public static String getTruckNumber(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getString(TRUCK_NUM_PREF, "").replace("\\u00A0", "");
    }

    public static int getTruckNumberInt(Context context) {
        try {
            return Integer.parseInt(getTruckNumber(context));
        }
        catch (NumberFormatException ne) {
            return 0;
        }
    }

    public static String getPiccoloTruckNumber(Context context, boolean errorCode) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        String truckNum = prefs.getString(PICCOLO_TRUCK_NUM_PREF, Integer.toString(PiccoloManager.PICCOLO_NEVER_DOCKED)).replace("\\u00A0", "");
        if (!errorCode) {
            try {
                if (Integer.parseInt(truckNum) < 0) {
                    return ("");
                }
            } catch (NumberFormatException e) {
                return ("");
            }
        }
        return truckNum;
    }

    public static int getPiccoloTruckNumberInt(Context context, boolean errorCode) {
        int truckNum;
        try {
            truckNum = Integer.parseInt(TruckNumberHandler.getPiccoloTruckNumber(context, errorCode));
        }
        catch (NumberFormatException nfe) {
            truckNum = -1;
        }
        return truckNum;
    }

    public static String getPiccoloTruckNumber(Context context) {
        return getPiccoloTruckNumber(context, false);
    }

    public static String getPiccoloPrevTruckIdPref(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        return prefs.getString(PICCOLO_PREV_TRUCK_NUM_PREF, "").replace("\\u00A0", "");
    }

    public static boolean isTruckNumberSourceManual(Context context) {
        String piccoloTruckNumber = getPiccoloTruckNumber(context);
        return piccoloTruckNumber.isEmpty() || !piccoloTruckNumber.equals(getTruckNumber(context));
    }

    public static boolean piccoloTruckNumberReceived(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getBoolean(TruckNumberHandler.PICCOLO_TRUCK_NUM_RECEIVED, false);
    }

    public static void setPiccoloTruckNumberReceived(Context context, boolean val) {
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
        editor.putBoolean(TruckNumberHandler.PICCOLO_TRUCK_NUM_RECEIVED, val).apply();
        editor.commit();
    }

    public static String getHackedTruckNumber() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext());
        return String.valueOf(prefs.getInt(HACKED_TRUCK_NUM_PREF, HACKED_TRUCK_NUM_DEFAULT));
    }

    public static void incrementHackedTruckNumber() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext());
        int truckNum = prefs.getInt(HACKED_TRUCK_NUM_PREF, HACKED_TRUCK_NUM_DEFAULT);
        truckNum+=2; // increment by twos since truck number has to be odd
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(HACKED_TRUCK_NUM_PREF, truckNum);
        editor.commit();
        CommonUtility.showText("Truck # at next dock: " + truckNum, Toast.LENGTH_LONG);
    }

    /**
     * Sets the truck number.
     *
     * @param context
     * @param truckNumber The new truck number.
     */
    public static void setPiccoloTruckNumber(Context context, String truckNumber) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();


        log.debug(Logs.DEBUG, "Received Piccolo truck number: " + truckNumber);
        String previousTruckNumber = prefs.getString(PICCOLO_TRUCK_NUM_PREF, Integer.toString(PiccoloManager.PICCOLO_NEVER_DOCKED));

        editor.putString(PICCOLO_TRUCK_NUM_PREF, truckNumber);
        editor.putLong(PICCOLO_TRUCK_NUM_TIME, new Date().getTime());
        /*
        if (truckNumber.isEmpty() || Integer.parseInt(truckNumber) < 0) {
            editor.commit();
            log.debug(Logs.DEBUG, "Ignoring empty truck number from Piccolo");
            return;
        }

        editor.putString(PICCOLO_PREV_TRUCK_NUM_PREF, previousTruckNumber);

        if (!previousTruckNumber.equals(truckNumber) || isTruckNumberSourceManual(context)) {
            CommonUtility.getCurrentActivity().runOnUiThread(() -> {
                CommonUtility.showText("Truck number from GPS: " + truckNumber, Toast.LENGTH_LONG);
            }); */
            editor.putBoolean(PICCOLO_TRUCK_NUM_RECEIVED, true);
        //}
        editor.commit();

        Intent i = new Intent(ACTION_PICCOLO_TRUCK_NUM_RECEIVED);
        i.putExtra(PICCOLO_TRUCK_NUM_PREF, truckNumber);
        i.putExtra(PICCOLO_PREV_TRUCK_NUM_PREF, previousTruckNumber);
        context.sendBroadcast(i);
    }

    /**
     * Sets the truck number manually.
     *
     * @param context
     * @param truckNumber The new truck number.
     */
    public static void setTruckNumber(Context context, String truckNumber) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
        SharedPreferences.Editor editor = prefs.edit();

        editor.putString(TRUCK_NUM_PREF, truckNumber);
        editor.commit();
    }

    public static boolean isValid(String truckNumber) {
        // Cassens truck numbers are 5 numeric digits organized as follows:
        // Digits 1-2 - two digit year
        // Digit 3    - make/model
        // Digits 4-5 - individual truck ID which must be an odd number (even numbers are for trailers)

        if (HelperFuncs.isNullOrWhitespace(truckNumber) || truckNumber.length() != 5) {
            return false;
        }
        try {
            int truckNumInt = Integer.parseInt(truckNumber);
            int truckNumTwoDigitYear =  truckNumInt / 1000;
            int thisTwoDigitYear = Calendar.getInstance().get(Calendar.YEAR) % 100;
            if (truckNumTwoDigitYear > thisTwoDigitYear + 2 || (truckNumInt % 2) == 0) {
                return false;
            }
        }
        catch (NumberFormatException nfe) {
            return false;
        }

        return true;
    }
}
