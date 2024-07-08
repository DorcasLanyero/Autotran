package com.cassens.autotran.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.GlobalState;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.backendpoc.PoCPerformanceStats;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.User;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleStopwatch;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Project : AUTOTRAN Description : DeliveryActivity class show delivery information of driver
 *
 * @author Hemant Creation Date : 12-11-2013
 */
public class DeliveryActivity extends VINListSelectActivity {
    private static final Logger log = LoggerFactory.getLogger(DeliveryActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private static final String DELIVERED = " -- DELIVERED";
    private static final String UPLOADED = " -- UPLOADED";
    private static final String IMAGES_UPLOADING = " -- IMAGES UPLOADING";
    private static final String NOT_UPLOADED = " -- NOT UPLOADED";
    private boolean isHighClaimsDriver;
    private int mNumIncompleteLoads = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        infoIconEnabled = true;
        ((TextView) findViewById(R.id.titleTextView)).setText(getResources().getString(R.string.delivery_inspection));

        User driver = DataManager.getUserForDriverNumber(this, CommonUtility.getDriverNumber(this));
        isHighClaimsDriver = (driver == null) ? false : (driver.highClaims != 0);
    }

    private String formatDate(String datetime) {
        SimpleDateFormat iFormat = new SimpleDateFormat("yyyy-MM-dd");
        SimpleDateFormat oFormat = new SimpleDateFormat("M/d/yyyy");
        try {
            return oFormat.format(iFormat.parse(datetime.substring(0, 10)));
        } catch (ParseException e) {
            return "";
        }
    }

    @Override
    public void onBackPressed() {
        log.debug(Logs.INTERACTION, "Back pressed");
        super.onBackPressed();
    }

    @Override
    protected void populateSelectionList(HashMap<String, ArrayList<SelectionListElement>> selectionList, int driver_id) {
        log.debug(Logs.INTERACTION, "populating delivery list");
        SimpleStopwatch stopwatch = new SimpleStopwatch();

        super.populateSelectionList(selectionList, driver_id);

        stopwatch.startTimer();
        ArrayList<Delivery> deliveryList = (ArrayList<Delivery>) DataManager.getAllDeliveriesLazy(this,
                driver_id,
                20);
        Collections.sort(deliveryList);

        log.debug(Logs.INTERACTION, "found " + deliveryList.size() + " deliveries");

        ArrayList<Load> loadList = new ArrayList<Load>();
        ArrayList<Integer> validDeliveryIds = new ArrayList<Integer>();
        ArrayList<Integer> validLoadIds = new ArrayList<Integer>();


        for (Delivery delivery : deliveryList) {

            //Load thisLoad = DataManager.getLoad(this, delivery.load_id);
            Load thisLoad = delivery.load;

            log.debug(Logs.DEBUG, "RelayLoad = " + thisLoad.relayLoad + " OriginLoad = " + thisLoad.originLoad + ", ldtyp: " + thisLoad.loadType);
            if (thisLoad == null) {
                log.debug(Logs.DEBUG, "load was null");
                continue;
            } else if (thisLoad.driverPreLoadSignature == null) {
                //log.debug(Logs.DEBUG, "load " + thisLoad.loadNumber + " did not have a preload signature");
                continue; // skip deliveries that haven't been preloaded yet.c
            } else if ((thisLoad.isPreTwoDotFourOriginLoad() || thisLoad.relayLoad) && thisLoad.isAutoDeliverLoadType()) {
                //log.debug(Logs.DEBUG, "load " + thisLoad.loadNumber + " is a relay load ");
                continue;
            } else if (thisLoad.parent_load_id != -1 && thisLoad.parent_load_id != 0){
                //log.debug(Logs.DEBUG, "load " + thisLoad.loadNumber + " is a child load for an inspection group");
                continue;
            } else {
                if (delivery.dealer != null) {
                    log.debug(Logs.DEBUG, "Adding delivery " + delivery.dealer.getDealerDisplayName() + " for load " + thisLoad.loadNumber);
                }
            }

            validDeliveryIds.add(delivery.delivery_id);


            if (!validLoadIds.contains(thisLoad.load_id)) {
                loadList.add(thisLoad);
                validLoadIds.add(thisLoad.load_id);
            }
        }

        Collections.sort(loadList);
        Collections.reverse(loadList);
        Load lastStartedLoad = null;
        mNumIncompleteLoads = 0;

        for (Load load : loadList) {

            List<Delivery> deliveries = load.deliveries;
            Collections.sort(deliveries);

            boolean loadIsComplete = true;

            for (Delivery delivery : load.deliveries) {
                if (validDeliveryIds.indexOf(delivery.delivery_id) == -1) {
                    log.debug(Logs.INTERACTION, "did not find delivery for " + delivery.dealer.customer_name);
                    continue;
                }

                SelectionListElement selectionListElement = new SelectionListElement();
                selectionListElement.lookupKey = Integer.toString(delivery.delivery_id);

                selectionListElement.enabled = true;
                if (infoIconEnabled && load.shuttleLoad) {
                    selectionListElement.showInfoIcon = false;
                }
                else {
                    selectionListElement.showInfoIcon = true;
                }

                if (delivery.dealer != null && delivery.dealer.hasUpdatedFields()){
                    selectionListElement.alert = true; // JUNK do this in preload also
                }

                if (!HelperFuncs.isNullOrEmpty(load.loadType)) {
                    if (load.shuttleLoad) {
                        if (load.shuttleMove == null) {
                            selectionListElement.primaryTextLine = "Load: " + load.loadNumber;
                        }
                        else {
                            selectionListElement.primaryTextLine = load.shuttleMove.terminal + ": " + load.shuttleMove.getDestinationName();
                        }
                    } else {
                        selectionListElement.primaryTextLine = delivery.getDealer().getDealerDisplayName();
                    }
                } else {
                    selectionListElement.primaryTextLine = delivery.getDealer().getDealerDisplayName();
                }
                log.debug(Logs.INTERACTION, selectionListElement.primaryTextLine);

                selectionListElement.secondaryTextLine = "load #" + load.loadNumber;
                if (load.isFillerLoadType()) {
                    selectionListElement.secondaryTextLine += " " + Constants.FILLER_LOAD_STRING;
                }
                selectionListElement.lookupKey = Integer.toString(delivery.delivery_id);
                log.debug(Logs.DEBUG, "customer=" + selectionListElement.primaryTextLine + " enabled=" + selectionListElement.enabled);

                log.debug(Logs.INTERACTION, selectionListElement.secondaryTextLine);

                if ((delivery.shuttleLoad || delivery.dealerSignature != null) && delivery.driverSignature != null) {
                    selectionListElement.primaryTextLine += DELIVERED;

                    if (delivery.deliveryUploadStatus == Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY) {
                        selectionListElement.primaryTextLine += delivery.deliveryImagesUploaded() ? UPLOADED : IMAGES_UPLOADING;

                        if (!HelperFuncs.isNullOrEmpty(delivery.getNotes()) || (delivery.getImages() != null && delivery.getImages().size() > 0)) {
                            selectionListElement.primaryTextLine += getResources().getString(R.string.supplemental_notes_uploaded).toString();
                        }
                    } else {
                        selectionListElement.primaryTextLine += NOT_UPLOADED;
                    }

                    if (!HelperFuncs.isNullOrEmpty(delivery.driverSignatureSignedAt)) {
                        selectionListElement.primaryTextLine += " " + formatDate(delivery.driverSignatureSignedAt);
                    }

                    selectionListElement.state = SelectionListElement.DAMAGE_COMPLETE;
                    selectionList.get(INSPECTION_COMPLETED).add(selectionListElement);
                } else {
                    if (!delivery.isInspected(isHighClaimsDriver)) {
                        selectionListElement.state = SelectionListElement.UNINSPECTED_VINS_REMAINING;
                    } else if (delivery.dealerSignature == null) {
                        selectionListElement.state = SelectionListElement.AWAITING_DEALER_SIGNATURE;
                    } else {
                        selectionListElement.state = SelectionListElement.AWAITING_DRIVER_SIGNATURE;
                    }
                    selectionList.get(INSPECTION_UNCOMPLETED).add(selectionListElement);
                    loadIsComplete = false;
                }
            }
            if (!loadIsComplete) {
                mNumIncompleteLoads++;
                if (lastStartedLoad == null || SimpleTimeStamp.timeStampStringToTimeInMillis(load.driverPreLoadSignatureSignedAt)
                    > SimpleTimeStamp.timeStampStringToTimeInMillis(lastStartedLoad.driverPreLoadSignatureSignedAt)) {
                    lastStartedLoad = load;
                }
            }
        }
        stopwatch.stopTimer();
        PoCPerformanceStats.recordDeliveryQueryTime(stopwatch.getElapsedTime());

        if (lastStartedLoad == null) {
            GlobalState.clearLastStartedLoadInfo();
        }
        else {
            GlobalState.setLastStartedLoadInfo(lastStartedLoad.loadNumber, lastStartedLoad.truckNumber);
        }
    }

    @Override
    protected void onInfoIconClick(int deliveryID) {
        super.onInfoIconClick(deliveryID);

        Delivery delivery = DataManager.getDelivery(this, deliveryID);
        if (delivery.dealer != null) {
            Intent intent = new Intent(this, DealerDetailsActivity.class).putExtra("delivery_id", deliveryID);
            this.startActivity(intent);
        } else {
            CommonUtility.showText("This delivery is a shuttle load and does not have a dealer associated with it.");
            log.debug(Logs.INTERACTION, "This delivery is a shuttle load and does not have a dealer associated with it.");
        }
    }

    @Override
    protected void onItemSelected(SelectionListElement curSelectionListElement) {

        super.onItemSelected(curSelectionListElement);

        log.debug(Logs.INTERACTION, "selected Item " + curSelectionListElement.lookupKey + " - " + curSelectionListElement.primaryTextLine);

        if (curSelectionListElement.state == SelectionListElement.DAMAGE_COMPLETE) {
            startDamageSummary(Integer.parseInt(curSelectionListElement.lookupKey));
        } else {
            Intent vinListIntent = new Intent(DeliveryActivity.this, DeliveryVinInspectionActivity.class);
            vinListIntent.putExtra(Constants.CURRENT_LOOKUP_ID, curSelectionListElement.lookupKey);
            vinListIntent.putExtra(Constants.CURRENT_OPERATION, Constants.DELIVERY_OPERATION);
            vinListIntent.putExtra(DeliveryVinInspectionActivity.EXTRA_IS_LAST_INCOMPLETE_LOAD, mNumIncompleteLoads == 1);
            vinListIntent.putExtra("user_id", this.driver_id);

            startActivity(vinListIntent);
        }
    }

    private void startDamageSummary(int lookupKey) {
        Intent intent = new Intent(DeliveryActivity.this,
                DamageSummaryActivity.class);
        intent.putExtra(Constants.CURRENT_LOOKUP_ID, lookupKey);
        intent.putExtra(Constants.CURRENT_OPERATION, Constants.DELIVERY_OPERATION);
        startActivity(intent);
    }

    public void backButton(View v) {
        log.debug(Logs.INTERACTION, "Back button pressed");
        this.finish();
    }

}
