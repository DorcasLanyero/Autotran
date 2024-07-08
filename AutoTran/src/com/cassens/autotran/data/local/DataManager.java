package com.cassens.autotran.data.local;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.text.format.Time;
import android.util.Log;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.activities.DashboardActivity;
import com.cassens.autotran.backendpoc.PoCPerformanceStats;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.DamageNote;
import com.cassens.autotran.data.model.DamageNoteTemplate;
import com.cassens.autotran.data.model.Dealer;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.DriverAction;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Inspection;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.model.LotCodeMessage;
import com.cassens.autotran.data.model.PlantReturn;
import com.cassens.autotran.data.model.PredefinedNote;
import com.cassens.autotran.data.model.ProblemReport;
import com.cassens.autotran.data.model.Questionnaire;
import com.cassens.autotran.data.model.ReceivedVehicle;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.TrendingAlert;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.VIN;
import com.cassens.autotran.data.model.WMRequest;
import com.cassens.autotran.data.model.YardExit;
import com.cassens.autotran.data.model.YardInventory;
import com.cassens.autotran.data.model.lookup.AreaCode;
import com.cassens.autotran.data.model.lookup.LotCode;
import com.cassens.autotran.data.model.lookup.ScacCode;
import com.cassens.autotran.data.model.lookup.SeverityCode;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.data.model.lookup.SpecialCode;
import com.cassens.autotran.data.model.lookup.Terminal;
import com.cassens.autotran.data.model.lookup.TrainingType;
import com.cassens.autotran.data.model.lookup.TypeCode;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.data.remote.tasks.ConsolidatedDataPullTask;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleStopwatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

public class DataManager {
	private static final Logger log = LoggerFactory.getLogger(DataManager.class.getSimpleName());

	private static final boolean DEBUG = false;
	private static final String TAG = "DataManager";
	private static Transactions mTransactions;


	public static final String PREDEF_NOTES_DEFAULT_LIST = "default";

	public static void refreshDispatchData(DashboardActivity activity, String driverNumber, final int driver_id) {
		NetworkInfo info = ((ConnectivityManager) activity.getSystemService(Context.CONNECTIVITY_SERVICE)).getActiveNetworkInfo();

		CommonUtility.dispatchLogMessage( "DISPATCH STARTED");
		if (info != null && info.isConnectedOrConnecting()) {
			CommonUtility.showText("Refreshing lookup codes");
			CommonUtility.dispatchLogMessage( "refreshDispatchData: Refreshing lookup data by starting ConsolidatedDataPullTask");
			DataManager.refreshLookupCodes(activity, false, new ConsolidatedDataPullTask.IConsolidatedDataPullCallback() {
				@Override
				public void updateProgress(String status) {
					//CommonUtility.showText(status);
					CommonUtility.dispatchLogMessage("refreshLookupCodes: " + status);
				}

				@Override
				public void complete() {
					CommonUtility.showText("Lookup codes refreshed");
					CommonUtility.dispatchLogMessage("refreshLookupCodes: Complete");
				}
			});

			CommonUtility.showText("Pulling latest load data");
			CommonUtility.dispatchLogMessage( "refreshDispatchData: calling SyncManager.pullRemoteData()");
			SyncManager.pullRemoteData(activity, driverNumber);

			final Context mContext = activity.getApplicationContext();

			if (!AppSetting.PRUNE_LOADS_DAILY.getBoolean()) {
				Thread thread = new Thread() {
					@Override
					public void run() {
						CommonUtility.dispatchLogThreadStartStop("Started pruneLoads thread at end of dispatch", true);
						pruneLoads(mContext);
						CommonUtility.dispatchLogThreadStartStop("Completed pruneLoads thread at end of dispatch", false);
					}
				};
				thread.start();
			}
		} else {
			CommonUtility.showText("Not refreshing data due to lack of connectivity");
			CommonUtility.dispatchLogMessage("Dispatch not started due to lack of network connectivity");
		}
	}

	private static boolean isLoadReadyToPrune(Load load, Time timeNow) {
		//if this delivery is not uploaded and not cleared, don't delete it
		for (Delivery delivery : load.deliveries) {

			String deliveryCompletedDateString = delivery.dealerSignatureSignedAt;

			if(delivery.shuttleLoad || load.shuttleLoad) {
				deliveryCompletedDateString = delivery.driverSignatureSignedAt;
			}

			if (!delivery.uploaded && !delivery.status.equals("cleared") || deliveryCompletedDateString == null) {
				return false;
			}

			//time.format("%Y-%m-%d %H:%M:%S");
			SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd H:m:s zzz");
			Long timeLong = 0l;
			try {
				timeLong = dateFormat.parse(deliveryCompletedDateString + " UTC").getTime();
			} catch (ParseException e) {
				e.printStackTrace();
				return false; // Treat a malformed date like null.
			}
			Time time = new Time();
			time.set(timeLong);

			long deleteDays = AppSetting.LOAD_RETENTION_DAYS.getInt();
			long deleteTime = TimeUnit.MILLISECONDS.convert(deleteDays, TimeUnit.DAYS);
			if ((timeNow.toMillis(true) - time.toMillis(true)) < (deleteTime)) {
				return false;
			}

		}
		return true;
	}

	public static int pruneLoads(Context context) {
		if (!AppSetting.PRUNE_LOADS_ENABLED.getBoolean()) {
			log.debug(Logs.DELETES, "Load pruning is is disabled. Loads not pruned.");
			return -1;
		}

		User driver = DataManager.getUserForDriverNumber(context, CommonUtility.getDriverNumber(context));
		if (driver == null) {
			log.debug(Logs.DELETES, "pruneLoads() failed: Unable to determine current driver");
			return -1;
		}

		SimpleStopwatch stopwatch = new SimpleStopwatch();
		stopwatch.startTimer();

		List<Load> loadsToDelete = new ArrayList<Load>();
		Time timeNow = new Time();
		timeNow.setToNow();

		// Add old, completed loads to the delete list.
		List<Load> loads = getAllLoads(context, driver.user_id, true, -1);
		for (Load load : loads) {
			if (load.isChildLoad()) {
				// Child loads will be deleted when the parent load is deleted.
				continue;
			}
			if (isLoadReadyToPrune(load, timeNow)) {
				loadsToDelete.add(load);
			}
		}

		// Add loads that have been marked for deletion for other reasons (e.g. orphaned)
		loads = DataManager.getLoadsMarkedDeletable(context);
		for (Load load: loads) {
			loadsToDelete.add(load);
		}

		if (loadsToDelete.size() > 0) {
			for (Load load : loadsToDelete) {
				DataManager.deleteLoadAndChildren(context, load, "auto-pruning of old load");
			}
		}
		else {
			log.debug(Logs.DELETES, "No loads needed auto-pruning for driver " + CommonUtility.getDriverNumberAsInt(context));
		}

		stopwatch.stopTimer();
		PoCPerformanceStats.recordPruneLoadsQueryTime(stopwatch.getElapsedTime());
		return loadsToDelete.size();
	}

	private static void addExternalDamageNotes(Delivery delivery) {
		List<DeliveryVin> deliveryVins = delivery.getDeliveryVinList();
		for (DeliveryVin deliveryVin : deliveryVins) {
			if (deliveryVin.damages != null && deliveryVin.damages.size() > 0) {
				for (Damage damage : deliveryVin.damages) {
					if (damage.preLoadDamage) {
						if (damage.readonly && damage.source.equals("external")) {
							String note = "Damage codes " + damage.areaCode.getCode() + "-" + damage.typeCode.getCode() + "-" + damage.severityCode.getCode() + " received from inspection company \n";
							if(deliveryVin.preloadNotes == null || !deliveryVin.preloadNotes.contains(note)) {
								log.debug(Logs.DEBUG, "Adding preload note: " + note);
								deliveryVin.addPreloadNote(note);
							}
						}
					}
				}
			}
		}
	}

	public static void resetPhotoUploadProgress(Context context) {
	    Cursor cursor = transactionFactory(context).getPhotoUploadInProgress();
		log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_0: Checking for in-progress images");

		if (cursor != null) {
	        while (cursor.moveToNext()) {
	            Image image = cursorToImage(context, cursor);
				log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_0: Found in-progress image. Setting status for retry: " + image.image_id);
				if (image.retries < AppSetting.S3_UPLOAD_MAX_RETRIES.getInt()) {
					image.retries++;
					image.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
				}
				else {
					image.s3_upload_status = Constants.SYNC_STATUS_MAX_RETRIES_EXCEEDED;
					log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_0: Max retries for image exceeded for image: " + image.image_id);
				}
                insertImageToLocalDB(context, image);
            }
	        cursor.close();
        }
    }

	public static void resetPhotoRetries(Context context) {
		Cursor cursor = transactionFactory(context).getPhotoUploadMaxRetriesExceeded();
		log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_0: Checking for images where max retries was exceeded");

		if (cursor != null) {
			while (cursor.moveToNext()) {
				Image image = cursorToImage(context, cursor);
				log.debug(Logs.UPLOAD, "IMAGE_UPLOAD_0: Resetting retries for image to 0: " + image.image_id);
				image.retries = 0;
				image.s3_upload_status = Constants.SYNC_STATUS_NOT_UPLOADED;
				insertImageToLocalDB(context, image);
			}
			cursor.close();
		}
	}

    public static Image getNextPhotoToUpload(Context context, boolean hires) {
		Image photo = null;

		Cursor cursor = transactionFactory(context).getNextPhotoToUpload(hires);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				photo = cursorToImage(context, cursor);
			}
			cursor.close();
		}

		return photo;
	}

	public static ArrayList<Image> getPhotosToUpload(Context context) {
		ArrayList<Image> photos = new ArrayList<>();
		Cursor cursor = transactionFactory(context).getPhotosToUpload();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				photos.add(cursorToImage(context, cursor));
			}
			cursor.close();
		}
		Collections.sort(photos, Image.HiResLast);
		return photos;
	}

	public static void setImageUploadStatus(Context context, int image_id, int status){
		Log.d(TAG, "ben: updating upload_status: " + image_id + ":" + status);
		transactionFactory(context).updateImageStatusColumn(image_id, "uploadStatus", status);
		transactionFactory(context).updateImageStatusColumn(image_id, "uploaded", 1);
	}

	public static void setImageUploaded(Context context, int image_id) {
		transactionFactory(context).updateImageStatusColumn(image_id, "uploaded", 1);
	}

	public static void setImageNotUploaded(Context context, int image_id) {
		transactionFactory(context).updateImageStatusColumn(image_id, "uploaded", 0);
	}

	public static void setImagePreloadUploadStatus(Context context, int image_id, int status) {
		transactionFactory(context).updateImageStatusColumn(image_id, "preloadUploadStatus", status);
	}

	public static void setImageDeliveryUploadStatus(Context context, int image_id, int status) {
		transactionFactory(context).updateImageStatusColumn(image_id, "deliveryUploadStatus", status);
	}

	public static void setImageS3UploadStatus(Context context, int image_id, int status){
		Log.d(TAG, "PHOTO_TRACE: updating s3 status: " + image_id + ":" + status);
		transactionFactory(context).updateImageStatusColumn(image_id, "s3_upload_status", status);
	}

	public static void setImagePreauthUrl(Context context, int image_id, String url) {
		transactionFactory(context).updatePreauthUrlColumn(image_id, url);
	}

	public static void setImageLoadId(Context context, int image_id, int load_id) {
		transactionFactory(context).updateImageParentIdColumn(image_id, "load_id", load_id);
	}

	public static void setImageDeliveryId(Context context, int image_id, int delivery_id) {
		transactionFactory(context).updateImageParentIdColumn(image_id, "delivery_id", delivery_id);
	}

	public static void setImageDeliveryVinId(Context context, int image_id, int delivery_vin_id) {
		transactionFactory(context).updateImageParentIdColumn(image_id, "delivery_vin_id", delivery_vin_id);
	}

	public static void setImageForeignKey(Context context, int image_id, int foreignkey) {
		transactionFactory(context).updateImageParentIdColumn(image_id, "foreignkey", foreignkey);
	}

	public static void deleteLoadAndChildren(Context context, int loadId, String reason) {
		Load load = getLoad(context, loadId);
		deleteLoadAndChildren(context, load, reason);
	}

	/**
	 * Delete a load and all related objects.  Note, this does NOT delete dealers or VINs since those can come up later
	 * @param context context to pass to the database engine
	 * @param load the load to delete containing all objects to remove
	 */
	public static void deleteLoadAndChildren(Context context, Load load, String reason) {
		SyncManager.archiveLoad(context, load);
		if (HelperFuncs.isNullOrEmpty(reason)) {
			reason = "unspecified";
		}
		logDeleteMessage("Deleting load " + load.loadNumber, reason);
		CommonUtility.highLevelLog("Deleting load $loadNumber (reason: " + reason + ")", load);

		reason = "deleting associated load, " + load.loadNumber;

		//clean up child loads
		if (load.parentLoad) {
			logDeleteMessage(load.loadNumber + " is a parent load. Deleting child loads.", reason);
			List<Load> childLoads = getChildLoadsOfLoad(context, load);

			for (Load childLoad : childLoads) {
				CommonUtility.highLevelLog("Deleting child load " + HelperFuncs.noNull(childLoad.loadNumber, "?") + " of load $loadNumber", load);
				deleteLoadAndChildren(context, childLoad, "child of load " + load.loadNumber);
			}
		}

		for (Image image : load.images) {
            //Delete the image
            deleteImage(context, image, reason);
        }

		for(Delivery delivery : load.deliveries) {
			for (Image image : delivery.images) {
				//Delete the image
				deleteImage(context, image, reason);
			}

	    	for(DeliveryVin deliveryVin : delivery.deliveryVins) {
				for(Damage damage : deliveryVin.damages) {
					//Delete the damage
					deleteDamage(context, damage, reason);
				}

				for(Image image : deliveryVin.images) {
					//Delete the image
					deleteImage(context, image, reason);
				}

	            if (getDeliveryVinCountForVin(context, deliveryVin.vin) == 1) {
	      	        //vin is not used anywhere else, ok to delete
			        deleteVinDataFromDB(context, deliveryVin.vin.vin_id, reason);
		        }
	            //Delete the deliveryvin
	            deleteDeliveryVinDataFromDB(context, deliveryVin.delivery_vin_id, reason);
	        }

            //delete flag for lookup screen
            SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
            editor.remove(delivery.delivery_remote_id + "_lookup_shown");
            editor.apply();

	        //Delete the delivery
	        deleteDeliveryDataFromDB(context, delivery.delivery_id, reason);
	    }

	    //Delete the load
	    deleteLoadDataFromDB(context, load.load_id, reason);
		if (load.parentLoad) {
			CommonUtility.highLevelLog("Deleted load $loadNumber and its deliveries, vins, images, and child loads", load);
		}
		else {
			CommonUtility.highLevelLog("Deleted load $loadNumber and its deliveries, vins, and images", load);
		}
	}

	public static List<Load> getChildLoadsOfLoad(Context context, Load load) {
		Cursor cursor = transactionFactory(context).getLoadsFromDBForParent(Integer.parseInt(load.load_remote_id));

		List <Load> childLoads = getLoads(context, cursor, true, false);
		// Note: getLoads() closes cursor
		return childLoads;
	}

	public static List<Load> getAllChildLoadsOfLoadNoImages(Context context, Load load) {
		Cursor cursor = transactionFactory(context).getLoadsFromDBForParent(Integer.parseInt(load.load_remote_id));

		List <Load> childLoads = getLoads(context, cursor, false, false, false);
		// Note: getLoads() closes cursor
		return childLoads;
	}

	public static Load getParentLoad(Context context, Load load) {
		if (!load.isChildLoad()) {
			return null;
		}
		return DataManager.getLoadForRemoteId(context, String.valueOf(load.parent_load_id));
	}

	private static void deleteLoadDataFromDB(Context context, int load_id, String reason) {
		logDeleteMessage( "deleting load data for load " + load_id, reason);
    	transactionFactory(context).deleteLoad(load_id);
  	}

	public static void pushLocalDataToRemoteServer(Context context, int driver_id, boolean force) {
        log.debug(Logs.DATAMANAGER, "Pushing local data to server");
        //Log.d("NARF", "pushing local data");
		SyncManager.pushLocalDataToRemoteServer(context, driver_id, force);
	}

	public static List<Load> getAllLoads(Context context, int driver_id) {
		return getAllLoads(context, driver_id, true, -1);
	}
//
	public static List<Load> getAllLoads(Context context, int driver_id, boolean includeImages, int limit) {
		return getAllLoads(context, driver_id, includeImages, limit, false);
	}

	public static List<Load> getAllLoads(Context context, int driver_id, boolean includeImages, int limit, boolean excludeNormalLoadStates) {
		return getAllLoads(context, driver_id, true, limit, excludeNormalLoadStates, true);
	}

	public static List<Load> getAllLoads(Context context, int driver_id, boolean includeImages, int limit, boolean excludeNormalLoadStates, boolean showPreload) {

		Log.d("TIMER", "START full retrieval");
		//Log.d("Narf", "exclude: " + excludeNormalLoadStates);

		Cursor cursor = transactionFactory(context).getLoadFromDB(-1, driver_id, null, limit);

		List<Load> allLoads = getLoads(context, cursor, includeImages, excludeNormalLoadStates, showPreload);
		// Note: getLoads() closes cursor
		Log.d("TIMER", "END full retrieval");

		return allLoads;
	}

	public static List<Load> getLoadsMarkedDeletable(Context context) {

		Log.d("TIMER", "START full retrieval");
		//Log.d("Narf", "exclude: " + excludeNormalLoadStates);

		Cursor cursor = transactionFactory(context).getLoadsMarkedDeletableFromDB();

		List<Load> allLoads = getLoads(context, cursor, true, false, false);
		// Note: getLoads() closes cursor
		Log.d("TIMER", "END full retrieval");

		return allLoads;
	}

	public static List<String> getAllLoadIds(Context context, int driver_id) {
		Cursor cursor = transactionFactory(context).getLoadIdsFromDb(context, driver_id);

		List<String> ids = new ArrayList<>(cursor.getCount());

		if (cursor != null) {
			while (cursor.moveToNext()) {
				ids.add(cursor.getString(0));
			}

			cursor.close();
		}

	    return ids;
    }

	public static List<Integer> getAllEmptyChildLoadIds(Context context, int driver_id) {
		Cursor cursor = transactionFactory(context).getEmptyChildLoads(driver_id);

		List<Integer> emptyLoadIds = new ArrayList<>();
		if (cursor != null) {
			while (cursor.moveToNext()) {
				emptyLoadIds.add(cursor.getInt(cursor.getColumnIndex("load_id")));
			}
			cursor.close();
		}

		return emptyLoadIds;
	}

	public static HashMap<Integer, String> getCurrentChildLoadIds(Context context, int driver_id) {
		Cursor cursor = transactionFactory(context).getChildLoadIds(driver_id);

		HashMap<Integer, String>  childParentHashmap = new HashMap<>();
		if (cursor != null) {
			while (cursor.moveToNext()) {
				childParentHashmap.put(cursor.getInt(cursor.getColumnIndex("load_id")),
								 cursor.getString(cursor.getColumnIndex("parent_load_id")));
			}
			cursor.close();
		}

		return childParentHashmap;
	}

	public static List<Load> getAllLoadsLazy(Context context, int driver_id) {
		Cursor cursor = transactionFactory(context).getLoadFromDB(-1, driver_id, null);
		// Note: getLoadsLazy() closes cursor
		return getLoadsLazy(context, cursor);
	}

	public static List<Load> getStandardLoads(Context context, int driver_id) {
		Cursor cursor = transactionFactory(context).getLoadFromDB(-1, driver_id, false);
		// Note: getLoads() closes cursor
		return getLoads(context, cursor);
	}

	public static List<Load> getShuttleLoads(Context context, int driver_id) {
		Cursor cursor = transactionFactory(context).getLoadFromDB(-1, driver_id, true);
		// Note: getLoads() closes cursor
		return getLoads(context, cursor);
	}

	public static int getLoadIdForDeliveryVin(Context context, DeliveryVin deliveryVin) {
		return transactionFactory(context).getLoadIdForDeliveryVinId(deliveryVin.delivery_vin_id);
	}

    /**
     *
     * @param context
     * @param oldNumber
     * @param newNumber
     * @return Number of rows updated (!= 1 is a failure of some kind)
     */
	public static int updateShuttleLoadNumber(Context context, String oldNumber, String newNumber) {
		return transactionFactory(context).updateShuttleLoadNumber(oldNumber, newNumber);
	}

	public static List<Load> getStandardLoadsLazy(Context context, int driver_id, int limit) {
		return getLoadsLazy(context, driver_id, false, limit);
	}

	public static List<Load> getLoadsLazy(Context context, int driver_id, Boolean shuttleLoad, int limit) {
		Cursor cursor = transactionFactory(context).getLoadFromDB(-1, driver_id, shuttleLoad, limit);
		// Note: getLoadsLazy() closes cursor
		return getLoadsLazy(context, cursor);
	}

	public static List<Load> getLoadsLazy(Context context, Cursor cursor) {
		List<Load> loads = new ArrayList<Load>();

		if (DEBUG) log.debug(Logs.DATAMANAGER, "Get Loads Cursor Length :: " + cursor.getCount());
		if (cursor != null) {
			while (cursor.moveToNext()) {
				Load load = null;
				load = cursorToLoad(cursor);

				load.driver = getUser(context, String.valueOf(load.driver_id));

				if (load.shuttleLoad) {
					load.shuttleMove = getShuttleMoveForLoad(context, load.shuttleMove.shuttleMoveId);
				}

				load.deliveries = getDeliveryListLazy(context, load.load_id);
				loads.add(load);
			}
			cursor.close();
		}

		return loads;

	}

	public static List<Load> getLoads(Context context, Cursor cursor) {
		return getLoads(context, cursor, true);
	}

	public static List<Load> getLoads(Context context, Cursor cursor, boolean includeImages) {
		return getLoads(context, cursor, includeImages, true);
	}

	public static List<Load> getLoads(Context context, Cursor cursor, boolean includeImages, boolean excludeNormalLoadStates) {
		return getLoads(context, cursor, includeImages, excludeNormalLoadStates, true);
	}

	public static List<Load> getLoads(Context context, Cursor cursor, boolean includeImages, boolean excludeNormalLoadStates, boolean showPreload) {
		List<Load> loads = new ArrayList<Load>();
		//Log.d("Narf", "getloads exclude: " + excludeNormalLoadStates);

        if (DEBUG) log.debug(Logs.DATAMANAGER, "Get Loads Cursor Length :: " + cursor.getCount());
        if (cursor != null) {
            while (cursor.moveToNext()) {
				Load load = null;
				load = cursorToLoad(cursor);

				load.driver = getUser(context, String.valueOf(load.driver_id));

				if (load.shuttleLoad) {
					load.shuttleMove = getShuttleMoveForLoad(context, load.shuttleMove.shuttleMoveId);
				}

				load.deliveries = getDeliveryList(context, load.load_id, includeImages);
                if (!HelperFuncs.isNullOrEmpty(load.load_remote_id) && includeImages) {
                    load.images = getLoadImageList(context, load.load_remote_id, true);
                }
                //log.debug(Logs.DATAMANAGER, "Found " + load.images.size() + " supplemental preload images.");

				boolean loadHasUploadIssues = load.uploadIssues(showPreload);

				if(loadHasUploadIssues) {
					loads.add(load);
				} else if(!excludeNormalLoadStates) {
					loads.add(load);
				}
			}
			cursor.close();
		}

		return loads;

	}

	public static Load getLastCompletedLoadForDriver(Context context, int driver_id) {
		Load lastCompletedLoad = null;

		List<Load> completedLoads = getCompletedLoadsForDriver(context, driver_id);

		if(completedLoads != null && completedLoads.size() > 0) {
			Collections.sort(completedLoads, new Comparator<Load>() {
				@Override
				public int compare(Load lhs, Load rhs) {
					Long thisDate = lhs.getLatestSignatureDate();
					Long thatDate = rhs.getLatestSignatureDate();
					return thisDate.compareTo(thatDate);
				}
			});
			lastCompletedLoad = completedLoads.get(completedLoads.size() - 1);
		}

		return lastCompletedLoad;
	}

	public static List<Load> getCompletedLoadsForDriver(Context context, int driver_id) {
		List<Load> completedLoads = new ArrayList<>();

		List<Load> allLoads = getAllLoadsLazy(context, driver_id);

		for(Load nextLoad : allLoads) {
			if(nextLoad.isComplete()) {
				completedLoads.add(nextLoad);
			}
		}

		return completedLoads;
	}

	public static List<Delivery> getAllDeliveriesLazy(Context context, int driver_id, int limit) {
		List<Load> loads = getLoadsLazy(context, driver_id, null, limit);
		List<Delivery> deliveriesList = new ArrayList<Delivery>();

		for (Load load: loads) {
			for (Delivery delivery: load.deliveries) {
				delivery.load = load;
				deliveriesList.add(delivery);
			}
		}

		return deliveriesList;
	}

	//Get the current list of damage codes from the remote server
	public static void refreshLookupCodes(Activity activity, boolean getAll, ConsolidatedDataPullTask.IConsolidatedDataPullCallback callback) {
		new ConsolidatedDataPullTask(activity, getAll, callback).execute(); 
	}

	private static Transactions transactionFactory(Context context) {
		if(mTransactions == null) {
			mTransactions = new Transactions(context);
		}

		return mTransactions;
	}

	public static long insertUserToLocalDB(Context context, User user) {
		return transactionFactory(context).insertUserToLocalDB(user);
	}

	public static long insertLoadToLocalDB(Context context, Load load) {
		return insertLoadToLocalDB(context, load, false);
	}

	public static long insertLoadToLocalDB(Context context, Load load, Boolean fromRemoteServer) {
		log.debug(Logs.DATAMANAGER, "Saving load " + load.load_remote_id + ":" + load.loadNumber);

		long load_id = transactionFactory(context).insertLoadInfoToLocalDB(
				fromRemoteServer,
				load
		);

		for(Delivery delivery: load.deliveries) {
			//Set the load id from the insert so that we have referential integrity
			delivery.load_id = (int)load_id;
			insertDeliveryToLocalDB(context, delivery, fromRemoteServer);
		}

		for(Image image: load.images) {
			Image dbImage = getImageForFilename(context, image.filename);
			if (dbImage != null) {
				if (load.load_remote_id != null) {
					setImageLoadId(context, dbImage.image_id, Integer.parseInt(load.load_remote_id));
				}
			} else {
				image.load_id = Integer.parseInt(load.load_remote_id);
				insertImageToLocalDB(context, image);
			}
		}

		return load_id;
	}

	public static int getLoadIdForLoadNumber(Context context, String loadNumber) {
		return transactionFactory(context).getLoadIdForLoadNumber(loadNumber);
	}

	public static int updateLoadDriverId(Context context, int load_id, int driver_id) {
		return transactionFactory(context).updateLoadDriverId(load_id, driver_id);
	}

	public static int markLoadDeletable(Context context, int load_id) {
		return transactionFactory(context).markLoadDeletable(load_id);
	}

	public static int updateLoadStatus(Context context, int load_id, String status) {
		return transactionFactory(context).updateLoadStatus(load_id, status);
	}

	public static long insertDeliveryToLocalDB(Context context, Delivery delivery) {
    	return insertDeliveryToLocalDB(context, delivery, false);
	}

	public static long insertDeliveryToLocalDB(Context context, Delivery delivery, Boolean fromRemoteServer) {
		//Log bellow is sufficient?
		//log.debug(Logs.DATAMANAGER, "insertDeliveryToLocalDB");
	  
		log.debug(Logs.DATAMANAGER, "Saving delivery " + delivery.delivery_remote_id);
	  
	    long dealerId = -1;
	    if (delivery.dealer != null) {
		    dealerId = insertDealerToLocalDB(context, delivery.dealer);
		    
		    //Don't try to set the dealer id unless the dealer object wasn't null
		    delivery.dealer.dealer_id = (int) dealerId;
	    }
	    
		long delivery_id = transactionFactory(context).insertDeliveryToLocalDB(
				delivery,
				fromRemoteServer);

		for(DeliveryVin deliveryVin : delivery.deliveryVins) {
			//Set the id for referential integrity
			deliveryVin.delivery_id = (int)delivery_id;
			
			//make sure that the delivery is not overwritten if coming from the remote server by passing in the variable
			insertDeliveryVinToLocalDB(context, deliveryVin, fromRemoteServer);
		}

		for(Image image: delivery.images) {
			Image dbImage = getImageForFilename(context, image.filename);
			if (dbImage != null) {
				setImageDeliveryId(context, dbImage.image_id, Integer.parseInt(delivery.delivery_remote_id));
			} else {
				image.delivery_id = Integer.parseInt(delivery.delivery_remote_id);
				insertImageToLocalDB(context, image);
			}
		}

		return delivery_id;
	}

	public static long insertDeliveryVinToLocalDB(Context context, DeliveryVin deliveryVin) {
    	return insertDeliveryVinToLocalDB(context, deliveryVin, false);
  	}

  	public static long insertDeliveryVinToLocalDB(Context context, DeliveryVin deliveryVin, Boolean fromRemoteServer) {
		//Unecessary given log below?
        //log.debug(Logs.DATAMANAGER, "insertDeliveryVinToLocalDB");
	  
        log.debug(Logs.DATAMANAGER, "Saving delivery vin " + deliveryVin.delivery_vin_remote_id + ":" + deliveryVin.vin.vin_remote_id + ":" + deliveryVin.vin.vin_number);
	  
		long vin_id = insertVinToLocalDB(context, deliveryVin.vin);
		deliveryVin.vin_id = (int) vin_id;
		
		//make sure that the delivery is not overwritten if coming from the remote server by passing in the variable
		long delivery_vin_id = transactionFactory(context).insertDeliveryVinToLocalDB(fromRemoteServer, deliveryVin);
		//long delivery_vin_id = transactionFactory(context).insertDeliveryVinToLocalDB(deliveryVin);

		Map<Integer, Long> imageMapper = new HashMap<Integer, Long>();
		for(Damage damage: deliveryVin.damages) {
			//Set the id for referential integrity
			damage.delivery_vin_id = (int)delivery_vin_id;

			imageMapper.put(damage.damage_id, insertDamageToLocalDB(context, damage));
		}

		for(Image image : deliveryVin.images) {
			Image dbImage = getImageForFilename(context, image.filename);
			if (dbImage != null) {
				setImageDeliveryVinId(context, dbImage.image_id, (int) delivery_vin_id);

				if (image.foreignKeyLabel == Constants.IMAGE_DAMAGE && imageMapper.containsKey(image.foreignKey)) {
					setImageForeignKey(context, dbImage.image_id, imageMapper.get(image.foreignKey).intValue());
				}

			} else {
				image.delivery_vin_id = (int) delivery_vin_id;

				if (image.foreignKeyLabel == Constants.IMAGE_DAMAGE && imageMapper.containsKey(image.foreignKey)) {
					image.foreignKey = imageMapper.get(image.foreignKey).intValue();
				}

				if (image.filename == null || image.filename.length() == 0) {
					image.filename = deliveryVin.vin.vin_number + "-" + UUID.randomUUID().toString();
				}

				insertImageToLocalDB(context, image);
			}
		}

		return delivery_vin_id;
	}

	public static long insertDamageToLocalDB(Context context, Damage damage) {
		return transactionFactory(context).insertDamageToLocalDB(damage);
	}

	public static long insertDealerToLocalDB(Context context, Dealer dealer) {
		return transactionFactory(context).insertDealerToLocalDB(dealer);
	}

	public static long insertTypeCodeToLocalDB(Context context, TypeCode typeCode) {
		return transactionFactory(context).insertTypeCodeToLocalDB(typeCode);
	}

	public static long insertSvrtyCodeToLocalDB(Context context, SeverityCode severityCode) {
		return transactionFactory(context).insertSvrtyCodeToLocalDB(severityCode);
	}

	public static long insertSpecialCodeToLocalDB(Context context, SpecialCode specialCode) {
		return transactionFactory(context).insertSpecialCodeToLocalDB(specialCode);
	}

	public static long insertAreaCodeToLocalDB(Context context, AreaCode areaCode) {
		long area_code_id = transactionFactory(context).insertAreaCodeToLocalDB(areaCode);

		for(AreaCode childAreaCode : areaCode.childAreaCodes) {
			childAreaCode.parent_area_code_id = (int) area_code_id;

			//handle this recursively in case we want more than two levels in the future as well as
			//simplifying maintenance when method signatures change...
			insertAreaCodeToLocalDB(context, childAreaCode);
		}

		return area_code_id;
	}

	public static long insertTrainingTypeToLocalDB(Context context, TrainingType type) {
        return transactionFactory(context).insertTrainingTypeToLocalDB(type);
    }

    public static long insertTrainingRequirementToLocalDB(Context context, TrainingRequirement requirement) {
        return transactionFactory(context).insertTrainingRequirementToLocalDB(requirement);
    }

    public static long updateTrainingRequirement(Context context, TrainingRequirement requirement) {
	    return transactionFactory(context).updateTrainingRequirement(requirement);
    }

    public static long markTrainingRequirementUploaded(Context context, long id) {
	    return transactionFactory(context).markTrainingRequirementUploaded(id);
    }

    public static long deleteOrphanTrainingRequirements(Context context, List<String> currentLoadIds) {
        return transactionFactory(context).deleteOrphanTrainingRequirements(currentLoadIds);
    }

    public static List<TrainingType> getTrainingTypes(Context context) {
        Cursor cursor = transactionFactory(context).getTrainingTypeListFromLocalDB();
        List<TrainingType> types = new ArrayList<>();
        if(cursor != null) {
            while(cursor.moveToNext()) {
                TrainingType type = cursorToTrainingType(cursor);
                types.add(type);
            }

            cursor.close();
        }

        return types;
    }

    private static TrainingType cursorToTrainingType(Cursor cursor) {
	    TrainingType type = new TrainingType();

	    type.id = cursor.getInt(cursor.getColumnIndex("id"));
	    type.name = cursor.getString(cursor.getColumnIndex("name"));
	    type.defaultRequiredProgress = cursor.getInt(cursor.getColumnIndex("defaultRequiredProgress"));

	    return type;
    }

    public static TrainingRequirement getTrainingRequirement(Context context, long trainingRequirementId) {
        Cursor cursor = transactionFactory(context).getTrainingRequirementFromLocalDB(trainingRequirementId);
        TrainingRequirement req = null;
        if(cursor != null) {
            while(cursor.moveToNext()) {
                req = cursorToTrainingRequirement(cursor);
            }

            cursor.close();
        }

        return req;
    }

    public static List<TrainingRequirement> getTrainingRequirements(Context context, long[] trainingRequirementIds) {
        List<TrainingRequirement> reqs = new ArrayList<>();
        if(trainingRequirementIds == null || trainingRequirementIds.length == 0) return reqs;

	    Cursor cursor = transactionFactory(context).getTrainingRequirementsInListFromLocalDB(trainingRequirementIds);

        if(cursor != null) {
            while(cursor.moveToNext()) {
                TrainingRequirement req = cursorToTrainingRequirement(cursor);
                reqs.add(req);
            }

            cursor.close();
        }

        return reqs;
    }

    public static List<TrainingRequirement> getCompletedTrainingRequirements(Context context) {
	    List<TrainingRequirement> reqs = new ArrayList<>();

	    Cursor cursor = transactionFactory(context).getCompletedTrainingRequirementsFromLocalDB();

        if(cursor != null) {
            while(cursor.moveToNext()) {
                TrainingRequirement req = cursorToTrainingRequirement(cursor);
                reqs.add(req);
            }

            cursor.close();
        }

        return reqs;
    }

    public static List<TrainingRequirement> getTrainingRequirementsForLoad(Context context, String loadRemoteId) {
        Cursor cursor = transactionFactory(context).getTrainingRequirementsByLoadFromLocalDB(loadRemoteId);
        List<TrainingRequirement> reqs = new ArrayList<>();

        if(cursor != null) {
            while(cursor.moveToNext()) {
                TrainingRequirement req = cursorToTrainingRequirement(cursor);
                reqs.add(req);
            }

            cursor.close();
        }

        return reqs;
    }

    public static List<TrainingRequirement> getUnfinishedTrainingRequirementsForLoad(Context context, String loadRemoteId) {
        Cursor cursor = transactionFactory(context).getUnfinishedTrainingRequirementsByLoadFromLocalDB(loadRemoteId);
        List<TrainingRequirement> reqs = new ArrayList<>();

        if(cursor != null) {
            while(cursor.moveToNext()) {
                TrainingRequirement req = cursorToTrainingRequirement(cursor);
                reqs.add(req);
            }

            cursor.close();
        }

        return reqs;
    }

    public static List<TrainingRequirement> getTrainingRequirementForUser(Context context, long userId) {
        Cursor cursor = transactionFactory(context).getTrainingRequirementsByUserFromLocalDB(userId);
        List<TrainingRequirement> reqs = new ArrayList<>();

        if(cursor != null) {
            while(cursor.moveToNext()) {
                TrainingRequirement req = cursorToTrainingRequirement(cursor);
                reqs.add(req);
            }

            cursor.close();
        }

        return reqs;
    }

    public static ArrayList<TrendingAlert>getTrendingAlertsforLoad(Context context, Load load){
		if (load != null && load.load_remote_id != null) {
			return getTrendingAlertsForLoadRemoteId(context, Integer.valueOf(load.load_remote_id));
		} else {
			return new ArrayList<TrendingAlert>();
		}
	}

    public static ArrayList<TrendingAlert> getTrendingAlertsForLoadRemoteId(Context context, int load_remote_id) {
		Cursor cursor = transactionFactory(context).getTrendingAlertsForLoadRemoteId(load_remote_id);
		ArrayList<TrendingAlert> alerts = new ArrayList<>();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				TrendingAlert alert = cursorToTrendingAlert(cursor);
				alerts.add(alert);
			}
			cursor.close();
		}
		return alerts;
	}

	public static ArrayList<TrendingAlert> getTrendingAlertsForVinId(Context context, int load_remote_id, int vin_id) {
		Cursor cursor = transactionFactory(context).getTrendingAlertsForVinId(load_remote_id, vin_id);
		ArrayList<TrendingAlert> alerts = new ArrayList<>();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				TrendingAlert alert = cursorToTrendingAlert(cursor);
				alerts.add(alert);
			}
			cursor.close();
		}
		return alerts;
	}

	public static ArrayList<TrendingAlert> getTrendingAlertsForVin(Context context, Load load, VIN vin) {
		if (load.load_remote_id != null && vin.vin_remote_id != null) {
			return getTrendingAlertsForVinId(context, Integer.valueOf(load.load_remote_id), Integer.valueOf(vin.vin_remote_id));
		} else {
			return new ArrayList<TrendingAlert>();
		}
	}

    private static TrainingRequirement cursorToTrainingRequirement(Cursor cursor) {
	    TrainingRequirement req = new TrainingRequirement();

	    req.id = cursor.getInt(cursor.getColumnIndex("id"));
	    req.supervisor_id = cursor.getString(cursor.getColumnIndex("supervisor_id"));
	    req.user_id = cursor.getString(cursor.getColumnIndex("user_id"));
	    req.load_id = cursor.getString(cursor.getColumnIndex("load_id"));
	    req.type = cursor.getInt(cursor.getColumnIndex("type"));
	    req.progress = cursor.getInt(cursor.getColumnIndex("progress"));
	    req.requiredProgress = cursor.getInt(cursor.getColumnIndex("requiredProgress"));
	    req.setAssignedFromTimestamp(cursor.getString(cursor.getColumnIndex("assigned")));
	    req.setStartedFromTimestamp(cursor.getString(cursor.getColumnIndex("started")));
        req.setCompletedFromTimestamp(cursor.getString(cursor.getColumnIndex("completed")));
        req.vin = cursor.getString(cursor.getColumnIndex("vin"));
        req.setSupplementalReference(cursor.getString(cursor.getColumnIndex("supplementalReference")));
        req.supplementalData = cursor.getString(cursor.getColumnIndex("supplementalData"));
        req.startedLatitude = cursor.getDouble(cursor.getColumnIndex("startedLatitude"));
        req.startedLongitude = cursor.getDouble(cursor.getColumnIndex("startedLongitude"));
        req.completedLatitude = cursor.getDouble(cursor.getColumnIndex("completedLatitude"));
        req.completedLongitude = cursor.getDouble(cursor.getColumnIndex("completedLongitude"));

        req.uploaded = cursor.getInt(cursor.getColumnIndex("uploaded"));
        req.adHoc = cursor.getInt(cursor.getColumnIndex("adHoc"));

	    return req;
    }

	public static long insertTrendingAlertToLocalDB(Context context, TrendingAlert trendingAlert) {
		return transactionFactory(context).insertTrendingAlertToLocalDB(trendingAlert);
	}

    public static TrendingAlert cursorToTrendingAlert(Cursor cursor) {
		TrendingAlert alert = new TrendingAlert();
		alert.id = cursor.getInt(cursor.getColumnIndex("id"));
		alert.load_id = cursor.getInt(cursor.getColumnIndex("load_id"));
		alert.driver_id = cursor.getInt(cursor.getColumnIndex("driver_id"));
		alert.ldnbr = cursor.getString(cursor.getColumnIndex("loadNumber"));
		alert.vin_id = cursor.getString(cursor.getColumnIndex("vin_id"));
		alert.alert = cursor.getString(cursor.getColumnIndex("alert"));
		alert.count = cursor.getInt(cursor.getColumnIndex("count"));
		alert.order = cursor.getInt(cursor.getColumnIndex("ordr"));
		alert.type = cursor.getString(cursor.getColumnIndex("type"));

		return alert;
	}

	public static long insertVinToLocalDB(Context context, VIN vin) {
        log.debug(Logs.DATAMANAGER, "insertbVinb" + vin + "ToLocalDB");
		return transactionFactory(context).insertVinToLocalDB(
				vin);
	}

	private static VIN cursorToVIN(Cursor cursor) {
		VIN tmpVIN = new VIN();

		tmpVIN.vin_id = cursor.getInt(cursor.getColumnIndex("vin_id"));
		tmpVIN.vin_remote_id = cursor.getString(cursor.getColumnIndex("vin_remote_id"));
		tmpVIN.vin_number = cursor.getString(cursor.getColumnIndex("vin_number"));
		tmpVIN.status = cursor.getString(cursor.getColumnIndex("status"));
		tmpVIN.body = cursor.getString(cursor.getColumnIndex("body"));
		tmpVIN.weight = cursor.getString(cursor.getColumnIndex("weight"));
		tmpVIN.colordes = cursor.getString(cursor.getColumnIndex("colordes"));
		tmpVIN.type = cursor.getString(cursor.getColumnIndex("type"));
		tmpVIN.created = cursor.getString(cursor.getColumnIndex("created"));
		tmpVIN.driver_comment = cursor.getString(cursor.getColumnIndex("driver_comment"));
		tmpVIN.load_id = cursor.getInt(cursor.getColumnIndex("load_id"));
		tmpVIN.is_damage = cursor.getString(cursor.getColumnIndex("is_damage"));
		tmpVIN.fillers = cursor.getString(cursor.getColumnIndex("fillers"));
		tmpVIN.ats = cursor.getString(cursor.getColumnIndex("ats"));
		tmpVIN.notes_image = cursor.getString(cursor.getColumnIndex("notes_image"));
		tmpVIN.modified = cursor.getString(cursor.getColumnIndex("modified"));
		tmpVIN.color = cursor.getString(cursor.getColumnIndex("color"));
		tmpVIN.dealer_id = cursor.getInt(cursor.getColumnIndex("dealer_id"));
		tmpVIN.callback = cursor.getString(cursor.getColumnIndex("callback"));
		tmpVIN.notes = cursor.getString(cursor.getColumnIndex("notes"));
		tmpVIN.dealer_comment = cursor.getString(cursor.getColumnIndex("dealer_comment"));
		tmpVIN.customer_name = cursor.getString(cursor.getColumnIndex("customer_name"));
		return tmpVIN;
	}


	public static Load getLoad(Context context, int load_id) {
		Cursor cursor = transactionFactory(context).getLoadFromDB(load_id, -1, null);
		Load load = null;
		//System.out.println(DataManager.class.getName()+" Cursor Length :: "+cursor.getCount());
        if (cursor != null) {
            while (cursor.moveToNext()) {
				load = cursorToLoad(cursor);
				load.driver = getUser(context, String.valueOf(load.driver_id));
				break;
			}
			
			if(load != null) {
				if (load.shuttleLoad) {
					load.shuttleMove = getShuttleMoveForLoad(context, load.shuttleMove.shuttleMoveId);
				}
				load.deliveries = getDeliveryList(context, load.load_id);
				load.images = getLoadImageList(context, load.load_remote_id, true);
			}
			cursor.close();			
		}

		return load;
	}


	public static Load getLoadForRemoteId(Context context, String load_remote_id) {
		Cursor cursor = transactionFactory(context).getLoadFromDBForRemote(load_remote_id);
		Load load = null;
		//System.out.println(DataManager.class.getName()+" Cursor Length :: "+cursor.getCount());
        if (cursor != null) {
            while (cursor.moveToNext()) {
				load = cursorToLoad(cursor);
				load.driver = getUser(context, String.valueOf(load.driver_id));
				break;
			}

			if(load != null) {
				if (load.shuttleLoad) {
					load.shuttleMove = getShuttleMoveForLoad(context, load.shuttleMove.shuttleMoveId);
				}
				load.deliveries = getDeliveryList(context, load.load_id);
                load.images = getLoadImageList(context, load.load_remote_id, true);
            }
			cursor.close();
		}

		return load;
	}


	public static Load getLoadForLoadNumber(Context context, String loadNumber) {
		Cursor cursor = transactionFactory(context).getLoadFromDBForLoadNumber(loadNumber, true);
		Load load = null;
		//System.out.println(DataManager.class.getName()+" Cursor Length :: "+cursor.getCount());
		if (cursor != null) {
			while (cursor.moveToNext()) {
				load = cursorToLoad(cursor);
				load.driver = getUser(context, String.valueOf(load.driver_id));
				break;
			}

			if(load != null) {
				if (load.shuttleLoad) {
					load.shuttleMove = getShuttleMoveForLoad(context, load.shuttleMove.shuttleMoveId);
				}
				load.deliveries = getDeliveryList(context, load.load_id);
				load.images = getLoadImageList(context, load.load_remote_id, true);
			}
			cursor.close();
		}

		return load;
	}

	public static void copyChildLoadDataToParent(Context context, Load parentLoad) {
		if (parentLoad.parentLoad) {
			List<Load> childLoads = getAllChildLoadsOfLoadNoImages(context, parentLoad);
			for (Delivery delivery: parentLoad.deliveries) {
				if (delivery.deliveryVins == null) {
					continue;
				}
				for (DeliveryVin dvin: delivery.deliveryVins) {
					for (Load childLoad : childLoads) {
						DeliveryVin childDvin = childLoad.getDeliveryVinForVinNumber(dvin.vin.vin_number);
						if (childDvin != null) {
							dvin.position = childDvin.position;
							dvin.backdrv = childDvin.backdrv;
							dvin.inspectedPreload = childDvin.inspectedPreload;
						}
					}
				}
			}
		}
	}

	public static boolean allChildLoadsSigned(Context context, int parent_load_id) {
		boolean result = false;
		Cursor cursor = transactionFactory(context).getLoadsFromDBForParent(parent_load_id);
		Load load;
		if (cursor != null) {
			result = true;
			while (cursor.moveToNext()) {
				load = cursorToLoad(cursor);
				if (load.driverPreLoadSignature == null || load.driverPreLoadSignature.isEmpty()) {
					result = false;
					break;
				}
			}
			cursor.close();
		}

		return result;
	}

	public static List<String> getCurrentDVPositions(Context context, int parent_load_id) {
		Cursor cursor = transactionFactory(context).getLoadChildPositions(parent_load_id);

		List<String> dvPositions = new ArrayList<>();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				String position = cursor.getString(cursor.getColumnIndex("position"));
				if(position != null) {
					dvPositions.add(position);
				}
			}
			cursor.close();
		}

		return dvPositions;
	}


	private static Load cursorToLoad(Cursor cursor) {
		Load load = new Load();
		load.load_id = cursor.getInt(cursor.getColumnIndex("load_id"));
		load.load_remote_id = cursor.getString(cursor.getColumnIndex("load_remote_id"));
		load.loadNumber = cursor.getString(cursor.getColumnIndex("loadNumber"));
		load.truckNumber = cursor.getString(cursor.getColumnIndex("truck_number"));
		load.trailerNumber = cursor.getString(cursor.getColumnIndex("trailerNumber"));
		load.driver_id = cursor.getInt(cursor.getColumnIndex("driver_id"));
		load.driverPreLoadSignature = cursor.getString(cursor.getColumnIndex("driverPreLoadSignature"));
		load.driverPreLoadSignatureLat = cursor.getString(cursor.getColumnIndex("driverPreLoadSignatureLat"));
		load.driverPreLoadSignatureLon = cursor.getString(cursor.getColumnIndex("driverPreLoadSignatureLon"));
		load.driverPreLoadComment = cursor.getString(cursor.getColumnIndex("driverPreLoadComment"));
		load.driverPreLoadContact = cursor.getString(cursor.getColumnIndex("driverPreLoadContact"));
		load.driverPreLoadSignatureSignedAt = cursor.getString(cursor.getColumnIndex("driverPreLoadSignatureSignedAt"));
		load.status = cursor.getString(cursor.getColumnIndex("status"));
		load.uploaded = cursor.getInt(cursor.getColumnIndex("uploaded")) == 1 ? true : false;
		load.shuttleLoad = cursor.getInt(cursor.getColumnIndex("shuttleLoad")) == 1 ? true : false;

		load.originLoad = cursor.getInt(cursor.getColumnIndex("originLoad")) == 1 ? true : false;
		load.relayLoad = cursor.getInt(cursor.getColumnIndex("relayLoad")) == 1 ? true : false;

		load.originLoadNumber = cursor.getString(cursor.getColumnIndex("originLoadNumber"));
        load.relayLoadNumber = cursor.getString(cursor.getColumnIndex("relayLoadNumber"));
        load.relayLoadDealerName = cursor.getString(cursor.getColumnIndex("relayLoadDealerName"));
        load.loadType = cursor.getString(cursor.getColumnIndex("ldtyp"));

		if(load.loadType == null) {
			load.loadType = "";
		}

	    load.originTerminal = cursor.getString(cursor.getColumnIndex("originTerminal"));
		load.helpTerminal = cursor.getString(cursor.getColumnIndex("helpTerminal"));
		Integer shuttleMoveId = cursor.getInt(cursor.getColumnIndex("shuttleMoveId"));

		if (shuttleMoveId != null) {
			load.shuttleMove = new ShuttleMove();
			load.shuttleMove.shuttleMoveId = shuttleMoveId;
		}

		load.notes = cursor.getString(cursor.getColumnIndex("notes"));
		
		load.preloadUploadStatus = cursor.getInt(cursor.getColumnIndex("preloadUploadStatus"));
		Integer deliveryUploadStatus = cursor.getInt(cursor.getColumnIndex("deliveryUploadStatus"));
		if (deliveryUploadStatus != 0) {
			//load has a default value for deliveryUploadStatus
			load.deliveryUploadStatus = cursor.getInt(cursor.getColumnIndex("deliveryUploadStatus"));
		}
		try {
			load.lastUpdated = Constants.dateFormatter().parse(cursor.getString(cursor.getColumnIndex("lastUpdated")));
		} catch (Exception e) {
			load.lastUpdated = new Date();
		}
		load.supervisorSignature = cursor.getString(cursor.getColumnIndex("supervisorSignature"));
		try {
			load.supervisorSignedAt = cursor.getString(cursor.getColumnIndex("supervisorSignedAt"));
		} catch (Exception e) {
			load.supervisorSignedAt = null;
		}
		try {
			load.supervisorSignatureLat = cursor.getString(cursor.getColumnIndex("supervisorSignatureLat"));
		} catch (Exception e) {
			load.supervisorSignatureLat = null;
		}
		try {
			load.supervisorSignatureLon = cursor.getString(cursor.getColumnIndex("supervisorSignatureLon"));
		} catch (Exception e) {
			load.supervisorSignatureLon = null;
		}
		load.driverHighClaimsAudit = cursor.getString(cursor.getColumnIndex("driverHighClaimsAudit"));
		load.lotCodeMsgSeen = cursor.getInt(cursor.getColumnIndex("lotCodeMsgSeen")) == 1;
		load.nextDispatch = cursor.getString(cursor.getColumnIndex("nextDispatch"));

		load.firstDrop = cursor.getString(cursor.getColumnIndex("firstDrop"));
		load.lastDrop = cursor.getString(cursor.getColumnIndex("lastDrop"));

		load.parent_load_id = cursor.getInt(cursor.getColumnIndex("parent_load_id"));
		load.parentLoad = cursor.getInt(cursor.getColumnIndex("parentLoad")) == 1;

		load.preloadSupervisorSignature = cursor.getString(cursor.getColumnIndex("preloadSupervisorSignature"));
		load.preloadSupervisorSignedAt = cursor.getString(cursor.getColumnIndex("preloadSupervisorSignedAt"));

		load.pickSheetImageRequired = intToBool(cursor.getInt(cursor.getColumnIndex("pickSheetImageRequired")));
		load.extraDocImageRequired = cursor.getString(cursor.getColumnIndex("extraDocImageRequired"));

		return load;
	}

	private static boolean intToBool(int val) {
		return val != 0;
	}

	public static List<TypeCode> getTypeCodeList(Context context) {
		ArrayList<TypeCode> typeCodes = new ArrayList<TypeCode>();

		Cursor cursor = transactionFactory(context).getTypeCodeListFromLocalDB();
        if (cursor != null) {
            while (cursor.moveToNext()) {
				TypeCode tmpTypeCode = cursorToTypeCode(cursor);
				typeCodes.add(tmpTypeCode);
			}
			
			cursor.close();
		}
		
		return typeCodes;
	}

	public static List<SpecialCode> getSpecialCodeList(Context context) {
		ArrayList<SpecialCode> specialCodes = new ArrayList<SpecialCode>();

		Cursor cursor = transactionFactory(context).getSpecialCodeListFromLocalDB();
        if (cursor != null) {
            while (cursor.moveToNext()) {
				SpecialCode tmpSpecialCode = cursorToSpecialCode(cursor);
				specialCodes.add(tmpSpecialCode);
			}
			
			cursor.close();
		}
		
		return specialCodes;
	}

	private static boolean getActiveStatus(Cursor cursor) {
		if(cursor.getColumnIndex("active") < 0) {
			return true;
		}
		return (cursor.getInt(cursor.getColumnIndex("active")) == 1);
	}

	private static SpecialCode cursorToSpecialCode(Cursor cursor) {
		SpecialCode specialCode = new SpecialCode();

		specialCode.setDescription(cursor.getString(cursor.getColumnIndex("description")));
		specialCode.special_code_id = cursor.getInt(cursor.getColumnIndex("special_code_id"));
		specialCode.special_code_remote_id = cursor.getString(cursor.getColumnIndex("special_code_remote_id"));

		if(cursor.getString(cursor.getColumnIndex("area_code")) != null)
			specialCode.setAreaCode(cursor.getString(cursor.getColumnIndex("area_code")));

		if(cursor.getString(cursor.getColumnIndex("type_code")) != null)
		specialCode.setTypeCode(cursor.getString(cursor.getColumnIndex("type_code")));


		if(cursor.getString(cursor.getColumnIndex("severity_code")) != null)
		specialCode.setSeverityCode(cursor.getString(cursor.getColumnIndex("severity_code")));

		if(cursor.getColumnIndex("active") >= 0) {
			specialCode.active = getActiveStatus(cursor);
		}
		return specialCode;
	}

	public static List<AreaCode> getAreaCodeList(Context context) {
		ArrayList<AreaCode> areaCodes = new ArrayList<AreaCode>();
		ArrayList<AreaCode> childAreaCodes = new ArrayList<AreaCode>();
		Cursor cursor = transactionFactory(context).getAreaCodeListFromLocalDB();
        if (cursor != null) {
            while (cursor.moveToNext()) {

				AreaCode tmpAreaCode = cursorToAreaCode(cursor);

				if(tmpAreaCode.parent_area_code_id == -1) {

					areaCodes.add(tmpAreaCode);
				} else {

					//put children into a temp array since we don't know if they exist yet...
					childAreaCodes.add(tmpAreaCode);
				}
			}

			//loop through the children that we saved and put them in the appropriate parent
			AreaCode parentAreaCode;
	
			for(AreaCode childAreaCode : childAreaCodes) {
				for(int index = 0; index < areaCodes.size(); index++) {
					parentAreaCode = areaCodes.get(index);
					if(parentAreaCode.area_code_id == childAreaCode.parent_area_code_id) {
						parentAreaCode.childAreaCodes.add(childAreaCode);
						areaCodes.set(index, parentAreaCode);
					}
				}
			}
			
			cursor.close();
		}
		return areaCodes;
	}

	public static List<AreaCode> getHighClaimsAuditQuestionList(Context context) {
		ArrayList<AreaCode> areaCodes = new ArrayList<AreaCode>();
		ArrayList<AreaCode> childAreaCodes = new ArrayList<AreaCode>();
		Cursor cursor = transactionFactory(context).getQuestionnaireFromLocalDB();
		if (cursor != null) {
			while (cursor.moveToNext()) {

				AreaCode tmpAreaCode = cursorToAreaCode(cursor);

				if(tmpAreaCode.parent_area_code_id == -1) {

					areaCodes.add(tmpAreaCode);
				} else {

					//put children into a temp array since we don't know if they exist yet...
					childAreaCodes.add(tmpAreaCode);
				}
			}

			//loop through the children that we saved and put them in the appropriate parent
			AreaCode parentAreaCode;

			for(AreaCode childAreaCode : childAreaCodes) {
				for(int index = 0; index < areaCodes.size(); index++) {
					parentAreaCode = areaCodes.get(index);
					if(parentAreaCode.area_code_id == childAreaCode.parent_area_code_id) {
						parentAreaCode.childAreaCodes.add(childAreaCode);
						areaCodes.set(index, parentAreaCode);
					}
				}
			}

			cursor.close();
		}
		return areaCodes;
	}


	public static List<SeverityCode> getSeverityCodeList(Context context) {
		ArrayList<SeverityCode> severityCodes = new ArrayList<SeverityCode>();

		Cursor cursor = transactionFactory(context).getSvrtyCodeListFromLocalDB();
        if (cursor != null) {
            while (cursor.moveToNext()) {
				SeverityCode tmpSeverityCode = cursorToSeverityCode(cursor);
				severityCodes.add(tmpSeverityCode);
			}

			cursor.close();
		}


        log.debug(Logs.DATAMANAGER, "retrieved " + severityCodes.size() + " severity codes");
		return severityCodes;
	}


	public static VIN getVINForVinNumber(Context context, String vinNumber) {
		Cursor cursor = transactionFactory(context).getVinForDamageByVinNumber(vinNumber);
		VIN vin = null;

        if (cursor != null) {
            while (cursor.moveToNext()) {
				vin = cursorToVIN(cursor);
				break;
			}
			
			cursor.close();
		}
		
		return vin;
	}

	public static User  getUserForSupervisorCode(Context context, String supervisorCardCode) {
		Cursor cursor = transactionFactory(context).getUserFromDBForSupervisorCardCode(supervisorCardCode);
		User user = null;

		if (cursor != null) {
			while (cursor.moveToNext()) {
				user = cursorToUser(cursor);
				break;
			}

			cursor.close();
		}

		return user;
	}

	public static int getDeliveryVinCountForVin(Context context, VIN vin) {
		return transactionFactory(context).getDeliveryVinCountForVin(vin.vin_id);
	}


	public static User getUserForDriverNumber(Context context, String driverNumber) {
		Cursor cursor = transactionFactory(context).getUserFromDBForDriverNumber(driverNumber);
		User user = null;

        if (cursor != null) {
            while (cursor.moveToNext()) {
				user = cursorToUser(cursor);
				break;
			}
			
			cursor.close();
		}
		
		return user;
	}

	public static User getUser(Context context, String driverId) {
		Cursor cursor = transactionFactory(context).getUserFromDB(driverId);
		User user = null;

		if (cursor != null) {
			while (cursor.moveToNext()) {
				user = cursorToUser(cursor);
				break;
			}

			cursor.close();
		}

		return user;
	}

	private static User cursorToUser(Cursor cursor) {
		User user = new User();
		user.user_id = cursor.getInt(cursor.getColumnIndex("user_id"));
		user.user_remote_id = cursor.getString(cursor.getColumnIndex("user_remote_id"));
		user.firstName = cursor.getString(cursor.getColumnIndex("firstName"));
		user.lastName = cursor.getString(cursor.getColumnIndex("lastName"));
		user.email = cursor.getString(cursor.getColumnIndex("email"));
		user.driverNumber = cursor.getString(cursor.getColumnIndex("driverNumber"));
		user.deviceToken = cursor.getString(cursor.getColumnIndex("deviceToken"));
		user.deviceID = cursor.getString(cursor.getColumnIndex("deviceID"));
		user.password = cursor.getString(cursor.getColumnIndex("password"));
		user.role = cursor.getString(cursor.getColumnIndex("role"));
		user.userType = cursor.getString(cursor.getColumnIndex("userType"));
		user.activationLink = cursor.getString(cursor.getColumnIndex("activationLink"));
		user.status = cursor.getString(cursor.getColumnIndex("status"));
		user.created = cursor.getString(cursor.getColumnIndex("created"));
		user.modified = cursor.getString(cursor.getColumnIndex("modified"));
		user.fullName = cursor.getString(cursor.getColumnIndex("fullName"));
		user.highClaims = cursor.getInt(cursor.getColumnIndex("highClaims"));
		user.requiresAudit = cursor.getInt(cursor.getColumnIndex("requiresAudit"));
		user.inspectionAccess = cursor.getInt(cursor.getColumnIndex("inspectionAccess"));
		user.supervisorCardCode = cursor.getString(cursor.getColumnIndex("supervisorCardCode"));
		user.supervisorPreloadChk = cursor.getInt(cursor.getColumnIndex("supervisorPreloadChk"));
		user.helpTerm = cursor.getInt(cursor.getColumnIndex("helpTerm"));
		user.driverLicenseExpiration = HelperFuncs.stringToDateFutureDefault(cursor.getString(cursor.getColumnIndex("driverLicenseExpiration")));
		user.medicalCertificateExpiration = HelperFuncs.stringToDateFutureDefault(cursor.getString(cursor.getColumnIndex("medicalCertificateExpiration")));
		user.autoInspectLastDelivery = intToBool(cursor.getInt(cursor.getColumnIndex("autoInspectLastDelivery")));
		return user;
	}

	public static void deleteDeliveryVinTableData(Context context)	{
		transactionFactory(context).deleteDeliveryVinTableData();
	}

	public static void deleteLoadTableData(Context context)	{
		transactionFactory(context).deleteLoadTableData();
	}

	public static void setAreaCodeTableRowsInactive(Context context) {
		transactionFactory(context).setAreaCodeTableRowsInactive();
	}

	public static void setTypeCodeTableRowsInactive(Context context) {
		transactionFactory(context).setTypeCodeTableRowsInactive();
	}

	public static void setSvrtyCodeTableRowsInactive(Context context) {
		transactionFactory(context).setSvrtyCodeTableRowsInactive();
	}

	public static void setSpecialCodeTableRowsInactive(Context context) {
		transactionFactory(context).setSpecialCodeTableRowsInactive();
	}

	public static void setTerminalTableRowsInactive(Context context) {
		transactionFactory(context).setTerminalTableRowsInactive();
	}

	public static void setScacCodeTableRowsInactive(Context context) {
		transactionFactory(context).setScacCodeTableRowsInactive();
	}

	public static void setLotCodeTableRowsInactive(Context context) {
		transactionFactory(context).setLotCodeTableRowsInactive();
	}

	public static void setLotCodeMsgsTableRowsInactive(Context context) {
		transactionFactory(context).setLotCodeMsgsTableRowsInactive();
	}

	public static void setShuttleMoveTableRowsInactive(Context context) {
		transactionFactory(context).setShuttleMoveTableRowsInactive();
	}

	public static void setDamageNoteTemplatesTableRowsInactive(Context context) {
		transactionFactory(context).setDamageNoteTemplatesTableRowsInactive();
	}

	public static void setPredefinedNotesTableRowsInactive(Context context) {
		transactionFactory(context).setPredefinedNotesTableRowsInactive();
	}

	public static void deleteVinTableData(Context context) {
		transactionFactory(context).deleteVinTableData();
	}

	public static void deleteDeliveryTableData(Context context) {
		transactionFactory(context).deleteDeliveryTableData();
	}

	//
	private static void logDeleteMessage(String message, String reason) {
		if (HelperFuncs.isNullOrWhitespace(reason)) {
			log.debug(Logs.DELETES, message);
		}
		else {
			log.debug(Logs.DELETES, String.format("%s (reason: %s)", message, reason));
		}
	}

	public static boolean deleteVinDataFromDB(Context context, int vin_id) {
		return transactionFactory(context).deleteVinDataFromDB(vin_id);
	}

	public static boolean deleteVinDataFromDB(Context context, int vin_id, String reason) {
		logDeleteMessage("deleting vin data for vin " + vin_id, reason);
		return deleteVinDataFromDB(context, vin_id);
	}
	
	public static boolean deleteDeliveryVinDataFromDB(Context context, int delivery_id) {
		return transactionFactory(context).deleteDeliveryVinDataFromDB(delivery_id);
	}

	public static boolean deleteDeliveryVinDataFromDB(Context context, int delivery_id, String reason) {
		logDeleteMessage("deleting delivery vin " + delivery_id, reason);
		return deleteDeliveryVinDataFromDB(context, delivery_id);
	}

	public static boolean deleteDeliveryDataFromDB(Context context, int delivery_id, String reason) {
		logDeleteMessage("deleting delivery " + delivery_id, reason);
		return transactionFactory(context).deleteDeliveryDataFromDB(delivery_id);
	}

	public static DeliveryVin getDeliveryVinForRemoteId(Context context, String remote_id) {
		log.debug(Logs.DATAMANAGER, "getDeliveryVinForRemoteId: " + remote_id);
		return getDeliveryVin(context, transactionFactory(context).getDeliveryVinFromLocalDB(remote_id));
	}

	public static DeliveryVin getDeliveryVin(Context context, int delivery_vin_id) {
		log.debug(Logs.DATAMANAGER, "getDeliveryVin: " + delivery_vin_id);
		return getDeliveryVin(context, transactionFactory(context).getDeliveryVinFromLocalDB(delivery_vin_id));
	}

	public static DeliveryVin getDeliveryVin(Context context, Cursor cursor) {
		DeliveryVin deliveryVin = null;

        if (cursor != null) {
            log.debug(Logs.DATAMANAGER, "found delivery vin results");
            while (cursor.moveToNext()) {
                log.debug(Logs.DATAMANAGER, "converting cursor to delivery vin");
				deliveryVin = cursorToDeliveryVin(cursor);

				int vin_id = cursor.getInt(cursor.getColumnIndex("vin_id"));
				deliveryVin.vin = getVIN(context, vin_id);
                log.debug(Logs.DATAMANAGER, "found vin " + deliveryVin.vin.vin_number);

				deliveryVin.damages = getDeliveryVinDamageList(context, deliveryVin.delivery_vin_id);
				deliveryVin.images = getDeliveryVinImageList(context, deliveryVin.delivery_vin_id, true);

			}
			
			cursor.close();
		} else {
            log.debug(Logs.DATAMANAGER, "Didn't find delivery vin");
		}
		
		return deliveryVin;
	}

	public static List<DeliveryVin> getDeliveryVinList(Context context, int delivery_id) {
		return getDeliveryVinList(context, delivery_id, true);
	}

	public static List<DeliveryVin> getDeliveryVinList(Context context, int delivery_id, boolean includeImages) {
	    ArrayList<DeliveryVin> deliveryVins = new ArrayList<DeliveryVin>();

	    Cursor cursor = transactionFactory(context).getDeliveryVinListFromLocalDB(delivery_id);


        if (cursor != null) {
            while (cursor.moveToNext()) {
				DeliveryVin tmpDeliveryVin = cursorToDeliveryVin(cursor);

				int vin_id = cursor.getInt(cursor.getColumnIndex("vin_id"));
				tmpDeliveryVin.vin = getVIN(context, vin_id);

				tmpDeliveryVin.damages = getDeliveryVinDamageList(context, tmpDeliveryVin.delivery_vin_id);

				if(includeImages) {
					tmpDeliveryVin.images = getDeliveryVinImageList(context, tmpDeliveryVin.delivery_vin_id);
				}

				deliveryVins.add(tmpDeliveryVin);
			}
			
			cursor.close();
		}
		
		return deliveryVins;
	}

	public static DeliveryVin getChildLoadDeliveryVin(Context context, int vin_id) {
		DeliveryVin deliveryVin = null;
		Cursor cursor = transactionFactory(context).getChildLoadDeliveryVin(vin_id);

		if (cursor != null) {
			while (cursor.moveToNext()) {
				deliveryVin = cursorToDeliveryVin(cursor);
				deliveryVin.damages = getDeliveryVinDamageList(context, deliveryVin.delivery_vin_id);
			}
			cursor.close();
		}
		return deliveryVin;
	}

	private static DeliveryVin cursorToDeliveryVin(Cursor cursor) {
		DeliveryVin deliveryVin = new DeliveryVin();
		deliveryVin.delivery_vin_id = cursor.getInt(cursor.getColumnIndex("delivery_vin_id"));
		deliveryVin.delivery_vin_remote_id = cursor.getString(cursor.getColumnIndex("delivery_vin_remote_id"));
		deliveryVin.vin_id = cursor.getInt(cursor.getColumnIndex("vin_id"));
		deliveryVin.delivery_id = cursor.getInt(cursor.getColumnIndex("delivery_id"));
		deliveryVin.token = cursor.getString(cursor.getColumnIndex("token"));
		deliveryVin.timestamp = cursor.getString(cursor.getColumnIndex("timestamp"));
		deliveryVin.facing = cursor.getString(cursor.getColumnIndex("facing"));
		deliveryVin.ats = cursor.getString(cursor.getColumnIndex("ats"));
		deliveryVin.preloadNotes = cursor.getString(cursor.getColumnIndex("preloadNotes"));
		deliveryVin.deliveryNotes = cursor.getString(cursor.getColumnIndex("deliveryNotes"));
		deliveryVin.position = cursor.getString(cursor.getColumnIndex("position"));
		deliveryVin.user_type = cursor.getString(cursor.getColumnIndex("user_type"));
		deliveryVin.status = cursor.getString(cursor.getColumnIndex("status"));
		deliveryVin.key = cursor.getString(cursor.getColumnIndex("key"));
		deliveryVin.byteArray = cursor.getString(cursor.getColumnIndex("byteArray"));

		deliveryVin.preloadUploadStatus = cursor.getInt(cursor.getColumnIndex("preloadUploadStatus"));
		deliveryVin.deliveryUploadStatus = cursor.getInt(cursor.getColumnIndex("deliveryUploadStatus"));

		deliveryVin.pro = cursor.getString(cursor.getColumnIndex("pro"));
		deliveryVin.ldseq = cursor.getString(cursor.getColumnIndex("ldseq"));
		deliveryVin.bckhlnbr = cursor.getString(cursor.getColumnIndex("bckhlnbr"));
		deliveryVin.rowbay = cursor.getString(cursor.getColumnIndex("rowbay"));
		deliveryVin.backdrv = cursor.getString(cursor.getColumnIndex("backdrv"));
		deliveryVin.rejected_by = cursor.getString(cursor.getColumnIndex("rejected_by"));
		deliveryVin.rldspickup = cursor.getString(cursor.getColumnIndex("rldspickup"));

		if(cursor.getColumnIndex("do_lotlocate") != -1) {
			deliveryVin.do_lotlocate = cursor.getString(cursor.getColumnIndex("do_lotlocate"));
		}

		deliveryVin.lot = cursor.getString(cursor.getColumnIndex("lot"));
		deliveryVin.von = cursor.getString(cursor.getColumnIndex("von"));
		deliveryVin.rte1 = cursor.getString(cursor.getColumnIndex("rte1"));
		deliveryVin.rte2	 = cursor.getString(cursor.getColumnIndex("rte2"));
		deliveryVin.status = cursor.getString(cursor.getColumnIndex("status"));
		deliveryVin.ldseq = cursor.getString(cursor.getColumnIndex("ldseq"));
		deliveryVin.inspectedPreload = cursor.getInt(cursor.getColumnIndex("inspectedPreload")) == 1 ? true : false;
		deliveryVin.inspectedDelivery = cursor.getInt(cursor.getColumnIndex("inspectedDelivery")) == 1 ? true : false;
		deliveryVin.shuttleLoadProductionStatus = cursor.getString(cursor.getColumnIndex("shuttleLoadProdStatus"));
		deliveryVin.shuttleLoadRoute = cursor.getString(cursor.getColumnIndex("shuttleLoadRoute"));

		deliveryVin.supervisorSignature = cursor.getString(cursor.getColumnIndex("supervisorSignature"));
		deliveryVin.supervisorSignatureSignedAt = cursor.getString(cursor.getColumnIndex("supervisorSignatureSignedAt"));
		deliveryVin.supervisorSignatureLat = cursor.getString(cursor.getColumnIndex("supervisorSignatureLat"));
		deliveryVin.supervisorSignatureLon = cursor.getString(cursor.getColumnIndex("supervisorSignatureLon"));
		deliveryVin.supervisorComment = cursor.getString(cursor.getColumnIndex("supervisorComment"));
		deliveryVin.supervisorContact = cursor.getString(cursor.getColumnIndex("supervisorContact"));
		deliveryVin.uploaded = cursor.getInt(cursor.getColumnIndex("uploaded")) == 1 ? true : false;
		deliveryVin.finalMfg = cursor.getString(cursor.getColumnIndex("finalMfg"));
		deliveryVin.finalDealer = cursor.getString(cursor.getColumnIndex("finalDealer"));


		return deliveryVin;
	}

	public static ArrayList<Damage> getDeliveryVinDamageList(Context context,
			int delivery_vin_id) {
		Cursor cursor = transactionFactory(context).getDamagesForDeliveryVinFromLocalDB(delivery_vin_id);

		ArrayList<Damage> damages = new ArrayList<Damage>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
				Damage tmpDamage = cursorToDamage(cursor);

				tmpDamage.areaCode = getAreaCode(context, tmpDamage.area_code_id);
				tmpDamage.typeCode = getTypeCode(context, tmpDamage.type_code_id);
				tmpDamage.severityCode = getSeverityCode(context, tmpDamage.svrty_code_id);
				tmpDamage.specialCode = getSpecialCode(context, tmpDamage.special_code_id);
				damages.add(tmpDamage);
			}
			
			cursor.close();
		}
		
		return damages;
	}

	public static SeverityCode getSeverityCodeByRemoteId(Context context, int svrty_code_id) {
		return getSeverityCode(transactionFactory(context).getSeverityCodeByRemoteIdFromLocalDB(svrty_code_id));
	}

	public static SeverityCode getSeverityCode(Context context, int svrty_code_id) {
		return getSeverityCode(transactionFactory(context).getSeverityCodeFromLocalDB(svrty_code_id));
	}

	private static SeverityCode getSeverityCode(Cursor cursor) {
		SeverityCode severityCode= new SeverityCode();

        if (cursor != null) {
            while (cursor.moveToNext()) {
				severityCode = cursorToSeverityCode(cursor);
			}
			
			cursor.close();
		}
		
		return severityCode;
	}

	public static TypeCode getTypeCodeByRemoteId(Context context, int type_code_id) {
		return getTypeCodeByCursor(transactionFactory(context).getTypeCodeByRemoteIdFromLocalDB(type_code_id));
	}

	public static TypeCode getTypeCode(Context context, int type_code_id) {
		 return getTypeCodeByCursor(transactionFactory(context).getTypeCodeFromLocalDB(type_code_id));
	}

	private static TypeCode getTypeCodeByCursor(Cursor cursor) {
		TypeCode typeCode= new TypeCode();

        if (cursor != null) {
            while (cursor.moveToNext()) {
				typeCode = cursorToTypeCode(cursor);
			}
			
			cursor.close();
		}
		
		return typeCode;
	}

	public static AreaCode getAreaCode(Context context, int area_code_id) {
		Cursor cursor = transactionFactory(context).getAreaCodeFromLocalDB(area_code_id);

		AreaCode areaCode= new AreaCode();

        if (cursor != null) {
            while (cursor.moveToNext()) {
				areaCode = cursorToAreaCode(cursor);
			}
			
			cursor.close();
		}
		
		return areaCode;
	}

	private static AreaCode cursorToAreaCode(Cursor cursor) {
		AreaCode areaCode = new AreaCode();

		if(cursor.getColumnIndex("active") >= 0) {
			areaCode.active = getActiveStatus(cursor);
		}

		areaCode.setCode(cursor.getString(cursor.getColumnIndex("code")));
		areaCode.setDescription(cursor.getString(cursor.getColumnIndex("description")));
		areaCode.parent_area_code_id = cursor.getInt(cursor.getColumnIndex("parent_area_code_id"));
		areaCode.area_code_id = cursor.getInt(cursor.getColumnIndex("area_code_id"));
		areaCode.area_code_remote_id = cursor.getString(cursor.getColumnIndex("area_code_remote_id"));


		return areaCode;
	}

	private static TypeCode cursorToTypeCode(Cursor cursor) {
		TypeCode typeCode = new TypeCode();

		if(cursor.getColumnIndex("active") >= 0) {
			typeCode.active = getActiveStatus(cursor);
		}

		typeCode.setCode(cursor.getString(cursor.getColumnIndex("code")));
		typeCode.setDescription(cursor.getString(cursor.getColumnIndex("description")));
		typeCode.type_code_id = cursor.getInt(cursor.getColumnIndex("type_code_id"));
		typeCode.type_code_remote_id = cursor.getString(cursor.getColumnIndex("type_code_remote_id"));

		return typeCode;
	}


	private static SeverityCode cursorToSeverityCode(Cursor cursor) {
		SeverityCode severityCode = new SeverityCode();

		if(cursor.getColumnIndex("active") >= 0) {
			severityCode.active = getActiveStatus(cursor);
		}

		severityCode.setCode(cursor.getString(cursor.getColumnIndex("code")));
		severityCode.setDescription(cursor.getString(cursor.getColumnIndex("description")));
		severityCode.severity_code_id = cursor.getInt(cursor.getColumnIndex("severity_code_id"));
		severityCode.severity_code_remote_id = cursor.getString(cursor.getColumnIndex("severity_code_remote_id"));


		return severityCode;
	}

	private static Damage cursorToDamage(Cursor cursor) {
		Damage damage = new Damage();
		damage.damage_id = cursor.getInt(cursor.getColumnIndex("damage_id"));

		if(cursor.getColumnIndex("delivery_vin_id") != -1)
		  damage.delivery_vin_id = cursor.getInt(cursor.getColumnIndex("delivery_vin_id"));

		if(cursor.getColumnIndex("inspection_guid") != -1)
		  damage.inspection_id = cursor.getInt(cursor.getColumnIndex("inspection_guid"));

		if(cursor.getColumnIndex("guid") != -1)
			damage.guid = cursor.getString(cursor.getColumnIndex("guid"));

		damage.type_code_id = cursor.getInt(cursor.getColumnIndex("type_code_id"));
		damage.svrty_code_id = cursor.getInt(cursor.getColumnIndex("svrty_code_id"));
		damage.area_code_id = cursor.getInt(cursor.getColumnIndex("area_code_id"));
		damage.special_code_id = cursor.getInt(cursor.getColumnIndex("special_code_id"));
		damage.uploaded = cursor.getInt(cursor.getColumnIndex("uploaded")) == 1 ? true : false;
		damage.preLoadDamage = cursor.getInt(cursor.getColumnIndex("preLoadDamage")) == 1 ? true : false;
		damage.uploaded = cursor.getInt(cursor.getColumnIndex("uploaded")) == 1 ? true : false;
		damage.readonly = cursor.getInt(cursor.getColumnIndex("readonly")) == 1 ? true : false;
		damage.source = cursor.getString(cursor.getColumnIndex("source"));

		if(damage.source == null) {
			damage.source = "driver";
		}


		damage.preloadUploadStatus = cursor.getInt(cursor.getColumnIndex("preloadUploadStatus"));
		damage.deliveryUploadStatus = cursor.getInt(cursor.getColumnIndex("deliveryUploadStatus"));


		return damage;
	}

	private static VIN getVIN(Context context, int vin_id) {
		Cursor cursor = transactionFactory(context).getVinFromLocalDB(vin_id);

		VIN vin = new VIN();

        if (cursor != null) {
            while (cursor.moveToNext()) {
				vin = cursorToVIN(cursor);
			}
			
			cursor.close();
		}
		
		return vin;
	}

	public static Delivery getDeliveryForRemoteId(Context context, String delivery_remote_id) 	{
		Cursor cursor = transactionFactory(context).getDeliveryFromLocalDBForRemote(delivery_remote_id);

		Delivery delivery = new Delivery();

        if (cursor != null) {
            while (cursor.moveToNext()) {
				delivery = cursorToDelivery(cursor);

				delivery.deliveryVins = getDeliveryVinList(context, delivery.delivery_id);

				int dealer_id = cursor.getInt(cursor.getColumnIndex("dealer_id"));
				delivery.dealer = getDealer(context, dealer_id);

				if (!HelperFuncs.isNullOrEmpty(delivery.delivery_remote_id)) {
					delivery.images = getDeliveryImageList(context, Integer.parseInt(delivery.delivery_remote_id), true);
				}
			}
			
			cursor.close();
		}
		
		return delivery;
	}
	
	public static Delivery getDelivery(Context context, int delivery_id) 	{
		Cursor cursor = transactionFactory(context).getDeliveryFromLocalDB(delivery_id);

		Delivery delivery = new Delivery();

        if (cursor != null) {
            while (cursor.moveToNext()) {
				delivery = cursorToDelivery(cursor);

				delivery.deliveryVins = getDeliveryVinList(context, delivery.delivery_id);

				int dealer_id = cursor.getInt(cursor.getColumnIndex("dealer_id"));
				delivery.dealer = getDealer(context, dealer_id);
                if (!HelperFuncs.isNullOrEmpty(delivery.delivery_remote_id)) {
                    delivery.images = getDeliveryImageList(context, Integer.parseInt(delivery.delivery_remote_id), true);
                }
			}
			
			cursor.close();
		}
		
		return delivery;
	}

	public static List<Delivery> getDeliveryList(Context context, int load_id) 	{
		return getDeliveryList(context, load_id, true);
	}

	public static List<Delivery> getDeliveryList(Context context, int load_id, boolean includeImages) {
		Cursor cursor = transactionFactory(context).getDeliveriesFromLocalDB(load_id);
		ArrayList<Delivery> deliveries = new ArrayList<Delivery>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
				Delivery tmpDelivery = cursorToDelivery(cursor);

				tmpDelivery.deliveryVins = getDeliveryVinList(context, tmpDelivery.delivery_id, includeImages);

				int dealer_id = cursor.getInt(cursor.getColumnIndex("dealer_id"));
				tmpDelivery.dealer = getDealer(context, dealer_id);

			    if (!HelperFuncs.isNullOrEmpty(tmpDelivery.delivery_remote_id) && includeImages) {
                    tmpDelivery.images = getDeliveryImageList(context, Integer.parseInt(tmpDelivery.delivery_remote_id), true);
                }
                //log.debug(Logs.DATAMANAGER, "Found " + tmpDelivery.images.size() + " supplemental delivery images.");

				deliveries.add(tmpDelivery);
			}
			
			cursor.close();
		}
		return deliveries;
	}

	public static List<Delivery> getDeliveryListLazy(Context context, int load_id) 	{
		Cursor cursor = transactionFactory(context).getDeliveriesFromLocalDB(load_id);

		ArrayList<Delivery> deliveries = new ArrayList<Delivery>();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				Delivery tmpDelivery = cursorToDelivery(cursor);

				//tmpDelivery.deliveryVins = getDeliveryVinList(context, tmpDelivery.delivery_id);

				int dealer_id = cursor.getInt(cursor.getColumnIndex("dealer_id"));
				tmpDelivery.dealer = getDealer(context, dealer_id);

				/*
				if (!HelperFuncs.isNullOrEmpty(tmpDelivery.delivery_remote_id)) {
					tmpDelivery.images = getDeliveryImageList(context, Integer.parseInt(tmpDelivery.delivery_remote_id), true);
				}
				log.debug(Logs.DATAMANAGER, "Found " + tmpDelivery.images.size() + " supplemental delivery images.");
				*/

				deliveries.add(tmpDelivery);
			}

			cursor.close();
		}

		return deliveries;
	}
	
    //Shuttle moves are filtered based on terminal% or terminal%origin%

    public static List<ShuttleMove> getShuttleMoves(Context context, String terminal, String origin, String destination) {
        Cursor cursor = transactionFactory(context).getShuttleMovesFromDb(terminal, origin, destination);
        List<ShuttleMove> shuttleMoveList = new ArrayList<ShuttleMove>();
        
        if(cursor != null) {
        	ShuttleMove move = null;
            while (cursor.moveToNext()) {
	        	move = new ShuttleMove(cursor.getString(cursor.getColumnIndex("orgDestString")));
				move.shuttleMoveId = cursor.getInt(cursor.getColumnIndex("shuttle_move_id"));
				move.setProps(move.orgDestString);
                //log.debug(Logs.DATAMANAGER, cursor.getString(cursor.getColumnIndex("terminal")));

				shuttleMoveList.add(move);
			}

            cursor.close();
        }
        return shuttleMoveList;    	
    }

	public static Date getAreaCodesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getAreaCodesLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static Date getTypeCodesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getTypeCodesLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static Date getSeverityCodesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getSeverityCodesLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static Date getSpecialCodesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getSpecialCodesLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static Date getTerminalsLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getTerminalsLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static Date getScacCodesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getScacCodesLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static Date getShuttleMovesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getShuttleMovesLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	//Shuttle moves are filtered based on terminal% or terminal%origin%
	public static ShuttleMove getShuttleMoveForLoad(Context context, int move_id) {
		Cursor cursor = transactionFactory(context).getShuttleMovesForLoadFromDb(move_id);

		ShuttleMove move = null;

		if(cursor != null) {
        	
            while (cursor.moveToNext()) {
	        	move = new ShuttleMove(cursor.getString(cursor.getColumnIndex("orgDestString")));
				move.shuttleMoveId = cursor.getInt(cursor.getColumnIndex("shuttle_move_id"));
                //log.debug(Logs.DATAMANAGER, cursor.getString(cursor.getColumnIndex("terminal")));
			}
	        
            cursor.close();
        }
        
        return move;    	
    }    

	public static Dealer getDealer(Context context, int dealer_id) {
		Cursor cursor = transactionFactory(context).getDealerFromLocalDB(dealer_id);
		Dealer dealer = null;

        if (cursor != null) {
            while (cursor.moveToNext()) {
				dealer = cursorToDealer(cursor);
			}
		
			cursor.close();
		}
		
		return dealer;
	}

	public static Dealer getDealer(Context context, String customer_number, String mfg) {
		Cursor cursor = transactionFactory(context).getDealerFromLocalDB(customer_number, mfg);
		Dealer dealer = null;

		if (cursor != null) {
			while (cursor.moveToNext()) {
				dealer = cursorToDealer(cursor);
			}

			cursor.close();
		}

		return dealer;
	}

	private static Dealer cursorToDealer(Cursor cursor) {
		Dealer tmpDealer = new Dealer();

		tmpDealer.dealer_id = cursor.getInt(cursor.getColumnIndex("dealer_id"));
        tmpDealer.dealer_remote_id = cursor.getString(cursor.getColumnIndex("dealer_remote_id"));
		tmpDealer.mfg = cursor.getString(cursor.getColumnIndex("mfg"));
		tmpDealer.customer_number = cursor.getString(cursor.getColumnIndex("customer_number"));
		tmpDealer.customer_name = cursor.getString(cursor.getColumnIndex("customer_name"));
		tmpDealer.city = cursor.getString(cursor.getColumnIndex("city"));
		tmpDealer.state = cursor.getString(cursor.getColumnIndex("state"));
		tmpDealer.address = cursor.getString(cursor.getColumnIndex("address"));
		tmpDealer.zip = cursor.getString(cursor.getColumnIndex("zip"));
		tmpDealer.contact_name = cursor.getString(cursor.getColumnIndex("contact_name"));
		tmpDealer.email = cursor.getString(cursor.getColumnIndex("email"));
		tmpDealer.phone = cursor.getString(cursor.getColumnIndex("phone"));
		tmpDealer.monam = cursor.getInt(cursor.getColumnIndex("monam"));
		tmpDealer.tueam = cursor.getInt(cursor.getColumnIndex("tueam"));
		tmpDealer.wedam = cursor.getInt(cursor.getColumnIndex("wedam"));
		tmpDealer.thuam = cursor.getInt(cursor.getColumnIndex("thuam"));
		tmpDealer.friam = cursor.getInt(cursor.getColumnIndex("friam"));
		tmpDealer.satam = cursor.getInt(cursor.getColumnIndex("satam"));
		tmpDealer.sunam = cursor.getInt(cursor.getColumnIndex("sunam"));
		tmpDealer.monpm = cursor.getInt(cursor.getColumnIndex("monpm"));
		tmpDealer.tuepm = cursor.getInt(cursor.getColumnIndex("tuepm"));
		tmpDealer.wedpm = cursor.getInt(cursor.getColumnIndex("wedpm"));
		tmpDealer.thupm = cursor.getInt(cursor.getColumnIndex("thupm"));
		tmpDealer.fripm = cursor.getInt(cursor.getColumnIndex("fripm"));
		tmpDealer.satpm = cursor.getInt(cursor.getColumnIndex("satpm"));
		tmpDealer.sunpm = cursor.getInt(cursor.getColumnIndex("sunpm"));
		tmpDealer.afthr = cursor.getString(cursor.getColumnIndex("afthr"));
		tmpDealer.comments = cursor.getString(cursor.getColumnIndex("comments"));
		tmpDealer.high_claims = intToBool(cursor.getInt(cursor.getColumnIndex("high_claims")));
		tmpDealer.alwaysUnattended = intToBool(cursor.getInt(cursor.getColumnIndex("alwaysUnattended")));
		tmpDealer.photosOnUnattended = intToBool(cursor.getInt(cursor.getColumnIndex("photosOnUnattended")));
		tmpDealer.lotLocateRequired = intToBool(cursor.getInt(cursor.getColumnIndex("lotLocateRequired")));
		tmpDealer.lot_code_id = cursor.getInt(cursor.getColumnIndex("lot_code_id"));
		tmpDealer.countryCode = cursor.getString(cursor.getColumnIndex("countryCode"));

		try {
			if (!cursor.getString(cursor.getColumnIndex("updated_fields")).equals("")) {
				for (String field : Arrays.asList((cursor.getString(cursor.getColumnIndex("updated_fields")).split(",")))) {
					String[] fieldValues = field.split(" ");
					String fieldName = fieldValues[0];
					long dateModified = Long.parseLong(fieldValues[1]);
					tmpDealer.insertUpdatedField(fieldName, dateModified);
				}
			}
		} catch (Exception e) {
			System.out.println(e);
		}

		String lastUpdated = cursor.getString(cursor.getColumnIndex("last_updated"));

		if(!HelperFuncs.isNullOrEmpty(lastUpdated)) {
			try {
				tmpDealer.lastUpdated = Constants.dateFormatter().parse(lastUpdated);
			} catch (ParseException e) {
			}
		}

		return tmpDealer;
	}

	private static Delivery cursorToDelivery(Cursor cursor) {

		Delivery tmpDelivery = new Delivery();

		try {
			tmpDelivery.delivery_remote_id = cursor.getString(cursor.getColumnIndex("delivery_remote_id"));
		} catch (Exception ex) {
			ex.printStackTrace();
			throw ex;
		}
		tmpDelivery.delivery_id = cursor.getInt(cursor.getColumnIndex("delivery_id"));
		tmpDelivery.timestamp = cursor.getString(cursor.getColumnIndex("timestamp"));
		tmpDelivery.token = cursor.getString(cursor.getColumnIndex("token"));
		tmpDelivery.load_id = cursor.getInt(cursor.getColumnIndex("load_id"));
		tmpDelivery.driverSignature = cursor.getString (cursor.getColumnIndex("driverSignature"));
		tmpDelivery.driverSignatureSignedAt = cursor.getString (cursor.getColumnIndex("driverSignatureSignedAt"));
		tmpDelivery.dealerSignature = cursor.getString (cursor.getColumnIndex("dealerSignature"));
		tmpDelivery.dealerSignatureSignedAt = cursor.getString (cursor.getColumnIndex("dealerSignatureSignedAt"));
		tmpDelivery.dealerContact = cursor.getString (cursor.getColumnIndex("dealerContact"));
		tmpDelivery.dealerEmail = cursor.getString (cursor.getColumnIndex("dealerEmail"));
		tmpDelivery.dealerSignatureLat = cursor.getString (cursor.getColumnIndex("dealerSignatureLat"));
		tmpDelivery.dealerSignatureLon = cursor.getString (cursor.getColumnIndex("dealerSignatureLon"));
		tmpDelivery.driverSignatureLat = cursor.getString(cursor.getColumnIndex("driverSignatureLat"));
		tmpDelivery.driverSignatureLon = cursor.getString(cursor.getColumnIndex("driverSignatureLon"));
		tmpDelivery.sti = cursor.getInt(cursor.getColumnIndex("sti"));
		tmpDelivery.afrhrs = cursor.getInt(cursor.getColumnIndex("afrhrs"));
		tmpDelivery.userType = cursor.getString(cursor.getColumnIndex("userType"));
		tmpDelivery.driverComment = cursor.getString(cursor.getColumnIndex("driverComment"));
		tmpDelivery.driverContact = cursor.getString(cursor.getColumnIndex("driverContact"));
		tmpDelivery.dealerComment = cursor.getString(cursor.getColumnIndex("dealerComment"));
		tmpDelivery.ship_date = cursor.getString(cursor.getColumnIndex("ship_date"));
		tmpDelivery.estdeliverdate = cursor.getString(cursor.getColumnIndex("estdeliverdate"));
		tmpDelivery.status = cursor.getString(cursor.getColumnIndex("status"));
		tmpDelivery.delivery = cursor.getString(cursor.getColumnIndex("delivery"));
		tmpDelivery.callback = cursor.getString(cursor.getColumnIndex("callback"));
		tmpDelivery.uploaded = cursor.getInt(cursor.getColumnIndex("uploaded")) == 1 ? true : false;
		tmpDelivery.shuttleLoad = cursor.getInt(cursor.getColumnIndex("shuttleLoad")) == 1 ? true : false;
        tmpDelivery.notes = cursor.getString(cursor.getColumnIndex("notes"));
		tmpDelivery.dockTerm = cursor.getInt(cursor.getColumnIndex("dockTerm"));

		tmpDelivery.preloadUploadStatus = cursor.getInt(cursor.getColumnIndex("preloadUploadStatus"));
		tmpDelivery.deliveryUploadStatus = cursor.getInt(cursor.getColumnIndex("deliveryUploadStatus"));
		tmpDelivery.safeDelivery = cursor.getString(cursor.getColumnIndex("safeDelivery"));


		return tmpDelivery;
	}

	public static long insertImageToLocalDB(Context context, Image image) {
		return transactionFactory(context).insertImageToLocalDb(image);
	}

	public static ArrayList<Image> getDeliveryVinImageList(Context context, int delivery_vin_id) {
        return getImageList(context, getDeliveryVinImageCursor(context, delivery_vin_id, false));
	}
	
	public static ArrayList<Image> getDeliveryVinImageList(Context context, int delivery_vin_id, boolean includeImageData) {
        return getImageList(context, getDeliveryVinImageCursor(context, delivery_vin_id, includeImageData));
    }

    public static Cursor getDeliveryVinImageCursor(Context context, int delivery_vin_id, boolean includeImageData) {
        return transactionFactory(context).getDeliveryVinImageListFromLocalDB(delivery_vin_id, includeImageData);
    }


    public static ArrayList<Image> getLoadImageList(Context context, String load_id, boolean includeImageData) {
        return getImageList(context, getLoadImageCursor(context, load_id, includeImageData));
    }

    public static Cursor getLoadImageCursor(Context context, String load_id, boolean includeImageData) {
        return transactionFactory(context).getLoadImageListFromLocalDB(load_id, includeImageData);
    }

    public static ArrayList<Image> getDeliveryImageList(Context context, int delivery_vin_id) {
        return getImageList(context, getDeliveryImageCursor(context, delivery_vin_id, false));
    }

    public static ArrayList<Image> getDeliveryImageList(Context context, int delivery_id, boolean includeImageData) {
        return getImageList(context, getDeliveryImageCursor(context, delivery_id, includeImageData));
    }

    public static Cursor getDeliveryImageCursor(Context context, int delivery_vin_id, boolean includeImageData) {
        return transactionFactory(context).getDeliveryImageListFromLocalDB(delivery_vin_id, includeImageData);
    }

    public static ArrayList<Image> getImageList(Context context, Cursor cursor) {

		ArrayList<Image> images = new ArrayList<Image>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
				Image image = cursorToImage(context, cursor);

				images.add(image);
			}
			
			cursor.close();
		}
		
		return images;
	}

	public static Image getImageForFilename(Context context, String filename) {
		Cursor cursor = transactionFactory(context).getImageFromLocalDB(filename);
		Image image = null;

		if (cursor != null) {
			while (cursor.moveToNext()) {
				image = cursorToImage(context,cursor);
			}

			cursor.close();
		}

		return image;
	}

    public static Image getImage(Context context, int image_id) {
		Cursor cursor = transactionFactory(context).getImageFromLocalDB(image_id);
		Image image = null;

        if (cursor != null) {
            while (cursor.moveToNext()) {
				image = cursorToImage(context, cursor);
			}
			
			cursor.close();
		}
		
		return image;
	}

	public static Inspection getInspection(Context context, int inspection_id) {
		Cursor cursor = transactionFactory(context).getInspectionFromLocalDB(inspection_id);
		Inspection inspection = null;

		if (cursor != null) {
			while (cursor.moveToNext()) {
				inspection = cursorToInspection(cursor);
                inspection.damages = getInspectionDamageList(context, inspection.guid);
                inspection.images = getInspectionImageList(context, inspection.guid);

                inspection.lotCode = getLotCode(context, cursor.getInt(cursor.getColumnIndex("lot_code_id")));
                inspection.scacCode = getScacCode(context, cursor.getInt(cursor.getColumnIndex("scac_code_id")));
                inspection.terminal = getTerminal(context, cursor.getInt(cursor.getColumnIndex("terminal_id")));
			}

			cursor.close();
		}

		return inspection;
	}

	public static Inspection getInspectionFromGuid(Context context, String inspection_guid) {
		Cursor cursor = transactionFactory(context).getInspectionFromLocalDB(inspection_guid);
		Inspection inspection = null;

		if (cursor != null) {
			while (cursor.moveToNext()) {
				inspection = cursorToInspection(cursor);
                inspection.damages = getInspectionDamageList(context, inspection.guid);
                inspection.images = getInspectionImageList(context, inspection.guid);

                inspection.lotCode = getLotCode(context, cursor.getInt(cursor.getColumnIndex("lot_code_id")));
                inspection.scacCode = getScacCode(context, cursor.getInt(cursor.getColumnIndex("scac_code_id")));
                inspection.terminal = getTerminal(context, cursor.getInt(cursor.getColumnIndex("terminal_id")));
			}
			cursor.close();
		}

		return inspection;
	}

	public static String getFolderName(Context context, int image_id) {
		Image image = getImage(context, image_id);
		String folderName = "";

		if (image.problem_report_guid != null) {
			//no load number, assign to problem report folder
			ProblemReport problemReport = DataManager.getProblemReport(context, image.problem_report_guid);
			if (problemReport != null) {
				folderName = "problem_report" + "/" + problemReport.driver_id;
			}
		} else if (image.load_id != -1) {

			Load imageLoad = null;

			imageLoad = getLoadForRemoteId(context, String.valueOf(image.load_id));
			if(imageLoad == null) {
				log.debug("Finding remote load for " + image.load_id + " was null. probably a shuttle move...");
				imageLoad = getLoad(context, image.load_id);
			}

			if(imageLoad != null) {
				folderName = imageLoad.loadNumber;
			} else {
				folderName = "unknown_load_" + image.load_id + "-" + CommonUtility.getDriverNumber(context);
			}
		} else if (image.delivery_vin_id != -1) {
			DeliveryVin deliveryVin = getDeliveryVin(context, image.delivery_vin_id);
			if(deliveryVin != null && deliveryVin.delivery_id != -1) {
				int delivery_id = deliveryVin.delivery_id;
				if (getDelivery(context, delivery_id) == null) {
					log.debug(Logs.DEBUG, "getDelivery() returned null for delivery_id " + delivery_id);
					return ("unknown_images/");
				}
				else if (getLoad(context, getDelivery(context, delivery_id).load_id) == null) {
					log.debug(Logs.DEBUG, "getLoad() returned null for load_id " + getDelivery(context, delivery_id).load_id);
					return ("unknown_images/");
				}
				else {
					folderName = getLoad(context, getDelivery(context, delivery_id).load_id).loadNumber;
				}
			} else {
				return "unknown_images/";
			}

		} else if (image.inspection_guid != null) {
			Inspection inspection = DataManager.getInspectionFromGuid(context, image.inspection_guid);
			folderName = "inspections" + "/" + inspection.vin;
		}
		return folderName;
	}

	public static void deleteImage(Context context, Image image, String reason) {
		logDeleteMessage(
				String.format("deleting image id=%d, filename='%s'",
						image.image_id, HelperFuncs.noNull(image.filename, "null")),
				reason);
		if (image.filename != null) {
			CommonUtility.deleteCachedImageFile(context, image.filename);
		}
		transactionFactory(context).deleteImage(image.image_id);
	}

	public static void deleteOldPickSheetAndExtraImages(Context context, String load_remote_id, String extraImageTag) {
		//Log.d("NARF", "trying to delete old pick sheet and extra images");
		Cursor cursor = transactionFactory(context).getPickSheetAndExtraImages(load_remote_id, extraImageTag);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				Image extraImage = cursorToImage(context, cursor);
				deleteImage(context, extraImage, "reset extra/pick sheet images");
			}
			cursor.close();
		}
	}

	public static Image cursorToImage(Context context, Cursor cursor) {
		Image tmpImage = new Image();
		tmpImage.image_id = cursor.getInt(cursor.getColumnIndex("image_id"));

		if(cursor.getColumnIndex("filename") != -1)
			tmpImage.filename = cursor.getString(cursor.getColumnIndex("filename"));

		if(cursor.getColumnIndex("delivery_vin_id") != -1)
		  tmpImage.delivery_vin_id = cursor.getInt(cursor.getColumnIndex("delivery_vin_id"));
        
		if (cursor.getColumnIndex("load_id") != -1)
            tmpImage.load_id = cursor.getInt(cursor.getColumnIndex("load_id"));

        if (cursor.getColumnIndex("delivery_id") != -1)
            tmpImage.delivery_id = cursor.getInt(cursor.getColumnIndex("delivery_id"));

		if (cursor.getColumnIndex("inspection_guid") != -1)
		  tmpImage.inspection_guid = cursor.getString(cursor.getColumnIndex("inspection_guid"));

		if (cursor.getColumnIndex("problem_report_guid") != -1)
			tmpImage.problem_report_guid = cursor.getString(cursor.getColumnIndex("problem_report_guid"));

		tmpImage.uploaded = cursor.getInt(cursor.getColumnIndex("uploaded")) == 0 ? false : true;
		tmpImage.preloadImage = cursor.getInt(cursor.getColumnIndex("preloadImage")) == 0 ? false : true;
		tmpImage.imageLat = cursor.getString(cursor.getColumnIndex("imageLat"));
		tmpImage.imageLon = cursor.getString(cursor.getColumnIndex("imageLon"));


		tmpImage.preloadUploadStatus = cursor.getInt(cursor.getColumnIndex("preloadUploadStatus"));
		tmpImage.deliveryUploadStatus = cursor.getInt(cursor.getColumnIndex("deliveryUploadStatus"));
		tmpImage.uploadStatus = cursor.getInt(cursor.getColumnIndex("uploadStatus"));
		tmpImage.s3_upload_status = cursor.getInt(cursor.getColumnIndex("s3_upload_status"));
		tmpImage.foreignKey = cursor.getInt(cursor.getColumnIndex("foreignKey"));
		tmpImage.foreignKeyLabel = cursor.getString(cursor.getColumnIndex("foreignKeyLabel"));
		tmpImage.filename = cursor.getString(cursor.getColumnIndex("filename"));

		if (cursor.getColumnIndex("preauth_url") != -1) {
			tmpImage.preauth_url = cursor.getString(cursor.getColumnIndex("preauth_url"));
		}
		tmpImage.retries = cursor.getInt(cursor.getColumnIndex("retries"));

		return tmpImage;
	}

	public static User getUserForRemoteId(Context context,
		int driver_remote_id) {
		Cursor cursor = transactionFactory(context).getUserFromDBForRemoteId(driver_remote_id);
		User user = null;

        if (cursor != null) {
            while (cursor.moveToNext()) {
				user = cursorToUser(cursor);
				break;
			}
			
			cursor.close();
		}
		
		return user;

	}

	public static SpecialCode getSpecialCode(Context context,
			String area_code, String type_code, String severity_code) {
		SpecialCode specialCodes = new SpecialCode();
		SpecialCode tmpSpecialCode = null;

		Cursor cursor = transactionFactory(context).getSpecialCodeFromLocalDB(area_code, type_code, severity_code);
        if (cursor != null) {
            while (cursor.moveToNext()) {
				tmpSpecialCode = cursorToSpecialCode(cursor);
				break;
			}
			cursor.close();
		}
		
		return tmpSpecialCode;
	}

	public static SpecialCode getSpecialCode(Context context, int special_code_id) {
		SpecialCode tmpSpecialCode = null;

		Cursor cursor = transactionFactory(context).getSpecialCodeFromLocalDB(special_code_id);
        if (cursor != null) {
            while (cursor.moveToNext()) {
				tmpSpecialCode = cursorToSpecialCode(cursor);
				break;
			}
			cursor.close();
		}
		
		return tmpSpecialCode;
	}

	public static SpecialCode getSpecialCodeByCode(Context context, String special_code) {
		SpecialCode tmpSpecialCode = null;

		Cursor cursor = transactionFactory(context).getSpecialCodeForCodeFromLocalDB(special_code);
        if (cursor != null) {
            while (cursor.moveToNext()) {
				tmpSpecialCode = cursorToSpecialCode(cursor);
				break;
			}
			
			cursor.close();
		}
		
		return tmpSpecialCode;
	}	

	public static AreaCode getAreaCode(Context context, String areaString) {
		AreaCode tmpAreaCode = null;

		Cursor cursor = transactionFactory(context).getAreaCodeFromLocalDB(areaString);
        if (cursor != null) {
            while (cursor.moveToNext()) {
				tmpAreaCode = cursorToAreaCode(cursor);
              break;
			}
			cursor.close();
		}
		
		return tmpAreaCode;
	}

	public static AreaCode getAreaCodeByRemoteId(Context context, int area_code_remote_id) {
		return getAreaCode(transactionFactory(context).getAreaCodeByRemoteIdFromLocalDB(area_code_remote_id));
	}

	public static AreaCode getAreaCodeById(Context context, int areaId) {
		 return getAreaCode(transactionFactory(context).getAreaCodeByIdFromLocalDB(areaId));
	}

	private static AreaCode getAreaCode(Cursor cursor) {
		AreaCode tmpAreaCode = null;

        if (cursor != null) {
            while (cursor.moveToNext()) {
            	tmpAreaCode = cursorToAreaCode(cursor);
            	break;
			}
			cursor.close();
		}
		
		return tmpAreaCode;
	}

	public static TypeCode getTypeCode(Context context, String typeString) {
		TypeCode tmpTypeCode = null;

		Cursor cursor = transactionFactory(context).getTypeCodeFromLocalDB(typeString);
        if (cursor != null) {
            while (cursor.moveToNext()) {
				tmpTypeCode = cursorToTypeCode(cursor);
				break;
			}
			cursor.close();
		}
		
		return tmpTypeCode;
	}

	public static SeverityCode getSeverityCode(Context context, String svrtyString) {
		SeverityCode tmpSeverityCode = null;

		Cursor cursor = transactionFactory(context).getSeverityCodeFromLocalDB(svrtyString);
        if (cursor != null) {
            while (cursor.moveToNext()) {
            	tmpSeverityCode = cursorToSeverityCode(cursor);
            	break;
			}
			cursor.close();
		}
		
		return tmpSeverityCode;
	}

	public static void deleteDamage(Context context, Damage damage) {
		transactionFactory(context).deleteDamage(damage.damage_id);
	}

	public static void deleteDamage(Context context, Damage damage, String reason) {
		logDeleteMessage("deleting damage " + damage.damage_id, reason);
		deleteDamage(context, damage);
	}

	public static void deleteDamages(Context context, int delivery_vin_id) {
		transactionFactory(context).deleteDamages(delivery_vin_id);
	}

	public static void deleteImages(Context context, int delivery_vin_id) {
		transactionFactory(context).deleteImages(delivery_vin_id);
	}

    public static void insertInspection (Context context, Inspection inspection) {
		inspection.imageCount = inspection.images.size();
		inspection.damageCount = inspection.damages.size();
        transactionFactory(context).insertInspection(inspection);
        String guid = inspection.guid;

      	for(Image image : inspection.images) {
      		if (image.inspection_guid == null) {
				image.inspection_guid = guid;
			}

		  	if(image.filename == null || image.filename.length() == 0) {
			  	image.filename = inspection.vin + "-" + UUID.randomUUID().toString();
		  	}

        	insertImageToLocalDB(context, image);
      	}

      	for(Damage damage : inspection.damages) {
        	damage.inspection_guid = guid;
        	insertDamageToLocalDB(context, damage);
      	}
    }

    public static List<Inspection> getInspectionList(Context context, boolean getInspectionsForUpload) {
		Cursor cursor = transactionFactory(context).getInspectionsFromLocalDB(getInspectionsForUpload);

		ArrayList<Inspection> inspections = new ArrayList<Inspection>();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				Inspection tmpInspection = cursorToInspection(cursor);

				tmpInspection.damages = getInspectionDamageList(context, tmpInspection.guid);
				tmpInspection.images = getInspectionImageList(context, tmpInspection.guid);

				tmpInspection.lotCode = getLotCode(context, cursor.getInt(cursor.getColumnIndex("lot_code_id")));
				tmpInspection.scacCode = getScacCode(context, cursor.getInt(cursor.getColumnIndex("scac_code_id")));
				tmpInspection.terminal = getTerminal(context, cursor.getInt(cursor.getColumnIndex("terminal_id")));

				inspections.add(tmpInspection);
			}

			cursor.close();
		}

		return inspections;
	}
    
	private static Inspection cursorToInspection(Cursor cursor) {
		Inspection tmpInspection = new Inspection();
		tmpInspection.inspection_id = cursor.getInt(cursor.getColumnIndex("_id"));
		tmpInspection.inspector = cursor.getString(cursor.getColumnIndex("inspector"));
		tmpInspection.notes = cursor.getString(cursor.getColumnIndex("notes"));
		tmpInspection.type = cursor.getInt(cursor.getColumnIndex("type"));
		tmpInspection.imageCount = cursor.getInt(cursor.getColumnIndex("imageCount"));
		tmpInspection.damageCount = cursor.getInt(cursor.getColumnIndex("damageCount"));
		tmpInspection.uploadStatus = cursor.getInt(cursor.getColumnIndex("upload_status"));
		tmpInspection.vin = cursor.getString(cursor.getColumnIndex("vin"));
		tmpInspection.guid = cursor.getString(cursor.getColumnIndex("guid"));
		tmpInspection.latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
		tmpInspection.longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
		tmpInspection.timestamp = new Date(cursor.getLong(cursor.getColumnIndex("timestamp")));

		return tmpInspection;
	}

	private static ArrayList<Image> getInspectionImageList(Context context, String inspection_guid) {
	  Cursor cursor = transactionFactory(context).getInspectionImageListFromLocalDB(inspection_guid);

	  ArrayList<Image> images = new ArrayList<Image>();

	  if (cursor != null) {
		  while (cursor.moveToNext()) {
			  Image tmpImage = cursorToImage(context, cursor);

			  images.add(tmpImage);
		  }

		  cursor.close();
	  }

	  return images;
	}

	private static ArrayList<Damage> getInspectionDamageList(Context context, String inspection_guid) {
	  Cursor cursor = transactionFactory(context).getDamagesForInspectionFromLocalDB(inspection_guid);

	  ArrayList<Damage> damages = new ArrayList<Damage>();
	  if (cursor != null) {
		  while (cursor.moveToNext()) {
			  Damage tmpDamage = cursorToDamage(cursor);

			  tmpDamage.areaCode = getAreaCode(context, tmpDamage.area_code_id);
			  tmpDamage.typeCode = getTypeCode(context, tmpDamage.type_code_id);
			  tmpDamage.severityCode = getSeverityCode(context, tmpDamage.svrty_code_id);
			  tmpDamage.specialCode = getSpecialCode(context, tmpDamage.special_code_id);

			  damages.add(tmpDamage);
		  }
		  cursor.close();
	  }
	  return damages;
	}
  
	public static List<YardExit> getYardExitList(Context context, boolean getForUpload) {
	  Cursor cursor = transactionFactory(context).getYardExitsFromLocalDB(getForUpload);

	  ArrayList<YardExit> list = new ArrayList<YardExit>();

	  if (cursor != null) {
		  while (cursor.moveToNext()) {
			  YardExit tmpObject = cursorToYardExit(cursor);

			  tmpObject.scacCode = getScacCode(context, cursor.getInt(cursor.getColumnIndex("scac_code_id")));
			  tmpObject.terminal = getTerminal(context, cursor.getInt(cursor.getColumnIndex("terminal_id")));

			  list.add(tmpObject);
		  }
		  cursor.close();
	  }
	  return list;
	}
  
    private static boolean vinsMatch(String vin1, String vin2) {
		// TODO:  If length is 10 characters, vin is first two and last 8 digits.

		// for now, we're just doing a simple compare
		return vin1.equalsIgnoreCase(vin2);
	}
  
  public static List<YardInventory> getYardInventoryList(Context context, boolean getForUpload)  {
		Cursor cursor = transactionFactory(context).getYardInventoriesFromLocalDB(getForUpload);
	
		ArrayList<YardInventory> list = new ArrayList<YardInventory>();
	
		if (cursor != null) {
			while (cursor.moveToNext()) {
			  YardInventory tmpObject = cursorToYardInventory(cursor);

			  tmpObject.lotCode = getLotCode(context, cursor.getInt(cursor.getColumnIndex("lot_code_id")));
			  tmpObject.terminal = getTerminal(context, cursor.getInt(cursor.getColumnIndex("terminal_id")));

			  list.add(tmpObject);
			}

			cursor.close();
		}
		
		return list;
	}

	public static void insertYardInventory (Context context, YardInventory yardInventory) {
		transactionFactory(context).insertYardInventory(yardInventory);
	}

	public static boolean deleteYardInventory(Context context, int id) {
		return transactionFactory(context).deleteYardInventory(id);
	}

	public static YardInventory getYardInventoryForDeliveryVin(Context context, int delivery_vin_id) {
		Cursor cursor = transactionFactory(context).getYardInventoryFromLocalDBForDeliveryVin(delivery_vin_id);

		YardInventory yardInventory = null;

		if (cursor != null) {
			while (cursor.moveToNext()) {
				YardInventory tmpObject = cursorToYardInventory(cursor);
				yardInventory = tmpObject;

				if(cursor.getColumnIndex("lot_code_id") != -1) {
					yardInventory.lotCode = getLotCode(context, cursor.getInt(cursor.getColumnIndex("lot_code_id")));
				}
			}
		}

		cursor.close();

		return yardInventory;
	}

	private static YardInventory cursorToYardInventory(Cursor cursor) {
		YardInventory object = new YardInventory();
		object.yard_inventory_id= cursor.getInt(cursor.getColumnIndex("_id"));
		object.inspector = cursor.getString(cursor.getColumnIndex("inspector"));
		object.uploadStatus = cursor.getInt(cursor.getColumnIndex("upload_status"));
		object.VIN = cursor.getString(cursor.getColumnIndex("VIN"));
		object.lotLocate = cursor.getInt(cursor.getColumnIndex("lot_locate")) == 0 ? false : true;
		object.row = cursor.getString(cursor.getColumnIndex("row"));
		object.bay = cursor.getString(cursor.getColumnIndex("bay"));

		object.latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
		object.longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
		object.ldnbr = cursor.getString(cursor.getColumnIndex("ldnbr"));
		object.delivery_vin_id = cursor.getInt(cursor.getColumnIndex("delivery_vin_id"));

		return object;
	}

	public static LoadEvent getLoadEvent(Context context, int load_event_id)  {
		Cursor cursor = transactionFactory(context).getLoadEventFromLocalDb(load_event_id);

		LoadEvent tmpObject = null;

		if (cursor != null) {
			while (cursor.moveToNext()) {
				tmpObject = cursorToLoadEvent(cursor);
			}

			cursor.close();
		}

		return tmpObject;
	}

	public static List<LoadEvent> getLoadEventList(Context context, boolean getForUpload)  {
		Cursor cursor = transactionFactory(context).getLoadEventsFromLocalDB(getForUpload);

		ArrayList<LoadEvent> list = new ArrayList<LoadEvent>();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				LoadEvent tmpObject = cursorToLoadEvent(cursor);
				list.add(tmpObject);
			}

			cursor.close();
		}

		return list;
	}

	public static void insertLoadEvent (Context context, LoadEvent LoadEvent) {
		transactionFactory(context).insertLoadEvent(LoadEvent);
	}

	public static boolean deleteLoadEvent(Context context, int id) {
		return transactionFactory(context).deleteLoadEvent(id);
	}

	private static LoadEvent cursorToLoadEvent(Cursor cursor) {
		LoadEvent object = new LoadEvent();
		object.load_event_id= cursor.getInt(cursor.getColumnIndex("_id"));
		object.uploadStatus = cursor.getInt(cursor.getColumnIndex("upload_status"));
		object.csv = cursor.getString(cursor.getColumnIndex("csv"));

		return object;
	}

	public static Questionnaire getQuestionnaire(Context context, Questionnaire.Type type, boolean getForUpload)  {
		Cursor cursor = transactionFactory(context).getQuestionnaireFromLocalDB(type, getForUpload);

		if (cursor != null) {
			cursor.moveToFirst();
			Questionnaire questionnaire = cursorToQuestionnaire(cursor);

			cursor.close();
			return questionnaire;
		}

		return null;
	}

	private static Questionnaire cursorToQuestionnaire(Cursor cursor) {
		try {
			int id = cursor.getInt(cursor.getColumnIndex("_id"));
			String prompts = cursor.getString(cursor.getColumnIndex("prompts"));
			String type = cursor.getString(cursor.getColumnIndex("type"));
			int version = cursor.getInt(cursor.getColumnIndex("version"));
			return new Questionnaire(id, Questionnaire.Type.stringToType(type), version, prompts);
		} catch (Exception e) {
			System.out.println(e.getMessage());
		}

		return new Questionnaire(0, Questionnaire.Type.Unknown, 0, "");
	}

	public static List<PlantReturn> getPlantReturnList(Context context, boolean getForUpload)  {
		Cursor cursor = transactionFactory(context).getPlantReturnsFromLocalDB(getForUpload);

		ArrayList<PlantReturn> list = new ArrayList<PlantReturn>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
			  PlantReturn tmpObject = cursorToPlantReturn(cursor);

			  tmpObject.terminal = getTerminal(context, cursor.getInt(cursor.getColumnIndex("terminal_id")));

			  list.add(tmpObject);
			}

			cursor.close();
		}

		return list;
	}
  
  public static List<ReceivedVehicle> getReceivedVehicleList(Context context, boolean getForUpload)  {
    Cursor cursor = transactionFactory(context).getReceivedVehiclesFromLocalDB(getForUpload);

    ArrayList<ReceivedVehicle> list = new ArrayList<ReceivedVehicle>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
			  ReceivedVehicle tmpObject = cursorToReceivedVehicle(cursor);

			  tmpObject.terminal = getTerminal(context, cursor.getInt(cursor.getColumnIndex("terminal_id")));

			  list.add(tmpObject);
			}
        
			cursor.close();
		}
    
    	return list;
	}

  private static YardExit cursorToYardExit(Cursor cursor) {
    YardExit object = new YardExit();
    object.yard_exit_id= cursor.getInt(cursor.getColumnIndex("_id"));
    object.inspector = cursor.getString(cursor.getColumnIndex("inspector"));
    object.uploadStatus = cursor.getInt(cursor.getColumnIndex("upload_status"));
    object.VIN = cursor.getString(cursor.getColumnIndex("VIN"));
    object.inbound = cursor.getInt(cursor.getColumnIndex("inbound")) == 0 ? false : true;
    
    return object;
  }

  private static PlantReturn cursorToPlantReturn(Cursor cursor) {
    PlantReturn object = new PlantReturn();
    object.plant_return_id= cursor.getInt(cursor.getColumnIndex("_id"));
    object.inspector = cursor.getString(cursor.getColumnIndex("inspector"));
    object.uploadStatus = cursor.getInt(cursor.getColumnIndex("upload_status"));
    object.VIN = cursor.getString(cursor.getColumnIndex("VIN"));
    object.delayCode = cursor.getString(cursor.getColumnIndex("delay_code"));
    
    return object;
  }
  
  private static ReceivedVehicle cursorToReceivedVehicle(Cursor cursor) {
    ReceivedVehicle object = new ReceivedVehicle();
    object.received_vehicle_id= cursor.getInt(cursor.getColumnIndex("_id"));
    object.inspector = cursor.getString(cursor.getColumnIndex("inspector"));
    object.uploadStatus = cursor.getInt(cursor.getColumnIndex("upload_status"));
    object.VIN = cursor.getString(cursor.getColumnIndex("VIN"));

    return object;
  }      
  
  public static void insertYardExit (Context context, YardExit yardExit) {
	  transactionFactory(context).insertYardExit(yardExit);
	}

    public static void insertPlantReturn(Context context, PlantReturn plantReturn) {
      transactionFactory(context).insertPlantReturn(plantReturn);
    }

    public static void insertReceivedVehicle(Context context, ReceivedVehicle receivedVehicle) {
      transactionFactory(context).insertReceivedVehicle(receivedVehicle);
    }

    public static void insertLotCode(Context context, LotCode lotCode) {
      transactionFactory(context).insertLotCode(lotCode);
    }

    public static void insertScacCode(Context context, ScacCode scacCode) {
      transactionFactory(context).insertScacCode(scacCode);
    }

    public static void insertTerminal(Context context, Terminal terminal) {
      transactionFactory(context).insertTerminal(terminal);
    }

	public static void insertShuttleMove(Context context, ShuttleMove shuttleMove) {
		transactionFactory(context).insertShuttleMove(shuttleMove);
	}

    public static void insertShuttleMove(Context context, String shuttleMoveString) {
    	ShuttleMove shuttleMove = new ShuttleMove(shuttleMoveString);
    	transactionFactory(context).insertShuttleMove(shuttleMove);
    }

    public static List<ScacCode> getScacCodeList(Context context, int terminal_id) {
		List<ScacCode> scacCodeList = new ArrayList<ScacCode>();

		Cursor cursor = transactionFactory(context).getScacCodeList(terminal_id);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				scacCodeList.add(cursorToScacCode(cursor));
			}
			cursor.close();
		}

		return scacCodeList;
    }

    public static ScacCode getScacCode(Context context, int terminal_id, String description) {
		ScacCode scacCode= new ScacCode();

		Cursor cursor = transactionFactory(context).getScacCode(terminal_id, description);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				scacCode = cursorToScacCode(cursor);
			}
			cursor.close();
		}

		return scacCode;
    }        
    
    public static ScacCode getScacCode(Context context, int scac_code_id) {
		ScacCode scacCode= new ScacCode();

		Cursor cursor = transactionFactory(context).getScacCode(scac_code_id);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				scacCode = cursorToScacCode(cursor);
			}
			cursor.close();
		}

		return scacCode;
    }

	public static Date getDriverActionsLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getDriverActionsLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static Date getLotCodesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getLotCodesLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static Date getShuttleLotCodesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getShuttleLotCodesLastModified();
		Date maxModified = null;

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

    public static List<LotCode> getLotCodeListForTerminal(Context context, int terminal_id) {
		List<LotCode> lotCodeList = new ArrayList<LotCode>();

		Cursor cursor = transactionFactory(context).getLotCodeListForTerminal(terminal_id);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				lotCodeList.add(cursorToLotCode(cursor));
			}
			cursor.close();
		}

		return lotCodeList;
    }

	public static List<LotCode> getLotCodeListForShuttleMove(Context context, String terminal, String shuttleMoveCode) {
		List<LotCode> lotCodeList = new ArrayList<LotCode>();

		Cursor cursor = transactionFactory(context).getLotCodeListForShuttleMove(terminal, shuttleMoveCode);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				lotCodeList.add(cursorToLotCode(cursor));
			}
			cursor.close();
		}

		return lotCodeList;
	}

	public static LotCodeMessage getLotCodeMsg(Context context, String terminal, String code) {
		LotCodeMessage lotCodeMsg = null;

		Cursor cursor = transactionFactory(context).getLotCodeMsg(terminal, code);

		if (cursor != null) {
			while (cursor.moveToNext()) {
				lotCodeMsg = cursorToLotCodeMsg(cursor);
			}
			cursor.close();
		}

		return lotCodeMsg;
	}

	public static LotCode getLotCode(Context context, String code, int terminalId) {
		LotCode lotCode= new LotCode();

		Cursor cursor = transactionFactory(context).getLotCode(terminalId, code);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				lotCode = cursorToLotCode(cursor);
			}
			cursor.close();
		}

		return lotCode;
    }
    
    public static LotCode getLotCode(Context context, int lot_code_id) {
		LotCode lotCode= new LotCode();

		Cursor cursor = transactionFactory(context).getLotCode(lot_code_id);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				lotCode = cursorToLotCode(cursor);
			}
			cursor.close();
		}

		return lotCode;
    }    

    public static List<Terminal> getTerminalList(Context context) {
		List<Terminal> terminalList = new ArrayList<Terminal>();

		Cursor cursor = transactionFactory(context).getTerminalList();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				terminalList.add(cursorToTerminal(cursor));
			}
			cursor.close();
		}

		return terminalList;
    }

	public static List<Terminal> getShuttleTerminalList(Context context) {
		List<Integer> shuttleTerminalList = new ArrayList<Integer>();
		List<Terminal> terminalList = new ArrayList<Terminal>();

		Cursor cursor = transactionFactory(context).getShuttleTerminalList();
		if (cursor != null) {
			while (cursor.moveToNext()) {
				shuttleTerminalList.add(Integer.valueOf(cursor.getString(cursor.getColumnIndex("terminal"))));
			}
			cursor.close();
		}

		cursor = transactionFactory(context).getTerminalList();
		if (cursor != null) {
			while (cursor.moveToNext()) {
				Integer term = Integer.valueOf(cursor.getInt(cursor.getColumnIndex("terminal_id")));
				if (shuttleTerminalList.contains(term)) {
					terminalList.add(cursorToTerminal(cursor));
				}
			}
			cursor.close();
		}
		return terminalList;
	}
    
    public static Terminal getTerminal(Context context, int terminal_id) {
		Terminal terminal= new Terminal();

		Cursor cursor = transactionFactory(context).getTerminal(terminal_id);

		if (cursor != null) {
			while (cursor.moveToNext()) {
				terminal = cursorToTerminal(cursor);
			}
			cursor.close();
		}

		return terminal;
    }        

    public static LotCode cursorToLotCode(Cursor cursor) {
      LotCode lotCode = new LotCode();
      lotCode.code = cursor.getString(cursor.getColumnIndex("code"));
      lotCode.lot_code_id = cursor.getInt(cursor.getColumnIndex("lot_code_id"));
      lotCode.terminal_id = cursor.getInt(cursor.getColumnIndex("terminal_id"));
      lotCode.description = cursor.getString(cursor.getColumnIndex("description"));

      if(cursor.getColumnIndex("shuttleMoveCode") != -1) {
		  lotCode.shuttleMoveCode = cursor.getString(cursor.getColumnIndex("shuttleMoveCode"));
	  }
	  if(cursor.getColumnIndex("active") >= 0) {
      	  lotCode.active = cursor.getInt(cursor.getColumnIndex("active"));
      }

      return lotCode;
    }

    public static ScacCode cursorToScacCode(Cursor cursor) {
      ScacCode scacCode = new ScacCode();
      scacCode.setCode(cursor.getString(cursor.getColumnIndex("code")));
      scacCode.scac_code_id = cursor.getInt(cursor.getColumnIndex("scac_code_id"));
      scacCode.terminal_id = cursor.getInt(cursor.getColumnIndex("terminal_id"));
      scacCode.setDescription(cursor.getString(cursor.getColumnIndex("description")));
      if(cursor.getColumnIndex("active") >= 0) {
      	scacCode.active = getActiveStatus(cursor);
      }
      return scacCode;
    }

    public static Terminal cursorToTerminal(Cursor cursor) {
      Terminal terminal = new Terminal();
      terminal.terminal_id = cursor.getInt(cursor.getColumnIndex("terminal_id"));
      terminal.description = cursor.getString(cursor.getColumnIndex("description"));
      terminal.popupMessage = cursor.getString(cursor.getColumnIndex("popupMessage"));
      terminal.phoneNumber = cursor.getString(cursor.getColumnIndex("phoneNumber"));
      terminal.usToCanPhoneNumber = cursor.getString(cursor.getColumnIndex("usToCanPhoneNumber"));
      terminal.canToUsPhoneNumber = cursor.getString(cursor.getColumnIndex("canToUsPhoneNumber"));
      terminal.dispatchPhoneNumber = cursor.getString(cursor.getColumnIndex("dispatchPhoneNumber"));
      terminal.countryCode = cursor.getString(cursor.getColumnIndex("countryCode"));
      if(cursor.getColumnIndex("active") >= 0) {
      	terminal.active = getActiveStatus(cursor);
      }
      return terminal;
    }

    public static int getPreloadUploadQueueCount(Context context) {
        return 0;
    }

    public static int getDeliveryUploadQueueCount(Context context) {
        return 0;
    }

    public static int getYardInventoryUploadQueueCount(Context context) {
        Cursor cursor = transactionFactory(context).getYardInventoriesFromLocalDB(true, true, false);
        int count = 0;
        if (cursor != null) {
        	count = cursor.getCount();
        	cursor.close();
		}
        return count;
    }

    public static int getLotLocateUploadQueueCount(Context context) {
      	Cursor cursor = transactionFactory(context).getYardInventoriesFromLocalDB(true, true, true);
		int count = 0;
		if (cursor != null) {
			count = cursor.getCount();
			cursor.close();
		}
		return count;

    }

    public static int getYardExitUploadQueueCount(Context context) {
		Cursor cursor = transactionFactory(context).getYardExitsFromLocalDB(true);
		int count = 0;
		if (cursor != null) {
			count = cursor.getCount();
			cursor.close();
		}
		return count;
    }

    public static int getReturnToPlanUploadQueueCount(Context context) {
		Cursor cursor = transactionFactory(context).getPlantReturnsFromLocalDB(true);
		int count = 0;
		if (cursor != null) {
			count = cursor.getCount();
			cursor.close();
		}
		return count;
	}

    public static int getInspectVehicleUploadQueueCount(Context context) {
		Cursor cursor = transactionFactory(context).getInspectionsFromLocalDB(true);
		int count = 0;
		if (cursor != null) {
			cursor.moveToFirst();
			count = cursor.getCount();
			cursor.close();
		}
		return count;
    }

    public static int getReceivedVehicleUploadQueueCount(Context context) {
		Cursor cursor = transactionFactory(context).getReceivedVehiclesFromLocalDB(true);
		int count = 0;
		if (cursor != null) {
			count = cursor.getCount();
			cursor.close();
		}
		return count;
    }

	public static void deleteEmptyDeliveriesFromLoad(Context context, int load_id) {
		Load load = getLoad(context, load_id);
		
		for(Delivery delivery : load.deliveries ) {
			if(delivery.deliveryVins.size() == 0) {
				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
				editor.remove(delivery.delivery_remote_id + "_lookup_shown");
				editor.apply();
				//Duplicate since there is one in the function already
				//log.debug(Logs.DATAMANAGER, "Deleting empty delivery " + delivery.delivery_remote_id);
				deleteDeliveryDataFromDB(context, delivery.delivery_id, "delivery was empty after load change to " + load.loadNumber);
			}
		}
	}

	public static HashMap<String,Integer> getUploadQueueCount(Context context) {
		HashMap<String,Integer> counts = new HashMap<>();
		counts.put(Load.class.getName(), transactionFactory(context).getLoadUploadQueueCountFromLocalDB(false));
		counts.put(Load.class.getName() + "shuttle", transactionFactory(context).getLoadUploadQueueCountFromLocalDB(true));
		counts.put(Delivery.class.getName(), transactionFactory(context).getDeliveryUploadQueueCountFromLocalDB());
		counts.put(YardInventory.class.getName(), getYardInventoryUploadQueueCount(context));
		counts.put("LotLocate", getLotLocateUploadQueueCount(context));
		counts.put(YardExit.class.getName(), getYardExitUploadQueueCount(context));
		counts.put(PlantReturn.class.getName(), getReturnToPlanUploadQueueCount(context));
		counts.put(Inspection.class.getName(), getInspectVehicleUploadQueueCount(context));
		counts.put(ReceivedVehicle.class.getName(), getReceivedVehicleUploadQueueCount(context));

		return counts;
	}

	public static long upsertDriverActionToLocalDB(Context context, DriverAction driverAction) {
		return transactionFactory(context).upsertDriverActionToLocalDB(driverAction);
	}

	public static DriverAction getDriverAction(Context context, int id) {
		DriverAction driverAction = new DriverAction();

		Cursor cursor = transactionFactory(context).getDriverActionFromDb(id);

		if (cursor != null) {
			while (cursor.moveToNext()) {
				driverAction = cursorToDriverAction(cursor);
			}
			cursor.close();
		}

		return driverAction;
	}

	public static List<DriverAction> getDriverActions(Context context, String driver_number, boolean includeCompletedActions, String typeFilter) {
		return getDriverActions(context, driver_number, includeCompletedActions, typeFilter, true);
	}

	public static List<DriverAction> getDriverActions(Context context, String driver_number, boolean includeCompletedActions, String typeFilter, boolean includeUploadedActions) {
		ArrayList<DriverAction> driverActions = new ArrayList<DriverAction>();

		Cursor cursor = transactionFactory(context).getDriverActionsFromDb(driver_number, includeCompletedActions, typeFilter, includeUploadedActions);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				DriverAction tmpDriverAction = cursorToDriverAction(cursor);
				driverActions.add(tmpDriverAction);
			}
			cursor.close();
		}

		return driverActions;
	}

	private static DriverAction cursorToDriverAction(Cursor cursor) {
		DriverAction driverAction = new DriverAction();
		driverAction.setId(cursor.getInt(cursor.getColumnIndex("id")));
		driverAction.setDriver_id(cursor.getInt(cursor.getColumnIndex("driver_id")));
		driverAction.setAction(cursor.getString(cursor.getColumnIndex("action")));
		driverAction.setData(cursor.getString(cursor.getColumnIndex("data")));
		driverAction.setStatus(cursor.getString(cursor.getColumnIndex("status")));
		driverAction.setUploadStatus(cursor.getInt(cursor.getColumnIndex("upload_status")));
		driverAction.setReceived(cursor.getString(cursor.getColumnIndex("received")));
		driverAction.setSender_id(cursor.getString(cursor.getColumnIndex("sender_id")));
		driverAction.setProcessed(cursor.getString(cursor.getColumnIndex("processed")));
		driverAction.setCreated(cursor.getString(cursor.getColumnIndex("created")));
		return driverAction;
	}


	public static long insertDamageNoteTemplateToLocalDB(Context context, DamageNoteTemplate template) {
		return transactionFactory(context).insertDamageNoteTemplate(template);
	}

	public static long insertDamageNoteToLocalDB(Context context, DamageNote damageNote) {
		return transactionFactory(context).insertDamageNote(damageNote);
	}

	public static long insertLotCodeMsgToLocalDB(Context context, LotCodeMessage msg) {
		return transactionFactory(context).insertLotCodeMsg(msg);
	}

	private static DamageNoteTemplate cursorToDamageNoteTemplate(Cursor cursor) {
		DamageNoteTemplate template = new DamageNoteTemplate();
		template.id = cursor.getInt(cursor.getColumnIndex("id"));

		template.comment = cursor.getString(cursor.getColumnIndex("comment"));
		template.driver_prompt = cursor.getString(cursor.getColumnIndex("driver_prompt"));
		template.driver_prompt_type = cursor.getString(cursor.getColumnIndex("driver_prompt_type"));
		template.dealer_prompt = cursor.getString(cursor.getColumnIndex("dealer_prompt"));
		template.dealer_prompt_type = cursor.getString(cursor.getColumnIndex("dealer_prompt_type"));
		template.area_code = cursor.getString(cursor.getColumnIndex("area_code"));
		template.type_code = cursor.getString(cursor.getColumnIndex("type_code"));
		template.severity_code = cursor.getString(cursor.getColumnIndex("severity_code"));
		template.mfg = cursor.getString(cursor.getColumnIndex("mfg"));
		template.originTerminal = cursor.getString(cursor.getColumnIndex("originTerminal"));

		template.modified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
		template.active = cursor.getInt(cursor.getColumnIndex("active"));

		return template;
	}

	private static DamageNote cursorToDamageNote(Cursor cursor) {
		DamageNote note = new DamageNote();
		note.id = cursor.getInt(cursor.getColumnIndex("id"));
		note.damage_guid = cursor.getString(cursor.getColumnIndex("damage_guid"));
		note.preload_driver_comment = cursor.getString(cursor.getColumnIndex("preload_driver_comment"));
		note.delivery_driver_comment = cursor.getString(cursor.getColumnIndex("delivery_driver_comment"));
		note.delivery_dealer_comment = cursor.getString(cursor.getColumnIndex("delivery_dealer_comment"));
		note.damage_note_template_id = cursor.getInt(cursor.getColumnIndex("damage_note_template_id"));

		return note;
	}


	private static LotCodeMessage cursorToLotCodeMsg(Cursor cursor) {
		LotCodeMessage msg = new LotCodeMessage();
		msg.id = cursor.getInt(cursor.getColumnIndex("id"));
		msg.lot_code_id = cursor.getInt(cursor.getColumnIndex("lot_code_id"));
		msg.message = cursor.getString(cursor.getColumnIndex("message"));
		msg.prompt = cursor.getString(cursor.getColumnIndex("prompt"));
		msg.response = cursor.getString(cursor.getColumnIndex("response"));
		msg.modified = new Date(cursor.getInt(cursor.getColumnIndex("modified")));
		msg.active = getActiveStatus(cursor);

		return msg;
	}

	public static LotCodeMessage getLotCodeMsg(Context context, int lot_code_id) {
		LotCodeMessage msg = null;

		Cursor cursor = transactionFactory(context).getLotCodeMsgFromDb(lot_code_id);
		if (cursor != null && cursor.getCount() > 0) {
			cursor.moveToFirst();
			msg = cursorToLotCodeMsg(cursor);
			cursor.close();
		}

		return msg;
	}

	public static LotCodeMessage getGenericLotCodeMsg(Context ctx) {
		return getLotCodeMsg(ctx, 0);
	}

	public static Date getDamageNoteTemplatesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getDamageNoteTemplatesLastModified();
		Date maxModified = new Date(0);

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static ArrayList<DamageNote> getDamageNotes(Context context, Damage damage) {
		Cursor cursor = transactionFactory(context).getDamageNotesForDamage(damage.guid);

		ArrayList<DamageNote> damageNotes = new ArrayList<>();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				damageNotes.add(cursorToDamageNote(cursor));
			}
			cursor.close();
		}

		return damageNotes;
	}

	/**
	 * Get the list of active damage note templates that apply to this damage which have not yet been collected
	 * @param context
	 * @param isPreload
	 * @param isDealer set to false for driver templates, set to true for dealer templates
	 * @param damage the damage in question
	 * @param originTerminal the origin terminal of the LOAD
	 * @param mfg the manufacturer of the vin (dealer mfg for now...)
	 * @return List of applicable damage note templates that need to be filled out.
	 */
	public static ArrayList<DamageNoteTemplate> getRequiredDamageNoteTemplates(Context context, boolean isPreload, boolean isDealer, Damage damage, String originTerminal, String mfg) {
		ArrayList<DamageNoteTemplate> damageNoteTemplatesForDamage = DataManager.getDamageNoteTemplatesForDamage(context, damage, originTerminal, mfg);
		ArrayList<DamageNote> damageNotes = DataManager.getDamageNotes(context, damage);

		ArrayList<DamageNoteTemplate> removeTemplates = new ArrayList<>();

		//For each template
		for(DamageNoteTemplate damageNoteTemplate : damageNoteTemplatesForDamage) {

			boolean remove = false;

			//For each damage note, check to see if it matches the current template
			//and if the appropriate field has been filled in.  if it has, remove
			//it from the list of reported damage note templates to be completed
			for(DamageNote damageNote : damageNotes) {
				if(damageNote.damage_note_template_id == damageNoteTemplate.id) {
					if(isPreload) {
						if(damageNote.preload_driver_comment != null) {
							remove = true;
							break;
						}
					}
					else if(isDealer) {
						if(damageNote.delivery_dealer_comment != null) {
							remove = true;
							break;
						}
					} else {
						if(damageNote.delivery_driver_comment != null) {
							remove = true;
							break;
						}
					}
				}
			}

			if(remove) {
				removeTemplates.add(damageNoteTemplate);
			}
		}

		for(DamageNoteTemplate template : removeTemplates) {
			damageNoteTemplatesForDamage.remove(template);
		}

		return damageNoteTemplatesForDamage;
	}

	public static ArrayList<DamageNoteTemplate> getDamageNoteTemplatesForDamage(Context context, Damage damage, String originTerminal, String mfg) {
		Cursor cursor = transactionFactory(context).getDamageNoteTemplates();

		ArrayList<DamageNoteTemplate> damageNoteTemplates = new ArrayList<>();

		if (cursor != null) {
			while(cursor.moveToNext()) {
				DamageNoteTemplate template = cursorToDamageNoteTemplate(cursor);

				boolean addDamage = false;

				String areaCode = "0", typeCode = "0", severityCode = "0";

				if(damage.specialCode == null && damage.areaCode != null && damage.typeCode != null && damage.severityCode != null) {
					areaCode = damage.areaCode.getCode();
					typeCode = damage.typeCode.getCode();
					severityCode = damage.severityCode.getCode();
				} else {
					areaCode = damage.specialCode.getAreaCode();
					typeCode = damage.specialCode.getTypeCode();
					severityCode = damage.specialCode.getSeverityCode();
				}

				if (areaCode != null && (areaCode.equals(template.area_code) || template.area_code.equals("*"))) {
					addDamage = true;
				} else {
					continue;
				}

				if (typeCode != null && (typeCode.equals(template.type_code) || template.type_code.equals("*"))) {
					addDamage = true;
				} else {
					continue;
				}

				if (severityCode != null && (severityCode.equals(template.severity_code) || template.severity_code.equals("*"))) {
					addDamage = true;
				} else {
					continue;
				}

				if(originTerminal != null && (originTerminal.equals(template.originTerminal) || template.originTerminal.equals("*"))) {
					addDamage = true;
				} else {
					continue;
				}

				if(template.mfg.equals("*") || (mfg != null && mfg.equals(template.mfg))) {
					addDamage = true;
				} else {
					continue;
				}

				if(addDamage) {
					damageNoteTemplates.add(template);
				}
			}
			cursor.close();
		}
		return damageNoteTemplates;
	}

	public static Date getLotCodeMsgsLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getLotCodeMsgsLastModified();
		Date maxModified = new Date(0);

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static ArrayList<String> getPredefinedNotes(Context context, boolean damage, boolean signature) {
		ArrayList<String> notes = new ArrayList<>();
		Cursor cursor = transactionFactory(context).getPredefinedNotes();
		if (cursor != null) {
			while(cursor.moveToNext()){
				if (cursor.getInt(cursor.getColumnIndex("active")) == 1) {
					String mfg = cursor.getString(cursor.getColumnIndex("mfg"));
					if (HelperFuncs.isNullOrEmpty(mfg) || mfg.equals(PREDEF_NOTES_DEFAULT_LIST)) {
						if (damage && cursor.getInt(cursor.getColumnIndex("damageNote")) == 1) {
							notes.add(cursor.getString(cursor.getColumnIndex("note")));
						} else if (signature && cursor.getInt(cursor.getColumnIndex("signatureNote")) == 1) {
							notes.add(cursor.getString(cursor.getColumnIndex("note")));
						}
					}
				}
			}
			cursor.close();
		}

		return notes;
	}

	public static ArrayList<String> getPredefinedNotesByMfg(Context context, boolean damage, boolean signature, String mfg) {
		Set<String> notes = new HashSet<>(); // Use HashSet to prevent duplicate entries
		Cursor cursor = transactionFactory(context).getPredefinedNotes();
		if (HelperFuncs.isNullOrEmpty(mfg)) {
			mfg = PREDEF_NOTES_DEFAULT_LIST;
		}
		mfg = mfg.replaceAll(" ", "");
		List<String> mfgList = Arrays.asList(mfg.split(","));

		if (cursor != null) {
			while (cursor.moveToNext()) {
				String noteMfg = cursor.getString(cursor.getColumnIndex("mfg"));
				if (HelperFuncs.isNullOrEmpty(noteMfg)) {
					noteMfg = PREDEF_NOTES_DEFAULT_LIST;
				}
				noteMfg = noteMfg.replaceAll(" ", "");

				List<String> noteMfgList = new ArrayList<>(Arrays.asList(noteMfg.split(",")));
				noteMfgList.retainAll(mfgList);


				if (!noteMfgList.isEmpty()) {
					if (damage && cursor.getInt(cursor.getColumnIndex("damageNote")) == 1) {
						notes.add(cursor.getString(cursor.getColumnIndex("note")));
					} else if (signature && cursor.getInt(cursor.getColumnIndex("signatureNote")) == 1) {
						notes.add(cursor.getString(cursor.getColumnIndex("note")));
					}
				}
			}
			cursor.close();
		}

		return new ArrayList<String>(notes); // Convert HashSet to ArrayList
	}

	public static Date getPredefinedNotesLastModified(Context context) {
		Cursor cursor = transactionFactory(context).getPredefinedNotesLastModified();
		Date maxModified = new Date(0);

		if (cursor != null) {
			cursor.moveToNext();
			maxModified = new Date(cursor.getLong(cursor.getColumnIndex("modified")));
			cursor.close();
		}
		return maxModified;
	}

	public static ArrayList<DriverAction> getMessageList(Activity context, String driver_number) {
		ArrayList<DriverAction> driverActions = new ArrayList<DriverAction>();

		Cursor cursor = transactionFactory(context).getDriverActionsFromDb(driver_number, true, Constants.DRIVER_ACTION_DISPLAY_MESSAGE, true);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				DriverAction tmpDriverAction = cursorToDriverAction(cursor);
				driverActions.add(tmpDriverAction);
			}
			cursor.close();
		}

		return driverActions;
	}

	public static int getUnreadMessageCount(Activity context, List<DriverAction> messageList) {
		int counter = 0;

		for(DriverAction driverAction: messageList){
			if(driverAction.getProcessed() == null){
				counter++;
			}
		}
		return counter;
	}

	public static int getUnreadMessageCount(Activity context) {
		return getUnreadMessageCount(context, DataManager.getMessageList(context, CommonUtility.getDriverNumber(context)));
	}

	public static void markDriverActionUploaded(Context context, int id, boolean success) {
		transactionFactory(context).markDriverActionUploaded(id, success);
	}

	public static void saveDeliveryDealerComment(Context context, String id, String comment) {
		transactionFactory(context).saveDeliveryDealerComment(id, comment);
	}
	public static void saveDeliveryDealerContact(Context context, String id, String contact) {
		transactionFactory(context).saveDeliveryDealerContact(id, contact);
	}
	public static void saveDeliverySti(Context context, String id, int sti) {
		transactionFactory(context).saveDeliverySti(id, sti);
	}
	public static void saveDeliveryStiAndAfrhrs(Context context, String id, int sti, int afrhrs, String dealerContact) {
		transactionFactory(context).saveDeliveryStiAndAfrhs(id, sti, afrhrs, dealerContact);
	}
	public static void savePreloadDriverComment(Context context, String id, String comment) {
		transactionFactory(context).savePreloadDriverComment(id, comment);
	}
	public static void saveDeliveryDriverComment(Context context, String id, String comment) {
		transactionFactory(context).saveDeliveryDriverComment(id, comment);
	}
	public static void savePreloadSupervisorComment(Context context, int id, String comment) {
		transactionFactory(context).savePreloadSupervisorComment(id, comment);
	}
	public static void saveSafeDelivery(Context context, String id, String comment) {
		transactionFactory(context).saveSafeDelivery(id,comment);
	}

	public static void saveProblemReport(Context context, ProblemReport report) {
		transactionFactory(context).saveProblemReport(report);
	}

	public static ArrayList<ProblemReport> getProblemReports(Context context) {
		ArrayList<ProblemReport> problemReports = new ArrayList<ProblemReport>();

		Cursor cursor = transactionFactory(context).getProblemReportsFromDb();
		if (cursor != null) {
			while (cursor.moveToNext()) {
				ProblemReport tmpProblemReport = cursorToProblemReport(cursor);

				tmpProblemReport.images = getProblemReportImages(context, tmpProblemReport.guid);

				problemReports.add(tmpProblemReport);
			}
			cursor.close();
		}

		return problemReports;
	}

	public static ProblemReport getProblemReport(Context context, int id) {
		Cursor cursor = transactionFactory(context).getProblemReportFromDb(id);
		if (cursor != null) {
			cursor.moveToNext();
			ProblemReport tmpProblemReport = cursorToProblemReport(cursor);

			tmpProblemReport.images = getProblemReportImages(context, tmpProblemReport.guid);

			cursor.close();
			return tmpProblemReport;
		}

		return null;
	}

	public static ProblemReport getProblemReport(Context context, String guid) {
		Cursor cursor = transactionFactory(context).getProblemReportFromDb(guid);
		ProblemReport problemReport = null;
		if (cursor != null) {
			if (cursor.moveToNext()) {
				problemReport = cursorToProblemReport(cursor);

				problemReport.images = getProblemReportImages(context, problemReport.guid);
			}
			cursor.close();
		}

		return problemReport;
	}

	private static ProblemReport cursorToProblemReport(Cursor cursor) {
		ProblemReport msg = new ProblemReport();
		msg.id = cursor.getInt(cursor.getColumnIndex("id"));

		msg.guid = cursor.getString(cursor.getColumnIndex("guid"));
		msg.driver_id = cursor.getInt(cursor.getColumnIndex("driver_id"));
        msg.category = cursor.getString(cursor.getColumnIndex("category"));
		msg.description = cursor.getString(cursor.getColumnIndex("description"));
		msg.timestamp = cursor.getLong(cursor.getColumnIndex("timestamp"));
		msg.latitude = cursor.getDouble(cursor.getColumnIndex("latitude"));
		msg.longitude = cursor.getDouble(cursor.getColumnIndex("longitude"));
		msg.imageCount = cursor.getInt(cursor.getColumnIndex("imageCount"));
		msg.upload_status = cursor.getInt(cursor.getColumnIndex("upload_status"));

		return msg;
	}

	public static void markProblemReportUploaded(Context context, int id, boolean success) {
		transactionFactory(context).markProblemReportUploaded(id, success);
	}

    public static Image getNextProblemReportImage(Context context, String guid) {
        Cursor cursor = transactionFactory(context).getNextProblemReportImage(guid);
        Image image = null;
        if (cursor != null) {
			if (cursor.moveToFirst()) {
				image = cursorToImage(context, cursor);
				cursor.close();
			}
		}
        return image;
    }

	public static ArrayList<Image> getProblemReportImages(Context context, String guid) {
		ArrayList<Image> images = new ArrayList<Image>();

		Cursor cursor = transactionFactory(context).getProblemReportImagesFromDb(guid);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				Image tmpImage = cursorToImage(context, cursor);
				images.add(tmpImage);
			}
			cursor.close();
		}

		return images;
	}

	public static String getOriginTerminalForLoad(Context context, int load_id) {
		Cursor cursor = transactionFactory(context).getOriginTerminalForLoadId(load_id);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				if(cursor.getColumnIndex("originTerminal") != -1) {
					return cursor.getString(cursor.getColumnIndex("originTerminal"));
				}
			}
			cursor.close();
		}

		return null;
	}

	public static ArrayList<Image> getAllImagesWithLoadIds(Context context, boolean includeImageData) {
		ArrayList<Image> images = new ArrayList<Image>();

		Cursor cursor = transactionFactory(context).getImageListFromLocalDB("not load_id = -1", includeImageData);
		if (cursor != null) {
			while (cursor.moveToNext()) {
				Image tmpImage = cursorToImage(context, cursor);
				images.add(tmpImage);
			}
			cursor.close();
		}

		return images;
	}

	public static String getLoadNumberForLoadId(Context context, int load_id) {

		Load load = null;

		//Search for remote load
		Cursor cursor = transactionFactory(context).getLoadFromDBForRemote(String.valueOf(load_id));

		//System.out.println(DataManager.class.getName()+" Cursor Length :: "+cursor.getCount());
		if (cursor != null) {
			while (cursor.moveToNext()) {
				load = cursorToLoad(cursor);
				load.driver = getUser(context, String.valueOf(load.driver_id));
				break;
			}
			cursor.close();
		} else {
			cursor = transactionFactory(context).getLoadFromDB(load_id, -1, null);

			if (cursor != null) {
				while (cursor.moveToNext()) {
					load = cursorToLoad(cursor);
					load.driver = getUser(context, String.valueOf(load.driver_id));
					break;
				}
				cursor.close();
			}
		}

		if(load != null) {
			return load.loadNumber;
		} else {
			return null;
		}

	}

	public static int getRemoteLoadIdForLoadNumber(Context context, String ldnbr) {
		int load_id = -1;

		Load load = null;

		//Search for remote load
		Cursor cursor = transactionFactory(context).getLoadFromDBForLoadNumber(ldnbr);

		//System.out.println(DataManager.class.getName()+" Cursor Length :: "+cursor.getCount());
		if (cursor != null) {
			while (cursor.moveToNext()) {
				load = cursorToLoad(cursor);
				String remote_id = load.load_remote_id;
				if(remote_id != null) {
					load_id = Integer.valueOf(remote_id);
				}
				break;
			}
			cursor.close();
		}

		return load_id;
	}

	public static long insertPredefinedNoteToLocalDB(Context context, PredefinedNote predefinedNote) {
		return transactionFactory(context).insertPredefinedNote(predefinedNote);
	}


    public static Date dateStringToDate(String dateString) {
        if (dateString == null) {
            log.debug(Logs.DATAMANAGER, "dateStringToDate() got null date string.");
            return null;
        }
        try {
            Date date = (new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")).parse(dateString);
            return date;
        }
        catch (ParseException p) {
            log.debug(Logs.DATAMANAGER, "dateStringToDate() got invalid date string: " + dateString);
            return null;
        }
    }

    public static int supervisorCount(Context context) {
		return transactionFactory(context).getUserCount("supervisor");
	}

	public static int driverCount(Context context) {
		return transactionFactory(context).getUserCount("driver");
	}

	public static long insertWMRequestToLocalDB(Context context, WMRequest request) {
		return transactionFactory(context).insertWMRequestToLocalDB(request);
	}

	public static long insertWMRequestToLocalDB(Context context, int id, String uuid, String jsonData, int retries) {
		WMRequest request = new WMRequest();
		request.setId(id);
		request.setUuid(uuid);
		request.setJsonData(jsonData);
		request.setRetries(retries);
		return insertWMRequestToLocalDB(context, request);
	}

	public static long insertEmptyWMRequestToLocalDB(Context context) {
		return insertWMRequestToLocalDB(context, new WMRequest());
	}

	public static WMRequest getWMRequest(Context context, int id) {
		Cursor cursor = transactionFactory(context).getWMRequestFromLocalDB(id);

		WMRequest request= new WMRequest();

		if (cursor != null) {
			while (cursor.moveToNext()) {
				request = cursorToWMRequest(cursor);
			}

			cursor.close();
		}

		return request;
	}

	private static WMRequest cursorToWMRequest(Cursor cursor) {
		WMRequest request = new WMRequest();

		request.setId(cursor.getInt(cursor.getColumnIndex("id")));
		request.setUuid(cursor.getString(cursor.getColumnIndex("uuid")));
		request.setJsonData(cursor.getString(cursor.getColumnIndex("jsonData")));
		request.setRetries(cursor.getInt(cursor.getColumnIndex("retries")));
		return request;
	}

	public static void deleteWMRequest(Context context, int id) {
		log.debug(Logs.DEBUG, "deleting WMRequest " + id);
		transactionFactory(context).deleteWMRequest(id);
	}

	public static void deleteWMRequest(Context context, WMRequest request) {
		//Duplicate since there is one in the funcition being called down below.
		//log.debug(Logs.DEBUG, "deleting WMRequest " + request.getUuid());
		deleteWMRequest(context, request.getId());
	}
	public static void closeAndReopenDatabase(Context context) {
		transactionFactory(context).closeAndReopenDatabase();
	}
}
