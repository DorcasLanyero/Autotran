package com.cassens.autotran.activities;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.adapters.DeliveryVinAdapter;
import com.cassens.autotran.data.adapters.HighlightArrayAdapter;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.tasks.RemoteSyncTask;
import com.cassens.autotran.dialogs.LookUpScreenDialog;
import com.cassens.autotran.handlers.ImageHandler;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class ShuttleBuildLoadActivity extends GenericScanningActivity {
    private static final Logger log = LoggerFactory.getLogger(ShuttleBuildLoadActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private static final String TAG = "ShuttleLoadInspectionAct";
    ImageHandler imageHandler;

    private int loadId;
    private Load load;
    private DeliveryVinAdapter adapter;

    private TextView loadNumTextView;
    private TextView shuttleBuildTitle;
    private TextView vehiclesAdded;
    protected Spinner numVehiclesSpinner;
    private HighlightArrayAdapter numVehiclesAdapter;
    private TextView useDropdownMsg;
    private Button addVehiclesButton;
    protected ListView vinListView;
    protected Button auditButton;
    protected Button saveButton;
    protected TextView messageText;

    private String currentPhotoFileName;
    private int vehicleCount = 0;
    private Activity context = CommonUtility.getCurrentActivity();
    private int position = 0;
    private LookUpScreenDialog lookUpScreen;

    // Request Codes for Launched Activities
    private static final int REQ_CODE_HIGH_CLAIMS_AUDIT = 1000;
    public static final int REQ_CODE_VIN_INSPECTION = 1002;
    private static final int REQ_CODE_SIGNATURE = 1003;
    private static final int REQ_CODE_ADD_VIN = 1004;
    private User driver;
    private BroadcastReceiver deliveryVinChangedReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();

        setContentView(R.layout.activity_shuttle_build_load);

        mManualEntryEnabled = true;
        mScanDataType = ScanDataType.FULL_VIN;

        loadId = bundle.getInt(Constants.CURRENT_LOOKUP_ID);
        vehicleCount = bundle.getInt(Constants.PREF_VEHICLE_COUNT);

        deliveryVinChangedReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                drawLayout();
            }
        };
        LocalBroadcastManager.getInstance(this).registerReceiver(deliveryVinChangedReceiver, new IntentFilter(Constants.DELIVERY_VIN_CHANGED));

        loadNumTextView = findViewById(R.id.loadNumber);
        shuttleBuildTitle = findViewById(R.id.shuttleBuildTitle);
        vehiclesAdded  = findViewById(R.id.vehiclesAddedTextView);
        numVehiclesSpinner = findViewById(R.id.numVehiclesSpinner);
        useDropdownMsg  = findViewById(R.id.useDropdownMessage);
        addVehiclesButton = findViewById(R.id.addVehiclesButton);
        vinListView = findViewById(R.id.vinListView);
        saveButton = findViewById(R.id.saveButton);
        auditButton = (Button) findViewById(R.id.auditButton);
        messageText = findViewById(R.id.messageText);
        auditButton.setVisibility(View.GONE);

        drawLayout();

        imageHandler = new ImageHandler(this);
    }

    @Override
    protected void onPause() {
        if (lookUpScreen != null) {
            lookUpScreen.canceled();
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (deliveryVinChangedReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(deliveryVinChangedReceiver);
        }
        super.onDestroy();
    }

    private int getNumVehiclesSpinnerPosition(List<String> currentAdapterArray) {
        position = 0;
        for (String value : currentAdapterArray ){
            if(value.equals(String.valueOf(getNumVehiclesSharedPref()))){
                return position;
            }
            position += 1;
        }
        return position;
    }

    private List<String> generateNumVehiclesSelectionList(int numVehiclesAdded){
        List<String> vehicleAdapterArray = new ArrayList<String>();
        for (int i = numVehiclesAdded; i <= Constants.MAX_VEHICLES_ON_TRUCK; i++) {
            if (i > 0) {
                vehicleAdapterArray.add(String.valueOf(i));
            }
        }
        return vehicleAdapterArray;
    }

    protected void drawLayout() {
        load = DataManager.getLoad(this, loadId);
        if (load == null) {
            // If this is null, we can't really do much
            CommonUtility.showText(String.format("Error: Unable to access load (loadId=%d)", loadId), Toast.LENGTH_LONG);
            finish();
            return;
        }
        if (load.deliveries == null || load.deliveries.size() == 0) {
            Delivery d = new Delivery();
            d.shuttleLoad = true;
            load.deliveries = new ArrayList<>();
            load.deliveries.add(d);
        }
        if (load.deliveries.get(0).deliveryVins == null) {
            load.deliveries.get(0).deliveryVins = new ArrayList<>();
        }

        //Set the load number
        if (CommonUtility.isHoneywellLargeDisplaySet()) {
            loadNumTextView.setTextSize(14);
        }
        else {
            loadNumTextView.setTextSize(15);
        }
        loadNumTextView.setVisibility(View.VISIBLE);
        loadNumTextView.setText(load.loadNumber);

        List <String> numVehiclesSelectionList = generateNumVehiclesSelectionList(load.deliveries.get(0).getDeliveryVinList().size());
        numVehiclesAdapter = new HighlightArrayAdapter(this, R.layout.spinner_item, numVehiclesSelectionList);
        numVehiclesSpinner.setAdapter(numVehiclesAdapter);
        numVehiclesSpinner.setSelection(getNumVehiclesSpinnerPosition(numVehiclesSelectionList));
        numVehiclesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                String selectedValue = numVehiclesSpinner.getSelectedItem().toString();
                if (Integer.parseInt(selectedValue) < load.deliveries.get(0).getDeliveryVinList().size()) {
                    CommonUtility.simpleMessageDialog(CommonUtility.getCurrentActivity(), "You cannot select a value  less than the number of VINs scanned");
                    numVehiclesSpinner.setSelection(getNumVehiclesSpinnerPosition(generateNumVehiclesSelectionList(load.deliveries.get(0).getDeliveryVinList().size())));
                } else {
                    setNumVehiclesSharedPref(Integer.parseInt(selectedValue));
                    setButtonVisibility();
                }
                numVehiclesAdapter.setSelection(position);
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        vehiclesAdded.setText(String.valueOf(load.deliveries.get(0).getDeliveryVinList().size()));

        auditButton.setVisibility(View.GONE);

        ((TextView) findViewById(R.id.titleTextView)).setText("Shuttle Load");

        addVehiclesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonUtility.logButtonClick(log, v);
                startAddVehiclesActivity(null);
            }
        });
        setButtonVisibility();

        adapter = new DeliveryVinAdapter(this, R.layout.shuttle_load_vin_list_element,
                                        load.getDeliveryVinList(false),
                                        Constants.SHUTTLE_LOAD_OPERATION,
                                        new DeliveryVinAdapter.DeliveryVinAdapterCallback() {
                                            @Override
                                            public void onSelect(DeliveryVin deliveryVin) {
                                                startInspectionForDeliveryVin(deliveryVin);
                                            }
                                        });
        adapter.sort(new DeliveryVinPositionComparator());
        vinListView.setAdapter(adapter);
    }

    private void setButtonVisibility() {
        if (load.deliveries.get(0).getDeliveryVinList().size() >= 1
                && load.deliveries.get(0).isShuttleLoadInspected()) {

            if (driver == null) {
                driver = DataManager.getUser(this, String.valueOf(load.driver_id));
            }

            if (load.deliveries.get(0).getDeliveryVinList().size() != getNumVehiclesSharedPref()) {
                addVehiclesButton.setVisibility(View.VISIBLE);
                useDropdownMsg.setVisibility(View.GONE);
                auditButton.setVisibility(View.GONE);
                saveButton.setVisibility(View.GONE);
                shuttleBuildTitle.setText(R.string.shuttle_load_default_title);
            }
            else {
                addVehiclesButton.setVisibility(View.GONE);
                useDropdownMsg.setVisibility(View.VISIBLE);
                shuttleBuildTitle.setText(R.string.shuttle_load_complete_title);
                messageText.setVisibility(View.GONE);
                if (load.requiresSignature()) {
                    auditButton.setVisibility(View.GONE);
                    saveButton.setVisibility(View.GONE);
                    messageText.setVisibility(View.VISIBLE);
                }
                else {
                    if (driver.requiresAnAudit()) {
                        if (HelperFuncs.isNullOrEmpty(load.driverHighClaimsAudit) || HelperFuncs.isNullOrEmpty(load.supervisorSignature)) {
                            auditButton.setText(R.string.proceed_audit_checklist);
                            saveButton.setVisibility(View.GONE);
                        } else {
                            auditButton.setText(R.string.return_supervisor_audit);
                            saveButton.setVisibility(View.VISIBLE);
                        }
                        auditButton.setVisibility(View.VISIBLE);
                    } else {
                        auditButton.setVisibility(View.GONE);
                        saveButton.setVisibility(View.VISIBLE);
                    }
                }
            }
        }
    }

    private String getDelimitedVins(String s) {
        StringBuilder sb = new StringBuilder();
        List<DeliveryVin> vins = load.getDeliveryVinList(false);

        for (DeliveryVin dv : vins) {
            if (dv.vin != null && !HelperFuncs.isNullOrEmpty(dv.vin.vin_number) && !dv.pro.equals("deleted")) {
                sb.append(dv.vin.vin_number + s);
            }
        }

        log.debug(Logs.DEBUG, "VinList String: " + sb.toString());

        return sb.toString();
    }

    public void onSaveButton(View v) {
        //Display appropriate messages
        if (load.deliveries.get(0).deliveryVins.size() != getNumVehiclesSharedPref() ) {
            CommonUtility.simpleMessageDialog(this, "Number of vehicles scanned does not match the number expected. Please check to ensure you have scanned all the required vehicles before proceeding. If the expected number of vehicles is incorrect, please update it to the correct number.");
            return;
        }
        proceedToSignatureActivity();
    }

    private void proceedToSignatureActivity() {
        if (driver == null) {
            driver = DataManager.getUser(this, String.valueOf(load.driver_id));
        }

        int proIndex = 1;
        for(DeliveryVin dvin : load.deliveries.get(0).deliveryVins) {
            if(!dvin.pro.equals("deleted")) {
                dvin.pro = String.valueOf(proIndex);
                proIndex++;
            }
        }

        DataManager.insertLoadToLocalDB(this, load);
        boolean lookupShown = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(load.load_id + "_delivery_lookup_shown", false);

        Bundle bundle = new Bundle();
        bundle.putString("user_type", "driver");
        bundle.putInt("load_id", load.load_id);
        bundle.putInt(Constants.CURRENT_OPERATION, Constants.PRELOAD_OPERATION);
        Intent intent = new Intent(this, SignatureActivity.class).putExtras(bundle);
        lookUpScreen = new LookUpScreenDialog(CommonUtility.getCurrentActivity(), new LookUpScreenDialog.LookUpScreenCallback() {
            @Override
            public void proceed() {
                startActivityForResult(intent, REQ_CODE_SIGNATURE);
            }
        });
        if (!lookupShown) {
            lookUpScreen.showLookupScreen(CommonUtility.getCurrentActivity(), String.valueOf(load.load_id), Constants.SHUTTLE_LOAD_OPERATION, load);
        } else {
            startActivityForResult(intent, REQ_CODE_SIGNATURE);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        switch (requestCode) {
            case REQ_CODE_ADD_VIN:
                if (resultCode != RESULT_OK) {
                    break;
                }
                // Fall through (no break)
            case REQ_CODE_VIN_INSPECTION:
                if (resultCode != RESULT_OK) {
                    break;
                }
                drawLayout();
                break;
            case REQ_CODE_SIGNATURE:
                if (resultCode != RESULT_OK) {
                    break;
                }
                load = DataManager.getLoad(this, loadId);
                log.debug(Logs.DEBUG, "signed at: " + load.driverPreLoadSignatureSignedAt);
                if (!HelperFuncs.isNullOrEmpty(load.driverPreLoadSignature)) {
                    for (DeliveryVin dv: load.getDeliveryVinList()) {
                        addVinToRecentlyShuttledList(dv.vin.vin_number);
                    }
                    CommonUtility.highLevelLog("Calling RemoteSyncTask() to save shuttle load $loadNumber", load);

                    setResult(RESULT_OK);

                    //upload the shuttle load to the server at this point
                    RemoteSyncTask syncTask = new RemoteSyncTask(this);
                    LookUpScreenDialog.cleanLookUpEntry(String.valueOf(load.load_id));
                    syncTask.execute(load.driver_id);

                    finish();
                }
                break;

            case REQ_CODE_HIGH_CLAIMS_AUDIT:
                if (resultCode == RESULT_OK || resultCode == HighClaimsAuditActivity.RESULT_CODE_AUDIT_DID_NOT_PASS) {
                    if (resultCode == HighClaimsAuditActivity.RESULT_CODE_AUDIT_DID_NOT_PASS) {
                        log.debug(Logs.INTERACTION, "got high claims audit with supervisor corrections");
                    }
                    else {
                        log.debug(Logs.INTERACTION, "got high claims audit");
                    }
                    drawLayout();
                }
        }
    }

    private void generateFilename() {
        currentPhotoFileName = Constants.PRELOAD_IMAGE_FILE_PREFIX + load.loadNumber + Constants.IMAGE_FILE_DELIM + UUID.randomUUID().toString();
    }

    private void startHighClaimsAuditActivity() {
        if (load == null) {
            log.debug(Logs.INTERACTION, "HighClaimsActivity attempted but load was null. Doing nothing.");
            return;
        }
        log.debug(Logs.INTERACTION, "Starting HighClaimsAuditActivity.");

        Bundle bundle = new Bundle();
        bundle.putInt("load_id", load.load_id);
        Intent intent = new Intent(this, HighClaimsAuditActivity.class)
                .putExtras(bundle);
        this.startActivityForResult(intent, REQ_CODE_HIGH_CLAIMS_AUDIT);
    }

    public void onProceedToAuditButton(View v) {
        startHighClaimsAuditActivity();
    }

    private void setNumVehiclesSharedPref(int numVehicles) {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putInt(Constants.PREF_VEHICLE_COUNT, numVehicles )
                .commit();
    }

    private int getNumVehiclesSharedPref() {
        return PreferenceManager.getDefaultSharedPreferences(this).getInt(Constants.PREF_VEHICLE_COUNT, 0);
    }

    public void back(View v) {
        log.debug(Logs.INTERACTION, "Back pressed");
        onBackPressed();
    }

    @Override
    public void onBackPressed() {
        Intent backIntent = new Intent(this, ShuttleSelectLoadActivity.class);
        backIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
        Bundle extras = getIntent().getExtras();
        extras.remove(Constants.CURRENT_LOOKUP_ID);
        extras.remove(Constants.PREF_VEHICLE_COUNT);
        backIntent.putExtras(extras);
        startActivity(backIntent);
        finish();
    }

    private static class DeliveryVinPositionComparator implements Comparator<DeliveryVin> {
        @Override
        public int compare(DeliveryVin lhs,
                           DeliveryVin rhs) {

            try {
                if (lhs.position == null || lhs.position.equals("null") || lhs.position.trim().equals("")) {
                    return 0;
                } else if (rhs.position == null || rhs.position.equals("null") || rhs.position.trim().equals("")) {
                    return 1;
                } else if (Integer.valueOf(lhs.position) > Integer.valueOf(rhs.position))
                    return 1;
                else {
                    return -1;
                }
            } catch (Exception e) {
                // Can occur on range entries (e.g. "4-11")
                return lhs.position.compareTo(rhs.position);
            }
        }

    }

    private static final String RECENTLY_SHUTTLED_VINS = "recentlyShuttledVins";

    private void addVinToRecentlyShuttledList(String scannedVinBarcode) {
        SharedPreferences recentVinsPrefs = getSharedPreferences(RECENTLY_SHUTTLED_VINS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = recentVinsPrefs.edit();

        Date currentDate = new Date();

        Map<String, ?> allEntries = recentVinsPrefs.getAll();
        ArrayList<String> vinlist = new ArrayList<String>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            vinlist.add(entry.getKey());
        }

        if (!vinlist.contains(scannedVinBarcode)) {
            editor.putString(scannedVinBarcode, Long.toString(currentDate.getTime()));
            editor.commit();
        }
        else if (vinShuttledRecently(this, scannedVinBarcode)) {
            editor.remove(scannedVinBarcode);
            editor.commit();

            editor.putString(scannedVinBarcode, Long.toString(currentDate.getTime()));
            editor.commit();
        }
    }

    public static boolean vinShuttledRecently(Context context, String scannedVinBarcode) {
        SharedPreferences recentVinsPrefs = context.getSharedPreferences(RECENTLY_SHUTTLED_VINS, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = recentVinsPrefs.edit();

        Long age;
        boolean shuttledRecently = false;
        Date currentDate = null;
        Long scannedTimeInMillisecond, currentTimeInMilliseconds;

        Map<String, ?> allEntries = recentVinsPrefs.getAll();
        ArrayList<String> vinlist = new ArrayList<String>();
        for (Map.Entry<String, ?> entry : allEntries.entrySet()) {
            vinlist.add(entry.getKey());
        }

        for (String vinEntry : vinlist) {

            scannedTimeInMillisecond = Long.parseLong(recentVinsPrefs.getString(vinEntry, ""));
            currentDate = new Date();
            currentTimeInMilliseconds = currentDate.getTime();

            age = TimeUnit.HOURS.convert(Math.abs(currentTimeInMilliseconds - scannedTimeInMillisecond), TimeUnit.MILLISECONDS);

            if (age >= AppSetting.SHUTTLE_REPEAT_SHIP_WARNING_HRS.getInt()) {
                editor.remove(vinEntry);
                editor.commit();
            } else if (scannedVinBarcode.equalsIgnoreCase(vinEntry)) {
                shuttledRecently = true;
            }
        }
        return shuttledRecently;
    }

    @Override
    public void onScanFailureRunOnUiThread(String errorMsg) {
        super.onScanFailureRunOnUiThread("No VIN was scanned");
    }


    @Override
    protected void validateScannedValue(String scannedValue) throws GenericScanningActivity.ScannedValueException {
        if (HelperFuncs.isNullOrEmpty(scannedValue)) {
            throw new ScannedValueException(ScannedValueException.EMPTY_VALUE_SCANNED, "Empty Value Scanned");
        }
        if (!CommonUtility.checkVinNoPopup(scannedValue)) {
            String failureMessage = "Error: Not a valid VIN";
            log.debug(Logs.DEBUG, failureMessage + "(vinNumber=" + HelperFuncs.noNull(scannedValue, "NULL") + ")");
            throw new ScannedValueException(ScannedValueException.INVALID_VALUE_SCANNED, failureMessage);
        }
    }

    @Override
    protected void onScanResultRunOnUiThread(String barcode) throws ScannedValueException {
        String vinNumber = CommonUtility.processScannedVIN(barcode);

        DeliveryVin dv = adapter.getDeliveryVinForVin(vinNumber);
        if (dv == null) {
            startAddVehiclesActivity(vinNumber);
        }
        else {
            startInspectionForDeliveryVin(dv);
        }
    }

    public void startInspectionForDeliveryVin (DeliveryVin deliveryVin) {
        CommonUtility.showText("Starting inspection for vin " + deliveryVin.vin.vin_number);

        Intent intent = new Intent(this, VINInspectionActivity.class);
        intent.putExtra("vin_number", deliveryVin.vin.vin_number);
        intent.putExtra("vin_desc", deliveryVin.vin.getDescription());
        intent.putExtra("delivery_vin_id", deliveryVin.delivery_vin_id);
        intent.putExtra(Constants.CURRENT_OPERATION, Constants.SHUTTLE_LOAD_OPERATION);
        this.startActivityForResult(intent, ShuttleBuildLoadActivity.REQ_CODE_VIN_INSPECTION);
    }

    public void startAddVehiclesActivity(String newVin) {
        int vehicleCount = getNumVehiclesSharedPref();
        int vehiclesInLoad = load.deliveries.get(0).getDeliveryVinList().size();
        if ((vehiclesInLoad == vehicleCount) && vehicleCount < Constants.MAX_VEHICLES_ON_TRUCK) {
            CommonUtility.simpleMessageDialog(CommonUtility.getCurrentActivity(), "You have already scanned the expected number of vehicles. If the expected number of vehicles is incorrect, please update it to the correct number."
            );
        } else if ((vehiclesInLoad == vehicleCount) && vehicleCount == Constants.MAX_VEHICLES_ON_TRUCK) {
            CommonUtility.simpleMessageDialog(this, "You have already scanned the maximum number of vehicles allowed for a single shuttle load.");
        }
        else {
            Intent intent = new Intent(this, ShuttleAddVinsActivity.class);
            intent.putExtra(ShuttleAddVinsActivity.EXTRA_VINS, getDelimitedVins(","));
            intent.putExtra(ShuttleAddVinsActivity.EXTRA_SHUTTLE_MOVE, this.load.shuttleMove.orgDestString);
            intent.putExtra(ShuttleAddVinsActivity.EXTRA_LOAD_ID, load.load_id);
            if (newVin != null) {
                intent.putExtra(ShuttleAddVinsActivity.EXTRA_NEW_VIN, newVin);
            }
            startActivityForResult(intent, REQ_CODE_ADD_VIN);
        }
    }

}