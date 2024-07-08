package com.cassens.autotran;

import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cassens.autotran.hardware.PiccoloManager;
import com.sdgsystems.util.HelperFuncs;

// Convenience class for managing and accessing variables that store
// information about the global state of the app. Variables that need
// to persist across executions are stored in shared preferences.
public class GlobalState {
    private static final String PREF_LAST_STARTED_LOAD_NUM = "PREF_LAST_STARTED_LOAD_NUM";
    private static final String PREF_LAST_STARTED_LOAD_ASSIGNED_TRUCK = "PREF_LAST_STARTED_LOAD_ASSIGNED_TRUCK";

    private static String lastStartedLoadNumPref() {
        return PREF_LAST_STARTED_LOAD_NUM + CommonUtility.getDriverNumber(AutoTranApplication.getAppContext());
    }
    private static String lastStartedLoadAssignedTruckPref() {
        return PREF_LAST_STARTED_LOAD_ASSIGNED_TRUCK + CommonUtility.getDriverNumber(AutoTranApplication.getAppContext());
    }

    public static String getLastStartedLoadNum() {
        return PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext())
        .getString(lastStartedLoadNumPref(), "");
    }

    public static void setLastStartedLoadNum(String loadNum) {
        if (!loadNum.equalsIgnoreCase(getLastStartedLoadNum())) {
            PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext())
                    .edit()
                    .putString(lastStartedLoadNumPref(), HelperFuncs.noNull(loadNum))
                    .apply();
            PiccoloManager.setCustomVariables();
        }
    }

    public static String getLastStartedLoadAssignedTruck() {
        return PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext())
                .getString(lastStartedLoadAssignedTruckPref(), "");
    }

    public static void setLastStartedLoadAssignedTruck(String truckNum) {
        PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext())
                .edit()
                .putString(lastStartedLoadAssignedTruckPref(), HelperFuncs.noNull(truckNum))
                .apply();
    }

    public static void setLastStartedLoadInfo(String loadNumber, String truckNumber) {
        setLastStartedLoadNum(loadNumber);
        setLastStartedLoadAssignedTruck(truckNumber);
    }

    public static void clearLastStartedLoadInfo() {
        setLastStartedLoadInfo("", "");
    }
}
