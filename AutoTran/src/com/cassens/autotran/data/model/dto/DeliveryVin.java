package com.cassens.autotran.data.model.dto;

import com.cassens.autotran.data.model.VIN;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adam on 8/18/15.
 */
public final class DeliveryVin {
    private int id;
    private int delivery_vin_id;
    private int delivery_id;
    private int vin_id;
    private short status;
    private String user_type;
    private String rejected_by;
    private short ldseq;
    private String pro;
    //private String position;
    private short position;
    private char backdrv;
    private char rldspickup;
    private int bckhlnbr;
    private String lot;
    private String rowbay;
    private String rte1;
    private String rte2;
    private String von;
    private String device_token;
    private String supervisor_signature;
    private String supervisor_comment;
    private String supervisor_contact;
    private String finalMfg;
    private String finalDealer;
    private double supervisor_latitude;
    private double supervisor_longitude;
    private String supervisor_signature_signedat;
    private boolean inspected_preload;
    private String preload_notes;
    private boolean inspected_delivery;
    private String delivery_notes;
    private String timezone;
    private int preloadImageCount;
    private int deliveryImageCount;
    private String shuttleLoadProductionStatus;
    private String shuttleLoadRoute;
    private VIN vin;
    private String selection_history;

    public List<Damage> damages;

    public ArrayList<Image> images;

    public DeliveryVin() {
        damages = new ArrayList<>();
        images = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getDelivery_vin_id() {
        return delivery_vin_id;
    }

    public void setDelivery_vin_id(int delivery_vin_id) {
        this.delivery_vin_id = delivery_vin_id;
    }

    public int getDelivery_id() {
        return delivery_id;
    }

    public void setDelivery_id(int delivery_id) {
        this.delivery_id = delivery_id;
    }

    public int getVin_id() {
        return vin_id;
    }

    public void setVin_id(int vin_id) {
        this.vin_id = vin_id;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getUser_type() {
        return user_type;
    }

    public void setUser_type(String user_type) {
        this.user_type = user_type;
    }

    public String getRejected_by() {
        return rejected_by;
    }

    public void setRejected_by(String rejected_by) {
        this.rejected_by = rejected_by;
    }

    public short getLdseq() {
        return ldseq;
    }

    public void setLdseq(short ldseq) {
        this.ldseq = ldseq;
    }

    public String getPro() {
        return pro;
    }

    public void setPro(String pro) {
        this.pro = pro;
    }

    public short getPosition() {
        return position;
    }

    public void setPosition(short position) {
        this.position = position;
    }

    public char getBackdrv() {
        return backdrv;
    }

    public void setBackdrv(char backdrv) {
        this.backdrv = backdrv;
    }

    public char getRldspickup() {
        return rldspickup;
    }

    public void setRldspickup(char rldspickup) {
        this.rldspickup = rldspickup;
    }

    public int getBckhlnbr() {
        return bckhlnbr;
    }

    public void setBckhlnbr(int bckhlnbr) {
        this.bckhlnbr = bckhlnbr;
    }

    public String getLot() {
        return lot;
    }

    public void setLot(String lot) {
        this.lot = lot;
    }

    public String getRowbay() {
        return rowbay;
    }

    public void setRowbay(String rowbay) {
        this.rowbay = rowbay;
    }

    public String getRte1() {
        return rte1;
    }

    public void setRte1(String rte1) {
        this.rte1 = rte1;
    }

    public void setfinalMfg(String finalMfg){
        this.finalMfg = finalMfg;
    }

    public String getfinalMfg() {
        return finalMfg;
    }

    private void setFinalDealer(String finalDealer){
        this.finalDealer = finalDealer;
    }

    private String getFinalDealer() {
        return finalDealer;
    }

    public String getRte2() {
        return rte2;
    }

    public void setRte2(String rte2) {
        this.rte2 = rte2;
    }

    public String getVon() {
        return von;
    }

    public void setVon(String von) {
        this.von = von;
    }

    public String getDevice_token() {
        return device_token;
    }

    public void setDevice_token(String device_token) {
        this.device_token = device_token;
    }

    public String getSupervisor_signature() {
        return supervisor_signature;
    }

    public void setSupervisor_signature(String supervisor_signature) {
        this.supervisor_signature = supervisor_signature;
    }

    public String getSupervisor_comment() {
        return supervisor_comment;
    }

    public void setSupervisor_comment(String supervisor_comment) {
        this.supervisor_comment = supervisor_comment;
    }

    public String getSupervisor_contact() {
        return supervisor_contact;
    }

    public void setSupervisor_contact(String supervisor_contact) {
        this.supervisor_contact = supervisor_contact;
    }

    public double getSupervisor_latitude() {
        return supervisor_latitude;
    }

    public void setSupervisor_latitude(double supervisor_latitude) {
        this.supervisor_latitude = supervisor_latitude;
    }

    public double getSupervisor_longitude() {
        return supervisor_longitude;
    }

    public void setSupervisor_longitude(double supervisor_longitude) {
        this.supervisor_longitude = supervisor_longitude;
    }

    public String getSupervisor_signature_signedat() {
        return supervisor_signature_signedat;
    }

    public void setSupervisor_signature_signedat(String supervisor_signature_signedat) {
        this.supervisor_signature_signedat = supervisor_signature_signedat;
    }

    public boolean isInspected_preload() {
        return inspected_preload;
    }

    public void setInspected_preload(boolean inspected_preload) {
        this.inspected_preload = inspected_preload;
    }

    public String getPreload_notes() {
        return preload_notes;
    }

    public void setPreload_notes(String preload_notes) {
        this.preload_notes = preload_notes;
    }

    public boolean isInspected_delivery() {
        return inspected_delivery;
    }

    public void setInspected_delivery(boolean inspected_delivery) {
        this.inspected_delivery = inspected_delivery;
    }

    public String getDelivery_notes() {
        return delivery_notes;
    }

    public void setDelivery_notes(String delivery_notes) {
        this.delivery_notes = delivery_notes;
    }

    public String getTimezone() {
        return timezone;
    }

    public void setTimezone(String timezone) {
        this.timezone = timezone;
    }

    public int getPreloadImageCount() {
        return preloadImageCount;
    }

    public void setPreloadImageCount(int preloadImageCount) {
        this.preloadImageCount = preloadImageCount;
    }

    public int getDeliveryImageCount() {
        return deliveryImageCount;
    }

    public void setDeliveryImageCount(int deliveryImageCount) {
        this.deliveryImageCount = deliveryImageCount;
    }

    public String getShuttleLoadProductionStatus() {
        return shuttleLoadProductionStatus;
    }

    public void setShuttleLoadProductionStatus(String shuttleLoadProductionStatus) {
        this.shuttleLoadProductionStatus = shuttleLoadProductionStatus;
    }

    public String getShuttleLoadRoute() {
        return shuttleLoadRoute;
    }

    public void setShuttleLoadRoute(String shuttleLoadRoute) {
        this.shuttleLoadRoute = shuttleLoadRoute;
    }

    public VIN getVin() {
        return vin;
    }

    public void setVin(VIN vin) {
        this.vin = vin;
    }

    public String getSelection_history() {
        return selection_history;
    }

    public void setSelection_history(String selection_history) {
        this.selection_history = selection_history;
    }
}
