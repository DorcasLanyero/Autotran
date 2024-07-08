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

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.lookup.ShuttleMove;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class ShuttleDestinationListActivity extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(ShuttleDestinationListActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	ListView list;
	TextView title;
    TextView prompt;
	DestinationEntryAdapter adapter;
	ArrayList<ShuttleMove> destinationEntryList = new ArrayList<ShuttleMove>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.code_list);
		list = (ListView) findViewById(R.id.list);
        title = (TextView) findViewById(R.id.title);
        prompt = (TextView) findViewById(R.id.prompt);
		
        String terminal = this.getIntent().getExtras().getString("terminal");
        String origin = this.getIntent().getExtras().getString("origin");

        
		ArrayList<ShuttleMove> destinationList = (ArrayList<ShuttleMove>) DataManager.getShuttleMoves(this, terminal, origin, null);
        
		prompt.setText("Select Shuttle Load Destination");
		
        if (destinationList != null) {
            
            log.debug(Logs.DEBUG, "Getting terminal list: " + destinationList.size());
            for (ShuttleMove shuttleMove: destinationList) {
                    log.debug(Logs.DEBUG, "shuttleMove=" + shuttleMove.orgDestString);
                    
                    //String destinationName = shuttleMove.getDestinationName();
                    
                   	destinationEntryList.add(shuttleMove);
            }
            adapter = new DestinationEntryAdapter(ShuttleDestinationListActivity.this, R.layout.horizontal_list_with_color_text, destinationEntryList);
            list.setAdapter(adapter);
        }
	}

	public void menuList(View v)
	{
		// TODO Auto-generated method stub

	}

	public void back(View v)
	{
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
	class DestinationEntryAdapter extends ArrayAdapter<ShuttleMove>
	{
		private Context context;
		private int layoutResourceId;
		private List<ShuttleMove> originEntryList;

		public DestinationEntryAdapter(Context context, int textViewResourceId, List<ShuttleMove> objects)
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

			final ShuttleMove move = this.originEntryList.get(position);
			final String destinationName = move.destination;

			if (row == null)
			{
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);
			}
            
			// Make the irrelevant fields invisible.
            ((ImageView) row.findViewById(R.id.arrow_image)).setVisibility(View.GONE);
			
            main = (LinearLayout) row.findViewById(R.id.main);
            number = (TextView) row.findViewById(R.id.number);
            number.setText(move.getTerminal() + move.getMoveCode());
			des = (TextView) row.findViewById(R.id.des);
			des.setText(destinationName);
			main.setBackgroundColor(Color.WHITE);

			row.setTag(move.shuttleMoveId);
			row.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
                    Intent intent = new Intent();
                    intent.putExtra("destinationId", move.shuttleMoveId);
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
