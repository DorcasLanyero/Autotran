package com.sdgsystems.android.amazon.s3transfer.network;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.sdgsystems.android.amazon.s3transfer.S3BucketAuth;
import com.sdgsystems.android.amazon.s3transfer.models.S3Container;

import java.util.ArrayList;

class UploadRequest {
    private S3BucketAuth authInfo;
    private ArrayList<S3Container> containers;
    private int requestId;

    public UploadRequest(@NonNull S3BucketAuth authInfo, @NonNull ArrayList<S3Container> containers, int requestId) {
        this.authInfo = authInfo;
        this.containers = containers;
        this.requestId = requestId;
    }

    public ArrayList<S3Container> getContainers() {
        return containers;
    }

    public void setContainers(ArrayList<S3Container> containers) {
        this.containers = containers;
    }

    public S3BucketAuth getAuthInfo() {
        return authInfo;
    }

    public void setAuthInfo(S3BucketAuth authInfo) {
        this.authInfo = authInfo;
    }

    public int getRequestId() {
        return requestId;
    }

    public void setRequestId(int requestId) {
        this.requestId = requestId;
    }

    public String toJson() {
        return (new Gson()).toJson(this, this.getClass());
    }

    public static UploadRequest createFromJson(String json) {
        return (new Gson()).fromJson(json, UploadRequest.class);
    }
}
