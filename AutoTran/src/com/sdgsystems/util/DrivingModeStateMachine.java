package com.sdgsystems.util;

import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.activities.DrivingLockActivity;
import com.cassens.autotran.handlers.DrivingLocationHandler;
import com.sdgsystems.app_config.AppSetting;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DrivingModeStateMachine {
    private static final String TAG = "DrivingModeStateMachine";
    private static final Logger log = LoggerFactory.getLogger(DrivingModeStateMachine.class.getSimpleName());

    public enum DrivingEvent {
        IN_VEHICLE,
        NOT_IN_VEHICLE,
        MAYBE_IN_VEHICLE,
        INCONCLUSIVE,
        NEW_GPS_STATE;
    };

    private enum DrivingState {

        DRIVING {
            @Override
            public void processEvent(DrivingEvent drivingEvent) {
                switch (drivingEvent) {
                    case NOT_IN_VEHICLE:
                        setCurrentState(NOT_DRIVING);
                        break;

                    case INCONCLUSIVE:
                        setCurrentState(DRIVING_INCONCLUSIVE);
                        break;

                    case IN_VEHICLE:
                    case MAYBE_IN_VEHICLE:
                        // leave in DRIVING mode
                        setCurrentState(DRIVING);
                        break;

                    default: // Shouldn't happen
                        break;
                }
            }
        },
        DRIVING_INCONCLUSIVE {
            @Override
            public void processEvent(DrivingEvent drivingEvent) {
                switch (drivingEvent) {
                    case IN_VEHICLE:
                    case MAYBE_IN_VEHICLE:
                        setCurrentState(DRIVING);
                        break;

                    case NOT_IN_VEHICLE:
                        setCurrentState(NOT_DRIVING);
                        break;

                    case INCONCLUSIVE:
                        if (isDrivingInconclusiveTimerExpired()) {
                            setCurrentState(NOT_DRIVING);
                        }
                        break;

                    default: // Shouldn't happen
                        break;
                }
            }
        },
        NOT_DRIVING {
            @Override
            public void processEvent(DrivingEvent drivingEvent) {
                switch (drivingEvent) {
                    case IN_VEHICLE:
                        setCurrentState(DRIVING);
                        break;

                    case MAYBE_IN_VEHICLE:
                        // For now, don't switch to driving state from NOT_DRIVING on
                        // MAYBE_IN_VEHICLE.

                    case NOT_IN_VEHICLE:
                    case INCONCLUSIVE:
                        // INCONCLUSIVE event while not driving should be considered a no-op.
                        break;

                    default: // Shouldn't happen
                        break;
                }
            }
        };

        private static DrivingState sCurrentState = NOT_DRIVING;
        private static final String DRIVING_INCONCLUSIVE_TIMER_STARTED = "driving_inconclusive_timer_started";
        private static final long DRIVING_INCONCLUSIVE_TIMEOUT_SECS = 30;

        protected static void resetDrivingInconclusiveTimer() {
            HelperFuncs.setLongPref(AutoTranApplication.getAppContext(), DRIVING_INCONCLUSIVE_TIMER_STARTED, System.currentTimeMillis());
        }

        protected static boolean isDrivingInconclusiveTimerExpired() {
            long timerStarted = PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext()).getLong(DRIVING_INCONCLUSIVE_TIMER_STARTED, 0L);
            return (getDrivingInconclusiveElapsedTime() > DRIVING_INCONCLUSIVE_TIMEOUT_SECS * 1000);
        }

        protected static long getDrivingInconclusiveElapsedTime() {
            long timerStarted = PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext()).getLong(DRIVING_INCONCLUSIVE_TIMER_STARTED, 0L);
            return System.currentTimeMillis() - timerStarted;
        }

        protected static long getDrivingInconclusiveTimeLeft() {
            long elapsedTime = getDrivingInconclusiveElapsedTime();
            if (elapsedTime < DRIVING_INCONCLUSIVE_TIMEOUT_SECS * 1000) {
                return DRIVING_INCONCLUSIVE_TIMEOUT_SECS * 1000 - elapsedTime;
            }
            else {
                return 0;
            }
        }


        private static Handler recheckGpsHandler = new Handler(Looper.getMainLooper());
        private static final Runnable recheckGps = new Runnable(){
            public void run(){
                log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG:DrivingModeStateManchine.recheckGpsHandler(): Retrying startDrivingLockActivityIfNeeded()");
                DrivingLockActivity.startDrivingLockActivityIfNeeded(AutoTranApplication.getAppContext());
            }
        };

        protected static void setCurrentState(DrivingState state) {
            //log.debug(Logs.DEBUG, String.format("DRIVING_LOCK_DEBUG: setCurrentState(%s)", state.toString(), sCurrentState.toString()));

            if (state == sCurrentState) {
                if (state == DRIVING && !getLocationHandler().isLocationTrackingEnabled() && CommonUtility.getCurrentActivity() != null) {
                    log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Retrying GPS after app launch");
                }
                else {
                    return;
                }
            }
            log.debug(Logs.DEBUG, String.format("DRIVING_LOCK_DEBUG: State transition: %s to %s", sCurrentState.toString(), state.toString()));

            sCurrentState = state;
            if (state == DRIVING_INCONCLUSIVE) {
                //log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: setCurrentState(): Resetting drivingInconclusiveTimer()");
                resetDrivingInconclusiveTimer();
            }
            else if (state == DRIVING) {
                //log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Cancelling any scheduled stopGps() runnables");
                cancelStopGps();

                if (getLocationHandler().isLocationTrackingEnabled()) {
                    log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: setCurrentState(): GPS already on. Calling startDrivingLockActivityIfNeeded()");
                    DrivingLockActivity.startDrivingLockActivityIfNeeded(AutoTranApplication.getAppContext());
                }
                else {
                    log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: setCurrentState(): GPS off. Scheduling startDrivingLockActivityIfNeeded()");
                    startGpsAssist();
                    recheckGpsHandler.postDelayed(recheckGps, 3000);
                }
            }
            else if (state == NOT_DRIVING) {
                //scheduleStopGps();
                DrivingLockActivity.resetDrivingLockOverrideCount();
                scheduleStopGps();
            }
        }

        protected abstract void processEvent(DrivingEvent drivingEvent);
    }

    public static void processEvent(DrivingEvent drivingEvent) {
        //log.debug(Logs.DEBUG, String.format("Got %s event while in %s state", drivingEvent.toString(), DrivingState.sCurrentState.toString()));
        if (getLocationHandler().isLocationTrackingEnabled()) {
            // A conclusive DRIVING or NOT_DRIVING state from GPS overrides state detected via
            // the DetectedActivities.
            switch (sLocationHandler.getDrivingState()) {
                case DRIVING:
                    if (DrivingState.sCurrentState != DrivingState.DRIVING) {
                        log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: Transitioning to DRIVING state based on GPS");
                    }
                    DrivingState.setCurrentState(DrivingState.DRIVING);
                    return;

                case NOT_DRIVING:
                    if (DrivingState.sCurrentState != DrivingState.NOT_DRIVING) {
                        log.debug(Logs.DEBUG,"DRIVING_LOCK_DEBUG: Transitioning to NOT_DRIVING state based on GPS");
                    }
                    DrivingState.setCurrentState(DrivingState.NOT_DRIVING);
                    return;

                case UNKNOWN:
                default:
                    //log.debug(Logs.DEBUG, String.format("DRIVING_LOCK_DEBUG: Ignoring GPS driving state: %s", sLocationHandler.getDrivingState().toString()));
                    break;
            }
        }
        if (drivingEvent == DrivingEvent.NEW_GPS_STATE) {
            // This event is used by DrivingLocationHandler to force a driving state check whenever
            // the GPS driving state changes. (See above.)
            log.debug(Logs.DEBUG,"DRIVING_LOCK_DEBUG: Received NEW_GPS_STATE event");
            return;
        }
        // If we get this far, it means either GPS is not on or LocationHanlder does not yet have
        // enough speed data to determine whether we are driving.
        //log.debug(Logs.DEBUG,"DRIVING_LOCK_DEBUG: GPS not active processing event via Activity Detection Service.");
        DrivingState.sCurrentState.processEvent(drivingEvent);
    }

    public static boolean isDriving() {
        switch (DrivingState.sCurrentState) {
            case DRIVING:
            case DRIVING_INCONCLUSIVE:
                return true;

            default:
                return false;
        }
    }

    public static String currentState() {

        switch (DrivingState.sCurrentState) {
            case NOT_DRIVING:
                return "Not-Driving";

            case DRIVING:
                return "Driving";

            case DRIVING_INCONCLUSIVE:
                return "Driving-Inc";

            default:
                return "Not Set";
        }
    }

    public static int getDrivingInconclusiveTimeLeft() {
        if (DrivingState.sCurrentState == DrivingState.DRIVING_INCONCLUSIVE) {
            return (int)(DrivingState.getDrivingInconclusiveTimeLeft() / 1000);
        }
        return -1;
    }


    private static DrivingLocationHandler sLocationHandler = null;
    private static DrivingLocationHandler getLocationHandler() {
        if (sLocationHandler == null) {
            sLocationHandler = DrivingLocationHandler.getInstance(AutoTranApplication.getAppContext());
        }
        return sLocationHandler;
    }

    public static void startGpsAssist() {
        getLocationHandler();

        if (!sLocationHandler.isLocationTrackingEnabled()) {
            if (CommonUtility.getCurrentActivity() != null) {
                log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: startGpsAssist: Starting GPS location tracking");
                CommonUtility.getCurrentActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        sLocationHandler.startLocationTracking();
                    }
                });
            }
            else {
                log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: startGpsAssist: Not starting GPS. No active activities.");
            }
        }
        else {
            log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: startGpsAssist: locationTracking already started");
        }
        //sLocationHandler.getLocation();
    }

    private static Handler stopGpsHandler;
    private static Runnable stopGps;

    public static void scheduleStopGps() {
        log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: scheduleStopGps() Scheduling stopGps task to stop locationTracking in " + AppSetting.DRIVING_LOCK_STOP_GPS_TIMEOUT.getInt() + " seconds.");

        stopGpsHandler = new Handler();
        stopGps = new Runnable(){
            public void run(){
                try {
                    if (!getLocationHandler().gpsSpeedIndicatesDriving()) {
                        log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: stopGps(): STOPPING locationTracking");
                        CommonUtility.getCurrentActivity().runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                sLocationHandler.stopLocationTracking();
                            }
                        });
                    }
                    else {
                        log.debug(Logs.DEBUG, "DRIVING_LOCK_DEBUG: stopGps(): NOT STOPPING locationTracking. We are driving again.");
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        stopGpsHandler.postDelayed(stopGps, (long) AppSetting.DRIVING_LOCK_STOP_GPS_TIMEOUT.getInt() * 1000);
    }

    public static void cancelStopGps() {
        if (stopGpsHandler != null && stopGps != null) {
            stopGpsHandler.removeCallbacks(stopGps);
        }
        stopGpsHandler = null;
        stopGps = null;
    }

}
