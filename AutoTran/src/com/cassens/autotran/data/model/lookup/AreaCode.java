package com.cassens.autotran.data.model.lookup;

import com.cassens.autotran.CommonUtility;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class AreaCode implements Comparable<AreaCode>{
	public int area_code_id;
	public int parent_area_code_id;
	public String area_code_remote_id;
	private String code;
	private String description;
	public List<AreaCode> childAreaCodes;
	public Date modified;
	public boolean active = true;

	public AreaCode() {
		childAreaCodes = new ArrayList<AreaCode>();
	}

	@Override
	public int compareTo(AreaCode another) {
		
		int thisCode = 0;
		int thatCode = 0;
		
		thisCode = Integer.parseInt(getCode());
		thatCode = Integer.parseInt(another.getCode());
		
		return (thisCode > thatCode ? 1 : thisCode == thatCode ? 0 : -1);

	}

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
