package com.cassens.autotran.data.model;

import androidx.annotation.NonNull;

public class TrendingAlert implements Comparable<TrendingAlert> {

    public int id;
    public int load_id;
    public int driver_id;
    public String ldnbr;
    public String vin_id;
    public String alert;
    public int count;
    public String type;
    public int order;



    public TrendingAlert() {
    }


    @Override
    public int compareTo(@NonNull TrendingAlert otherTrendingAlert) {
        return Integer.compare(this.order, otherTrendingAlert.order);
    }
}
