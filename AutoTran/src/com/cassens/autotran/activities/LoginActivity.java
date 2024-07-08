package com.cassens.autotran.activities;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.EditText;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.URLS;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.TokenGenerator;
import com.cassens.autotran.data.remote.tasks.GetSupervisorUsersTask;
import com.cassens.autotran.dialogs.PiccoloAccessNeededDialog;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.cassens.autotran.hardware.PiccoloManager;
import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.sdgsystems.util.HelperFuncs;

import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class LoginActivity extends AutoTranActivity
{
	public static final String LAUNCH_DASHBOARD_EXTRA = "launchDashboard";

    private static final Logger log = LoggerFactory.getLogger(LoginActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

    // Time (in seconds) since last login after which user will be forced to re-login
	private static final long FORCE_RELOGIN_MAX_TIME_SINCE_LOGIN = 60 * 60 * 17; // 17 hrs

	EditText driverIDEditText;
	EditText truckIDEditText;
	String driverIDStr;

	boolean truckIdOverridden = false;

	String result = "";
	ProgressDialog dialog;
	Handler progressHandler;
	private boolean launchDashboardOnSuccess;

	/*
	private BroadcastReceiver piccoloTruckNumberReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(intent.getAction().equals(Constants.ACTION_PICCOLO_TRUCK_NUM_RECEIVED)) {
				String number = intent.getStringExtra(Constants.PICCOLO_TRUCK_ID_PREF);

				// BEGIN HACK
				String prevNumber = intent.getStringExtra(Constants.PICCOLO_PREV_TRUCK_ID_PREF);
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
				boolean numberChanged = TruckNumberHandler.isPiccoloTruckNumberChanged(context);

				String junkMsg = "JUNK_TRUCK_3: piccoloTruckNumberReceiver():";
				if (!number.equalsIgnoreCase(prevNumber) || numberChanged) {
					junkMsg += "Truck ID changed: " +  numberChanged + " prevNumber: " + prevNumber + " newNumber: " + number; // HACK
					SharedPreferences.Editor editor = prefs.edit();
					editor.putString(Constants.PICCOLO_PREV_TRUCK_ID_PREF, number);
					TruckNumberHandler.isPiccoloTruckNumberChanged(context);
					editor.commit();
				}
				else {
					junkMsg += "Login Activity Received: currentTruckId: " + number + " prevTruckId " + number;
				}
				CommonUtility.simpleMessageDialog(CommonUtility.getCurrentActivity(),  junkMsg);
				log.debug(Logs.DEBUG, junkMsg); // END HACK
				// END HACK

				if(number != null && !truckIdOverridden) {
					log.debug(Logs.DEBUG, "JUNK_TRUCK_4: piccoloTruckNumberReceiver(): setting Truck Number field to " + number);
					truckIDEditText.setText(number);
				}
			}
		}
	};
	 */

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_login);
	}

	// set driver ID onStart so Piccolo activity events don't reset it
	@Override
	protected void onStart() {
		super.onStart();
		PiccoloManager.refreshPiccoloTruckNumberAsNeeded(AutoTranApplication.getAppContext());
		TruckNumberHandler.setPiccoloTruckNumberReceived(this, true);
		setDriverID();
	}

	@Override
	protected void onResume() {
		super.onResume();

		//IntentFilter filter = new IntentFilter();
		//filter.addAction(Constants.ACTION_PICCOLO_TRUCK_NUM_RECEIVED);
		//registerReceiver(piccoloTruckNumberReceiver, filter);
	}

	@Override
	protected void onPause() {
		super.onPause();
		//unregisterReceiver(piccoloTruckNumberReceiver);
	}

	@Override
	public void onBackPressed()
	{
		//super.onBackPressed();
		//CommonUtility.simpleMessageDialog(this, "You must login.");
	}
	
	private void setDriverID() {
		launchDashboardOnSuccess = this.getIntent().getBooleanExtra(LAUNCH_DASHBOARD_EXTRA, false);
		driverIDEditText = (EditText) findViewById(R.id.driverIDEditText);
		truckIDEditText = (EditText) findViewById(R.id.truckIDEditText);

		String driverID = CommonUtility.getDriverNumber(this);

		String truckID;
		truckID = TruckNumberHandler.getTruckNumber(this);

		driverIDEditText.setText(driverID);
		driverIDEditText.setSelection(driverIDEditText.getText().length());

		truckIDEditText.setText(truckID);
		truckIDEditText.setEnabled(true);

		if (launchDashboardOnSuccess) {

			driverIDEditText.setSelection(driverIDEditText.getText().length());

			if(driverID == "000") {
				driverIDEditText.setText("103015");
				truckIDEditText.setText("0");
				driverIDEditText.setSelection(driverIDEditText.getText().length());
			}
			else {
				if(isValidInput() && driverID.trim().length() <= 6  && CommonUtility.isInteger(driverID) &&
							!isNewLoginNeeded(this)) {
					Log.d(TAG, "Logging in automatically");
					login(true);
				}
			}
		}
		
	}

	public void login(View v) {
		CommonUtility.logButtonClick(log, v);
		resetLoginTimer();

		if (PiccoloManager.isUsbPermissionIssueDetected()) {
			log.debug(Logs.PICCOLO_IO, "New login: Resetting PiccoloAccessNeededDialog reminders");
			PiccoloAccessNeededDialog.Companion.resetReminderTime();
		}
		login(false);
	}
	
	private void login(boolean automatic) {
		if(isValidInput()) {
			//Get the driver number

			String driverNumber;
			try {
				// Use parseInt to strip any leading zeros while also keeping a value of zero.
				driverNumber = String.valueOf(Integer.parseInt(driverIDEditText.getText().toString().trim()));
			} catch (NumberFormatException ex) {
				// This can't happen since the login screen doesn't allow non-integer driver IDs
				return;
			}

			// If login is automatic, use the last saved truck number.
			String truckNumber = automatic ? TruckNumberHandler.getTruckNumber(this) : truckIDEditText.getText().toString().trim();
			boolean useManualTruckNum = true;
			/*
			if (automatic) {
				useManualTruckNum = TruckNumberHandler.isTruckNumberSourceManual(this);
			}
			else {
				useManualTruckNum = true;
			} */


			if(driverNumber.trim().equals("12345")) {
				forwardToCommunicationTest();
			} else if(driverNumber.trim().length() > 6  || !CommonUtility.isInteger(driverNumber)) {
				CommonUtility.showText("Driver number " + driverNumber + " is invalid, please re-enter");
				return;
			} else if (!TruckNumberHandler.isValid(truckNumber)) {
				CommonUtility.showText("Truck number " + truckNumber + " is invalid, please re-enter");
				return;
			} else {

				//broadcast the current driver number back up to the id manager (assume that it has changed)
				log.debug(Logs.DEBUG, "broadcasting new id");

				//Check the database for driver number
				User driver = DataManager.getUserForDriverNumber(this, driverNumber);

				log.debug(Logs.INTERACTION, "Logging in user " + driverNumber);

				//If the driver number isn't in the database
				if (driver == null) {

					log.debug(Logs.DEBUG, "logging in from server");

					//Fetch the user information from the server
					loginFromServer(driverNumber, truckNumber, useManualTruckNum);

				}
				//If the driver number IS in the database
				else {
					setDriverAndTruckPrefs(driverNumber, driver.helpTerm, truckNumber, useManualTruckNum);

					CommonUtility.showText("Logging in with driver number " + driverNumber);

					//Forward the user on to the dashboard
					completeSuccessfulLogin(driver);
				}
			}
		}
		else {
			CommonUtility.showText("Please enter valid DriverID...");
		}
	}

	private void setDriverAndTruckPrefs(String driverNumber, int helpTerm, String truckNumber, boolean useManualTruckNum) {
		String driverNum = driverNumber.replace("\\u00A0", "");
		CommonUtility.setDriverNumAndTerminal(getApplicationContext(), driverNum, helpTerm);

		TruckNumberHandler.setTruckNumber(getApplicationContext(), truckNumber.replace("\\u00A0", ""));

		// Check validity of Truck Number?

		FirebaseCrashlytics crashlytics = FirebaseCrashlytics.getInstance();
		crashlytics.setUserId(driverNum);
		crashlytics.setCustomKey("TruckNumber", truckNumber);
	}

	private final String TAG = "login";
	
	private void loginFromServer(final String driverNumber, final String truckNumber, final boolean useManualTruckNum) {
		dialog = ProgressDialog.show(LoginActivity.this, "", "Please Wait...", true, true);
		dialog.setContentView(R.layout.customprogressdialog);

		Thread background = new Thread(new Runnable()
		{
			@Override
			public void run()
			{
				HttpClient client = new DefaultHttpClient();
				HttpConnectionParams.setConnectionTimeout(client.getParams(), 20000);
				HttpResponse response;
				int what = 0;

				log.debug(Logs.DEBUG, "logging in");
				
				try
				{
				  Long timestamp = System.currentTimeMillis() / 1000;
				  String token = new TokenGenerator().getToken(URLS.login, timestamp);
					
					HttpPost post = new HttpPost(URLS.login);

					List<NameValuePair> nameValuePairs = new ArrayList<NameValuePair>(2);
					nameValuePairs.add(new BasicNameValuePair("user_id", driverNumber));
					nameValuePairs.add(new BasicNameValuePair("token", token));
					nameValuePairs.add(new BasicNameValuePair("timestamp", timestamp + ""));

					System.out.println(LoginActivity.class.getName() +" NVPS ::"+nameValuePairs);

					post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

					response = client.execute(post);
					InputStream in = response.getEntity().getContent();
					result = CommonUtility.convertStreamToString(in);
					System.out.println(LoginActivity.class.getName() +" Result :: "+result);
					
					System.out.println(response.getStatusLine().getStatusCode());

					if (response.getStatusLine().getStatusCode() == 200)
					{
						what = 1;
					}
					else
					{
						what = 0;
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
				progressHandler.sendMessage(progressHandler.obtainMessage(what));
			}
		});
		background.start();

		progressHandler = new Handler()
		{
			@Override
			public void handleMessage(Message mesg)
			{
				dialog.dismiss();
				try
				{
					JSONObject jsonObject = new JSONObject(result);

					String success = "";
					if(jsonObject.has("success")) {  
					  jsonObject.getString("success");
					}
					String status = "";
					
					if(jsonObject.has("status")) {
					  status = jsonObject.getString("status");
					}

					String message = jsonObject.getString("message");
					
					if (success.equalsIgnoreCase("Failure") || status.equalsIgnoreCase("Failure"))
					{
						failedLoginPrompt(message, driverNumber.replace("\\u00A0", ""),
														truckNumber.replace("\\u00A0", ""));
						return;
					}
					else
					{
						JSONObject dataObject = jsonObject.getJSONObject("data");
					
						User driver = addLoginDetailsToDB(result);

						if (driver == null) {
							failedLoginPrompt("Encountered error when processing login information from server",
									driverNumber.replace("\\u00A0", ""),
									truckNumber.replace("\\u00A0", ""));
							return;
						}

						setDriverAndTruckPrefs(driverNumber, driver.helpTerm, truckNumber, useManualTruckNum);
						completeSuccessfulLogin(driver);
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
					CommonUtility.showText("Network connection error, Try again.");
				}
			}

			
		};
	}

	private void failedLoginPrompt(String message, final String newDriverID, final String newTruckID)
	{

		final SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this.getApplicationContext());

		AlertDialog.Builder builder = new AlertDialog.Builder(this);

		builder.setTitle("Login Failed");
		builder.setMessage(String.format("Unable to login Driver %s\n\nReason: %s", HelperFuncs.noNull(newDriverID),
				HelperFuncs.isNullOrEmpty(message) ? "Undetermined" : message));
		builder.setCancelable(false);
		builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				dialogInterface.dismiss();
			}
		});
		builder.show();

	}

	private void forwardToCommunicationTest() {
		Bundle bundle=new Bundle();
		Intent intent = new Intent(LoginActivity.this, CommunicationsActivity.class);
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
		startActivity(intent);

		//finish();
	}

	private void completeSuccessfulLogin(User driver) {
		if (launchDashboardOnSuccess) {
			//TODO I want to store these application level values in shared prefs rather than a bundle.  I don't like state objects when I don't have lifecycle control
			Bundle bundle = new Bundle();
			Intent intent = new Intent(LoginActivity.this, DashboardActivity.class);
			bundle.putString("id", String.valueOf(driver.user_id));
			bundle.putString("user_id", String.valueOf(driver.user_id));
			bundle.putString("driverNumber", driver.driverNumber);
			bundle.putString("user_type", driver.userType);
			intent.putExtras(bundle);

			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			Editor editor = prefs.edit();
			editor.putString("driverName", driver.firstName + " " + driver.lastName);
			long currentTime = System.currentTimeMillis() / 1000;
			editor.commit();

			if (CommonUtility.isConnected(this)) {
				if (DataManager.supervisorCount(this) == 0) {
					new GetSupervisorUsersTask(this).execute();
				}
			}

			CommonUtility.showText("Welcome " + driver.firstName + " " + driver.lastName);

			startActivity(intent);
		}
		finish();
	}
	
	private boolean isValidInput()
	{
		boolean isValidInputVar = false;

		driverIDStr = driverIDEditText.getText().toString();

		if (driverIDStr.length() > 0 && !driverIDStr.matches(".*[^\\d]+.*"))
		{
			isValidInputVar = true;
		}
		else
		{
			isValidInputVar = false;
		}

		return isValidInputVar;
	}
	
	private User addLoginDetailsToDB(String result)
	{
		try
		{
			JSONObject jsonObj = new JSONObject(result);
			JSONObject jsonObject = jsonObj.getJSONObject("data");
			JSONObject jsonObjectUser = jsonObject.getJSONObject("User");
			
			User newUser = new User();
			
			newUser.user_remote_id = jsonObjectUser.getString("id");
			newUser.firstName = jsonObjectUser.getString("first_name"); 
			newUser.lastName = jsonObjectUser.getString("last_name");
			newUser.email = jsonObjectUser.getString("email");
			newUser.driverNumber = jsonObjectUser.getString("user_id");
			newUser.deviceToken = jsonObjectUser.getString("device_token");
			newUser.deviceID = jsonObjectUser.getString("device_id");
			newUser.password = jsonObjectUser.getString("password");
			newUser.role = jsonObjectUser.getString("role");
			newUser.userType = jsonObjectUser.getString("user_type");
			newUser.activationLink = jsonObjectUser.getString("activation_link");
			newUser.status = jsonObjectUser.getString("status");
			newUser.created = jsonObjectUser.getString("created");
			newUser.modified = jsonObjectUser.getString("modified");
			newUser.fullName = jsonObjectUser.getString("full_name");
			newUser.highClaims = jsonObjectUser.optInt("highClaims", 0);
			newUser.requiresAudit = jsonObjectUser.optInt("requiresAudit", 0);
			newUser.inspectionAccess = jsonObjectUser.optInt("inspectionAccess", 0);
			newUser.supervisorCardCode = jsonObjectUser.getString("supervisorCardCode");
			newUser.supervisorPreloadChk = jsonObjectUser.optInt("supervisorPreloadChk", 0);
			newUser.helpTerm = jsonObjectUser.optInt("helpTerm", -1);
			newUser.driverLicenseExpiration =  HelperFuncs.simpleDateStringToDate(jsonObjectUser.getString("driver_license_expiration"));
			newUser.medicalCertificateExpiration = HelperFuncs.simpleDateStringToDate(jsonObjectUser.getString("medical_certificate_expiration"));
			newUser.autoInspectLastDelivery = jsonObjectUser.getBoolean("autoInspectLastDelivery");


			DataManager.insertUserToLocalDB(this, newUser);

			return DataManager.getUserForDriverNumber(this, newUser.driverNumber);
		
		}
		catch (Exception e) {
			log.debug(Logs.DEBUG, "addLoginDetailsToDb() caught exception: " + e.getClass().getName());
			e.printStackTrace();
		}
		
		return null;
		
	}

	private void resetLoginTimer() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor editor = prefs.edit();
		editor.putLong("lastLoginTime", System.currentTimeMillis()/1000).commit();
		editor.commit();
	}

	public static boolean isNewLoginNeeded(Context context) {
		long currentTime = System.currentTimeMillis() / 1000;
		long secondsSinceLastLogin = currentTime -
				PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext()).getLong("lastLoginTime", 0);
		boolean needed = (secondsSinceLastLogin >= FORCE_RELOGIN_MAX_TIME_SINCE_LOGIN);
		return needed;
	}
}
