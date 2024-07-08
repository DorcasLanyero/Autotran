package com.cassens.autotran.dialogs;

import android.app.Activity;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.util.Base64;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.activities.SignatureActivity;
import com.cassens.autotran.views.SignView;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by bfriedberg on 2/3/15.
 */
public class SignatureDialog extends Dialog implements View.OnClickListener {
    private static final String TAG = "ImageViewDialog";
    private final Activity mCurrentActivity;
    private String mCurrentSignature;

    private SignView mSignatureView;
    private TextView mSigOverlayMessage;
    private TextView mSignatureType;
    private TextView mSignaturePrompt;
    private TextView mBottomMessage;
    private Button mSaveButton;
    private LinearLayout mSmallHighlightBox;
    private LinearLayout mLargeHighlightBox;
    private boolean mReviewButtonPressed = false;

    private static final Logger log = LoggerFactory.getLogger(SignatureDialog.class.getSimpleName());

    public interface ISignatureCaptured {
        boolean signatureCaptured(String signatureString);
        boolean signatureCleared();
        boolean reviewRequested();
    }

    private String mUserType;
    ISignatureCaptured mCallback;

    public SignatureDialog(final Activity activity, String currentSignature, String message, boolean useMsgErrorColor, String userType, String contact, ISignatureCaptured callback) {
        super(activity);

        mUserType = HelperFuncs.noNull(userType);
        mCallback = callback;

        mCurrentActivity = activity;
        mCurrentSignature = currentSignature;

        requestWindowFeature(Window.FEATURE_NO_TITLE);

        final Window window = getWindow();
        window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);

        window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED);
        window.addFlags(WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        window.addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

        WindowManager.LayoutParams wlp = window.getAttributes();

        wlp.gravity = Gravity.LEFT|Gravity.TOP|Gravity.RIGHT;

        Drawable d = new ColorDrawable(Color.BLACK);
        d.setAlpha(200);

        window.setBackgroundDrawable(d);

        if (CommonUtility.isHoneywellLargeDisplaySet()) {
            this.setContentView(R.layout.dialog_signature_view_large);
        }
        else {
            this.setContentView(R.layout.dialog_signature_view);
        }

        mSaveButton = findViewById(R.id.dialog_signature_save);
        mSaveButton.setOnClickListener(this);
        findViewById(R.id.dialog_signature_cancel).setOnClickListener(this);
        findViewById(R.id.dialog_signature_clear).setOnClickListener(this);
        Button reviewButton = findViewById(R.id.dialog_signature_review);
        reviewButton.setOnClickListener(this);

        final BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                SignatureDialog.this.dismiss();
            }
        };

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);

        activity.registerReceiver(receiver, filter);

        this.setOnDismissListener(dialogInterface -> activity.unregisterReceiver(receiver));

        mSigOverlayMessage = findViewById(R.id.sig_overlay_message);
        mSigOverlayMessage.setText(message);

        if (useMsgErrorColor) {
            mSigOverlayMessage.setTextColor(Color.RED);
        } else {
            mSigOverlayMessage.setTextColor(getContext().getResources().getColor(R.color.custom_green));
        }

        mSignatureType = findViewById(R.id.signature_type);
        mSignaturePrompt = findViewById(R.id.signature_prompt);
        mBottomMessage = findViewById(R.id.bottom_message);
        mSmallHighlightBox = findViewById(R.id.highlight_box_small);
        mLargeHighlightBox = findViewById(R.id.highlight_box_large);


        if (!HelperFuncs.isNullOrWhitespace(contact)) {
            mSignaturePrompt.setText(contact);
        }
        if (mUserType.equalsIgnoreCase("dealer")) {
            int dealerIndicatorColor = mCurrentActivity.getResources().getColor(R.color.DealerIndicatorColor);
            mSignatureType.setText("Dealer Signature");
            mSignatureType.setTextColor(dealerIndicatorColor);
            findViewById(R.id.SignatureViewLayout).setBackgroundColor(mCurrentActivity.getResources().getColor(R.color.DarkLightBlue));
            mSaveButton.setEnabled(false);
            mSmallHighlightBox.setVisibility(View.VISIBLE);
            mBottomMessage.setTextColor(mCurrentActivity.getResources().getColor(R.color.Yellow));
            mBottomMessage.setTypeface(Typeface.DEFAULT_BOLD);
            mBottomMessage.setText("PLEASE REVIEW BEFORE SIGNING\nPress Cancel to modify exceptions or comments");
        }
        else if (mUserType.equalsIgnoreCase("supervisor")) {
            mSignatureType.setText("Supervisor");
            mBottomMessage.setText("Supervisor must sign above");
        }
        else {
            mSignatureType.setText("Driver");
            mBottomMessage.setText("Driver must sign above");
        }

        updateSignature();
    }

    private void updateSignature() {

        mSignatureView = findViewById(R.id.view_signature);
        if(mCurrentSignature != null) {
            byte[] byte_arr = Base64.decode(mCurrentSignature, Base64.DEFAULT);

            Bitmap bitmap = BitmapFactory.decodeByteArray(byte_arr, 0, byte_arr.length);

            if(bitmap != null) {
                bitmap = transformBitmap(bitmap,-90, 2f);

                mSignatureView.setBitmap(
                        bitmap
                );
            }
        }
    }

    @Override
    public void onClick(View view) {
        CommonUtility.logButtonClick(log, view);
        if(view.getId() == R.id.dialog_signature_cancel) {
            log.debug(Logs.SIGNATURES, "Hit cancel: " + SignatureActivity.getSignature(mSignatureView, mCurrentActivity));
            dismiss();
        }
        else if (view.getId() == R.id.dialog_signature_clear) {
            log.debug(Logs.SIGNATURES, "Hit clear: " + SignatureActivity.getSignature(mSignatureView, mCurrentActivity));
            clearSignature();
        }
        else if (view.getId() == R.id.dialog_signature_save) {

            int rotation = 90;
            float scale = .5f;
            Bitmap bitmap = mSignatureView.getBitmap();

            Bitmap rotatedBitmap = transformBitmap(bitmap, rotation, scale);

            if(rotatedBitmap != null) {

                log.debug(Logs.SIGNATURES, "Hit done: " + SignatureActivity.getSignature(mSignatureView, mCurrentActivity));
                if (!mSignatureView.isEmpty()) {
                    String signature = SignatureActivity.getCompressedImageString(rotatedBitmap);
                    if (signature == null || signature.length() < SignatureActivity.signature_length_limit) {
                        if (signature == null) {
                            log.debug(Logs.INTERACTION, "signature getCompressedImageString() returned null");
                        }
                        log.debug(Logs.INTERACTION, "signature too short message shown");
                        clearSignature();
                        CommonUtility.simpleMessageDialog(mCurrentActivity, "Your signature was too short to be recognized, please try again");
                        return;
                    }
                    if (mCallback.signatureCaptured(signature)) {
                        dismiss();
                    }
                } else {
                    if (mCallback.signatureCleared()) {
                        dismiss();
                    }
                    else {
                        CommonUtility.simpleMessageDialog(mCurrentActivity, "Signature is required");
                    }
                }
            }
        }
        else if (view.getId() == R.id.dialog_signature_review) {
            if (!mReviewButtonPressed) {
                mReviewButtonPressed = true;
                if (mUserType.equalsIgnoreCase("dealer")) {
                    mSaveButton.setEnabled(true);
                    mSmallHighlightBox.setVisibility(View.GONE);
                    mLargeHighlightBox.setVisibility(View.VISIBLE);
                }
            }
            if (mCallback.reviewRequested()) {
                dismiss();
            }
        }
    }

    private void clearSignature() {
        mCurrentSignature = "";
        mSignatureView.resetView();
        mSignatureView.setBackground(mCurrentActivity.getDrawable(R.color.white));
    }

    public static Bitmap transformBitmap(Bitmap bitmap, int rotation, float scale) {
        Bitmap scaledBitmap = bitmap.createScaledBitmap(bitmap, ((int)(bitmap.getWidth()*scale)), ((int)(bitmap.getHeight()*scale)), false);
        Matrix matrix = new Matrix();
        matrix.postRotate(rotation);
        return Bitmap.createBitmap(scaledBitmap, 0,0,scaledBitmap.getWidth(), scaledBitmap.getHeight(), matrix,false);
    }
}