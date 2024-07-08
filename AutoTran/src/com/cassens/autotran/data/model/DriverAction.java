package com.cassens.autotran.data.model;


import com.cassens.autotran.constants.Constants;

public final class DriverAction {
    private int id;
    private int driver_id;
    private String action;
    private String data;
    private String status;
    private String sender_id;
    private String created;
    private String processed;
    private String received;

    public String getReceived(){
        return received;
    }

    public void setReceived(String received) {
        this.received = received;
    }

    public String getProcessed() {
        return processed;
    }

    public void setProcessed(String processed) {
        this.processed = processed;
    }

    public int uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;

    public int getUploadStatus() {
        return uploadStatus;
    }

    public void setUploadStatus(int uploadStatus) {
        this.uploadStatus = uploadStatus;
    }

    public String getCreated() {
        return created;
    }

    public void setCreated(String created) {
        this.created = created;
    }

    public DriverAction() {}


    public int getId() {
        return id;
    }

    public String getSender_id() {
        return sender_id;
    }

    public void setSender_id(String sender_id) {
        this.sender_id = sender_id;
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

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getData() {
        return data;
    }

    public void setData(String data) {
        this.data = data;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
