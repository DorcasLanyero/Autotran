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
import com.cassens.autotran.data.model.lookup.ScacCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * Project : AUTOTRAN Description : DamageCodeList class show list of area code main
 * 
 * @author Hemant Creation Date : 12-11-2013
 */
public class ScacCodeList extends AutoTranActivity
{
    private static final Logger log = LoggerFactory.getLogger(ScacCodeList.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	ListView list;
	TextView title;
    TextView prompt;
	CodeEntryAdapter adapter;
	ArrayList<CodeEntry> codeEntryList = new ArrayList<CodeEntry>();

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		// TODO Auto-generated method stub
		super.onCreate(savedInstanceState);
		setContentView(R.layout.code_list);
		list = (ListView) findViewById(R.id.list);
        title = (TextView) findViewById(R.id.title);
        prompt = (TextView) findViewById(R.id.prompt);
		
        int terminal_id = getIntent().getIntExtra("terminal_id", 50);
		List<ScacCode> scacCodeList = (List<ScacCode>) DataManager.getScacCodeList(this, terminal_id);

log.debug(Logs.DEBUG, "in ScacCodeList terminal_id=" + terminal_id + " list.size()=" + scacCodeList.size());

		prompt.setText("Select SCAC Code");
		
        if (scacCodeList != null) {
            
            log.debug(Logs.DEBUG, "Getting SCAC code list: " + scacCodeList.size());
            for (ScacCode scacCode: scacCodeList) {
                    log.debug(Logs.DEBUG, "terminal=" + scacCode.terminal_id);
                    CodeEntry codeEntry = new CodeEntry();

                    codeEntry.id = scacCode.scac_code_id;
                    codeEntry.description = scacCode.getDescription();
                    codeEntryList.add(codeEntry);
            }
            adapter = new CodeEntryAdapter(ScacCodeList.this, R.layout.horizontal_list_with_color_text, codeEntryList);
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
	class CodeEntryAdapter extends ArrayAdapter<CodeEntry>
	{
		private Context context;
		private int layoutResourceId;
		private List<CodeEntry> codeEntryList;

		public CodeEntryAdapter(Context context, int textViewResourceId, List<CodeEntry> objects)
		{
			super(context, textViewResourceId, objects);
			this.context = context;
			this.layoutResourceId = textViewResourceId;
			this.codeEntryList = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View row = convertView;
			TextView number, des, dealerView;
			LinearLayout main;

			final CodeEntry codeEntry = this.codeEntryList.get(position);

			if (row == null)
			{
				LayoutInflater inflater = ((Activity) context).getLayoutInflater();
				row = inflater.inflate(layoutResourceId, parent, false);
			}
            
			// Make the irrelevant fields invisible.
            ((ImageView) row.findViewById(R.id.arrow_image)).setVisibility(View.GONE);
			
            main = (LinearLayout) row.findViewById(R.id.main);
            number = (TextView) row.findViewById(R.id.number);
            number.setText(codeEntry.description);
			
            /*
            des = (TextView) row.findViewById(R.id.des);
			des.setText(codeEntry.description);
			*/
            
			main.setBackgroundColor(Color.WHITE);

			row.setTag(codeEntry.id);
			row.setOnClickListener(new OnClickListener()
			{

				@Override
				public void onClick(View v)
				{
                    Intent intent = new Intent();
                    intent.putExtra("code_id", codeEntry.id);
                    intent.putExtra("code", codeEntry.description);
                    setResult(RESULT_OK, intent);
                    finish();
				}
			});

			return row;
		}

		@Override
		public int getCount()
		{
			return this.codeEntryList.size();
		}
	}
}
