package com.cassens.autotran.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project : AUTOTRAN Description : DamageCodeEnter class enter the area code and verifying
 * 
 * @author Hemant Creation Date : 12-11-2013
 */
public class ScacCodeEnter extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(ScacCodeEnter.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	EditText codeTextView;
	Button ok;
	TextView prompt, title;
	ProgressDialog dialog;
	int terminal_id;
	
	// Subclass
	// Request Codes for Launched Activities
    private static final int REQ_CODE_SCAC = 1002;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.code_enter_string);
		codeTextView = (EditText) findViewById(R.id.code);
		
		prompt = (TextView) findViewById(R.id.prompt);
		title = (TextView) findViewById(R.id.title);
		ok = (Button) findViewById(R.id.ok);
		terminal_id = Integer.parseInt(getIntent().getStringExtra("terminal_id")); 
log.debug(Logs.DEBUG, "in ScacCodeEnter terminal_id=" + terminal_id);
		// Subclass
		prompt.setText("Enter SCAC Code");
		title.setText("SCAC");
		
		codeTextView.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				// TODO Auto-generated method stub
	            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
	                checkCode(v);
	            }    
	            return false;
				
			}
	    });

	}

	public boolean codeIsValid(String code)
    {
	    // Overrride
	    return true;
    }
	
	public void checkCode(View v)
	{
		CommonUtility.logButtonClick(log, v, "check code");
	    // Subclass
        String code = codeTextView.getText().toString().trim();
		if (CommonUtility.isNullOrBlank(code)){
			CommonUtility.showText("Invalid SCAC Code");
            return;
        }
		
		if (!codeIsValid(code)) {
		    return;
		}
		
		Intent intent = new Intent();
		intent.putExtra("code", code);
		setResult(RESULT_OK, intent);
		finish();
		//Set up the return intent and return
	}

	public void menuList(View v)
	{
	    Intent intent = new Intent(ScacCodeEnter.this, ScacCodeList.class);
	    intent.putExtra("terminal_id", terminal_id);
		startActivityForResult(intent, REQ_CODE_SCAC);
	}

	public void back(View v)
	{
        Intent intent = new Intent();
        setResult(RESULT_CANCELED, intent);
		finish();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		Intent intent = new Intent();
		
		if (resultCode == RESULT_OK) {
		    if (requestCode == REQ_CODE_SCAC) {
                intent.putExtra("code", data.getStringExtra("code"));
                setResult(resultCode, intent);
                finish();
		    }
		}
	}
	
}
