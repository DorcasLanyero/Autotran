package com.cassens.autotran.data.remote.tasks;

import android.content.Intent;
import android.os.AsyncTask;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.activities.DashboardActivity;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.constants.URLS;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.remote.CallWebServices;
import com.cassens.autotran.data.remote.ExceptionHandling;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class GetSingleLoadTask extends AsyncTask<String, String, String>
{
    private static final Logger log = LoggerFactory.getLogger(GetSingleLoadTask.class.getSimpleName());

	public static String intentAction = "com.cassens.autotran.data.remote.tasks.GetSingleLoadTask";

	/**
	 * 
	 */
	private final String driverNumber;
	private final String loadNumber;
	private final int parentLoad;
	private final int parent_load_id;
	final DashboardActivity dashboardActivity;
	private final LocalBroadcastManager broadcaster;
	
	public GetSingleLoadTask(String driverNumber, DashboardActivity dashboardActivity, String loadNumber, int parentLoad, int parent_load_id) {
		this.driverNumber = driverNumber;
		this.dashboardActivity = dashboardActivity;
		this.loadNumber = loadNumber;
		this.parentLoad = parentLoad;
		this.parent_load_id = parent_load_id;
		this.broadcaster = LocalBroadcastManager.getInstance(dashboardActivity);
	}

	@Override
	protected void onPreExecute()
	{
		this.publishProgress("state", "PRE EXECUTE", Constants.TASK_STATUS.PRE_EXECUTE.toString());
	}

	@Override
	protected String doInBackground(String... params)
	{
		try
		{
			log.debug(Logs.INTERACTION, "Dispatching loads from server for driver " + driverNumber);
			
			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();
			
			nameValuePairs.add(new BasicNameValuePair("user_id", driverNumber));
			nameValuePairs.add(new BasicNameValuePair("load_number", loadNumber));
			nameValuePairs.add(new BasicNameValuePair("parentLoad", String.valueOf(parentLoad)));
			nameValuePairs.add(new BasicNameValuePair("parent_load_id", String.valueOf(parent_load_id)));
			nameValuePairs.add(new BasicNameValuePair("serial", CommonUtility.getDeviceSerial()));

			publishProgress("status", "Retrieving loads from server");
			String response = CallWebServices.sendJson(URLS.pick_load, nameValuePairs, dashboardActivity);
			publishProgress("status", "Data retrieved, checking for loads...");

			CommonUtility.logJson(Logs.DISPATCH, "pick_load response", response);
			
			JSONObject j = new JSONObject(response);

			//Parse load data
			if(j.has("data") && !j.getJSONObject("data").has("empty")) {

				JSONObject data = j.getJSONObject("data");

				publishProgress("status", "Loads retrieved, parsing...");

    			//Each 'name' is a load id
    			JSONArray names = data.names();
    			
    			log.debug(Logs.INTERACTION, "Pulled " + names.length() + " loads from the server");
    			
    			for(int index = 0; index < names.length(); index++) {
    				final Load load = new Load();

					//pull the load from the json based on the load number
					final JSONObject loadJson = data.getJSONObject(String.valueOf(names.get(index).toString()));
    				
    				load.load_remote_id = loadJson.getString("id");
    				load.loadNumber = loadJson.getString("ldnbr");
    				publishProgress("status", "Parsing Load " + load.loadNumber  + "... " + index + " of " + names.length() + " load(s)");
    				log.debug(Logs.INTERACTION, "Parsing Load " + load.loadNumber  + "... " + index + " of " + names.length() + " load(s)");

    				final Load existingLoad = DataManager.getLoadForLoadNumber(dashboardActivity, load.loadNumber);

    				GetDriverLoadsTask.completeDispatchProcessingForLoad(dashboardActivity, existingLoad, load, loadJson);
    			}
			}
			
		}
		catch (Exception e)
		{
			e.printStackTrace();
			publishProgress("error", ExceptionHandling.getFormattedMessageFromException(e.getMessage()));
		}
		return "";
	}

	@Override
	protected void onProgressUpdate(String... values)
	{
		super.onProgressUpdate(values);
		
		Intent update = new Intent(intentAction);
		update.setFlags(Constants.TASK_STATUS.PROGRESS_UPDATE.ordinal());
		
		if (values.length == 2) {
			update.putExtra(values[0].toString(), values[1].toString());
		}
		this.broadcaster.sendBroadcast(update);
	}
	
	@Override
	protected void onPostExecute(String result)
	{
		super.onPostExecute(result);
		
		this.publishProgress("status", "Load Data updated");
	
		this.publishProgress("state", "PRE EXECUTE", Constants.TASK_STATUS.POST_EXECUTE.toString());

		CommonUtility.showText(dashboardActivity.getString(R.string.load_updated));
	}
}
