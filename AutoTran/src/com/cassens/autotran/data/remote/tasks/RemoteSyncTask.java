package com.cassens.autotran.data.remote.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.os.AsyncTask;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.data.local.DataManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RemoteSyncTask extends AsyncTask<Integer, Void, Void>
{
    private static final Logger log = LoggerFactory.getLogger(RemoteSyncTask.class.getSimpleName());

	Context context;
	boolean result;
	ProgressDialog dialog;

	public RemoteSyncTask(Context context)
	{
		this.context = context;
	}
	
	@Override
	protected void onPreExecute()
	{
		super.onPreExecute();
	}

	@Override
	protected Void doInBackground(Integer... params)
	{
		System.out.println("#### RemoteSyncTask doInBackground started");

		CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from RemoteSyncTask.doInBackground()");
		DataManager.pushLocalDataToRemoteServer(context, params[0], false);
		return null;
	}
}
