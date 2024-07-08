package com.cassens.autotran.activities;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.lookup.ShuttleMove;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ShuttleOriginListActivity extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(ShuttleOriginListActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	ListView list;
	TextView title;
    TextView prompt;
	OriginEntryAdapter adapter;
	ArrayList<String> originEntryList = new ArrayList<String>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.code_list);
		list = (ListView) findViewById(R.id.list);
        title = (TextView) findViewById(R.id.title);
        prompt = (TextView) findViewById(R.id.prompt);
        
		ArrayList<ShuttleMove> originList = (ArrayList<ShuttleMove>) DataManager.getShuttleMoves(this, this.getIntent().getExtras().getString("terminal"), null, null);
        
		prompt.setText("Select Shuttle Load Origin");
		
        if (originList != null) {
            
            log.debug(Logs.DEBUG, "Getting terminal list: " + originList.size());
            for (ShuttleMove shuttleMove: originList) {
                    log.debug(Logs.DEBUG, "shuttleMove=" + shuttleMove.orgDestString);
                    
                    String originName = shuttleMove.getOriginName();
                    if(!originEntryList.contains(originName))
                    	originEntryList.add(originName);
            }
            adapter = new OriginEntryAdapter(ShuttleOriginListActivity.this, R.layout.horizontal_list_with_color_text, originEntryList);
            list.setAdapter(adapter);
        }
	}

	public void menuList(View v) {
		// Never called because menuList is never visible in this activity
	}

	public void back(View v)
	{
		CommonUtility.logButtonClick(log, v);
	    Intent i = new Intent();
        setResult(RESULT_CANCELED, i);
		finish();
	}

	@Override
	protected void onStart()
	{
		// TODO Auto-generated method stub
		super.onStart();
	}
	

	// Inner Bean class to capture and store response object
	class CodeEntry
	{
	    int id;
		String description;
	}

	// Inner adapter class facilitating list view with the objects downloaded from web server
	class OriginEntryAdapter extends ArrayAdapter<String>
	{
		private Context context;
		private int layoutResourceId;
		private List<String> originEntryList;

		public OriginEntryAdapter(Context context, int textViewResourceId, List<String> objects)
		{
			super(context, textViewResourceId, objects);
			this.context = context;
			this.layoutResourceId = textViewResourceId;
			this.originEntryList = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View row = convertView;
			TextView number, des;
			LinearLayout main;

			final String originName = this.originEntryList.get(position);

			if (row == null)
			{
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);
			}
            
			// Make the irrelevant fields invisible.
            ((ImageView) row.findViewById(R.id.arrow_image)).setVisibility(View.GONE);
			
            main = (LinearLayout) row.findViewById(R.id.main);
			des = (TextView) row.findViewById(R.id.des);
			des.setText(originName);
			main.setBackgroundColor(Color.WHITE);

			row.setTag(originName);
			row.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
                    Intent intent = new Intent();
                    intent.putExtra("origin", originName);
                    setResult(RESULT_OK, intent);
                    finish();
				}
			});

			return row;
		}

		@Override
		public int getCount()
		{
			return this.originEntryList.size();
		}
	}
}
