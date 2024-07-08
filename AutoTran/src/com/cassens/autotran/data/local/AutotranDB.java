package com.cassens.autotran.data.local;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.model.Questionnaire;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AutotranDB extends SQLiteOpenHelper {
	private static final Logger log = LoggerFactory.getLogger(AutotranDB.class.getSimpleName());
	private final static String DATABASE_NAME = "AutotranLocalDB";

	public final static String TABLE_USER = "userDetails";

	//A load is a truck with vehicles loaded
	public final static String TABLE_LOAD = "load";

	//A delivery is a set of vehicles on a load destined for a specific dealer
	public final static String TABLE_DELIVERY = "delivery";

	//Many to many to map VINs to Deliveries (this info COULD go in vehicle)
	public final static String TABLE_DELIVERY_VIN = "delivery_vin";

	//Damage images
	public final static String TABLE_IMAGE = "image";

	//All vehicles
	public final static String TABLE_VIN = "vin";

	//All vehicles
	public final static String TABLE_DEALER = "dealers";

	//Training requirements
	public final static String TABLE_TRAINING_REQUIREMENT = "trainingRequirement";

    //Training type lookup table
    public final static String TABLE_TRAINING_TYPE = "trainingType";

    //Trending alerts
    public final static String TABLE_TRENDING_ALERT = "trendingAlert";

	//An damage is a set of area/type/svrty tuples that indicate a damage to a vehicle
	public final static String TABLE_DAMAGE = "damage";

	public final static String TABLE_PLANT_RETURN = "plantReturn";
	public final static String TABLE_RECEIVED_VEHICLE = "receivedVehicle";
	public final static String TABLE_YARD_EXIT = "yardExit";
	public final static String TABLE_YARD_INVENTORY = "yardInventory";
	public final static String TABLE_INSPECTION = "inspection";
	public final static String TABLE_PROBLEM_REPORT = "problemReport";

	//Damage lookup tables
	public final static String TABLE_TYPE_CODE = "typeCode";
	public final static String TABLE_SVRTY_CODE = "svrtyCode";
	public final static String TABLE_AREA_CODE = "areaCode";
	public final static String TABLE_SPECIAL_CODE = "specialCode";

	public final static String TABLE_SCAC_CODE = "scacCode";
	public final static String TABLE_LOT_CODE = "lotCode";
	public final static String TABLE_TERMINAL = "terminal";
	public final static String TABLE_SHUTTLE_MOVE = "shuttleMove";

	public final static String TABLE_DRIVER_ACTIONS = "driver_action";
	public final static String TABLE_LOT_CODE_MSGS = "lot_code_messages";
	public final static String TABLE_DAMAGE_NOTE_TEMPLATES = "damage_note_templates";
	public final static String TABLE_DAMAGE_NOTES = "damage_note";
	public final static String TABLE_PREDEFINED_NOTES = "predefined_notes";
	public final static String TABLE_LOAD_EVENTS = "load_events";

	//audit questionnaire look up table for high claims audits
	public final static String TABLE_QUESTIONNAIRE = "questionnaire";
	public final static String TABLE_WM_REQUESTS = "wm_requests";

	public final static int VERSION = 120;

	public Context appContext;

	public AutotranDB(Context context) {
		super(context.getApplicationContext(), DATABASE_NAME, null, VERSION);
		appContext = context.getApplicationContext();
//		context.deleteDatabase(DATABASE_NAME);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {

		log.debug(Logs.DEBUG, "Creating new AutoTran SQLite database. Version is " + VERSION);

		//User
		db.execSQL("Create table " + TABLE_USER + "(user_id INTEGER  PRIMARY KEY AUTOINCREMENT, " +
				"user_remote_id TEXT, " +
				"firstName TEXT, " +
				"lastName TEXT, " +
				"email TEXT, " +
				"driverNumber TEXT, " +
				"deviceToken TEXT, " +
				"deviceID TEXT, " +
				"password TEXT, " +
				"role TEXT, " +
				"userType TEXT, " +
				"activationLink TEXT, " +
				"status TEXT, " +
				"created TEXT, " +
				"modified TEXT, " +
				"highClaims INTEGER," +
				"requiresAudit INTEGER," +
				"inspectionAccess INTEGER," +
				"supervisorCardCode TEXT," +
				"supervisorPreloadChk INTEGER," +
				"helpTerm INTEGER," +
				"driverLicenseExpiration TEXT," +
				"medicalCertificateExpiration TEXT," +
				"autoInspectLastDelivery INTEGER NOT NULL DEFAULT 0," +
				"fullName TEXT)"
				);

		//Load
		//I removed the vin FKs from this table, hopefully that makes sense down the line
		db.execSQL("Create table " + TABLE_LOAD + "(load_id INTEGER  PRIMARY KEY AUTOINCREMENT, " +
				"load_remote_id TEXT, " +
				"driver_id INTEGER, " +
				"driver_number TEXT, " +
				"truck_number TEXT, " +
				"driverPreLoadSignature TEXT, " +
				"driverPreLoadContact TEXT, " +
				"driverPreLoadSignatureLat TEXT, " +
				"driverPreLoadSignatureLon TEXT, " +
				"driverPreLoadComment TEXT, " +
				"driverPreLoadSignatureSignedAt TEXT, " +
				"preloadSupervisorSignedAt TEXT," +
				"preloadSupervisorSignature TEXT," +
				"preloadSupervisorSignatureLat TEXT," +
				"preloadSupervisorSignatureLon TEXT," +
				"status TEXT, " +
				"preloadUploadStatus INTEGER, " +
				"deliveryUploadStatus INTEGER, " +
				"uploaded INTEGER, " +
				"shuttleLoad INTEGER, " +
				"shuttleMoveId INTEGER, " +
				"loadNumber TEXT, " +
				"lastUpdated TEXT, " +
				"originLoadNumber TEXT, " +
				"relayLoadNumber TEXT, " +
				"relayLoadDealerName TEXT, " +
				"relayLoad TEXT, " +
				"originLoad TEXT, " +
				"notes TEXT, " +
				"ldtyp TEXT, " +
				"supervisorSignature TEXT, " +
				"supervisorSignedAt TEXT, " +
				"supervisorSignatureLat TEXT, " +
				"supervisorSignatureLon TEXT, " +
				"driverHighClaimsAudit TEXT, " +
				"originTerminal TEXT, " +
				"helpTerminal TEXT, " +
				"nextDispatch TEXT, " +
				"firstDrop TEXT, " +
				"lastDrop TEXT, " +
				"lotCodeMsgSeen INTEGER, " +
				"parentLoad INTEGER, " +
				"parent_load_id INTEGER, " +
				"pickSheetImageRequired INTEGER NOT NULL DEFAULT 1, " +
				"extraDocImageRequired TEXT NOT NULL DEFAULT \'\', " +
				"trailerNumber TEXT " +
				")");

		//Delivery
		//FK: load
		db.execSQL("Create table " + TABLE_DELIVERY + "(delivery_id INTEGER  PRIMARY KEY AUTOINCREMENT, " +
				"delivery_remote_id TEXT, " +
				"load_id INTEGER, " +
				"dealer_id INTEGER, " +
				"timestamp TEXT, " +
				"token TEXT, " +
				"ship_date TEXT, " +
				"estdeliverdate TEXT, " +
				"dealerSignature TEXT, " +
				"dealerContact TEXT, " +
				"dealerEmail TEXT, " +
				"dealerSignatureLat TEXT, " +
				"dealerSignatureLon TEXT, " +
				"dealerSignatureSignedAt TEXT, " +
				"dealerComment TEXT, " +
				"driverSignature TEXT, " +
				"driverSignatureLat TEXT, " +
				"driverSignatureLon TEXT, " +
				"driverSignatureSignedAt TEXT, " +
				"driverComment TEXT, " +
				"driverContact TEXT, " +
				"sti INTEGER, " +
				"afrhrs INTEGER, " +
				"delivery TEXT, " +
				"callback TEXT, " +
				"preloadUploadStatus INTEGER, " +
				"deliveryUploadStatus INTEGER, " +
				"status TEXT, " +
				"userType TEXT," +
				"shuttleLoad INTEGER," +
				"uploaded INTEGER," +
				"notes TEXT," +
				"dockTerm INTEGER," +
				"safeDelivery TEXT " +
				")");

		//Delivery_Vin
		//FK: delivery, vin
		db.execSQL("Create table " + TABLE_DELIVERY_VIN + "(delivery_vin_id INTEGER  PRIMARY KEY AUTOINCREMENT, " +
				"delivery_vin_remote_id TEXT," +
				"vin_id TEXT, " +
				"delivery_id TEXT, " +
				"token TEXT, " +
				"timestamp TEXT, " +
				"facing TEXT, " +
				"ats TEXT, " +
				"preloadNotes TEXT, " +
				"deliveryNotes TEXT, " +
				"position TEXT, " +
				"user_type TEXT, " +
				appContext.getString(R.string.KEY) +
				"byteArray TEXT," +
				"pro TEXT," +
				"bckhlnbr TEXT," +
				"rowbay TEXT," +
				"backdrv TEXT," +
				"rejected_by TEXT," +
				"rldspickup TEXT," +
				"do_lotlocate TEXT," +
				"lot TEXT," +
				"von TEXT," +
				"rte1 TEXT," +
				"rte2	 TEXT," +
				"status TEXT," +
				"ldseq TEXT, " +
				"shuttleLoadProdStatus TEXT, " +
				"shuttleLoadRoute TEXT, " +
				"inspectedPreload INTEGER, " +
				"inspectedDelivery INTEGER, " +
				"supervisorSignatureSignedAt TEXT, " +
				"supervisorSignature TEXT, " +
				"supervisorSignatureLat TEXT, " +
				"supervisorSignatureLon TEXT, " +
				"supervisorComment TEXT, " +
				"supervisorContact TEXT, " +
				"preloadUploadStatus INTEGER, " +
				"deliveryUploadStatus INTEGER, " +
				"uploaded INTEGER, " +
				"finalMfg TEXT, " +
				"finalDealer TEXT " +
				")");

		//Image
		//FK: delivery_vin
		db.execSQL("Create table " + TABLE_IMAGE + "(image_id INTEGER  PRIMARY KEY AUTOINCREMENT, " +
				"delivery_vin_id INTEGER, " +
				"load_id TEXT," +
				"delivery_id TEXT," +
				"inspection_id INTEGER, " +
				"problem_report_guid TEXT, " +
				"filename TEXT," +
				"image_bytes TEXT, " +
				"imageLat TEXT, " +
				"imageLon TEXT," +
				"preloadImage INTEGER," +
				"preloadUploadStatus INTEGER, " +
				"deliveryUploadStatus INTEGER, " +
				"uploadStatus INTEGER, " +
				"uploadIndex INTEGER, " +
				"uploaded INTEGER, " +
				"foreignKey INTEGER, " +
				"foreignKeyLabel TEXT, " +
				"preauth_url TEXT, " +
				"s3_upload_status INTEGER, " +
				"retries INTEGER NOT NULL DEFAULT 0, " +
                "inspection_guid TEXT " +
				")");

		//vin
		//FK: load (I'm not convinced, but this might be for some reason)
		db.execSQL("Create table " + TABLE_VIN + "(vin_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" vin_remote_id TEXT, " +
				" load_id TEXT," +
				" dealer_id TEXT," +
				" id TEXT," + //Need to determine what this ID is.  Is it a cassens ID?
				" vin_number TEXT," +
				" body TEXT," +
				" weight TEXT," +
				" colordes TEXT," +
				" ldnbr TEXT," +
				" type TEXT," +
				" created TEXT," +
				" driver_comment TEXT," +
				" status TEXT," +
				" is_damage TEXT," +
				" fillers TEXT," +
				" ats TEXT," +
				" notes_image TEXT," +
				" modified TEXT," +
				" color TEXT," +
				" callback TEXT," +
				" notes TEXT," +
				" dealer_comment TEXT," +
				" position TEXT," +
				" customer_name TEXT)");

		//Damage
		//FK: vin, typecode, areacode, svrtycode, specialcode
		db.execSQL("Create table " + TABLE_DAMAGE + "(damage_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" delivery_vin_id TEXT," +
				" inspection_id TEXT," +
				" type_code_id TEXT," +
				" svrty_code_id TEXT," +
				" area_code_id TEXT," +
				" special_code_id TEXT, " +
				" preLoadDamage INTEGER, " +
				" preloadUploadStatus INTEGER, " +
				" deliveryUploadStatus INTEGER, " +
				" uploadStatus INTEGER, " +
				" uploaded INTEGER," +
				" readonly INTEGER," +
				" source TEXT, " +
				" guid TEXT, " +
				" inspection_guid TEXT " +
				")");

		//Dealer
		db.execSQL("Create table " + TABLE_DEALER + "(dealer_id INTEGER  PRIMARY KEY AUTOINCREMENT, " +
				" dealer_remote_id INTEGER," +
				" mfg TEXT," +
				" customer_number TEXT," +
				" customer_name TEXT," +
				" city TEXT," +
				" state TEXT," +
				" address TEXT," +
				" zip INTEGER," +
				" contact_name TEXT," +
				" email TEXT," +
				" phone TEXT," +
				" status TEXT," +
				" monam INTEGER," +
				" tueam INTEGER," +
				" wedam INTEGER," +
				" thuam INTEGER," +
				" friam INTEGER," +
				" satam INTEGER," +
				" sunam INTEGER," +
				" monpm INTEGER," +
				" tuepm INTEGER," +
				" wedpm INTEGER," +
				" thupm INTEGER," +
				" fripm INTEGER," +
				" satpm INTEGER," +
				" sunpm INTEGER," +
				" afthr TEXT," +
				" comments TEXT," +
				" high_claims INTEGER," +
				" alwaysUnattended INTEGER NOT NULL DEFAULT 0," +
				" photosOnUnattended INTEGER NOT NULL DEFAULT 0," +
				" lotLocateRequired INTEGER," +
				" lot_code_id INTEGER NOT NULL DEFAULT -1," +
				" countryCode TEXT," +
				" last_updated TEXT," +
				"updated_fields TEXT NOT NULL DEFAULT ''" +
				")");
		//INSPECTION
		//FK:
		db.execSQL("Create table " + TABLE_INSPECTION + "(_id INTEGER  PRIMARY KEY AUTOINCREMENT, " +
				" vin TEXT," +
				" guid TEXT, " +
				" inspector TEXT," +
				" notes TEXT," +
				" terminal_id INTEGER," +
				" lot_code_id INTEGER," +
				" type INTEGER," +
				" imageCount INTEGER, " +
				" damageCount INTEGER, " +
				" upload_status INTEGER," +
				" scac_code_id INTEGER," +
				" latitude REAL," +
				" longitude REAL," +
				" timestamp DATETIME" +
		")");

		db.execSQL("Create table " + TABLE_YARD_EXIT + "(_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" terminal_id INTEGER," +
				" inspector TEXT," +
				" VIN TEXT," +
				" scac_code_id INTEGER," +
				" inbound INTEGER," +
				" upload_status INTEGER)");

		db.execSQL("Create table " + TABLE_PLANT_RETURN + "(_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" terminal_id INTEGER," +
				" inspector TEXT," +
				" VIN TEXT," +
				" delay_code INTEGER," +
				" upload_status INTEGER)");

		db.execSQL("Create table " + TABLE_RECEIVED_VEHICLE + "(_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" terminal_id INTEGER," +
				" inspector TEXT," +
				" VIN TEXT," +
				" upload_status INTEGER)");

		//Lookup tables
		db.execSQL("Create table " + TABLE_TYPE_CODE + "(type_code_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" type_code_remote_id TEXT," +
				" code TEXT," +
				" active INTEGER," +
				" modified DATE," +
				" description TEXT)");
		db.execSQL("Create table " + TABLE_SVRTY_CODE + "(severity_code_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" severity_code_remote_id TEXT," +
				" code TEXT," +
				" active INTEGER," +
				" modified DATE," +
				" description TEXT)");
		db.execSQL("Create table " + TABLE_AREA_CODE + "(area_code_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" parent_area_code_id INTEGER, " +
				" area_code_remote_id TEXT," +
				" code TEXT," +
				" active INTEGER," +
				" modified DATE," +
				" description TEXT)");
		db.execSQL("Create table " + TABLE_SPECIAL_CODE + "(special_code_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" special_code_remote_id TEXT, " +
				" area_code TEXT, " +
				" type_code TEXT, " +
				" severity_code TEXT, " +
				" specialCode TEXT," +
				" active INTEGER," +
				" modified DATE," +
				" description TEXT)");

		db.execSQL("Create table " + TABLE_TERMINAL + "(terminal_id INTEGER  PRIMARY KEY ," +
				" code TEXT," +
				" description TEXT," +
				" rowCharacters TEXT," +
				" bayCharacters TEXT," +
				" popupMessage TEXT," +
				" phoneNumber TEXT," +
				" canToUsPhoneNumber TEXT," +
				" usToCanPhoneNumber TEXT," +
				" dispatchPhoneNumber TEXT," +
				" countryCode TEXT," +
				" active INTEGER," +
				" modified DATE)");

		db.execSQL("Create table " + TABLE_SCAC_CODE + "(scac_code_id INTEGER  PRIMARY KEY ," +
				" code TEXT," +
				" terminal_id INTEGER," +
				" description TEXT," +
				" active INTEGER," +
				" modified DATE)");

		//Audit Questionnaire
//		db.execSQL("Create table " + TABLE_AUDIT_QUESTIONNAIRE + "(id INTEGER  PRIMARY KEY AUTOINCREMENT, " +
//				"driver_id INTEGER, " +
//				"load_id INTEGER, " +
//				"questionnaire TEXT, " +
//				"note TEXT," +
//				"supervisor_signedat  DATE)");

		buildLotCodeTable(db);
		buildYardInventoryTable(db);

		buildShuttleMoveTable(db);
		buildDriverActionTable(db);
		buildPredefinedNoteTable(db);
		buildLotCodeMsgTable(db);
		buildProblemReportTable(db);

		buildDamageNoteTemplatesTable(db);
		buildDamageNotesTable(db);

		buildLoadEventTable(db);
		buildQuestionnaireTable(db);

		buildTrainingTypeTable(db);
		buildTrainingRequirementTable(db);
		buildTrendingAlertTable(db);
		buildWMRequestsTable(db);
	}

	private void buildLotCodeTable(SQLiteDatabase db) {
		db.execSQL("Create table " + TABLE_LOT_CODE + "(lot_code_id INTEGER  PRIMARY KEY ," +
				" code TEXT," +
				" terminal_id INTEGER, " +
				" shuttleMoveCode TEXT, " +
				" active INTEGER," +
				" modified DATE," +
				" description TEXT)");
	}

	private void buildYardInventoryTable(SQLiteDatabase db) {
		db.execSQL("Create table " + TABLE_YARD_INVENTORY + "(_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" terminal_id INTEGER," +
				" inspector TEXT," +
				" VIN TEXT," +

				"latitude REAL," +
				"longitude REAL," +
				"ldnbr TEXT," +
				"delivery_vin_id NUMBER," +

				" lot_code_id INTEGER," +
				appContext.getString(R.string.ROW) +
				" bay TEXT," +
				" upload_status INTEGER," +
				" lot_locate INTEGER)");
	}

	private void buildLoadEventTable(SQLiteDatabase db) {
		db.execSQL("Create table " + TABLE_LOAD_EVENTS + "(_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" csv TEXT," +
				" upload_status INTEGER)");
	}

	// jha Add function similar to above one for getting the questions from the questionnaire
	private void buildQuestionnaireTable(SQLiteDatabase db) {
		db.execSQL("Create table " + TABLE_QUESTIONNAIRE + "(_id INTEGER  PRIMARY KEY AUTOINCREMENT," +
				" type TEXT," +
				" version INTEGER," +
				" prompts TEXT)");
		populateAuditQuestionnaire(db);
	}

	// HACK to populate the audit_questionnaire table.  This will be removed later. For the release,
	// the table will be populated with data from the server via ConsolidatedDataPullTask() - PDK
	private void populateAuditQuestionnaire(SQLiteDatabase db) {
		String[] prompts = AutoTranApplication.getAppContext().getResources().getStringArray(R.array.driver_high_claims_questions);

		String promptsJson = "[ ";
		for (int i= 0; i < prompts.length; i++) {
			promptsJson += "{\"text\":\"" + prompts[i] + "\", \"type\":\"boolean\", \"format\":\"Y/N\" }, ";
		}
		promptsJson += " ]";

		db.execSQL("INSERT INTO " + TABLE_QUESTIONNAIRE + " (type, version, prompts) " + "VALUES(\'" +
				Questionnaire.Type.PreloadAudit.name() + "\',0,\'" + promptsJson + "\')");
		/*db.execSQL("INSERT INTO " + TABLE_QUESTIONNAIRE + " (type, prompts) " + "VALUES(\'" +
				Questionnaire.Type.HighClaimsAudit.name() + "\',\'" +
				StringUtils.join(prompts, "\n") + "\')"); */
	}


	private void buildShuttleMoveTable(SQLiteDatabase db) {

		db.execSQL("Create table " + TABLE_SHUTTLE_MOVE + "(shuttle_move_id INTEGER  PRIMARY KEY," +
				" orgDestString TEXT, " +
				" terminal TEXT," +
				" origin TEXT," +
				" destination TEXT," +
				" active INTEGER," +
				" modified INTEGER" +
				")");


	}

	private void buildDriverActionTable(SQLiteDatabase db) {

		db.execSQL("Create table " + TABLE_DRIVER_ACTIONS + "(id INTEGER  PRIMARY KEY," +
				" driver_id TEXT, " +
				appContext.getString(R.string.ACTION) +
				" data TEXT," +
				" status TEXT," +
				" sender_id TEXT," +
				" upload_status INTEGER," +
				" received TEXT," +
				" processed TEXT," +
				" created TEXT," +
				" modified TEXT" +
				")");
	}

	private void buildLotCodeMsgTable(SQLiteDatabase db) {

		db.execSQL("Create table " + TABLE_LOT_CODE_MSGS + "(id INTEGER  PRIMARY KEY," +
				" lot_code_id INTEGER, " +
				" message TEXT," +
				" prompt TEXT," +
				" response TEXT," +
				" modified INTEGER," +
				" active INTEGER" +
				")");
	}

	private void buildPredefinedNoteTable(SQLiteDatabase db) {
	    db.execSQL("Create table " + TABLE_PREDEFINED_NOTES + "(id INTEGER  PRIMARY KEY,"+
                " note TEXT," +
                " active INTEGER," +
                " damageNote INTEGER," +
                " signatureNote INTEGER," +
				" mfg TEXT," +
                " modified INTEGER" +
                ")");
    }

	private void buildProblemReportTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_PROBLEM_REPORT + "(id INTEGER PRIMARY KEY AUTOINCREMENT," +
			" guid TEXT," +
			" driver_id INTEGER," +
			" category TEXT," +
			" description TEXT," +
			" timestamp INTEGER," +
			" timezone INTEGER," +
			" latitude TEXT," +
			" longitude TEXT," +
			" imageCount INTEGER," +
			" upload_status INTEGER" +
			")");
	}

	private void buildDamageNoteTemplatesTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_DAMAGE_NOTE_TEMPLATES + "(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"comment TEXT, " +
				"driver_prompt TEXT, " +
				"driver_prompt_type TEXT, " +
				"dealer_prompt TEXT, " +
				"dealer_prompt_type TEXT, " +
				"area_code TEXT, " +
				"type_code TEXT, " +
				"severity_code TEXT, " +
				"originTerminal TEXT, " +
				"mfg TEXT, " +
				"modified DATETIME, " +
				"active INTEGER " +
				")");
	}

	private void buildDamageNotesTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_DAMAGE_NOTES + "(id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"guid TEXT," +
				"damage_local_id INTEGER , " +
				"damage_guid TEXT , " +
				"preload_driver_comment TEXT , " +
				"delivery_driver_comment TEXT , " +
				"delivery_dealer_comment TEXT , " +
				"damage_note_template_id INTEGER " +
				")");
	}


    private void buildTrainingTypeTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TRAINING_TYPE + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "name TEXT," +
                "defaultRequiredProgress INTEGER" +
                ")");
    }

    private void buildTrainingRequirementTable(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TABLE_TRAINING_REQUIREMENT + "(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "supervisor_id INTEGER," +
                "load_id INTEGER," +
                "user_id INTEGER," +
                "type INTEGER NOT NULL DEFAULT 1," + // coaching
                "assigned TEXT," +
                "started TEXT," +
                "completed TEXT," +
                "progress INTEGER," +
                "requiredProgress INTEGER," +
                "uploaded INTEGER DEFAULT 0," +
                "vin TEXT," +
                "supplementalData TEXT," +
                "supplementalReference TEXT," +
                "startedLatitude REAL," +
                "startedLongitude REAL," +
                "completedLatitude REAL," +
                "completedLongitude REAL," +
                "adHoc INTEGER," +
                "FOREIGN KEY (type) REFERENCES " + TABLE_TRAINING_TYPE + "(id)" +
                ")");
    }

    private void buildTrendingAlertTable(SQLiteDatabase db) {
		db.execSQL("CREATE TABLE " + TABLE_TRENDING_ALERT + "(" +
				"id INTEGER PRIMARY KEY AUTOINCREMENT," +
				"load_id INTEGER," +
				"driver_id INTEGER," +
				"loadNumber TEXT," +
				"vin_id TEXT," +
				"alert TEXT," +
				"ordr INTEGER," +
				"count INTEGER," +
				"type TEXT" +
				")");
	}

	private void buildWMRequestsTable(SQLiteDatabase db) {
		db.execSQL("Create table " + TABLE_WM_REQUESTS + "(id INTEGER PRIMARY KEY AUTOINCREMENT, " +
				"uuid TEXT, " +
				"jsonData TEXT, " +
				"retries INTEGER)");
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        log.debug(Logs.DEBUG, "upgrading database from " + oldVersion + " to " + newVersion);

		int startingOldVersion = oldVersion;

		/* 
		 * use this pattern for migrations
		 * 
		 * if (oldVersion == old && newVersion >= new) {
		 * 	...migration statements
		 * 
		 * 	oldVersion = new;
		 * }
		 * 
		 * This allows for incremental migration from any version to current
		 */
		if (oldVersion == 71 && newVersion >= 73) {
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN lastUpdated TEXT");
			db.execSQL("ALTER TABLE " + TABLE_IMAGE + " ADD COLUMN foreignKey INTEGER");
			db.execSQL("ALTER TABLE " + TABLE_IMAGE + " ADD COLUMN foreignKeyLabel TEXT");
			db.execSQL("ALTER TABLE " + TABLE_DEALER + " ADD COLUMN high_claims INTEGER");

			oldVersion = 73;
		}

		if (oldVersion < 74 && newVersion >= 74) {
			db.execSQL("ALTER TABLE " + TABLE_DELIVERY_VIN + " ADD COLUMN shuttleLoadProdStatus TEXT");
			db.execSQL("ALTER TABLE " + TABLE_DELIVERY_VIN + " ADD COLUMN shuttleLoadRoute TEXT");
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN shuttleMoveId INT");
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN driver_number TEXT");
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN truck_number TEXT");
			db.execSQL("ALTER TABLE " + TABLE_SHUTTLE_MOVE + " RENAME TO " + TABLE_SHUTTLE_MOVE + "_backup");
			buildShuttleMoveTable(db);
			db.execSQL("INSERT INTO " + TABLE_SHUTTLE_MOVE + " (orgDestString, terminal, origin, destination) " +
					" SELECT orgDestString, terminal, origin, destination FROM " + TABLE_SHUTTLE_MOVE + "_backup");
			db.execSQL("DROP TABLE " + TABLE_SHUTTLE_MOVE + "_backup");

			oldVersion = 74;
		}

        if(oldVersion < 76 && newVersion >= 76) {
            log.debug(Logs.DEBUG, "adding relay columns to damage and load");

            db.execSQL("ALTER TABLE " + TABLE_DAMAGE + " ADD COLUMN readonly INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN relayLoad TEXT");
            db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN originLoad TEXT");
            db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN originLoadNumber TEXT");

            oldVersion = 76;
        }

        if(oldVersion < 78 && newVersion >= 78) {

            try {
                db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN relayLoadNumber TEXT");
                db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN relayLoadDealerName TEXT");
            } catch (SQLiteException ex) {
                log.debug(Logs.DEBUG, "couldn't add the duplicate table, try to modify");

                db.execSQL("ALTER TABLE " + TABLE_LOAD + appContext.getString(R.string.MODIFY) + " COLUMN relayLoadNumber TEXT");
                db.execSQL("ALTER TABLE " + TABLE_LOAD + appContext.getString(R.string.MODIFY) + " COLUMN relayLoadDealerName TEXT");

            }

            oldVersion = 79;
        }

		if(oldVersion < 80 && newVersion >= 80) {
				db.execSQL("ALTER TABLE " + TABLE_IMAGE + " ADD COLUMN filename TEXT");

			oldVersion = 80;
        }

        if (oldVersion < 81 && newVersion >= 81) {
            db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN notes TEXT");
            db.execSQL("ALTER TABLE " + TABLE_DELIVERY + "origin ADD COLUMN notes TEXT");
            db.execSQL("ALTER TABLE " + TABLE_IMAGE + " ADD COLUMN load_id INTEGER");
            db.execSQL("ALTER TABLE " + TABLE_IMAGE + " ADD COLUMN delivery_id INTEGER");

            oldVersion = 81;
        }

        if (oldVersion < 82 && newVersion >= 82) {
            db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN ldtyp TEXT");

            oldVersion = 82;
		}

		if(oldVersion < 83 && newVersion >= 83) {
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN originTerminal TEXT");

			oldVersion = 83;
		}

		if(oldVersion < 84 && newVersion >= 84) {
			db.execSQL("ALTER TABLE " + TABLE_AREA_CODE + " ADD COLUMN active INTEGER");
			db.execSQL("ALTER TABLE " + TABLE_TYPE_CODE + " ADD COLUMN active INTEGER");
			db.execSQL("ALTER TABLE " + TABLE_SVRTY_CODE + " ADD COLUMN active INTEGER");
			db.execSQL("ALTER TABLE " + TABLE_SPECIAL_CODE + " ADD COLUMN active INTEGER");

			buildDriverActionTable(db);
			oldVersion = 84;
		}

		if(oldVersion < 85 && newVersion >= 85) {
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN driverHighClaimsAudit TEXT");
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN supervisorSignature TEXT");
			db.execSQL("ALTER TABLE " + TABLE_USER + " ADD COLUMN highClaims INTEGER");
			db.execSQL("ALTER TABLE " + TABLE_DAMAGE + " ADD COLUMN source TEXT");
			db.execSQL("ALTER TABLE " + TABLE_DAMAGE + " ADD COLUMN guid TEXT");

			oldVersion = 85;
		}

		if(oldVersion < 86 && newVersion >= 86) {
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN nextDispatch TEXT");
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN firstDrop TEXT");
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN lastDrop TEXT");

			oldVersion = 86;
		}

		if(oldVersion < 87 && newVersion >= 87) {
			db.execSQL("ALTER TABLE " + TABLE_DEALER + " ADD COLUMN last_updated TEXT");

			oldVersion = 87;
		}

		if(oldVersion < 88 && newVersion >= 88) {
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN helpTerminal TEXT");

			oldVersion = 88;
		}

		if(oldVersion < 89 && newVersion >= 89) {
			db.execSQL("ALTER TABLE " + TABLE_LOAD + " ADD COLUMN lotCodeMsgSeen INTEGER");

			buildLotCodeMsgTable(db);

			oldVersion = 89;
		}

		if(startingOldVersion == 89 && newVersion >= 90) {
                        //This is only for v89 -> newer.  
                        //this is a one-off situation where we need to be
                        //handle upgrading the load messages table after it
                        //has been created but before we added the 'active'
                        //flag.  

			createColumn(db, TABLE_LOT_CODE_MSGS, "active", "INTEGER");

            oldVersion = 90;
        }

		if (oldVersion < 91 && newVersion >= 91) {
			db.execSQL("ALTER TABLE "  + TABLE_DELIVERY + " ADD COLUMN dockTerm INTERGER");

			oldVersion = 91;
		}

		if(oldVersion < 92 && newVersion >= 92) {
			db.execSQL("ALTER TABLE " + TABLE_DRIVER_ACTIONS + " ADD COLUMN upload_status TEXT");
			db.execSQL("ALTER TABLE " + TABLE_DRIVER_ACTIONS + " ADD COLUMN processesd TEXT");

			oldVersion = 92;
		}

		if(oldVersion < 93 && newVersion >= 93) {
			createColumn(db, TABLE_DRIVER_ACTIONS, "sender_id", "TEXT");
			createColumn(db, TABLE_DRIVER_ACTIONS, "created", "INTEGER");
			oldVersion = 93;
		}

		if(oldVersion < 94 && newVersion >= 94) {
			createColumn(db, TABLE_IMAGE, "problem_report_guid", "TEXT");
			oldVersion = 94;
		}

		if(oldVersion < 95 && newVersion >= 95) {
			buildProblemReportTable(db);
			oldVersion = 95;
		}

		if(oldVersion < 96 && newVersion >= 96) {
			createColumn(db, TABLE_IMAGE, "problem_report_guid", "TEXT");
			oldVersion = 96;
		}

		if(oldVersion < 97 && newVersion >= 97) {
			createColumn(db, TABLE_DRIVER_ACTIONS, "processed", "TEXT");
			oldVersion = 97;
		}

		if(oldVersion < 98 && newVersion >= 98) {
			createColumn(db, TABLE_DRIVER_ACTIONS, "sender_id", "TEXT");
			oldVersion = 98;
		}

		if(oldVersion < 99 && newVersion >= 99) {
			buildDamageNoteTemplatesTable(db);
			buildDamageNotesTable(db);
			oldVersion = 99;
		}

		if(oldVersion < 100 && newVersion >= 100) {
			createColumn(db, TABLE_DAMAGE_NOTE_TEMPLATES, "originTerminal", "TEXT");
			createColumn(db, TABLE_DAMAGE_NOTE_TEMPLATES, "mfg", "TEXT");
			oldVersion = 100;
		}

		if(oldVersion < 101 && newVersion >= 101) {
			createColumn(db, TABLE_YARD_INVENTORY, "latitude", "REAL");
			createColumn(db, TABLE_YARD_INVENTORY, "longitude", "REAL");
			createColumn(db, TABLE_YARD_INVENTORY, "ldnbr", "TEXT");
			createColumn(db, TABLE_YARD_INVENTORY, "delivery_vin_id", "NUMBER");

			createColumn(db, TABLE_LOT_CODE, "shuttleMoveCode", "text");
			createColumn(db, TABLE_LOT_CODE, "active", "INTEGER");
			createColumn(db, TABLE_LOT_CODE, "modified", "DATE");

			oldVersion = 101;
		}

		if(oldVersion < 102 && newVersion >= 102){
			createColumn(db, TABLE_TERMINAL, "popupMessage", "TEXT");

			oldVersion = 102;
		}
		if(oldVersion <103 && newVersion >= 103) {
            buildPredefinedNoteTable(db);
			buildLoadEventTable(db);
            oldVersion = 103;
        }

        if(oldVersion <104 && newVersion >= 104) {
			createColumn(db, TABLE_IMAGE, "preauth_url", "TEXT");
			createColumn(db, TABLE_IMAGE, "s3_upload_status", "INTEGER");
			createColumn(db, TABLE_AREA_CODE, "modified", "DATE");
			createColumn(db, TABLE_TYPE_CODE, "modified", "DATE");
			createColumn(db, TABLE_SVRTY_CODE, "modified", "DATE");
			createColumn(db, TABLE_SPECIAL_CODE, "modified", "DATE");
			createColumn(db, TABLE_SCAC_CODE, "modified", "DATE");
			createColumn(db, TABLE_SCAC_CODE, "active", "INTEGER");
			createColumn(db, TABLE_TERMINAL, "modified", "DATE");
			createColumn(db, TABLE_TERMINAL, "active", "INTEGER");
			createColumn(db, TABLE_DEALER, "lotLocateRequired", "INTEGER");
			createColumn(db, TABLE_DEALER, "terminal_id", "INTEGER");

			oldVersion = 104;
		}

		if(oldVersion <105 && newVersion >= 105) {
			createColumn(db, TABLE_INSPECTION, "guid", "TEXT");
			createColumn(db, TABLE_INSPECTION, "imageCount", "INTEGER");
			createColumn(db, TABLE_INSPECTION, "damageCount", "INTEGER");
			createColumn(db, TABLE_IMAGE, "inspection_guid", "TEXT");
			createColumn(db, TABLE_DAMAGE, "inspection_guid", "TEXT");
			createColumn(db, TABLE_USER, "inspectionAccess", "INTEGER");

			oldVersion = 105;
		}

		if(oldVersion < 106 && newVersion >= 106) {
			createColumn(db, TABLE_LOAD, "parentLoad", "INTEGER");
			createColumn(db, TABLE_LOAD, "parent_load_id", "INTEGER");

			oldVersion = 106;
		}

		if(oldVersion < 107 && newVersion >= 107) {
			createColumn(db, TABLE_USER, "supervisorCardCode", "TEXT");
			createColumn(db, TABLE_DELIVERY, "safeDelivery", "TEXT");
			createColumn(db, TABLE_DELIVERY_VIN, "do_lotlocate", "TEXT");
			createColumn(db, TABLE_USER, "supervisorPreloadChk", "INTEGER");
			createColumn(db, TABLE_LOAD, "preloadSupervisorSignedAt", "TEXT");
			createColumn(db, TABLE_LOAD, "preloadSupervisorSignature", "TEXT");
			createColumn(db, TABLE_LOAD, "preloadSupervisorSignatureLat", "TEXT");
			createColumn(db, TABLE_LOAD, "preloadSupervisorSignatureLon", "TEXT");

			oldVersion = 107;
		}

		if(oldVersion < 108 && newVersion >= 108) {
			createColumn(db, TABLE_TERMINAL, "phoneNumber", "TEXT");
			createColumn(db, TABLE_TERMINAL, "canToUsPhoneNumber", "TEXT");
			createColumn(db, TABLE_TERMINAL, "usToCanPhoneNumber", "TEXT");
			createColumn(db, TABLE_TERMINAL, "countryCode", "TEXT");
			createColumn(db, TABLE_DEALER, "countryCode", "TEXT");
			createColumn(db, TABLE_LOAD, "trailerNumber", "TEXT");

		    buildTrainingTypeTable(db);
		    buildTrainingRequirementTable(db);

		    oldVersion = 108;
        }

		if(oldVersion < 109 && newVersion >= 109) {
		    createColumn(db, TABLE_TRAINING_REQUIREMENT, "vin", "TEXT");
		    createColumn(db, TABLE_TRAINING_REQUIREMENT, "started", "TEXT");
		    createColumn(db, TABLE_TRAINING_REQUIREMENT, "supplementalReference", "TEXT");
		    createColumn(db, TABLE_TRAINING_REQUIREMENT, "supplementalData", "TEXT");

		    createColumn(db, TABLE_TRAINING_REQUIREMENT, "startedLatitude", "REAL");
            createColumn(db, TABLE_TRAINING_REQUIREMENT, "startedLongitude", "REAL");
            createColumn(db, TABLE_TRAINING_REQUIREMENT, "completedLatitude", "REAL");
            createColumn(db, TABLE_TRAINING_REQUIREMENT, "completedLongitude", "REAL");

            createColumn(db, TABLE_TRAINING_REQUIREMENT, "adHoc", "INTEGER");

			oldVersion = 109;
        }

		if(oldVersion < 110 && newVersion >= 110) {
			createColumn(db, TABLE_INSPECTION, "latitude", "REAL");
			createColumn(db, TABLE_INSPECTION, "longitude", "REAL");
			createColumn(db, TABLE_INSPECTION, "timestamp", "DATETIME");
			createColumn(db, TABLE_PROBLEM_REPORT, "category", "TEXT");

			createColumn(db, TABLE_USER, "helpTerm", "INTEGER");
            createColumn(db, TABLE_USER, "requiresAudit", "INTEGER");
			createColumn(db, TABLE_USER, "driverLicenseExpiration", "TEXT");
			createColumn(db, TABLE_TERMINAL, "dispatchPhoneNumber", "TEXT");
			createColumn(db, TABLE_USER, "medicalCertificateExpiration", "TEXT");

            buildTrendingAlertTable(db);

			oldVersion = 110;
		}

		if(oldVersion < 111 && newVersion >= 111) {
			createColumn(db, TABLE_USER, "requiresAudit", "INTEGER");

			oldVersion = 111;
		}

		if(oldVersion < 112 && newVersion >= 112) {
			db.execSQL("ALTER TABLE " + TABLE_DEALER + " ADD COLUMN alwaysUnattended INTEGER NOT NULL DEFAULT 0");
			db.execSQL("ALTER TABLE " + TABLE_DEALER + " ADD COLUMN photosOnUnattended INTEGER NOT NULL DEFAULT 0");

			oldVersion = 112;
		}

		if(oldVersion < 113 && newVersion >= 113) {
			db.execSQL("UPDATE " + TABLE_DELIVERY + " SET sti = 0 WHERE sti = 1 AND afrhrs = 1");
			oldVersion = 113;
		}

		if(oldVersion < 114 && newVersion >= 114) {
			createColumn(db, TABLE_DRIVER_ACTIONS, "received", "TEXT");
			oldVersion = 114;
		}

		if(oldVersion < 115 && newVersion >= 115) {
			buildWMRequestsTable(db);
			oldVersion = 115; 
		}

		if(oldVersion < 116 && newVersion >= 116) {
			createColumn(db, TABLE_LOAD, "supervisorSignedAt", "TEXT");
			createColumn(db, TABLE_LOAD, "supervisorSignatureLat", "TEXT");
			createColumn(db, TABLE_LOAD, "supervisorSignatureLon", "TEXT");
			buildQuestionnaireTable(db);
			oldVersion = 116;
		}
		if(oldVersion < 117 && newVersion >= 117) {
			createColumn(db, TABLE_LOAD,"pickSheetImageRequired", "INTEGER NOT NULL DEFAULT 1");
			createColumn(db, TABLE_LOAD,"extraDocImageRequired", "TEXT NOT NULL DEFAULT \'\'");
			createColumn(db, TABLE_USER,"autoInspectLastDelivery", "INTEGER NOT NULL DEFAULT 0");
			oldVersion = 117;
		}
		if(oldVersion < 118 && newVersion >= 118) {
			createColumn(db, TABLE_DELIVERY_VIN,"finalMfg", "TEXT");
			createColumn(db, TABLE_DELIVERY_VIN,"finalDealer", "TEXT");
			createColumn(db, TABLE_DEALER,"updated_fields", "TEXT NOT NULL DEFAULT \'\'");
			createColumn(db, TABLE_DEALER, "lot_code_id", "INTEGER NOT NULL DEFAULT -1");
			// We SHOULD delete the terminal_id field from the dealer table here; however, the
			// SQLite "UPDATE TABLE ... DROP COLUMN" statement fails. - PDK
			oldVersion = 118;
		}
		if(oldVersion < 119 && newVersion >= 119) {
			// To indicate a hidden load, we now set driver_id to 0 instead of -1.
			db.execSQL("UPDATE " + TABLE_LOAD + " SET driver_id = 0 WHERE driver_id = -1");
			oldVersion = 119;
		}
		if(oldVersion < 120 && newVersion >= 120) {
			createColumn(db, TABLE_IMAGE, "retries", "INTEGER NOT NULL DEFAULT 0");
			oldVersion = 120;
		}
	}


	private void createColumn(SQLiteDatabase inDatabase, String table, String column, String type) {
		if(!existsColumnInTable(inDatabase, table, column)) {
			try {
				inDatabase.execSQL("ALTER TABLE " + table + " ADD COLUMN " + column + " " + type);
			} catch (SQLiteException ex) {
				if(ex.getMessage().contains("duplicate column name")) {
					//do nothing
					log.debug(Logs.DEBUG, "Didn't add the new " + column + "  column to the the " + table + " table since it was already there (despite empty table)...");
				} else {
					ex.printStackTrace();
				}
			}
		} else {
			log.debug(Logs.DEBUG, "Didn't add the new " + column + "  column to the the " + table + " table since it was already there...");
		}
	}


	private boolean existsColumnInTable(SQLiteDatabase inDatabase, String inTable, String columnToCheck) {
		Cursor cursor = null;
		try {
			// Query 1 row
			cursor = inDatabase.rawQuery("SELECT * FROM " + inTable + " LIMIT 1", null);

			log.debug(Logs.DEBUG, "checking " + inTable + " for column " + columnToCheck);

			if (cursor != null)
				cursor.moveToFirst();

			// getColumnIndex() gives us the index (0 to ...) of the column - otherwise we get a -1
			if (cursor.getColumnIndex(columnToCheck) != -1) {
				log.debug(Logs.DEBUG, "found " + inTable + " column " + columnToCheck);

				return true;
			} else {
				log.debug(Logs.DEBUG, "did not find " + inTable + " column " + columnToCheck);
				return false;
			}

		} catch (Exception Exp) {
			// Something went wrong. Missing the database? The table?
			log.debug(Logs.DEBUG, "When checking whether a column exists in the table, an error occurred: " + Exp.getMessage());
			return true;
		} finally {
			if (cursor != null) cursor.close();
		}
	}

}

