package com.cassens.autotran.data.remote.tasks;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.text.TextUtils;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.handlers.LocationHandler;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;

public class SendPiccoloDockedTask extends AsyncTask<Void, Void, Void> {

    public static final Logger log = LoggerFactory.getLogger(SendPiccoloDockedTask.class.getSimpleName());

    private Context context;
    private LocationHandler locationHandler;
    private SimpleTimeStamp sts;

    public SendPiccoloDockedTask(Context context) {
        this.context = context;
        locationHandler = LocationHandler.getInstance(context);
        sts = new SimpleTimeStamp();
    }

    /* Can't start and stop location tracking in a thread because it's not currently thread safe.
    @Override
    protected void onPreExecute() {
        super.onPreExecute();
        locationHandler.startLocationTracking();
    }
     */

    @Override
    protected Void doInBackground(Void... params) {
        if(context != null) {
            CommonUtility.dispatchUploadLogThreadStartStop("Started SendPiccoloDockedTask", true);
            String driverNumber = CommonUtility.getDriverNumber(context);
            User driver = DataManager.getUserForDriverNumber(context, driverNumber);

            if (driver != null && driverNumber != null && driverNumber.length() > 0) {
                CommonUtility.dispatchUploadLog("Starting pushLoadEvents() in SendPiccoloDockedTask");

                String currentDriverID = CommonUtility.getDriverNumber(context);
                String currentTruckID = TruckNumberHandler.getTruckNumber(context);
                Location location = locationHandler.getLocation();
                Calendar calendar = Calendar.getInstance();
                boolean isTruckNumSourceManual = TruckNumberHandler.isTruckNumberSourceManual(context);

                String piccoloMessageString = TextUtils.join(",",
                        new String[] {
                                "DOCKED",
                                currentDriverID,
                                currentTruckID,
                                (isTruckNumSourceManual ? "Manual" : "Piccolo"),
                                String.valueOf(location.getLatitude()),
                                String.valueOf(location.getLongitude()),
                                sts.getUtcDateTime(),
                                sts.getUtcTimeZone()
                        });

                LoadEvent event = new LoadEvent();
                event.csv = piccoloMessageString;
                /* Don't send DOCKED event. Same info is sent with TRUCKLOC event
                DataManager.insertLoadEvent(context, event);

                if (CommonUtility.isConnected(context)) {
                    SyncManager.pushLoadEventsLatched(context);
                } */
            }
            CommonUtility.dispatchUploadLogThreadStartStop("Completed SendPiccoloDockedTask", false);
        } else {
            log.error(Logs.DEBUG, "Error sending DOCKED event");
        }
        return null;
    }

    /* Can't start and stop location tracking in a thread because it's not currently thread safe.
    @Override
    protected void onPostExecute(Void nothing) {
        super.onPostExecute(nothing);

        locationHandler.stopLocationTracking();
    }
    */
}
