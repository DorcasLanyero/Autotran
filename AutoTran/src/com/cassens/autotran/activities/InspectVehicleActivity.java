package com.cassens.autotran.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Inspection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;


public class InspectVehicleActivity extends VINSelectActivity {
    private static final Logger log = LoggerFactory.getLogger(InspectVehicleActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    protected List<Inspection> inspectionList;
    InspectionAdapter adapter;

    Button manualButton;
    EditText enterVinEditText;
    EditText input;

	private Bundle bundle;
    
    // Request Codes for Launched Activities
    private static final int REQ_CODE_VEHICLE_INSPECTION = 1001;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		bundle = getIntent().getExtras();
	}

	@Override
	protected void onResume() {
	    super.onResume();
		enterVinEditText = findViewById(R.id.enterVinEditText);
	    manualButton = findViewById(R.id.manualButton);
	    manualButton.setVisibility(View.VISIBLE);
	    manualButton.setOnClickListener(view -> {
	    	scanEntryDialog(true);

	    	/*
			AlertDialog.Builder builder = new AlertDialog.Builder(InspectVehicleActivity.this);
			builder.setTitle("Enter VIN");
			input = new EditText(InspectVehicleActivity.this);
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
	
	// Adapter for handling VINs for Inspection record 
	public class InspectionAdapter extends ArrayAdapter<Inspection>	{
	    private Context context;
	    private int layoutResourceId;
	    private List<Inspection> inspectionList;

	    public InspectionAdapter(Context context, int textViewResourceId, List<Inspection> objects) {
	        super(context, textViewResourceId, objects);
	        this.context = context;
	        this.layoutResourceId = textViewResourceId;
	        this.inspectionList = objects;
	    }

	    @Override
	    public View getView(final int position, View convertView, ViewGroup parent) {
	        View row = convertView;
	        TextView vinView, vinStatus;
	        ImageView infoIcon;
	        LinearLayout vinNumArea;

	        final Inspection thisInspection = this.inspectionList.get(position);
	        
	        if (row == null) {
	            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
	            row = inflater.inflate(layoutResourceId, parent, false);
	        }
	        

	        vinView = row.findViewById(R.id.ACTIVITY_TITLE);
	        vinView.setText(thisInspection.vin);
	        vinStatus = row.findViewById(R.id.ACTIVITY_PROMPT);
	        vinNumArea = row.findViewById(R.id.LinearLayout1);

	        vinNumArea.setOnClickListener(v -> ((InspectVehicleActivity)context).onVINSelected(thisInspection.vin, false));

	        infoIcon = row.findViewById(R.id.imageView1);
	        infoIcon.setVisibility(View.GONE);

	        // TODO: Display a status here (inspected or not; uploaded or not)
	        vinStatus.setVisibility(View.GONE);
	        
	        return row;
	    }
	    
	    @Override
	    public int getCount()
	    {
	        return this.inspectionList.size();
	    }
	}

	@Override
	protected void drawLayout()	{
	    super.drawLayout();
        reviewOnly = false;

        if (displayMode == DisplayMode.VIN_PICK_MODE) {
            vinGetLayout.setVisibility(View.GONE);
            vinPickLayout.setVisibility(View.VISIBLE);
        }
        else {
            vinPickLayout.setVisibility(View.GONE);
            vinGetLayout.setVisibility(View.VISIBLE);
        }
        
        findViewById(R.id.imageView4).setVisibility(View.GONE);
        reviewButton.setVisibility(View.GONE);
        proceedButton.setVisibility(View.GONE);
 
        inspectionList = DataManager.getInspectionList(this, false);
        screenTitle.setText(getResources().getString(R.string.inspect_vehicle));

        if (displayMode == DisplayMode.VIN_PICK_MODE) {
            adapter = new InspectionAdapter(this, R.layout.vin_list_element, inspectionList);
            listView.setAdapter(adapter);
        }
        else log.debug(Logs.DEBUG, "displayMode=VIN_GET_MODE");   

	}
    
    private boolean vinNumberInInspectionList(List<Inspection> inspectionList, String vinNumber) {
        Iterator<Inspection> itr = inspectionList.iterator();
        
        while (itr.hasNext()) {
            Inspection inspection = itr.next();
            if (inspection.vin.equals(vinNumber)) {
                return true;
            }
        }
        return false;
    }

	@Override
	public void onVINSelected(String vinNumber, boolean vinScanned) {
	    super.onVINSelected(vinNumber, vinScanned);

        try {
            reviewOnly = vinNumberInInspectionList(inspectionList, vinNumber);
            
            Intent intent = new Intent();
            intent.putExtra("vin_number", vinNumber);
            intent.putExtra(Constants.CURRENT_OPERATION, Constants.INSPECT_VEHICLE_OPERATION);
          
            //disconnectScanner();
            
            if (reviewOnly) {
                intent.putExtra("is_editable", false);
                Intent tmpIntent = new Intent(this, VehicleInspectionActivity.class).putExtras(intent.getExtras());
                startActivity(tmpIntent);
            }
            else {
                Intent tmpIntent = new Intent(this, VehicleInspectionActivity.class).putExtras(intent.getExtras());
                startActivityForResult(tmpIntent, REQ_CODE_VEHICLE_INSPECTION);
            }
        }
        catch(Exception e)
        {
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
    log.debug(Logs.DEBUG, "InspectionActivity: onActivityResult requestCode=" + requestCode
			+ "resultCode=" + resultCode);
    
        switch (requestCode) {

        case REQ_CODE_VEHICLE_INSPECTION:
            if (resultCode == RESULT_OK) {
                displayMode = DisplayMode.VIN_GET_MODE;
            }
            break;
            
        default:
            break;
        }
    }
}
