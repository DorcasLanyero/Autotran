package com.sdgsystems.android.amazon.s3transfer.network;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.sdgsystems.android.amazon.s3transfer.S3BucketAuth;

class DownloadRequest {
    private S3BucketAuth authInfo;
    private String[] keys;

    public DownloadRequest(@NonNull S3BucketAuth authInfo, @NonNull String[] keys) {
        this.authInfo = authInfo;
        this.keys = keys;
    }

    public S3BucketAuth getAuthInfo() {
        return authInfo;
    }

    public void setAuthInfo(S3BucketAuth authInfo) {
        this.authInfo = authInfo;
    }

    public String toJson() {
        return (new Gson()).toJson(this, this.getClass());
    }

    public String[] getKeys() {
        return keys;
    }

    public void setKeys(String[] keys) {
        this.keys = keys;
    }

    public static DownloadRequest createFromJson(String json) {
        return (new Gson()).fromJson(json, DownloadRequest.class);
    }
}
