package com.cassens.autotran.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.VIN;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VINDetailsActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(VINDetailsActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

	private VIN thisVIN;
	private DeliveryVin thisDeliveryVin;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_vin_details);
		String s;
		
        int deliveryVinID = getIntent().getIntExtra("delivery_vin_id", -1);
        int operation = getIntent().getIntExtra(Constants.CURRENT_OPERATION, Constants.DELIVERY_OPERATION);
        thisDeliveryVin = DataManager.getDeliveryVin(this, deliveryVinID);
        thisVIN = thisDeliveryVin.vin;

        Button infoButton = findViewById(R.id.btn_vin_info);
		infoButton.setOnClickListener((OnClickListener) v -> {
            CommonUtility.logButtonClick(log, v);
                startActivity(new Intent(VINDetailsActivity.this, DealerDetailsActivity.class)
                        .putExtra("delivery_id", thisDeliveryVin.delivery_id).putExtra(Constants.CURRENT_OPERATION, operation));});
        ((TextView)findViewById(R.id.txt_vin_id)).setText(thisVIN.vin_number);

        //((TextView)findViewById(R.id.txt_vin_status)).setText(thisVIN.status);
        //((TextView)findViewById(R.id.txt_vin_load)).setText(thisVIN.ldnbr);

        s = thisVIN.color;
        if (!isNull(thisVIN.colordes)) {
            s += " (" + thisVIN.colordes + ")";
        }

        ((TextView)findViewById(R.id.txt_vin_color)).setText(s);
        ((TextView)findViewById(R.id.txt_vin_body)).setText(thisVIN.body);
        ((TextView)findViewById(R.id.txt_vin_weight)).setText(thisVIN.weight);
        ((TextView)findViewById(R.id.txt_vin_type)).setText(thisVIN.type);
        TextView tv = ((TextView)findViewById(R.id.txt_vin_location));

        if (operation == Constants.DELIVERY_OPERATION) {
            tv.setVisibility(View.GONE);
            ((TextView)findViewById(R.id.label_vin_location)).setVisibility(View.GONE);
        }
        else {
            tv.setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.label_vin_location)).setVisibility(View.VISIBLE);
            ((TextView)findViewById(R.id.txt_vin_location)).setText(noNull(thisDeliveryVin.lot) +
                    "-" + noNull(thisDeliveryVin.rowbay));
        }
	}
	
    private String noNull(String s)
    {
        return ((s == null || s.equalsIgnoreCase("null")) ? "" : s);
    }
    
    private boolean isNull(String s)
    {
        return (noNull(s).length() == 0);
    }
    
    public void back(View v)
    {
        finish();
    }    
}
