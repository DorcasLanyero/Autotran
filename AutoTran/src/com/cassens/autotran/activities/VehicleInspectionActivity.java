package com.cassens.autotran.activities;

import static com.cassens.autotran.activities.VINInspectionActivity.formatATSValue;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.gridlayout.widget.GridLayout;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Inspection;
import com.cassens.autotran.data.model.lookup.AreaCode;
import com.cassens.autotran.data.model.lookup.ScacCode;
import com.cassens.autotran.data.model.lookup.SpecialCode;
import com.cassens.autotran.data.model.lookup.TypeCode;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.dialogs.ImageViewDialog;
import com.cassens.autotran.handlers.ImageHandler;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.NoNullsArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Project : AUTOTRAN
 * Description : VehicleInspectionActivity class record results of inspections;
 * but only if initiated via the Dashboard's "Inspect Vehicle"
 *
 * @author Hemant Creation Date : 12-11-2013
 */
public class VehicleInspectionActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(VehicleInspectionActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	String employeeNumber;
    Button cancelButton, notesButton, saveButton, specialButton, cameraButton;
    ImageView backButton;
	TextView vinTextView;
	TextView terminalTextView;
	TextView lotCodeTextView;
	TextView scacCodeTextView;
	TextView inspectionTypeDropdown;
	ImageView dropDownImage;
	Spinner inspectionTypeSpinner;
	LinearLayout three_text_view_lay;
	private int position;
	private ArrayList<AreaTypeSvrty> areaTypeSvrties;
	private FusedLocationProviderClient fusedLocationClient;
	private LocationCallback locationCallback;
	private LocationRequest locationRequest;
	private Location lastLocation;
	private Bundle bundle;
	private String reasonString;
    boolean isEditable = true;
	public static boolean dataNotSent = false;
	private LinearLayout llUp, llFirst, llMid, llLast;
	private GridLayout cameraImageLayout;
	private ScrollView scrollView;
	private HorizontalScrollView imgScrollView;
	private int operation;

	private static String ATS_NO_VAL = "";
	private Inspection thisInspection;

	private static boolean locationConnected = false;
	private Location mLocation;
	private static final boolean DEBUG = false;
	private String mImageFileNamePrefix = "Inspection_";

    private String mCurrentPhotoFileName;

	private Runnable scrollDown = new Runnable() {
		@Override
		public void run() {
			scrollView.fullScroll(View.FOCUS_DOWN);
		}
	};

	private Runnable scrollImagesLeft = new Runnable(){
		@Override
		public void run() {
			imgScrollView.fullScroll(View.FOCUS_LEFT);
		}
	};

    // Request Codes for Launched Activities
    private static final int REQ_CODE_AREA = 1001;
    private static final int REQ_CODE_TYPE = 1002;
    private static final int REQ_CODE_SVRTY = 1003;
    private static final int REQ_CODE_SPECIAL = 1005;
    private static final int REQ_CODE_NOTES = 1006;
    private static final int REQ_CODE_CAPTURE_IMAGE = 1007;
    private static final int REQ_CODE_REJECTION_VIN = 1008;
    private static final int REQ_CODE_TERMINAL_NUM = 1010;
    private static final int REQ_CODE_LOT_NUM = 1011;
    private static final int REQ_CODE_SCAC_CODE = 1012;
	private static final String TAG = "VehicleInspection";

	private NoNullsArrayList<Image> activeImages;
	private final Activity currentActivity = this;

	// must start new damage_ids at -2 so we know when something hasn't been touched
	private int nextDamageId = -1;

	private cameraController camera;

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_vehicle_inspection);

		llUp = (LinearLayout) findViewById(R.id.ll_up);
		llFirst = (LinearLayout) findViewById(R.id.ll_first);
		llMid = (LinearLayout) findViewById(R.id.ll_mid);
		llLast = (LinearLayout) findViewById(R.id.ll_last);
		cameraImageLayout = (GridLayout) findViewById(R.id.cameraImagesLL);
		scrollView = (ScrollView) findViewById(R.id.verticalScrollView);
		imgScrollView = findViewById(R.id.imageScrollView);

		cancelButton = (Button) findViewById(R.id.cancel);
		notesButton = (Button) findViewById(R.id.notes);
		saveButton = (Button) findViewById(R.id.save);
		specialButton = (Button) findViewById(R.id.special);
		backButton = (ImageView) findViewById(R.id.back);

		cameraButton = (Button) findViewById(R.id.cameraButton);

		bundle = getIntent().getExtras();
		//isEditable = bundle.getBoolean("is_editable", true);
		isEditable = true;
		operation = bundle.getInt(Constants.CURRENT_OPERATION);  
        employeeNumber = bundle.getString("driverNumber", "");

		terminalTextView = (TextView) findViewById(R.id.terminal);
        terminalTextView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
				Intent i = new Intent(VehicleInspectionActivity.this, TerminalCodeList.class);
				startActivityForResult(i, REQ_CODE_TERMINAL_NUM);
            }
        });

        lotCodeTextView = (TextView) findViewById(R.id.lot);
        lotCodeTextView.setShowSoftInputOnFocus(false);
        lotCodeTextView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0) {
				if (!HelperFuncs.isNullOrEmpty(terminalTextView.getText().toString())) {
					Intent intent = new Intent(VehicleInspectionActivity.this, LotCodeList.class);
					intent.putExtra("title", lotCodeTextView.getText().toString());
					intent.putExtra("terminal_id", Integer.parseInt(terminalTextView.getText().toString()));
					startActivityForResult(intent, REQ_CODE_LOT_NUM);
				} else {
					CommonUtility.showText("Please select a terminal before picking a lot");
				}
            }
        });

		inspectionTypeDropdown = (TextView) findViewById(R.id.inspection_type);
		areaTypeSvrties = new ArrayList<VehicleInspectionActivity.AreaTypeSvrty>();

		inspectionTypeSpinner = (Spinner) findViewById(R.id.spinner);
		dropDownImage = (ImageView) findViewById(R.id.inspection_type_drop_down);
		notesButton.setClickable(true);
		reasonString = "";

		vinTextView = (TextView) findViewById(R.id.id);
		vinTextView.setText(HelperFuncs.splitVin(bundle.getString("vin_number")));

		String[] inspectionTypeValues = { "Gate", "Offsite" };

		three_text_view_lay = (LinearLayout) findViewById(R.id.three_text_view_lay);

		ArrayAdapter<String> adapter = new ArrayAdapter<String>(VehicleInspectionActivity.this, android.R.layout.simple_spinner_item, inspectionTypeValues);
		adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
		inspectionTypeSpinner.setAdapter(adapter);
		inspectionTypeSpinner.setOnItemSelectedListener(new OnItemSelectedListener()
		{
			@Override
			public void onItemSelected(AdapterView<?> arg0, View arg1, int arg2, long arg3) {
				inspectionTypeDropdown.setText(inspectionTypeSpinner.getSelectedItem().toString());
			}

			@Override
			public void onNothingSelected(AdapterView<?> arg0) {
			}
		});

        scacCodeTextView = (TextView) findViewById(R.id.scac_code);
		scacCodeTextView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                Intent  intent = new Intent(VehicleInspectionActivity.this, ScacCodeList.class);
                intent.putExtra("title", scacCodeTextView.getText().toString());
                try {
					intent.putExtra("terminal_id", Integer.parseInt(terminalTextView.getText().toString()));
					startActivityForResult(intent, REQ_CODE_SCAC_CODE);
				} catch (NumberFormatException ex) {
                	CommonUtility.simpleMessageDialog(VehicleInspectionActivity.this, "Need terminal number");
				}
            }
        });
        scacCodeTextView.setEnabled(isEditable);

		this.camera = new cameraController(Constants.STANDALONE_INSPECTION_OPERATION);


		log.debug(Logs.DEBUG, "VehicleInspectionActivity: vin_number=" + bundle.getString("vin_number"));
	    log.debug(Logs.DEBUG, "VehicleInspectionActivity: delivery_id=" + bundle.getString("vin_select_key"));

		activeImages = new NoNullsArrayList<Image>();

        thisInspection = new Inspection();
        thisInspection.vin = bundle.getString("vin_number");
        thisInspection.inspector = employeeNumber;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        try {
			String defaultTerminal = prefs.getString("inspection_default_terminal", null);

			if(defaultTerminal == null) {
				// Note: If default terminal isn't set, will return default value of "50".
				//       Won't return null.
				defaultTerminal = CommonUtility.getDefaultTerminalPref(this);
			}

			thisInspection.terminal = DataManager.getTerminal(this, Integer.parseInt(defaultTerminal));
			terminalTextView.setText(defaultTerminal);

			String defaultLot = prefs.getString("inspection_default_lot", "");
			if (HelperFuncs.isNullOrEmpty(defaultLot)) {
				defaultLot = CommonUtility.getDefaultTerminalPref(this);
			}
			if (defaultLot != null) {
				if (defaultLot.equals("")) {
					lotCodeTextView.setText("");
				} else {
					thisInspection.lotCode = DataManager.getLotCode(this, defaultLot, Integer.parseInt(defaultTerminal));
					if (thisInspection.lotCode != null && thisInspection.lotCode.code != null) {
						lotCodeTextView.setText(thisInspection.lotCode.code);
					}
				}
			}

			refreshTerminalDependentFields(Integer.parseInt(defaultTerminal));

		} catch (NumberFormatException ex) {
        	thisInspection.terminal = null;
		}


		fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

		locationRequest = LocationRequest.create();
		locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
		locationRequest.setInterval(5000);

		locationCallback = new LocationCallback() {
			@Override
			public void onLocationResult(LocationResult locationResult) {
				super.onLocationResult(locationResult);
				if (locationResult != null) {
					Location location = locationResult.getLastLocation();
					lastLocation = location;
					log.error(Logs.DEBUG, "using location update " + location);
				}
			}
		};

		displayInspectionData();
	}

	private int getNextDamageId() {
		nextDamageId -= 1;
		return nextDamageId;
	}

    private Inspection getInspection(String vinNumber) {
        List <Inspection> inspectionList = DataManager.getInspectionList(this, false);
        Iterator<Inspection> itr = inspectionList.iterator();
        
        while (itr.hasNext()) {
            Inspection inspection = (Inspection)itr.next();
            if (inspection.vin.equals(vinNumber)) {
                return inspection;
            }
        }
        return null;
    }
    
	private void startLocationUpdates() {
		// Prime with the last known location, then kick off location updates.
		fusedLocationClient.getLastLocation()
			.addOnSuccessListener(this, location -> {
				if (location != null) {
					makeUseOfNewLocation(location);
				}
				fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null);
			});

	}

	private void stopLocationUpdates() {
		fusedLocationClient.removeLocationUpdates(locationCallback);
	}

    private void makeUseOfNewLocation(Location location) {
    	
    	mLocation = location;
    	
    	if(mLocation != null) {
            thisInspection.latitude = mLocation.getLatitude();
            thisInspection.longitude = mLocation.getLongitude();

        	locationConnected = true;
        	if(DEBUG) {
				CommonUtility.showText("current location: " + location.getLatitude() + ", " + location.getLongitude());
			}
        	if(DEBUG) log.debug(Logs.DEBUG, "current location: " + location.getLatitude() + ", " + location.getLongitude());
        } else {
        	if(DEBUG) log.debug(Logs.DEBUG, "no current location");
        }
	}
    
	public void noteClick(View v) {
		CommonUtility.logButtonClick(log, v);
	    Intent intent = new Intent(VehicleInspectionActivity.this, NotesActivity.class);

        intent.putExtra(NotesActivity.EXTRA_STATE, NotesActivity.GENERIC_VIN_DAMAGE);
        intent.putExtra(NotesActivity.EXTRA_NOTES, thisInspection.notes);
        intent.putExtra(Constants.CURRENT_OPERATION, operation);
        intent.putExtra(NotesActivity.EXTRA_TITLE, "Notes");
        intent.putExtra(NotesActivity.EXTRA_PROMPT, "Type a note, or choose predefined notes");
        intent.putExtra(NotesActivity.EXTRA_IS_EDITABLE, isEditable);
        startActivityForResult(intent, REQ_CODE_NOTES);
	}

	public void cameraClick(View v)	{
		CommonUtility.logButtonClick(log, v);
		log.debug(Logs.INTERACTION, "Camera clicked");

		this.camera.getImage(v);
		imgScrollView.post(scrollImagesLeft);
		scrollView.post(scrollDown);
	}

	private void showDialog(String msg)
	{
	  showDialog(msg, false);
	}
	
	private void showDialog(String msg, boolean cancelable) {
        Builder builder = new AlertDialog.Builder(VehicleInspectionActivity.this);
        //builder.setTitle("Notification");
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

        	@Override
        	public void onClick(DialogInterface dialog, int which) {
        		// TODO Auto-generated method stub
        	}
        });
        builder.setCancelable(cancelable);
        builder.create().show();
    }

	public void saveClick(View v) {
		CommonUtility.logButtonClick(log, v);

		if (CommonUtility.isNullOrBlank(terminalTextView.getText().toString())) {
		    CommonUtility.simpleMessageDialog(this, "Terminal is required");
		    log.debug(Logs.INTERACTION, "Message shown: Terminal is required");
		    return;
		}
		else if (CommonUtility.isNullOrBlank(lotCodeTextView.getText().toString())) {
            CommonUtility.simpleMessageDialog(this, "Lot Code is required");
			log.debug(Logs.INTERACTION, "Message shown: Lot Code is required");
            return;
        }
		else if (scacCodeTextView.getVisibility() == View.VISIBLE
		         && CommonUtility.isNullOrBlank(scacCodeTextView.getText().toString())) {
            CommonUtility.simpleMessageDialog(this, "Scac Code is required for selected terminal");
			log.debug(Logs.INTERACTION, "Message shown: Scac Code is required for selected terminal");
            return;
        }

		if (camera.getTotalImagesRequired() > 0) {
			camera.getNextRequiredImage(v);
			return;
		}

		for (AreaTypeSvrty areaTypeSvrty : areaTypeSvrties) {
			if(!areaTypeSvrty.isReadOnly()) {
				boolean hasBlank = areaTypeSvrty.getAreaString().equals(ATS_NO_VAL) ||
						areaTypeSvrty.getTypeString().equals(ATS_NO_VAL) ||
						areaTypeSvrty.getSvrtyString().equals(ATS_NO_VAL);
				boolean hasZero = areaTypeSvrty.getAreaString().equals("0") ||
						areaTypeSvrty.getTypeString().equals("0") ||
						areaTypeSvrty.getSvrtyString().equals("0");
				if (hasBlank || (hasZero && !areaTypeSvrty.isSpecial)) {
					log.debug(Logs.INTERACTION, "message shown: " + "All damages must have values for area, type, and severity.");
					showDialog("All damages must have values for area, type, and severity.");
					return;
				}
			}
		}

		for(int i = cameraImageLayout.getChildCount()-1; i >= 0;  i--) {
			ImageView imageView =(ImageView)cameraImageLayout.getChildAt(i);
			Image image = (Image)imageView.getTag();

			//if the image is NEW
			if(image.image_id == -1) {
     		 	image.inspection_guid = thisInspection.guid;
     		 	image.preloadImage = false;
     		 	image.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
     		 	thisInspection.images.add(image);
     		 	CommonUtility.upsertHiResCopy(this, image);
			}
   	 	}

		boolean isSevere = false;
        for (AreaTypeSvrty areaTypeSvrty : areaTypeSvrties) {
            log.debug(Logs.DEBUG, "areaTypeSvrty='"+areaTypeSvrty.getSvrtyString()+"'");
             if (!areaTypeSvrty.isSpecial && Integer.parseInt(areaTypeSvrty.getSvrtyString()) > 2) {
                 isSevere = true;
             }
             // TODO: Damage class needs to be modified to use the code rather than the ID.
             // BFF - This SHOULD have been implemented with damage objects attached to an adapter and
             //displayed in a nice list view.  Honestly, I don't know what they were thinking

             //This mechanism won't let us enter in special codes w/out some odd decoration of the data or something

             Damage damage = new Damage();
             boolean found = false;
			 damage.damage_id = areaTypeSvrty.damage_id;

             damage.preLoadDamage = false;
             damage.inspection_id = thisInspection.inspection_id;
             //Log.d(TAG, "NARF: id - " + damage.inspection_id);

             if(areaTypeSvrty.damage_id < 0) {
                 if(areaTypeSvrty.isSpecial) {
                 	damage.specialCode = DataManager.getSpecialCode(this, areaTypeSvrty.areaString, areaTypeSvrty.typeString, areaTypeSvrty.svrtyString);
                 	damage.special_code_id = damage.specialCode.special_code_id;
                 	for(Damage tmpDamage : thisInspection.damages) {
                 	    if(tmpDamage.special_code_id == damage.special_code_id)
                 	        found = true;
                 	}
				  } else {
					 damage.areaCode = DataManager.getAreaCode(this, areaTypeSvrty.areaString);
					 damage.typeCode = DataManager.getTypeCode(this, areaTypeSvrty.typeString);
					 damage.severityCode = DataManager.getSeverityCode(this, areaTypeSvrty.svrtyString);

					 damage.area_code_id = damage.areaCode.area_code_id;
					 damage.type_code_id = damage.typeCode.type_code_id;
					 damage.svrty_code_id = damage.severityCode.severity_code_id;

					 for(Damage tmpDamage : thisInspection.damages) {
					   if(tmpDamage.area_code_id == damage.area_code_id && tmpDamage.type_code_id == damage.type_code_id && tmpDamage.svrty_code_id == damage.svrty_code_id)
						   found = true;
					 }
				  }

				  thisInspection.damages.add(damage);
             } else {
            	 for(int damageIndex = 0; damageIndex < thisInspection.damages.size(); damageIndex++) {
            		 if(thisInspection.damages.get(damageIndex).damage_id == areaTypeSvrty.damage_id) {
            		     Damage tmpDamage = thisInspection.damages.get(damageIndex);
                         if(areaTypeSvrty.isSpecial()) {
                             tmpDamage.specialCode = DataManager.getSpecialCode(this, areaTypeSvrty.getAreaString(), areaTypeSvrty.getTypeString(), areaTypeSvrty.getSvrtyString());
                             tmpDamage.special_code_id = tmpDamage.specialCode.special_code_id;
                             thisInspection.damages.set(damageIndex, tmpDamage);
                         } else {
                             tmpDamage.areaCode = DataManager.getAreaCode(this, areaTypeSvrty.getAreaString());
                             tmpDamage.typeCode = DataManager.getTypeCode(this, areaTypeSvrty.getTypeString());
                             tmpDamage.severityCode = DataManager.getSeverityCode(this, areaTypeSvrty.getSvrtyString());

                             tmpDamage.area_code_id = tmpDamage.areaCode.area_code_id;
                             tmpDamage.type_code_id = tmpDamage.typeCode.type_code_id;
                             tmpDamage.svrty_code_id = tmpDamage.severityCode.severity_code_id;
                             thisInspection.damages.set(damageIndex, tmpDamage);
                        }
            		 }
            	 }
             }
        }


        log.debug(Logs.DEBUG, "isSevere=" + isSevere + " ");
        
        thisInspection.terminal =  DataManager.getTerminal(this, Integer.parseInt((String)terminalTextView.getText()));
        
        thisInspection.lotCode = DataManager.getLotCode(this, (String)lotCodeTextView.getText(), thisInspection.terminal.terminal_id);

        thisInspection.inspector = CommonUtility.getDriverNumber(this);
        if (scacCodeTextView.getVisibility() == View.VISIBLE) {
            thisInspection.scacCode = DataManager.getScacCode(this, thisInspection.terminal.terminal_id, (String)scacCodeTextView.getText());
        } else {
            thisInspection.scacCode = null;
        }
        if (inspectionTypeDropdown.getText().toString().equalsIgnoreCase("gate")) {
            thisInspection.type = Constants.INSPECTION_TYPE_GATE;
        } else {
            thisInspection.type = Constants.INSPECTION_TYPE_GATE;
        }

        thisInspection.timestamp = new Date();

        log.debug(Logs.DEBUG, "Saving thisInspection record");

        DataManager.insertInspection(this, thisInspection);
        thisInspection = getInspection(bundle.getString("vin_number"));

		SyncManager.pushInspections(this);

        SyncManager.sendNextPhoto(this);

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
	}

	public void cancelClick(View v) {
	    // For now, we're hiding the cancel button.  May put this back in later, so leaving code in for now.
		Intent returnIntent = new Intent();
		setResult(RESULT_CANCELED, returnIntent);
		finish();
	}

	private boolean atsIsIncomplete(TextView area, TextView type, TextView svrty) {
		return (area.getText().toString().equals(ATS_NO_VAL)
				|| type.getText().toString().equals(ATS_NO_VAL)
				|| svrty.getText().toString().equals(ATS_NO_VAL));
	}

	private boolean atsIsBlank(TextView area, TextView type, TextView svrty) {
        return (area.getText().toString().equals(ATS_NO_VAL)
                && type.getText().toString().equals(ATS_NO_VAL)
                && svrty.getText().toString().equals(ATS_NO_VAL));
    }

	public void specialClick(View v) {
		CommonUtility.logButtonClick(log, v);
	    position = this.three_text_view_lay.getChildCount() - 1;

        startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeListSecond.class).putExtra("check", "special")
            .putExtra("mode", "add")), REQ_CODE_SPECIAL);
	}

    public void back(View v) {
		CommonUtility.logButtonClick(log, v);
        if (!isEditable) {
            finish();
            return;
        }
        Builder builder = new AlertDialog.Builder(VehicleInspectionActivity.this);
        //builder.setTitle("Notification");
        builder.setMessage("Do you wish to save any current changes, or discard and lose unsaved progress?");
        builder.setPositiveButton("Save", (dialog, which) -> saveClick(v));
        builder.setNegativeButton("Discard", new DialogInterface.OnClickListener() {

			@Override
			public void onClick(DialogInterface dialog, int which) {
				VehicleInspectionActivity.this.finish();
			}
		});
		builder.setNeutralButton("Cancel", null);
        //builder.setCancelable(true);
        builder.create().show();    
    }
    
    @Override
    public void onBackPressed()
    {
        back(null);
    }

	public void addATS(View v) {
    	log.debug(Logs.INTERACTION, "add damage button clicked");
		addOneMoreATS(three_text_view_lay);

		position = this.three_text_view_lay.getChildCount() - 1;
		TextView area = (TextView)VehicleInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.one);
		if (area.getText().equals(ATS_NO_VAL)) {
            startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
                .putExtra("mode", "add")).putExtra("text", area.getText().toString()), REQ_CODE_AREA);
		}
	}

	public void inspectionSpinnerClick(View v)
	{
		inspectionTypeSpinner.performClick();
	}

	@Override
	protected void onPause() {
		super.onPause();
		stopLocationUpdates();
	}

	private void addOneMoreATS(LinearLayout three_text_view_lay) {
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout inflate_layout = (LinearLayout) inflater.inflate(R.layout.three_text_view, three_text_view_lay, false);
		final TextView one = (TextView) inflate_layout.findViewById(R.id.one);
		final TextView two = (TextView) inflate_layout.findViewById(R.id.two);
		final TextView three = (TextView) inflate_layout.findViewById(R.id.three);
		final Button delete = (Button) inflate_layout.findViewById(R.id.deleteButton);
		one.setText(ATS_NO_VAL);
		two.setText(ATS_NO_VAL);
		three.setText(ATS_NO_VAL);
		System.out.println("Child total : " + this.three_text_view_lay.getChildCount());
		int textPosition = three_text_view_lay.getChildCount();
		one.setTag(0 + "," + textPosition);
		two.setTag(0 + "," + textPosition);
		three.setTag(0 + "," + textPosition);
		delete.setTag(0 + "," + textPosition);

		if(this.three_text_view_lay.getChildCount() == 0) {
			three_text_view_lay.addView(inflate_layout);
		}

		one.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				log.info(Logs.DEBUG, "tag:" + v.getTag().toString());
				position = Integer.parseInt(v.getTag().toString().split(",")[1]);
				System.out.println("One position : "+position);
				if (v.getTag().toString().split(",").length == 3) {
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
							.putExtra("mode", "edit")).putExtra("text", one.getText().toString()), REQ_CODE_AREA);
				} else {
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
							.putExtra("mode", "edit")).putExtra("text", one.getText().toString()), REQ_CODE_AREA);
				}
			}
		});

		two.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				position = Integer.parseInt(v.getTag().toString().split(",")[1]);
				System.out.println("Two position : "+position);
				if (v.getTag().toString().split(",").length == 3) {
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
							.putExtra("mode", "edit")).putExtra("text", two.getText().toString()), REQ_CODE_TYPE);
				} else {
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
							.putExtra("mode", "edit")).putExtra("text", two.getText().toString()), REQ_CODE_TYPE);
				}
			}
		});

		three.setOnClickListener(new OnClickListener() {
			@Override
			public void onClick(View v) {
				position = Integer.parseInt(v.getTag().toString().split(",")[1]);
				System.out.println("Three position : "+position);
				if (v.getTag().toString().split(",").length == 3) {
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
							.putExtra("mode", "edit")).putExtra("text", three.getText().toString()), REQ_CODE_SVRTY);
				}
				else {
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
							.putExtra("mode", "edit")).putExtra("text", three.getText().toString()), REQ_CODE_SVRTY);
				}
			}
		});

		delete.setOnClickListener(new deleteClickListener(one, two, three, delete));

		for (int position = 0; position < VehicleInspectionActivity.this.three_text_view_lay.getChildCount(); position++)	{
			if(position == this.three_text_view_lay.getChildCount() - 1) {
				TextView area = (TextView)VehicleInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.one);
				TextView type = (TextView)VehicleInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.two);
				TextView svrty = (TextView)VehicleInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.three);
			    System.out.println(area.getText().toString() +" :: "+ type.getText().toString() + " :: " +svrty.getText().toString());
			    if (atsIsIncomplete(area, type, svrty)) {
					if (position > 0) {
						CommonUtility.showText("Last entry was not completed");
					}
			    } else if (!atsIsIncomplete(area, type, svrty)){
			    	three_text_view_lay.addView(inflate_layout);
			    }
			    break;
			}
		}

		//three_text_view_lay.addView(inflate_layout);
	}

	private void inflateLayout(LinearLayout three_text_view_lay) {
		final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
		LinearLayout inflate_layout = (LinearLayout) inflater.inflate(R.layout.three_text_view, three_text_view_lay, false);
		final TextView one = (TextView) inflate_layout.findViewById(R.id.one);
		final TextView two = (TextView) inflate_layout.findViewById(R.id.two);
		final TextView three = (TextView) inflate_layout.findViewById(R.id.three);
		final Button delete = (Button) inflate_layout.findViewById(R.id.deleteButton);
		one.setText(ATS_NO_VAL);
		two.setText(ATS_NO_VAL);
		three.setText(ATS_NO_VAL);
		//delete.setText("D");

		if (three_text_view_lay.getChildCount() == 0) {
			one.setTag(0 + "," + 0);
			two.setTag(0 + "," + 0);
			three.setTag(0 + "," + 0);
			delete.setTag(0 + "," + 0);
		}

		one.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				log.info(Logs.DEBUG, "tag:" + v.getTag().toString());
				position = Integer.parseInt(v.getTag().toString().split(",")[1]);
				System.out.println("One position : "+position);
				if (v.getTag().toString().split(",").length == 3) {
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
							.putExtra("mode", "edit")).putExtra("text", one.getText().toString()), REQ_CODE_AREA);
				}
				else
				{
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
							.putExtra("mode", "edit")).putExtra("text", one.getText().toString()), REQ_CODE_AREA);
				}
			}
		});

		two.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				position = Integer.parseInt(v.getTag().toString().split(",")[1]);
				System.out.println("Two position : "+position);
				if (v.getTag().toString().split(",").length == 3)
				{
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
							.putExtra("mode", "edit")).putExtra("text", two.getText().toString()), REQ_CODE_TYPE);
				}
				else
				{
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
							.putExtra("mode", "edit")).putExtra("text", two.getText().toString()), REQ_CODE_TYPE);
				}
			}
		});

		three.setOnClickListener(new OnClickListener()
		{
			@Override
			public void onClick(View v)
			{
				position = Integer.parseInt(v.getTag().toString().split(",")[1]);
				System.out.println("Three position : "+position);
				if (v.getTag().toString().split(",").length == 3)
				{
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
							.putExtra("mode", "edit")).putExtra("text", three.getText().toString()), REQ_CODE_SVRTY);
				}
				else
				{
					startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
							.putExtra("mode", "edit")).putExtra("text", three.getText().toString()), REQ_CODE_SVRTY);
				}
			}
		});

		delete.setOnClickListener(new deleteClickListener(one, two, three, delete));

		three_text_view_lay.addView(inflate_layout);
	}

	private class deleteClickListener implements View.OnClickListener {

		TextView one, two, three, delete;

		public deleteClickListener(TextView one, TextView two, TextView three, TextView delete) {
			CommonUtility.logButtonClick(log, "Delete", "vehicle inspection");
			this.one = one;
			this.two = two;
			this.three = three;
			this.delete = delete;
		}

		@Override
		public void onClick(View v) {
			position = Integer.parseInt(v.getTag().toString().split(",")[1]);
			System.out.println("Delete Position Tag Value : "+position);
			//CommonUtility.showText("Delete Click on "+position);
			Builder builder = new AlertDialog.Builder(VehicleInspectionActivity.this);
		    builder.setTitle("Delete ?");
		    builder.setMessage("Would you like to delete this damage record?");
		    DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {

				@Override
				public void onClick(DialogInterface dialog, int which) {

					View deletedRow = VehicleInspectionActivity.this.three_text_view_lay.getChildAt(position);

					if(areaTypeSvrties.size() > position) {
						AreaTypeSvrty ats = areaTypeSvrties.get(position);
						areaTypeSvrties.remove(position);

						//Delete codes from the database that have been saved.  This WILL delete duplicates
						//TODO: implement the damage widget so completely differently you can't even recognize it
						for(Damage damage : thisInspection.damages) {
							if(ats.isSpecial) {
								//We can check all of the codes, but only look at special codes if we know it'll be there...
								if(damage.specialCode != null) {
									if(damage.specialCode.getAreaCode() == ats.areaString &&
										damage.specialCode.getTypeCode() == ats.typeString &&
										damage.specialCode.getSeverityCode() == ats.svrtyString) {
										DataManager.deleteDamage(getApplicationContext(), damage);
										thisInspection.damages.remove(damage);
										break;
									}
								}
							} else {
								if(damage.areaCode != null && damage.typeCode != null && damage.severityCode != null) {
									if (damage.areaCode.getCode() == ats.areaString &&
											damage.typeCode.getCode() == ats.typeString &&
											damage.severityCode.getCode() == ats.svrtyString) {
										DataManager.deleteDamage(getApplicationContext(), damage);
										thisInspection.damages.remove(damage);
										break;
									}
								}
							}
						}
					}

					VehicleInspectionActivity.this.three_text_view_lay.removeView(deletedRow);
					System.out.println("After Delete One Tag :"+one.getTag().toString());
					System.out.println("After Delete Two Tag :"+two.getTag().toString());
					System.out.println("After Delete Three Tag :"+three.getTag().toString());
					System.out.println("After Delete Del Tag :"+delete.getTag().toString());
					int firstDel, firstOne, firstTwo, firstThree;

					String deleteTag = delete.getTag().toString();
					String oneTag;
					String twoTag;
					String threeTag;

				    oneTag = one.getTag().toString();
				    twoTag = two.getTag().toString();
				    threeTag = three.getTag().toString();

					firstDel = Integer.parseInt(deleteTag.split(",")[0]);
					firstOne = Integer.parseInt(oneTag.split(",")[0]);
					firstTwo = Integer.parseInt(twoTag.split(",")[0]);
					firstThree = Integer.parseInt(threeTag.split(",")[0]);
					//int latestTagOnDeleteButton = Integer.parseInt(delete.getTag().toString().split(",")[1]) - 1;
					//delete.setTag(first + "," + latestTagOnDeleteButton);
					for (int position=0; position < VehicleInspectionActivity.this.three_text_view_lay.getChildCount(); position++) {
						//int pos = position -1;
						TextView one = (TextView)VehicleInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.one);
						one.setTag(firstOne+","+position);
						TextView two = (TextView)VehicleInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.two);
						two.setTag(firstTwo+","+position);
						TextView three = (TextView)VehicleInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.three);
						three.setTag(firstThree+","+position);
						Button delete = (Button)VehicleInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.deleteButton);
					    delete.setTag(firstDel+","+position);
					    System.out.println("After Delete Del :: Position : "+position+" :: TAG : "+delete.getTag().toString());
					}
				}
			};
		    builder.setPositiveButton("Yes", dialogListener);
		    builder.create().show();
		}

	};


    private void refreshTerminalDependentFields(int terminal_id) {
		List<ScacCode> scacCodeList = (List<ScacCode>) DataManager.getScacCodeList(this, terminal_id);
		if (scacCodeList.size() > 0) {
			((TextView) findViewById(R.id.scac_code_header)).setVisibility(View.VISIBLE);
			scacCodeTextView.setVisibility(View.VISIBLE);
		}
		else {
			((TextView) findViewById(R.id.scac_code_header)).setVisibility(View.GONE);
			scacCodeTextView.setVisibility(View.GONE);
		}

    	if (terminalTextView.getText().toString().equalsIgnoreCase(String.valueOf(terminal_id))) {
            return;
        }
        terminalTextView.setText(String.valueOf(terminal_id));
        lotCodeTextView.setText("");
		PreferenceManager
				.getDefaultSharedPreferences(getApplicationContext())
				.edit()
				.putString("inspection_default_lot", "")
				.commit();

        scacCodeTextView.setText("");
    }
    
	@SuppressLint("NewApi")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent incomingIntent)
	{
		super.onActivityResult(requestCode, resultCode, incomingIntent);

		//BFF limiting to 10 images per vin is arbitrary
		cameraButton.setClickable(true);
		cameraButton.setAlpha(1.0f);

		try {
            switch (requestCode) {
            case REQ_CODE_CAPTURE_IMAGE:
                if ( resultCode == RESULT_OK ) {

                    CommonUtility.showText("Picture was taken for " + vinTextView.getText().toString());

					String opString = "";
					if (operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) {
						opString = "- Preload -";
					}
					else {
						opString = "- Delivery -";
					}



					Bitmap thumbnail = ImageHandler.processThumbnailImage(opString, thisInspection.vin, mCurrentPhotoFileName, true);
					Bitmap bitmap = ImageHandler.processImage(opString, thisInspection.vin, mCurrentPhotoFileName );

					Image newImage = this.camera.imageTakenSuccessful(true);
					newImage.filename = mCurrentPhotoFileName;

                    addBitmapToList(thumbnail, true, newImage);

                    this.camera.getNextRequiredImage(getCurrentFocus());
                } else {
                    CommonUtility.showText(" Picture was not taken ");
                }

                System.out.println("Image capture Result Code :: "+resultCode);
                break;

            case REQ_CODE_NOTES:
                if (resultCode == RESULT_OK && incomingIntent != null) {
                    thisInspection.notes = incomingIntent.getStringExtra("notes");
                    DataManager.insertInspection(this, thisInspection);
                    thisInspection = getInspection(bundle.getString("vin_number"));
                }
                break;

            case REQ_CODE_REJECTION_VIN:
                if (resultCode == RESULT_OK && incomingIntent != null) {
                    reasonString = incomingIntent.getStringExtra("reason");
                    if (!reasonString.equalsIgnoreCase("")) {
                        saveButton.setVisibility(View.VISIBLE);
                    }
                }
                break;

            case REQ_CODE_AREA:
                if (resultCode == RESULT_CANCELED) {
                    break;
                }
                if (three_text_view_lay.getChildCount() != 0) {
                    if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("edit")) {
                        log.info(Logs.DEBUG, "in edit mode:pos" + position);
                        View v = three_text_view_lay.getChildAt(position);
                        TextView one = (TextView) v.findViewById(R.id.one);
                        TextView two = (TextView) v.findViewById(R.id.two);
                        Button delete = (Button) v.findViewById(R.id.deleteButton);

                        one.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                        one.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
                        System.out.println("REQ_CODE_AREA : Tag "+incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                        delete.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);

                        if (position == areaTypeSvrties.size()) {
                            areaTypeSvrties.add(new AreaTypeSvrty());
                        }
                        AreaTypeSvrty areaTypeSvrty = areaTypeSvrties.get(position);
                        areaTypeSvrty.setAreaString(incomingIntent.getStringExtra("id").split(",")[0]);

                        log.debug(Logs.DEBUG, "OnActivityResult returned id='" + incomingIntent.getStringExtra("id") + "'");
                    } else if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("add")) {
						int i = three_text_view_lay.getChildCount() - 1;
						View v = three_text_view_lay.getChildAt(i);
						TextView one = (TextView) v.findViewById(R.id.one);
						TextView two = (TextView) v.findViewById(R.id.two);
						TextView three = (TextView) v.findViewById(R.id.three);
						Button delete = (Button) v.findViewById(R.id.deleteButton);

						if(atsIsBlank(one, two, three)) {
							one.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
							one.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
						} else {
							inflateLayout(three_text_view_lay);
							System.out.println("three_text_view_lay.getChildAt(i)   => " + three_text_view_lay.getChildCount());
							i = three_text_view_lay.getChildCount() - 1;
							v = three_text_view_lay.getChildAt(i);

							one = (TextView) v.findViewById(R.id.one);
							two = (TextView) v.findViewById(R.id.two);
							three = (TextView) v.findViewById(R.id.three);
							delete = (Button) v.findViewById(R.id.deleteButton);

							one.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
							one.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
							two.setTag("0" + "," + i);
							two.setText(ATS_NO_VAL);
							three.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
							three.setText(ATS_NO_VAL);
							System.out.println("REQ_CODE_AREA :: Tag "+"0" + "," + i);
							delete.setTag("0" + "," + i);

						}
						String code = incomingIntent.getStringExtra("id").split(",")[0];
						String id = incomingIntent.getStringExtra("id").split(",")[1];

						AreaTypeSvrty areaTypeSvrty = new AreaTypeSvrty(code, ATS_NO_VAL, ATS_NO_VAL, Integer.parseInt(id), -1, -1, false);
						areaTypeSvrties.add(areaTypeSvrty);
						startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
								.putExtra("mode", "edit")).putExtra("text", two.getText().toString()), REQ_CODE_TYPE);
                    }
                }
                break;

            case REQ_CODE_TYPE:
                if (resultCode == RESULT_CANCELED) {
                    break;
                }
                if (three_text_view_lay.getChildCount() != 0) {
                    if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("edit")) {
                        View v = three_text_view_lay.getChildAt(position);
                        TextView two = (TextView) v.findViewById(R.id.two);
                        TextView three = (TextView) v.findViewById(R.id.three);
                        Button delete = (Button) v.findViewById(R.id.deleteButton);

                        two.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                        two.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));

                        System.out.println("REQ_CODE_TYPE : TAG "+incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                        delete.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                        if (position == areaTypeSvrties.size()) {
                           log.debug(Logs.DEBUG, "Adding ATS entry");
                           areaTypeSvrties.add(new AreaTypeSvrty());
                        }
                        areaTypeSvrties.get(position).setTypeString(incomingIntent.getStringExtra("id").split(",")[0]);
                        log.debug(Logs.DEBUG, "OnActivityResult returned id='" + incomingIntent.getStringExtra("id") + "'");
                        if (three.getText().equals(ATS_NO_VAL)) {
                            startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
                                    .putExtra("mode", "edit")).putExtra("text", three.getText().toString()), REQ_CODE_SVRTY);
                        }
                    } else if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("add")) {
                        int i = three_text_view_lay.getChildCount() - 1;
                        View v = three_text_view_lay.getChildAt(i);
                        TextView one = (TextView) v.findViewById(R.id.one);
                        TextView two = (TextView) v.findViewById(R.id.two);
                        TextView three = (TextView) v.findViewById(R.id.three);
                        Button delete = (Button) v.findViewById(R.id.deleteButton);

                        if(atsIsBlank(one, two, three)) {
                            two.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                            two.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
                        } else {
                            inflateLayout(three_text_view_lay);
                            System.out.println("three_text_view_lay.getChildAt(i)   => " + three_text_view_lay.getChildCount());
                            i = three_text_view_lay.getChildCount() - 1;
                            v = three_text_view_lay.getChildAt(i);

                            one = (TextView) v.findViewById(R.id.one);
                            two = (TextView) v.findViewById(R.id.two);
                            three = (TextView) v.findViewById(R.id.three);
                            delete = (Button) v.findViewById(R.id.deleteButton);

                            one.setTag("0" + "," + i);
                            one.setText(ATS_NO_VAL);
                            two.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                            two.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
                            three.setTag("0" + "," + i);
                            three.setText(ATS_NO_VAL);
                            System.out.println("REQ_CODE_TYPE :: TAG "+"0" + "," + i);
                            delete.setTag("0" + "," + i);

                        }

                        String code = incomingIntent.getStringExtra("id").split(",")[0];
                        String id = incomingIntent.getStringExtra("id").split(",")[1];

                        areaTypeSvrties.add(new AreaTypeSvrty(ATS_NO_VAL, code, ATS_NO_VAL, -1, Integer.parseInt(id), -1, false));
						camera.invalidateQueue();
                    }
                }
                break;

            case REQ_CODE_SVRTY:
                if (resultCode == RESULT_CANCELED) {
                    break;
                }
                if (three_text_view_lay.getChildCount() != 0) {
                    if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("edit")) {
                        View v = three_text_view_lay.getChildAt(position);
                        TextView three = (TextView) v.findViewById(R.id.three);
                        TextView one = (TextView) v.findViewById(R.id.one);
                        Button delete = (Button) v.findViewById(R.id.deleteButton);

                        three.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                        three.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0], false));

                        System.out.println("REQ_CODE_SVRTY : TAG "+incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                        delete.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                        if (position == areaTypeSvrties.size()) {
                            log.debug(Logs.DEBUG, "Adding ATS entry");
                            areaTypeSvrties.add(new AreaTypeSvrty());
                        }
                        areaTypeSvrties.get(position).setSvrtyString(incomingIntent.getStringExtra("id").split(",")[0]);
                        log.debug(Logs.DEBUG, "OnActivityResult returned id='" + incomingIntent.getStringExtra("id") + "'");
                        if (one.getText().equals(ATS_NO_VAL)) {
                            startActivityForResult((new Intent(VehicleInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
                                    .putExtra("mode", "edit")).putExtra("text", one.getText().toString()), REQ_CODE_AREA);
                        }
                        log.debug(Logs.DEBUG, "OnActivityResult returned id='" + incomingIntent.getStringExtra("id") + "'");
                        this.camera.addDmgImages(areaTypeSvrties.get(position));
                        this.camera.getNextRequiredImage(getCurrentFocus());

                    } else if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("add")) {
                        int i = three_text_view_lay.getChildCount() - 1;
                        View v = three_text_view_lay.getChildAt(i);
                        TextView one = (TextView) v.findViewById(R.id.one);
                        TextView two = (TextView) v.findViewById(R.id.two);
                        TextView three = (TextView) v.findViewById(R.id.three);
                        Button delete = (Button)v.findViewById(R.id.deleteButton);

                        if(atsIsBlank(one, two, three)) {
                            three.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                            three.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0], false));
                        } else {
                            inflateLayout(three_text_view_lay);
                            System.out.println("three_text_view_lay.getChildAt(i)   => " + three_text_view_lay.getChildCount());
                            i = three_text_view_lay.getChildCount() - 1;
                            v = three_text_view_lay.getChildAt(i);

                            one = (TextView) v.findViewById(R.id.one);
                            two = (TextView) v.findViewById(R.id.two);
                            three = (TextView) v.findViewById(R.id.three);
                            delete = (Button) v.findViewById(R.id.deleteButton);

                            one.setTag("0" + "," + i);
                            one.setText(ATS_NO_VAL);
                            two.setTag("0" + "," + i);
                            two.setText(ATS_NO_VAL);
                            three.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                            three.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0], false));
                            System.out.println("REQ_CODE_SVRTY :: TAG "+"0" + "," + i);
                            delete.setTag("0" + "," + i);
                        }
                        String code = incomingIntent.getStringExtra("id").split(",")[0];
                        String id = incomingIntent.getStringExtra("id").split(",")[1];

                        AreaTypeSvrty areaTypeSvrty = new AreaTypeSvrty(ATS_NO_VAL, code, ATS_NO_VAL, -1, -1,  Integer.parseInt(id), false);
                        areaTypeSvrties.add(areaTypeSvrty);

                        camera.invalidateQueue();
                    }
                }
                break;

            case REQ_CODE_SPECIAL:
                if (resultCode == RESULT_CANCELED) {
                    break;
                }
                System.out.println("REQ_CODE_SPECIAL me aa gya");
                System.out.println("$$$ ID :: "+incomingIntent.getStringExtra("id"));

                //Special codes have a special code id that we can use to manually get the code values from
                SpecialCode specialCode = DataManager.getSpecialCode(this, Integer.parseInt(incomingIntent.getStringExtra("id").split(",")[1]));

                 if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("add")) {
                     inflateLayout(three_text_view_lay);
                     int i = three_text_view_lay.getChildCount() - 1;
                     View v = three_text_view_lay.getChildAt(i);

                     TextView one = (TextView) v.findViewById(R.id.one);
                     TextView two = (TextView) v.findViewById(R.id.two);
                     TextView three = (TextView) v.findViewById(R.id.three);
                     Button delete = (Button) v.findViewById(R.id.deleteButton);

                     System.out.println("three_text_view_lay.getChildAt(i)   => " + three_text_view_lay.getChildCount());
                     i = three_text_view_lay.getChildCount() - 1;
                     v = three_text_view_lay.getChildAt(i);
                     one = (TextView) v.findViewById(R.id.one);
                     two = (TextView) v.findViewById(R.id.two);
                     three = (TextView) v.findViewById(R.id.three);
                     delete = (Button) v.findViewById(R.id.deleteButton);

                     one.setTag(specialCode.getAreaCode() + "," + i + ",special");
                     one.setText(formatATSValue(specialCode.getAreaCode()));
                     two.setTag(specialCode.getTypeCode() + "," + i + ",special");
                     two.setText(formatATSValue(specialCode.getTypeCode()));
                     three.setTag(specialCode.getSeverityCode() + "," + i + ",special");
                     three.setText(formatATSValue(specialCode.getSeverityCode(), false));

                     System.out.println("REQ_CODE_SPECIAL :: TAG "+ specialCode.special_code_id + "," + i + ",special");
                     delete.setTag(specialCode.special_code_id + "," + i + ",special");

                     AreaTypeSvrty ats = new AreaTypeSvrty();

                     //We are adding a special so there aren't any ids in there....
					 ats.setSpecialId(specialCode.special_code_id);
					 ats.setAreaString(specialCode.getAreaCode());
                     ats.setTypeString(specialCode.getTypeCode());
                     ats.setSvrtyString(specialCode.getSeverityCode());
                     ats.setSpecial(true);

                     areaTypeSvrties.add(ats);
                     this.camera.addDmgImages(ats);
                     this.camera.getNextRequiredImage(getCurrentFocus());
                }
                break;


           case REQ_CODE_TERMINAL_NUM:
           		log.debug(Logs.DEBUG, "new terminal code=" + incomingIntent.getStringExtra("code"));

           		try {
					String terminalNum = incomingIntent.getStringExtra("code");

					PreferenceManager
							.getDefaultSharedPreferences(getApplicationContext())
							.edit()
							.putString("inspection_default_terminal", terminalNum)
							.commit();

					refreshTerminalDependentFields(Integer.parseInt(terminalNum));
				} catch (NumberFormatException ex) {
           			// Shouldn't happen since terminal number is checked in the activity.
				}

                break;
            
           case REQ_CODE_LOT_NUM:
                if (resultCode == RESULT_CANCELED) {
                    break;
                }
			   PreferenceManager
					   .getDefaultSharedPreferences(getApplicationContext())
					   .edit()
					   .putString("inspection_default_lot", incomingIntent.getStringExtra("code"))
					   .commit();

                lotCodeTextView.setText(incomingIntent.getStringExtra("code"));
                break;

            case REQ_CODE_SCAC_CODE:
                if (resultCode == RESULT_CANCELED) {
                    break;
                }
                scacCodeTextView.setText(incomingIntent.getStringExtra("code"));
                break;

            default:
                break;

            }
		} catch(Exception e) {
		    //TODO: this is absolutely horrible.  no really, catch EXCEPTION after this block of code?!?!
			e.printStackTrace();
		}
	}

	private class cameraController {
		private int operation;
		private Image imageBeingTaken;
		//private List<AreaTypeSvrty> areaTypeSvrties = new NoNullsArrayList<AreaTypeSvrty>();
		private List<Image> requiredImageQueue = new NoNullsArrayList<Image>();
		//private List<Image> activeImages = new NoNullsArrayList<Image>();

		public cameraController(int operation) {
			this.operation = operation;

			this.invalidateQueue();
		}

		private Image imageTakenSuccessful(boolean success) {
			Image newImage;

			if (imageBeingTaken == null) {
				newImage = new Image();
			} else if (success) {
				//activeImages.add(imageBeingTaken);
				newImage = imageBeingTaken;
			} else {
				this.requiredImageQueue.add(0, imageBeingTaken);
				newImage = imageBeingTaken;
			}

			imageBeingTaken = null;
			return newImage == null ? new Image() : newImage;
		}

		private final String generatePrompt(Image image) {
			final String areaPrefix = "Take a picture of the whole side of the vehicle which includes ";
			final String damagePrefix = "Take a close-up picture of the damage:\n";

			if (image.foreignKeyLabel.equals(Constants.IMAGE_AREA)) {
				String area = (DataManager.getAreaCodeById(currentActivity, image.foreignKey)).getDescription();
				return areaPrefix + area;
			} else if (image.foreignKeyLabel.equals(Constants.IMAGE_DAMAGE)) {
				for (VehicleInspectionActivity.AreaTypeSvrty d : areaTypeSvrties) {
					if (d.damage_id == image.foreignKey) {

						if (d.isSpecial()) {
							return damagePrefix + (DataManager.getSpecialCode(currentActivity, d.getSpecial_code_id()).getDescription());
						} else {
							AreaCode area = (DataManager.getAreaCode(currentActivity, d.getAreaString()));
							TypeCode type = (DataManager.getTypeCode(currentActivity, d.getTypeString()));
							if ((area == null || area.getDescription() == null || area.getDescription().length() == 0) && (type == null || type.getDescription() == null || type.getDescription().length() == 0)) {
								return damagePrefix + "unknown";
							} else if (area == null || area.getDescription() == null || area.getDescription().length() == 0) {
								return String.format("%s%s area", damagePrefix, type.getDescription());
							} else if (type == null || type.getDescription() == null || type.getDescription().length() == 0) {
								return String.format("%s%s", damagePrefix, area.getDescription());
							} else {
								return String.format("%s%s/%s", damagePrefix, area.getDescription(), type.getDescription());
							}
						}
					}

				}
			} else if (image.foreignKeyLabel.startsWith(Constants.IMAGE_EXTERIOR)) {
				String headlightsOff = "\n\nTake picture with headlights OFF.";
				String specialFormat = "Take a picture of the entire %s of the vehicle." + headlightsOff;
				if (image.foreignKeyLabel.equals(Constants.IMAGE_EXTERIOR_FRONT)) {
					return String.format(specialFormat, "front");
				} else if (image.foreignKeyLabel.equals(Constants.IMAGE_EXTERIOR_DRIVER_SIDE)) {
					return String.format(specialFormat, "driver side");
				} else if (image.foreignKeyLabel.equals(Constants.IMAGE_EXTERIOR_REAR)) {
					return String.format(specialFormat, "rear");
				} else if (image.foreignKeyLabel.equals(Constants.IMAGE_EXTERIOR_PASSENGER_SIDE)) {
					return String.format(specialFormat, "passenger side");
				} else if (image.foreignKeyLabel.equals(Constants.IMAGE_EXTERIOR_TOP)) {
					return String.format(specialFormat, "top");
				} else if (image.foreignKeyLabel.equals(Constants.IMAGE_EXTERIOR_FULL_FRONT_CORNER)) {
					return "Take a picture of the front corner of the vehicle so that the grille, roof, and one side of the vehicle are all visible."
							+ headlightsOff;
				} else if (image.foreignKeyLabel.equals(Constants.IMAGE_EXTERIOR_FULL_REAR_CORNER)) {
					return "Take a picture of the opposite rear corner of the vehicle so that both tail-lights, roof, and different side of the vehicle are all visible."
							+ headlightsOff;
				}
			} else if (image.foreignKeyLabel.equals(Constants.IMAGE_VIN_PLATE)) {
				return getResources().getString(R.string.image_vin_plate_prompt);
			} else if (image.foreignKeyLabel.equals(Constants.IMAGE_PICK_SHEET)) {
				return getResources().getString(R.string.image_pick_sheet_prompt);
			}
			return "Vehicle image required.";
		}

		public void getImage(View v) {
			invalidateQueue();

			if (requiredImageQueue.size() > 0) {
				this.getNextRequiredImage(v);
			} else {
				this.cameraClick(v);
			}
		}

		public int getTotalImagesRequired() {
			this.invalidateQueue();
			return this.requiredImageQueue.size();
		}

		private void addExtraImage(String label) {
			log.debug(Logs.DEBUG, "Adding required " + label + " image");

			log.debug(Logs.DEBUG, "Checking for duplicate required images");
			for (Image i : requiredImageQueue) {
				log.debug(Logs.DEBUG, String.format("FK Label - FK: %s - %d", i.foreignKeyLabel, i.foreignKey));
				if (i.foreignKeyLabel != null && i.foreignKeyLabel.equals(label)) {
					log.debug(Logs.DEBUG, "found duplicate vin plate required image, not adding");
					return;
				}
			}
			log.debug(Logs.DEBUG, "Checking for images already captured");
			for (Image i : activeImages) {
				log.debug(Logs.DEBUG, String.format("captured image: FK Label - FK: %s - %d", i.foreignKeyLabel, i.foreignKey));
				if (i.foreignKeyLabel != null && i.foreignKeyLabel.equals(label)) {
					log.debug(Logs.DEBUG, "required " + label + " Image already captured.  Not adding...");
					return;
				}
			}

			Image image = new Image();
			//Hmmm... there is not an actual image being captured?
			image.foreignKey = 1;
			image.foreignKeyLabel = label;
			this.requiredImageQueue.add(image);
			log.debug(Logs.DEBUG, "Required " + label + " image added.");
		}

		private void addAreaImage(VehicleInspectionActivity.AreaTypeSvrty dmg) {
			int id = dmg.getArea_code_id();
			log.debug(Logs.DEBUG, "Adding area image with ID " + id);

			if (dmg.isSpecial()) {
				return;
			}

			log.debug(Logs.DEBUG, "Checking for duplicate Area images");
			for (Image i : requiredImageQueue) {
				log.debug(Logs.DEBUG, String.format("required image: FK Label - FK: %s - %d", i.foreignKeyLabel, i.foreignKey));
				if (i.foreignKeyLabel != null && i.foreignKeyLabel.equals(Constants.IMAGE_AREA) && i.foreignKey == id) {
					log.debug(Logs.DEBUG, "Found duplicate required area image, not adding");
					return;
				}
			}
			log.debug(Logs.DEBUG, "Checking for area images already captured");
			for (Image i : activeImages) {
				log.debug(Logs.DEBUG, String.format("Captured image: FK Label - FK: %s - %d", i.foreignKeyLabel, i.foreignKey));
				if (i.foreignKeyLabel != null && i.foreignKeyLabel.equals(Constants.IMAGE_AREA) && i.foreignKey == id) {
					log.debug(Logs.DEBUG, "Found captured area image, not adding");
					return;
				}
			}

			Image image = new Image();
			image.foreignKeyLabel = Constants.IMAGE_AREA;
			image.foreignKey = id;
			this.requiredImageQueue.add(0, image);
			log.debug(Logs.DEBUG, "Required Area Image added.");
		}

		public void addDmgImages(VehicleInspectionActivity.AreaTypeSvrty dmg) {

			if (dmg.isReadOnly() && !dmg.isExternal()) {
				return;
			}

			boolean snowCovered = false;
			if (dmg.isSpecial()) {
				SpecialCode specialCode = DataManager.getSpecialCode(currentActivity, dmg.getSpecial_code_id());

				if (specialCode.getDescription().toLowerCase().contains("dirt") || specialCode.getDescription().toLowerCase().contains("snow")) {
					this.addExtraImages(Constants.IMAGE_KEYS_EXTERIOR_CORNER_ONLY);
					snowCovered = true;
				}
			}

			//Check for vinplate and area AFTER adding the damage image
			//
			if (operation == Constants.STANDALONE_INSPECTION_OPERATION) {
				if (!snowCovered) {
					this.addExtraImage(Constants.IMAGE_VIN_PLATE);
					this.addAreaImage(dmg);
				}
			}


			log.debug(Logs.DEBUG, "Adding required damage image with ID " + dmg.damage_id);

			if (dmg.isIncomplete()) {
				return;
			}

			log.debug(Logs.DEBUG, "Checking for duplicate damage images");
			for (Image i : requiredImageQueue) {
				log.debug(Logs.DEBUG, String.format("required image: FK Label - FK: %s - %d", i.foreignKeyLabel, i.foreignKey));
				if (i.foreignKeyLabel.equals(Constants.IMAGE_DAMAGE) && i.foreignKey == dmg.damage_id) {
					log.debug(Logs.DEBUG, "Found duplicate damage image, not adding...");
					return;
				}
			}
			log.debug(Logs.DEBUG, "Checking for damage images already captured");
			for (Image i : activeImages) {
				log.debug(Logs.DEBUG, String.format("captured image: FK Label - FK: %s - %d", i.foreignKeyLabel, i.foreignKey));
				if (i.foreignKeyLabel != null && i.foreignKeyLabel.equals(Constants.IMAGE_DAMAGE) && i.foreignKey == dmg.damage_id) {
					log.debug(Logs.DEBUG, "found captured damage image, not adding...");
					return;
				}
			}

			if (!snowCovered) {
				Image image = new Image();
				image.foreignKeyLabel = Constants.IMAGE_DAMAGE;
				image.foreignKey = dmg.damage_id;
				this.requiredImageQueue.add(0, image);
				log.debug(Logs.DEBUG, "Required Damage image added since it was not found: " + image.foreignKeyLabel + " - " + image.foreignKey);
			}
		}

		public void removeDmgImages(VehicleInspectionActivity.AreaTypeSvrty dmg) {
			areaTypeSvrties.remove(dmg);

			invalidateQueue();
		}

		public void invalidateQueue() {
			requiredImageQueue.clear();

			int externalImageCount = 0;
			for (VehicleInspectionActivity.AreaTypeSvrty dmg : areaTypeSvrties) {
				if(dmg.isExternal()) {
					externalImageCount++;
				}
			}

			if(externalImageCount >= 5) {
				addExtraImages(Constants.IMAGE_KEYS_EXTERIOR_FULL_SET);
			}

			for (VehicleInspectionActivity.AreaTypeSvrty dmg : areaTypeSvrties) {
				//We are only capturing images for external damages separately if there are less than five
				if(!dmg.isExternal() || externalImageCount < 5) {
					addDmgImages(dmg);
				}
			}
		}

		private void addExtraImages(final List<String> extraList) {
			for (int i = 0; i < extraList.size(); i++) {
				addExtraImage(extraList.get(i));
			}
		}

		// returns true when there is another required image to be taken
		// returns false if there are no more required images
		public Boolean getNextRequiredImage(View v) {
			if (this.requiredImageQueue.size() == 0) {
				return false;
			}

			final View focus = v;
			final VehicleInspectionActivity.cameraController currentCamera = this;
			final Image next = this.requiredImageQueue.remove(0); // pop first element off required images

			Builder builder = new AlertDialog.Builder(VehicleInspectionActivity.this);
			builder.setTitle("Image Required");
			builder.setMessage(generatePrompt(next));
			builder.setPositiveButton("Camera", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					imageBeingTaken = next;
					currentCamera.cameraClick(focus);
				}
			});
			builder.setNegativeButton("Postpone", new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int which) {
					requiredImageQueue.add(0, next);
				}
			});
			builder.setCancelable(false);
			builder.create().show();
			return true;
		}

		//Move to the Image handler class
		public void cameraClick(View v) {
			if (locationConnected) {
				lastLocation = mLocation;
			} else {
				if (DEBUG) {
					CommonUtility.showText("Could not determine location for images, ignoring");
				}
			}

			if (lastLocation != null) {
				if (DEBUG) log.debug(Logs.DEBUG, "Location for image is: " + lastLocation.toString());
			}

			Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
			if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

				try {
					//Add the current path of the full size image
					mCurrentPhotoFileName = mImageFileNamePrefix + UUID.randomUUID().toString();
					File photoFile = new File(CommonUtility.cachedImageFileFullPath(getApplicationContext(), mCurrentPhotoFileName + "_hires"));
					photoFile.createNewFile();

					if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
						takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, Uri.fromFile(photoFile));
					} else {
						Uri photoUri = FileProvider.getUriForFile(getApplicationContext(), getApplicationContext().getPackageName() + ".provider", photoFile);
						takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
					}
					takePictureIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

					if (takePictureIntent.resolveActivity(getApplicationContext().getPackageManager()) != null) {
						startActivityForResult(takePictureIntent, REQ_CODE_CAPTURE_IMAGE);
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	private void addBitmapToList(Bitmap bitmap, Image image) {
	  addBitmapToList(bitmap, false, image);
	}

	private void addBitmapToList(Bitmap bitmap, boolean getLocation , Image image) {

		//Todo: 4412919265-photo-post-processing - Move - create a separate function that does the location stuff and a different function that does the image view stuff
		//Todo: The onclick listener should have it's own function
		//Todo: the watermark should have it's own function
		//Todo:
		ImageView imageView = new ImageView(this);
		//setting image resource
		imageView.setImageBitmap(bitmap);
		//setting image position
		imageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
		imageView.setPadding(10, 10, 10, 10);

		if(getLocation) {
	          Location mCurrentLocation = null;

	          if(locationConnected) {
	        	  mCurrentLocation = mLocation;
	          }
	          
	          if(mCurrentLocation != null) {
	          	if(DEBUG) log.debug(Logs.DEBUG, "Location for signature is: " + mCurrentLocation.toString());
	          }

	          if(locationConnected) {
	              image.imageLat = String.valueOf(mCurrentLocation.getLatitude());
	              image.imageLon = String.valueOf(mCurrentLocation.getLongitude());
	          } else if (lastLocation != null) {
	            image.imageLat = String.valueOf(lastLocation.getLatitude());
	            image.imageLon = String.valueOf(lastLocation.getLongitude());
	          }
	          else {
	              if(DEBUG) { CommonUtility.showText("Could not determine location for captured image, ignoring"); }
	          }
		}
		
		//Add the current path of the full size image
		image.filename = mCurrentPhotoFileName;
		//Log.d("narf", image.filename);

		//Todo: 4412919265-photo-post-processing - retain
		activeImages.add(image);

		imageView.setTag(image);

		//Todo: 4412919265-photo-post-processing - retain
		cameraImageLayout.addView(imageView, 0);

		if (!isEditable) {
		    return;
		}
		imageView.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				// Prevent rapid clicks from opening multiple windows
				if (CommonUtility.doubleClickDetected()) {
					return;
				}

				final View view = v;

				//Todo: 4412919265-photo-post-processing - move to the image handler class and retain some of the code in a delete function in the activity where the rest of the specific work is done.
				final ImageViewDialog imageViewDialog = new ImageViewDialog(VehicleInspectionActivity.this,
						"vin #" + vinTextView.getText().toString()) {
					@Override
					public void DeleteImage() {
						final ImageViewDialog imageViewDialog = this;
						super.DeleteImage();
						Builder builder = new AlertDialog.Builder(VehicleInspectionActivity.this);
						builder.setTitle("Delete ?");
						builder.setMessage("Would you like to delete this image?");
						DialogInterface.OnClickListener dialogListener = new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								switch (which) {
									case -1:
										Image image = (Image) view.getTag();

										if (image == null) {
											log.debug(Logs.DEBUG, "The image tag of the view being deleted was null, resulting in the image not being removed from active images");
											return;
										}

//										if (image.image_id != -1) {
//											garbageImages.add(image);
//										}
										else if (image.filename != null) {
											CommonUtility.deleteCachedImageFile(getApplicationContext(), image.filename + "_hires");
											CommonUtility.deleteCachedImageFile(getApplicationContext(), image.filename);
										}

										log.debug(Logs.DEBUG, "activeimages size: " + activeImages.size());

										//Todo: 4412919265-photo-post-processing - retain
										activeImages.remove(image);
										log.debug(Logs.DEBUG, "activeimages post-remove size: " + activeImages.size());

										//Todo: 4412919265-photo-post-processing - retain
										cameraImageLayout.removeView(view);
										imageViewDialog.dismiss();
										break;
									case -2:
										dialog.dismiss();
										break;
									default:
										System.out.println("which value : " + which);
										break;
								}
							}
						};
						builder.setPositiveButton("Yes", dialogListener);
						builder.setNegativeButton("No", dialogListener);
						builder.create().show();
					}
				};

				Image image = (Image) view.getTag();

				//
				// The jpg with the full-resolution image created by the camera is cached on the device
				// until the load associated with the image is deleted.  If the file for the image still
				// exists, we use that in order to display the highest resolution; otherwise, we use
				// the lower-resolution data stored in the database.
				//
				// Note: Prior to version 1.129, the filenames stored in the image didn't match up to
				//       any temporary files on disk.


				//Todo: 4412919265-photo-post-processing - move to the image handler class
				//Todo: This all works together to go into the adding of the watermark
				//
				String filename = "";

				File bigFile = new File(CommonUtility.cachedImageFileFullPath(getApplicationContext(),image.filename));

				if(bigFile.exists()) {
					filename = bigFile.getAbsolutePath();
				}

				if (new File(filename).exists()) {
					String fileflag = "- Inspection -";

					Bitmap bigBitmap = BitmapFactory.decodeFile(filename);

					if (bigBitmap == null) {
						log.debug(Logs.DEBUG, "bigBitmap was null");
					} else {
						//If this is a honeywell device
						if(CommonUtility.hasHoneywellScanner()) {
							//We need to resolve the image rotation
							int rotation = CommonUtility.getImageRotation(getApplicationContext(), filename);
							//if(rotation > 90) {
							bigBitmap = CommonUtility.rotateImage(bigBitmap, rotation);
                            /*} else {
                                bigBitmap = CommonUtility.rotateImage(bigBitmap, 90);
                            }*/
						}
					}

					//Todo: 4412919265-photo-post-processing - move to the image handler class
					bigBitmap = HelperFuncs.addWatermarkOnBottom(bigBitmap,
							vinTextView.getText().toString(),
							fileflag, 8);

					imageViewDialog.SetBitmap(bigBitmap);
				}

				//Todo: 4412919265-photo-post-processing - Create image view dialog function that returns 
				imageViewDialog.deleteButtonEnabled(isEditable);
				imageViewDialog.show();
			}
		});
	}

	/**
	 * Author : Navjot Singh Bedi Creation Date : 05-Dec-2013 Description : @TODO
	 */
	class AreaTypeSvrty {
		private String areaString;
		private int area_code_id;
		private String typeString;
		private int type_code_id;
		private String svrtyString;
		private int severity_code_id;
		private int special_code_id;
		private int damage_id = -1;
		private boolean readonly;
		private boolean external;

		public boolean isReadOnly() {
			return readonly;
		}

		public boolean isExternal() { return external; }

		public void setExternal(boolean external) {this.external = external; }

		public void setReadyOnly(boolean _readonly) {
			readonly = _readonly;
		}


		public int getArea_code_id() {
			return area_code_id;
		}

		public void setArea_code_id(int area_code_id) {
			this.area_code_id = area_code_id;
		}

		public int getType_code_id() {
			return type_code_id;
		}

		public void setType_code_id(int type_code_id) {
			this.type_code_id = type_code_id;
		}

		public int getSeverity_code_id() {
			return severity_code_id;
		}

		public void setSeverity_code_id(int severity_code_id) {
			this.severity_code_id = severity_code_id;
		}

		public boolean isSpecial() {
			return isSpecial;
		}

		public void setSpecial(boolean isSpecial) {
			this.isSpecial = isSpecial;
		}

		private boolean isSpecial;

		public int getSpecial_code_id() {
			return special_code_id;
		}

		public void setSpecialId(int special_code_id) {
			this.special_code_id = special_code_id;
		}

		private boolean isZeroOrEmpty(String s) {
			return s == null || s.length() == 0 || s.equals("0");
		}

		public boolean isIncomplete() {
			return isZeroOrEmpty(this.areaString) || isZeroOrEmpty(this.typeString) || isZeroOrEmpty(this.svrtyString);
		}

		public AreaTypeSvrty() {
			this.damage_id = getNextDamageId();
			this.setAreaString("");
			this.setTypeString("");
			this.setSvrtyString("");

			this.isSpecial = false;

			this.readonly = false;
			this.external = false;
		}

		public AreaTypeSvrty(String areaString, String typeString, String svrtyString, int area_code_id, int type_code_id, int severity_code_id, boolean isSpecial) {
			this.damage_id = getNextDamageId();
			this.setAreaString(areaString);
			this.setTypeString(typeString);
			this.setSvrtyString(svrtyString);

			this.area_code_id = area_code_id;
			this.type_code_id = type_code_id;
			this.severity_code_id = severity_code_id;
			this.isSpecial = isSpecial;
		}

		public String getAreaString() {
			if (areaString == null || areaString.length() == 0) {
				return "0";
			} else {
				return areaString;
			}
		}

		public boolean setAreaString(String areaString) {

			boolean changed = true;
			if(areaString != null && this.areaString != null && areaString.equals(this.areaString))
				changed = false;

			this.areaString = areaString;

			return changed;
		}

		public String getTypeString() {
			if (typeString == null || typeString.length() == 0) {
				return "0";
			} else {
				return typeString;
			}
		}

		public boolean setTypeString(String typeString) {

			boolean changed = true;
			if(typeString != null && this.typeString != null && typeString.equals(this.typeString))
				changed = false;

			this.typeString = typeString;

			return changed;
		}

		public String getSvrtyString() {
			return svrtyString;
		}

		public boolean setSvrtyString(String svrtyString) {

			boolean changed = true;
			if(svrtyString != null && this.svrtyString != null && svrtyString.equals(this.svrtyString))
				changed = false;

			this.svrtyString = svrtyString;

			return changed;
		}

		public boolean isSevereDamage() {
			return (!this.getSvrtyString().equals("") && Integer.parseInt(this.getSvrtyString()) > 2 );
		}

	}


	private void displayInspectionData()
	{
        log.debug(Logs.DEBUG, "isEditable=" + isEditable);

        thisInspection.inspector = new String(employeeNumber);
        if (thisInspection.terminal != null) {
            refreshTerminalDependentFields(thisInspection.terminal.terminal_id);
        }
        terminalTextView.setEnabled(isEditable);
        
        if (thisInspection.lotCode != null) {
            lotCodeTextView.setText(String.valueOf(thisInspection.lotCode.code));
        }
        lotCodeTextView.setEnabled(isEditable);

        
	    // ScacCode
        if (thisInspection.scacCode != null) {
            scacCodeTextView.setText(thisInspection.scacCode.getDescription());
        }
        scacCodeTextView.setEnabled(isEditable);
        String inspectionTypeVal;
        
        if (thisInspection.type == Constants.INSPECTION_TYPE_GATE) {
            inspectionTypeDropdown.setText("Gate");
        }
        else {
            inspectionTypeDropdown.setText("Offsite");
        }

        //dropDownImage.setVisibility(isEditable ? View.VISIBLE : View.GONE);
		dropDownImage.setVisibility(View.GONE); // Inspection type is disabled for now


		//Set up image list
        for(Image image : thisInspection.images) {

			String newImageFilePath = CommonUtility.cachedImageFileFullPath(this, mCurrentPhotoFileName);
        	Bitmap bitmap = CommonUtility.getBitmapThumbnail(getApplicationContext(), newImageFilePath);

    		if(!image.preloadImage) {
    			addBitmapToList(bitmap, image);
    		}
        }

        //Set up damages list
        if (thisInspection.damages != null && thisInspection.damages.size() > 0)
        {
            System.out.println("# damage records: " + thisInspection.damages.size());

            int i = 0;
            for (Damage damage: thisInspection.damages)
            {

            	AreaTypeSvrty ats = new AreaTypeSvrty();

            	ats.damage_id = damage.damage_id;
            	if(damage.specialCode != null) {
            		ats.setSpecial(true);
            		ats.setAreaString(damage.specialCode.getAreaCode());
            		ats.setTypeString(damage.specialCode.getTypeCode());
            		ats.setSvrtyString(damage.specialCode.getSeverityCode());
            	} else {
            		ats.setAreaString(damage.areaCode.getCode());
            		ats.setTypeString(damage.typeCode.getCode());
            		ats.setSvrtyString(damage.severityCode.getCode());
            		ats.setSpecial(false);
            		ats.setArea_code_id(damage.areaCode.area_code_id);
            		ats.setType_code_id(damage.typeCode.type_code_id);
            		ats.setSeverity_code_id(damage.severityCode.severity_code_id);
            	}

            	areaTypeSvrties.add(ats);

                inflateLayout(three_text_view_lay);
                View v = three_text_view_lay.getChildAt(i);

                TextView one = (TextView) v.findViewById(R.id.one);
                TextView two = (TextView) v.findViewById(R.id.two);
                TextView three = (TextView) v.findViewById(R.id.three);
                Button delete = (Button) v.findViewById(R.id.deleteButton);

                //Don't forget to check for special codes which get the strings from a different place!
                one.setTag("0" + "," + i);
                one.setText(damage.specialCode != null ? formatATSValue(damage.specialCode.getAreaCode()) : formatATSValue(damage.areaCode.getCode()));
                two.setTag("0" + "," + i);
                two.setText(damage.specialCode != null ? formatATSValue(damage.specialCode.getTypeCode()) : formatATSValue(damage.typeCode.getCode()));
                three.setTag("0" + "," + i);
                three.setText(damage.specialCode != null ? formatATSValue(damage.specialCode.getSeverityCode(), false) : formatATSValue(damage.severityCode.getCode(), false));
                delete.setTag("0" + "," + i);
                //delete.setText(i+"");

                // disable if we're in review-only mode
                one.setEnabled(isEditable);
                two.setEnabled(isEditable);
                three.setEnabled(isEditable);
                delete.setVisibility(isEditable ? View.VISIBLE : View.INVISIBLE);

                i++;
            }
        }
        else
        {
            //inflateLayout(three_text_view_lay);
        }

        specialButton.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        cameraButton.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        saveButton.setVisibility(isEditable ? View.VISIBLE : View.GONE);
	}

	@Override
	protected void onResume() {
		super.onResume();

		// Connect to location services.
		locationConnected = false;
		imgScrollView.post(scrollImagesLeft);
		scrollView.post(scrollDown);
		startLocationUpdates();
	}
}
