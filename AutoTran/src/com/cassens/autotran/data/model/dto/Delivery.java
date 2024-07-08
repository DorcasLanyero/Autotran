package com.cassens.autotran.data.model.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adam on 8/18/15.
 */
public final class Delivery {
    private int id;
    private int delivery_id;
    private int load_id;
    private String ldnbr;
    private String status;
    private String download_status;
    private String dealer_signature;
    private String dealer_comment;
    private String dealer_contact;
    private String dealer_email;
    private double dealer_latitude;
    private double dealer_longitude;
    private String dealer_signature_signedat;
    private String driver_signature;
    private String driver_comment;
    private String driver_contact;
    private double driver_latitude;
    private double driver_longitude;
    private String driver_signature_signedat;
    private String sti;
    private String afrhrs;
    private String delivery;
    private char callback;
    private String timezone;
    private int imageCount;
    private boolean shuttleLoad;
    private boolean uploaded;
    private String notes;
    private String safe_delivery;

    private String truck_number;

    public List<DeliveryVin> deliveryVins;
    private String MACAddress;
    public List<Image> images;

    public Delivery() {
        this.deliveryVins = new ArrayList<>();
        this.images = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDelivery_id() {
        return delivery_id;
    }

    public void setDelivery_id(int delivery_id) {
        this.delivery_id = delivery_id;
    }

    public int getLoad_id() {
        return load_id;
    }

    public void setLoad_id(int load_id) {
        this.load_id = load_id;
    }

    public String getLdnbr() {
        return ldnbr;
    }

    public void setLdnbr(String ldnbr) {
        this.ldnbr = ldnbr;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDownload_status() {
        return download_status;
    }

    public void setDownload_status(String download_status) {
        this.download_status = download_status;
    }

    public String getDealer_signature() {
        return dealer_signature;
    }

    public void setDealer_signature(String dealer_signature) {
        this.dealer_signature = dealer_signature;
    }

    public String getDealer_comment() {
        return dealer_comment;
    }

    public void setDealer_comment(String dealer_comment) {
        this.dealer_comment = dealer_comment;
    }

    public String getDealer_contact() {
        return dealer_contact;
    }

    public void setDealer_contact(String dealer_contact) {
        this.dealer_contact = dealer_contact;
    }

    public String getDealer_email() {
        return dealer_email;
    }

    public void setDealer_email(String dealer_email) {
        this.dealer_email = dealer_email;
    }

    public double getDealer_latitude() {
        return dealer_latitude;
    }

    public void setDealer_latitude(double dealer_latitude) {
        this.dealer_latitude = dealer_latitude;
    }

    public double getDealer_longitude() {
        return dealer_longitude;
    }

    public void setDealer_longitude(double dealer_longitude) {
        this.dealer_longitude = dealer_longitude;
    }

    public String getDealer_signature_signedat() {
        return dealer_signature_signedat;
    }

    public void setDealer_signature_signedat(String dealer_signature_signedat) {
        this.dealer_signature_signedat = dealer_signature_signedat;
    }

    public String getDriver_signature() {
        return driver_signature;
    }

    public void setDriver_signature(String driver_signature) {
        this.driver_signature = driver_signature;
    }

    public String getDriver_comment() {
        return driver_comment;
    }

    public void setDriver_comment(String driver_comment) {
        this.driver_comment = driver_comment;
    }

    public String getDriver_contact() {
        return driver_contact;
    }

    public void setDriver_contact(String driver_contact) {
        this.driver_contact = driver_contact;
    }

    public double getDriver_latitude() {
        return driver_latitude;
    }

    public void setDriver_latitude(double driver_latitude) {
        this.driver_latitude = driver_latitude;
    }

    public double getDriver_longitude() {
        return driver_longitude;
    }

    public void setDriver_longitude(double driver_longitude) {
        this.driver_longitude = driver_longitude;
    }

    public String getDriver_signature_signedat() {
        return driver_signature_signedat;
    }

    public void setDriver_signature_signedat(String driver_signature_signedat) {
        this.driver_signature_signedat = driver_signature_signedat;
    }

    public String getSti() {
        return sti;
    }

    public void setSti(String sti) {
        this.sti = sti;
    }

    public String getAfrhrs() {
        return afrhrs;
    }

    public void setAfrhrs(String afrhrs) {
        this.afrhrs = afrhrs;
    }

    public String getDelivery() {
        return delivery;
    }

    public void setDelivery(String delivery) {
        this.delivery = delivery;
    }

    public char getCallback() {
        return callback;
    }

    public void setCallback(char callback) {
        this.callback = callback;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    public boolean isShuttleLoad() {
        return shuttleLoad;
    }

    public void setShuttleLoad(boolean shuttleLoad) {
        this.shuttleLoad = shuttleLoad;
    }

    public boolean isUploaded() {
        return uploaded;
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setMACAddress(String MACAddress) {
        this.MACAddress = MACAddress;
    }

    public String getMACAddress() {
        return MACAddress;
    }

    public String getTruck_number() {
        return truck_number;
    }

    public void setTruck_number(String truck_number) {
        this.truck_number = truck_number;
    }

    public String getSafe_delivery() {
        return safe_delivery;
    }

    public void setSafe_delivery(String safe_delivery) {
        this.safe_delivery = safe_delivery;
    }
}
