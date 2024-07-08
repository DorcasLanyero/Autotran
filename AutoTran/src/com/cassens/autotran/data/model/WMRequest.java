package com.cassens.autotran.data.model;

public class WMRequest {
    private int id = -1;
    private String uuid;
    private String jsonData;
    private int retries = 0;

    @Override
    public String toString() {
        return "WMRequest [id = " + id + ", uuid = " + uuid + ", jsonData = " + jsonData + ", retries = " + retries + "]";
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUuid() {
        return uuid;
    }

    public void setUuid(String uuid) {
        this.uuid = uuid;
    }

    public String getJsonData() {
        return jsonData;
    }

    public void setJsonData(String jsonData) {
        this.jsonData = jsonData;
    }

    public int getRetries() {
        return retries;
    }

    public void setRetries(int retries) {
        this.retries = retries;
    }
}

