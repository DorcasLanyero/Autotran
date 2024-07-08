package com.cassens.autotran.data.adapters;

import android.app.Activity;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.cassens.autotran.R;
import com.cassens.autotran.activities.VINInspectionActivity;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.DeliveryVin;
import com.sdgsystems.util.HelperFuncs;

import java.util.List;

/**
 * Created by adam on 3/16/16.
 */
public class VINDamagesExpandableAdapter extends BaseExpandableListAdapter {

    final private Context context;
    final private List<DeliveryVin> deliveryVins;
    final private int groupLayoutId;
    final private int subGroupLayoutId;

    public VINDamagesExpandableAdapter(Context context, List<DeliveryVin> deliveryVins, int groupLayoutId, int subGroupLayoutId) {
        this.context = context;
        this.deliveryVins = deliveryVins;
        this.groupLayoutId = groupLayoutId;
        this.subGroupLayoutId = subGroupLayoutId;
    }

    @Override
    public int getGroupCount() {
        return deliveryVins.size();
    }

    @Override
    public int getChildrenCount(int i) {
        return deliveryVins.get(i).damages.size();
    }

    @Override
    public Object getGroup(int i) {
        return deliveryVins.get(i);
    }

    @Override
    public Object getChild(int i, int i1) {
        return deliveryVins.get(i).damages.get(i1);
    }

    @Override
    public long getGroupId(int i) {
        return deliveryVins.get(i).delivery_vin_id;
    }

    @Override
    public long getChildId(int i, int i1) {
        return deliveryVins.get(i).damages.get(i1).damage_id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int i, boolean b, View view, ViewGroup viewGroup) {
        final DeliveryVin dv = (DeliveryVin) this.getGroup(i);

        if (view == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(groupLayoutId, viewGroup, false);
        }

        TextView vinView = (TextView) view.findViewById(R.id.ACTIVITY_TITLE);
        TextView subtextView = (TextView) view.findViewById(R.id.textView1Subtext);
        TextView secondLine = (TextView) view.findViewById(R.id.ACTIVITY_PROMPT);
        TextView loadPos = (TextView) view.findViewById(R.id.LoadPosition);
        TextView drivenBacked = (TextView) view.findViewById(R.id.LoadOrientation);

        vinView.setText(HelperFuncs.splitVin(String.format("%s", dv.vin.vin_number)));

        String color = "", format;
        if (!HelperFuncs.isNullOrEmpty(dv.vin.colordes)) {
            color = dv.vin.colordes;
        } else if (!HelperFuncs.isNullOrEmpty(dv.vin.color)) {
            color = dv.vin.color;
        }

        if (!HelperFuncs.isNullOrEmpty(color)) {
            format = "%s %s %s - (%s lbs)";
        } else {
            format = "%s%s %s - (%s lbs";
        }

        subtextView
                .setText(String
                        .format(format,
                                color,
                                dv.vin.type,
                                dv.vin.body,
                                dv.vin.weight));

        loadPos.setText(String.format("%s: %s", "Position",
                dv.position == null ? ""
                        : dv.position));
        if (dv.backdrv.equalsIgnoreCase("D")) {
            drivenBacked.setText("Driven");
        } else if (dv.backdrv.equalsIgnoreCase("B")) {
            drivenBacked.setText("Backed");
        }

        secondLine.setText("Pro: " + (dv.pro != null ? dv.pro : ""));

        return view;
    }

    @Override
    public View getChildView(int i, int i1, boolean b, View view, ViewGroup viewGroup) {
        final Damage d = (Damage) this.getChild(i, i1);

        if (view == null) {
            LayoutInflater inflater = ((Activity) context).getLayoutInflater();
            view = inflater.inflate(subGroupLayoutId, viewGroup, false);
        }

        view.findViewById(R.id.deleteButton).setVisibility(View.GONE);

        if (d.specialCode == null) {
            ((TextView) view.findViewById(R.id.one)).setText(VINInspectionActivity.formatATSValue(d.areaCode.getCode()));
            ((TextView) view.findViewById(R.id.two)).setText(VINInspectionActivity.formatATSValue(d.typeCode.getCode()));
            ((TextView) view.findViewById(R.id.three)).setText(VINInspectionActivity.formatATSValue(d.severityCode.getCode()));
        } else {
            ((TextView) view.findViewById(R.id.one)).setText(VINInspectionActivity.formatATSValue(d.specialCode.getAreaCode()));
            ((TextView) view.findViewById(R.id.two)).setText(VINInspectionActivity.formatATSValue(d.specialCode.getTypeCode()));
            ((TextView) view.findViewById(R.id.three)).setText(VINInspectionActivity.formatATSValue(d.specialCode.getSeverityCode()));
        }
        TextView preloadOrDelivery = (TextView) view.findViewById(R.id.preloadOrDelivery);
        preloadOrDelivery.setVisibility(View.VISIBLE);
        preloadOrDelivery.setText(d.preLoadDamage ? "PRELOAD" : "DELIVERY");

        return view;
    }

    @Override
    public boolean isChildSelectable(int i, int i1) {
        return false;
    }
}
