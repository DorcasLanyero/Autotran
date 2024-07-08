package com.sdgsystems.android.amazon.s3transfer.network;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.sdgsystems.android.amazon.s3transfer.S3BucketAuth;

class IdRequest {
    private S3BucketAuth authInfo;
    private int id;

    public IdRequest(@NonNull S3BucketAuth authInfo, int id) {
        this.authInfo = authInfo;
        this.id = id;
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

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public static IdRequest createFromJson(@NonNull String json) {
        return (new Gson()).fromJson(json, IdRequest.class);
    }
}
