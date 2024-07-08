package com.cassens.autotran.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.cassens.autotran.BuildConfig;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.Load;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressLint("UseSparseArrays")
public class ScanSupervisorCodeActivity extends NfcScanningActivity {
    private static final Logger log = LoggerFactory.getLogger(ScanSupervisorCodeActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private static final String TAG = "CheckScannerActivity";
    private String mSupervisorPrefix;

    private String cameFrom;
    private String driverNumber;
    private Load thisLoad;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan_supervisor_code);

        TextView scanSupervisorPrompt;

        scanSupervisorPrompt = (TextView) findViewById(R.id.scanSupervisorPrompt);

        if (getIntent().hasExtra("cameFrom")) {
            cameFrom = getIntent().getStringExtra("cameFrom");
        }
        if (cameFrom == null) {
            cameFrom = ""; // To avoid null checks
        }
        if (cameFrom.equals(ClearLoadActivity.OPERATION_CLEAR_LOAD)) {
            scanSupervisorPrompt.setText("You must scan a supervisor code to clear or delete loads");
        } else {
            scanSupervisorPrompt.setText("You must scan a supervisor code to continue");
        }

        if (cameFrom.equals(DeliveryVinInspectionActivity.OPERATION_INSPECTION)) {
            thisLoad = DataManager.getLoad(this, getIntent().getIntExtra("loadId", -1));
        }

        if (getIntent().hasExtra("driverNumber")) {
            driverNumber = getIntent().getStringExtra("driverNumber");
        }
        if (BuildConfig.AUTOTRAN_SUPERVISOR_SCAN_OPTIONAL) {
            findViewById(R.id.SimulateSupervisorScanButton).setVisibility(View.VISIBLE);
        }
        mSupervisorPrefix = getResources().getString(R.string.supervisor_code_prefix);
    }

    @Override
    protected void onScanResultRunOnUiThread(String barcode) {

        if (barcode.contains(mSupervisorPrefix)) {
            User supervisor = DataManager.getUserForSupervisorCode(ScanSupervisorCodeActivity.this, barcode);
            if (supervisor != null && supervisor.status.equals("1")) {
                log.debug(Logs.INTERACTION, "user scanned active supervisor code: " + barcode);
                supervisorCodeAccepted(barcode);
            } else {
                CommonUtility.showText("Not an active supervisor ID!");
                log.debug(Logs.INTERACTION, "scanned code: " + barcode + " is NOT an active supervisor id");
            }
        } else {
            CommonUtility.showText("Not a valid supervisor ID!");
            log.debug(Logs.INTERACTION, "scanned code: " + barcode + " is not a valid supervisor id");
        }
    }

    private void supervisorCodeAccepted(String barcode) {
        if (cameFrom.equals(ClearLoadActivity.OPERATION_CLEAR_LOAD)) {

            Intent intent = new Intent(ScanSupervisorCodeActivity.this, ClearLoadActivity.class);
            intent.putExtra("cameFrom", ClearLoadActivity.OPERATION_CLEAR_LOAD);
            intent.putExtra("driverNumber", driverNumber);
            intent.putExtra("supervisorInfo", barcode);
            startActivity(intent);
            finish();

        } else if (cameFrom.equals(DeliveryVinInspectionActivity.OPERATION_INSPECTION)) {
            thisLoad.preloadSupervisorSignature = barcode;
            thisLoad.preloadSupervisorSignedAt = Constants.dateFormatter().format(HelperFuncs.getTimestamp());
            DataManager.insertLoadToLocalDB(ScanSupervisorCodeActivity.this, thisLoad);
            Intent returnIntent = getIntent();
            returnIntent.putExtra("supervisorInfo", barcode);
            setResult(RESULT_OK, returnIntent);
            finish();
        }
    }

    public void simulateSupervisorScan(View v) {
        if (BuildConfig.AUTOTRAN_VIN_SCAN_OPTIONAL) {
            supervisorCodeAccepted(mSupervisorPrefix + "99999");
        }
    }

    public void back(View v) {
        finish();
    }
}
