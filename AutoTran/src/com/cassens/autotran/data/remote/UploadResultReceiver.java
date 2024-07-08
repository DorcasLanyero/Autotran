package com.cassens.autotran.data.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cassens.autotran.Logs;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DriverAction;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Inspection;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.PlantReturn;
import com.cassens.autotran.data.model.ProblemReport;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.YardExit;
import com.cassens.autotran.data.model.YardInventory;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.data.remote.workers.HttpCallWorker;
import com.google.gson.GsonBuilder;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UploadResultReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(UploadResultReceiver.class.getSimpleName());

    private static String TAG = "UploadResultReceiver";

    public UploadResultReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        log.debug(Logs.DEBUG, "Received response from upload request");

        int id = intent.getIntExtra(HttpCallWorker.EXTRA_OBJECT_ID, -1);
        String type = intent.getStringExtra(HttpCallWorker.EXTRA_TYPE);
        String responseBody = intent.getStringExtra(HttpCallWorker.EXTRA_BODY);
        String url = intent.getStringExtra(HttpCallWorker.EXTRA_URL);
        //boolean isMirrorToLambda = intent.getBooleanExtra(HttpCallWorker.EXTRA_LAMBDA_CALL, false);
        boolean success = false;

        log.debug(Logs.UPLOAD, url);
        log.debug(Logs.UPLOAD, responseBody);

        if (responseBody != null && responseBody.equals("failure handled")) {
            success = false;
        } else {
            //We only want to try and parse out the results and stuff we haven't handled the failure higher up...
            try {
                if (HelperFuncs.isNullOrWhitespace(responseBody)) {
                    success = false;
                }
                else {
                    JsonResponse j = new GsonBuilder().create().fromJson(responseBody, JsonResponse.class);

                    if ((j.code != null && j.code.equals("500")) || (j.success != null && j.success.equalsIgnoreCase("false")) || (j.status != null && j.status.equalsIgnoreCase("Failure"))) {
                        success = false;
                    } else {
                        success = true;
                    }
                }
            }
            // e.g. JsonSyntaxException "Caused by" IllegalStateException
            catch (Exception e) {
                if (responseBody != null
                &&  responseBody.startsWith("<!DOCTYPE HTML")
                &&  responseBody.contains("404")) {
                    log.warn(Logs.DEBUG, "HTTP 404: probably a connection drop");
                } else {
                    log.error(Logs.DEBUG, "Exception caught while interpreting server's JSON response");
                    e.printStackTrace();
                }

                log.debug(Logs.DEBUG, "Calling the listener's onFailure event.");
                success = false;
            }

            /*
            if (isMirrorToLambda) {
                PoCUtils.log("Logging response from UploadResultReceiver");
                PoCUtils.logHttpResponse(url, responseBody);
                return;
            } */

            if (type != null) {
                if (type.equals(Load.class.toString())) {
                    SyncManager.handleUploadLoadResponse(context, success, id, responseBody);
                } else if (type.equals(Delivery.class.toString())) {
                    SyncManager.handleUploadDeliveryResponse(context, success, id, responseBody);
                } else if (type.equals(Image.class.toString())) {
                    SyncManager.handleUploadImageResponse(context, success, id);
                } else if (type.equals(YardInventory.class.toString())) {
                    SyncManager.handleUploadYardInventoryResponse(context, success, id, responseBody);
                } else if (type.equals(LoadEvent.class.toString())) {
                    SyncManager.handleUploadLoadEventResponse(context, success, id, responseBody);
                } else if (type.equals(Inspection.class.toString())) {
                    SyncManager.handleUploadInspectionResponse(context, success, id, responseBody);
                } else if (type.equals(PlantReturn.class.toString())) {
                    SyncManager.handleUploadPlantReturn(context, success, id, responseBody);
                } else if (type.equals(YardExit.class.toString())) {
                    SyncManager.handleUploadYardExit(context, success, id, responseBody);
                } else if (type.equals(DriverAction.class.toString())) {
                    SyncManager.handleUploadDriverAction(context, success, id, responseBody);
                } else if (type.equals(ProblemReport.class.toString())) {
                    SyncManager.handleUploadProblemReport(context, success, id, responseBody);
                } else if (type.equals(TrainingRequirement.class.toString())) {
                    SyncManager.handleUploadTrainingRequirementResponse(context, success, id, responseBody);
                }
            }
        }
        if (success) {
            log.debug(Logs.DEBUG, "Upload request succeeded");
        }
        else {
            log.debug(Logs.DEBUG, "Upload request failed");
        }
    }

    public class JsonResponse {
        public String status = "", code = "", message = "", success = "";
    }
}
