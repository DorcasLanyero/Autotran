package com.cassens.autotran.data.model;

import com.sdgsystems.util.HelperFuncs;

/**
 * Created by john on 3/6/18.
 */

public class DeliveryVinModel {

    String vin;
    String color;
    String description;
    String damages;

    public DeliveryVinModel() {
        this.vin = "";
        this.color = "";
        this.description = "";
        this.damages = "";
    }

    public String getVin() {
        return vin;
    }

    public void setVin(String vin) {
        this.vin = "VIN: " + HelperFuncs.splitVin(vin);
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = "Color: " + color;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = "Description: " + description;
    }

    public String getDamages() {
        return damages;
    }

    public void setDamages(String damages) {
        if(this.damages.equals("")) {
            this.damages = "Exceptions: " + damages;
        } else {
            this.damages = this.damages + ", " + damages;
        }
    }
}
