package com.sdgsystems.workmanagerhelper;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.cassens.autotran.Logs;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.WMRequest;
import com.cassens.autotran.data.remote.workers.HttpCallWorker;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Type;
import java.util.List;

public class WMHelperWorker extends Worker {
    private static final Logger log = LoggerFactory.getLogger(WMHelperWorker.class.getSimpleName());
    private static String TAG = "WMHelperWorker";

    public static final String WM_HELPER_TAG = "WM_HELPER";
    public static final String WM_DATA_REQUEST_ID = "WM_DATA_REQUEST_ID";
    private Context context;

    public WMHelperWorker (@NonNull Context context,
                           @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context;
    }

    @Override
    public final Result doWork() {
        Data inputData = getInputData();

        return null;
    }

    public Result doWork(Object request) {
        return null;
    };

    private static Data.Builder encodeInputDataBuilder(int wmRequestId, Object inputObject, Type inputObjectClass) {
        return new Data.Builder().putInt(WM_DATA_REQUEST_ID, wmRequestId);
    }

    public static Data encodeInputData(int wmRequestId, Object inputObject, Type inputObjectClass) {
        return encodeInputDataBuilder(wmRequestId, inputObject, inputObjectClass).build();
    }

    public static Object decodeInputData(Data inputData, Type requestClass) {
        return (new Gson()).fromJson(inputData.getString("REQUEST"), requestClass);
    }

    public static WorkRequest encodeOneTimeWMRequest(Context context, Object inputObject, Type inputObjectClass) {
        int wmRequestId = (int)DataManager.insertEmptyWMRequestToLocalDB(context);

        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(HttpCallWorker.class)
                .setInputData((new Data.Builder().putInt(WM_DATA_REQUEST_ID, wmRequestId).build()));
        WorkRequest workRequest = builder.addTag(WM_HELPER_TAG).build();
        //WorkManager.getInstance(context).pruneWork();
        DataManager.insertWMRequestToLocalDB(context, wmRequestId, workRequest.getId().toString(),
                                        (new Gson()).toJson(inputObject, inputObjectClass), 0);
        log.debug(Logs.DEBUG, "HTTP_WORKER: Encoded and saved WMRequest.id: " + wmRequestId);
        log.debug(Logs.DEBUG, "HTTP_WORKER: Encoded and saved WMRequest.uuid: " + workRequest.getId());
        return workRequest;
    }

    public static Object decodeWMRequest(Context context, Data inputData, Type requestClass) {
        int wmRequestId = inputData.getInt(WM_DATA_REQUEST_ID, -1);
        if (wmRequestId < 0) {
            log.debug(Logs.DEBUG, "HTTP_WORKER: Invalid request ID");
        }

        WMRequest wmRequest = null;
        Object requestData = null;
        String resultMsg = "Decoded and";
        try {
            wmRequest = DataManager.getWMRequest(context, wmRequestId);
            if (wmRequest == null) {
                log.debug(Logs.DEBUG, "HTTP_WORKER: Error reading WMRequest.id: " + wmRequestId);
            }
            else {
                requestData = (new Gson()).fromJson(wmRequest.getJsonData(), requestClass);
                if (requestData == null) {
                    log.debug(Logs.DEBUG, "HTTP_WORKER: Error decoding WMRequest.id: " + wmRequest.toString());
                }
            }
        } catch (JsonSyntaxException ex) {
            log.debug(Logs.DEBUG, "HTTP_WORKER: Invalid JSON for WMRequest.id: " + wmRequestId);
        } finally {
            DataManager.deleteWMRequest(context, wmRequestId);
            String msg = "Decoding error prior to deleting";
            if (requestData != null) {
                msg = "Decoded and deleted";
            }
            log.debug(Logs.DEBUG, String.format("HTTP_WORKER: %s WMRequest.id: %d", msg, wmRequestId));
            if (wmRequest != null) {
                log.debug(Logs.DEBUG, String.format("HTTP_WORKER: %s WMRequest.uuid: %s", msg, wmRequest.getUuid()));
            }
        }
        return requestData;
    }

    public static List<WorkInfo> getWMHelperRequests(Context context) {
        try {
            return WorkManager.getInstance(context).getWorkInfosByTag(WM_HELPER_TAG).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void pruneRequests(Context context) {
        WorkManager.getInstance(context).pruneWork();
    }
}
