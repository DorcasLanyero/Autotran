package com.cassens.autotran.backendpoc;

import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.sdgsystems.util.HelperFuncs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoCVinDelivery {
    String vin;
    String loadPosition;
    int loadSeqNum;
    String pro;
    boolean backed;
    String rldsPickup;
    boolean doLotLocate;
    String backHaulLoadNum;
    String lot;
    String rowbay;
    String rte1;
    String rte2;
    String von;
    String finalDealer;
    String finalMfg;
    boolean inspectedPreload;
    boolean inspectedDelivery;

    PoCVinInfo vinInfo;
    List<PoCDamage> damages = new ArrayList<>();

    static PoCVinDelivery convertFromV2(DeliveryVin oldDelivVin) {
        PoCVinDelivery vinDeliv = new PoCVinDelivery();

        vinDeliv.vin = oldDelivVin.vin.vin_number;
        vinDeliv.loadPosition = oldDelivVin.position;
        vinDeliv.loadSeqNum = HelperFuncs.stringToInt(oldDelivVin.ldseq, -1);
        vinDeliv.pro = oldDelivVin.pro;
        vinDeliv.backed = oldDelivVin.backdrv != null && oldDelivVin.backdrv.equalsIgnoreCase("B");
        vinDeliv.rldsPickup = oldDelivVin.rldspickup;
        vinDeliv.doLotLocate = HelperFuncs.stringToBoolean(oldDelivVin.do_lotlocate, false);
        vinDeliv.backHaulLoadNum = oldDelivVin.bckhlnbr;
        vinDeliv.lot = oldDelivVin.lot;
        vinDeliv.rowbay = oldDelivVin.rowbay;
        vinDeliv.rte1 = oldDelivVin.rte1;
        vinDeliv.rte2 = oldDelivVin.rte2;
        vinDeliv.von = oldDelivVin.von;
        vinDeliv.finalDealer = oldDelivVin.finalDealer;
        vinDeliv.finalMfg = oldDelivVin.finalMfg;
        vinDeliv.inspectedPreload = oldDelivVin.inspectedPreload;
        vinDeliv.inspectedDelivery = oldDelivVin.inspectedDelivery;

        // Fill in VIN info
        PoCVinInfo vinInfo = PoCVinInfo.convertFromV2(oldDelivVin.vin);

        vinDeliv.vinInfo = vinInfo;


        for (Damage oldDamage: oldDelivVin.damages) {
            PoCDamage damage = PoCDamage.convertFromV2(oldDamage);
            vinDeliv.damages.add(damage);
        }
        return vinDeliv;
    }

    static Map<String, PoCVinDelivery> convertFromV2(Load oldLoad) {
        // Fill in deliveries and vinDeliveries from parent load
        Map<String, PoCVinDelivery> vinDeliveries = new HashMap<>();
        for (DeliveryVin oldDv: oldLoad.getDeliveryVinList()) {
            PoCVinDelivery vinDelivery = PoCVinDelivery.convertFromV2(oldDv);
            vinDeliveries.put(vinDelivery.vin, vinDelivery);
        }
        return vinDeliveries;
    }
}
