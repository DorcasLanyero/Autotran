package com.cassens.autotran.activities;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.PlantReturn;
import com.cassens.autotran.data.model.ReceivedVehicle;
import com.cassens.autotran.data.model.YardExit;
import com.cassens.autotran.data.model.YardInventory;
import com.cassens.autotran.data.model.lookup.ScacCode;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.handlers.LocationHandler;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;


/**m
 * Project : AUTOTRAN Description : VINInventoryActivity class record results of inventory
 *
 * @author Philip Knight
 */
public class VINInventoryActivity extends AutoTranActivity 
{
    private static final Logger log = LoggerFactory.getLogger(VINInventoryActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

	TextView titleTextView;
    String vinNumber;
	String employeeNumber;
    Button saveButton;
    ImageView backButton;
	TextView vinTextView;
	TextView employeeNumTextView; 
	Button inboundButton, outboundButton;
	
	TextView terminalTextView;
    TextView lotCodeTextView;
    EditText delayCode;
    
    String[] rowValues = { "none" };   
    EditText rowTextView;
    
    String[] bayValues = { "none" }; 
    EditText bayTextView;   
        

	private Bundle bundle;
    boolean isEditable = true;

	// Subclass
	
	private int inventoryType;
	public static final int YARD_INVENTORY = 1;
    public static final int LOT_LOCATE = 2;
    public static final int YARD_ENTRY_EXIT = 3;
    public static final int PLANT_RETURN = 4;
    public static final int RECEIVE_VEHICLE = 5;
	        
	private YardInventory yardInventory;
	private YardExit yardEntryExit;
	private PlantReturn plantReturn;
	private ReceivedVehicle receivedVehicle;

    // Request Codes for Launched Activities
    private static final int REQ_CODE_TERMINAL_NUM = 1001;
    private static final int REQ_CODE_LOT_NUM = 1002;
    private static final int REQ_CODE_SCAC_NUM = 1003;

    private boolean inbound = false;
    private int deliveryVinId;

    private LocationHandler locationHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

        locationHandler = LocationHandler.getInstance(getApplicationContext());
        locationHandler.startLocationTracking();

        drawLayout();
        initializeInventoryRecord();
	}
	
	@Override
	protected void onResume()
	{
	    super.onResume();
	}
	
	@SuppressLint("NewApi")
	public void drawLayout()
	{
        bundle = getIntent().getExtras();
        isEditable = bundle.getBoolean("is_editable", true);
        vinNumber = bundle.getString("vin_number", "");
        inventoryType = bundle.getInt("inventory_type");
    
        employeeNumber = bundle.getString("driverNumber", "");
        deliveryVinId = bundle.getInt("delivery_vin_id", -1);
        
        setContentView(R.layout.activity_vin_inventory);
 
        titleTextView = (TextView) findViewById(R.id.title);
        saveButton = (Button) findViewById(R.id.save);
        backButton = (ImageView) findViewById(R.id.back);
        inboundButton = (Button) findViewById(R.id.inbound_button);
        outboundButton = (Button) findViewById(R.id.outbound_button);
    
        vinTextView = (TextView) findViewById(R.id.vin);
        vinTextView.setText(HelperFuncs.splitVin(vinNumber));
        employeeNumTextView = (TextView) findViewById(R.id.employee_num);
        employeeNumTextView.setText(employeeNumber);
        
        terminalTextView = (TextView) findViewById(R.id.terminal);
        terminalTextView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                Intent i = new Intent(VINInventoryActivity.this, TerminalCodeList.class);
                startActivityForResult(i, REQ_CODE_TERMINAL_NUM);
            }
        });
        
        lotCodeTextView = (TextView) findViewById(R.id.lot);
 
        rowTextView = (EditText) findViewById(R.id.row);
        rowTextView.setFilters(Constants.ALL_CAPS_INPUT_FILTER);
    
        bayTextView = (EditText) findViewById(R.id.bay);
        bayTextView.setFilters(Constants.ALL_CAPS_INPUT_FILTER);
	}
	
    private void initializeYardInventoryRecord()
    {
        // Subclass
        if (inventoryType == LOT_LOCATE) {
            titleTextView.setText(getResources().getString(R.string.lot_locate));
        }
        else {
            titleTextView.setText(getResources().getString(R.string.yard_inventory));
        }
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);

        yardInventory = new YardInventory();
        yardInventory.VIN = new String(vinNumber);
        yardInventory.inspector = new String(employeeNumber);
        yardInventory.terminal = DataManager.getTerminal(this, CommonUtility.getDefaultTerminalNum(this));

        if (yardInventory.terminal != null) {
            refreshTerminalDependentFields(yardInventory.terminal.terminal_id);
            rowValues = new String[yardInventory.terminal.rowCharacters.size()];
            yardInventory.terminal.rowCharacters.toArray(rowValues);
            bayValues = new String[yardInventory.terminal.bayCharacters.size()];
            yardInventory.terminal.bayCharacters.toArray(bayValues);
        }
        terminalTextView.setEnabled(isEditable);

        String lotCode = prefs.getString("default_lot_code", "");
        if(!HelperFuncs.isNullOrEmpty(lotCode)) {
            yardInventory.lotCode = DataManager.getLotCode(this, lotCode, yardInventory.terminal.terminal_id);
        }
        if (yardInventory.lotCode != null && yardInventory.lotCode.code != null) {
            lotCodeTextView.setText(String.valueOf(yardInventory.lotCode.code));
        }
        
        lotCodeTextView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                Intent  intent = new Intent(VINInventoryActivity.this, LotCodeList.class);
                intent.putExtra("title", lotCodeTextView.getText().toString());
                intent.putExtra("terminal_id", Integer.parseInt(terminalTextView.getText().toString()));
                startActivityForResult(intent, REQ_CODE_LOT_NUM);
            }
        });
        lotCodeTextView.setEnabled(isEditable);

        //lotCodeTextView.setText(yardInventory.lotCode.code);

        //populateSpinnerAdapter(rowSpinner, rowValues);
        String val = "";
        if (yardInventory.row != null && !yardInventory.row.equals("")) {
            val = yardInventory.row;
        }
        rowTextView.setText(val); 
        rowTextView.setEnabled(isEditable);
        
        //populateSpinnerAdapter(baySpinner, bayValues);
        val = "";
        if (yardInventory.bay != null && !yardInventory.bay.equals("")) {
            val = yardInventory.bay;
        }       
        bayTextView.setText(val);       
        bayTextView.setEnabled(isEditable);
    }   


    private YardExit getYardExit(String vinNumber)
    {
        List <YardExit> yardEntryExitList = DataManager.getYardExitList(this, false);
        Iterator<YardExit> itr = yardEntryExitList.iterator();

        while (itr.hasNext()) {
            YardExit yardEntryExit = (YardExit)itr.next();
            if (yardEntryExit.VIN.equals(vinNumber)) {
                return yardEntryExit;
            }
        }
        return null;
    }

    private void setInboundOutboundButtons(boolean inbound)
    {
        inboundButton.setBackgroundResource(inbound
                                        ? R.drawable.button_small_pressed
                                        : R.drawable.button_small);
        outboundButton.setBackgroundResource(inbound
                ? R.drawable.button_small
                : R.drawable.button_small_pressed);
    }

    public void inboundClick (View v) {
        inbound = true;
        setInboundOutboundButtons(inbound);
    }

    public void outboundClick (View v) {
        inbound = false;
        setInboundOutboundButtons(inbound);
    }

    private void initializeYardEntryExitRecord()
    {

        titleTextView.setText(getResources().getString(R.string.yard_entry_exit));

        yardEntryExit = getYardExit(vinNumber);

        if (yardEntryExit == null) {
            yardEntryExit = new YardExit();
            yardEntryExit.VIN = new String(vinNumber);
            yardEntryExit.inspector = new String(employeeNumber);
            yardEntryExit.inbound = false;
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            yardEntryExit.terminal = DataManager.getTerminal(this, CommonUtility.getDefaultTerminalNum(this));
        }
        if (yardEntryExit.terminal != null) {
            refreshTerminalDependentFields(yardEntryExit.terminal.terminal_id);
        }
        terminalTextView.setEnabled(isEditable);

        setInboundOutboundButtons(yardEntryExit.inbound);
        ((LinearLayout)findViewById(R.id.ll_inbound)).setVisibility(View.VISIBLE);

        // Use Lot Code field for SCAC code
        ((TextView) findViewById(R.id.lot_header)).setText("SCAC Code");
        ((TextView) findViewById(R.id.lot)).setHint("enter SCAC code");
        if (yardEntryExit.scacCode != null) {
            lotCodeTextView.setText(String.valueOf(yardEntryExit.scacCode.getCode()));
        }

        lotCodeTextView.setOnClickListener(new OnClickListener()
        {
            @Override
            public void onClick(View arg0)
            {
                Intent  intent = new Intent(VINInventoryActivity.this, ScacCodeList.class);
                intent.putExtra("title", lotCodeTextView.getText().toString());
                intent.putExtra("terminal_id", Integer.parseInt(terminalTextView.getText().toString()));
                startActivityForResult(intent, REQ_CODE_SCAC_NUM);
            }
        });
        lotCodeTextView.setEnabled(isEditable);

        ((LinearLayout) findViewById(R.id.ll_second)).setVisibility(View.GONE);
        ((LinearLayout) findViewById(R.id.ll_second_header)).setVisibility(View.GONE);

        saveButton.setVisibility(isEditable ? View.VISIBLE : View.GONE);
    }
  
    private PlantReturn getPlantReturn(String vinNumber)
    {
        List <PlantReturn> plantReturnList = DataManager.getPlantReturnList(this, false);
        Iterator<PlantReturn> itr = plantReturnList.iterator();
        
        while (itr.hasNext()) {
            PlantReturn plantReturn = (PlantReturn)itr.next();
            if (plantReturn.VIN.equals(vinNumber)) {
                return plantReturn;
            }
        }
        return null;
    }
    
    private void initializePlantReturnRecord()
    {
       
        titleTextView.setText(getResources().getString(R.string.return_to_plant));
 
        plantReturn = getPlantReturn(vinNumber);
    
        if (plantReturn == null) {
            plantReturn = new PlantReturn();
            plantReturn.VIN = new String(vinNumber);
            plantReturn.inspector = new String(employeeNumber);
            plantReturn.delayCode = "";
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            plantReturn.terminal = DataManager.getTerminal(this, CommonUtility.getDefaultTerminalNum(this));
        }
        if (plantReturn.terminal != null) {
            terminalTextView.setText(String.valueOf(plantReturn.terminal.terminal_id));
        }
        terminalTextView.setEnabled(isEditable);
         
        // Use delayCode EditText field instead of lot TextView, but use the lot header.
        delayCode = (EditText)findViewById(R.id.delayCode);
        delayCode.setVisibility(View.VISIBLE);
        lotCodeTextView.setVisibility(View.GONE);
        ((TextView) findViewById(R.id.lot_header)).setText("Delay Code");
        if (plantReturn.delayCode != null) {
            delayCode.setText(String.valueOf(plantReturn.delayCode));
        }
        delayCode.setEnabled(isEditable);    
        
        ((LinearLayout) findViewById(R.id.ll_second)).setVisibility(View.GONE);        
        ((LinearLayout) findViewById(R.id.ll_second_header)).setVisibility(View.GONE);

        saveButton.setVisibility(isEditable ? View.VISIBLE : View.GONE);
    }
    
    
    private ReceivedVehicle getReceivedVehicle(String vinNumber)
    {
        List <ReceivedVehicle> receivedVehicleList = DataManager.getReceivedVehicleList(this, false);
        Iterator<ReceivedVehicle> itr = receivedVehicleList.iterator();
        
        while (itr.hasNext()) {
            ReceivedVehicle receivedVehicle = (ReceivedVehicle)itr.next();
            if (receivedVehicle.VIN.equals(vinNumber)) {
                return receivedVehicle;
            }
        }
        return null;
    }
    
    private void initializeReceivedVehicleRecord()
    {
       
        titleTextView.setText(getResources().getString(R.string.receive_vehicle));
 
        receivedVehicle = getReceivedVehicle(vinNumber);
    
        if (receivedVehicle == null) {
            receivedVehicle = new ReceivedVehicle();
            receivedVehicle.VIN = new String(vinNumber);
            receivedVehicle.inspector = new String(employeeNumber);
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            receivedVehicle.terminal = DataManager.getTerminal(this, CommonUtility.getDefaultTerminalNum(this));
        }
        if (receivedVehicle.terminal != null) {
            terminalTextView.setText(String.valueOf(receivedVehicle.terminal.terminal_id));
        }
        terminalTextView.setEnabled(isEditable);
         
        // Use delayCode EditText field instead of lot TextView, but use the lot header.
        ((EditText)findViewById(R.id.delayCode)).setVisibility(View.GONE);
        lotCodeTextView.setVisibility(View.GONE);
        ((TextView) findViewById(R.id.lot_header)).setVisibility(View.GONE);  
        
        ((LinearLayout) findViewById(R.id.ll_second)).setVisibility(View.GONE);        
        ((LinearLayout) findViewById(R.id.ll_second_header)).setVisibility(View.GONE);

        saveButton.setVisibility(isEditable ? View.VISIBLE : View.GONE);
    }
    
    
    private void initializeInventoryRecord()
    {
        switch (inventoryType) {
        
        case YARD_INVENTORY:
        case LOT_LOCATE:
            initializeYardInventoryRecord();
        break;
        
        case YARD_ENTRY_EXIT:
            initializeYardEntryExitRecord();
        break;
        
        case PLANT_RETURN:
            initializePlantReturnRecord();
        break;
        
        case RECEIVE_VEHICLE:
            initializeReceivedVehicleRecord();
        break;

        default:
        break;
        }
    }

	private void showDialog(String msg)
	{
	  showDialog(msg, false);
	}
	
	private void showDialog(String msg, boolean cancelable)
	{
        Builder builder = new AlertDialog.Builder(VINInventoryActivity.this);
        
        builder.setMessage(msg);
        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {

        	@Override
        	public void onClick(DialogInterface dialog, int which) {
        		// TODO Auto-generated method stub
        	}
        });
        builder.setCancelable(cancelable);
        builder.create().show();
    }

	public void rowClick(View v) {
	  
	}
	
	public void bayClick(View v) {
	      
	}
	
	private void doneYardInventory()
	{
        if (CommonUtility.isNullOrBlank(lotCodeTextView.getText().toString())
            || CommonUtility.isNullOrBlank(rowTextView.getText().toString())
            || CommonUtility.isNullOrBlank(bayTextView.getText().toString()))
        {
            showDialog("All fields must be completed.");
            return;
        }
        
        // terminal and lotCode should already be set.
    
        yardInventory.terminal =  DataManager.getTerminal(this, Integer.parseInt((String)terminalTextView.getText()));
        yardInventory.lotCode = DataManager.getLotCode(this, (String)lotCodeTextView.getText(), yardInventory.terminal.terminal_id);

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("default_lot_code", (String)lotCodeTextView.getText());
        editor.commit();


        yardInventory.row = rowTextView.getText().toString();
        yardInventory.bay = bayTextView.getText().toString();
        Location currentLocation = locationHandler.getLocation();
        yardInventory.latitude = currentLocation.getLatitude();
        yardInventory.longitude = currentLocation.getLongitude();

        yardInventory.lotLocate = (inventoryType == LOT_LOCATE);
        yardInventory.uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
        yardInventory.delivery_vin_id = deliveryVinId;
    
        log.debug(Logs.DEBUG, "Saving yardInventory record");
    
        DataManager.insertYardInventory(this, yardInventory);

        CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from doneYardInventory");
        SyncManager.pushLocalDataToRemoteServer(getApplicationContext(), CommonUtility.getDriverNumberAsInt(getApplicationContext()),false);

        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
	}

    
    private void doneYardEntryExit()
    {
             
        // terminal and lotCode should already be set.
    
        yardEntryExit.terminal =  DataManager.getTerminal(this, Integer.parseInt((String) terminalTextView.getText()));
        yardEntryExit.scacCode = DataManager.getScacCode(this,  yardEntryExit.terminal.terminal_id, ((String)lotCodeTextView.getText()));
        yardEntryExit.inbound = inbound;
        
        // vin and inspector should already be set.
       
        log.debug(Logs.DEBUG, "Saving yardExit record");
    
        DataManager.insertYardExit(this, yardEntryExit);
    
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }
    
    private void donePlantReturn()
    {
             
        // terminal and lotCode should already be set.
    
        plantReturn.terminal =  DataManager.getTerminal(this, Integer.parseInt((String) terminalTextView.getText()));
        plantReturn.delayCode = delayCode.getText().toString();
        
        // vin and inspector should already be set.
       
        log.debug(Logs.DEBUG, "Saving yardExit record");
    
        DataManager.insertPlantReturn(this, plantReturn);
    
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }
    
    private void doneReceiveVehicle()
    {
             
        // terminal and lotCode should already be set.
    
        receivedVehicle.terminal =  DataManager.getTerminal(this, Integer.parseInt((String)terminalTextView.getText()));
        
        // vin and inspector should already be set.
       
        log.debug(Logs.DEBUG, "Saving yardExit record");
    
        DataManager.insertReceivedVehicle(this, receivedVehicle);
    
        Intent returnIntent = new Intent();
        setResult(RESULT_OK, returnIntent);
        finish();
    }
    
    
	public void saveClick(View v)
	{
        CommonUtility.logButtonClick(log, v);

        switch (inventoryType) {
        
        case YARD_INVENTORY:
        case LOT_LOCATE:
            doneYardInventory();
        break;
        
        case YARD_ENTRY_EXIT:
            doneYardEntryExit();
        break;
        
        case PLANT_RETURN:
            donePlantReturn();
        break;
        
        case RECEIVE_VEHICLE:
            doneReceiveVehicle();
        break;
    
        default:
        break;
        }

	}
	
    public void back(View v)
    {
        if (!isEditable) {
            finish();
            return;
        }

        Builder builder = new AlertDialog.Builder(VINInventoryActivity.this);
        //builder.setTitle("Notification");
        builder.setMessage("Do you wish to save any current changes, or discard and lose unsaved progress?");
        builder.setPositiveButton("Save", (dialog, which) -> saveClick(v));
        builder.setNegativeButton("Discard", (dialog, which) -> finish());
        builder.setNeutralButton("Cancel", null);
        //builder.setCancelable(true);
        builder.create().show();
    }
    
    @Override
    public void onBackPressed()
    {
        back(null);
    }

    public void terminalClick(View v)
    {
        //
    }

    public void lotClick(View v)
    {
        //
    }


	@Override
	protected void onPause() {
		// TODO Auto-generated method stub
		super.onPause();
	}

    @Override
    protected void onDestroy() {
        locationHandler.stopLocationTracking();
        super.onDestroy();
    }

	private void refreshTerminalDependentFields(int terminal_id)
	{
	    if (terminalTextView.getText().toString().equalsIgnoreCase(String.valueOf(terminal_id))) {
	        return;
	    }
        terminalTextView.setText(String.valueOf(terminal_id));
        lotCodeTextView.setText("");
        if (inventoryType == YARD_ENTRY_EXIT) {
            List<ScacCode> scacCodeList = (List<ScacCode>) DataManager.getScacCodeList(this, terminal_id);
            if (scacCodeList.size() > 0) {
                ((TextView) findViewById(R.id.lot_header)).setVisibility(View.VISIBLE);
                lotCodeTextView.setVisibility(View.VISIBLE);
            }
            else {
                ((TextView) findViewById(R.id.lot_header)).setVisibility(View.INVISIBLE);
                lotCodeTextView.setVisibility(View.INVISIBLE);
            }
        }
        rowTextView.setText("");
        bayTextView.setText("");
	}

	@SuppressLint("NewApi")
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent incomingIntent)
	{
		super.onActivityResult(requestCode, resultCode, incomingIntent);

        if (resultCode != RESULT_OK) {
            return;
        }
        
		switch (requestCode) {

        case REQ_CODE_TERMINAL_NUM:
            log.debug(Logs.DEBUG, "new terminal code=" + incomingIntent.getStringExtra("code"));
            refreshTerminalDependentFields(Integer.parseInt(incomingIntent.getStringExtra("code")));
            break;
            
        case REQ_CODE_LOT_NUM:
        case REQ_CODE_SCAC_NUM:
            lotCodeTextView.setText(incomingIntent.getStringExtra("code"));
            break;
            
		default:
			break;

		}
	}


    /*
     * Called when the Activity becomes visible.
     */
    @Override
    protected void onStart() {
        super.onStart();
    }

    /*
     * Called when the Activity is no longer visible.
     */
    @Override
    protected void onStop() {
        super.onStop();
    }

}
