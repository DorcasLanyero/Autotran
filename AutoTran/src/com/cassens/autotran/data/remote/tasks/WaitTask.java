package com.cassens.autotran.data.remote.tasks;

import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.activities.DeliveryVinInspectionActivity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by john on 11/28/17.
 */

public class WaitTask extends AsyncTask {
    private static final Logger log = LoggerFactory.getLogger(WaitTask.class.getSimpleName());
    Thread thread;
    DeliveryVinInspectionActivity activity;

    public WaitTask(DeliveryVinInspectionActivity deliveryVinInspectionActivity) {
        thread = new Thread();
        activity = deliveryVinInspectionActivity;
    }

    @Override
    protected void onPreExecute() {
        this.activity.dialog = new ProgressDialog(this.activity,android.R.style.Theme_Black_NoTitleBar_Fullscreen);
        this.activity.dialog.setCancelable(false);
        this.activity.dialog.show();
        this.activity.dialog.setContentView(R.layout.lookup_img);
    }

    @Override
    protected Object doInBackground(Object[] objects) {

        try {
            thread.sleep(10000L);


        } catch (InterruptedException e) {
            log.error(Logs.EXCEPTIONS, "interrupted exception: " + e.getMessage());
        }
        return null;
    }

    @Override
    protected void onPostExecute(Object o) {
        super.onPostExecute(o);
        this.activity.dialog.dismiss();
        //this.activity.setLookupShown();
        this.activity.startSignatureActivity();
    }
}
