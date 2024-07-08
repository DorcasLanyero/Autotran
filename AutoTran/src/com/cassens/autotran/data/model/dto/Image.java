package com.cassens.autotran.data.model.dto;

import java.util.ArrayList;

public class Image {
    private Integer delivery_vin_id;
    private Integer load_id;
    private Integer delivery_id;
    private String inspection_id;
    private String filename;
    private String problem_report_guid;
    private String inspection_guid;
    private double image_latitude;
    private double image_longitude;
    private short preload_image;
    private short part;
    private String image;
    private boolean largeImage = false;
    private String preauth_url;

    public Image() {
    }

    public Integer getDelivery_vin_id() {
        return delivery_vin_id;
    }

    public void setDelivery_vin_id(Integer delivery_vin_id) {
        if (delivery_vin_id != null && delivery_vin_id == -1) {
            this.delivery_vin_id = null;
        } else {
            this.delivery_vin_id = delivery_vin_id;
        }

        if (this.delivery_vin_id != null) {
            this.load_id = null;
            this.delivery_id = null;
            this.inspection_id = null;
        }
    }

    public Integer getLoad_id() {
        return load_id;
    }

    public void setLoad_id(Integer load_id) {
        if (load_id != null && load_id == -1) {
            this.load_id = null;
        } else {
            this.load_id = load_id;
        }

        if (this.load_id != null) {
            this.delivery_vin_id = null;
            this.delivery_id = null;
            this.inspection_id = null;
        }
    }

    public Integer getDelivery_id() {
        return delivery_id;
    }

    public void setDelivery_id(Integer delivery_id) {
        if (delivery_id != null && delivery_id == -1) {
            this.delivery_id = null;
        } else {
            this.delivery_id = delivery_id;
        }

        if (this.delivery_id != null) {
            this.load_id = null;
            this.delivery_vin_id = null;
            this.inspection_id = null;
        }
    }

    public String getInspection_id() {
        return inspection_id;
    }

    public void setInspection_id(String inspection_id) {
        this.inspection_id = inspection_id;

        if (this.inspection_id != null) {
            this.load_id = null;
            this.delivery_vin_id = null;
            this.delivery_id = null;
        }
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public double getImage_latitude() {
        return image_latitude;
    }

    public void setImage_latitude(double image_latitude) {
        this.image_latitude = image_latitude;
    }

    public double getImage_longitude() {
        return image_longitude;
    }

    public void setImage_longitude(double image_longitude) {
        this.image_longitude = image_longitude;
    }

    public short getPreload_image() {
        return preload_image;
    }

    public void setPreload_image(short preload_image) {
        this.preload_image = preload_image;
    }

    public short getPart() {
        return part;
    }

    public void setPart(short part) {
        this.part = part;
    }

    public String getImage() {
        return image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public String getProblem_report_guid() {
        return problem_report_guid;
    }

    public void setProblem_report_guid(String problem_report_guid) {
        this.problem_report_guid = problem_report_guid;
    }

    public boolean isLargeImage() {
        return largeImage;
    }

    public void setLargeImage(boolean largeImage) {
        this.largeImage = largeImage;
    }

    public String getPreauth_url() {
        return preauth_url;
    }

    public void setPreauth_url(String preauthUrl) {
        this.preauth_url = preauthUrl;
    }

    public String getInspection_guid() {
        return inspection_guid;
    }

    public void setInspection_guid(String inspection_guid) {
        this.inspection_guid = inspection_guid;
    }
}
