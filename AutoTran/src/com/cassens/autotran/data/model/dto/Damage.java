package com.cassens.autotran.data.model.dto;


public final class Damage {
    private int delivery_vin_id, inspection_id, typecode_id, svrtycode_id, areacode_id, specialcode_id;
    private boolean preload_damage;
    private String guid;

    public Damage() {}

    public int getInspection_id() {
        return inspection_id;
    }

    public void setInspection_id(int inspection_id) {
        this.inspection_id = inspection_id;
    }

    public int getDelivery_vin_id() {
        return delivery_vin_id;
    }

    public void setDelivery_vin_id(int delivery_vin_id) {
        this.delivery_vin_id = delivery_vin_id;
    }

    public int getTypecode_id() {
        return typecode_id;
    }

    public void setTypecode_id(int typecode_id) {
        this.typecode_id = typecode_id;
    }

    public int getSvrtycode_id() {
        return svrtycode_id;
    }

    public void setSvrtycode_id(int svrtycode_id) {
        this.svrtycode_id = svrtycode_id;
    }

    public int getAreacode_id() {
        return areacode_id;
    }

    public void setAreacode_id(int areacode_id) {
        this.areacode_id = areacode_id;
    }

    public int getSpecialcode_id() {
        return specialcode_id;
    }

    public void setSpecialcode_id(int specialcode_id) {
        this.specialcode_id = specialcode_id;
    }

    public boolean isPreload_damage() {
        return preload_damage;
    }

    public void setPreload_damage(boolean preload_damage) {
        this.preload_damage = preload_damage;
    }

    public String getGuid() { return guid;}

    public void setGuid(String guid) { this.guid = guid; }
}
