package com.cassens.autotran.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

abstract class VINSelectActivity extends GenericScanningActivity
{
    private static final Logger log = LoggerFactory.getLogger(VINSelectActivity.class.getSimpleName());
	
    protected enum DisplayMode { VIN_GET_MODE, VIN_PICK_MODE }

	protected DisplayMode displayMode;
    protected boolean reviewOnly;

    // Named resources for screen widgets
    protected TextView screenTitle;  
    // vinGetLayout Resources
    protected RelativeLayout vinGetLayout;
	protected Button doneButton;
	protected Button scanButton;
	protected EditText vinNumberField;
    // vinPickLayout Resources
    protected LinearLayout vinPickLayout;
	protected LinearLayout vinPickScanControls;
	protected View relayPoint;
	protected TextView vinPickSelectVinPrompt;
	protected Button vinPickScanButton;
    protected ListView listView;
    protected Button reviewButton;
    protected Button proceedButton;
	protected Button customPickButton;
	protected TextView messageText;
	protected LinearLayout dealerUnavailableLayout;
	protected TextView loadNum;
	protected CheckBox dealerUnavailableCheckbox;
	protected TextView dealerUnavailableReason;
    
    // Request Codes for Launched Activities
    protected static final int REQ_CODE_SCAN = 1001;
    protected static final String TAG = "VINSelectActivity";
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
		displayMode = DisplayMode.VIN_GET_MODE;
		mManualEntryEnabled = true;
	    reviewOnly = false;
        mScanDataType = ScanDataType.FULL_VIN;
	}

	@Override
	protected void onResume() {
		super.onResume();
	    drawLayout();
	}

	protected void drawLayout() {
		setContentView(R.layout.activity_vin_select);
		screenTitle = findViewById(R.id.titleTextView);
		log.debug(Logs.DEBUG, "In super.drawlayout()");
      
        // Resources for vinGetLayout
        vinGetLayout = findViewById(R.id.vinGetLayout);
        vinNumberField = findViewById(R.id.enterVinEditText);
		loadNum = findViewById(R.id.load_num);
		doneButton = findViewById(R.id.doneButton);
		scanButton = findViewById(R.id.scanButton);
		scanButton.setOnClickListener(v -> startScan());
		messageText = findViewById(R.id.messageText);
        
        //disable scans until barcode service is available
        ///scanButton.setEnabled(false);
        
        // Resources for vinPickLayout
        vinPickLayout = findViewById(R.id.vinPickLayout);
        listView = findViewById(R.id.vinListView);
		vinPickScanControls = findViewById(R.id.vinPickScanControls);
		vinPickScanControls.setVisibility(View.VISIBLE);
		vinPickSelectVinPrompt = findViewById(R.id.vinPickSelectVinPrompt);
		vinPickScanButton = findViewById(R.id.vinPickScanButton);
		vinPickScanButton.setOnClickListener(v -> startScan());
		if (Constants.FEATURE_ALLOW_VIN_CLICK_AFTER_INSPECTION) {
			vinPickSelectVinPrompt.setText(R.string.tap_or_scan_vin_to_select);
		}
		else {
			vinPickScanButton.setVisibility(View.VISIBLE);
		}
        proceedButton = findViewById(R.id.proceedButton);
        reviewButton = findViewById(R.id.reviewButton);
        reviewButton.setVisibility(View.GONE);
		customPickButton = findViewById(R.id.customPickButton);
		customPickButton.setVisibility(View.GONE);
		dealerUnavailableLayout = findViewById(R.id.dealerUnavailableToggleLayout);
		dealerUnavailableLayout.setVisibility(View.GONE);
		dealerUnavailableCheckbox = findViewById(R.id.dealerUnavailableCheckbox);
		dealerUnavailableCheckbox.setVisibility(View.VISIBLE);
		dealerUnavailableReason = findViewById(R.id.dealerUnavailableMsg);

		//Resource for relay point
		relayPoint = findViewById(R.id.relay_title);
	}


	@Override
	public void onScanFailureRunOnUiThread(String errorMsg) {
		super.onScanFailureRunOnUiThread("No VIN was scanned");
	}

	@Override
	protected void onScanResultRunOnUiThread(String barcode) {
		//log.debug(Logs.DEBUG, "onScanResultRunOnUiThread() got barcode: " + barcode);

		User supervisor = DataManager.getUserForSupervisorCode(VINSelectActivity.this, barcode);
		if (supervisor != null) {
			log.debug(Logs.DEBUG, "Scanned a supervisor card in a VIN select activity");
			return;
		}

		String vinNumber = CommonUtility.processScannedVIN(barcode);

		onVINSelected(vinNumber, true);
	}
	 

	public void onVINSelected(String vinNumber, boolean vinScanned) {
	    // Subclass should override this class
	    log.debug(Logs.DEBUG, "VINSelectActivity: VIN " + vinNumber + " selected");
	}
	
	
    public void onProceedButton(View v) {
        // Override, if action needed
    }

	public void onDoneButton(View v) {
		// Override, if action needed
	}
    
    public void onReviewButton(View v) {
        // Override if needed
    }

	public void onCustomPickButton(View v) {
		// Override if needed
	}

	private boolean dealerUnavailable;

	protected void setDealerUnavailableCheckbox(boolean checked) {
		if (dealerUnavailableCheckbox.isChecked() != checked) {
			dealerUnavailableCheckbox.setChecked(checked);
		}
		this.dealerUnavailable = checked;
	}

	protected void enableDealerUnavailableToggle(boolean on) {
		dealerUnavailableLayout.setVisibility(View.VISIBLE);
		setDealerUnavailableCheckbox(on);
	}

	protected void disableDealerUnavailableToggle() {
		dealerUnavailableLayout.setVisibility(View.GONE);
	}

	protected void setDealerUnavailable(boolean on) {
		if (!onDealerUnavailableToggle(on)) {
			return;
		}
		setDealerUnavailableCheckbox(on);
	}

	protected boolean isDealerUnavailableSet() {
		return this.dealerUnavailable;
	}

	public void onDealerUnavailableClick(View v) {
		setDealerUnavailable(!this.dealerUnavailable);
	}

	protected boolean onDealerUnavailableToggle(boolean newState) {
		// Override if needed. Return false if toggle should not happen.
		return true;
	}

    @Override
	protected void back(View v) {
		// By default, back button from VIN_PICK_MODE takes you back to
        // VIN_GET_MODE in order to simulate hierarchy of activities.
        log.debug(Logs.DEBUG, "VINSelectActivity: Back button pressed");
        if (displayMode == DisplayMode.VIN_PICK_MODE) {
            displayMode = DisplayMode.VIN_GET_MODE;
            drawLayout();
        }
        else {
        	log.debug(Logs.DEBUG, "pressed 'back'");
            finish();
        }
	}
	
	@Override
	public void onBackPressed() {
		log.debug(Logs.DEBUG, "onBackPressed"); 
		log.debug(Logs.DEBUG, "disconnecting scan manager");
		back(null);
	}
	
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		log.debug(Logs.DEBUG, "activity result"); 
		
		super.onActivityResult(requestCode, resultCode, data);
    	log.debug(Logs.DEBUG, "VINListActivity: onActivityResult requestCode=" + requestCode
                         + "resultCode=" + resultCode);
    
        switch (requestCode) {
        
        case REQ_CODE_SCAN:
          
            break;
            
        default:
            break;
        }
    }
    
	public static boolean hasLoadPositionIssues(List<Delivery> deliveryList) {
		String duplicate = null;
		ArrayList<String> loadPositionMap = new ArrayList<>();
		for (Delivery d : deliveryList) {
			for (DeliveryVin dv : d.deliveryVins) {
				if (loadPositionMap.contains(dv.position)) {
					duplicate = dv.position;
				} else if (dv.position == null
						|| dv.position.toLowerCase().equals("0")
						|| dv.position.toLowerCase().equals("null")
						|| dv.position.trim().equals("")) {
					CommonUtility.showText("VIN " + dv.vin.vin_number+ " does not have a position set");
					return true;
				} else {
					loadPositionMap.add(dv.position);
				}
			}
		}

		if (duplicate != null) {
			CommonUtility.showText("You cannot have more than one vehicle in position ");
			return true;
		}

		return false;
	}

	public class ScannedVinException extends ScannedValueException {
		public static final int INVALID_VIN_SCANNED = 3;
		public static final int VIN_NOT_IN_LOAD = 4;

		public ScannedVinException() {
			failureCode = INVALID_VIN_SCANNED;
			failureMessage = "Error: Not a valid VIN";
		}

		public ScannedVinException(int failureCode, String failureMessage) {
			this.failureCode = failureCode;
			this.failureMessage = failureMessage;
		}
	}

	@Override
	protected void validateScannedValue(String scannedValue) throws GenericScanningActivity.ScannedValueException {
		super.validateScannedValue(scannedValue);
		if (!CommonUtility.checkVinNoPopup(scannedValue)) {
			throw new ScannedVinException();
		}
	}

	public boolean isReviewOnly() {
		return reviewOnly;
	}
}
