package com.cassens.autotran.handlers;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.constants.Constants;
import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityRecognitionClient;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.Task;
import com.sdgsystems.util.DetectedActivitiesIntentService;

import java.util.ArrayList;
import java.util.List;

import static com.cassens.autotran.constants.Constants.USE_ACTIVITY_TRANSITIONS_INTERFACE;

public class DrivingActivityHandler {
    private static DrivingActivityHandler instance = null;
    private Task<Void> drivingActivityTask;
    private Task<Void> drivingActivityRecognitionTask;
    private ActivityRecognitionClient mActivityRecognitionClient;


    public static DrivingActivityHandler getInstance(Context ctx) {
        if (instance == null) {
            instance = new DrivingActivityHandler(ctx);
        }
        return instance;
    }

    public static DrivingActivityHandler refreshHandler(Context ctx) {

        if (instance != null) {
            instance.stopInstance();
        }

        instance = new DrivingActivityHandler(ctx);

        return instance;
    }

    private DrivingActivityHandler(Context ctx) {
        mActivityRecognitionClient = ActivityRecognition.getClient(ctx);

        if (USE_ACTIVITY_TRANSITIONS_INTERFACE) {
            ActivityTransitionRequest request = buildTransitionRequest();
            drivingActivityTask = ActivityRecognition.getClient(ctx)
                    .requestActivityTransitionUpdates(request, getActivityDetectionPendingIntent());
            //startActivityUpdates();
        }
        else {
            drivingActivityTask = mActivityRecognitionClient.requestActivityUpdates(
                    Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                    getActivityDetectionPendingIntent());
        }
    }

    public ActivityRecognitionClient getActivityRecognitionClient() {
        return mActivityRecognitionClient;
    }

    /**
     * Gets a PendingIntent to be sent for each activity detection.
     */
    private PendingIntent getActivityDetectionPendingIntent() {
        Context context = AutoTranApplication.getAppContext();
        Intent intent = new Intent(context, DetectedActivitiesIntentService.class);

        // We use FLAG_UPDATE_CURRENT so that we get the same pending intent back when calling
        // requestActivityUpdates() and removeActivityUpdates().
        return PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_MUTABLE);
    }

    private ActivityTransitionRequest buildTransitionRequest() {
        List transitions = new ArrayList<>();
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.IN_VEHICLE)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        transitions.add(new ActivityTransition.Builder()
                 .setActivityType(DetectedActivity.ON_FOOT)
                 .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                 .build());
        transitions.add(new ActivityTransition.Builder()
                 .setActivityType(DetectedActivity.ON_FOOT)
                 .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                 .build());

        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                .build());
        transitions.add(new ActivityTransition.Builder()
                .setActivityType(DetectedActivity.STILL)
                .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                .build());

        return new ActivityTransitionRequest(transitions);
    }

    private Task<Void> stopActivityTask;

    public void startActivityUpdates() {
        drivingActivityRecognitionTask = mActivityRecognitionClient.requestActivityUpdates(
                Constants.DETECTION_INTERVAL_IN_MILLISECONDS,
                getActivityDetectionPendingIntent());
    }

    public void stopActivityUpdates() {
        stopActivityTask =  mActivityRecognitionClient.removeActivityUpdates(getActivityDetectionPendingIntent());
    }

    public void stopInstance() {
        mActivityRecognitionClient.removeActivityTransitionUpdates(getActivityDetectionPendingIntent());
        mActivityRecognitionClient = null;
        instance = null;
    }
}
