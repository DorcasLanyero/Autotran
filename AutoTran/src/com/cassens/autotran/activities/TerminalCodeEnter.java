package com.cassens.autotran.activities;

import android.app.Activity;
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
import com.cassens.autotran.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project : AUTOTRAN Description : DamageCodeEnter class enter the area code and verifying
 * 
 * @author Hemant Creation Date : 12-11-2013
 */
public class TerminalCodeEnter extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(TerminalCodeEnter.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	EditText codeTextView;
	Button ok;
	TextView prompt;
	ProgressDialog dialog;
	
	// Subclass
	// Request Codes for Launched Activities
	private static final int REQ_CODE_TERMINAL = 1001;
    private static final int REQ_CODE_LOT = 1002;	
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.code_enter);
		codeTextView = (EditText) findViewById(R.id.code);

		String currentTerminalNum = getIntent().getStringExtra("currentTerminalNum");
		if (currentTerminalNum != null) {
			codeTextView.setText(currentTerminalNum);
		}
		
		prompt = (TextView) findViewById(R.id.prompt);
		ok = (Button) findViewById(R.id.ok);

		// Subclass
		prompt.setText("Enter Terminal Number");
		
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
	    try {
			Integer.parseInt(code);
		} catch (NumberFormatException ex) {
	    	return false;
		}
	    return true;
    }
	
	public void checkCode(View v)
	{
	    // Subclass
        String code = codeTextView.getText().toString().trim();
		if (CommonUtility.isNullOrBlank(code)){
			CommonUtility.showText("Please specify terminal");
            return;
        }
		
		if (!codeIsValid(code)) {
			CommonUtility.showText("Invalid Terminal: " + code);
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
		startActivityForResult(new Intent(TerminalCodeEnter.this, TerminalCodeList.class), REQ_CODE_TERMINAL);
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
		    if (requestCode == REQ_CODE_TERMINAL) {
                intent.putExtra("code", data.getStringExtra("code"));
                setResult(resultCode, intent);
                finish();
		    }
		}
	}
	
}
