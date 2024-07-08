package com.cassens.autotran.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.Logs;
import com.cassens.autotran.activities.DrivingLockActivity;
import com.sdgsystems.util.DetectedActivitiesIntentService;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.cassens.autotran.constants.Constants.PREF_DRIVING_LOCK_OVERRIDDEN;

public class DrivingLockOverrideDoneReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(DrivingLockOverrideDoneReceiver.class.getSimpleName());

    @Override
    public void onReceive(Context context, Intent intent) {
        log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Calling startDrivingLocakActiivtyIfNeeded after end driving lock override");
        HelperFuncs.setBoolPref(context, PREF_DRIVING_LOCK_OVERRIDDEN, false);
        DrivingLockActivity.startDrivingLockActivityIfNeeded(AutoTranApplication.getAppContext());
    }
}