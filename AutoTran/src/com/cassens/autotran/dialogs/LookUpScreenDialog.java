package com.cassens.autotran.dialogs;

import static com.cassens.autotran.data.model.lookup.ShuttleMove.TAG;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Load;

public class LookUpScreenDialog {
    final Activity mActivity;
    final LookUpScreenCallback mcallback;

    Boolean mLookupDialogCancelled = false;

    private Delivery mThisDeliery;

    public LookUpScreenDialog(Activity activity, LookUpScreenCallback callback ) {
        this.mActivity = activity;
        this.mcallback = callback;
    }

    public interface LookUpScreenCallback {
        void proceed();
    }

    public void canceled(){
        mLookupDialogCancelled = true;

    }

    public void showLookupScreen(Context context,String id, int operation, Load thisLoad) {
        ProgressDialog dialog = new ProgressDialog(context,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        dialog.setCancelable(false);
        dialog.show();
        dialog.setContentView(R.layout.lookup_img);

        Vibrator v =  (Vibrator) CommonUtility.getCurrentActivity().getSystemService(Context.VIBRATOR_SERVICE);
        v.vibrate(500);

        CommonUtility.getCurrentActivity().runOnUiThread(() -> {
            final Handler handler = new Handler();
            handler.postDelayed(() -> {

                if (!mLookupDialogCancelled && dialog != null  && dialog.isShowing()) {
                    Log.d(TAG, "lookup screen is done!!");

                    dialog.dismiss();
                    setLookupShown(context, operation, id,thisLoad);
                    mcallback.proceed();
                } else {
                    Log.d(TAG, "lookup screen was cancelled!");
                }

            }, 1000L);
        });
    }

    private void setLookupShown(Context context, int operation , String id, Load thisLoad) {
        if(operation == Constants.DELIVERY_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) {
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.putBoolean(id + "_delivery_lookup_shown", true);
            editor.apply();
        } else {
            if(thisLoad != null) {
                SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
                editor.putBoolean(id + "_load_lookup_shown", true);
                editor.apply();
            }
        }
    }

    public static void cleanLookUpEntry(String id){
        boolean lookupShown = PreferenceManager.getDefaultSharedPreferences(CommonUtility.getCurrentActivity()).getBoolean(id + "_delivery_lookup_shown", false);
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(CommonUtility.getCurrentActivity()).edit();

        if (lookupShown) {
            editor.remove(id + "_delivery_lookup_shown");
            editor.commit();
        }
    }
}
