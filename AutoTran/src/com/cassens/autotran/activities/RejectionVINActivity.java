package com.cassens.autotran.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RejectionVINActivity extends AutoTranActivity implements OnClickListener
{
	private static final Logger log = LoggerFactory.getLogger(ProblemReportActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_rejection_vin);
		
		((Button) findViewById(R.id.btn_reject_ok)).setOnClickListener(this);
		((Button) findViewById(R.id.btn_reject_cancel)).setOnClickListener(this);
		((ImageView) findViewById(R.id.img_back)).setOnClickListener(this);
	}

	@Override
	public void onClick(View v)
	{
		CommonUtility.logButtonClick(log, v);

		int id = v.getId();
		if (id == R.id.btn_reject_ok) {
			String message = ((EditText) findViewById(R.id.et_reject_message)).getText().toString();
			if (message.equalsIgnoreCase(""))
			{
				CommonUtility.showText("Reason cannot be blank.");
			}
			else
			{
				Intent i = new Intent();
				i.putExtra("reason", message);
				this.setResult(RESULT_OK, i);
				this.finish();
			}
		} else if (id == R.id.btn_reject_cancel || id == R.id.img_back) {
			Intent i = new Intent();
			this.setResult(RESULT_CANCELED, i);
			this.finish();
		}
	}

}
