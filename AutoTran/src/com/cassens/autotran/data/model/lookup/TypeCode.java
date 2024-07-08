package com.cassens.autotran.data.model.lookup;

import com.cassens.autotran.CommonUtility;

import java.util.Date;

public class TypeCode {
	public int type_code_id;
	public String type_code_remote_id;
	private String code;
	private String description;
	public Date modified;
	public boolean active;

	public String getFormattedCode() {
		return CommonUtility.getZeroPaddedCode(this.getCode());
	}

	public String getCode() {
		return code;
	}

	public void setCode(String code) {
		this.code = code;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}
}
