package com.cassens.autotran.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.model.YardInventory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;


public class YardInventoryActivity extends VINSelectActivity {
    private static final Logger log = LoggerFactory.getLogger(YardInventoryActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    protected List<YardInventory> yardInventoryList;

	private Bundle bundle;
	private boolean lotLocate;
    Button manualButton;
    
    // Request Codes for Launched Activities
    private static final int REQ_CODE_VIN_INVENTORY = 1001;

	@Override
	protected void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
		bundle = getIntent().getExtras();
		lotLocate = bundle.getBoolean("lot_locate", false);
	}

	@Override
	protected void onResume()	{
	    super.onResume();
        manualButton = findViewById(R.id.manualButton);
        manualButton.setVisibility(View.VISIBLE);
        manualButton.setOnClickListener(view -> {
            scanEntryDialog(true);

            /*
            AlertDialog.Builder builder = new AlertDialog.Builder(YardInventoryActivity.this);
            builder.setTitle("Enter VIN");
            final EditText input = new EditText(YardInventoryActivity.this);
            builder.setView(input);
            builder.setPositiveButton("OK", (dialog, which) -> {
                String vin = input.getText().toString();
                if(vin.length() >= 10) {
                    onVINSelected(vin, false);
                } else {
                    CommonUtility.showText("Invalid VIN " + (vin != null ? vin : ""));
                }

            });
            builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());
            builder.show(); */
        });
	}


	@Override
	protected void drawLayout()	{
	    super.drawLayout();

        vinPickLayout.setVisibility(View.GONE);
        vinGetLayout.setVisibility(View.VISIBLE);
        
        findViewById(R.id.imageView4).setVisibility(View.GONE);
        reviewButton.setVisibility(View.GONE);
        proceedButton.setVisibility(View.GONE);
        
        if (lotLocate) {
            screenTitle.setText(getResources().getString(R.string.lot_locate));
        }
        else {
			screenTitle.setText(getResources().getString(R.string.yard_inventory));
		}
	}


	@Override
	public void onVINSelected(String vinNumber, boolean vinScanned)	{
	    super.onVINSelected(vinNumber, vinScanned);
	    
        try {
            bundle.putString("vin_number", vinNumber);
            if (lotLocate) {
                bundle.putInt("inventory_type", VINInventoryActivity.LOT_LOCATE);
            }
            else {
                bundle.putInt("inventory_type", VINInventoryActivity.YARD_INVENTORY);          
            }
            bundle.putBoolean("is_editable", true);

            Intent tmpIntent = new Intent(this, VINInventoryActivity.class).putExtras(bundle);
            startActivityForResult(tmpIntent, REQ_CODE_VIN_INVENTORY);
        } catch(Exception e) {
            e.printStackTrace();
        }  
	}

	@Override
    public void back(View v)
    {
	    super.back(v);
    }
    
	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        log.debug(Logs.DEBUG, "YardInventoryInspectionActivity: onActivityResult requestCode=" + requestCode
                         + "resultCode=" + resultCode);
    
        switch (requestCode) {

        case REQ_CODE_VIN_INVENTORY:
            if (resultCode == RESULT_OK) {
                displayMode = DisplayMode.VIN_GET_MODE;
            }
            break;
            
        default:
            break;
        }
    }
}
