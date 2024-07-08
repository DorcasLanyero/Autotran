package com.cassens.autotran.backendpoc;

import com.cassens.autotran.data.model.VIN;

public class PoCVinInfo {
    String vin;
    String type;
    String body;
    String weight;
    String color;
    String colorDesc;

    static PoCVinInfo convertFromV2(VIN vin) {
        PoCVinInfo vinInfo = new PoCVinInfo();
        vinInfo.vin = vin.vin_number;
        vinInfo.type = vin.type;
        vinInfo.body = vin.body;
        vinInfo.weight = vin.weight;
        vinInfo.color = vin.color;
        vinInfo.colorDesc = vin.colordes;
        return vinInfo;
    }
}
