package com.cassens.autotran.data.model;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.lookup.AreaCode;
import com.cassens.autotran.data.model.lookup.SeverityCode;
import com.cassens.autotran.data.model.lookup.SpecialCode;
import com.cassens.autotran.data.model.lookup.TypeCode;
import com.google.gson.annotations.SerializedName;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.UUID;

public class Damage {

	private static final Logger log = LoggerFactory.getLogger(Damage.class.getSimpleName());


	public int  damage_id;
	
	//Only one of these should get set since the damage is related either to a deliveryVin OR an inspection
	public int  delivery_vin_id = -1;
	public int  inspection_id = -1;

	@SerializedName("typecode_id")
	public int type_code_id;

	@SerializedName("svrtycode_id")
	public int svrty_code_id;

	@SerializedName("areacode_id")
	public int area_code_id;

	@SerializedName("specialcode_id")
	public int special_code_id;

	@SerializedName("preload_damage")
	public boolean preLoadDamage;
	public boolean readonly;
	
	public AreaCode areaCode = null;
	public SeverityCode severityCode = null;
	public TypeCode typeCode = null;
	public SpecialCode specialCode = null;
	
	public int preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
	public int deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
	
	//For non-preload/delivery damages
	public int uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
	
	public boolean uploaded;

	//We default damage source to 'driver' unless overridden
	public String source = "driver";

	//added in db v86
	public String guid;
	public String inspection_guid;

	public Damage() {
		preLoadDamage = false;	
		
		uploaded = false;
		
		damage_id = -1;
		//special_code_id = -1;
		
		readonly = false;

		guid = UUID.randomUUID().toString();

		source = "driver";
	}

	private static final String DEFAULT_NULL_INDICATOR = "null";

	public String getAreaCode(String nullIndicator) {
		if (specialCode != null) {
			return specialCode.getAreaCode();
		}
		if (areaCode == null) {
			return nullIndicator;
		}
		return HelperFuncs.noNull(areaCode.getCode(), nullIndicator);
	}

	public String getAreaCode() {
		return getAreaCode(DEFAULT_NULL_INDICATOR);
	}

	public String getAreaCodeFormatted() {
		return CommonUtility.getZeroPaddedCode(getAreaCode());
	}

	public boolean areaCodeIsSet() {
		return !getAreaCode().equalsIgnoreCase(DEFAULT_NULL_INDICATOR);
	}

	public String getTypeCode(String nullIndicator) {
		if (specialCode != null) {
			return specialCode.getTypeCode();
		}
		if (typeCode == null) {
			return nullIndicator;
		}
		return HelperFuncs.noNull(typeCode.getCode(), nullIndicator);
	}

	public String getTypeCode() {
		return getTypeCode(DEFAULT_NULL_INDICATOR);
	}

	public String getTypeCodeFormatted() {
		return CommonUtility.getZeroPaddedCode(getTypeCode());
	}

	public boolean typeCodeIsSet() {
		return !getTypeCode().equalsIgnoreCase(DEFAULT_NULL_INDICATOR);
	}

	public String getSeverityCode(String nullIndicator) {
		if (specialCode != null) {
			return specialCode.getSeverityCode();
		}
		if (severityCode == null) {
			return nullIndicator;
		}
		return HelperFuncs.noNull(severityCode.getCode(), nullIndicator);
	}

	public String getSeverityCode() {
		return getSeverityCode(DEFAULT_NULL_INDICATOR);
	}

	public String getSeverityCodeFormatted() {
		return getSeverityCode(DEFAULT_NULL_INDICATOR);
	}

	public boolean severityCodeIsSet() {
		return !getSeverityCode().equalsIgnoreCase(DEFAULT_NULL_INDICATOR);
	}

	public boolean isReal() {
		// Eventually, damages should have a field to signify whether they represent real
		// damages. Until then, we just hard code the special cases.

		if (this.readonly && this.source.equals("driver")) {
			// This is an "orange" code that is for reference only.
			return false;
		}
		if (specialCode != null) {
			String desc = HelperFuncs.noNull(specialCode.getDescription()).trim().toLowerCase();
			if (desc.contains("dirt") || desc.contains("no damage")) {
				return false;
			}
		}
		return true;
	}

	public String toString() {
		String s = getAreaCode() + "-" + getTypeCode() + "-" + getSeverityCode();
		if (specialCode != null) {
			return String.format("%s (special %d)",  s, specialCode.special_code_id);
		}
		return s;
    }

	public com.cassens.autotran.data.model.dto.Damage getDTO() {
		com.cassens.autotran.data.model.dto.Damage dto = new com.cassens.autotran.data.model.dto.Damage();

		dto.setDelivery_vin_id(this.delivery_vin_id);
		dto.setInspection_id(this.inspection_id);

		if (this.specialCode != null && !HelperFuncs.isNullOrEmpty(this.specialCode.special_code_remote_id) && this.special_code_id != -1) {
			dto.setSpecialcode_id(Integer.parseInt(this.specialCode.special_code_remote_id.trim()));
		} else {
			if (this.areaCode != null && !HelperFuncs.isNullOrEmpty(this.areaCode.area_code_remote_id)) {
				dto.setAreacode_id(Integer.parseInt(this.areaCode.area_code_remote_id.trim()));
			}
			if (this.typeCode != null && !HelperFuncs.isNullOrEmpty(this.typeCode.type_code_remote_id)) {
				dto.setTypecode_id(Integer.parseInt(this.typeCode.type_code_remote_id.trim()));
			}
			if (this.severityCode != null && !HelperFuncs.isNullOrEmpty(this.severityCode.severity_code_remote_id)) {
				dto.setSvrtycode_id(Integer.parseInt(this.severityCode.severity_code_remote_id.trim()));
			}
		}
		dto.setPreload_damage(this.preLoadDamage);
		dto.setGuid(guid);
		return dto;
	}

	public com.cassens.autotran.data.model.dto.Damage getDTO(boolean forUpload) {
		if (forUpload && this.uploaded) {
			return null;
		} else {
			return this.getDTO();
		}
	}

	public void collectDamageNoteForTemplate(final Activity activity, final DamageNoteTemplate template,
											 final DamageNoteCallback callback, final boolean isDriver,
											 final DeliveryVin deliveryVin) {
        collectDamageNoteForTemplate(activity, template, callback, isDriver, deliveryVin, null);
    }

	public void collectDamageNoteForTemplate(final Activity activity,
											 final DamageNoteTemplate template, final DamageNoteCallback callback,
											 final boolean isDriver, final DeliveryVin deliveryVin, final Delivery thisDelivery) {
		DamageNote damageNote = new DamageNote();

		if(!isDriver) {
			//If this is a dealer note, we need to look up the driver version of this note so that it's an update
			ArrayList<DamageNote> damageNotes = DataManager.getDamageNotes(activity, this);

			if(damageNotes != null) {
				for(DamageNote tempNote : damageNotes) {
					//we don't need to check guids since the db query handled that...
					if(tempNote.damage_note_template_id == template.id) {
						damageNote = tempNote;
					}
				}
			}
		}


		damageNote.damage_note_template_id = template.id;
		damageNote.damage_guid = guid;

		AlertDialog.Builder builder = new AlertDialog.Builder(activity);
		String title = deliveryVin.vin.vin_number;
		builder.setTitle(title);

		String message = "Damage: " + this.toString() + "\n" +
				(isDriver ? template.driver_prompt : template.dealer_prompt);

		log.debug(Logs.INTERACTION, title);
		log.debug(Logs.INTERACTION, message);

		builder.setMessage(message);
		final DamageNote finalDamageNote = damageNote;

		final String commentMessage = "* Damage: " + this.toString() + "\n" + template.comment;

		builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				if(isDriver) {
					log.debug(Logs.INTERACTION, "Driver clicked 'yes'");
				} else {
					log.debug(Logs.INTERACTION, "Dealer clicked 'yes'");
				}

				if (preLoadDamage) {
					finalDamageNote.preload_driver_comment = "* Yes";
					deliveryVin.addPreloadNote(commentMessage + " Driver: Yes");

					DataManager.insertDeliveryVinToLocalDB(activity.getApplicationContext(), deliveryVin);
				} else if(isDriver){
					finalDamageNote.delivery_driver_comment = "* Yes";
					deliveryVin.addDeliveryNote(commentMessage + " Driver: Yes");

					DataManager.insertDeliveryVinToLocalDB(activity.getApplicationContext(), deliveryVin);
				} else {
					finalDamageNote.delivery_dealer_comment = "* Yes";
					thisDelivery.addDealerComment(deliveryVin.vin.vin_number + " " + commentMessage + ": Yes");
					DataManager.insertDeliveryToLocalDB(activity.getApplicationContext(), thisDelivery);
				}

				DataManager.insertDamageNoteToLocalDB(activity.getApplicationContext(), finalDamageNote);
				dialog.dismiss();
				callback.damageNoteCollected();
			}
		});
		builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {

				if(isDriver) {
					log.debug(Logs.INTERACTION, "Driver clicked 'no'");
				} else {
					log.debug(Logs.INTERACTION, "Dealer clicked 'no'");
				}

				//We were asked not to include 'no' in the comments but we need them in the NOTE to make sure that we have it marked as collected...

				if(preLoadDamage) {
					finalDamageNote.preload_driver_comment = "* No";
					//deliveryVin.addPreloadNote(commentMessage + " Driver: No");
					DataManager.insertDeliveryVinToLocalDB(activity.getApplicationContext(), deliveryVin);
				} else if(isDriver) {
					finalDamageNote.delivery_driver_comment = "* No";
					//deliveryVin.addDeliveryNote(commentMessage + " Driver: No");
					DataManager.insertDeliveryVinToLocalDB(activity.getApplicationContext(), deliveryVin);
				} else {
					finalDamageNote.delivery_dealer_comment = "* No";
					//thisDelivery.addDealerComment(deliveryVin.vin.vin_number + " " + commentMessage + ": No");
					DataManager.insertDeliveryToLocalDB(activity.getApplicationContext(), thisDelivery);
				}

				DataManager.insertDamageNoteToLocalDB(activity.getApplicationContext(), finalDamageNote);

				dialog.dismiss();
				callback.damageNoteCollected();
			}
		});
		builder.setCancelable(false);

		builder.create().show();
	}

	public interface DamageNoteCallback {
        void damageNoteCollected();
    }
}
