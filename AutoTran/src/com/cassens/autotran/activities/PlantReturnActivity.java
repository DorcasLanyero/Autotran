package com.cassens.autotran.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.PlantReturn;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;


public class PlantReturnActivity extends VINSelectActivity
{
    private static final Logger log = LoggerFactory.getLogger(PlantReturnActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

    protected List<PlantReturn> plantReturnList;
    PlantReturnAdapter adapter;

	private Bundle bundle;
    
    // Request Codes for Launched Activities
    private static final int REQ_CODE_VIN_INVENTORY = 1001;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		bundle = getIntent().getExtras();
	}

	@Override
	protected void onResume()
	{
	    super.onResume();
	}
	
	// Adapter for handling VINs for PlantReturn record 
	public class PlantReturnAdapter extends ArrayAdapter<PlantReturn>
	{
	    private Context context;
	    private int layoutResourceId;
	    private List<PlantReturn> plantReturnList;

	    public PlantReturnAdapter(Context context, int textViewResourceId, List<PlantReturn> objects)
	    {
	        super(context, textViewResourceId, objects);
	        this.context = context;
	        this.layoutResourceId = textViewResourceId;
	        this.plantReturnList = objects;
	    }

	    @Override
	    public View getView(final int position, View convertView, ViewGroup parent)
	    {
	        View row = convertView;
	        TextView vinView, vinStatus;
	        ImageView infoIcon;
	        LinearLayout vinNumArea;
	        RelativeLayout itemLayout;

	        final PlantReturn thisPlantReturn = this.plantReturnList.get(position);
	        
	        if (row == null)
	        {
	            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
	            row = inflater.inflate(layoutResourceId, parent, false);
	        }
	        

	        vinView = (TextView) row.findViewById(R.id.ACTIVITY_TITLE);
	        vinView.setText(thisPlantReturn.VIN);
	        vinStatus = (TextView) row.findViewById(R.id.ACTIVITY_PROMPT);
	        vinNumArea = (LinearLayout) row.findViewById(R.id.LinearLayout1);
	        itemLayout = (RelativeLayout) row.findViewById(R.id.RelativeLayout1);

	        vinNumArea.setOnClickListener(new OnClickListener()
	        {
	            @Override
	            public void onClick(View v)
	            {
	                ((PlantReturnActivity)context).onVINSelected(thisPlantReturn.VIN, false);
	            }
	    
	        });
	        
	        infoIcon = (ImageView) row.findViewById(R.id.imageView1);
	        infoIcon.setVisibility(View.GONE);

	        // TODO: Display a status here (inspected or not; uploaded or not)
	        vinStatus.setVisibility(View.GONE);
	        
	        return row;
	    }
	    
	    @Override
	    public int getCount()
	    {
	        return this.plantReturnList.size();
	    }
	}

	@Override
	protected void drawLayout()
	{
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
        
        ((ImageView) findViewById(R.id.imageView4)).setVisibility(View.GONE);
        reviewButton.setVisibility(View.GONE);
        proceedButton.setVisibility(View.GONE);
 
        plantReturnList = DataManager.getPlantReturnList(this, false);
        screenTitle.setText(getResources().getString(R.string.return_to_plant));

        if (displayMode == DisplayMode.VIN_PICK_MODE) {
            adapter = new PlantReturnAdapter(this, R.layout.vin_list_element, plantReturnList);
            listView.setAdapter(adapter);
        }
        else log.debug(Logs.DEBUG, "displayMode=VIN_GET_MODE");   

	}
    
    private boolean vinNumberInPlantReturnList(List<PlantReturn> plantReturnList, String vinNumber)
    {
        Iterator<PlantReturn> itr = plantReturnList.iterator();
        
        while (itr.hasNext()) {
            PlantReturn plantReturn = (PlantReturn)itr.next();
            if (plantReturn.VIN.equals(vinNumber)) {
                return true;
            }
        }
        return false;
    }

	@Override
	public void onVINSelected(String vinNumber, boolean vinScanned)
	{
	    super.onVINSelected(vinNumber, vinScanned);
	    
        try
        {                  
            bundle.putString("vin_number", vinNumber);
            bundle.putInt("inventory_type", VINInventoryActivity.PLANT_RETURN);
            bundle.putBoolean("is_editable", !reviewOnly);

            //disconnectScanner();
            if (reviewOnly) {
                Intent tmpIntent = new Intent(this, VINInventoryActivity.class).putExtras(bundle);
                startActivity(tmpIntent);
            }
            else {
                Intent tmpIntent = new Intent(this, VINInventoryActivity.class).putExtras(bundle);
                startActivityForResult(tmpIntent, REQ_CODE_VIN_INVENTORY);
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data)
    {
        super.onActivityResult(requestCode, resultCode, data);
    log.debug(Logs.DEBUG, "PlantReturnActivity: onActivityResult requestCode=" + requestCode
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
