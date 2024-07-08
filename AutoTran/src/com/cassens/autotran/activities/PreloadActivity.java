package com.cassens.autotran.activities;

import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.backendpoc.PoCPerformanceStats;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleStopwatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

/**
 * Project : AUTOTRAN Description : PreloadActivity class show load information
 * of driver
 * 
 * @author Hemant Creation Date : 12-11-2013
 */
public class PreloadActivity extends VINListSelectActivity {
    private static final Logger log = LoggerFactory.getLogger(PreloadActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	private static final String PRELOADED = " -- PRELOADED";
	private static final String UPLOADED = " -- UPLOADED";
	private static final String IMAGES_UPLOADING = " -- IMAGES UPLOADING";
	private static final String NOT_UPLOADED = " -- NOT UPLOADED";

	private int mLoadLimit = AppSetting.DISPLAYED_LOADS_MAX.getInt();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		infoIconEnabled = false;
		((TextView) findViewById(R.id.titleTextView)).setText(getResources()
				.getString(R.string.preload_inspection));
	}

	private String formatDate(String datetime) {
		SimpleDateFormat iFormat = new SimpleDateFormat("yyyy-MM-dd");
		SimpleDateFormat oFormat = new SimpleDateFormat("M/d/yyyy");
		try {
			return oFormat.format(iFormat.parse(datetime.substring(0, 10)));
		} catch (Exception e) {
			return "";
		}
	}

	@Override
	protected void populateSelectionList(HashMap<String, ArrayList<SelectionListElement>> selectionList, int driver_id)
	{
		log.debug(Logs.INTERACTION, "Showing preload selection list");
        super.populateSelectionList(selectionList, driver_id);
        
        // we are deleting loads that haven't been preloaded for a week
        Calendar cal = GregorianCalendar.getInstance();
		cal.add(Calendar.DAY_OF_YEAR, -7);
		Date oneWeekAgo = cal.getTime();
		SimpleStopwatch stopwatch = new SimpleStopwatch();
            
        // We are looking at a list of loads that need preloading
		stopwatch.startTimer();
		ArrayList<Load> loadList = (ArrayList<Load>) DataManager.getStandardLoadsLazy(this, driver_id, mLoadLimit);
		Collections.sort(loadList);
		Collections.reverse(loadList);
        int completedLoadCount = 0;
		int notCompletedLoadCount = 0;

        for (Load load: loadList) {
            List<TrainingRequirement> trainingRequirements = DataManager.getTrainingRequirementsForLoad(this, load.load_remote_id);
            long[] trainingRequirementIds = new long[trainingRequirements.size()];

            if(trainingRequirements.size() > 0) {
                TrainingRequirement.ByStatus filtered = TrainingRequirement.filterList(trainingRequirements);
                log.debug(Logs.DEBUG, "Load " + load.load_id + " has " + trainingRequirements.size() + " training requirement(s), " +
                        + filtered.finished.size() + "/" + filtered.unfinished.size() + " finished/unfinished");
                for(int i = 0; i < trainingRequirementIds.length; i++) {
                    trainingRequirementIds[i] = trainingRequirements.get(i).id;
                }
            }

			//ignore parent loads
			if(!load.parentLoad) {

				SelectionListElement selectionListElement = new SelectionListElement();
				selectionListElement.trainingRequirementIds = trainingRequirementIds;
				selectionListElement.primaryTextLine = load.loadNumber + " - " + load.loadType;
				if (load.isFillerLoadType()) {
					selectionListElement.primaryTextLine += " " + Constants.FILLER_LOAD_STRING;
				}

				String dealerList = "";

				for (Delivery delivery : load.deliveries) {
					if (delivery.dealer != null && delivery.dealer.customer_name != null) {
						dealerList += delivery.dealer.getDealerDisplayName() + " | ";
					}
				}

				if (dealerList.length() >= 2) {
					dealerList = dealerList.substring(0, dealerList.length() - 2);
				}
				if (load.originLoad) {
					selectionListElement.secondaryTextLine = "Relay via " + load.deliveries.get(0).dealer.getDealerDisplayName();
				}
				else if (load.relayLoad) {
					selectionListElement.secondaryTextLine = "Relay to " + dealerList;
				}
				else {
					selectionListElement.secondaryTextLine = dealerList;
				}

				selectionListElement.lookupKey = Integer.toString(load.load_id);
				selectionListElement.enabled = true;
				if (load.driverPreLoadSignature != null) {
					//we have signed off on this preload and it should no longer be available
					selectionListElement.primaryTextLine += PRELOADED;

					if (load.preloadUploadStatus == Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD) {
						selectionListElement.primaryTextLine += load.preloadImagesUploaded() ? UPLOADED : IMAGES_UPLOADING;

						if (!HelperFuncs.isNullOrEmpty(load.getNotes()) || (load.getImages() != null && load.getImages().size() > 0)) {
							selectionListElement.primaryTextLine += getResources().getString(R.string.supplemental_notes_uploaded).toString();
						}
					} else {
						selectionListElement.primaryTextLine += NOT_UPLOADED;
					}

					if (!HelperFuncs.isNullOrEmpty(load.driverPreLoadSignatureSignedAt)) {
						selectionListElement.primaryTextLine += " " + formatDate(load.driverPreLoadSignatureSignedAt);
					}

					selectionListElement.state = SelectionListElement.DAMAGE_COMPLETE;
					selectionList.get(INSPECTION_COMPLETED).add(selectionListElement);
					completedLoadCount++;
				} else {
					if (!load.isInspected()) {
						selectionListElement.state = SelectionListElement.UNINSPECTED_VINS_REMAINING;
						//log.debug(Logs.INTERACTION, selectionListElement.primaryTextLine + " " + Integer.toString(selectionListElement.state));
					} else {
						selectionListElement.state = SelectionListElement.AWAITING_DRIVER_SIGNATURE;
						//log.debug(Logs.INTERACTION, selectionListElement.primaryTextLine + " " + Integer.toString(selectionListElement.state));
					}
					selectionList.get(INSPECTION_UNCOMPLETED).add(selectionListElement);
					if (notCompletedLoadCount++ == 0) {
						log.debug(Logs.INTERACTION, "Selection list has the following uncompleted loads:");
					}
					String extra = "";
					if (!HelperFuncs.isNullOrWhitespace(load.originLoadNumber)) {
						extra = " relay";
					}
					else if (!HelperFuncs.isNullOrWhitespace(load.relayLoadNumber)) {
						extra = " origin";
					}
					log.debug(Logs.INTERACTION, String.format("Load %s - %s%s (remoteId=%s)", load.loadNumber, load.loadType, extra, HelperFuncs.noNull(load.load_remote_id, "?")));
				}
			}
		}
		stopwatch.stopTimer();
		PoCPerformanceStats.recordPreloadQueryTime(stopwatch.getElapsedTime());
		if (notCompletedLoadCount == 0) {
			log.debug(Logs.INTERACTION, "Selection list has 0 uncompleted load(s)");
		}
		log.debug(Logs.INTERACTION, String.format("Selection list has %d completed load(s)", completedLoadCount));
	}

	@Override
	protected void onItemSelected(final SelectionListElement curSelectionListElement) {
		super.onItemSelected(curSelectionListElement);
		log.debug(Logs.INTERACTION,
				"Selecting load " + curSelectionListElement.primaryTextLine);

		if (curSelectionListElement.state == SelectionListElement.DAMAGE_COMPLETE) {
			startSupplementalNotes(Integer.parseInt(curSelectionListElement.lookupKey));
		} else {
			startInspection(curSelectionListElement.lookupKey, curSelectionListElement.trainingRequirementIds);
		}
	}

	private void startSupplementalNotes(int lookupKey) {
		Intent notesIntent = new Intent(PreloadActivity.this,
				SupplementalNotesActivity.class);
		notesIntent.putExtra(Constants.CURRENT_LOOKUP_ID, lookupKey);
		notesIntent.putExtra(Constants.CURRENT_OPERATION, Constants.PRELOAD_OPERATION);
		startActivity(notesIntent);
	}

	protected void startInspection(String lookupKey) {
		startInspection(lookupKey, new long[]{});
	}

	protected void startInspection(String lookupKey, long[] trainingRequirementIds) {
        Intent vinListIntent = new Intent(PreloadActivity.this,
                DeliveryVinInspectionActivity.class);
        vinListIntent.putExtra(Constants.CURRENT_LOOKUP_ID, lookupKey);
        vinListIntent.putExtra(Constants.CURRENT_OPERATION,
                Constants.PRELOAD_OPERATION);
        vinListIntent.putExtra(Constants.EXTRA_TRAINING_REQ_IDS, trainingRequirementIds);
        vinListIntent.putExtra("user_id", this.driver_id);
        startActivity(vinListIntent);
    }


	@Override
	public void onBackPressed() {
		log.debug(Logs.INTERACTION, "Back pressed");
		super.onBackPressed();
	}
}
