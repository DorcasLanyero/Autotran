package com.cassens.autotran.activities;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.ShuttleLoadDefaults;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

public class ShuttleSelectLoadActivity extends VINListSelectActivity {
    private static final Logger log = LoggerFactory.getLogger(ShuttleSelectLoadActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private static final String PRELOADED = " -- PRELOADED";
    private static final String UPLOADED = " -- UPLOADED";
    private static final String NOT_UPLOADED = " -- NOT UPLOADED";

    private static final int REQ_CODE_SHUTTLE_LOAD = 1008;
    private boolean allLoadsComplete = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        infoIconEnabled = false;
        ((TextView) findViewById(R.id.titleTextView)).setText("Shuttle Move");

    }

    private boolean allLoadsComplete() {
        return allLoadsComplete;
    }

    @Override
    protected void populateSelectionList(HashMap<String, ArrayList<SelectionListElement>> selectionList, int driver_id) {

        log.debug(Logs.INTERACTION, "populating shuttle load list");
        super.populateSelectionList(selectionList, driver_id);

        ArrayList<Load> loadList = (ArrayList<Load>) DataManager.getLoadsLazy(this, driver_id, true, -1);
        Collections.sort(loadList);
        Collections.reverse(loadList);

        allLoadsComplete = true;

        for (Load load : loadList) {

            if(load.deliveries != null  && load.deliveries.size() > 0) {
                for(Delivery delivery : load.deliveries) {
                    if(delivery.driverSignature == null || delivery.driverSignature.trim().equals("")) {
                        allLoadsComplete = false;
                        break;
                    }
                }
            }

            VINListSelectActivity.SelectionListElement selectionListElement = new VINListSelectActivity.SelectionListElement();
            selectionListElement.primaryTextLine = load.loadNumber;
            selectionListElement.secondaryTextLine = String.format("%s: %s to %s", load.shuttleMove.getTerminal(), load.shuttleMove.getOriginName(), load.shuttleMove.getDestinationName());

            selectionListElement.lookupKey = Integer.toString(load.load_id);
            selectionListElement.enabled = true;
            if (load.driverPreLoadSignature != null) {
                selectionListElement.primaryTextLine += PRELOADED;

                if (load.preloadUploadStatus == Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD)
                    selectionListElement.primaryTextLine += UPLOADED;
                else
                    selectionListElement.primaryTextLine += NOT_UPLOADED;

                selectionListElement.state = VINListSelectActivity.SelectionListElement.DAMAGE_COMPLETE;
                selectionList.get(INSPECTION_COMPLETED).add(selectionListElement);
            } else {
                if (!load.isInspected()) {
                    selectionListElement.state = VINListSelectActivity.SelectionListElement.UNINSPECTED_VINS_REMAINING;
                } else {
                    selectionListElement.state = VINListSelectActivity.SelectionListElement.AWAITING_DRIVER_SIGNATURE;
                }
                selectionList.get(INSPECTION_UNCOMPLETED).add(selectionListElement);
            }
        }


        Button newShuttleMoveButton = (Button) findViewById(R.id.newShuttleMoveButton);

        if(allLoadsComplete()) {
            newShuttleMoveButton.setVisibility(View.VISIBLE);
            newShuttleMoveButton.setOnClickListener(new View.OnClickListener() {

                @Override
                public void onClick(View v) {

                    Intent intent = new Intent(ShuttleSelectLoadActivity.this, ShuttleCreateLoadActivity.class);
                    intent.putExtras(getIntent().getExtras());

                    ShuttleLoadDefaults defaults = ShuttleLoadDefaults.get(ShuttleSelectLoadActivity.this);
                    if (defaults.numVehicles == 0) {
                        // This is our first shuttle load after installing app
                        intent.putExtra("startWithDefaults", false);
                        startActivityForResult(intent, REQ_CODE_SHUTTLE_LOAD);
                    }
                    else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(ShuttleSelectLoadActivity.this);
                        StringBuilder stringBuilder = new StringBuilder("Previous configuration:\n\n");
                        stringBuilder.append(String.format("Terminal: %s\n\n", HelperFuncs.noNull(defaults.terminalId)));
                        stringBuilder.append(String.format("Origin: %s\n\n", HelperFuncs.noNull(defaults.originText)));
                        stringBuilder.append(String.format("Destination: %s\n\n", HelperFuncs.noNull(defaults.destinationText)));
                        stringBuilder.append(String.format("# Vehicles: %d\n\n", defaults.numVehicles));
                        stringBuilder.append(String.format("How would you like to set up your load?", defaults.originText));
                        builder.setMessage(stringBuilder.toString());
                        builder.setPositiveButton("Same as Previous", (dialog, which) -> {
                            intent.putExtra("startWithDefaults", true);
                            startActivityForResult(intent, REQ_CODE_SHUTTLE_LOAD);
                        });

                        builder.setNegativeButton("Set up as New", (dialog, which) -> {
                            intent.putExtra("startWithDefaults", false);
                            startActivityForResult(intent, REQ_CODE_SHUTTLE_LOAD);
                        });

                        builder.setCancelable(true);
                        builder.create().show();
                    }
                }
            });
        } else  {
            newShuttleMoveButton.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onItemSelected(
            final VINListSelectActivity.SelectionListElement curSelectionListElement) {
        super.onItemSelected(curSelectionListElement);
        log.debug(Logs.INTERACTION,
                "Selecting load " + curSelectionListElement.primaryTextLine);

        if (curSelectionListElement.state == SelectionListElement.DAMAGE_COMPLETE) {
            startSupplementalNotes(Integer.parseInt(curSelectionListElement.lookupKey));
        } else {
            startInspection(Integer.parseInt(curSelectionListElement.lookupKey));
        }
    }

    private void startSupplementalNotes(int lookupKey) {
        Intent notesIntent = new Intent(ShuttleSelectLoadActivity.this,
                SupplementalNotesActivity.class);
        notesIntent.putExtra(Constants.CURRENT_LOOKUP_ID, lookupKey);
        notesIntent.putExtra(Constants.CURRENT_OPERATION, Constants.PRELOAD_OPERATION);
        startActivity(notesIntent);
    }

    protected void startInspection(int lookupKey) {
        Intent vinListIntent = new Intent(ShuttleSelectLoadActivity.this,
                ShuttleBuildLoadActivity.class);
        vinListIntent.putExtra(Constants.CURRENT_LOOKUP_ID, lookupKey);
        vinListIntent.putExtra(Constants.CURRENT_OPERATION,
                Constants.PRELOAD_OPERATION);
        startActivity(vinListIntent);
    }
}

