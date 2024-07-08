package com.cassens.autotran.activities;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressLint("UseSparseArrays")
public class CheckScannerActivity extends GenericScanningActivity {
  private static final Logger log = LoggerFactory.getLogger(CheckScannerActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

  private static final String TAG = "CheckScannerActivity";
  
  private Button mButtonTrigger;
  
  @Override
  protected void onCreate(Bundle savedInstanceState)
  {
      super.onCreate(savedInstanceState);
      setContentView(R.layout.activity_check_scanner);
  }


  @Override
  protected void onResume() {
	  super.onResume();
	  
	  mButtonTrigger = findViewById(R.id.btnscan);
	  mButtonTrigger.setVisibility(View.VISIBLE);
	  mButtonTrigger.setOnClickListener(v -> startScan());
  }

  @Override
  protected void onScanResultRunOnUiThread(String barcode) {
      log.debug(Logs.DEBUG, barcode);
      String vin;
      if (barcode.length() < 8) {
          vin = barcode;
      } else {
          vin = CommonUtility.processScannedVIN(barcode);
      }
      ((EditText)findViewById(R.id.editText_enterVinNumber)).setText(vin);
      log.debug(Logs.DEBUG, ((EditText) findViewById(R.id.editText_enterVinNumber)).getText().toString());

      if (CommonUtility.checkVin(CheckScannerActivity.this, vin)) {
          CommonUtility.showText("Valid VIN");
      }
  }

  public void ok(View v) {
    finish();
  }
  
  public void back(View v) {
      finish();
  }
}
