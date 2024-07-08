package com.cassens.autotran.data.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cassens.autotran.CodeEntry;
import com.cassens.autotran.CodeEntryCallback;
import com.cassens.autotran.R;

import java.util.List;

public class CodeEntryAdapter extends ArrayAdapter<CodeEntry>{

    private Context context;
    private int layoutResourceId;
    private List<CodeEntry> codeEntryList;
    CodeEntryCallback callback;

    public CodeEntryAdapter(Context context, int textViewResourceId, List<CodeEntry> objects, CodeEntryCallback callback)
    {
        super(context, textViewResourceId, objects);
        this.context = context;
        this.layoutResourceId = textViewResourceId;
        this.codeEntryList = objects;
        this.callback = callback;
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
        number.setText(Integer.toString(codeEntry.id));
        des = (TextView) row.findViewById(R.id.des);
        des.setText(codeEntry.description);
        main.setBackgroundColor(Color.WHITE);

        row.setTag(codeEntry.id);
        row.setOnClickListener(new View.OnClickListener()
        {

            @Override
            public void onClick(View v)
            {
                callback.callbackCall(Integer.toString(codeEntry.id), codeEntry.description);
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
