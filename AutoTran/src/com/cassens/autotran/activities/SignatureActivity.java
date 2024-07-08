package com.cassens.autotran.activities;

import static android.view.View.VISIBLE;
import static com.sdgsystems.util.HelperFuncs.hideSoftKeyboard;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.GlobalState;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.adapters.DeliveryVinModelAdapter;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.DamageNoteTemplate;
import com.cassens.autotran.data.model.Dealer;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.DeliveryVinModel;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.ShuttleLoadDefaults;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.data.remote.tasks.SendTruckEventsTask;
import com.cassens.autotran.dialogs.DealerUnavailableDialog;
import com.cassens.autotran.dialogs.LookUpScreenDialog;
import com.cassens.autotran.dialogs.SignatureDialog;
import com.cassens.autotran.handlers.LocationHandler;
import com.cassens.autotran.views.SignView;
import com.google.gson.GsonBuilder;
import com.nicdahlquist.pngquant.LibPngQuant;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Random;

public class SignatureActivity extends AutoTranActivity implements OnClickListener
{
    private static final Logger log = LoggerFactory.getLogger(SignatureActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private String userType;
    private String currentUserId;
    private DeliveryVin thisDeliveryVin;
    private Delivery thisDelivery = null;
    private Load thisLoad = null;
    private int operation;
    private boolean isEditable;

    private Bundle bundle;
    private LookUpScreenDialog lookUpScreen;

    private SignView signatureView;
    private TextView txt_signatureTap;
    private TextView txt_signatureNotNeeded;
    /** Base64-encoded Bitmap */
    private String signature;
    private String comment;
    private Button saveButton;
    private Button reviewButton;
    private Button commentButton;
    private TextView contactTextView;
    private TextView commentsEntered;

    protected CheckBox dealerUnavailableCheckbox;
    protected TextView contactlessDeliveryDisallowedReminder;
    protected TextView dealerUnavailableReason;
    protected TextView infoIcon;

    private boolean stiClicked = false;
    private boolean afrhrsClicked = false;

    // Request Codes for Launched Activities
    private static final int REQ_CODE_NOTES = 1001;

    public static final int RESULT_CODE_REVIEW = 1000;

    public static final int signature_length_limit = 1000;
    private static final String TAG = "PreloadSignatureActivity";
    private static final boolean DEBUG = false;
    private int mDamageCount;
    private String mStiInBusinessHoursTxt;
    private boolean isHighClaimsDriver;
    private LocationHandler locationHandler;

    SharedPreferences prefs;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_signature);
        comment = "";
        bundle = getIntent().getExtras();

        userType = bundle.getString("user_type");
        signatureView = findViewById(R.id.view_signature);
        txt_signatureTap = findViewById(R.id.txt_signatureTap);
        txt_signatureNotNeeded = findViewById(R.id.txt_signatureNotNeeded);
        infoIcon = findViewById(R.id.infoIcon);

        reviewButton = findViewById(R.id.btn_review);
        reviewButton = findViewById(R.id.btn_review);
        commentButton = findViewById(R.id.btn_comment);
        dealerUnavailableCheckbox = findViewById(R.id.dealerUnavailableCheckbox);
        dealerUnavailableReason = findViewById(R.id.dealerUnavailableMsg);
        contactlessDeliveryDisallowedReminder = findViewById(R.id.contactlessDeliveryDisallowed);

        contactTextView = findViewById(R.id.contact);
        commentsEntered = findViewById(R.id.comments_entered);

        mStiInBusinessHoursTxt = getResources().getString(R.string.sti_in_business_hours_text);
        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        infoIcon.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonUtility.logButtonClick(log, v);
                showDealerDetails();
            }
        });

        if (userType.equalsIgnoreCase("driver")) {
            CommonUtility.disableEditText((EditText) contactTextView);
        }
        else {
            contactTextView.setOnClickListener(v -> {
                //When you select the contact text view, pop up the full name dialog
                if (userType.equalsIgnoreCase("dealer") && thisDelivery != null && thisDelivery.isAfterHoursDelivery()) {
                    showGetFullNameDialog(false);
                }
                else {
                    showGetFullNameDialog(true);
                }
            });
        }

        saveButton = findViewById(R.id.btn_save);
        saveButton.setOnClickListener(this);

        reviewButton.setOnClickListener(this);
        commentButton.setOnClickListener(this);

        dealerUnavailableCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                 @Override
                 public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                     onDealerUnavailableToggle(isChecked);
                     setDealerUnavailableReason(thisDelivery);
                 }
             }
        );

        saveButton.setEnabled(true);

        findViewById(R.id.img_back).setOnClickListener(this);
        operation = bundle.getInt(Constants.CURRENT_OPERATION);
        isEditable = bundle.getBoolean("is_editable", true);

        if(!(userType.equalsIgnoreCase("dealer"))){
            infoIcon.setVisibility(View.GONE);
        }

        updateSignatureClickBehavior();
        if (userType == null) {
            goBack("Signature Activity Failed: Missing Information");  // Calls finish, among other things.
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());
        this.currentUserId = CommonUtility.getDriverNumber(this);

        User driver = DataManager.getUserForDriverNumber(this, currentUserId);
        isHighClaimsDriver = (driver == null) ? false : (driver.highClaims != 0);



        TextView damagesEntered = findViewById(R.id.damages_entered);

        if (operation == Constants.DELIVERY_OPERATION) {
            thisDelivery = DataManager.getDelivery(this, bundle.getInt("delivery_id"));

            mDamageCount = 0;

            for(DeliveryVin dv : thisDelivery.deliveryVins) {
                for(Damage damage : dv.damages) {
                    if(!damage.preLoadDamage) {
                        mDamageCount++;
                    }
                }
            }

            updateDamageCount(damagesEntered);

            if (thisDelivery == null) {
                goBack("Cannot get delivery record: delivery_id=" + thisDeliveryVin.delivery_id);
            }
            if (userType.equalsIgnoreCase("dealer")) {
                comment = thisDelivery.dealerComment;
                contactTextView.setText(blankIfNull(thisDelivery.dealerContact));
                setStiAndAfrhrs(thisDelivery.isStiDelivery(), thisDelivery.isAfterHoursDelivery(), false);
                if (!thisDelivery.isDealerUnavailable()) { // Does thisDelivery have a dealerSignature?
                    setSignature(thisDelivery.dealerSignature);
                }
            } else {
                comment = thisDelivery.driverComment;
                updateDealerCommentFlag();

                if(thisDelivery.driverContact != null) {
                    contactTextView.setText(blankIfNull(thisDelivery.driverContact));
                } else {
                    contactTextView.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("driverName", ""));
                }

            }
        } else if (operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) {
            if (userType.equalsIgnoreCase("driver")) {
                thisLoad = DataManager.getLoad(this, bundle.getInt("load_id"));

                if (thisLoad == null) {
                    goBack("Cannot get load record: load_id=" + bundle.getInt("load_id"));
                }

                int damageCount = 0;

                for(Delivery d : thisLoad.deliveries) {
                    for (DeliveryVin dv : d.deliveryVins) {
                        for(Damage damage : dv.damages) {
                            if(damage.preLoadDamage && !damage.readonly) {
                                damageCount++;
                            }
                        }
                    }
                }

                String message = "";

                if(damageCount > 0) {
                    if(damageCount == 1) {
                        message = "1 damage recorded";
                    } else {
                        message =  damageCount + " damages recorded";
                    }

                    damagesEntered.setText(message);
                }

                comment = thisLoad.driverPreLoadComment;

                if(thisLoad.driverPreLoadContact != null) {
                    contactTextView.setText(blankIfNull(thisLoad.driverPreLoadContact));
                } else {
                    contactTextView.setText(PreferenceManager.getDefaultSharedPreferences(this).getString("driverName", ""));
                }

                if(operation == Constants.PRELOAD_OPERATION && thisLoad.shuttleLoad && thisLoad.shuttleMove != null) {
                    ShuttleLoadDefaults defaults = ShuttleLoadDefaults.get(this);
                    defaults.terminalId = thisLoad.shuttleMove.getTerminal();
                    defaults.terminalText = DataManager.getTerminal(this, Integer.parseInt(thisLoad.shuttleMove.getTerminal())).description;
                    defaults.originText = thisLoad.shuttleMove.origin;
                    defaults.destinationText = thisLoad.shuttleMove.destination;

                    //I could forward this as an extra 
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(Constants.PREF_SHUTTLE_LOAD_DEFAULTS, new GsonBuilder().create().toJson(defaults, ShuttleLoadDefaults.class));
                    editor.commit();
                }

                getTmpSignature(operation + "_" + userType);
            } else {  // Must be supervisor
                thisDeliveryVin = DataManager.getDeliveryVin(this, bundle.getInt("delivery_vin_id"));

                if(DEBUG) log.debug(Logs.DEBUG, "Got supervisor comment ('" + thisDeliveryVin.supervisorComment + "')");

                if (thisDeliveryVin == null) {
                    goBack("Cannot get deliveryVin record: delivery_vin_id=" + bundle.getInt("delivery_vin_id"));
                }
                int loadId = bundle.getInt("load_id", -1);
                if (loadId == -1) {
                    if (thisDelivery == null) {
                        thisDelivery = DataManager.getDelivery(this, thisDeliveryVin.delivery_id);
                    }
                    loadId = thisDelivery.load_id;
                }
                thisLoad = DataManager.getLoad(this, loadId);

                damagesEntered.setTypeface(Typeface.MONOSPACE, Typeface.BOLD);
                if (thisDeliveryVin.damages.size() > 0) {
                    damagesEntered.setText(thisDeliveryVin.getFormattedDamagesSummary(true));
                } else {
                    damagesEntered.setText("");
                }

                comment = thisDeliveryVin.supervisorComment;
                contactTextView.setText(thisDeliveryVin.supervisorContact);
                getTmpSignature(operation + "_" + userType);
            }
        }

        if (userType.equalsIgnoreCase("dealer")) {
            ((TextView) findViewById(R.id.txt_heading)).setText("Dealer Signature");

            Dealer thisDealer = thisDelivery.dealer;
            ((TextView) findViewById(R.id.txt_sub_heading)).setText(
                            thisDealer.customer_name + "\n" +
                            HelperFuncs.noNull(thisDealer.address) + "\n" +
                            HelperFuncs.noNull(thisDealer.city) + ", " + HelperFuncs.noNull(thisDealer.state) +
                            "  " + HelperFuncs.noNull(thisDealer.zip));

            // findViewById(R.id.lin_layout).setBackgroundColor(getResources().getColor(R.color.DarkLightBlue)); // HACK - probably want to remove
            setTopBarColor(R.id.lin_layout, R.id.img_back, R.drawable.back_button_dealer, -1, -1, R.color.DealerIndicatorColor);
            findViewById(R.id.dealerUnavailableToggleLayout).setVisibility(VISIBLE);
            findViewById(R.id.delivery_receipt_notice).setVisibility(VISIBLE);
            reviewButton.setText("Return to Inspections");
            reviewButton.setVisibility(VISIBLE);
            getTmpSignature(operation + "_" + userType);
            if (!HelperFuncs.isNullOrEmpty(thisDelivery.dealerSignature)) {
                txt_signatureTap.setVisibility(View.GONE);
                if (!thisDelivery.isDealerUnavailable()) {
                    setSignature(thisDelivery.dealerSignature);
                    signatureView.setVisibility(VISIBLE);
                }
            }
            if ((thisDelivery.dealer.afthr == null || thisDelivery.dealer.comments == null || thisDelivery.dealer.comments.equals("") || !thisDelivery.dealer.hoursSet()) && !thisDelivery.isStiDelivery()) {
                showDealerHoursWebsite();
            }
        } else if(userType.equalsIgnoreCase("driver")) {
            if (operation == Constants.DELIVERY_OPERATION) {
                ((TextView) findViewById(R.id.txt_heading)).setText("Driver Signature");
                ((TextView) findViewById(R.id.txt_sub_heading)).setText("Driver's Signature to \n Confirm Delivery");
                log.debug(Logs.INTERACTION, "prompt for driver's signature to confirm delivery");
                reviewButton.setVisibility(VISIBLE);
                getTmpSignature("driverDelivery");
            }
            else {
                ((TextView) findViewById(R.id.txt_heading)).setText("Driver Signature");
                ((TextView) findViewById(R.id.txt_sub_heading)).setText("Driver's Signature to \n Confirm Preload Inspection");
                log.debug(Logs.INTERACTION, "prompt for driver's signature to confirm preload inspection");
                reviewButton.setVisibility(VISIBLE);
            }
        } else if (userType.equalsIgnoreCase("supervisor")) {
            ((TextView) findViewById(R.id.txt_heading)).setText("Supervisor Signature");
            ((TextView) findViewById(R.id.txt_sub_heading)).setText("Supervisor Signature to \n Confirm Severe Damage");
            log.debug(Logs.INTERACTION, "Prompt for supervisor signature to confirm severe damage");
            reviewButton.setVisibility(VISIBLE);
        }

        if (!isEditable) {
            hideSoftKeyboard(this);
            signatureView.setEnabled(false);
            reviewButton.setVisibility(View.GONE);
            contactlessDeliveryDisallowedReminder.setVisibility(View.GONE);
            if (thisDelivery != null && (thisDelivery.isAfterHoursDelivery() || thisDelivery.isStiDelivery())) {
                dealerUnavailableCheckbox.setEnabled(false);
            }
            else {
                dealerUnavailableCheckbox.setVisibility(View.INVISIBLE);
            }
            contactTextView.setEnabled(false);
            saveButton.setVisibility(View.GONE);
        }

        showCommentsEnteredStatus();

        locationHandler = LocationHandler.getInstance(getApplicationContext());
        locationHandler.startLocationTracking();
    }

    @Override
    protected void onDestroy() {
        locationHandler.stopLocationTracking();
        super.onDestroy();
    }

    private void updateSignatureClickBehavior() {
        if(!CommonUtility.isTablet(this) && isEditable) {
            signatureView.setEnabled(false);
            signatureView.setOnTouchCallback(event -> {
                //CommonUtility.showText(SignatureActivity.this, "touched disabled sig box");
                log.debug(Logs.DEBUG, "motion event: " + event.getAction());

                if(event.getAction() == MotionEvent.ACTION_DOWN) {
                    onSignatureAreaTapped();
                }
            });

            updateSignatureViewVisibility();
        }
    }

    private void updateSignatureViewVisibility() {
        if (stiClicked || afrhrsClicked) {
            signatureView.setVisibility(View.GONE);
            txt_signatureTap.setVisibility(View.GONE);
            txt_signatureNotNeeded.setText(getString(R.string.dealer_unavailable_signature_text));
            txt_signatureNotNeeded.setVisibility(VISIBLE);
        }
        else {
            txt_signatureNotNeeded.setVisibility(View.GONE);
            if(!CommonUtility.isTablet(SignatureActivity.this)) {
                if(signatureLength < 10) {
                    signatureView.setVisibility(View.GONE);
                    txt_signatureTap.setVisibility(VISIBLE);
                } else {
                    signatureView.setVisibility(VISIBLE);
                    txt_signatureTap.setVisibility(View.GONE);
                }
            }
        }
    }

    SignatureDialog signatureDialog;

    public void showSignatureDialog(String message, boolean useMsgErrorColor) {
        if(signatureDialog == null || (signatureDialog != null && !signatureDialog.isShowing())) {
            String contact = null;
            if (userType.equalsIgnoreCase("driver")) {
                User driver = DataManager.getUserForDriverNumber(this, currentUserId);
                if (driver != null) {
                    contact = driver.fullName;
                }
            }
            else {
                contact = contactTextView.getText().toString().trim();
            }
            signatureDialog = new SignatureDialog(
                    SignatureActivity.this, signature, message, useMsgErrorColor, userType, contact,
                    new SignatureDialog.ISignatureCaptured() {
                        @Override
                        public boolean signatureCaptured(String signatureString) {
                            if (signatureString != null && signatureString.length() > 0) {
                                signatureView.setVisibility(VISIBLE);
                                txt_signatureTap.setVisibility(View.GONE);
                                setSignature(signatureString);
                                updateSignatureClickBehavior();
                                if(operation != Constants.PRELOAD_OPERATION && userType.equalsIgnoreCase("dealer")) {
                                    saveSignatureData(signatureString);
                                    finish();
                                }
                            } else {
                                clearSignature();
                            }
                            return true;
                        }

                        @Override
                        public boolean signatureCleared() {
                            if(operation != Constants.PRELOAD_OPERATION && userType.equalsIgnoreCase("dealer")) {
                                return false;
                            }
                            else {
                                signature = "";
                                clearSignature();
                                return true;
                            }
                        }

                        @Override
                        public boolean reviewRequested() {
                            log.debug(Logs.INTERACTION, "Review pressed from dialog");
                            if (userType.equalsIgnoreCase("dealer")) {
                                if(mDamageCount > 0) {
                                    //This is a dealer signing for a delivery with damages
                                    collectRemainingDamageNotesOrFinish(true, true);
                                } else {
                                    showDealerConfirmDialog(null, comment, true);
                                }
                                return false;
                            }
                            else {
                                setResult(RESULT_CODE_REVIEW);
                                finish();
                                return true;
                            }
                        }
                    });

            log.debug(Logs.SIGNATURES, "Opening signature fullscreen dialog: " + getSignature(signatureView, SignatureActivity.this));

            signatureDialog.show();
        } else {
            log.debug(Logs.SIGNATURES, "signature already showing...");
        }
    }

    public void clearSignature() {
        signature = "";
        signatureLength = 0;
        signatureView.resetView();
        updateSignatureViewVisibility();
    }

    private void updateDealerCommentFlag() {
        if(thisDelivery.dealerComment != null && thisDelivery.dealerComment.length() > 0) {
            TextView commentsEntered = findViewById(R.id.comments_entered);
            commentsEntered.setText("Dealer comments entered");
            commentsEntered.setTextColor(Color.RED);
        }
    }

    private void updateDamageCount(TextView damagesEntered) {
        String message = "";

        if(mDamageCount > 0) {
            if(mDamageCount == 1) {
                message = "1 damage recorded";
            } else {
                message = mDamageCount + " damages recorded";
            }

            damagesEntered.setText(message);
        }
    }

    private String getStatusString() {
        String statusString = " Operation: " + this.operation +
                " User Id: " + this.currentUserId +
                " User Type: " + this.userType
                ;

        if(thisDelivery != null) {
            statusString += " DeliveryId: " + thisDelivery.delivery_id + " remote id: " + thisDelivery.delivery_remote_id;
        }

        if(thisDeliveryVin != null) {
            statusString += " DeliveryVinId: " + thisDeliveryVin.delivery_vin_id + " remote id: " + thisDeliveryVin.delivery_vin_remote_id;
        }

        if(thisLoad != null) {
            statusString += " Loadnumber " + thisLoad.loadNumber + " Load id: " + thisLoad.load_id + " remote id: " + thisLoad.load_remote_id;
        }

        return statusString;

    }

    @Override
    protected void onResume() {
        super.onResume();
        log.debug(Logs.SIGNATURES, "Displayed Signature Page " + getStatusString());
    }

    @Override
    protected void onPause() {
        super.onPause();
        log.debug(Logs.SIGNATURES, "Leaving Signature Page" + getStatusString() + " signature: " + getSignature(signatureView, this));
        signatureView.cache();
    }

    private String blankIfNull(String s) {
        if (s == null) {
            return "";
        }
        return s;
    }

    private void goBack(String toastMessage) {
        CommonUtility.logButtonClick(log, "Back");
        Intent returnIntent = new Intent();

        if (toastMessage != null) {
            CommonUtility.showText(toastMessage);
        }

        //If we are moving back to the driver signature capture, we need to redo our intent
        // TODO: This would be unnecessary if it were designed correctly.  This data *should* be
        //       saved in non-static variables rather than relying on the bundle here.
        if(operation == Constants.DELIVERY_OPERATION && userType.equalsIgnoreCase("driver")) {
            bundle.putString("user_type", "dealer");
        }

        returnIntent.putExtras(bundle);

        setResult(RESULT_CANCELED, returnIntent);
        finish();
    }

    @SuppressLint("NewApi")
    private void saveSignatureData(boolean includeSignature, String signatureString) {
        SimpleTimeStamp sts = new SimpleTimeStamp();
        Date now = new Date();

        if(DEBUG) log.debug(Logs.DEBUG, "Saving Signature Data is=" + includeSignature);
        try {
            Location curLocation = locationHandler.getLocation();
            // This is a temporary "bandaid" to get a fix available in a hurry.  This needs
            // to be revisited when we re-work the LocationHandler module.
            SendTruckEventsTask.LocationData locData = SendTruckEventsTask.chooseBestLocation(curLocation);
            if (includeSignature) {
                if (signatureString == null) {
                    signature = getSignature(signatureView, this);
                }
                else {
                    signature = signatureString;
                }

                if (DEBUG) log.debug(Logs.DEBUG, "Location for signature is: " + locData.toString());
            }

            if (operation == Constants.DELIVERY_OPERATION) {
                if (userType.equalsIgnoreCase("dealer")) {
                    thisLoad = DataManager.getLoad(this, thisDelivery.load_id);

                    thisDelivery.dealerContact = contactTextView.getText().toString().trim();

                    //Don't use the signature if we clicked 'sti'...
                    String msg = "";
                    if (includeSignature) {
                        log.debug(Logs.INTERACTION, "Saving dealer delivery signature. Contact: " + thisDelivery.dealerContact);
                        if (stiClicked) {
                            thisDelivery.dealerSignature = "**** STI Delivery, no signature ****";
                            msg = "Delivery NOT signed by dealer $dealerNumber\nReason: STI";
                        } else if (afrhrsClicked) {
                            thisDelivery.dealerSignature = "**** Delivered during normal delivery hours, dealer unavailable or refusing to sign. ****";
                            msg = "Delivery NOT signed by dealer $dealerNumber\nReason: DO/RTS\nPerson unavailable/refusing to sign: $dealerContact";
                        }
                        else {
                            thisDelivery.dealerSignature = signature;
                            msg = "Delivery signed by dealer $dealerNumber Contact: $dealerContact";
                        }

                        thisDelivery.dealerSignatureLat = String.valueOf(locData.getLat());
                        thisDelivery.dealerSignatureLon = String.valueOf(locData.getLon());
                        thisDelivery.dealerSignatureSignedAt = Constants.dateFormatter().format(HelperFuncs.getTimestamp());


                        //Copied from Delivery Vin Inspection Activity to mark inspections as read-only
                        log.debug(Logs.DEBUG, "setting delivery status to submitted");
                        thisDelivery.status = "submitted";
                    } else {
                        log.debug(Logs.INTERACTION, "saving dealer STI entry");
                        msg = "Delivery NOT signed by dealer $dealerNumber Contact: $dealerContact";
                    }

                    thisDelivery.dealerComment = comment;
                    thisDelivery.sti = stiClicked ? 1 : 0;
                    thisDelivery.afrhrs = afrhrsClicked ? 1 : 0;

                    if(thisDelivery.sti == 1 && thisDelivery.dealer.shouldBeOpen()) {
                        if (thisDelivery.dealerComment == null) {
                            thisDelivery.dealerComment = mStiInBusinessHoursTxt;
                        } else {
                            if (!thisDelivery.dealerComment.isEmpty()) {
                                thisDelivery.dealerComment += "\n";
                            }
                            thisDelivery.dealerComment += mStiInBusinessHoursTxt;
                        }
                    }

                    DataManager.insertDeliveryToLocalDB(this, thisDelivery, false);
                    msg += String.format("\n\nDealer Comments: %s", HelperFuncs.noNullOrWhitespace(thisDelivery.dealerComment, "none"));
                    CommonUtility.highLevelLog(msg, thisLoad, thisDelivery);
                } else {
                    thisLoad = DataManager.getLoad(this, thisDelivery.load_id);

                    // driver delivery signature
                    thisDelivery.driverComment = comment;
                    thisDelivery.driverContact = contactTextView.getText().toString().trim();

                    //Set all delivery images ready to be sent to s3 after the DRIVER signature
                    for(Image image : thisDelivery.images) {
                        image.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
                        DataManager.insertImageToLocalDB(this.getApplicationContext(), image);
                    }

                    for(DeliveryVin dv : thisDelivery.deliveryVins) {
                        for(Image image : dv.images) {
                            if(!image.preloadImage) {
                                image.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
                                DataManager.insertImageToLocalDB(this.getApplicationContext(), image);
                            }
                        }
                    }
                    if (includeSignature) {
                        log.debug(Logs.INTERACTION, "Saving driver delivery signature");

                        //Log.d("NARF", "saving driver delivery signature");
                        thisDelivery.driverSignature = signature;
                        thisDelivery.driverSignatureLat = String.valueOf(locData.getLat());
                        thisDelivery.driverSignatureLon = String.valueOf(locData.getLon());
                        thisDelivery.driverSignatureSignedAt  = Constants.dateFormatter().format(HelperFuncs.getTimestamp());

                        if (thisLoad.originLoad) {
                            thisDelivery.dealerSignature = "**** Relay Type load ("+thisLoad.loadType+"), no dealer signature required. ****";
                            thisDelivery.dealerSignatureSignedAt = Constants.dateFormatter().format(HelperFuncs.getTimestamp());
                        }

                        if (thisLoad.isSVLoadType()) {
                            thisDelivery.dealerSignature = "**** Shuttle Type load ("+thisLoad.loadType+"), no dealer signature required. ****";
                            thisDelivery.dealerSignatureSignedAt = Constants.dateFormatter().format(HelperFuncs.getTimestamp());
                        }

                        if (thisDelivery.isDealerAlwaysUnattended()) {
                            thisDelivery.dealerSignature = "**** This location does not require a dealer signature ****";
                            thisDelivery.dealerSignatureSignedAt = Constants.dateFormatter().format(HelperFuncs.getTimestamp());
                        }

                        DataManager.insertDeliveryToLocalDB(this, thisDelivery, false);
                        String msg = "Delivery for $dealerNumber signed by driver $driverNumber";
                        msg += String.format("\n\nComments: %s", HelperFuncs.noNullOrWhitespace(thisDelivery.driverComment, "none"));
                        CommonUtility.highLevelLog(msg, thisLoad, thisDelivery);
                        CommonUtility.highLevelLog(String.format("Delivery $deliveryId from load $loadNumber completed:\n%s", thisDelivery.getFormattedSummary(false)), thisLoad, thisDelivery, false);


                        String customer = "UNKNOWN";
                        String mfg = "UNKNOWN";
                        if (thisDelivery.shuttleLoad) {
                            customer = Constants.LOAD_SHUTTLE;
                        } else if (!HelperFuncs.isNullOrEmpty(thisDelivery.dealer.customer_name)) {
                            customer = thisDelivery.dealer.customer_number;
                            mfg = thisDelivery.dealer.mfg;
                        }

                        String eventString =  TextUtils.join(",",
                                new String[]{
                                        "D",
                                        thisLoad.driver.driverNumber,
                                        thisLoad.loadNumber,
                                        customer,
                                        mfg,
                                        sts.getUtcDateTime(),
                                        sts.getUtcTimeZone(),
                                        String.valueOf(locData.getLat()),
                                        String.valueOf(locData.getLon())
                                });

                        LoadEvent event = new LoadEvent();
                        event.csv = eventString;
                        DataManager.insertLoadEvent(this, event);
                        SyncManager.pushLoadEventsLatched(getApplicationContext());

                        for (DeliveryVin dv : thisDelivery.getDeliveryVinList()) {
                            String damages = "";
                            for (Damage d : dv.damages) {
                                if (!d.preLoadDamage) {
                                    damages += TextUtils.join("|",
                                            new String[]{
                                                    d.getAreaCode(),
                                                    d.getTypeCode(),
                                                    d.getSeverityCode(),
                                            });
                                    damages += ";";
                                }
                            }

                            eventString = TextUtils.join(",",
                                    new String[]{
                                            "D-V_sig",
                                            thisLoad.driver.driverNumber,
                                            thisLoad.loadNumber,
                                            customer,
                                            dv.vin.vin_number,
                                            dv.position,
                                            dv.backdrv,
                                            sts.getDateTime(),
                                            sts.getLocalTimeZone(),
                                            String.valueOf(locData.getLat()),
                                            String.valueOf(locData.getLon()),
                                            damages
                                    });

                            LoadEvent dvEvent = new LoadEvent();
                            dvEvent.csv = eventString;

                            //Not uploading these at signature time
                            //DataManager.insertLoadEvent(this, dvEvent);
                            //SyncManager.pushLoadEventsLatched(getApplicationContext());
                        }


                        /*
                        // don't sync here, the deliveryvininspection activity should decide whether or not to
                        // sync

                        //This should push the delivery to the remote server now that the driver has signed it
                        RemoteSyncTask syncTask = new RemoteSyncTask(this);
                        syncTask.execute(thisLoad.driver_id);
                        */
                    }
                    else {

                        log.debug(Logs.INTERACTION, "NOT saving driver delivery signature");

                        DataManager.insertDeliveryToLocalDB(this , thisDelivery, false);
                    }
                }
            } else {
                // Preload signature
                if (userType.equalsIgnoreCase("driver")) {
                    // driver preload signature
                    thisLoad.driverPreLoadComment = (comment != null) ? comment : "";
                    thisLoad.driverPreLoadContact = contactTextView.getText().toString().trim();

                    //Set all delivery images ready to be sent to s3 after the DRIVER signature
                    for(Image image : thisLoad.images) {
                        image.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
                        DataManager.insertImageToLocalDB(this.getApplicationContext(), image);
                    }

                    for(Delivery tmpDelivery : thisLoad.deliveries) {
                        for (DeliveryVin dv : tmpDelivery.deliveryVins) {
                            for (Image image : dv.images) {
                                if (image.preloadImage) {
                                    image.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
                                    DataManager.insertImageToLocalDB(this.getApplicationContext(), image);
                                }
                            }
                        }
                    }

                    if (includeSignature) {
                        log.debug(Logs.INTERACTION, "Saving driver preload signature");

                        thisLoad.driverPreLoadSignature = signature;
                        thisLoad.driverPreLoadSignatureLat = String.valueOf(locData.getLat());
                        thisLoad.driverPreLoadSignatureLon = String.valueOf(locData.getLon());
                        thisLoad.driverPreLoadSignatureSignedAt = Constants.dateFormatter().format(HelperFuncs.getTimestamp());

                        //Only set the load to submitted AFTER getting the signature
                        log.debug(Logs.DEBUG, "setting load status to submitted");
                        thisLoad.status = "submitted";

                        GlobalState.setLastStartedLoadInfo(thisLoad.loadNumber, thisLoad.truckNumber);

                        if (thisLoad.relayLoad && thisLoad.isAutoDeliverLoadType()) {
                            for (Delivery delivery : thisLoad.deliveries) {
                                delivery.dealerSignature = "**** Relay Type load ("+thisLoad.loadType+"), no dealer signature required. ****";
                                delivery.driverSignature = "**** Relay Type load ("+thisLoad.loadType+"), no driver signature required. ****";

                                for (DeliveryVin dv : delivery.getDeliveryVinList()) {
                                    dv.inspectedDelivery = true;
                                }
                            }
                        }

                        if (thisLoad.isSVLoadType()) {
                            for (Delivery delivery : thisLoad.deliveries) {
                                delivery.dealerSignature = "**** Shuttle Type load ("+thisLoad.loadType+"), no dealer signature required. ****";

                                for (DeliveryVin dv : delivery.getDeliveryVinList()) {
                                    dv.inspectedDelivery = true;
                                }
                            }
                        }

                        String msg = "Preload signed by driver $driverNumber";
                        msg += String.format("\n\nComments: %s", HelperFuncs.noNullOrWhitespace(thisLoad.driverPreLoadComment, "none"));

                        CommonUtility.highLevelLog(msg, thisLoad, thisDelivery);

                        CommonUtility.highLevelLog(thisLoad.getFormattedSummary(true), thisLoad, null, false);
                        DataManager.insertLoadToLocalDB(this, thisLoad, false);

                        lookUpScreen = new LookUpScreenDialog(CommonUtility.getCurrentActivity(), new LookUpScreenDialog.LookUpScreenCallback() {
                            @Override
                            public void proceed() {
                                //No callback passed
                            }
                        });

                        LookUpScreenDialog.cleanLookUpEntry(String.valueOf(thisLoad.load_id));

                        if (thisLoad.parent_load_id != -1) {
                            checkAndSignParentLoad(thisLoad, sts, now, locData);
                        }

                        createLoadEventsForSignedLoad(thisLoad, sts, now, locData);

                        /*
                        // don't sync here, the deliveryvininspection activity should decide whether or not to
                        // sync
                        RemoteSyncTask syncTask = new RemoteSyncTask(this);
                        syncTask.execute(thisLoad.driver_id);
                        */
                        //DataManager.pushLocalDataToRemoteServer(this, thisLoad.driver_id, false);
                    } else {
                        DataManager.insertLoadToLocalDB(this, thisLoad, false);
                    }
                    
                    //save last used origin for shuttle loads
                } else {
                    // must be supervisor preload
                    thisDeliveryVin.supervisorComment = comment;
                    thisDeliveryVin.supervisorContact = contactTextView.getText().toString().trim();

                    if (includeSignature) {

                        thisDeliveryVin.supervisorSignature = signature;
                        thisDeliveryVin.supervisorSignatureLat = String.valueOf(locData.getLat());
                        thisDeliveryVin.supervisorSignatureLon = String.valueOf(locData.getLon());
                        thisDeliveryVin.supervisorSignatureSignedAt = Constants.dateFormatter().format(HelperFuncs.getTimestamp());
                        String logMsg = String.format("Supervisor %s signed for %d preload damage(s) on VIN %s", HelperFuncs.noNull(thisDeliveryVin.supervisorContact), thisDeliveryVin.getDriverAddedDamageCount(), HelperFuncs.noNull(thisDeliveryVin.vin.vin_number));
                        log.debug(Logs.INTERACTION, logMsg);
                        // TODO: Ask Cassens whether they would like to log the actual damages for the VIN in the high-level log at this point.
                        //       This would provide a better history of the interaction in the case where there are multiple iterations of the
                        //       damages for a VIN before the final signature.
                        logMsg += String.format("\n\nComments: %s", HelperFuncs.noNullOrWhitespace(thisDeliveryVin.supervisorComment, "none"));

                        CommonUtility.highLevelLog(logMsg, thisLoad, thisDelivery);
                    }
                    log.debug(Logs.DEBUG, "Saving supervisor comment ('" + thisDeliveryVin.supervisorComment + "')");

                    DataManager.insertDeliveryVinToLocalDB(this, thisDeliveryVin, false);
                }
            }
        }
        catch (Exception e) // Make this more specific later.
        {
            log.error(Logs.EXCEPTIONS, "Exception in signatureactivity: ", e);
            e.printStackTrace();
        }
    }

    private void saveSignatureData(boolean includeSignature) {
        saveSignatureData(includeSignature, null);
    }

    private void saveSignatureData(String signatureString) {
        saveSignatureData(true, signatureString);
    }

    public static void checkAndSignParentLoad (Load childLoad, SimpleTimeStamp sts, Date now, SendTruckEventsTask.LocationData locData) {
        Context context = AutoTranApplication.getAppContext();
        if (DataManager.allChildLoadsSigned(context, childLoad.parent_load_id)) {
            log.debug(Logs.INTERACTION, "all inspection groups signed, saving driver preload signature for PARENT load");
            Load parentLoad = DataManager.getLoadForRemoteId(context, String.valueOf(childLoad.parent_load_id));

            parentLoad.driverPreLoadSignature = "** ALL INSPECTION GROUPS SIGNED **";
            parentLoad.driverPreLoadSignatureLat = childLoad.driverPreLoadSignatureLat;
            parentLoad.driverPreLoadSignatureLon = childLoad.driverPreLoadSignatureLon;
            parentLoad.driverPreLoadSignatureSignedAt = Constants.dateFormatter().format(HelperFuncs.getTimestamp());
            log.debug(Logs.DEBUG, "setting PARENT load status to submitted");
            parentLoad.status = "submitted";

            if (parentLoad.relayLoad && parentLoad.isAutoDeliverLoadType()) {
                for (Delivery delivery : parentLoad.deliveries) {
                    delivery.dealerSignature = "**** Relay Type load ("+parentLoad.loadType+"), no dealer signature required. ****";
                    delivery.driverSignature = "**** Relay Type load ("+parentLoad.loadType+"), no driver signature required. ****";

                    for (DeliveryVin dv : delivery.getDeliveryVinList()) {
                        dv.inspectedDelivery = true;
                    }
                }
            }

            DataManager.copyChildLoadDataToParent(context, parentLoad);

            DataManager.insertLoadToLocalDB(context, parentLoad, false);

            createLoadEventsForSignedLoad(parentLoad, sts, now, locData);
        }
    }

    private static void createLoadEventsForSignedLoad(Load load, SimpleTimeStamp sts, Date now, SendTruckEventsTask.LocationData locData) {
        if (locData == null) {
            locData = new SendTruckEventsTask.LocationData();
        }
        String eventString =  TextUtils.join(",",
            new String[]{
                "P",
                load.driver.driverNumber,
                load.loadNumber,
                sts.getUtcDateTime(),
                sts.getUtcTimeZone(),
                String.valueOf(locData.getLat()),
                String.valueOf(locData.getLon())
            });

        LoadEvent event = new LoadEvent();
        event.csv = eventString;
        DataManager.insertLoadEvent(AutoTranApplication.getAppContext(), event);
        SyncManager.pushLoadEventsLatched(AutoTranApplication.getAppContext());

        for (DeliveryVin dv : load.getDeliveryVinList()) {
            String damages = "";
            for (Damage d : dv.damages) {
                if (d.preLoadDamage) {
                    damages += TextUtils.join("|",
                        new String[]{
                            d.getAreaCode(),
                            d.getTypeCode(),
                            d.getSeverityCode(),
                        });
                    damages += ";";
                }
            }

            eventString = TextUtils.join(",",
                new String[]{
                    "PL-V_sig",
                    load.driver.driverNumber,
                    load.loadNumber,
                    dv.vin.vin_number,
                    dv.position,
                    dv.backdrv,
                    sts.getDateTime(),
                    sts.getLocalTimeZone(),
                    String.valueOf(locData.getLat()),
                    String.valueOf(locData.getLon()),
                    damages
                });
            LoadEvent dvEvent = new LoadEvent();
            dvEvent.csv = eventString;
            //We're not uploading these at signature time
        }
    }


    @Override
    @SuppressLint("NewApi")
    public void onClick(View v) {
        log.debug(Logs.SIGNATURES, "Clicked a button: " + getStatusString() + " " + getSignature(signatureView, this));

        int id = v.getId();
        if (id == R.id.img_back) {
            CommonUtility.logButtonClick(log, "Back");
            goBack(null);
            return;
        }
        CommonUtility.logButtonClick(log, v);
        if (id == R.id.btn_comment) {
            Intent commentIntent = new Intent(SignatureActivity.this, NotesActivity.class);

            boolean hasPredefinedComments = true;

            if (operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) {
                if(userType.equalsIgnoreCase("supervisor")) {
                    commentIntent.putExtra("state", NotesActivity.PRELOAD_SUPERVISOR_VIN_SIGNOFF);
                    commentIntent.putExtra("title", "Supervisor Comments");

                    if(this.comment != null) {
                        commentIntent.putExtra(NotesActivity.EXTRA_NOTES, comment);
                    } else {
                        commentIntent.putExtra(NotesActivity.EXTRA_NOTES, thisDeliveryVin.supervisorComment);
                    }

                    hasPredefinedComments = false;
                } else {  // Must be the driver
                    commentIntent.putExtra("state", NotesActivity.PRELOAD_DRIVER_LOAD_SIGNOFF);
                    commentIntent.putExtra("title", "Driver Comments");

                    if(this.comment != null) {
                        commentIntent.putExtra(NotesActivity.EXTRA_NOTES, comment);
                    } else {
                        commentIntent.putExtra(NotesActivity.EXTRA_NOTES, HelperFuncs.noNull(thisLoad.driverPreLoadComment));
                    }

                    hasPredefinedComments = false;
                }

            } else {
                if (thisDelivery != null && thisDelivery.dealer != null) {
                    commentIntent.putExtra(NotesActivity.EXTRA_MFG, thisDelivery.dealer.mfg);
                }

                if(userType.equalsIgnoreCase("dealer")) {
                    commentIntent.putExtra(NotesActivity.EXTRA_STATE, NotesActivity.DELIVERY_DEALER_SIGNOFF);
                    commentIntent.putExtra(NotesActivity.EXTRA_TITLE, "Dealer Comments");

                    if(this.comment != null) {
                        commentIntent.putExtra(NotesActivity.EXTRA_NOTES, comment);
                    } else {
                        commentIntent.putExtra(NotesActivity.EXTRA_NOTES, thisDelivery.dealerComment);
                    }
                    commentIntent.putExtra(NotesActivity.EXTRA_TOP_BAR_COLOR, R.color.DealerIndicatorColor);
                } else {  // Must be the driver
                    commentIntent.putExtra(NotesActivity.EXTRA_STATE, NotesActivity.DELIVERY_DRIVER_SIGNOFF);
                    commentIntent.putExtra(NotesActivity.EXTRA_TITLE, "Driver Comments");

                    if(this.comment != null) {
                        commentIntent.putExtra(NotesActivity.EXTRA_NOTES, comment);
                    } else {
                        commentIntent.putExtra(NotesActivity.EXTRA_NOTES, thisDelivery.driverComment);
                    }
                }
            }

            if(hasPredefinedComments) {
                commentIntent.putExtra("prompt", "Please type a comment, or choose predefined comments");
            } else {
                commentIntent.putExtra("prompt", "Please type a comment");
            }

            //commentIntent.putExtra("notes", comment);
            commentIntent.putExtra("is_editable", isEditable);

            startActivityForResult(commentIntent, REQ_CODE_NOTES);
        } else if (id == R.id.btn_clear) {
            clearSignature();
        } else if (id == R.id.btn_review) {
            if (!userType.equalsIgnoreCase("supervisor") && !userType.equalsIgnoreCase("dealer")) {
                if(!signatureView.isEmpty() || signatureLength > 0) {
                    saveSignatureData(false);
                    saveTmpSignature(operation + "_" + userType);
                }
            }
            setResult(RESULT_CODE_REVIEW);
            finish();
        } else if (id == R.id.btn_save) {
            int sig = getSignature(signatureView, SignatureActivity.this).length();
            log.debug(Logs.DEBUG, String.format("sig=%d", sig));

            //Only require a contact if sti is not clicked (ie: a contact is available) or we are not a supervisor
            if (contactTextView.isShown() && contactTextView.getText().toString().trim().equals("")) {
                showSimpleDialog("Contact information is required", true);
                return;
            } else if (signatureView.isShown() && signatureView.isEmpty()) {
                showSimpleDialog("Signature is required", true);
                return;
            } else if (!stiClicked && !afrhrsClicked && getSignature(signatureView, SignatureActivity.this).length() < signature_length_limit) {
                log.debug(Logs.INTERACTION, "signature too short message shown");
                showSimpleDialog("Your signature was too short to be recognized, please try again", true);
                clearSignature();
                // Re-display the signature dialog here.
            } else {
                log.debug(Logs.INTERACTION, "saving signature data");

                setResult(RESULT_OK);

                if (userType.equalsIgnoreCase("driver")) {
                    showConfirmationDialogForDriverUpload();
                } else if(userType.equalsIgnoreCase("supervisor")) {

                    //We don't need to do anything if this is a supervisor signature...
                    saveSignatureData(true);
                    finish();
                } else {
                    // We can reach this code branch only if we are proceeding without a dealer signature.
                    log.debug(Logs.INTERACTION, "Displaying Dealer confirmation dialog" + userType);
                    if(mDamageCount > 0 || !userType.equalsIgnoreCase("dealer")) {
                        //This is a dealer signing for a delivery with damages
                        collectRemainingDamageNotesOrFinish(true, false);
                    } else {
                        showDealerConfirmDialog(null, comment, false);
                    }
                }
            }
        }
    }

    private boolean onDealerUnavailableToggle(boolean isChecked) {
        // Override if needed
        if (thisDelivery == null) {
            CommonUtility.simpleMessageDialog(this, "No delivery record is selected");
            return false;
        }
        if (isChecked != thisDelivery.isDealerUnavailable()) {
            if (!isChecked) {
                thisDelivery.sti = 0;
                thisDelivery.afrhrs = 0; // turn off afrhrs, since afrhrs implies STI (in current workflow anyway)
                //thisDelivery.dealerContact = "";
                setStiAndAfrhrs(thisDelivery.isStiDelivery(), thisDelivery.isAfterHoursDelivery(), true);
            }
            else {
                // Dialog logic will set sti and afthrs to appropriate values
                DealerUnavailableDialog dealerUnavailableDialog = new DealerUnavailableDialog(SignatureActivity.this, thisDelivery,
                        new DealerUnavailableDialog.IDealerUnavailableDialogCallback() {

                            @Override
                            public void onAfterHoursClicked() {
                                log.debug(Logs.INTERACTION, "User selected after hours delivery");
                                setStiAndAfrhrs(true, false, true);
                            }

                            @Override
                            public void onDealerOpenRefusedToSignClicked(String contact) {
                                log.debug(Logs.INTERACTION, "User selected DORTS");
                                setStiAndAfrhrs(false, true, true);
                                contactTextView.setText(blankIfNull(thisDelivery.dealerContact));
                            }

                            @Override
                            public void onCancel() {
                                log.debug(Logs.INTERACTION, "User canceled");
                                dealerUnavailableCheckbox.setChecked(false);
                                setStiAndAfrhrs(false, false, true);
                            }
                        });
                dealerUnavailableDialog.show();
            }
        }
        return true;
    }

    private void refreshDealerUnavailableControls() {
        dealerUnavailableCheckbox.setChecked(thisDelivery.isDealerUnavailable());
        setDealerUnavailableReason(thisDelivery);
    }

    private void setDealerUnavailableReason(Delivery delivery) {
        int messageId = -1;
        if (delivery != null) {
            if (delivery.isStiDelivery()) {
                messageId = R.string.dealer_unavailable_after_hours;
            } else if (delivery.isAfterHoursDelivery()) {
                messageId = R.string.dealer_unavailable_dorts;
            }
        }
        if (messageId == -1) {
            dealerUnavailableReason.setText("");
            dealerUnavailableReason.setVisibility(View.INVISIBLE);
        }
        else {
            dealerUnavailableReason.setText(messageId);
            dealerUnavailableReason.setVisibility(View.VISIBLE);
        }
    }

    private void setStiAndAfrhrs(final boolean stiEnabled, final boolean afrhrsEnabled, final boolean saveToDb) {
        log.debug(Logs.INTERACTION, "setting sti to " + Boolean.toString(stiEnabled));
        boolean checkForStiPhotos = false;

        stiClicked = stiEnabled;
        afrhrsClicked = afrhrsEnabled;

        if (thisDelivery != null) {
            thisDelivery.sti =  stiEnabled ? 1 : 0;
            thisDelivery.afrhrs = afrhrsEnabled ? 1 : 0;
            if (thisDelivery.isDealerUnavailable() && thisDelivery.isDealerPhotosOnUnAttended()) {
                checkForStiPhotos = true;
            }
            if (saveToDb) {
                DataManager.saveDeliveryStiAndAfrhrs(this, thisDelivery.getId(), thisDelivery.sti, thisDelivery.afrhrs, thisDelivery.dealerContact);
            }
            if (checkForStiPhotos && !thisDelivery.isInspected(isHighClaimsDriver)) {
                Builder builder = new AlertDialog.Builder(this);
                //builder.setTitle((title == null) ? "" : title);
                builder.setMessage("Completing this delivery without a dealer signature requires more photos.\n\n"
                                    + "When you press Ok, you will be returned to the delivery screen to take more photos.");
                builder.setPositiveButton("Ok", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                });
                /*
                builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener()
                {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        setStiAndAfrhrs(false, false, saveToDb);
                    }
                }); */
                builder.setCancelable(false);
                builder.create().show();
            }
            refreshDealerUnavailableControls();
        }
        setContactInfoStatus(!stiEnabled);
        updateSignatureViewVisibility();
    }

    private void undoStiAndSave(){
        thisDelivery.dealerSignature = null;
        thisDelivery.dealerSignatureLat = null;
        thisDelivery.dealerSignatureLon = null;
        thisDelivery.dealerSignatureSignedAt = null;
        thisDelivery.status = "new";

        if(thisDelivery.dealerComment != null) {
            thisDelivery.dealerComment.replace(mStiInBusinessHoursTxt, "");
        }

        setStiAndAfrhrs(false, false,false);
        DataManager.insertDeliveryToLocalDB(this, thisDelivery, false);
    }

    private void collectRemainingDamageNotesOrFinish(boolean finish, boolean reviewOnly) {
        DamageNoteTemplate template = null;
        Damage damage = null;

        DeliveryVin dv = null;
        ArrayList<DeliveryVinModel> damagedVins = new ArrayList<>();

        //if there are any damages at all,
        if(thisDelivery != null && thisDelivery.hasNewDeliveryDamages()) {

            //Loop through all delivery vins
            for(DeliveryVin tmpDv : thisDelivery.deliveryVins) {
                if(tmpDv.damages != null && tmpDv.damages.size() > 0) {
                    DeliveryVinModel damagedVin = new DeliveryVinModel();
                    damagedVin.setVin(tmpDv.vin.vin_number);
                    damagedVin.setColor(tmpDv.vin.color);
                    damagedVin.setDescription(tmpDv.vin.colordes); //confirm correct field

                    //Loop through all damages
                    for(Damage tmpDamage : tmpDv.damages) {

                        //If this is a delivery damage
                        if(!tmpDamage.preLoadDamage) {
                            damagedVin.setDamages(tmpDamage.getAreaCodeFormatted() + "-" + tmpDamage.getTypeCodeFormatted() + "-" + tmpDamage.getSeverityCodeFormatted());

                            //Identify next note
                            ArrayList<DamageNoteTemplate> templates = DataManager.getRequiredDamageNoteTemplates(getApplicationContext(),
                                    false, true, tmpDamage, DataManager.getOriginTerminalForLoad(getApplicationContext(), thisDelivery.load_id),
                                    (thisDelivery.dealer != null) ? thisDelivery.dealer.mfg : null);

                            if (templates != null && templates.size() > 0) {
                                template = templates.remove(0);
                                damage = tmpDamage;
                                dv = tmpDv;
                                break;
                            }
                        }
                    }
                    if(!damagedVin.getDamages().equals("")) {
                        damagedVins.add(damagedVin);
                    }
                }
            }
        }

        //if next note isn't null
        if(template != null) {
            //Collect note
            damage.collectDamageNoteForTemplate(this, template, new Damage.DamageNoteCallback() {
                @Override
                public void damageNoteCollected() {
                    //recurse once the note is collected
                    collectRemainingDamageNotesOrFinish(false, reviewOnly);

                    updateDealerCommentFlag();
                }
            }, false, dv, thisDelivery);

        } else if(finish){
            if(operation != Constants.PRELOAD_OPERATION && !userType.equalsIgnoreCase("supervisor")) {
                showDealerConfirmDialog(damagedVins, comment, reviewOnly);
            } else {
                saveSignatureData(true);
                finish();
            }
        }
    }



    private void showDealerConfirmDialog(ArrayList<DeliveryVinModel> damagedVins, String dealerComment, boolean reviewOnly) {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_dealer_confirm, null);
        String positiveButtonText = "Confirm";
        String negativeButtonText = "Cancel";

        TextView messageView = dialogView.findViewById(R.id.message);
        ListView listView = dialogView.findViewById(R.id.damages);
        TextView commentsView = dialogView.findViewById(R.id.comments);
        LinearLayout dealerCommentsLayout = dialogView.findViewById(R.id.ll_dealer_comments);

        // Use custom onClickListener so that we can return from the listener without closing
        // the dialog whenever the required pay number has not been entered.
        class DealerConfirmOnClickListener implements View.OnClickListener {
            private final Dialog dialog;

            public DealerConfirmOnClickListener(Dialog dialog) {
                this.dialog = dialog;
            }

            @Override
            public void onClick(View v) {
                CommonUtility.logButtonClick(log, v, "DealerConfirmDialog");
                saveSignatureData(true);
                dialog.dismiss();
                finish();
            }
        }

        boolean positiveButtonSet = false;
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);
        builder.setCancelable(false);

        if (damagedVins == null || damagedVins.size() == 0) {
            if (reviewOnly) {
                builder.setTitle("Clean delivery");
            }
            else {
                builder.setTitle("Confirming a clean delivery");
            }
            messageView.setText("There are no exceptions recorded for this delivery.");
            listView.setVisibility(View.GONE);
            messageView.setVisibility(VISIBLE);
            log.debug(Logs.INTERACTION, "Clean delivery confirmation dialog shown for no damages in load");
        } else {
            builder.setTitle("Exceptions present on delivery");
            log.debug(Logs.INTERACTION, "Exception confirm dialog shown for load with damages");
            listView.setVisibility(VISIBLE);
            messageView.setVisibility(View.GONE);
            ArrayAdapter<DeliveryVinModel> adapter = new DeliveryVinModelAdapter(this, damagedVins);
            for (DeliveryVinModel damagedVin: damagedVins) {
                log.debug(Logs.INTERACTION, String.format("%s %s", damagedVin.getVin(), damagedVin.getDamages()));
            }
            builder.setAdapter(adapter, null);
        }
        if (reviewOnly) {
            negativeButtonText = "Ok";
        }
        else {
            builder.setPositiveButton(positiveButtonText, (dialogInterface, i) -> {

            });
            positiveButtonSet = true;
        }
        final String negButtonText = negativeButtonText; // Needs to be final for lambda
        builder.setNegativeButton(negButtonText, (dialogInterface, i) -> CommonUtility.logButtonClick(log, negButtonText, "DealerConfirmDialog"));
        if (HelperFuncs.isNullOrWhitespace(dealerComment)) {
            dealerCommentsLayout.setVisibility(View.GONE);
        } else {
            final SignatureActivity sa = this;
            dealerCommentsLayout.setVisibility(VISIBLE);
            commentsView.setText(NotesActivity.formatPredefNotesForTextView(dealerComment));
            /*
            builder.setNeutralButton("Edit Comments", (dialog, which) -> {
                log.debug(Logs.INTERACTION, "user clicked Edit Comments");
                dialog.dismiss();
                sa.onClick(commentButton);
            }); */
        }
        AlertDialog dialog = builder.create();
        dialog.show();
        if (positiveButtonSet) {
            // Must reset positiveButton AFTER showing the dialog; otherwise the button
            // lookup will fail.
            //
            // See https://stackoverflow.com/questions/6142308/android-dialog-keep-dialog-open-when-button-is-pressed
            Button positiveButton = dialog.getButton(DialogInterface.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(new DealerConfirmOnClickListener(dialog));
        }
    }

    private void showGetFullNameDialog(final boolean standardContactInfo) {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_full_name, null);

        Builder builder = new Builder(this);
        final EditText driverId = (EditText) dialogView.findViewById(R.id.pay_number_textbox);
        final TextView personRefusingTv = (TextView) dialogView.findViewById(R.id.person_refusing_tv);

        if (standardContactInfo) {
            //We don't want to show the 'refusal' message if we're not hiding the contact info since that's a normal contact
            personRefusingTv.setVisibility(View.GONE);
        }
        else {
            log.debug(Logs.INTERACTION, "Showing Dealer unavailable/refused to sign confirmation");
            builder.setTitle("Dealer Unavailable / Refused to Sign");
        }
        builder.setView(dialogView)
                .setCancelable(true);

        final Button doneButton = dialogView.findViewById(R.id.done_button);
        final Button cancelButton =  dialogView.findViewById(R.id.cancel_button);

        final TextView firstNameTv = dialogView.findViewById(R.id.first_name_edit_text);
        final TextView lastNameTv = dialogView.findViewById(R.id.last_name_edit_text);
        firstNameTv.requestFocus();

        if(contactTextView.getText().toString().trim().contains(" ")) {
            String[] names = contactTextView.getText().toString().trim().split(" ");
            if(names.length > 0) {
                firstNameTv.setText(names[0]);
            }

            if(names.length > 1) {
                lastNameTv.setText(contactTextView.getText().toString().substring(names[0].length() + 1));
            }
        }

        final Dialog dialog;

        dialog = builder.create();

        doneButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                /*if (!standardContactInfo && !driverId.getText().toString().trim().equals(CommonUtility.getDriverNumber(getApplicationContext()))) {
                    log.debug(Logs.INTERACTION, "Message shown: " + R.string.error_wrong_driver_number);
                    CommonUtility.showText(getString(R.string.error_wrong_driver_number));
                } else */
                if (firstNameTv.getText().toString().trim().isEmpty() || lastNameTv.getText().toString().trim().isEmpty() ) {
                    CommonUtility.showText("Must enter both first and last names");
                } else {
                    contactTextView.setText(firstNameTv.getText().toString().trim() + " " + lastNameTv.getText().toString().trim());
                    if (standardContactInfo) {
                        setContactInfoStatus(true);
                        if (operation != Constants.PRELOAD_OPERATION && userType.equalsIgnoreCase("dealer")) {
                            //thisDelivery.dealerContact = contactTextView.getText().toString();
                            DataManager.saveDeliveryDealerContact(getApplicationContext(), thisDelivery.getId(), contactTextView.getText().toString());

                        }
                    }
                    else {
                        log.debug(Logs.INTERACTION, "Driver updated name of dealer who is unavailable or refusing to sign");
                        thisDelivery.dealerContact = contactTextView.getText().toString();
                        setStiAndAfrhrs(false, true,true);
                    }
                    updateSignatureClickBehavior();
                    dialog.dismiss();
                }


            }
        });

        cancelButton.setOnClickListener(v -> {
            // setStiAndAfrhrs(false, false, true);

            dialog.dismiss();
        });
        
        lastNameTv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doneButton.callOnClick();;
                }
                return false;
            }
        });
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void onSignatureAreaTapped() {
        if (operation != Constants.PRELOAD_OPERATION && userType.equalsIgnoreCase("dealer")) {
            if (contactTextView.getText().toString().trim().isEmpty()) {
                CommonUtility.simpleMessageDialog(this, "Enter dealer contact name");
                return;
            }
            String message;
            boolean useMsgErrorColor = false;
            if (mDamageCount > 0) {
                message = String.format("%d exception(s) recorded for this delivery", mDamageCount);
                useMsgErrorColor = true;
            } else {
                message = "No exceptions recorded for this delivery";
            }
            message += "\n" + commentsEntered.getText();
            showSignatureDialog(message, useMsgErrorColor);
        }
        else {
            showSignatureDialog("", false);
        }
    }

    public void addSignature(View view) {
        if(isEditable) {
            onSignatureAreaTapped();
        }
    }

    private void showConfirmationDialogForDriverUpload() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_tips, null);

        Builder builder = new Builder(this);
        builder.setView(dialogView);

        TextView tip = dialogView.findViewById(R.id.tips_container);
        final CheckBox confirmation = dialogView.findViewById(R.id.tips_confirmation);
        final EditText driverId = dialogView.findViewById(R.id.tips_textbox);
        final Button doneButton = dialogView.findViewById(R.id.done_button);
        ImageView signatureReview = dialogView.findViewById(R.id.signature_review);

        //((LinearLayout)dialogView.findViewById(R.id.tips_button_layout)).setOrientation(LinearLayout.VERTICAL);
        signatureReview.setImageBitmap(signatureView.getBitmap());

        final Dialog dialog;

        if (operation == Constants.DELIVERY_OPERATION) {
            builder.setTitle(getResources().getString(R.string.tips_title));
            builder.setCancelable(false);
            log.debug(Logs.INTERACTION, "Showing Confirmation Dialog for driver pay number -- Drop Complete");
            dialog = builder.create();

            String tipContent = this.getTip();
            tip.setText(tipContent);
        } else {
            builder.setTitle("Preload Complete");
            log.debug(Logs.INTERACTION, "Showing Confirmation Dialog for driver pay number -- Preload Complete");
            dialog = builder.create();

            tip.setVisibility(View.GONE);
            confirmation.setTextSize(18);
            confirmation.setPadding(30, 10, 30, 10);
        }

        doneButton.setOnClickListener(v -> {

            String submittedId = driverId.getText().toString().trim();

            CommonUtility.logButtonClick(log, v, "pay number: " + submittedId);
            if(!CommonUtility.isInteger(submittedId)) {
                log.debug(Logs.INTERACTION, "Message shown: Pay number is invalid");
                CommonUtility.showText("'"  + submittedId + "' is not a valid pay number");
            }
            else if (!submittedId.equals(currentUserId)) {
                dialogView.findViewById(R.id.tips_textbox_border).setBackgroundColor(Color.RED);
                log.debug(Logs.INTERACTION, "Message shown: " + R.string.error_wrong_driver_number);
                CommonUtility.showText(getString(R.string.error_wrong_driver_number));
            } else if (confirmation.isChecked()) {
                dialog.dismiss();

                saveSignatureData(true);

                User driver = DataManager.getUserForDriverNumber(SignatureActivity.this, currentUserId);

                Log.d("SignatureActivity", "Pushing remote data rather than 'next photo' to see if that resolves the hang");
                CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from SignatureActivity.doneButton.onClickListener()");
                DataManager.pushLocalDataToRemoteServer(getApplicationContext(), driver.user_id, false);
                finish();
            } else {
                log.debug(Logs.INTERACTION, "Message shown: " + "Ramps are not down!");
                CommonUtility.showText("Ramps are not down!");
            }
        });

        driverId.setOnEditorActionListener((v, actionId, event) -> {
            if (event != null) {
                doneButton.callOnClick();
                return true;
            } else {
                return false;
            }
        });
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }


    /**
     * Currently this returns a random tip selected from 'tips' in strings.xml
     *
     * @return String representing the tip selected
     */
    private String getTip() {
        Load load = null;
        if (thisDelivery != null) {
            load = DataManager.getLoad(this, thisDelivery.load_id);
        }

        if (thisDelivery != null && thisDelivery.callback != null && thisDelivery.callback.equalsIgnoreCase("Y")) {
            return getString(R.string.call_FMC);
        } else if (thisDelivery != null && thisDelivery.callback != null && thisDelivery.callback.equalsIgnoreCase("N")
                && load != null && !HelperFuncs.isNullOrEmpty(load.helpTerminal) && thisDelivery.dockTerm == Integer.parseInt(load.helpTerminal)) {
            return "Return to home terminal";
        } else if (thisDelivery != null && thisDelivery.callback != null && thisDelivery.callback.equalsIgnoreCase("N")
                && load != null && !HelperFuncs.isNullOrEmpty(load.helpTerminal) && thisDelivery.dockTerm != Integer.parseInt(load.helpTerminal)) {
            return "Direct report to Terminal " + thisDelivery.dockTerm;
        } else {
            String[] tips = getResources().getStringArray(R.array.tips);
            Random r = new Random();
            return tips[r.nextInt(tips.length)];
        }
    }

    private void setContactInfoStatus(boolean enabled) {
        if(!enabled) {
            signatureView.setVisibility(View.GONE);
            findViewById(R.id.lin_contact).setVisibility(View.GONE);
            findViewById(R.id.delivery_receipt_notice).setVisibility(View.GONE);
        } else {
            signatureView.setEnabled(CommonUtility.isTablet(this));
            signatureView.setVisibility(VISIBLE);
            findViewById(R.id.lin_contact).setVisibility(VISIBLE);
            findViewById(R.id.delivery_receipt_notice).setVisibility(VISIBLE);
        }
    }

    private void saveTmpSignature(String pref) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        String signature = getSignature(signatureView, SignatureActivity.this);
        editor.putString(pref, signature);
        editor.commit();
    }

    private void showDealerHoursWebsite() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_forward_website, null);
        AlertDialog.Builder builder = new AlertDialog.Builder(SignatureActivity.this);
        //final TextView message = (TextView) dialogView.findViewById(R.id.sti_comment_message);

        //todo: How would you like these interactive messages to be structured?
        log.debug(Logs.INTERACTION, "Showing dealer hours/instructions dialog");
        //if (thisDelivery.dealer.acceptsAfterHoursDelivery() && this.mThisDelivery.dealer.shouldBeOpen()){
            //todo: fetch message string from strings.xml programatically.
          //  message.setText("You appear to be delivering STI during the dealer's check-in hours. Please provide the name of the authorized person who approved this delivery and the reason for the exception.");
        //}



        builder.setTitle("Provide Missing Information");
        builder.setView(dialogView).setCancelable(true);

        final TextView websiteLink = dialogView.findViewById(R.id.website_button);
        final TextView instructions = dialogView.findViewById(R.id.dealer_hrs_unavailable_textview);
        final Button okButton =  dialogView.findViewById(R.id.ok_button);
        final Dialog dialog;

        if(!thisDelivery.dealer.hoursSet()){
            instructions.append("\n    • Delivery hours");
        }
        if(thisDelivery.dealer.comments == null || thisDelivery.dealer.comments.equals("")){
            instructions.append("\n    • Delivery comments");
        }
        if(thisDelivery.dealer.afthr == null){
            instructions.append("\n    • After hours deliveries accepted?");
        }

        dialog = builder.create();

        websiteLink.setOnClickListener(v -> {
            goToWebsite();
            dialog.dismiss();
        });

        okButton.setOnClickListener(v -> {
            CommonUtility.logButtonClick(log, v, "provide missing dealer information dialog / show dealer hours website");
            dialog.dismiss();
        });

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();

    }

    public void goToWebsite(){
        //goToUrl("http://dealer.cassens.com/pages/dealer-display?dealercode=" + thisDelivery.dealer.customer_number + "&mfg=" + thisDelivery.dealer.mfg + "&dealer_display=true");
        goToUrl("https://www.cassens.com/#/dealer-display/" + thisDelivery.dealer.mfg + "/" + thisDelivery.dealer.customer_number);
    }

    private void goToUrl(String url){
        Uri uriUrl = Uri.parse(url);
        Intent launchBrowser = new Intent(Intent.ACTION_VIEW, uriUrl);
        startActivity(launchBrowser);
    }

    private void getTmpSignature(String prefName) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String signatureString = preferences.getString(prefName, "");
        if (!signatureString.equals("")) {
            setSignature(signatureString);
            if (!CommonUtility.isTablet(this)) {
                signatureView.setVisibility(VISIBLE);
                txt_signatureTap.setVisibility(View.GONE);
            }
            preferences.edit().remove(prefName).commit();
        }
    }

    private void showSimpleDialog(String msg, boolean cancelable) {
         Builder builder = new AlertDialog.Builder(SignatureActivity.this);
            builder.setTitle("Notification");
            builder.setMessage(msg);
            builder.setPositiveButton("Ok", (dialog, which) -> {
                // TODO Auto-generated method stub
            });
            builder.setCancelable(cancelable);
            builder.create().show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQ_CODE_NOTES) {
            if (resultCode == RESULT_OK & data != null) {

                comment = data.getStringExtra("notes");
                log.debug(Logs.DEBUG, "NotesActivity returned msg=" + comment);
                if (userType.equalsIgnoreCase("supervisor") || userType.equalsIgnoreCase("dealer")) {
                    clearSignature();
                }
                showCommentsEnteredStatus();
            }
        }
    }

    private void showCommentsEnteredStatus() {
        String commentMsg = null;
        if (comment != null && comment.length() > 0) {
            if (userType.equalsIgnoreCase("dealer")) {
                if (operation == Constants.DELIVERY_OPERATION) {
                    commentsEntered.setText("Dealer comments entered");

                    DataManager.saveDeliveryDealerComment(getApplicationContext(), thisDelivery.getId(), comment);
                }
            }
            else if (userType.equalsIgnoreCase("driver")) {
                if (operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) {
                    DataManager.savePreloadDriverComment(getApplicationContext(), thisLoad.getId(), comment);

                    commentsEntered.setText("Driver comments entered");
                } else if (operation == Constants.DELIVERY_OPERATION) {
                    DataManager.saveDeliveryDriverComment(getApplicationContext(),thisDelivery.getId(), comment);

                    if(thisDelivery.dealerComment != null && thisDelivery.dealerComment.length() > 0) {
                        commentsEntered.setText("Dealer and driver comments entered");
                    } else {
                        commentsEntered.setText("Driver comments entered");
                    }
                }
            } else if (userType.equalsIgnoreCase("supervisor")) {

                DataManager.savePreloadSupervisorComment(getApplicationContext(),thisDeliveryVin.delivery_vin_id, comment);
                commentsEntered.setText("Supervisor comments entered");
            }
            commentMsg = comment;
        } else  if (operation == Constants.DELIVERY_OPERATION) {
            if(thisDelivery.dealerComment != null && thisDelivery.dealerComment.length() > 0) {
                commentsEntered.setText("Dealer comments entered");
            }
            commentMsg = thisDelivery.dealerComment;
        }
        if (commentMsg != null && commentsEntered.getText().toString().length() > 0) {
            log.debug(Logs.INTERACTION, String.format("%s: %s", commentsEntered.getText().toString(), commentMsg.replace("\n", " ")));
        }
    }

    private int signatureLength = 0;

    /** @param sig Base64-encoded signature Bitmap for SignView display */
    private void setSignature(String sig) {
        if (sig == null) {
            signatureLength = 0;
            return;
        }

        signatureLength = sig.length();

        // Base64 docs indicate only DEFAULT and URL_SAFE as "decoder flags"
        byte[] byte_arr = Base64.decode(signature = sig, Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(byte_arr, 0, byte_arr.length);
        if (CommonUtility.isTablet(SignatureActivity.this)) {
            bitmap = Bitmap.createScaledBitmap(bitmap, bitmap.getWidth() * 4, bitmap.getHeight() *4, true);
        }

        if(bitmap != null) {
            signatureView.setBitmap(bitmap);
        } else {
            log.error(Logs.DEBUG, "signature was null, not setting it...");
        }
    }

    /** @return Base64 encoding of the SignView's current signature image */
    public static String getSignature(SignView signatureView, Activity activity) {
        if(signatureView != null && signatureView.getVisibility() == VISIBLE) {

            Bitmap bitmap = signatureView.getBitmap();
            // Scaling results: could push to 1/5, but 1/8 lost parts of signature.

            if(bitmap != null) {

                if (CommonUtility.isTablet(activity)) {
                    //We only scale down the bitmap for large tablets...
                    bitmap = Bitmap.createScaledBitmap(bitmap,
                            bitmap.getWidth() / 4, bitmap.getHeight() / 4,
                            // filter is for up-scaling: http://stackoverflow.com/a/3867341
                            false);
                }

                String compressedSig = getCompressedImageString(bitmap);
                if (compressedSig == null) {
                    return "Error processing signature";
                }
                return compressedSig;
            } else {
                return "No signature";
            }
        } else {
            return "No signature";
        }
    }

    @Nullable
    public static String getCompressedImageString(Bitmap bitmap) {
        String finalSignature = null;

        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        /* Bitmap.compress has very limited PNG output control/support:
         * 1) It yields 24-bit color PNG independent of input depth.
         * 2) "PNG which is lossless, will ignore the quality setting"
         */
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);

        FileOutputStream out = null;
        try {
            out = new FileOutputStream("/sdcard/AutoTran/sig_original.png");
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out); // bmp is your Bitmap instance

            out.close();


            File inputSignature = new File("/sdcard/AutoTran/sig_original.png");
            File outputSignature = new File("/sdcard/AutoTran/sig_compressed.png");

            outputSignature.delete();

            new LibPngQuant().pngQuantFile(inputSignature, outputSignature);

            FileInputStream compressedInputStream = new FileInputStream(outputSignature);

            byte[] buffer =   new byte[(int) compressedInputStream.getChannel().size()];
            compressedInputStream.read(buffer);

            finalSignature = Base64.encodeToString(buffer, Constants.BASE64_ENCODE_FLAGS);

            inputSignature.delete();
            outputSignature.delete();

        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return finalSignature;
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

    boolean isDealerMode() {
        return userType.equalsIgnoreCase("dealer");
    }
    //Not sure if recreating this fuction is the best way to implement this.
    public void showDealerDetails() {
        Intent intent = new Intent(this, DealerDetailsActivity.class);
        intent.putExtra("delivery_id", thisDelivery.delivery_id);
        intent.putExtra("operation", operation);
        ((Activity) this).startActivity(intent);
    }
}
