package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.LinearLayout.LayoutParams;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.DriverAction;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.data.remote.tasks.ProcessDriverActionQueueTask;
import com.cassens.autotran.views.CustomScrollView;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


// The mechanism to pop up an alert dialog from anywhere in the application is liberally taken from
// http://nsamteladze.blogspot.com/2012/10/show-dialog-in-android-after-device.html

/**
 * Created by bfriedberg on 2/12/16.
 */
public class DriverMessageDialogActivity extends AutoTranActivity{
    private static final Logger log = LoggerFactory.getLogger(DriverMessageDialogActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private List<Integer> mMsgIds;
    private CustomScrollView customScrollView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dialog_overlay);

        Intent intent = getIntent();
        ArrayList<String> messages = intent.getStringArrayListExtra(Constants.DIALOG_ACTIVITY_MESSAGE);
        String title = intent.getStringExtra(Constants.DIALOG_ACTIVITY_TITLE);
        Boolean showCancel = intent.getBooleanExtra(Constants.DIALOG_ACTIVITY_SHOW_CANCEL, true);
        Boolean oldMessagePresent = intent.getBooleanExtra(Constants.DIALOG_ACTIVITY_DRIVER_OLD_MESSAGES, false);

        mMsgIds = intent.getIntegerArrayListExtra(Constants.DIALOG_ACTIVITY_DRIVER_ACTION_IDS);

        showTipsDialogForDriverMessage(messages, title, showCancel, oldMessagePresent);
        customScrollView = findViewById(R.id.tips_scroll);
    }

    private void showTipsDialogForDriverMessage(ArrayList<String> messages, String title, boolean showCancel, boolean oldMessagePresent) {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_messages, null);
        final Context context = this;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(false);

        LinearLayout tipContainer = dialogView.findViewById(R.id.tips_container);
        final EditText driverId = dialogView.findViewById(R.id.tips_textbox);
        final Button doneButton = dialogView.findViewById(R.id.done_button);
        final Button cancelButton = dialogView.findViewById(R.id.cancel_button);

        doneButton.setText("Acknowledge");
        cancelButton.setText("Read Later");

        if(showCancel && !oldMessagePresent) {
            cancelButton.setVisibility(View.VISIBLE);
        }
        else{
            cancelButton.setVisibility(View.GONE);
        }

        final Dialog dialog;

        builder.setTitle(title);
        log.debug(Logs.INTERACTION, "Showing " + title + " and prompting for pay number");
        dialog = builder.create();

        LayoutParams lparams = new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT);
        lparams.setMargins(0, 5, 0, 5);

        for (String message : messages) {
            log.debug(Logs.INTERACTION, "Showing message: " + message);
            TextView msgview = new TextView(this);
            msgview.setText(message);
            msgview.setBackgroundColor(Color.LTGRAY);
            msgview.setTextColor(Color.BLACK);
            msgview.setPadding(5, 5, 5, 5);
            msgview.setLayoutParams(lparams);
            //msgview.setMaxLines(4);
            //msgview.setEllipsize(TextUtils.TruncateAt.END);
            tipContainer.addView(msgview, 0);
        }

        doneButton.setOnClickListener(v -> {
            CommonUtility.logButtonClick(log, v, "pay number: " + driverId.getText().toString().trim());

            if (!driverId.getText().toString().trim().equals(CommonUtility.getDriverNumber(context))) {
                dialogView.findViewById(R.id.tips_textbox_border).setBackgroundColor(Color.RED);
                log.debug(Logs.INTERACTION, "Message shown: " + R.string.error_wrong_driver_number);
                CommonUtility.showText(getString(R.string.error_wrong_driver_number) +
                        (driverId.getText().toString() != null ? driverId.getText().toString() +
                                " doesn't equal '" + CommonUtility.getDriverNumber(context) : "'"));
            } else {
                if (mMsgIds != null) {
                    for (Integer msgId :mMsgIds) {
                        DriverAction msgAction = DataManager.getDriverAction(context, msgId.intValue());
                        msgAction.setProcessed(Constants.dateFormatter().format(HelperFuncs.getTimestamp()));
                        msgAction.setUploadStatus(Constants.SYNC_STATUS_NOT_UPLOADED);
                        msgAction.setStatus(ProcessDriverActionQueueTask.COMPLETED);
                        DataManager.upsertDriverActionToLocalDB(context, msgAction);
                    }
                    SyncManager.pushCompletedDriverActions(context, Integer.parseInt(driverId.getText().toString()));
                }
                setResult(Activity.RESULT_OK);
                closeDialogAndFinish(dialog);
            }
        });

        cancelButton.setOnClickListener(v -> {
            setResult(Activity.RESULT_CANCELED);
            CommonUtility.logButtonClick(log, v);

            closeDialogAndFinish(dialog);
        });

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

    public void closeDialogAndFinish(Dialog dialog) {

        dialog.dismiss();
        finish();
    }

}