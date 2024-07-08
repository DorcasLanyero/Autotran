package com.cassens.autotran.activities;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.core.content.FileProvider;
import androidx.gridlayout.widget.GridLayout;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.adapters.DeliveryDealerExpandableAdapter;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.DamageHelper;
import com.cassens.autotran.data.model.DamageNoteTemplate;
import com.cassens.autotran.data.model.Dealer;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.TrendingAlert;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.lookup.AreaCode;
import com.cassens.autotran.data.model.lookup.SpecialCode;
import com.cassens.autotran.data.model.lookup.Terminal;
import com.cassens.autotran.data.model.lookup.TrainingType;
import com.cassens.autotran.data.model.lookup.TypeCode;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.dialogs.ImageViewDialog;
import com.cassens.autotran.handlers.ImageHandler;
import com.cassens.autotran.handlers.LocationHandler;
import com.cassens.autotran.views.DrivenBackedButton;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.NoNullsArrayList;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

/**
 * Project : AUTOTRAN Description : VINInspectionActivity class record results of inspection
 *
 * @author Hemant Creation Date : 12-11-2013
 */
@SuppressLint("SimpleDateFormat")
public class VINInspectionActivity extends NfcScanningActivity {
    private static final Logger log = LoggerFactory.getLogger(VINInspectionActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private final Activity currentActivity = this;
    private LocationHandler locationHandler;

    Button cancelButton, notesButton, saveButton, specialButton, rejectButton,
            cameraButton, addDmgButton;
    ImageView backButton;
    TextView vinTextView;
    TextView vinDescTextView;
    TextView loadPosTextView;
    DrivenBackedButton drivenOrBackButton;
    LinearLayout three_text_view_lay;
    private int position;
    private NoNullsArrayList<AreaTypeSvrty> areaTypeSvrties;
    private NoNullsArrayList<AreaTypeSvrty> startingAreaTypeSvrties;

    private NoNullsArrayList<Image> activeImages;
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
    private cameraController camera;

    private TrainingRequirement.ByStatus trainingRequirements;
    private TrainingRequirement activeTraining;
    private boolean allowTrainingToFinish = false;
    private AlertDialog finishTrainingDialog;

    private static String ATS_NO_VAL = "";
    private DeliveryVin deliveryVin;

    private static final boolean DEBUG = false;
    private static final int INSPECTION_CO_DAMAGES_ITEMIZE_MAX = 12;

    // must start new damage_ids at -2 so we know when something hasn't been touched
    // (damage_id == -1 == untouched)
    private int nextDamageId = -1;
    private HashSet<Image> garbageImages = new HashSet<>();
    private HashSet<Damage> garbageDamages = new HashSet<>();
    private Load mLoad;
    private Delivery mDelivery;
    private boolean mIsHighClaimsDriver;
    private ArrayList<DamageNoteTemplate> requiredDamageNoteTemplates;
    private boolean mIsDealerUnavailable = false;

    private ArrayList<TrendingAlert> alerts;

    private String autoInsertedPreloadNotes = null;

    private int getNextDamageId() {
        nextDamageId -= 1;
        return nextDamageId;
    }

    public static String formatATSValue(String val, boolean zeroPad) {
        String format;
        if (zeroPad) {
            format = "%2s";
        }
        else {
            format = "%s";
        }
        return HelperFuncs.noNull(String.format(format, val).replace(" ", "0"), "--");
    }

    public static String formatATSValue(String val) {
        return formatATSValue(val, true);
    }


        private boolean supervisorSignatureOnChanges;
    private String mCurrentPhotoFileName;
    private String mImageFileNamePrefix;

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
    private static final int REQ_CODE_POS = 1004;
    private static final int REQ_CODE_SPECIAL = 1005;
    private static final int REQ_CODE_NOTES = 1006;
    private static final int REQ_CODE_CAPTURE_IMAGE = 1007;
    private static final int REQ_CODE_REJECTION_VIN = 1008;
    private static final int REQ_CODE_SIGNATURE = 1009;

    private static final String TAG = "VINInspectionActivity";

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        locationHandler = LocationHandler.getInstance(this);
        locationHandler.startLocationTracking();

        setContentView(R.layout.activity_vin_inspection);

        llUp = findViewById(R.id.ll_up);
        llFirst = findViewById(R.id.ll_first);
        llMid = findViewById(R.id.ll_mid);
        llLast = findViewById(R.id.ll_last);
        cameraImageLayout = findViewById(R.id.cameraImagesLL);
        scrollView = findViewById(R.id.verticalScrollView);
        imgScrollView = findViewById(R.id.imageScrollView);

        cancelButton = findViewById(R.id.cancel);
        notesButton = findViewById(R.id.notes);
        saveButton = findViewById(R.id.save);

        addDmgButton = findViewById(R.id.add_damage);
        specialButton = findViewById(R.id.special);
        backButton = findViewById(R.id.back);

        cameraButton = findViewById(R.id.cameraButton);

        bundle = getIntent().getExtras();
        isEditable = bundle.getBoolean("is_editable", true);
        operation = bundle.getInt(Constants.CURRENT_OPERATION);

        boolean vinScanned = bundle.getBoolean("vin_scanned", false);

        loadPosTextView = findViewById(R.id.pos);
        drivenOrBackButton = findViewById(R.id.db);
        areaTypeSvrties = new NoNullsArrayList<>();
        startingAreaTypeSvrties = new NoNullsArrayList<>();
        activeImages = new NoNullsArrayList<>();

        notesButton.setClickable(true);
        reasonString = "";

        vinTextView = findViewById(R.id.id);
        vinTextView.setText(HelperFuncs.splitVin(bundle.getString("vin_number")));
        vinDescTextView = findViewById(R.id.vin_desc);
        vinDescTextView.setText(bundle.getString("vin_desc",""));

        three_text_view_lay = findViewById(R.id.three_text_view_lay);

        loadPosTextView.setOnClickListener(arg0 -> {
            Intent posIntent = new Intent(VINInspectionActivity.this,
                    DamageCodeEnter.class);
            posIntent.putExtra("check", "pos");

            if (loadPosTextView.getText().toString() != null
                    && loadPosTextView.getText().toString() != "")
                posIntent.putExtra("text", loadPosTextView.getText()
                        .toString());

            posIntent.putExtra("mode", "edit");

            ArrayList<String> positions = (ArrayList<String>) DeliveryDealerExpandableAdapter.getAvailablePositionStrings(getApplicationContext(), deliveryVin, false);
            log.debug(Logs.INTERACTION, "vin position dialog displayed. positions available: " + TextUtils.join("|", positions));
            posIntent.putStringArrayListExtra("positions", positions);

            startActivityForResult(posIntent, REQ_CODE_POS);
        });

        cameraButton.setText("Camera");

        long[] trainingRequirementIds = bundle.getLongArray(Constants.EXTRA_TRAINING_REQ_IDS);
        List<TrainingRequirement> reqs = DataManager.getTrainingRequirements(this, trainingRequirementIds);
        trainingRequirements = TrainingRequirement.filterList(reqs);

        if(trainingRequirements.unfinished.size() > 0) {
            ImageView titleBarIcon = findViewById(R.id.titleBarIcon);
            titleBarIcon.setImageDrawable(getResources().getDrawable(R.drawable.coaching_icon));
            titleBarIcon.setVisibility(View.VISIBLE);
        }
        else if(trainingRequirements.finished.size() > 0) {
            ImageView titleBarIcon = findViewById(R.id.titleBarIcon);
            titleBarIcon.setImageDrawable(getResources().getDrawable(R.drawable.coaching_complete_icon));
            titleBarIcon.setVisibility(View.VISIBLE);
        }

        log.debug(Logs.DEBUG,
                "VINInspectionActivity: vin_number="
                        + bundle.getString("vin_number"));
        log.debug(Logs.DEBUG,
                "VINInspectionActivity: delivery_id="
                        + bundle.getString("vin_select_key"));

        if(bundle.getString("vin_number") == null && bundle.getString("vin_select_key") == null) {
            // This was incorrectly started by a non-VIN NFC read, so finish now so that
            // GenericScanningActivity will unregister us

            log.warn(Logs.DEBUG, "VINInspectionActivity has null vin/delivery ID from non-VIN NFC tag. Closing activity.");

            finish();
            return;
        }

        deliveryVin = DataManager.getDeliveryVin(this,
                bundle.getInt("delivery_vin_id"));
        
        if (operation == Constants.PRELOAD_OPERATION) {
            Delivery delivery = DataManager.getDelivery(this, deliveryVin.delivery_id);
            boolean highClaims = (delivery.dealer != null) ? delivery.dealer.high_claims : false;
            findViewById(R.id.high_claims_warning).setVisibility(highClaims ? VISIBLE : GONE);
        }

        //mark the delivery vin as having been scanned or not...

        String scannedMessage = "L|";

        if(vinScanned) {
            scannedMessage = "S|";
        }

        if(deliveryVin.key == null) {
            deliveryVin.key = "";
        }

        if(operation == Constants.PRELOAD_OPERATION) {
            deliveryVin.key += "P," + scannedMessage;

            if (deliveryVin.damages != null) {
                for (Damage d : deliveryVin.damages) {
                    if (d.readonly && d.source.equals("driver")) {
                        showDialog(getResources().getString(R.string.damage_warning), false);
                        break;
                    }
                }
            }
        } else if(operation == Constants.DELIVERY_OPERATION) {
            deliveryVin.key += "D," + scannedMessage;
        }

        DataManager.insertDeliveryVinToLocalDB(this, deliveryVin);

        if (deliveryVin == null) {
            // If we don't have a valid vin, disable the controls.
            vinTextView.setText("Vin Not Found");
            vinTextView.setTextSize(22);
            vinTextView.setTextColor(getResources().getColor(R.color.lite_gray));
            llUp.setVisibility(GONE);
            llFirst.setVisibility(GONE);
            llMid.setVisibility(GONE);
            llLast.setVisibility(GONE);
            cancelButton.setVisibility(GONE);
            notesButton.setVisibility(GONE);
            saveButton.setVisibility(GONE);
        } else {
            bundle.putInt("delivery_vin_id", deliveryVin.delivery_vin_id);


            this.camera = new cameraController(operation);
            displayDeliveryVinData(true);
        }

        if (operation == Constants.DELIVERY_OPERATION) {
            notesButton.setVisibility(GONE);
        }

        User driver = DataManager.getUserForDriverNumber(this, CommonUtility.getDriverNumber(this));
        mIsHighClaimsDriver = (driver == null) ? false : (driver.highClaims != 0);

        mDelivery = DataManager.getDelivery(this,deliveryVin.delivery_id);
        if(mDelivery != null) {
            this.mLoad = DataManager.getLoad(this, mDelivery.load_id);
            if (operation == Constants.DELIVERY_OPERATION && mDelivery.isDealerUnavailable()) {
                mIsDealerUnavailable = true;
                camera.invalidateQueue();
                if (mIsDealerUnavailable) {
                    camera.getNextRequiredImage(getCurrentFocus());
                }
            }
        }

        refreshSupervisorSignatureButton();

        scrollView.post(scrollDown);
        imgScrollView.post(scrollImagesLeft);

        alerts = DataManager.getTrendingAlertsForVin(this, mLoad, deliveryVin.vin);
        Collections.sort(alerts);
        if (operation != Constants.DELIVERY_OPERATION && alerts.size() > 0) {
            findViewById(R.id.alerts).setVisibility(View.VISIBLE);
            findViewById(R.id.invisiblePlaceholderForCentering).setVisibility(View.GONE);
            HelperFuncs.showAlerts(alerts, VINInspectionActivity.this);
        }
    }

    @Override
    protected void onDestroy() {
        locationHandler.stopLocationTracking();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        imgScrollView.post(scrollImagesLeft);
        scrollView.post(scrollDown);

    }

    @Override
    protected void onScanResultRunOnUiThread(String scannedValue) {
        User supervisor = DataManager.getUserForSupervisorCode(this, scannedValue);

        if(operation != Constants.PRELOAD_OPERATION) {
            log.debug(Logs.DEBUG, "Attempted to start or finish training during a non-preload inspection");
            return;
        }

        if(supervisor == null) {
            log.debug(Logs.DEBUG, "Attempted to start or finish training with an invalid supervisor");

            if(activeTraining != null && allowTrainingToFinish) {
                finishTrainingDialog.setMessage(getResources().getString(R.string.coaching_complete_invalid_tag_message));
            }
            return;
        }

        if(activeTraining != null) {
            if(allowTrainingToFinish) {
                log.debug(Logs.DEBUG, "Supervisor tag detected after Done tapped. Training complete.");
                finishInspectionCoaching(supervisor);
            }
            else {
                log.debug(Logs.DEBUG, "Supervisor tag detected during training before Done tapped.");
                showDialog("Supervisor tag detected, but coaching is not complete. Finish the inspection, press Done, then scan the supervisor tag when prompted.");
            }
        }
        else if(trainingRequirements.unfinished.size() > 0) { // and active training is null
            for(TrainingRequirement r : trainingRequirements.unfinished) {
                if(activeTraining == null && r.type == TrainingType.SUPERVISOR_COACHING) {
                    activeTraining = r;
                }
                else {
                    log.warn(Logs.DEBUG, "Multiple trainings per load not yet supported");
                }
            }

            if(activeTraining != null) {
                log.debug(Logs.DEBUG, "Starting training " + activeTraining.id + " with supervisor " + supervisor.fullName);
                startInspectionCoaching(supervisor);
            }
            else {
                log.debug(Logs.DEBUG, "No training found for supervisor scan");
            }
        }
        else { // start an ad-hoc training
            // Ad-hoc training is started by bumping a supervisor tag. We insert it into the
            // DB
            TrainingRequirement r = new TrainingRequirement();
            r.adHoc = 1; // flag so that we upload it sans ID and let the server create one
            r.type = 1; // supervisor inspection coaching
            r.assigned = new Date();
            r.load_id = mLoad.load_remote_id;
            r.progress = 0;
            r.requiredProgress = 1;
            activeTraining = r;
            startInspectionCoaching(supervisor);
        }
    }

    private void startInspectionCoaching(User supervisor) {
        showDialog("Supervisor tag detected. Beginning supervisor coaching.");

        // Ad-hoc training is marked in the server DB by assigned == started,
        // so make sure that's true
        if(activeTraining.adHoc != 0) {
            activeTraining.started = activeTraining.assigned;
        }
        else {
            activeTraining.started = new Date();
        }

        Location l = locationHandler.getLocation();
        activeTraining.startedLatitude = l.getLatitude();
        activeTraining.startedLongitude = l.getLongitude();

        allowTrainingToFinish = false;
        ImageView titleBarIcon = findViewById(R.id.titleBarIcon);
        titleBarIcon.setVisibility(VISIBLE);
        titleBarIcon.setImageDrawable(getResources().getDrawable(R.drawable.coaching_progress_icon));
        activeTraining.supervisor_id = supervisor.user_remote_id;
    }

    private void finishInspectionCoaching(User supervisor) {
        if(supervisor.user_remote_id.equals(activeTraining.supervisor_id)) {
            String message = getResources().getString(R.string.coaching_complete_message);
            message += " " + supervisor.fullName;
            finishTrainingDialog.setMessage(message);
            finishTrainingDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(true);
        }
        else {
            finishTrainingDialog.setMessage(getResources().getString(R.string.coaching_complete_wrong_super_message));
        }
    }

    public void showAlerts(View view) {
        HelperFuncs.showAlerts(alerts, VINInspectionActivity.this);
    }

    private void refreshSupervisorSignatureButton() {
        Button supervisorSignatureButton = findViewById(R.id.supervisorButton);

        boolean shouldShow = false;

        if(operation == Constants.DELIVERY_OPERATION) {
            shouldShow = false;
        } else if(deliveryVin.wantsSupervisorSignature()) {
            shouldShow = true;
        }

        if(shouldShow) {
            supervisorSignatureButton.setVisibility(VISIBLE);
            // If both the supervisor signature and high claims warning are on the screen
            // hide the vehicle description to make room on the screen.
            if (findViewById(R.id.high_claims_warning).getVisibility() == VISIBLE) {
                vinDescTextView.setVisibility(GONE);
            }
            else {
                vinDescTextView.setVisibility(VISIBLE);
            }
        } else {
            supervisorSignatureButton.setVisibility(GONE);
            vinDescTextView.setVisibility(VISIBLE);
        }
    }

    public void noteClick(View v) {
        CommonUtility.logButtonClick(log, v);
        Intent intent = new Intent(VINInspectionActivity.this, NotesActivity.class);

        if (bundle.getInt(Constants.CURRENT_OPERATION) == Constants.DELIVERY_OPERATION) {
            intent.putExtra(NotesActivity.EXTRA_STATE, NotesActivity.DELIVERY_VIN_DAMAGE);
            intent.putExtra(NotesActivity.EXTRA_NOTES, deliveryVin.deliveryNotes);
        } else {
            intent.putExtra(NotesActivity.EXTRA_STATE, NotesActivity.PRELOAD_VIN_DAMAGE);
            intent.putExtra(NotesActivity.EXTRA_NOTES, stripAutoInsertedNotes(deliveryVin.preloadNotes));
            if (autoInsertedPreloadNotes != null) {
                // Reduce maximum note field length to accommodate inserted notes.
                intent.putExtra(NotesActivity.EXTRA_MAX_LENGTH, getResources().getInteger(R.integer.max_note_length) - autoInsertedPreloadNotes.length());
            }
        }
        Delivery delivery = DataManager.getDelivery(this, deliveryVin.delivery_id);
        if (delivery.dealer != null && delivery.dealer.mfg != null) {
            intent.putExtra(NotesActivity.EXTRA_MFG, delivery.dealer.mfg);
        }
        intent.putExtra(Constants.CURRENT_OPERATION, operation);
        intent.putExtra(NotesActivity.EXTRA_TITLE, "Notes");
        intent.putExtra(NotesActivity.EXTRA_PROMPT, "Type a note, or choose predefined notes");
        intent.putExtra(NotesActivity.EXTRA_IS_EDITABLE, isEditable);
        startActivityForResult(intent, REQ_CODE_NOTES);
    }

    public void cameraButtonClick(View v) {
        CommonUtility.logButtonClick(log, v);
        this.camera.getImage(v);
        imgScrollView.post(scrollImagesLeft);
        scrollView.post(scrollDown);
    }



    //Collect a supervisor signature for this delivery vin
    public void supervisorButtonClick(View v) {
        CommonUtility.logButtonClick(log, v);
        boolean hasSevere = false;
        if (hasIncompleteDamage()) {
            // Dialog will be displayed by hadIncompleteDamage();
            return;
        } //
        if (camera.getTotalImagesRequired() > 0) {
            log.debug(Logs.INTERACTION, "Supervisor signature disallowed because required pictures are missing");
            camera.getNextRequiredImage(v);
            return;
        }
        preserveDamagesAndImages(hasSevere);
        obtainSupervisorSignatureForDamage();
    }

    private void showDialog(String msg) {
        showDialog(msg, false);
    }

    private void showDialog(String msg, boolean cancelable) {
        Builder builder = new AlertDialog.Builder(VINInspectionActivity.this);
        //builder.setTitle("Notification");
        builder.setMessage(msg);
        builder.setPositiveButton("OK", (dialog, which) -> {
        });
        builder.setCancelable(cancelable);
        builder.create().show();
    }

    public void saveClick(View v) {
        CommonUtility.logButtonClick(log, v);
        saveInspection(v);
    }

    public void saveInspection(View v) {
        boolean severeDamages = false;

        log.debug(Logs.DEBUG, "ats.size()=" + areaTypeSvrties.size() + " imageCount =" + cameraImageLayout.getChildCount());

        if (camera.getTotalImagesRequired() > 0) {
            log.debug(Logs.INTERACTION, "Save disallowed because required pictures are missing");
            camera.getNextRequiredImage(v);
            return;
        }

        deliveryVin.position = (String) loadPosTextView.getText();
        deliveryVin.facing = drivenOrBackButton.getOrientation().toString();
        deliveryVin.backdrv = drivenOrBackButton.getOrientation().getValue();

        //Load position is required
        if (operation == Constants.PRELOAD_OPERATION && (deliveryVin.position == null || deliveryVin.position.trim().equals("") || deliveryVin.position.toLowerCase().equals("null"))) {
            log.debug(Logs.INTERACTION, "message shown: " + "Load position not recorded.  You must specify a load position between 1-13");
            showDialog("Load position not recorded.  You must specify a load position between 1-13");
            return;
        }

        if (hasIncompleteDamage()) {
            // Dialog will be displayed by hadIncompleteDamage();
            return;
        }

        severeDamages = preserveDamagesAndImages(severeDamages);

        log.debug(Logs.DEBUG, "severeDamages=" + severeDamages + " ");

        // If this is a preload or shuttle load, and damages list has been modified,
        // check to see if a supervisor signature is needed.
        if ((operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION)) {
            if (supervisorSignatureOnChanges) {
                if (damagesMatch(startingAreaTypeSvrties, areaTypeSvrties)) {
                    //The latest supervisor signature matches the collected damages
                    finishInspection();
                }
                else {
                    // This happens if: (1) at the time the inspection activity was displayed, there were  severe
                    // damages for which a supervisor's signature had already been collected, and (2) during this
                    // session the user changed the list of damages in any way (deleting, modifying, or adding damages).
                    // Under those conditions, we force the collection of the supervisor's signature again.
                    //prompt for user to optionally include a supervisor signature on these damages
                    Builder builder = new AlertDialog.Builder(VINInspectionActivity.this);
                    //builder.setTitle("Damages De");
                    builder.setMessage("You have edited damages for an inspection that had already been signed by a supervisor. If you save these changes, a new supervisor signature will be required.");
                    log.debug(Logs.INTERACTION, "Dialog: " + "You have edited damages for an inspection that had already been signed by a supervisor. If you save these changes, a new supervisor signature will be required.");
                    builder.setPositiveButton("Get Updated Supervisor Signature", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CommonUtility.logButtonClick(log, "Get Updated Supervisor Signature");
                            obtainSupervisorSignatureForDamage();
                        }
                    });
                    builder.setNegativeButton("Later", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            CommonUtility.logButtonClick(log, "Later");
                            // set the flag to indicate that there are damages which are not signed for
                            deliveryVin.setWantsSupervisorSignature(true);
                            finishInspection();
                        }
                    });
                    builder.create().show();
                }
                // ALL damages prompt the option supervisor signature popup
            } else if (areaTypeSvrties.size() > 0 && !damagesMatch(startingAreaTypeSvrties, areaTypeSvrties) && !deliveryVin.ats.isEmpty()) {
                //prompt for user to optionally include a supervisor signature on these damages
                Builder builder = new AlertDialog.Builder(VINInspectionActivity.this);
                //builder.setTitle("Damages De");
                builder.setMessage("Damages have been recorded or modified for this vehicle. Do you require a supervisor signature?");
                log.debug(Logs.INTERACTION, "Dialog: " + "Damages have been recorded or modified for this vehicle. Do you require a supervisor signature?");
                builder.setPositiveButton("Yes", (dialog, which) -> {
                    CommonUtility.logButtonClick(log, "Yes");
                    obtainSupervisorSignatureForDamage();
                });
                builder.setNegativeButton("No / Later", (dialog, which) -> {
                    CommonUtility.logButtonClick(log, "No / Later");
                    // set the flag to indicate that there are damages which are not signed for
                    deliveryVin.setWantsSupervisorSignature(true);
                    finishInspection();
                });
                builder.create().show();
            }
            //Note: we aren't forcing this signature until the end of the preload
            /*else if (severeDamages) {
                //Check for the supervisor signature as well and only forward on if it has not been saved
                // On a load damage, get supervisor signature after each vehicle damage if
                // severity on any damage record is > 2
                obtainSupervisorSignatureForDamage();
            }*/
            else {
                finishInspection();
            }
        } else {
            if (areaTypeSvrties.size() > 0) {
                //compare delivery damage list against preload
                final ArrayList<DamageHelper> deliveryDamages = new ArrayList<>();
                ArrayList<DamageHelper> preloadDamages = new ArrayList<>();
                String newDamages = "";
                for (Damage damage : deliveryVin.damages) {
                    if (!damage.areaCodeIsSet() || !damage.typeCodeIsSet() || !damage.severityCodeIsSet()) {
                        continue;
                    }
                    if (damage.preLoadDamage) {
                        preloadDamages.add(new DamageHelper(damage));
                    } else {
                        deliveryDamages.add(new DamageHelper(damage));
                    }
                }

                if(mLoad.parentLoad) {
                    //this is a parent load, get damages from preload on the child load
                    DeliveryVin childDeliveryVin = DataManager.getChildLoadDeliveryVin(this, deliveryVin.vin_id);
                    if (childDeliveryVin != null) {
                        for (Damage damage : childDeliveryVin.damages) {
                            preloadDamages.add(new DamageHelper(damage));
                        }
                    }
                    else {
                        log.debug(Logs.DEBUG, "Database integrity issue: Unable to retrieve child vin info for parent load.");
                    }
                }

                boolean callInRequired = false;
                for (DamageHelper damageHelper : deliveryDamages) {
                    if(!preloadDamages.contains(damageHelper)) {
                        callInRequired = true;
                        if (!newDamages.equals("")) {
                            newDamages += ", ";
                        }
                        newDamages += damageHelper.toString();
                        continue;
                    }
                }

                if (callInRequired) {
                    //generate call-in popup
                    log.debug(Logs.INTERACTION, "showing administrative approval required popup");
                    String fourLetterCode = HelperFuncs.getFourLetterCode();
                    String phoneNumber = getClaimsPhoneNumber();
                    CommonUtility.promptForCallbackNumber(this,
                            new IPayNumberDialogCallback() {
                                @Override
                                public void complete() {
                                    log.debug(Logs.INTERACTION, "approval code entered successfully");
                                    mDelivery.notes += "approval code entered successfully";
                                    finishInspection();
                                }
                                @Override
                                public void cancelled() {
                                    //remove delivery damages from arraylist, since it's only filled when we hit Done
                                    for (Iterator<Damage> iterator = deliveryVin.damages.iterator(); iterator.hasNext(); ) {

                                        Damage damage = iterator.next();
                                        if (!damage.preLoadDamage && damage.damage_id <= 0 ) {
                                            iterator.remove();
                                        }
                                    }
                                }
                            },
                            "Please call Claims (" + phoneNumber + "). The following " +
                                    "damages may not have been covered on preload: ",
                            newDamages,
                            "Administrative approval is required to proceed.  Please call " +
                                    "Claims (" + phoneNumber + ") to receive an unlock code.",
                            "Call Claims Hotline",
                            fourLetterCode);

                    //load event for call-in popup
                    SimpleTimeStamp sts = new SimpleTimeStamp();
                    Location location = locationHandler.getLocation();
                    String eventString = TextUtils.join(",",
                            new String[]{
                                    "DMG-CALL",
                                    mLoad.driver.driverNumber,
                                    mLoad.loadNumber,
                                    deliveryVin.vin.vin_number,
                                    sts.getUtcDateTime(),
                                    sts.getUtcTimeZone(),
                                    String.valueOf(location.getLatitude()),
                                    String.valueOf(location.getLongitude()),
                                    newDamages
                            });
                    //be sure we're less than 512 characters
                    int maxLength = (eventString.length() < 512)?eventString.length():512;
                    eventString = eventString.substring(0, maxLength);

                    LoadEvent event = new LoadEvent();
                    event.csv = eventString;
                    DataManager.insertLoadEvent(getApplicationContext(), event);
                    SyncManager.pushLoadEventsLatched(getApplicationContext());

                } else {
                    finishInspection();
                }
            } else {
                finishInspection();
            }
        }
    }

    private String getClaimsPhoneNumber() {
        //default phone number
        String phoneNumber = "618-655-2788";
        Terminal originTerminal = DataManager.getTerminal(this, Integer.parseInt(mLoad.originTerminal));
        Dealer dealer = mDelivery.dealer;
        if (originTerminal != null) {
            if(dealer != null) {
                if (!HelperFuncs.isNullOrEmpty(originTerminal.countryCode) && !HelperFuncs.isNullOrEmpty(dealer.countryCode)) {
                    if (originTerminal.countryCode.equalsIgnoreCase("US") && dealer.countryCode.equalsIgnoreCase("CA") &&
                            !HelperFuncs.isNullOrEmpty(originTerminal.usToCanPhoneNumber)) {
                        return originTerminal.usToCanPhoneNumber;
                    } else if (originTerminal.countryCode.equalsIgnoreCase("CA") && dealer.countryCode.equalsIgnoreCase("US")
                            && !HelperFuncs.isNullOrEmpty(originTerminal.canToUsPhoneNumber)) {
                        return originTerminal.canToUsPhoneNumber;
                    }
                }
            }
            if (!HelperFuncs.isNullOrEmpty(originTerminal.phoneNumber)) {
                return originTerminal.phoneNumber;
            }
        }
        //just return default
        return phoneNumber;
    }

    private boolean hasIncompleteDamage() {
        for (AreaTypeSvrty areaTypeSvrty : areaTypeSvrties) {
            if(!areaTypeSvrty.isReadOnly()) {
                boolean hasBlank = areaTypeSvrty.getAreaString().equals(ATS_NO_VAL) ||
                        areaTypeSvrty.getTypeString().equals(ATS_NO_VAL) ||
                        areaTypeSvrty.getSvrtyString().equals(ATS_NO_VAL);
                boolean hasZero = areaTypeSvrty.getAreaString().equals("0") ||
                        areaTypeSvrty.getTypeString().equals("0") ||
                        areaTypeSvrty.getSvrtyString().equals("0");
                if (hasBlank || (hasZero && !areaTypeSvrty.isSpecial)) {
                    String msg;
                    if (hasZero) {
                        msg = "All non-special damages must have non-zero values for area, type, and severity.";
                    }
                    else {
                        msg = "All damages must have values for area, type, and severity.";
                    }
                    log.debug(Logs.INTERACTION, "message shown: " + msg);
                    showDialog(msg);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean preserveDamagesAndImages(boolean severeDamages) {
        for (AreaTypeSvrty areaTypeSvrty : areaTypeSvrties) {
            log.debug(Logs.DEBUG, "areaTypeSvrty='" + areaTypeSvrty.getSvrtyString() + "'");

            // BFF - This SHOULD have been implemented with damage objects attached to an adapter and
            //displayed in a nice list view.  Honestly, I don't know what they were thinking

            //This mechanism won't let us enter in special codes w/out some odd decoration of the data or something

            Damage damage = new Damage();
            boolean found = false;
            damage.damage_id = areaTypeSvrty.damage_id;

            damage.preLoadDamage = operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION;


            if (areaTypeSvrty.isSevereDamage() && !areaTypeSvrty.isReadOnly()) {
                severeDamages = true;
            }

            if (areaTypeSvrty.damage_id < 0) {
                if (areaTypeSvrty.isSpecial) {
                    damage.specialCode = DataManager.getSpecialCode(this, areaTypeSvrty.getAreaString(), areaTypeSvrty.getTypeString(), areaTypeSvrty.getSvrtyString());
                    damage.special_code_id = damage.specialCode.special_code_id;
                } else {
                    damage.areaCode = DataManager.getAreaCode(this, areaTypeSvrty.getAreaString());
                    damage.typeCode = DataManager.getTypeCode(this, areaTypeSvrty.getTypeString());
                    damage.severityCode = DataManager.getSeverityCode(this, areaTypeSvrty.getSvrtyString());

                    damage.area_code_id = damage.areaCode.area_code_id;
                    damage.type_code_id = damage.typeCode.type_code_id;
                    damage.svrty_code_id = damage.severityCode.severity_code_id;
                }
                deliveryVin.damages.add(damage);
            } else {
                for (int damageIndex = 0; damageIndex < deliveryVin.damages.size(); damageIndex++) {
                    if (deliveryVin.damages.get(damageIndex).damage_id == areaTypeSvrty.damage_id) {
                        Damage tmpDamage = deliveryVin.damages.get(damageIndex);
                        if (areaTypeSvrty.isSpecial()) {
                            tmpDamage.specialCode = DataManager.getSpecialCode(this, areaTypeSvrty.getAreaString(), areaTypeSvrty.getTypeString(), areaTypeSvrty.getSvrtyString());
                            tmpDamage.special_code_id = tmpDamage.specialCode.special_code_id;
                        } else if(!tmpDamage.readonly){
                            tmpDamage.areaCode = DataManager.getAreaCode(this, areaTypeSvrty.getAreaString());
                            tmpDamage.typeCode = DataManager.getTypeCode(this, areaTypeSvrty.getTypeString());
                            tmpDamage.severityCode = DataManager.getSeverityCode(this, areaTypeSvrty.getSvrtyString());

                            tmpDamage.area_code_id = tmpDamage.areaCode.area_code_id;
                            tmpDamage.type_code_id = tmpDamage.typeCode.type_code_id;
                            tmpDamage.svrty_code_id = tmpDamage.severityCode.severity_code_id;
                        }
                        deliveryVin.damages.set(damageIndex, tmpDamage);
                    }
                }
            }
        }

        for (int i = 0; i < cameraImageLayout.getChildCount(); i++) {
            ImageView imageView = (ImageView) cameraImageLayout.getChildAt(i);
            Image image = (Image) imageView.getTag();

            deliveryVin.images.add(0, image);
        }
        return severeDamages;
    }

    private void saveDeliveryVinChangesToLocalDB() {

        DataManager.insertDeliveryVinToLocalDB(this, deliveryVin, false);
        for (Image i : garbageImages) {
            DataManager.deleteImage(getApplicationContext(), i, "delete during VIN inspection");
        }
        garbageImages.clear();

        if(operation == Constants.PRELOAD_OPERATION && !garbageDamages.isEmpty()) {
            // Remove pick sheet and extra doc images, if present
            DataManager.deleteOldPickSheetAndExtraImages(this, mLoad.load_remote_id, mLoad.docTypeToTag(mLoad.extraDocImageRequired));
        }
        for (Damage i : garbageDamages) {
            DataManager.deleteDamage(getApplicationContext(), i);
        }
        garbageDamages.clear();
    }

    private void finishInspection() {
        if (operation == Constants.DELIVERY_OPERATION) {
            deliveryVin.inspectedDelivery = true;
        } else {
            deliveryVin.inspectedPreload = true;
        }
        log.debug(Logs.DEBUG, "Saving deliveryVin record");

        saveDeliveryVinChangesToLocalDB();

        insertInspectionLoadEvent();

        checkForDamageNotesAndFinish();
    }

    private void insertInspectionLoadEvent() {
        String damages = "";
        for (Damage d : deliveryVin.damages) {
            if ((d.preLoadDamage && operation == Constants.PRELOAD_OPERATION)||
                    (!d.preLoadDamage && operation == Constants.DELIVERY_OPERATION)
                    ){
                damages += TextUtils.join("|",
                        new String[]{
                                d.getAreaCode(),
                                d.getTypeCode(),
                                d.getSeverityCode(),
                        });
                damages += ";";
            }
        }

        SimpleTimeStamp sts = new SimpleTimeStamp();
        Location location = locationHandler.getLocation();
        Date now = new Date();

        String eventString = TextUtils.join(",",
                new String[]{
                        operation == Constants.PRELOAD_OPERATION ? "PL-V" : "D-V",
                        mLoad.driver.driverNumber,
                        mLoad.loadNumber,
                        deliveryVin.vin.vin_number,
                        deliveryVin.position,
                        deliveryVin.backdrv,
                        sts.getUtcDateTime(),
                        sts.getUtcTimeZone(),
                        String.valueOf(location.getLatitude()),
                        String.valueOf(location.getLongitude()),
                        damages
                });
        LoadEvent dvEvent = new LoadEvent();
        dvEvent.csv = eventString;
        DataManager.insertLoadEvent(this, dvEvent);
        SyncManager.pushLoadEventsLatched(getApplicationContext());
    }

    private void checkForDamageNotesAndFinish() {
        //Now that damages have been saved, we need to check for required damage note templates
        //only check for deliveries right now...
        if(operation == Constants.DELIVERY_OPERATION) {
            for (Damage damage : deliveryVin.damages) {

                if(!damage.preLoadDamage) {
                    String originTerminal = DataManager.getOriginTerminalForLoad(getApplicationContext(), mDelivery.load_id);

                    requiredDamageNoteTemplates = DataManager.getRequiredDamageNoteTemplates(getApplicationContext(),
                            (operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION),
                            false, damage, originTerminal, (mDelivery.dealer != null) ? mDelivery.dealer.mfg : null);

                    if (requiredDamageNoteTemplates != null && requiredDamageNoteTemplates.size() > 0) {

                        log.debug(Logs.INTERACTION, "Getting required damage notes for " + damage.getAreaCode() + " " + damage.getTypeCode() + " " + damage.getSeverityCode());

                        final DamageNoteTemplate next = requiredDamageNoteTemplates.remove(0); // pop first element off required damage note templates

                        damage.collectDamageNoteForTemplate(this, next, () -> {
                            log.debug(Logs.INTERACTION, "Collected damage note, checking for additional required notes");
                            checkForDamageNotesAndFinish();
                        }, true, deliveryVin);
                        return;
                    }
                }
            }
        }

        if(activeTraining != null && operation == Constants.PRELOAD_OPERATION) {
            allowTrainingToFinish = true;

            // require a second supervisor tap to finish
            AlertDialog.Builder d = new AlertDialog.Builder(this);
            d.setTitle("Coaching Complete");
            d.setMessage(R.string.coaching_complete_message);
            d.setCancelable(true);
            d.setPositiveButton("Finish", (dialog, which) -> {
                dialog.dismiss();
                Intent returnIntent = new Intent();
                setResult(RESULT_OK, returnIntent);

                activeTraining.progress = activeTraining.requiredProgress;
                activeTraining.completed = new Date();

                activeTraining.vin = deliveryVin.vin.vin_number;

                Location l = locationHandler.getLocation();
                activeTraining.completedLatitude = l.getLatitude();
                activeTraining.completedLongitude = l.getLongitude();

                StringBuilder damageString = new StringBuilder();
                for(Damage damage : deliveryVin.damages) {
                    damageString.append(damage.toString()).append(" ");
                }
                activeTraining.supplementalData = damageString.toString();

                saveActiveTraining();

                finish();
            });
            d.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    allowTrainingToFinish = false;
                }
            });

            finishTrainingDialog = d.create();
            finishTrainingDialog.show();
            finishTrainingDialog.getButton(AlertDialog.BUTTON_POSITIVE).setEnabled(false);
        }
        else {
            Intent returnIntent = new Intent();
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    }

    private void saveActiveTraining() { //??
        if(activeTraining.adHoc != 0) {
            DataManager.insertTrainingRequirementToLocalDB(this, activeTraining);
        }
        else {
            DataManager.updateTrainingRequirement(this, activeTraining);
        }
        SyncManager.uploadTrainingRequirement(this, activeTraining);
        activeTraining = null;
    }

    private void obtainSupervisorSignatureForDamage() {
        bundle.putString("user_type", "supervisor");
        bundle.putInt("delivery_vin_id", deliveryVin.delivery_vin_id);

        // In some cases, an inspection is required to obtain the signature a second time,
        // so reset the supervisorSignature to null to invalidate the old signature.
        deliveryVin.supervisorSignature = null;
        //log.debug(Logs.DEBUG, "deliveryVin has " + deliveryVin.damages.size() + " damages");
        saveDeliveryVinChangesToLocalDB();
        deliveryVin = DataManager.getDeliveryVin(this, deliveryVin.delivery_vin_id);
        displayDeliveryVinData(false);
        Intent intent = new Intent(this, SignatureActivity.class).putExtras(bundle);
        this.startActivityForResult(intent, REQ_CODE_SIGNATURE);
    }

    public void cancelClick(View v) {
        // For now, we're hiding the cancel button.  May put this back in later, so leaving code in for now.
        CommonUtility.logButtonClick(log, v);
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
        if (operation == Constants.DELIVERY_OPERATION) {
            if (three_text_view_lay.getChildCount() == 0) {
                Terminal terminal = DataManager.getTerminal(this, Integer.parseInt(mLoad.originTerminal));
                String message = "Call Edwardsville (618-655-2788) to report damages on delivery.";
                if (terminal != null && !HelperFuncs.isNullOrWhitespace(terminal.popupMessage)) {
                    message = terminal.popupMessage;
                }
                Builder builder = new AlertDialog.Builder(VINInspectionActivity.this);
                builder.setMessage(message);
                log.debug(Logs.INTERACTION, "Dialog: " + message);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    log.debug(Logs.INTERACTION, "Call terminal dialog, user clicked 'OK'");
                    addSpecial();
                });
                builder.setCancelable(false);
                builder.create().show();
            } else {
                addSpecial();
            }
        } else {
            addSpecial();
        }
    }

    public void addSpecial() {

        position = this.three_text_view_lay.getChildCount() - 1;

        startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeListSecond.class).putExtra("check", "special")
                .putExtra("mode", "add")), REQ_CODE_SPECIAL);
    }

    public void posClick(View v) {
        Intent positionIntent = (new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "pos"));

        ArrayList<String> positions = (ArrayList<String>) DeliveryDealerExpandableAdapter.getAvailablePositionStrings(this, deliveryVin, false);
        log.debug(Logs.INTERACTION, "vin position dialog displayed. positions available: " + TextUtils.join("|", positions));
        positionIntent.putStringArrayListExtra("positions", positions);
        startActivityForResult(positionIntent, REQ_CODE_POS);
    }

    public void back(final View v) {
        CommonUtility.logButtonClick(log, "Back");

        if(isEditable) {
            String message = "Do you wish to save any current changes, or discard and lose unsaved progress?";

            if(activeTraining != null) {
                message = "Do you wish to leave this screen, lose unsaved progress, and cancel supervisor coaching?";
            }

            Builder builder = new AlertDialog.Builder(VINInspectionActivity.this);
            //builder.setTitle("Notification");
            builder.setMessage(message);
            log.debug(Logs.INTERACTION, "Dialog: " + message);
            builder.setPositiveButton("Save", (dialog, which) -> {
                CommonUtility.logButtonClick(log, "Save");
                saveInspection(v);
            });

            builder.setNegativeButton("Discard", (dialogInterface, i) -> {
                CommonUtility.logButtonClick(log, "Discard", "Leaving inspection without saving" );
                //Delete cached image files for any images created that weren't saved
                discardChangesAndQuit();
            });

            builder.setNeutralButton("Cancel", (dialogInterface, i) -> {
                CommonUtility.logButtonClick(log, "Cancel");
                //Delete cached image files for any images created that weren't saved
            });
            //builder.setCancelable(true);
            builder.create().show();
        } else {
            discardChangesAndQuit();
        }
    }

    private void discardChangesAndQuit() {
        for (Image i : activeImages) {
            if (i.image_id < 0 && i.filename != null) {
                CommonUtility.deleteCachedImageFile(getApplicationContext(), i.filename + "_hires");
                CommonUtility.deleteCachedImageFile(getApplicationContext(), i.filename);
            }
        }
        VINInspectionActivity.this.finish();
    }

    @Override
    public void onBackPressed() {
        back(null);
    }

    public void areaClick(View v) {
        CommonUtility.logButtonClick(log, v);
        if (operation == Constants.DELIVERY_OPERATION) {
            if (three_text_view_lay.getChildCount() == 0) {
                String tmp = DataManager.getOriginTerminalForLoad(this, mLoad.load_id);

                String message = "Call Edwardsville (618-655-2788) to report damages on delivery.";

                //Optionally pull a message from the current terminal based on the origin term value
                if(tmp != null) {
                    Terminal terminal = DataManager.getTerminal(this, Integer.parseInt(tmp));
                    if (terminal != null && !HelperFuncs.isNullOrWhitespace(terminal.popupMessage)) {
                        message = terminal.popupMessage;
                    }
                }
                Builder builder = new AlertDialog.Builder(VINInspectionActivity.this);
                builder.setMessage(message);
                log.debug(Logs.INTERACTION, "Dialog: " + message);
                builder.setPositiveButton("OK", (dialog, which) -> {
                    log.debug(Logs.INTERACTION, "Call terminal dialog, user clicked 'OK'");
                    addATS();
                });
                builder.setCancelable(false);
                builder.create().show();
            } else {
                addATS();
            }
        } else {
            addATS();
        }
    }

    public void addATS() {

        addOneMoreATS(three_text_view_lay);

        position = this.three_text_view_lay.getChildCount() - 1;
        TextView area = VINInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.one);
        if (area.getText().equals(ATS_NO_VAL)) {
            startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
                    .putExtra("mode", "add")).putExtra("text", area.getText().toString()), REQ_CODE_AREA);
        }
    }

    private String getCodeFromView(TextView v) {
        if (v.getText().length() > 0 && v.getText().charAt(0) == '0') {
            return v.getText().toString().substring(1);
        }
        return v.getText().toString();
    }

    private void addOneMoreATS(LinearLayout three_text_view_lay) {
        final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout inflate_layout = (LinearLayout) inflater.inflate(R.layout.three_text_view, three_text_view_lay, false);
        //inflate_layout.setBackgroundColor(Color.RED);

        final TextView one = inflate_layout.findViewById(R.id.one);
        one.setTextColor(Color.RED);
        final TextView two = inflate_layout.findViewById(R.id.two);
        two.setTextColor(Color.RED);
        final TextView three = inflate_layout.findViewById(R.id.three);
        three.setTextColor(Color.RED);
        final Button delete = inflate_layout.findViewById(R.id.deleteButton);
        one.setText(ATS_NO_VAL);
        two.setText(ATS_NO_VAL);
        three.setText(ATS_NO_VAL);
        System.out.println("Child total : " + this.three_text_view_lay.getChildCount());
        int textPosition = three_text_view_lay.getChildCount();
        one.setTag(0 + "," + textPosition);
        two.setTag(0 + "," + textPosition);
        three.setTag(0 + "," + textPosition);
        delete.setTag(0 + "," + textPosition);

        if (this.three_text_view_lay.getChildCount() == 0) {
            three_text_view_lay.addView(inflate_layout);
        }


        one.setOnClickListener(v -> {
            log.info(Logs.DEBUG, "tag:" + v.getTag().toString());
            position = Integer.parseInt(v.getTag().toString().split(",")[1]);
            System.out.println("One position : " + position);
            if (v.getTag().toString().split(",").length == 3) {
                startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
                        .putExtra("mode", "edit")).putExtra("text", getCodeFromView(one)), REQ_CODE_AREA);
            } else {
                startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
                        .putExtra("mode", "edit")).putExtra("text", getCodeFromView(one)), REQ_CODE_AREA);
            }
        });

        two.setOnClickListener(v -> {
            position = Integer.parseInt(v.getTag().toString().split(",")[1]);
            System.out.println("Two position : " + position);
            if (v.getTag().toString().split(",").length == 3) {
                startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
                        .putExtra("mode", "edit")).putExtra("text", getCodeFromView(two)), REQ_CODE_TYPE);
            } else {
                startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
                        .putExtra("mode", "edit")).putExtra("text", getCodeFromView(two)), REQ_CODE_TYPE);
            }
        });

        three.setOnClickListener(v -> {
            position = Integer.parseInt(v.getTag().toString().split(",")[1]);
            System.out.println("Three position : " + position);
            if (v.getTag().toString().split(",").length == 3) {
                startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
                        .putExtra("mode", "edit")).putExtra("text", getCodeFromView(three)), REQ_CODE_SVRTY);
            } else {
                startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
                        .putExtra("mode", "edit")).putExtra("text", getCodeFromView(three)), REQ_CODE_SVRTY);
            }
        });

        delete.setOnClickListener(new deleteClickListener(one, two, three, delete));

        for (int position = 0; position < VINInspectionActivity.this.three_text_view_lay.getChildCount(); position++) {
            if (position == this.three_text_view_lay.getChildCount() - 1) {
                TextView area = VINInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.one);
                TextView type = VINInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.two);
                TextView svrty = VINInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.three);
                System.out.println(area.getText().toString() + " :: " + type.getText().toString() + " :: " + svrty.getText().toString());
                if (atsIsIncomplete(area, type, svrty)) {
                    if (position > 0) {
                        CommonUtility.showText("Last entry was not completed");
                    }
                } else {
                    three_text_view_lay.addView(inflate_layout);
                }
                break;
            }
        }

        scrollView.post(scrollDown);
        //imgScrollView.post(scrollRight);

        if(operation == Constants.PRELOAD_OPERATION) {
            // Adding new damage so remove pick sheet and extra doc images, if present
            DataManager.deleteOldPickSheetAndExtraImages(this, mLoad.load_remote_id, mLoad.docTypeToTag(mLoad.extraDocImageRequired));
        }
    }

    private void specialDamageCodeEditPopup(String fieldName) {
        showDialog("This is a special damage code. " + fieldName + " cannot be modified.");
        log.debug(Logs.INTERACTION, "Dialog: " + "This is a special damage code. " + fieldName + " cannot be modified.");
    }


    private void inflateLayout(LinearLayout three_text_view_lay, boolean isSpecialDamage) {
        final LayoutInflater inflater = (LayoutInflater) getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        LinearLayout inflate_layout = (LinearLayout) inflater.inflate(R.layout.three_text_view, three_text_view_lay, false);
        final TextView one = inflate_layout.findViewById(R.id.one);
        final TextView two = inflate_layout.findViewById(R.id.two);
        final TextView three = inflate_layout.findViewById(R.id.three);
        final Button delete = inflate_layout.findViewById(R.id.deleteButton);
        one.setText(ATS_NO_VAL);
        two.setText(ATS_NO_VAL);
        three.setText(ATS_NO_VAL);

        if (three_text_view_lay.getChildCount() == 0) {
            one.setTag(0 + "," + 0);
            two.setTag(0 + "," + 0);
            three.setTag(0 + "," + 0);
            delete.setTag(0 + "," + 0);
        }

        if (isSpecialDamage) {
            one.setOnClickListener(v -> specialDamageCodeEditPopup("Area"));
            two.setOnClickListener(v -> specialDamageCodeEditPopup("Type"));
            three.setOnClickListener(v -> specialDamageCodeEditPopup("Severity"));
        }
        else {
            one.setOnClickListener(v -> {
                log.info(Logs.DEBUG, "tag:" + v.getTag().toString());
                position = Integer.parseInt(v.getTag().toString().split(",")[1]);
                System.out.println("One position : " + position);
                if (v.getTag().toString().split(",").length == 3) {
                    startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
                            .putExtra("mode", "edit")).putExtra("text", getCodeFromView(one)), REQ_CODE_AREA);
                } else {
                    startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "area")
                            .putExtra("mode", "edit")).putExtra("text", getCodeFromView(one)), REQ_CODE_AREA);
                }
            });

            two.setOnClickListener(v -> {
                position = Integer.parseInt(v.getTag().toString().split(",")[1]);
                System.out.println("Two position : " + position);
                if (v.getTag().toString().split(",").length == 3) {
                    startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
                            .putExtra("mode", "edit")).putExtra("text", getCodeFromView(two)), REQ_CODE_TYPE);
                } else {
                    startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
                            .putExtra("mode", "edit")).putExtra("text", getCodeFromView(two)), REQ_CODE_TYPE);
                }
            });

            three.setOnClickListener(v -> {
                position = Integer.parseInt(v.getTag().toString().split(",")[1]);
                System.out.println("Three position : " + position);
                if (v.getTag().toString().split(",").length == 3) {
                    startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
                            .putExtra("mode", "edit")).putExtra("text", getCodeFromView(three)), REQ_CODE_SVRTY);
                } else {
                    startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
                            .putExtra("mode", "edit")).putExtra("text", getCodeFromView(three)), REQ_CODE_SVRTY);
                }
            });
        }
        delete.setOnClickListener(new deleteClickListener(one, two, three, delete));

        three_text_view_lay.addView(inflate_layout);
    }

    private class deleteClickListener implements View.OnClickListener {

        TextView one, two, three, delete;

        public deleteClickListener(TextView one, TextView two, TextView three, TextView delete) {
            this.one = one;
            this.two = two;
            this.three = three;
            this.delete = delete;
        }

        @Override
        public void onClick(View v) {
            position = Integer.parseInt(v.getTag().toString().split(",")[1]);
            log.debug(Logs.INTERACTION, "Delete damage clicked: "
                     + String.format("area/type/severity = %s/%s/%s",
                        HelperFuncs.noNull(one.getText().toString(), "<blank>"),
                        HelperFuncs.noNull(two.getText().toString(), "<blank>"),
                        HelperFuncs.noNull(three.getText().toString(), "<blank>")));

            if (operation == Constants.DELIVERY_OPERATION) {
                showDriverPromptDeleteDialog();
            } else {
                showSimpleDeleteDialog();
            }
        }

        private void showDriverPromptDeleteDialog() {
            LayoutInflater inflater = VINInspectionActivity.this.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.dialog_tips, null);
            final Context ctx = VINInspectionActivity.this;


            Builder builder = new Builder(VINInspectionActivity.this);
            builder.setView(dialogView)
                    .setCancelable(false);

            TextView tip = dialogView.findViewById(R.id.tips_container);

            final CheckBox confirmation = dialogView.findViewById(R.id.tips_confirmation);
            final EditText driverId = dialogView.findViewById(R.id.tips_textbox);
            final Button doneButton = dialogView.findViewById(R.id.done_button);
            final Button cancelButton = dialogView.findViewById(R.id.cancel_button);

            ImageView signatureReview = dialogView.findViewById(R.id.signature_review);
            signatureReview.setVisibility(GONE);
            confirmation.setVisibility(GONE);
            tip.setVisibility(GONE);
            doneButton.setText("Yes, delete the damage");
            cancelButton.setVisibility(VISIBLE);

            final Dialog dialog;

            builder.setTitle(getResources().getString(R.string.delete_prompt));
            dialog = builder.create();

            final String currentUserId = CommonUtility.getDriverNumber(ctx);

            doneButton.setOnClickListener(v -> {
                if (!driverId.getText().toString().trim().equals(currentUserId)) {
                    dialogView.findViewById(R.id.tips_textbox_border).setBackgroundColor(Color.RED);
                    CommonUtility.showText(getString(R.string.error_wrong_driver_number));
                } else {
                    CommonUtility.logButtonClick(log, v, "Damage deleted");
                    deleteDamageRecord();
                    dialog.dismiss();
                }
            });

            cancelButton.setOnClickListener(v -> {
                CommonUtility.logButtonClick(log, v, "Delete damage confirmation");
                dialog.dismiss();});

            driverId.setOnEditorActionListener((v, actionId, event) -> {
                if (event != null) {
                    doneButton.callOnClick();
                    return true;
                } else {
                    return false;
                }
            });
            dialog.show();
        }

        private void showSimpleDeleteDialog() {
            Builder builder = new Builder(VINInspectionActivity.this);
            builder.setTitle("Delete ?");
            builder.setMessage("Would you like to delete this damage record?");
            log.debug(Logs.INTERACTION, "Dialog: " + "Would you like to delete this damage record?");
            DialogInterface.OnClickListener dialogListener = (dialog, which) -> {
                log.debug(Logs.INTERACTION, "Delete damage record dialog, user clicked 'Yes'");
                deleteDamageRecord();
            };
            builder.setPositiveButton("Yes", dialogListener);
            builder.setNegativeButton("No", (dialog, which) -> {
                log.debug(Logs.INTERACTION, "Delete damage record dialog, user clicked 'No'");
                dialog.dismiss();
            });
            builder.create().show();
        }

        private void deleteDamageRecord() {
            View deletedRow = VINInspectionActivity.this.three_text_view_lay.getChildAt(position);

            if (areaTypeSvrties.size() > position) {
                AreaTypeSvrty ats = areaTypeSvrties.remove(position);
                camera.removeDmgImages(ats);
                ats.dumpToLog("Removed");

                SimpleTimeStamp sts = new SimpleTimeStamp();
                Location location = locationHandler.getLocation();
                HashMap<String,String> reqBody = new HashMap<>();
                String eventString = TextUtils.join(",",
                        new String[]{
                                "DMG-DEL",
                                mLoad.driver.driverNumber,
                                mLoad.loadNumber,
                                deliveryVin.vin.vin_number,
                                ats.areaString,ats.typeString,ats.svrtyString,
                                sts.getUtcDateTime(),
                                sts.getUtcTimeZone(),
                                String.valueOf(location.getLatitude()),
                                String.valueOf(location.getLongitude())
                        });

                LoadEvent event = new LoadEvent();
                event.csv = eventString;
                DataManager.insertLoadEvent(getApplicationContext(), event);
                SyncManager.pushLoadEventsLatched(getApplicationContext());

                //Delete codes from the database that have been saved.  This WILL delete duplicates
                //TODO: implement the damage widget so completely differently you can't even recognize it


                for (Iterator<Damage> iterator = deliveryVin.damages.iterator(); iterator.hasNext(); ) {
                    Damage damage = iterator.next();

                    if (ats.isSpecial) {
                        //We can check all of the codes, but only look at special codes if we know it'll be there...
                        if (damage.specialCode != null) {
                            if (damage.specialCode.getAreaCode().equals(ats.areaString) &&
                                    damage.specialCode.getTypeCode().equals(ats.typeString) &&
                                    damage.specialCode.getSeverityCode().equals(ats.svrtyString)) {
                                if (ats.damage_id >= 0) {
                                    garbageDamages.add(damage);
                                }
                                deliveryVin.damages.remove(damage);
                                break;
                            }
                        }
                    } else {
                        if (damage.areaCode != null && damage.typeCode != null && damage.severityCode != null
                                && damage.areaCode.getCode() != null && damage.typeCode.getCode() != null && damage.severityCode.getCode() != null
                                && ats.areaString != null && ats.typeString != null && ats.svrtyString != null)
                            if (    damage.areaCode.getCode().equals(ats.areaString) &&
                                    damage.typeCode.getCode().equals(ats.typeString) &&
                                    damage.severityCode.getCode().equals(ats.svrtyString)) {
                                //DataManager.deleteDamage(getApplicationContext(), damage);
                                if (ats.damage_id >= 0) {
                                    garbageDamages.add(damage);
                                }
                                iterator.remove();
                                //deliveryVin.damages.remove(damage);
                                break;
                            }
                    }
                }
            }

            VINInspectionActivity.this.three_text_view_lay.removeView(deletedRow);
            System.out.println("After Delete One Tag :" + one.getTag().toString());
            System.out.println("After Delete Two Tag :" + two.getTag().toString());
            System.out.println("After Delete Three Tag :" + three.getTag().toString());
            System.out.println("After Delete Del Tag :" + delete.getTag().toString());
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
            for (int position = 0; position < VINInspectionActivity.this.three_text_view_lay.getChildCount(); position++) {
                //int pos = position -1;
                TextView one = VINInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.one);
                one.setTag(firstOne + "," + position);
                TextView two = VINInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.two);
                two.setTag(firstTwo + "," + position);
                TextView three = VINInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.three);
                three.setTag(firstThree + "," + position);
                Button delete = VINInspectionActivity.this.three_text_view_lay.getChildAt(position).findViewById(R.id.deleteButton);
                delete.setTag(firstDel + "," + position);
                System.out.println("After Delete Del :: Position : " + position + " :: TAG : " + delete.getTag().toString());
            }


            deliveryVin.setWantsSupervisorSignature(false);

            //only ask for the supervisor signature if we have NEW damages that weren't from elsewhere
            for(AreaTypeSvrty ats : areaTypeSvrties) {
                if(!ats.readonly) {
                    deliveryVin.setWantsSupervisorSignature(true);
                }
            }

            refreshSupervisorSignatureButton();
        }

    }


    @SuppressLint({"NewApi", "RestrictedApi"})
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent incomingIntent) {
        super.onActivityResult(requestCode, resultCode, incomingIntent);

        //BFF limiting to 10 images per vin is arbitrary
        cameraButton.setClickable(true);
        cameraButton.setAlpha(1.0f);

        try {
            switch (requestCode) {

                case REQ_CODE_CAPTURE_IMAGE:
                    if (resultCode == RESULT_OK) {
                        CommonUtility.showText(" Picture was taken for " + vinTextView.getText().toString());
                        log.debug(Logs.INTERACTION, "message shown: " + " Picture was taken ");

                        Image newImage = this.camera.imageTakenSuccessful(true);
                        newImage.filename = mCurrentPhotoFileName;

                        String opString = "";
                        if (operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) {
                            opString = "- Preload -";
                        }
                        else {
                            opString = "- Delivery -";
                        }

                        Bitmap thumbnailBitmap = ImageHandler.processThumbnailImage(opString, String.valueOf(deliveryVin.vin.vin_number), mCurrentPhotoFileName, true);
                        Bitmap hiresBitmap = ImageHandler.processImage(opString, String.valueOf(deliveryVin.vin.vin_number), mCurrentPhotoFileName);

                        addBitmapToList(thumbnailBitmap, true, newImage, true);

                        this.camera.getNextRequiredImage(getCurrentFocus());
                    } else {
                        CommonUtility.showText(" Picture was not taken ");
                        log.debug(Logs.INTERACTION, "message shown: " + " Picture was not taken ");
                        this.camera.imageTakenSuccessful(false);
                        CommonUtility.deleteCachedImageFile(this, mCurrentPhotoFileName);
                    }

                    System.out.println("Image capture Result Code :: " + resultCode);

                    break;

                case REQ_CODE_NOTES:
                    if (resultCode == RESULT_OK && incomingIntent != null) {
                        if (operation == Constants.DELIVERY_OPERATION) {
                            deliveryVin.deliveryNotes = incomingIntent.getStringExtra("notes");
                        } else {
                            if (autoInsertedPreloadNotes != null) {
                                deliveryVin.preloadNotes = autoInsertedPreloadNotes + incomingIntent.getStringExtra("notes");
                            }
                            else {
                                deliveryVin.preloadNotes = incomingIntent.getStringExtra("notes");
                            }
                        }

                        DataManager.insertDeliveryVinToLocalDB(this, deliveryVin, false);
                        deliveryVin = DataManager.getDeliveryVin(this, deliveryVin.delivery_vin_id);
                    }
                    break;

                case REQ_CODE_REJECTION_VIN:
                    if (resultCode == RESULT_OK && incomingIntent != null) {
                        reasonString = incomingIntent.getStringExtra("reason");
                        if (!reasonString.equalsIgnoreCase("")) {
                            saveButton.setVisibility(VISIBLE);
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
                            TextView one = v.findViewById(R.id.one);
                            TextView two = v.findViewById(R.id.two);
                            Button delete = v.findViewById(R.id.deleteButton);

                            one.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                            one.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
                            System.out.println("REQ_CODE_AREA : Tag " + incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                            delete.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);

                            if (position == areaTypeSvrties.size()) {
                                areaTypeSvrties.add(new AreaTypeSvrty());
                                deliveryVin.setWantsSupervisorSignature(true);
                            }
                            AreaTypeSvrty areaTypeSvrty = areaTypeSvrties.get(position);
                            if(areaTypeSvrty.setAreaString(incomingIntent.getStringExtra("id").split(",")[0])) {
                                deliveryVin.setWantsSupervisorSignature(true);
                            }
                            camera.invalidateQueue();

                            areaTypeSvrty.dumpToLog("Entered");

                            log.debug(Logs.DEBUG, "OnActivityResult returned id='" + incomingIntent.getStringExtra("id") + "'");
                        } else if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("add")) {
                            int i = three_text_view_lay.getChildCount() - 1;
                            View v = three_text_view_lay.getChildAt(i);
                            TextView one = v.findViewById(R.id.one);
                            TextView two = v.findViewById(R.id.two);
                            TextView three = v.findViewById(R.id.three);
                            Button delete = v.findViewById(R.id.deleteButton);

                            if (atsIsBlank(one, two, three)) {
                                one.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                                one.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
                            } else {
                                inflateLayout(three_text_view_lay, false);
                                System.out.println("three_text_view_lay.getChildAt(i)   => " + three_text_view_lay.getChildCount());
                                i = three_text_view_lay.getChildCount() - 1;
                                v = three_text_view_lay.getChildAt(i);

                                one = v.findViewById(R.id.one);
                                two = v.findViewById(R.id.two);
                                three = v.findViewById(R.id.three);
                                delete = v.findViewById(R.id.deleteButton);

                                one.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                                one.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
                                two.setTag("0" + "," + i);
                                two.setText(ATS_NO_VAL);
                                three.setTag(String.format("%2s", incomingIntent.getStringExtra("id").split(",")[1] + "," + i));
                                three.setText(ATS_NO_VAL);
                                System.out.println("REQ_CODE_AREA :: Tag " + "0" + "," + i);
                                delete.setTag("0" + "," + i);

                            }
                            String code = incomingIntent.getStringExtra("id").split(",")[0];
                            String id = incomingIntent.getStringExtra("id").split(",")[1];

                            AreaTypeSvrty areaTypeSvrty =new AreaTypeSvrty(code, ATS_NO_VAL, ATS_NO_VAL, Integer.parseInt(id), -1, -1, false);
                            areaTypeSvrties.add(areaTypeSvrty);
                            deliveryVin.setWantsSupervisorSignature(true);
                            areaTypeSvrty.dumpToLog("Added");

                            startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "type")
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
                            TextView two = v.findViewById(R.id.two);
                            TextView three = v.findViewById(R.id.three);
                            Button delete = v.findViewById(R.id.deleteButton);

                            two.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                            two.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));

                            System.out.println("REQ_CODE_TYPE : TAG " + incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                            delete.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                            if (position == areaTypeSvrties.size()) {
                                log.debug(Logs.DEBUG, "Adding ATS entry");
                                areaTypeSvrties.add(new AreaTypeSvrty());
                                deliveryVin.setWantsSupervisorSignature(true);
                            }
                            AreaTypeSvrty areaTypeSvrty = areaTypeSvrties.get(position);
                            if(areaTypeSvrty.setTypeString(incomingIntent.getStringExtra("id").split(",")[0])) {
                                deliveryVin.setWantsSupervisorSignature(true);
                            }
                            areaTypeSvrty.dumpToLog("Entered");
                            log.debug(Logs.DEBUG, "OnActivityResult returned id='" + incomingIntent.getStringExtra("id") + "'");
                            if (three.getText().equals(ATS_NO_VAL)) {
                                startActivityForResult((new Intent(VINInspectionActivity.this, DamageCodeEnter.class).putExtra("check", "svrty")
                                        .putExtra("mode", "edit")).putExtra("text", three.getText().toString()), REQ_CODE_SVRTY);
                            }
                        } else if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("add")) {
                            int i = three_text_view_lay.getChildCount() - 1;
                            View v = three_text_view_lay.getChildAt(i);
                            TextView one = v.findViewById(R.id.one);
                            TextView two = v.findViewById(R.id.two);
                            TextView three = v.findViewById(R.id.three);
                            Button delete = v.findViewById(R.id.deleteButton);

                            if (atsIsBlank(one, two, three)) {
                                two.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                                two.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
                            } else {
                                inflateLayout(three_text_view_lay, false);
                                System.out.println("three_text_view_lay.getChildAt(i)   => " + three_text_view_lay.getChildCount());
                                i = three_text_view_lay.getChildCount() - 1;
                                v = three_text_view_lay.getChildAt(i);

                                one = v.findViewById(R.id.one);
                                two = v.findViewById(R.id.two);
                                three = v.findViewById(R.id.three);
                                delete = v.findViewById(R.id.deleteButton);

                                one.setTag("0" + "," + i);
                                one.setText(ATS_NO_VAL);
                                two.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                                two.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0]));
                                three.setTag("0" + "," + i);
                                three.setText(ATS_NO_VAL);
                                System.out.println("REQ_CODE_TYPE :: TAG " + "0" + "," + i);
                                delete.setTag("0" + "," + i);

                            }

                            String code = incomingIntent.getStringExtra("id").split(",")[0];
                            String id = incomingIntent.getStringExtra("id").split(",")[1];

                            AreaTypeSvrty areaTypeSvrty =new AreaTypeSvrty(ATS_NO_VAL, code, ATS_NO_VAL, -1, Integer.parseInt(id), -1, false);
                            areaTypeSvrties.add(areaTypeSvrty);
                            deliveryVin.setWantsSupervisorSignature(true);
                            areaTypeSvrty.dumpToLog("Added");
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
                            TextView three = v.findViewById(R.id.three);
                            Button delete = v.findViewById(R.id.deleteButton);

                            three.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                            three.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0], false));

                            System.out.println("REQ_CODE_SVRTY : TAG " + incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                            delete.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + position);
                            if (position == areaTypeSvrties.size()) {
                                log.debug(Logs.DEBUG, "Adding ATS entry");
                                areaTypeSvrties.add(new AreaTypeSvrty());
                                deliveryVin.setWantsSupervisorSignature(true);
                            }
                            AreaTypeSvrty areaTypeSvrty =areaTypeSvrties.get(position);
                            if(areaTypeSvrty.setSvrtyString(incomingIntent.getStringExtra("id").split(",")[0])) {
                                deliveryVin.setWantsSupervisorSignature(true);
                            }
                            areaTypeSvrty.dumpToLog("Entered");
                            log.debug(Logs.DEBUG, "OnActivityResult returned id='" + incomingIntent.getStringExtra("id") + "'");
                            this.camera.addDmgImages(areaTypeSvrties.get(position));
                            this.camera.getNextRequiredImage(getCurrentFocus());
                        } else if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("add")) {
                            int i = three_text_view_lay.getChildCount() - 1;
                            View v = three_text_view_lay.getChildAt(i);
                            TextView one = v.findViewById(R.id.one);
                            TextView two = v.findViewById(R.id.two);
                            TextView three = v.findViewById(R.id.three);
                            Button delete = v.findViewById(R.id.deleteButton);

                            if (atsIsBlank(one, two, three)) {
                                three.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                                three.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0], false));
                            } else {
                                inflateLayout(three_text_view_lay, false);
                                System.out.println("three_text_view_lay.getChildAt(i)   => " + three_text_view_lay.getChildCount());
                                i = three_text_view_lay.getChildCount() - 1;
                                v = three_text_view_lay.getChildAt(i);

                                one = v.findViewById(R.id.one);
                                two = v.findViewById(R.id.two);
                                three = v.findViewById(R.id.three);
                                delete = v.findViewById(R.id.deleteButton);

                                one.setTag("0" + "," + i);
                                one.setText(ATS_NO_VAL);
                                two.setTag("0" + "," + i);
                                two.setText(ATS_NO_VAL);
                                three.setTag(incomingIntent.getStringExtra("id").split(",")[1] + "," + i);
                                three.setText(formatATSValue(incomingIntent.getStringExtra("id").split(",")[0], false));
                                System.out.println("REQ_CODE_SVRTY :: TAG " + "0" + "," + i);
                                delete.setTag("0" + "," + i);
                            }
                            String code = incomingIntent.getStringExtra("id").split(",")[0];
                            String id = incomingIntent.getStringExtra("id").split(",")[1];

                            AreaTypeSvrty areaTypeSvrty =new AreaTypeSvrty(ATS_NO_VAL, ATS_NO_VAL, code, -1, -1, Integer.parseInt(id), false);
                            areaTypeSvrties.add(areaTypeSvrty);
                            areaTypeSvrty.dumpToLog("Added");
                            deliveryVin.setWantsSupervisorSignature(true);
                            camera.invalidateQueue();
                        }
                    }
                    break;

                case REQ_CODE_POS:
                    if (resultCode == RESULT_CANCELED) {
                        break;
                    }
                    System.out.println("$$$ extraString ::" + incomingIntent.getStringExtra("id"));
                    loadPosTextView.setText(incomingIntent.getStringExtra("id").split(",")[0]);
                    break;

                case REQ_CODE_SPECIAL:
                    if (resultCode == RESULT_CANCELED) {
                        break;
                    }
                    System.out.println("REQ_CODE_SPECIAL me aa gya");
                    System.out.println("$$$ ID :: " + incomingIntent.getStringExtra("id"));

                    //Special codes have a special code id that we can use to manually get the code values from
                    SpecialCode specialCode = DataManager.getSpecialCode(this, Integer.parseInt(incomingIntent.getStringExtra("id").split(",")[1]));

                    if (incomingIntent.getStringExtra("id").split(",")[2].equalsIgnoreCase("add")) {
                        inflateLayout(three_text_view_lay, true);
                        int i = three_text_view_lay.getChildCount() - 1;
                        View v = three_text_view_lay.getChildAt(i);

                        TextView one = v.findViewById(R.id.one);
                        TextView two = v.findViewById(R.id.two);
                        TextView three = v.findViewById(R.id.three);
                        Button delete = v.findViewById(R.id.deleteButton);

                        System.out.println("three_text_view_lay.getChildAt(i)   => " + three_text_view_lay.getChildCount());
                        i = three_text_view_lay.getChildCount() - 1;
                        v = three_text_view_lay.getChildAt(i);
                        one = v.findViewById(R.id.one);
                        two = v.findViewById(R.id.two);
                        three = v.findViewById(R.id.three);
                        delete = v.findViewById(R.id.deleteButton);

                        one.setTag(specialCode.getAreaCode() + "," + i + ",special");
                        one.setText(formatATSValue(specialCode.getAreaCode()));
                        two.setTag(specialCode.getTypeCode() + "," + i + ",special");
                        two.setText(formatATSValue(specialCode.getTypeCode()));
                        three.setTag(specialCode.getSeverityCode() + "," + i + ",special");
                        three.setText(formatATSValue(specialCode.getSeverityCode(), false));

                        System.out.println("REQ_CODE_SPECIAL :: TAG " + specialCode.special_code_id + "," + i + ",special");
                        delete.setTag(specialCode.special_code_id + "," + i + ",special");

                        AreaTypeSvrty ats = new AreaTypeSvrty();

                        //We are adding a special so there aren't any ids in there....
                        ats.setSpecialId(specialCode.special_code_id);
                        ats.setAreaString(specialCode.getAreaCode());
                        ats.setTypeString(specialCode.getTypeCode());
                        ats.setSvrtyString(specialCode.getSeverityCode());
                        ats.setSpecial(true);

                        areaTypeSvrties.add(ats);
                        deliveryVin.setWantsSupervisorSignature(true);
                        ats.dumpToLog("Added");
                        this.camera.addDmgImages(ats);
                        this.camera.getNextRequiredImage(getCurrentFocus());
                    }
                    break;

                case REQ_CODE_SIGNATURE:
                    // We get here after return from supervisor signature screen on a load inspection.

                    if (resultCode == RESULT_OK || resultCode == SignatureActivity.RESULT_CODE_REVIEW) {
                        DeliveryVin dvSignature = DataManager.getDeliveryVin(this, bundle.getInt("delivery_vin_id"));
                        deliveryVin.supervisorSignature = dvSignature.supervisorSignature;
                        deliveryVin.supervisorComment = dvSignature.supervisorComment;
                        deliveryVin.supervisorContact = dvSignature.supervisorContact;
                        deliveryVin.supervisorSignatureSignedAt = dvSignature.supervisorSignatureSignedAt;
                        deliveryVin.supervisorSignatureLat = dvSignature.supervisorSignatureLat;
                        deliveryVin.supervisorSignatureLon = dvSignature.supervisorSignatureLon;

                        //unset the 'unsigned damages' flag
                        if(resultCode != SignatureActivity.RESULT_CODE_REVIEW) {
                            deliveryVin.setWantsSupervisorSignature(false);
                        }

                        deliveryVin.inspectedPreload = true;

                        //saveClick(null);
                        if (resultCode == RESULT_OK) {
                            finishInspection();
                        }
                    }

                    break;
                default:
                    break;
            }

            //check to see if we need to display the supervisor signature button
            refreshSupervisorSignatureButton();

        } catch (Exception e) {
            //TODO: this is absolutely horrible.  no really, catch EXCEPTION after this block of code?!?!
            e.printStackTrace();
        }
    }

    private class cameraController {
        private int operation;
        private Image imageBeingTaken;
        private List<Image> requiredImageQueue = new NoNullsArrayList<Image>();

        public cameraController(int operation) {
            this.operation = operation;

            this.invalidateQueue();
        }

        private Image imageTakenSuccessful(boolean success) {
            Image newImage;

            if (imageBeingTaken == null) {
                newImage = new Image();
            } else if (success) {
                newImage = imageBeingTaken;
            } else {
                this.requiredImageQueue.add(0, imageBeingTaken);
                newImage = imageBeingTaken;
            }

            imageBeingTaken = null;
            return newImage == null ? new Image() : newImage;
        }

        private final String generatePrompt(Image image) {
            log.debug(Logs.DEBUG, "generating prompt for label: " + image.foreignKeyLabel + " key: " + image.foreignKey);

            final String areaPrefix = "Take a picture of the whole side of the vehicle which includes ";
            final String damagePrefix = "Take a close-up picture of the damage:\n";

            if (image.foreignKeyLabel.equals(Constants.IMAGE_AREA)) {
                AreaCode areaCode = DataManager.getAreaCodeById(currentActivity, image.foreignKey);
                if(areaCode != null) {
                    String area = (areaCode).getDescription();
                    return areaPrefix + area;
                } else {
                    return "";
                }
            } else if (image.foreignKeyLabel.equals(Constants.IMAGE_DAMAGE)) {
                for (AreaTypeSvrty d : areaTypeSvrties) {
                    if (d.damage_id == image.foreignKey) {
                        if (d.isSpecial()) {
                            SpecialCode specialCode = DataManager.getSpecialCode(currentActivity, d.getSpecial_code_id());
                            if (specialCode != null) {
                                return damagePrefix + (specialCode).getDescription();
                            } else {
                                return "";
                            }
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
            } else if (image.foreignKeyLabel.equals(Constants.IMAGE_ODOMETER)) {
                return getResources().getString(R.string.image_odometer_prompt);
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

            log.debug(Logs.DEBUG, "Checking for duplicate required images: " + label);
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
            image.foreignKey = 1;
            image.foreignKeyLabel = label;
            this.requiredImageQueue.add(image);
            log.debug(Logs.DEBUG, "Required " + label + " image added.");
        }

        private void addAreaImage(AreaTypeSvrty dmg) {
            int id = dmg.getArea_code_id();
            log.debug(Logs.DEBUG, "Adding area image with ID " + id);

            if (dmg.isSpecial()) {
                return;
            }
            log.debug(Logs.DEBUG, "Checking for duplicate Area images: FK Label: " + Constants.IMAGE_AREA + " (fk=" + Integer.toString(id) + ")");
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

        public void addDmgImages(AreaTypeSvrty dmg) {

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
            if (operation == Constants.DELIVERY_OPERATION || operation == Constants.PRELOAD_OPERATION) {
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

        public void removeDmgImages(AreaTypeSvrty dmg) {
            areaTypeSvrties.remove(dmg);

            invalidateQueue();
        }

        public void invalidateQueue() {
            requiredImageQueue.clear();

            int externalImageCount = 0;
            for (AreaTypeSvrty dmg : areaTypeSvrties) {
               if(dmg.isExternal()) {
                   externalImageCount++;
               }
            }


            if(externalImageCount >= 5) {
                addExtraImages(Constants.IMAGE_KEYS_EXTERIOR_FULL_SET);
            }
            else if (mIsDealerUnavailable && mDelivery.isDealerPhotosOnUnAttended()) {

                if (mLoad.driver.highClaims != 0) {
                    addExtraImages(Constants.IMAGE_KEYS_EXTERIOR_FULL_SET);
                    //addExtraImages(Constants.IMAGE_KEYS_EXTERIOR_CORNER_ONLY);
                }
                else {
                    addExtraImages(Constants.IMAGE_KEYS_EXTERIOR_CORNER_ONLY);
                }
            }
            if (mIsDealerUnavailable) {
                if (mDelivery.isDealerPhotosOnUnAttended()) {
                    if (DeliveryVin.isOdometerPicRequired(mDelivery.dealer.mfg, CommonUtility.getDriverHelpTerm())) {
                        addExtraImage(Constants.IMAGE_ODOMETER);
                    }
                    addExtraImage(Constants.IMAGE_VIN_PLATE);
                }
            }

            // This is where the image is not found in the list
            for (AreaTypeSvrty dmg : areaTypeSvrties) {
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
            final cameraController currentCamera = this;
            final Image next = this.requiredImageQueue.remove(0); // pop first element off required images

            String message = generatePrompt(next);
            if (message.equals("")) {
                    return getNextRequiredImage(v);
            }
            if (operation == Constants.DELIVERY_OPERATION && mDelivery.isDealerUnavailable() && DeliveryVin.isRequiredDealerUnavailableImage(next, mIsHighClaimsDriver, mDelivery.dealer)) {
                message += "\n\nREASON FOR IMAGE: Required for unattended deliveries";
            }

            Builder builder = new AlertDialog.Builder(VINInspectionActivity.this);
            builder.setTitle("Image Required");

            builder.setMessage(message);
            builder.setPositiveButton("Camera", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    CommonUtility.logButtonClick(log, "Camera");
                    imageBeingTaken = next;
                    currentCamera.cameraClick(focus);
                }
            });
            builder.setNegativeButton("Postpone", (dialog, which) -> {
                CommonUtility.logButtonClick(log, "Postpone");
                requiredImageQueue.add(0, next);
            });
            builder.setCancelable(false);
            builder.create().show();
            log.debug(Logs.INTERACTION, "'Image Required' popup displayed: " + message.replace("\n", " "));
            return true;
        }

        public void cameraClick(View v) {
            Location location = locationHandler.getLocation();

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
        addBitmapToList(bitmap, false, image, false);
    }

    private void addBitmapToList(final Bitmap bitmap, boolean getLocation, Image image, boolean addToFront) {

        if (image == null) {
            log.debug(Logs.DEBUG, "creating new image since image object was null...");
            return;
        }

        ImageView imageView = new ImageView(this);
        imageView.setTag(image.foreignKey);
        //setting image resource
        imageView.setImageBitmap(bitmap);
        //setting image position
        imageView.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        imageView.setPadding(10, 10, 10, 10);

        if (getLocation) {
            Location location = locationHandler.getLocation();
            image.imageLat = String.valueOf(location.getLatitude());
            image.imageLon = String.valueOf(location.getLongitude());
        }

        if (operation != Constants.DELIVERY_OPERATION) {
                image.preloadImage = true;
        }
        activeImages.add(image);

        imageView.setTag(image);

        if (addToFront) {
            cameraImageLayout.addView(imageView, 0);
        }
        else {
            cameraImageLayout.addView(imageView);
        }

        imageView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Prevent rapid clicks from opening multiple popups
                if (CommonUtility.doubleClickDetected()) {
                    return;
                }

                final View view = v;

                final ImageViewDialog imageViewDialog = new ImageViewDialog(VINInspectionActivity.this,
                        "vin #" + deliveryVin.vin.vin_number) {
                    @Override
                    public void DeleteImage() {
                        final ImageViewDialog imageViewDialog = this;
                        super.DeleteImage();
                        Builder builder = new AlertDialog.Builder(VINInspectionActivity.this);
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

                                        if (image.image_id != -1) {
                                            garbageImages.add(image);
                                        }
                                        else if (image.filename != null) {
                                            CommonUtility.deleteCachedImageFile(getApplicationContext(), image.filename + "_hires");
                                            CommonUtility.deleteCachedImageFile(getApplicationContext(), image.filename);
                                        }

                                        log.debug(Logs.DEBUG, "activeimages size: " + activeImages.size());
                                        activeImages.remove(image);
                                        log.debug(Logs.DEBUG, "activeimages post-remove size: " + activeImages.size());

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

                String filename = CommonUtility.cachedImageFileFullPath(getApplicationContext(),image.filename);

                File bigFile = new File(CommonUtility.cachedImageFileFullPath(getApplicationContext(),image.filename + "_hires"));

                if(bigFile.exists()) {
                    filename = bigFile.getAbsolutePath();
                }

                if (new File(filename).exists()) {
                    Bitmap bigBitmap = BitmapFactory.decodeFile(filename);

                    if (bigBitmap == null) {
                        log.debug(Logs.DEBUG, "bigBitmap was null");
                    }
                    // Note: If bigBitmap is null, it will display as a blank image
                    imageViewDialog.SetBitmap(bigBitmap);
                }

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

        private boolean isEmpty(String s) {
            return s == null || s.length() == 0;
        }

        public boolean isIncomplete() {
            return isEmpty(this.areaString) || isEmpty(this.typeString) || isEmpty(this.svrtyString);
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

        public void dumpToLog(String action) {
            String logMsg = action + (isSpecial ? " special" : "") +
                    (operation != Constants.DELIVERY_OPERATION ? " preload":" delivery") + " damage. Load:" +
                    mLoad.loadNumber + " vin:" + deliveryVin.vin.vin_number + " Damage:" +
                    this.areaString +"|" + this.typeString + "|" + this.svrtyString;
            log.debug(Logs.INTERACTION, logMsg);
            log.info(Logs.DAMAGES, logMsg);
        }
    }


    private boolean damagesMatch(NoNullsArrayList<AreaTypeSvrty> startingDamages, NoNullsArrayList<AreaTypeSvrty> endingDamages) {

        if (startingDamages.size() != endingDamages.size()) {
            return false;
        }
        for (int i=0; i < startingDamages.size(); i++) {
            AreaTypeSvrty startingDamage = startingDamages.get(i);
            AreaTypeSvrty endingDamage = endingDamages.get(i);
            if (startingDamage.getAreaString() != endingDamage.getAreaString() ||
                    startingDamage.getTypeString() != endingDamage.getTypeString() ||
                    startingDamage.getSvrtyString() != endingDamage.getSvrtyString()) {
                return false;
            }
        }
        return true;
    }


    private void displayDeliveryVinData(boolean clear) {
        log.debug(Logs.DEBUG, "isEditable=" + isEditable);
        if (clear) {
            areaTypeSvrties.clear();
            activeImages.clear();
            three_text_view_lay.removeAllViews();
            cameraImageLayout.removeAllViews();
        }

        deliveryVin = DataManager.getDeliveryVin(this, deliveryVin.delivery_vin_id);

        // Load Position
        if (deliveryVin.position != null && deliveryVin.position.trim() != "" && !deliveryVin.position.equals("null")) {
            loadPosTextView.setText(deliveryVin.position);
        } else {
            loadPosTextView.setText("");
        }

        areaTypeSvrties.clear();
        three_text_view_lay.removeAllViews();

        boolean externalDamagePopup = false;
        int externalDamageCount = 0;
        //Set up damages list
        if (deliveryVin.damages != null && deliveryVin.damages.size() > 0) {
            System.out.println("# damage records: " + deliveryVin.damages.size());

            int i = 0;

            autoInsertedPreloadNotes = null; // Regenerate auto-inserted notes each time.
            for (Damage damage : deliveryVin.damages) {
                if (damage.preLoadDamage) {
                    if (operation == Constants.DELIVERY_OPERATION) {
                        continue;
                    }
                } else {
                    if (operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) {
                        continue;
                    }
                }

                AreaTypeSvrty ats = new AreaTypeSvrty();

                log.debug(Logs.DEBUG, "damage id: " + damage.damage_id);
                ats.damage_id = damage.damage_id;
                if (damage.specialCode != null) {
                    ats.setSpecial(true);
                    ats.setSpecialId(damage.special_code_id);
                    ats.setAreaString(damage.specialCode.getAreaCode());
                    ats.setTypeString(damage.specialCode.getTypeCode());
                    ats.setSvrtyString(damage.specialCode.getSeverityCode());
                } else {
                    ats.setAreaString(HelperFuncs.noNull(damage.areaCode.getCode(), "--"));
                    ats.setTypeString(HelperFuncs.noNull(damage.typeCode.getCode(), "--"));
                    ats.setSvrtyString(HelperFuncs.noNull(damage.severityCode.getCode(), "--"));
                    ats.setSpecial(false);
                    ats.setSpecialId(-1);
                    ats.setArea_code_id(damage.areaCode.area_code_id);
                    ats.setType_code_id(damage.typeCode.type_code_id);
                    ats.setSeverity_code_id(damage.severityCode.severity_code_id);
                }

                ats.setReadyOnly(damage.readonly);
                ats.setExternal(damage.source.equals("external"));
                // If this inspection record is being revisited after a supervisor signature has already been
                // collected, force a new supervisor signature to be collected on any changes.
                if (clear && deliveryVin.supervisorSignature != null && !ats.isReadOnly() && ats.isSevereDamage() && !deliveryVin.ats.trim().isEmpty()) {
                    supervisorSignatureOnChanges = true;
                }
                areaTypeSvrties.add(ats);
                AreaTypeSvrty tmpAts = new AreaTypeSvrty(ats.getAreaString(), ats.getTypeString(), ats.getSvrtyString(), -1, -1, -1, ats.isSpecial());
                if (clear) {
                    // Save the initial set of damages so that we can detect whether damages have been modified on done click.
                    startingAreaTypeSvrties.add(tmpAts);
                }


                inflateLayout(three_text_view_lay, ats.isSpecial());
                View v = three_text_view_lay.getChildAt(i);

                int fontColor = Color.RED;

                if (damage.readonly && damage.source.equals("external")) {
                    fontColor = Color.GREEN;

                    Image image = new Image();
                    image.foreignKeyLabel = Constants.IMAGE_DAMAGE;
                    image.foreignKey = damage.damage_id;
                    if (clear) {
                        externalDamagePopup = true;
                    }
                    externalDamageCount++;

                    String insertedNote = AUTO_INSERT_PREFIX + damage.areaCode.getCode() + "-" + damage.typeCode.getCode() + "-" + damage.severityCode.getCode() + "\n";
                    log.debug(Logs.DEBUG, "Adding preload note: " + insertedNote);
                    if (autoInsertedPreloadNotes == null) {
                        autoInsertedPreloadNotes = AUTO_INSERT_PREFIX + "Damage notes received from inspection company:\n";
                    }
                    if (externalDamageCount <= INSPECTION_CO_DAMAGES_ITEMIZE_MAX){
                        autoInsertedPreloadNotes += insertedNote;
                    }
                } else if (damage.readonly && damage.source.equals("driver")) {
                    fontColor = this.getResources().getColor(R.color.Orange);
                }

                TextView one = v.findViewById(R.id.one);
                TextView two = v.findViewById(R.id.two);
                TextView three = v.findViewById(R.id.three);
                Button delete = v.findViewById(R.id.deleteButton);

                //Don't forget to check for special codes which get the strings from a different place!
                one.setTextColor(fontColor);
                one.setTag("0" + "," + i);
                one.setText(damage.specialCode != null ? formatATSValue(damage.specialCode.getAreaCode()) : formatATSValue(damage.areaCode.getCode()));

                two.setTag("0" + "," + i);
                two.setText(damage.specialCode != null ? formatATSValue(damage.specialCode.getTypeCode()) : formatATSValue(damage.typeCode.getCode()));
                two.setTextColor(fontColor);

                three.setTag("0" + "," + i);
                three.setText(damage.specialCode != null ? formatATSValue(damage.specialCode.getSeverityCode(), false) : formatATSValue(damage.severityCode.getCode(), false));
                three.setTextColor(fontColor);

                delete.setTag("0" + "," + i);
                //delete.setText(i+"");

                // disable if we're in review-only mode
                one.setEnabled(isEditable && !ats.isReadOnly());
                two.setEnabled(isEditable && !ats.isReadOnly());
                three.setEnabled(isEditable && !ats.isReadOnly());
                delete.setVisibility(isEditable && !ats.isReadOnly() ? VISIBLE : View.INVISIBLE);

                i++;
            }
            if (autoInsertedPreloadNotes != null) {
                if (externalDamageCount > INSPECTION_CO_DAMAGES_ITEMIZE_MAX) {
                    autoInsertedPreloadNotes = AUTO_INSERT_PREFIX + "The inspection company reported " + externalDamageCount + " damages for this vehicle\n";
                }
                if (deliveryVin.preloadNotes == null) {
                    deliveryVin.preloadNotes = autoInsertedPreloadNotes;
                }
                else {
                    // Strip any old auto-inserted notes and replace with the new
                    deliveryVin.preloadNotes = autoInsertedPreloadNotes + stripAutoInsertedNotes(deliveryVin.preloadNotes);
                }
            }

            if(externalDamagePopup) {

                String message = "This load has damages reported by the inspection company.  Images of the " +
                        "affected areas will be required.";

                if(externalDamageCount > 4) {
                    message = "This load has damages reported by the inspection company.  Since there are " + externalDamageCount + " damages, " +
                            "images of the four sides and roof will be required.";
                }
                Builder builder = new AlertDialog.Builder(VINInspectionActivity.this);
                builder.setTitle("Images Required");
                builder.setMessage(message);
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                            imageBeingTaken = next;
//                            currentCamera.cameraClick(focus);
                    }
                });
                builder.create().show();
                log.debug(Logs.INTERACTION, "Popup message displayed: " + message);
            }
        }

        cameraImageLayout.setVisibility(GONE);
        cameraImageLayout.removeAllViews();
        activeImages.clear();

        // Create the prefix for the image file name here to avoid re-doing the lookups on each picture taken.
        if (operation == Constants.DELIVERY_OPERATION) {

            Load load = DataManager.getLoad(this, DataManager.getDelivery(this,deliveryVin.delivery_id).load_id);
            Delivery delivery = DataManager.getDelivery(this, deliveryVin.delivery_id);

            String dealerNumber;

            if(load.shuttleLoad) {
                dealerNumber = "-shuttle-";
            } else if(delivery.dealer == null || delivery.dealer.customer_number == null) {
                dealerNumber = "-xx-";
            } else {
                dealerNumber = delivery.dealer.customer_number;
            }


            mImageFileNamePrefix = Constants.DELIVERY_VIN_IMAGE_FILE_PREFIX
                    + load.loadNumber
                    + Constants.IMAGE_FILE_DELIM + dealerNumber
                    + Constants.IMAGE_FILE_DELIM + deliveryVin.vin.vin_number + Constants.IMAGE_FILE_DELIM;
        }
        else {
            mImageFileNamePrefix = Constants.PRELOAD_VIN_IMAGE_FILE_PREFIX
                                   + DataManager.getLoad(this, DataManager.getDelivery(this,deliveryVin.delivery_id).load_id).loadNumber
                                   + Constants.IMAGE_FILE_DELIM + deliveryVin.vin.vin_number + Constants.IMAGE_FILE_DELIM;
        }

        //Set up image list
        for (int i = deliveryVin.images.size() - 1; i >= 0; i--) {
            Image image = deliveryVin.images.get(i);

            log.debug(Logs.DEBUG, "Checking image " + image.image_id);

            if ((operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) && image.preloadImage) {
                log.debug(Logs.DEBUG, "Adding image to preload image list");
                log.debug(Logs.DEBUG, "Foreign key: " + image.foreignKeyLabel + " - " + image.foreignKey);

                if (new File(CommonUtility.cachedImageFileFullPath(getApplicationContext(),image.filename)).exists()) {
                    Bitmap bigBitmap = CommonUtility.getBitmapThumbnail(getApplicationContext(), CommonUtility.cachedImageFileFullPath(getApplicationContext(), image.filename));

                    if (bigBitmap == null) {
                        log.debug(Logs.DEBUG, "bigBitmap was null");
                    } else {
                        //If this is a honeywell device
                        /*if (CommonUtility.hasHoneywellScanner()) {
                            //We need to resolve the image rotation
                            int rotation = CommonUtility.getImageRotation(getApplicationContext(), CommonUtility.cachedImageFileFullPath(getApplicationContext(), image.filename));
                            if(rotation > 90) {
                                bigBitmap = CommonUtility.rotateImage(bigBitmap, rotation);
                            } else {
                                bigBitmap = CommonUtility.rotateImage(bigBitmap, -90);
                            }
                        }*/
                        addBitmapToList(bigBitmap, deliveryVin.images.remove(i));
                    }
                }

            } else if (!image.preloadImage) {
                log.debug(Logs.DEBUG, "Adding image to delivery image list");
                log.debug(Logs.DEBUG, "Foreign key: " + image.foreignKeyLabel + " - " + image.foreignKey);
                if (new File(CommonUtility.cachedImageFileFullPath(getApplicationContext(),image.filename)).exists()) {
                    Bitmap bigBitmap = CommonUtility.getBitmapThumbnail(getApplicationContext(), CommonUtility.cachedImageFileFullPath(getApplicationContext(), image.filename));

                    if (bigBitmap == null) {
                        log.debug(Logs.DEBUG, "bigBitmap was null");
                    } else {
                        //Rotation happens in the thumbnail request
                        addBitmapToList(bigBitmap, deliveryVin.images.remove(i));
                    }
                }
            }
        }
        cameraImageLayout.setVisibility(VISIBLE);

        // Load Position
        if (deliveryVin.position != null && deliveryVin.position.trim() != "" && !deliveryVin.position.equals("null")) {
            loadPosTextView.setText(deliveryVin.position);
        } else {
            loadPosTextView.setText("");
        }
        loadPosTextView.setEnabled(isEditable);

        // Facing position (dropdown whose value is either 'D' (driven) or 'B' (back)
        drivenOrBackButton.setOrientation(deliveryVin.backdrv);
        drivenOrBackButton.setEnabled(isEditable);

        // If status is two, change delete button to special format
        /*
        if (Integer.parseInt(deliveryVin.status) == 2) {
            ((Button) findViewById(R.id.reject)).setBackgroundResource(R.drawable.small_button_bg);
        }
        */

        log.debug(Logs.DEBUG, "found " + deliveryVin.images.size() + " images");
        
        /*
        areaButton.setEnabled(isEditable);
        typeButton.setEnabled(isEditable);
        svrtyButton.setEnabled(isEditable);
        */
        addDmgButton.setEnabled(isEditable);
        addDmgButton.setVisibility(isEditable ? VISIBLE : GONE);


        specialButton.setVisibility(isEditable? VISIBLE: GONE);
        cameraButton.setVisibility(isEditable ? VISIBLE : GONE);
        //rejectButton.setVisibility(isEditable ? View.VISIBLE : View.GONE);
        saveButton.setVisibility(isEditable ? VISIBLE : GONE);
    }

    private static final String AUTO_INSERT_PREFIX=">> ";
    
    private String stripAutoInsertedNotes(String notes) {
        if (notes == null) {
            return null;
        }
        StringBuilder str = new StringBuilder();
        String[] lines = notes.split("\n");
        boolean autoInserted = false;
        final int prefLen = AUTO_INSERT_PREFIX.length();
        
        for (String line : lines) {
            if (line.length() >= prefLen && line.substring(0, prefLen).equalsIgnoreCase(AUTO_INSERT_PREFIX)) {
                continue;
            }
            str.append(line + "\n");
        }
        return str.toString();
    }


    public String getPath(Uri uri) {
        String[] projection = {MediaStore.Images.Media.DATA};
        Cursor cursor = managedQuery(uri, projection, null, null, null);
        if (cursor != null) {
            // HERE YOU WILL GET A NULLPOINTER IF CURSOR IS NULL
            // THIS CAN BE, IF YOU USED OI FILE MANAGER FOR PICKING THE MEDIA
            int column_index = cursor
                    .getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
            cursor.moveToFirst();
            String path = getString(column_index);
            cursor.close();
            return path;
        } else
            return null;
    }

    public Bitmap decodeFile(String filePath) {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeFile(filePath, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 1024;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp < REQUIRED_SIZE && height_tmp < REQUIRED_SIZE)
                break;
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }
        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        return BitmapFactory.decodeFile(filePath, o2);
        //imgView.setImageBitmap(bitmap);
    }

    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

}
