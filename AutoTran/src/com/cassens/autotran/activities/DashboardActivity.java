package com.cassens.autotran.activities;

import static com.cassens.autotran.hardware.PiccoloManager.PICCOLO_OPEN_ERROR;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.res.Resources;
import android.graphics.Color;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cassens.autotran.BuildConfig;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.local.UserDetails;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Inspection;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.YardInventory;
import com.cassens.autotran.data.model.lookup.Terminal;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.data.remote.tasks.ConsolidatedDataPullTask;
import com.cassens.autotran.data.remote.tasks.GetSingleLoadTask;
import com.cassens.autotran.dialogs.PiccoloAccessNeededDialog;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.cassens.autotran.hardware.PiccoloManager;
import com.cassens.autotran.receivers.DriverActionQueueReceiver;
import com.cassens.autotran.receivers.RemoteSyncReceiver;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.DetectedActivitiesIntentService;
import com.sdgsystems.util.HelperFuncs;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.Date;
import java.util.List;

/**
 * Project : AUTOTRAN Description : DashboardActivity class shows all button of application
 *
 * @author Hemant
 **/


public class DashboardActivity extends ProgressDialogActivity {
	public String response;
	public static String id = "";
	public static String user_id = "";
	public static String driverNumber = "";

	private String userType;
	public Bundle bundle;
	UserDetails mCurrentUser;
	User currentUser;
	public int loadCount;
	public ImageView ivPreload;
	public ImageView ivDelivery;
	public final Activity currentActivity = this;

    // Request Codes for Launched Activities
    private static final int REQ_CODE_LOAD_OR_DELIVERY_LIST = 1001;
    private static final int REQ_CODE_UTILITIES = 1002;
    private static final int REQ_CODE_YARD_INVENTORY = 1003;
    private static final int REQ_CODE_YARD_ENTRY_EXIT = 1004;
    private static final int REQ_CODE_PLANT_RETURN = 1005;
    private static final int REQ_CODE_RECEIVE_VEHICLE = 1006;
    private static final int REQ_CODE_INSPECT_VEHICLE = 1007;
    private static final int REQ_CODE_SHUTTLE_LOAD = 1008;
    private static final int REQ_CODE_COMMUNICATIONS = 1009;

	private static final Logger log = LoggerFactory.getLogger(DashboardActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	private UploadsReceiver pendingUploadsReceiver;
	private RemoteSyncReceiver remoteSyncReceiver;
	private DriverActionQueueReceiver driverActionQueueReceiver;
	private Receiver updateReceiver;

	private RemoteSyncReceiver getSyncReceiverInstance() {
		if (remoteSyncReceiver == null) {
			remoteSyncReceiver =  new RemoteSyncReceiver();
		}
		return remoteSyncReceiver;
	}

	private DriverActionQueueReceiver getDriverActionQueueReceiver() {
		if (driverActionQueueReceiver == null) {
			driverActionQueueReceiver = new DriverActionQueueReceiver();
		}
		return driverActionQueueReceiver;
	}

	public static TextView mDrivingModeText;
	static Activity dashboardActivity;
	public static void setDetectedActivityIndicator(final String msg) {
		if (dashboardActivity != null) {
			dashboardActivity.runOnUiThread(new Runnable() {
				@Override
				public void run() {
					mDrivingModeText.setText(msg);
					mDrivingModeText.invalidate();
				}
			});
		}
	}


	private void updateUnreadMessagesCount(Activity thisContext, TextView unreadMessageCount) {
		int counter = DataManager.getUnreadMessageCount(thisContext);

		unreadMessageCount.setText(Integer.toString(counter));
		if (counter == 0) {
			unreadMessageCount.setVisibility(View.INVISIBLE);
		}
		else {
			unreadMessageCount.setVisibility(View.VISIBLE);
		}
	}


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		log.debug(Logs.DEBUG, "Creating activity");

		setContentView(R.layout.activity_dashboard);
		ivPreload = findViewById(R.id.imageView3);
		ivDelivery = findViewById(R.id.imageView4);

		TextView title = findViewById(R.id.ACTIVITY_TITLE);
		mDrivingModeText = findViewById(R.id.LocationMessageBox);

		ImageView messageButton = findViewById(R.id.imageView1);

		bundle = getIntent().getExtras();

		id = bundle.getString("id");
		user_id = bundle.getString("user_id");
		userType = bundle.getString("user_type");
		driverNumber = bundle.getString("driverNumber");

		currentUser = DataManager.getUserForDriverNumber(this, driverNumber);

		TextView contactInfo = findViewById(R.id.ContactInfoTv);
		CommonUtility.appendEpodContactInfo(this, contactInfo, currentUser);

		String source = bundle.getString("source");
		if (source != null && source.equals("error")) {
			//came from crash
			//final Context context = getApplicationContext();
			//CommonUtility utility = new CommonUtility();
			//utility.sendLogList(CommonUtility.getDriverNumber(context), context, "small");
			CommonUtility.showText("An error has occurred. Please use the Utilities menu to upload your logs");
		}

		System.out.println("userId=" + currentUser.user_id + " user_id=" + user_id);

		System.out.println("@@@@ :: "+currentUser.user_remote_id);
		System.out.println("@@@@ :: "+currentUser.fullName);
		System.out.println("@@@@ :: "+currentUser.driverNumber);
		System.out.println("@@@@ :: "+currentUser.userType);

//		DataManager.getInitialData(this, driverNumber, Integer.getInteger(user_id));
//		FakeDataManager.getAllInfo();

		setRemoteSyncEvents();

		this.updateReceiver = new Receiver();
		this.pendingUploadsReceiver = new UploadsReceiver();

		dashboardActivity = this;

		enableTruckNumberChangeHandler(new TruckNumberChangeCallback() {
			@Override
			public void onTruckNumberChange(String newTruckNumber){
				updateSubtitle();
			}
		});
    }

	@Override
	protected void onResume() {
	    super.onResume();
		log.debug(Logs.DEBUG, "Resuming dashboard activity");

		TextView unreadMessageCount = findViewById(R.id.message_counter);
		updateUnreadMessagesCount(this, unreadMessageCount);

		if (LoginActivity.isNewLoginNeeded(this)) {
			CommonUtility.showText("You are required to login at least once per day. Please log back in.", Toast.LENGTH_LONG);
			//CommonUtility.setManualTruckNumberOverride(getApplicationContext(), false);
			Intent intent = new Intent(this, SplashActivity.class);
			startActivity(intent);
			finish();
			return;
		}

		CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from DashboardActivity.onResume()");
		SyncManager.pushLocalDataToRemoteServer(DashboardActivity.this.getApplicationContext(), Integer.parseInt(currentUser.driverNumber), true);

		if(PreferenceManager.getDefaultSharedPreferences(this).getBoolean("actionsPollInterval-changed", false)) {
			setRemoteSyncEvents();

			PreferenceManager.getDefaultSharedPreferences(this).edit().putBoolean("actionsPollInterval-changed", false).commit();
		}

		TextView title = findViewById(R.id.ACTIVITY_TITLE);
		try {
			title.setText("Cassens AutoTran v" + getPackageManager().getPackageInfo(getPackageName(), 0).versionName);
		} catch (NameNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		if (PiccoloManager.isDocked() && TruckNumberHandler.getPiccoloTruckNumberInt(this,
				true) == PICCOLO_OPEN_ERROR) {
			// This condition occurs whenever the handheld is booted while docked. Re-docking the
			// device clears the condition, so we prompt the driver to do it.
			CommonUtility.simpleMessageDialog(this,
					this.getString(R.string.piccolo_redock_prompt),
					this.getString(R.string.piccolo_redock_title));
		}
		showPiccoloAccessDialogIfNeeded();
		updateSubtitle();

		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.UPDATE_NEEDED);
		filter.addAction(Constants.RESTRICTED_DISPATCH);
		LocalBroadcastManager.getInstance(this).registerReceiver(this.updateReceiver, filter);

		updateOverlays();
		LocalBroadcastManager.getInstance(this).registerReceiver(pendingUploadsReceiver, new IntentFilter(SyncManager.PENDING_UPLOADS_CHANGE));

		String address = CommonUtility.getMACAddress();

		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
		prefs.edit().putString("MAC_ADDRESS", address).commit();
		log.debug(Logs.DEBUG, "MAC: " + address);
		DetectedActivitiesIntentService.refreshDetectedActivityIndicators();

		//IntentFilter piccoloFilter = new IntentFilter(Constants.ACTION_PICCOLO_TRUCK_NUM_RECEIVED);
		//registerReceiver(piccoloNumberChangedReceiver, piccoloFilter);
	}

	public void updateSubtitle() {
		String truckNumber = TruckNumberHandler.getTruckNumber(this);
		TextView subtitle = findViewById(R.id.ACTIVITY_SUBTITLE);

		subtitle.setText(String.format("Driver: %s   Truck: %s", driverNumber, truckNumber));

		if (BuildConfig.AUTOTRAN_TRUCK_HACK) {
			subtitle.setText(String.format("Driver: %s   Truck: %s (%s)", driverNumber, truckNumber, (TruckNumberHandler.isTruckNumberSourceManual(this) ? "Manual" : "Piccolo")));

			TextView dockedStatus = findViewById(R.id.DockedStatus);
			dockedStatus.setVisibility(View.VISIBLE);
			if (PiccoloManager.isDocked()) {
				dockedStatus.setText(String.format("Docked (Piccolo truck #%s)", TruckNumberHandler.getPiccoloTruckNumber(this, true)));
				dockedStatus.setTextColor(getResources().getColor(R.color.ForestGreen));
			} else if (PiccoloManager.isPlugged(this)) {
				dockedStatus.setText(String.format("Charging (Piccolo truck #%s)", TruckNumberHandler.getPiccoloTruckNumber(this, true)));
				dockedStatus.setTextColor(getResources().getColor(R.color.DarkGreen));
			} else {
				dockedStatus.setText(String.format("Undocked (Piccolo truck #%s)", TruckNumberHandler.getPiccoloTruckNumber(this, true)));
				dockedStatus.setTextColor(getResources().getColor(R.color.Gray));
			}
		}
		subtitle.invalidate();
	}

	private void setRemoteSyncEvents() {
		int pollInterval = AppSetting.REMOTE_SYNC_INTERVAL.getInt() * 60;
		getDriverActionQueueReceiver().setRepeatingTask(getApplicationContext(), pollInterval);
		getSyncReceiverInstance().setRepeatingTask(getApplicationContext(), pollInterval);

		log.debug(Logs.DEBUG, "setting poll interval to " + pollInterval + " seconds");
	}

	@Override
	protected void onPause() {
		super.onPause();
		LocalBroadcastManager.getInstance(this).unregisterReceiver(pendingUploadsReceiver);
		LocalBroadcastManager.getInstance(this).unregisterReceiver(this.updateReceiver);
		PiccoloAccessNeededDialog.Companion.dismissIfShowing();
		//unregisterReceiver(piccoloNumberChangedReceiver);
	}

	@Override
	protected void onDestroy() {
		dashboardActivity = null;
		super.onDestroy();

		getSyncReceiverInstance().cancelRepeatingTask(this);
		getDriverActionQueueReceiver().cancelRepeatingTask(this);
	}

	public void getMessages(View v) {
		log.debug(Logs.INTERACTION, "launch driver message inbox");
		startActivity(new Intent(this, ViewMessagesActivity.class));
	}

	public void setting(View v) {
		CommonUtility.showText("Settings");

		float percentageFree = CommonUtility.getPercentageMemoryFree();

		CommonUtility.showText(new DecimalFormat("#.##").format(percentageFree) + "% Memory Free");
	}

	public void dispatch(View v) throws JSONException {
		CommonUtility.logButtonClick(log, "Dispatch");

		if(currentUser.driverNumber == null || currentUser.driverNumber.trim() == "" || currentUser.user_id <= 0) {
			CommonUtility.showText("Driver number not set, please log out and provide a driver number before dispatching");

			log.debug(Logs.INTERACTION, "message: "  + "Driver number not set, please log out and provide a driver number before dispatching");
			return;
		}

		CommonUtility.showText("Downloading current loads...");

		CommonUtility.dispatchLogMessage("Dispatch started from dashboard");
		DataManager.refreshDispatchData(DashboardActivity.this, currentUser.driverNumber, currentUser.user_id);
    }

	private boolean showPiccoloAccessDialogIfNeeded() {
		return PiccoloAccessNeededDialog.Companion.displayIfNeeded(this);
	}

	public void preload(View v) {
		CommonUtility.logButtonClick(log, "Preload");
		if (showPiccoloAccessDialogIfNeeded()) {
			return;
		}
		currentUser = DataManager.getUserForDriverNumber(this, driverNumber);

		final int restrictionLevel = currentUser.getLicenseExpirationRestrictionLevel();
		if (restrictionLevel == User.LICENSE_EXPIRE_OK) {
			startPreload();
		} else {
			String message = getLicenseStatusMessage(currentUser);
			log.debug(Logs.INTERACTION, "showing message: " + message);
			Builder builder = new AlertDialog.Builder(DashboardActivity.this);
			builder.setMessage(message);
			builder.setPositiveButton("OK", (dialogInterface, i) -> {
				if (restrictionLevel != User.LICENSE_EXPIRE_LOCK) {
					startPreload();
				}
			});
			builder.create().show();
		}
	}

	private void startPreload() {
		Intent intent = new Intent(DashboardActivity.this, PreloadActivity.class);
		intent.putExtras(bundle);
		myStartActivityForResult(intent, REQ_CODE_LOAD_OR_DELIVERY_LIST);
	}

	public void delivery(View v) {
		CommonUtility.logButtonClick(log, "Delivery");
		if (showPiccoloAccessDialogIfNeeded()) {
			return;
		}
		Intent intent = new Intent(DashboardActivity.this, DeliveryActivity.class);
		intent.putExtras(bundle);
		myStartActivityForResult(intent, REQ_CODE_LOAD_OR_DELIVERY_LIST);
	}

	public void shuttle(View v) {
		CommonUtility.logButtonClick(log, "Shuttle");
		if (showPiccoloAccessDialogIfNeeded()) {
			return;
		}
        currentUser = DataManager.getUserForDriverNumber(this, driverNumber);

		final int restrictionLevel = currentUser.getLicenseExpirationRestrictionLevel();
		String message = getLicenseStatusMessage(currentUser);
		if (restrictionLevel == User.LICENSE_EXPIRE_OK) {
			shuttlePrep();
		} else {
			log.debug(Logs.INTERACTION, "showing message: " + message);
			Builder builder = new AlertDialog.Builder(DashboardActivity.this);
			builder.setMessage(message);
			builder.setPositiveButton("OK", (dialogInterface, i) -> {
				if (restrictionLevel != User.LICENSE_EXPIRE_LOCK) {
					shuttlePrep();
				}
			});
			builder.create().show();
		}
	}

	private void shuttlePrep() {
		CommonUtility.logButtonClick(log, "Shuttle Load");
        List<Terminal> terminals = DataManager.getTerminalList(getApplicationContext());

        if(terminals == null || terminals.size() == 0) {
            showProgressDialog("Pulling remote data for shuttle moves");
            DataManager.refreshLookupCodes(this, false, new ConsolidatedDataPullTask.IConsolidatedDataPullCallback() {
                @Override
                public void updateProgress(String status) {
                    updateStatus(status);
                }

                @Override
                public void complete() {
                    dismissDialog();
                    startShuttleMove();
                }
            });
        } else {
            startShuttleMove();
        }
    }

	private void startShuttleMove() {
		Intent intent = new Intent(DashboardActivity.this, ShuttleSelectLoadActivity.class);
		intent.putExtras(bundle);
		myStartActivityForResult(intent, REQ_CODE_SHUTTLE_LOAD);
	}

	public void yardI(View v)
	{
		CommonUtility.logButtonClick(log, "Yard Inventory");
        Intent intent = new Intent(DashboardActivity.this, YardInventoryActivity.class);
        intent.putExtras(bundle);
        intent.putExtra("lot_locate", false);
        myStartActivityForResult(intent, REQ_CODE_YARD_INVENTORY);
	}

	public void lot(View v)
	{
		CommonUtility.logButtonClick(log, "Lot Locate");
        Intent intent = new Intent(DashboardActivity.this, YardInventoryActivity.class);
        intent.putExtras(bundle);
        intent.putExtra("lot_locate", true);
        myStartActivityForResult(intent, REQ_CODE_YARD_INVENTORY);
	}

	public void yardE(View v)
	{
        Intent intent = new Intent(DashboardActivity.this, YardEntryExitActivity.class);
        intent.putExtras(bundle);
        myStartActivityForResult(intent, REQ_CODE_YARD_ENTRY_EXIT);
	}

	public void plantReturn(View v)
	{
        Intent intent = new Intent(DashboardActivity.this, PlantReturnActivity.class);
        intent.putExtras(bundle);
        myStartActivityForResult(intent, REQ_CODE_PLANT_RETURN);
	}

    public void receiveVehicle(View v)
    {
        Intent intent = new Intent(DashboardActivity.this, ReceiveVehicleActivity.class);
        intent.putExtras(bundle);
        myStartActivityForResult(intent, REQ_CODE_RECEIVE_VEHICLE);
    }
    
	public void inspect(View v) {
		CommonUtility.logButtonClick(log, "Inspect Vehicle");
		currentUser = DataManager.getUserForDriverNumber(this, driverNumber);     //refresh in case there's been a change to user db from driver message
		if(currentUser.inspectionAccess == 1) {
			Intent intent = new Intent(DashboardActivity.this, InspectVehicleActivity.class);
			intent.putExtras(bundle);
			myStartActivityForResult(intent, REQ_CODE_INSPECT_VEHICLE);
		} else {
			AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
			builder.setMessage("Driver " + currentUser.driverNumber + " does not have access to the inspection screen")
					.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int id) {
							dialog.dismiss();
						}
					});
			log.debug(Logs.INTERACTION, "Dialog: " + "Driver " + currentUser.driverNumber + " does not have access to the inspection screen");
			builder.create().show();
		}
	}

	public void communications(View v)
	{
		CommonUtility.logButtonClick(log, "Communications");
		if (BuildConfig.AUTOTRAN_TRUCK_HACK) {
			TruckNumberHandler.incrementHackedTruckNumber();
		}
        Intent intent = new Intent(DashboardActivity.this, CommunicationsActivity.class);
        intent.putExtras(bundle);
        intent.putExtra("cameFrom", "communications");
        intent.putExtra("driverNumber", driverNumber);
        myStartActivityForResult(intent, REQ_CODE_COMMUNICATIONS);
	}

	public void utilities(View v)
	{
		CommonUtility.logButtonClick(log, "Utilities");
		Intent intent = new Intent(DashboardActivity.this, UtilitiesActivity.class);
		intent.putExtras(bundle);
		intent.putExtra("cameFrom", "utilities");
		intent.putExtra("driverNumber", driverNumber);
		myStartActivityForResult(intent, REQ_CODE_UTILITIES);
    }

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {

        switch (requestCode) {

		case REQ_CODE_UTILITIES:
			if (resultCode == UtilitiesActivity.RESULT_LOGOFF) {
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
				SharedPreferences.Editor editor = prefs.edit();
				editor.putLong("lastLoginTime", 0).commit();
				editor.commit();
				CommonUtility.setDriverNumAndTerminal(DashboardActivity.this, "", -1);
				Intent intent = new Intent(this, SplashActivity.class);
				startActivity(intent);
				finish();
			}
			break;


		case REQ_CODE_LOAD_OR_DELIVERY_LIST:
        case REQ_CODE_YARD_INVENTORY:
        case REQ_CODE_INSPECT_VEHICLE:
   /*
            // These codes indicate that we want to restart the activity
            // (as a short-cut to refreshing the choice list).
            if (resultCode == 10) {
                delivery(null);
            }
            else if (resultCode == 11) {
                preload(null);
            } */
            break;

        default:
            break;
        }
    }

	private void updateOverlays() {
		// HACK - Remove the overlay updates for the WorkManager pilot. This is a
		//        *very* expensive query.
		/*
		HashMap<String,Integer> pendingCounts = DataManager.getUploadQueueCount(this);

	    updateOverlay(R.id.preloadUploadOverlay, pendingCounts.get(Load.class.getName()));
		updateOverlay(R.id.shuttleLoadUploadOverlay, pendingCounts.get(Load.class.getName() + "shuttle"));
	    updateOverlay(R.id.deliveryUploadOverlay, pendingCounts.get(Delivery.class.getName()));
	    updateOverlay(R.id.yardInventoryUploadOverlay, pendingCounts.get(YardInventory.class.getName()));
	    updateOverlay(R.id.lotLocateUploadOverlay, pendingCounts.get("LotLocate"));
	    updateOverlay(R.id.inspectVehicleUploadOverlay, pendingCounts.get(Inspection.class.getName()));

		 */

		// Set each overlay to 0, which makes it invisible.
		updateOverlay(R.id.preloadUploadOverlay, 0);
		updateOverlay(R.id.shuttleLoadUploadOverlay, 0);
		updateOverlay(R.id.deliveryUploadOverlay, 0);
		updateOverlay(R.id.yardInventoryUploadOverlay, 0);
		updateOverlay(R.id.lotLocateUploadOverlay, 0);
		updateOverlay(R.id.inspectVehicleUploadOverlay, 0);

		// END_HACK

/*
		updateOverlay(R.id.yardExitUploadOverlay, pendingCounts.get(YardExit.class.getName()));
		updateOverlay(R.id.returnToPlantUploadOverlay, pendingCounts.get(PlantReturn.class.getName()));
	    updateOverlay(R.id.receiveVehicleUploadOverlay, pendingCounts.get(ReceivedVehicle.class.getName()));
 */
	}

	private void updateOverlay(int resId, int uploadCount) {
	    RelativeLayout layout;
	    layout = findViewById(resId);

	    // HACK
		// Pending redesign of update notifications, we're just making updateOverlay
		// invisible.
		layout.setVisibility(View.GONE);

	    /*
	    if(uploadCount <= 0) {
	        layout.setVisibility(View.GONE);
	        return;
	    }

	    layout.setVisibility(View.VISIBLE);
	    if(layout.getChildCount() != 1) {
	        throw new IllegalStateException("Too many children for upload count overlay relative layout");
	    }

	    TextView label = (TextView) layout.getChildAt(0);
	    label.setText("" + uploadCount);

	     */
		// END HACK
	}

	private int getOverlayValue(int resId) {
		RelativeLayout layout;
		layout = findViewById(resId);

		if(!layout.isShown()) {
			return 0;
		}

		TextView label = (TextView) layout.getChildAt(0);
		return Integer.parseInt(label.getText().toString());
	}

	private class UploadsReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String uploadObject = intent.getStringExtra(SyncManager.UPLOAD_OBJECT);
			int change = intent.getIntExtra(SyncManager.UPLOAD_CHANGE, 0);

			if (uploadObject.equals(Load.class.getName())) {
				updateOverlay(R.id.preloadUploadOverlay, getOverlayValue(R.id.preloadUploadOverlay) + change);
			} else if (uploadObject.equals(Load.class.getName() + "shuttle")) {
				updateOverlay(R.id.shuttleLoadUploadOverlay, getOverlayValue(R.id.shuttleLoadUploadOverlay) + change);
			} else if (uploadObject.equals(Delivery.class.getName())) {
				updateOverlay(R.id.deliveryUploadOverlay, getOverlayValue(R.id.deliveryUploadOverlay) + change);
			} else if (uploadObject.equals(YardInventory.class.getName())) {
				updateOverlay(R.id.yardInventoryUploadOverlay, getOverlayValue(R.id.yardInventoryUploadOverlay) + change);
			} else if (uploadObject.equals("LotLocate")) {
				updateOverlay(R.id.lotLocateUploadOverlay, getOverlayValue(R.id.lotLocateUploadOverlay) + change);
			} else if (uploadObject.equals(Inspection.class.getName())) {
				updateOverlay(R.id.inspectVehicleUploadOverlay, getOverlayValue(R.id.inspectVehicleUploadOverlay) + change);
			}
/*
			} else if (uploadObject.equals(YardExit.class.getName())) {
				updateOverlay(R.id.yardExitUploadOverlay, getOverlayValue(R.id.yardExitUploadOverlay) + change);
			} else if (uploadObject.equals(PlantReturn.class.getName())) {
				updateOverlay(R.id.returnToPlantUploadOverlay, getOverlayValue(R.id.returnToPlantUploadOverlay) + change);
			} else if (uploadObject.equals(ReceivedVehicle.class.getName())) {
				updateOverlay(R.id.receiveVehicleUploadOverlay, getOverlayValue(R.id.receiveVehicleUploadOverlay) + change);
			}
 */
		}
	}
	
	private class Receiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {

			if (intent.getAction().equals(Constants.UPDATE_NEEDED)) {
				AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
				builder.setTitle("Update Needed")
						.setMessage(R.string.update_needed_msg)
						.setPositiveButton("Dismiss", new DialogInterface.OnClickListener() {
							@Override
							public void onClick(DialogInterface dialog, int id) {
								dialog.dismiss();
							}
						});
				log.debug(Logs.INTERACTION, "Dialog: " + R.string.update_needed_msg);
				builder.create().show();
			} else if (intent.getAction().equals(Constants.RESTRICTED_DISPATCH)) {
				String data = intent.getStringExtra("DATA");
				final boolean alreadyInProgress = intent.getBooleanExtra("ALREADY_IN_PROGRESS", false);
				CommonUtility.logJson(Logs.DEBUG, "Loads provided during restricted dispatch", data);
				//log.debug(Logs.INTERACTION, data);

				LayoutInflater inflater = currentActivity.getLayoutInflater();
				final View dialogView = inflater.inflate(R.layout.dialog_dispatch, null);

				((TextView) dialogView.findViewById(R.id.headline)).setText(intent.getStringExtra("MESSAGE"));
				JSONArray loads = new JSONArray();
				try {
					loads = new JSONArray(intent.getStringExtra("DATA"));
				} catch (JSONException e) {}

				if (loads.length() == 0) {
					CommonUtility.simpleMessageDialog(DashboardActivity.this, "No new loads available");
					return;
				}

				JSONObject[] loadArray = new JSONObject[loads.length()];
				for (int i = 0; i < loads.length(); i++) {
					try {
						loadArray[i] = loads.getJSONObject(i);
					} catch (JSONException e) {}
				}

				if (loadArray.length == 1) {
					// If there's only one load, just process it.
					// Note: This *probably* obviates the alreadyInProgress logic, since the server
					//       should always return only one load if the load is in progress.
					String ln;
					try {
						ln = loadArray[0].getJSONObject("Load").getString("ldnbr");
					} catch (JSONException e) {
						ln = "?";
					}
					log.debug(Logs.INTERACTION, "Load " + ln + " updated via restricted dispatch");

					startSingleLoadTaskForLoad(loadArray[0]);
				}
				else {
					try {
						String loadChoices = loadArray[0].getJSONObject("Load").getString("ldnbr");
						for (int i = 1; i < loadArray.length; i++) {
							loadChoices += ", " + loadArray[i].getJSONObject("Load").getString("ldnbr");
						}
						log.debug(Logs.INTERACTION, "Dispatch dialog offered following choice of loads: " + loadChoices);
					} catch (JSONException e) {
						log.debug(Logs.INTERACTION, "Error processing load list on restricted dispatch!");
					};
					AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
					builder.setTitle("Dispatch")
							.setView(dialogView)
							.setPositiveButton((alreadyInProgress ? "Ok" : "Cancel"), new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int id) {
									if (alreadyInProgress) {
										try {
											log.debug(Logs.INTERACTION, "In-progress load " + loadArray[0].getJSONObject("Load").getString("ldnbr") + " displayed to user during restricted dispatch");
										} catch (JSONException e) {
											e.printStackTrace();
										}
										startSingleLoadTaskForLoad(loadArray[0]);
									}
									dialog.dismiss();
								}
							});
					AlertDialog dialogInstance = builder.create();

					final ListView loadListView = dialogView.findViewById(R.id.list);
					LoadSelectAdapter loadListAdapter = new LoadSelectAdapter(currentActivity, dialogInstance, R.layout.load_or_delivery_element, loadArray);
					loadListView.setAdapter(loadListAdapter);

					dialogInstance.show();
				}
			}
		}

		private void startSingleLoadTaskForLoad(JSONObject load) {
			try {
			new GetSingleLoadTask(driverNumber, DashboardActivity.this,
				load.getJSONObject("Load").getString("ldnbr"),
				load.getJSONObject("Load").getInt("parentLoad"),
				load.getJSONObject("Load").getInt("id")).execute();
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}

		public class LoadSelectAdapter extends ArrayAdapter<JSONObject> {
			private JSONObject[] loads;
			private Context ctx;
			private int layout;
			private DialogInterface dialog;

			public LoadSelectAdapter(Context context, DialogInterface dialog, int resource, JSONObject[] objects) {
				super(context, resource, objects);

				this.loads = objects;
				this.ctx = context;
				this.layout = resource;
				this.dialog = dialog;
			}

			@Override
			public View getView(final int position, View convertView, ViewGroup parent) {
				View row = convertView;

				final JSONObject load = this.loads[position];

				if (row == null) {
					LayoutInflater inflater = ((Activity) this.ctx).getLayoutInflater();
					row = inflater.inflate(this.layout, parent, false);
				}

				row.findViewById(R.id.imageView1).setVisibility(View.GONE);

				String tmpLoadNumber = "";
				int tmpParentLoad = 0;
				int tmpParentLoadId = -1;

				final TextView loadNumber = row.findViewById(R.id.ACTIVITY_TITLE);
				final TextView subtitle = row.findViewById(R.id.tvSubtitle);
				try {
					tmpLoadNumber = load.getJSONObject("Load").getString("ldnbr");
					tmpParentLoad = load.getJSONObject("Load").getInt("parentLoad");
					tmpParentLoadId = load.getJSONObject("Load").getInt("id");

					loadNumber.setText(tmpLoadNumber + " - First Drop: " + load.getJSONObject("Load").getString("firstDrop"));

					String dealers = "";
					for (int i = 0; i < load.getJSONArray("Deliveries").length(); i++) {
						dealers += load.getJSONArray("Deliveries").get(i) + ", ";
					}
					subtitle.setText(dealers.substring(0, dealers.length() - 2));
				} catch (JSONException e) {
					e.printStackTrace();
				}

				final String ldnbr = tmpLoadNumber;
				final int parentLoad = tmpParentLoad;
				final int parent_load_id = tmpParentLoadId;

				row.setOnClickListener(new View.OnClickListener() {
					@Override
					public void onClick(View v) {
						CommonUtility.logChoiceClick(log, ldnbr, "load to dispatch");
						new GetSingleLoadTask(driverNumber, DashboardActivity.this, ldnbr, parentLoad, parent_load_id).execute();
						dialog.dismiss();
					}
				});

				if (position % 2 == 0) {
					loadNumber.setBackgroundColor(Color.WHITE);
					subtitle.setBackgroundColor(Color.WHITE);
				}

				return row;
			}
		}

	}

	private void myStartActivityForResult(Intent intent, int requestCode) {
		//if (!forceReloginIfNeeded()) {
			startActivityForResult(intent, requestCode);
		//}
	}


	private String getLicenseStatusMessage(String licenseName, Date expirationDate, User.ExpirationStatus status) {
		String message = "";
		String expireDateStr = HelperFuncs.dateOnlyString(expirationDate);

		switch (status) {
			case InWarningPeriod:
				message += String.format("Your %s expires %s. It must be renewed by %s or you will not be permitted to start new loads.\n",
										licenseName, expireDateStr,
										HelperFuncs.dateOnlyString(HelperFuncs.addSubtractDays(expirationDate, -AppSetting.LICENSE_LOCK_DAYS.getInt())));
				break;

			case InRestrictedPeriod:
				message += String.format("Your %s expires on %s. You are not allowed to start new loads until it is renewed.\n", licenseName, expireDateStr);
				break;

			case Expired:
				message += String.format("Your %s expired on %s. You are not allowed to start new loads until it is renewed.\n", licenseName, expireDateStr);
				break;

			case Okay:
				// fall through
			default:
				return message;
		}
		return message;
	}

	private String getLicenseStatusMessage(User user) {
		Resources res = getResources();

		final String contactLocalMgmtMsg = res.getString(R.string.contact_local_management);
		final String contactMgmtOrSupMsg = res.getString(R.string.contact_local_mgmt_or_support);

		String message = "";
		message += getLicenseStatusMessage("Driver's License", user.driverLicenseExpiration, user.getDriversLicenseExpirationStatus());
		if (!message.isEmpty()) {
			message += "\n";
		}
		String medCertMsg = getLicenseStatusMessage("Medical Certificate", user.medicalCertificateExpiration, user.getMedicalCertificateExpirationStatus());
		if (!medCertMsg.isEmpty()) {
			message += medCertMsg + "\n";
		}

		int restrictionLevel = user.getLicenseExpirationRestrictionLevel();
		if (restrictionLevel != User.LICENSE_EXPIRE_OK) {
			if (restrictionLevel == User.LICENSE_EXPIRE_LOCK) {
				if(user.helpTerm != -1) {
					Terminal helpTerminal = DataManager.getTerminal(this, user.helpTerm);
					if (!HelperFuncs.isNullOrEmpty(helpTerminal.dispatchPhoneNumber)) {
						message += "Call dispatch at\n    " + helpTerminal.dispatchPhoneNumber;
					} else {
						message += contactMgmtOrSupMsg;
					}
				} else {
					message += contactMgmtOrSupMsg;
				}
			}
			else {
				message += contactLocalMgmtMsg;
			}
		}
		return message;
	}
}