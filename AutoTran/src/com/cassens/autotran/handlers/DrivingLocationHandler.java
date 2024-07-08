package com.cassens.autotran.handlers;

import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;

import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.DetectedActivitiesIntentService;
import com.sdgsystems.util.DrivingModeStateMachine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class DrivingLocationHandler {
    public static final String TAG = "DrivingLocationHandler";
    private static final Logger log = LoggerFactory.getLogger(DrivingLocationHandler.class.getSimpleName());

    private static DrivingLocationHandler instance = null;

    public LocationManager manager;
    private String mProvider = null;
    private boolean locationTrackingEnabled = false;
    private Location lastLocation = null;
    private boolean speedTracking = true;

    private LocationListener listener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //Log.d(TAG, "DRIVING_LOCK_DEBUG: LocationListener() got location");
            updateLocationAndSpeed(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
            //Log.d(TAG, String.format("DRIVING_LOCK_DEBUG: LocationListener.onStatusChanged() callback: %d '%s'", status, HelperFuncs.noNull(extras.toString())));
        }

        public void onProviderEnabled(String provider) {
            Log.d(TAG, String.format("DRIVING_LOCK_DEBUG: LocationListener: Provider %s was enabled", provider));
        }

        public void onProviderDisabled(String provider) {
            Log.d(TAG, String.format("DRIVING_LOCK_DEBUG: gLocationListener: Provider %s was disabled", provider));
            mLastSpeedMsg = "GPS disabled. Check Location Mode in Settings.";
            mLastSpeed = -1;
            DetectedActivitiesIntentService.refreshDetectedActivityIndicators();
        }
    };

    private DrivingLocationHandler(Context ctx) {
        this.setManager(ctx);
    }

    public static DrivingLocationHandler getInstance(Context ctx) {
        if (instance == null) {
            instance = new DrivingLocationHandler(ctx);
        }
        return instance;
    }

    private void setManager(Context context) {
        this.manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);

        //List<String> allProviders = manager.getAllProviders();
        //List <String> availableProviders = manager.getProviders(false);
        //List<String> enabledProviders = manager.getProviders(true);
    }

    public void setSpeedTracking(boolean speedTracking) {
        this.speedTracking = speedTracking;
    }

    public enum GpsDrivingState {
        DRIVING,
        NOT_DRIVING,
        UNKNOWN;
    };
    private GpsDrivingState mLastDrivingState = GpsDrivingState.UNKNOWN;
    private GpsDrivingState mConfirmedDrivingState = GpsDrivingState.UNKNOWN;

    private int mUpdateSeqNum = 0;
    private float mLastSpeed = -1; // Last speed in mph
    private static final String GPS_LOCATION_TRACKING_OFF = "GPS location tracking off";
    private String mLastSpeedMsg = GPS_LOCATION_TRACKING_OFF;
    private long mLastSpeedUpdateTime = 0;
    private long mLastDrivingStateChange = 0;

    private GpsDrivingState getDrivingState(float speed) {
        if (speed < 0) {
            return GpsDrivingState.UNKNOWN;
        }
        else if (speed >= AppSetting.DRIVING_LOCK_MPH_THRESHOLD.getFloat()) {
            return GpsDrivingState.DRIVING;
        }
        else {
            return GpsDrivingState.NOT_DRIVING;
        }
    }

    public GpsDrivingState getDrivingState() {
        if (hasCurrentGpsSpeed()) {
            return mConfirmedDrivingState;
        }
        return GpsDrivingState.UNKNOWN;
    }

    private void updateLocationAndSpeed(Location location) {
        mUpdateSeqNum++;

        if (speedTracking) {
            if (mProvider.equalsIgnoreCase(LocationManager.GPS_PROVIDER)) {
                String indicator = "provided";
                if (location.hasSpeed()) {
                    mLastSpeed = convertMetersPerSecToMph(location.getSpeed());
                } else {
                    indicator = "calculated";
                    if (lastLocation == null) {
                        mLastSpeed = -1;
                    } else {
                        float d = (float) distance(lastLocation, location);
                        float mph = convertMetersPerSecToMph(d / ((location.getTime() - lastLocation.getTime()) / 1000));

                        // Speed calculated based on latitude and longitude is less accurate than speed
                        // provided by location.getspeed()--especially at low speeds--and will often
                        // show speed even when still.  To avoid that, we treat anything < 3 MPH as zero.
                        mLastSpeed = (mph < 3.0) ? 0 : mph;
                    }
                }
                mLastSpeedUpdateTime = System.currentTimeMillis();
                if (getDrivingState(mLastSpeed) != mLastDrivingState) {
                    //Log.d(TAG, String.format("mLastDrivingState %s->%s", mLastDrivingState.toString(), getDrivingState(mLastSpeed).toString()));
                    mLastDrivingStateChange = mLastSpeedUpdateTime;
                    mLastDrivingState = getDrivingState(mLastSpeed);
                } else if (mLastDrivingState != mConfirmedDrivingState && (mLastSpeedUpdateTime - mLastDrivingStateChange) > 2000) {
                    Log.d(TAG, String.format("DRIVING_LOCK_DEBUG: mConfirmedDrivingState %s->%s", mConfirmedDrivingState.toString(), mLastDrivingState.toString()));

                    mConfirmedDrivingState = mLastDrivingState;
                    DrivingModeStateMachine.processEvent(DrivingModeStateMachine.DrivingEvent.NEW_GPS_STATE);
                }
                mLastSpeedMsg = String.format("%s-%s speed: %1.1f mph", mProvider.toUpperCase(), indicator, mLastSpeed);
            } else {
                mLastSpeedMsg = "mProvider is not a GPS provider";
                mLastSpeed = -1;
            }
            DetectedActivitiesIntentService.refreshDetectedActivityIndicators();
        }
        else {
            mLastSpeedMsg = "speedTracking is OFF";
        }
        // Note: lastLocation must be updated *after* speed calculations.
        //log.debug(Logs.DEBUG, String.format("JUNK: location: %f,%f %s", location.getLatitude(), location.getLongitude(), mLastSpeedMsg));
        this.lastLocation = location;
    }

    /**
     * Calculate distance between two points in latitude and longitude taking
     * into account height difference. If you are not interested in height
     * difference pass 0.0. Uses Haversine method as its base.
     *
     * lat1, lon1 Start point lat2, lon2 End point el1 Start altitude in meters
     * el2 End altitude in meters
     * @returns Distance in Meters
     */

    private static final int RADIUS_OF_EARTH_IN_METERS = 6371;

    private double distance(double lat1, double lon1, double el1, double lat2,
                                  double lon2, double el2) {

        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = RADIUS_OF_EARTH_IN_METERS * c * 1000; // convert to meters

        double height = el1 - el2;

        distance = Math.pow(distance, 2) + Math.pow(height, 2);

        return Math.sqrt(distance);
    }
    public double distanceInFeet(double lat1, double lon1, double lat2, double lon2) {
        return distance(lat1, lon1, 0,  lat2, lon2, 0) * FEET_PER_METER;
    }

    private double distance (Location l1, Location l2) {
        return distance(l1.getLatitude(), l1.getLongitude(), 0,  l2.getLatitude(), l2.getLongitude(), 0);
    }

    private static final float FEET_PER_METER = (float)3.28084;

    private static final float METERS_PER_SEC_TO_MPH_FACTOR = (float)(FEET_PER_METER * 3600) / (float)5280;

    private float convertMetersPerSecToMph(float metersPerSec) {
        return metersPerSec * METERS_PER_SEC_TO_MPH_FACTOR;
    }

    public Location getLocation() {
        if (this.locationTrackingEnabled && this.lastLocation != null) {
            return this.lastLocation;
        } else {
            if (mProvider == null) {
                return this.manager.getLastKnownLocation(this.manager.getBestProvider(new Criteria(), true));
            }
            else {
                return this.manager.getLastKnownLocation(mProvider);
            }
        }
    }

    public float getSpeed() {
        if (lastLocation != null && (System.currentTimeMillis() - mLastSpeedUpdateTime) < 10000) {
            if (mLastSpeed == Float.NEGATIVE_INFINITY || mLastSpeed == Float.POSITIVE_INFINITY
                || mLastSpeed == Float.NaN) {
                    return -1;
            }
            return mLastSpeed;
        }
        else {
            return -1;
        }
    }

    public boolean hasCurrentGpsSpeed() {
        return this.getSpeed() >= 0;
    }

    public boolean gpsSpeedIndicatesDriving() {
        return this.getSpeed() > AppSetting.DRIVING_LOCK_MPH_THRESHOLD.getFloat();
    }

    public String getLastSpeedMsg() {
        if (mLastSpeedMsg.isEmpty()) {
            return ("");
        }
        return String.format("%03d: %s", mUpdateSeqNum, mLastSpeedMsg);
    }

    public void startLocationTracking(String provider) {
        if (provider == null) {
            mProvider = this.manager.getBestProvider(new Criteria(), true);
        }
        else {
            mProvider = provider;
        }
        Log.d(TAG, String.format("DRIVING_LOCK_DEBUG: startLocationTracking %s", mProvider));

        locationTrackingEnabled = true;
        this.manager.requestLocationUpdates(mProvider, 0, 0, this.listener);
    }

    public void startLocationTracking() {
        //startLocationTracking(mProvider);
        // Hard-code GPS provider, since it's the only one that provides accurate speed.

        startLocationTracking(LocationManager.GPS_PROVIDER);
    }

    public void stopLocationTracking() {
        Log.d(TAG, "DRIVING_LOCK_DEBUG: stopLocationTracking()");
        this.manager.removeUpdates(this.listener);

        mLastDrivingState = GpsDrivingState.UNKNOWN;
        mConfirmedDrivingState = GpsDrivingState.UNKNOWN;

        this.mLastSpeed = -1;
        this.mLastSpeedMsg = GPS_LOCATION_TRACKING_OFF;
        this.mLastSpeedUpdateTime = 0;
        this.mLastDrivingStateChange = 0;
        this.locationTrackingEnabled = false;
    }

    public boolean isLocationTrackingEnabled() {
        return this.locationTrackingEnabled;
    }

    public boolean isGpsProviderEnabled() {
        return manager.isProviderEnabled(LocationManager.GPS_PROVIDER);
    }
}
