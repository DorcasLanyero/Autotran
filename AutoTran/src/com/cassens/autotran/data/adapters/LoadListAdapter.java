package com.cassens.autotran.data.adapters;

import java.util.List;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import com.cassens.autotran.R;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Load;

// Inner adapter class facilitating list view with the objects downloaded from web server
public class LoadListAdapter extends ArrayAdapter<Load>
{
	private Context context;
	private int layoutResourceId;
	private List<Load> loadList;

	public LoadListAdapter(Context context, int textViewResourceId, List<Load> objects)
	{
		super(context, textViewResourceId, objects);
		this.context = context;
		this.layoutResourceId = textViewResourceId;
		this.loadList = objects;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
	
		View row = convertView;
		TextView loadNumber;
		TextView subtitle;

		final Load load = this.loadList.get(position);
		//System.out.println("getView() :: "+position+" :: "+tickerItem.vin_number);
		if (row == null)
		{
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
		}

		loadNumber = (TextView) row.findViewById(R.id.ACTIVITY_TITLE);
		subtitle = (TextView) row.findViewById(R.id.tvSubtitle);
		
		loadNumber.setText(load.loadNumber);
		
		if (position % 2 == 0)
		{
			loadNumber.setBackgroundColor(Color.WHITE);
			subtitle.setBackgroundColor(Color.WHITE);

		}
		
		
		String subString = "";
		
		for(Delivery delivery: load.deliveries)
		{
			if(delivery.shuttleLoad) {
				//subString += load.shuttleMove.destination + " -- ";
			} else {
				subString += delivery.dealer.getDealerDisplayName() + " -- ";
			}
		}
		subtitle.setText(subString);
		return row;
	}

	@Override
	public int getCount()
	{
		return this.loadList.size();
	}
}
