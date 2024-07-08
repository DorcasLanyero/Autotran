package com.cassens.autotran.activities;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;

import androidx.core.app.ActivityCompat;

import com.cassens.autotran.AutoTranApplication;

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.cassens.autotran.constants.Constants.PREF_DRIVING_LOCK_OVERRIDDEN;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.util.ContextInitializer;

public class SplashActivity extends AutoTranActivity {
	private static final Logger log = LoggerFactory.getLogger(SplashActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	private static final int MY_PERMISSIONS_REQUESTS = 1;
    private boolean mPermissionRequestInFlight = false;

    @Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		AutoTranApplication.initAutoTran();

		setContentView(R.layout.activity_splash);

		HelperFuncs.setBoolPref(this, PREF_DRIVING_LOCK_OVERRIDDEN, false);
	}

	@Override
	protected void onResume() {
		super.onResume();

		//ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);

		if (hasNeededPermissions()) {
			startSplashThread();
		} else {
			getAllPermissions();
		}
	}

	private void getAllPermissions() {
	    if(!mPermissionRequestInFlight) {
            mPermissionRequestInFlight = true;
            ActivityCompat.requestPermissions(this,
                    new String[]{
                            Manifest.permission.CAMERA,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.READ_PHONE_STATE
                    },
                    MY_PERMISSIONS_REQUESTS);
        }
	}

	private void startSplashThread() {
		//Thread that manages splash screen
		Thread splashThread = new Thread() {
			@Override
			public void run() {
				try {
					sleep(1000);
					SplashActivity.this.finish();

					Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
					intent.putExtra(LoginActivity.LAUNCH_DASHBOARD_EXTRA, true);
					startActivity(intent);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		};

		splashThread.start();

	}

	@Override
	public void onRequestPermissionsResult(int requestCode,
										   String permissions[], int[] grantResults) {
		switch (requestCode) {
			case MY_PERMISSIONS_REQUESTS: {
				// If request is cancelled, the result arrays are empty.
				if (grantResults.length > 0) {

					boolean allGranted = true;

					for (int permission : grantResults) {
						if (permission != PackageManager.PERMISSION_GRANTED) {
							allGranted = false;
						}
					}

					if (!allGranted) {
						//getAllPermissions();
						askUserToSetPermissions();
						return;
					} else {
						initializeLog4jConfig();
						startSplashThread();
					}
				}
				return;
			}
		}
	}

	private void initializeLog4jConfig() {
		// This is necessary because log4j gets initialized in the app startup sequence prior
		// to display of the splash screen. This creates a problem the first time the app is
		// started after a new installation: Log4j requires the user to grant the app
		// permission to write to "external" storage. On initial startup this permission will
		// not have been given permission, causing the initialization attempt to fail, meaning
		// that all log writes would fail until the app is re-started. The code below
		// re-initialized log4j so that writes will work.
		LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
		loggerContext.reset();
		ContextInitializer ci = new ContextInitializer(loggerContext);
		try {
			ci.autoConfig();
			log.debug(Logs.INTERACTION, "Reinitialized log4j after storage permissions granted");
		}
		catch (Exception ex) {
			Log.w("SplashActivity", "Warning: Unable to initialize logging on app init. This will probably resolve itself after the next app restart");
		}
	}

	private void askUserToSetPermissions() {
		log.debug(Logs.INTERACTION, "Showing dialog to ask user to grant required app permissions");
		AlertDialog.Builder builder = new AlertDialog.Builder(SplashActivity.this);
		builder.setMessage(buildPermissionMessage());
		builder.setPositiveButton("Set Permissions", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				log.debug(Logs.INTERACTION, "Launching settings app for user to grant permissions");
				startActivityForResult(new Intent(Settings.ACTION_APPLICATION_SETTINGS), 0);
			}
		});
		builder.setNeutralButton("Exit App", new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialogInterface, int i) {
				log.debug(Logs.INTERACTION, "User exited app instead of granting permissions");
				finish();
			}
		});
		builder.setCancelable(false);
		builder.create().show();
	}

	private boolean hasNeededPermissions() {
		return this.checkCallingOrSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
				this.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED &&
				this.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
				this.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED &&
				this.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) == PackageManager.PERMISSION_GRANTED;
	}

	private String buildPermissionMessage() {
    	String permissionsNeeded = "";

		if (this.checkCallingOrSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded += "    Camera\n";
		}
		if (this.checkCallingOrSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
				this.checkCallingOrSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded += "    Location\n";
		}
		if (this.checkCallingOrSelfPermission(Manifest.permission.READ_PHONE_STATE) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded += "    Phone\n";
		}
		if (this.checkCallingOrSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
			permissionsNeeded += "    Storage\n";
		}
		return String.format("%s\n\n%s\n%s\n\n%s", getResources().getString(R.string.missingAppPermissionsMsgPart1),
				permissionsNeeded, getResources().getString(R.string.missingAppPermissionsMsgPart2),
				getResources().getString(R.string.call_support));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		mPermissionRequestInFlight = false;
	}
}
