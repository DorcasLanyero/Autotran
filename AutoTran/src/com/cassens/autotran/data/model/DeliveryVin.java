package com.cassens.autotran.data.model;

import android.content.Context;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.google.gson.annotations.SerializedName;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class DeliveryVin implements Comparable<DeliveryVin> {

	public int delivery_vin_id = -1;

	@SerializedName("id")
	public String delivery_vin_remote_id;
	public int 	vin_id;
	public int 	delivery_id;
	public 	String  token;
	public String  timestamp;
	public String  facing;

	@SerializedName("preload_notes")
	public String preloadNotes;

	@SerializedName("delivery_notes")
	public String deliveryNotes;

	public String  position;
	public String  user_type;
	public String status = "0";

	public String  byteArray;

	public String ldseq;
	public String  pro;
	public String  bckhlnbr;
	public String  rowbay;
	public String  backdrv;
	public String  rldspickup;
	public String  do_lotlocate;
	public String  rejected_by;
	public String  lot;
	public String  von;
	public String  rte1;
	public String  rte2;
	public String  finalDealer;
	public String finalMfg;

	public int preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
	public int deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
	private static final int STI_EXTRA_IMAGES_PER_VIN_DEFAULT = 2;
	private static final int STI_EXTRA_IMAGES_PER_VIN_HIGH_CLAIMS = 5;

	@SerializedName("inspected_preload")
	public boolean inspectedPreload;

	@SerializedName("inspected_delivery")
	public boolean inspectedDelivery;

	@SerializedName("supervisor_signature")
	public String supervisorSignature;

	@SerializedName("supervisor_latitude")
	public String supervisorSignatureLat;

	@SerializedName("supervisor_longitude")
	public String supervisorSignatureLon;

	@SerializedName("supervisor_signature_signedat")
	public String supervisorSignatureSignedAt;

	@SerializedName("supervisor_comment")
	public String supervisorComment;

	@SerializedName("supervisor_contact")
	public String supervisorContact;

	public VIN vin;
	public ArrayList<Damage> damages;

	public String shuttleLoadRoute;
	public String shuttleLoadProductionStatus;


	public boolean uploaded;

	public ArrayList<Image> images;

    public String preloadImageCount = "0";
    public String deliveryImageCount = "0";

	//Using this field as a flag for whether or not the supervisor signature was pending on this delivery vin
	public String ats = "";

	//Using this database field for tracking the selection history of the delivery vin because I am
	//avoiding a new db column.
	public String key;

	private static List<String> sValidPositions;


	public DeliveryVin() {
		damages = new ArrayList<Damage>();
		images = new ArrayList<Image>();
		inspectedPreload = false;
		inspectedDelivery = false;

		uploaded = false;
		backdrv = "D";
		pro = "";
	}

    public void refreshImageCounts() {
        int preloadImages = 0;
        int deliveryImages = 0;

        for (Image image : images) {
            if (image.preloadImage) {
                preloadImages++;
            } else {
                deliveryImages++;
            }
        }

        preloadImageCount = String.valueOf(preloadImages);
        deliveryImageCount = String.valueOf(deliveryImages);
    }

	public boolean lotLocateRequired() {
		if (AppSetting.ENABLE_PER_UNIT_LOT_LOCATE.getBoolean()) {
			return this.do_lotlocate != null && this.do_lotlocate.trim().equalsIgnoreCase("y");
		}
		else {
			return false;
		}
	}

    public void addPreloadNote(String message) {
		if(preloadNotes != null) {
			preloadNotes += "\n" + message;
		} else {
			preloadNotes = message;
		}
	}

	public void addDeliveryNote(String message) {
		if(deliveryNotes != null) {
			deliveryNotes += "\n" + message;
		} else {
			deliveryNotes = message;
		}
	}

	public boolean hasDamages(boolean realOnly) {
		if (damages == null || damages.size() == 0) {
			return false;
		}
		else if (realOnly) {
			for (Damage d : this.damages) {
				if (d.isReal()) {
					return true;
				}
			}
			return false;
		}
		else {
			return true;
		}
	}

	public boolean hasDamages() {
		return hasDamages(false);
	}

	public boolean hasRealDamages() {
		return hasDamages(true);
	}

	public boolean hasNewDamages() {
		for (Damage d : this.damages) {
			if (!d.readonly) {
				return true;
			}
		}
		return false;
	}

	public boolean hasNewDamagesOfType(boolean preload) {
		for (Damage d : this.damages) {
			if (!d.readonly && d.preLoadDamage == preload) {
				return true;
			}
		}
		return false;
	}

	public int getDriverAddedDamageCount() {
		int count = 0;
		for (Damage d : this.damages) {
			if (d.source.equals("driver")) {
				count++;
			}
		}
		return count;
	}

	public String getFormattedDamagesSummary(boolean preload) {
		return getFormattedDamagesSummary(preload, 0);
	}

	public String getFormattedDamagesSummary(boolean preload, int indent) {
		String damagesSummary = "";

		if (!this.hasNewDamagesOfType(preload)) {
			return "No damages";
		}
		final String lineFormat = "%7s%7s%7s\n";
		damagesSummary += String.format(lineFormat, "Area", "Type", "Svrty");
		for (Damage damage : this.damages) {
			if (damage.preLoadDamage == preload && !damage.readonly) {
				damagesSummary += String.format(lineFormat, damage.getAreaCodeFormatted(), damage.getTypeCodeFormatted(), damage.getSeverityCodeFormatted());
			}
		}
		return HelperFuncs.indentString(damagesSummary, indent);
	}


	public String getFormattedSummary(boolean preload, int indent) {
		final String damageType = (preload ? "Preload" : "Delivery");
		String header;
		String summary = "";
		if (this.vin == null || this.vin.vin_number == null) {
			header = String.format("VIN local id=%d", this.vin_id);
		} else {
			header = String.format("VIN %s (id=%s)", this.vin.vin_number, this.vin.vin_remote_id);
		}
		if (this.hasNewDamagesOfType(preload)) {
			summary += String.format("\n%s Damages:\n\n%s", damageType, this.getFormattedDamagesSummary(preload));
		}
		else {
			summary += String.format("\n%s Damages: none", damageType);
		}

		this.refreshImageCounts();
		if (preload) {
			summary += String.format("\nPreload Images: %s", this.preloadImageCount);
		}
		else {
			summary += String.format("\nDelivery Images: %s", this.deliveryImageCount);
		}

		if (preload && this.supervisorSignature != null) {
			summary += CommonUtility.highLevelLogMsgWordWrap(String.format("\n\nSupervisor: %s\n Signed at: %s UTC\nSupervisor Comments:\n%s\n",
					HelperFuncs.noNull(this.supervisorContact, "Name not specified"),
					HelperFuncs.noNull(this.supervisorSignatureSignedAt, "unknown"),
					HelperFuncs.noNull(this.supervisorComment, "none")), indent, 52);
		}
		if (preload && !HelperFuncs.isNullOrWhitespace(this.preloadNotes)) {
			summary += CommonUtility.highLevelLogMsgWordWrap(String.format("\n\nPreload Damage Notes:\n%s", this.preloadNotes), indent, 52);
		}
		else if (!preload && !HelperFuncs.isNullOrWhitespace(this.deliveryNotes)){
			summary += CommonUtility.highLevelLogMsgWordWrap(String.format("\n\nDelivery Damage Notes:\n%s", this.deliveryNotes), indent, 52);
		}
		summary = HelperFuncs.indentString(summary, indent);
		return HelperFuncs.indentString(header + "\n" + summary, indent);
	}

	public String getFormattedSummary(boolean preload) {
		return getFormattedSummary(preload, 0);
	}

	public com.cassens.autotran.data.model.dto.DeliveryVin getDTO(Context context) {
		com.cassens.autotran.data.model.dto.DeliveryVin dto = new com.cassens.autotran.data.model.dto.DeliveryVin();

		if (!HelperFuncs.isNullOrEmpty(this.delivery_vin_remote_id)) {
			dto.setId(Integer.parseInt(this.delivery_vin_remote_id));
		}
		dto.setDelivery_vin_id(this.delivery_vin_id);
		Delivery delivery = DataManager.getDelivery(context, this.delivery_id);
		if (delivery != null && !HelperFuncs.isNullOrEmpty(delivery.delivery_remote_id)) {
			dto.setDelivery_id(Integer.parseInt(delivery.delivery_remote_id));
		}
		if (this.vin != null && !HelperFuncs.isNullOrEmpty(this.vin.vin_remote_id)) {
			dto.setVin_id(Integer.parseInt(this.vin.vin_remote_id));
		}
		if (!HelperFuncs.isNullOrEmpty(this.backdrv)) {
			dto.setBackdrv(this.backdrv.charAt(0));
		}
		if (!HelperFuncs.isNullOrEmpty(this.bckhlnbr)) {
			dto.setBckhlnbr(Integer.parseInt(this.bckhlnbr));
		}
		dto.setInspected_preload(this.inspectedPreload);
		dto.setInspected_delivery(this.inspectedDelivery);
		if (dto.isInspected_delivery()) {
			dto.setStatus((short) Constants.DELIVERY_VIN_STATUS_DELIVERED);
		} else if (dto.isInspected_preload()) {
			dto.setStatus((short) Constants.DELIVERY_VIN_STATUS_LOADED);
		} else {
			dto.setStatus((short) Constants.DELIVERY_VIN_STATUS_NOT_LOADED);
		}
		dto.setPreload_notes(this.preloadNotes);
		dto.setDelivery_notes(this.deliveryNotes);
		dto.setPreloadImageCount(Integer.parseInt(this.preloadImageCount));
		dto.setDeliveryImageCount(Integer.parseInt(this.deliveryImageCount));
		if (!HelperFuncs.isNullOrEmpty(this.ldseq)) {
			dto.setLdseq(Short.parseShort(this.ldseq));
		}
		dto.setLot(this.lot);
		if (!HelperFuncs.isNullOrEmpty(this.position)) {
		    if (this.position.contains("-")) {
		        String truncatedPosition = this.position.replace("-", "");
		        truncatedPosition = truncatedPosition.substring(0,2);
                dto.setPosition(Short.parseShort(truncatedPosition));
            } else {
				Short shortPosition = Short.parseShort(CommonUtility.removeNonNumericCharacters(this.position));
				dto.setPosition(shortPosition);
            }
		}
		dto.setPro(this.pro);
		dto.setRejected_by(this.rejected_by);
		if (!HelperFuncs.isNullOrEmpty(this.rldspickup)) {
			dto.setRldspickup(this.rldspickup.charAt(0));
		}
		dto.setRowbay(this.rowbay);
		dto.setRte1(this.rte1);
		dto.setRte2(this.rte2);
		dto.setShuttleLoadProductionStatus(this.shuttleLoadProductionStatus);
		dto.setShuttleLoadRoute(this.shuttleLoadRoute);
		dto.setSupervisor_signature(this.supervisorSignature);
		dto.setSupervisor_contact(this.supervisorContact);
		dto.setSupervisor_comment(this.supervisorComment);
		if (!HelperFuncs.isNullOrEmpty(this.supervisorSignatureLat)) {
			dto.setSupervisor_latitude(Double.parseDouble(this.supervisorSignatureLat));
		}
		if (!HelperFuncs.isNullOrEmpty(this.supervisorSignatureLon)) {
			dto.setSupervisor_longitude(Double.parseDouble(this.supervisorSignatureLon));
		}
		dto.setSupervisor_signature_signedat(this.supervisorSignatureSignedAt);
		if (this.vin != null) {
			dto.setVin(this.vin);
			if (!HelperFuncs.isNullOrEmpty(this.vin.vin_remote_id)) {
				dto.setVin_id(Integer.parseInt(this.vin.vin_remote_id));
			}
		}
		dto.setVon(this.von);

		for(Damage dmg : this.damages) {
			com.cassens.autotran.data.model.dto.Damage dmgDto = dmg.getDTO();
			dmgDto.setDelivery_vin_id(dto.getId());
			dto.damages.add(dmgDto);
		}

		dto.setSelection_history(this.key);

		for (Image image : images) {
			com.cassens.autotran.data.model.dto.Image imageDto = image.getDTO(context);
			image.delivery_vin_id = dto.getId();
			dto.images.add(imageDto);
		}

		return dto;
	}

	public com.cassens.autotran.data.model.dto.DeliveryVin getDTO(Context context, boolean forUpload, boolean preload) {
		com.cassens.autotran.data.model.dto.DeliveryVin dto = this.getDTO(context);

		dto.damages.clear();
		for (Damage dmg : this.damages) {
			if (forUpload && dmg.preLoadDamage == preload) {
				com.cassens.autotran.data.model.dto.Damage dmgDto = dmg.getDTO();
				dmgDto.setDelivery_vin_id(dto.getId());
				dto.damages.add(dmgDto);
			}
		}

		return dto;
	}

	@Override
	public int compareTo(DeliveryVin another) {
		int thatMaxLdSeq, thisMaxLdSeq;
		try {
			thatMaxLdSeq = Integer.parseInt(another.ldseq);
		} catch (Exception e) {
			thatMaxLdSeq = 0;
		}
		try {
			thisMaxLdSeq = Integer.parseInt(this.ldseq);
		} catch (Exception e) {
			thisMaxLdSeq = 0;
		}


		return (thisMaxLdSeq > thatMaxLdSeq ? 1 : thisMaxLdSeq == thatMaxLdSeq ? 0 : -1);
	}

	public void setWantsSupervisorSignature(boolean wantsSupervisorSignature) {
		ats = wantsSupervisorSignature ? "1" : "";
	}

	public boolean wantsSupervisorSignature() {
		if(ats != null && !ats.trim().isEmpty()) {
			return true;
		}

		return false;
	}

	// TODO: - The functions below that relate to image requirements are misplaced and should
	//         be moved to a different class in a near-term release. - PDK
	public static boolean isOdometerPicRequired(String mfg, int terminalNum)
	{
		if (mfg != null) {
			String[] odometerPicsNotRequiredMfgs = AppSetting.ODOMETER_PIC_NOT_REQUIRED_MFGS.getString().replace(" ", "").split(",");
			if (odometerPicsNotRequiredMfgs != null && Arrays.asList(odometerPicsNotRequiredMfgs).contains(mfg)) {
				return false;
			}
		}

		if (AppSetting.ODOMETER_PIC_REQUIRED_TERMINALS.getString().strip().equalsIgnoreCase("all")) {
			return true;
		}
		String[] odometerPicsTerminals = AppSetting.ODOMETER_PIC_REQUIRED_TERMINALS.getString().replace(" ", "").split(",");
		if (odometerPicsTerminals == null) {
			return false;
		}
		return Arrays.asList(odometerPicsTerminals).contains(Integer.toString(terminalNum));
	}

	private static List<String> getRequiredDealerUnavailableImages(boolean highClaims, Dealer dealer) {
		List<String> keyList = new ArrayList<>();;
		if (dealer.photosOnUnattended) {
			if (isOdometerPicRequired(dealer.mfg, CommonUtility.getDriverHelpTerm())) {
				keyList.add(Constants.IMAGE_ODOMETER);
			}
			if (highClaims) {
				keyList.addAll(Constants.IMAGE_KEYS_EXTERIOR_FULL_SET);
			}
			else {
				keyList.addAll(Constants.IMAGE_KEYS_EXTERIOR_CORNER_ONLY);
			}
		}
		return keyList;
	}

	public boolean hasRequiredDealerUnavailableImages(boolean highClaims, Dealer dealer) {

		List<String> keyList = getRequiredDealerUnavailableImages(highClaims, dealer);

		if (keyList.size() == 0) {
			return true;
		}

		int foundStiImages = 0;
		for (Image i : this.images) {
			if (!i.preloadImage && i.isForeignKeyInList(keyList) && ++foundStiImages == keyList.size()) {
				return true;
			}
		}
		return false;
	}

	public static boolean isRequiredDealerUnavailableImage(Image image, boolean highClaims, Dealer dealer) {
		return image.isForeignKeyInList(getRequiredDealerUnavailableImages(highClaims, dealer));
	}

	public static List<String> getValidLoadPositions() {
		if (sValidPositions == null) {
			sValidPositions = Collections.unmodifiableList(Arrays.asList(AutoTranApplication.getAppContext().getResources().getStringArray(R.array.truck_position_opts)));
		}
		return sValidPositions;
	}

	public static boolean isValidLoadPosition(String position) {
		return !HelperFuncs.isNullOrWhitespace(position) && getValidLoadPositions().contains(position.trim());
	}
}
