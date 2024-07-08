package com.cassens.autotran.activities;

import android.annotation.TargetApi;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Looper;
import android.view.View;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ErrorActivity extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(ErrorActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_error);

		log.debug(Logs.INTERACTION, "Starting error activity");
	}
	
	@Override
	protected void onResume() {
		super.onResume();


	}

	public void restart(View view) {
		CommonUtility.logButtonClick(log, view, "to restart");
		new Thread() {
			@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR2)
			@Override
			public void run() {

				Looper.prepare();
				final Context context = getApplicationContext();
				Intent intent = new Intent(getApplicationContext(), DashboardActivity.class);
				intent.putExtra("source", "error");
				int mPendingIntentId = 123456;
				PendingIntent mPendingIntent = PendingIntent.getActivity(context, mPendingIntentId, intent, PendingIntent.FLAG_CANCEL_CURRENT | PendingIntent.FLAG_IMMUTABLE);
				AlarmManager mgr = (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
				mgr.set(AlarmManager.RTC, System.currentTimeMillis() + 75, mPendingIntent);
				System.exit(0);
				Looper.loop();
				Looper.myLooper().quitSafely();
			}
		}.start();
	}
}
