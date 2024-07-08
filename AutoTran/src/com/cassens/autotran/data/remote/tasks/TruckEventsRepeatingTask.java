package com.cassens.autotran.data.remote.tasks;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

import com.cassens.autotran.Logs;
import com.sdgsystems.app_config.AppSetting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TruckEventsRepeatingTask {
    private static final Logger log = LoggerFactory.getLogger(TruckEventsRepeatingTask.class.getSimpleName());
    private static final int PENDING_INTENT_REQ_CODE = 100;
    private static final Object LOCK = new Object();

    private static boolean sIsStarted = false;
    private static AlarmManager sAlarmManager;
    private static Handler sHandler = new Handler(Looper.getMainLooper());
    private static Intent sIntent;
    private static PendingIntent sPendingIntent;

    public static void start(Context context, int initialDelay) {
        synchronized (LOCK) {
            if (sIsStarted) {
                log.debug(Logs.DEBUG, "TEMP_LOC: Repeating alarm not started: Already running");
                return;
            }
            log.debug(Logs.DEBUG, "TEMP_LOC: Starting TruckEventsRepeatingTask" + uiThreadMsg());
            if (sPendingIntent != null) {
                log.debug(Logs.DEBUG, "TEMP_LOC: Cancelling current alarm" + uiThreadMsg());
                sAlarmManager.cancel(sPendingIntent);
            }
            sAlarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
            sIntent = new Intent(context, TruckEventsAlarmReceiver.class);
            sPendingIntent = PendingIntent.getBroadcast(
                    context, PENDING_INTENT_REQ_CODE, sIntent,
                    PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            scheduleTask(initialDelay);
            sIsStarted = true;
        }
    }

    public synchronized static void stop() {
        synchronized (LOCK) {
            if (sIsStarted) {
                log.debug(Logs.DEBUG, "TEMP_LOC: Cancelling repeating alarm" + uiThreadMsg());
                sAlarmManager.cancel(sPendingIntent);
                sIsStarted = false;
            }
        }
    }

    private static void scheduleTask(int delayMinutes) {
        // Schedule the task with AlarmManager
        //log.debug(Logs.DEBUG, "JUNK: Scheduling alarm" + uiThreadMsg());

        // Note: We don't use setRepeatingAlarm() here because Android stopped supporting
        // precise delivery times in API 19. Instead we schedule the task the first time,
        // then each successive task schedules the next one..
        sAlarmManager.setExact(
                AlarmManager.RTC_WAKEUP,
                System.currentTimeMillis() + (long)delayMinutes * 60 * 1000,
                sPendingIntent
        );
    }

    private static String uiThreadMsg() {
        return " - " + ((Looper.myLooper() == Looper.getMainLooper()) ? "" : "Non-UI thread") + "UI thread";
    }

    private static void task(Context context) {
        log.debug(Logs.DEBUG, "TEMP_LOC: Running SendTruckEventsTask" + uiThreadMsg());
        new SendTruckEventsTask().run(context);
    }

    public static class TruckEventsAlarmReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            // Schedule the next alarm in the receiver BEFORE we start the background task so that
            // we keep the alarm interval as consistent as we can.
            sHandler.post(new Runnable() {
                @Override
                public void run() {
                    scheduleTask(AppSetting.TRUCK_EVENTS_INTERVAL.getInt());
                }
            });

            log.debug(Logs.DEBUG, "TEMP_LOC: Got alarm broadcast" + uiThreadMsg());
            // Do the background work here
            new Thread(new Runnable() {
                @Override
                public void run() {
                    task(context);
                }
            }).start();
        }
    }
}
