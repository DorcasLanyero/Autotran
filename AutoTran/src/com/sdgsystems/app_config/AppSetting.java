package com.sdgsystems.app_config;

import android.content.Context;
import android.content.SharedPreferences;

import com.cassens.autotran.Logs;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// AppSetting manages app-wide, persistent configuration settings. Features include:
// - Settings values are preserved across app restarts and system reboots using a dedicated
//   shared preference file.
// - The names, data types, and type-checking are defined and implemented in one place via enum,
//   which simplifies access and modification of the settings in the rest of the app.
// - When config settings are removed from the app, old shared preference associated with the
//   prior settings are removed automatically from the system on first startup of the new version
//   of the app.\
//
//   Groups of drivers can be specified and given custom settings that override the
//   default settings. A group is specified in the settings file by providing the
//   word "Group" followed by a colon and the group name (e.g. "Group: PiccoloGPSPilot")
//   with the value set to a comma-separated list of drver numbers in the group.
//   The group settings are then specified via group name followed by a colon and
//   the setting name (e.g., "PiccoloGPSPilot: TRUCK_EVENTS_DISTANCE_THRESHOLD") with
//   the override value of the setting in the value.
public enum AppSetting {
    AUTO_DELIVERY_LOAD_TYPES("DR,F1,TX,EX,UX,WX,ZX,YX,DX,HX,IX,JX,OX,PX,HZ,IZ"),
    DAILY_TASK_INACTIVE_THRESHOLD((float)10.0),           // 10 minutes
    DAILY_TASK_INTERVAL_HOURS(24),              // 24 except for debug purposes
    DEALER_UPDATE_NOTIFY_DAYS(30),
    DEALER_UPDATE_TEST_MINUTES(5),              // time in minutes
    DEALER_UPDATE_TEST_MODE(false),
    DISPLAYED_LOADS_MAX(10),
    DRIVING_LOCK_MAX_QUICK_OVERRIDES(1),
    DRIVING_LOCK_MPH_THRESHOLD((float)7.0),
    DRIVING_LOCK_OVERRIDE_TIMEOUT(5 * 60),      // time in seconds
    DRIVING_LOCK_QUICK_OVERRIDE_TIMEOUT(20),    // time in seconds
    DRIVING_LOCK_STOP_GPS_TIMEOUT(5 * 60),      // time in seconds
    ENABLE_PER_UNIT_LOT_LOCATE(false),
    FEATURE_EXPANDED_DORTS_DIALOG(false),
    SEND_HIRES_ON_SUPPLEMENTALS(true),
    LICENSE_LOCK_DAYS(5),
    LICENSE_WARNING_DAYS(7),
    LOAD_AUTO_HIDE_DAYS(28),
    LOAD_RETENTION_DAYS(365),
    POC_ECHO_TO_LAMBDA(false),
    POC_ENDPOINTS_TO_ECHO("save_load_full, save_delivery_full"),
    POC_LOG_PRETTY_PRINT(true),
    POC_LOG_ESCAPE_SPECIAL_CHARS(false),
    /***
     * Note on POC_SEND_LOAD_CHANGES:
     * When POC_LAMBDA_TEST is true and the app receives a new load on a dispatch, it will issue
     * an upload_load() lambda request to upload the same load to the new server.  If this config
     * setting is true, it will also send a new upload_load() lamba request when it receives load
     * changes on dispatch. Because the implementation of upload_load from the tablet is a
     * hack, we have a potential issue if we try to do this: When the tablet receives a
     * load change from the dispatch, it immediately merges its data into the new load structure
     * and sends that reconciled load back to the server via an update_load(). However, under
     * normal operations, the change has already been applied on the server by the time the
     * tablet receives it.  Due to the temporary hack we're using for the PoC, that won't be
     * the case, which introduces a race condition. Namely, since load updates are echoed to
     * lambda AND lambda doesn't guarantee that requests will be executed in the order they were
     * sent, it's possible that the update_load request might be executed by the server prior to
     * the upload_load request that provides the load changes.  By turning off the upload_load()
     * in this case, we rely on the update_load() to send the correct data to new server.
     *
     * In other words, this attribute is there just in case we want to turn it on for some
     * reason for debugging purposes (for example of th update_load() starts failing for some
     * reason in this case.
     */
    POC_SEND_LOAD_CHANGES(false),       // TODO: TEMPORARY HACK. Remove before end of PoC
    ODOMETER_PIC_NOT_REQUIRED_MFGS(""),
    ODOMETER_PIC_REQUIRED_TERMINALS(""),
    PICCOLO_PERMISSION_LOCKOUT_MINUTES((long) 4),
    PRELOAD_AUDIT_TIMEOUT((long)30),                      // time in seconds
    PRUNE_LOADS_DAILY(true),                    // TEMPORARY FEATURE FLAG
    PRUNE_LOADS_ENABLED(false),
    REMOTE_SYNC_INTERVAL(5),                    // time in minutes
    REPORT_TABLET_STATUS_DAILY(false),
    S3_UPLOAD_MAX_LOCK_SECONDS(60),
    S3_UPLOAD_MAX_RETRIES(5),
    S3_UPLOAD_TIMEOUT_IMMEDIATE_RETRY(false),
    SHUTTLE_DELIVERY_TIMEOUT(10),               // time in minutes
    SHUTTLE_DO_NOT_SHIP_LIST(""),
    SHUTTLE_DO_NOT_SHIP_MIN_VIN_LEN(7),
    SHUTTLE_REPEAT_SHIP_WARNING_HRS(24),        // time in hours
    TRUCK_EVENTS_DISTANCE_THRESHOLD(200),       // distance in feet
    TRUCK_EVENTS_GPS_AGE_THRESHOLD(3600),       // time in seconds
    TRUCK_EVENTS_INTERVAL(5),                   // time in minutes
    GPS_MAX_AGE_MINUTES(100),
    //TEMP_INT_SETTING(1),
    //TEMP_FLOAT_SETTING((float)1.0),
    //TEMP_LONG_SETTING((long)1),
    //TEMP_BOOLEAN_SETTING((boolean)true),
    //TEMP_STRING_SETTING("test value"),
    TRUCK_EVENTS_SEND_TRUCKLOC2(false),
    TRUCK_EVENTS_UNDOCK_TIMEOUT(15),            // time in minutes
    WM_HELPER_FINISHED_WORK_MAX(100);

    private static final Logger log = LoggerFactory.getLogger(AppSetting.class.getSimpleName());
    private static final String APP_CONFIG_SETTINGS = "AppConfigSettings";

    private static SharedPreferences sAppConfigPrefs = null;

    Object initialValue;

    static void init(Context appContext) {
        if (sAppConfigPrefs == null) {
            sAppConfigPrefs = appContext.getSharedPreferences(APP_CONFIG_SETTINGS, Context.MODE_PRIVATE);

            // Get list of existing shared preferences. As we go through the list of enum values,
            // to initialize things, delete the shared preference from this list. If any remain
            // at the end, it means they are no longer defined in AppSetting settings, so we delete
            // them. This ensures that no shared preferences get left over from obsolete settings.
            Map<String, ?> sharedPrefsMap = sAppConfigPrefs.getAll();
            AppSetting[] settings = values();
            for (int i=0; i < settings.length; i++) {
                // If sharedPreference for the setting doesn't exist
                if (sharedPrefsMap.containsKey(settings[i].name())) {
                    // Shared preference already exists, so remove it from the list, so that
                    // we don't delete it at the end.
                    sharedPrefsMap.remove(settings[i].name());
                }
                else {
                    // This will create the shared preference value and initialize it to
                    // the default value.
                    log.debug(Logs.DEBUG, "init(): Creating shared preference for new config setting " + settings[i].name());
                    settings[i].setValue(settings[i].initialValue);
                }
            }
            for (Map.Entry<String, ?> prefEntry : sharedPrefsMap.entrySet()) {
                log.debug(Logs.DEBUG, "init(): Removing shared preference for obsolete config setting: " + prefEntry.getKey());
                sAppConfigPrefs.edit().remove(prefEntry.getKey()).commit();
            }
            AppSetting.logSettings("AppSettings after init()");
        }
    }

    private static void resetToDefaults() {
        AppSetting[] settings = values();
        for (int i=0; i < settings.length; i++) {
            settings[i].resetValue();
        }
    }

    public static void applyNewSettings(HashMap<String, String> newSettings) {
        if (sAppConfigPrefs == null) {
            log.debug(Logs.DEBUG, "JUNK_ERR: Attempt to apply settings before init()");
            return;
        }
        AppSetting[] settings = values();
        for (int i=0; i < settings.length; i++) {
            if (newSettings.containsKey(settings[i].name())) {
                log.debug(Logs.DEBUG, String.format("JUNK_TRACE: Overriding %s to %s", settings[i].name(), newSettings.get(settings[i].name())));
                String junk = settings[i].name();
                String junk2 = newSettings.get(junk);
                settings[i].setValue(junk2);
            }
            else {
                settings[i].resetValue();
                log.debug(Logs.DEBUG, String.format("JUNK_TRACE: Resetting %s to %s", settings[i].name(), settings[i].getAsString()));
            }
        }
    }

    public static void logSettings(String header) {
        if (!HelperFuncs.isNullOrEmpty(header)) {
            log.debug(Logs.DEBUG, "logSettings():");
            log.debug(Logs.DEBUG, "logSettings(): =========================");
            log.debug(Logs.DEBUG, "logSettings(): " + header);
            log.debug(Logs.DEBUG, "logSettings(): =========================");
        }
        AppSetting[] settings = values();
        for (int i=0; i < settings.length; i++) {
            String overridden = "";
            if (!settings[i].getAsString().equalsIgnoreCase(settings[i].initialValue.toString())) {
                overridden = String.format("<=========== Overrides default of %s", settings[i].initialValue.toString());
            }
            log.debug(Logs.DEBUG, String.format("logSettings(): %s=%s %s", settings[i].name(), settings[i].getAsString(), overridden));
        }
    }

    private static boolean isDefinedSetting(String settingName) {
        try {
            AppSetting setting = valueOf(settingName);
        } catch (IllegalArgumentException ex) {
            return false;
        }
        return true;
    }

    public void set(String stringValue) {
        setValue(stringValue);
    }

    public void set(int intValue) {
        setValue(new Integer(intValue));
    }

    public void set(long longValue) {
        setValue(new Long(longValue));
    }

    public void set(boolean booleanValue) {
        setValue(new Boolean(booleanValue));
    }

    public void set(float floatValue) {
        setValue(new Float(floatValue));
    }

    public void setValue(Object value) {
        if (getType() != value.getClass()) {
            if (value.getClass() == String.class) {
                String s = (String)value;
                try {
                    if (getType() == String.class) {
                        value = s;
                    }
                    else if (getType() == Integer.class) {
                        value = Integer.valueOf(s);
                    } else if (getType() == Boolean.class) {
                        value = Boolean.valueOf(s);
                    } else if (getType() == Long.class) {
                        value = Long.valueOf(s);
                    } else if (getType() == Float.class) {
                        value = Float.valueOf(s);
                    } else {
                        throw new IllegalArgumentException();
                    }
                } catch (NumberFormatException nfe) {
                    throw new IllegalArgumentException();
                }
            }
            else {
                throw new IllegalArgumentException();
            }
        }
        if (sAppConfigPrefs == null) {
            this.initialValue = value;
        }
        else {
            SharedPreferences.Editor editor = sAppConfigPrefs.edit();
            if (getType() == Integer.class) {
                editor.putInt(this.name(), ((Integer)value).intValue());
            }
            else if (getType() == Boolean.class) {
                editor.putBoolean(this.name(), ((Boolean)value).booleanValue());
            }
            else if (getType() == Long.class) {
                editor.putLong(this.name(), ((Long)value).longValue());
            }
            else if (getType() == Float.class) {
                editor.putFloat(this.name(), ((Float)value).floatValue());
            }
            else if (getType() == String.class) {
                editor.putString(this.name(), (String)value);
            }
            else {
                editor.putString(this.name(), value.toString());
            }
            editor.commit();
        }
    }

    public void resetValue() {
        setValue(this.initialValue);
    }

    public int getInt() {
        int intValue = ((Integer)this.initialValue).intValue();
        if (getType() != Integer.class) {
            throw new IllegalArgumentException();
        }
        else if (sAppConfigPrefs == null) {
            return intValue;
        }
        else {
            return sAppConfigPrefs.getInt(this.name(), intValue);
        }
    }

    public boolean getBoolean() {
        boolean booleanValue = ((Boolean)this.initialValue).booleanValue();
        if (getType() != Boolean.class) {
            throw new IllegalArgumentException();
        }
        else if (sAppConfigPrefs == null) {
            return booleanValue;
        }
        else {
            return sAppConfigPrefs.getBoolean(this.name(), booleanValue);
        }
    }

    public long getLong() {
        long longValue = ((Long)this.initialValue).longValue();
        if (getType() != Long.class) {
            throw new IllegalArgumentException();
        }
        else if (sAppConfigPrefs == null) {
            return longValue;
        }
        else {
            return sAppConfigPrefs.getLong(this.name(), longValue);
        }
    }

    public float getFloat() {
        float floatValue = ((Float)this.initialValue).floatValue();
        if (getType() != Float.class) {
            throw new IllegalArgumentException();
        }
        else if (sAppConfigPrefs == null) {
            return floatValue;
        }
        else {
            return sAppConfigPrefs.getFloat(this.name(), floatValue);
        }
    }

    public String getString() {
        String stringValue = (String)this.initialValue;
        if (getType() != String.class) {
            throw new IllegalArgumentException();
        }
        else if (sAppConfigPrefs == null) {
            return stringValue;
        }
        else {
            return sAppConfigPrefs.getString(this.name(), stringValue);
        }
    }

    public Type getType() {
        // Note: The type is established based on the data type provided in the
        //       enum initializer.
        return initialValue.getClass();
    }

    public String getAsString() {
        if (getType() == String.class) {
            return getString();
        }
        else if (getType() == Integer.class) {
            return Integer.toString(getInt());
        }
        else if (getType() == Boolean.class) {
            return Boolean.toString(getBoolean());
        }
        else if (getType() == Long.class) {
            return Long.toString(getLong());
        }
        else if (getType() == Float.class) {
            return Float.toString(getFloat());
        }
        return "";
    }

    HashSet stringHashList;
    String lastDelimiter = ",";

    public Set getAsHashSet(String delimiter, boolean trim) {
        if (this.stringHashList != null && this.lastDelimiter.equals(delimiter)) {
            return stringHashList;
        }
        String[] values = getAsString().split(delimiter);
        stringHashList = new HashSet<>();
        for (String value : values) {
            stringHashList.add(trim ? value.trim() : value);
        }
        return stringHashList;
    }

    public Set getAsTrimmedCsvSet() {
        return getAsHashSet(",", true);
    }

    private AppSetting(int initialValue) {
        this.initialValue = Integer.valueOf(initialValue);
    }

    private AppSetting(boolean initialValue) {
        this.initialValue = Boolean.valueOf(initialValue);
    }

    private AppSetting(long initialValue) {
        this.initialValue = Long.valueOf(initialValue);
    }

    private AppSetting(float initialValue) {
        this.initialValue = Float.valueOf(initialValue);
    }

    private AppSetting(String initialValue) {
        this.initialValue = initialValue;
    }
}
