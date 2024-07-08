package com.cassens.autotran.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.Logs;
import com.cassens.autotran.data.remote.tasks.TruckEventsRepeatingTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DeviceBootReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(AutoTranApplication.class.getSimpleName());

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent.getAction().equals("android.intent.action.BOOT_COMPLETED") ||
                intent.getAction().equals("android.intent.action.QUICKBOOT_POWERON intent")) {
            log.debug(Logs.DEBUG, "TEMP_LOC Got " + intent.getAction() + " intent");
            /* Starting TruckEventsRepeatingTask is probably not necessary here. Since we have
             * an active alarm, the system automatically starts the app on reboots. That means
             * the onCreate() method in AutoTranApplication.AppLifecycleObserver will restart
             * the repeating task from there.
             */
            TruckEventsRepeatingTask.start(context,2);
        }
    }
}
