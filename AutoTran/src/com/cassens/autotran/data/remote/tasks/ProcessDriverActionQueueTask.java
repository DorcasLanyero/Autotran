package com.cassens.autotran.data.remote.tasks;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import android.text.TextUtils;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.activities.ClearLoadActivity;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.EventBusManager;
import com.cassens.autotran.data.event.DriverActionEvent;
import com.cassens.autotran.data.event.NetworkEvent;
import com.cassens.autotran.data.model.DriverAction;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.sdgsystems.util.HelperFuncs;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.List;

public class ProcessDriverActionQueueTask extends AsyncTask<Void, Void, Void> {
	private static final Logger log = LoggerFactory.getLogger(ProcessDriverActionQueueTask.class.getSimpleName());
	public static final String COMPLETED = "completed";
	public static final String IN_PROCESS = "in process";
	public static final String IMAGE_NOT_FOUND = "missing photo";
	public static final String UPLOAD_ERROR = "upload error";
	public static final String NO_DATA = "no data";
	public static final String BAD_DATA = "invalid data";
	public static final String READY = "ready";
	public static final String BAD_LOAD = "bad load number";
	public static final String WRONG_DRIVER_FOR_LOAD = "wrong driver for load";
	public static final String BAD_COMMAND_FORMAT = "wrong driver for load";
	public static final String FAILED = "failed";

	Context context;
	boolean result;
	ProgressDialog dialog;

	public ProcessDriverActionQueueTask(Context context)
	{
		this.context = context;
	}

	public void setContext(Context context) {
		this.context = context;
	}

	@Override
	protected Void doInBackground(Void... params) {
		if(context != null) {
			String driverNumber = CommonUtility.getDriverNumber(context);


			if (driverNumber != null && driverNumber.length() > 0) {
				int driverNumInt;

				try {
					driverNumInt = Integer.parseInt(driverNumber);
				} catch (NumberFormatException ex) {
					//log.debug(Logs.DEBUG, "Invalid driver number: " + driverNumber + " not processing queue.");
					return null;
				}

				//Get the oldest driver action
				List<DriverAction> driverActionList = DataManager.getDriverActions(context, driverNumber, false, null, true);

				log.debug(Logs.DEBUG, "action list size: " + driverActionList.size());

				//if the oldest driver action exists and we're on the ctc network, process the action
				for (DriverAction oldestDriverAction : driverActionList) {
					//perform the driver action
					if (oldestDriverAction.getAction().equals(Constants.DRIVER_ACTION_DISPLAY_MESSAGE)) { //check if the driver action is a message
						continue;
					}

					if (oldestDriverAction.getProcessed() == null || oldestDriverAction.getStatus().equals(READY)) {

						log.debug(Logs.DEBUG, "Marking driver action " + oldestDriverAction.getId() + " " + oldestDriverAction.getAction() + " as in process");
						oldestDriverAction.setStatus(IN_PROCESS); //assign a status to the action
						DataManager.upsertDriverActionToLocalDB(context, oldestDriverAction); //upload the action to the database

						String actionStatus = processDriverAction(oldestDriverAction);

						if (!actionStatus.equals("")) {
							//for EACH of the pending driver actions
                            log.debug(Logs.DEBUG, "Driver action " + oldestDriverAction.getId() + " finished with status: " + actionStatus);
							markDuplicateActionsFinished(oldestDriverAction, driverActionList, actionStatus);
						}
						else {
						    log.debug(Logs.DEBUG, "Driver action " + oldestDriverAction.getId() + " awaiting background processing");
                        }
					} else {
						log.debug(Logs.DEBUG, "driver action was already completed");
					}

				}

                SyncManager.pushCompletedDriverActions(context, driverNumInt);
			}
		} else {
			log.error(Logs.DEBUG, "the context for the get driver actions task was null, not executing");
		}

		return null;
	}

	/**
	 * Process a driver action and mark it (as well as all duplicates) as complete
	 * @param currentDriverAction The driver action to be processed
	 */
	private @NonNull String processDriverAction(DriverAction currentDriverAction) {
		String actionStatus = "";

		switch (currentDriverAction.getAction()) {
            case Constants.DRIVER_ACTION_UPLOAD_IMAGE:
				actionStatus = handleUploadImage(currentDriverAction);
                break;
            case Constants.DRIVER_ACTION_UPLOAD_LOGS:
				actionStatus = handleUploadLogs(currentDriverAction);
                break;
//			case Constants.DRIVER_ACTION_DISPLAY_MESSAGE:
//				if (!DrivingLockActivity.isLockScreenDisplayed()) {
//					finishedAction = true;
//					log.debug(Logs.DEBUG, "Driver action to display message (really shouldn't ever happen...): " + currentDriverAction.getId());
//				}
//				break;
			case Constants.DRIVER_ACTION_UPDATE_SHUTTLE_LOAD_NUMBER:
				actionStatus = COMPLETED;

				if(currentDriverAction.getData() != null) {
					log.debug(Logs.INTERACTION, "Got a driver action to change a shuttle move number");

					String[] shuttleNumberInfo = currentDriverAction.getData().split(",");

					if(shuttleNumberInfo.length == 2) {
						String oldNumber = shuttleNumberInfo[0];
						String newNumber = shuttleNumberInfo[1];
						log.debug(Logs.INTERACTION, "Changing " + oldNumber + " to " + newNumber);
						int rows = DataManager.updateShuttleLoadNumber(context, oldNumber, newNumber);
						if(rows == 0) {
						    actionStatus = BAD_LOAD;
                        }
					} else {
						log.warn(Logs.INTERACTION, "Got a bad data string: " + currentDriverAction.getData());
						actionStatus = BAD_DATA;
					}
				} else {
					log.warn(Logs.INTERACTION, "Got a null data string for a driver action shuttle load number update");
                    actionStatus = NO_DATA;
				}

				DataManager.upsertDriverActionToLocalDB(context, currentDriverAction);

				break;
			case Constants.DRIVER_ACTION_SYNC_DRIVER:
				actionStatus = COMPLETED;

				SyncManager.syncCurrentDriver(context);

				break;
			case Constants.DRIVER_ACTION_CHANGE_PREFERENCE:
				actionStatus = COMPLETED;

				if(HelperFuncs.isNullOrEmpty(currentDriverAction.getData())) {
				    actionStatus = NO_DATA;
					break;
				}

				String[] pref = TextUtils.split(currentDriverAction.getData(), "=");
				if (pref.length < 2) {
				    actionStatus = BAD_DATA;
					break;
				}

				SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(context).edit();
				editor.putString(pref[0], pref[1]);
				editor.putBoolean(pref[0]+"-changed", true);
				editor.commit();
				break;
			case Constants.DRIVER_ACTION_HIDE_LOAD:
			case Constants.DRIVER_ACTION_UNHIDE_LOAD:
				// For backward compatibility, convert HIDE and UNHIDE commands
				// to a DRIVER_ACTION_LOAD_COMMAND
				String data = currentDriverAction.getData().trim();
				if (currentDriverAction.getAction().equalsIgnoreCase(Constants.DRIVER_ACTION_HIDE_LOAD)) {
					if (data.equalsIgnoreCase("FORCE_DAILY_CLEANUP")) {
						// Force daily cleanup to happen at the next opportunity
						HelperFuncs.setLongPref(context, "LAST_DAILY_TASK_TIME", 0);
						actionStatus = COMPLETED;
						break;
					}
					String cmdWords[] = data.split(" ");
					if (cmdWords.length < 2) {
						data = String.format("hide %s", data);
					}
					// if data already has two words, assume it contains a full
					// command (clear, delete, or hide).
				}
				else {
					data = String.format("unhide %s", data);
				}
				currentDriverAction.setAction(Constants.DRIVER_ACTION_LOAD_COMMAND);
				currentDriverAction.setData(data);
				// fall through (no break)
			case Constants.DRIVER_ACTION_LOAD_COMMAND:
				actionStatus = handleLoadCommand(currentDriverAction);
				String logMsg = String.format("Executed %s action for driver %d (cmd='%s'). Result: %s",
						currentDriverAction.getAction(), currentDriverAction.getDriver_id(),
						currentDriverAction.getData(), actionStatus);
				log.debug(Logs.DEBUG, logMsg);
				log.debug(Logs.TRANSACTIONS, logMsg);
				break;
            default:
                break;
        }

		return actionStatus;
	}

	private String handleLoadCommand(DriverAction driverAction) {
		String actionData = driverAction.getData().trim();
		String loadCmd = StringUtils.substringBefore(actionData, " ");
		String loadNumber = StringUtils.substringAfter(actionData, " ");
		String driverNumber = String.valueOf(driverAction.getDriver_id());

		Load load = DataManager.getLoadForLoadNumber(context, loadNumber);
		if (load == null) {
			return BAD_LOAD;
		}

		if (loadCmd.equalsIgnoreCase("HIDE")) {
			if (ClearLoadActivity.hideOrUnhideLoadAndChildren(context, load, driverNumber, true)) {
				return COMPLETED;
			}
		}
		else if (loadCmd.equalsIgnoreCase("UNHIDE")) {
			if (ClearLoadActivity.hideOrUnhideLoadAndChildren(context, load, driverNumber, false)) {
				return COMPLETED;
			}
		}
		else if (loadCmd.equalsIgnoreCase("CLEAR")) {
			ClearLoadActivity.clearLoadAndChildren(context, load);
			return COMPLETED;
		}
		else if (loadCmd.equalsIgnoreCase("DELETE")) {
			DataManager.deleteLoadAndChildren(context, load.load_id, "Load deleted via driver action");
			return COMPLETED;
		}
		return BAD_COMMAND_FORMAT;
	}

	private String handleUploadImage(DriverAction currentDriverAction) {
        log.debug(Logs.DEBUG, "Driver action to upload an image: " + currentDriverAction.getId() + " image id: " + currentDriverAction.getData());
        String actionStatus = "";

        //find the appropriate image and mark it for upload if we are on the CTC network
        if(CommonUtility.hasHoneywellScanner() || CommonUtility.connectedToCtcWifi()) {

            //move the indicated image from the file system into the database
            if(currentDriverAction.getData() != null) {
                try {
                    //int imageId = Integer.valueOf(currentDriverAction.getData());
                    //Image lowResImage = DataManager.getImage(context, imageId);

                    String imageFilename = currentDriverAction.getData();
                    Image image = DataManager.getImageForFilename(context, imageFilename);

                    if(image != null) {
                        Image hiresCopy = DataManager.getImageForFilename(context, imageFilename + "_hires");

                        if(hiresCopy != null && hiresCopy.s3_upload_status == Constants.SYNC_STATUS_UPLOADING) {
                            log.debug(Logs.DEBUG, "HIRES image " + imageFilename + "_hires was already uploading, skipping");
                            actionStatus = COMPLETED;
                        } else {

                            Image hiResCopy = CommonUtility.upsertHiResCopy(context, image, true);

                            String filename = hiResCopy.filename;
                            //Log.d("narf2", "hi id: " + hiResCopy.image_id);

                            log.debug(Logs.DEBUG, "checking for image with filename: " + filename);

                            BitmapFactory.Options bmOptions = new BitmapFactory.Options();
                            //bmOptions.inJustDecodeBounds = true;
                            String newImageFilePath = CommonUtility.cachedImageFileFullPath(context, filename);

                            log.debug(Logs.DEBUG, "full image path: " + newImageFilePath);

                            File file = new File(newImageFilePath);

                            if (file.exists()) {
                                //start the upload process.  This will catch ALL images pending upload, not just this
                                //hi res one...

                                DriverActionEvent event = new DriverActionEvent(DriverActionEvent.Type.UPLOAD_IMAGE, currentDriverAction.getId());

                                EventBusManager.getInstance().listenForEvents(EventBusManager.Queue.DRIVER_ACTIONS, (e) -> {
                                    if(e instanceof DriverActionEvent) {
                                        DriverActionEvent dae = (DriverActionEvent) e;
                                        if(dae.id == event.id) {
                                            if(dae.result == NetworkEvent.Result.SUCCESS) {
                                                markDriverActionFinished(currentDriverAction, COMPLETED);
                                            }
                                            else {
                                                markDriverActionFinished(currentDriverAction, UPLOAD_ERROR);
                                            }

                                            SyncManager.pushCompletedDriverActions(context, currentDriverAction.getDriver_id());
                                            return true;
                                        }
                                    }

                                    return false;
                                });
								CommonUtility.uploadLogMessage("Calling pushLocalDataToRemoteServer from ProcessDriverActionQueueTask()");
                                SyncManager.pushLocalDataToRemoteServer(context, CommonUtility.getDriverNumberAsInt(context), false, event);

                                log.debug(Logs.DEBUG, "Finished upload hires image call");
                            } else {
                                actionStatus = IMAGE_NOT_FOUND;

                                log.debug(Logs.DEBUG, "Specified hi res image not found: " + newImageFilePath);
                            }
                        }
                    } else {
                        actionStatus = IMAGE_NOT_FOUND;

                        log.debug(Logs.DEBUG, "Specified low res image not found: " + currentDriverAction.getData());
                    }


                } catch (NumberFormatException ex) {
                    actionStatus = IMAGE_NOT_FOUND;

                    log.debug(Logs.DEBUG, "Image id wasn't a valid integer : " + currentDriverAction.getData());
                }
            }
        }

        return actionStatus;
    }

    private String handleUploadLogs(DriverAction currentDriverAction) {
        log.debug(Logs.DEBUG, "Driver action to upload logs: " + currentDriverAction.getId());

        //start the log upload if we are on the ctc network

        if(CommonUtility.connectedToCtcWifi() || CommonUtility.hasHoneywellScanner()) {
            DataManager.upsertDriverActionToLocalDB(context, currentDriverAction);
            log.debug(Logs.DEBUG, "Sending logs as requested");

            final int id = currentDriverAction.getId();
            DriverActionEvent event = new DriverActionEvent(DriverActionEvent.Type.UPLOAD_LOGS, id);

            EventBusManager.getInstance().listenForEvents(EventBusManager.Queue.DRIVER_ACTIONS, (e) -> {
                if(e instanceof DriverActionEvent) {
                    String resultString = COMPLETED;
                    DriverActionEvent dae = (DriverActionEvent) e;

                    if(dae.result != NetworkEvent.Result.SUCCESS) {
                        resultString = UPLOAD_ERROR;
                    }

                    if(dae.id == id) {
                        markDriverActionFinished(currentDriverAction, resultString);
                        SyncManager.pushCompletedDriverActions(context, currentDriverAction.getDriver_id());
                    }
                }

                return false;
            });

            CommonUtility utility = new CommonUtility();
            utility.sendLogList(CommonUtility.getDriverNumber(context), context, "both", event);
        }

        return "";
    }

    private void markDuplicateActionsFinished(DriverAction oldestDriverAction, List<DriverAction> driverActionList, @NonNull String actionStatus) {
        for (DriverAction action : driverActionList) {
            //if the driver action has the same action / data of the one we just did, mark it with the given status.
            if (action.getAction().equals(oldestDriverAction.getAction()) && action.getData().equals(oldestDriverAction.getData())) {
                markDriverActionFinished(action, actionStatus);
            }
        }
    }

    private void markDriverActionFinished(DriverAction action, @NonNull String actionStatus) {
        action.setProcessed(Constants.dateFormatter().format(HelperFuncs.getTimestamp()));
        action.setUploadStatus(Constants.SYNC_STATUS_NOT_UPLOADED);
        action.setStatus(actionStatus);
        log.debug(Logs.DEBUG, "Marking driver action " + action.getId() + " as " + actionStatus);
        DataManager.upsertDriverActionToLocalDB(context, action);
    }

	@Override
	protected void onProgressUpdate(Void... values) {
		super.onProgressUpdate(values);
	}
	
	@Override
	protected void onPostExecute(Void result) {
		super.onPostExecute(result);
	}

}
