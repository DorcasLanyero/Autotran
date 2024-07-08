package com.cassens.autotran.data.remote;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.cassens.autotran.Logs;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.sdgsystems.android.amazon.s3transfer.network.NetworkWorker;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class S3UploadResultReceiver extends BroadcastReceiver {
    private static final Logger log = LoggerFactory.getLogger(S3UploadResultReceiver.class.getSimpleName());
    private static String TAG = "S3UploadResultReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getAction().equals(NetworkWorker.ACTION_S3_SUCCEEDED)) {
            int image_id = intent.getIntExtra("image_id", -1);
            log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_5: s3 upload success for image: " + image_id);
            SyncManager.handleUploadPhotoToS3Response(context, true, image_id);
        } else if (intent.getAction().equals(NetworkWorker.ACTION_S3_FAILED)) {
            int image_id = intent.getIntExtra("image_id", -1);
            log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_5: s3 upload failed for image: " + image_id);
            SyncManager.handleUploadPhotoToS3Response(context, false, intent.getIntExtra("image_id", -1));
        }
        else {
            log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_5: s3 upload got unrecognized action: " + HelperFuncs.noNull(intent.getAction()));
        }

    }
}
