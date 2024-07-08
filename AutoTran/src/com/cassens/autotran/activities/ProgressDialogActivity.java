package com.cassens.autotran.activities;

import android.app.ProgressDialog;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ProgressDialogActivity extends AutoTranActivity {
    protected ProgressDialog dialog;
    protected TextView statusView;

    private static final Logger log = LoggerFactory.getLogger(ProgressDialogActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    public void showProgressDialog(String message) {
        dialog = new ProgressDialog(this);
        //dialog.setIndeterminate(true);
        dialog.setCancelable(false);
        dialog.show();
        dialog.setContentView(R.layout.customprogressdialog);
        statusView = (TextView)dialog.findViewById(R.id.dialog_status);
        statusView.setText(message);
    }

    public void updateStatus(String message) {
        if(statusView != null) {
            statusView.setText(message);
        }
        else {
            CommonUtility.showText(message);
        }
    }

    public void dismissDialog() {
        if(dialog != null && dialog.isShowing()) {

            //You can't dismiss a dialog if an activity is finishing.  checking for this since we had some crashes
            try {
                if (!isFinishing()) {
                    dialog.dismiss();
                }
            } catch (IllegalArgumentException ex) {
                log.error(Logs.EXCEPTIONS, "dismiss dialog threw an illegal argument exception:" + ex.getMessage());
            }
        }
    }
}
