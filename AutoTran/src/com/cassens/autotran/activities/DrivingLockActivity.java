package com.cassens.autotran.activities;

import static com.cassens.autotran.constants.Constants.PREF_DRIVING_LOCK_OVERRIDDEN;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.graphics.PorterDuff;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.view.View;
import android.view.Window;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.handlers.DrivingLocationHandler;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.cassens.autotran.receivers.DrivingLockOverrideDoneReceiver;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.DetectedActivitiesIntentService;
import com.sdgsystems.util.DrivingModeStateMachine;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Calendar;

public class DrivingLockActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(DrivingLockActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    public static final int REQUEST_CODE = 101;
    private static int sDrivingLockOverrideCount = 0;

    private static TextView mDrivingModeText;
    private static ImageView mGpsStatusIcon;
    private static TextView mGpsStatusMsg;
    private static ImageView mActivityRecognitionStatusIcon;
    private static Activity mDrivingLockActivity;
    private static final Object mDrivingLockActivityLock = new Object();

    public static void setDetectedActivityIndicator(final String msg) {
        synchronized (mDrivingLockActivityLock) {
            if (mDrivingLockActivity != null) {
                mDrivingLockActivity.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        mDrivingModeText.setText(msg);
                        DrivingLocationHandler dh = DrivingLocationHandler.getInstance(mDrivingLockActivity);
                        Resources res = AutoTranApplication.getAppContext().getResources();
                        if (dh.isGpsProviderEnabled()) {
                            if (dh.getSpeed() >= 0) {
                                setIconColor(mGpsStatusIcon, R.drawable.ic_gps_fixed_24px, R.color.dark_green);
                                mGpsStatusMsg.setText("");
                            } else {
                                setIconColor(mGpsStatusIcon, R.drawable.ic_gps_not_fixed_24px, R.color.dark_orange);
                                mGpsStatusMsg.setText("Searching for GPS signal...");
                            }
                        } else {
                            setIconColor(mGpsStatusIcon, R.drawable.ic_gps_off_24px, R.color.lite_red);
                            mGpsStatusMsg.setText("GPS is Off");
                        }
                        mDrivingModeText.invalidate();
                        mGpsStatusMsg.invalidate();
                    }
                });
            }
        }
    }

    private static void setIconColor(ImageView icon, int drawableId, int colorId) {
        synchronized (mDrivingLockActivityLock) {
            if (mDrivingLockActivity != null) {
                icon.setImageDrawable(mDrivingLockActivity.getDrawable(drawableId));
                icon.setColorFilter(ContextCompat.getColor(mDrivingLockActivity, colorId), PorterDuff.Mode.SRC_IN);
            }
        }
    }


    Handler checkDrivingHandler = new Handler();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        log.debug(Logs.DEBUG, "Creating activity");
        setContentView(R.layout.activity_driving_mode);

        TextView header = findViewById(R.id.header_tv);
        TextView tv1 = findViewById(R.id.drivingOverrideMsg_tv);
        TextView tv2 = findViewById(R.id.drivingOverrideMsg2_tv);
        TextView contactSupport = findViewById(R.id.contactSupport_tv);
        EditText driverNumberText = findViewById(R.id.pay_number_textbox);
        Button notDrivingButton = findViewById(R.id.notDrivingButton);

        if (sDrivingLockOverrideCount >= AppSetting.DRIVING_LOCK_MAX_QUICK_OVERRIDES.getInt()) {
            driverNumberText.setVisibility(View.VISIBLE);
            tv1.setText(R.string.drivingOverridePayNumMsg);
            tv2.setVisibility(View.VISIBLE);
        }

        notDrivingButton.setOnClickListener(view -> {
            CommonUtility.logButtonClick(log, view);
            if (sDrivingLockOverrideCount >= AppSetting.DRIVING_LOCK_MAX_QUICK_OVERRIDES.getInt()) {
                if (!driverNumberText.getText().toString().trim().equals(CommonUtility.getDriverNumber(getApplicationContext()))) {
                    log.debug(Logs.INTERACTION, "Message shown: " + R.string.error_wrong_driver_number);
                    CommonUtility.showText(getString(R.string.error_wrong_driver_number));
                } else {
                    //temporarily override driving lock
                    log.debug(Logs.INTERACTION, "Driver overrode driving mode with pay number");
                    //send message
                    sendDrivingLockOverrideMsg();
                    overrideDrivingLock(AppSetting.DRIVING_LOCK_OVERRIDE_TIMEOUT.getInt());
                }
            }
            else {
                log.debug(Logs.INTERACTION, "Driver overrode driving mode");
                //send message
                sendDrivingLockOverrideMsg();
                overrideDrivingLock(AppSetting.DRIVING_LOCK_QUICK_OVERRIDE_TIMEOUT.getInt());
            }
        });

        ((Button) findViewById(R.id.EndDrivingSimulation)).setVisibility(AutoTranApplication.simulatingDriving() ? View.VISIBLE : View.GONE);
        mGpsStatusIcon = (ImageView)findViewById(R.id.GpsStatusIcon);
        mGpsStatusMsg = (TextView) findViewById(R.id.GpsStatusMsg);
        mDrivingModeText = (TextView) findViewById(R.id.LocationMessageBox);

        synchronized (mDrivingLockActivityLock) {
            mDrivingLockActivity = this;
        }

        boolean largeFont = CommonUtility.isLargeFontSet();
        boolean largeScreen = CommonUtility.isHoneywellLargeDisplaySet();
        if (largeFont || largeScreen) {
            header.setTextSize(26);
            if (largeFont) {
                tv1.setTextSize(14);
                tv2.setTextSize(14);
                contactSupport.setTextSize(14);
                mGpsStatusMsg.setTextSize(12);
                mDrivingModeText.setTextSize(12);
            }
            if (largeScreen) {
                CommonUtility.scaleImageView(this, R.id.noPhoneUseIcon, 0.8);
                CommonUtility.scaleImageView(this, R.id.GpsStatusIcon, 0.8);
            }
        }
        CommonUtility.appendEpodContactInfo(this, contactSupport, null, "Driving Lock");

        checkDrivingHandler.post(checkDriving);
    }

    public static boolean isLockScreenDisplayed() {
        return CommonUtility.getCurrentActivity() instanceof DrivingLockActivity;
    }

    public static boolean isLockScreenNeeded() {
        if (DrivingModeStateMachine.isDriving()) {
            return !HelperFuncs.getBoolPref(AutoTranApplication.getAppContext(), PREF_DRIVING_LOCK_OVERRIDDEN, false);
        }
        else {
            return false;
        }
    }

    public static void resetDrivingLockOverrideCount() {
        sDrivingLockOverrideCount = 0;
    }

    private void sendDrivingLockOverrideMsg() {
        String lat = "--";
        String lon = "--";
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String currentDriverID = CommonUtility.getDriverNumber(this);
        String currentTruckID = TruckNumberHandler.getTruckNumber(this);
        Location location = DrivingLocationHandler.getInstance(this).getLocation();
        if (location != null) {
            lat = String.valueOf(location.getLatitude());
            lon = String.valueOf(location.getLongitude());
        }
        float speed = DrivingLocationHandler.getInstance(this).getSpeed();
        String speedStr = (speed >= 0.0) ? String.format("%3.1f", speed) : "--";
        SimpleTimeStamp sts = new SimpleTimeStamp();
        log.debug(Logs.DEBUG, String.format("DRIVING_LOCK_DEBUG: lat='%s' lon='%s' speed=%s", lat, lon, speedStr));

        String version = "--";

        try {
            version = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        String vinMessageString = TextUtils.join(",",
                new String[] {
                        "Driving Lock Overridden",
                        currentDriverID,
                        currentTruckID,
                        lat,
                        lon,
                        sts.getUtcDateTime(),
                        sts.getUtcTimeZone(),
                        speedStr,
                        version
                });
        LoadEvent event = new LoadEvent();
        event.csv = vinMessageString;
        DataManager.insertLoadEvent(this, event);
        if( CommonUtility.isConnected(this)) {
            SyncManager.pushLoadEventsLatched(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        //if we've stopped driving while activity is hidden, finish
        /*
        if (!isLockScreenNeeded()) {
            log.debug(Logs.DEBUG, "no longer driving, finish activity");
            finish();
            return;
        } */

        DetectedActivitiesIntentService.refreshDetectedActivityIndicators();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        log.debug(Logs.DEBUG, "in onDestroy before locationTracking");
        synchronized (mDrivingLockActivityLock) {
            mDrivingLockActivity = null;
        }
        checkDrivingHandler.removeCallbacks(checkDriving);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }

    private final Runnable checkDriving = new Runnable(){
        public void run(){
            try {
                if (!isLockScreenNeeded()) {
                    log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Lock screen no longer needed.  Calling finish().");
                    finish();
                }
                else {
                    checkDrivingHandler.postDelayed(this, 1000);
                }
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    };

    public void refreshActivityState(View view) {
        drivingActivityHandler.refreshHandler(getApplicationContext());
    }

    public void onEndDrivingSimulationClick(View view) {
        AutoTranApplication.toggleSimulateDriving();
        view.setVisibility(View.GONE);
    }

    public void overrideDrivingLock(long timeout) {
        log.debug(Logs.INTERACTION, "DRIVING_LOCK_DEBUG: Overriding driving lock for " + timeout + " seconds.");
        sDrivingLockOverrideCount++;
        HelperFuncs.setBoolPref(this, PREF_DRIVING_LOCK_OVERRIDDEN, true);
        Intent intent = new Intent (this, DrivingLockOverrideDoneReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        alarmManager.setExact(AlarmManager.RTC_WAKEUP, Calendar.getInstance().getTimeInMillis() + timeout*1000, pendingIntent);
        finish();
    }


    public static void startDrivingLockActivityIfNeeded(Context context) {
        Activity currentActivity = CommonUtility.getCurrentActivity();
        if (currentActivity == null) {
            //log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Not starting driving lock. No activities running.");
            return;
        }
        currentActivity.runOnUiThread(new Runnable() {
            public void run() {
                if (isLockScreenDisplayed()) {
                    //log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Lock screen is already displayed");
                    return;
                }
                if (!AutoTranApplication.inForeground()) {
                    //log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Not starting driving lock. App is backgrounded");
                    return;
                }
                if (CommonUtility.getCurrentActivity() instanceof SplashActivity) {
                    return;
                }
                if (!isLockScreenNeeded()) {
                    //log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Driving lock screen not needed.  Not driving or lock overridden");
                    return;
                }
                log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Starting driving lock activity");
                Intent drivingIntent = new Intent(context, DrivingLockActivity.class);
                drivingIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(drivingIntent);
            }
        });
    }
}
