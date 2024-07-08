package com.cassens.autotran.activities;

import com.cassens.autotran.R;
import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DealerMessagesActivity extends AutoTranActivity
{
	private static final Logger log = LoggerFactory.getLogger(DealerMessagesActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_dealer_messages);

	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.dealer_messages, menu);
		return true;
	}
}
