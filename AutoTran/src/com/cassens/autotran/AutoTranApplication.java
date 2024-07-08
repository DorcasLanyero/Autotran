package com.cassens.autotran;

import android.app.Application;

import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.os.Handler;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.WindowManager;

import androidx.lifecycle.ProcessLifecycleOwner;

import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.EventBusManager;
import com.cassens.autotran.data.remote.tasks.TruckEventsRepeatingTask;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.cassens.autotran.scanning.GenericScanManager;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.sdgsystems.app_config.AppConfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by john on 10/31/17.
 */

public class AutoTranApplication extends Application {
    private static final Logger log = LoggerFactory.getLogger(AutoTranApplication.class.getSimpleName());
    private static Context context;
    private static AutoTranApplication _application;
    private static long lastInteractionTime = 0;

    @Override
    public void onCreate() {

        if (BuildConfig.DEBUG && BuildConfig.AUTOTRAN_STRICT_MODE) {
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    //.detectLeakedClosableObjects() // Turn off for now
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }

        super.onCreate();

        context = getApplicationContext();

        _application = this;

        initUncaughtExceptionHandler();

        initCrashlyticsVariables();

        AppConfig.init(context);

        // Set up process lifecycle owner
        ProcessLifecycleOwner.get().getLifecycle().addObserver(new AppLifecycleObserver());
    }
    
    private void initCrashlyticsVariables() {
        FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
        crashlytics.setUserId(CommonUtility.getDriverNumber(AutoTranApplication.getAppContext()));
        crashlytics.setCustomKey("TruckNumber", TruckNumberHandler.getTruckNumber(AutoTranApplication.getAppContext()));
    }

    public static void initAutoTran() {
        _application.initApplication();
    }

    private void initApplication() {
        // prime the event bus
        EventBusManager.getInstance();

        //reset any photo uploads in progress
        DataManager.resetPhotoUploadProgress(this);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String previousVersion = prefs.getString("PREF_CURRENT_VERSION_CODE", "-1");
        String previousVersionName = prefs.getString("PREF_CURRENT_VERSION_NAME", "");
        String currentVersion = "-1";
        String currentVersionName = "";

        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(getPackageName(), 0);
            currentVersion = String.valueOf(pInfo.versionCode);
            currentVersionName = pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        if (!currentVersion.equalsIgnoreCase("") && !currentVersion.equalsIgnoreCase(previousVersion)) {
            //Handle an upgrade possibility
            log.debug(Logs.UPGRADES, "The version strings don't match: " + currentVersion + " (" + currentVersionName + ")" +
                    " vs " + previousVersion + " (" + previousVersionName +")" + " testing for upgrade");

            handleUpgrade(currentVersion, previousVersion);

            prefs.edit().putString("PREF_CURRENT_VERSION_CODE", currentVersion).commit();
            prefs.edit().putString("PREF_CURRENT_VERSION_NAME", currentVersionName).commit();

        }
    }

    private void handleUpgrade(String currentVersion, String previousVersion) {
        int previousVersionCode = Integer.parseInt(previousVersion);
        int currentVersionCode = Integer.parseInt(currentVersion);

        if(currentVersionCode > previousVersionCode) {
            log.debug(Logs.UPGRADES, "upgrading!");

            //incremented the version code for this inspection (since we need to make sure that we go back through if there was an error...)
            if(previousVersionCode < 2019020701 && currentVersionCode >= 2019020701) {
                log.debug(Logs.UPGRADES, "Upgrading from a point before the shuttleload image load_id fix");

                AutoTranUpgrades.fix_shuttle_load_id_propogation_error(getAppContext());
            }
        }
    }

    public static Context getAppContext() {
        return context;
    }

    public static void forceDelayedException() {
        (new Handler()).postDelayed(new Runnable(){
            public void run(){
                log.debug(Logs.DEBUG, "Forcing an exception");
                throw new RuntimeException("Forced exception to test Firebase Crashlytics");
            }
        }, 5000);
    }

/*
    private void initUncaughtExceptionHandler() {

        // Important: We must delay 5 seconds to allow Firebase Crashlytics to set the
        // defaultUncaughtExceptionHandler before we set it. Then we must save the old
        // default and call it before ours.
        final ScheduledThreadPoolExecutor c = new ScheduledThreadPoolExecutor(1);
        c.schedule(new Runnable() {
            @Override
            public void run() {
                final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
                Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
                    @Override
                    public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                        handleUncaughtException(paramThrowable);
                        defaultHandler.uncaughtException(paramThread, paramThrowable);
                    }
                });
            }
        }, 5, TimeUnit.SECONDS);
    }
*/

    private void initUncaughtExceptionHandler() {

        final Thread.UncaughtExceptionHandler defaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler(new Thread.UncaughtExceptionHandler() {
            @Override
            public void uncaughtException(Thread paramThread, Throwable paramThrowable) {
                handleUncaughtException(paramThrowable);
                defaultHandler.uncaughtException(paramThread, paramThrowable);
            }
        });
    }

    public void handleUncaughtException(Throwable throwable) {
        log.debug(Logs.EXCEPTIONS, Log.getStackTraceString(throwable));

        // PDK: Commenting out the code below because it doesn't play well with Firebase
        // Crashlytics' UncaughtExceptionHandler.
        //
        // See this link: https://github.com/firebase/firebase-android-sdk/issues/2005
        /*
        new Thread() {
            @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
            @Override
            public void run() {
                Looper.prepare();
                final Context context = getApplicationContext();
                Intent intent = new Intent(context, ErrorActivity.class);
                int mPendingIntentId = 123456;
                PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, intent,
                        PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
                mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 75, mPendingIntent);

                //System.exit(0);

                Looper.loop();
                Looper.myLooper().quitSafely();
            }
        }.start();

         */
    }


    public static boolean mInForeground = false;

    public static boolean inForeground() {
        return mInForeground;
    }

    public static class AppLifecycleObserver implements LifecycleObserver {

        @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
        public void onCreate() {
            log.debug(Logs.DEBUG, "TEMP_LOC: In AppLifecycleObserver:onCreate()");
            TruckEventsRepeatingTask.start(getAppContext(),2);
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_START)
        public void onStart() {
            mInForeground = true;
            log.debug(Logs.DEBUG, "TEMP_LOC: In AppLifecycleObserver:onStart()");
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
        public void onMoveToForeground() {
            log.debug(Logs.DEBUG, "TEMP_LOC: In AppLifecycleObserver:onMoveToForeground()");
            // We need to check the font scale each time the app moves to the foreground,
            // since user could have change the setting while the app was backgrounded.
            adjustFontScale();
            GenericScanManager.resumeScanManagerIfActive();
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        public void onMoveToBackground() {
            log.debug(Logs.DEBUG, "TEMP_LOC: In AppLifecycleObserver:onMoveToBackground()");
            GenericScanManager.pauseScanManagerIfActive();
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
        public void onStop() {
            mInForeground = false;
            log.debug(Logs.DEBUG, "TEMP_LOC: In AppLifecycleObserver:onStop()");
        }

        @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        public void onDestroy() {
            // According to Android reference, this will never happen for a ProcessLifecycleOwner.
            log.debug(Logs.DEBUG, "TEMP_LOC: In AppLifecycleObserver:onDestroy()");
        }
    }

    public static void setLastInteractionTime() {
        lastInteractionTime = System.currentTimeMillis();
    }

    public static long getLastInteractionTime() {
        return lastInteractionTime;
    }

    public static float minutesSinceLastInteraction() {
        return (float)(System.currentTimeMillis() - lastInteractionTime) / (float)(1000 * 60);
    }

    private static boolean sSimulateDriving = false;

    public static void toggleSimulateDriving() {
        sSimulateDriving = !sSimulateDriving;
    }

    public static boolean simulatingDriving() {
        return sSimulateDriving;
    }

    private static final float MAX_FONT_SCALE_FACTOR = 1.15f;
    private static final float MIN_FONT_SCALE_FACTOR = 1.0f;
    private static final int DEFAULT_SCREEN_HEIGHT_DP = 616;
    private static final int DEFAULT_SCREEN_WIDTH_DP = 360;
    private static final int DEFAULT_SCREEN_DENSITY_DPI = 320;

    public static void adjustFontScale() {
        // If fontScale is set to a value that AutoTran won't handle well, adjust it appropriately
        // in order to provide the best UI experience that accommodates the user's preferences.
        Configuration config = getAppContext().getResources().getConfiguration();
        //log.debug(Logs.DEBUG, "fontScale=" + config.fontScale);
        if (config.screenHeightDp > DEFAULT_SCREEN_HEIGHT_DP) {
            // If display size setting is set to small. Force display size and font size to defaults.
            config.screenHeightDp = DEFAULT_SCREEN_HEIGHT_DP;
            config.screenHeightDp = DEFAULT_SCREEN_HEIGHT_DP;
            config.densityDpi = DEFAULT_SCREEN_DENSITY_DPI;
            config.fontScale = MIN_FONT_SCALE_FACTOR;
        }
        else if (config.screenHeightDp < DEFAULT_SCREEN_HEIGHT_DP) {
            // If display size setting is set to large, force fontScale to 1.15 (large setting)
            //log.debug(Logs.DEBUG, "Detected large screen size. Forcing fontScale from " + config.fontScale + " to " + MAX_FONT_SCALE_FACTOR);
            config.fontScale = MAX_FONT_SCALE_FACTOR;
        }
        else if (config.fontScale > MAX_FONT_SCALE_FACTOR) {
            //log.debug(Logs.DEBUG, "Font scale " + config.fontScale + " too high. Setting fontScale to " + MAX_FONT_SCALE_FACTOR);
            config.fontScale = MAX_FONT_SCALE_FACTOR;

        }
        else if (config.fontScale < MIN_FONT_SCALE_FACTOR) {
            config.fontScale = MIN_FONT_SCALE_FACTOR;
        }
        else {
            return;
        }
        DisplayMetrics metrics = getAppContext().getResources().getDisplayMetrics();
        WindowManager wm = (WindowManager) getAppContext().getSystemService(WINDOW_SERVICE);
        wm.getDefaultDisplay().getMetrics(metrics);
        metrics.densityDpi = config.densityDpi;
        metrics.scaledDensity = config.fontScale * metrics.density;
        getAppContext().getResources().updateConfiguration(config, metrics);
    }
}

