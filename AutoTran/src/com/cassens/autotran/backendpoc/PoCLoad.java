package com.cassens.autotran.backendpoc;

import android.content.Context;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Load;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PoCLoad {
    String loadNum;
    String userId;
    String truckNum;
    String trailerNum;
    String loadType;
    String macAddress;
    String nextLoadNum;
    String prevLoadNum;
    String firstDrop;
    String lastDrop;
    String originTerminal;
    String helpTerminal;
    boolean shuttleLoad = false;
    int shuttleMoveId = -1;

    Map<String, PoCPreload> preloads = new HashMap<>();
    Map<String, PoCDelivery> deliveries = new HashMap<>();
    Map<String, PoCVinDelivery> vinDeliveries = new HashMap<>();

    public static PoCLoad convertFromV2(Load ol) {
        PoCLoad nl = new PoCLoad();

        Context context = AutoTranApplication.getAppContext();
        if (ol.isParentLoad() || ol.isChildLoad()) {
            if (ol.isChildLoad()) {
                // If this is a child load, get the parent load
                Load parentLoad = DataManager.getParentLoad(context, ol);
                if (parentLoad == null) {
                    PoCUtils.log("Warning: Unable to get parent load for child load: " + ol.loadNumber);
                    return null;
                }
                ol = parentLoad;
            }
            // If this is a parent load, retrieve the child loads and use them all to fill in preloads
            List<Load> childLoads = DataManager.getAllChildLoadsOfLoadNoImages(AutoTranApplication.getAppContext(), ol);

            // If the vin count of the parent should match the sum of the child vin counts. If
            // there's not a match, we punt here and wait for dispatch of remaining child loads.
            int childVinCount = 0;
            for (Load childLoad : childLoads) {
                Load fullChildLoad = DataManager.getLoad(context, childLoad.load_id);
                PoCPreload preload = PoCPreload.convertFromV2(fullChildLoad);
                if (preload == null) {
                    continue;
                }
                nl.preloads.put(preload.inspectionGroup, preload);
                childVinCount += fullChildLoad.getDeliveryVinList(false).size();
            }
            if (ol.getDeliveryVinList(false).size() != childVinCount) {
                PoCUtils.log("Warning: All child loads are not yet present for load: " + ol.loadNumber);
                return null;
            }

            nl.deliveries = PoCDelivery.convertFromV2(ol);
            nl.vinDeliveries = PoCVinDelivery.convertFromV2(ol);
        }
        else {
            /****
             * Fill in the preload data
             ****/
            PoCPreload preload = PoCPreload.convertFromV2(ol);
            nl.preloads.put(preload.inspectionGroup, preload);

            /****
             * Fill in the delivery data
             ****/
            nl.deliveries = PoCDelivery.convertFromV2(ol);

            /****
             * Fill in the vinDeliveries data
             ****/
            nl.vinDeliveries = PoCVinDelivery.convertFromV2(ol);
        }

        /****
         * Fill in the load data
         ****/
        nl.loadNum = ol.loadNumber;
        nl.truckNum = ol.truckNumber;
        nl.trailerNum = ol.trailerNumber;
        nl.userId = CommonUtility.getDriverNumber(AutoTranApplication.getAppContext());
        nl.loadType = ol.loadType;
        nl.macAddress = CommonUtility.getMACAddress();
        nl.nextLoadNum = ol.relayLoadNumber;
        nl.prevLoadNum = ol.originLoadNumber;
        nl.firstDrop = ol.firstDrop;
        nl.lastDrop = ol.lastDrop;
        nl.originTerminal = ol.originTerminal;
        nl.helpTerminal = ol.helpTerminal;
        nl.shuttleLoad = ol.shuttleLoad;
        if (ol.shuttleMove != null) {
            nl.shuttleMoveId = ol.shuttleMove.shuttleMoveId;
        }
        else {
            nl.shuttleMoveId = -1;
        }

        return nl;
    }
}
