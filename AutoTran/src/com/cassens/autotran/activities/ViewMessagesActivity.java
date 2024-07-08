package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.DriverAction;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.data.remote.tasks.ProcessDriverActionQueueTask;
import com.sdgsystems.util.DetectedActivitiesIntentService;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Project : AUTOTRAN Description : VIEW MESSAGES THAT HAVE BEEN SENT TO THE USER
 * 
 * @author Hemant Creation Date : 12-11-2013
 */
public class ViewMessagesActivity extends AutoTranActivity{
    private static final Logger log = LoggerFactory.getLogger(ViewMessagesActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	SimpleTimeStamp localDateTIme = new SimpleTimeStamp();

	ListView list;
	TextView title;
	CodeEntryAdapter adapter;
	ArrayList<DriverMessageEntry> driverMessageEntryList = new ArrayList<>();

	@Override
	protected void onCreate(Bundle savedInstanceState)	{
		super.onCreate(savedInstanceState);
		log.debug(Logs.INTERACTION, "Entering view message activity");
		setContentView(R.layout.code_list);
		list = findViewById(R.id.list);
        title = findViewById(R.id.title);
		title.setText("Driver Messages");

		findViewById(R.id.prompt).setVisibility(View.GONE);
	}

	public void menuList(View v) {

	}

	public void back(View v) {
		log.debug(Logs.INTERACTION, "ViewMessagesActivity: Back Button Pressed");
	    Intent i = new Intent();
        setResult(RESULT_CANCELED, i);
		finish();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}


	@Override
	protected void onResume() {
		super.onResume();

		List<DriverAction> messageList = DataManager.getMessageList(this, CommonUtility.getDriverNumber(this));

		driverMessageEntryList.clear();
		if (messageList != null) {

			log.debug(Logs.DEBUG, "Getting lot list: " + messageList.size());
			for (DriverAction driverAction: messageList) {
				DriverMessageEntry driverMessageEntry = new DriverMessageEntry();
				driverMessageEntry.sender = driverAction.getSender_id();
				driverMessageEntry.message = driverAction.getData();
				driverMessageEntry.timeReceived = SimpleTimeStamp.convertUTCTimestampToLocal(driverAction.getReceived());
				driverMessageEntry.timeSent = SimpleTimeStamp.convertUTCTimestampToLocal(driverAction.getCreated());
				if (!HelperFuncs.isNullOrEmpty(driverAction.getProcessed())) {
					driverMessageEntry.timeRead = SimpleTimeStamp.convertUTCTimestampToLocal(driverAction.getProcessed());
				}

				driverMessageEntryList.add(driverMessageEntry);
			}
			adapter = new CodeEntryAdapter(ViewMessagesActivity.this, R.layout.driver_message_list, driverMessageEntryList);
			list.setAdapter(adapter);
		}
		showPendingMessages(this, messageList, CommonUtility.getDriverNumber(this), false);
	}

	// Inner Bean class to capture and store response object
	class DriverMessageEntry {
	    int id;
	    String sender;
		String timeReceived;
		String timeSent;
		String timeRead;
		String message;

		public String getFullMessage() {
			String message = "";
			message += this.message + "\n\n";
			return message.trim();
		}


		public String getListHeader() {
			if (CommonUtility.isNullOrBlank(sender)) {
				return "";
			}
			else {
				return "From: " + this.sender;
			}
		}

		public String getListFooter() {
			if (CommonUtility.isNullOrBlank(timeRead)) {
				return "";
			}
			else {
				return "Read: " + this.timeRead;
			}
		}

		public String getTimesLogged(){
			return "Sent: " + this.timeSent + "\n" + "Rcvd: " + this.timeReceived + "\n" + "Read: " + this.timeRead;
		}
	}

	// Inner adapter class facilitating list view with the objects downloaded from web server
	class CodeEntryAdapter extends ArrayAdapter<DriverMessageEntry> {
		private Context context;
		private int layoutResourceId;
		private List<DriverMessageEntry> driverMessageEntryList;

		public CodeEntryAdapter(Context context, int textViewResourceId, List<DriverMessageEntry> objects) {
			super(context, textViewResourceId, objects);
			this.context = context;
			this.layoutResourceId = textViewResourceId;
			this.driverMessageEntryList = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent) {
			View row = convertView;
			TextView timeStamp, header, message, footer;
			LinearLayout main;
			int lines = 0;

			final DriverMessageEntry driverMessageEntry = this.driverMessageEntryList.get(position);

			if (row == null)
			{
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);
			}
            
			// Make the irrelevant fields invisible.
            row.findViewById(R.id.arrow_image).setVisibility(View.GONE);

			main = row.findViewById(R.id.msgEntry);
			timeStamp = row.findViewById(R.id.msgTimeStamp);
			header = row.findViewById(R.id.msgHeader);
			message = row.findViewById(R.id.msgText);
			footer = row.findViewById(R.id.msgFooter);

			if (!CommonUtility.isNullOrBlank(driverMessageEntry.timeRead)) {
				timeStamp.setText(driverMessageEntry.timeRead) ;
			} else {
				timeStamp.setText("");
			}

			if (driverMessageEntry.getListHeader().isEmpty()) {
				header.setVisibility(View.GONE);
			}
			else {
				header.setText(driverMessageEntry.getListHeader());
			}

			message.setText(driverMessageEntry.message);

			if (driverMessageEntry.getListFooter().isEmpty()) {
				footer.setVisibility(View.GONE);
			}
			else {
				footer.setVisibility(View.VISIBLE);
				footer.setText(driverMessageEntry.getListFooter());
			}

			main.setBackgroundColor(Color.WHITE);
			row.setTag(driverMessageEntry.id);

			row.setOnClickListener(v -> {
				DriverMessageEntry entry = this.driverMessageEntryList.get(position);
				//CommonUtility.simpleMessageDialog(CommonUtility.getCurrentActivity(), entry.getFullMessage());
				showFullMessage(entry);
			});

			return row;
		}

		@Override
		public int getCount()
		{
			return this.driverMessageEntryList.size();
		}
	}


	/**
	 * Go through the message actions and aggregate them into a single message popup
	 */
	public static void showPendingMessages(Context context, final List<DriverAction> msgActions, String driverNumber, boolean allowReadLater) {
		List<Integer> msgIds = new ArrayList<>();
		ArrayList<String> messages = new ArrayList<>();
		Boolean oldMessagesPresent;

		for (DriverAction action : msgActions) {
			String message = "";

			if (!HelperFuncs.isNullOrEmpty(action.getSender_id())) {
				message += "From: " + action.getSender_id() + "\n";
			}
			message += action.getData();

			if (action.getStatus().equals("ready")) {
				SimpleTimeStamp time = new SimpleTimeStamp();
				action.setReceived(time.getUtcDateTime());
				msgIds.add(new Integer(action.getId()));
				action.setStatus(ProcessDriverActionQueueTask.COMPLETED);
				action.setUploadStatus(Constants.SYNC_STATUS_NOT_UPLOADED);
				DataManager.upsertDriverActionToLocalDB(context, action);
				messages.add(message);
			} else if (action.getStatus().equals("completed")) {
				if (action.getProcessed() == null) {
					msgIds.add(new Integer(action.getId()));
					messages.add(message);
				}
			}
		}

		// Don't display the messages if app is displaying a dealer-facing screen or is backgrounded
		Activity currentActivity = CommonUtility.getCurrentActivity();
		if ((currentActivity instanceof SignatureActivity && ((SignatureActivity)currentActivity).isDealerMode())
				|| (currentActivity instanceof NotesActivity && ((NotesActivity)currentActivity).isDealerMode())
				|| (currentActivity instanceof NotesListActivity && ((NotesListActivity)currentActivity).isDealerMode())) {
			//log.debug(Logs.DEBUG, "Pending driver messages not displayed because dealer-facing screen is currently displayed.");
			return;
		}
		if ((currentActivity instanceof DriverMessageDialogActivity) || DetectedActivitiesIntentService.isInDrivingState() || !AutoTranApplication.inForeground()) {
			//log.debug(Logs.DEBUG, "Pending driver messages not displayed because app is backgrounded or driving is detected.");
			return;
		}

		oldMessagesPresent = checkForOldMessages(context, msgIds);

		if (messages.size() > 0) {
			Intent intent = new Intent(context, DriverMessageDialogActivity.class);
			intent.putStringArrayListExtra(Constants.DIALOG_ACTIVITY_MESSAGE, messages);
			if (messages.size() > 1) {
				intent.putExtra(Constants.DIALOG_ACTIVITY_TITLE, messages.size() + " New Messages");
			}
			else {
				intent.putExtra(Constants.DIALOG_ACTIVITY_TITLE, "New Message");
			}
			intent.putExtra(Constants.DIALOG_ACTIVITY_SHOW_CANCEL, allowReadLater);
			intent.putIntegerArrayListExtra(Constants.DIALOG_ACTIVITY_DRIVER_ACTION_IDS, (ArrayList<Integer>) msgIds);
			intent.putExtra(Constants.DIALOG_ACTIVITY_DRIVER_OLD_MESSAGES, oldMessagesPresent);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);

			SyncManager.pushCompletedDriverActions(context, Integer.parseInt(driverNumber));
		}
	}

	public static void processPendingMessages(Context context, String driverNumber, boolean allowReadLater) {
		final List<DriverAction> msgActions = DataManager.getMessageList(CommonUtility.getCurrentActivity(), CommonUtility.getDriverNumber(context));
		showPendingMessages(context, msgActions, driverNumber, allowReadLater);
	}

	private void showFullMessage(DriverMessageEntry entry){
		LayoutInflater inflater = this.getLayoutInflater();
		final View dialogView = inflater.inflate(R.layout.dialog_message, null);

		// Use fullscreen translucent theme to force the dialog to full screen width
		// (see https://stackoverflow.com/questions/6329360/how-to-set-dialog-to-show-in-full-screen)
		AlertDialog.Builder builder = new AlertDialog.Builder(this, android.R.style.Theme_Translucent_NoTitleBar_Fullscreen);
		builder.setView(dialogView)
				.setCancelable(false);

		final Button okButton = dialogView.findViewById(R.id.ok_button);
		final TextView msgText = dialogView.findViewById(R.id.msg_text);
		final TextView timesLogged = dialogView.findViewById(R.id.times_logged);

		msgText.setText(entry.getFullMessage());
		timesLogged.setText(entry.getTimesLogged());

		msgText.setBackgroundColor(Color.LTGRAY);
		msgText.setTextColor(Color.BLACK);

		builder.setTitle(entry.getListHeader());
		final Dialog dialog;
		dialog = builder.create();

		okButton.setOnClickListener(v -> {
			dialog.dismiss();
		});

		// Set the layout and gravity to force the visible dialog to be centered vertically
		Window window = dialog.getWindow();
		window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
		window.setGravity(Gravity.CENTER);

		dialog.show();
	}

	private static boolean checkForOldMessages(Context context, List<Integer> msgIds) {
		if (msgIds.size() > 0) {
			DriverAction action;
			Long messageAge;
			Date timeCreated = null,currentTime = null;
			SimpleTimeStamp time = new SimpleTimeStamp();

			for (Integer msgId : msgIds) {
				action = DataManager.getDriverAction(context, msgId);

				if (action.getReceived() != null) {
					try {
						timeCreated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(action.getCreated());
						currentTime = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(time.getUtcDateTime());
					} catch (ParseException e) {
						e.printStackTrace();
					}

					messageAge = TimeUnit.HOURS.convert(Math.abs(currentTime.getTime() - timeCreated.getTime()), TimeUnit.MILLISECONDS);

					if (messageAge > 24) {
						return true;
					}
				}
			}
		}
		return false;
	}
}
