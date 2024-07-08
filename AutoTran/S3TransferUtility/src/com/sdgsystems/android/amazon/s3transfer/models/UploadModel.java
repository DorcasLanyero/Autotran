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

package com.sdgsystems.android.amazon.s3transfer.models;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.ContentResolver;
import android.content.Context;
import android.net.Uri;
import android.os.Handler;
import androidx.core.app.NotificationCompat;
import android.util.Log;
import android.webkit.MimeTypeMap;

import com.cassens.autotran.CommonUtility;
import com.sdgsystems.android.amazon.s3transfer.S3BucketAuth;
import com.amazonaws.services.s3.model.ProgressEvent;
import com.amazonaws.services.s3.model.ProgressListener;
import com.amazonaws.mobileconnectors.s3.transfermanager.PersistableUpload;
import com.amazonaws.mobileconnectors.s3.transfermanager.Transfer;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;
import com.amazonaws.mobileconnectors.s3.transfermanager.exception.PauseException;
import com.sdgsystems.android.amazon.s3transfer.network.UploadDoneCallback;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;
import java.util.Locale;

/* UploadModel handles the interaction between the Upload and TransferManager.
 * This also makes sure that the file that is uploaded has the same file extension
 *
 * One thing to note is that we always create a copy of the file we are given. This
 * is because we wanted to demonstrate pause/resume which is only possible with a
 * File parameter, but there is no reliable way to get a File from a Uri(mainly
 * because there is no guarantee that the Uri has an associated File).
 *
 * You can easily avoid this by directly using an InputStream instead of a Uri.
 */
public class UploadModel extends TransferModel {
    private static final String TAG = "UploadModel";
    public static final int NOTIFICATION_ID = 11234;

    private Upload mUpload;
    private PersistableUpload mPersistableUpload;
    private ProgressListener mListener;
    private Status mStatus;
    private File mFile;
    private String mExtension;
    private NotificationManager mNotificationManager;
    private Notification mUploadProgressNotification;
    private NotificationCompat.Builder mNotificationBuilder;
    private long mTotalBytes = 0;
    private long mBytesTransferred = 0;

    private long lastUpdate = 0;

    public UploadModel(Context context, Uri uri, TransferManager manager, final UploadDoneCallback callback) {
        super(context, uri, manager);
        mStatus = Status.IN_PROGRESS;
        mExtension = MimeTypeMap.getSingleton().getExtensionFromMimeType(
                context.getContentResolver().getType(uri));
        ;
        mNotificationManager = (NotificationManager) getContext().getSystemService(Context.NOTIFICATION_SERVICE);
        mUploadProgressNotification = null;
        mNotificationBuilder = null;

        mListener = new ProgressListener() {
            @Override
            public void progressChanged(ProgressEvent event) {
                if (event.getEventCode() == ProgressEvent.COMPLETED_EVENT_CODE) {
                    mStatus = Status.COMPLETED;
                    if (mFile != null) {
                        mFile.delete();
                    }

                    mNotificationManager.cancel(NOTIFICATION_ID);
                    Handler h = new Handler(getContext().getMainLooper());
                    h.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            CommonUtility.showText("Files uploaded.");
                        }
                    }, 0);
                    Log.w(TAG, "upload done" );
                    callback.onUploadDone();
                }
                else if(event.getBytesTransferred() > 0) {
                    long currentTime = new Date().getTime();

                    mBytesTransferred += event.getBytesTransferred();

                    int kbTransferred = (int) mBytesTransferred / 1024;
                    int kbTotal = (int) mTotalBytes / 1024;

                    //Only update once a second I think this is slowing down the UI?
                    if(lastUpdate == 0 || currentTime - lastUpdate > 500) {

                        lastUpdate = currentTime;

                        Log.d(TAG, "Logupload: " + kbTransferred + " / " + kbTotal);

                        if(mNotificationBuilder == null) {
                            mNotificationBuilder = new NotificationCompat.Builder(getContext());
                        }

                        mUploadProgressNotification = mNotificationBuilder
                            .setTicker("Upload in progress")
                            .setContentTitle("Upload in progress")
                            .setContentText(kbTransferred + "kb / " + kbTotal + "kb")
                            .setSmallIcon(android.R.drawable.stat_sys_upload)
                            .setProgress(kbTotal, kbTransferred, false).build();

                        if (mStatus == Status.IN_PROGRESS)
                            mNotificationManager.notify(NOTIFICATION_ID, mUploadProgressNotification);
                    }
                }
                else if(event.getEventCode() == ProgressEvent.STARTED_EVENT_CODE) {

                    if(mNotificationBuilder == null) {
                        mNotificationBuilder = new NotificationCompat.Builder(getContext());
                    }

                    mUploadProgressNotification = mNotificationBuilder
                            .setTicker("Upload in progress")
                            .setContentTitle("Upload in progress")
                            .setSmallIcon(android.R.drawable.stat_sys_upload)
                            .setProgress(0, 0, true).build();
                    if(mStatus == Status.IN_PROGRESS) mNotificationManager.notify(NOTIFICATION_ID, mUploadProgressNotification);
                    mBytesTransferred = 0;
                }
                else if(event.getEventCode() == ProgressEvent.FAILED_EVENT_CODE) {
                    mNotificationManager.cancel(NOTIFICATION_ID);
                    callback.onUploadFailed();
                }
            }
        };
    }

    public Runnable getUploadRunnable(final S3BucketAuth authInfo) {
        return new Runnable() {
            @Override
            public void run() {
                upload(authInfo);
            }
        };
    }

    @Override
    public void abort() {
        if (mUpload != null) {
            mStatus = Status.CANCELED;
            mUpload.abort();
            if (mFile != null) {
                mFile.delete();
            }
        }
    }

    @Override
    public Status getStatus() {
        return mStatus;
    }

    @Override
    public Transfer getTransfer() {
        return mUpload;
    }

    @Override
    public void pause() {
        if (mStatus == Status.IN_PROGRESS) {
            if (mUpload != null) {
                mStatus = Status.PAUSED;
                try {
                    mPersistableUpload = mUpload.pause();
                } catch (PauseException e) {
                    Log.d(TAG, "", e);
                }
            }
        }
    }

    @Override
    public void resume(S3BucketAuth authInfo) {
        if (mStatus == Status.PAUSED) {
            mStatus = Status.IN_PROGRESS;
            if (mPersistableUpload != null) {
                // if it paused fine, resume
                mUpload = getTransferManager().resumeUpload(mPersistableUpload);
                mUpload.addProgressListener(mListener);
                mPersistableUpload = null;
            } else {
                // if it was actually aborted, start a new one
                upload(authInfo);
            }
        }
    }

    public void upload(S3BucketAuth authInfo) {
        if (mFile == null) {
            saveTempFile();
        }
        if (mFile != null) {
            try {
            	
            	String finalName = authInfo.folderName + "/" + super.getFileName();
        		
        		if(mExtension != null && !mExtension.equals("null")) {
        			finalName += "." + mExtension;
        		}
                        
                mTotalBytes = mFile.length();
                mUpload = getTransferManager().upload(
                		authInfo.BUCKET_NAME.toLowerCase(Locale.US),
                		finalName,
                        mFile);
                mUpload.addProgressListener(mListener);
            } catch (Exception e) {
                Log.e(TAG, "", e);
            }
        }
    }

    private void saveTempFile() {
        ContentResolver resolver = getContext().getContentResolver();
        InputStream in = null;
        FileOutputStream out = null;

        try {
            in = resolver.openInputStream(getUri());
            mFile = File.createTempFile(
                    "s3_demo_file_" + getId(),
                    mExtension,
                    getContext().getCacheDir());
            out = new FileOutputStream(mFile, false);
            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
        } catch (IOException e) {
            Log.e(TAG, "", e);
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
            if (out != null) {
                try {
                    out.close();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        }
    }
}
