package com.cassens.autotran.data.model.lookup;

import com.sdgsystems.util.HelperFuncs;

import java.util.Date;

public class SeverityCode {
	public int severity_code_id;
	public String severity_code_remote_id;
	private String code;
	private String description;
	public Date modified;
	public boolean active;

	public String getFormattedCode() {
		return HelperFuncs.noNullOrWhitespace(getCode(), "");
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
