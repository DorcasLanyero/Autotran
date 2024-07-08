package com.cassens.autotran.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.adapters.CheckListAdapter;
import com.cassens.autotran.data.model.Prompt;
import com.cassens.autotran.data.model.Questionnaire;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.handlers.LocationHandler;
import com.cassens.autotran.hardware.PiccoloManager;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.AuditResponse;
import com.sdgsystems.util.Check;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HighClaimsAuditActivity extends NfcScanningActivity {

    // TODO: Rename HighClaimsAuditActivity to PreloadAuditActivity.
    //       Preload Audit is a more accurate name, since it can be required of drivers that
    //       are not high claims.
    private static final Logger log = LoggerFactory.getLogger(HighClaimsAuditActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private static final int REQ_CODE_NOTES = 1001;

    public static final int RESULT_CODE_AUDIT_DID_NOT_PASS = 2;

    private Load load;
    private CheckListAdapter listAdapter;
    private Button signoffButton;
    private Button noSupervisorButton;
    private TextView doneInstructions;
    private String notes;

    private boolean supervisorReview;
    ArrayList<Check> checks;
    private LocationHandler locationHandler;
    private int mQuestionnnaireVersion;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean auditAlreadyStarted = false;

        locationHandler = LocationHandler.getInstance(getApplicationContext());
        locationHandler.startLocationTracking();


        Bundle bundle = getIntent().getExtras();
        this.load = DataManager.getLoad(this, bundle.getInt("load_id", -1));

        if (this.load == null) {
            CommonUtility.showText("Could not find load.");
            finish();
            return;
        }

        supervisorReview = false;

        setContentView(R.layout.activity_high_claims_audit);
        ListView list = findViewById(R.id.checklist);
        View footerView = ((LayoutInflater)this.getSystemService(Context.LAYOUT_INFLATER_SERVICE)).inflate(R.layout.high_claims_footer, null, false);
        list.addFooterView(footerView);

        noSupervisorButton = (Button) findViewById(R.id.noSupervisorButton);
        signoffButton = (Button) findViewById(R.id.signoffButton);
        doneInstructions = (TextView) findViewById(R.id.doneInstructions);
        final ListView checklist = (ListView) findViewById(R.id.checklist);

        Questionnaire questionnaire = DataManager.getQuestionnaire(this.getApplicationContext(), Questionnaire.Type.PreloadAudit,true);
        mQuestionnnaireVersion = questionnaire.version;
        List<Prompt> prompts = questionnaire.parsePrompt();

        ArrayList<String> questions = new ArrayList<String>();
        for( Prompt prompt : prompts) {
            if (prompt != null) { // Prompts can be null if json had an empty entry
                questions.add(prompt.getText());
            }
        }
        checks = new ArrayList<Check>(questions.size());
        Map<Integer, Check> savedChecklist = new HashMap<Integer, Check>();

        if (!CommonUtility.isNullOrBlank(load.driverHighClaimsAudit)) {
            //Type CheckArrayListType = new TypeToken< ArrayList<Check> >(){}.getType();
            try {
                Gson gson = new Gson();
                AuditResponse ar = gson.fromJson(load.driverHighClaimsAudit, AuditResponse.class);

                for (Check check : ar.checkList) {
                    savedChecklist.put((Integer.valueOf(check.id)), check);
                }
                notes = ar.notes;
                auditAlreadyStarted = true;
            } catch (Exception e){
                log.debug(Logs.INTERACTION, "Encountered invalid JSON string in driverHighClaimsAudit.");
            }
        }

        for (int i = 0; i < questions.size(); i++) {
            boolean marked = false;
            if (auditAlreadyStarted) {
                Check savedCheck = savedChecklist.get(Integer.valueOf(i));
                if (savedCheck != null) {
                    marked = savedCheck.getMarked();
                }
            }
            checks.add(new Check(i, questions.get(i), marked));
        }

        this.listAdapter = new CheckListAdapter(this, R.layout.check_list, checks);
        checklist.setAdapter(this.listAdapter);

        noSupervisorButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtility.logButtonClick(log, noSupervisorButton);
                if(listAdapter.allChecksMarked()) {
                    new AlertDialog.Builder(HighClaimsAuditActivity.this)
                        .setMessage("You are required to have your load audited before leaving the terminal if there is a supervisor available.  Do you wish to continue?")
                        .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                SimpleTimeStamp currentTime = new SimpleTimeStamp();
                                Location currentLocation = locationHandler.getLocation();
                                load.supervisorSignature = "**** Driver performed a self-audit ****";
                                load.supervisorSignedAt = currentTime.getDateTime();
                                load.supervisorSignatureLat = (currentLocation != null) ? String.valueOf(currentLocation.getLatitude()) : "0.0";
                                load.supervisorSignatureLon =  (currentLocation != null) ? String.valueOf(currentLocation.getLongitude()) : "0.0";
                                saveAndFinish();
                            }
                        })
                        .setNegativeButton("No", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        }).show();
                    return;
                } else {
                    promptForCompletion();
                    return;
                }
            }
        });

        signoffButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtility.logButtonClick(log, signoffButton);
                if(listAdapter.checklistComplete()) {
                    checklist.setSelection(0);
                    showSupervisorSignoffView();
                } else {
                    promptForCompletion();
                    return;
                }
            }
        });

        if (auditAlreadyStarted) {
            showSupervisorSignoffView();
        }
    }

    @Override
    protected void onPause() {
        if (mSupervisorSignoffDialog != null) {
            mSupervisorSignoffDialog.dismiss();
            mSupervisorSignoffDialog = null;
        }
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        locationHandler.stopLocationTracking();
        cancelWaitForDock();
        super.onDestroy();
    }

    private AlertDialog mSupervisorSignoffDialog = null;

    private void showSupervisorSignoffView() {
        AlertDialog.Builder builder = new AlertDialog.Builder(HighClaimsAuditActivity.this);
        builder.setMessage("Uncheck any items that do not pass audit and then scan your supervisor tag to proceed.");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
        });
        builder.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface dialog) {
                mSupervisorSignoffDialog = null;
            }
        });
        mSupervisorSignoffDialog = builder.create();
        mSupervisorSignoffDialog.show();
        log.debug(Logs.INTERACTION, "Dialog Shown: " + "Uncheck any items that do not pass audit and then click Done.");
        signoffButton.setVisibility(View.GONE);
        noSupervisorButton.setVisibility(View.GONE);
        doneInstructions.setVisibility(View.VISIBLE);
        supervisorReview = true;
    }

    private void promptForCompletion() {
        log.debug(Logs.INTERACTION, "Checklist incomplete, showing prompt for completion");
        new AlertDialog.Builder(HighClaimsAuditActivity.this)
            .setMessage("Please perform all checks before continuing.")
            .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    return;
                }
            }).show();
    }

    private void promptForSupervisor() {
        log.debug(Logs.INTERACTION, "Supervisor review not done, show prompt for supervisor");
        new AlertDialog.Builder(HighClaimsAuditActivity.this)
                .setMessage("Please have supervisor review before scanning code.")
                .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        return;
                    }
                }).show();
    }

    private void saveAndFinish() {
        saveAndFinish(RESULT_OK);
    }

    private void saveAndFinish(int resultCode) {
        AuditResponse auditResponse = new AuditResponse(mQuestionnnaireVersion, listAdapter.getCheckListResults(), notes);
        Gson gson = new GsonBuilder().excludeFieldsWithoutExposeAnnotation().create();
        load.driverHighClaimsAudit = gson.toJson(auditResponse, AuditResponse.class);
        //Log.d("NARF!! ", load.driverHighClaimsAudit);
        DataManager.insertLoadToLocalDB(this, load);
        setResult(resultCode);
        finish();
    }

    public void back(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(HighClaimsAuditActivity.this);
        builder.setMessage("Going back will lose your work!");
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
                return;
            }
        });
        builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {}
        });
        builder.setCancelable(true);
        builder.create().show();
    }

    @Override
    protected void onScanResultRunOnUiThread(String scannedValue) {
        String supervisorPrefix = getResources().getString(R.string.supervisor_code_prefix);

        if(!supervisorReview) {
            promptForSupervisor();
        } else if (scannedValue.contains(supervisorPrefix)) {
            User supervisor = DataManager.getUserForSupervisorCode(HighClaimsAuditActivity.this, scannedValue);
            if(supervisor != null && supervisor.status.equals("1")) {
                log.debug(Logs.INTERACTION, "user scanned active supervisor code: '" + scannedValue + "'");
                SimpleTimeStamp currentTime = new SimpleTimeStamp();
                Location currentLocation = locationHandler.getLocation();
                this.load.supervisorSignature = scannedValue;
                this.load.supervisorSignedAt = currentTime.getDateTime();
                this.load.supervisorSignatureLat = String.valueOf(currentLocation.getLatitude());
                this.load.supervisorSignatureLon = String.valueOf(currentLocation.getLongitude());
                if (!listAdapter.allChecksMarked()) {
                    Intent notesIntent = new Intent(HighClaimsAuditActivity.this, NotesActivity.class);
                    notesIntent.putExtra(NotesActivity.EXTRA_STATE, NotesActivity.HIGH_CLAIMS_AUDIT);
                    notesIntent.putExtra(NotesActivity.EXTRA_TITLE, "High Claims Audit Notes");
                    notesIntent.putExtra(NotesActivity.EXTRA_PROMPT, "Please explain any items that did not pass audit");
                    notesIntent.putExtra(NotesActivity.EXTRA_IS_REQUIRED, true);
                    notesIntent.putExtra(NotesActivity.EXTRA_NOTES, notes);
                    notesIntent.putParcelableArrayListExtra("checks", checks);
                    startActivityForResult(notesIntent, REQ_CODE_NOTES);
                } else {
                    waitForDock(RESULT_OK);
                }
            } else {
                log.debug(Logs.INTERACTION, "scanned code: " + scannedValue + " is NOT an active supervisor code");
                listAdapter.Unlock();
                AlertDialog.Builder builder = new AlertDialog.Builder(HighClaimsAuditActivity.this);
                builder.setMessage(scannedValue + " is not an active supervisor code. Please call EPOD support at 618-655-2770 in US or 519-690-2603 x2770 in Canada.");
                builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
                });
                builder.show();
                badSignature(scannedValue, "Inactive");
            }
        } else {
            listAdapter.Unlock();
            log.debug(Logs.INTERACTION, "scanned code: " + scannedValue + "is not a valid supervisor code");
            AlertDialog.Builder builder = new AlertDialog.Builder(HighClaimsAuditActivity.this);
            builder.setMessage("Scanned tag is not a supervisor tag");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { dialog.dismiss(); }
            });
            builder.show();
            badSignature(scannedValue, "Invalid");
        }
    }

    private static final Object mWaitForDockSync = new Object();
    private ProgressDialog mWaitForDockDialog = null;
    private CountDownTimer mWaitForDockCountdownTimer = null;

    private void waitForDock(int result) {
        if(PiccoloManager.isDocked() || PiccoloManager.isPlugged(getApplicationContext())) {
            saveAndFinish(result);
        } else {
            boolean restartCounter = false;
            synchronized (mWaitForDockSync) {
                if (mWaitForDockCountdownTimer != null) {
                    CommonUtility.showText("New supervisor tag scanned. Restarting timer.");
                    // Can't call cancelWaitForDoc() in synchronized block; it'll cause deadlock.
                    restartCounter = true;
                }
            }
            if (restartCounter) {
                log.debug(Logs.INTERACTION, "Restarting wait-for-dock timer after supervisor tag rescanned");
                cancelWaitForDock();
            }
            else {
                log.debug(Logs.INTERACTION, "Starting wait-for-dock timer after supervisor tag scanned");
            }
            mWaitForDockDialog = new ProgressDialog(this);
            mWaitForDockDialog.setCancelable(false);
            mWaitForDockDialog.show();
            mWaitForDockDialog.setContentView(R.layout.countdown_display);

            TextView message = mWaitForDockDialog.findViewById(R.id.message);
            TextView clock = mWaitForDockDialog.findViewById(R.id.clock);

            message.setText("Please dock this device to save the supervisor audit.\n\nTime remaining:");

            mWaitForDockCountdownTimer = new CountDownTimer(AppSetting.PRELOAD_AUDIT_TIMEOUT.getLong() * 1000, 1000) {

                public void onTick(long millisUntilFinished) {
                    clock.setText(Long.toString(millisUntilFinished / 1000));

                    if (PiccoloManager.isDocked() || PiccoloManager.isPlugged(getApplicationContext())) {
                        this.cancel();
                        this.onFinish();
                    }
                }

                //Can be used to fire a notification and when the timer is done
                //Can also be used to enable the deliveryVin
                public void onFinish() {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (!HighClaimsAuditActivity.this.isFinishing()) {
                                cancelWaitForDock();
                            }
                            if (!PiccoloManager.isDocked() && !PiccoloManager.isPlugged(getApplicationContext())) {
                                CommonUtility.showText("Unit was not docked in time, please re-sign audit");
                            } else {
                                saveAndFinish(result);
                            }
                        }
                    });
                }

            }.start();
        }
    }

    private void cancelWaitForDock() {
        // I'm not sure if the synchronization variable is needed here. Android's documentation
        // is not specific about how runOnUiThread works with the Activity lifecycle methods.
        // Specifically in this case, when the onFinish() callback in mWaitForDockCountdownTimer
        // calls runOnUiThread(), can that run() code  block interrupt the Activity's onDestroy()
        // method (or vice versa)?  Since I couldn't find the answer, I'm protecting this code.
        // - PDK
        //
        // P.S. The investigation here was prompted by an intermittent crash reported by
        //      Crashlytics show that mWaitForDockDialog.dismiss() was getting called after
        //      the Activity had already been destroyed.
        synchronized (mWaitForDockSync) {
            if (mWaitForDockCountdownTimer != null) {
                mWaitForDockCountdownTimer.cancel();
                mWaitForDockCountdownTimer = null;
            }
            if (mWaitForDockDialog != null) {
                mWaitForDockDialog.dismiss();
                mWaitForDockDialog = null;
            }
        }
    }

    private void badSignature(String badCode, String reason) {

        SimpleTimeStamp sts = new SimpleTimeStamp();
        Calendar calendar = Calendar.getInstance();


        Location currentLocation = locationHandler.getLocation();
        String eventString =  TextUtils.join(",",
                new String[]{
                        "InvalidSupervisor",
                        badCode,
                        reason,
                        load.driver.driverNumber,
                        load.loadNumber,
                        sts.getUtcDateTime(),
                        sts.getUtcTimeZone(),
                        String.valueOf(currentLocation.getLatitude()),
                        String.valueOf(currentLocation.getLongitude())
                });

        LoadEvent event = new LoadEvent();
        event.csv = eventString;
        DataManager.insertLoadEvent(this, event);
        SyncManager.pushLoadEventsLatched(getApplicationContext());
    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQ_CODE_NOTES) {
            if (resultCode == RESULT_OK & data != null) {
                log.debug(Logs.INTERACTION, "received notes and saved");

                notes = data.getStringExtra("notes");
                log.debug(Logs.DEBUG, "NotesActivity returned msg=" + notes);

                waitForDock(RESULT_CODE_AUDIT_DID_NOT_PASS);
            }
        }
    }
}
