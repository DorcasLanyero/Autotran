package com.cassens.autotran.activities;

import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.adapters.LoadListAdapter;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.sdgsystems.util.SimpleTimeStamp;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

public class ClearLoadActivity extends AutoTranActivity implements OnClickListener {
	
	static String OPERATION_CLEAR_LOAD = "ClearLoad";

	private static final Logger log = LoggerFactory.getLogger(ClearLoadActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	private int driver_id;

	private List<Load> loadList;
	private Context ctx;
	private LoadListAdapter adapter;
	private String driverNumber;
	private OnItemClickListener clearLoadListener = new OnItemClickListener() {
		@Override
		public void onItemClick(AdapterView<?> arg0, View arg1,
				final int load_id, long arg3) {
			try {
				final Load load = loadList.get(load_id);

				String prompt;
				String title;
				Builder builder = new AlertDialog.Builder(ctx);
				if (load.driverPreLoadSignature == null) {
					title = "Clear Load?";
					if (hasImagesToUpload(load)) {
						prompt = "Load " + load.loadNumber + " has images that are not uploaded yet. " +
								 "Are you sure you want to hide" +
								 " or clear it?";

					} else {
						prompt = "Do you wish to hide or clear load " + load.loadNumber
								 + "?  Clearing will preserve images and damages already recorded.";
					}
					log.debug(Logs.INTERACTION, "Dialog: " + prompt);
					builder.setNeutralButton("Clear",
							new DialogInterface.OnClickListener() {

								@Override
								public void onClick(DialogInterface dialog,
										int which) {
									String msg = String.format("Supervisor %s cleared load %s via %s", supervisorDriverNum, load.loadNumber, ClearLoadActivity.class.getSimpleName());
									CommonUtility.logButtonClick(log, "Clear", msg);
									CommonUtility.highLevelLog(msg);

									sendLoadEvent(load, "LoadClear");

									clearLoad(ctx, load, true, true, true);
									loadList.remove(load_id);
									adapter.notifyDataSetChanged();
								}
							});
				} else {
					title = "Hide Load?";
					if (hasImagesToUpload(load)) {
						prompt = "Load " + load.loadNumber + " has images that are not " +
								"uploaded yet. Are you sure you want to hide it?";
						builder.setMessage(prompt);
						log.debug(Logs.INTERACTION, "Hide Load Dialog" + prompt);
					} else {
						prompt = "Do you wish to hide load " + load.loadNumber + "?";
						builder.setMessage(prompt);
						log.debug(Logs.INTERACTION, prompt);
					}
				}
				builder.setTitle(title);
				builder.setMessage(prompt);
				builder.setPositiveButton("Hide",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog,
									int which) {
								String msg = String.format("Supervisor %s hid load %s via %s", supervisorDriverNum, load.loadNumber, this.getClass().getSimpleName());
								CommonUtility.logButtonClick(log, "Hide", msg);
								CommonUtility.highLevelLog(msg);

								sendLoadEvent(load, "LoadHide");

								hideOrUnhideLoadAndChildren(ctx, load, driverNumber, true);
								if (load.isParentLoad()) {
									Iterator<Load> iterator = loadList.iterator();
									while (iterator.hasNext()) {
										Load thisLoad = iterator.next();
										if (thisLoad.loadNumber.equals(load.loadNumber)
											|| StringUtils.startsWith(thisLoad.loadNumber, load.loadNumber + "-")) {
											iterator.remove();
										}
									}
								}
								else {
									loadList.remove(load_id);
								}
								adapter.notifyDataSetChanged();
							}


						});
				builder.setNegativeButton("No",
						new DialogInterface.OnClickListener() {

							@Override
							public void onClick(DialogInterface dialog, int which) {
								CommonUtility.logButtonClick(log, "No", "User chose to hide load " + load.loadNumber);
							}
						});
				log.debug(Logs.INTERACTION, String.format("Dialog: %s: %s", title, prompt));
				builder.create().show();
			} catch (Exception e) {
				log.debug(Logs.INTERACTION, "Error: Got exception in onItemClick() while attempting to display dialog");
				e.printStackTrace();
			}
		}
	};
	private String supervisorDriverNum;

	public ClearLoadActivity() {
		ctx = this;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_clearload);

		String supervisorInfo = null;
		if(getIntent().hasExtra("supervisorInfo")) {
			supervisorInfo = getIntent().getStringExtra("supervisorInfo");
		}
		supervisorDriverNum = "?";
		if (supervisorInfo != null) {
			User supervisor = DataManager.getUserForSupervisorCode(ClearLoadActivity.this, supervisorInfo);
			if (supervisor != null) {
				supervisorDriverNum = supervisor.driverNumber;
			}
		}
	}

	@Override
	protected void onResume() {
		super.onResume();
		((ImageView) findViewById(R.id.img_back)).setOnClickListener(this);
		TextView title = (TextView) findViewById(R.id.clearLoadTitle);
		ListView loadListview = ((ListView) findViewById(R.id.ClearLoadList));

        driverNumber = getIntent().getStringExtra("driverNumber");
        User driver = DataManager.getUserForDriverNumber(getApplicationContext(), driverNumber);
        driver_id = driver.user_id;

		loadList = DataManager.getAllLoads(this, driver_id);

		Collections.sort(loadList);
		Collections.reverse(loadList);

		adapter = new LoadListAdapter(this, R.layout.load_or_delivery_element,
				loadList);
		loadListview.setAdapter(adapter);

		title.setText(R.string.clearload);

		loadListview.setOnItemClickListener(clearLoadListener);
	}

	private static boolean hideOrUnhideLoad(Context context, int load_id, String driverNumber, boolean hide) {
		User driver = DataManager.getUserForDriverNumber(context, driverNumber);
		if (driver == null) {
			return false;
		}
		if (DataManager.updateLoadDriverId(context, load_id, hide ? 0 : driver.user_id) != 1) {
			return false;
		}
		return true;
	}

	public static boolean hideOrUnhideLoadAndChildren(final Context ctx, Load load, String driverNumber, boolean hide) {
		boolean retVal = true;
		if (load.parentLoad) {
			List<Load> childLoads = DataManager.getChildLoadsOfLoad(ctx, load);
			for (Load childLoad : childLoads) {
				if (!hideOrUnhideLoad(ctx, childLoad.load_id, driverNumber, hide)) {
					retVal = false;
				}
			}
		}
		if (!hideOrUnhideLoad(ctx, load.load_id, driverNumber, hide)) {
			retVal = false;
		}
		return retVal;
	}

	public static void clearLoad(final Context ctx, final Load load, boolean setClearedFlag, boolean preserveDriverSignatures, boolean preserveDriverNotes) {
		if (load == null) {
			return;
		}

		if (setClearedFlag) {
			load.status = "cleared";
		}

		log.debug(Logs.DEBUG, "clearing load " + load.load_id + " " + setClearedFlag);

		load.preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
		if (!preserveDriverSignatures) {
			load.driverPreLoadSignature = null;
			load.driverPreLoadSignatureLat = "";
			load.driverPreLoadSignatureLon = "";
			load.driverPreLoadSignatureSignedAt = "";
			if (!preserveDriverNotes) {
				load.driverPreLoadComment = "";
			}
		}


		for (Delivery delivery : load.deliveries) {
			log.debug(Logs.DEBUG, "clearing delivery " + delivery.delivery_id + " " + setClearedFlag);

			if (setClearedFlag) {
				delivery.status = "cleared";
			}

			delivery.preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
			delivery.deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;

			if (!preserveDriverSignatures) {
				delivery.driverSignature = null;
				delivery.driverSignatureLat = "";
				delivery.driverSignatureLon = "";
				delivery.driverSignatureSignedAt = "";
				if (!preserveDriverNotes) {
					delivery.driverComment = "";
				}
			}

			for (DeliveryVin deliveryVin : delivery.deliveryVins) {
				log.debug(Logs.DEBUG, "clearing delivery vin " + deliveryVin + " " + setClearedFlag);

				if(setClearedFlag) {
					deliveryVin.status = "cleared";
				}

				deliveryVin.preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
				deliveryVin.deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;

				//deliveryVin.supervisorSignature = null; // Never reset a supervisorSignature


				for (Image image : deliveryVin.images) {
					log.debug(Logs.DEBUG, "cleared image for vin " + deliveryVin);
					image.preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
					image.deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
					image.uploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED;
				}

				for (Damage damage : deliveryVin.damages) {
					String damageInfo = "area: " + damage.getAreaCode() + " type: "
							+ damage.getTypeCode() + " severity: " + damage.getSeverityCode();
					log.debug(Logs.DEBUG, "cleared damage " + damageInfo + " from vin " + deliveryVin);
					damage.preloadUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD;
					damage.deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
				}
			}
		}
		DataManager.insertLoadToLocalDB(ctx, load, false);
	}

	public static void clearLoadAndChildren(final Context ctx, Load load) {
		if (load.parentLoad) {
			List<Load> childLoads = DataManager.getChildLoadsOfLoad(ctx, load);
			for (Load childLoad : childLoads) {
				clearLoadAndChildren(ctx, childLoad);
			}
		}
		if (load.driverPreLoadSignature == null) {
			clearLoad(ctx, load, true, true, true);
		}
	}

	private void sendLoadEvent(Load load, String action) {
		SimpleTimeStamp sts = new SimpleTimeStamp();
		HashMap<String,String> reqBody = new HashMap<>();

		int preloadImageCount = 0;
		int deliveryImageCount = 0;
		int preloadSupplementalImageCount = 0;
		int deliverySupplementalImageCount = 0;
		int preloadDamageCount = 0;
		int deliveryDamageCount = 0;

		if(load.images != null) {
			preloadSupplementalImageCount = load.images.size();
		}

		for(Delivery delivery : load.deliveries) {
			for(DeliveryVin dv : delivery.deliveryVins) {
				for(Image image : dv.images) {
					if(image.preloadImage) {
						preloadImageCount++;
					} else {
						deliveryImageCount++;
					}
				}

				for(Damage damage : dv.damages) {
					if(damage.preLoadDamage) {
						preloadDamageCount++;
					} else {
						deliveryDamageCount++;
					}
				}
			}

			if(delivery.images != null) {
				deliverySupplementalImageCount = delivery.images.size();
			}
		}

		String eventString = TextUtils.join(",",
				new String[]{
						action,
						supervisorDriverNum,
						load.driver.driverNumber,
						load.loadNumber,
						sts.getUtcDateTime(),
						sts.getUtcTimeZone(),
						"plImgs:" + preloadImageCount,
						"dvImgs:" + deliveryImageCount,
						"plSupImgs:" + preloadSupplementalImageCount,
						"dvSupImgs:" + deliverySupplementalImageCount,
						"plDmgs:" + preloadDamageCount,
						"dvDmgs:" + deliveryDamageCount
				});

		reqBody.put("csv", eventString);

		LoadEvent event = new LoadEvent();
		event.csv = eventString;
		DataManager.insertLoadEvent(this, event);
		SyncManager.pushLoadEventsLatched(getApplicationContext());
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.img_back) {
			log.debug(Logs.INTERACTION, "Back pressed");
			this.finish();
		} else {
		}

	}

	public boolean hasImagesToUpload(Load load) {
		boolean remaining = false;
		for(Image image : load.images) {
			if(image.s3_upload_status != Constants.SYNC_STATUS_UPLOADED) {
				remaining = true;
				break;
			}
		}

		for(Delivery tmpDelivery : load.deliveries) {
			for (DeliveryVin dv : tmpDelivery.deliveryVins) {
				for (Image image : dv.images) {
					if(image.s3_upload_status != Constants.SYNC_STATUS_UPLOADED) {
						remaining = true;
						break;
					}
				}
			}

			for (Image image : tmpDelivery.images) {
				if(image.s3_upload_status != Constants.SYNC_STATUS_UPLOADED) {
					remaining = true;
					break;
				}
			}
		}

		return remaining;
	}
}
