package com.cassens.autotran.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.handlers.LocationHandler;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class WifiStateChangedReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(WifiStateChangedReceiver.class.getSimpleName());

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        /*
        log.debug(Logs.DEBUG, action);
        log.debug(Logs.DEBUG, "{");
        Bundle bundle = intent.getExtras();
        if (bundle != null) {
            for (String key : bundle.keySet()) {
                log.debug(Logs.DEBUG, "    " + key + " : " + (bundle.get(key) != null ? bundle.get(key) : "NULL") + "\n");
            }
        }
        log.debug(Logs.DEBUG, "}");
        log.debug(Logs.DEBUG, "============================================================");
         */

        if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION)){

            // TODO: When we upgrade to Android 10, handle deprecated NetworkInfo class
            //       NetworkInfo was deprecated in API level 29 (Android 10)
            NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
            if(info.getType() == ConnectivityManager.TYPE_WIFI){
                /*
                if (intent.hasExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE)) {
                    action += " From: " + intent.getStringExtra(WifiManager.EXTRA_PREVIOUS_WIFI_STATE);
                }

                if(intent.hasExtra(WifiManager.EXTRA_WIFI_STATE)) {
                    action += " To: " + intent.getStringExtra(WifiManager.EXTRA_WIFI_STATE);
                }

                log.debug(Logs.DEBUG, action);
                 */
                WifiManager myWifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
                WifiInfo wifiInfo = myWifiManager.getConnectionInfo();


                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                String driverID = CommonUtility.getDriverNumber(context);
                String currentSSID = prefs.getString(Constants.SSID_PREF, "");

                if(!currentSSID.equals(wifiInfo.getSSID()) && info.isConnected()) {
                    LocationHandler locationHandler = LocationHandler.getInstance(context);

                    if (!locationHandler.isReceivingUpdates()) {
                        locationHandler.startLocationTracking();
                    }
                    Location currentLocation = locationHandler.getLocation();
                    locationHandler.stopLocationTracking();

                    log.debug(Logs.DEBUG, "SSID change: BSSID :: " +  wifiInfo.getBSSID() + "SSID :: " + wifiInfo.getSSID());
                    prefs.edit().putString(Constants.SSID_PREF, wifiInfo.getSSID()).commit();

                    //SSID,"ssid",driver#,load number if in a loadtimestamp,timezone,lat,long
                    LoadEvent event = new LoadEvent();
                    SimpleTimeStamp sts = new SimpleTimeStamp();
                    String eventString = TextUtils.join(",",
                            new String[]{
                                    "SSID",
                                    driverID,
                                    "ldnbr",
                                    wifiInfo.getSSID().replace("\"", "").replace("<", "").replace(">",""),
                                    sts.getUtcDateTime(),
                                    sts.getUtcTimeZone(),
                                    currentLocation != null ? String.valueOf(currentLocation.getLatitude()) : "0.0",
                                    currentLocation != null ? String.valueOf(currentLocation.getLongitude()) : "0.0"
                            });

                    event.csv = eventString.toString();

                    DataManager.insertLoadEvent(context, event);
                    SyncManager.pushLoadEventsLatched(context);
                }
            } else {
                log.debug(Logs.DEBUG, "non-wifi: " + action);
            }

        }
    }
}

