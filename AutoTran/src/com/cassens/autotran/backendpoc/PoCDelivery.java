package com.cassens.autotran.backendpoc;

import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.sdgsystems.util.HelperFuncs;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoCDelivery {
    PoCDealer dealer;
    String shipDate;
    String estDeliverDate;
    int dockTerm;
    boolean callback;
    String dealerSignature;
    String dealerSignatureSignedat;
    String dealerLatitude;
    String dealerLongitude;
    String dealerContact;
    String dealerComment;
    String driverSignature;
    String driverSignatureSignedat;
    String driverSignatureLat;
    String driverSignatureLon;
    String driverContact;
    String driverComment;
    List<String> vins = new ArrayList<>();

    PoCDelivery() {
        dealer = new PoCDealer();
    }

    static PoCDelivery convertFromV2(Delivery oldDelivery) {
        PoCDelivery delivery = new PoCDelivery();

        // Fill in dealer info
        if (oldDelivery.dealer != null) {
            PoCDealer dealer = new PoCDealer();
            dealer.customerNum = oldDelivery.dealer.customer_number;
            dealer.mfg = oldDelivery.dealer.mfg;
            dealer.customerName = oldDelivery.dealer.customer_name;
            dealer.city = oldDelivery.dealer.city;
            dealer.state = oldDelivery.dealer.state;
            dealer.address = oldDelivery.dealer.address;
            dealer.zip = oldDelivery.dealer.zip;
            dealer.contact = oldDelivery.dealer.contact_name;
            dealer.email = oldDelivery.dealer.email;
            dealer.phone = oldDelivery.dealer.phone;
            dealer.monam = oldDelivery.dealer.monam;
            dealer.tueam = oldDelivery.dealer.tueam;
            dealer.wedam = oldDelivery.dealer.wedam;
            dealer.thuam = oldDelivery.dealer.thuam;
            dealer.friam = oldDelivery.dealer.friam;
            dealer.satam = oldDelivery.dealer.satam;
            dealer.sunam = oldDelivery.dealer.sunam;
            dealer.monpm = oldDelivery.dealer.monpm;
            dealer.tuepm = oldDelivery.dealer.tuepm;
            dealer.wedpm = oldDelivery.dealer.wedpm;
            dealer.thupm = oldDelivery.dealer.thupm;
            dealer.fripm = oldDelivery.dealer.fripm;
            dealer.satpm = oldDelivery.dealer.satpm;
            dealer.sunpm = oldDelivery.dealer.sunpm;
            dealer.afterHours = oldDelivery.dealer.afthr;
            dealer.comments = oldDelivery.dealer.comments;
            dealer.highClaims = oldDelivery.dealer.high_claims;
            dealer.alwaysUnattended = oldDelivery.dealer.alwaysUnattended;
            dealer.photosOnUnattended = oldDelivery.dealer.photosOnUnattended;
            dealer.lotLocateRequired = oldDelivery.dealer.lotLocateRequired;
            dealer.lotCodeId = oldDelivery.dealer.lot_code_id;
            dealer.countryCode = oldDelivery.dealer.countryCode;
            if (oldDelivery.dealer.lastUpdated != null) {
                dealer.lastUpdate = oldDelivery.dealer.lastUpdated.toString();
            }
            delivery.dealer = dealer;
        }

        delivery.shipDate =  oldDelivery.ship_date;
        delivery.estDeliverDate = oldDelivery.estdeliverdate;
        delivery.dockTerm = oldDelivery.dockTerm;
        delivery.callback = HelperFuncs.stringToBoolean(oldDelivery.callback, false);

        for (DeliveryVin dv: oldDelivery.deliveryVins) {
            delivery.vins.add(dv.vin.vin_number);
        }
        return delivery;
    }

    static Map<String, PoCDelivery> convertFromV2(Load oldLoad) {
        // Fill in deliveries and vinDeliveries from parent load
        Map<String, PoCDelivery> deliveries = new HashMap<>();
        for (Delivery oldDelivery: oldLoad.deliveries) {
            PoCDelivery delivery = PoCDelivery.convertFromV2(oldDelivery);
            String deliveryKey;
            if (oldLoad.shuttleLoad) {
                deliveryKey = "shuttle";
            }
            else {
                deliveryKey = String.format("%s-%s", delivery.dealer.customerNum, delivery.dealer.mfg);
            }
            deliveries.put(deliveryKey, delivery);
        }
        return deliveries;
    }
}
