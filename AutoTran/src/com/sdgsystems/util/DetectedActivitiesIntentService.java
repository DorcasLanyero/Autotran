package com.sdgsystems.util;

import android.app.IntentService;
import android.content.Intent;
import android.location.Location;

import androidx.annotation.Nullable;
import android.util.Log;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.activities.DashboardActivity;
import com.cassens.autotran.activities.DrivingLockActivity;
import com.cassens.autotran.handlers.DrivingLocationHandler;
import com.google.android.gms.location.ActivityRecognitionResult;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionEvent;
import com.google.android.gms.location.ActivityTransitionResult;
import com.google.android.gms.location.DetectedActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

import static com.cassens.autotran.constants.Constants.USE_ACTIVITY_TRANSITIONS_INTERFACE;

public class DetectedActivitiesIntentService extends IntentService {
    protected static final String TAG = "DetectedActivitiesIS";
    private static final Logger log = LoggerFactory.getLogger(DetectedActivitiesIntentService.class.getSimpleName());

    //private static final long DRIVING_LOCK_DELAY_SECS = 8 * 60;
    //private static final long DRIVING_UNLOCK_DELAY_SECS = 60;


    /**
     * This constructor is required, and calls the super IntentService(String)
     * constructor with the name for a worker thread.
     */
    public DetectedActivitiesIntentService() {
        // Use the TAG to name the worker thread.
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        //log.debug(Logs.DEBUG, "DetectedActivitiesIntentService: onCreate()");
    }

    // Sequence number to increment on each new received intent
    private static int sIntentSeqNum = 0;


    private void processActivityTransitionResult(@Nullable Intent intent) {

        if (!ActivityTransitionResult.hasResult(intent)) {
            return;
        }

        ActivityTransitionResult result = ActivityTransitionResult.extractResult( intent);

        Log.e( TAG, "Got ActivityTransitionResult()");

        String activitiesMsg = new String();
        String eventField = "No-Op";

        String prevState = DrivingModeStateMachine.currentState();

        for ( ActivityTransitionEvent event : result.getTransitionEvents()) {
            activitiesMsg += " ";
            if (isInVehicleStart(event)) {
                Log.e( TAG, "Got inVehicle START event");
                activitiesMsg += "IVS";
                DrivingModeStateMachine.processEvent(DrivingModeStateMachine.DrivingEvent.IN_VEHICLE);
                eventField = "IV";
            } else if (isInVehicleEnd(event)) {
                Log.e( TAG, "Got inVehicle END event");
                activitiesMsg += "IVE";
                DrivingModeStateMachine.processEvent(DrivingModeStateMachine.DrivingEvent.NOT_IN_VEHICLE);
                eventField = "NIV";
            } else if (isOnFootEnd(event)) {
                Log.e( TAG, "Got onFoot END event");
                activitiesMsg += "OFE";
                //HelperFuncs.setBoolPref(this, "foot_trans", false);
            } else if (isOnFootStart(event)) {
                Log.e( TAG, "Got onFoot START event");
                DrivingModeStateMachine.processEvent(DrivingModeStateMachine.DrivingEvent.NOT_IN_VEHICLE);
                activitiesMsg += "OFS";
            } else if (isStillStart(event)) {
                Log.e( TAG, "Got still START event");
                DrivingModeStateMachine.processEvent(DrivingModeStateMachine.DrivingEvent.NOT_IN_VEHICLE);
                activitiesMsg += "SS";
            } else if (isStillEnd(event)) {
                Log.e( TAG, "END event");
                activitiesMsg += "SE";
            } else {
                Log.e( TAG, "Other event: " + event.toString());
            }
        }

        String stateField;
        if (prevState.equals(DrivingModeStateMachine.currentState())) {
            stateField = DrivingModeStateMachine.currentState();
        }
        else {
            stateField = String.format("%s->%s", prevState, DrivingModeStateMachine.currentState());
        }
        updateDetectedActivityIndicators(String.format("%s (%s:%s)", stateField, eventField, activitiesMsg));

    }

    private void processActivityRecognitionResult(Intent intent) {
        if (!ActivityRecognitionResult.hasResult(intent)) {
            return;
        }

        ActivityRecognitionResult result = ActivityRecognitionResult.extractResult(intent);

        //Log.e(TAG, "Got ActivityRecognitionResult(): " + result.toString());


        // Get the list of the probable activities associated with the current state of the
        // device. Each activity is associated with a confidence level, which is an int between
        // 0 and 100.
        ArrayList<DetectedActivity> detectedActivities = (ArrayList) result.getProbableActivities();

        String activitiesMsg = new String();
        boolean first = true;
        int inVehicleConfidence = -1;
        int notInVehicleConfidence = -1;

        for (DetectedActivity da : detectedActivities) {
            //Log.e(TAG, "DetectedActivity: " + detectedActivity.toString() + " conf=" + detectedActivity.getConfidence());
            if (first) {
                first = false;
            }
            else {
                activitiesMsg += ",";
            }
            activitiesMsg += String.format("%s=%d", detectedActivityTypeToString(da), da.getConfidence());

            switch (da.getType()) {
                case DetectedActivity.TILTING:
                    // Ignore tilting, since these events sometimes occur while driving
                    // or not driving and show up with confidence=100%.
                    break;

                case DetectedActivity.IN_VEHICLE:
                    inVehicleConfidence = da.getConfidence();
                    break;

                case DetectedActivity.STILL:
                case DetectedActivity.ON_BICYCLE:
                case DetectedActivity.ON_FOOT:
                case DetectedActivity.RUNNING:
                case DetectedActivity.WALKING:
                    if (da.getConfidence() > notInVehicleConfidence) {
                        notInVehicleConfidence = da.getConfidence();
                    }
                    break;

                case DetectedActivity.UNKNOWN:
                default:
            }
        }

        String prevState  = DrivingModeStateMachine.currentState();
        String eventField;

        if (inVehicleConfidence > 50) {
            DrivingModeStateMachine.processEvent(DrivingModeStateMachine.DrivingEvent.IN_VEHICLE);
            eventField = "IV";
        }
        else if (notInVehicleConfidence > 50) {
            DrivingModeStateMachine.processEvent(DrivingModeStateMachine.DrivingEvent.NOT_IN_VEHICLE);
            eventField = "NIV";
        }
        else if (inVehicleConfidence >= 10 && inVehicleConfidence >= notInVehicleConfidence) {
            DrivingModeStateMachine.processEvent(DrivingModeStateMachine.DrivingEvent.MAYBE_IN_VEHICLE);
            eventField = "MIV";
        }
        else if (inVehicleConfidence == -1 && notInVehicleConfidence == -1) {
            // This happens on a TILTING notification, which we don't care about.git
            eventField = "NO-OP";
        }
        else {
            DrivingModeStateMachine.processEvent(DrivingModeStateMachine.DrivingEvent.INCONCLUSIVE);
            eventField = "INC";
        }

        String stateField;
        /*
        if (prevState.equals(DrivingModeStateMachine.currentState())) {
            stateField = DrivingModeStateMachine.currentState();
        }
        else {
            stateField = String.format("%s->%s", prevState, DrivingModeStateMachine.currentState());
        }

        updateDetectedActivityIndicators(String.format("%s (%s iv=%d niv=%d to=%d)", stateField, eventField,
                inVehicleConfidence, notInVehicleConfidence, DrivingModeStateMachine.getDrivingInconclusiveTimeLeft()));
        */
        DetectedActivity mpa = result.getMostProbableActivity();
        stateField = String.format("%s=%d", detectedActivityTypeToString(mpa), mpa.getConfidence());

        updateDetectedActivityIndicators(String.format("%s (iv=%d niv=%d)", stateField, inVehicleConfidence, notInVehicleConfidence));
    }

    private String detectedActivityTypeToString(DetectedActivity da) {
        String typeAndConfidence;
        switch (da.getType()) {
            case DetectedActivity.IN_VEHICLE:
                typeAndConfidence = "v";
                break;
            case DetectedActivity.ON_BICYCLE:
                typeAndConfidence = "b";
                break;
            case DetectedActivity.ON_FOOT:
                typeAndConfidence = "f";
                break;
            case DetectedActivity.RUNNING:
                typeAndConfidence = "r";
                break;
            case DetectedActivity.STILL:
                typeAndConfidence = "s";
                break;
            case DetectedActivity.TILTING:
                typeAndConfidence = "t";
                break;
            case DetectedActivity.WALKING:
                typeAndConfidence = "w";
                break;
            case DetectedActivity.UNKNOWN:
                typeAndConfidence = "u";
                break;
            default:
                typeAndConfidence = "invalid";
        }
        return typeAndConfidence;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        if (sIntentSeqNum++ == 999) {
            sIntentSeqNum = 0;
        }

        if (USE_ACTIVITY_TRANSITIONS_INTERFACE) {
            processActivityTransitionResult(intent);
        }
        else {
            processActivityRecognitionResult(intent);
        }
    }


    private static String sCurrentActivityIndicatorMsg = "Awaiting first activity intent";
    private static Location location = null;

    private static void updateDetectedActivityIndicators(String message) {

        if (message != null) {
            //String speed = String.valueOf(location.getSpeed());
            sCurrentActivityIndicatorMsg = String.format("%03d: %s", sIntentSeqNum, message);
            //log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: " + sCurrentActivityIndicatorMsg);
        }

        String tmpMsg = String.format("%s\n%s", DrivingLocationHandler.getInstance(AutoTranApplication.getAppContext()).getLastSpeedMsg(), sCurrentActivityIndicatorMsg);
        DashboardActivity.setDetectedActivityIndicator(tmpMsg);
        DrivingLockActivity.setDetectedActivityIndicator(tmpMsg);
    }


    public static void refreshDetectedActivityIndicators() {
        updateDetectedActivityIndicators(null);
    }

    public static boolean isInDrivingState() {
        return DrivingModeStateMachine.isDriving();

        /*
        switch (getCurrentActivityState()) {
            case DRIVING:
            case DRIVING_CONFIRMING:
                return true;

            default:
                return false;
        }
         */
    }


    private boolean isInVehicleEnd(ActivityTransitionEvent event) {
        return event.getActivityType() == DetectedActivity.IN_VEHICLE && event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT;
    }

    private boolean isInVehicleStart(ActivityTransitionEvent event) {
        return event.getActivityType() == DetectedActivity.IN_VEHICLE && event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER;
    }

    private boolean isStillEnd(ActivityTransitionEvent event) {
        return event.getActivityType() == DetectedActivity.STILL && event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT;
    }

    private boolean isStillStart(ActivityTransitionEvent event) {
        return event.getActivityType() == DetectedActivity.STILL && event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER;
    }

    private boolean isWalkingEnd(ActivityTransitionEvent event) {
        return event.getActivityType() == DetectedActivity.WALKING && event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT;
    }

    private boolean isWalkingStart(ActivityTransitionEvent event) {
        return event.getActivityType() == DetectedActivity.WALKING && event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER;
    }

    private boolean isOnFootEnd(ActivityTransitionEvent event) {
        return event.getActivityType() == DetectedActivity.ON_FOOT && event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_EXIT;
    }

    private boolean isOnFootStart(ActivityTransitionEvent event) {
        return event.getActivityType() == DetectedActivity.ON_FOOT && event.getTransitionType() == ActivityTransition.ACTIVITY_TRANSITION_ENTER;
    }
}
