package com.cassens.autotran.activities;

import static com.cassens.autotran.BuildConfig.AUTOTRAN_API_URL;

import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.BuildConfig;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.R;
import com.cassens.autotran.backendpoc.PoCTabletStatus;
import com.cassens.autotran.backendpoc.PoCUtils;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.handlers.TruckNumberHandler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UtilitiesActivity extends ProgressDialogActivity implements OnClickListener {
    private static final Logger log = LoggerFactory.getLogger(UtilitiesActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    public static final int RESULT_LOGOFF = 1000;

    String driverNumber = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utilities);
        ((ImageView) findViewById(R.id.img_back)).setOnClickListener(this);
        ((Button) findViewById(R.id.btnclearload)).setOnClickListener(this);
        ((Button) findViewById(R.id.btnupload)).setOnClickListener(this);
        ((Button) findViewById(R.id.btnlogoff)).setOnClickListener(this);
        ((Button) findViewById(R.id.btnsetdefaults)).setOnClickListener(this);
        ((Button) findViewById(R.id.btncheckScanner)).setOnClickListener(this);
        ((Button) findViewById(R.id.sendLogs)).setOnClickListener(this);
        ((Button) findViewById(R.id.displayDamageCodes)).setOnClickListener(this);
        ((Button) findViewById(R.id.problemReport)).setOnClickListener(this);
        ((Button) findViewById(R.id.tabletStatsReport)).setOnClickListener(this);
        ((Button) findViewById(R.id.testError)).setOnClickListener(this);
        ((Button) findViewById(R.id.toggleDrivingSimulation)).setOnClickListener(this);
        /*
		Button clearLogs = ((Button) findViewById(R.id.clearLogs));
		clearLogs.setOnClickListener(this);
		clearLogs.setText("Clear " + CommonUtility.getLogFileSize() + "K of log files");
		 */

        setSimulateDrivingToggleButton();

        driverNumber = getIntent().getStringExtra("driverNumber");
    }



    @Override
    protected void onResume() {

        try {
            ((TextView) findViewById(R.id.deviceInfo)).setText(
                    String.format("AutoTran Version: %s Build %s\nVersionCode: %d\nDevice MAC Address: %s\nDevice Serial: %s\nTruck Number: %s\nServer: %s",
                            getPackageManager().getPackageInfo(getPackageName(), 0).versionName, BuildConfig.BUILD_NUMBER, BuildConfig.VERSION_CODE,
                            CommonUtility.getMACAddress(), CommonUtility.getDeviceSerial(), TruckNumberHandler.getTruckNumber(this), backendServerInfo()));
        } catch (PackageManager.NameNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        super.onResume();
    }

    private String backendServerInfo() {
        if (AUTOTRAN_API_URL.contains("autotranadmin-env-9rvvkiwnzp.elasticbeanstalk")) {
            return "autotran-prod";
        }
        else if (AUTOTRAN_API_URL.contains("autotranadmin-env-php74-no-lb.us-east-1.elasticbeanstalk.com")) {
            return "autotran-test-php-7.4-no-lb";
        }
        else if (AUTOTRAN_API_URL.contains("autotranadmin-env-php74.h2q2zpazim.us-east-1.elasticbeanstalk")) {
            return "autotran-test-php-7.4";
        }
        else if (AUTOTRAN_API_URL.contains("autotran-test.sdgsystems.net")) {
            return "autotran-test";
        }
        else if (AUTOTRAN_API_URL.contains("sdgsystems.net")) {
            return AUTOTRAN_API_URL.replaceAll("https://sdgsystems.net/", "").replaceAll("/", "");
        }
        return "unknown";
    }

    @Override
    public void onClick(View v) {
        Intent intent;

        if (v.getId() == R.id.img_back) {
            CommonUtility.logButtonClick(log, "Back");
            this.finish();
            return;
        }
        CommonUtility.logButtonClick(log, v);
        switch (v.getId()) {
            case R.id.btnclearload:
                intent = new Intent(this, ScanSupervisorCodeActivity.class);
                intent.putExtra("cameFrom", ClearLoadActivity.OPERATION_CLEAR_LOAD);
                intent.putExtra("driverNumber", driverNumber);
                startActivity(intent);
                break;

            case R.id.btnupload:
                intent = new Intent(this, UploadStatusActivity.class);
                intent.putExtra("driverNumber", driverNumber);
                startActivity(intent);
                break;

            case R.id.btnlogoff:
                setResult(RESULT_LOGOFF);
                this.finish();
                break;

            case R.id.btnsetdefaults:
                intent = new Intent(this, SetTerminalActivity.class);
                intent.putExtra("cameFrom", Constants.CAME_FROM_SETDEFAULTS);
                startActivity(intent);
                break;

            case R.id.btncheckScanner:
                intent = new Intent(this, CheckScannerActivity.class);
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_HISTORY);

                intent.putExtra("cameFrom", Constants.CAME_FROM_CHECKSCANNER);
                startActivity(intent);
                break;

            case R.id.sendLogs:
                CommonUtility.showText("Sending application logs to Cassens...");

                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        CommonUtility utility = new CommonUtility();
                        utility.sendLogList(driverNumber, UtilitiesActivity.this, "both");
                    }
                });
                thread.start();
                break;

            case R.id.displayDamageCodes:
                intent = new Intent(this, DamageCodeTabbedListActivity.class);
                //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_NO_HISTORY);

                intent.putExtra("cameFrom", Constants.CAME_FROM_CHECKSCANNER);
                startActivity(intent);
                break;

            case R.id.problemReport:
                intent = new Intent(this, ProblemReportActivity.class);
                startActivity(intent);
                break;

            case R.id.tabletStatsReport:
                new ReportTabletStatusTask().execute();
                break;

            case R.id.testError:
                //generateUncaughtException();
                AlertDialog.Builder builder = new AlertDialog.Builder(this);

                Load latestLoad = DataManager.getLastCompletedLoadForDriver(this, DataManager.getUserForDriverNumber(this, driverNumber).user_id);
                if(latestLoad != null)
                    builder.setMessage("Latest delivered load is " + latestLoad.loadNumber);
                else
                    builder.setMessage("No delivered loads");

                builder.show();
                break;

            case R.id.toggleDrivingSimulation:
                AutoTranApplication.toggleSimulateDriving();
                setSimulateDrivingToggleButton();
                break;

            default:
                break;
        }
    }

    private void setSimulateDrivingToggleButton() {
        Button button = (Button)findViewById(R.id.toggleDrivingSimulation);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (AutoTranApplication.simulatingDriving()) {
                    button.setText("Stop Driving Simulation");
                    button.invalidate();
                }
                else {

                    button.setText("Simulate Driving");
                    button.invalidate();
                }
            }
        });
    }

    public void generateUncaughtException() {
        //Log.d("narf", "Generating a null pointer exception");
        returnNull().trim();
    }

    public String returnNull() {
        return null;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		if (resultCode == 11)
//		{
//			((EditText) findViewById(R.id.et_notes_message)).setText(data.getStringExtra("note"));
//		}
        super.onActivityResult(requestCode, resultCode, data);
    }

    // AsyncTask to execute the SQLite query in the background
    private class ReportTabletStatusTask extends AsyncTask<Void, Void, PoCTabletStatus> {

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            showProgressDialog("Reporting tablet status...");
        }

        @Override
        protected PoCTabletStatus doInBackground(Void... params) {
            // Perform your long-running SQLite query here
            // Replace the comment below with your actual SQLite query code
            // For example, you can use a SQLiteOpenHelper or SQLiteDatabase instance

            // SQLiteDatabase db = dbHelper.getReadableDatabase();
            // Cursor cursor = db.rawQuery("SELECT * FROM your_table", null);

            // Process the query result or perform other necessary operations


            PoCTabletStatus tabletStatus = PoCUtils.getTabletStatus(AutoTranApplication.getAppContext());
            PoCUtils.sendLambdaReportTabletStatusRequest(AutoTranApplication.getAppContext(), tabletStatus);

            return tabletStatus;
        }

        @Override
        protected void onPostExecute(PoCTabletStatus tabletStatus) {
            super.onPostExecute(null);
            // This method is called on the main thread after the background task is completed
            // You can update the UI or perform any other post-execution tasks here
            dismissDialog();
            CommonUtility.simpleMessageDialog(UtilitiesActivity.this, tabletStatus.toString(), null, true);
        }
    }

}
