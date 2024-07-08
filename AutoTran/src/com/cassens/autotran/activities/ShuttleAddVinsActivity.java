package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.VIN;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.data.model.parcelable.ShuttleLoadVin;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.handlers.LocationHandler;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleTimeStamp;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ShuttleAddVinsActivity extends GenericScanningActivity {
    private static final Logger log = LoggerFactory.getLogger(ShuttleAddVinsActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private static String TAG = "ShuttleAddVinsActivity";

    public static final String EXTRA_VINS = "vins";
    public static final String EXTRA_SHUTTLE_MOVE = "shuttleMove";
    public static final String EXTRA_LOAD_ID = "loadId";
    public static final String EXTRA_NEW_VIN = "newVin";

    private TextView loadNumTextView;
    private TextView vinTextView;
    private EditText routeEditText;
    private EditText prodStatusEditText;
    private TextView messageBoxTextView;

    private static final int REQ_HW_BUTTON = 0;
    private static final int REQ_VIN = 1;
    private static final int REQ_ROUTE = 2;
    private static final int REQ_PROD_STATUS = 3;
    private int scanningStatus = REQ_HW_BUTTON;

    Button saveAndContinueButton;
    Button inspectVehicleButton;

    private List<String> vinList = new ArrayList<>();
    private ShuttleMove shuttleMove;
    private Load load;
    private int currentProgress;
    private int maxProgress;
    private String defaultProdStatus = "";
    private LocationHandler locationHandler;


    SharedPreferences prefs;

    private String buttonType(int request) {

        switch (request) {
            case REQ_VIN:
                return "VIN";
            case REQ_ROUTE:
                return "ROUTE";
            case REQ_PROD_STATUS:
                return "PROD STATUS";
            default:
                break;
        }
        return "UNKNOWN";
    }

    private View.OnClickListener onClickListenerFactory(final int request) {
        return new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                CommonUtility.logButtonClick(log, v);
                scanningStatus = request;
                startScan();
            }
        };
    }

    // Request Codes for Launched Activities
    public static final int REQ_CODE_ROUTE_SELECT = 1001;
    public static final int REQ_CODE_VIN_INSPECTION = 1002;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_shuttle_vin);

        locationHandler = LocationHandler.getInstance(getApplicationContext());
        locationHandler.startLocationTracking();

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        Bundle bundle = getIntent().getExtras();
        //Why are we getting extra vins here?
        String vins = bundle.getString(EXTRA_VINS);
        String newVin = bundle.getString(EXTRA_NEW_VIN, null);

        mManualEntryEnabled = true;

        //We are grabbing these vins because we need them to be able to know what has already been shuttled and what has not been shuttled.
        if (!HelperFuncs.isNullOrEmpty(vins)) {
            for (String v : vins.split(",")) {
                if (!HelperFuncs.isNullOrEmpty(v)) {
                    log.debug(Logs.DEBUG, "Adding vin: " + v);
                    vinList.add(v);
                }
            }
        }

        //I am not exactly sure what all this right here is doing.
        int loadId = bundle.getInt(EXTRA_LOAD_ID);
        load = DataManager.getLoad(this, loadId);
        shuttleMove = new ShuttleMove(getIntent().getExtras().getString(EXTRA_SHUTTLE_MOVE));
        log.debug(Logs.DEBUG, "Shuttle Move: " + shuttleMove.orgDestString);

        loadNumTextView = findViewById(R.id.loadNumber);
        Button vinScanButton = (Button) findViewById(R.id.vinScan);
        vinScanButton.setOnClickListener(onClickListenerFactory(REQ_VIN));

        Button routeScanButton = (Button) findViewById(R.id.routeScan);
        routeScanButton.setOnClickListener(onClickListenerFactory(REQ_ROUTE));

        Button prodStatusScanButton = (Button) findViewById(R.id.productionStatusScan);
        prodStatusScanButton.setOnClickListener(onClickListenerFactory(REQ_PROD_STATUS));

        inspectVehicleButton = (Button) findViewById(R.id.inspectVehicleButton);
        inspectVehicleButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                log.debug(Logs.INTERACTION, "user clicked the save and inspect button");
                    if (isComplete(true)) {
                        String vinNumber = vinTextView.getText().toString();
                        saveVin(false);
                        updateProgress();
                        DeliveryVin thisDeliveryVin = load.getDeliveryVinForVinNumber(vinNumber);
                        CommonUtility.showText("Selected vin " + thisDeliveryVin.vin.vin_number);
                        Intent intent = new Intent(CommonUtility.getCurrentActivity(), VINInspectionActivity.class);
                        intent.putExtra("vin_number", thisDeliveryVin.vin.vin_number);
                        intent.putExtra("vin_desc", thisDeliveryVin.vin.getDescription());
                        intent.putExtra("delivery_vin_id", thisDeliveryVin.delivery_vin_id);
                        intent.putExtra(Constants.CURRENT_OPERATION, Constants.SHUTTLE_LOAD_OPERATION);
                        ((Activity) CommonUtility.getCurrentActivity()).startActivityForResult(intent, REQ_CODE_VIN_INSPECTION);
                    }
            } });

        Button returnToListButton = (Button) findViewById(R.id.returnToListButton);
        returnToListButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                log.debug(Logs.INTERACTION, "user clicked the return to list button");
                back(null);
            }
        });

        saveAndContinueButton = (Button) findViewById(R.id.saveAndContinueButton);
        saveAndContinueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isComplete(true)) {
                    if (vinList.size() < prefs.getInt(Constants.PREF_VEHICLE_COUNT, 1) - 1) {
                        saveVin(false);
                        updateProgress();
                    }
                    else {
                        saveVin(true);
                    }
                }
            }
        });

        messageBoxTextView = findViewById(R.id.messageBox);

        /*
        saveButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    saveButton.setFocusable(true);
                    saveButton.requestFocus();
                }
                return false;
            }
        });  */

        toggleSaveAndContinueButton();

        vinTextView = (TextView) findViewById(R.id.enterShuttleLoadVinEditText);
        vinTextView.setOnClickListener(view -> {
            scanningStatus = REQ_VIN;
            scanEntryDialog(true, null, vinTextView.getText().toString());
        });
        if (newVin != null && !checkIfVinOnDisallowedList(newVin)) {
            vinTextView.setText(newVin);
        }

        prodStatusEditText = (EditText) findViewById(R.id.enterShuttleLoadProductionStatusEditText);
        prodStatusEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean inFocus) {
                if (!inFocus) {
                    //checkPopulatedFields();
                }
            }
        });

        if (shuttleMove.jsAsn != null && !shuttleMove.jsAsn) {
            defaultProdStatus = "KZ";
            prodStatusEditText.setText(defaultProdStatus);
            prodStatusEditText.setEnabled(false);
            prodStatusEditText.setTextColor(Color.GRAY);
            prodStatusEditText.setBackgroundColor(Color.TRANSPARENT);
            // We should probably disable and gray out the button, too; however, for now we're
            // leaving it enabled in case we run into a situation where the default value isn't
            // correct. (Discussed with Matthew.)
            //prodStatusScanButton.setEnabled(false);
            //prodStatusScanButton.setAlpha(0.5f);
        }

        log.debug(Logs.DEBUG, "Route options total: " + shuttleMove.routes.length);
        routeEditText = (EditText) findViewById(R.id.enterShuttleLoadRouteEditText);
        routeEditText.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean focus) {
                if (!focus){
                    //opulatedFields();
                }
            }
        });

        if (shuttleMove.routes != null && shuttleMove.routes.length > 0) {
            routeEditText.setFocusable(false);
            routeEditText.setFocusableInTouchMode(false);
            routeEditText.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                        Intent intent = new Intent(ShuttleAddVinsActivity.this, NotesListActivity.class);
                        intent.putExtra(NotesListActivity.EXTRA_TITLE, "Routes");
                        intent.putExtra(NotesListActivity.EXTRA_OPTIONS, shuttleMove.routes);
                        startActivityForResult(intent, REQ_CODE_ROUTE_SELECT);
                }
            });
        }
        updateProgress();
    }

    @Override
    protected void onDestroy() {
        locationHandler.stopLocationTracking();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();

        //Set the load number
        if(load != null) {
            if (CommonUtility.isHoneywellLargeDisplaySet()) {
                loadNumTextView.setTextSize(14);
            }
            else {
                loadNumTextView.setTextSize(15);
            }
            loadNumTextView.setVisibility(View.VISIBLE);
            loadNumTextView.setText(load.loadNumber);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
            case REQ_CODE_ROUTE_SELECT:
                if (resultCode == RESULT_OK) {
                    String newRoute = data.getStringExtra(NotesListActivity.RESPONSE_SELECTION);
                    if (newRoute != null) {
                        if (!HelperFuncs.isNullOrEmpty(vinTextView.getText().toString()) && vinTextView.getText().toString().length() > 10) {
                            setRoute(vinTextView.getText().toString().charAt(10) + " " + newRoute);
                        } else {
                            setRoute("? " + newRoute);
                        }
                    }
                }
                break;

            case REQ_CODE_VIN_INSPECTION:
                if (resultCode == RESULT_OK) {
                    load = DataManager.getLoad(this, load.load_id);
                    //CommonUtility.showText("Got SUCCESS result from Inspection!");
                }
                else {
                    //CommonUtility.showText("Got ERROR result from Inspection!");
                }
                if (vinList.size() == prefs.getInt(Constants.PREF_VEHICLE_COUNT, 1)) {
                    finish();
                }
                break;
        }
    }

    private void updateProgress(){
        currentProgress = load.deliveries.get(0).getDeliveryVinList().size() + 1;
        maxProgress = PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.PREF_VEHICLE_COUNT, 0);

        TextView progressText = findViewById(R.id.vehicleCountProgressText);
        progressText.setText(String.format("Entering vehicle %d of %d", currentProgress, maxProgress));
    }

    private void saveVin(boolean finish) {
        if (isEmpty() && finish) {
            finishActivity();
        } else if (vinList.indexOf(vinTextView.getText().toString()) != -1) {
            CommonUtility.showText(vinTextView.getText().toString() + " already added to load.");
        } else if (isComplete(true)) {
            String newVin = vinTextView.getText().toString();

            log.debug(Logs.DEBUG, "vin: " + newVin);
            vinList.add(newVin);
            ShuttleLoadVin slv = new ShuttleLoadVin(newVin,
                    routeEditText.getText().toString().replace('?',
                    vinTextView.getText().toString().charAt(10)),
                    prodStatusEditText.getText().toString());
            DeliveryVin dv = new DeliveryVin();
            dv.vin = new VIN();
            dv.inspectedPreload = true;
            dv.inspectedDelivery = true;
            dv.vin.vin_number = slv.vin;
            dv.shuttleLoadRoute = slv.route;
            dv.shuttleLoadProductionStatus = slv.productionStatus;
            dv.pro = String.valueOf(load.deliveries.get(0).deliveryVins.size() + 1);
            load.deliveries.get(0).deliveryVins.add(dv);
            DataManager.insertLoadToLocalDB(this, load);
            // Re-read load to get refreshed delivery_vin id
            load = DataManager.getLoad(this, load.load_id);
            if (currentProgress == 1) {
                saveShuttleLoadStartEvent(load, slv.vin);
            }
            log.debug(Logs.DEBUG, "Inserted VIN to Load ID: " + load.load_id + " Prod Status: " + dv.shuttleLoadProductionStatus + " Route: " + dv.shuttleLoadRoute);
            messageBoxTextView.setText("VIN " + slv.vin + " added to load");
            toggleSaveAndContinueButton();

            // TODO: Check to see if finish flag still needed here?
            if (finish || currentProgress == maxProgress) {
                finishActivity();
            } else {
                vinTextView.setText("");
                routeEditText.setText("");
                prodStatusEditText.setText(defaultProdStatus);
            }
        }
    }

    private void finishActivity() {
        setResult(RESULT_OK);
        finish();
    }

    private void toggleSaveAndContinueButton() {
        log.debug(Logs.DEBUG, "VINs wanted: " + (prefs.getInt(Constants.PREF_VEHICLE_COUNT, 1)));
        log.debug(Logs.DEBUG, "VINs obtained:" + vinList.size());

        if (vinList.size() >= prefs.getInt(Constants.PREF_VEHICLE_COUNT, 1) - 1) {
            saveAndContinueButton.setText("Save");
        } else {
            saveAndContinueButton.setText("Save And Continue");
        }
    }

    private void checkPopulatedFields(){
        if (isComplete(false)){
            saveAndContinueButton.setEnabled(true);
            inspectVehicleButton.setEnabled(true);
        }
        else {
            saveAndContinueButton.setEnabled(false);
            inspectVehicleButton.setEnabled(false);
        }
    }

    private void setRoute(String rte) {
        for (String s : shuttleMove.routes) {
            log.debug(Logs.DEBUG, "route: " + s);
        }
        String[] rteSplit = rte.split("\\s");
        String rteVal;
        if (rteSplit.length == 1) {
            rteVal = rteSplit[0].trim();
        } else {
            rteVal = rteSplit[1].trim();
        }
        log.debug(Logs.DEBUG, "routes length: " + rteVal);
        if (!shuttleMove.checkRoute || shuttleMove.routes == null || Arrays.asList(shuttleMove.routes).indexOf(rteVal) != -1) {
            routeEditText.setText(rte);
        } else {
            log.debug(Logs.INTERACTION, "message: " + "Not a valid Route!");
            CommonUtility.showText("Not a valid Route!");
        }
    }

    private void badScanValueMsg(String scannedValue, String message) {
        String fullMsg = String.format("Scanned value: %s\n\n%s", scannedValue, message);
        CommonUtility.simpleMessageDialog(this, fullMsg);
    }

    private void badScanValueMsg(String scannedValue, int msgId) {
        badScanValueMsg(scannedValue, getResources().getString(msgId));
    }

    @Override
    protected void onScanResultRunOnUiThread(String scannedValue) {
        Log.d(TAG, "barcode: " + scannedValue);

        if (scanningStatus == REQ_HW_BUTTON) {
            if ((scannedValue.contains(" ") && scannedValue.substring(scannedValue.lastIndexOf(" ")).trim().length() < 10)) {
                scanningStatus = REQ_ROUTE;
            } else if (scannedValue.length() > 10) {
                scanningStatus = REQ_VIN;
                scannedValue = scannedValue.trim();
            } else if (scannedValue.length() <= 10) {
                if (scannedValue.length() == 4) {
                    // Some route codes are exactly 4 characters with no spaces. Since a four-character code is usually a
                    // prod status, we assume that's what it is, but display a warning.
                    //badScanValueMsg(scannedValue, R.string.shuttle_ambiguous_barcode_scan);
                    CommonUtility.showText(getString(R.string.shuttle_ambiguous_barcode_scan));
                }
                scanningStatus = REQ_PROD_STATUS;
            }
        }

        switch (scanningStatus) {
            case REQ_VIN:
                String vinBarcode = "";
                if (scannedValue.startsWith("http") && scannedValue.length() > 17) {
                    Pattern pattern = Pattern.compile("([v]?=[a-z0-9]{17}[&]?)", Pattern.CASE_INSENSITIVE);
                    Matcher matcher = pattern.matcher(scannedValue);

                    while (matcher.find()) {
                        vinBarcode = matcher.group();
                        if (vinBarcode.startsWith("v=")) {
                            break;
                        }
                    }

                    vinBarcode = vinBarcode.replace("v=", "").replace("=", "").replace("&", "");
                } else {
                    vinBarcode = CommonUtility.processScannedVIN(scannedValue);
                }

                if (CommonUtility.checkVinNoPopup(vinBarcode)) {
                    vinTextView.setText(vinBarcode);
                    messageBoxTextView.setText("");
                    log.debug(Logs.INTERACTION, "vin: " + vinBarcode);
                    if (checkIfVinOnDisallowedList(vinBarcode)) {
                        vinTextView.setText("");
                    }
                    else if (vinList.indexOf(vinTextView.getText().toString()) != -1) {
                        CommonUtility.simpleMessageDialog(this, "VIN " + vinTextView.getText().toString() + " has already been added to this load.");
                        vinTextView.setText("");
                    } else if (ShuttleBuildLoadActivity.vinShuttledRecently(this, vinBarcode)) {
                        CommonUtility.simpleMessageDialog(this, "Note: This VIN was shuttled less than " + AppSetting.SHUTTLE_REPEAT_SHIP_WARNING_HRS.getInt() +  " hours ago. Make sure this is the correct VIN.");
                    }
                } else {
                    badScanValueMsg(vinBarcode, R.string.shuttle_invalid_vin);
                    log.debug(Logs.INTERACTION, "Invalid vin scanned: " + vinBarcode);
                }
                break;

            case REQ_PROD_STATUS:

                String newProdStatus = scannedValue;

                if (scannedValue.length() > 10) {
                    String msg = getResources().getString(R.string.shuttle_invalid_prod_stat_code);
                    if (CommonUtility.checkVinNoPopup(scannedValue)) {
                        msg += " " + getResources().getString(R.string.shuttle_invalid_code_looks_like_vin);
                    }
                    badScanValueMsg(scannedValue, msg);
                    break;
                } else if (scannedValue.length() > 2) {
                    newProdStatus = scannedValue.substring(scannedValue.length() - 2);
                }

                prodStatusEditText.setText(newProdStatus);
                log.debug(Logs.INTERACTION, "production status: " + newProdStatus);

                break;

            case REQ_ROUTE:
                String[] routePieces = scannedValue.trim().split("\\s+");

                String newRoute;
                if (routePieces.length >= 2) {
                    newRoute = routePieces[routePieces.length - 2].charAt(routePieces[routePieces.length - 2].length() - 1) + " " + routePieces[routePieces.length - 1];
                } else {
                    newRoute = routePieces[0];
                    if (newRoute.length() > 4) {
                        log.debug(Logs.INTERACTION, "New Route: " + newRoute);

                        String msg = getResources().getString(R.string.shuttle_invalid_route_code);
                        if (CommonUtility.checkVinNoPopup(newRoute)) {
                            msg += " " + getResources().getString(R.string.shuttle_invalid_code_looks_like_vin);
                        }
                        badScanValueMsg(scannedValue, msg);
                        break;
                    }
                }
                log.debug(Logs.INTERACTION, "New Route: " + newRoute);
                setRoute(newRoute);
                break;
        }

        scanningStatus = REQ_HW_BUTTON;
    }

    @Override
    public void onScanFailureRunOnUiThread(String errorMsg) {
        errorMsg = "No barcode was scanned. Try again.";
        if (scanningStatus == REQ_HW_BUTTON) {
            super.onScanFailureRunOnUiThread(errorMsg);
        }
        else {
            scanningStatus = REQ_HW_BUTTON;
            CommonUtility.simpleMessageDialog(this, errorMsg);
        }
    }

    private boolean isComplete(boolean showPopup) {
        boolean retVal = true;
        String vinText = vinTextView.getText().toString();
        String routeText = routeEditText.getText().toString();
        String prodStatusText = prodStatusEditText.getText().toString();
        String popupMsg = null;

        if (HelperFuncs.isNullOrEmpty(vinText)
                || HelperFuncs.isNullOrEmpty(routeText)
                || HelperFuncs.isNullOrEmpty(prodStatusText)) {
            log.debug(Logs.INTERACTION, "message: " + "Values are missing.");
            popupMsg = "All fields must be filled in";
            retVal = false;
        }
        else if (!CommonUtility.checkVinNoPopupNoLogging(vinText)) {
            popupMsg = vinText + " is not a valid VIN.";
            retVal = false;
        }
        if (showPopup && popupMsg != null) {
            CommonUtility.simpleMessageDialog(this, popupMsg);
        }
        return retVal;
    }

    private boolean isEmpty() {
        String vinText = vinTextView.getText().toString();
        String routeText = routeEditText.getText().toString();
        String prodStatusText = prodStatusEditText.getText().toString();

        if (HelperFuncs.isNullOrEmpty(vinText)
                && HelperFuncs.isNullOrEmpty(routeText)
                && HelperFuncs.isNullOrEmpty(prodStatusText)) {
            return true;
        } else {
            return false;
        }
    }

    public void back(View v) {
        if ((!HelperFuncs.isNullOrEmpty(vinTextView.getText().toString()))
                || !HelperFuncs.isNullOrEmpty(routeEditText.getText().toString())
                || ! (HelperFuncs.isNullOrEmpty(prodStatusEditText.getText().toString())
                      || prodStatusEditText.getText().toString().equalsIgnoreCase(defaultProdStatus))){
            AlertDialog.Builder builder = new AlertDialog.Builder(CommonUtility.getCurrentActivity());
            String promptMessage = "Would you like to save this VIN?";
            log.debug(Logs.INTERACTION, "Would you like to save this VIN?");

            builder.setMessage(promptMessage);
            builder.setPositiveButton("Yes", (dialog, which) -> {
                log.debug(Logs.INTERACTION, "user clicked 'yes'");
                saveVin(true);
            });
            builder.setNegativeButton("No", (dialog, which) -> {
                log.debug(Logs.INTERACTION, "user clicked 'no'");
                finishActivity();
            });
            //builder.setCancelable(true);
            builder.create().show();
        } else {
            finishActivity();
        }
    }

    private void saveShuttleLoadStartEvent(Load load, String vinNumber) {
        log.debug(Logs.INTERACTION, "first inspection for load " + load.loadNumber);
        SimpleTimeStamp sts = new SimpleTimeStamp();
        HashMap<String,String> reqBody = new HashMap<>();

        Location currentLocation = locationHandler.getLocation();

        String eventString = TextUtils.join(",",
                new String[]{
                        "PS",
                        load.driver.driverNumber,
                        load.loadNumber,
                        vinNumber,
                        sts.getUtcDateTime(),
                        sts.getUtcTimeZone(),
                        String.valueOf(currentLocation.getLatitude()),
                        String.valueOf(currentLocation.getLongitude()),
                        String.format("%s%s", HelperFuncs.noNull(shuttleMove.getTerminal()).trim(),
                                        HelperFuncs.noNull(shuttleMove.getMoveCode())),
                        TruckNumberHandler.getTruckNumber(getApplicationContext())
                }) ;

        reqBody.put("csv", eventString);

        LoadEvent event = new LoadEvent();
        event.csv = eventString;
        DataManager.insertLoadEvent(this, event);
        SyncManager.pushLoadEventsLatched(getApplicationContext());
    }

    @Override
    public void onBackPressed() {
        back(null);
    }

    public void showVinNotAllowedDialog(String vin, String msg) {
    }

    public boolean checkIfVinOnDisallowedList(String vin) {
        final int MAX_VIN_LEN = 17;
        String msg = null;

        if (vin.length() < MAX_VIN_LEN) {
            return false;
        }
        String[] list = AppSetting.SHUTTLE_DO_NOT_SHIP_LIST.getString().split("\\|");
        for (int i = 0; i < list.length; i++) {
            String entry = list[i].strip();
            String vinPart = StringUtils.substringBefore(entry, " ");
            String msgPart = StringUtils.substringAfter(entry, " ");
            int len = vinPart.length();
            if (len <= MAX_VIN_LEN && len >= AppSetting.SHUTTLE_DO_NOT_SHIP_MIN_VIN_LEN.getInt()
                && vinPart.equalsIgnoreCase(vin.substring(MAX_VIN_LEN-len))) {
                    if (HelperFuncs.isNullOrWhitespace(msgPart)) {
                        msg = "VIN is on DO NOT SHIP list";
                    }
                    else {
                        msg = msgPart;
                    }
                    break;
            }
        }
        if (msg == null) {
            return false;
        }
        CommonUtility.simpleMessageDialog(this,
                    String.format("%s\n\nReason: %s",
                        getResources().getString(R.string.shuttle_vin_on_do_not_ship_list), msg),
                    "VIN #" + vin);
        return true;
    }

}


