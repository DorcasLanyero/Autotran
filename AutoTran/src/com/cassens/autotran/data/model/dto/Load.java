package com.cassens.autotran.data.model.dto;

import com.sdgsystems.util.HelperFuncs;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adam on 8/18/15.
 */
public final class Load {
    private int id;
    private int load_id;
    private String ldnbr;
    private int trknbr;
    private int trailerNumber;
    private int user_id;
    private String fillers;
    private String driver_preload_signature;
    private String driver_preload_comment;
    private String driver_preload_contact;
    private double driver_preload_latitude;
    private double driver_preload_longitude;
    private String driver_preload_signature_signedat;
    private String preload_supervisor_signature;
    private String preload_supervisor_signedat;
    private String supervisorSignature;
    private String supervisorSignedAt;
    private double supervisorSignatureLat;
    private double supervisorSignatureLon;
    private short status;
    private String download_status;
    private String timezone;
    private int imageCount;
    private String dispatch_date;
    private String ldtyp;
    private String ldtypedes;
    private String pickCode;
    private String pickCodeDes;
    private int dockTerm;
    private String relayldnbr;
    private int originldnbr;
    private String shipdate;
    private String estdeliverdate;
    private boolean shuttle_load;
    private int shuttle_move_id;
    private String notes;
    private String MACAddress;
    public List<Image> images;
    private int parentLoad;
    private int parent_load_id;
    private boolean pick_sheet_image_required;
    private String extra_doc_image_required;

    public String getSupervisorSignature() {
        return supervisorSignature;
    }

    public void setSupervisorSignature(String supervisorSignature) {
        this.supervisorSignature = supervisorSignature;
    }

    public boolean getPick_sheet_image_required(){return pick_sheet_image_required;}
    public void setPick_sheet_image_required(boolean pick_sheet_image_required){this.pick_sheet_image_required = pick_sheet_image_required;}

    public String getExtra_doc_image_required(){return extra_doc_image_required;}
    public void setExtra_doc_image_required(String other_document){this.extra_doc_image_required = other_document;}

    public String getSupervisorSignedAt() {
        return supervisorSignedAt;
    }

    public void setSupervisorSignedAt(String supervisorSignedAt) {
        this.supervisorSignedAt = supervisorSignedAt;
    }
    public double getSupervisorSignatureLat() {
        return supervisorSignatureLat;
    }

    private double parseDouble(String value) {
        try {
            return Double.parseDouble(HelperFuncs.noNull(value));
        }
        catch (NumberFormatException ne) {
            return 0.0;
        }
    }

    public void setSupervisorSignatureLat(double supervisorSignatureLat) {
        this.supervisorSignatureLat = supervisorSignatureLat;
    }

    public void setSupervisorSignatureLat(String supervisorSignatureLat) {
        this.supervisorSignatureLat =  parseDouble(supervisorSignatureLat);
    }

    public double getSupervisorSignatureLon() {
        return supervisorSignatureLon;
    }

    public void setSupervisorSignatureLon(String supervisorSignatureLon) {
        this.supervisorSignatureLon =  parseDouble(supervisorSignatureLon);
    }

    public void setSupervisorSignatureLon(double supervisorSignatureLon) {
        this.supervisorSignatureLon = supervisorSignatureLon;
    }

    public String getDriverHighClaimsAudit() {
        return driverHighClaimsAudit;
    }

    public void setDriverHighClaimsAudit(String driverHighClaimsAudit) {
        this.driverHighClaimsAudit = driverHighClaimsAudit;
    }

    private String driverHighClaimsAudit;

    public List<Delivery> deliveries;

    public Load() {
        this.deliveries = new ArrayList<>();
        this.images = new ArrayList<>();
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
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

    public int getTrknbr() {
        return trknbr;
    }

    public void setTrknbr(int trknbr) {
        this.trknbr = trknbr;
    }

    public int getUser_id() {
        return user_id;
    }

    public void setUser_id(int user_id) {
        this.user_id = user_id;
    }

    public String getFillers() {
        return fillers;
    }

    public void setFillers(String fillers) {
        this.fillers = fillers;
    }

    public String getDriver_preload_signature() {
        return driver_preload_signature;
    }

    public void setDriver_preload_signature(String driver_preload_signature) {
        this.driver_preload_signature = driver_preload_signature;
    }

    public String getDriver_preload_comment() {
        return driver_preload_comment;
    }

    public void setDriver_preload_comment(String driver_preload_comment) {
        this.driver_preload_comment = driver_preload_comment;
    }

    public String getDriver_preload_contact() {
        return driver_preload_contact;
    }

    public void setDriver_preload_contact(String driver_preload_contact) {
        this.driver_preload_contact = driver_preload_contact;
    }

    public double getDriver_preload_latitude() {
        return driver_preload_latitude;
    }

    public void setDriver_preload_latitude(double driver_preload_latitude) {
        this.driver_preload_latitude = driver_preload_latitude;
    }

    public double getDriver_preload_longitude() {
        return driver_preload_longitude;
    }

    public void setDriver_preload_longitude(double driver_preload_longitude) {
        this.driver_preload_longitude = driver_preload_longitude;
    }

    public String getDriver_preload_signature_signedat() {
        return driver_preload_signature_signedat;
    }

    public void setDriver_preload_signature_signedat(String driver_preload_signature_signedat) {
        this.driver_preload_signature_signedat = driver_preload_signature_signedat;
    }

    public short getStatus() {
        return status;
    }

    public void setStatus(short status) {
        this.status = status;
    }

    public String getDownload_status() {
        return download_status;
    }

    public void setDownload_status(String download_status) {
        this.download_status = download_status;
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

    public String getDispatch_date() {
        return dispatch_date;
    }

    public void setDispatch_date(String dispatch_date) {
        this.dispatch_date = dispatch_date;
    }

    public String getLdtyp() {
        return ldtyp;
    }

    public void setLdtyp(String ldtyp) {
        this.ldtyp = ldtyp;
    }

    public String getLdtypedes() {
        return ldtypedes;
    }

    public void setLdtypedes(String ldtypedes) {
        this.ldtypedes = ldtypedes;
    }

    public String getPickCode() {
        return pickCode;
    }

    public void setPickCode(String pickCode) {
        this.pickCode = pickCode;
    }

    public String getPickCodeDes() {
        return pickCodeDes;
    }

    public void setPickCodeDes(String pickCodeDes) {
        this.pickCodeDes = pickCodeDes;
    }

    public int getDockTerm() {
        return dockTerm;
    }

    public void setDockTerm(int dockTerm) {
        this.dockTerm = dockTerm;
    }

    public String getRelayldnbr() {
        return relayldnbr;
    }

    public void setRelayldnbr(String relayldnbr) {
        this.relayldnbr = relayldnbr;
    }

    public int getOriginldnbr() {
        return originldnbr;
    }

    public void setOriginldnbr(int originldnbr) {
        this.originldnbr = originldnbr;
    }

    public String getShipdate() {
        return shipdate;
    }

    public void setShipdate(String shipdate) {
        this.shipdate = shipdate;
    }

    public String getEstdeliverdate() {
        return estdeliverdate;
    }

    public void setEstdeliverdate(String estdeliverdate) {
        this.estdeliverdate = estdeliverdate;
    }

    public boolean isShuttle_load() {
        return shuttle_load;
    }

    public void setShuttle_load(boolean shuttle_load) {
        this.shuttle_load = shuttle_load;
    }

    public int getShuttle_move_id() {
        return shuttle_move_id;
    }

    public void setShuttle_move_id(int shuttle_move_id) {
        this.shuttle_move_id = shuttle_move_id;
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

    public int getParentLoad() {
        return parentLoad;
    }

    public void setParentLoad(int parentLoad) {
        this.parentLoad = parentLoad;
    }

    public int getParent_load_id() {
        return parent_load_id;
    }

    public void setParent_load_id(int parent_load_id) {
        this.parent_load_id = parent_load_id;
    }

    public String getPreload_supervisor_signature() {
        return preload_supervisor_signature;
    }

    public void setPreload_supervisor_signature(String preload_supervisor_signature) {
        this.preload_supervisor_signature = preload_supervisor_signature;
    }

    public String getPreload_supervisor_signedat() {
        return preload_supervisor_signedat;
    }

    public void setPreload_supervisor_signedat(String preload_supervisor_signedat) {
        this.preload_supervisor_signedat = preload_supervisor_signedat;
    }

    public int getTrailerNumber() {
        return trailerNumber;
    }

    public void setTrailerNumber(int trailerNumber) {
        this.trailerNumber = trailerNumber;
    }
}
