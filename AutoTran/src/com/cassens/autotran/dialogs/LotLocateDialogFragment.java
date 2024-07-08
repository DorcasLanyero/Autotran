package com.cassens.autotran.dialogs;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.YardInventory;
import com.cassens.autotran.data.model.lookup.LotCode;
import com.cassens.autotran.data.model.lookup.Terminal;
import com.sdgsystems.util.HelperFuncs;

import java.util.ArrayList;
import java.util.List;


/**
 * Used to edit vehicle position and driven/backed
 * 
 * @author Adam
 */
@SuppressLint({"NewApi", "ValidFragment"})
public class LotLocateDialogFragment extends DialogFragment implements OnItemSelectedListener {

	private  View layout;
	private  Context context;
    private Location latestLocation;
    private  DeliveryVin dv;

    private EditText row, bay;
    private TextView terminalTv;

    private YardInventory yardInventory;
    private YardInventory originalYardInventory;

    private int inventoryType;
    public static final int LOT_LOCATE = 2;

    private Load thisLoad;
    private Delivery thisDelivery;
    private Spinner spinnerLot;

    List<LotCode> mLotCodes;

    private LotCode newLot = null;

    ViewGroup mParent;

    public LotLocateDialogFragment() {
        setUpLotLocateDialog();
    }

    public LotLocateDialogFragment(DeliveryVin thisDeliveryVin, Load load, Delivery delivery, Context context, ViewGroup parent, Location latestLocation, YardInventory inventory) {
        thisDelivery = delivery;
        thisLoad = load;
        mParent = parent;

        this.dv = thisDeliveryVin;
        this.context = context;
        this.latestLocation = latestLocation;
        originalYardInventory = inventory;

        setUpLotLocateDialog();
    }

    public LotCode getNewLot() {
        return newLot;
    }


    @SuppressLint("DefaultLocale")
    public LotLocateDialogFragment(final DeliveryVin dv, final Load load, Context context,
                                   ViewGroup parent, Location latestLocation, YardInventory inventory) {
        thisLoad = load;
        mParent = parent;

        this.dv = dv;
        this.context = context;
        this.latestLocation = latestLocation;
        originalYardInventory = inventory;
        setUpLotLocateDialog();
    }

    public void setUpLotLocateDialog() {

        LayoutInflater inflater = ((Activity) this.context).getLayoutInflater();
        this.layout = inflater.inflate(R.layout.dialog_lot_locate, mParent, false);

        ((TextView) this.layout.findViewById(R.id.vinNumber)).setText("vin: " + HelperFuncs.splitVin(dv.vin.vin_number));


        terminalTv = layout.findViewById(R.id.terminal_num);
        row = (EditText) layout.findViewById(R.id.row);
        bay = (EditText) layout.findViewById(R.id.bay);

        row.setFilters(new InputFilter[]{filter, new InputFilter.AllCaps()});
        bay.setFilters(new InputFilter[]{filter, new InputFilter.AllCaps()});

        if (yardInventory == null) {
            yardInventory = new YardInventory();
            yardInventory.VIN = dv.vin.vin_number;
            yardInventory.ldnbr = thisLoad.loadNumber;
            yardInventory.delivery_vin_id = dv.delivery_vin_id;
            yardInventory.lotLocate = true;
            yardInventory.inspector = thisLoad.driver.driverNumber;

            if(latestLocation != null) {
                yardInventory.latitude = latestLocation.getLatitude();
                yardInventory.longitude = latestLocation.getLongitude();
            }
        } else {
            yardInventory = originalYardInventory;
        }

        if (originalYardInventory != null) {
            row.setText(originalYardInventory.row);
            bay.setText(originalYardInventory.bay);
        }

        if (thisLoad.shuttleLoad) {
            try {
                yardInventory.terminal = DataManager.getTerminal(context, Integer.parseInt(thisLoad.shuttleMove.getTerminal()));
            } catch (NumberFormatException ex) {
            }
            mLotCodes = DataManager.getLotCodeListForShuttleMove(context, thisLoad.shuttleMove.getTerminal(), thisLoad.shuttleMove.getMoveCode());
        }
        else if (thisDelivery != null){
            LotCode code = DataManager.getLotCode(context, thisDelivery.dealer.lot_code_id);
            if (code != null) {
                yardInventory.terminal = DataManager.getTerminal(context, code.terminal_id);
                if (yardInventory.terminal == null || yardInventory.terminal.description == null) {
                    terminalTv.setText(String.format("(Terminal %s)", code.terminal_id));
                } else {
                    terminalTv.setText(String.format("(Terminal %s - %s)", code.terminal_id, yardInventory.terminal.description));
                }
                mLotCodes = DataManager.getLotCodeListForTerminal(context, code.terminal_id);
            }
        }

        // Defer upload of lot locates until delivery is saved.
        yardInventory.uploadStatus = Constants.SYNC_STATUS_NOT_READY_FOR_UPLOAD;
        setUpLotSpinner();
    }

    private void setUpLotSpinner() {

        List<String> associatedLotCodes = new ArrayList<>();

        for(LotCode lot : mLotCodes) {
            associatedLotCodes.add(lotCodeSpinnerEntry(lot));
        }

        if(mLotCodes.size() == 0) {
            associatedLotCodes.add("Default Lot");

            LotCode tmp = new LotCode();
            tmp.code = "00";
            tmp.description = "Default Lot";
            tmp.lot_code_id = 0;

            mLotCodes.add(tmp);
        }

        this.spinnerLot = (Spinner) this.layout.findViewById(R.id.spinnerLotCode);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this.context, android.R.layout.simple_spinner_item, associatedLotCodes);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        this.spinnerLot.setAdapter(adapter);

        int spinnerIndex = -1;

        if(yardInventory.lotCode != null) {
            spinnerIndex = associatedLotCodes.indexOf(lotCodeSpinnerEntry(yardInventory.lotCode));
        }
        else if (originalYardInventory != null && originalYardInventory.lotCode != null) {
            spinnerIndex = associatedLotCodes.indexOf(lotCodeSpinnerEntry(originalYardInventory.lotCode));
        }
        else if (thisDelivery != null && thisDelivery.dealer != null && thisDelivery.dealer.lot_code_id >= 0) {
            LotCode dealerLotCode = DataManager.getLotCode(context, thisDelivery.dealer.lot_code_id);
            if (dealerLotCode != null) {
                spinnerIndex = associatedLotCodes.indexOf(lotCodeSpinnerEntry(dealerLotCode));
            }
        }

        this.spinnerLot.setSelection(spinnerIndex != -1 ? spinnerIndex : 0);
        this.spinnerLot.setOnItemSelectedListener(this);
    }

    private String lotCodeSpinnerEntry(LotCode lot) {
        return String.format("%s - %s", lot.code, lot.description);
    }

    private DialogInterface.OnClickListener positiveListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            if(validEntries()) {
                save();
            } else {
                AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                alertDialog.setTitle("Alert");
                alertDialog.setMessage("Please provide a valid lot location");
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

    private boolean validEntries() {
        if(row.getText().toString() != null && row.getText().toString().length() > 0) {
            if(bay.getText().toString() != null && bay.getText().toString().length() > 0) {
                if(newLot != null) {
                    return true;
                }
            }
        }

        return false;
    }

    public void save() {
        //Check to see if we have updated from an old inventory to verify

        yardInventory.row = row.getText().toString();
        yardInventory.bay = bay.getText().toString();
        if (newLot != null) {
            yardInventory.lotCode = newLot;
        }
        if(originalYardInventory != null) {
            if(!originalYardInventory.row.equals(yardInventory.row) ||
                !originalYardInventory.bay.equals(yardInventory.bay) ||
                !originalYardInventory.lotCode.codesMatch(yardInventory.lotCode)) {

                if(latestLocation != null) {
                    yardInventory.latitude = latestLocation.getLatitude();
                    yardInventory.longitude = latestLocation.getLongitude();
                }
            }
        }

        if (thisLoad.shuttleMove != null && thisLoad.shuttleMove.getTerminal() != null) {
            try {
                Terminal terminal = DataManager.getTerminal(context, Integer.parseInt(thisLoad.shuttleMove.getTerminal()));
                if (terminal != null) {
                    yardInventory.terminal = terminal;
                }
            } catch (NumberFormatException ex) {
                //invalid terminal number
            }
        }

        DataManager.insertYardInventory(context, yardInventory);

        LocalBroadcastManager.getInstance(context).sendBroadcast(new Intent(Constants.DELIVERY_VIN_CHANGED));
    }

    private DialogInterface.OnClickListener negativeListener = new DialogInterface.OnClickListener() {
        public void onClick(DialogInterface dialog, int id) {
            return;
        }
    };

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        newLot = mLotCodes.get(position);
    }

    public void onNothingSelected(AdapterView<?> parent) {}
	
	@Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setView(this.layout)
        	   .setTitle("Lot Locate Vehicle")
               .setPositiveButton("OK", this.positiveListener)
        	   .setNegativeButton("Cancel", this.negativeListener);

        final AlertDialog dialog = builder.create();

        return dialog;
    }

    InputFilter filter = new InputFilter() {
        public CharSequence filter(CharSequence source, int start, int end,
                                   Spanned dest, int dstart, int dend) {
            // Only keep characters that are alphanumeric
            StringBuilder builder = new StringBuilder();
            for (int i = start; i < end; i++) {
                char c = source.charAt(i);
                if (Character.isLetterOrDigit(c)) {
                    builder.append(c);
                }
            }

            // If all characters are valid, return null, otherwise only return the filtered characters
            boolean allCharactersValid = (builder.length() == end - start);
            return allCharactersValid ? null : builder.toString();
        }
    };

}
