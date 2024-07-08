package com.cassens.autotran.activities;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ExpandableListView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.adapters.ExpandableCodeListAdapter;
import com.cassens.autotran.data.model.lookup.AreaCode;
import com.cassens.autotran.data.model.lookup.TypeCode;
import com.cassens.autotran.data.model.lookup.SeverityCode;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by pknight on 3/18/16.
 *
 * This activity implements a tabbed view but without using an action bar. That was intentional
 * because the standard pattern that uses the action bar relies on using fragments, and we wanted
 * to avoid introducing fragments into the code base at this point.  To accomplish this, we use
 * two list views: an ExpandableListView, which is used to display the two-level action codes,
 * and an normal ListView for displying the Type and Severity codes in a simple list.  Only one
 * of these is visible at a time based on whichever tab is active.
 */
public class DamageCodeTabbedListActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(DamageCodeTabbedListActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    ExpandableCodeListAdapter listAdapter;
    ExpandableListView expandableListView;
    ListView singleListView;
    List<String> areaCodeListParents;
    HashMap<String, List<String>> areaCodeListChildren;

    SingleLevelItemAdapter typeCodeListAdapter;
    List<String> typeCodeList;

    SingleLevelItemAdapter severityCodeListAdapter;
    List<String> severityCodeList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.expandable_code_list);

        // get the expandable list view
        expandableListView = (ExpandableListView) findViewById(R.id.expandableCodeList);
        singleListView = (ListView) findViewById(R.id.singleLevelCodeList);

        // preparing list data
        prepareListData();

        listAdapter = new ExpandableCodeListAdapter(this, areaCodeListParents, areaCodeListChildren);

        typeCodeListAdapter = new SingleLevelItemAdapter(this, R.layout.expandable_code_list_item, typeCodeList);

        severityCodeListAdapter = new SingleLevelItemAdapter(this, R.layout.expandable_code_list_item, severityCodeList);

        // setting list adapter
        expandableListView.setAdapter(listAdapter);
    }

    /*
     * Preparing the list data
     */
    private void prepareListData() {

        // Get area code data

        areaCodeListParents = new ArrayList<String>();
        areaCodeListChildren = new HashMap<String, List<String>>();
        List<String> allCodes = new ArrayList<String>();

        ArrayList<AreaCode> areaCodes = (ArrayList<AreaCode>) DataManager.getAreaCodeList(this);

        if (areaCodes.size() == 0) {
            // This means damage codes haven't been downloaded.

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            //builder.setTitle("Notification");
            builder.setMessage("Damage codes have not been downloaded yet.  To download them, run a dispatch.");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            // builder.setCancelable(cancelable);
            builder.create().show();
            return;
        }

        areaCodeListParents.add("ALL");
        for (AreaCode areaCode: areaCodes) {
            List<String> childCodes = new ArrayList<String>();

            areaCodeListParents.add(areaCode.getDescription());
            for(AreaCode childCode : areaCode.childAreaCodes) {
                String codeEntry = new String(String.format("%02d - %s",
                                        Integer.parseInt(childCode.getCode()), childCode.getDescription()));
                childCodes.add(codeEntry);
                allCodes.add(codeEntry);
            }
            Collections.sort(childCodes);
            areaCodeListChildren.put(areaCode.getDescription(), childCodes);
        }
        Collections.sort(allCodes);
        areaCodeListChildren.put("ALL", allCodes);

        // Get type code data

        typeCodeList = new ArrayList<String>();

        ArrayList<TypeCode> typeCodes = (ArrayList<TypeCode>)DataManager.getTypeCodeList(this);

        if (typeCodes != null) {
            for (TypeCode typeCode: typeCodes) {
                String codeEntry = new String(String.format("%02d - %s",
                        Integer.parseInt(typeCode.getCode()), typeCode.getDescription()));
                typeCodeList.add(codeEntry);
            }
        }

        // Get severity code data

        severityCodeList = new ArrayList<String>();

        ArrayList<SeverityCode> severityCodes = (ArrayList<SeverityCode>)DataManager.getSeverityCodeList(this);

        if (severityCodes != null) {
            for (SeverityCode severityCode: severityCodes) {
                String codeEntry = new String(String.format("%02d - %s",
                        Integer.parseInt(severityCode.getCode()), severityCode.getDescription()));
                severityCodeList.add(codeEntry);
            }
        }
    }

    public void back(View v)
    {
        finish();
    }

    public void onAreaCodesClick(View v)
    {
        ((TextView)findViewById(R.id.typeCodesUnderline)).setVisibility(View.INVISIBLE);
        ((TextView)findViewById(R.id.severityCodesUnderline)).setVisibility(View.INVISIBLE);
        ((TextView)findViewById(R.id.areaCodesUnderline)).setVisibility(View.VISIBLE);

        expandableListView.setAdapter(listAdapter);
        singleListView.setVisibility(View.GONE);
        expandableListView.setVisibility(View.VISIBLE);
    }

    public void onTypeCodesClick(View v)
    {
        ((TextView)findViewById(R.id.areaCodesUnderline)).setVisibility(View.INVISIBLE);
        ((TextView)findViewById(R.id.severityCodesUnderline)).setVisibility(View.INVISIBLE);
        ((TextView)findViewById(R.id.typeCodesUnderline)).setVisibility(View.VISIBLE);

        singleListView.setAdapter(typeCodeListAdapter);
        expandableListView.setVisibility(View.GONE);
        singleListView.setVisibility(View.VISIBLE);
    }

    public void onSeverityCodesClick(View v)
    {
        ((TextView)findViewById(R.id.typeCodesUnderline)).setVisibility(View.INVISIBLE);
        ((TextView)findViewById(R.id.areaCodesUnderline)).setVisibility(View.INVISIBLE);
        ((TextView)findViewById(R.id.severityCodesUnderline)).setVisibility(View.VISIBLE);

        singleListView.setAdapter(severityCodeListAdapter);
        expandableListView.setVisibility(View.GONE);
        singleListView.setVisibility(View.VISIBLE);
    }

    // Inner adapter class facilitating list view with the objects downloaded from web server
    class SingleLevelItemAdapter extends ArrayAdapter<String>
    {
        private Context context;
        private int layoutResourceId;
        private List<String> listItems;

        public SingleLevelItemAdapter(Context context, int textViewResourceId, List<String> objects)
        {
            super(context, textViewResourceId, objects);
            this.context = context;
            this.layoutResourceId = textViewResourceId;
            this.listItems = objects;
        }

        @Override
        public View getView(final int position, View convertView, ViewGroup parent)
        {
            View row = convertView;
            TextView itemTextView;
            LinearLayout main;

            final String listItem = this.listItems.get(position);

            if (row == null)
            {
                LayoutInflater inflater = ((Activity) context).getLayoutInflater();
                row = inflater.inflate(layoutResourceId, parent, false);
            }

            itemTextView = (TextView) row.findViewById(R.id.expandableCodeListItem);
            itemTextView.setText(listItem);

            return row;
        }

        @Override
        public int getCount()
        {
            return this.listItems.size();
        }
    }
}
