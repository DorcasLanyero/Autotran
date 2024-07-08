package com.cassens.autotran.dialogs;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.activities.DealerDetailsActivity;
import com.cassens.autotran.activities.SupplementalNotesActivity;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Dealer;
import com.cassens.autotran.data.model.Delivery;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DealerUnavailableDialog extends Dialog implements View.OnClickListener {

    private static final String TAG = "DealerUnavailableDialog";

    private static final Logger log = LoggerFactory.getLogger(DealerUnavailableDialog.class.getSimpleName());

    public interface IDealerUnavailableDialogCallback {
        void onAfterHoursClicked();
        void onDealerOpenRefusedToSignClicked(String dealerContact);
        void onCancel();
    }

    final IDealerUnavailableDialogCallback mCallback;
    final Activity mActivity;
    final String mCurrentUserId;
    final EditText mDriverId;

    private Delivery mThisDelivery;
    private boolean mAcceptsAfterHoursDelivery;
    private String mApprover = "";
    private String mDealerContact = "";
    private String mReason = "";
    private Dealer.AfterHoursDeliveryPermission mAfterHourDeliverySpecification;

    public DealerUnavailableDialog(final Activity activity, final Delivery delivery, IDealerUnavailableDialogCallback callback) {
        super(activity);

        mCallback = callback;
        mActivity = activity;
        mCurrentUserId = CommonUtility.getDriverNumber(mActivity);
        mThisDelivery = delivery;
        mAcceptsAfterHoursDelivery = delivery.isAfterHoursDelivery();
        mDealerContact = HelperFuncs.noNull(delivery.dealerContact);
        mAfterHourDeliverySpecification = delivery.dealer.getAfterHoursDeliveryPermission();

        this.setTitle(R.string.unattended_delivery_title);

        final Window window = getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.CENTER;

        /*

        Drawable d = new ColorDrawable(Color.TRANSPARENT);
        d.setAlpha(200);

        window.setBackgroundDrawable(d); */

        this.setContentView(R.layout.dialog_dealer_unavailable);

        mDriverId = findViewById(R.id.driver_num_textbox);

        Button cancelButton = findViewById(R.id.cancel_button);
        cancelButton.setOnClickListener(this);

        Button afterHoursButton = findViewById(R.id.after_hours_sti_button);
        afterHoursButton.setOnClickListener(this);

        Button dortsButton = findViewById(R.id.dorts_button);
        dortsButton.setOnClickListener(this);

        if (mAcceptsAfterHoursDelivery) {
            findViewById(R.id.dealer_after_hrs_textview).setVisibility(View.VISIBLE);
        }

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                DealerUnavailableDialog.this.dismiss();
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);

        activity.registerReceiver(receiver, filter);

        // Leaving the code below commented out for now in case we need it later.
        // This would show the Glovis popup before the user selects STI, which is probably not
        // what we want. However, we might want to change the popup to let the driver just go
        // ahead and choose STI for the load (or maybe even default to STI for the load).
        /*
        this.setOnShowListener(new OnShowListener() {
            @Override
            public void onShow(DialogInterface di) {
                showGlovisPopupIfNeeded();
            }
        });
         */
        this.setOnDismissListener(dialogInterface -> activity.unregisterReceiver(receiver));

        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        this.setCancelable(false);
        this.setCanceledOnTouchOutside(false);
    }

    private boolean isStiCommentRequired() {
        if (!this.mThisDelivery.dealer.acceptsAfterHoursDelivery()
                || this.mThisDelivery.dealer.shouldBeOpen()
                || mThisDelivery.dealer.getAfterHoursDeliveryPermission()
                    == Dealer.AfterHoursDeliveryPermission.ALLOWED_WITH_CALL_AHEAD) {
            return true;
        }
        return false;
    }

    private boolean checkPayNumber() {
        String submittedId = mDriverId.getText().toString().trim();
        String message;

        if (submittedId.isEmpty()) {
            message = "Pay number is required";
        }
        else if (!CommonUtility.isInteger(submittedId)) {
            message = "'"  + submittedId + "' is not a valid pay number";
        }
        else if (!submittedId.equals(mCurrentUserId)) {
            message = mActivity.getString(R.string.error_wrong_driver_number);
        }
        else {
            findViewById(R.id.driver_num_textbox_border).setBackgroundColor(Color.TRANSPARENT);
            return true;
        }

        findViewById(R.id.driver_num_textbox_border).setBackgroundColor(Color.RED);
        CommonUtility.showText(message);
        log.debug(Logs.INTERACTION, "Message shown: " + message);
        return false;
    }

    // TODO: Uncomment calls to  showGlovisPopupIfNeeded() when Cassens is ready to introduce.
    private void showGlovisPopupIfNeeded() {
        if (mThisDelivery == null || mThisDelivery.dealer == null) {
            return;
        }
        if (mThisDelivery.dealer.requiresGlovisStiProcedure()) {
            CommonUtility.simpleMessageDialog(mActivity,
                    "Make sure you are using the delivery area specified in the instructions.\n\n" +
                            "Ensure all doors are locked and all windows shut on delivered units " +
                            "and all keys are placed in an envelope.\n\n" +
                            "Ensure the envelope is legibly labeled with the following:\n" +
                            "  • Delivery date and time\n" +
                            "  • Name and phone number of your dispatcher or local POC\n" +
                            "  • Your name\n" +
                            "  • \"STI\"\n\n" +
                            "Place this envelope in the dealer's service department's key drop box.",
                    "GLOVIS STI Procedure");
        }
    }

    @Override
    public void onClick(View view) {
        int id = view.getId();

        switch(view.getId()) {

            case R.id.cancel_button:
                CommonUtility.logButtonClick(log, view);
                mCallback.onCancel();
                dismiss();
                break;

            case R.id.after_hours_sti_button:
                if (checkPayNumber()) {
                    CommonUtility.logButtonClick(log, view);
                    if(isStiCommentRequired()) {
                        showSTICommentsDialog();
                    }
                    else{
                        mThisDelivery.sti = 1;
                        mThisDelivery.afrhrs = 0;
                        DataManager.saveDeliverySti(mActivity, mThisDelivery.getId(), mThisDelivery.sti );
                        mCallback.onAfterHoursClicked();
                        dismiss();
                    }

                }
                break;

            case R.id.dorts_button:
                if (checkPayNumber()) {
                    CommonUtility.logButtonClick(log, view);
                    if (AppSetting.FEATURE_EXPANDED_DORTS_DIALOG.getBoolean()) {
                        showDORTSCommentsDialog(mApprover, mReason, mDealerContact);
                    }
                    else {
                        showGetFullNameDialog(mDealerContact);
                    }
                    //dismiss();
                }
                break;
        }
    }

    private void showSTICommentsDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_sti_comments, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        final TextView message = (TextView) dialogView.findViewById(R.id.sti_comment_message);

        log.debug(Logs.INTERACTION, "Showing STI confirmation");

        builder.setTitle("STI/After Hours Exception");
        builder.setView(dialogView).setCancelable(true);

        final Button doneButton = dialogView.findViewById(R.id.done_button);
        final Button cancelButton =  dialogView.findViewById(R.id.cancel_button);

        final TextView approverName = dialogView.findViewById(R.id.approver_name_editText);
        final LinearLayout approverNameBorder = dialogView.findViewById(R.id.approver_name_border);
        final TextView hours = dialogView.findViewById(R.id.today_hours);
        final TextView exceptionReasonLabel = dialogView.findViewById(R.id.exception_reason_label);
        final TextView exceptionReason = dialogView.findViewById(R.id.exception_reason_editText);
        final LinearLayout exceptionReasonBorder = dialogView.findViewById(R.id.exception_reason_border);

        final boolean stiInBusinessHours = this.mThisDelivery.dealer.acceptsAfterHoursDelivery() && this.mThisDelivery.dealer.shouldBeOpen();
        if (stiInBusinessHours){
            // Note sti_in_business_hours_length has a %s
            message.setText(String.format(mActivity.getString(R.string.sti_in_business_hours_long), DealerDetailsActivity.todaysHours(mThisDelivery.dealer)));
        }
        else if (mAfterHourDeliverySpecification == Dealer.AfterHoursDeliveryPermission.ALLOWED_WITH_CALL_AHEAD) {
            message.setText(String.format(mActivity.getString(R.string.sti_call_ahead)));
            exceptionReasonLabel.setVisibility(GONE);
            exceptionReasonBorder.setVisibility(GONE);
        }

        final Dialog dialog;
        final boolean driverWarned = stiInBusinessHours;


        DealerDetailsActivity dealerDetails = new DealerDetailsActivity();
        hours.setText(dealerDetails.todaysHours(mThisDelivery.dealer));

        dialog = builder.create();

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                approverNameBorder.setBackgroundColor(Color.TRANSPARENT);
                exceptionReasonBorder.setBackgroundColor(Color.TRANSPARENT);
                if (approverName.getText().toString().trim().isEmpty()) {
                    approverNameBorder.setBackgroundColor(Color.RED);
                    CommonUtility.showText("Name of approver is required.");
                } else if (exceptionReasonBorder.getVisibility() == VISIBLE && exceptionReason.getText().toString().trim().isEmpty()) {
                    exceptionReasonBorder.setBackgroundColor(Color.RED);
                    CommonUtility.showText("Reason for exception is required.");
                } else {
                    String stiNote = "UNATTENDED DELIVERY: ";
                    if (stiInBusinessHours) {
                        stiNote += "STI during business hours (driver confirmed)";
                    }
                    else if (exceptionReasonBorder.getVisibility() == VISIBLE) {
                        stiNote += "After Hours / STI (selected but not allowed)";
                    }
                    else {
                        stiNote += "After Hours / STI (with call ahead approval)";
                    }
                    stiNote += "\nApproved by: " + approverName.getText().toString().trim();
                    if (!exceptionReason.getText().toString().trim().isEmpty()) {
                        stiNote += "\nReason: " + exceptionReason.getText().toString().trim();
                    }
                    CommonUtility.logButtonClick(log, v, stiNote);
                    SupplementalNotesActivity.addNoteToVehicleBatch(getContext(), mThisDelivery, stiNote);
                    mThisDelivery.afrhrs = 0;
                    mThisDelivery.sti = 1;
                    DataManager.saveDeliverySti(mActivity, mThisDelivery.getId(), mThisDelivery.sti);
                    mCallback.onAfterHoursClicked();
                    dialog.dismiss();
                    DealerUnavailableDialog.this.dismiss();
                }
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_HIDDEN);
        dialog.show();
    }

    private void showGetFullNameDialog(final String contact) {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_full_name, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
        final EditText driverId = (EditText) dialogView.findViewById(R.id.pay_number_textbox);
        final TextView personRefusingTv = (TextView) dialogView.findViewById(R.id.person_refusing_tv);


        log.debug(Logs.INTERACTION, "Showing Dealer unavailable or refusing to sign confirmation");
        builder.setTitle("Dealer Unavailable");
        dialogView.findViewById(R.id.enter_pay_number_ll).setVisibility(GONE);
        builder.setView(dialogView).setCancelable(true);

        final Button doneButton = dialogView.findViewById(R.id.done_button);
        final Button cancelButton =  dialogView.findViewById(R.id.cancel_button);

        final TextView firstNameTv = dialogView.findViewById(R.id.first_name_edit_text);
        final TextView lastNameTv = dialogView.findViewById(R.id.last_name_edit_text);

        if (contact.trim().contains(" ")) {
            String[] names = contact.trim().split(" ");
            if(names.length > 0) {
                firstNameTv.setText(names[0]);
            }

            if(names.length > 1) {
                lastNameTv.setText(contact.substring(names[0].length() + 1));
            }
        }


        final Dialog dialog;

        dialog = builder.create();

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firstNameTv.getText().toString().trim().isEmpty() || lastNameTv.getText().toString().trim().isEmpty() ) {
                    CommonUtility.showText("Must enter both first and last names");
                } else {
                    mDealerContact = firstNameTv.getText().toString().trim() + " " + lastNameTv.getText().toString().trim();


                    String stiNote = "UNATTENDED DELIVERY: Dealer unavailable or refused to sign during provided hours\n" +
                            "Dealer Contact: " + mDealerContact;
                    SupplementalNotesActivity.addNoteToVehicleBatch(getContext(), mThisDelivery, stiNote);

                    mThisDelivery.afrhrs = 1;
                    mThisDelivery.sti = 0;
                    mThisDelivery.dealerContact = mDealerContact;
                    DataManager.saveDeliveryStiAndAfrhrs(mActivity, mThisDelivery.getId(), mThisDelivery.sti, mThisDelivery.afrhrs, mThisDelivery.dealerContact);
                    mCallback.onDealerOpenRefusedToSignClicked(mDealerContact);
                    dialog.dismiss();
                    DealerUnavailableDialog.this.dismiss();
                }
            }
        });

        cancelButton.setOnClickListener(v -> {
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
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    private void showDORTSCommentsDialog(final String approver, final String reason, final String dealerContact) {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_dorts_comments, null);

        AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);

        log.debug(Logs.INTERACTION, "Showing Dealer unavailable or refusing to sign confirmation");
        builder.setTitle("Dealer Unavailable");
        builder.setView(dialogView).setCancelable(true);

        final Button doneButton = dialogView.findViewById(R.id.done_button);
        final Button cancelButton =  dialogView.findViewById(R.id.cancel_button);

        final TextView approverFirstNameTv = dialogView.findViewById(R.id.dortsApproverFirstName_et);
        final TextView approverLastNameTv = dialogView.findViewById(R.id.dortsApproverLastName_et);
        final TextView reasonTv = dialogView.findViewById(R.id.dortsExceptionReason_et);
        final TextView dealerFirstNameTv = dialogView.findViewById(R.id.dortsDealerFirstName_et);
        final TextView dealerLastNameTv = dialogView.findViewById(R.id.dortsDealerLastName_et);

        if (approver.trim().contains(" ")) {
            String[] names = approver.trim().split(" ");
            if(names.length > 0) {
                approverFirstNameTv.setText(names[0]);
            }

            if(names.length > 1) {
                approverLastNameTv.setText(approver.substring(names[0].length() + 1));
            }
        }

        reasonTv.setText(reason);

        if (dealerContact.trim().contains(" ")) {
            String[] names = dealerContact.trim().split(" ");
            if(names.length > 0) {
                dealerFirstNameTv.setText(names[0]);
            }

            if(names.length > 1) {
                dealerLastNameTv.setText(dealerContact.substring(names[0].length() + 1));
            }
        }


        final Dialog dialog;

        dialog = builder.create();

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (dealerFirstNameTv.getText().toString().trim().isEmpty() || dealerLastNameTv.getText().toString().trim().isEmpty() ) {
                    CommonUtility.showText("Must enter both first and last names");
                } else {
                    mApprover = approverFirstNameTv.getText().toString().trim() + " " + approverLastNameTv.getText().toString().trim();
                    mDealerContact = dealerFirstNameTv.getText().toString().trim() + " " + dealerLastNameTv.getText().toString().trim();
                    mReason = reasonTv.getText().toString().trim();

                    String stiNote = "UNATTENDED DELIVERY: Dealer unavailable or refused to sign during provided hours\n" +
                            "Dispatcher or manager who approved: " + mApprover +
                            "\nDealer contact who confirmed: " + mDealerContact +
                            "\nReason:\n" + mReason;

                    SupplementalNotesActivity.addNoteToVehicleBatch(getContext(), mThisDelivery, stiNote);

                    mThisDelivery.afrhrs = 1;
                    mThisDelivery.sti = 0;
                    mThisDelivery.dealerContact = dealerContact;
                    DataManager.saveDeliveryStiAndAfrhrs(mActivity, mThisDelivery.getId(), mThisDelivery.sti, mThisDelivery.afrhrs, mThisDelivery.dealerContact);
                    mCallback.onDealerOpenRefusedToSignClicked(dealerContact);
                    dialog.dismiss();
                    DealerUnavailableDialog.this.dismiss();
                }
            }
        });

        cancelButton.setOnClickListener(v -> {
            dialog.dismiss();
        });

        dealerLastNameTv.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView arg0, int actionId, KeyEvent event) {
                if (actionId == EditorInfo.IME_ACTION_DONE) {
                    doneButton.callOnClick();;
                }
                return false;
            }
        });
        dialog.setCancelable(false);
        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }
}
