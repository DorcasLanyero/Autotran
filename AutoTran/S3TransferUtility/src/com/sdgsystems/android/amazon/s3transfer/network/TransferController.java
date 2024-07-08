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

import com.sdgsystems.android.amazon.s3transfer.S3BucketAuth;
import com.sdgsystems.android.amazon.s3transfer.models.S3Container;
import com.sdgsystems.android.amazon.s3transfer.models.TransferModel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;

/*
 * This class is a bridge to WorkManager, making it easy to have the WorkManager
 * do operations for us. This just makes it so that we don't have to worry about
 * missing parameters in the Data bundle.
 *
 * This module previously used the IntentService, NetworkService. However, as of
 * Android 8, starting IntentServices in the background is no longer permitted.
 */
public class TransferController {
    private static final Logger log = LoggerFactory.getLogger(TransferController.class.getSimpleName());

    public static void abort(Context context, TransferModel model, S3BucketAuth authInfo) {
        enqueueIdRequest(context, NetworkWorker.ACTION_ABORT,  model, authInfo);
    }

    public static void upload(Context context, ArrayList<S3Container> containers, S3BucketAuth authInfo) {
        uploadWithId(context, containers, authInfo, -1);
    }

    public static void uploadWithId(Context context, ArrayList<S3Container> containers, S3BucketAuth authInfo, int requestId) {
        UploadRequest uploadRequest = new UploadRequest(authInfo, containers, requestId);
        NetworkWorker.enqueueTransferRequest(context, Intent.ACTION_SEND, uploadRequest, UploadRequest.class);
    }

    public static void download(Context context, String[] keys, S3BucketAuth authInfo) {
        DownloadRequest downloadRequest = new DownloadRequest(authInfo, keys);
        NetworkWorker.enqueueTransferRequest(context, Intent.ACTION_GET_CONTENT, downloadRequest, DownloadRequest.class);
    }

    public static void pause(Context context, TransferModel model, S3BucketAuth authInfo) {
        enqueueIdRequest(context, NetworkWorker.ACTION_PAUSE,  model, authInfo);
    }

    public static void resume(Context context, TransferModel model, S3BucketAuth authInfo) {
        enqueueIdRequest(context, NetworkWorker.ACTION_RESUME,  model, authInfo);
    }

    private static void enqueueIdRequest(Context context, String action, TransferModel model, S3BucketAuth authInfo) {
        IdRequest idRequest = new IdRequest(authInfo, model.getId());
        NetworkWorker.enqueueTransferRequest(context, action, idRequest, IdRequest.class);
    }
}
