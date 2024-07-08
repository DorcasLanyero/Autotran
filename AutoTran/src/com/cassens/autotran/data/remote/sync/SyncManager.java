package com.cassens.autotran.data.remote.sync;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.util.Log;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.GeneratePresignedUrlRequest;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.activities.DashboardActivity;
import com.cassens.autotran.backendpoc.PoCUtils;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.constants.URLS;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.EventBusManager;
import com.cassens.autotran.data.event.DriverActionEvent;
import com.cassens.autotran.data.event.S3UploadEvent;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.DriverAction;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Inspection;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.PlantReturn;
import com.cassens.autotran.data.model.ProblemReport;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.YardExit;
import com.cassens.autotran.data.model.YardInventory;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.data.remote.CallWebServices;
import com.cassens.autotran.data.remote.GsonTypeAdapters;
import com.cassens.autotran.data.remote.workers.HttpCallWorker;
import com.cassens.autotran.data.remote.tasks.GetDriverLoadsTask;
import com.cassens.autotran.data.remote.tasks.GetSupervisorUsersTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.sdgsystems.android.amazon.s3transfer.S3BucketAuth;
import com.sdgsystems.android.amazon.s3transfer.Util;
import com.sdgsystems.android.amazon.s3transfer.models.S3Container;
import com.sdgsystems.android.amazon.s3transfer.network.TransferController;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;


public class SyncManager {
    private static final Logger log = LoggerFactory.getLogger(SyncManager.class.getSimpleName());

    public static final String PENDING_UPLOADS_CHANGE = "com.cassens.autotran.data.remote.sync.PENDING_UPLOADS_CHANGE";
    public static final String UPLOAD_OBJECT = "com.cassens.autotran.data.remote.sync.UPLOAD_OBJECT";
    public static final String UPLOAD_CHANGE = "com.cassens.autotran.data.remote.sync.UPLOAD_CHANGE";

    private static final String TAG = "SyncManager";

    private static final Object mLock = new Object();


    private static Handler mHandler;

    private static boolean DEBUG = true;

    private static PowerManager.WakeLock uploadWakelock;
    private static Uri sound = null;//RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);;
    public static void pullRemoteData(DashboardActivity activity, String driverNumber) {

        //Remote data load

        //pull down everything for a driver

        NetworkInfo info = ((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

        if (info != null && info.isConnectedOrConnecting()) {
            CommonUtility.dispatchLogMessage( "SyncManager.pullRemoteData: Executing GetDriverLoadsTask");
            new GetDriverLoadsTask(activity, driverNumber, activity).execute();
            CommonUtility.dispatchLogMessage("SyncManager.pullRemoteData: Executing GetSupervisorUsersTask()");
            new GetSupervisorUsersTask(activity).execute();
        } else {
            CommonUtility.showText("Not refreshing data due to lack of connectivity");
        }
    }


    private static boolean mPushLocalDataRunning = false;
    private static final Object mPushLocalDataRunningLock = new Object();

    /*
     * Push each load / delivery / delivery_vin / damage / image record
     *
     * When force is set to true, we should ignore the current state of any loads that are not FULLY uploaded
     *
     * If completionEvent is not null, this will send CompletionEvent on the DRIVER_ACTIONS event bus queue
     * with the result set to the result of the S3 photo upload.
     */
    public static void pushLocalDataToRemoteServer(final Context context, final int driver_number, final boolean force) {
        pushLocalDataToRemoteServer(context, driver_number, force, null);
    }

    /*
     * Push each load / delivery / delivery_vin / damage / image record
     *
     * When force is set to true, we should ignore the current state of any loads that are not FULLY uploaded
     *
     * If completionEvent is not null, this will send CompletionEvent on the DRIVER_ACTIONS event bus queue
     * with the result set to the result of the S3 photo upload.
     */
    public static void pushLocalDataToRemoteServer(final Context context, final int driver_number, final boolean force, @Nullable final DriverActionEvent completionEvent) {

        Thread thread = new Thread() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                CommonUtility.uploadLogThreadStartStop("Started pushLocalDataToRemoteServer thread", true);

                synchronized (mPushLocalDataRunningLock) {
                    if (mPushLocalDataRunning) {
                        log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_0: pushLocalDataToRemoteServer ABORTED (already running)");
                        CommonUtility.uploadLogMessage("pushLocalDataToRemoteServer(): Thread aborted. Another thread already running");
                        CommonUtility.uploadLogThreadStartStop("Completed pushLocalDataToRemoteServer thread", false);
                        return;
                    } else {
                        CommonUtility.uploadLogMessage("pushLocalDataToRemoteServer(): Pushing local data");
                        mPushLocalDataRunning = true;
                    }
                }

                if (Looper.myLooper() == null) {
                    Looper.prepare();
                }

                mHandler = new Handler();

                establishWakelock(context);

                try {
                    int driver_id = driver_number;

                    if (driver_id != -1 && driver_id > 100) {

                        User user = DataManager.getUserForDriverNumber(context, String.valueOf(driver_id));

                        if (user == null) {
                            CommonUtility.uploadLogMessage("Current driver is not available, can't upload");
                            CommonUtility.uploadLogThreadStartStop("Completed pushLocalDataToRemoteServer thread", false);
                            return;
                        }

                        driver_id = user.user_id;
                    }

                    //The full push should be able to push load events without being latched since it's already latched...
                    CommonUtility.uploadLogMessage("Starting pushLoadEvents() in pushLocalDataToRemoteServer()");
                    pushLoadEvents(context);
                    CommonUtility.uploadLogMessage("Completed pushLoadEvents() in pushLocalDataToRemoteServer()");

                    CommonUtility.uploadLogMessage("Starting getAllLoads() in pushLocalDataToRemoteServer()");
                    List<Load> allLoads = DataManager.getAllLoads(context, driver_id, true, -1);
                    CommonUtility.uploadLogMessage("Completed getAllLoads() in pushLocalDataToRemoteServer()");
                    log.debug(Logs.DEBUG, "Pulled " + allLoads.size() + " loads from the db");
                    List<Load> uploadLoads = new ArrayList<>();
                    for (Load l : allLoads) {
                        if (!HelperFuncs.isNullOrEmpty(l.driverPreLoadSignature)) {
                            uploadLoads.add(l);
                        }
                    }

                    log.debug(Logs.UPLOAD, uploadLoads.size() + " loads available to push");
                    pushLoads(context, uploadLoads, force, driver_id);

                    List<Delivery> allDeliveries = new ArrayList<Delivery>();
                    for (Load load : uploadLoads) {
                        for (Delivery d : load.deliveries) {
                            if (
                                    (!HelperFuncs.isNullOrEmpty(d.dealerSignature) || load.shuttleLoad)
                                            && !HelperFuncs.isNullOrEmpty(d.driverSignature)) {
                                allDeliveries.add(d);
                            }
                        }
                    }

                    log.debug(Logs.UPLOAD, allDeliveries.size() + " deliveries available to push");
                    pushDeliveries(context, allDeliveries, force, driver_id);

                    pushTrainingRequirements(context);

                    //We aren't manually uploading image objects since they need to go with their containers now

                    pushYardInfo(context);

                    pushCompletedDriverActions(context, driver_number);

                    pushProblemReports(context);

                    if (completionEvent != null) {
                        EventBusManager.getInstance().listenForEvents(EventBusManager.Queue.NETWORK_REQUESTS, e -> {
                            if (e instanceof S3UploadEvent) {
                                S3UploadEvent s3e = (S3UploadEvent) e;
                                if (s3e.id == completionEvent.id) {
                                    completionEvent.result = s3e.result;
                                    EventBusManager.getInstance().publish(completionEvent);
                                    return true;
                                }
                            }

                            return false;
                        });
                    }

                    int requestId = completionEvent != null ? completionEvent.id : -1;

                    log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_0: pushLocalDataToRemoteServer calling sendNextPhoto()");
                    sendNextPhoto(context, requestId);

                    CommonUtility.uploadLogThreadStartStop("Completed pushLocalDataToRemoteServer thread", false);
                }
                finally {
                    synchronized (mPushLocalDataRunningLock) {
                        mPushLocalDataRunning = false;
                    }
                }
            }
        };

        if(CommonUtility.isConnected(context)) {
            thread.start();
        } else {
            log.debug(Logs.UPLOAD, "Not starting sync due to lack of connectivity");
        }
    }

    private static void pushProblemReports(Context context) {
        ArrayList<ProblemReport> reports = DataManager.getProblemReports(context);

        for(ProblemReport report : reports) {
            if(report.getUpload_status() != Constants.SYNC_STATUS_UPLOADED) {
                pushProblemReport(context, report);
            }
        }
    }

    public static void pushCompletedDriverActions(Context context, int driver_id) {
        log.debug(Logs.UPLOAD, "DRIVER_ACTION_SYNC - push");

        List<DriverAction> driverActions = DataManager.getDriverActions(context, String.valueOf(driver_id), true, null, false);


        log.debug(Logs.DEBUG, "DRIVER_ACTION_SYNC - entries: " + driverActions.size());
        for(DriverAction driverAction : driverActions) {
            driverAction.setUploadStatus(Constants.SYNC_STATUS_UPLOADING);

            log.debug(Logs.DEBUG, "DRIVER_ACTION_SYNC - calling json endpoint with " + driverAction.getAction() + " " + driverAction.getId());
            //log.debug(Logs.DEBUG, String.format("mark_driver_action: id=%d act='%s' s='%s', us=%d rcvd=%s, procd=%s", driverAction.getId(), HelperFuncs.noNull(driverAction.getAction(), "null"), HelperFuncs.noNull(driverAction.getStatus(), "null"), driverAction.getUploadStatus(), HelperFuncs.noNull(driverAction.getReceived(), "null"), HelperFuncs.noNull(driverAction.getProcessed(), "null")));

            // This call sets the status and processed columns for the the given driverAction
            // record on the server database. It does not update any other columns in the record.
            boolean started = HttpCallWorker.makeJsonRequest(context, URLS.mark_driver_action_status, driverAction, DriverAction.class, driverAction.getId());

            if(!started) {
                log.debug(Logs.UPLOAD, "Did NOT start upload");
                driverAction.setUploadStatus(Constants.SYNC_STATUS_NOT_UPLOADED);
            }
        }


    }

    private static void establishWakelock(Context context) {
        if (uploadWakelock == null || !uploadWakelock.isHeld()) {
            PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            uploadWakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "autotran_upload_wakelock");
            uploadWakelock.acquire();
        }
    }

    public static void pushYardInfo(Context context) {
        clearNotifications(context);


        List<YardInventory> yardInventories = DataManager.getYardInventoryList(context, true);
        pushYardInventories(context, yardInventories);

        /*
        //don't push yard info for now until we have it working on the remote server (since it breaks and chews bandwidth)
        List<YardExit> yardExits = DataManager.getYardExitList(context, true);
        List<PlantReturn> plantReturns = DataManager.getPlantReturnList(context, true);
        List<Inspection> inspections = DataManager.getInspectionList(context, true);
        List<ReceivedVehicle> receivedVehicles = DataManager.getReceivedVehicleList(context, true);


        pushInspections(context, inspections);
        pushYardExits(context, yardExits);
        pushPlantReturns(context, plantReturns);
        pushReceivedVehicles(context, receivedVehicles);
        */
    }

    public static void pushInspections(final Context context) {
        clearNotifications(context);

        establishWakelock(context);

        List<Inspection> inspections = DataManager.getInspectionList(context, true);
        for(Inspection inspection : inspections) {
            log.debug(Logs.UPLOAD, "Attempting upload of inspection for vin " + inspection.vin);
            pushInspection(context, inspection);
        }

    }

    private static void pushInspection(Context context, Inspection inspection) {
        Gson gson = new GsonBuilder()
            .registerTypeAdapter(Date.class, new GsonTypeAdapters.DateSerializer())
            .create();
        
        com.cassens.autotran.data.model.dto.Inspection payload = inspection.getDTO(context);
        String payloadJson = gson.toJson(payload, com.cassens.autotran.data.model.dto.Inspection.class);

        boolean started = HttpCallWorker.makeJsonRequest(context, URLS.save_inspection, payloadJson, Inspection.class, payload.getInspection_id());

        if(!started) {
            log.debug(Logs.UPLOAD, "Did NOT start upload for inspection");
        }
    }


    public static void handleUploadInspectionResponse(Context context, boolean success, int id, String result) {
        List<Inspection> inspections = DataManager.getInspectionList(context, false);
        Inspection inspection = null;
        for (Inspection i : inspections) {
            if (i.inspection_id == id) {
                inspection = i;
                break;
            }
        }

        if (inspection != null) {
            if (success) {
                inspection.uploadStatus = Constants.SYNC_STATUS_UPLOADED;
                DataManager.insertInspection(context, inspection);
                uploadSucceededNotify(context, Constants.UPLOAD_INSPECTION, inspection.inspection_id, -1);
            } else {
                CommonUtility.showText("Inspection Upload failed for inspection of " + inspection.vin);
                log.debug(Logs.UPLOAD, "Inspection Upload failed for inspection of " + inspection.vin);
                inspection.uploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED;
                DataManager.insertInspection(context, inspection);

                uploadFailedNotify(context, Constants.UPLOAD_INSPECTION, inspection.inspection_id, -1);
            }

        }
    }

    public static void handleUploadPlantReturn(Context context, boolean success, int id, String result) {
        List<PlantReturn> returns = DataManager.getPlantReturnList(context, false);
        PlantReturn plantReturn = null;
        for (PlantReturn p : returns) {
            if (p.plant_return_id == id) {
                plantReturn = p;
                break;
            }
        }

        if (success) {
            plantReturn.uploadStatus = Constants.SYNC_STATUS_UPLOADED;
            DataManager.insertPlantReturn(context, plantReturn);
            uploadSucceededNotify(context, Constants.UPLOAD_PLANT_RETURN, plantReturn.plant_return_id, -1);
        } else {
            CommonUtility.showText("Delivery Upload failed for receivedVehicle " + plantReturn.VIN);

            plantReturn.uploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED;
            DataManager.insertPlantReturn(context, plantReturn);

            uploadFailedNotify(context, Constants.UPLOAD_PLANT_RETURN, plantReturn.plant_return_id, -1);
        }
    }

    public static void handleUploadYardExit(Context context, boolean success, int id, String result) {
        YardExit yardExit = null;
        for (YardExit y : DataManager.getYardExitList(context, false)) {
            if (y.yard_exit_id == id) {
                yardExit = y;
                break;
            }
        }

        if (yardExit != null) {
            if (success) {
                yardExit.uploadStatus = Constants.SYNC_STATUS_UPLOADED;
                DataManager.insertYardExit(context, yardExit);
                uploadSucceededNotify(context, Constants.UPLOAD_YARD_EXIT, yardExit.yard_exit_id, -1);
            } else {
                CommonUtility.showText("Delivery Upload failed for receivedVehicle " + yardExit.VIN);

                yardExit.uploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED;
                DataManager.insertYardExit(context, yardExit);

                uploadFailedNotify(context, Constants.UPLOAD_YARD_EXIT, yardExit.yard_exit_id, -1);
            }

        }
    }

    private static void pushYardInventories(final Context context, List<YardInventory> yardInventories) {
        clearNotifications(context);

        for (final YardInventory yardInventory : yardInventories) {
            List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
            Bundle bundle = new Bundle();

            yardInventory.uploadStatus = Constants.SYNC_STATUS_UPLOADING;
            DataManager.insertYardInventory(context, yardInventory);
            LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(YardInventory.class.getName(), 1));

            com.cassens.autotran.data.model.dto.YardInventory yardInventoryDto = yardInventory.getDTO("ready");
            boolean started = HttpCallWorker.makeJsonRequest(context, URLS.POST_SAVE_YARD_INVENTORY, yardInventoryDto, YardInventory.class, yardInventory.yard_inventory_id);

            if(!started) {
                log.debug(Logs.DEBUG, "Did NOT start upload for yard inventory " + yardInventory.yard_inventory_id);
                yardInventory.uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
                DataManager.insertYardInventory(context, yardInventory);
            }
        }
    }

    public static void handleUploadYardInventoryResponse(Context context, boolean success, int id, String result) {
        YardInventory yardInventory = null;
        for (YardInventory y : DataManager.getYardInventoryList(context, false)) {
            if (y.yard_inventory_id == id) {
                yardInventory = y;
                break;
            }
        }

        if (yardInventory != null) {
            if (success) {
                if(yardInventory.delivery_vin_id == -1) {
                    DataManager.deleteYardInventory(context, id);
                } else {
                    yardInventory.uploadStatus = Constants.SYNC_STATUS_UPLOADED;
                    DataManager.insertYardInventory(context, yardInventory);
                }
                uploadSucceededNotify(context, Constants.UPLOAD_YARD_INVENTORY, yardInventory.yard_inventory_id, -1);
            } else {
                CommonUtility.showText("Delivery Upload failed for yard inventory of " + yardInventory.VIN);

                yardInventory.uploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED;
                DataManager.insertYardInventory(context, yardInventory);

                uploadFailedNotify(context, Constants.UPLOAD_YARD_INVENTORY, yardInventory.yard_inventory_id, -1);
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(YardInventory.class.getName(), -1));
        }
    }

    public static boolean pushingLoadEvents = false;

    public static void pushLoadEventsLatched(Context context) {
        //pushLoadEvents(context);

        //Refactoring this method to start a push for everything
        int driverNumber = -1;

        try {
            String driverNumberString = CommonUtility.getDriverNumber(context);
            if(!CommonUtility.isNullOrBlank(driverNumberString)) {
                driverNumber = Integer.parseInt(driverNumberString);
                CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from pushLoadEventsLatched()");
                pushLocalDataToRemoteServer(context, driverNumber, false);
            } else {
                CommonUtility.uploadLogMessage("Error parsing driver number '" + CommonUtility.getDriverNumber(context) + "'. Not pushing load events.");
            }
        } catch (NumberFormatException e) {
            log.error(Logs.EXCEPTIONS, "Error parsing driver number '" + CommonUtility.getDriverNumber(context) + "'. Not pushing load events.", e);
            CommonUtility.uploadLogMessage("Got NumberFormatException on driver " + CommonUtility.getDriverNumber(context) + "'. Not pushing load events.");
        }
    }

    private static void pushLoadEvents(final Context context) {
        if(!pushingLoadEvents) {
            pushingLoadEvents = true;

            final List<LoadEvent> loadEvents = DataManager.getLoadEventList(context, true);

            if (loadEvents.size() > 0) {
                clearNotifications(context);

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                        LoadEvent firstTruckLocEvent = null;
                        LoadEvent lastTruckLocEvent = null;
                        int numTruckLocEvents = 0;
                        int numVinsLoadedEvents = 0;

                        for (final LoadEvent loadEvent : loadEvents) {
                            loadEvent.uploadStatus = Constants.SYNC_STATUS_UPLOADING;
                            DataManager.insertLoadEvent(context, loadEvent);
                            LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(LoadEvent.class.getName(), 1));

                            com.cassens.autotran.data.model.dto.LoadEvent loadEventDto = loadEvent.getDTO("ready");

                            boolean started = HttpCallWorker.makeJsonRequest(context, URLS.POST_SAVE_LOAD_EVENT, loadEventDto, LoadEvent.class, loadEvent.load_event_id);

                            if (!started) {
                                CommonUtility.uploadLogMessage("Could not start upload for loadEvent " + loadEvent.load_event_id);
                                loadEvent.uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
                                DataManager.insertLoadEvent(context, loadEvent);
                            }

                            if (loadEvent.csv.contains("TRUCKLOC")) {
                                numTruckLocEvents++;
                                if (numTruckLocEvents == 1) {
                                    firstTruckLocEvent = loadEvent;
                                } else {
                                    lastTruckLocEvent = loadEvent;
                                }
                            }
                            else if (loadEvent.csv.contains("VINSLOADED")) {
                                numVinsLoadedEvents++;
                            }
                        }

                        if (numTruckLocEvents > 1) {
                            CommonUtility.uploadLogMessage(String.format("TRUCK_EVENT_DEBUG: %d TRUCKLOC events, %d VINSLOADED events", numTruckLocEvents, numVinsLoadedEvents));
                            CommonUtility.uploadLogMessage(String.format("TRUCK_EVENT_DEBUG: First TRUCKLOC: id: %d timestamp: %s",
                                    firstTruckLocEvent.load_event_id,
                                    firstTruckLocEvent.csv.split(",")[7]));
                            CommonUtility.uploadLogMessage(String.format("TRUCK_EVENT_DEBUG: Last TRUCKLOC: id: %d timestamp: %s",
                                    lastTruckLocEvent.load_event_id,
                                    lastTruckLocEvent.csv.split(",")[7]));
                        }
                        pushingLoadEvents = false;
                    }
                };
                thread.run();
            } else {
                pushingLoadEvents = false;
            }
        }
    }

    public static void handleUploadLoadEventResponse(Context context, boolean success, int id, String result) {
        LoadEvent loadEvent = null;

        loadEvent = DataManager.getLoadEvent(context, id);

        if (loadEvent != null) {
            if (success) {
                loadEvent.uploadStatus = Constants.SYNC_STATUS_UPLOADED;
                DataManager.insertLoadEvent(context, loadEvent);
                uploadSucceededNotify(context, Constants.UPLOAD_LOAD_EVENT, loadEvent.load_event_id, -1);
            } else {
                //hiding load event failure toast since they can happen a lot
                //CommonUtility.showText("Delivery Upload failed for a load event" + loadEvent.load_event_id);
                loadEvent.uploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED;
                DataManager.insertLoadEvent(context, loadEvent);
                uploadFailedNotify(context, Constants.UPLOAD_LOAD_EVENT, loadEvent.load_event_id, -1);
            }
            LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(LoadEvent.class.getName(), -1));
        }
    }


    private static void pushLoads(Context context, List<Load> allLoads, boolean force, int driver_id) {
        clearNotifications(context);

        establishWakelock(context);

        //log.debug(Logs.DEBUG, "pushing loads NEW");

        log.debug(Logs.DEBUG, "Call to push loads for driver " + driver_id);

        for (Load load : allLoads) {
            if (load != null && !load.uploaded) {
                //allLoadsUploaded = false;
                if (load.driverPreLoadSignature != null || force) {

                    log.debug(Logs.UPLOAD, "Attempting Upload of load " +
                            load.load_remote_id + ":" + load.loadNumber);

                    uploadLoad(context, load);
                }
            }
        }
    }

    private static void pushDeliveries(Context context, List<Delivery> allDeliveries, boolean force, int driver_id) {
        clearNotifications(context);

        establishWakelock(context);

        //log.debug(Logs.DEBUG, "pushing loads NEW");

        log.debug(Logs.DEBUG, "Call to push deliveries for driver " + driver_id);

        for (Delivery delivery : allDeliveries) {
            if (delivery != null && !delivery.uploaded) {
                if (((!HelperFuncs.isNullOrEmpty(delivery.dealerSignature) || delivery.shuttleLoad) && !HelperFuncs.isNullOrEmpty(delivery.driverSignature)) || force) {

                    log.debug(Logs.UPLOAD, "Attempting Upload of delivery " +
                            delivery.delivery_remote_id + ":" + delivery.delivery_id);
                    for (DeliveryVin dv : delivery.deliveryVins) {
                        YardInventory yardInventory = DataManager.getYardInventoryForDeliveryVin(context, dv.delivery_vin_id);
                        if (yardInventory != null && yardInventory.uploadStatus == Constants.SYNC_STATUS_NOT_READY_FOR_UPLOAD) {
                            yardInventory.uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
                            DataManager.insertYardInventory(context, yardInventory);
                        }
                    }
                    uploadDelivery(context, delivery, delivery.load_id);
                }
            } else {
                /*
                log.debug(Logs.UPLOAD, "gfad of delivery " +
                        delivery.delivery_remote_id + ":" + delivery.delivery_id);
                        */
            }
        }
    }


    private static void clearNotifications(Context context) {
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        mNotificationManager.cancelAll();
    }


    public static void uploadDelivery(final Context context, final Delivery delivery, final int load_id) {
        uploadDelivery(context, delivery, load_id, false);
    }

    public static void uploadDelivery(final Context context, final Delivery delivery, final int load_id, boolean reflag) {
        uploadDelivery(context, delivery, load_id, reflag, false);
    }

    public static void uploadDelivery(final Context context, final Delivery delivery, final int load_id, boolean reflag, boolean forceUpload) {
        establishWakelock(context);
        boolean uploadReady = true;

        //We only reflag after a delivery has been completely uploaded and need to re-mark it for download
        //due to images finishing...

        log.debug(Logs.DEBUG, "attempting delivery upload for " +
                delivery.delivery_remote_id);

        if (!forceUpload && !reflag && ((!delivery.shuttleLoad && delivery.dealerSignature == null) || delivery.driverSignature == null
                || delivery.deliveryUploadStatus == Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY)) {
            log.debug(Logs.DEBUG, "delivery upload not ready for " +
                    delivery.delivery_remote_id);
            uploadReady = false;
        }

        if (uploadReady) {
            log.debug(Logs.DEBUG, "Ready to upload delivery, reflag is " + reflag);

            //save the delivery vin information prior to adjusting the data for the reflag...

            delivery.deliveryUploadStatus = Constants.SYNC_STATUS_UPLOADING_FOR_DELIVERY;
            DataManager.insertDeliveryToLocalDB(context, delivery, false);

            //Only include signature info if we're not reflagging
            if (reflag) {
                delivery.callback = null;
                delivery.dealerSignature = null;
                delivery.dealerSignatureSignedAt = null;
                delivery.dealerContact = null;
                delivery.dealerEmail = null;
                delivery.dealerSignatureLat = null;
                delivery.dealerSignatureLon = null;
                delivery.driverSignature = null;
                delivery.driverContact = null;
                delivery.driverSignatureSignedAt = null;
                delivery.sti = 0;
                delivery.afrhrs = 0;
                delivery.driverSignatureLat = null;
                delivery.driverSignatureLon = null;
                delivery.driverComment = null;
                delivery.dealerComment = null;

                //don't save the delivery vins
                delivery.deliveryVins.clear();
            } else {
                LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(Delivery.class.getName(), 1));
            }

            //massage delivery for upload
            delivery.download_status = "ready";

            delivery.imageCount = delivery.getDeliveryImageCount();

            for (DeliveryVin deliveryVin : delivery.deliveryVins) {
                log.debug(Logs.UPLOAD, "There are " + deliveryVin.damages.size() + " damages");
                for (int index = deliveryVin.damages.size() - 1; index >= 0; index--) {
                    Damage damage = deliveryVin.damages.get(index);

                    if (damage.preLoadDamage) {
                        log.debug(Logs.UPLOAD, "Removing preload damage");
                        deliveryVin.damages.remove(index);
                    }
                }
            }

            com.cassens.autotran.data.model.dto.Delivery payload = delivery.getDTO(context, true, false);
            CommonUtility.highLevelLog("Delivery $deliveryId upload starting", null, delivery);
            boolean started = HttpCallWorker.makeJsonRequest(context, URLS.POST_SAVE_DELIVERY_FULL, payload, Delivery.class, payload.getDelivery_id());

            if(!started) {
                CommonUtility.highLevelLog("Delivery $deliveryId upload failed to start", null, delivery);
                log.debug(Logs.DEBUG, "Did NOT start upload");
                delivery.deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
                DataManager.insertDeliveryToLocalDB(context, delivery, false);
            }
        }
    }

    public static void handleUploadDeliveryResponse(Context context, boolean success, int id, String result) {
        Delivery finalDelivery = DataManager.getDelivery(context, id);
        if (success) {
            CommonUtility.highLevelLog("Delivery $deliveryId upload succeeded", null, finalDelivery);
            finalDelivery.uploaded = true;

            finalDelivery.deliveryUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY;

            //unwind remote ids that were set for uploads
            //BFF - 08-18-2015 - I don't think we have to unwind this because I don't think we SAVED the delivery
            for (DeliveryVin deliveryVin : finalDelivery.deliveryVins) {

                deliveryVin.deliveryUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY;

                deliveryVin.vin_id = deliveryVin.vin.vin_id;

                for (Damage damage : deliveryVin.damages) {

                    damage.deliveryUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY;

                    if (!damage.preLoadDamage) {
                        damage.uploaded = true;
                    }
                }

                for(Image image: deliveryVin.images) {
                    DataManager.setImageDeliveryUploadStatus(context, image.image_id, Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY);

                    if (!image.preloadImage) {
                        DataManager.setImageUploaded(context, image.image_id);
                        DataManager.setImageUploadStatus(context, image.image_id, Constants.SYNC_STATUS_UPLOADED);
                    }
                }
            }

            for(Image image: finalDelivery.images) {
                DataManager.setImageDeliveryUploadStatus(context, image.image_id, Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY);

                if (!image.preloadImage) {
                    DataManager.setImageUploaded(context, image.image_id);
                    DataManager.setImageUploadStatus(context, image.image_id, Constants.SYNC_STATUS_UPLOADED);
                }
            }

            finalDelivery.deliveryVins.clear();

            DataManager.insertDeliveryToLocalDB(context, finalDelivery, false);
            uploadSucceededNotify(context, Constants.UPLOAD_DELIVERY, finalDelivery.delivery_id, finalDelivery.load_id);
        } else {
            String message = "Delivery upload failed";

            if (!HelperFuncs.isNullOrWhitespace(result)) {
                JSONObject jsonResult = null;
                try {
                    jsonResult = new JSONObject(result);

                    if (jsonResult.has("message")) {
                        message = jsonResult.getString("message");
                    }
                } catch (JSONException e) {
                    message = "Could not parse JSON result";
                    e.printStackTrace();
                }
            }

            CommonUtility.highLevelLog("Delivery $deliveryId upload failed: " + message, null, finalDelivery);

            if (finalDelivery.dealer != null) {
                CommonUtility.showText("Delivery Upload failed for delivery to " + finalDelivery.dealer.getDealerDisplayName() + ": " + message);
                log.debug(Logs.UPLOAD, "Delivery Upload failed for delivery "+ finalDelivery.delivery_remote_id + " to " + finalDelivery.dealer.getDealerDisplayName() );
            } else {
                CommonUtility.showText("Delivery Upload failed for shuttle load: " + message);
                if (finalDelivery.load != null && finalDelivery.load.loadNumber != null) {
                    log.debug(Logs.UPLOAD, "Delivery Upload failed for shuttle load: " + finalDelivery.load.loadNumber + " " + message);
                }
                else {
                    log.debug(Logs.UPLOAD, "Delivery Upload failed for shuttle load: <null load info> " + message);
                }
            }
            finalDelivery.deliveryUploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED_FOR_DELIVERY;

            for (DeliveryVin deliveryVin : finalDelivery.deliveryVins) {
                deliveryVin.deliveryUploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED_FOR_DELIVERY;

                deliveryVin.vin_id = deliveryVin.vin.vin_id;

                for (Damage damage : deliveryVin.damages) {

                    damage.deliveryUploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED_FOR_DELIVERY;

                    if (damage.specialCode != null) {
                        damage.special_code_id = damage.specialCode.special_code_id;
                    } else if (damage.areaCode != null) {
                        damage.area_code_id = damage.areaCode.area_code_id;
                        damage.type_code_id = damage.typeCode.type_code_id;
                        damage.svrty_code_id = damage.severityCode.severity_code_id;
                    }
                }
            }

            DataManager.insertDeliveryToLocalDB(context, finalDelivery, false);

            uploadFailedNotify(context, Constants.UPLOAD_DELIVERY, finalDelivery.delivery_id, finalDelivery.load_id);
        }
        boolean reflag = true;
        for (Image img : finalDelivery.getDeliveryImages()) {
            log.debug(Logs.DEBUG, "Image uploaded? " + img.uploaded);
            if (!img.uploaded) {
                reflag = false;
                break;
            }
        }

        if (reflag) {
            LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(Delivery.class.getName(), -1));
        }
    }



    public static void  uploadImage(final Context context, final Image image, String filename, String delivery_vin_remote_id, final int load_id) {
        uploadImage(context, image);
    }

    public static void uploadImage(final Context context, final Image image) {
        establishWakelock(context);

        log.debug(Logs.DEBUG, "saving image");
        DataManager.insertImageToLocalDB(context, image);

        com.cassens.autotran.data.model.dto.Image dtoImage = image.getDTO(context);
        log.debug(Logs.DEBUG, "fancy URL: " + dtoImage.getPreauth_url());

        boolean started = HttpCallWorker.makeJsonRequest(context, URLS.POST_SAVE_VIN_IMAGE, dtoImage, Image.class, image.image_id);

        if(!started) {
            log.debug(Logs.UPLOAD, "Did NOT start upload for image " + image.filename);
        }
    }

    public static void handleUploadPhotoToS3Response(final Context context, boolean success, int id) {
        //Log.d(TAG, "s3 Response");
        final Image image = DataManager.getImage(context, id);

        leavePhotoUploadInProgressState(context);

        if (image != null) {
            if (success) {
                log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_6: Photo S3 upload succeeded and marked uploaded: " + image.filename);
                DataManager.setImageS3UploadStatus(context, image.image_id, Constants.SYNC_STATUS_UPLOADED);

                if(image.preloadImage || image.load_id != -1) {
                    DataManager.setImagePreloadUploadStatus(context, image.image_id, Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD);
                } else if(image.delivery_id != -1 || image.delivery_vin_id != -1) {
                    DataManager.setImageDeliveryUploadStatus(context, image.image_id, Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY);
                }

                //reset upload status of image object so that we resend it with the preauth url
                DataManager.setImageNotUploaded(context, image.image_id);

                final Context applicationContext = context.getApplicationContext();

                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
                        String securePhotoLink = getSecurePhotoLink(applicationContext, DataManager.getFolderName(applicationContext, image.image_id) + "/" + image.filename + ".jpg", "autotran-photos");
                        DataManager.setImagePreauthUrl(applicationContext, image.image_id, securePhotoLink);

                        SyncManager.handleUploadImageResponse(applicationContext, true, image.image_id);
                        log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_7: Calling pushLocalDataToRemoteServer from handleUploadPhotoToS3Response()");
                        CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from handleUploadPhotoToS3Response()");
                        SyncManager.pushLocalDataToRemoteServer(applicationContext, CommonUtility.getDriverNumberAsInt(applicationContext), false);
                    }};

                thread.start();
            } else {
                log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_6: Photo S3 upload failed and marked for later retry: " + image.filename);
                DataManager.setImageS3UploadStatus(context, image.image_id, Constants.SYNC_STATUS_NOT_UPLOADED);
            }
        }
    }

    private static Object sPhotoUploadStatus = new Object();
    private static boolean sPhotoUploadInProgress = false;
    private static long sPhotoUploadStartedMillis = 0L;
    private static int sInProgressImageId = -1;

    public static void leavePhotoUploadInProgressState(Context context) {
        synchronized (sPhotoUploadStatus) {
            if (sPhotoUploadInProgress) {
                sPhotoUploadInProgress = false;
            }
        }
    }

    public static boolean enterPhotoUploadInProgressState(Context context, int imageId) {
        int oldImageId = -1;
        synchronized (sPhotoUploadStatus) {
            if (sPhotoUploadInProgress) {
                if (AppSetting.S3_UPLOAD_MAX_LOCK_SECONDS.getInt() < 0 || (System.currentTimeMillis() - sPhotoUploadStartedMillis) / 1000 < AppSetting.S3_UPLOAD_MAX_LOCK_SECONDS.getInt()) {
                    log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_1: Photo upload NOT cleared to proceed: " + imageId);
                    log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_1: Still uploading image " + sInProgressImageId);
                    return false;
                }
                oldImageId = sInProgressImageId;
            }
            sPhotoUploadInProgress = true;
            sInProgressImageId = imageId;
            sPhotoUploadStartedMillis = System.currentTimeMillis();
        }
        if (oldImageId >= 0) {
            log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_1: Photo S3 upload timed out. " + oldImageId);
            if (AppSetting.S3_UPLOAD_TIMEOUT_IMMEDIATE_RETRY.getBoolean()) {
                log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_1: Setting image s3 upload status for immediate retry: " + oldImageId);
                DataManager.setImageS3UploadStatus(context, oldImageId, Constants.SYNC_STATUS_NOT_UPLOADED);
            }
            else {
                log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_1: Leaving image S3 status at UPLOADING for deferred retry" + oldImageId);
            }
        }
        log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_1: Photo upload cleared to proceed: " + sInProgressImageId);
        return true;
    }

    public static String getSecurePhotoLink(Context context, String objectKey, String bucketName) {
        AmazonS3 s3Client = new AmazonS3Client(Util.getCredProvider(context, s3Auth(bucketName)));

        Date expiration = new Date();
        long expTimeMillis = expiration.getTime();
        expTimeMillis += 604800000;  //seven days in the future
        expiration.setTime(expTimeMillis);
        // Generate the presigned URL.
        //Log.d(TAG, "Generating pre-signed URL.");
        GeneratePresignedUrlRequest generatePresignedUrlRequest =
                new GeneratePresignedUrlRequest(bucketName, objectKey)
                        .withExpiration(expiration);
        // TODO: Need to check for AmazonClientException here.
        URL url = s3Client.generatePresignedUrl(generatePresignedUrlRequest);
        return url.toString();
    }

    public static void handleUploadImageResponse(Context context, boolean success, int id) {
        Image image = DataManager.getImage(context, id);

        if (image != null) {
            if (success) {
                log.debug(Logs.DEBUG, "Should notify...");
                if(image.delivery_vin_id != -1 && image.load_id == -1 && image.delivery_id == -1) {
                    DeliveryVin dv = DataManager.getDeliveryVin(context, image.delivery_vin_id);
                    if (dv != null) {
                        Delivery delivery = DataManager.getDelivery(context, dv.delivery_id);

                        uploadSucceededNotify(context, Constants.UPLOAD_IMAGE, image.image_id, delivery.load_id);
                    }
                } else if(image.problem_report_guid != null) {
                    log.debug(Logs.DEBUG, "notifying of problem log image upload sucess...");

                    uploadSucceededNotify(context, Constants.UPLOAD_IMAGE, image.image_id, -1);
                } else if (image.inspection_guid != null) {
                    log.debug(Logs.DEBUG, "notifying of inspection image upload success...");

                    uploadSucceededNotify(context, Constants.UPLOAD_IMAGE, image.image_id, -1);
                } else {
                    int load_id = -1;
                    if(image.load_id != -1 && image.delivery_id == -1) {

                        Load imageLoad = DataManager.getLoadForRemoteId(context, String.valueOf(image.load_id));

                        if(imageLoad != null) {
                            load_id = imageLoad.load_id;
                        } else {
                            load_id = image.load_id;
                        }
                    } else {
                        load_id = image.load_id;
                    }

                    log.debug(Logs.DEBUG, "saving image with load id " + load_id);
                    uploadSucceededNotify(context, Constants.UPLOAD_IMAGE, image.image_id, load_id);
                }

            } else {
                log.debug(Logs.DEBUG, "caught an image upload failure");
                log.debug(Logs.UPLOAD, "Image Upload failed for image " + image.filename);
                CommonUtility.showText("Image Upload failed for image " + image.filename);

                DataManager.setImageS3UploadStatus(context, image.image_id, Constants.SYNC_STATUS_NOT_UPLOADED);

                DeliveryVin dv = DataManager.getDeliveryVin(context, image.delivery_vin_id);
                Delivery delivery = DataManager.getDelivery(context, dv.delivery_id);
                uploadFailedNotify(context, Constants.UPLOAD_IMAGE, image.image_id, delivery.load_id);
            }
        }
    }

    public static S3BucketAuth s3Auth(String bucketName) {
        S3BucketAuth authInfo = new S3BucketAuth();
        authInfo.AWS_ACCOUNT_ID 		= "331592269501";
        authInfo.COGNITO_POOL_ID 		= "us-east-1:6ece6298-b343-4499-9533-db0d4130e8e1";
        authInfo.COGNITO_ROLE_UNAUTH	= "arn:aws:iam::331592269501:role/Cognito_AutoTranUnauth_DefaultRole";
        authInfo.COGNITO_ROLE_AUTH   	= "arn:aws:iam::331592269501:role/Cognito_AutoTranAuth_DefaultRole";
        authInfo.BUCKET_NAME 			=  bucketName;
        return authInfo;
    }

    /**
     * Get the next photo to upload (lowres first, then hires)
     * @param context
     * @return
     */
    public static Image getNextPhoto(Context context) {
        Image imageToSend = DataManager.getNextPhotoToUpload(context, false);
        if(imageToSend == null && (CommonUtility.hasHoneywellScanner() || CommonUtility.connectedToCtcWifi())) {
            //Log.d(TAG, "ben: looking for hires");
            imageToSend = DataManager.getNextPhotoToUpload(context, true);
        }

        return imageToSend;
    }

    public static void sendNextPhoto(Context context) {
        sendNextPhoto(context, -1);
    }

    /**
     *
     * @param context
     * @param requestId If != -1, this request will send an S3UploadEvent
     *                  on the NETWORK_REQUESTS event bus queue on completion,
     *                  with the given ID.
     */
    public static void sendNextPhoto(Context context, int requestId) {
        //Log.d("NARF", "progress: " + photoUploadInProgress());
        //Log.d(TAG, "ben: attempt sendNextPhoto, will check for photo to send");
        S3BucketAuth authInfo = s3Auth("autotran-photos");

        Image imageToSend = getNextPhoto(context);

        if (imageToSend == null) {
            log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_1: getNextPhoto(): no photos to send");
            return;
        }

        if (enterPhotoUploadInProgressState(context, imageToSend.image_id)) {
            log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_1: starting upload to s3 for image: " + imageToSend.image_id);
            String uristring;
            File file = new File(CommonUtility.cachedImageFileFullPath(context, imageToSend.filename));
            uristring = Uri.fromFile(file).toString();

            S3Container photo = new S3Container(imageToSend.image_id, uristring, DataManager.getFolderName(context, imageToSend.image_id), true);
            log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_1: marking image s3 status as UPLOADING: " + imageToSend.image_id);
            DataManager.setImageS3UploadStatus(context, imageToSend.image_id, Constants.SYNC_STATUS_UPLOADING);

            ArrayList<S3Container> photos = new ArrayList<>();
            photos.add(photo);

            TransferController.uploadWithId(context, photos, authInfo, requestId);
        }
    }

    public static void archiveLoad(Context context, Load load) {
        S3BucketAuth authInfo = s3Auth("autotran-archived");
        authInfo.folderName = load.loadNumber;
        com.cassens.autotran.data.model.dto.Load payload = load.getDTO(context, true, true);
        log.debug(Logs.DEBUG, "archiving load " + load.loadNumber);

        //NOTE: For now we're deleting images if they're not uploaded.

        //make load json
        Gson gson = new GsonBuilder().create();
        String loadJson = gson.toJson(payload, com.cassens.autotran.data.model.dto.Load.class);


        String logFileUri = CommonUtility.generateLogZipFile(CommonUtility.getDriverNumber(context), context, false, false);

        File path = context.getFilesDir();
        File logDirectory = new File("/sdcard/AutoTran/logs/zipped");
        File[] logDirectoryFiles = logDirectory.listFiles();

        File[] logFiles = null;

        if(logFileUri != null) {
            logFiles = new File[1];
            logFiles[0] = new File(logFileUri.substring(5));
        }

        File zipFile = new File (path + "/" + load.loadNumber + ".zip");

        try {
            FileOutputStream fos = new FileOutputStream(zipFile);
            ZipOutputStream zos = new ZipOutputStream(fos);

            try {
                ZipEntry zipEntry = new ZipEntry(load.loadNumber +"-json.txt");
                zos.putNextEntry(zipEntry);
                zos.write(loadJson.getBytes());
                zos.closeEntry();

                //add logs in case we need that data to recreate something
                if(logFiles != null) {
                    int count;
                    ZipEntry entry;
                    FileInputStream fi;
                    int bufsize = 1024 * 10;
                    BufferedInputStream origin;
                    byte[] data = new byte[bufsize];

                    for(int i = 0; i < logFiles.length; i++) {
                        fi = new FileInputStream(logFiles[i]);
                        origin = new BufferedInputStream(fi, bufsize);
                        entry = new ZipEntry(logFiles[i].getName());
                        zos.putNextEntry(entry);
                        while ((count = origin.read(data, 0, bufsize)) != -1) {
                            zos.write(data, 0, count);
                        }
                        count = 0;
                        origin.close();

                        fi.close();
                    }
                }
            } catch (Exception e) {
                log.debug(Logs.EXCEPTIONS, "exception -- unable to write to zip file for load " + load.loadNumber);
                // unable to write zip
            } finally {
                zos.close();
            }
        } catch (Exception e) {
            log.debug(Logs.EXCEPTIONS, "exception -- unable to create file output stream");
        }
        log.debug(Logs.UPLOAD, "attempting to upload archived load " + load.loadNumber);

        ArrayList<S3Container> loads = new ArrayList<>();
        S3Container zipContainer = new S3Container(load.load_id, Uri.fromFile(zipFile).toString(), load.loadNumber, false);
        loads.add(zipContainer);

        TransferController.upload(context, loads, authInfo);
    }

    public static void uploadLoad(final Context context, final Load load) {
        uploadLoad(context, load, false, false);
    }

    public static void uploadLoad(final Context context, final Load load, boolean reflag, boolean forceUpload) {
        log.debug(Logs.UPLOAD, "Load " + load.load_remote_id + ":" + load.loadNumber + " is being uploaded in full as JSON");

        if (!reflag) {
            log.debug(Logs.DEBUG, "Load Upload Status: " + load.preloadUploadStatus);

            if(load.driverPreLoadSignature != null) {
                log.debug(Logs.DEBUG, "Load Signature: " + load.driverPreLoadSignature.substring(0, 10));
            }
            //We only check for uploads if we're not reflagging.  a reflag means 'force load upload'
            if (!forceUpload && (load.preloadUploadStatus == Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD || load.driverPreLoadSignature == null)) {
                log.debug(Logs.DEBUG, "uploaded or we don't have a signature");

                log.debug(Logs.UPLOAD, "Load was uploaded or missing a signature");

                return;
            }

            if (load.shuttleLoad) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(Load.class.getName() + "shuttle", 1));
            } else {
                LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(Load.class.getName(), 1));
            }
        }

        //We're really only ever caring about 'new' load information when we're uploading a load for preload
        load.preloadUploadStatus = Constants.SYNC_STATUS_UPLOADING_FOR_PRELOAD;
        DataManager.insertLoadToLocalDB(context, load, false);

        load.imageCount = String.valueOf(load.getPreloadImageCount());

        //massage load data
        for (DeliveryVin dv : load.getDeliveryVinList()) {

            //Get the image counts set properly for the upload
            dv.refreshImageCounts();
            //dv.images.clear();

            for (int index = dv.damages.size() - 1; index >= 0; index--) {
                Damage damage = dv.damages.get(index);

                if (damage.readonly) {
                    log.debug(Logs.UPLOAD, "Removing preload readonly damage");
                    dv.damages.remove(index);
                }
            }
        }

        if (reflag) {
            //we need to avoid the deliveries for this load since this is a reflag to capture signatures.
            load.deliveries.clear();
        }

        com.cassens.autotran.data.model.dto.Load payload = load.getDTO(context, true, true);

        if(load.parentLoad || load.parent_load_id == -1) {
            payload.setStatus((short) 0);
        } else {
            //This is a child load and should only have its status set to 0 if the parent load is completely signed
            if(!DataManager.allChildLoadsSigned(context, load.parent_load_id)) {
                payload.setStatus((short) 1);
            } else {
                payload.setStatus((short) 0);
            }
        }

        CommonUtility.highLevelLog("Load $loadNumber upload starting", load);
        boolean started = HttpCallWorker.makeJsonRequest(context, URLS.POST_SAVE_LOAD_FULL, payload, load.getClass(), payload.getLoad_id());

        if(!started) {
            log.debug(Logs.UPLOAD, "Did NOT start upload for load " + load.loadNumber);
            CommonUtility.highLevelLog("Load $loadNumber upload failed to start", load);
            load.preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
            DataManager.insertLoadToLocalDB(context, load);
        }
        else if (AppSetting.POC_ECHO_TO_LAMBDA.getBoolean()) {
            PoCUtils.sendLambdaSaveLoadRequest(context, load);
        }

    }

    public static void  handleUploadLoadResponse(Context context, boolean success, int id, String result) {
        log.debug(Logs.DEBUG, "Handling load upload response");
        Load load = DataManager.getLoad(context, id);

        if (success) {
            CommonUtility.highLevelLog("Load $loadNumber upload succeeded", load);
            CommonUtility.showText("Load Upload succeeded for load " + load.loadNumber);
            log.debug(Logs.UPLOAD, "Upload of full load " + load.load_remote_id + ":" + load.loadNumber + " succeeded");

            load.preloadUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD;

            if (load.shuttleLoad) {
                try {
                    JSONObject jsonResult = new JSONObject(result);

                    if (jsonResult.has("data")) {
                        Gson gson = new Gson();

                        JSONObject dataJson = (JSONObject) jsonResult.get("data");

                        Load remoteLoad = gson.fromJson(dataJson.get("load").toString(), Load.class);
                        load.load_remote_id = remoteLoad.load_remote_id;

                        for (Delivery d : load.deliveries) {
                            for (Delivery rd : remoteLoad.deliveries) {
                                if (d.delivery_id == rd.delivery_id) {
                                    d.delivery_remote_id = rd.delivery_remote_id;
                                }
                            }
                        }

                        for (DeliveryVin d : load.getDeliveryVinList()) {
                            for (DeliveryVin rd : remoteLoad.getDeliveryVinList()) {
                                if (d.delivery_vin_id == rd.delivery_vin_id) {
                                    d.delivery_vin_remote_id = rd.delivery_vin_remote_id;
                                    d.vin_id = rd.vin_id;
                                    d.vin.vin_remote_id = rd.vin.vin_remote_id;
                                }
                            }
                        }
                    }
                } catch (JSONException e) {
                    log.debug(Logs.DEBUG, "Error parsing JSON result: " + result);
                }
            }

            load.uploaded = true;

            load.preloadUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD;

            for (Delivery delivery : load.deliveries) {

                delivery.preloadUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD;

                for (DeliveryVin deliveryVin : delivery.deliveryVins) {

                    for (Damage d : deliveryVin.damages) {
                        if (d.preLoadDamage) {
                            d.uploaded = true;
                        }
                    }

                    deliveryVin.preloadUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD;

                    if (!load.shuttleLoad) {
                        deliveryVin.vin_id = deliveryVin.vin.vin_id;
                    }

                    for (Damage damage : deliveryVin.damages) {
                        damage.preloadUploadStatus = Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD;
                    }

                    for(Image image : deliveryVin.images ) {
                        DataManager.setImageUploadStatus(context, image.image_id, Constants.SYNC_STATUS_UPLOADED); //also sets image.uploaded
                        DataManager.setImagePreloadUploadStatus(context, image.image_id, Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD);
                    }
                }
            }

            for(Image image : load.images ) {
                DataManager.setImageUploadStatus(context, image.image_id, Constants.SYNC_STATUS_UPLOADED); //also sets image.uploaded
                DataManager.setImagePreloadUploadStatus(context, image.image_id, Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD);
            }

            DataManager.insertLoadToLocalDB(context, load, false);

            uploadSucceededNotify(context, Constants.UPLOAD_LOAD, load.load_id, load.load_id);
        } else {
            String message = "Upload of load " + load.loadNumber + " failed";

            if (!HelperFuncs.isNullOrWhitespace(result)) {
                JSONObject jsonResult = null;
                try {
                    jsonResult = new JSONObject(result);

                    if (jsonResult.has("message")) {
                        message = jsonResult.getString("message");
                    }
                } catch (JSONException e) {
                    message = "Could not parse JSON result";
                    e.printStackTrace();
                }
            }

            CommonUtility.highLevelLog("Load $loadNumber upload failed: " + message, load);
            CommonUtility.showText("Load Upload failed for load " + load.loadNumber+ ": " + message);

            log.debug(Logs.UPLOAD, "Upload of load " + load.load_remote_id + ":" + load.loadNumber + " failed.  " + result);

            load.preloadUploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED_FOR_PRELOAD;

            //'rewind' faulty uploads to hold the proper id fields
            for (Delivery delivery : load.deliveries) {

                delivery.preloadUploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED_FOR_PRELOAD;


                for (DeliveryVin deliveryVin : delivery.deliveryVins) {

                    deliveryVin.preloadUploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED_FOR_PRELOAD;

                    if (!load.shuttleLoad) {
                        deliveryVin.vin_id = deliveryVin.vin.vin_id;
                    }

                    for (Damage damage : deliveryVin.damages) {
                        damage.preloadUploadStatus = Constants.SYNC_STATUS_UPLOAD_FAILED_FOR_PRELOAD;
                    }
                }
            }

            DataManager.insertLoadToLocalDB(context, load, false);
            uploadFailedNotify(context, Constants.UPLOAD_LOAD, load.load_id, load.load_id);
        }

        boolean reflag = true;
        for (Image img : load.getPreloadImages()) {
            log.debug(Logs.DEBUG, "Image uploaded? " + img.uploaded);
            if (!img.uploaded) {
                reflag = false;
                break;
            }
        }

        if (reflag) {
            if (load.shuttleLoad) {
                LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(Load.class.getName() + "shuttle", -1));
            } else {
                LocalBroadcastManager.getInstance(context).sendBroadcast(getPendingUploadChangeIntent(Load.class.getName(), -1));
            }
        }
    }

    private static void pushTrainingRequirements(final Context context) {
        List<TrainingRequirement> completedTrainings = DataManager.getCompletedTrainingRequirements(context);

        for(TrainingRequirement r : completedTrainings) {
            if(r.uploaded == 0) {
                uploadTrainingRequirement(context, r);
            }
        }
    }

    public static void uploadTrainingRequirement(final Context context, @NonNull final TrainingRequirement req) {
        establishWakelock(context);

        log.debug(Logs.DEBUG, "Saving training requirement ID " + req.id + " with completed date " + req.completed);
        Object body = req;

        String url = URLS.POST_SAVE_TRAINING_REQUIREMENT;

        if(req.adHoc != 0) {
            // Remember, our DB id is the same as the server DB id. We need to remove the ID from the object
            // so it's autogenerated on the server, so get a JSON object and remove that field.
            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(ShuttleMove.class, new GsonTypeAdapters.ShuttleMoveSerializer())
                    .registerTypeAdapter(Date.class, new GsonTypeAdapters.DateSerializer())
                    .create();
            JsonElement element = gson.toJsonTree(req);
            JsonObject o = element.getAsJsonObject();
            o.remove("id");

            // Not super-proud of this hack, but HttpCallIntentService tries to be too cute, and there isn't
            // a way around it
            body = gson.fromJson(o.toString(), Map.class);
            url = URLS.POST_SAVE_ADHOC_TRAINING;
        }

        // Use req.id here so we can mark the DB object as uploaded.
        boolean started = HttpCallWorker.makeJsonRequest(context, url, body, TrainingRequirement.class, req.id);

        if(!started) {
            log.debug(Logs.DEBUG, "Failed to start training requirement upload");
        }
    }

    public static void handleUploadTrainingRequirementResponse(Context context, boolean success, int id, String responseBody) {
        if(!success) {
            log.warn(Logs.DEBUG, "Failed to upload training requirement " + id + ": " + responseBody);
        }
        else {
            DataManager.markTrainingRequirementUploaded(context, id);
        }
    }

    /**
     * Called when one of the remote upload calls fails.  The double runnable model is set up to catch the last failure in the
     * last 20 seconds and then to schedule a retry 60 seconds after THAT
     *
     * @param context
     * @param object_type
     * @param object_id
     * @param load_id
     */
    public static void uploadFailedNotify(final Context context, final int object_type, final int object_id, final int load_id) {
        log.debug(Logs.UPLOAD, "received notification that an upload failed... checking object type and scheduling a re-try");
        CommonUtility.uploadLogMessage("Got upload failed notification. Scheduling re-try.");
        //if(mHandler == null) {
        log.debug(Logs.UPLOAD, "re-creating mhandler every time...");
        mHandler = new Handler();
        //}

        Runnable notifyAndRerun = new Runnable() {
            public void run() {
                log.debug(Logs.UPLOAD, "starting 'notifyandrerun'");

                String objectType = "";

                switch (object_type) {
                    case Constants.UPLOAD_DELIVERY:
                        objectType = "Delivery";
                        break;
                    case Constants.UPLOAD_DELIVERY_VIN:
                        objectType = "Delivery Vin";
                        break;
                    case Constants.UPLOAD_IMAGE:
                        objectType = "Inpection image";
                        break;
                    case Constants.UPLOAD_LOAD:
                        objectType = "Load";
                        break;
                    case Constants.UPLOAD_DAMAGE:
                        objectType = "Damage";
                        break;
                    case Constants.UPLOAD_RECEIVED_VEHICLE:
                    case Constants.UPLOAD_YARD_EXIT:
                    case Constants.UPLOAD_YARD_INVENTORY:
                    case Constants.UPLOAD_PLANT_RETURN:
                    case Constants.UPLOAD_INSPECTION:
                        break;
                }

                if (!objectType.equals("")) {
                    String message = "A Sync failed for " + objectType + ": " + object_id + ", trying again in 1 minute.";
                    log.debug(Logs.UPLOAD, message);
                    String title = "Sync Failed";
                    String tag = objectType + ":" + object_id;
                    //Uri sound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                    sendNotification(context, message, title, load_id, tag, null);


                    if (DEBUG)
                        log.debug(Logs.UPLOAD, "Sent failure notification: " + message + " and scheduling a download in the future");
                    //Since we had a failure, we want to try and re-send the upload in one minute:
                } else {
                    log.debug(Logs.UPLOAD, "received an empty object type (" + object_type + "), not sending a notification... still scheduling a retry, however...");
                }


                Runnable rerunPush = new Runnable() {
                    public void run() {
                        if (DEBUG) log.debug(Logs.UPLOAD, "trying to upload again");

                        if (load_id != -1) {

                            if (DEBUG) log.debug(Logs.UPLOAD, "trying to upload preload / delivery object");
                            Load load = DataManager.getLoad(context, load_id);
                            final int driver_id;
                            if (load != null) {
                                driver_id = DataManager.getLoad(context, load_id).driver_id;
                            } else {
                                driver_id = -1;
                            }

                            if (load_id != -1 && driver_id != -1) {
                                if (object_type != Constants.UPLOAD_IMAGE) {
                                    if (DEBUG) {
                                        log.debug(Logs.UPLOAD, "trying to upload preload / delivery elements");
                                    }
                                    CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from uploadFailedNotify() rerunPush thread");
                                    SyncManager.pushLocalDataToRemoteServer(context, driver_id, false);
                                } else {
                                    //images uploaded with their containers now
                                    //if (DEBUG) log.debug(Logs.UPLOAD, "trying to upload next image");

                                    //log.debug(Logs.UPLOAD, "Calling upload next image: 1687");
                                    //uploadNextImage(context, driver_id);
                                }
                            }
                        } else {
                            int driver_id = CommonUtility.getDriverNumberAsInt(context);

                            CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from uploadFailedNotify() rerunPush thread load_id == -1");
                            SyncManager.pushLocalDataToRemoteServer(context, driver_id, false);
                        }

                    }
                };

                log.debug(Logs.DEBUG, "bumping for 60 seconds...");
                mHandler.removeCallbacksAndMessages(null);
                mHandler.postDelayed(rerunPush, 60000);
            }
        };

        log.debug(Logs.DEBUG, "Removing callbacks and messages...");
        mHandler.removeCallbacksAndMessages(null);

        log.debug(Logs.DEBUG, "Starting the retry runnable in 10 seconds...");
        mHandler.postDelayed(notifyAndRerun, 10000);


        //notify any listeners (usually the sync status page) that the sync data set has changed
        Intent sync_status_updated = new Intent(Constants.SYNC_STATUS_UPDATED_DATA);
        sync_status_updated.putExtra("updateLoadList", true);


        context.sendBroadcast(sync_status_updated);
        context.sendBroadcast(new Intent(Constants.SYNC_STATUS_SYNC_FAILED));
    }

    private static void sendNotification(final Context context, String message, String title, int id, String tag, Uri sound) {
        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(context)
                        .setContentTitle(title)
                        .setContentText(message)
                        .setSmallIcon(R.drawable.ic_launcher_autotran);

        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        Notification notification = mBuilder.build();
        notification.flags = Notification.FLAG_AUTO_CANCEL;

        if (sound != null) {
            log.debug(Logs.DEBUG, "Playing sound for notification: " + message + " - " + title);
            notification.sound = sound;
        } else {
            log.debug(Logs.DEBUG, "NOT Playing sound for notification: " + message + " - " + title);
        }
        mNotificationManager.notify(tag, id, notification);
    }


    //Handle all upload successes here
    public static void uploadSucceededNotify(Context tempcontext, final int object_type, final int object_id, final int load_id) {

        final Context context = tempcontext.getApplicationContext();

        //Wrap this up in a background thread
        Thread thread = new Thread() {
            @Override
            public void run() {
                android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);

                //Load load = null;

                /*
                if (load_id != -1) {
                    load = DataManager.getLoad(context, load_id);
                }
                */

                boolean updateLoadList = false;
                boolean shouldNotify = true;

                //if we have succeeded in uploading a preload object (a delivery_vin, image or damage, check to see if the load should be uploaded
                //unfortunately, this test is only possible with complete copy of the load object until we have a centrally
                //maintained data store in memory

                String notification = "";

                switch (object_type) {
                    case Constants.UPLOAD_LOAD:
                        String title = "Preload upload successful";
                        updateLoadList = true;

                        //notification = "Uploaded preload data for Load " + load.loadNumber;
                        notification = "Uploaded preload data";

                        log.debug(Logs.UPLOAD, notification);
                        sendNotification(context, notification, title, Constants.NOTIFICATION_PRELOAD, load_id + ":preload", sound);

                        checkForWakeLockRelease(context, load_id);

                        //Check for any preload images that need to be uploaded
                        //uploadNextImage(context, load.driver_id);
                        break;
                    case Constants.UPLOAD_DELIVERY:
                        title = "Delivery upload successful";
                        shouldNotify = false;
                        updateLoadList = true;
                        Delivery delivery = DataManager.getDelivery(context, object_id);
                        if (!delivery.shuttleLoad && delivery.dealer != null) {
                            notification = "Uploaded delivery data for " + delivery.dealer.customer_name;
                        } else {
                            //notification = "Uploaded delivery data for shuttle load " + load.loadNumber;
                            notification = "Uploaded delivery data for shuttle load";
                        }

                        log.debug(Logs.UPLOAD, notification);

                        sendNotification(context, notification, title, Constants.NOTIFICATION_DELIVERY, load_id + ":" + object_id + ":delivery", sound);

                        checkForWakeLockRelease(context, load_id);

                        //Check for any delivery images that need to be uploaded
                        //uploadNextImage(context, load.driver_id);

                        break;
                    case Constants.UPLOAD_DAMAGE:
                    case Constants.UPLOAD_DELIVERY_VIN:
                        shouldNotify = false;

                        /*
                        if (load_id != -1 && load != null) {
                            //Not necessary to check whether this object was a preload or a delivery object since the upload commands handle it

                            //try and upload the preload data
                            uploadLoad(context, load);

                            //try and upload all of the delivery objects for this load
                            for (Delivery tmpDelivery : load.deliveries) {
                                uploadDelivery(context, tmpDelivery, load_id);
                            }
                        } else {
                            //this upload was not associated with a load, so it was probably a damage / image for an inspection
                            log.debug(Logs.DEBUG, "we're not pushing inspections for now...");

                            //List<Inspection> inspections = DataManager.getInspectionList(context, true);
                            //pushInspections(context, inspections);
                        }
                        */

                        break;
                    case Constants.UPLOAD_IMAGE:

                        log.debug(Logs.UPLOAD, "Upload image succeeded notification");
                        shouldNotify = true; //JWBB - quiet the beeps

                        Image image = DataManager.getImage(context, object_id);
                        Log.d(TAG, "Ben: image " +  image.filename + " status " + image.s3_upload_status + " url: " + image.preauth_url);

                        Load load = null;

                        if (image.preloadImage) {
                            if(image.load_id != -1) {
                                load = DataManager.getLoadForRemoteId(context, String.valueOf(image.load_id));
                            }

                            if (load == null && load_id != -1){
                                load = DataManager.getLoad(context, load_id);
                            }

                            if(load != null) {
                                log.debug(Logs.UPLOAD, "found the preload object, re-uploading");
                                uploadLoad(context, load, false, true);
                            } else {
                                log.debug(Logs.UPLOAD, "image upload succeeded but load not found");
                            }
                        } else if (image.problem_report_guid != null) {
                            ProblemReport report = DataManager.getProblemReport(context, image.problem_report_guid);

                            if(report != null) {
                                pushProblemReport(context, report);
                            }
                        } else if (image.inspection_guid != null) {
                            Inspection inspection = DataManager.getInspectionFromGuid(context, image.inspection_guid);

                            if (inspection != null) {
                                pushInspection(context, inspection);
                            }
                        } else {     //delivery

                            if(image.delivery_vin_id != -1) {
                                DeliveryVin dv = DataManager.getDeliveryVin(context, image.delivery_vin_id);
                                log.debug(Logs.UPLOAD, "got deliveryvin vetting delivery");
                                Delivery thisDelivery = DataManager.getDelivery(context, dv.delivery_id);
                                log.debug(Logs.UPLOAD, "uploaded image, re-flagging delivery for delivery vin image");
                                uploadDelivery(context, thisDelivery, thisDelivery.load_id, false, true);
                            } else if(image.delivery_id != -1){
                                Delivery thisDelivery = DataManager.getDeliveryForRemoteId(context, String.valueOf(image.delivery_id));
                                log.debug(Logs.UPLOAD, "uploaded image, re-flagging delivery");
                                uploadDelivery(context, thisDelivery, thisDelivery.load_id, true);
                            }
                        }

                        break;
                    case Constants.UPLOAD_RECEIVED_VEHICLE:
                    case Constants.UPLOAD_YARD_EXIT:
                    case Constants.UPLOAD_YARD_INVENTORY:
                    case Constants.UPLOAD_PLANT_RETURN:
                    case Constants.UPLOAD_INSPECTION:
                    default:
                        break;
                }

                //notify any listeners (usually the sync status page) that the sync data set has changed

                Intent sync_status_updated = new Intent(Constants.SYNC_STATUS_UPDATED_DATA);

                if (updateLoadList) {
                    sync_status_updated.putExtra("uploadLoadList", true);
                }

                if (shouldNotify) {
                    context.sendBroadcast(sync_status_updated);
                }

            }
        };
        thread.start();
    }

    public static void pushProblemReport(Context context, ProblemReport report) {
        Gson gson = new GsonBuilder().create();

        String problemReportJson = gson.toJson(report, ProblemReport.class);

        boolean started = HttpCallWorker.makeJsonRequest(context, URLS.save_problem_report, problemReportJson, ProblemReport.class, report.getId());

        if(!started) {
            log.debug("Did not start the push for problem report " + report.id);
        }
    }

    //This method is called whenever a load, delivery or image is done uploading, it walks through the
    //list of objects pending upload and if there are NONE pending / failed / retrying, release the wakelock
    private static void checkForWakeLockRelease(Context context, int load_id) {

        log.debug(Logs.DEBUG, "checking load status to see if the wakelock should be released");

        Load load = DataManager.getLoad(context, load_id);

        boolean shouldReleaseWakeLock = true;

        if(load == null) {
            log.debug(Logs.DEBUG, "load " + load_id + " wasn't there, releasing wakelock");
        } else if (load.preloadUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD) {
            log.debug(Logs.DEBUG, "load was not uploaded for preload, don't release the wakelock");
            shouldReleaseWakeLock = false;
        }

        if (load != null && shouldReleaseWakeLock) {
            for (Delivery delivery : load.deliveries) {

                //If this delivery is ready to upload (based on signature)
                if (delivery.driverSignature != null && delivery.deliveryUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY) {
                    log.debug(Logs.DEBUG, "delivery had a signature and was not yet uploaded for delivery, not releasing the wakelock");
                    shouldReleaseWakeLock = false;
                    break;
                }

                if (shouldReleaseWakeLock) {
                    for (DeliveryVin deliveryVin : delivery.deliveryVins) {

                        if ((deliveryVin.inspectedDelivery &&
                                deliveryVin.deliveryUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY &&
                                delivery.dealerSignature != null &&
                                delivery.driverSignature != null
                        )
                                ||
                                (deliveryVin.inspectedPreload &&
                                        deliveryVin.preloadUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD &&
                                        load.driverPreLoadSignature != null)
                                ) {
                            log.debug(Logs.DEBUG, "deliveryvin was not uploaded, not releasing wakelock");
                            shouldReleaseWakeLock = false;
                            break;
                        }


                        for (Image image : deliveryVin.images) {
                            if ((deliveryVin.inspectedPreload && image.preloadImage
                                    && image.preloadUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD
                                    && load.driverPreLoadSignature != null)
                                    ||

                                    (deliveryVin.inspectedDelivery && !image.preloadImage
                                            && image.deliveryUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY
                                    )
                                            && delivery.dealerSignature != null && delivery.driverSignature != null) {

                                log.debug(Logs.DEBUG, "image was not uploaded, not releasing wakelock");
                                shouldReleaseWakeLock = false;
                                break;
                            }
                        }

                        if (!shouldReleaseWakeLock)
                            break;
                    }
                }

                if (!shouldReleaseWakeLock)
                    break;
            }
        }

        if (shouldReleaseWakeLock) {
            log.debug(Logs.DEBUG, "Releasing wakelock");
            if (uploadWakelock != null && uploadWakelock.isHeld()) {
                uploadWakelock.release();
            }
        } else {
            log.debug(Logs.DEBUG, "Not releasing wakelock, the upload process has not concluded");
        }

    }


    private static Intent getPendingUploadChangeIntent(String uploadObject, int change) {
        Intent i = new Intent(PENDING_UPLOADS_CHANGE);
        i.putExtra(UPLOAD_OBJECT, uploadObject);
        i.putExtra(UPLOAD_CHANGE, change);
        return i;
    }

    public static void handleUploadDriverAction(Context context, boolean success, int id, String responseBody) {
        DataManager.markDriverActionUploaded(context, id, success);

    }

    public static void handleUploadProblemReport(Context context, boolean success, int id, String responseBody) {

        ProblemReport problemReport = DataManager.getProblemReport(context, id);
        problemReport.upload_status = Constants.SYNC_STATUS_UPLOADED;
        for(Image image : problemReport.images) {
            image.uploaded = true;
            image.uploadStatus = Constants.SYNC_STATUS_UPLOADED;
        }

        DataManager.saveProblemReport(context, problemReport);

        sendNotification(context, "--", "Problem Report Uploaded", Constants.NOTIFICATION_PRELOAD, id + ":problemreport", sound);
    }

    public static void syncCurrentDriver(Context context) {
        List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
        nameValuePairs.add(new BasicNameValuePair("user_id", CommonUtility.getDriverNumber(context)));

        try {
            String result = CallWebServices.sendJson(URLS.login, nameValuePairs, context);

            JSONObject jsonObj = new JSONObject(result);
            JSONObject jsonObject = jsonObj.getJSONObject("data");
            JSONObject jsonObjectUser = jsonObject.getJSONObject("User");

            User newUser = new User();

            newUser.user_remote_id = jsonObjectUser.getString("id");
            newUser.firstName = jsonObjectUser.getString("first_name");
            newUser.lastName = jsonObjectUser.getString("last_name");
            newUser.email = jsonObjectUser.getString("email");
            newUser.driverNumber = jsonObjectUser.getString("user_id");
            newUser.deviceToken = jsonObjectUser.getString("device_token");
            newUser.deviceID = jsonObjectUser.getString("device_id");
            newUser.password = jsonObjectUser.getString("password");
            newUser.role = jsonObjectUser.getString("role");
            newUser.userType = jsonObjectUser.getString("user_type");
            newUser.activationLink = jsonObjectUser.getString("activation_link");
            newUser.status = jsonObjectUser.getString("status");
            newUser.created = jsonObjectUser.getString("created");
            newUser.modified = jsonObjectUser.getString("modified");
            newUser.fullName = jsonObjectUser.getString("full_name");
            newUser.highClaims = jsonObjectUser.getInt("highClaims");
            newUser.requiresAudit = jsonObjectUser.getInt("requiresAudit");
            newUser.inspectionAccess = jsonObjectUser.getInt("inspectionAccess");
            newUser.supervisorCardCode = jsonObjectUser.getString("supervisorCardCode");
            newUser.supervisorPreloadChk = jsonObjectUser.getInt("supervisorPreloadChk");
            newUser.driverLicenseExpiration =  HelperFuncs.simpleDateStringToDate(jsonObjectUser.getString("driver_license_expiration"));
            newUser.medicalCertificateExpiration = HelperFuncs.simpleDateStringToDate(jsonObjectUser.getString("medical_certificate_expiration"));
            newUser.helpTerm = jsonObjectUser.getInt("helpTerm");
            newUser.autoInspectLastDelivery = jsonObjectUser.getBoolean("autoInspectLastDelivery");

            DataManager.insertUserToLocalDB(context, newUser);
        } catch (Exception e) {
            log.debug(Logs.DEBUG, "syncCurrentDriver() caught exception: " + e.getClass().getName());
            e.printStackTrace();
        }
    }
}
