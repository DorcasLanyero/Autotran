package com.cassens.autotran.activities;

import android.app.ActionBar.LayoutParams;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.google.android.gms.common.internal.service.Common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.RejectedExecutionException;

public class UploadStatusActivity extends AutoTranActivity implements OnClickListener {
    private static final Logger log = LoggerFactory.getLogger(UploadStatusActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	private static final String TAG = "UploadStatus";
	String driverNumber = "";

	boolean mPreload = false;
	private int mLoadLimit = 10;
	private boolean mExcludeNormalLoadStates = true;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_upload_status);

		findViewById(R.id.img_back).setOnClickListener(this);
		findViewById(R.id.btnupload).setOnClickListener(this);
		findViewById(R.id.btn_switchView_Preload).setOnClickListener(this);
		findViewById(R.id.btn_switchView_Delivery).setOnClickListener(this);

		mPreload = false;
		updateStatusOutput();
	}

	@Override
	protected void onResume() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.SYNC_STATUS_UPDATED_DATA);
		registerReceiver(receiver, filter);
		super.onResume();
	}

	@Override
	protected void onPause() {
		unregisterReceiver(receiver);
		super.onPause();
	}

	@Override
	public void onBackPressed() {
		log.debug(Logs.INTERACTION, "Back pressed");
		super.onBackPressed();
	}

	private void updateStatusOutput() {
		driverNumber = getIntent().getStringExtra("driverNumber");

		LinearLayout uploadStatusView = findViewById(R.id.uploadStatusLog);

		uploadStatusView.removeAllViews();
		TextView progressTextView = new TextView(this);
		progressTextView.setTextSize(20);
		progressTextView.setTextColor(Color.BLACK);
		progressTextView.setText("Loading items, please wait...");

		uploadStatusView.addView(progressTextView);

		AsyncTask<Void, Void, List<Load>> task = new AsyncTask<Void, Void, List<Load>>() {
			@Override
			protected List<Load> doInBackground(Void... voids) {
				// Get the loads for all users on this tablet--not just the current user.

				List<Load> loads = DataManager.getAllLoads(UploadStatusActivity.this.getApplicationContext(),
						-1, true, mLoadLimit, mExcludeNormalLoadStates, mPreload);

				return loads;
			}

			@Override
			protected void onPostExecute(List<Load> loads) {
				displayUploadInfo(loads);

				return;
			}
		};

		try {
			task.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
		} catch (RejectedExecutionException | IllegalStateException e) {
			// We got occasional crashes due to RejectedExecutionException. For now,
			// we just ignore them and catch the changes on the next broadcast or refresh.
			// We can revisit this on the planned redesign of this feature and/or when we
			// replace AsyncTasks, which are now deprecated).
		}
	}

	private void displayUploadInfo(List<Load> loads) {
		LinearLayout uploadStatusView = findViewById(R.id.uploadStatusLog);

		Collections.sort(loads);
		Collections.reverse(loads);

		uploadStatusView.removeAllViews();

		final Context context = this;

		for (final Load load : loads) {
			TextView loadView = new TextView(this);
			loadView.setTextSize(20);
			loadView.setTextColor(getStatusColor(mPreload ? load.preloadUploadStatus
					: load.deliveryUploadStatus));

			//if this is a delivery, the load should be green if all of the deliveries are green
			boolean allDelivered = true;
			if(!mPreload) {
				for(Delivery delivery : load.deliveries) {
					if(delivery.deliveryUploadStatus != Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY) {
						allDelivered = false;
					}
				}

				if(allDelivered) {
					loadView.setTextColor(Color.rgb(10, 150, 10));
				}
			}

			loadView.setText("Load: " + load.loadNumber);

			if ((getStatusColor(mPreload ? load.preloadUploadStatus
					: load.deliveryUploadStatus) != Color.BLUE) || mPreload) {

				loadView.setOnClickListener(new OnClickListener() {

					@Override
					public void onClick(View arg0) {
						Builder builder = new Builder(
								UploadStatusActivity.this);
						// builder.setTitle("Notification");
						if (getStatusColor(mPreload ? load.preloadUploadStatus
								: load.deliveryUploadStatus) != Color.BLUE) {

							String message = load.load_remote_id + " with deliveries: " + load.deliveries.size();

							builder.setMessage("Do you want to attempt to reupload this load? (remote id: " + message + ")  Note: this won't work if the VINs and damages haven't been uploaded");
							builder.setPositiveButton("Yes",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog,	int which) {
											SyncManager.uploadLoad(UploadStatusActivity.this, load, false, true);
										}
									});
							builder.setNegativeButton("No",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog,
												int which) { /* do nothing */
										}
									});
						} else if(mPreload) {
							TextView dialogText = new TextView(context);
							dialogText.setGravity(Gravity.CENTER_HORIZONTAL);
							dialogText.setPadding(10, 10, 10, 10);
							dialogText.setTextSize(20);

							String msg = "This load cannot be uploaded due to the following reasons:\n\n";
							if (!load.isInspected()) {
								msg += "There are uninspected VINs.\n";
							}
							if (load.driverPreLoadSignature == null || load.driverPreLoadSignature.isEmpty()) {
								msg += "Missing driver signature.";
							}
							dialogText.setText(msg);
							builder.setView(dialogText);
							builder.setPositiveButton("Dismiss",
									new DialogInterface.OnClickListener() {
										@Override
										public void onClick(DialogInterface dialog, int which) {}
									});
						}
						builder.setCancelable(true);
						builder.create().show();

					}
				});
			}

			uploadStatusView.addView(loadView);

			for (final Delivery delivery : load.deliveries) {
				LinearLayout.LayoutParams llp;
				if (!mPreload) {
					TextView deliveryView = new TextView(this);
					deliveryView.setTextSize(20);
					deliveryView
							.setTextColor(getStatusColor(mPreload ? delivery.preloadUploadStatus
									: delivery.deliveryUploadStatus));

					if (delivery.dealer != null
							&& delivery.dealer.customer_name != null)
						deliveryView.setText("Dealer: "
								+ delivery.dealer.getDealerDisplayName());
					else {
						deliveryView.setText("Dealer: --not specified--");
					}
					llp = new LinearLayout.LayoutParams(
							LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT);
					llp.setMargins(20, 0, 0, 0);
					deliveryView.setLayoutParams(llp);

					// We should allow deliveries to be force-uploaded as long
					// as they have both signatures
					if (delivery.driverSignature != null
							&& delivery.driverSignature.length() > 0
							&& delivery.dealerSignature != null
							&& delivery.dealerSignature.length() > 0) {
						deliveryView.setOnClickListener(new OnClickListener() {

							@Override
							public void onClick(View arg0) {
								Builder builder = new Builder(
										UploadStatusActivity.this);
								// builder.setTitle("Notification");
								builder.setMessage("Do you want to attempt to force the upload for this delivery to "
										+ (delivery.dealer != null ? delivery.dealer.customer_name : "--not specified--") + "?");
								builder.setPositiveButton("Yes",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) {
												for (DeliveryVin deliveryVin : delivery.deliveryVins) {
													deliveryVin.inspectedDelivery = true;
													deliveryVin.deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
													DataManager
															.insertDeliveryVinToLocalDB(
																	UploadStatusActivity.this,
																	deliveryVin,
																	true);
												}
												log.debug(Logs.DEBUG,
														"all delivery vins accounted for, uploading delivery...");

												delivery.deliveryUploadStatus = Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY;
												SyncManager
														.uploadDelivery(
																UploadStatusActivity.this,
																delivery,
																load.load_id);
											}


										});
								builder.setNegativeButton("No",
										new DialogInterface.OnClickListener() {
											@Override
											public void onClick(
													DialogInterface dialog,
													int which) { /* do nothing */
											}
										});
								builder.setCancelable(true);
								builder.create().show();

							}
						});
					}

					uploadStatusView.addView(deliveryView);
				}

				for (final DeliveryVin deliveryVin : delivery.deliveryVins) {
					TextView deliveryVinView = new TextView(this);
					deliveryVinView.setTextSize(20);
					deliveryVinView
							.setTextColor(getStatusColor(mPreload ? load.preloadUploadStatus
									: delivery.deliveryUploadStatus));
					deliveryVinView
							.setText("vin:" + deliveryVin.vin.vin_number);
					llp = new LinearLayout.LayoutParams(
							LayoutParams.WRAP_CONTENT,
							LayoutParams.WRAP_CONTENT);
					llp.setMargins(40, 0, 0, 0);
					deliveryVinView.setLayoutParams(llp);

					uploadStatusView.addView(deliveryVinView);


					for (final Damage damage : deliveryVin.damages) {
						if ((mPreload && damage.preLoadDamage)
								|| (!mPreload && !damage.preLoadDamage)) {
							TextView damageView = new TextView(this);
							damageView.setTextSize(20);
							damageView
									.setTextColor(getStatusColor(mPreload ? load.preloadUploadStatus
											: delivery.deliveryUploadStatus));

							String damagePrefix = "";

							if (mPreload)
								damagePrefix = "Preload Damage: ";
							else
								damagePrefix = "Delivery Damage: ";

							if (!mPreload && !damage.preLoadDamage) {
								if (damage.specialCode == null)
									damagePrefix += damage.areaCode.getCode() + ","
											+ damage.typeCode.getCode() + ","
											+ damage.severityCode.getCode();
								if ((damage.specialCode != null))
									damagePrefix += damage.specialCode.getAreaCode()
											+ "," + damage.specialCode.getTypeCode()
											+ ","
											+ damage.specialCode.getSeverityCode();
							} else if (mPreload && damage.preLoadDamage) {
								if (damage.specialCode == null)
									damagePrefix += damage.areaCode.getCode() + ","
											+ damage.typeCode.getCode() + ","
											+ damage.severityCode.getCode();
								if ((damage.specialCode != null))
									damagePrefix += damage.specialCode.getAreaCode()
											+ "," + damage.specialCode.getTypeCode()
											+ ","
											+ damage.specialCode.getSeverityCode();
							}

							damageView.setText(damagePrefix);
							llp = new LinearLayout.LayoutParams(
									LayoutParams.WRAP_CONTENT,
									LayoutParams.WRAP_CONTENT);
							llp.setMargins(60, 0, 0, 0);
							damageView.setLayoutParams(llp);

							uploadStatusView.addView(damageView);
						}
					}

					for (final Image image : deliveryVin.images) {
						addImage(image, uploadStatusView);
					}
				}

				for (final Image image : delivery.images) {
					addImage(image, uploadStatusView);
				}
			}

			for (final Image image : load.images) {
				addImage(image, uploadStatusView);
			}

			TextView blankView = new TextView(this);
			blankView.setText(" ");
			uploadStatusView.addView(blankView);
		}

		Button moreButton = new Button(this);


		if(!mExcludeNormalLoadStates) {
			moreButton.setText("Show All");
			moreButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mLoadLimit = -1;
					updateStatusOutput();
				}
			});
		} else {
			moreButton.setText("Show Top 10");
			moreButton.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					mExcludeNormalLoadStates = false;
					updateStatusOutput();
				}
			});
		}

		moreButton.setBackgroundResource(R.drawable.button_small);
		moreButton.setTextSize(16);
		moreButton.setTextColor(Color.WHITE);
		moreButton.setTypeface(Typeface.DEFAULT_BOLD);

		if(mLoadLimit != -1) {
			uploadStatusView.addView(moreButton);
		}
	}

	private void addImage(final Image image, LinearLayout uploadStatusView) {
		if ((mPreload && image.preloadImage)
				|| (!mPreload && !image.preloadImage)) {
			TextView imageView = new TextView(this);
			imageView.setTextSize(20);
			final Image latestImage = DataManager.getImage(
					this, image.image_id);
			imageView
					.setTextColor(getStatusColor(mPreload ? latestImage.preloadUploadStatus
							: latestImage.deliveryUploadStatus));
			String imagePrefix = "";

			if (mPreload && image.preloadImage)
				imagePrefix = "Preload Image: ";
			else if (!mPreload && !image.preloadImage)
				imagePrefix = "Delivery Image: ";

			imageView.setText(imagePrefix
					+ image.filename/*latestImage.image_id/* + " "
									+ (latestImage.uploadIndex + 1) + "/"
									+ Constants.NUMBER_OF_IMAGE_CHUNKS + "pl:" + image.preloadUploadStatus + " dl:" + image.deliveryUploadStatus + " us:" + image.uploadStatus + " s3:" + image.s3_upload_status*/);
			LinearLayout.LayoutParams llp = new LinearLayout.LayoutParams(
					LayoutParams.WRAP_CONTENT,
					LayoutParams.WRAP_CONTENT);
			llp.setMargins(60, 0, 0, 0);
			imageView.setLayoutParams(llp);

			imageView.setOnClickListener(new OnClickListener() {

				@Override
				public void onClick(View arg0) {
					Builder builder = new AlertDialog.Builder(
							UploadStatusActivity.this);
					// builder.setTitle("Notification");
					builder.setMessage("Do you want to attempt to reupload image "
							+ image.image_id + "?");
					builder.setPositiveButton(
							"Yes",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog,	int which) {
									DataManager	.insertImageToLocalDB(UploadStatusActivity.this, latestImage);

									log.debug(Logs.DEBUG, "skipping upload and dumping image info: ");
									log.debug(Logs.DEBUG,
											"image id "
													+ latestImage.image_id);
									log.debug(Logs.DEBUG,
											"filename: "
													+ latestImage.filename);

									SyncManager
											.uploadImage(
													UploadStatusActivity.this,
													latestImage,
													latestImage.filename,
													"",
													-1);
								}
							});
					builder.setNegativeButton(
							"No",
							new DialogInterface.OnClickListener() {
								@Override
								public void onClick(
										DialogInterface dialog,
										int which) {}
							});
					builder.setCancelable(true);
					builder.create().show();

				}
			});

			uploadStatusView.addView(imageView);
		}
	}

	private int getStatusColor(int uploadStatus) {
		switch (uploadStatus) {
		case 0:
		case Constants.SYNC_STATUS_NOT_UPLOADED_FOR_DELIVERY:
		case Constants.SYNC_STATUS_NOT_UPLOADED_FOR_PRELOAD:
			return (Color.BLUE);
		case Constants.SYNC_STATUS_UPLOADED_FOR_DELIVERY:
		case Constants.SYNC_STATUS_UPLOADED_FOR_PRELOAD:
			return (Color.rgb(10, 150, 10));
		case Constants.SYNC_STATUS_UPLOADING_FOR_DELIVERY:
		case Constants.SYNC_STATUS_UPLOADING_FOR_PRELOAD:
			return (Color.YELLOW);
		case Constants.SYNC_STATUS_UPLOAD_FAILED_FOR_DELIVERY:
		case Constants.SYNC_STATUS_UPLOAD_FAILED_FOR_PRELOAD:
			return (Color.RED);
		default:
			log.debug(Logs.DEBUG, "preloadUploadStatus: " + uploadStatus);
			return Color.LTGRAY;
		}
	}

	@Override
	public void onClick(View v) {
		int id = v.getId();
		if (id == R.id.img_back) {
			CommonUtility.logButtonClick(log, "Back");
			this.finish();
			return;
		}
		CommonUtility.logButtonClick(log, v);
		if (id == R.id.btnupload) {
			CommonUtility.showText(getString(R.string.force_upload_message));
			// Note: By passing -1 for the driver_id, we upload all the local data on the tablet--not just the data for the current driver
			CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from UploadStatusActivity on uploadButton onClick()");
			DataManager.pushLocalDataToRemoteServer(this,-1, true);
		} else if (id == R.id.btn_switchView_Preload) {
			mLoadLimit = 10;

			Button preloadButton = findViewById(R.id.btn_switchView_Preload);
			Button deliveryButton = findViewById(R.id.btn_switchView_Delivery);
			mPreload = true;

			preloadButton.setBackgroundResource(R.drawable.button_small_pressed);
			preloadButton.setEnabled(false);

			deliveryButton.setBackgroundResource(R.drawable.button_small);
			deliveryButton.setEnabled(true);


			updateStatusOutput();
		} else if (id == R.id.btn_switchView_Delivery) {
			mLoadLimit = 10;

			Button preloadButton = findViewById(R.id.btn_switchView_Preload);
			Button deliveryButton = findViewById(R.id.btn_switchView_Delivery);
			mPreload = false;

			preloadButton.setBackgroundResource(R.drawable.button_small);
			preloadButton.setEnabled(true);
			deliveryButton.setBackgroundResource(R.drawable.button_small_pressed);
			deliveryButton.setEnabled(false);

			updateStatusOutput();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		// if (resultCode == 11)
		// {
		// ((EditText)
		// findViewById(R.id.et_notes_message)).setText(data.getStringExtra("note"));
		// }
		super.onActivityResult(requestCode, resultCode, data);
	}

	private BroadcastReceiver receiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			updateStatusOutput();
		}
	};

}
