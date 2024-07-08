package com.cassens.autotran.data.model;

import com.cassens.autotran.constants.Constants;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

public class ProblemReport {
    public transient int id = -1;
    public String guid;
    public int driver_id;
    public String category;
    public String description;
    public Long timestamp;
    public double latitude;
    public double longitude;
    public int imageCount;
    public int upload_status;

    public List<Image> images;

    public ProblemReport() {
        this.description = "";
        this.category = "";
        this.latitude = 0.0;
        this.longitude = 0.0;
        this.imageCount = 0;
        this.upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
        this.guid = UUID.randomUUID().toString();

        images = new ArrayList<>();

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDriver_id() {
        return driver_id;
    }

    public void setDriver_id(int driver_id) {
        this.driver_id = driver_id;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    public int getUpload_status() {
        return upload_status;
    }

    public void setUpload_status(int upload_status) {
        this.upload_status = upload_status;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
