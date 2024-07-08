/*
 * Copyright 2010-2014 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License").
 * You may not use this file except in compliance with the License.
 * A copy of the License is located at
 *
 *  http://aws.amazon.com/apache2.0
 *
 * or in the "license" file accompanying this file. This file is distributed
 * on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing
 * permissions and limitations under the License.
 */

package com.sdgsystems.android.amazon.s3transfer.network;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.services.s3.AmazonS3Client;
import com.cassens.autotran.Logs;
import com.cassens.autotran.data.EventBusManager;
import com.cassens.autotran.data.event.NetworkEvent;
import com.cassens.autotran.data.event.S3UploadEvent;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.remote.S3UploadResultReceiver;
import com.google.gson.Gson;
import com.sdgsystems.android.amazon.s3transfer.S3BucketAuth;
import com.sdgsystems.android.amazon.s3transfer.Util;
import com.sdgsystems.android.amazon.s3transfer.models.DownloadModel;
import com.sdgsystems.android.amazon.s3transfer.models.S3Container;
import com.sdgsystems.android.amazon.s3transfer.models.TransferModel;
import com.sdgsystems.android.amazon.s3transfer.models.UploadModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/*
 * This class handles starting all the downloads/uploads. We use WorkManager to do this
 * so that the transfers will continue even though the activity has ended(ie due to
 * orientation change).
 *
 * This replaces the prior NetworkService, which was an IntentService. Beginning with
 * Android 8, IntentServices cannot be invoked from a backgrounded app.
 */
public class NetworkWorker extends Worker {
    private static final Logger log = LoggerFactory.getLogger(NetworkWorker.class.getSimpleName());

    public static final String ACTION_ABORT = "abort";
    public static final String ACTION_PAUSE = "pause";
    public static final String ACTION_RESUME = "resume";
    public static final String ACTION_S3_SUCCEEDED = "s3_succeeded";
    public static final String ACTION_S3_FAILED = "s3_failed";

    private static final String TAG = "NetworkWorker";
    private static final int DEFAULT_INT = -1;

    private TransferManager mTransferManager;

    private S3BucketAuth mAuthInfo;

    private ArrayList<S3Container> mContainerList;
    private Context mContext;
    int mIndex;

    public final static String S3_TRANSFER_TAG = "s3transfer";
    public final static String KEY_ACTION = "action";
    public final static String KEY_JSON_DATA = "jsonData";

    public NetworkWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {

        super(context, params);

        this.mContext = context;
    }

    public static Data encodeInputData(String action, Object inputObject, Type inputObjectClass) {
        return new Data.Builder()
                .putString(KEY_ACTION, action)
                .putString(KEY_JSON_DATA, new Gson().toJson(inputObject, inputObjectClass))
                .build();
    }

    public static void enqueueTransferRequest(Context context, String action, Object inputObject, Type inputObjectClass) {
        OneTimeWorkRequest.Builder builder = new OneTimeWorkRequest.Builder(NetworkWorker.class)
                .setInputData(encodeInputData(action, inputObject, inputObjectClass));
        WorkRequest workRequest = builder.addTag(S3_TRANSFER_TAG).build();
        log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_2: NetworkWorker: enqueuing transfer request: " + workRequest.getId());
        WorkManager.getInstance(context).enqueue(workRequest);
        listS3TransferRequests(context);
    }

    @Override
    public void onStopped() {
        log.debug(Logs.DEBUG, "REQUEST STOPPED: " + this.getId());
    }

    @Override
    public Result doWork() {
        //log.debug(Logs.DEBUG, "NetworkWorker: in doWork()");
        Data inputData = getInputData();

        String action = inputData.getString(KEY_ACTION);
        String jsonData = inputData.getString(KEY_JSON_DATA);

    	int requestId = DEFAULT_INT;

        if (action != null) {
            if (action.equals(Intent.ACTION_GET_CONTENT)) {
                DownloadRequest downloadRequest = DownloadRequest.createFromJson(jsonData);
                initializeTransferSetup(downloadRequest.getAuthInfo());
                download(downloadRequest);
            } else if (action.equals(Intent.ACTION_SEND)) {
                UploadRequest uploadRequest = UploadRequest.createFromJson(jsonData);
                initializeTransferSetup(uploadRequest.getAuthInfo());
                mIndex = 0;
                mContainerList = uploadRequest.getContainers();
                requestId = uploadRequest.getRequestId();

                if (mIndex < mContainerList.size() && mContainerList.get(mIndex).isImage) {
                    log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_3: doWork(): Photo S3 upload starting: List of " + mContainerList.size() + " photos");
                    uploadPhoto(requestId);
                } else {
                    log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_3: doWork(): Image S3 upload starting: List of " + mContainerList.size() + " images");
                    uploadNext(requestId);
                }
            } else {
                IdRequest idRequest = IdRequest.createFromJson(jsonData);
                initializeTransferSetup(idRequest.getAuthInfo());
                int notifId = idRequest.getId();
                if (action.equals(ACTION_PAUSE)) {
                    pause(notifId);
                } else if (action.equals(ACTION_ABORT)) {
                    abort(notifId);
                } else if (action.equals(ACTION_RESUME)) {
                    resume(notifId);
                }
            }
        }

        // Indicate whether the work finished successfully with the Result
        // For now, we return success since other parts of the code do the
        // status checking and retries.  Eventually we should transition to
        // using the built-in retry capabilities of WorkManager.
        return Result.success();
    }

    private void initializeTransferSetup(S3BucketAuth authInfo) {
        mAuthInfo = authInfo;

        ClientConfiguration clientConf = new ClientConfiguration();

        clientConf.setSocketTimeout(120000);

        AmazonS3Client s3Client = new AmazonS3Client(Util.getCredProvider(mContext, mAuthInfo), clientConf);

        mTransferManager = new TransferManager(s3Client);
    }


    private void uploadPhoto(final int requestId) {
        if(mIndex < mContainerList.size()) {
            log.debug(Logs.UPLOAD, String.format("Running upload for photo container %d", mIndex));

            UploadDoneCallback callback = new UploadDoneCallback() {
                @Override
                public void onUploadDone() {
                    log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_4: onUploadDone() callback");
                    log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_4: sending success action to S3UploadResultReceiver");
                    Intent intent = new Intent(mContext, S3UploadResultReceiver.class);
                    intent.setAction(ACTION_S3_SUCCEEDED);
                    intent.putExtra("image_id", mContainerList.get(mIndex).id);
                    mContext.sendBroadcast(intent);

                    EventBusManager.getInstance().publish(new S3UploadEvent(requestId, NetworkEvent.Result.SUCCESS));
                }

                @Override
                public void onUploadFailed() {
                    log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_4: onUploadFailed() callback");
                    Image image = DataManager.getImage(mContext, mContainerList.get(mIndex).id);

                    log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_4: sending failure action to S3UploadResultReceiver");
                    Intent intent = new Intent(mContext, S3UploadResultReceiver.class);
                    intent.setAction(ACTION_S3_FAILED);
                    intent.putExtra("image_id", mContainerList.get(mIndex).id);
                    mContext.sendBroadcast(intent);

                    EventBusManager.getInstance().publish(new S3UploadEvent(requestId, NetworkEvent.Result.S3_ERROR));
                }
            };

            final UploadModel model = new UploadModel(mContext, Uri.parse(mContainerList.get(mIndex).uri), mTransferManager, callback);
            mAuthInfo.folderName = mContainerList.get(mIndex).foldername;
            model.getUploadRunnable(mAuthInfo).run();
        }
        else {
           log.debug(Logs.UPLOAD, "Photo S3 upload finished");
        }
    }

    public void uploadNext(final int requestId) {
        Log.d(TAG, "uploadNext: " + mIndex);
        if(mIndex < mContainerList.size()){
            UploadDoneCallback callback = new UploadDoneCallback() {
                @Override
                public void onUploadDone() {
                    Log.d(TAG, "uploadNext: Hit onUploadDone() callback");
                    mIndex++;
                    uploadNext(requestId);
                }

                @Override
                public void onUploadFailed() {
                    Log.d(TAG, "uploadNext: Hit onUploadFailed() callback");

                    EventBusManager.getInstance().publish(new S3UploadEvent(requestId, NetworkEvent.Result.S3_ERROR));
                }
            };
            final UploadModel model = new UploadModel(mContext, Uri.parse(mContainerList.get(mIndex).uri), mTransferManager, callback);
            model.getUploadRunnable(mAuthInfo).run();
            new File(mContainerList.get(mIndex).uri).delete();
        }
        else {
            EventBusManager.getInstance().publish(new S3UploadEvent(requestId, NetworkEvent.Result.SUCCESS));
        }
    }

    private void abort(int notifId) {
        TransferModel model = TransferModel.getTransferModel(notifId);
        model.abort();
    }

    private void download(DownloadRequest request) {
        String[] keys = request.getKeys();
        for (int i = 0; i < keys.length; i++) {
            DownloadModel model = new DownloadModel(mContext, keys[i], mTransferManager);
            model.download(mAuthInfo.BUCKET_NAME);
        }
    }

    private void pause(int notifId) {
        TransferModel model = TransferModel.getTransferModel(notifId);
        model.pause();
    }

    private void resume(int notifId) {
        TransferModel model = TransferModel.getTransferModel(notifId);
        model.resume(mAuthInfo);
    }

    public static void listS3TransferRequests(Context context) {
        try {
            List<WorkInfo> requestList = WorkManager.getInstance(context).getWorkInfosByTag(S3_TRANSFER_TAG).get();
            if (requestList == null) {
                log.debug(Logs.DEBUG, "NetworkWorker: getWorkerInfosByTag() returned null");
                return;
            }
            int finished = 0;
            for (WorkInfo request : requestList) {
                if (request.getState().isFinished()) {
                    finished++;
                }
                else {
                    //log.debug(Logs.DEBUG, "NetworkWorker Request id: " + request.getId() + " Status: " + request.getState().toString());
                }
            }
            log.debug(Logs.DEBUG, "NetworkWorker: Finished requests: " + finished + " out of " + requestList.size() + " total requests.");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
