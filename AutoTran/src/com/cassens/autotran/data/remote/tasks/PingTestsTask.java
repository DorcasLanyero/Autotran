package com.cassens.autotran.data.remote.tasks;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.R;
import com.cassens.autotran.data.remote.CallWebServices;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PingTestsTask extends AsyncTask<String, String, String>
{
    private static final Logger log = LoggerFactory.getLogger(PingTestsTask.class.getSimpleName());

	Activity activity;
	boolean result;
	ProgressDialog dialog;

	public PingTestsTask(Activity activity)
	{
		this.activity = activity;
	}
	
	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
		dialog = new ProgressDialog(activity);
		//dialog.setIndeterminate(true);
		dialog.setCancelable(false);
		dialog.show();
		dialog.setContentView(R.layout.customprogressdialog);
	}

	@Override
	protected String doInBackground(String... params)
	{
		System.out.println("#### PingTestTask doInBackground started");
		
		return CallWebServices.pingTests();
	}
	
	@Override
	protected void onProgressUpdate(String... values)
	{
		// TODO Auto-generated method stub
		super.onProgressUpdate(values);
		CommonUtility.showText(values[0]);
		if(dialog.isShowing())
			dialog.dismiss();
	}
	
	@Override
	protected void onPostExecute(String result)
	{
		// TODO Auto-generated method stub
		super.onPostExecute(result);

		if(dialog.isShowing())
			dialog.dismiss();

		if (result == null) {
            CommonUtility.simpleMessageDialog(activity, "Ping Tests Succeeded");
		}
		else {
		    CommonUtility.simpleMessageDialog(activity, result, "Ping Tests Failed");
		}
	}
	
}
