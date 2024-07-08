package com.sdgsystems.android.amazon.s3transfer.models;

import android.net.Uri;

import java.io.Serializable;

public class S3Container implements Serializable {

    public int id;
    public String uri;
    public String foldername;
    public boolean isImage;

    public S3Container() {
        id = -1;
        uri = "";
        foldername = "";
        isImage = false;
    }

    public S3Container(int id, String uri, String foldername, boolean isImage) {
        this.id = id;
        this.uri = uri;
        this.foldername = foldername;
        this.isImage = isImage;
    }
}
