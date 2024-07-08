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
import com.cassens.autotran.data.model.ReceivedVehicle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;


public class ReceiveVehicleActivity extends VINSelectActivity
{
    private static final Logger log = LoggerFactory.getLogger(ReceiveVehicleActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

    protected List<ReceivedVehicle> receivedVehicleList;
    ReceivedVehicleAdapter adapter;

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
	
	// Adapter for handling VINs for ReceivedVehicle record 
	public class ReceivedVehicleAdapter extends ArrayAdapter<ReceivedVehicle>
	{
	    private Context context;
	    private int layoutResourceId;
	    private List<ReceivedVehicle> receivedVehicleList;

	    public ReceivedVehicleAdapter(Context context, int textViewResourceId, List<ReceivedVehicle> objects)
	    {
	        super(context, textViewResourceId, objects);
	        this.context = context;
	        this.layoutResourceId = textViewResourceId;
	        this.receivedVehicleList = objects;
	    }

	    @Override
	    public View getView(final int position, View convertView, ViewGroup parent)
	    {
	        View row = convertView;
	        TextView vinView, vinStatus;
	        ImageView infoIcon;
	        LinearLayout vinNumArea;
	        RelativeLayout itemLayout;

	        final ReceivedVehicle thisReceivedVehicle = this.receivedVehicleList.get(position);
	        
	        if (row == null)
	        {
	            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
	            row = inflater.inflate(layoutResourceId, parent, false);
	        }
	        

	        vinView = (TextView) row.findViewById(R.id.ACTIVITY_TITLE);
	        vinView.setText(thisReceivedVehicle.VIN);
	        vinStatus = (TextView) row.findViewById(R.id.ACTIVITY_PROMPT);
	        vinNumArea = (LinearLayout) row.findViewById(R.id.LinearLayout1);
	        itemLayout = (RelativeLayout) row.findViewById(R.id.RelativeLayout1);

	        vinNumArea.setOnClickListener(new OnClickListener()
	        {
	            @Override
	            public void onClick(View v)
	            {
	                ((ReceiveVehicleActivity)context).onVINSelected(thisReceivedVehicle.VIN, false);
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
	        return this.receivedVehicleList.size();
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
 
        receivedVehicleList = DataManager.getReceivedVehicleList(this, false);
        screenTitle.setText(getResources().getString(R.string.receive_vehicle));

        if (displayMode == DisplayMode.VIN_PICK_MODE) {
            adapter = new ReceivedVehicleAdapter(this, R.layout.vin_list_element, receivedVehicleList);
            listView.setAdapter(adapter);
        }
        else log.debug(Logs.DEBUG, "displayMode=VIN_GET_MODE");   

	}
    
    private boolean vinNumberInReceivedVehicleList(List<ReceivedVehicle> receivedVehicleList, String vinNumber)
    {
        Iterator<ReceivedVehicle> itr = receivedVehicleList.iterator();
        
        while (itr.hasNext()) {
            ReceivedVehicle receivedVehicle = (ReceivedVehicle)itr.next();
            if (receivedVehicle.VIN.equals(vinNumber)) {
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
            bundle.putInt("inventory_type", VINInventoryActivity.RECEIVE_VEHICLE);
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
    log.debug(Logs.DEBUG, "ReceiveVehicleActivity: onActivityResult requestCode=" + requestCode 
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
