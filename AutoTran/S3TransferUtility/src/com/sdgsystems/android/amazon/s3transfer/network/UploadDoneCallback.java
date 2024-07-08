package com.sdgsystems.android.amazon.s3transfer.network;

public interface UploadDoneCallback {
    public void onUploadDone();
    public void onUploadFailed();
}