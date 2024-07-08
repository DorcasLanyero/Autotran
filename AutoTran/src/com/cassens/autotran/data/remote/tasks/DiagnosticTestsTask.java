package com.cassens.autotran.data.remote.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.R;
import com.cassens.autotran.data.remote.CallWebServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Date;

public class DiagnosticTestsTask extends AsyncTask<String, String, Long[]>
{
    private static final Logger log = LoggerFactory.getLogger(DiagnosticTestsTask.class.getSimpleName());

	Activity activity;
	boolean result;
	ProgressDialog dialog;

	public DiagnosticTestsTask(Activity activity)
	{
		this.activity = activity;
	}
	
	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();

		if (activity.isFinishing()) {
			cancel(true);
			return;
		}
		dialog = new ProgressDialog(activity);
		dialog.setCancelable(false);
		dialog.show();
		dialog.setContentView(R.layout.customprogressdialog);

	}

	@Override
	protected Long[] doInBackground(String... params)
	{
		System.out.println("#### DiagnosticTestTask doInBackground started");

		Long[] transferTimes = new Long[4];

		long startTime = new Date().getTime();
		long length = CallWebServices.downloadTests();
		transferTimes[0] = (new Date().getTime()) - startTime;
		transferTimes[1] = length;

		startTime = new Date().getTime();
		length = CallWebServices.uploadTests();
		transferTimes[2] = (new Date().getTime()) - startTime;
		transferTimes[3] = length;


		return transferTimes;
	}
	
	@Override
	protected void onProgressUpdate(String... values)
	{
		super.onProgressUpdate(values);

		if (activity.isFinishing()) {
			cancel(true);
			return;
		}
		CommonUtility.showText(values[0]);
		if (dialog.isShowing()) {
			dialog.dismiss();
		}
	}
	
	@Override
	protected void onPostExecute(Long[] result)
	{
		super.onPostExecute(result);

		if (activity.isFinishing()) {
			return;
		}

		if (dialog.isShowing()) {
			dialog.dismiss();
		}


		DecimalFormat df = new DecimalFormat("#.00000");

		CommonUtility.simpleMessageDialog(activity, "Download Test ("+result[1]+" bytes) Completed in " + result[0] + " milliseconds\n" +
		"Upload Test ("+ result[3] +" bytes) Completed in " + result[2] + " milliseconds"
		);


	}

}
