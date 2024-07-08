package com.cassens.autotran.activities;

import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.User;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Project : AUTOTRAN Description : VINListSelect class show list of items that will
 *           help create a VINList for inspections.
 * 
 * @author Hemant Creation Date : 12-11-2013
 */
public class VINListSelectActivity extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(VINListSelectActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	private SyncBroadcastReceiver mReceiver;
    
	protected ImageView backIcon;
	protected TextView titleTextView;
    protected ExpandableListView selectionListView;
    protected boolean infoIconEnabled;
    
	ProgressDialog dialog;
	private SelectionListAdapter adapter;
	protected static final String INSPECTION_COMPLETED="Completed";
	protected static final String INSPECTION_UNCOMPLETED="Not Completed";
	protected static final String categories[] = {
			INSPECTION_UNCOMPLETED,
			INSPECTION_COMPLETED
	};
	protected HashMap<String, ArrayList<SelectionListElement>> selectionList;

	private Bundle bundle;

	protected int driver_id;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_vin_list_select);
		backIcon = (ImageView) findViewById(R.id.backIcon);
		selectionListView = (ExpandableListView) findViewById(R.id.selectionListView);
		titleTextView = (TextView) findViewById(R.id.titleTextView);

		bundle = getIntent().getExtras();

		if(bundle.getString("user_id") != null) {
	    	driver_id = Integer.parseInt(bundle.getString("user_id"));
		} else {
			String driver_number = CommonUtility.getDriverNumber(this);

			User driver = DataManager.getUserForDriverNumber(getApplicationContext(), driver_number);
			if(driver != null) {
				driver_id = driver.user_id;
			}
		}

		adapter = null;

		selectionList = new HashMap<String, ArrayList<SelectionListElement>>();
		for (String category : categories) {
			selectionList.put(category, new ArrayList<SelectionListElement>());
		}

	    mReceiver = new SyncBroadcastReceiver();
	}
	
	protected void populateSelectionList(HashMap<String, ArrayList<SelectionListElement>> selectionList,
										 int driver_id)
	{
		// Override this method to fill in completedSelectionList.
	}
	
	private void populateAdapter(int driver_id)
	{
		for(HashMap.Entry<String, ArrayList<SelectionListElement>> selectionListElement : selectionList.entrySet()){
			selectionListElement.getValue().clear();
		}

	    populateSelectionList(selectionList, driver_id);
        adapter = new SelectionListAdapter(VINListSelectActivity.this);
        selectionListView.setAdapter(adapter);
		selectionListView.expandGroup(0);
	}

	@Override
	public void onResume() {
		super.onResume();

		populateAdapter(driver_id);

		registerReceiver(mReceiver, new IntentFilter(Constants.SYNC_STATUS_UPDATED_DATA));
	}
	
	@Override
	public void onPause() {
	  super.onPause();
	  unregisterReceiver(mReceiver);
	}
	
 	public void back(View v)
	{
 	   finish();
	}
 	
 	  
    public void backButton(View v)
    {
      finish();
    }
    
	

	// Inner class to capture and store response object
	protected class SelectionListElement
	{
	    String lookupKey;
	    long[] trainingRequirementIds;
		String primaryTextLine;
		String secondaryTextLine;
	    boolean enabled;
		boolean alert = false;
		boolean showInfoIcon = false;

	    // Subclass??
		static final int UNINSPECTED_VINS_REMAINING = 1;
		static final int AWAITING_SUBMISSION = 2;
        static final int AWAITING_DEALER_SIGNATURE = 3;
        static final int AWAITING_DRIVER_SIGNATURE = 4;
        static final int DAMAGE_COMPLETE = 5;
		int state; // true if inspected, but does not mean signatures have been collected

		SelectionListElement()
		{
		  enabled = true;
		  state = UNINSPECTED_VINS_REMAINING;
		}
	}
/*
	// Inner adapter class facilitating list view with the objects from which
	// to extract a vin list.
	class oldSelectionListAdapter extends ArrayAdapter<SelectionListElement>
	{
		private Context context;
		private int layoutResourceId;
		private List<SelectionListElement> selectionListElements;

		public SelectionListAdapter(Context context, int textViewResourceId, List<SelectionListElement> objects)
		{
			super(context, textViewResourceId, objects);
			this.context = context;
			this.layoutResourceId = textViewResourceId;
			this.selectionListElements = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View row = convertView;
			TextView primaryTextView, secondaryTextView, separatorLine, infoIcon;
			LinearLayout itemLayout;

			final SelectionListElement selectionListElement = this.selectionListElements.get(position);

			if (row == null)
			{
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);
			}

			row.setPadding(0, 40, 0, 0);
			;
			
			primaryTextView = (TextView) row.findViewById(R.id.primaryTextView);
			secondaryTextView = (TextView) row.findViewById(R.id.secondaryTextView);
			separatorLine = (TextView) row.findViewById(R.id.separatorLine);
			infoIcon = (TextView) row.findViewById(R.id.infoIcon);



			if (infoIconEnabled) {
                infoIcon.setOnClickListener(new OnClickListener()
                {
                    @Override
                    public void onClick(View v)
                    {
                        onInfoIconClick(Integer.parseInt(selectionListElement.lookupKey));
                    }
    
                });
			}
			else {
			    infoIcon.setVisibility(View.GONE);
			}
			itemLayout = (LinearLayout) row.findViewById(R.id.itemLayout);
			
			separatorLine.setVisibility(View.GONE);
			
			if (selectionListElement.primaryTextLine == null) {
			    primaryTextView.setVisibility(View.GONE);
			}
			else {
                primaryTextView.setVisibility(View.VISIBLE);
			    primaryTextView.setText(selectionListElement.primaryTextLine);
			}            
            if (selectionListElement.secondaryTextLine == null) {
                secondaryTextView.setVisibility(View.GONE);
            }
            else {
                secondaryTextView.setVisibility(View.VISIBLE);
                secondaryTextView.setText(selectionListElement.secondaryTextLine);
            }
		    row.setBackgroundColor(Color.WHITE);
		    
			row.setTag(selectionListElement.lookupKey);

			itemLayout.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
				    onItemSelected(selectionListElement);
				}
			});
			
			
			if(!selectionListElement.enabled) {
				log.debug(Logs.DEBUG, "setting selection list item disabled");
			} else {
				log.debug(Logs.DEBUG, "setting selection list item enabled");	
			}
			
			itemLayout.setEnabled(selectionListElement.enabled);
			return row;
		}

		@Override
		public int getCount()
		{
			return this.selectionListElements.size();
		}
	}
*/


	// Inner adapter class facilitating list view with the objects from which
	// to extract a vin list.
	class SelectionListAdapter extends BaseExpandableListAdapter
	{
		private Context context;

		public SelectionListAdapter(Context context)
		{
			super();
			this.context = context;
		}

		@Override
		public Object getChild(int groupPosition, int childPosition) {
			if (selectionList.get(categories[groupPosition]).size() == 0) {
				return null;
			}
			return selectionList.get(categories[groupPosition]).get(childPosition);
		}

		@Override
		public long getChildId(int groupPosition, int childPosition) {
			return childPosition;
		}

		@Override
		public View getChildView(int groupPosition, final int childPosition,
								 boolean isLastChild, View convertView, ViewGroup parent) {

			final SelectionListElement selectionListElement = (SelectionListElement) getChild(groupPosition, childPosition);
			TextView primaryTextView, secondaryTextView, separatorLine, infoIcon;
			ImageView backgroundIcon;
			LinearLayout itemLayout;

			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.generic_list_element, parent, false);
			}
			convertView.setPadding(0, 20, 0, 20);

			primaryTextView = (TextView) convertView.findViewById(R.id.primaryTextView);
			secondaryTextView = (TextView) convertView.findViewById(R.id.secondaryTextView);
			separatorLine = (TextView) convertView.findViewById(R.id.separatorLine);
			infoIcon = (TextView) convertView.findViewById(R.id.infoIcon);
			backgroundIcon = convertView.findViewById(R.id.backgroundIcon);

			if (infoIconEnabled && selectionListElement.showInfoIcon) {
				infoIcon.setOnClickListener(new OnClickListener()
				{
					@Override
					public void onClick(View v)
					{
						onInfoIconClick(Integer.parseInt(selectionListElement.lookupKey));
					}

				});

				// used to notify user of recent updates made to the dealer
				if (selectionListElement.alert) {
					infoIcon.setBackgroundColor(context.getResources().getColor(R.color.InformationBlue));
					infoIcon.setTextColor(context.getResources().getColor(R.color.White));
					infoIcon.setTypeface(Typeface.DEFAULT_BOLD);
					infoIcon.setText(R.string.dealerInfoIconNewInfo);
				}
			}
			else {
				infoIcon.setVisibility(View.GONE);
			}

			if (selectionListElement.trainingRequirementIds != null && selectionListElement.trainingRequirementIds.length > 0) {

			    List<TrainingRequirement> reqs = DataManager.getTrainingRequirements(context, selectionListElement.trainingRequirementIds);
			    TrainingRequirement.ByStatus filteredReqs = TrainingRequirement.filterList(reqs);

			    if(filteredReqs.unfinished.size() > 0) {
			        backgroundIcon.setImageDrawable(getResources().getDrawable(R.drawable.coaching_bg_icon));
                    backgroundIcon.setVisibility(View.VISIBLE);

                }
			    else if(filteredReqs.finished.size() > 0) {
			        backgroundIcon.setImageDrawable(getResources().getDrawable(R.drawable.coaching_complete_bg_icon));
                    backgroundIcon.setVisibility(View.VISIBLE);

                }
			    else {
			        backgroundIcon.setVisibility(View.GONE);
                }
            }

			itemLayout = (LinearLayout) convertView.findViewById(R.id.itemLayout);

			separatorLine.setVisibility(View.GONE);

			if (selectionListElement.primaryTextLine == null) {
				primaryTextView.setVisibility(View.GONE);
			}
			else {
				primaryTextView.setVisibility(View.VISIBLE);
				primaryTextView.setText(selectionListElement.primaryTextLine);
			}
			if (selectionListElement.secondaryTextLine == null) {
				secondaryTextView.setVisibility(View.GONE);
			}
			else {
				secondaryTextView.setVisibility(View.VISIBLE);
				secondaryTextView.setText(selectionListElement.secondaryTextLine);
			}
			convertView.setBackgroundColor(Color.WHITE);

			convertView.setTag(selectionListElement.lookupKey);

			itemLayout.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					onItemSelected(selectionListElement);
				}
			});


			if(!selectionListElement.enabled) {
				log.debug(Logs.DEBUG, "setting selection list item disabled");
			} else {
				log.debug(Logs.DEBUG, "setting selection list item enabled");
			}

			itemLayout.setEnabled(selectionListElement.enabled);
			return convertView;
		}

		@Override
		public int getChildrenCount(int groupPosition) {
			return selectionList.get(categories[groupPosition]).size();
		}

		@Override
		public Object getGroup(int groupPosition) {
			return categories[groupPosition];
		}

		@Override
		public int getGroupCount() {
			return categories.length;
		}

		@Override
		public long getGroupId(int groupPosition) {
			return groupPosition;
		}

		@Override
		public View getGroupView(int groupPosition, boolean isExpanded,
								 View convertView, ViewGroup parent) {
			String headerTitle = (String) getGroup(groupPosition);
			if (convertView == null) {
				LayoutInflater infalInflater = (LayoutInflater) this.context
						.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
				convertView = infalInflater.inflate(R.layout.expandable_code_list_group, parent, false);
			}

			TextView lblListHeader = (TextView) convertView.findViewById(R.id.expandableCodeListHeader);
			//lblListHeader.setTypeface(null, Typeface.BOLD);
			lblListHeader.setText(headerTitle);

			TextView groupExpandIcon = (TextView) convertView.findViewById(R.id.groupExpandIcon);
			groupExpandIcon.setVisibility(getChildrenCount(groupPosition) == 0 ? View.INVISIBLE : View.VISIBLE);
			groupExpandIcon.setText(isExpanded ? "-" : "+");

			return convertView;
		}

		@Override
		public boolean hasStableIds() {
			return false;
		}

		@Override
		public boolean isChildSelectable(int groupPosition, int childPosition) {
			return true;
		}
	}

    protected void onInfoIconClick(int lookupKey)
    {
        // Override this method to display information about selected item
    }

	protected void onItemSelected(SelectionListElement curSelectionListElement)
	{
	    // Override this method to take action when an item is selected.
    }

	private class SyncBroadcastReceiver extends BroadcastReceiver {

	  @Override
	  public void onReceive(Context context, Intent intent) {

		//only update the list if there was a significant change
		boolean updateLoadList = intent.getBooleanExtra("uploadLoadList", false);
		
		if(updateLoadList) {
			log.debug(Logs.DEBUG, "updating list since uploadLoadList was true");
			populateAdapter(driver_id);
			adapter.notifyDataSetChanged();
		} else {
			log.debug(Logs.DEBUG, "not updating list since uploadLoadList was false");
		}
	  }
	}
}

