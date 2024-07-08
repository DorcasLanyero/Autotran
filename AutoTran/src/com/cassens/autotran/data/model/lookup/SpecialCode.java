package com.cassens.autotran.data.model.lookup;

import com.cassens.autotran.CommonUtility;
import com.sdgsystems.util.HelperFuncs;

import java.util.Date;

public class SpecialCode {
	public int special_code_id;
	public String special_code_remote_id;
	private String areaCode;
	private String typeCode;
	private String severityCode;
	private String description;
	public Date modified;
	public boolean active;

	public String getFormattedAreaCode() {
		return CommonUtility.getZeroPaddedCode(this.getAreaCode());
	}

	public String getFormattedTypeCode() {
		return CommonUtility.getZeroPaddedCode(this.getTypeCode());
	}

	public String getFormattedSeverityCode() {
		return HelperFuncs.noNullOrWhitespace(this.getSeverityCode(), "");
	}

	public String getAreaCode() {
		return areaCode;
	}

	public void setAreaCode(String areaCode) {
		this.areaCode = areaCode;
	}

	public String getTypeCode() {
		return typeCode;
	}

	public void setTypeCode(String typeCode) {
		this.typeCode = typeCode;
	}

	public String getSeverityCode() {
		return severityCode;
	}

	public void setSeverityCode(String severityCode) {
		this.severityCode = severityCode;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
