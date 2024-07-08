package com.cassens.autotran.data.model;

import android.text.TextUtils;

import com.sdgsystems.app_config.AppSetting;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

public class Dealer {
	public int dealer_id;
	public String dealer_remote_id;
	public String mfg;
	public String customer_number;
	public String customer_name;
	public String city;
	public String state;
	public String address;
	public String zip;
	public String contact_name;
	public String email;
	public String phone;
	public Integer monam = -1, tueam = -1, wedam = -1, thuam = -1, friam = -1, satam = -1, sunam = -1;
	public Integer monpm = -1, tuepm = -1, wedpm = -1, thupm = -1, fripm = -1, satpm = -1, sunpm = -1;
	//public String monam, tueam, wedam, thuam, friam, satam, sunam;
	//public String monpm, tuepm, wedpm, thupm, fripm, satpm, sunpm;
	public String afthr = "";

	public String comments;
	public String status;
	public boolean high_claims;
	public boolean alwaysUnattended;
	public boolean photosOnUnattended;
	public boolean lotLocateRequired;
	public Date lastUpdated;
	public int lot_code_id;
	public String countryCode;

	private ArrayList<UpdatedField> updatedFields = new ArrayList<>();

	public String getDealerDisplayName() {
		return String.format("%s - %s, %s", this.customer_name, this.city,
				this.state);
	}

	public boolean shouldBeOpen() {
		boolean shouldBeOpen = false;

		Calendar today = Calendar.getInstance();
		int current = (today.get(Calendar.HOUR_OF_DAY) * 100) + today.get(Calendar.MINUTE);

		switch (today.get(Calendar.DAY_OF_WEEK)) {
			case Calendar.SUNDAY:
				if (current >= sunam && current < sunpm) {
					shouldBeOpen = true;
				}
				break;

			case Calendar.MONDAY:
				if (current >= monam && current < monpm) {
					shouldBeOpen = true;
				}
				break;

			case Calendar.TUESDAY:
				if (current >= tueam && current < tuepm) {
					shouldBeOpen = true;
				}
				break;
			case Calendar.WEDNESDAY:
				if (current >= wedam && current < wedpm) {
					shouldBeOpen = true;
				}
				break;

			case Calendar.THURSDAY:
				if (current >= thuam && current < thupm) {
					shouldBeOpen = true;
				}
				break;

			case Calendar.FRIDAY:
				if (current >= friam && current < fripm) {
					shouldBeOpen = true;
				}
				break;

			case Calendar.SATURDAY:
				if (current >= satam && current < satpm) {
					shouldBeOpen = true;
				}
				break;
		}

		return shouldBeOpen;
	}

	public enum AfterHoursDeliveryPermission {
		ALLOWED,
		ALLOWED_WITH_CALL_AHEAD,
		NOT_ALLOWED
	}

	public AfterHoursDeliveryPermission getAfterHoursDeliveryPermission() {
		AfterHoursDeliveryPermission specification;
		if (afthr.equalsIgnoreCase("Y")) {
			specification = AfterHoursDeliveryPermission.ALLOWED;
		} else if (afthr.equalsIgnoreCase("C")) {
			specification = AfterHoursDeliveryPermission.ALLOWED_WITH_CALL_AHEAD;
		} else {
			specification = AfterHoursDeliveryPermission.NOT_ALLOWED;
		}
		return specification;
	}

	public boolean acceptsAfterHoursDelivery() {

		return getAfterHoursDeliveryPermission() != AfterHoursDeliveryPermission.NOT_ALLOWED;
	}

	public boolean requiresSafeDeliveryLocation() {

		return false;

    	/*
    	//Safe delivery is not currently required by any mfg
		if(mfg != null && Constants.safeDeliveryMfgs.contains(mfg)) {
			return true;
		}

		return false;
		*/
	}

	public boolean requiresGlovisStiProcedure() {
		return mfg != null &&
				(
						mfg.equalsIgnoreCase("HY")
								|| mfg.equalsIgnoreCase("KI")
								|| mfg.equalsIgnoreCase("GE")
				);
	}

	public boolean hoursSet() {
		if (monam != -1 || monpm != -1) {
			return true;
		} else if (tueam != -1 || tuepm != -1) {
			return true;
		} else if (wedam != -1 || wedpm != -1) {
			return true;
		} else if (thuam != -1 || thupm != -1) {
			return true;
		} else if (friam != -1 || fripm != -1) {
			return true;
		} else if (satam != -1 || satpm != -1) {
			return true;
		} else if (sunam != -1 || sunpm != -1) {
			return true;
		}
		return false;
	}


	public class UpdatedField {
		public String fieldName;
		public long lastUpdated;

		UpdatedField(String fieldName, long lastUpdated) {
			this.fieldName = fieldName;
			this.lastUpdated = lastUpdated;
		}

		public boolean changedAlertExpired() {
			long updatedFieldsMaxNotifyTime;
			if (AppSetting.DEALER_UPDATE_TEST_MODE.getBoolean()) {
				// In test mode we ignore DEALER_UPDATE_NOTIFY_DAYS use a 5 minute timeout
				updatedFieldsMaxNotifyTime = (long) AppSetting.DEALER_UPDATE_TEST_MINUTES.getInt() * 60 * 1000;
			}
			else {
				updatedFieldsMaxNotifyTime = (long) AppSetting.DEALER_UPDATE_NOTIFY_DAYS.getInt() * 24 * 60 * 60 * 1000;
			}
			return ((System.currentTimeMillis()) - this.lastUpdated) > updatedFieldsMaxNotifyTime;
		}

		@Override
		public String toString() {
			if (fieldName == null) {
				return "";
			}
			else {
				return fieldName + " " + lastUpdated;
			}
		}

		public UpdatedField fromString(String updatedFieldEntry) {
			if (fieldName != null && updatedFieldEntry != null) {
				String[] fields = updatedFieldEntry.split(" ");
				if (fields.length == 2) {
					try {
						return new UpdatedField(fields[0], Long.parseLong(fields[1]));
					} catch (NumberFormatException ex) {
						// do nothing
					}
				}
			}
			return null;
		}
	}

	public void clearUpdatedFields() {
		updatedFields.clear();
	}

	public void insertUpdatedField(String fieldName, long dateModified) {
		Iterator<UpdatedField> iter = updatedFields.iterator();
		while (iter.hasNext()) {
			UpdatedField field = iter.next();
			// Go ahead and prune expired entries since we're going through the list
			if (field.fieldName.equals(fieldName) || field.changedAlertExpired()) {
				iter.remove();
			}
		}
		UpdatedField newField = new UpdatedField(fieldName, dateModified);
		if (!newField.changedAlertExpired()) {
			updatedFields.add(newField);
		}
	}

	private void pruneUpdatedFields() {
		Iterator<UpdatedField> iter = updatedFields.iterator();
		while (iter.hasNext()) {
			if (iter.next().changedAlertExpired()) {
				iter.remove();
			}
		}
	}

	public ArrayList<UpdatedField> getUpdatedFields() {
		pruneUpdatedFields();
		return updatedFields;
	}

	public boolean hasUpdatedFields() {
		return getUpdatedFields().size() > 0;
	}

	public void setUpdatedFields(ArrayList<UpdatedField> updatedFields) {
		if (updatedFields == null) {
			clearUpdatedFields();
		}
		else {
			this.updatedFields = updatedFields;
		}
	}

	public List<String> getUpdatedFieldsStringList() {
		List<String> updatedFieldStrings = new ArrayList<>();
		if (updatedFields != null ) {
			for (UpdatedField updatedField : getUpdatedFields()) {
				updatedFieldStrings.add(updatedField.toString());
			}
		}
		return updatedFieldStrings;
	}

	public String getUpdatedFieldsCsv() {
		String updatedFieldsCsv = "";
		if (updatedFields == null ) {
			return "";
		}
		return TextUtils.join(",", getUpdatedFieldsStringList());
	}
}

