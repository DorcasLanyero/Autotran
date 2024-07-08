package com.cassens.autotran.data.model;

import androidx.appcompat.widget.ThemedSpinnerAdapter;

import com.google.gson.annotations.SerializedName;
import com.sdgsystems.util.HelperFuncs;

public class VIN {;
	
	public int vin_id;

	@SerializedName("id")
	public String vin_remote_id;

	public int load_id;
	public String vin_number;
	public int  dealer_id;
	public String  status;
	public String  body;
	public String  weight;
	public String  colordes;
	public String  ldnbr;
	public String  type;
	public String 	created;
	public String  driver_comment;
	public String  is_damage;
	public String 	fillers;
	public String  ats;
	public String  notes_image;
	public String  modified;
	public String  color;
	public String 	callback;
	public String  rldspickup;
	public String  notes;
	public String  ldseq;
	public String  dealer_comment;
	public String  customer_name;

	public String getDescription() {
		if (HelperFuncs.isNullOrEmpty(this.type)) {
			return(HelperFuncs.noNull(this.colordes).trim());
		}
		else if (HelperFuncs.isNullOrEmpty(this.colordes)) {
			return(HelperFuncs.noNull(this.type).trim());
		}
		else {
			return this.type.trim() + " - " + this.colordes.trim();
		}
	}
}