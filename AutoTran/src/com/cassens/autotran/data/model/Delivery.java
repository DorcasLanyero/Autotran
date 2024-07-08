package com.cassens.autotran.data.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.interfaces.VehicleBatchInterface;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.google.gson.annotations.SerializedName;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class Delivery implements VehicleBatchInterface, Comparable<Delivery> {
    private static final Logger log = LoggerFactory.getLogger(Delivery.class.getSimpleName());

	public int delivery_id = -1;

	@SerializedName("id")
	public String delivery_remote_id;

	public int  load_id;
	public Load load;
	public String  timestamp;
	public String  token;
	
	public String ship_date;
	public String estdeliverdate;
	public String status;
	public String delivery;
	public String callback;
	public String safeDelivery = "";

	@SerializedName("dealer_signature")
	public String dealerSignature;

	@SerializedName("dealer_signature_signedat")
	public String dealerSignatureSignedAt;

	@SerializedName("dealer_contact")
	public String dealerContact;

	@SerializedName("dealer_email")
	public String dealerEmail;

	@SerializedName("latitude")
	public String dealerSignatureLat;

	@SerializedName("longitude")
	public String dealerSignatureLon;

	@SerializedName("driver_signature")
	public String driverSignature;

	@SerializedName("driver_contact")
	public String driverContact;

	@SerializedName("driver_signature_signedat")
	public String driverSignatureSignedAt;

	@SerializedName("driver_latitude")
	public String driverSignatureLat;

	@SerializedName("driver_longitude")
	public String driverSignatureLon;

	public int  sti;    // This is set to true on after hours deliveries
	public int  afrhrs; // This is misnamed. It's set to 1 on when dealer unavailable during delivery hours; 0 otherwise.
	public String  userType;

	@SerializedName("driver_comment")
	public String driverComment;

	@SerializedName("dealer_comment")
	public String dealerComment;

	public boolean uploaded;
	
	public int preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
	public int deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
	
	public List<DeliveryVin> deliveryVins;
	public Dealer dealer;
	
	public boolean shuttleLoad = false;

	public String download_status;
	public int imageCount;
	public String notes;
	public List<Image> images;
	public int dockTerm;

    public String getRemoteId() {
		return this.delivery_remote_id;
	}
	public String getId() {
		return String.valueOf(this.delivery_id);
	}

	public boolean getUploaded() {
		return this.uploaded;
	}

	public void setUploaded(boolean uploaded) {
		this.uploaded = uploaded;
		this.deliveryUploadStatus = this.uploaded ? Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY : Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
	}

	public String getNotes() {
		return this.notes != null ? this.notes : "";
	}

	public void setNotes(String s) {
		this.notes = s;
	}

	public List<Image> getImages() {
		return this.images;
	}
	
	public Delivery() {
		deliveryVins = new ArrayList<>();
		uploaded = false;
		status = "new";
		images = new ArrayList<>();
	}

	public void save(Context ctx) {
		DataManager.insertDeliveryToLocalDB(ctx, this);
		DataManager.insertDeliveryToLocalDB(ctx, this);
	}

	public boolean isEmpty() {
		return this.deliveryVins == null || this.deliveryVins.size() == 0;
	}

	public boolean isStiDelivery() {
    	return this.sti != 0;
	}

	public boolean isAfterHoursDelivery() {
		return this.afrhrs != 0;
	}

	public boolean isDealerUnavailable () {
    	return this.isStiDelivery() || this.isAfterHoursDelivery() || this.isDealerAlwaysUnattended();
	}

	public boolean isDealerAlwaysUnattended() {
    	return this.dealer != null && this.dealer.alwaysUnattended;
	}


	public boolean isDealerPhotosOnUnAttended() {
		return this.dealer != null && this.dealer.photosOnUnattended;
	}
	
	public boolean isInspected(boolean isHighClaimsDriver)
	{
		if (this.isEmpty()) {
			return false;
		}

	    for (DeliveryVin dvin: this.deliveryVins) {

            //we don't care if deleted delivery vins are inspected
	        if (! dvin.inspectedDelivery && !dvin.pro.equals("deleted")) {
	            return false;
	        }
	        if (this.isDealerUnavailable() && !dvin.hasRequiredDealerUnavailableImages(isHighClaimsDriver, this.dealer)) {
	        	return false;
			}
	    }
	    return true;
	}
	
	public boolean isPreloadInspected()
	{
		if (this.isEmpty()) {
			return false;
		}
	    for (DeliveryVin dvin: this.deliveryVins) {
	        if (! dvin.inspectedPreload) {
	            return false;
	        }
	    }
	    return true;
	}

	public boolean isShuttleLoadInspected() {
		if (this.isEmpty()) {
			return false;
		}
		for (DeliveryVin dvin : this.deliveryVins) {

			/*
			//At this point, this heuristic is just wrong...  production status and route can only be entered in manually entered
			//vins anyways
			if (dvin.shuttleLoadProductionStatus == null || dvin.shuttleLoadProductionStatus.length() == 0
					|| dvin.shuttleLoadRoute == null || dvin.shuttleLoadRoute.length() == 0) {
				return false;
			}*/

			if(!dvin.inspectedPreload) {
				return false;
			}
		}
		return true;
	}

	public boolean hasDamages(boolean realOnly) {
		for (DeliveryVin dvin : this.deliveryVins) {
			if (dvin.hasDamages(realOnly)) {
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
		for (DeliveryVin dvin : this.deliveryVins) {
			if (dvin.hasNewDamages()) {
				return true;
			}
		}
		return false;
	}

	public boolean hasNewDeliveryDamages() {
		for (DeliveryVin dvin : this.deliveryVins) {
			if (dvin.hasNewDamagesOfType(false)) {
				return true;
			}
		}
		return false;
	}

	public Dealer getDealer() {
		return this.dealer != null ? this.dealer : new Dealer();
	}
	
	public ArrayList<VIN> getVinList()
	{
	    ArrayList <VIN> vinList = new ArrayList<VIN>();
	    
        for (DeliveryVin deliveryVin: this.deliveryVins) {
            vinList.add(deliveryVin.vin);
        }
	    return vinList;
	}

	public int getDeliveryImageCount() {
		int count = 0;
		for(DeliveryVin deliveryVin: deliveryVins) {
			for(Image image : deliveryVin.images) {
				if(!image.preloadImage)
					count++;
			}
		}

		if(images != null && images.size() > 0) {
			count += images.size();
		}
		
		return count;
	}
	
	public ArrayList<Image> getDeliveryImages() {
		ArrayList<Image> images = new ArrayList<Image>();
		for(DeliveryVin deliveryVin: deliveryVins) {
			for(Image image : deliveryVin.images) {
				
				if(!image.preloadImage)
					images.add(image);
			}
		}
		
		return images;
	}

	public int getPreloadImageCount() {
		int count = 0;
		for(DeliveryVin deliveryVin: deliveryVins) {
			for(Image image : deliveryVin.images) {
				if(image.preloadImage)
					count++;
			}
		}
		//Log.d("NARF", "id " + delivery_id +", count: " + count);
		return count;
	}

	public ArrayList<Image> getPreloadImages() {
		ArrayList<Image> images = new ArrayList<Image>();
		for(DeliveryVin deliveryVin: deliveryVins) {
			for(Image image : deliveryVin.images) {
				
				if(image.preloadImage)
					images.add(image);
			}
		}
		
		return images;
	}

	public boolean deliveryImagesUploaded() {
		for(DeliveryVin deliveryVin : deliveryVins) {
			for(Image image : deliveryVin.images){
				if(!image.preloadImage 
						&& image.inspection_guid == null // This flags it as a delivery image rather than an inspection image
						&& image.deliveryUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY)
				{
					log.debug(Logs.DEBUG, "delivery image was NOT uploaded");
					return false;
				}
			} 
		}
		
		return true;
	}	
	
	public boolean preloadImagesUploaded() {
		for(DeliveryVin deliveryVin : deliveryVins) {
			for(Image image : deliveryVin.images){
				if(image.preloadImage && image.preloadUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD)
					return false;
			}
		}
		
		return true;
	}
	
	@Override
	public int compareTo(Delivery another) {
		
		int thatMaxLdSeq = getMaxLdSeq(another);;		
		int thisMaxLdSeq = getMaxLdSeq(this);
						
		//log.debug(Logs.DEBUG, "this max ldseq: " + thisMaxLdSeq +", that max ldseq: " + thatMaxLdSeq);
		return (thisMaxLdSeq > thatMaxLdSeq ? 1 : thisMaxLdSeq == thatMaxLdSeq ? 0 : -1);
	}
	
	public static int getMaxLdSeq(Delivery delivery) {
		int max = 0;
		for(DeliveryVin dv : delivery.deliveryVins) {
			if(dv.ldseq != null && dv.ldseq.length() > 0) {
				try {
					int ldseq = Integer.parseInt(dv.ldseq);
					if(ldseq > max) {
						max = ldseq;
					}
				} catch (NumberFormatException ex) {
					//don't do anything here
				}
			}
		}	
		return max;
	}

    public List<DeliveryVin> getDeliveryVinList() {
        return getDeliveryVinList(true);
    }

    public List<DeliveryVin> getDeliveryVinList(boolean includeDeleted)
    {
        List <DeliveryVin> deliveryVinList = new ArrayList<DeliveryVin>();

        for (DeliveryVin dvin: deliveryVins) {

            if(!includeDeleted && dvin.pro.equals("deleted")) {
                continue;
            }

            deliveryVinList.add(dvin);
        }

        return deliveryVinList;
    }

	public void addDealerComment(String message) {
		if(dealerComment != null) {
			dealerComment += "\n" + message;
		} else {
			dealerComment = message;
		}
	}

	private com.cassens.autotran.data.model.dto.Delivery getDTOParentFields(Context context) {
		com.cassens.autotran.data.model.dto.Delivery dto = new com.cassens.autotran.data.model.dto.Delivery();

		if (!HelperFuncs.isNullOrEmpty(this.delivery_remote_id)) {
			dto.setId(Integer.parseInt(this.delivery_remote_id));
		}
		dto.setDelivery_id(this.delivery_id);
		dto.setAfrhrs(Integer.toString(this.afrhrs));
		if (!HelperFuncs.isNullOrEmpty(this.callback)) {
			dto.setCallback(this.callback.charAt(0));
		}
		dto.setDealer_contact(this.dealerContact);
		dto.setDealer_email(this.dealerEmail);
		if (!HelperFuncs.isNullOrEmpty(this.dealerSignatureLat)) {
			dto.setDealer_latitude(Double.parseDouble(this.dealerSignatureLat));
		}
		if (!HelperFuncs.isNullOrEmpty(this.dealerSignatureLon)) {
			dto.setDealer_longitude(Double.parseDouble(this.dealerSignatureLon));
		}
		if (!HelperFuncs.isNullOrEmpty(this.driverSignature) && !this.driverSignature.equals(Constants.DUMMY_SIGNATURE)) {
			dto.setDealer_signature_signedat(this.dealerSignatureSignedAt);
			dto.setDealer_signature(this.dealerSignature);
			dto.setDealer_comment(this.dealerComment);

			dto.setDriver_comment(this.driverComment);
			dto.setDriver_contact(this.driverContact);
			if (!HelperFuncs.isNullOrEmpty(this.driverSignatureLat)) {
				dto.setDriver_latitude(Double.parseDouble(this.driverSignatureLat));
			}
			if (!HelperFuncs.isNullOrEmpty(this.driverSignatureLon)) {
				dto.setDriver_longitude(Double.parseDouble(this.driverSignatureLon));
			}
			dto.setDriver_signature(this.driverSignature);
			dto.setDriver_signature_signedat(this.driverSignatureSignedAt);
		}
		if (this.dealer != null) {
			dto.setDelivery(dealer.customer_number + "-" + dealer.mfg);
		}
		else {
			dto.setDelivery(this.delivery);
		}
		dto.setDownload_status(this.download_status);
		dto.setImageCount(this.imageCount);
		Load load = DataManager.getLoad(context, this.load_id);
		if (load != null) {
			if(!HelperFuncs.isNullOrEmpty(load.load_remote_id)) {
				dto.setLoad_id(Integer.parseInt(load.load_remote_id));
			}
			dto.setLdnbr(load.loadNumber);
		}
		dto.setStatus(this.status);
		dto.setSti(Integer.toString(this.sti));
		dto.setShuttleLoad(this.shuttleLoad);
		dto.setUploaded(this.uploaded);
		dto.setNotes(this.notes);
		dto.setSafe_delivery(this.safeDelivery);

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
		if (preferences != null) {
			dto.setTruck_number(CommonUtility.removeNonNumericCharacters(TruckNumberHandler.getTruckNumber(context)));
		}

		for (Image image : images){
			com.cassens.autotran.data.model.dto.Image imageDto = image.getDTO(context);
			dto.images.add(imageDto);
		}

		return dto;
	}

	public com.cassens.autotran.data.model.dto.Delivery getDTO(Context context) {
		com.cassens.autotran.data.model.dto.Delivery dto = this.getDTOParentFields(context);
		dto.setMACAddress(CommonUtility.getMACAddress());

		for(DeliveryVin dv : this.deliveryVins) {
			com.cassens.autotran.data.model.dto.DeliveryVin dvDto = dv.getDTO(context);
			dto.deliveryVins.add(dvDto);
		}

		return dto;
	}

	public com.cassens.autotran.data.model.dto.Delivery getDTO(Context context, boolean forUpload, boolean preload) {
		com.cassens.autotran.data.model.dto.Delivery dto = this.getDTOParentFields(context);
		if (forUpload) {
			dto.setMACAddress(CommonUtility.getMACAddress());
		}

		for(DeliveryVin dv : this.deliveryVins) {
			com.cassens.autotran.data.model.dto.DeliveryVin dvDto = dv.getDTO(context, forUpload, preload);
			dto.deliveryVins.add(dvDto);
		}

		return dto;
	}

	public boolean needsLotLocates(Context context) {
    	if(shuttleLoad) {
    		ShuttleMove move = load.shuttleMove;
    		if(move.lotLocateRequired) {
				if (hasEmptyLotLocates(context)) return true;
			}
		} else if(dealer.lotLocateRequired){
			if (hasEmptyLotLocates(context)) return true;
		} else {
			for(DeliveryVin dv : deliveryVins) {
				if(dv.lotLocateRequired()) {
					if( DataManager.getYardInventoryForDeliveryVin(context, dv.delivery_vin_id) == null) {
						return true;
					}
				}
			}
		}

    	return false;
	}

	public boolean hasEmptyLotLocates(Context context) {
		for(DeliveryVin dv : deliveryVins) {
            if( DataManager.getYardInventoryForDeliveryVin(context, dv.delivery_vin_id) == null) {
				return true;
            }
        }
		return false;
	}

    /***
     * Returns whether or not a delivery is pending (preloaded and not signed).
     * @return
     */
	public boolean deliveryIsPending() {
        if(load != null && load.parent_load_id == -1) {			//child loads never get delivered
            return  load.driverPreLoadSignature != null && HelperFuncs.isNullOrEmpty(driverSignature);
        } else {
            return false;
        }

	}

	public boolean uploadIssues(boolean preload) {
		if(!preload) {
			if(deliveryUploadStatus != Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY
					&& deliveryUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY) {
				return true;
			}
		} else {
			if(preloadUploadStatus != Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD &&
					preloadUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD) {
				return true;
			}
		}

		for (DeliveryVin dv : deliveryVins) {
			for(Image image : dv.images) {
				if(preload) {
					if(image.preloadImage && image.uploadStatus != Constants.SYNC_STATUS_NOT_UPLOADED
							&& image.uploadStatus != Constants.SYNC_STATUS_UPLOADED) {
						return true;
					}
				} else {
					if(!image.preloadImage && image.uploadStatus != Constants.SYNC_STATUS_NOT_UPLOADED
							&& image.uploadStatus != Constants.SYNC_STATUS_UPLOADED) {
						return true;
					}
				}

				if(image.s3_upload_status != Constants.SYNC_STATUS_NOT_UPLOADED
						&& image.s3_upload_status != Constants.SYNC_STATUS_NOT_READY_FOR_UPLOAD
						&& image.s3_upload_status != Constants.SYNC_STATUS_UPLOADED) {
					return true;
				}
			}
		}
		return false;
	}

	public String getFormattedSummary(boolean preload, int indent) {
		final String damageType = (preload ? "Preload" : "Delivery");
		String summary = String.format("Delivery ID %s", HelperFuncs.noNull(this.getRemoteId()));
		if (this.dealer != null) {
			summary += String.format(" to dealer #%s", HelperFuncs.noNull(this.dealer.customer_number, "?"));
			if (!HelperFuncs.isNullOrWhitespace(this.dealer.customer_name)) {
				summary += String.format(" - %s", HelperFuncs.noNull(this.dealer.customer_name, "?"));
			}
		}
		summary += ":\n";
		if (this.isStiDelivery()) {
			summary += "Delivery Type: After Hours (STI)\n";
		}
		else if (this.isAfterHoursDelivery()) {
			summary += "Delivery Type: Dealer Unavailable or Refusing to Sign\n";
		}

		for (DeliveryVin dvin: this.deliveryVins) {
			if (dvin.pro.equals("deleted")) {
				continue; // skip deleted vins
			}
			summary += String.format("\n%s", dvin.getFormattedSummary(preload, 2));
		}

		if (!HelperFuncs.isNullOrWhitespace(this.notes)) {
			summary += "\n\n" + CommonUtility.highLevelLogMsgWordWrap("Delivery notes: " + this.notes, indent);
		}
		return HelperFuncs.indentString(summary, indent);
	}

	public String getFormattedSummary(boolean preload) {
		return getFormattedSummary(preload, 0);
	}

}
