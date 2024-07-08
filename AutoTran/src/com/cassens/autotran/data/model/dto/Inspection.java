package com.cassens.autotran.data.model.dto;

import com.cassens.autotran.data.model.lookup.LotCode;
import com.cassens.autotran.data.model.lookup.ScacCode;
import com.cassens.autotran.data.model.lookup.Terminal;

import java.util.ArrayList;
import java.util.Date;

public class Inspection {

    private int inspection_id;
    private String vin;
    private String guid;
    private String notes;
    private String inspector;

    private Terminal terminal;
    private LotCode lotCode;
    private int type;
    private ScacCode scacCode;
    private int imageCount;
    private int damageCount;
    private Double latitude;
    private Double longitude;
    private Date timestamp;

    public ArrayList<Damage> damages;
    public ArrayList<Image> images;

    public Inspection() {
        damages = new ArrayList<>();
        images = new ArrayList<>();
    }

    public int getInspection_id() {
        return inspection_id;
    }

    public void setInspection_id(int inspection_id) {
        this.inspection_id = inspection_id;
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = vin;
    }

    public String getGuid() {
        return guid;
    }

    public void setGuid(String guid) {
        this.guid = guid;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getInspector() {
        return inspector;
    }

    public void setInspector(String inspector) {
        this.inspector = inspector;
    }

    public Terminal getTerminal() {
        return terminal;
    }

    public void setTerminal(Terminal terminal) {
        this.terminal = terminal;
    }

    public LotCode getLotCode() {
        return lotCode;
    }

    public void setLotCode(LotCode lotCode) {
        this.lotCode = lotCode;
    }

    public int getType() {
        return type;
    }

    public void setType(int type) {
        this.type = type;
    }

    public ScacCode getScacCode() {
        return scacCode;
    }

    public void setScacCode(ScacCode scacCode) {
        this.scacCode = scacCode;
    }

    public int getImageCount() {
        return imageCount;
    }

    public void setImageCount(int imageCount) {
        this.imageCount = imageCount;
    }

    public int getDamageCount() {
        return damageCount;
    }

    public void setDamageCount(int damageCount) {
        this.damageCount = damageCount;
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

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

}
