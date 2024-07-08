package com.sdgsystems.app_config;
import android.content.Context;
import android.content.SharedPreferences;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.sdgsystems.util.HelperFuncs;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class AppConfig {
    private static final Logger log = LoggerFactory.getLogger(AppConfig.class.getSimpleName());
    private static final String APP_CONFIG_GROUP_SETTINGS = "AppConfigGroupSettings";

    private static SharedPreferences sAppConfigGroupPrefs = null;

    public static void init(Context appContext) {
        AppSetting.init(appContext);

        sAppConfigGroupPrefs = appContext.getSharedPreferences(APP_CONFIG_GROUP_SETTINGS, Context.MODE_PRIVATE);
        Map<String, ?> sharedPrefsMap = sAppConfigGroupPrefs.getAll();
        updateSettingsOverrides(appContext, sharedPrefsMap);
    }

    public static void updateSettingsOverrides(Context appContext, Map<String, ?> newSettingsMap) {
        if (newSettingsMap.size() == 0) {
            log.debug(Logs.DEBUG, "updateSettingsOverrides(): No updates received from server");
            return;
        }
        SharedPreferences.Editor prefEditor =  sAppConfigGroupPrefs.edit().clear();
        prefEditor.clear().commit();
        for (Map.Entry<String, ?> newSetting : newSettingsMap.entrySet()) {
            String name = standardizeSettingName(newSetting.getKey());
            if (name.isEmpty()) {
                log.debug(Logs.DEBUG, "updateSettingsOverrides(): Skipping mal-formed config setting: " + newSetting.getKey());
                continue;
            }
            prefEditor.putString(name, newSetting.getValue().toString());
        }
        prefEditor.commit();
        applySettingsOverrides(appContext);
    }

    public static String standardizeSettingName(String name) {
        if (HelperFuncs.noNull(name).isEmpty()) {
            return "";
        }
        String formattedName = StringUtils.substringBefore(name, ConfigHashMap.GROUP_DELIM).trim();
        if (formattedName.isEmpty()) {
            return "";
        }
        String secondPart = StringUtils.substringAfter(name, ConfigHashMap.GROUP_DELIM).trim();
        if (!secondPart.isEmpty()) {
            formattedName += ConfigHashMap.GROUP_DELIM + secondPart;
        }
        return formattedName;
    }

    private static void logSettings(String header) {
        log.debug(Logs.DEBUG, "logSettings()");
        log.debug(Logs.DEBUG, String.format("logSettings(): ##### %s #####", header));
        log.debug(Logs.DEBUG, "======= logSettings(): AppSettings");
        AppSetting[] appSettings = AppSetting.values();
        for (int i=0; i < appSettings.length; i++) {
            log.debug(Logs.DEBUG, String.format("logSettings(): %s='%s'", appSettings[i].name(), appSettings[i].getAsString()));
        }
        log.debug(Logs.DEBUG, "======= logSettings(): END AppSettings");
        log.debug(Logs.DEBUG, "======= logSettings(): SharedPrefs");
        Map<String, ?> sharedPrefsMap = sAppConfigGroupPrefs.getAll();
        for (Map.Entry<String, ?> pref : sharedPrefsMap.entrySet()) {
            log.debug(Logs.DEBUG, String.format("logSettings(): %s='%s'", pref.getKey(), pref.getValue()));
        }
        log.debug(Logs.DEBUG, "======= logSettings(): END sharedPrefs");
    }

    public static void applySettingsOverrides(Context appContext) {
        String driverNum = CommonUtility.getMostRecentLoginDriverNumber(appContext);
        int terminal = CommonUtility.getMostRecentLoginDriverHelpTerm(appContext);

        Map<String, ?> sharedPrefsMap = sAppConfigGroupPrefs.getAll();
        ConfigHashMap configOverridesMap = ConfigHashMap.createFromPreferences(sharedPrefsMap);

        HashMap<String, String>  newSettings = configOverridesMap.getSettingsForDriver(driverNum, terminal);

        AppSetting.applyNewSettings(newSettings);
        AppSetting.logSettings("AppSettings after applySettingsOverrides()");
    }

    public static boolean isDriverInList(String driverList, String driverNum) {
        String[] drivers = driverList.split(",");
        for (int i = 0; i < drivers.length; i++) {
            if (drivers[i].trim().equalsIgnoreCase(driverNum.trim())) {
                return true;
            }
        }
        return false;
    }
}
