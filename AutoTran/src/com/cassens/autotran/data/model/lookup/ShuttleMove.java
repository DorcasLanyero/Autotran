package com.cassens.autotran.data.model.lookup;

import com.google.gson.annotations.SerializedName;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class ShuttleMove {
    private static final Logger log = LoggerFactory.getLogger(ShuttleMove.class.getSimpleName());

	@SerializedName("id")
	public int shuttleMoveId = -1;

	public String orgDestString;

	public String terminal;
	public String moveCode;
	public boolean checkRoute;
	public boolean updateLot;
	public Boolean jsAsn = null;
	public Boolean lotLocateRequired = false;
	public String lot = null;
	public String alphaLot = null;
	public String origin = null;
	public String destination = null;
	public String[] routes = null;

	public Date modified;
	public int active;

	public ShuttleMove() {
	}

	public ShuttleMove(String orgDestString)
	{
		this.orgDestString = orgDestString;
		this.setProps(orgDestString);
	}

	public final static String TAG = "ShuttleMove";

/*
50MQ YNMOUND RD STORAGE YARD    NEW BOSTON RAILHEAD      CLN   20   21   23   26   29  29A   49   51   57   58   60   62   63   66  66A  66L   89   97   AA   AB   AD   AE   AF   AI   AJ   AK   AM  ANT   AO   AR   AT   AY   B2   BA   BE   BH   BO  BPR   BR BRVF BRVX BULF   CA  CAM   CH   CI   CL   CO CROF   D0   DA   DD   DZ   EC   EG   EX   FI   FR   GH   GR HNKF   IL   JO   JX   K3   KW   LB LIVF   LV   MA   ME   NC   NO   NZ   OM   PE   PL   PR PRTF   PT PTBF   PY   QA ROMF   RU   SA   SB   SK   SL SLOF
50KG NNCN STORAGE YARD          GATEWAY YARD             CLN*
50JO NNDETROIT                 CASSENS STICKNEY         CLN   3A   4H   4J   4L   4M   4P   4R   4T   4W**
*/


	public void setProps(String orgDestString) {
		if (!HelperFuncs.isNullOrEmpty(orgDestString)) {
			//log.debug(Logs.DEBUG, "orgDestString.length(): " + orgDestString.length());
			this.origin = orgDestString.substring(7, 32).trim();
			//log.debug(Logs.DEBUG, "origin: " + this.origin);
			this.destination = orgDestString.substring(32, 57).trim();
			//log.debug(Logs.DEBUG, "destination: " + this.destination);
			this.terminal = orgDestString.substring(0, 2);
			this.moveCode = orgDestString.substring(2, 4);
			this.checkRoute = charToBool(orgDestString.charAt(5));

			if(orgDestString.charAt(6) == 'Y' || orgDestString.charAt(6) == 'y' || orgDestString.charAt(6) == 'L' || orgDestString.charAt(6) == 'l') {
				lotLocateRequired = true;
			} else {
				this.updateLot = charToBool(orgDestString.charAt(6));
			}

			if (orgDestString.charAt(orgDestString.length() - 1) != '*'
					&& orgDestString.charAt(orgDestString.length() - 2) == '*') {
				this.jsAsn = charToBool(orgDestString.charAt(orgDestString.length() - 1));
			}
			List<String> routes = new ArrayList<>();
			for (int i = 60; i < orgDestString.length() - 5 && orgDestString.charAt(i) != '*'; i += 5) {
				routes.add(orgDestString.substring(i, i + 5).trim());
			}
			this.routes = routes.toArray(new String[routes.size()]);
		}
	}

	private boolean charToBool(char c) {
		return c == 'Y' || c == 'y';
	}

	public String getTerminal() {
		return this.terminal;
	}
	
	public String getMoveCode() {
		try {
			return orgDestString.substring(2, 4);
		}
		catch (IndexOutOfBoundsException ex) {
			return null;
		}
	}
	
	public String getOriginName() {
		return this.origin;
	}
	
	public String getDestinationName() {
		return this.destination;
	}
}
