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
import com.cassens.autotran.data.model.lookup.AreaCode;
import com.cassens.autotran.data.model.lookup.SeverityCode;
import com.cassens.autotran.data.model.lookup.SpecialCode;
import com.cassens.autotran.data.model.lookup.TypeCode;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class DamageCodeListSecond extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(DamageCodeListSecond.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	public ListView list;
	public NotiAdapter adapter;
	public ArrayList<Noti> notis = new ArrayList<Noti>();
	TextView txt, textView1;
	public static final String SPLITTER = ",";

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.damage_code_list_second);
		list = (ListView) findViewById(R.id.list);
		txt = (TextView) findViewById(R.id.txt);
		textView1 = (TextView) findViewById(R.id.ACTIVITY_TITLE);
		if (getIntent().getStringExtra("check").equals("type")){
			txt.setText("Enter Type Code");
			ArrayList<TypeCode> typeCodes = (ArrayList<TypeCode>)DataManager.getTypeCodeList(this);
			
			if (typeCodes != null) {
                log.debug(Logs.DEBUG, "Getting Type Code list");
                for (TypeCode typeCode: typeCodes) {
                    Noti noti = new Noti();
                    
                    noti.ats_code = typeCode.getCode();
                    noti.ats_id = typeCode.type_code_id;
                    noti.ats_desc = typeCode.getDescription();
                    notis.add(noti);
                }
                adapter = new NotiAdapter(DamageCodeListSecond.this, R.layout.horizontal_list_with_color_text, notis);
                list.setAdapter(adapter);
			}
			else {
				log.debug(Logs.DEBUG, "Need to connect to internet to get type code list");
			}
		}
		else if (getIntent().getStringExtra("check").equals("svrty")) {
				log.debug(Logs.DEBUG, "Getting Severity Code list");

				txt.setText("Enter Severity Code");
	            ArrayList<SeverityCode> severityCodes = (ArrayList<SeverityCode>)DataManager.getSeverityCodeList(this);

	            if (severityCodes != null) {
	                log.debug(Logs.DEBUG, "Getting Severity Code list (" + severityCodes.size() + ") entries");
	                for (SeverityCode severityCode: severityCodes) {
                        Noti noti = new Noti();

                        noti.ats_code = severityCode.getCode();
                        noti.ats_id = severityCode.severity_code_id;
                        noti.ats_desc = severityCode.getDescription();
                        notis.add(noti);
                    }
                    adapter = new NotiAdapter(DamageCodeListSecond.this, R.layout.horizontal_list_with_color_text, notis);
                    list.setAdapter(adapter);
                }
				else {
					log.debug(Logs.DEBUG, "Need to connect to internet to get severity code list");
				}
			}
			else if (getIntent().getStringExtra("check").equals("pos")) {
				txt.setText(R.string.enter_load_position);

				ArrayList<String> positions = getIntent().getStringArrayListExtra("positions");

				for (String position : positions) {
					Noti noti = new Noti();
					noti.ats_code = position;
					noti.ats_id = positions.indexOf(position);
					noti.ats_desc = "";
					notis.add(noti);
				}

				adapter = new NotiAdapter(DamageCodeListSecond.this, R.layout.horizontal_list_with_color_text, notis);
                list.setAdapter(adapter);
			}
			else if (getIntent().getStringExtra("check").equals("special")) {
				txt.setVisibility(View.GONE);
				textView1.setText("Special Damage Codes");
                ArrayList<SpecialCode> specialCodes = (ArrayList<SpecialCode>)DataManager.getSpecialCodeList(this);
                
                if (specialCodes != null) {
                    log.debug(Logs.DEBUG, "Getting Special Code list");
                    for (SpecialCode specialCode: specialCodes) {
                            Noti noti = new Noti();

                            noti.ats_id = specialCode.special_code_id;
                            noti.ats_desc = specialCode.getDescription();
                            notis.add(noti);
					}
					adapter = new NotiAdapter(DamageCodeListSecond.this, R.layout.horizontal_list_with_color_text,
							notis);
					list.setAdapter(adapter);
				}
			}
			else {
	            ArrayList<AreaCode> areaCodes = (ArrayList<AreaCode>)DataManager.getAreaCodeList(this);
	            ArrayList<AreaCode> displayedCodes = new ArrayList<AreaCode>();
	            if (areaCodes != null) {
	            
	                // TODO: Need to lookup parent here, then retrieve its children.
	                // "value" should be set to the key needed to lookup the parent (is it the ID or the code?)
	                int code_id = getIntent().getIntExtra("value", -2); 
	                
	                
	                log.debug(Logs.DEBUG, "Getting second level Area Code list: "
							+ getIntent().getIntExtra("value", -2));
	                for (AreaCode areaCode: areaCodes) {
	                	
	                		if(areaCode.area_code_id == code_id || code_id == -1) {
	                	
	                			for(AreaCode childCode : areaCode.childAreaCodes) {
		                        log.debug(Logs.DEBUG, "areaCode.area_code_id=" + childCode.area_code_id);
		                        
		                        displayedCodes.add(childCode);
	                			}
	                		}
	                }
	                
	                Collections.sort(displayedCodes);

	                for(AreaCode childCode : displayedCodes) {
	                	Noti noti = new Noti();
                        noti.ats_code = childCode.getCode();
                        noti.ats_id = childCode.area_code_id;
                        noti.ats_desc = childCode.getDescription();
                        notis.add(noti);
	                }
	                adapter = new NotiAdapter(DamageCodeListSecond.this, R.layout.horizontal_list_with_color_text, notis);
	                list.setAdapter(adapter);
	            }
				
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

	// Inner Bean class to capture and store response object
	 public class Noti
	{
		public String ats_desc, ats_code;
		public int ats_id;

		public String getFormattedCode() {
			if (!HelperFuncs.isNullOrEmpty(this.ats_code) && this.ats_code.length() < 2) {
				return this.ats_code;
			}
			return this.ats_code;
		}
	}

	// Inner adapter class facilitating list view with the objects downloaded from web server
	 public class NotiAdapter extends ArrayAdapter<Noti>
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
			TextView number, des;
			LinearLayout main;
			final Noti tickerItem = this.tickerItems.get(position);

			if (row == null)
			{
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);
			}
			number = (TextView) row.findViewById(R.id.number);
			des = (TextView) row.findViewById(R.id.des);
			main = (LinearLayout) row.findViewById(R.id.main);
			((ImageView) row.findViewById(R.id.arrow_image)).setVisibility(View.GONE);

			System.out.println("special code:" + tickerItem.ats_code);//.split(SPLITTER)[0]);
			number.setText(tickerItem.getFormattedCode());//.split(SPLITTER)[0]);
			des.setText(tickerItem.ats_desc);
			if (position % 2 == 0)
			{
				main.setBackgroundColor(Color.WHITE);
			}

			row.setTag(tickerItem.ats_code);
			row.setOnClickListener(new OnClickListener()
			{
				@Override
				public void onClick(View v)
				{
					Intent i = new Intent();
                    if (getIntent().getStringExtra("check").equals("special")) {
                        i.putExtra("id", tickerItem.ats_code + "," + tickerItem.ats_id + ",add");
                    }
                    else {
                        i.putExtra("id", tickerItem.ats_code + "," + tickerItem.ats_id);
                    }
                    setResult(RESULT_OK, i);
                    finish();
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
