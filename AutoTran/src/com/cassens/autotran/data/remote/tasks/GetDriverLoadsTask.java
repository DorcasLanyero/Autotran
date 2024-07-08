package com.cassens.autotran.data.remote.tasks;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.activities.SignatureActivity;
import com.cassens.autotran.backendpoc.PoCUtils;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.activities.ClearLoadActivity;
import com.cassens.autotran.activities.DashboardActivity;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.constants.URLS;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.CallWebServices;
import com.cassens.autotran.data.remote.ExceptionHandling;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleTimeStamp;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.Date;

public class GetDriverLoadsTask extends AsyncTask<String, String, String>
{
    private static final Logger log = LoggerFactory.getLogger(GetDriverLoadsTask.class.getSimpleName());


	private static final boolean DEBUG = true;

	/**
	 * 
	 */
	private final DashboardActivity dashboardActivity;

	private final String driverNumber;
	
	private final Context context;

	private HashMap<Integer, String> childParentLookupHashMap;

	private boolean needUpgrade(String currentVersion, String newVersion) {
		String[] currentVals = currentVersion.split("\\.");
		String[] newVals = newVersion.split("\\.");

		for(int i = 0; i < currentVals.length && i < newVals.length; i++) {
			if (Integer.parseInt(currentVals[i]) > Integer.parseInt(newVals[i])) {
				return false;
			} else if (Integer.parseInt(newVals[i]) > Integer.parseInt(currentVals[i])) {
				return true;
			}
		}
		
		if(currentVals.length >= newVals.length) {
			return false;
		} else {
			return true;
		}
	}
	
	/**
	 * @param dashboardActivity
	 */
	public GetDriverLoadsTask(DashboardActivity dashboardActivity, String driverNumber, Context context) {
		this.dashboardActivity = dashboardActivity;
		this.driverNumber = driverNumber;
		this.context = context;
		this.childParentLookupHashMap = new HashMap<Integer, String>();

		checkDiskSpace();
	}

	private void checkDiskSpace() {
		float memoryFree = CommonUtility.getPercentageMemoryFree();

		log.debug(Logs.DEBUG, "Checking local diskspace: " + memoryFree + "% free");

		if(memoryFree < 15)
		{
			log.debug(Logs.DEBUG, "Low Memory Warning: " + memoryFree + " percent available");
			sendLowMemoryLoadEvent("LowMem");

		}
	}

	private void sendLowMemoryLoadEvent(String action) {
		SimpleTimeStamp sts = new SimpleTimeStamp();

		//Get the user and tablet info to include in the low memory message
		String memoryLevel = String.valueOf(CommonUtility.getPercentageMemoryFree());
		String tabletMac = CommonUtility.getMACAddress();
		String deviceSerial = CommonUtility.getDeviceSerial();
		String driverId= driverNumber;

		HashMap<String,String> reqBody = new HashMap<>();
		String eventString = TextUtils.join(",",
				new String[]{
						action,
						driverId,
						tabletMac,
                        deviceSerial,
						memoryLevel,
						sts.getUtcDateTime(),
						sts.getUtcTimeZone()
				});

		LoadEvent event = new LoadEvent();
		event.csv = eventString;
		DataManager.insertLoadEvent(context, event);
		SyncManager.pushLoadEventsLatched(context);
	}

	@Override
	protected void onPreExecute()
	{
		this.dashboardActivity.showProgressDialog("Starting dispatch");
	}

	@Override
	protected String doInBackground(String... params)
	{
		try
		{
			CommonUtility.dispatchLogThreadStartStop( "Started GetDriverLoadsTask", true);

			log.debug(Logs.DEBUG, "refreshing current driver on dispatch...");
			SyncManager.syncCurrentDriver(context);
			publishProgress("status", "Synced Driver");

			log.debug(Logs.INTERACTION, "Pulling loads from server for driver " + driverNumber);

			List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>();

			nameValuePairs.add(new BasicNameValuePair("user_id", driverNumber));
			nameValuePairs.add(new BasicNameValuePair("serial", CommonUtility.getDeviceSerial()));
			User driver = DataManager.getUserForDriverNumber(context, driverNumber);

			Load lastLoad = DataManager.getLastCompletedLoadForDriver(context, driver.user_id);

			if(lastLoad != null) {
				nameValuePairs.add(new BasicNameValuePair("lastCompletedLoad", lastLoad.loadNumber));
			}



			publishProgress("status", "Retrieving loads from server");
			this.dashboardActivity.response = CallWebServices.sendJson(URLS.poll_loads, nameValuePairs, context);
			if (DEBUG) CommonUtility.logJson(Logs.DISPATCH, "poll_loads response", this.dashboardActivity.response);
			publishProgress("status", "Data retrieved, processing response...");

			if(HelperFuncs.isNullOrEmpty(this.dashboardActivity.response)) {
				publishProgress("error", "Received no response from server, check network connection");
			} else {

				JSONObject j = new JSONObject(this.dashboardActivity.response);

				//get min app version
				if (j.has("minVersionRequired") && !j.getString("minVersionRequired").isEmpty()) {
					String minVersion = j.getString("minVersionRequired");
					log.debug(Logs.DEBUG, "MinAppVersion required: " + minVersion);
					String appVersion = this.context.getPackageManager().getPackageInfo(this.context.getPackageName(), 0).versionName;
					log.debug(Logs.DEBUG, "Current app version: " + appVersion);
					if (needUpgrade(appVersion, minVersion)) {
						log.debug(Logs.DEBUG, "need to upgrade");
						Intent bIntent = new Intent();
						bIntent.setAction(Constants.UPDATE_NEEDED);
						LocalBroadcastManager.getInstance(this.context).sendBroadcast(bIntent);
					}
				}

				//Parse load data
				if (j.has("restricted")) {
					log.debug(Logs.DEBUG, "driver is on restricted dispatch");

					Intent bIntent = new Intent();
					bIntent.setAction(Constants.RESTRICTED_DISPATCH);
					bIntent.putExtra("MESSAGE", j.getString("message"));
					bIntent.putExtra("DATA", j.getJSONArray("data").toString());
					bIntent.putExtra("ALREADY_IN_PROGRESS", j.getBoolean("alreadyInProgress"));
					LocalBroadcastManager.getInstance(this.context).sendBroadcast(bIntent);
					return null;
				}

				if (j.has("data") && !j.getJSONObject("data").has("empty")) {

					// START
					JSONObject data = j.getJSONObject("data");

					publishProgress("status", "Loads retrieved, parsing...");

					//Each 'name' is a load id
					JSONArray names = data.names();

					// For clearing out old TrainingRequirements
					Set<String> remoteIds = new HashSet<>();

					log.debug(Logs.INTERACTION, "Pulled " + names.length() + " loads from the server");

					//get ALL loads which are child loads
                    childParentLookupHashMap = DataManager.getCurrentChildLoadIds(context, driver.user_id);

                    //Loop through all of the load numbers of the INCOMING loads
					for (int index = 0; index < names.length(); index++) {
						final Load load = new Load();

						//pull the load from the json based on the load number
						final JSONObject loadJson = data.getJSONObject(String.valueOf(names.get(index).toString()));

						load.load_remote_id = loadJson.getString("id");

						remoteIds.add(load.load_remote_id);

						load.loadNumber = loadJson.getString("ldnbr");
						publishProgress("status", "Parsing Load " + load.loadNumber + "... " + (index + 1) + " of " + names.length() + " load(s)");

						//Pull an existing version of this load from the db based on the incoming remote id
						final Load existingLoad = DataManager.getLoadForLoadNumber(context, load.loadNumber);

						if (existingLoad != null && load.load_remote_id != null && !existingLoad.load_remote_id.equalsIgnoreCase(load.load_remote_id)) {
							CommonUtility.highLevelLog("WARNING: Received update for load $loadNumber with non-matching remote ID: " + load.load_remote_id, existingLoad);
						}

						//If the newly dispatched load already exists in the db and is a child load,
						// remove it from the list of child loads that we're keeping so that we
						// don't delete it.
						if (existingLoad != null && existingLoad.parent_load_id != -1) {
							childParentLookupHashMap.remove(Integer.valueOf(existingLoad.load_id));
						}
						completeDispatchProcessingForLoad(dashboardActivity, existingLoad, load, loadJson);

						// get training requirements for the load
                        JSONArray requirementsArray = loadJson.getJSONArray("TrainingRequirements");
						for(int i = 0; i < requirementsArray.length(); i++) {
						    JSONObject trainingJson = requirementsArray.getJSONObject(i);

                            final TrainingRequirement incomingReq = JsonUtils.parseTrainingRequirement(context, trainingJson);

                            if(incomingReq != null) {

                                TrainingRequirement existing = DataManager.getTrainingRequirement(context, incomingReq.id);
                                if(existing != null && !incomingReq.equals(existing)) {
                                    log.debug(Logs.DEBUG, "Updating existing training requirement with id " + incomingReq.id + " for load " + load.loadNumber + " in local DB");
                                    DataManager.updateTrainingRequirement(context, incomingReq);
                                }
                                else if(existing == null) {
                                    log.debug(Logs.DEBUG, "Adding new training requirement with id " + incomingReq.id + " for load " + load.loadNumber + " in local DB");
                                    DataManager.insertTrainingRequirementToLocalDB(context, incomingReq);
                                }
                            }
                        }
					}

					//Get all currently empty child loads and mark then deletable
					List <Integer> emptyLoadIds = DataManager.getAllEmptyChildLoadIds(context, driver.user_id);
					if(emptyLoadIds.size() > 0) {

						log.debug(Logs.DEBUG, "found " + emptyLoadIds.size() + " empty loads to delete");
						for (Integer loadId : emptyLoadIds) {
							if (AppSetting.PRUNE_LOADS_DAILY.getBoolean()) {
								log.debug(Logs.DELETES, "Child load was empty after dispatch. Marking for deletion: load_id=" + loadId);
								DataManager.markLoadDeletable(context, loadId);
							}
							else {
								DataManager.deleteLoadAndChildren(context, loadId, "child load was empty after dispatch");
							}
						}
					}

					// If any child loads remaining in the hash list have parents whose remote id
					// matches one that came down in the dispatch, it means that child load has
					// been orphaned.  Delete it.
					if (childParentLookupHashMap.size() != 0) {
						for (Integer childLoadId : childParentLookupHashMap.keySet()) {
							String parentId = childParentLookupHashMap.get(childLoadId);
							if (remoteIds.contains(parentId)) {
								Load childLoad = DataManager.getLoad(context, childLoadId);
								if (AppSetting.PRUNE_LOADS_DAILY.getBoolean()) {
									// Mark load deletable so that it will be auto-pruned
									log.debug(Logs.DELETES, "Child load was orphaned after dispatch. Marking for deletion: load_id=" + childLoadId);
									DataManager.markLoadDeletable(context, childLoadId);
								}
								else {
									DataManager.deleteLoadAndChildren(context, childLoadId, "child load was orphaned after dispatch");
								}
								// If we have orphaned the last uncompleted child load on the load,
								// we need to put a signature in the parent load, since it would
								// not have been done in SignatureActivity.  This is a HACK, but
								// it's reasonable since this whole parent/child structure (which
								// is the root HACK) is being mercifully eliminated in the new
								// architecture.
								SignatureActivity.checkAndSignParentLoad(childLoad, new SimpleTimeStamp(), new Date(), null);
							}
						}
					}

					// delete any training requirements whose loads aren't needed anymore
                    List<String> ids = DataManager.getAllLoadIds(context, driver.user_id);
                    log.debug(Logs.DEBUG, "Load ids for training requirements: " + ids);
                    long deleted = DataManager.deleteOrphanTrainingRequirements(context, ids);
					log.debug(Logs.DEBUG, "Deleted " + deleted + " orphan training requirements");
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

	private static boolean isDeliveryStarted(Load load) {
		for (Delivery delivery : load.deliveries) {
			if (!HelperFuncs.isNullOrEmpty(delivery.dealerSignature) ||
					!HelperFuncs.isNullOrEmpty(delivery.driverSignature)) {
				return true;
			}
			for (DeliveryVin deliveryVin : delivery.deliveryVins) {
				if (deliveryVin.inspectedDelivery) {
					return true;
				}
			}
		}
		return false;
	}

	@Override
	protected void onProgressUpdate(String... values)
	{
		super.onProgressUpdate(values);

		CommonUtility.dispatchLogMessage("GetDriverLoads: " + values[0] + ": " + values[1]);
		if(values[0].equals("status")) {
			dashboardActivity.updateStatus(values[1]);
		} else {
			CommonUtility.showText(values[1]);
		}

		this.dashboardActivity.dismissDialog();
	}
	
	@Override
	protected void onPostExecute(String result)
	{
		super.onPostExecute(result);
		CommonUtility.dispatchLogThreadStartStop( "Completed GetDriverLoadsTask", false);

		dashboardActivity.updateStatus("Load Data Updated");

		dashboardActivity.dismissDialog();

		CommonUtility.showText(context.getString( R.string.dispatch_complete));
	}


	public static void completeDispatchProcessingForLoad(DashboardActivity dashboardActivity, Load existingLoad, Load load, JSONObject loadJson) {
		//Load overwrite logic
		if (existingLoad == null) {
			// This is a new load
			CommonUtility.highLevelLog("New load downloaded from server: $loadNumber (remoteId=$loadId)", load);
			log.debug(Logs.INTERACTION, String.format("Pulled load %s: new", load.loadNumber));
			saveLoad(dashboardActivity, load, loadJson, false);
			if (AppSetting.POC_ECHO_TO_LAMBDA.getBoolean()) {
				PoCUtils.log("Got new load from dispatch. Uploading to Lambda: " + load.loadNumber);
				PoCUtils.sendLambdaUploadLoadRequest(AutoTranApplication.getAppContext(), load);
			}
			log.debug(Logs.DEBUG, String.format("Load %s is a new load", load.loadNumber));
		}
		else {
			log.debug(Logs.INTERACTION, String.format("Pulled load %s: existing", existingLoad.loadNumber));
			if (!HelperFuncs.isNullOrEmpty(existingLoad.driverPreLoadSignature) && !existingLoad.isParentLoad() && !existingLoad.isChildLoad()  && !isDeliveryStarted(existingLoad)) {
				CommonUtility.highLevelLog("Load $loadNumber update received from server after preload was signed", existingLoad);
				dashboardActivity.runOnUiThread(new Runnable() {
					@Override
					public void run() {
						AlertDialog.Builder builder = new AlertDialog.Builder(dashboardActivity);
						builder.setTitle("Load Update Notification");
						String prompt = String.format("Changes were received for load %s, but the preload has already been signed. " +
								"\n\nAre you still at the yard where this load was picked up?", existingLoad.getLoadNumber());
						builder.setMessage(prompt);

						builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								CommonUtility.logButtonClick(log, "Yes", prompt.replace("\n", " "));
								CommonUtility.highLevelLog("Driver said he/she is still at the lot, so load $loadNumber was updated and driver preload signature was cleared.", existingLoad);
								clearLoadForRefreshAndSave(dashboardActivity, existingLoad, load, loadJson,false);
								if (dashboardActivity != null && !dashboardActivity.paused) {
									CommonUtility.simpleMessageDialog(dashboardActivity, "The driver preload signature has been reset for load " + existingLoad.getLoadNumber() + ". Please review the preload, take any needed actions, then re-sign for the load before leaving the lot.");
								}
							}
						});
						builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int which) {
								CommonUtility.logButtonClick(log, "No", prompt.replace("\n", " "));
								CommonUtility.highLevelLog("Driver said he/she is no longer at the lot, so load $loadNumber was updated, but driver preload signature was retained.", existingLoad);
								clearLoadForRefreshAndSave(dashboardActivity, existingLoad, load, loadJson, true);
							}
						});
						builder.setCancelable(false);
						AlertDialog d = builder.create();
						builder.create().show();
					}
				});
			}
			else {
				if (HelperFuncs.isNullOrEmpty(existingLoad.driverPreLoadSignature)) {
					CommonUtility.highLevelLog("Update to load $loadNumber received from server before preload was signed. Updating load.", existingLoad);
					clearLoadForRefreshAndSave(dashboardActivity, existingLoad, load, loadJson,false);
				}
				else {
					if (isDeliveryStarted(existingLoad)) {
						CommonUtility.highLevelLog("Update to load $loadNumber received from server after deliveries were started. Updating load.", existingLoad);
					}
					else {
						CommonUtility.highLevelLog("Update to load $loadNumber received after preload was signed. Updating load.", existingLoad);
					}
					clearLoadForRefreshAndSave(dashboardActivity, existingLoad, load, loadJson, true);
				}
			}
		}
	}

	private static void clearLoadForRefreshAndSave(Context ctx, Load existingLoad, Load load, JSONObject loadJson, boolean preserveSignatures) {
		if (existingLoad == null) {
			return;
		}
		// Remove inspection company-inserted damages, since they'll be replaced wholesale
		// by the dispatch data.
		for (Delivery delivery : existingLoad.deliveries) {
			for (DeliveryVin deliveryVin : delivery.deliveryVins) {
				for (Iterator<Damage> it = deliveryVin.damages.iterator(); it.hasNext();) {
					Damage damage = it.next();
					// Delete any external damages, as these will be replaced
					if (damage.source.equals("external")) {
						DataManager.deleteDamage(ctx,damage);
						it.remove();
					}
				}
			}
		}
		// Now do the normal clear load.
		ClearLoadActivity.clearLoad(ctx, existingLoad, false, preserveSignatures, true);

		if (loadJson.has("driver_preload_signature_signedat")) {
			loadJson.remove("driver_preload_signature_signedat");
		}
		saveLoad(ctx, load, loadJson, preserveSignatures);
		if (AppSetting.POC_ECHO_TO_LAMBDA.getBoolean() && AppSetting.POC_SEND_LOAD_CHANGES.getBoolean()) {
			// This is a hack that is *probably* not necessary - PDK
			PoCUtils.log("Got update to existing load from dispatch. Uploading to Lambda: " + load.loadNumber);
			PoCUtils.sendLambdaUploadLoadRequest(AutoTranApplication.getAppContext(), load);
		}
	}

	private static void saveLoad(Context context, Load load, JSONObject loadJson, boolean preserveSignatures) {
		try {
			JsonUtils.parseAndSaveLoad(context, load, loadJson, preserveSignatures);
		} catch (JSONException e) {
			log.debug(Logs.DEBUG, "Error parsing load JSON.");
			CommonUtility.highLevelLog(String.format("Error: Load %s was not updated: Got invalid JSON from server", load.getLoadNumber()), load);
		}
	}
}
