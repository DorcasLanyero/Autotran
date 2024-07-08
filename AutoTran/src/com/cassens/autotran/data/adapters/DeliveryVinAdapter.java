package com.cassens.autotran.data.adapters;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.R;
import com.cassens.autotran.activities.VINDetailsActivity;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.DeliveryVin;
import com.sdgsystems.util.HelperFuncs;

import java.util.List;

// Adapter for handling DeliveryVin record for Preload and Delivery activities
public class DeliveryVinAdapter extends ArrayAdapter<DeliveryVin>
{
	private Context context;
	private int layoutResourceId;
	private List<DeliveryVin> deliveryVinList;
	public int operation;

	public interface DeliveryVinAdapterCallback {
		public void onSelect(DeliveryVin deliveryVin);
	}
	private DeliveryVinAdapterCallback mCallback;

	public DeliveryVinAdapter(Context context, int textViewResourceId, List<DeliveryVin> objects, int operation, DeliveryVinAdapterCallback callback)
	{
		super(context, textViewResourceId, objects);
		this.context = context;
		this.layoutResourceId = textViewResourceId;
		this.deliveryVinList = objects;
		this.operation = operation;
		this.mCallback = callback;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		TextView vinView, dealerName;
		final RelativeLayout itemLayout;
		Button deleteButton;
		LinearLayout truckPosition;

		final DeliveryVinAdapter thisAdapter = this;
		final DeliveryVin thisDeliveryVin = this.deliveryVinList.get(position);
		
		if (row == null)
		{
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
		}

		vinView = (TextView) row.findViewById(R.id.ACTIVITY_TITLE);
		dealerName = (TextView) row.findViewById(R.id.ACTIVITY_PROMPT);
		itemLayout = (RelativeLayout) row.findViewById(R.id.RelativeLayout1);
		deleteButton = (Button) row.findViewById(R.id.deleteButton);

		OnClickListener inspect = new OnClickListener() {
			@Override
            public void onClick(View v)
            {
				CommonUtility.showText("Selected vin " + thisDeliveryVin.vin.vin_number);
				mCallback.onSelect(thisDeliveryVin);
			}
		};
		((LinearLayout) row.findViewById(R.id.LinearLayout1)).setOnClickListener(inspect);

		deleteButton.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View v) {
				Builder builder = new AlertDialog.Builder(context);
				builder.setTitle("Remove?");
				builder.setMessage("Would you like to remove vin " + thisDeliveryVin.vin.vin_number + " from the load?");
				builder.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
						deliveryVinList.remove(position);
                        //thisDeliveryVin.pro = "deleted";
                        //DataManager.insertDeliveryVinToLocalDB(context, thisDeliveryVin, false);
						DataManager.deleteDeliveryVinDataFromDB(context, thisDeliveryVin.delivery_vin_id, "deleted vin from shuttle load");
						LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.DELIVERY_VIN_CHANGED));
					}
				});
				builder.setNegativeButton("No", new DialogInterface.OnClickListener() {
					@Override
					public void onClick(DialogInterface dialog, int which) {
					}
				});
				builder.setCancelable(true);
				builder.show();
			}
		});

		String shuttleLoadVals = String.format("Route: %s -- Production Status: %s", thisDeliveryVin.shuttleLoadRoute, thisDeliveryVin.shuttleLoadProductionStatus);			
		
		String damages = "";
		
		for (Damage damage : thisDeliveryVin.damages){
			if(operation == Constants.DELIVERY_OPERATION && !damage.preLoadDamage) {
				if(damage.specialCode == null)
					damages += damage.areaCode.getCode() + "," + damage.typeCode.getCode() + "," + damage.severityCode.getCode() + "\n";
				if((damage.specialCode != null))
					damages += damage.specialCode.getAreaCode() + "," + damage.specialCode.getTypeCode() + "," + damage.specialCode.getSeverityCode() + "\n";
			} else if(operation == Constants.PRELOAD_OPERATION && damage.preLoadDamage) {
				if(damage.specialCode == null)
					damages += damage.areaCode.getCode() + "," + damage.typeCode.getCode() + "," + damage.severityCode.getCode() + "\n";
				if((damage.specialCode != null))
					damages += damage.specialCode.getAreaCode() + "," + damage.specialCode.getTypeCode() + "," + damage.specialCode.getSeverityCode() + "\n";
			}
		}

		if (!HelperFuncs.isNullOrEmpty(damages)) {
			shuttleLoadVals += "\n" + damages;
		}

		boolean localDamages = false;

		for (Damage damage : thisDeliveryVin.damages) {
			if(damage.source.equals("driver") && !damage.readonly) {
				localDamages = true;
			}
		}

		if(localDamages && thisDeliveryVin.ats != null && !thisDeliveryVin.ats.trim().isEmpty()) {
			shuttleLoadVals += "\nNO SUPERVISOR SIGNATURE";
		}

		dealerName.setText(shuttleLoadVals.trim());

		vinView.setText(HelperFuncs.splitVin(thisDeliveryVin.vin.vin_number));
			
		if ((operation == Constants.DELIVERY_OPERATION && thisDeliveryVin.inspectedDelivery)
				|| ((operation == Constants.PRELOAD_OPERATION || operation == Constants.SHUTTLE_LOAD_OPERATION) && thisDeliveryVin.inspectedPreload)) {
            itemLayout.setBackgroundColor(Color.LTGRAY);
		}
		else {
            itemLayout.setBackgroundColor(Color.WHITE);
		}
		
		return row;
	}
	   
    private void showVinDetails(DeliveryVin deliveryVin)
    {
        Intent intent=new Intent(context, VINDetailsActivity.class);
        intent.putExtra("delivery_vin_id", deliveryVin.delivery_vin_id);
        intent.putExtra("operation", operation);
        ((Activity) context).startActivity(intent);
    }

	public DeliveryVin getDeliveryVinForVin(String vin) {
		for (DeliveryVin deliveryVin : deliveryVinList) {
			if (vin.equalsIgnoreCase(deliveryVin.vin.vin_number)) {
				return deliveryVin;
			}
		}
		return null;
	}
	
	@Override
	public int getCount()
	{
		return this.deliveryVinList.size();
	}
}
