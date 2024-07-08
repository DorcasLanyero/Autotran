package com.cassens.autotran.data.adapters;

import android.content.Context;
import android.graphics.Color;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;

import com.cassens.autotran.R;

import java.util.List;

// Used to highlight the selected choice on simple Spinner widget.
public class HighlightArrayAdapter extends ArrayAdapter<CharSequence> {

    private int selectedIndex = -1;
    private int highlightColor = getContext().getColor(R.color.LightGrey);;

    public void setSelection(int position) {
        selectedIndex =  position;
        notifyDataSetChanged();
    }

    public void setHighlightColor(int highlightColor) {
        this.highlightColor = highlightColor;
    }

    public HighlightArrayAdapter(Context context, int resource, CharSequence[] objects) {
        super(context, resource, objects);
    }

    public HighlightArrayAdapter(Context context, int resource, List objects) {
        super(context, resource, objects);
    }

    @Override
    public View getDropDownView(int position, View convertView, ViewGroup parent) {
        View itemView =  super.getDropDownView(position, convertView, parent);

        if (position == selectedIndex) {
            itemView.setBackgroundColor(highlightColor);
        } else {
            itemView.setBackgroundColor(Color.TRANSPARENT);
        }

        return itemView;
    }
}
