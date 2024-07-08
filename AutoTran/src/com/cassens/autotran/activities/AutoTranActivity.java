package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.util.Log;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.handlers.DrivingActivityHandler;
import com.cassens.autotran.handlers.TruckNumberHandler;

import org.slf4j.Logger;

abstract public class AutoTranActivity extends Activity {
    private final Logger log = getLogger();
    public boolean paused = true;
    protected DrivingActivityHandler drivingActivityHandler;
    public Dialog questionnaireDialog;
    protected boolean logLifecycleMessages = true;

    // Each subclass should create its own static final logger and implement getStaticLogger()
    // to pass it back to this base class. By making it static final, we ensure that the
    // logger for each Activity will be created only once rather than being recreated
    // anew each time an activity is destroyed and re-created. The tradeoff, of course, is
    // that all of these loggers stay in RAM.
    //
    // PDK - Frankly, I'm not sure this is worth the added complexity. In fact, I'm not even
    // convinced the above is a good tradeoff. However, adding this abstract function
    // provided a way for us to log messages in the base class (e.g. onResume() and onPause()
    // while preserving the prior implementation (static final instantiation of the loggers in
    // each activity). At a later time, we should evaluate whether it's better to just recreate
    // the logger every time an activity is created. If so, we can implement that as a
    // standalone change rather than as part of a massive set of log changes.
    //
    // TODO: Evaluate how we instantiate loggers (static final vs. dynamic). See code comments.
    //
    abstract public Logger getLogger();
/*
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (logLifecycleMessages) {
            log.debug(Logs.INTERACTION, "onCreate()");
        }
    }

    @Override
    protected void onDestroy() {
        if (logLifecycleMessages) {
            log.debug(Logs.INTERACTION, "onDestroy()");
        }
        super.onDestroy();
    }


    @Override
    protected void onStart() {
        super.onStart();
        if (logLifecycleMessages) {
            log.debug(Logs.INTERACTION, "onStart()");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (logLifecycleMessages) {
            log.debug(Logs.INTERACTION, "onStop()");
        }
    }
*/

    @Override
    protected void onResume() {
        super.onResume();
        paused = false;
        CommonUtility.setCurrentActivity(this);
        AutoTranApplication.setLastInteractionTime();
        if (logLifecycleMessages) {
            log.debug(Logs.INTERACTION, "Resuming");
        }
        drivingActivityHandler.getInstance(this);
        DrivingLockActivity.startDrivingLockActivityIfNeeded(this);
        if (mTruckNumberChangeCallback != null) {
            handleTruckNumberChange();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (logLifecycleMessages) {
            log.debug(Logs.INTERACTION, "Pausing");
        }
        if (questionnaireDialog != null && questionnaireDialog.isShowing()) {
            Log.d("AutoTranActivity", "Would have dismissed question dialog");
            //questionnaireDialog.dismiss();
        }
        dismissIfShowing(piccoloDialog);
        dismissIfShowing(simpleMessageDialog);
        dismissIfShowing(truckNumberDialog);
        paused = true;
        suppressPiccoloDialog = false;
    }


    private void dismissIfShowing(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            if (dialog == piccoloDialog) log.debug(Logs.DEBUG, "Dismissing Piccolo dialog");
            dialog.dismiss();
        }
    }

    protected void setTopBarColor(int topBarLayout, int backButton,  int backButtonDrawable, int menuButton, int menuButtonDrawable, int color) {
        findViewById(topBarLayout).setBackgroundColor(getResources().getColor(color));
        ImageView ivBack = findViewById(backButton);
        ivBack.setImageDrawable(getDrawable(backButtonDrawable));
        if (menuButton > 0 && menuButtonDrawable > 0) {
            ImageView ivMenu = findViewById(menuButton);
            ivMenu.setImageDrawable(getDrawable(menuButtonDrawable));
        }
    }

    protected void enableTruckNumberChangeHandler(TruckNumberChangeCallback callback) {
        mTruckNumberChangeCallback = callback;
    }

    protected interface TruckNumberChangeCallback {
        public void onTruckNumberChange(String newTruckNumber);
    }

    private TruckNumberChangeCallback mTruckNumberChangeCallback = null;

    private void handleTruckNumberChange() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        if (TruckNumberHandler.piccoloTruckNumberReceived(this)) {
            String piccoloTruckNum = TruckNumberHandler.getPiccoloTruckNumber(this);
            if (!piccoloTruckNum.isEmpty() && !piccoloTruckNum.equals(TruckNumberHandler.getTruckNumber(this))) {
                showPiccoloDialog();
                mTruckNumberChangeCallback.onTruckNumberChange(TruckNumberHandler.getTruckNumber(this));
            }
        }
    }

    protected boolean suppressPiccoloDialog = false;
    private AlertDialog piccoloDialog;
    private AlertDialog simpleMessageDialog;
    private Dialog truckNumberDialog;

    private void showPiccoloDialog() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (!suppressPiccoloDialog) {
            log.debug(Logs.DEBUG, "Showing Piccolo dialog");
            AlertDialog.Builder builder = new AlertDialog.Builder(this)
                    .setTitle("Mismatching Truck Numbers Detected")
                    .setMessage("The last truck number received from the truck differs from the truck number you entered. Please select the correct truck number below.")
                    .setCancelable(false)
                    .setNegativeButton("Other", null)
                    .setPositiveButton(TruckNumberHandler.getPiccoloTruckNumber(AutoTranActivity.this), (dialog, which) -> {
                        suppressPiccoloDialog = false;
                        log.debug(Logs.INTERACTION, String.format("TruckNumberDialog: Driver %s chose to change truck number from %s to Piccolo-provided truck number (%s)",
                                CommonUtility.getDriverNumber(AutoTranActivity.this), TruckNumberHandler.getTruckNumber(AutoTranActivity.this),
                                TruckNumberHandler.getPiccoloTruckNumber(AutoTranActivity.this)));
                        dialog.dismiss();
                        TruckNumberHandler.setTruckNumber(AutoTranActivity.this, TruckNumberHandler.getPiccoloTruckNumber(AutoTranActivity.this));
                        mTruckNumberChangeCallback.onTruckNumberChange(TruckNumberHandler.getTruckNumber(this));
                        TruckNumberHandler.setPiccoloTruckNumberReceived(getApplicationContext(), false);
                    })
                    .setNeutralButton(TruckNumberHandler.getTruckNumber(AutoTranActivity.this), (dialog, which) -> {
                        suppressPiccoloDialog = false;
                        dialog.dismiss();
                        simpleMessageDialog = CommonUtility.simpleMessageDialog(AutoTranActivity.this, getResources().getString(R.string.truck_number_override_msg), "Overriding GPS Truck Number");
                        log.debug(Logs.INTERACTION, String.format("TruckNumberDialog: Driver %s chose to continue using truck number %s instead of the Piccolo-provided truck number (%s)",
                                CommonUtility.getDriverNumber(AutoTranActivity.this), TruckNumberHandler.getTruckNumber(AutoTranActivity.this),
                                TruckNumberHandler.getPiccoloTruckNumber(AutoTranActivity.this)));
                        TruckNumberHandler.setPiccoloTruckNumberReceived(getApplicationContext(), false);
                    });
            suppressPiccoloDialog = true;
            piccoloDialog = builder.create();
            piccoloDialog.setOnShowListener(new DialogInterface.OnShowListener() {

                @Override
                public void onShow(DialogInterface dialogInterface) {
                    Button button = ((AlertDialog) piccoloDialog).getButton(AlertDialog.BUTTON_NEGATIVE);
                    button.setOnClickListener(new View.OnClickListener() {

                        @Override
                        public void onClick(View view) {
                            manuallyEnteredTruckNumberDialog(AutoTranActivity.this);
                            TruckNumberHandler.setPiccoloTruckNumberReceived(getApplicationContext(), false);
                        }
                    });
                }
            });
            piccoloDialog.show();
        }
    }

    private void manuallyEnteredTruckNumberDialog(Activity activity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        LayoutInflater inflater = activity.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_truck_number_override, null);

        builder.setView(dialogView);
        builder.setTitle("Override GPS Truck Number");
        builder.setCancelable(false);

        builder.setPositiveButton("Ok", null);
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            dialog.dismiss();
        });

        truckNumberDialog = builder.create();
        truckNumberDialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                Button button = ((AlertDialog) truckNumberDialog).getButton(AlertDialog.BUTTON_POSITIVE);
                button.setOnClickListener(new View.OnClickListener() {

                    @Override
                    public void onClick(View view) {
                        String newTruckNumber = ((EditText) dialogView.findViewById(R.id.truckIDEditText)).getText().toString();
                        if (TruckNumberHandler.isValid(newTruckNumber)) {
                            TruckNumberHandler.setTruckNumber(AutoTranActivity.this, newTruckNumber);
                            log.debug(Logs.INTERACTION, String.format("TruckNumberDialog: Driver %s overrode Piccolo truck number (%s) with %s",
                                    CommonUtility.getDriverNumber(AutoTranActivity.this),
                                    TruckNumberHandler.getPiccoloTruckNumber(AutoTranActivity.this), newTruckNumber));
                            suppressPiccoloDialog = false;
                            mTruckNumberChangeCallback.onTruckNumberChange(newTruckNumber);
                            piccoloDialog.dismiss();
                            truckNumberDialog.dismiss();
                        }
                        else {
                            CommonUtility.showText("Invalid truck number");
                        }
                    }
                });
            }
        });
        truckNumberDialog.show();
    }
}
