package com.cassens.autotran.constants;

import android.annotation.SuppressLint;
import android.text.InputFilter;

import com.google.android.gms.location.DetectedActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TimeZone;

@SuppressLint("SimpleDateFormat")
public class Constants {

    // Android SDK references
    public static final InputFilter[] ALL_CAPS_INPUT_FILTER = new InputFilter[]
            {new InputFilter.AllCaps()};
    public static final InputFilter[] VIN_VALIDATION_FILTER = new InputFilter[]
            {new InputFilter.AllCaps(), new InputFilter.LengthFilter(17)};
    public static final int BASE64_ENCODE_FLAGS = android.util.Base64.NO_WRAP;

	//Sync statii
	public static final int SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD        = 1;
	public static final int SYNC_STATUS_UPLOADED_FOR_PRELOAD            = 2;
	public static final int SYNC_STATUS_UPLOADING_FOR_PRELOAD           = 3;
	public static final int SYNC_STATUS_UPLOAD_FAILED_FOR_PRELOAD       = 6;	
	
	public static final int SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY       = 7;
	public static final int SYNC_STATUS_UPLOADED_FOR_DELIVERY           = 8;
	public static final int SYNC_STATUS_UPLOADING_FOR_DELIVERY          = 9;
	public static final int SYNC_STATUS_UPLOAD_FAILED_FOR_DELIVERY      = 10;
    public static final int SYNC_STATUS_UPLOADING_PART_1_FOR_PRELOAD    = 4;
    public static final int SYNC_STATUS_UPLOADING_PART_2_FOR_PRELOAD    = 5;
    public static final int SYNC_STATUS_UPLOADING_PART_1_FOR_DELIVERY   = 11;
    public static final int SYNC_STATUS_UPLOADING_PART_2_FOR_DELIVERY   = 12;
    public static final int SYNC_STATUS_UPLOADED_PART_1_FOR_PRELOAD     = 13;
    public static final int SYNC_STATUS_UPLOADED_PART_1_FOR_DELIVERY    = 15;
	
    public static final int SYNC_STATUS_NOT_UPLOADED     = 9;
    public static final int SYNC_STATUS_UPLOADED         = 10;
    public static final int SYNC_STATUS_UPLOADING        = 11;
    public static final int SYNC_STATUS_UPLOAD_FAILED    = 12;
    public static final int SYNC_STATUS_UPLOADING_PART_1        = 13;
    public static final int SYNC_STATUS_UPLOADING_PART_2        = 14;
    public static final int SYNC_STATUS_UPLOADED_PART_1        = 15;
	public static final int SYNC_STATUS_NOT_READY_FOR_UPLOAD     = 16;
	public static final int SYNC_STATUS_MAX_RETRIES_EXCEEDED     = 17;
  
	
	//Remote statii
	public static final int DELIVERY_VIN_STATUS_NOT_LOADED = 0;
	public static final int DELIVERY_VIN_STATUS_LOADED = 1;
	public static final int DELIVERY_VIN_STATUS_DELIVERED = 3;
	public static final int DELIVERY_VIN_STATUS_REJECTED = 2;    
	
	//State Constants
	public static final int CAME_FROM_SETDEFAULTS = 1;
	public static final int CAME_FROM_CHECKSCANNER = 2;

	// Training requirement intent extras
    public static final String EXTRA_TRAINING_REQ_IDS = "training_requirement_ids";

	//State information saved in shared preferences
	public static final String CURRENT_OPERATION = "operation";
	public static final int PRELOAD_OPERATION = 1;
	public static final int DELIVERY_OPERATION = 2;
	public static final int INSPECT_VEHICLE_OPERATION = 3;
	public static final int SHUTTLE_LOAD_OPERATION = 4;
	public static final int STANDALONE_INSPECTION_OPERATION = 6;

    public static final String CURRENT_LOOKUP_ID = "lookup_id"; // Load or Delivery
	
	//Upload Constants
	public static final int UPLOAD_IMAGE = 1;
	public static final int UPLOAD_LOAD = 2;
	public static final int UPLOAD_DELIVERY = 3;
	public static final int UPLOAD_DELIVERY_VIN = 4;
	public static final int UPLOAD_DAMAGE = 5;
	
	   public static final int UPLOAD_INSPECTION = 6;
	   public static final int UPLOAD_PLANT_RETURN = 7;
	   public static final int UPLOAD_YARD_INVENTORY = 8;
	   public static final int UPLOAD_YARD_EXIT = 9;
	   public static final int UPLOAD_RECEIVED_VEHICLE = 10;
	public static final int UPLOAD_LOAD_EVENT = 11;
	
    public static final String SYNC_STATUS_UPDATED_DATA = "com.cassens.autotran.intents.sync.updated_data";
    public static final String SYNC_STATUS_SYNC_FAILED = "com.cassens.autotran.intents.sync.sync_failed";
    public static final String DEFAULT_TERMINAL_PREF = "com.cassens.autotran.sharedprefs.default_terminal";

    public static final String PICCOLO_DETECTED = "com.cassens.autotran.intents.hardware.piccolo_detected";

    public static final int INSPECTION_TYPE_GATE = 1;
    public static final int INSPECTION_TYPE_OFFSITE = 2;
    
    public static final short NUMBER_OF_IMAGE_CHUNKS = 30;
    
    public static final int NOTIFICATION_PRELOAD = 4343;
    public static final int NOTIFICATION_DELIVERY = 4344;
    public static final String DRIVER_ID_PREF = "cassens.driver.id";
	public static final String DRIVER_ID_PREV_LOGIN_PREF = "cassens.driver.idPrevLogin";
	public static final String DRIVER_ID_PREV_HELP_TERM_PREF = "cassens.driver.idPrevTerminal";
	public static final String DRIVER_HELP_TERM_PREF = "cassens.driver.helpterm";
	public static final String TRAILER_ID_PREF_PREFIX = "cassens.trailer.id";

	public static final String TRUCK_EVENT_LAST_GPS_MILLIS_PREF = "cassens.gps.lastGpsMillis";

	public static final int PHOTOS_DELIVERY_DMG = 2;
    public static final int PHOTOS_PRELOAD_DMG = 1;
    public static final String SSID_PREF = "com." +
			"cassens.autotran.sharedprefs.currentSSID";


	public static SimpleDateFormat dateFormatter() {
		SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		df.setTimeZone(TimeZone.getTimeZone("UTC"));
		return df;
	}
	
    public static final String UPDATE_NEEDED = "com.cassens.autotran.UPDATE_NEEDED";
	public static final String PREF_SHUTTLE_LOAD_DEFAULTS = "cassens.shuttle_load.shuttle_load_defaults";
	public static final String PREF_VEHICLE_COUNT = "cassens.shuttle_load.vehicle_count";
	public static final String PREF_PICCOLO_UNDOCKED_TIME = "cassens.piccolo.undocked_time";
	public static final String DRIVER_ACTION_DISPLAY_MESSAGE = "DISPLAY_MESSAGE";
	public static final String DRIVER_ACTION_UPLOAD_LOGS = "UPLOAD_LOGS";
	public static final String DRIVER_ACTION_UPLOAD_IMAGE= "UPLOAD_IMAGE";
	public static final String DRIVER_ACTION_UPLOAD_HIRES_IMAGE = "UPLOAD_HIRES_IMAGE";
	public static final String DRIVER_ACTION_UPDATE_SHUTTLE_LOAD_NUMBER= "UPDATE_SHUTTLE_LOAD_NUMBER";
	public static final String DRIVER_ACTION_SYNC_DRIVER = "SYNC_DRIVER";
	public static final String DRIVER_ACTION_HIDE_LOAD = "HIDE_LOAD";
	public static final String DRIVER_ACTION_UNHIDE_LOAD = "UNHIDE_LOAD";
	public static final String DRIVER_ACTION_LOAD_COMMAND = "LOAD_COMMAND";
	public static final String DRIVER_ACTION_CHANGE_PREFERENCE = "CHANGE_PREFERENCE";

	public static final String DIALOG_ACTIVITY_MESSAGE = "DIALOG_ACTIVITY_MESSAGE";
	public static final String DIALOG_ACTIVITY_TITLE = "DIALOG_ACTIVITY_TITLE";
	public static final String DIALOG_ACTIVITY_SHOW_CANCEL = "DIALOG_ACTIVITY_SHOW_CANCEL";
	public static final String DIALOG_ACTIVITY_DRIVER_ACTION_IDS = "DIALOG_ACTIVITY_DRIVER_ACTION_IDS";
	public static final String DIALOG_ACTIVITY_DRIVER_OLD_MESSAGES = "DIALOG_ACTIVITY_DRIVER_OLD_MESSAGES";
	public static final String PREF_DRIVING_LOCK_OVERRIDDEN = "driving_lock_overridden";

	public static final int LARGE_FILE_SIZE = 800000;

	public static enum TASK_STATUS {
		PRE_EXECUTE,
		EXECUTION,
		PROGRESS_UPDATE,
		POST_EXECUTE
	}

	public static ArrayList<String> safeDeliveryMfgs = new ArrayList<>(Arrays.asList("TO"));

	// load types
	public static final String LOAD_NORMAL = "R";
	public static final String LOAD_SHUTTLE = "SR";
	public static final String LOAD_PICKUP = "PX";

	public static final int MAX_VEHICLES_ON_TRUCK = 13;

	final public static String DELIVERY_VIN_CHANGED = "DELIVERY_VIN_CHANGED";

	final public static String RESTRICTED_DISPATCH = "RESTRICTED_DISPATCH";
	
	final public static int POSITION_MIN = 1;
	final public static int POSITION_MAX = 13;
    final public static ArrayList<Integer> POSITION_BLACK_LIST = new ArrayList<Integer>(){
		private static final long serialVersionUID = 1L;
		{ add(12); add(14); }};
		
	public static final String IMAGE_DAMAGE = "damage";
	public static final String IMAGE_AREA = "area";
	public static final String IMAGE_VIN_PLATE = "vin_plate";
	public static final String IMAGE_PICK_SHEET = "image.pick_sheet";
	public static final String IMAGE_ODOMETER = "odometer";
	public static final String IMAGE_EXTERIOR = "exterior.";

	public static final String IMAGE_EXTERIOR_FULL_FRONT_CORNER = IMAGE_EXTERIOR +  "FULL_FRONT_CORNER";
	public static final String IMAGE_EXTERIOR_FULL_REAR_CORNER = IMAGE_EXTERIOR +  "FULL_REAR_CORNER";

	public static final String IMAGE_EXTERIOR_FRONT = IMAGE_EXTERIOR + "FRONT";
	public static final String IMAGE_EXTERIOR_DRIVER_SIDE = IMAGE_EXTERIOR + "DRIVER_SIDE";
	public static final String IMAGE_EXTERIOR_REAR = IMAGE_EXTERIOR +  "REAR";
	public static final String IMAGE_EXTERIOR_PASSENGER_SIDE = IMAGE_EXTERIOR + "PASSENGER_SIDE";
	public static final String IMAGE_EXTERIOR_TOP = IMAGE_EXTERIOR + "TOP";

	public static final List<String> IMAGE_KEYS_EXTERIOR_CORNER_ONLY = Collections.unmodifiableList(Arrays.asList(
			IMAGE_EXTERIOR_FULL_FRONT_CORNER,
			IMAGE_EXTERIOR_FULL_REAR_CORNER
	));
	public static final List<String> IMAGE_KEYS_EXTERIOR_FULL_SET = Collections.unmodifiableList(Arrays.asList(
			IMAGE_EXTERIOR_FRONT,
			IMAGE_EXTERIOR_DRIVER_SIDE,
			IMAGE_EXTERIOR_REAR,
			IMAGE_EXTERIOR_PASSENGER_SIDE,
			IMAGE_EXTERIOR_TOP
	));

	public static final String FILLER_LOAD_STRING = "FILLER";
	public static final String DUMMY_SIGNATURE = "***DO NOT SEND TO SERVER***";

	public static final String IMAGE_FILE_DELIM = "_";
	public static final String PRELOAD_VIN_IMAGE_FILE_PREFIX = "PreloadVIN" + IMAGE_FILE_DELIM;
	public static final String DELIVERY_VIN_IMAGE_FILE_PREFIX = "DeliveryVIN" + IMAGE_FILE_DELIM;
	public static final String PRELOAD_IMAGE_FILE_PREFIX = "Preload" + IMAGE_FILE_DELIM;
	public static final String DELIVERY_IMAGE_FILE_PREFIX = "Delivery" + IMAGE_FILE_DELIM;


	private static final String PACKAGE_NAME = "com.cassens.autotran";
	public static final String KEY_DETECTED_ACTIVITIES = PACKAGE_NAME + ".DETECTED_ACTIVITIES";
	public static final boolean USE_ACTIVITY_TRANSITIONS_INTERFACE = false;
	/**
	 * The desired time between activity detections. Larger values result in fewer activity
	 * detections while improving battery life. A value of 0 results in activity detections at the
	 * fastest possible rate.
	 */
	public static final long DETECTION_INTERVAL_IN_MILLISECONDS = 5000;

	/**
	 * List of DetectedActivity types that we monitor in this sample.
	 */
	public static final int[] MONITORED_ACTIVITIES = {
			DetectedActivity.STILL,
			DetectedActivity.ON_FOOT,
			DetectedActivity.WALKING,
			DetectedActivity.RUNNING,
			DetectedActivity.ON_BICYCLE,
			DetectedActivity.IN_VEHICLE,
			DetectedActivity.TILTING,
			DetectedActivity.UNKNOWN
	};

	public static final boolean FEATURE_ALLOW_VIN_CLICK_AFTER_INSPECTION = false;
}
