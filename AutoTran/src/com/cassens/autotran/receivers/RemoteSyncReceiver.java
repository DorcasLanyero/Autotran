package com.cassens.autotran.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cassens.autotran.Logs;
import com.cassens.autotran.data.remote.tasks.ConsolidatedDataPullTask;
import com.cassens.autotran.data.remote.tasks.GetSupervisorUsersTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteSyncReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(RemoteSyncReceiver.class.getSimpleName());


    private static String TAG = "RemoteSyncReceiver";

    private static Context mGuiContext;

    public RemoteSyncReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {

        if(mGuiContext == null) {
            if(context != null) {
                Context applicationContext = context.getApplicationContext();
                if(applicationContext != null) {
                    mGuiContext = applicationContext;
                } else {
                    mGuiContext = context;
                }
            }
        }

        if(mGuiContext != null) {
            log.debug(Logs.DEBUG, "Getting driver data updates.");
            new ConsolidatedDataPullTask(mGuiContext, false, new ConsolidatedDataPullTask.IConsolidatedDataPullCallback() {
                @Override
                public void updateProgress(String status) {
                    //do nothing
                }

                @Override
                public void complete() {
                    //do nothing
                }
            }).execute();
            new GetSupervisorUsersTask(mGuiContext);
         } else {
            log.error(Logs.DEBUG, "The context being used to fetch shuttle moves and driver actions is null, until it is reinstantiated, the driver actions and shuttle move templates will not be pulled");
        }
    }

    public void setRepeatingTask(Context context, int seconds) {
        log.debug(Logs.DEBUG, "Setting remote sync to occur every " + seconds + " seconds.");
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        Intent i = new Intent(context, RemoteSyncReceiver.class);
        PendingIntent pi = PendingIntent.getBroadcast(context, 0, i, PendingIntent.FLAG_IMMUTABLE);
        am.setRepeating(AlarmManager.RTC, System.currentTimeMillis(), 1000 * seconds, pi); // Millisec * Second
        mGuiContext = context;
    }

    public void cancelRepeatingTask(Context context) {

        log.debug(Logs.DEBUG, "canceling repeating task");

        Intent intent = new Intent(context, RemoteSyncReceiver.class);
        PendingIntent sender = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_IMMUTABLE);
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(sender);
    }
}
