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
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.lookup.AreaCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Project : AUTOTRAN Description : DamageCodeList class show list of area code main
 * 
 * @author Hemant Creation Date : 12-11-2013
 */
public class DamageCodeList extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(DamageCodeList.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	ListView list;
	NotiAdapter adapter;
	ArrayList<Noti> notis = new ArrayList<Noti>();
    private static final int REQ_CODE_SECOND_LEVEL_LIST = 1001;

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.damage_code_list);
		list = (ListView) findViewById(R.id.list);
		
		ArrayList<AreaCode> areaCodes = (ArrayList<AreaCode>)DataManager.getAreaCodeList(this);
        
		Noti noti_all = new Noti();

		noti_all.child = "all";
		noti_all.ats_id = -1;
		noti_all.name = "ALL";
        notis.add(noti_all);
        
        
		
        if (areaCodes != null) {
            
            log.debug(Logs.DEBUG, "Getting first level Area Code list: " + areaCodes.size());
            for (AreaCode areaCode: areaCodes) {
                    log.debug(Logs.DEBUG, "areaCode.area_code_id=" + areaCode.area_code_id);
                    Noti noti = new Noti();
                    noti.child = areaCode.getCode();
                    noti.ats_id = areaCode.area_code_id;
                    noti.name = areaCode.getDescription();
                    notis.add(noti);
            }
            
            adapter = new NotiAdapter(DamageCodeList.this, R.layout.horizontal_list_with_color_text, notis);
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
	
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data)
	{
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode == REQ_CODE_SECOND_LEVEL_LIST)
		{
		    if (resultCode == RESULT_OK) {
			    Intent i = new Intent();		
			    i.putExtra("id", data.getStringExtra("id"));
			    setResult(resultCode, i);
			    finish();
		    }
		}
		// Shouldn't get here.
	}

	// Inner Bean class to capture and store response object
	class Noti
	{
		String name, child;
		int ats_id;
	}

	// Inner adapter class facilitating list view with the objects downloaded from web server
	class NotiAdapter extends ArrayAdapter<Noti>
	{
		private Context context;
		private int layoutResourceId;
		private List<Noti> tickerItems;

		public NotiAdapter(Context context, int textViewResourceId, List<Noti> objects)
		{
			super(context, textViewResourceId, objects);
			this.context = context;
			this.layoutResourceId = textViewResourceId;
			this.tickerItems = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View row = convertView;
			TextView des, dealerView;
			LinearLayout main;

			final Noti tickerItem = this.tickerItems.get(position);

			if (row == null)
			{
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);
			}

			// Make the irrelevant fields invisible.
            ((TextView) row.findViewById(R.id.number)).setVisibility(View.GONE);
			
            main = (LinearLayout) row.findViewById(R.id.main);
			des = (TextView) row.findViewById(R.id.des);
			des.setText(tickerItem.name);
			if (position % 2 == 0)
			{
				main.setBackgroundColor(Color.WHITE);
			}

			row.setTag(tickerItem.ats_id);
			row.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					// TODO Auto-generated method stub
					Intent intent = new Intent(DamageCodeList.this, DamageCodeListSecond.class);
					intent.putExtra("value", tickerItem.ats_id);
					intent.putExtra("check", "edit");
					startActivityForResult(intent, REQ_CODE_SECOND_LEVEL_LIST);
				}
			});

			return row;
		}

		@Override
		public int getCount()
		{
			return this.tickerItems.size();
		}
	}
}
