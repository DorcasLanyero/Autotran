package com.cassens.autotran.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnKeyListener;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.lookup.AreaCode;
import com.cassens.autotran.data.model.lookup.SeverityCode;
import com.cassens.autotran.data.model.lookup.TypeCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Project : AUTOTRAN Description : DamageCodeEnter class enter the area code and verifying
 * 
 * @author Hemant Creation Date : 12-11-2013
 */
public class DamageCodeEnter extends AutoTranActivity implements OnKeyListener
{
    private static final Logger log = LoggerFactory.getLogger(DamageCodeEnter.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	EditText code;
	Button ok;
	TextView txt;
	ProgressDialog dialog;
	
	// Request Codes for Launched Activities
	private static final int REQ_CODE_AREA = 1001;
    private static final int REQ_CODE_TYPE = 1002;
    private static final int REQ_CODE_SVRTY = 1003;
    private static final int REQ_CODE_POS = 1004;
	
    boolean first = true;
    
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		logLifecycleMessages = false;
		super.onCreate(savedInstanceState);
		setContentView(R.layout.damage_code_enter);
		code = (EditText) findViewById(R.id.code);
		
		txt = (TextView) findViewById(R.id.txt);
		ok = (Button) findViewById(R.id.ok);
		String mode = getIntent().getStringExtra("mode");
		if (mode != null)
		{
			if (mode.equalsIgnoreCase("edit"))
			{
				code.setText(getIntent().getStringExtra("text"));
				code.setSelection(code.getText().length());
			}
		}

		if (getIntent().getStringExtra("check").equals("area"))
		{

		}
		else {
			if (getIntent().getStringExtra("check").equals("type"))
			{
				txt.setText("Type");
			}
			else {
				if (getIntent().getStringExtra("check").equals("svrty"))
				{
					txt.setText("Severity");
				}
				else {
					if (getIntent().getStringExtra("check").equals("pos"))
					{
						this.menuList(getCurrentFocus());
						txt.setText(R.string.enter_load_position);
					}
				}
			}
		}
		
		code.setOnEditorActionListener(new OnEditorActionListener() {
			@Override
			public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
				// TODO Auto-generated method stub
	            if ((event != null && (event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) || (actionId == EditorInfo.IME_ACTION_DONE)) {
	                checkCode(v);
	            }    
	            return false;
				
			}
	    });
		
		code.setOnKeyListener(this);

	}

	public void checkCode(View v)
	{
		CommonUtility.logButtonClick(log, v, "DamageCodeEnterActivity");

		if (code.getText().toString().equals("")) {
			if (getIntent().getStringExtra("check").equals("area")) {
				CommonUtility.showText("Enter the Area Code....");
			}
			else if (getIntent().getStringExtra("check").equals("type")) {
				CommonUtility.showText("Enter the Type Code....");
			}
			else if (getIntent().getStringExtra("check").equals("svrty")) {
				CommonUtility.showText("Enter the Severity Code....");
			}
			else if (getIntent().getStringExtra("check").equals("pos")) {
				CommonUtility.showText("Enter the Load Position....");
			}
		}
		else {			
			//Check the database for the value based on the current input type
			System.out.println("Damage Code Enter check");		
			
			if (getIntent().getStringExtra("check").equals("area")) {
					AreaCode areaCode = DataManager.getAreaCode(this, code.getText().toString());
					
					if(areaCode != null && areaCode.active) {
    					Intent i = new Intent();
    					i.putExtra("id", code.getText().toString() + "," + areaCode.area_code_id + ","
    							+ getIntent().getStringExtra("mode"));
    					setResult(RESULT_OK, i);
    					finish();
					} else {
						CommonUtility.showText("Invalid Area Code: " + code.getText());
					}
			
			}
			else if (getIntent().getStringExtra("check").equals("type")) {
					TypeCode typeCode = DataManager.getTypeCode(this, code.getText().toString());
					
					if(typeCode != null && typeCode.active) {
    					Intent i = new Intent();
    					i.putExtra("id", code.getText().toString() + "," +typeCode.type_code_id+ ","
    							+ getIntent().getStringExtra("mode"));
    					setResult(RESULT_OK, i);
    					finish();
					}else {
						CommonUtility.showText("Invalid Type Code: " + code.getText());
					}
			
			} else if (getIntent().getStringExtra("check").equals("svrty")) {

					SeverityCode severityCode = DataManager.getSeverityCode(this, code.getText().toString());
					
					if(severityCode != null && severityCode.active) {
    					Intent i = new Intent();
    					i.putExtra("id",code.getText().toString() + "," +severityCode.severity_code_id + ","
    							+ getIntent().getStringExtra("mode"));
    					setResult(RESULT_OK, i);
    					finish();
					} else {
						CommonUtility.showText("Invalid Severity Code: " + code.getText());
					}
			
			} else if (getIntent().getStringExtra("check").equals("pos")) {

					Intent i = new Intent();
					i.putExtra("id", code.getText().toString()+","+code.getText().toString());
					setResult(RESULT_OK, i);
					finish();			
			}			
			
			//Set up the return intent and return
		}
	}

	public void menuList(View v)
	{
		if (getIntent().getStringExtra("check").equals("area")) {
			startActivityForResult(new Intent(DamageCodeEnter.this, DamageCodeList.class), REQ_CODE_AREA);
		}
		else if (getIntent().getStringExtra("check").equals("type")) {
			startActivityForResult(
					new Intent(DamageCodeEnter.this, DamageCodeListSecond.class).putExtra("check", "type"), REQ_CODE_TYPE);
		}
		else if (getIntent().getStringExtra("check").equals("svrty")) {
			startActivityForResult(
					new Intent(DamageCodeEnter.this, DamageCodeListSecond.class).putExtra("check", "svrty"), REQ_CODE_SVRTY);
		}
		else if (getIntent().getStringExtra("check").equals("pos")) {

			Intent posIntent =new Intent(DamageCodeEnter.this, DamageCodeListSecond.class);

			posIntent.putExtras(getIntent().getExtras());

			startActivityForResult(posIntent
					, REQ_CODE_POS);
		}
	}

	public void back(View v)
	{
        Intent i = new Intent();
		CommonUtility.logButtonClick(log, "Back");
        setResult(RESULT_CANCELED, i);
		finish();
	}


	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		
		Intent i = new Intent();
		
		if (resultCode == RESULT_OK) {
		    if (requestCode == REQ_CODE_POS) {
	          i.putExtra("id", data.getStringExtra("id"));
		    }
		    else {
                i.putExtra("id", data.getStringExtra("id") + "," + getIntent().getStringExtra("mode"));
		    }
	        setResult(resultCode, i);
	        finish();
		}
		if (resultCode == RESULT_CANCELED) {
		    // position *must* be selected from list
		    if (requestCode == REQ_CODE_POS) {
		        setResult(RESULT_CANCELED);
		        finish();
            }
        }
	}

	@Override
	public boolean onKey(View v, int keyCode, KeyEvent event) {

		if(first) {
			log.debug(Logs.DEBUG, "Pressed a key ("+keyCode+"), clearing the text box");
			
			code.setText("");
			first = false;
		} else {
			log.debug(Logs.DEBUG, "Pressed a key, but we already cleared...");
		}
		
		return false;
	}
	
}
