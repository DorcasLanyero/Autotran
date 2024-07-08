package com.cassens.autotran.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cassens.autotran.Logs;
import com.cassens.autotran.data.remote.tasks.ProcessDriverActionQueueTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DriverActionQueueReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(DriverActionQueueReceiver.class.getSimpleName());


    private static String TAG = "RemoteSyncReceiver";

    public DriverActionQueueReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        log.debug(Logs.DEBUG, "Processing the pending driver actions");
        new ProcessDriverActionQueueTask(context).execute();
    }

    public void setRepeatingTask(Context context, int seconds) {

        cancelRepeatingTask(context);

        log.debug(Logs.DEBUG, "Setting remote sync to occur every " + seconds + " seconds.");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, DriverActionQueueReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 1000 * seconds, pi); // Millisec * Second
    }

    public void cancelRepeatingTask(Context context) {

        log.debug(Logs.DEBUG, "canceling repeating task");

        Intent intent = new Intent(context, DriverActionQueueReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
