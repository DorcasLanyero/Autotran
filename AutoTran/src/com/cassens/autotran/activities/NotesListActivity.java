package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.cassens.autotran.R;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;


public class NotesListActivity extends AutoTranActivity
{
	private static final Logger log = LoggerFactory.getLogger(NotesActivity.class.getSimpleName());

	@Override
	public Logger getLogger() {
		return log;
	}

	public static final String EXTRA_TITLE = "title";
	public static final String EXTRA_OPTIONS = "list_resource";
	public static final String RESPONSE_SELECTION = "selection";
	public static final String EXTRA_TOP_BAR_COLOR = "top_bar_color";

	ListView listView;
	NotesListAdapter adapter;
	String response;
	ProgressDialog dialog;
	private static String[] notes;
	private int topBarColor = -1;
	
	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_notes_list);

		((TextView) findViewById(R.id.ACTIVITY_TITLE)).setText(getIntent().getExtras().getString(EXTRA_TITLE));
		topBarColor = getIntent().getIntExtra(EXTRA_TOP_BAR_COLOR, -1);
		if (topBarColor >= 0) {
			setTopBarColor(R.id.topBarLayout, R.id.img_back, R.drawable.back_button_dealer, -1, -1, R.color.DealerIndicatorColor);
		}

		listView = (ListView) findViewById(R.id.listView1);
		notes = getIntent().getExtras().getStringArray(EXTRA_OPTIONS);
		Arrays.sort(notes);
        adapter = new NotesListAdapter(NotesListActivity.this, R.layout.horizontal_list_with_color_text, Arrays.asList(notes));
        listView.setAdapter(adapter);
	}

	public void back(View v)
	{
        Intent intent = new Intent();
        //intent.putExtra("note", ((TextView)v).getText());
        setResult(RESULT_CANCELED, intent);
		finish();
	}

	// Inner adapter class facilitating list view with the objects downloaded from web server
	class NotesListAdapter extends ArrayAdapter<String>
	{
		private Context context;
		private int layoutResourceId;
		private List<String> notes;
		private int size;

		public NotesListAdapter(Context context, int textViewResourceId, List<String> objects)
		{
			super(context, textViewResourceId, objects);
			this.context = context;
			this.layoutResourceId = textViewResourceId;
			this.notes = objects;
		}

		@Override
		public View getView(final int position, View convertView, ViewGroup parent)
		{
			View row = convertView;
			TextView vinView;

			final String noteItem = this.notes.get(position);

			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);

			row.findViewById(R.id.arrow_image).setVisibility(View.GONE);
			row.findViewById(R.id.number).setVisibility(View.GONE);

            vinView = (TextView) row.findViewById(R.id.des);
            vinView.setTextColor(Color.BLACK);
			vinView.setTextSize(25);
			row.findViewById(R.id.main).setBackgroundColor(Color.WHITE);
            vinView.setText(noteItem);
            vinView.setOnClickListener(new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    Intent intent = new Intent();
                    intent.putExtra(RESPONSE_SELECTION, noteItem);
                    setResult(RESULT_OK, intent);
                    finish();
                }
            });

			return row;
		}

		@Override
		public int getCount()
		{
			return this.notes.size();
		}
	}

	boolean isDealerMode() {
		return topBarColor >= 0;
	}
}
