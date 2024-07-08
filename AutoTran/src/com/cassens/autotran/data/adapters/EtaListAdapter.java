package com.cassens.autotran.data.adapters;

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
import com.cassens.autotran.data.model.EtaHolder;
import com.sdgsystems.util.HelperFuncs;

import java.util.List;

// Inner adapter class facilitating list view with the objects downloaded from web server
public class EtaListAdapter extends ArrayAdapter<EtaHolder>
{
	private Context context;
	private int layoutResourceId;
	private List<EtaHolder> etaList;

	public EtaListAdapter(Context context, int textViewResourceId, List<EtaHolder> objects)
	{
		super(context, textViewResourceId, objects);
		this.context = context;
		this.layoutResourceId = textViewResourceId;
		this.etaList = objects;
	}

	@Override
	public View getView(final int position, View convertView, ViewGroup parent)
	{
		View row = convertView;
		TextView dealerName;
		TextView eta;

		final EtaHolder etaHolder = this.etaList.get(position);

		if (row == null) {
			LayoutInflater inflater = ((Activity) context).getLayoutInflater();
			row = inflater.inflate(layoutResourceId, parent, false);
		}

		dealerName = (TextView) row.findViewById(R.id.dealer_name);
		eta = (TextView) row.findViewById(R.id.eta);

		dealerName.setText(etaHolder.mTitle);
		eta.setText(etaHolder.mEta);

		if (position % 2 == 0) {
			row.setBackgroundColor(Color.DKGRAY);
		}

		return row;
	}

	@Override
	public int getCount()
	{
		return this.etaList.size();
	}
}
