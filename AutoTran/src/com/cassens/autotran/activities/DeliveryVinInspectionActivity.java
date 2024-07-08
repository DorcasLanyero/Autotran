package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.location.Location;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Parcelable;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.cassens.autotran.BuildConfig;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.adapters.DeliveryDealerExpandableAdapter;
import com.cassens.autotran.data.adapters.EtaListAdapter;
import com.cassens.autotran.data.model.Dealer;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.EtaHolder;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.TrendingAlert;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.VIN;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.data.remote.tasks.RemoteSyncTask;
import com.cassens.autotran.dialogs.DealerUnavailableDialog;
import com.cassens.autotran.dialogs.LookUpScreenDialog;
import com.cassens.autotran.dialogs.VehiclePositionDialogFragment;
import com.cassens.autotran.handlers.ImageHandler;
import com.cassens.autotran.handlers.LocationHandler;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DeliveryVinInspectionActivity extends VINSelectActivity {
    private static final Logger log = LoggerFactory.getLogger(DeliveryVinInspectionActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	static final String EXTRA_IS_LAST_INCOMPLETE_LOAD = "IS_LAST_INCOMPLETE_LOAD";
	static final String OPERATION_INSPECTION = "Inspection";
	protected ExpandableListView mExpListView;

	protected List<Delivery> mDeliveries;

	public DeliveryDealerExpandableAdapter adapter;
	public ProgressDialog dialog;
	private Bundle mBundle;

	private Delivery mThisDelivery = null;
	private Load mThisLoad = null;

	private int mOperation;
	private int mLookupID;
	private int mDriverId;
	private User mDriver;
	private boolean mIsHighClaimsDriver;

	private boolean mResumed = false;
	private LookUpScreenDialog lookUpScreen;

	// Request Codes for Launched Activities
	private static final int REQ_CODE_VIN_INSPECTION = 1002;
	private static final int REQ_CODE_SIGNATURE = 1003;
	private static final int REQ_CODE_HIGH_CLAIMS_AUDIT = 1004;
	public static final int REQ_CODE_CAPTURE_IMAGE = 1007;
	public static final int REQ_CODE_VIEW_LOT_CODE_MSG = 1008;
	public static final int REQ_CODE_LOSS_AND_DAMAGE = 1009;
	public static final int REQ_CODE_SAFE_DELIVERY = 1010;
	public static final int REQ_CODE_SUPERVISOR_PRELOAD_CHECK = 1011;

	private String mCurrentPhotoFileName;
	private String mCurrentDocType;

	public enum InspectionStatus {
		UNINSPECTED_VINS_REMAINING, AWAITING_SUBMISSION, AWAITING_DEALER_SIGNATURE, AWAITING_DRIVER_SIGNATURE, COMPLETE, AWAITING_EXTRAS, AWAITING_SUPERVISOR_SIGNATURE
	}

	private InspectionStatus mInspectionStatus;

	private Parcelable mListState = null;

	private final ArrayList<Boolean> mGroupExpanded = new ArrayList<Boolean>();

	private ImageHandler mImageHandler;

	private TrainingRequirement.ByStatus mTrainingRequirements;
	private long[] mTrainingRequirementIds;
	private boolean mLeaveWithUnfinishedTraining = false;

	private ArrayList<TrendingAlert> alerts;

	private boolean alertsProcessed = false;
	private boolean mIsLastIncompleteLoad = false;
	private BroadcastReceiver mDeliveryVinChangedReceiver;
	private boolean mLastDeliveryPopupShown;
	private LocationHandler locationHandler;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		mBundle = getIntent().getExtras();

		locationHandler = LocationHandler.getInstance(this);
		locationHandler.startLocationTracking();

		this.mDriverId = mBundle.getInt("user_id", -1);
		this.mDriver = DataManager.getUser(this, String.valueOf(this.mDriverId));
		mIsHighClaimsDriver = (mDriver == null) ? false : (mDriver.highClaims != 0);

		mTrainingRequirementIds = mBundle.getLongArray(Constants.EXTRA_TRAINING_REQ_IDS);

		mOperation = mBundle.getInt(Constants.CURRENT_OPERATION);
		String lookupIDString = mBundle.getString(Constants.CURRENT_LOOKUP_ID, Constants.CURRENT_LOOKUP_ID + " not set");
		try {
			mLookupID = Integer.parseInt(lookupIDString);
		} catch (NumberFormatException nfe) {
			mLookupID = -1;
		}
		if (mLookupID < 0) {
			String errorMsg = "Error: invalid lookup ID for " + mOperation + ": " + lookupIDString;
			log.debug(Logs.INTERACTION, errorMsg);
			CommonUtility.showText(errorMsg);
			finish();
			return;
		}

		mDeliveryVinChangedReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				drawLayout(true);
			}
		};
		LocalBroadcastManager.getInstance(this).registerReceiver(mDeliveryVinChangedReceiver, new IntentFilter(Constants.DELIVERY_VIN_CHANGED));

		mImageHandler = new ImageHandler(this);
		mScanDataType = ScanDataType.PARTIAL_VIN;
		mLastDeliveryPopupShown = false;
	}


	public void showPopup(View v) {
		PopupMenu popup = new PopupMenu(this, v);
		MenuInflater inflater = popup.getMenuInflater();
		inflater.inflate(R.menu.clear_positions, popup.getMenu());
		popup.setOnMenuItemClickListener( new PopupMenu.OnMenuItemClickListener() {

		  @Override
		  public boolean onMenuItemClick(
				  android.view.MenuItem item) {
			  switch (item.getItemId()) {
				  case R.id.clear_vehicle_positions:
					  for (Delivery delivery : mThisLoad.deliveries) {
						  for (DeliveryVin deliveryVin : delivery.deliveryVins) {
							  deliveryVin.position = null;
							  deliveryVin.backdrv = "D";
							  DataManager.insertDeliveryVinToLocalDB(CommonUtility.getCurrentActivity(), deliveryVin);
						  }
					  }
					  drawLayout(false);
					  return true;
				  default:
					  return false;
			  }
		  }
	  });

		popup.show();
	}

	@Override
	protected void onResume() {
		super.onResume();

		mIsLastIncompleteLoad = mBundle.getBoolean(EXTRA_IS_LAST_INCOMPLETE_LOAD);

		drawLayout(true);

		if (mOperation == Constants.PRELOAD_OPERATION) {
			if (!alertsProcessed) {
				alerts = DataManager.getTrendingAlertsforLoad(this, mThisLoad);
				Collections.sort(alerts);
				HelperFuncs.showAlerts(alerts, DeliveryVinInspectionActivity.this);
				alertsProcessed = true;
			}
			if (alerts.size() > 0) {
				findViewById(R.id.alerts).setVisibility(View.VISIBLE);
				findViewById(R.id.invisiblePlaceholderForCentering).setVisibility(View.GONE);
			}
		}
	}

	@Override
	protected void onPostResume() {
		super.onPostResume();
		mResumed = true;

		if(mVinNumber != null) {
			onVINSelected(mVinNumber, mVinScanned);
			mVinNumber = null;
		}

		if(dialog != null && dialog.isShowing()) {
			dialog.cancel();
		}
	}

	@Override
	protected void onPause() {
		if (lookUpScreen != null) {
			lookUpScreen.canceled();
		}

		mResumed = false;
		dismissShuttlePreloadCountdownDialog();
		super.onPause();
	}

	@Override
	protected void onDestroy() {
		if (mDeliveryVinChangedReceiver != null) {
			LocalBroadcastManager.getInstance(this).unregisterReceiver(mDeliveryVinChangedReceiver);
		}
		locationHandler.stopLocationTracking();
		super.onDestroy();
	}

	public void showAlerts(View view) {
		HelperFuncs.showAlerts(alerts, DeliveryVinInspectionActivity.this);
	}

	public void enterVinPickMode() {
		displayMode = DisplayMode.VIN_PICK_MODE;
		mListState = mExpListView.onSaveInstanceState();
		drawLayout(true);
		findViewById(R.id.parentLayout).invalidate();
	}

	protected void drawLayout(boolean resume) {
		super.drawLayout();

        Boolean shuttleLoad = false;

		listView.setVisibility(View.GONE);
		customPickButton.setVisibility(View.GONE);
		reviewOnly = false;

		log.debug(Logs.DEBUG, "VINListActivity: operation="
				+ ((mOperation == Constants.DELIVERY_OPERATION) ? "delivery"
						: "preload") + " lookup_id=" + mLookupID);

		if (mOperation == Constants.DELIVERY_OPERATION) {
			mDeliveries = new ArrayList<>();

			mThisDelivery = DataManager.getDelivery(this, mLookupID);

			if (mThisDelivery == null) {
				log.debug(Logs.INTERACTION, "Delivery not found");
				CommonUtility.showText("Something went wrong.  The delivery can't be found.");
				finish();
				return;
			}

			setDealerUnavailableReason(mThisDelivery);

			screenTitle.setText(getResources().getString(
					R.string.delivery_inspection));

			mDeliveries.add(mThisDelivery);

            shuttleLoad = mThisDelivery.shuttleLoad;

			mThisLoad = DataManager.getLoad(this, mThisDelivery.load_id);
			if (mThisLoad == null) {
				log.debug(Logs.INTERACTION, "Load for Delivery not found");
				CommonUtility.showText("Something went wrong.  The load for this delivery can't be found.");
				finish();
				return;
			}

			if (shuttleLoad || mThisLoad.originLoad || mThisDelivery.isDealerAlwaysUnattended()) {
				disableDealerUnavailableToggle();
			}
			else {
				enableDealerUnavailableToggle(mThisDelivery.isDealerUnavailable());
			}

			// Do "auto-inspections," but only if the dealer doesn't require lot-locates.
			// Note: Dealer can be null on shuttle loads.
			if (mThisDelivery.dealer == null || !mThisDelivery.dealer.lotLocateRequired) {
				//
				// In the following cases, VINs on a delivery get "auto-inspected." When a VIN is,
				// auto-inspected, the driver doesn't have to scan it, but may edit it, if needed.
				//
				// o Deliveries for relay origin loads (aka "first leg" relay loads), certain
				//   load types designated as "auto-deliver" load types and Shelbyville loads.
				//
				// o The last delivery in the last load on the truck, but only if the driver's
				//   autoInspectLastDelivery flag is set. There's also an exception in the case
				//   where a VIN wasn't inspected on the preload. That can happen if a VIN is
				//   added via a load change AFTER the preload has been signed.
				//
				if (mThisLoad.originLoad || mThisLoad.isAutoDeliverLoadType()) {
					for (DeliveryVin dvin : mThisDelivery.deliveryVins) {
						dvin.inspectedDelivery = true;
					}
				} else if (mIsLastIncompleteLoad
 						&& mThisLoad.isLastUninspectedDelivery(mThisDelivery.delivery_id)
						&& mThisLoad.driver.autoInspectLastDelivery) {
					int inspectedVins = 0;

					for (DeliveryVin dvin : mThisDelivery.deliveryVins) {
						if (dvin.inspectedPreload && dvin.inspectedDelivery) {
							inspectedVins += 1;
							// Note: Elsewhere in the code, we handle the case where the dealer is
							//       unavailable and requires extra photos, so don't have to check
							//       for it here.
						}
					}

					if (!mLastDeliveryPopupShown && inspectedVins == 1 && mThisDelivery.deliveryVins.size() > 1) {
						AlertDialog.Builder builder = new AlertDialog.Builder(this);
						//builder.setTitle((title == null) ? "" : title);
						builder.setMessage("This is the last delivery. You do not need to scan other vins");
						builder.setPositiveButton("Ok", null);
						builder.setCancelable(true);
						builder.create().show();
						mLastDeliveryPopupShown = true;
					}
					if (inspectedVins >= 1) {
						for (DeliveryVin dvin : mThisDelivery.deliveryVins) {
							if (dvin.inspectedPreload) {
								dvin.inspectedDelivery = true;
							}
						}
					}
				}
			}
            if (!mThisDelivery.isInspected(mIsHighClaimsDriver)) {
				log.debug(Logs.INTERACTION, "uninspected VINS remaining");
                mInspectionStatus = InspectionStatus.UNINSPECTED_VINS_REMAINING;

				if (!shuttleLoad) {
					boolean callFMC = !HelperFuncs.isNullOrEmpty(mThisDelivery.callback) && mThisDelivery.callback.equals("Y");

					((TextView) findViewById(R.id.callFMCTitle)).setText("Call FMC = " + (callFMC ? "Y" : "N"));
					//get the load for the current delivery and determine if it is a filler
					if(mThisLoad != null && mThisLoad.isFillerLoadType()) {
						((TextView)findViewById(R.id.callFMCTitle)).append("\nFILLER");
						((TextView)findViewById(R.id.callFMCTitle)).setTextSize((float) 23.0);
					}

					findViewById(R.id.viewETAs).setOnClickListener(v -> showEtas(mThisLoad));
					findViewById(R.id.callFMCBanner).setVisibility(View.VISIBLE);
					//enableStiToggle(mThisDelivery.sti != 0);
				}
			} else {
				if (mThisDelivery.dealerSignature == null && !mThisDelivery.isDealerAlwaysUnattended() && !mThisDelivery.shuttleLoad && !mThisLoad.originLoad && !mThisLoad.isSVLoadType()) {
					log.debug(Logs.DEBUG, "delivery.status=" + mThisDelivery.status);
					if (mThisDelivery.status.equalsIgnoreCase("submitted")) {
						log.debug(Logs.INTERACTION, "submitted, return to dealer signature");
						proceedButton.setText("Return to Dealer Signature");
						mInspectionStatus = InspectionStatus.AWAITING_DEALER_SIGNATURE;
					} else {
						if (mThisDelivery.isDealerUnavailable()) {
							proceedButton.setText(R.string.proceed_with_delivery);
						}
						else {
							proceedButton.setText(R.string.proceed_dealer_signature);
						}
						mInspectionStatus = InspectionStatus.AWAITING_SUBMISSION;
					}
				} else if (mThisDelivery.driverSignature == null) {
					if (mThisDelivery.isDealerAlwaysUnattended()) {
						log.debug(Logs.INTERACTION, "dealer always unattended, skipping dealer signature");
						reviewButton.setVisibility(View.GONE);
						proceedButton.setText(R.string.proceed_with_delivery);
						mInspectionStatus = InspectionStatus.AWAITING_DRIVER_SIGNATURE;
					}
					if (mThisDelivery.hasNewDeliveryDamages() && HelperFuncs.isNullOrEmpty(mThisDelivery.getNotes())) {
						log.debug(Logs.INTERACTION, "needs loss and damage statement");
						proceedButton.setText("Loss & Damage Statement");
						mInspectionStatus = InspectionStatus.AWAITING_EXTRAS;
						reviewOnly = true;
					} else {

						if (mThisDelivery.shuttleLoad || mThisLoad.originLoad || mThisLoad.isSVLoadType() || mThisDelivery.isDealerAlwaysUnattended()) {
							reviewButton.setVisibility(View.GONE);
						} else {
							reviewButton.setVisibility(View.VISIBLE);
						}
						log.debug(Logs.INTERACTION, "needs driver signature");
						proceedButton.setText(R.string.proceed_driver_signature);
						mInspectionStatus = InspectionStatus.AWAITING_DRIVER_SIGNATURE;
					}
				} else {
					log.debug(Logs.DEBUG, "Setting status to complete");
					mInspectionStatus = InspectionStatus.COMPLETE;
				}
			}
		} else {
			//Preload vin select
			mThisLoad = DataManager.getLoad(this, mLookupID);
			if (mThisLoad == null) {
				log.debug(Logs.INTERACTION, "Load not found");
				CommonUtility.showText("Something went wrong. The load " + mLookupID +" can't be found.");
				finish();
				return;
			}

			if (!mThisLoad.lotCodeMsgSeen) {
				log.debug(Logs.INTERACTION, "Load needs to display a lot code message");
				Intent intent = new Intent(this, LotCodeMessageActivity.class);
				intent.putExtra(LotCodeMessageActivity.LOAD_ID, mThisLoad.load_id);
				startActivityForResult(intent, REQ_CODE_VIEW_LOT_CODE_MSG);
			}

			if (mThisLoad.lotCodeMsgSeen && mDriver.supervisorPreloadChk == 1 && mThisLoad.preloadSupervisorSignature == null) {
				log.debug(Logs.INTERACTION, "load needs supervisor signoff before preload begins");
				Intent intent = new Intent(this, ScanSupervisorCodeActivity.class);
				intent.putExtra("cameFrom", DeliveryVinInspectionActivity.OPERATION_INSPECTION);
				intent.putExtra("loadId", mThisLoad.load_id);
				startActivityForResult(intent, REQ_CODE_SUPERVISOR_PRELOAD_CHECK);
			}

			screenTitle.setText(getResources().getString(
					R.string.preload_inspection));

			if (mThisLoad.originLoad) {
				// If this is a first-leg relay, we group the vins based on finalMfg
				// and put the relay point info in a special header.
				mDeliveries = new ArrayList<>();

				if (mThisLoad.deliveries != null || mThisLoad.deliveries.size() > 0) {
					Delivery relayDelivery = mThisLoad.deliveries.get(0);
					final Dealer relayDealer = relayDelivery.dealer;
					final Activity activity = this;
					View relayTitle = findViewById(R.id.relay_title);
					TextView relayPointName = relayTitle.findViewById(R.id.relay_point_name);
					TextView relayPointInfo = relayTitle.findViewById(R.id.infoIcon);
					TextView relayLocation = relayTitle.findViewById(R.id.relay_point_location);

					String mfgs = "";
					boolean relayPointDealerUpdated = false;
					for (Delivery nextDeliv: mThisLoad.deliveries) {
						if (nextDeliv.dealer.hasUpdatedFields()) {
							relayPointDealerUpdated = true;
						}
						mfgs += String.format("%s%s", (mfgs.isEmpty() ? "" : ", "), nextDeliv.dealer.mfg);
					}
					relayPointName.setText(String.format("%s (%s)", relayDelivery.dealer.customer_name, mfgs));
					String relayToText = relayDelivery.dealer.customer_name;

					relayLocation.setText(String.format("%s, %s", relayDealer.city, relayDealer.state));
					relayPointInfo.setOnClickListener(new View.OnClickListener() {
						@Override
						public void onClick(View v) {
							Intent intent = new Intent(activity, DealerDetailsActivity.class);
							intent.putExtra("delivery_id", relayDelivery.delivery_id);
							intent.putExtra("operation", Constants.PRELOAD_OPERATION);
							((Activity) activity).startActivity(intent);
						}
					});
					if (relayPointDealerUpdated) {
						relayPointInfo.setBackgroundColor(getResources().getColor(R.color.InformationBlue));
						relayPointInfo.setTextColor(getResources().getColor(R.color.White));
						relayPointInfo.setTypeface(Typeface.DEFAULT_BOLD);
						relayPointInfo.setText(R.string.dealerInfoIconNewInfo);
					}
					relayPoint.setVisibility(View.VISIBLE);
				}

				ArrayList<String> finalDestinationDealers = new ArrayList<>();
				List<DeliveryVin> deliveryVins = mThisLoad.getDeliveryVinList();
				for (DeliveryVin deliveryVin:deliveryVins) {
					String dealerEntry = makeFinalDealerEntry(deliveryVin);
					if (!finalDestinationDealers.contains(dealerEntry)) {
						finalDestinationDealers.add(dealerEntry);
					}
				}

				for (String de: finalDestinationDealers) {
					Delivery fakeDelivery = new Delivery();
					Dealer finalDealer = null;
					if (de.split("-").length > 1) {
						String custNum = de.split("-")[0];
						String mfg = de.split("-")[1];
						finalDealer = DataManager.getDealer(this, custNum, mfg);
					}
					if (finalDealer == null) {
						finalDealer = new Dealer();
						finalDealer.customer_name = de;
					}
					fakeDelivery.dealer = finalDealer;
					fakeDelivery.load_id = mThisLoad.load_id;

					for (DeliveryVin deliveryVin:deliveryVins) {
						if (de.equals(makeFinalDealerEntry(deliveryVin))) {
							fakeDelivery.deliveryVins.add(deliveryVin);
						}
					}
					mDeliveries.add(fakeDelivery);
				}
			} else if (mThisLoad.deliveries != null) {
				mDeliveries = mThisLoad.deliveries;
			} else {
				mDeliveries = new ArrayList<>();
			}


			Collections.sort(mDeliveries);

            shuttleLoad = mThisLoad.shuttleLoad;

			if (!shuttleLoad) {
				boolean callFMC = false;

				findViewById(R.id.hamburger_menu).setVisibility(View.VISIBLE);

				for (Delivery d : mThisLoad.deliveries) {
					if (!HelperFuncs.isNullOrEmpty(d.callback) && d.callback.equals("Y")) {
						callFMC = true;
					}
				}

				final ArrayList<Delivery> deliveryETAs = new ArrayList<>(mDeliveries);
				if (!HelperFuncs.isNullOrEmpty(mThisLoad.nextDispatch)) {
					Delivery nextDispatch = new Delivery();
					nextDispatch.dealer = new Dealer();
					nextDispatch.dealer.customer_name = "Next Dispatch";
					nextDispatch.estdeliverdate = mThisLoad.nextDispatch.substring(0,10);
					deliveryETAs.add(nextDispatch);
				}

				((TextView) findViewById(R.id.callFMCTitle)).setText("Call FMC = " + (callFMC ? "Y" : "N"));
				if (mThisLoad.isFillerLoadType()) {
					((TextView)findViewById(R.id.callFMCTitle)).append("\nFILLER");
					((TextView)findViewById(R.id.callFMCTitle)).setTextSize((float) 23.0);
				}
				log.debug(Logs.INTERACTION, ((TextView)findViewById(R.id.callFMCTitle)).getText().toString());

				findViewById(R.id.viewETAs).setOnClickListener(v -> showEtas(mThisLoad));

				findViewById(R.id.callFMCBanner).setVisibility(View.VISIBLE);
			}

			proceedButton.setVisibility(View.VISIBLE);
			messageText.setVisibility(View.GONE);

			if (!mThisLoad.isInspected()) {
				log.debug(Logs.INTERACTION, "Needs inspection(s)");
				mInspectionStatus = InspectionStatus.UNINSPECTED_VINS_REMAINING;
			} else if (mThisLoad.needsLoadPosition()) {
				log.debug(Logs.INTERACTION, "Needs load position(s)");
				mInspectionStatus = InspectionStatus.UNINSPECTED_VINS_REMAINING;
			} else if(mThisLoad.requiresSignature()) {
				log.debug(Logs.INTERACTION, "Needs supervisor signature");
				mInspectionStatus = InspectionStatus.AWAITING_SUPERVISOR_SIGNATURE;
				proceedButton.setEnabled(false);
				proceedButton.setVisibility(View.GONE);
				messageText.setVisibility(View.VISIBLE);
			} else if (mThisLoad.needsPickSheetImage() || mThisLoad.needsExtraDocImage()) {
				if (mThisLoad.needsPickSheetImage() && mThisLoad.needsExtraDocImage()) {
					log.debug(Logs.INTERACTION, "Needs pick sheet & additional document image");
					proceedButton.setText("Take Required Document Pictures");
				}
				else if (mThisLoad.needsPickSheetImage()) {
					log.debug(Logs.INTERACTION, "Needs pick sheet");
					proceedButton.setText("Take Pick Sheet Picture");
				}
				else {
					log.debug(Logs.INTERACTION, "Needs document image");
					proceedButton.setText("Take Required Document Picture");
				}
				proceedButton.setEnabled(true);
				mInspectionStatus = InspectionStatus.AWAITING_SUBMISSION;
			} else if (this.mDriver != null && this.mDriver.requiresAnAudit() && (HelperFuncs.isNullOrEmpty(mThisLoad.driverHighClaimsAudit) || HelperFuncs.isNullOrEmpty(mThisLoad.supervisorSignature))) {
				proceedButton.setText(R.string.proceed_audit_checklist);
				proceedButton.setEnabled(true);
				log.debug(Logs.INTERACTION, "Needs preload audit");
				mInspectionStatus = InspectionStatus.AWAITING_SUBMISSION;
			} else if (mThisLoad.driverPreLoadSignature == null) {
				log.debug(Logs.DEBUG, "load.status=" + mThisLoad.status);
				if (mThisLoad.status.equalsIgnoreCase("submitted")) {
					proceedButton.setText("Return to Driver Signature");
					proceedButton.setEnabled(true);
					log.debug(Logs.INTERACTION, "Allowing return to driver signature");
					mInspectionStatus = InspectionStatus.AWAITING_DRIVER_SIGNATURE;
				} else {
					proceedButton.setText(R.string.proceed_driver_signature);
					proceedButton.setEnabled(true);
					log.debug(Logs.INTERACTION, "Needs driver signature");
					if (this.mDriver != null && this.mDriver.requiresAnAudit()) {
						customPickButton.setText(R.string.return_supervisor_audit);
						customPickButton.setVisibility(View.VISIBLE);
					}
					mInspectionStatus = InspectionStatus.AWAITING_SUBMISSION;
				}
			} else {
				log.debug(Logs.INTERACTION, "Inspections are complete");
				mInspectionStatus = InspectionStatus.COMPLETE;
			}
		}

		//Set the load number in the title bar
		if (mThisLoad != null) {
			if (CommonUtility.isHoneywellLargeDisplaySet()) {
				loadNum.setTextSize(14);
			}
			else {
				loadNum.setTextSize(15);
			}
			loadNum.setVisibility(View.VISIBLE);
			if (mOperation == Constants.DELIVERY_OPERATION && mThisDelivery != null
					&& mThisDelivery.dealer != null
					&& !CommonUtility.isNullOrBlank(mThisDelivery.dealer.customer_number)) {
				if (CommonUtility.isNullOrBlank(mThisDelivery.dealer.mfg)) {
					loadNum.setText("Load " + mThisLoad.loadNumber + " - " + mThisDelivery.dealer.customer_number);
				}
				else {
					loadNum.setText("Load " + mThisLoad.loadNumber + " - " + mThisDelivery.dealer.mfg + " " +mThisDelivery.dealer.customer_number);
				}
			}
			else if (shuttleLoad) {
				//loadNum.setText(mThisLoad.loadNumber + " -" + mThisLoad.shuttleMove.terminal + mThisLoad.shuttleMove.getMoveCode());
				loadNum.setText(mThisLoad.loadNumber);
			}
			else {
				loadNum.setText("Load " + mThisLoad.loadNumber);
			}
		}

		// Handle training requirement display
		if (mOperation == Constants.PRELOAD_OPERATION && mThisLoad != null) {
            // Get the requirements here so that they're correct when we return from finishing ad-hoc coaching
            List<TrainingRequirement> reqs = DataManager.getTrainingRequirementsForLoad(this, mThisLoad.load_remote_id);
            mTrainingRequirements = TrainingRequirement.filterList(reqs);

            ImageView titleBarIcon = findViewById(R.id.titleBarIcon);;
            if(mTrainingRequirements.unfinished.size() > 0) {
                titleBarIcon.setImageDrawable(getResources().getDrawable(R.drawable.coaching_icon));
                titleBarIcon.setVisibility(View.VISIBLE);
				//findViewById(R.id.invisiblePlaceholderForCentering).setVisibility(View.GONE);
            }
            else if(mTrainingRequirements.finished.size() > 0) {
                titleBarIcon.setImageDrawable(getResources().getDrawable(R.drawable.coaching_complete_icon));
                titleBarIcon.setVisibility(View.VISIBLE);
				//findViewById(R.id.invisiblePlaceholderForCentering).setVisibility(View.GONE);
            }
            if (titleBarIcon.getVisibility() == View.VISIBLE) {
            	// If the training icon is visible and we're in large font or large screen,
				// we reduce the size of the text to prevent overflow. This is a temporary
				// hack until we upgrade to Android 8+
				//
				// TODO: Use Auto Scale TextViews for large font/screen mode (requires Android 8+)
				if (CommonUtility.isHoneywellLargeDisplaySet()) {
					screenTitle.setTextSize(15);
				}
				else if (CommonUtility.isLargeFontSet()) {
					screenTitle.setTextSize(15);
				}
			}
        }

		for (Delivery d : mDeliveries) {
			Collections.sort(d.deliveryVins);
		}

		log.debug(Logs.DEBUG, "inspection status: " + mInspectionStatus);

		switch (mInspectionStatus) {

		case UNINSPECTED_VINS_REMAINING:
			if (displayMode == DisplayMode.VIN_PICK_MODE) {
				vinGetLayout.setVisibility(View.GONE);
				proceedButton.setVisibility(View.GONE);
				vinPickLayout.setVisibility(View.VISIBLE);
			} else {
				vinPickLayout.setVisibility(View.GONE);
				vinGetLayout.setVisibility(View.VISIBLE);
			}
			break;

		case AWAITING_DEALER_SIGNATURE:
		case AWAITING_DRIVER_SIGNATURE:
			//Can we turn the info button off here?
			//infoBotton.setVisibility(GONE);
            reviewOnly = !shuttleLoad && !(mOperation == Constants.DELIVERY_OPERATION && (mThisLoad.originLoad || mThisDelivery.isDealerAlwaysUnattended()));
            // no break; fall through..
		case AWAITING_EXTRAS:
		case AWAITING_SUBMISSION:
			proceedButton.setVisibility(View.VISIBLE);
		case AWAITING_SUPERVISOR_SIGNATURE:
			displayMode = DisplayMode.VIN_PICK_MODE;
			vinPickLayout.setVisibility(View.VISIBLE);
			if (reviewOnly) {
				dealerUnavailableCheckbox.setVisibility(View.GONE);
				dealerUnavailableReason.setVisibility(View.GONE);
				vinPickSelectVinPrompt.setText(R.string.tap_or_scan_vin_to_view);
			}
			break;

		case COMPLETE:
			//if our inspection is complete, start the upload;
			RemoteSyncTask syncTask = new RemoteSyncTask(this);
			syncTask.execute(mDriverId);

			finishActivity();
			return;

		default:
			break;
		}

		int expListViewPosition = -1;

		if(mExpListView != null) {
			expListViewPosition = mExpListView.getFirstVisiblePosition();
		}

		if (displayMode == DisplayMode.VIN_PICK_MODE) {
			mExpListView = findViewById(R.id.expVinListView);
			adapter = new DeliveryDealerExpandableAdapter(this,
					R.layout.dealer_list_element, R.layout.vin_list_element,
					mDeliveries, mOperation, true);
		} else {
			mExpListView = findViewById(R.id.expDeliveryVinList);
			adapter = new DeliveryDealerExpandableAdapter(this,
					R.layout.dealer_list_element_small,
					R.layout.vin_list_element_small, mDeliveries, mOperation, false);
		}

		adapter.setRelevantTrainings(mTrainingRequirements);
		mExpListView.setAdapter(adapter);
		mExpListView.setVisibility(View.VISIBLE);
		mExpListView.setOnGroupExpandListener(groupPosition -> mGroupExpanded.set(groupPosition, true));
		mExpListView.setOnGroupCollapseListener(groupPosition -> mGroupExpanded.set(groupPosition, false));

		//Set the visible position to the first visible position previously set
		if(expListViewPosition != -1) {
			mExpListView.setSelection(expListViewPosition);
		}
		
		if (mGroupExpanded.size() == 0) {
			for (int i = 0; i < adapter.getGroupCount(); i++) {
				mGroupExpanded.add(false);

				if (adapter.getGroupCount() < 2
						|| (mOperation == Constants.PRELOAD_OPERATION || mOperation == Constants.SHUTTLE_LOAD_OPERATION)
						&& !((Delivery) adapter.getGroup(i)).isInspected(mIsHighClaimsDriver)) {
					mExpListView.expandGroup(i);
				}
			}
		} else {
			for (int i = 0; i < adapter.getGroupCount() && i < mGroupExpanded.size(); i++) {
				if (mGroupExpanded.get(i)) {
					mExpListView.expandGroup(i);
				}
			}
		}

		if (resume && mListState != null) {
			mExpListView.onRestoreInstanceState(mListState);
		}
	}

	private String makeFinalDealerEntry(DeliveryVin deliveryVin) {
		if (HelperFuncs.isNullOrWhitespace(deliveryVin.finalDealer)) {
			return "UNKNOWN";
		}
		else {
			return deliveryVin.finalDealer + "-"
					+ HelperFuncs.noNull(deliveryVin.finalMfg);
		}
	}

	private void showEtas(Load parentLoad) {
		log.debug(Logs.INTERACTION, "show ETAs for parent load " + parentLoad.load_id);
		final ListView dealerETAGrid = new ListView(DeliveryVinInspectionActivity.this);
		ArrayList<EtaHolder> etaHolderArrayList = new ArrayList<EtaHolder>();

		if(CommonUtility.isLargeFontSet()){
			etaHolderArrayList.add(new EtaHolder("First Drop:", ""));
			etaHolderArrayList.add(new EtaHolder(parentLoad.firstDrop, ""));
			etaHolderArrayList.add(new EtaHolder("Last Drop:", ""));
			etaHolderArrayList.add(new EtaHolder(parentLoad.lastDrop, ""));
			etaHolderArrayList.add(new EtaHolder("Next Dispatch:", ""));
			etaHolderArrayList.add(new EtaHolder(parentLoad.nextDispatch, ""));
		}
		else {
			etaHolderArrayList.add(new EtaHolder("First Drop:", parentLoad.firstDrop));
			etaHolderArrayList.add(new EtaHolder("Last Drop:", parentLoad.lastDrop));
			etaHolderArrayList.add(new EtaHolder("Next Dispatch:", parentLoad.nextDispatch));
		}
		log.debug(Logs.INTERACTION, "ETA First Drop: " + parentLoad.firstDrop +
					" Last Drop: " + parentLoad.lastDrop + " Next Dispatch: " + parentLoad.nextDispatch);


		EtaListAdapter etaListAdapter = new EtaListAdapter(DeliveryVinInspectionActivity.this, R.layout.dealer_eta_item, etaHolderArrayList);
		dealerETAGrid.setAdapter(etaListAdapter);


		AlertDialog.Builder builder = new AlertDialog.Builder(DeliveryVinInspectionActivity.this);
		builder.setTitle("ETAs");
		builder.setView(dealerETAGrid);
		builder.setPositiveButton("Ok", (dialog, which) -> CommonUtility.logButtonClick(log, "Ok", "for ETA estimates"));
		builder.setNegativeButton("Cancel", (dialogInterface, i) -> CommonUtility.logButtonClick(log, "Cancel", "for ETA estimates"));
		builder.setCancelable(true);
		builder.create().show();
	}

	private DeliveryVin deliveryVinInDeliveryVins(List<Delivery> deliveries, String vinNumber) {

		for (Delivery d : deliveries) {
			for (DeliveryVin dv : d.deliveryVins) {
				if (dv.vin.vin_number.toLowerCase().trim().equals(vinNumber.toLowerCase().trim())) {
					return dv;
				}
			}
		}
		return null;
	}

	private DeliveryVin deliveryVinInDeliveryVins(List<Delivery> deliveries, String vinFirst3, String vinLast8) {
		log.debug(Logs.INTERACTION, "testing for " + vinFirst3 + " " + vinLast8);
		for (Delivery delivery : deliveries) {
			for (DeliveryVin deliveryVin : delivery.deliveryVins) {
				String vinNumber = deliveryVin.vin.vin_number.toLowerCase().trim();
				int length = vinNumber.length();
				int last8Start = length - 8;

				String thisFirst3 = vinNumber.substring(0, 3);
				String thisLast8 = vinNumber.substring(last8Start, length);

				Log.v(TAG, "first3: " + thisFirst3 + " " + vinFirst3 + " last8: " + thisLast8 + " " + vinLast8);

				if (thisFirst3.equals(vinFirst3.toLowerCase().trim()) && thisLast8.equals(vinLast8.toLowerCase().trim())) {
					return deliveryVin;
				}
			}
		}
		return null;
	}

	String mVinNumber = null;
	boolean mVinScanned = false;

	private DeliveryVin getDeliveryVinFromVinNumber(String vinNumber) {
        DeliveryVin deliveryVin;
        if (vinNumber.contains("******")) {
        	if ((vinNumber.length() < 17 && BuildConfig.AUTOTRAN_VALIDATE_VINS) || vinNumber.length() < 8) {
        		return null;
			}
            String first3 = vinNumber.substring(0,3);

        	int length = vinNumber.length();
            String last8 = vinNumber.substring(length - 8, length);

            Log.v(TAG, "VIN: " + vinNumber + " first3: " + first3 + " last8: " + last8);
            deliveryVin = deliveryVinInDeliveryVins(mDeliveries, first3, last8);
        } else {
            deliveryVin= deliveryVinInDeliveryVins(mDeliveries, vinNumber);
        }
        return deliveryVin;
    }

    @Override
    protected void validateScannedValue(String scannedValue) throws GenericScanningActivity.ScannedValueException {
		if (HelperFuncs.isNullOrEmpty(scannedValue)) {
			throw new ScannedValueException(ScannedValueException.EMPTY_VALUE_SCANNED, "Empty Value Scanned");
		}
		Log.v(TAG, "Scanned value: " + scannedValue);
		String vinNumber = CommonUtility.processScannedVIN(scannedValue);
		DeliveryVin deliveryVin = getDeliveryVinFromVinNumber(vinNumber);
        if (deliveryVin == null) {
			if (CommonUtility.checkVinNoPopup(vinNumber) || vinNumber.contains("******")) {
				String groupType;
				String action;
				if (mOperation == Constants.DELIVERY_OPERATION) {
					groupType = "delivery";
					action = "unloaded";
				} else {
					groupType = "load";
					action = "loaded";
				}
				String failureMessage = "Error: That VIN isn't part of this "
						+ groupType + ". Please ensure you have the correct " + groupType +
						" selected and that you have " + action + " the correct unit.";
				log.debug(Logs.DEBUG, failureMessage + "(vinNumber=" + HelperFuncs.noNull(vinNumber, "NULL") + ")");
				throw new ScannedVinException(ScannedVinException.VIN_NOT_IN_LOAD, failureMessage);
			}
			else {
				log.debug(Logs.DEBUG,"Scanned value " + HelperFuncs.noNull(vinNumber, "NULL") + " is not a valid VIN and was not found in load");
				throw new ScannedVinException();
			}
        }
    }

	@Override
	public void onVINSelected(String vinNumber, boolean vinScanned)  {
		log.debug(Logs.INTERACTION, "Selected VIN " + vinNumber);
		super.onVINSelected(vinNumber, vinScanned);

		if(!mResumed) {
			//We need to wait for the activity to sort itself out
			mVinNumber = vinNumber;
			this.mVinScanned = vinScanned;

			log.debug(Logs.INTERACTION, "Waiting for vin selection response until after the activity resumes");

			return;
		}

		try {
			int deliveryVinId = -1;

			DeliveryVin deliveryVin = getDeliveryVinFromVinNumber(vinNumber);
            if(deliveryVin != null) {
                vinNumber = deliveryVin.vin.vin_number;
            }

			log.debug(Logs.DEBUG, "vin found, disconnecting scanner.");
			// disconnectScanner();

			VIN thisVIN = DataManager.getVINForVinNumber(this, vinNumber);

			final Intent intent = new Intent();
			intent.putExtra("vin_number", thisVIN.vin_number);
			intent.putExtra("vin_desc", thisVIN.getDescription());
			intent.putExtra("delivery_vin_id", deliveryVin.delivery_vin_id);
			intent.putExtra("vin_scanned", vinScanned);
			intent.putExtra(Constants.CURRENT_OPERATION, mOperation);

			//if the current delivery vin has a position set that is valid, pass it along
			if(mOperation == Constants.PRELOAD_OPERATION) {
				//If this LOAD doesn't have ANY inspected deliveryVins, send a preload start event to the server
				boolean firstInspection = true;
				for(Delivery delivery : mThisLoad.deliveries) {
					for(DeliveryVin tmpDeliveryVin : delivery.deliveryVins) {
						if(tmpDeliveryVin.inspectedPreload) {
							firstInspection = false;
						}
					}
				}

				if(firstInspection) {
					log.debug(Logs.INTERACTION, "Sending PS load event for load " + mThisLoad.loadNumber);
					SimpleTimeStamp sts = new SimpleTimeStamp();
					Location location = locationHandler.getLocation();
					HashMap<String,String> reqBody = new HashMap<>();

					String eventString = TextUtils.join(",",
							new String[]{
									"PS",
									mThisLoad.driver.driverNumber,
									mThisLoad.loadNumber,
									vinNumber,
									sts.getUtcDateTime(),
									sts.getUtcTimeZone(),
									String.valueOf(location.getLatitude()),
									String.valueOf(location.getLongitude()),
									"",
									TruckNumberHandler.getTruckNumber(getApplicationContext())
							}) ;

					reqBody.put("csv", eventString);

					LoadEvent event = new LoadEvent();
					event.csv = eventString;
					DataManager.insertLoadEvent(this, event);
					SyncManager.pushLoadEventsLatched(getApplicationContext());
				}

				if(DeliveryVin.isValidLoadPosition(deliveryVin.position)) {
					proceedToVINInspection(intent);
				} else {
					//we need to capture the position in the popup

					List <String> opts = DeliveryDealerExpandableAdapter.getAvailablePositionStrings(this, deliveryVin, true);
					//log.debug(Logs.INTERACTION, "VIN position dialog displayed. positions available: " + TextUtils.join("|", opts));

					final VehiclePositionDialogFragment dialog = VehiclePositionDialogFragment.newInstance(deliveryVin.delivery_vin_id,(ArrayList<String>) opts, false);
					dialog.setPositiveClickListener((dialogInstance, which) -> {


						if (dialog.getNewPosition() != null && !dialog.getNewPosition().contains("--")) {

							CommonUtility.showText("Picked position: " + dialog.getNewPosition());
							log.debug(Logs.INTERACTION, "Picked position: " + dialog.getNewPosition());

							dialog.saveDeliveryVin();

							CommonUtility.showText("Selected VIN " + deliveryVin.vin.vin_number);
							log.debug(Logs.INTERACTION, "Selected VIN " + deliveryVin.vin.vin_number);
							proceedToVINInspection(intent);
						} else if (dialog.getNewPosition().contains("--") && dialog.getAllowClear()) {
							dialog.clearDeliveryVinPosition();
						} else {

							AlertDialog alertDialog = new AlertDialog.Builder(DeliveryVinInspectionActivity.this).create();
							alertDialog.setTitle("Alert");
							alertDialog.setMessage("Please choose a valid position");
							log.debug(Logs.INTERACTION, "Please choose a valid position");
							alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
									(subdialog, which1) -> {
										subdialog.dismiss();
										dialog.show(DeliveryVinInspectionActivity.this.getFragmentManager(),
												"dialog");
									});
							alertDialog.show();
						}
					});
					dialog.show(DeliveryVinInspectionActivity.this.getFragmentManager(),
							"dialog");
				}
			} else {
				proceedToVINInspection(intent);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void proceedToVINInspection(Intent intent) {
		if (reviewOnly) {
			log.debug(Logs.INTERACTION, "Proceed to vin Inspection in review only mode");
            intent.putExtra("is_editable", false);
            intent.putExtra(Constants.EXTRA_TRAINING_REQ_IDS, mTrainingRequirementIds);
            Intent tmpIntent = new Intent(this, VINInspectionActivity.class)
                    .putExtras(intent.getExtras());
            startActivity(tmpIntent);
        } else {
			log.debug(Logs.INTERACTION, "Proceed to vin Inspection in edit mode");
            intent.putExtra(Constants.EXTRA_TRAINING_REQ_IDS, mTrainingRequirementIds);
            Intent tmpIntent = new Intent(this, VINInspectionActivity.class)
                    .putExtras(intent.getExtras());
            startActivityForResult(tmpIntent, REQ_CODE_VIN_INSPECTION);
        }
	}

	@Override
	public void onProceedButton(View v) {
		super.onProceedButton(v);
		CommonUtility.logButtonClick(log, v);

		if (mOperation == Constants.PRELOAD_OPERATION) {
			// Cannot have duplicate load positions during the preload...
			if (hasLoadPositionIssues(mDeliveries)) {
				log.debug(Logs.INTERACTION, "Load has position issues, aborting");
				return;
			}

			// Warn if finishing a preload with a training requirement unmet
            // TODO: halt progress?
			if(mTrainingRequirements.unfinished.size() > 0 && !mLeaveWithUnfinishedTraining) {
			    AlertDialog.Builder b = new AlertDialog.Builder(this);
			    b.setTitle("Unfinished Training");
			    b.setMessage("This load has unfinished training requirements. Proceed anyway?");
			    b.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        mLeaveWithUnfinishedTraining = true;
                        dialog.dismiss();
                        onProceedButton(findViewById(R.id.proceedButton));
                    }
                });
			    b.setNegativeButton("No", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                });
			    b.create().show();
			    return;
            }
		}

		if (mOperation == Constants.DELIVERY_OPERATION) {

			if(mThisDelivery.load == null) {
				mThisDelivery.load = mThisLoad;
			}

			if ((mThisDelivery.shuttleLoad || mThisLoad.originLoad || mThisDelivery.dealerSignature != null || mThisDelivery.isDealerAlwaysUnattended())
					&& mThisDelivery.hasNewDeliveryDamages()
					&& HelperFuncs.isNullOrEmpty(mThisDelivery.getNotes())) {
				log.debug(Logs.INTERACTION, "Delivery load with new damages, launching notes activity");
				Intent i = new Intent(this, LossAndDamageActivity.class);
				startActivityForResult(i, REQ_CODE_LOSS_AND_DAMAGE);
				return;
			} else if (!mThisLoad.originLoad && mThisDelivery.needsLotLocates(getApplicationContext())) {
				AlertDialog.Builder builder = new AlertDialog.Builder(DeliveryVinInspectionActivity.this);
				builder.setMessage("Please lot locate all of the VINs for this delivery");
				builder.create().show();
				return;
			} else if (mInspectionStatus == InspectionStatus.AWAITING_DRIVER_SIGNATURE && mThisDelivery.dealer != null && mThisDelivery.dealer.requiresSafeDeliveryLocation() && HelperFuncs.isNullOrEmpty(mThisDelivery.safeDelivery)){
				log.debug(Logs.INTERACTION, "Delivery load for Toyota dealer, launching safe area check");
				Intent i = new Intent(this, SafeDeliveryActivity.class);
				startActivityForResult(i, REQ_CODE_SAFE_DELIVERY);
				return;
			} else if (mInspectionStatus == InspectionStatus.AWAITING_SUBMISSION) {

				/*
				 * moved to the signature activity since I want to be able to
				 * edit things until the sig is captured log.debug(Logs.DEBUG,
				 * "setting delivery status to submitted"); mThisDelivery.status
				 * = "submitted"; DataManager.insertDeliveryToLocalDB(this,
				 * mThisDelivery);
				 */
				log.debug(Logs.INTERACTION, "Dealer signature required");
				mInspectionStatus = InspectionStatus.AWAITING_DEALER_SIGNATURE;
			}
			if (mInspectionStatus == InspectionStatus.AWAITING_DEALER_SIGNATURE) {
				mBundle.putString("user_type", "dealer");
			} else {
				mBundle.putString("user_type", "driver");
			}
			mBundle.putInt("delivery_id", mThisDelivery.delivery_id);
		} else if (mThisLoad.needsPickSheetImage() || mThisLoad.needsExtraDocImage()) {
			if (mThisLoad.needsExtraDocImage()) {
				buildPromptDialog(mThisLoad.extraDocImageRequired);
			}
			if (mThisLoad.needsPickSheetImage()) {
				buildPromptDialog("Pick Sheet");
			}
			return;
		} else if (this.mDriver.requiresAnAudit() && (HelperFuncs.isNullOrEmpty(mThisLoad.driverHighClaimsAudit) || HelperFuncs.isNullOrEmpty(mThisLoad.supervisorSignature))) {
			log.debug(Logs.INTERACTION, "High claims audit required");
			startHighClaimsActivity();
			return;
		}	else {
			mBundle.putString("user_type", "driver");
			mBundle.putInt("load_id", mThisLoad.load_id);
		}

		if(mOperation != Constants.DELIVERY_OPERATION && mThisLoad != null && mThisLoad.isInspected() && mThisLoad.mightRequireSignature()) {
			log.debug(Logs.INTERACTION,
					"Might require supervisor signature");
			//prompt for user to optionally include a supervisor signature on these damages
			AlertDialog.Builder builder = new AlertDialog.Builder(DeliveryVinInspectionActivity.this);

			String promptMessage = "Damages have been recorded for the following vehicle(s) on this load: \n";

			List<DeliveryVin> damagedVins = mThisLoad.getDamagedVinsMissingSupervisorSignature();

			for(DeliveryVin damagedVin : damagedVins) {
				promptMessage += "\n" + damagedVin.vin.vin_number;
			}

			String prompt = "Do you require a supervisor signature?";
			promptMessage += "\n\n" + prompt;
			log.debug(Logs.INTERACTION, prompt);

			builder.setMessage(promptMessage);
			builder.setPositiveButton("Yes", (dialog, which) -> {
				CommonUtility.logButtonClick(log, "Yes", "for " + prompt);
				return;
			});
			builder.setNegativeButton("No", (dialog, which) -> {
				CommonUtility.logButtonClick(log, "No", "for " + prompt);
				collectSignature();
			});
			//builder.setCancelable(true);
			builder.create().show();
		} else {
			if (mThisLoad.shuttleLoad){
				checkTimeIntervalThenProceed();
			}else {
				collectSignature();
			}
		}
	}

	private void buildPromptDialog(final String docType) {
		final String tag = mThisLoad.docTypeToTag(docType);
		AlertDialog.Builder builder = new AlertDialog.Builder(DeliveryVinInspectionActivity.this);
		log.debug(Logs.INTERACTION, "Delivery load requires " +
				docType + " photo, launching camera");
		builder.setMessage("Take a picture of your " + docType + " to show damage coverage.\n\nMAKE CERTAIN ALL FOUR CORNERS ARE VISIBLE.");
		builder.setPositiveButton("Camera", (dialog, which) -> {
			mCurrentPhotoFileName = (Constants.PRELOAD_IMAGE_FILE_PREFIX + mThisLoad.loadNumber + Constants.IMAGE_FILE_DELIM + "_" + tag + "_" + UUID.randomUUID().toString());
			mCurrentDocType = docType;
			mImageHandler.cameraButtonClick(mCurrentPhotoFileName, REQ_CODE_CAPTURE_IMAGE);
		});

		builder.setCancelable(false);
		builder.create().show();
	}

	private ProgressDialog shuttlePreloadTimeoutDialog = null;
	private CountDownTimer shuttleCountDownTimer = null;

	private void dismissShuttlePreloadCountdownDialog() {
		if (shuttleCountDownTimer != null) {
			shuttleCountDownTimer.cancel();
			shuttleCountDownTimer = null;
		}
		if (shuttlePreloadTimeoutDialog != null){
			shuttlePreloadTimeoutDialog.dismiss();
			shuttlePreloadTimeoutDialog = null;
		}
	}

	private void showShuttlePreloadCountdownDialog(long millisUntilFinished) {
		shuttlePreloadTimeoutDialog  = new ProgressDialog(this);
		shuttlePreloadTimeoutDialog.show();
		shuttlePreloadTimeoutDialog.setCancelable(false);
		shuttlePreloadTimeoutDialog.setContentView(R.layout.countdown_display);

		((TextView)shuttlePreloadTimeoutDialog.findViewById(R.id.countdownTitle)).setText("Notice");
		TextView message = shuttlePreloadTimeoutDialog.findViewById(R.id.message);
		TextView clock = shuttlePreloadTimeoutDialog.findViewById(R.id.clock);
		TextView clockCaption = shuttlePreloadTimeoutDialog.findViewById(R.id.clockCaption);
		Button dismiss = shuttlePreloadTimeoutDialog.findViewById(R.id.dismissButton);
		dismiss.setText("Back");
		dismiss.setVisibility(View.VISIBLE);

		message.setText("You cannot proceed with this delivery until the minimum time elapses. In the future, please make sure to collect scans and signatures at the time and location of the pickup or drop-off.");
		message.setTextSize(20);
		shuttleCountDownTimer = new CountDownTimer(millisUntilFinished, 1000) {

			public void onTick(long millisUntilFinished) {
				int minutes = (int)millisUntilFinished / 60000;
				int seconds = ((int)millisUntilFinished % 60000)/1000;
				if (minutes > 0) {
					clock.setText(String.format("%d:%02d", minutes, seconds));
					clockCaption.setVisibility(View.INVISIBLE);
				}
				else {
					clock.setText(Integer.toString(seconds));
					clockCaption.setVisibility(View.VISIBLE);
				}
			}
			public void onFinish() {
				shuttlePreloadTimeoutDialog.dismiss();
				collectSignature();
			}
		}.start();

		dismiss.setOnClickListener(v -> dismissShuttlePreloadCountdownDialog());
	}

	private void checkTimeIntervalThenProceed(){
		long minTimeLimit = AppSetting.SHUTTLE_DELIVERY_TIMEOUT.getInt() * 60 * 1000; // 10 minutes
		long deliveryAge = 0;
		long millisUntilFinished = 0;

		try {
			String driverSignatureTimeString = mThisLoad.driverPreLoadSignatureSignedAt;
			if (HelperFuncs.isNullOrWhitespace(driverSignatureTimeString)) {
				throw new IllegalArgumentException();
			}
			SimpleTimeStamp time = new SimpleTimeStamp();
			Date timeCreated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(driverSignatureTimeString);
			Date currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time.getUtcDateTime());
			deliveryAge = TimeUnit.MILLISECONDS.convert(Math.abs(currentTime.getTime() - timeCreated.getTime()), TimeUnit.MILLISECONDS);
			millisUntilFinished = TimeUnit.MILLISECONDS.convert(Math.abs(minTimeLimit- deliveryAge), TimeUnit.MILLISECONDS);
		} catch (Exception e) {
			// If the data in the time string is bad, skip the timer, log the event
			log.debug(Logs.DEBUG, "Bad driver signature time for load " + mThisLoad.loadNumber);
			deliveryAge = 0;
			millisUntilFinished = 0;
		}

		if (deliveryAge < minTimeLimit) {
			showShuttlePreloadCountdownDialog(millisUntilFinished);
		} else {
			collectSignature();
		}
	}

	@Override
	protected boolean onDealerUnavailableToggle(boolean isDealerUnavailable) {
		// Override if needed
		if (mThisDelivery == null) {
			CommonUtility.simpleMessageDialog(this, "No delivery record is selected");
			return false;
		}
		if (isDealerUnavailable != mThisDelivery.isDealerUnavailable()) {
			if (isDealerUnavailable) {
				dealerUnavailableDialog(); // Dialog logic will set sti and afthrs to appropriate values
			}
			else {
				mThisDelivery.sti = 0;
				mThisDelivery.afrhrs = 0; // turn off afrhrs, since afrhrs implies STI (in current workflow anyway)
				//mThisDelivery.dealerContact = "";
				DataManager.saveDeliveryStiAndAfrhrs(this, mThisDelivery.getId(), mThisDelivery.sti, mThisDelivery.afrhrs, mThisDelivery.dealerContact);
				drawLayout(true);
			}
		}
		return true;
	}


	private void setDealerUnavailableReason(Delivery delivery) {
		int messageId = -1;
		if (delivery != null) {
			if (delivery.isStiDelivery()) {
				messageId = R.string.dealer_unavailable_after_hours;
			} else if (delivery.isAfterHoursDelivery()) {
				messageId = R.string.dealer_unavailable_dorts;
			}
		}
		if (messageId == -1) {
			dealerUnavailableReason.setVisibility(View.INVISIBLE);
		}
		else {
			dealerUnavailableReason.setText(messageId);
			dealerUnavailableReason.setVisibility(View.VISIBLE);
		}
	}

	private void dealerUnavailableDialog() {

		final Context ctx = this;

		DealerUnavailableDialog dealerUnavailableDialog = new DealerUnavailableDialog(DeliveryVinInspectionActivity.this, mThisDelivery,
				new DealerUnavailableDialog.IDealerUnavailableDialogCallback() {

					@Override
					public void onAfterHoursClicked() {
						if (mThisDelivery != null) {
							displayMode = DisplayMode.VIN_GET_MODE;
							drawLayout(true);
						}
					}

					@Override
					public void onDealerOpenRefusedToSignClicked(String contact) {
						displayMode = DisplayMode.VIN_GET_MODE;
						drawLayout(true);
					}

					@Override
					public void onCancel() {
						setDealerUnavailableCheckbox(false);
					}
				});

		dealerUnavailableDialog.show();
	}

	private void collectSignature() {
		String remoteId;

		lookUpScreen = new LookUpScreenDialog(CommonUtility.getCurrentActivity(), new LookUpScreenDialog.LookUpScreenCallback() {
			@Override
			public void proceed() {
				startSignatureActivity();
			}
		});

		if(mOperation == Constants.DELIVERY_OPERATION) {
			remoteId = mThisDelivery.getRemoteId();
			boolean lookupShown = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(remoteId + "_delivery_lookup_shown", false);

			if (!lookupShown && (mThisDelivery.isDealerUnavailable() || mBundle.getString("user_type").equalsIgnoreCase("dealer"))) {
				log.debug(Logs.INTERACTION, "showing 'look up' image for this delivery");
				lookUpScreen.showLookupScreen(CommonUtility.getCurrentActivity(), remoteId, mOperation,mThisLoad);
			} else {
				startSignatureActivity();
			}
		} else {
			if(mThisLoad != null && mOperation == Constants.PRELOAD_OPERATION || mOperation == Constants.SHUTTLE_LOAD_OPERATION) {
				remoteId = mThisLoad.getRemoteId();
				boolean lookupShown = PreferenceManager.getDefaultSharedPreferences(this).getBoolean(remoteId + "_load_lookup_shown", false);
				if (!lookupShown && mBundle.getString("user_type").equalsIgnoreCase("driver")) {
					log.debug(Logs.INTERACTION, "show 'look up' image for this load");
					lookUpScreen.showLookupScreen(CommonUtility.getCurrentActivity(), remoteId, mOperation,mThisLoad);
				} else {
					startSignatureActivity();
				}
			} else {
				startSignatureActivity();
			}
		}
	}

	// This method was introduced in v2.4 to make drivers aware a change in policy requiring
	// a signature on the first leg of a relay load (on the delivery screen).  It will display
	// a reminder prior to the driver signature on all new preloads for front-end relay loads
	// until: 1) The driver has delivered more than five front-end relay loads AND 2) it has
	// been at least 17 days since the driver's first preload after the change.
	//
	// TODO: Remove shared preference (ProcessChange-[driver-num]) Removed dialog display in 2.8.
	/*
	private boolean isProcessChangeDialogNeeded() {
		if (mOperation == Constants.PRELOAD_OPERATION && mThisLoad != null && mThisLoad.originLoad && !mThisLoad.isSVLoadType()) {
			try {
				final String processChangePref = "ProcessChange-" + CommonUtility.getDriverNumber(this);
				SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());

				String lastRelayLoad;
				long firstReminderTime;
				int reminderCount;
				long currentTime = System.currentTimeMillis() / 1000;
				final long REMINDER_TIMEOUT_PERIOD = 60 * 60 * 24 * 17; // 17 days
				final long LOAD_REMINDER_MAX = 5;

				String processChangeVal = prefs.getString(processChangePref, "");
				if (CommonUtility.isNullOrBlank(processChangeVal)) {
					firstReminderTime = currentTime;
					reminderCount = 0;
				}
				else {
					String parts[] = processChangeVal.split(",", 3);
					lastRelayLoad = parts[0];
					firstReminderTime = Long.parseLong(parts[1]);
					reminderCount = Integer.parseInt(parts[2]);
					if (lastRelayLoad.equals(mThisLoad.loadNumber)) {
						return false;
					}
				}
				lastRelayLoad = mThisLoad.loadNumber;
				reminderCount++;

				SharedPreferences.Editor editor = prefs.edit();
				String val = String.format("%s,%d,%d", lastRelayLoad, firstReminderTime, reminderCount);
				editor.putString(processChangePref, val);
				editor.commit();

				if (reminderCount <= LOAD_REMINDER_MAX || (currentTime - firstReminderTime) <= REMINDER_TIMEOUT_PERIOD) {
					return true;
				}
			} catch (Exception e) {
				log.debug(Logs.DEBUG, "isProcessChangeDialogNeeded() exception: " + e.getClass().getSimpleName());
			}
		}
		return false;
	}
	*/

	public void startSignatureActivity() {
		Intent intent = new Intent(this, SignatureActivity.class).putExtras(mBundle);
		/*
		if (isProcessChangeDialogNeeded()) {
			final Activity thisActivity = this;

			InformationDialog informationDialog = new InformationDialog(DeliveryVinInspectionActivity.this,
					"Attention - Process Change!",
					"Read Before Continuing",
					"Due to changes related to pay on delivery, you are now required to sign when you deliver relay loads, just as you do for regular loads.\n\n" +
							"When you arrive at the destination, be sure to open the delivery portion of this app and sign for the delivery.",
					new InformationDialog.IInfoDialogCallback() {
						@Override
						public void onAcknowledgeButton(boolean stopShowing) {
							log.debug(Logs.DEBUG, "Ack button pressed. stopShowing=" + stopShowing);
							thisActivity.startActivityForResult(intent, REQ_CODE_SIGNATURE);
						}
					});
			informationDialog.show();
		}
		else {
			this.startActivityForResult(intent, REQ_CODE_SIGNATURE);
		}
		 */
		this.startActivityForResult(intent, REQ_CODE_SIGNATURE);
	}

	@Override
	public void onReviewButton(View v) {
		super.onReviewButton(v);
		CommonUtility.logButtonClick(log, v);
		Intent intent = new Intent(this, SignatureActivity.class);

		intent.putExtra(Constants.CURRENT_OPERATION, mOperation);
		intent.putExtra("user_type", "dealer");
		intent.putExtra("delivery_id", mThisDelivery.delivery_id);
		intent.putExtra("is_editable", false);
		this.startActivity(intent);
	}

	private void startHighClaimsActivity() {
		if (mThisLoad == null) {
			log.debug(Logs.INTERACTION, "HighClaimsActivity attempted but mThisLoad was null. Doing nothing.");
			return;
		}
		log.debug(Logs.INTERACTION, "Starting HighClaimsAuditActivity.");

		mBundle.putInt("load_id", mThisLoad.load_id);
		Intent intent = new Intent(this, HighClaimsAuditActivity.class)
				.putExtras(mBundle);
		this.startActivityForResult(intent, REQ_CODE_HIGH_CLAIMS_AUDIT);
	}

	@Override
	public void onCustomPickButton(View v) {
		super.onCustomPickButton(v);
		startHighClaimsActivity();
	}

	@Override
	public void back(View v) {

		if (mThisDelivery != null && (mThisDelivery.shuttleLoad || mThisLoad.originLoad || mThisDelivery.dealerSignature != null)
				&& mThisDelivery.hasNewDeliveryDamages()
				&& HelperFuncs.isNullOrEmpty(mThisDelivery.getNotes())) {

			log.debug(Logs.INTERACTION, "VINListActivity: Back button pressed but explanation of loss or damage required");
			String msg;
			msg = "You must enter an explanation of your loss or damage.";

			CommonUtility.simpleMessageDialog(this, msg);
		} else {
			if (displayMode == DisplayMode.VIN_PICK_MODE
					&& mInspectionStatus != InspectionStatus.UNINSPECTED_VINS_REMAINING) {
				finish();
				return;
			}
			log.debug(Logs.DEBUG, "VINListActivity: Back button pressed");
			if (displayMode == DisplayMode.VIN_PICK_MODE) {
				displayMode = DisplayMode.VIN_GET_MODE;
				mListState = mExpListView.onSaveInstanceState();
				drawLayout(true);
			} else {
				log.debug(Logs.DEBUG, "pressed 'back'");
				finish();
				return;
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		log.debug(Logs.DEBUG,
				"DeliveryVinInspectionActivity: onActivityResult requestCode="
						+ requestCode + "resultCode=" + resultCode);

		switch (requestCode) {

			case REQ_CODE_VIN_INSPECTION:
				if (mInspectionStatus == InspectionStatus.UNINSPECTED_VINS_REMAINING) {
					if (resultCode == RESULT_OK) {
						displayMode = DisplayMode.VIN_GET_MODE;
					}
				}
				break;

			case REQ_CODE_HIGH_CLAIMS_AUDIT:
				if (resultCode == RESULT_OK) {
					log.debug(Logs.INTERACTION, "got high claims audit");
				} else if (resultCode == HighClaimsAuditActivity.RESULT_CODE_AUDIT_DID_NOT_PASS) {
					log.debug(Logs.INTERACTION, "got high claims audit with supervisor corrections");
				}
				CommonUtility.showText("Supervisor audit saved", Toast.LENGTH_LONG);
				break;
			case REQ_CODE_LOSS_AND_DAMAGE:
				if (resultCode == RESULT_OK) {
					log.debug(Logs.INTERACTION, "got loss and damage note");
					boolean calledTerminal = data.getBooleanExtra(LossAndDamageActivity.EXTRA_TERM_CALLED, false);
					StringBuilder note = new StringBuilder("\nCalled terminal: ");
					note.append(calledTerminal ? "YES" : "NO");
					note.append("\n");
					if (calledTerminal) {
						note.append("\nSpoke with: " + data.getStringExtra(LossAndDamageActivity.EXTRA_SPOKE_WITH) + "\n");
						log.debug(Logs.INTERACTION, "user called and spoke with " +
								data.getStringExtra(LossAndDamageActivity.EXTRA_SPOKE_WITH));
					} else {
						log.debug(Logs.INTERACTION, "user did not call");
					}
					note.append("\nDamage explanation: " + data.getStringExtra(LossAndDamageActivity.EXTRA_EXPLANATION));
					log.debug(Logs.INTERACTION, "Damage explanation: " + data.getStringExtra(LossAndDamageActivity.EXTRA_EXPLANATION));

					note.append("\nDamaged VINs:\n");
					for (DeliveryVin vin : mThisDelivery.getDeliveryVinList()) {
						if (vin.hasNewDamagesOfType(false)) {
							note.append(vin.vin.vin_number + "\n");
						}
					}

					SupplementalNotesActivity.addNoteToVehicleBatch(this, mThisDelivery, note.toString());
				}
				break;
			case REQ_CODE_SAFE_DELIVERY:
				if (resultCode == RESULT_OK) {
					log.debug(Logs.INTERACTION, "got safe delivery area note");
					boolean safeArea = data.getBooleanExtra(SafeDeliveryActivity.SAFE_AREA, true);
					StringBuilder note = new StringBuilder("Safe Area: ");
					note.append(safeArea ? "YES" : "NO");
					note.append("\n");
					if (!safeArea) {
						note.append("No safe area explanation: " + data.getStringExtra(SafeDeliveryActivity.EXTRA_EXPLANATION));
						log.debug(Logs.INTERACTION, "No safe area explanation: " + data.getStringExtra(SafeDeliveryActivity.EXTRA_EXPLANATION));
					}
					mThisDelivery.safeDelivery = note.toString();
					DataManager.saveSafeDelivery(this, mThisDelivery.getId(), note.toString());
					mBundle.putString("user_type", "driver");
					collectSignature();
				}
				break;
			case REQ_CODE_CAPTURE_IMAGE:
				if (resultCode != RESULT_OK) {
					break;
				}

				mCurrentPhotoFileName = mCurrentPhotoFileName + "_hires";
				log.debug(Logs.INTERACTION, "got image: " + mCurrentPhotoFileName);

				Bitmap newImage = mImageHandler.buildHiresBitmap(mCurrentPhotoFileName);

				if(newImage == null) {
					log.debug(Logs.DEBUG, mCurrentDocType + " image " + mCurrentPhotoFileName + " NOT FOUND!");
				    CommonUtility.showText(mCurrentDocType + " image capture failed for load " + (mThisLoad != null ? mThisLoad.loadNumber: "") + ". Please try again.");
				    break;
                }

				Bitmap thumbnail = CommonUtility.getBitmapThumbnail(this, CommonUtility.cachedImageFileFullPath(this, mCurrentPhotoFileName));
				thumbnail = HelperFuncs.addWatermarkOnBottom(thumbnail,"Preload " + mThisLoad.loadNumber, mCurrentDocType, 1);

				final String hiresDocPhotoFileName = mCurrentPhotoFileName;

				//Add watermark to high res image and save asynchronously
				AsyncTask<Bitmap, Void, Void> saveBigBitmap = new AsyncTask<Bitmap, Void, Void>() {
					@Override
					protected Void doInBackground(Bitmap... bitmaps) {

						Bitmap docImageBitmap = HelperFuncs.addWatermarkOnBottom(bitmaps[0], "Preload " + mThisLoad.loadNumber, mCurrentDocType, 8);

						log.debug(Logs.DEBUG, "Saving hires " + mCurrentDocType + " image with watermark " + hiresDocPhotoFileName);
						CommonUtility.saveBitmap(docImageBitmap, CommonUtility.cachedImageFileFullPath(DeliveryVinInspectionActivity.this, hiresDocPhotoFileName));

						return null;
					}
				};

				if(newImage != null) {
					saveBigBitmap.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, newImage);
				}

				// Save an image record for the hires image
//				Image image = new Image();
//				image.preloadImage = true;
//				image.load_id = Integer.parseInt(mThisLoad.getRemoteId());
//				image.filename = mCurrentPhotoFileName;
//				DataManager.insertImageToLocalDB(this, image);

				// Now, create the low-res image file and its image record
				mCurrentPhotoFileName = mCurrentPhotoFileName.replace("_hires", "");
				CommonUtility.saveBitmap(thumbnail, CommonUtility.cachedImageFileFullPath(this, mCurrentPhotoFileName));

				Image lowResImage = new Image();
				lowResImage.preloadImage = true;
				lowResImage.load_id = Integer.parseInt(mThisLoad.getRemoteId());
				lowResImage.filename = mCurrentPhotoFileName;
				DataManager.insertImageToLocalDB(this, lowResImage);
				break;
			case REQ_CODE_SIGNATURE:
				if (mOperation == Constants.DELIVERY_OPERATION && resultCode == SignatureActivity.RESULT_CODE_REVIEW) {
					log.debug(Logs.INTERACTION, "got signature");
					Delivery dSignature = DataManager.getDelivery(this, mThisDelivery.delivery_id);
					mThisDelivery.dealerSignature = dSignature.dealerSignature;
					mThisDelivery.dealerComment = dSignature.dealerComment;
					mThisDelivery.dealerContact = dSignature.dealerContact;
					mThisDelivery.dealerSignatureSignedAt = dSignature.dealerSignatureSignedAt;
					mThisDelivery.dealerSignatureLat = dSignature.dealerSignatureLat;
					mThisDelivery.dealerSignatureLon = dSignature.dealerSignatureLon;
				}
				break;
			case REQ_CODE_VIEW_LOT_CODE_MSG:
				if (resultCode != RESULT_OK) {
					onBackPressed();
				}
			default:
				break;
		}
	}

	private void finishActivity() {
		setResult(RESULT_OK);
		finish();
	}

	private static class DeliveryVinPositionComparator implements
			Comparator<DeliveryVin> {

		@Override
		public int compare(DeliveryVin lhs, DeliveryVin rhs) {

			if (lhs.position == null || lhs.position.equals("null")
					|| lhs.position.trim().equals("")) {
				return 0;
			} else if (rhs.position == null || rhs.position.equals("null")
					|| rhs.position.trim().equals("")) {
				return 1;
			} else if (Integer.valueOf(lhs.position) > Integer
					.valueOf(rhs.position))
				return 1;
			else
				return -1;
		}

	}

	private static boolean locationConnected = false;

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
