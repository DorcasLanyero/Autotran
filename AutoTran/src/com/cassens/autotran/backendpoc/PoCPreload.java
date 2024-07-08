package com.cassens.autotran.backendpoc;

import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.sdgsystems.util.HelperFuncs;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class PoCPreload {
    String inspectionGroup;
    boolean pickSheetImageRequired;
    String extraDocImage;
    String driverPreloadSignature;
    String driverPreloadSignatureSignedAt;
    double driverPreloadSignatureLat;
    double driverPreloadSignatureLon;
    String driverPreloadComment;
    String driverPreloadContact;
    String notes;
    String auditScanCode;
    String auditScannedAt;
    double auditScanLat;
    double auditScanLon;
    String auditResults;
    String preloadProceedScanCode;
    String preloadProceedScannedAt;
    List<String> vins = new ArrayList<>();

   static PoCPreload convertFromV2(Load ol) {
        if (ol.isParentLoad()) {
            return null;
        }
        PoCPreload preload = new PoCPreload();

        if (ol.isChildLoad()) {
            String loadNum = StringUtils.substringBefore(ol.loadNumber, "-");
            String inspectionGroup = StringUtils.substringAfter(ol.loadNumber, "-");
            if (HelperFuncs.isNullOrWhitespace(loadNum) || HelperFuncs.isNullOrWhitespace(inspectionGroup)) {
                // Shouldn't happen, but if it does, assume the load number contains
                // some string that will uniquely identify the preload;
                preload.inspectionGroup = ol.loadNumber;
            }
            else {
                preload.inspectionGroup = String.format("%s-%s", loadNum, inspectionGroup);
            }
        }
        else {
            preload.inspectionGroup = "default";
        }
        preload.pickSheetImageRequired = ol.pickSheetImageRequired;
        preload.extraDocImage = ol.extraDocImageRequired;

        preload.driverPreloadSignature = ol.driverPreLoadSignature;
        preload.driverPreloadSignatureSignedAt = ol.driverPreLoadSignatureSignedAt;
        preload.driverPreloadSignatureLat = HelperFuncs.stringToDouble(ol.driverPreLoadSignatureLat, -1.0);
        preload.driverPreloadSignatureLon = HelperFuncs.stringToDouble(ol.driverPreLoadSignatureLon, -1.0);
        preload.driverPreloadComment = ol.driverPreLoadComment;
        preload.driverPreloadContact = ol.driverPreLoadContact;
        preload.notes = ol.notes;
        preload.auditScanCode = ol.supervisorSignature;
        preload.auditScannedAt = ol.supervisorSignedAt;
        preload.auditScanLat = HelperFuncs.stringToDouble(ol.supervisorSignatureLat, -1.0);
        preload.auditScanLon = HelperFuncs.stringToDouble(ol.supervisorSignatureLon, -1.0);
        preload.auditResults = ol.driverHighClaimsAudit;
        preload.preloadProceedScanCode = ol.preloadSupervisorSignature;
        preload.preloadProceedScannedAt = ol.preloadSupervisorSignedAt;

        for (DeliveryVin dv: ol.getDeliveryVinList()) {
            preload.vins.add(dv.vin.vin_number);
        }

        return preload;
   }
}
