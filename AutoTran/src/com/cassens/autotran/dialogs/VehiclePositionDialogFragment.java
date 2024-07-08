package com.cassens.autotran.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.views.DrivenBackedButton;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;


/**
 * Used to edit vehicle position and driven/backed
 * 
 * @author Adam
 */
@SuppressLint({"NewApi", "ValidFragment"})
public class VehiclePositionDialogFragment extends DialogFragment implements OnItemSelectedListener {
    private static final Logger log = LoggerFactory.getLogger(VehiclePositionDialogFragment.class.getSimpleName());

    List<String> mOpts;
    private int delivery_vin_id;
	private Spinner spinnerPosition;
	private DrivenBackedButton rbg;
    private String newPosition = null;
    private Button clearButton;
    private boolean allowClear;

    public void saveDeliveryVin() {
        saveDeliveryVin(delivery_vin_id);
    }
    public void saveDeliveryVin(int delivery_vin_id) {
        Context context = getContext().getApplicationContext();
        DeliveryVin dv = DataManager.getDeliveryVin(context, delivery_vin_id);

        if(dv != null) {
            dv.backdrv = rbg.getOrientation().getValue();

            if (newPosition.contains("--")) {
                dv.position = null;
            } else {
                dv.position = newPosition;
            }

            DataManager.insertDeliveryVinToLocalDB(context, dv);
        }

        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.DELIVERY_VIN_CHANGED));
    }

    public void clearDeliveryVinPosition() {
        Context context = getContext().getApplicationContext();
        DeliveryVin dv = DataManager.getDeliveryVin(context, delivery_vin_id);
        int loadID = DataManager.getLoadIdForDeliveryVin(context,dv);
        Load load = DataManager.getLoad(context, loadID);
        newPosition = "--";

        saveDeliveryVin();

    }

    public String getNewPosition() {
        return newPosition;
    }

    public boolean getAllowClear() {
        return allowClear;
    }

	private DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            if(!newPosition.contains("--")) {
                log.debug(Logs.INTERACTION, "Load position selected: " + newPosition);
                saveDeliveryVin();
            } else if (newPosition.contains("--") && allowClear) {
                log.debug(Logs.INTERACTION, "Load position cleared");
                clearDeliveryVinPosition();
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage("Please choose a valid position");
                alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface subdialog, int which) {
                            subdialog.dismiss();
                        }
                    });
                alertDialog.show();
            }
        }
    };

    private DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            CommonUtility.logButtonClick(log, "Cancel");
        }
    };

    public static VehiclePositionDialogFragment newInstance(int delivery_vin_id, ArrayList<String> opts, boolean allowClear){
        VehiclePositionDialogFragment frag = new VehiclePositionDialogFragment();
		Bundle args = new Bundle();
		args.putInt("delivery_vin_id", delivery_vin_id);
        args.putStringArrayList("opts", opts);
        args.putBoolean("allow_clear", allowClear);
		frag.setArguments(args);

		return frag;
    }

/*
    @Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.dialog_vehicle_position, container, false);
    }
*/

    public VehiclePositionDialogFragment() {
    }

    /*
	@SuppressLint("DefaultLocale")
	public VehiclePositionDialogFragment(final DeliveryVin dv, List<String> opts, Context context, ViewGroup parent) {
    	this.context = context;
    	LayoutInflater inflater = ((Activity) this.context).getLayoutInflater();
        this.layout = inflater.inflate(layoutId, parent, false);

        ((TextView) this.layout.findViewById(R.id.vinNumber)).setText("vin: " + dv.vin.vin_number);
        this.spinnerPosition = (Spinner) this.layout.findViewById(R.id.spinnerTruckPosition);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this.context, android.R.layout.simple_spinner_item, opts);
        this.mOpts = opts;
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinnerPosition.setAdapter(adapter);
        int spinnerIndex = opts.indexOf(dv.position);
        this.spinnerPosition.setSelection(spinnerIndex != -1 ? spinnerIndex : 0);
        this.spinnerPosition.setOnItemSelectedListener(this);
        
        this.rbg = (DrivenBackedButton) this.layout.findViewById(R.id.drivenBackedToggle);

        this.rbg.setOrientation(dv.backdrv);
        this.rbg.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
				dv.backdrv = ((DrivenBackedButton) v).getOrientation().getValue();
			}
		});
    }*/
    
    public void setPositiveClickListener(DialogInterface.OnClickListener newClickListener) {
    	this.positiveListener = newClickListener;
    }

    public void onItemSelected(AdapterView<?> parent, View view, int pos, long id) {
        newPosition = mOpts.get(pos);
        //clearButton.setEnabled(!newPosition.equals("--"));
    }

    public void onNothingSelected(AdapterView<?> parent) {}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle(getResources().getString(R.string.title_dialog_truck_position))
               .setPositiveButton("Save", this.positiveListener)
        	   .setNegativeButton("Cancel", this.negativeListener);

        View view = getActivity().getLayoutInflater().inflate(R.layout.dialog_vehicle_position, null, false);

        Context context = view.getContext();

        /*
        if(savedInstanceState != null) {
            setArguments(savedInstanceState);
        }
        */

        final DeliveryVin dv = DataManager.getDeliveryVin(context, getArguments().getInt("delivery_vin_id"));
        this.delivery_vin_id = dv.delivery_vin_id;

        ((TextView) view.findViewById(R.id.vinNumber)).setText("VIN: " + dv.vin.vin_number);

        this.spinnerPosition = (Spinner) view.findViewById(R.id.spinnerTruckPosition);

        ArrayList<String> opts = getArguments().getStringArrayList("opts");
        this.allowClear = getArguments().getBoolean("allow_clear");

        ArrayAdapter<String> adapter = new ArrayAdapter<>(context, android.R.layout.simple_spinner_item, opts);
        this.mOpts = opts;
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinnerPosition.setAdapter(adapter);
        int spinnerIndex = opts.indexOf(dv.position);
        this.spinnerPosition.setSelection(spinnerIndex != -1 ? spinnerIndex : 0);
        this.spinnerPosition.setOnItemSelectedListener(this);

        clearButton = view.findViewById(R.id.clearPosition);
        if (allowClear) {

            clearButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    spinnerPosition.setSelection(0);
                }
            });
        }
        else {
            clearButton.setVisibility(View.GONE);
        }

        this.rbg = (DrivenBackedButton) view.findViewById(R.id.drivenBackedToggle);

        this.rbg.setOrientation(dv.backdrv);
        this.rbg.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {
                dv.backdrv = ((DrivenBackedButton) v).getOrientation().getValue();
            }
        });

        builder.setView(view);

        final AlertDialog dialog = builder.create();
        log.debug(Logs.INTERACTION, "Displaying VIN Position dialog");
        return dialog;
    }

}
