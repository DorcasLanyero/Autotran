package com.cassens.autotran.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.TextView;

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.activities.SignatureActivity;
import com.cassens.autotran.views.SignView;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

// IMPORTANT: Retain this in the code, even when it is unused. It's a special dialog for
// communicating information, such as a process change, that the needs to be aware
// of. Its color and layout ("information blue") is similar to the blue information
// roadsigns on interstate highways and is designed to get the driver's attention.
public class InformationDialog extends Dialog implements View.OnClickListener {

    private static final String TAG = "InformationDialog";

    private CheckBox stopShowingCheckbox;
    private boolean stopShowing = false;

    private static final Logger log = LoggerFactory.getLogger(InformationDialog.class.getSimpleName());

    public interface IInfoDialogCallback {
        void onAcknowledgeButton(boolean stopShowing);
    }

    IInfoDialogCallback mCallback;

    public InformationDialog(final Activity activity, String header, String mainMessage, String detailedMessage, IInfoDialogCallback callback) {
        super(activity);

        mCallback = callback;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Window window = getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.CENTER;

        Drawable d = new ColorDrawable(Color.TRANSPARENT);
        d.setAlpha(200);

        window.setBackgroundDrawable(d);

        this.setContentView(R.layout.dialog_information);

        TextView headerView = (TextView)findViewById(R.id.tvHeader);
        TextView subHeaderView = (TextView)findViewById(R.id.tvSubHeader);
        TextView detailedMessageView = (TextView)findViewById(R.id.tvDetailedMessage);
        stopShowingCheckbox = (CheckBox)findViewById(R.id.checkboxStopShowing);

        if (header == null) {
            headerView.setVisibility(View.GONE);
        }
        else {
            headerView.setText(header);
        }
        if (mainMessage == null) {
            subHeaderView.setVisibility(View.GONE);
        }
        else {
            subHeaderView.setText(mainMessage);
        }
        if (detailedMessage == null) {
            detailedMessageView.setVisibility(View.GONE);
        }
        else {
            detailedMessageView.setText(detailedMessage);
        }

        stopShowingCheckbox.setChecked(false);
        stopShowingCheckbox.setOnClickListener(this);
        findViewById(R.id.buttonAcknowledge).setOnClickListener(this);

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                InformationDialog.this.dismiss();
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);

        activity.registerReceiver(receiver, filter);

        this.setOnDismissListener(dialogInterface -> activity.unregisterReceiver(receiver));
    }


    @Override
    public void onClick(View view) {
        switch(view.getId()) {

            case R.id.buttonAcknowledge:
                log.debug(Logs.DEBUG, "Pressed acknowledge");
                mCallback.onAcknowledgeButton(stopShowing);
                dismiss();
                break;

            case R.id.checkboxStopShowing:
                stopShowing = stopShowingCheckbox.isChecked();
                log.debug(Logs.DEBUG, "Stop Showing Checkbox set to " + stopShowing);
                break;
        }
    }
}
