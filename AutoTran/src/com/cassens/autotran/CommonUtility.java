package com.cassens.autotran;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.media.MediaScannerConnection;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.os.StatFs;
import android.preference.PreferenceManager;
import android.text.SpannableStringBuilder;
import android.text.util.Linkify;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.exifinterface.media.ExifInterface;

import com.cassens.autotran.activities.AutoTranActivity;
import com.cassens.autotran.activities.IPayNumberDialogCallback;
import com.cassens.autotran.activities.LoginActivity;
import com.cassens.autotran.activities.SplashActivity;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.constants.URLS;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.EventBusManager;
import com.cassens.autotran.data.event.DriverActionEvent;
import com.cassens.autotran.data.event.S3UploadEvent;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.User;
import com.sdgsystems.android.amazon.s3transfer.S3BucketAuth;
import com.sdgsystems.android.amazon.s3transfer.models.S3Container;
import com.sdgsystems.android.amazon.s3transfer.network.TransferController;
import com.sdgsystems.app_config.AppConfig;
import com.sdgsystems.util.HelperFuncs;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import me.drakeet.support.toast.BadTokenListener;
import me.drakeet.support.toast.ToastCompat;

public class CommonUtility {
    private static final Logger log = LoggerFactory.getLogger(CommonUtility.class.getSimpleName());

    public static final String pref_question_answered = "pref_question_answered";
    public static final String pref_open_load_number = "pref_open_load_number";

    private static boolean questionnaireShowing = false;
    private static boolean questionnaireConfirmed = false;

	private static final long MILLIS_FOR_QUESTIONNAIRE = 1000 * 60 * 60 * 18;

	public static void printStatement(String TAG, String message) {
		log.error(Logs.DEBUG, message);
	}

	public static boolean isEmailValid(String email) {
		boolean isValid = false;

		if (email.length() > 0) {
			String expression = "^[\\w\\.-]+@([\\w\\-]+\\.)+[A-Z]{2,4}$";
			CharSequence inputStr = email;
			Pattern pattern = Pattern.compile(expression,Pattern.CASE_INSENSITIVE);
			Matcher matcher = pattern.matcher(inputStr);
			if (matcher.matches()) {
				isValid = true;
			}
		}

		return isValid;
	}


	public static boolean hasPanasonicScanner() {
		if(android.os.Build.MANUFACTURER.toLowerCase().contains("panasonic")) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean hasHoneywellScanner() {
		if(android.os.Build.MANUFACTURER.toLowerCase().contains("honeywell")) {
			log.debug(Logs.DEBUG, "honeywell scanner detected");
			return true;
		} else {
			log.debug(Logs.DEBUG, "honeywell scanner NOT detected");
			return false;
		}
	}

	//https://stackoverflow.com/questions/15055458/detect-7-inch-and-10-inch-tablet-programmatically
	public static boolean isTablet(Activity activity) {
		boolean isTablet = false;
		DisplayMetrics metrics = new DisplayMetrics();
		activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

		int widthPixels = metrics.widthPixels;
		int heightPixels = metrics.heightPixels;

		float scaleFactor = metrics.density;

		float widthDp = widthPixels / scaleFactor;
		float heightDp = heightPixels / scaleFactor;

		/*
			320dp: a typical phone screen (240x320 ldpi, 320x480 mdpi, 480x800 hdpi, etc).
			480dp: a tweener tablet like the Streak (480x800 mdpi).
			600dp: a 7” tablet (600x1024 mdpi).
			720dp: a 10” tablet (720x1280 mdpi, 800x1280 mdpi, etc).
		 */

		/*
		//dp calculation (but this might change based on variable screen densities...)
		float smallestWidth = Math.min(widthDp, heightDp);

		if (smallestWidth > 720) {
			//Device is a 10" tablet
			return true;
		}
		else if (smallestWidth > 600) {
			//Device is a 7" tablet
			return true;
		}
		*/

		float widthDpi = metrics.xdpi;
		float heightDpi = metrics.ydpi;
		float widthInches = widthPixels / widthDpi;
		float heightInches = heightPixels / heightDpi;

		//The size of the diagonal in inches is equal to the square root of the height in inches squared plus the width in inches squared.
		double diagonalInches = Math.sqrt(
			(widthInches * widthInches)
			+ (heightInches * heightInches));

		if (diagonalInches >= 10) {
			//Device is a 10" tablet
			return true;
		}
		else if (diagonalInches >= 7) {
			//Device is a 7" tablet
			return true;
		} else if(diagonalInches >= 5) {
			//Device is a 5" >= tablet
			return true;
		}

		return isTablet;
	}

    public static boolean connectedToCtcWifi() {
        Context context = AutoTranApplication.getAppContext();
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(context.WIFI_SERVICE);
        WifiInfo wifiInfo = wifiManager.getConnectionInfo();

        //lowercase and remove the quotes
        String ssid = wifiInfo.getSSID().toLowerCase().replace("\"", "");
        //log.debug(Logs.DEBUG, "SSID: " + ssid);
        return ssid.equalsIgnoreCase("friedberg") || ssid.equalsIgnoreCase("ctc") || ssid.equalsIgnoreCase("sdg-wlan") || ssid.equalsIgnoreCase("knights-net-att");
    }

	public static boolean checkVin(Activity activity, String vin)	{
        if (!validVin(vin, true)) {
        	String msg;
        	if (CommonUtility.isNullOrBlank(vin)) {
				msg = "A valid VIN is required!";
			}
        	else {
				msg = "'" + vin + "' is not a valid VIN!";
			}
        	if (activity.hasWindowFocus()) {
				showText(msg);
			}
        	else if (!activity.isFinishing() && !activity.isDestroyed()){
				CommonUtility.simpleMessageDialog(activity, msg);
			}
            return false;
        }
        return true;
	}

	public static boolean checkVinNoPopup(String vin) {
		if (!validVin(vin, true)) {
			return false;
		}
		return true;
	}

	public static boolean checkVinNoPopupNoLogging(String vin) {
		if (!validVin(vin, false)) {
			return false;
		}
		return true;
	}

	private static AutoTranActivity mCurrentActivity;

	public static void setCurrentActivity(AutoTranActivity autoTranActivity) {
		mCurrentActivity = autoTranActivity;

		// To enable AUTOTRAN_SHOW_ACTIVITY_TOAST, add the following line to your
		// local.properties file and re-build:
		//
		//      autotran.showActivityToast=true
		//
		// This is handy for debugging to quickly determine which activity is controlling the
		// screen
		if (BuildConfig.AUTOTRAN_SHOW_ACTIVITY_TOAST) {
			CommonUtility.showText("Current activity: " + mCurrentActivity.getClass().getSimpleName());
		}
	}

	public static String getOpenLoadNumber(Context context) {
		return PreferenceManager.getDefaultSharedPreferences(context).getString(pref_open_load_number, "");
	}

	public static void setOpenLoadNumber(Context context, String loadNumber) {
		String currentLoadNumber = getOpenLoadNumber(context);
		if (HelperFuncs.isNullOrEmpty(currentLoadNumber) && !HelperFuncs.isNullOrEmpty(loadNumber)) {
			currentLoadNumber = loadNumber;
		} else if (!HelperFuncs.isNullOrEmpty(loadNumber)) {
			//multiple loadnumbers
			currentLoadNumber += ";" + loadNumber;
		}

		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putString(pref_open_load_number, currentLoadNumber).commit();
	}

	public static void removeOpenLoadNumber(Context context, String loadNumber) {
		//right now assuming only one or two loads possible
		String currentLoadNumber = getOpenLoadNumber(context);

		if (currentLoadNumber.contains(loadNumber + ";")) {
			currentLoadNumber.replace(loadNumber + ";", "");
		} else if (currentLoadNumber.contains(";" + loadNumber)) {
			currentLoadNumber.replace(";" + loadNumber, "");
		} else {
			currentLoadNumber.replace(loadNumber, "");
		}

		PreferenceManager.getDefaultSharedPreferences(context).edit()
				.putString(pref_open_load_number, currentLoadNumber).commit();
	}

	public static boolean isUserLoggedIn() {
		return !(mCurrentActivity == null || mCurrentActivity instanceof LoginActivity || mCurrentActivity instanceof SplashActivity);
	}

	public static Activity getCurrentActivity() {
		return mCurrentActivity;
	}

	//Utility method to process scanned VINs in case they are not ONLY a 17-digit VIN.
	public static String processScannedVIN(String stringExtra) {
		String processedVIN;

		if (stringExtra == null || stringExtra.length() == 0) {
			log.debug(Logs.DEBUG, "processScannedVIN() got null or empty scan value");
			return "";
		}

		if (stringExtra.length() >= 18 && stringExtra.charAt(2) == stringExtra.charAt(3) &&
				stringExtra.charAt(2) == stringExtra.charAt(4) &&
				stringExtra.charAt(2) == stringExtra.charAt(5) &&
				stringExtra.charAt(2) == stringExtra.charAt(6) &&
				stringExtra.charAt(2) == stringExtra.charAt(7)
				) {
			//This is a special case of Derek's testing VIN format
			processedVIN = stringExtra;
		} else if (stringExtra.length() == 18 && stringExtra.charAt(17) == '.') {
			// Some Hyundai VIN barcodes have a period character appended
			return(stringExtra.substring(0, 17));
		} else if (Character.isLetter(stringExtra.charAt(stringExtra.length() - 1)) && BuildConfig.AUTOTRAN_VALIDATE_VINS) {
			processedVIN = stringExtra.substring(0, stringExtra.length() - 1);
		} else {
			processedVIN = stringExtra;
		}

		//2d barcode handling
		if (stringExtra.length() > 17) {
			//Lake Orion QR Codes
			if(stringExtra.contains(",")) {
				String[] values = stringExtra.split(",");
				processedVIN = values[0];

			} else {
				//Some 2d barcodes Prepend data to the VIN.  If the barcode is longer than 17 characters, only grab the LAST 17
				processedVIN = stringExtra.substring(stringExtra.length() - 17);
			}
		}

		return processedVIN;
	}

	public static void showText(String message, int duration) {
		if(message != null && message.trim().length() > 0) {

			//In order to use a snackbar, we need to use an appCompat theme which would take a LOT of redoing in terms of graphics
			//Snackbar.make(mCurrentActivity.findViewById(R.id.parentLayout), message, Snackbar.LENGTH_SHORT).show();

			log.debug(Logs.INTERACTION, "Toast: " + message);
			log.debug(Logs.DEBUG, "Showing toast: " + message);

			if (Build.VERSION.SDK_INT < 26) {
				// The BadToken crash was addressed in Android 8, so we can delete this
				// along with the ToastCompat library whenever we no longer need support
				// for Android 7.1.
				ToastCompat toast = ToastCompat.makeText(AutoTranApplication.getAppContext(), message, duration);
				toast.setGravity(Gravity.TOP, 0, 200);
				toast.setBadTokenListener(new BadTokenListener() {
					@Override
					public void onBadTokenCaught(@NonNull Toast toast) {
						log.debug(Logs.DEBUG, "CAUGHT THE BAD TOKEN EXCEPTION!!!");
					}
				});
				toast.show();
			}
			else {
				Toast toast = Toast.makeText(AutoTranApplication.getAppContext(), message, duration);
				toast.setGravity(Gravity.TOP, 0, 200);
				toast.show();
			}
		} else {
			log.debug(Logs.INTERACTION, "NOT showing toast message because it was null or empty");
		}
	}


	public static void showText(String message)	{
		showText(message, Toast.LENGTH_SHORT);
	}

	public static boolean isConnected(Context context) {
		ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting();

		return isConnected;
	}

	private static void outputNetworkInfo(Context context) {
		ConnectivityManager cm =
				(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);

		NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
		boolean isConnected = activeNetwork != null &&
				activeNetwork.isConnectedOrConnecting();

		String message = "Network state: ";

		if(isConnected) {
			message += "Connected: ";
			message += CommonUtility.getIPAddress(true) + "\n";
		} else {
			message += "Not Connected\n";
		}

		if(activeNetwork != null)
			message += activeNetwork;

		log.debug(Logs.DEBUG, message);
		Log.d("ipstate", message);
	}

	public static HttpResponse tryURL(Context ctx, HttpClient client, HttpPost post) {
		outputNetworkInfo(ctx);

		HttpResponse response = null;
		try {
			response = client.execute(post);
		} catch (UnknownHostException uhe) {
			//If the hostname wasn't found, try the ip address
			try {
				String updatedHostURL = post.getURI().toString().replace(URLS.HOST_URL_CONSTANT, URLS.IP_OVERRIDE);
				post.setURI(new URI(updatedHostURL));

				log.debug(Logs.DEBUG, "hostname wasn't found, attempting IP address: " + updatedHostURL);

				response = client.execute(post);
			} catch (URISyntaxException e) {
				e.printStackTrace();
			} catch (Exception e) {
				e.printStackTrace();
			}
		} catch (Exception e) {
			e.printStackTrace();
		}

		return response;
	}

	//Method to convert response stream into a string object
	public static String convertStreamToString(InputStream is) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(is));
		StringBuilder sb = new StringBuilder();
		String line = null;
		try {
			while ((line = reader.readLine()) != null)
			{
				sb.append(line + "\n");
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				is.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return sb.toString();
	}

	public static String convertToTwelveHourTime(String time){
		try{
				SimpleDateFormat _24HourSDF = new SimpleDateFormat("H:mm");
				SimpleDateFormat _12HourSDF = new SimpleDateFormat("h:mma");
				Date _24HourDt = _24HourSDF.parse(time);
				return _12HourSDF.format(_24HourDt);
			} catch (Exception e) {
				e.printStackTrace();
			}
		return "";
	}

	//Method to create alert dialog with a message
	public static void simpleMessageDialog(Activity activity, String msg) {
		simpleMessageDialog(activity, msg, null, false);
	}

	//Method to create alert dialog with a message
	public static AlertDialog simpleMessageDialog(Activity activity, String msg, String title) {
		return simpleMessageDialog(activity, msg, title, false);
	}

	//Method to create alert dialog with a message
	public static AlertDialog simpleMessageDialog(Activity activity, String msg, String title, boolean fixedWidth) {
		log.debug(Logs.INTERACTION, "Message dialog: " + msg);
		Builder builder = new AlertDialog.Builder(activity);
		builder.setTitle((title == null) ? "" : title);
		if (fixedWidth) {
			TextView tv = new TextView(activity);
			tv.setTypeface(Typeface.MONOSPACE);
			tv.setText(msg);
			builder.setView(tv);
		}
		else {
			builder.setMessage(msg);
		}
		builder.setPositiveButton("Ok", null);
		builder.setCancelable(false);
		try {
			AlertDialog dialog = builder.create();
			dialog.show();
			return dialog;
		} catch (Exception e) {
			log.debug(Logs.DEBUG, "simpleMessageDialog() call from " + activity.getClass().getSimpleName() + " got exception: " + e.getClass().getName());
			e.printStackTrace();
		}
		return null;
	}

	public static boolean isNullOrBlank(String s)
		{
		    return (s == null || s.trim().equals(""));
		}

	public static String getLogFileSize() {
		String size = "";

		File Root = Environment.getExternalStorageDirectory();
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if(filename.toLowerCase().contains("autotran"))
					return true;
				else
					return false;
			}
		};

		File[] files = Root.listFiles(filter);

		long totalSize = 0;

		for(File file: files) {
			if(file.exists() && file.isFile())
				totalSize += file.length();
		}

		return String.valueOf(totalSize / 1000);
	}

	/*************************************************************************************************
	Returns size in bytes.

	If you need calculate external memory, change this:
		StatFs statFs = new StatFs(Environment.getRootDirectory().getAbsolutePath());
	to this:
		StatFs statFs = new StatFs(Environment.getExternalStorageDirectory().getAbsolutePath());

	 TODO: Convert these functions to use the getTotalBytes() and getAvailableBytes() StatFS
	       methods instead of the block-related methods, which are now deprecated. - PDK
	**************************************************************************************************/
	public static long TotalMemory(boolean internal) {
		StatFs statFs = new StatFs(internal ? Environment.getRootDirectory().getAbsolutePath() : Environment.getExternalStorageDirectory().getAbsolutePath());
		long   Total  = ( (long) statFs.getBlockCount() * (long) statFs.getBlockSize());
		return Total;
	}

	public static long FreeMemory(boolean internal) {
		StatFs statFs = new StatFs(internal ? Environment.getRootDirectory().getAbsolutePath() : Environment.getExternalStorageDirectory().getAbsolutePath());
		long   Free   = (statFs.getAvailableBlocks() * (long) statFs.getBlockSize());
		return Free;
	}

	public static long BusyMemory(boolean internal) {
		StatFs statFs = new StatFs(internal ? Environment.getRootDirectory().getAbsolutePath() : Environment.getExternalStorageDirectory().getAbsolutePath());
		long   Total  = ((long) statFs.getBlockCount() * (long) statFs.getBlockSize());
		long   Free   = (statFs.getAvailableBlocks()   * (long) statFs.getBlockSize());
		long   Busy   = Total - Free;
		return Busy;
	}

	public static String floatForm (double d)
		{
		   return new DecimalFormat("#.##").format(d);
		}

	public static String bytesToHuman (long size) {
		long Kb = 1  * 1024;
		long Mb = Kb * 1024;
		long Gb = Mb * 1024;
		long Tb = Gb * 1024;
		long Pb = Tb * 1024;
		long Eb = Pb * 1024;

		if (size <  Kb)                 return floatForm(        size     ) + " byte";
		if (size >= Kb && size < Mb)    return floatForm((double)size / Kb) + " Kb";
		if (size >= Mb && size < Gb)    return floatForm((double)size / Mb) + " Mb";
		if (size >= Gb && size < Tb)    return floatForm((double)size / Gb) + " Gb";
		if (size >= Tb && size < Pb)    return floatForm((double)size / Tb) + " Tb";
		if (size >= Pb && size < Eb)    return floatForm((double)size / Pb) + " Pb";
		if (size >= Eb)                 return floatForm((double)size / Eb) + " Eb";

		return "???";
	}

	public static float getPercentageMemoryFree() {
		long freeMem = CommonUtility.FreeMemory(false);

		long totalMem = CommonUtility.TotalMemory(false);

		return 100 * ((float)freeMem / (float)totalMem);
	}

	/*
	 * Function to write logs to a file
	 * http://stackoverflow.com/questions/5726519/creating-and-storing-log-file-on-device-in-android
	 */
	public static void logMessage_deprecated(Context context, String filename, String message)  {

		String loggingName = "";

		StackTraceElement[] stack = Thread.currentThread().getStackTrace();

		//Stack trace element (3) is the method that CALLED this method.  hinky, but it gets things happening automagically...
		loggingName = stack[3].getClassName().replace("com.cassens.autotran.", "" ) + "." + stack[3].getMethodName() + ":" + stack[3].getLineNumber();

		//log.debug(Logs.DEBUG, "Logging to " + filename);
		File Root = Environment.getExternalStorageDirectory();
		try {
			if(Root.canWrite()){
				 File  LogFile = new File(Root, filename);

				 //boolean newFile = LogFile.exists();

				 FileWriter LogWriter = new FileWriter(LogFile, true);
				 BufferedWriter out = new BufferedWriter(LogWriter);
				 Date date = new Date();

				 //formatting date in Java using SimpleDateFormat
				 SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("MM-dd-yyyy HH:mm:ss");
				 String dateString = DATE_FORMAT.format(date);
				 String versionName = "";
				 try {
					versionName = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
				} catch (NameNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}

				 String finalMessage = dateString + ": " + versionName + " - " + loggingName + " " + message + "\n";
				 log.debug(Logs.DEBUG, finalMessage);
				 out.write(String.valueOf(finalMessage));
				 out.close();

				 if(LogFile.length() > 3000000) {
					 log.debug(Logs.DEBUG, "need to truncate " + filename);

				 }

				 MediaScannerConnection.scanFile(context.getApplicationContext(), new String[] { LogFile.getPath() }, null, null);

			} else {
				log.debug(Logs.DEBUG, "Can't write to logfile " + filename);
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void clearLogs() {
		File Root = Environment.getExternalStorageDirectory();
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if(filename.toLowerCase().contains("autotran"))
					return true;
				else
					return false;
			}
		};

		File[] files = Root.listFiles(filter);

		for(File file: files) {
			if(file.exists() && file.isFile())
				file.delete();
		}
	}

	public static int getImageScaleFactor(int photoW, int photoH) {

	    	int scale = 150;

	    	if(hasHoneywellScanner()) {
	    		scale = 210;
			}

		return Math.min(photoW / scale, photoH / scale);
    }

	public static Bitmap getBitmapThumbnail(Context context, String newImageFilePath) {
		BitmapFactory.Options bmOptions = new BitmapFactory.Options();
		bmOptions.inJustDecodeBounds = true;
		BitmapFactory.decodeFile(newImageFilePath, bmOptions);
		int photoW = bmOptions.outWidth;
		int photoH = bmOptions.outHeight;
		// Determine how much to scale down the image
		int scaleFactor = CommonUtility.getImageScaleFactor(photoW, photoH);


		// Decode the image file into a Bitmap sized to fill the View
		bmOptions.inJustDecodeBounds = false;
		bmOptions.inSampleSize = scaleFactor;
		bmOptions.inPurgeable = true;

		Bitmap bitmap = BitmapFactory.decodeFile(newImageFilePath, bmOptions);

		//If this is a honeywell device
		if(CommonUtility.hasHoneywellScanner()) {
			//We need to resolve the image rotation
			int rotation = CommonUtility.getImageRotation(context, newImageFilePath);
			bitmap = CommonUtility.rotateImage(bitmap, rotation);
		}
		return bitmap;
	}

	public static void PromptForPayNumber(final Activity activity, final IPayNumberDialogCallback callback, String message, String title) {
		PromptForPayNumber(activity, callback, message, title, null, false, null, 0);
	}

    public static void PromptForPayNumber(final Activity activity, final IPayNumberDialogCallback callback, String message, String title, String prompt, boolean showCancel,
										  String doneButtonLabel, long timeoutSeconds) {
		LayoutInflater inflater = activity.getLayoutInflater();
		final View dialogView = inflater.inflate(R.layout.dialog_tips, null);
		final Context ctx = activity;

		Builder builder = new Builder(activity);
		builder.setView(dialogView)
				.setCancelable(false);

		TextView tip = (TextView) dialogView.findViewById(R.id.tips_container);
		final CheckBox confirmation = (CheckBox) dialogView.findViewById(R.id.tips_confirmation);
		confirmation.setVisibility(View.GONE);
		final EditText driverId = (EditText) dialogView.findViewById(R.id.tips_textbox);
		if (prompt != null) {
			((TextView) dialogView.findViewById(R.id.tips_textbox_prompt)).setText(prompt);
		}
		final Button doneButton = (Button) dialogView.findViewById(R.id.done_button);
		if (doneButtonLabel != null) {
			doneButton.setText(doneButtonLabel);
		}
		final Button cancelButton = (Button) dialogView.findViewById(R.id.cancel_button);
		if(showCancel) {
			cancelButton.setVisibility(View.VISIBLE);
		}

		ImageView signatureReview = (ImageView)dialogView.findViewById(R.id.signature_review);
		signatureReview.setVisibility(View.GONE);

		tip.setText(message);
		builder.setTitle(title);

		final Dialog dialog = builder.create();

		doneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CommonUtility.logButtonClick(log, v, "PromptForPayNumber dialog: pay number: " + driverId.getText().toString().trim());
				if (!driverId.getText().toString().trim().equals(getDriverNumber(activity.getApplicationContext()))) {
					((LinearLayout) dialogView.findViewById(R.id.tips_textbox_border)).setBackgroundColor(Color.RED);
					log.debug(Logs.INTERACTION, "Message shown: " + R.string.error_wrong_driver_number);
					CommonUtility.showText(ctx.getString(R.string.error_wrong_driver_number) + ", Entered '" + driverId.getText() + "'");
				}  else {
					callback.complete();
					dialog.dismiss();
				}
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CommonUtility.logButtonClick(log, v, "PromptForPayNumber dialog");
				callback.cancelled();
				dialog.dismiss();
			}
		});

		driverId.setOnEditorActionListener(new TextView.OnEditorActionListener() {
			@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH_MR1)
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				if (event != null) {
					doneButton.callOnClick();
					return true;
				} else {
					return false;
				}
			}
		});

		dialog.show();
		dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);

		if (timeoutSeconds > 0) {
			// Hide after timeoutSeconds elapses
			final Handler handler = new Handler();
			final Runnable runnable = new Runnable() {
				@Override
				public void run() {
					if (dialog.isShowing()) {
						dialog.dismiss();
					}
				}
			};

			dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
				@Override
				public void onDismiss(DialogInterface dialog) {
					handler.removeCallbacks(runnable);
				}
			});

			handler.postDelayed(runnable, timeoutSeconds * 1000);
		}

    }

    public static void promptForCallbackNumber(final Activity activity, final IPayNumberDialogCallback callback,
											   String preamble, String damages, String message, String title, final String fourLetterCode) {
		LayoutInflater inflater = activity.getLayoutInflater();
		final View dialogView = inflater.inflate(R.layout.dialog_callback_number, null);
		final Context ctx = activity;

		Builder builder = new Builder(activity);
		builder.setView(dialogView)
				.setCancelable(false);

		final boolean regularBusinessHours = HelperFuncs.regularBusinessHours();

		final EditText readbackCode = (EditText) dialogView.findViewById(R.id.callbackText);
		final TextView codeView = (TextView) dialogView.findViewById(R.id.codeView);
		final TextView messageView = (TextView) dialogView.findViewById(R.id.messageView);
		final TextView damageView = (TextView)dialogView.findViewById(R.id.damageView);
		final TextView preambleView = (TextView) dialogView.findViewById(R.id.preambleView);
		final Button doneButton = (Button) dialogView.findViewById(R.id.done_button);
		final Button cancelButton = (Button) dialogView.findViewById(R.id.cancel_button);

		preambleView.setText(preamble);
		damageView.setText(damages);
		messageView.setText(message);
		codeView.setText(fourLetterCode);

		//if (!regularBusinessHours) {
			//don't show rest of message/code/code entry if there's no one to call
		if (true) { //initially don't require code yet per Matthew
			readbackCode.setVisibility(View.GONE);
			codeView.setVisibility(View.GONE);
			messageView.setVisibility(View.GONE);
		}

		final Dialog dialog;
		builder.setTitle(title);

		dialog = builder.create();

		doneButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				callback.complete();
				dialog.dismiss();
				CommonUtility.logButtonClick(log, v, "PromptForCallbackNumber dialog");

				/*
				//We're not testing for this code, currently, due to availability issues
				if (regularBusinessHours) {
					CommonUtility.logButtonClick(log, v, "readback code: " + readbackCode.getText().toString().trim() + " for input code: " + fourLetterCode);
					if (!readbackCode.getText().toString().trim().equals(HelperFuncs.getEdwardsvilleReadback(fourLetterCode))) {
						dialogView.findViewById(R.id.errorView).setVisibility(View.VISIBLE);
					} else {
						callback.complete();
						dialog.dismiss();
					}
				} else {
					CommonUtility.logButtonClick(v, "outside of regular delivery hours");
					callback.complete();
					dialog.dismiss();
				}
				*/
			}
		});

		cancelButton.setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View v) {
				CommonUtility.logButtonClick(log, v, "PromptForCallbackNumber dialog");
				callback.cancelled();
				dialog.dismiss();
			}
		});
        dialog.show();
        //dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
	}

	public static String getDriverNumber(Context context) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
		return prefs.getString(Constants.DRIVER_ID_PREF, "").replace("\\u00A0", "");
	}

	public static String getMostRecentLoginDriverNumber(Context context) {
		String mostRecentDriver = getDriverNumber(context);
		if (HelperFuncs.isNullOrWhitespace(mostRecentDriver)) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
			mostRecentDriver = prefs.getString(Constants.DRIVER_ID_PREV_LOGIN_PREF, "");
		}
		return mostRecentDriver;
	}

	public static int getMostRecentLoginDriverHelpTerm(Context context) {
		int mostRecentDriverHelpTerm = getDriverHelpTerm();
		if (mostRecentDriverHelpTerm < 0) {
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context.getApplicationContext());
			mostRecentDriverHelpTerm = prefs.getInt(Constants.DRIVER_ID_PREV_HELP_TERM_PREF, -1);
		}
		return mostRecentDriverHelpTerm;
	}

	public static int getDriverNumberAsInt(Context context) {
		try {
			return Integer.parseInt(getDriverNumber(context));
		}
		catch (NumberFormatException e) {
			return -1;
		}
	}

	public static int getDriverHelpTerm() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext());
		return prefs.getInt(Constants.DRIVER_HELP_TERM_PREF, -1);
	}

	public static void setDriverNumAndTerminal(Context context, String driverNumber, int helpTerm) {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext());
		String currentDriverNumber = prefs.getString(Constants.DRIVER_ID_PREF,"");
		int currentHelpTerm = prefs.getInt(Constants.DRIVER_HELP_TERM_PREF, -1);
		if (driverNumber.equalsIgnoreCase(currentDriverNumber)) {
			return;
		}
		SharedPreferences.Editor editor = prefs.edit();
		if (HelperFuncs.isNullOrWhitespace(driverNumber)) { // Driver logging out
			editor.putString(Constants.DRIVER_ID_PREV_LOGIN_PREF, currentDriverNumber);
			editor.putInt(Constants.DRIVER_ID_PREV_HELP_TERM_PREF, currentHelpTerm);
			editor.putInt(Constants.DRIVER_HELP_TERM_PREF, -1); // force to -1 on logout
		}
		else {
			editor.putInt(Constants.DRIVER_HELP_TERM_PREF, helpTerm);
		}
		editor.putString(Constants.DRIVER_ID_PREF, driverNumber);
		editor.commit();
		AppConfig.applySettingsOverrides(AutoTranApplication.getAppContext());
	}

	public static int getTruckLocLastGpsMillis() {
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(AutoTranApplication.getAppContext());
		return prefs.getInt(Constants.TRUCK_EVENT_LAST_GPS_MILLIS_PREF, -1);
	}

	public static String getDbFileSize(Context context) {
		File appDb = new File("/data/data/" + context.getPackageName() + "/databases/AutotranLocalDB");

		return HelperFuncs.getFolderSizeLabel(appDb);
	}

    public static String generateLogZipFile(String driverNumber, Context context, boolean largeUpload, boolean useExternalStorage) {
		log.debug(Logs.DEBUG, "Attempting to send logs to Cassens S3 bucket");

		File Root = new File("/sdcard/AutoTran/logs");
		FilenameFilter filter = new FilenameFilter() {

			@Override
			public boolean accept(File dir, String filename) {
				if (filename.contains("AutoTran"))
					return true;
				else
					return false;
			}
		};

		File[] files = Root.listFiles(filter);

		File directory = new File(Root.getAbsolutePath() + "/zipped");

		if (directory.exists()) {
			File[] deletableFiles = directory.listFiles();
			log.debug(Logs.DEBUG, "logger has " + deletableFiles.length + " files in it");

			for (File file: deletableFiles) {
				if (file.exists() && file.isFile()) {
					if ((largeUpload && file.getName().endsWith("complete.zip")) || !largeUpload && file.getName().endsWith("small.zip")) {
						if (file.delete()) {
							log.debug(Logs.DEBUG, "Deleted " + file.getName());
						}
						else {
							log.debug(Logs.DEBUG, "Delete failed for " + file.getName());
						}
					}
				}
			}
			if (directory.delete()) {
				log.debug(Logs.DEBUG, "Deleted " + directory.getName());
			}
		}

		if (!directory.mkdirs()){
			//log.warn(Logs.DEBUG, "Could not create logger directory.");
		}

		for (File file: files) {
			if (file.exists() && file.isFile()) {
				try {
					copy(file, new File(directory + "/" + file.getName()));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}

		try {
			byte[] buffer = new byte[1024 * 32];
			int count;

			//logcat (and suppress airwatch lockdown messages...
			Process mainP = Runtime.getRuntime().exec("/system/bin/logcat -v threadtime -d -f " + Root.getAbsolutePath() + "/zipped/LogcatMain.log");
			mainP.waitFor();

			//output from things that can't be dumped to files
			//currently: ifconfig
			Process ifconfigP = Runtime.getRuntime().exec("ifconfig");

			BufferedInputStream ifconfigInput = new BufferedInputStream(ifconfigP.getInputStream(), 1024 * 32);
			FileOutputStream ifconfigOutput = new FileOutputStream(new File(Root.getAbsolutePath() + "/zipped/ifconfig.log"));

			while((count = ifconfigInput.read(buffer, 0, 1024 * 32)) != -1){
				ifconfigOutput.write(buffer, 0, count);
			}

			ifconfigInput.close();
			ifconfigOutput.close();

		}
		catch (Exception e){
			log.error(Logs.DEBUG, "Error getting logs");
			log.error(Logs.DEBUG, e.toString());
		}

		File zippedLogs;
		if (largeUpload) {
			zippedLogs = new File(Root.getAbsolutePath() + "/zipped/autotran-log-files." + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date()) + "_complete.zip");
		} else {
			zippedLogs = new File(Root.getAbsolutePath() + "/zipped/autotran-log-files." + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date()) + "_small.zip");
		}

		if (zippedLogs.exists()) {
			//Delete old versions of the file;
			log.debug(Logs.DEBUG, "attempting to delete " + zippedLogs.getName());
			zippedLogs.delete();
		}

		File logDirectory = directory;
		File[] logFiles = logDirectory.listFiles();

		if(logFiles != null) {
			log.debug(Logs.DEBUG, "Log count: " + logFiles.length);
		}

		BufferedInputStream origin;
		FileOutputStream dest;
		try {
			dest = new FileOutputStream(zippedLogs);

			ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(dest));
			FileInputStream fi;
			ZipEntry e;

			int bufsize = 1024 * 10;
			byte[] data = new byte[bufsize];
			int count;

			if(largeUpload) {
				// include database file in zip
				File appDb = new File("/data/data/" + context.getPackageName() + "/databases/AutotranLocalDB");
				fi = new FileInputStream(appDb);
				origin = new BufferedInputStream(fi, bufsize);
				e = new ZipEntry(appDb.getName() + ".sqlite");
				zip.putNextEntry(e);

				while ((count = origin.read(data, 0, bufsize)) != -1) {
					zip.write(data, 0, count);
				}
			}
			count = 0;

			for(int i = 0; i < logFiles.length; i++) {
				if ((largeUpload && !logFiles[i].getName().endsWith("small.zip")) || !logFiles[i].getName().endsWith(".zip")) {
					fi = new FileInputStream(logFiles[i]);
					origin = new BufferedInputStream(fi, bufsize);
					e = new ZipEntry(logFiles[i].getName());
					zip.putNextEntry(e);
					while ((count = origin.read(data, 0, bufsize)) != -1) {
						zip.write(data, 0, count);
					}
					count = 0;
					origin.close();

					fi.close();
				}
			}
			zip.close();
			dest.close();
		} catch (FileNotFoundException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		} catch (IOException e2) {
			//
			e2.printStackTrace();
		}

		String location = Uri.fromFile(zippedLogs).toString();

		if (useExternalStorage) {
			String externalName = getDeviceSerial() + "_" + new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.US).format(new Date());
			if (largeUpload) {
				externalName += "_with_db.zip";
			} else {
				externalName += ".zip";
			}
			File externalPath = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + "/" +  externalName);
			try {
				copy(zippedLogs, externalPath );
				location = Uri.fromFile(externalPath).toString();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		return location;
	}

    public static void setDefaultTerminalPref(Context ctx, String terminalNum) throws NumberFormatException {
        int i = Integer.parseInt(terminalNum); // Forces NumberFormatException if terminalNum invalid
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(Constants.DEFAULT_TERMINAL_PREF, terminalNum);
        editor.commit();
    }

	public static String getDefaultTerminalPref(Context ctx) {
		return Integer.toString(getDefaultTerminalNum(ctx));
	}

	public static int getDefaultTerminalNum(Context ctx) {
      int defaultTerminal;  //
      SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

      try {
          String defaultTerminalString = prefs.getString(Constants.DEFAULT_TERMINAL_PREF, "");
          if (HelperFuncs.isNullOrWhitespace(defaultTerminalString)) {
              throw new NumberFormatException();
          }
          defaultTerminal = Integer.parseInt(defaultTerminalString);
      }
      catch (NumberFormatException ne) {
          // Note we have to check to ensure that DEFAULT_TERMINAL_PREF is valid because revs prior
          // to 2.4.13 didn't error-check the value when it was set.
          setDefaultTerminalPref(ctx, "50");
          defaultTerminal = 50;
      }
      return defaultTerminal;
  	}


	public void sendLogList(String driverNumber, Context context, String whichUpload) {
	    sendLogList(driverNumber, context, whichUpload, null);
    }

	//wrapper so we can automate sending in the normal case but split up for admin mode if something is really broken
	public void sendLogList(String driverNumber, Context context, String whichUpload, @Nullable DriverActionEvent event) {
		ArrayList<S3Container> logs = getLogS3Containers(driverNumber, context, whichUpload,false);

		uploadLogs(driverNumber, context, logs, false, event);
	}

	public static void uploadLogs(String driverNumber, Context context, ArrayList<S3Container> logs, boolean useExternalStorage) {
		uploadLogs(driverNumber, context, logs, useExternalStorage, null);
	}

	public static void uploadLogs(String driverNumber, Context context, ArrayList<S3Container> logs, boolean useExternalStorage, @Nullable final DriverActionEvent event) {
        S3BucketAuth authInfo = new S3BucketAuth();
        authInfo.AWS_ACCOUNT_ID 		= "331592269501";
        authInfo.COGNITO_POOL_ID 		= "us-east-1:6ece6298-b343-4499-9533-db0d4130e8e1";
        authInfo.COGNITO_ROLE_UNAUTH	= "arn:aws:iam::331592269501:role/Cognito_AutoTranUnauth_DefaultRole";
        authInfo.COGNITO_ROLE_AUTH   	= "arn:aws:iam::331592269501:role/Cognito_AutoTranAuth_DefaultRole";
        authInfo.BUCKET_NAME 			= "autotran-logs";
        authInfo.folderName 			= "driverLogs/" + driverNumber + "/" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());

        final int requestId = new Random().nextInt(Integer.MAX_VALUE);

        if(event != null) {
            EventBusManager.getInstance().listenForEvents(EventBusManager.Queue.NETWORK_REQUESTS, (e) -> {
                if (e instanceof S3UploadEvent) {
                    S3UploadEvent s3e = (S3UploadEvent) e;
                    log.debug(Logs.DEBUG, "Got upload event: " + e);
                    if(s3e.id == requestId) {
                        log.debug(Logs.DEBUG, "Upload event ID matches log upload request: " + e);
                        event.result = s3e.result;
                        EventBusManager.getInstance().publish(event);
                        return true;
                    }
                }

                return false;
            });
        }
        TransferController.uploadWithId(context, logs, authInfo, requestId);

        //Clean up after ourselves, but don't delete the files we're trying to send
        File root = new File("/sdcard/AutoTran/logs");
        File[] logFiles = new File(root.getAbsolutePath() + "/zipped").listFiles();
        for(int i = 0; i < logFiles.length; i++){
            if (!logFiles[i].getName().endsWith("small.zip") && !logFiles[i].getName().endsWith("complete.zip")) {
                logFiles[i].delete();
            }
        }
    }

	public static ArrayList<S3Container> getLogS3Containers(String driverNumber, Context context, String whichUpload, boolean useExternalStorage) {
	    	return getLogS3Containers(driverNumber, context, whichUpload, useExternalStorage, null);
	}

	public static ArrayList<S3Container> getLogS3Containers(String driverNumber, Context context, String whichUpload, boolean useExternalStorage, DirSaveCallback callback) {
		ArrayList<S3Container> logs = new ArrayList<>();
		String directory = "";

			if (whichUpload.equals("small")) {
				S3Container container = new S3Container();
				container.uri = generateLogZipFile(driverNumber, context, false, useExternalStorage);
                directory = container.uri.toString();
				container.foldername = "driverLogs/" + driverNumber + "/" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
				logs.add(container);

			} else if (whichUpload.equals("large")) {
				S3Container container = new S3Container();
				container.uri = generateLogZipFile(driverNumber, context, true, useExternalStorage);
                directory = container.uri.toString();
				container.foldername = "driverLogs/" + driverNumber + "/" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
				logs.add(container);

			} else if (whichUpload.equals("both")) {
				//uriList.add(generateLogZipFile(driverNumber, context, false));
				S3Container container = new S3Container();
				container.uri = generateLogZipFile(driverNumber, context, true, useExternalStorage);
                directory = container.uri.toString();
				container.foldername = "driverLogs/" + driverNumber + "/" + new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
				logs.add(container);
			}

		if (callback != null) {
			callback.fileSaved(directory);
		}

		return logs;
	}


	public static void copy(File src, File dst) throws IOException {
		    InputStream in = new FileInputStream(src);
		    OutputStream out = new FileOutputStream(dst);

		    // Transfer bytes from in to out
		    byte[] buf = new byte[1024];
		    int len;
		    while ((len = in.read(buf)) > 0) {
		        out.write(buf, 0, len);
		    }
		    in.close();
		    out.close();
		}

	private static final Map<Character, Integer> vci = new HashMap<Character, Integer>();
    static {
		vci.put('0',0);		vci.put('1',1);		vci.put('2',2);		vci.put('3',3);		vci.put('4',4);		vci.put('5',5);		vci.put('6',6);
		vci.put('7',7);		vci.put('8',8);		vci.put('9',9);		vci.put('A',1);		vci.put('B',2);		vci.put('C',3);		vci.put('D',4);
		vci.put('E',5);		vci.put('F',6);		vci.put('G',7);		vci.put('H',8);		vci.put('J',1);		vci.put('K',2);		vci.put('L',3);
		vci.put('M',4);		vci.put('N',5);		vci.put('P',7);		vci.put('R',9);		vci.put('S',2);		vci.put('T',3);		vci.put('U',4);
		vci.put('V',5);		vci.put('W',6);		vci.put('X',7);		vci.put('Y',8);		vci.put('Z',9);
    }

	private static int[] vcmult = {8,7,6,5,4,3,2,10,0,9,8,7,6,5,4,3,2};

    public static boolean validVin(String vin) {
    	return validVin(vin, false);
	}

	private static boolean validVin(String vin, boolean shouldLog) {
		if(!BuildConfig.AUTOTRAN_VALIDATE_VINS) {
			log.debug(Logs.DEBUG, "Skipping validation for " + vin + " because of local.properties settings");
			return true;
		}
		if(shouldLog) log.debug(Logs.DEBUG, "Checking vin " + vin);

		if(vin.length() > 17) {
			vin = vin.substring(vin.length() - 17);
			if(shouldLog) log.debug(Logs.DEBUG, "truncating to " + vin);
		}

		if (vin.length() != 17) {
			if (shouldLog) log.debug(Logs.DEBUG, "Invalid VIN length");
			return false;
		}

		char[] vinArray = vin.toCharArray();

		int vtot = 0;

		int index = 0;
		for(char vinChar : vinArray) {
			if(!vci.containsKey(vinChar)) {
				if(shouldLog) log.debug(Logs.DEBUG, "VINs can't contain " + vinChar);
				return false;
			} else {
				vtot = vtot + (vci.get(vinChar) * vcmult[index]);
			}
			index++;
		}

		int result = vtot % 11;

		String tst_ch = String.valueOf(result);

		if(shouldLog) log.debug(Logs.DEBUG, "tst_ch: " + tst_ch);

		if(result == 10) {
			tst_ch = "X";
		}

		if(tst_ch.equals(String.valueOf(vinArray[8]))) {
			if(shouldLog) log.debug(Logs.DEBUG, "vinArray[8]: " + vinArray[8]);
			return true;
		} else {
			if(vin.length() == 17 && vinArray[0] == 'Z' && vinArray[8] == '0') {
				if(shouldLog) log.debug(Logs.DEBUG, "vinArray[0]: " + vinArray[0] + " and vinArray[8]:" + vinArray[8]);
				return true;
			}
		}

		return false;
	}

	static private String picturesDirectory = null;

	public static String cachedImageFileFullPath(Context context, String fileName) {
		if (picturesDirectory == null) {

			Context applicationContext = context.getApplicationContext();
			File externalFilesDir = applicationContext.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

			picturesDirectory = new String(externalFilesDir.getAbsolutePath());
			//"/sdcard/Android/data/com.cassens.autotran/files/Pictures/";
		}

		return (picturesDirectory + "/" + fileName + ".jpg");
	}

	public static void deleteCachedImageFile(Context context, String imageFileName) {
		try{
			File imageFile = new File(cachedImageFileFullPath(context, imageFileName));
			System.out.println("Deleting image file " + imageFileName);
			imageFile.delete();
		}catch(Exception e){
			// if any error occurs
			System.out.println("Error deleting image file " + imageFileName);
			e.printStackTrace();
		}
	}

	    /**
     * Get IP address from first non-localhost interface
     * @param useIPv4  true=return ipv4, false=return ipv6
     * @return  address or empty string
     */
    public static String getIPAddress(boolean useIPv4) {
        try {
            List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface intf : interfaces) {
                List<InetAddress> addrs = Collections.list(intf.getInetAddresses());
                for (InetAddress addr : addrs) {
                    if (!addr.isLoopbackAddress()) {
                        String sAddr = addr.getHostAddress();
                        //boolean isIPv4 = InetAddressUtils.isIPv4Address(sAddr);
                        boolean isIPv4 = sAddr.indexOf(':')<0;

                        if (useIPv4) {
                            if (isIPv4)
                                return sAddr;
                        } else {
                            if (!isIPv4) {
                                int delim = sAddr.indexOf('%'); // drop ip6 zone suffix
                                return delim<0 ? sAddr.toUpperCase() : sAddr.substring(0, delim).toUpperCase();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) { } // for now eat exceptions
        return "";
    }

    public static int getImageRotation(Context context, String newImageFilePath) {
    	if(!newImageFilePath.contains("/")) {
    		newImageFilePath = CommonUtility.cachedImageFileFullPath(context, newImageFilePath);
		}

        try {
            ExifInterface ei = null;
            ei = new ExifInterface(newImageFilePath);

            int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_UNDEFINED);

            switch(orientation) {

                case ExifInterface.ORIENTATION_ROTATE_90:
                    return 90;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    return 180;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    return 270;
                case ExifInterface.ORIENTATION_NORMAL:
                default:
                    return 0;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return 0;
    }

    public static Bitmap rotateImage(Bitmap source, float angle) {
        Matrix matrix = new Matrix();
        matrix.postRotate(angle);
        Bitmap rotatedBitmap = Bitmap.createBitmap(source, 0, 0, source.getWidth(), source.getHeight(),
				matrix, true);
        return rotatedBitmap;
    }

	public static void saveBitmap(Bitmap rotatedBitmap, String newImageFilePath) {
		//write out the rotated bitmap to the original bitmap location
		log.debug("Writing out bitmap " + newImageFilePath);

		FileOutputStream out = null;
		try {
			out = new FileOutputStream(newImageFilePath);
			rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, 90, out); // bmp is your Bitmap instance
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				if (out != null) {
					out.close();
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}

	public static String getDeviceSerial() {
    	//Log.d("NARF", "serial number: " + Build.SERIAL);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			try {
				return Build.getSerial();
			} catch (SecurityException se) {
				// Beginning with Android 10, more rigrorous requirements
				// have been imposed in order to call Build.getSerial(). If
				// the app doesn't meet the requirements, Android throws
				// a security exception.
				// See https://developer.android.com/reference/android/os/Build#getSerial()
				return "unavailable";
			}
		}
		else {
			return Build.SERIAL;
		}
	}

	public static String removeNonNumericCharacters(String input) {
    	return input.replaceAll("[^\\d]", "");
	}

	public static String getMACAddress() {
		try {
			List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
			for (NetworkInterface nif : all) {
				if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

				byte[] macBytes = nif.getHardwareAddress();
				if (macBytes == null) {
					return "";
				}

				StringBuilder res1 = new StringBuilder();
				for (byte b : macBytes) {
					res1.append(String.format("%02X:",b));
				}

				if (res1.length() > 0) {
					res1.deleteCharAt(res1.length() - 1);
				}
				return res1.toString();
			}
		} catch (Exception ex) {
		}
		return "02:00:00:00:00:00";
	}

	public static boolean isInteger(String s) {
		try {
			Integer.parseInt(s);
		} catch(NumberFormatException e) {
			return false;
		} catch(NullPointerException e) {
			return false;
		}
		// only got here if we didn't return false
		return true;
	}

	public static Image upsertHiResCopy(Context context, Image loResImage) {
		return upsertHiResCopy(context, loResImage, false);
	}

	public static Image upsertHiResCopy(Context context, Image loResImage, boolean queueForUpload) {
		Image hiResImage = HelperFuncs.getHiresCopy(loResImage);
		if (queueForUpload) {
			hiResImage.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
		}

		hiResImage.image_id = (int) DataManager.insertImageToLocalDB(context, hiResImage);

		return hiResImage;
	}

	// "Quick & dirty" helper to prevent successive rapid clicks from launching multiple things.
	// Obiously, this is not thread save, but doesn't need to be for this use case.
	private static long mLastClickTime = 0;
	public static boolean doubleClickDetected() {
		// Prevent rapid clicks from opening multiple windows
		if ((System.currentTimeMillis() - mLastClickTime) < 1000) {
			return true;
		}
		mLastClickTime = System.currentTimeMillis();
		return false;
	}

	private static final String FIELD_SEPARATOR = "|";
	private static final String FIELD_SEPARATOR_HTML_CODE = "&vert;";
	private static final int HIGH_LEVEL_LOG_MESSAGE_LINE_LENGTH = 58;

	public static String highLevelLogMsgWordWrap(String message, int indent, int lineLength) {
		return HelperFuncs.wordWrapString(message, lineLength - indent);
	}
	public static String highLevelLogMsgWordWrap(String message, int indent) {
		return highLevelLogMsgWordWrap(message, indent, HIGH_LEVEL_LOG_MESSAGE_LINE_LENGTH );
	}


	public static void highLevelLog(String message, Load load, Delivery delivery) {
		highLevelLog(message, load, delivery, true);
	}

	public static void highLevelLog(String message, Load load, Delivery delivery, boolean wordWrap) {
		String loadNumber = "?";
		String loadId = "?";
		String delivId = "?";
		String dealerNumber = "?";
		String dealerName = "?";
		String dealerContact = "?";
		if (load != null) {
			loadNumber = HelperFuncs.noNull(load.loadNumber);
			loadId = HelperFuncs.noNull(load.load_remote_id);
		}
		if (delivery != null) {
			delivId = HelperFuncs.noNull(delivery.delivery_remote_id);
			if (delivery.dealer != null) {
				dealerNumber = HelperFuncs.noNull(delivery.dealer.customer_number);
				dealerName = HelperFuncs.noNull(delivery.dealer.customer_name);
			}
			dealerContact = HelperFuncs.noNull(delivery.dealerContact);
		}
		message = message.replaceAll("\\$loadNumber", loadNumber)
				.replaceAll("\\$loadId", loadId)
				.replaceAll("\\$deliveryId", delivId)
				.replaceAll("\\$dealerNumber", dealerNumber)
				.replaceAll("\\$dealerName", dealerName)
				.replaceAll("\\$dealerContact", dealerContact)
				.replaceAll("\\$driverNumber", getDriverNumber(AutoTranApplication.getAppContext()));

		if (wordWrap) {
			message = highLevelLogMsgWordWrap(message, 0);
		}

		// Escape the newlines and field separator characters with their HTML equivalents
		message = message.replace("\n", "<br>")
						//\.replace("\"", "&quot;")
						//.replace("'", "&apos;")

						.replace(FIELD_SEPARATOR, FIELD_SEPARATOR_HTML_CODE);
		log.debug(Logs.DEBUG, "HIGH_LEVEL:" + message);

		log.debug(Logs.HIGH_LEVEL, String.format("loadNum=%s loadId=%s deliveryId=%s%s%s",
				HelperFuncs.noNull(loadNumber, "?"),
				HelperFuncs.noNull(loadId, "?"),
				HelperFuncs.noNull(delivId, "?"),
				FIELD_SEPARATOR,
				message));
	}

	public static void highLevelLog(String message, Load load) {
		highLevelLog(message, load, null);
	}

	public static void highLevelLog(String message) {
		highLevelLog(message, null, null);
	}

	public static void dispatchUploadLog(String message) {
		log.debug(Logs.DEBUG, message);
		log.debug(Logs.DISPATCH_UPLOAD, message);
	}

	public static void dispatchLogMessage(String message) {
		dispatchUploadLog("DISPATCH: " + message);
	}

	public static void uploadLogMessage(String message) {
		dispatchUploadLog("UPLOAD: " + message);
	}

	private static final Object concurrentDBAccess = new Object();
	private static int numDBAccessThreads = 0;

	public static void dispatchUploadLogThreadStartStop(String message, boolean starting) {
		String line = "========================================";

		synchronized (concurrentDBAccess) {
			if (starting) {
				numDBAccessThreads++;
			} else {
				numDBAccessThreads--;
			}
			dispatchUploadLog("<" + line);
			dispatchUploadLog(String.format("%s (numDBAccessThreads=%d)", message, numDBAccessThreads));
			if (starting && numDBAccessThreads > 1) {
				dispatchUploadLog("################# MULTIPLE CONCURRENT DB THREADS #################");
			}
			dispatchUploadLog(line + ">");
		}
	}



	public static void dispatchLogThreadStartStop(String message, boolean starting) {
		dispatchUploadLogThreadStartStop("DISPATCH: " + message, starting);
	}

	public static void uploadLogThreadStartStop(String message, boolean starting) {
		dispatchUploadLogThreadStartStop("UPLOAD: " + message, starting);
	}


	public static String getZeroPaddedCode(String code) {
		try {
			return String.format("%02d", Integer.parseInt(code));
		}
		catch (NumberFormatException e) {
			return "";
		}
	}

	public static boolean isHoneywellLargeDisplaySet() {

		// Unfortunately, the Display Size setting on the Honeywell doesn't seem to affect the screenLayout size
		// returned in the config, so the following doesn't work:
		//
		//		int screenSize = context.getResources().getConfiguration().screenLayout & Configuration.SCREENLAYOUT_SIZE_MASK;
		//		log.debug(Logs.DEBUG, String.format("screenSize: 0x%x h=%d w=%d", screenSize, config.screenHeightDp, config.screenWidthDp));
		//		return screenSize == Configuration.SCREENLAYOUT_SIZE_LARGE;
		//
		// Instead, we just check for height resolution that's lower than the default screen height.
		//log.debug(Logs.DEBUG, String.format("screenLayout: 0x%x h=%d w=%d", config.screenLayout, config.screenHeightDp, config.screenWidthDp));

		return AutoTranApplication.getAppContext().getResources().getConfiguration().screenHeightDp < 616;
	}

	public static boolean isLargeFontSet() {
		return AutoTranApplication.getAppContext().getResources().getConfiguration().fontScale > 1.0;
	}

	public static void scaleImageView(Activity activity, int id, double scaleFactor) {
		ImageView imageView = activity.findViewById(R.id.noPhoneUseIcon);
		ViewGroup.LayoutParams lp = imageView.getLayoutParams();
		lp.width = (int) (lp.width * scaleFactor);
		lp.height = (int) (lp.height * scaleFactor);
		imageView.setLayoutParams(lp);
	}
	public static boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		try {
			double d = Double.parseDouble(strNum);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}

	public static void disableEditText(EditText editText) {
		editText.setFocusable(false);
		editText.setEnabled(false);
		editText.setCursorVisible(false);
		editText.setKeyListener(null);
		//editText.setBackgroundColor(Color.TRANSPARENT);
	}

	public static void logButtonClick(Logger log, String buttonName, String extraContext) {
		log.debug(Logs.INTERACTION, String.format("Button click: '%s'%s",
				HelperFuncs.noNull(buttonName, "?").replace("\n", " "),
				(extraContext == null) ? "" : " " + extraContext));
	}

	public static void logButtonClick(Logger log, String buttonName) {
		logButtonClick(log, buttonName, null);
	}

	public static void logButtonClick(Logger log, View view, String extraContext) {
		try {
			if (view == null) {
				logButtonClick(log, "<NULL view>", extraContext);
			}
			else {
				logButtonClick(log, ((TextView) view).getText().toString(), extraContext);
			}
		}
		catch (ClassCastException ex) {
			logButtonClick(log, "<clickable " + view.getClass().getSimpleName() + ">", extraContext);
		}
	}
	public static void logButtonClick(Logger log, View view) {
		logButtonClick(log, view, null);
	}

	public static void logChoiceClick(Logger log, String choiceName, String choiceContext) {
		log.debug(Logs.INTERACTION, String.format("Selection: '%s' for '%s'",
				HelperFuncs.noNull(choiceName, "?"),
				HelperFuncs.noNull(choiceContext, "?")));
	}

	public static String formatJson(String prefix, String json) {
		String formattedJson;
		try {
			formattedJson = new JSONObject(json).toString(2);
		} catch (JSONException ex) {
			formattedJson = json;
		}
		if (HelperFuncs.isNullOrEmpty(prefix)) {
			return formattedJson;
		}
		else {
			return String.format("%s %s", prefix, formattedJson);
		}
	}

	public static String escapeWhitespaceControlChars(String str) {
		StringBuilder escapedStr = new StringBuilder();

		for (int i = 0; i < str.length(); i++) {
			char c = str.charAt(i);

			switch (c) {
				case '\n':
					escapedStr.append("\\\\n");
					break;
				case '\r':
					escapedStr.append("\\\\r");
					break;
				case '\t':
					escapedStr.append("\\\\t");
					break;
				default:
					escapedStr.append(c);
					break;
			}
		}

		return escapedStr.toString();
	}

	public static String formatJson(String json) {
		return formatJson(null, json);
	}

	public static void logJson(Marker marker, String tag, String json) {
		if (json.length() <= 100000) {  // HACK
			if (isNullOrBlank(tag)) {
				log.debug(marker, json);
			}
			else {
				log.debug(marker, String.format("%s: %s", tag, json));
			}
			return;
		}
		String tagString;
		if (HelperFuncs.isNullOrEmpty(tag)) {
			tagString = "";
		}
		else {
			tagString = String.format("%s:\n", tag);
		}
		log.debug(marker, String.format("%sBEGIN_JSON\n%s\nEND_JSON", tagString, formatJson(json)));
		// Note: We need the following empty log message in order to create a timestamp for parsing
		//       the log later with our scripts.
		log.debug(marker, String.format(""));
	}


	public static void appendEpodContactInfo(Context ctx, TextView tv, User driver, String extraContext) {
		String email = ctx.getString(R.string.support_email);
		String phoneUsa = ctx.getString(R.string.support_phone_usa);
		final String phoneCanada = ctx.getString(R.string.support_phone_canada);
		final String subject;
		if (HelperFuncs.isNullOrWhitespace(extraContext)) {
			subject = "AutoTran Support Request";
		}
		else {
			subject = "AutoTran Support Request: " + extraContext;
		}
		String body;
		if (driver == null) {
			body = String.format("Driver: %s\n", getDriverNumber(ctx));
		}
		else {
			body = String.format("Driver: %s %s %s\n", driver.driverNumber, driver.firstName, driver.lastName);
		}

		SpannableStringBuilder textBuilder = new SpannableStringBuilder();
		String existingText = tv.getText().toString();
		if (!HelperFuncs.isNullOrEmpty(existingText)) {
			textBuilder.append(StringUtils.substringBeforeLast(existingText, "\n") + "\n");
		}
		textBuilder.append(createClickableEmail(email));
		textBuilder.append("\n");
		textBuilder.append(phoneUsa);
		textBuilder.append(" in US");
		textBuilder.append("\n");
		textBuilder.append(phoneCanada);
		textBuilder.append(" in Canada");
		tv.setText(textBuilder);
		tv.setOnClickListener(view -> {
			Intent intent = new Intent(Intent.ACTION_SEND);
			intent.setType( "message/rfc822");
			intent.putExtra(Intent.EXTRA_EMAIL, new String[]{email});
			intent.putExtra(Intent.EXTRA_SUBJECT, subject);
			intent.putExtra(Intent.EXTRA_TEXT, body);
			ctx.startActivity(Intent.createChooser(intent, "Send Email"));
		});
	}
	public static void appendEpodContactInfo(Context ctx, TextView tv, User driver) {
		appendEpodContactInfo(ctx, tv, driver, null);
	}

	private static SpannableStringBuilder createClickableEmail(String email) {
		SpannableStringBuilder ssb = new SpannableStringBuilder(email);
		Linkify.addLinks(ssb, Linkify.EMAIL_ADDRESSES);
		return ssb;
	}
}
