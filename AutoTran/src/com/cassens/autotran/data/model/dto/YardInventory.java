package com.cassens.autotran.data.model.dto;


public class YardInventory {
    private int id;
    private String inspector;
    private int terminal_id;
    private String VIN;
    private short lot_locate;
    private String bay;
    private String row;
    private int lot_code_id;
    private String download_status;
    private Double latitude;
    private Double longitude;
    private String ldnbr;

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

    public String getLdnbr() {
        return ldnbr;
    }

    public void setLdnbr(String ldnbr) {
        this.ldnbr = ldnbr;
    }

    public YardInventory() {
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getInspector() {
        return inspector;
    }

    public void setInspector(String inspector) {
        this.inspector = inspector;
    }

    public int getTerminal_id() {
        return terminal_id;
    }

    public void setTerminal_id(int terminal_id) {
        this.terminal_id = terminal_id;
    }

    public String getVIN() {
        return VIN;
    }

    public void setVIN(String VIN) {
        this.VIN = VIN;
    }

    public short getLot_locate() {
        return lot_locate;
    }

    public void setLot_locate(boolean lot_locate) {
        this.lot_locate = (short) (lot_locate ? 1 : 0);
    }

    public String getBay() {
        return bay;
    }

    public void setBay(String bay) {
        this.bay = bay;
    }

    public String getRow() {
        return row;
    }

    public void setRow(String row) {
        this.row = row;
    }

    public int getLot_code_id() {
        return lot_code_id;
    }

    public void setLot_code_id(int lot_code_id) {
        this.lot_code_id = lot_code_id;
    }

    public String getDownload_status() {
        return download_status;
    }

    public void setDownload_status(String download_status) {
        this.download_status = download_status;
    }
}
