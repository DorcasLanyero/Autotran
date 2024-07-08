package com.sdgsystems.app_config;


import com.cassens.autotran.Logs;
import com.sdgsystems.util.HelperFuncs;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ConfigHashMap {
    private static final Logger log = LoggerFactory.getLogger(ConfigHashMap.class.getSimpleName());

    static final String GROUP_DELIM = ":";
    private static final String GROUP_KEYWORD = "GROUP";
    private static final String GLOBAL_GROUP = "GLOBAL";

    HashMap<String, ConfigGroup> map;

    public ConfigHashMap() {
        map = new HashMap<String, ConfigGroup>();
        try {
            map.put(GLOBAL_GROUP, new ConfigGroup(GLOBAL_GROUP));
        }
        catch (Exception e) {
            log.debug(Logs.DEBUG, "Unable to add group GLOBAL_GROUP");
            log.debug(Logs.DEBUG, "Caught exception: " + e.toString());
        }
    }

    public ConfigGroup putConfigGroup(String groupName, String members) {
        ConfigGroup group = new ConfigGroup(groupName, members);
        return map.put(groupName, group);
    }

    public ConfigSetting putConfigSetting(String groupName, String name, String value) {
        return map.get(groupName).settings.put(name, ConfigSetting.createFromSharedPref(name, value));
    }

    public ConfigGroup getConfigGroup(String groupName) {
        return map.get(groupName);
    }

    public ConfigSetting getConfigSetting(String groupName, String settingName) {
        return map.get(groupName).settings.get(settingName);
    }

    public static ConfigHashMap createFromPreferences(Map<String, ?> prefsMap) {
        ConfigHashMap map = new ConfigHashMap();
        // Create all the groups first
        for (Map.Entry<String, ?> pref : prefsMap.entrySet()) {
            if (ConfigGroup.isGroupSettingPref(pref.getKey())) {
                ConfigGroup group = ConfigGroup.createFromSharedPref(pref.getKey(), pref.getValue().toString());
                try {
                    map.putConfigGroup(group.name, pref.getValue().toString());
                }
                catch (Exception e) {
                    log.debug(Logs.DEBUG, "Unable to add group " + pref.getKey());
                    log.debug(Logs.DEBUG, "Caught exception: " + e.toString());
                }
            }
        }
        for (Map.Entry<String, ?> pref : prefsMap.entrySet()) {
            if (ConfigSetting.isConfigSettingPref(pref.getKey())) {
                ConfigSetting setting = ConfigSetting.createFromSharedPref(pref.getKey(), pref.getValue().toString());
                try {
                    map.putConfigSetting(setting.group, setting.name, setting.value);
                }
                catch (Exception e) {
                    log.debug(Logs.DEBUG, "createFromPreferences(): Group not found");
                }
            }
        }
        return map;
    }

    public static class ConfigGroup {
        String name;
        ArrayList<String> members = new ArrayList<String>();
        HashMap<String, ConfigSetting> settings = new HashMap<String, ConfigSetting>();

        private ConfigGroup() {}

        ConfigGroup(String name) {
            this.name = name;
        }

        ConfigGroup(String name, String members) {
            this.name = name;
            for (String member : members.split(",")) {
                this.members.add(member.trim().toUpperCase());
            }
        }

        public ConfigSetting putConfigSetting(String name, String value) {
            ConfigSetting setting = new ConfigSetting(this.name, name, value);
            return this.settings.put(setting.generateKey(), setting);
        }

        public static ConfigGroup createFromSharedPref(String prefName, String prefValue) {
            if (isGroupSettingPref(prefName)) {
                return new ConfigGroup(StringUtils.substringAfter(prefName, GROUP_DELIM).trim(), prefValue);
            }
            return null;
        }

        public static boolean isGroupSettingPref(String prefName) {
            return prefName.contains(GROUP_DELIM)
                    && StringUtils.substringBefore(prefName, GROUP_DELIM)
                    .trim()
                    .equalsIgnoreCase(GROUP_KEYWORD);
        }
    }

    public static class ConfigSetting {
        String group;
        String name;
        String value;

        String generateKey() {
            return this.group + GROUP_DELIM + this.name;
        }

        ConfigSetting() {};

        ConfigSetting(String group, String name, String value) {
            this.group = group;
            this.name = name;
            this.value = value;
        }

        static ConfigSetting createFromSharedPref(String prefName, String prefValue) {
            ConfigSetting setting = new ConfigSetting();

            if (prefName.contains(GROUP_DELIM)) {
                String[] fields = prefName.split(GROUP_DELIM);
                if (fields.length < 2 || fields[0].isEmpty() || fields[1].isEmpty()) {
                    log.debug(Logs.DEBUG, "ConfigSetting: Invalid shared preference: " + prefName);
                    return null;
                }
                setting.group = fields[0].trim();
                if (setting.group.equalsIgnoreCase(GROUP_KEYWORD)) {
                    log.debug(Logs.DEBUG, "ConfigSetting: " + prefName + " is a group declaration");
                    return null;
                }
                String afterFirstColon = fields[1];
                for (int i = 2; i < fields.length; i++) {
                    afterFirstColon += GROUP_DELIM + fields[i];
                }
                setting.name = afterFirstColon.trim();
            } else {
                setting.group = GLOBAL_GROUP;
                setting.name = prefName.trim();
            }
            setting.value = prefValue;
            return setting;
        }

        public static boolean isConfigSettingPref(String prefName) {
            return !ConfigGroup.isGroupSettingPref(prefName);
        }
    }

    public HashMap<String, String> getSettingsForDriver(String driver, int terminal) {
        HashMap<String, String> newSettingsMap = new HashMap<String, String>();
        ConfigGroup globalGroup = map.get(GLOBAL_GROUP);
        for (Map.Entry<String, ConfigHashMap.ConfigSetting> setting : globalGroup.settings.entrySet()) {
            //String.format("Putting GLOBAL %s='%s' in newSettingsMap\n", setting.getKey(), setting.getValue().value);
            newSettingsMap.put(setting.getKey(), setting.getValue().value);
        }

        // Now get the group-specific overrides for this driver. These will take precedence
        // over the global overrides.
        if (!HelperFuncs.isNullOrWhitespace(driver)) {
            String terminalEntry = String.format("T%d", terminal);
            for (Map.Entry<String, ConfigHashMap.ConfigGroup> group : map.entrySet()) {
                if (group.getValue().members.contains(driver) || group.getValue().members.contains(terminalEntry)) {
                    for (Map.Entry<String, ConfigHashMap.ConfigSetting> setting : group.getValue().settings.entrySet()) {
                        //String.format("Putting %s %s='%s' in newSettingsMap\n", group.getValue().name, setting.getKey(), setting.getValue().value);
                        newSettingsMap.put(setting.getKey(), setting.getValue().value);
                    }
                }
            }
        }
        return newSettingsMap;
    }

    @Override
    public String toString() {
        String s = "";
        for (Map.Entry<String, ConfigGroup> group : map.entrySet()) {
            s += String.format("Group: %s numMembers=%d numSettings=%d\n", group.getValue().name,
                        group.getValue().members.size(), group.getValue().settings.size());
            for (Map.Entry<String, ConfigSetting> setting : group.getValue().settings.entrySet()) {
                s += String.format("%s: %s='%s' key='%s'\n", setting.getValue().group, setting.getValue().name,
                        setting.getValue().value, setting.getKey());
            }
        }
        return s;
    }
}

