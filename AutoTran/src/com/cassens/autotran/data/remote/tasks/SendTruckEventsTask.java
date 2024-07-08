package com.cassens.autotran.data.remote.tasks;

import android.content.Context;
import android.location.Location;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.GlobalState;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.VIN;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.handlers.DrivingLocationHandler;
import com.cassens.autotran.handlers.LocationHandler;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.cassens.autotran.hardware.PiccoloManager;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class SendTruckEventsTask {
    private static final Logger log = LoggerFactory.getLogger(SendTruckEventsTask.class.getSimpleName());

    private static final String VINSLOADED_EVENT_TAG = "VINSLOADED";
    private static final String TRUCKLOC_EVENT_TAG = "TRUCKLOC";
    private static final String TRUCKLOC2_EVENT_TAG = "TRUCKLOC2";
    private static final int TRUCKLOC_EVENT_VERSION = 8;
    private static final String GPS_SOURCE_DEVICE= "D";
    private static final String GPS_SOURCE_TRUCK = "T";

    private static final Object sLock = new Object();
    private static LocationData sPriorBestLocation = null;
    private static int sLocationUnchangedCount = 0;
    private static int sLocationOldCount = 0;

    public void run(Context context) {
        if (okayToRecordEvents(context)) {
            getBestLocationData(context);
            if (bestLocation != null) {
                sendLoadEventsIfLocationNewerAndChanged(context);
            }
            PiccoloManager.detectUsbPermissionState(context);
            CommonUtility.uploadLogThreadStartStop("Completed " + SendTruckEventsTask.class.getSimpleName(), false);
        }
        runDailyTaskIfNeeded(context);
    }

    private boolean okayToRecordEvents(Context context) {
        if (!PiccoloManager.isPlugged(context)) {
            long lastUndockedTime = PreferenceManager.getDefaultSharedPreferences(context)
                    .getLong(Constants.PREF_PICCOLO_UNDOCKED_TIME, 0);
            long currentTime = System.currentTimeMillis();
            long minutesSinceUndock = TimeUnit.MINUTES.convert(Math.abs(currentTime - lastUndockedTime), TimeUnit.MILLISECONDS);
            if (minutesSinceUndock >= (long) AppSetting.TRUCK_EVENTS_UNDOCK_TIMEOUT.getInt()) {
                log.debug(Logs.DEBUG, "TEMP_LOC: Unit has been undocked for > " + AppSetting.TRUCK_EVENTS_UNDOCK_TIMEOUT.getInt() + " minutes. No events recorded");
                return false;
            }
        }
        return true;
    }

    private void sendLoadEventsIfLocationNewerAndChanged(Context context) {
        boolean saveLoadEvent = false;
        int locationUnchangedCount = 0;
        int locationOldCount = 0;
        synchronized (sLock) {
            if (sPriorBestLocation != null && !bestLocation.laterThan(sPriorBestLocation)) {
                CommonUtility.uploadLogMessage(String.format(
                        "TRUCK_EVENT_DEBUG: Skipping truck event: Current GPS timestamp (%s) older than current best timestamp (%s):",
                        bestLocation.getTimeStamp(), sPriorBestLocation.getTimeStamp()));
                sLocationOldCount++;
                return;
            }
            if (sPriorBestLocation == null ||
                    DrivingLocationHandler.getInstance(context).distanceInFeet(
                            sPriorBestLocation.lat, sPriorBestLocation.lon,
                            bestLocation.lat, bestLocation.lon
                    ) > Double.valueOf(AppSetting.TRUCK_EVENTS_DISTANCE_THRESHOLD.getInt())) {
                sPriorBestLocation = bestLocation;
                saveLoadEvent = true;
                // save sLocationUnchangedCount in a local variable before resting it to
                // zero because we'll need to pass that value to metDistanceThreshold outside
                // the synchronized block.
                locationUnchangedCount = sLocationUnchangedCount;
                sLocationUnchangedCount = 0;
            } else {
                sLocationUnchangedCount++;
            }

            if (bestLocation.isOld()) {
                // We wait to check for old location here because even if the location is old we
                // know it's newer than the prior location, since we checked that earlier.
                sLocationOldCount++;
                String msgTag;
                if (saveLoadEvent) {
                    msgTag = "Warning";
                    locationOldCount = sLocationOldCount;
                    sLocationOldCount = 0;
                } else {
                    msgTag = "Skipping Load Event";
                    sLocationOldCount++;
                }
                CommonUtility.uploadLogMessage(String.format(
                        "TRUCK_EVENT_DEBUG: %s: New GPS location is older than %d minutes",
                        msgTag, AppSetting.GPS_MAX_AGE_MINUTES.getInt()));
            }
        }
        if (saveLoadEvent) {
            sendLoadEvents(context, locationUnchangedCount, locationOldCount);
        }
        else {
            CommonUtility.uploadLogMessage(String.format(
                    "TRUCK_EVENT_DEBUG: Skipping truck event: Distance from last sent event: %f feet",
                    DrivingLocationHandler.getInstance(context).distanceInFeet(
                            sPriorBestLocation.lat, sPriorBestLocation.lon,
                            bestLocation.lat, bestLocation.lon
                    )));
        }
    }

    public static class LocationData {
        private String source = GPS_SOURCE_DEVICE;
        private double lat = 0.0;
        private double lon = 0.0;
        private long timeStampMillis = -1;
        private double speed = 0.0;

        LocationData(String source, double lat, double lon, long timeStampMillis, double speed) {
            this.source = source;
            this.lat = lat;
            this.lon = lon;
            this.timeStampMillis = timeStampMillis;
            this.speed = speed;
        }

        public LocationData() {}

        public String getTimeStamp() {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
            return sdf.format(new Date(this.timeStampMillis));
        }

        public long getTimeStampMillis() {
            return this.timeStampMillis;
        }

        public boolean laterThan(LocationData otherLocation) {
            return this.timeStampMillis > otherLocation.timeStampMillis;
        }

        public double getAgeInSeconds(long now) {
            if (this.timeStampMillis < 0) {
                return -1.0;
            }
            return ((double)(now - this.timeStampMillis)) / 1000.0;
        }

        public double getAgeInSeconds() {
            return getAgeInSeconds(System.currentTimeMillis());
        }

        public boolean isOld() {
            double ageInSeconds = this.getAgeInSeconds();
            int maxAge = AppSetting.GPS_MAX_AGE_MINUTES.getInt();
            return this.getAgeInSeconds() / 60 > AppSetting.GPS_MAX_AGE_MINUTES.getInt();
        }

        public String getSource() {
            return source;
        }

        public double getLat() {
            return lat;
        }

        public double getLon() {
            return lon;
        }

        public double getSpeed() {
            return speed;
        }
    }

    public static LocationData chooseBestLocation(Location loc) {
        LocationData bestLocation;
        synchronized (sLock) {
            if (sPriorBestLocation != null && sPriorBestLocation.getTimeStampMillis() > loc.getTime()) {
                bestLocation = new LocationData(sPriorBestLocation.source,
                        sPriorBestLocation.getLat(),
                        sPriorBestLocation.getLon(),
                        sPriorBestLocation.getTimeStampMillis(),
                        sPriorBestLocation.getSpeed());
            }
            else {
                bestLocation = new LocationData(GPS_SOURCE_DEVICE, loc.getLatitude(), loc.getLongitude(), loc.getTime(), -1);
            }
        }
        if (bestLocation.isOld()) {
            bestLocation = new LocationData();
        }
        return bestLocation;
    }

    LocationData bestLocation = null;
    LocationData secondBestLocation = null;

    private void getBestLocationData(Context context) {
        int tryPiccoloSeconds = AppSetting.TRUCK_EVENTS_GPS_AGE_THRESHOLD.getInt();
        LocationData devLocation;
        LocationData piccoloLocation = null;
        Location loc;

        bestLocation = null;
        secondBestLocation = null;

        LocationHandler locationHandler = LocationHandler.getInstance(context);

        locationHandler.startLocationTracking(Looper.getMainLooper());
        try {
            Thread.sleep(15000); // Sleep to give GPS time to get a solid reading
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        loc = locationHandler.getLocation();
        devLocation = new LocationData(
                GPS_SOURCE_DEVICE,
                loc.getLatitude(),
                loc.getLongitude(),
                loc.getTime(),
                DrivingLocationHandler.getInstance(context).getSpeed());

        locationHandler.stopLocationTracking();
        long now = System.currentTimeMillis();
        double dAge = devLocation.getAgeInSeconds(now);

        if ((tryPiccoloSeconds <= 0 || dAge > (long)tryPiccoloSeconds) && PiccoloManager.isDocked()) {
            /*
             TODO: Enhance requestPositionInfo to accept a callback and timeout
             */
            PiccoloManager.requestGpsPosition(context);
            try {
                Thread.sleep(2000);
            } catch (InterruptedException e) {
                log.debug(Logs.DEBUG, "Sleep failed after requesting location from Piccolo");
                e.printStackTrace();
            }
            if (PiccoloManager.getLatitude() != 0.0 && PiccoloManager.getLongitude() != 0.0) {
                long piccoloTimeStampMillis = -1;
                String piccoloTimeStamp = PiccoloManager.getTimeStamp();
                if (!piccoloTimeStamp.isEmpty()) {
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss z");
                    try {
                        piccoloTimeStampMillis = sdf.parse(piccoloTimeStamp + " UTC").getTime();
                    } catch (ParseException ex) {
                        log.debug(Logs.DEBUG, "Got invalid timestamp from Piccolo: " + piccoloTimeStamp);
                    }
                }
                piccoloLocation = new LocationData(
                        GPS_SOURCE_TRUCK,
                        PiccoloManager.getLatitude(),
                        PiccoloManager.getLongitude(),
                        piccoloTimeStampMillis,
                        PiccoloManager.getTruckSpeed());
            }
        }

        if (piccoloLocation == null) {
            bestLocation = devLocation;
            secondBestLocation = null;
        }
        else if (tryPiccoloSeconds < 0) {
            bestLocation = piccoloLocation;
            secondBestLocation = devLocation;
        }
        else {
            double devAge = devLocation.getAgeInSeconds(now);
            double piccoloAge = piccoloLocation.getAgeInSeconds(now);
            if (piccoloAge < 0.0 || devAge <= piccoloAge) {
                bestLocation = devLocation;
                secondBestLocation = piccoloLocation;
            }
            else {
                bestLocation = piccoloLocation;
                secondBestLocation = devLocation;
            }
        }
    }

    private String buildMsg(LocationData l, long now) {
        return String.format("%s: %3.3f,%3.3f a=%3.3f ts='%s'",
                l.source, l.lat, l.lon, l.getAgeInSeconds(now), l.getTimeStamp());
    }

    private void sendLoadEvents(Context context, int locUnchangedCount, int oldLocationCount) {
        SimpleTimeStamp sts = new SimpleTimeStamp();
        String driverNumber = CommonUtility.getDriverNumber(context);
        if (driverNumber.isEmpty()) {
            log.debug(Logs.DEBUG, "Driver number not set");
            return;
        }
        User driver = DataManager.getUserForDriverNumber(context, driverNumber);
        if (driver == null) {
            log.debug(Logs.DEBUG, "Could not read driver record for driver " + driverNumber);
            return;
        }
        String currentTruckID = TruckNumberHandler.getTruckNumber(context);
        String piccoloTruckID = TruckNumberHandler.getPiccoloTruckNumber(context, true);

        Calendar loadAutoHideDate = Calendar.getInstance();
        loadAutoHideDate.add(Calendar.DAY_OF_YEAR, -AppSetting.LOAD_AUTO_HIDE_DAYS.getInt());

        //log.debug(Logs.DEBUG, "LOAD_AUTO_HIDE_DAYS=" + AppSetting.LOAD_AUTO_HIDE_DAYS.getInt());

        // Send VINSLOADED event if VINs are on truck
        List<Load> loadList = DataManager.getAllLoads(context, driver.user_id, false, -1);

        String timeStamp = sts.getUtcDateTime();
        String timeZone = sts.getUtcTimeZone();

        for (Load load : loadList) {
            String loadNumber = "";
            String vinsOnTruck = "";
            boolean hideLoad = false;

            if (HelperFuncs.isNullOrEmpty(load.driverPreLoadSignature)) {
                hideLoad = load.lastUpdated.before(loadAutoHideDate.getTime());
            }
            else {
                List<Delivery> deliveries = load.deliveries;
                for (Delivery delivery : deliveries) {

                    delivery.load = load;

                    if (delivery.deliveryIsPending()) {
                        if (load.lastUpdated.before(loadAutoHideDate.getTime())) {
                            hideLoad = true;
                            break;
                        }
                        if (loadNumber.equals("")) {
                            loadNumber = load.loadNumber;
                        }

                        List<VIN> vins = delivery.getVinList();
                        for (VIN vin : vins) {
                            if (!vinsOnTruck.equals("")) {
                                vinsOnTruck += "|" + vin.vin_number;
                            } else if (vin != null && vin.vin_number != null) {
                                vinsOnTruck = vin.vin_number;
                            }
                        }
                    }
                }
            }
            if (hideLoad) {
                CommonUtility.highLevelLog(
                        String.format("Incomplete load $loadNumber is more than %d days old. Hiding load.",
                                AppSetting.LOAD_AUTO_HIDE_DAYS.getInt()), load, null
                );
                if (DataManager.updateLoadDriverId(context, load.load_id, 0) != 1) {
                    CommonUtility.highLevelLog("Error: Unable to hide load $loadNumber", load, null);
                }
                continue;
            }

            if (vinsOnTruck != null && !vinsOnTruck.equals("")) {
                String vinMessageString = TextUtils.join(",",
                        new String[]{
                                VINSLOADED_EVENT_TAG,
                                driverNumber,
                                currentTruckID,
                                loadNumber,
                                vinsOnTruck,
                                String.valueOf(bestLocation.lat),
                                String.valueOf(bestLocation.lon),
                                timeStamp,
                                timeZone
                        });
                LoadEvent event = new LoadEvent();
                event.csv = vinMessageString;
                DataManager.insertLoadEvent(context, event);
                //log.debug(Logs.DEBUG, "VINSLOADED event inserted: " + sts.getDateTime());
            }
        }

        long now = System.currentTimeMillis();
        log.debug(Logs.PICCOLO_IO, "TRUCKLOC GPS data: " + buildMsg(bestLocation, now));
        createTruckLocEvent(context, TRUCKLOC_EVENT_TAG,
                driverNumber,
                currentTruckID,
                piccoloTruckID,
                timeStamp,
                timeZone,
                bestLocation,
                now,
                locUnchangedCount,
                oldLocationCount);

        if (secondBestLocation != null && AppSetting.TRUCK_EVENTS_SEND_TRUCKLOC2.getBoolean()) {
            log.debug(Logs.PICCOLO_IO, "TRUCKLOC2 GPS data: " + buildMsg(secondBestLocation, now));
            createTruckLocEvent(context, TRUCKLOC2_EVENT_TAG,
                    driverNumber,
                    currentTruckID,
                    piccoloTruckID,
                    timeStamp,
                    timeZone,
                    secondBestLocation,
                    now,
                    locUnchangedCount,
                    oldLocationCount);
        }
        if (CommonUtility.isConnected(context)) {
            CommonUtility.uploadLogMessage("TRUCK_EVENT_DEBUG: Starting pushLoadEventsLatched() from SendVinsOnTruck()");
            SyncManager.pushLoadEventsLatched(context);
        }
        else {
            CommonUtility.uploadLogMessage("TRUCK_EVENT_DEBUG: Not sending event(s) to server. No internet connection");
        }
    }

    private void createTruckLocEvent(Context context, String tag, String driverNumber,
                                     String currentTruckID,
                                     String piccoloTruckID,
                                     String timeStamp,
                                     String timeZone,
                                     LocationData loc,
                                     long now,
                                     int locUnchangedCount,
                                     int oldLocationCount) {
        // Send TRUCKLOC event
        String locMessageString = TextUtils.join(",",
                new String[] {
                        tag,
                        Integer.toString(TRUCKLOC_EVENT_VERSION),
                        driverNumber,
                        currentTruckID,
                        piccoloTruckID,
                        String.valueOf(loc.lat),
                        String.valueOf(loc.lon),
                        timeStamp,
                        timeZone,
                        GlobalState.getLastStartedLoadNum(),
                        GlobalState.getLastStartedLoadAssignedTruck(),
                        loc.source,
                        String.format("%3.3f", loc.getAgeInSeconds(now)),
                        String.format("%3.1f", loc.speed),
                        loc.getTimeStamp(),
                        String.valueOf(locUnchangedCount),
                        String.valueOf(oldLocationCount)
                });

        LoadEvent event = new LoadEvent();
        event.csv = locMessageString;
        DataManager.insertLoadEvent(context, event);
    }

    private void runDailyTaskIfNeeded(Context context) {
        //log.debug(Logs.DEBUG, String.format("DAILY_TASK: runDailyTaskIfNeeded minutesSinceLastInteraction=%.2f", AutoTranApplication.minutesSinceLastInteraction()));
        //float minutesSinceLast = (float)(System.currentTimeMillis() - HelperFuncs.getLongPref(context, "LAST_DAILY_TASK_TIME", (long)0)) / (float)(1000 * 60);
        //log.debug(Logs.DEBUG, String.format("DAILY_TASK: runDailyTaskIfNeeded minutesSinceLastTask=%.2f", minutesSinceLast));

        if (PiccoloManager.isPlugged(context) &&
            AutoTranApplication.minutesSinceLastInteraction() > AppSetting.DAILY_TASK_INACTIVE_THRESHOLD.getFloat() &&
            ((System.currentTimeMillis() - HelperFuncs.getLongPref(context, "LAST_DAILY_TASK_TIME", (long)0))
                > (1000 * 60 * 60 * AppSetting.DAILY_TASK_INTERVAL_HOURS.getInt())))
        {
            HelperFuncs.setLongPref(context, "LAST_DAILY_TASK_TIME", System.currentTimeMillis());
            log.debug(Logs.BACKEND_POC, "DAILY_TASK: Running daily task");
            new DailyBackgroundTask(context).execute();
        }
    }
}
