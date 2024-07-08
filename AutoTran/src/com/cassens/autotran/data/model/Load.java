package com.cassens.autotran.data.model;

import android.content.Context;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.interfaces.VehicleBatchInterface;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.google.gson.annotations.SerializedName;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

public class Load implements VehicleBatchInterface, Comparable<Load> {
    private static final Logger log = LoggerFactory.getLogger(Load.class.getSimpleName());
    private static final String PICK_SHEET = "Pick Sheet";

    public int load_id = -1;

    @SerializedName("id")
    public String load_remote_id = null;

    @SerializedName("ldnbr")
    public String loadNumber = null;

    @SerializedName("user_id")
    public int driver_id;
  
	public int preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
	public int deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;

    @SerializedName("driver_preload_signature")
    public String driverPreLoadSignature = null;

    @SerializedName("driver_preload_signature_signedat")
    public String driverPreLoadSignatureSignedAt = null;

    @SerializedName("driver_preload_latitude")
    public String driverPreLoadSignatureLat = null;

    @SerializedName("driver_preload_longitude")
    public String driverPreLoadSignatureLon = null;

    @SerializedName("driver_preload_comment")
    public String driverPreLoadComment = null;

    @SerializedName("driver_preload_contact")
    public String driverPreLoadContact = null;

    public String  status;
	public boolean uploaded;

	public boolean relayLoad = false;
	public boolean originLoad = false;
	public String  originLoadNumber;
    public String  relayLoadNumber;
    public String  relayLoadDealerName;
    public String originTerminal;
    public boolean lotCodeMsgSeen = false;
    public String helpTerminal;

    @SerializedName("ldtyp")
    public String  loadType;

    @SerializedName("modified")
    public Date lastUpdated;

	public List<Delivery> deliveries;

    public User driver;
    /*
     New Shuttle load values - 11/7/2014
	 */
    @SerializedName("shuttle_load")
    public boolean shuttleLoad = false;

    public String driverNumber = null;

    @SerializedName("trknbr")
    public String truckNumber = null;

    public String trailerNumber = "";

    @SerializedName("shuttle_move_id")
    public ShuttleMove shuttleMove = null;
    public String imageCount = "0";
    public String notes;
    public List<Image> images;
    public String driverHighClaimsAudit;
    public String supervisorSignature;
    public String supervisorSignedAt;
    public String supervisorSignatureLat;
    public String supervisorSignatureLon;
    public String nextDispatch;
    public String firstDrop;
    public String lastDrop;
    public String extraDocImageRequired = "";
    public boolean parentLoad;
    public boolean pickSheetImageRequired;
    public int parent_load_id = -1;
    public String preloadSupervisorSignature = null;
    public String preloadSupervisorSignedAt = null;
    public String  do_lotlocate;
    public String getId() {
        return String.valueOf(this.load_id);
    }
    public String getRemoteId() {
        return this.load_remote_id;
    }

    public String getLoadNumber() {
        return HelperFuncs.noNull(this.loadNumber);
    }

    public boolean getUploaded() {
        return this.uploaded;
    }

    public void setPickSheetImageRequired(boolean pickSheetImageRequired) {
        this.pickSheetImageRequired = pickSheetImageRequired;
    }

    public boolean isPickSheetImageRequired()  {
        return this.pickSheetImageRequired;
    }

    public void setExtraDocImageRequired(String extraDocImageRequired) {
        this.extraDocImageRequired = HelperFuncs.noNull(extraDocImageRequired);
    }

    public String getExtraDocImageRequired() { return HelperFuncs.noNull(this.extraDocImageRequired);}

    public boolean hasDocImage(String docType) {
        String tag = this.docTypeToTag(docType);
        for (Image image : this.getImages()) {
            if (image.filename.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    public String docTypeToTag(String requiredDocType){
        return requiredDocType.toUpperCase().replaceAll(" ", "_");
    }

    public boolean needsPickSheetImage() {
        return this.isPickSheetImageRequired() && this.hasRealDamages() && !this.hasDocImage(PICK_SHEET);
    }

    public boolean needsExtraDocImage() {
        return !this.getExtraDocImageRequired().isEmpty() && this.hasRealDamages() && !this.hasDocImage(this.getExtraDocImageRequired());
    }

    public void setUploaded(boolean uploaded) {
        this.uploaded = uploaded;
        this.preloadUploadStatus = this.uploaded ? Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD : Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
    }

    public String getNotes() {
        return this.notes != null ? this.notes : "";
    }

    public void setNotes(String s) {
        this.notes = s;
    }

    public List<Image> getImages() {
        return this.images != null ? this.images : new ArrayList<Image>();
    }

    public Load() {
		deliveries = new ArrayList<>();
		uploaded = false;
		status = "new";
        images = new ArrayList<>();
        pickSheetImageRequired = true;
        extraDocImageRequired = "";
    }

    public void save(Context ctx) {
        DataManager.insertLoadToLocalDB(ctx, this);
    }

    public boolean isInspected()
    {
        for (Delivery delivery: this.deliveries) {
            if (delivery.deliveryVins == null || delivery.deliveryVins.size() == 0) {
                return false;
            }
            for (DeliveryVin dvin: delivery.deliveryVins) {
                if (! dvin.inspectedPreload) {
                    return false;
                }
            }
        }
        return true;
    }

    public boolean needsLoadPosition()
    {
        for (Delivery delivery: this.deliveries) {
            if (delivery.deliveryVins == null || delivery.deliveryVins.size() == 0) {
                return false;
            }
            for (DeliveryVin dv: delivery.deliveryVins) {
                if (dv.position == null || dv.position.equalsIgnoreCase("null") || dv.position.equals("0")) {
                    return true;
                }
            }
        }
        return false;
    }

    public boolean mightRequireSignature() {
        boolean requiresSignature = false;

        if(getDamagedVinsMissingSupervisorSignature().size() > 0) {
            requiresSignature = true;
        }

        return requiresSignature;
    }

    public Delivery getDeliveryForDealerRemoteId(String dealer_remote_id)
    {
        if (dealer_remote_id == null) {
            return null;
        }
        for (Delivery delivery: this.deliveries) {
            if (delivery.dealer == null || delivery.dealer.dealer_remote_id == null) {
                continue;
            }
            if (dealer_remote_id.equals(delivery.dealer.dealer_remote_id)) {
                return delivery;
            }
        }
        return null;
    }

    public List<DeliveryVin> getDamagedVinsMissingSupervisorSignature() {
        List<DeliveryVin> damagedVins = new ArrayList<>();

        if(hasDamages()) {
            for (Delivery delivery : this.deliveries) {
                for(DeliveryVin deliveryVin : delivery.deliveryVins) {
                    if(HelperFuncs.isNullOrEmpty(deliveryVin.supervisorSignature) || (deliveryVin.ats != null && !deliveryVin.ats.trim().isEmpty())) {
                        for (Damage damage : deliveryVin.damages) {
                            if (!damage.readonly
                                    && !damage.getSeverityCode().equals("")
                                    && Integer.parseInt(damage.getSeverityCode()) <= 2) {
                                damagedVins.add(deliveryVin);
                                break;
                            }
                        }
                    }
                }
            }
        }

        return damagedVins;
    }

    public boolean requiresSignature() {
        boolean requiresSignature = false;

        if(hasDamages()) {
            for (Delivery delivery : this.deliveries) {
                for(DeliveryVin deliveryVin : delivery.deliveryVins) {
                    for(Damage damage : deliveryVin.damages) {
                        if(!damage.readonly
                                && !damage.getSeverityCode().equals("")
                                && Integer.parseInt(damage.getSeverityCode()) > 2
                                && deliveryVin.ats != null
                                && !deliveryVin.ats.trim().isEmpty()) {
                            requiresSignature = true;
                        }
                    }
                }
            }
        }

        return requiresSignature;
    }

    public boolean hasDamages(boolean realOnly) {
        for (Delivery delivery: this.deliveries) {
            if (delivery.hasDamages(realOnly)) {
                return true;
            }
        }
        return false;
    }

    public boolean hasDamages() {
        return hasDamages(false);
    }

    public boolean hasRealDamages() {
        return hasDamages(true);
    }

    public boolean hasNewDamages() {
        for (Delivery delivery: this.deliveries) {
            if (delivery.hasNewDamages()) {
                return true;
            }
        }
        return false;
    }
    
    public boolean preloadImagesUploaded() {
    	for (Delivery delivery: this.deliveries) {
            if (!delivery.preloadImagesUploaded()) {
            	return false;
            }
    	}
    	
    	return true;
    }

    public boolean isSVLoadType() {
        return !HelperFuncs.isNullOrEmpty(this.loadType) && this.loadType.toUpperCase().equals("SV");
    }

    public boolean isAutoDeliverLoadType()
    {
        String[] loadTypes = AppSetting.AUTO_DELIVERY_LOAD_TYPES.getString().toUpperCase().replace(" ", "").split(",");
        if (loadTypes == null || HelperFuncs.isNullOrWhitespace(this.loadType)) {
            return false;
        }
        return Arrays.asList(loadTypes).contains(this.loadType.toUpperCase().trim());
    }

    public boolean isPreTwoDotFourOriginLoad() {
        // Return true if this load was signed prior to the 2.4 GA on 10/15/2020.
        return this.originLoad && !HelperFuncs.isNullOrEmpty(this.driverPreLoadSignatureSignedAt)
                && this.driverPreLoadSignatureSignedAt.compareTo("2020-10-15 00:00:00") < 0;
    }

    public boolean isFillerLoadType()
    {
        return this.loadType != null && this.loadType.equalsIgnoreCase("HH");
    }

    public boolean isParentLoad () {
        return parentLoad;
    }

    public boolean isChildLoad() {
        return parent_load_id != -1;
    }

    public List<DeliveryVin> getDeliveryVinList() {
        return getDeliveryVinList(true);
    }

    public List<DeliveryVin> getDeliveryVinList(boolean includeDeleted)
    {
        List <DeliveryVin> deliveryVinList = new ArrayList<DeliveryVin>();
        
        for (Delivery delivery: this.deliveries) {
            if (delivery.deliveryVins == null) {
                continue;
            }
            for (DeliveryVin dvin: delivery.deliveryVins) {

                if(!includeDeleted && dvin.pro.equals("deleted")) {
                    continue;
                }

                deliveryVinList.add(dvin);
            }
        }
        return deliveryVinList;
    }

    public DeliveryVin getDeliveryVinForVinNumber(String vinNumber)
    {
        for (Delivery delivery: this.deliveries) {
            if (delivery.deliveryVins == null) {
                continue;
            }
            for (DeliveryVin dvin: delivery.deliveryVins) {
                if (dvin.pro.equals("deleted")) {
                    continue;
                }
                if (dvin.vin.vin_number.equalsIgnoreCase(vinNumber)) {
                    return dvin;
                }
            }
        }
        return null;
    }
    
    public ArrayList<VIN> getVinList()
    {
        ArrayList <VIN> vinList = new ArrayList<VIN>();
        
        for (Delivery delivery: this.deliveries) {
            if (delivery.deliveryVins == null) {
                continue;
            }
            for (DeliveryVin dvin: delivery.deliveryVins) {
                vinList.add(dvin.vin);
            }
        }
        return vinList;
    }
   
    public int getPreloadImageCount() {
    	int count = 0;
    	
    	for(Delivery delivery : deliveries) {
    		count += delivery.getPreloadImageCount();
    	}

    	//Log.d("NARF", "load count: " + count);
    	return count;
    }
    
    public ArrayList<Image> getPreloadImages() {
    	ArrayList<Image> loadImages = new ArrayList<Image>();
    	
    	for(Delivery delivery : deliveries) {
    		loadImages = delivery.getPreloadImages();
    	}
    		
    	return loadImages;
    }

	@Override
	public int compareTo(Load another) {

        //log.debug(Logs.DEBUG, "comparing '" + this.loadNumber + "' to '" + this.loadNumber + "'");

        if(this.loadNumber == null || another.loadNumber == null || this.loadNumber.trim() == "" || another.loadNumber.trim() == "" || this.loadNumber.length() == 0) {
            return 0;
        }

        try {
            String thisLoadNumber;
            String thatLoadNumber;
            if (this.loadNumber.startsWith("SL-")) {
                thisLoadNumber = this.loadNumber.substring(this.loadNumber.indexOf("-", 3));  //handles shuttle loads correctly
            } else {
                thisLoadNumber = this.loadNumber.replaceAll("[^0-9]", "");  //handles child loads correctly
            }

            if (another.loadNumber.startsWith("SL-")) {
                thatLoadNumber = another.loadNumber.substring(another.loadNumber.indexOf("-", 3));
            } else {
                thatLoadNumber = another.loadNumber.replaceAll("[^0-9]", "");
            }

            Long thisLoad = Long.parseLong(thisLoadNumber);
            Long thatLoad = Long.parseLong(thatLoadNumber);

            return (thisLoad > thatLoad ? 1 : thisLoad == thatLoad ? 0 : -1);
        } catch (NumberFormatException ex) {
            log.debug(Logs.EXCEPTIONS, "Number format exception when comparing: " + this.loadNumber +" and " + another.loadNumber);
        }

        return 0;
    }

    public String getFormattedTypeInfo() {
        if (HelperFuncs.isNullOrEmpty(this.loadType)) {
            return "null";
        }
        String loadTypeInfo =  this.loadType;
        if (isAutoDeliverLoadType()) {
            loadTypeInfo += ", AutoDeliver";
        }
        if (isSVLoadType()) {
            loadTypeInfo += ", Shuttle";
        }
        if (isFillerLoadType()) {
            loadTypeInfo += ", Filler";
        }
        if (this.originLoad) {
            loadTypeInfo += ", Origin for relay " + HelperFuncs.noNull(this.relayLoadNumber, "unknown");
        } else if (this.relayLoad) {
            loadTypeInfo += ", Relay for origin " + HelperFuncs.noNull(this.originLoadNumber, "unknown");
        }
        return loadTypeInfo;
    }

    public String getFormattedSummary(boolean preload, int indent) {
        String summary = String.format("%s Summary for Load %s (ID %s):\nLoad Type: %s", (preload ? "Preload" : "Delivery"),
                HelperFuncs.noNull(this.loadNumber), HelperFuncs.noNull(this.load_remote_id), this.getFormattedTypeInfo());
        for (Delivery delivery:deliveries) {
            summary += String.format("\n\n%s", delivery.getFormattedSummary(preload, 2));
        }

        if (preload && !HelperFuncs.isNullOrWhitespace(this.notes)) {
            summary += "\n\n" + CommonUtility.highLevelLogMsgWordWrap("Load notes: " + this.notes, indent);
        }
        return HelperFuncs.indentString(summary, indent);
    }

    public String getFormattedSummary(boolean preload) {
        return getFormattedSummary(preload, 0);
    }


    private com.cassens.autotran.data.model.dto.Load getDTOParentFields(Context context) {
        com.cassens.autotran.data.model.dto.Load dto = new com.cassens.autotran.data.model.dto.Load();

        if (!HelperFuncs.isNullOrEmpty(this.load_remote_id)) {
            dto.setId(Integer.parseInt(this.load_remote_id));
        }
        dto.setLoad_id(this.load_id);
        if (this.driver != null && !HelperFuncs.isNullOrEmpty(this.driver.user_remote_id)) {
            dto.setUser_id(Integer.parseInt(CommonUtility.removeNonNumericCharacters(this.driver.user_remote_id)));
        }
        if (!HelperFuncs.isNullOrEmpty(this.imageCount)) {
            dto.setImageCount(Integer.parseInt(this.imageCount));
        }
        if (!HelperFuncs.isNullOrEmpty(this.driverPreLoadSignature) && !this.driverPreLoadSignature.equals(Constants.DUMMY_SIGNATURE)) {
            dto.setDriver_preload_signature(this.driverPreLoadSignature);
            dto.setDriver_preload_signature_signedat(this.driverPreLoadSignatureSignedAt);

            dto.setDriver_preload_comment(this.driverPreLoadComment);
            dto.setDriver_preload_contact(this.driverPreLoadContact);
            if (!HelperFuncs.isNullOrEmpty(this.driverPreLoadSignatureLat)) {
                dto.setDriver_preload_latitude(Double.parseDouble(this.driverPreLoadSignatureLat));
            }
            if (!HelperFuncs.isNullOrEmpty(this.driverPreLoadSignatureLon)) {
                dto.setDriver_preload_longitude(Double.parseDouble(this.driverPreLoadSignatureLon));
            }
        }
        if (!HelperFuncs.isNullOrEmpty(this.preloadSupervisorSignature) && !this.preloadSupervisorSignature.equals(Constants.DUMMY_SIGNATURE)) {
            dto.setPreload_supervisor_signature(this.preloadSupervisorSignature);
            dto.setPreload_supervisor_signedat(this.preloadSupervisorSignedAt);
        }

        dto.setLdnbr(this.loadNumber);
        if (!HelperFuncs.isNullOrEmpty(this.originLoadNumber)) {
            dto.setOriginldnbr(Integer.parseInt(this.originLoadNumber));
        }
        if (!HelperFuncs.isNullOrEmpty(this.relayLoadNumber)) {
            dto.setRelayldnbr(this.relayLoadNumber);
        }

        dto.setLdtyp(this.loadType);

        dto.setShuttle_load(this.shuttleLoad);
        if (this.shuttleMove != null) {
            dto.setShuttle_move_id(this.shuttleMove.shuttleMoveId);
        }
        if (!HelperFuncs.isNullOrEmpty(this.truckNumber)) {
            dto.setTrknbr(Integer.parseInt(CommonUtility.removeNonNumericCharacters(this.truckNumber)));
        }
        if(!HelperFuncs.isNullOrEmpty(this.trailerNumber)) {
            dto.setTrailerNumber(Integer.parseInt(CommonUtility.removeNonNumericCharacters(this.trailerNumber)));
        }

        dto.setNotes(this.notes);
        if (!HelperFuncs.isNullOrEmpty(this.supervisorSignature)){
            dto.setSupervisorSignature(this.supervisorSignature);
            dto.setSupervisorSignedAt(this.supervisorSignedAt);
            dto.setSupervisorSignatureLat(this.supervisorSignatureLat);
            dto.setSupervisorSignatureLon(this.supervisorSignatureLon);
        }
        dto.setDriverHighClaimsAudit(this.driverHighClaimsAudit);
        
        for(Image image : images) {
            com.cassens.autotran.data.model.dto.Image imageDto = image.getDTO(context);
            image.load_id = dto.getId();
            dto.images.add(imageDto);
        }
        dto.setPick_sheet_image_required(this.pickSheetImageRequired);
        dto.setExtra_doc_image_required(this.extraDocImageRequired);
        dto.setParentLoad(parentLoad ? 1 : 0);
        dto.setParent_load_id(parent_load_id);

        return dto;
    }

    //Returns the latest 'signed at' date on a load.
    public long getLatestSignatureDate() {
        Date latestDate = new Date(0L);

        SimpleDateFormat formatter = Constants.dateFormatter();


        if(!HelperFuncs.isNullOrWhitespace(driverPreLoadSignatureSignedAt)) {
            try {
                latestDate = formatter.parse(driverPreLoadSignatureSignedAt);
            } catch (ParseException e) {
                e.printStackTrace();
            }
        }

        for(Delivery delivery:deliveries) {
            if(!HelperFuncs.isNullOrWhitespace(delivery.dealerSignatureSignedAt)) {
                try {
                    Date dealerDate = formatter.parse(delivery.dealerSignatureSignedAt);
                    if(dealerDate.getTime() > latestDate.getTime()) {
                        latestDate = dealerDate;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }

            if(!HelperFuncs.isNullOrWhitespace(delivery.driverSignatureSignedAt)) {
                try {
                    Date driverDate = formatter.parse(delivery.driverSignatureSignedAt);
                    if(driverDate.getTime() > latestDate.getTime()) {
                        latestDate = driverDate;
                    }
                } catch (ParseException e) {
                    e.printStackTrace();
                }
            }
        }

        return latestDate.getTime();
    }

    public com.cassens.autotran.data.model.dto.Load getDTO(Context context, boolean forUpload, boolean preload) {
        com.cassens.autotran.data.model.dto.Load dto = this.getDTOParentFields(context);
        dto.setMACAddress(CommonUtility.getMACAddress());

        for(Delivery d : this.deliveries) {
            com.cassens.autotran.data.model.dto.Delivery dDto = d.getDTO(context, forUpload, preload);
            dto.deliveries.add(dDto);
        }

        return dto;
    }

    public boolean isComplete() {
        if(isAutoDeliverLoadType() && driverPreLoadSignature != null) {
            return true;
        } else {
            for(Delivery delivery : deliveries) {
                if (CommonUtility.isNullOrBlank(delivery.driverSignature)) {
                    return false;
                }
            }

            //We made it through all of the signatures...
            return true;
        }
    }

    /**
     * Check the preload upload status to determine if the load is in a temporary state (uploading or failed)
     * @return
     */
    public boolean uploadIssues(boolean preload) {
        if(preload && preloadUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD
                &&  preloadUploadStatus != Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD) {
            return true;
        }

        for(Delivery delivery : deliveries) {
            //if we haven't tried to upload anything yet, we *can't* have upload issues
            if ((preload && preloadUploadStatus != Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD) ||
                    (deliveryUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY &&
                            deliveryUploadStatus != Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY)) {
                if (delivery.uploadIssues(preload)) {
                    return true;
                }
            }
        }

        return false;

    }

    public boolean isLastUninspectedDelivery(int deliveryId) {
        for(Delivery delivery : deliveries) {
            if (delivery.delivery_id != deliveryId && HelperFuncs.isNullOrEmpty(delivery.driverSignature)) {
                return false;
            }
        }
        return true;
    }
}
