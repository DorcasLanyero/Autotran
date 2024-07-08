package com.cassens.autotran.data.adapters;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.location.Location;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.BaseExpandableListAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;

import com.cassens.autotran.BuildConfig;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.R;
import com.cassens.autotran.activities.DealerDetailsActivity;
import com.cassens.autotran.activities.DeliveryVinInspectionActivity;
import com.cassens.autotran.activities.ShuttleBuildLoadActivity;
import com.cassens.autotran.activities.VINInventoryActivity;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.Dealer;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.TrainingRequirement;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.YardInventory;
import com.cassens.autotran.dialogs.LotLocateDialogFragment;
import com.cassens.autotran.dialogs.VehiclePositionDialogFragment;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

public class DeliveryDealerExpandableAdapter extends BaseExpandableListAdapter {
    private static final Logger log = LoggerFactory.getLogger(ShuttleBuildLoadActivity.class.getSimpleName());

    private DeliveryVinInspectionActivity activity;
    private Load load;
    private List<Delivery> deliveries;
    private int groupLayoutId;
    private int subGroupLayoutId;
    public int operation;
    private boolean isVinPickMode = true;
    private Location latestLocation;
    private TrainingRequirement.ByStatus relevantTrainings;
    private boolean isHighClaimsDriver = false;
    public String driverNumber = "";

    public DeliveryDealerExpandableAdapter(DeliveryVinInspectionActivity activity,
                                           int groupViewResourceId, int subGroupViewResourceId,
                                           List<Delivery> deliveries, int operation, boolean isVinPickMode) {
        this.activity = activity;
        this.deliveries = deliveries;
        this.groupLayoutId = groupViewResourceId;
        this.subGroupLayoutId = subGroupViewResourceId;
        this.operation = operation;
        this.isVinPickMode = isVinPickMode;
        if (this.deliveries != null && this.deliveries.size() > 0) {
            this.load = DataManager.getLoad(this.activity, this.deliveries.get(0).load_id);
        }
        User driver = DataManager.getUserForDriverNumber(this.activity, CommonUtility.getDriverNumber(this.activity));
        if (driver != null) {
            isHighClaimsDriver = driver.highClaims != 0;
            driverNumber = driver.driverNumber;
        }
    }

    public void setRelevantTrainings(TrainingRequirement.ByStatus reqs) {
        relevantTrainings = reqs;
    }

    @Override
    public int getGroupCount() {
        return deliveries.size();
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        return deliveries.get(groupPosition).getDeliveryVinList(false).size();
    }

    @Override
    public Object getGroup(int groupPosition) {
        return deliveries.get(groupPosition);
    }

    @Override
    public Object getChild(int groupPosition, int childPosition) {
        return deliveries.get(groupPosition).getDeliveryVinList(false).get(childPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return deliveries.get(groupPosition).delivery_id;
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return deliveries.get(groupPosition).getDeliveryVinList(false).get(childPosition).delivery_vin_id;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    @Override
    public View getGroupView(int groupPosition, boolean isExpanded,
                             View convertView, ViewGroup parent) {

        View row = convertView;
        final Delivery delivery = (Delivery) this.getGroup(groupPosition);

        if (row == null) {
            LayoutInflater inflater = ((Activity) activity).getLayoutInflater();
            row = inflater.inflate(groupLayoutId, parent, false);
        }

        TextView dealerView = (TextView) row.findViewById(R.id.ACTIVITY_TITLE);
        TextView location = (TextView) row.findViewById(R.id.ACTIVITY_PROMPT);
        RelativeLayout itemLayout = (RelativeLayout) row
                .findViewById(R.id.RelativeLayout1);
        TextView infoIcon = (TextView) row.findViewById(R.id.infoIcon);



        if (!delivery.shuttleLoad) {
            final Dealer dealer = delivery.dealer;

            if (load.originLoad && (dealer == null || HelperFuncs.isNullOrWhitespace(dealer.customer_name)
                    || dealer.customer_name.equalsIgnoreCase("UNKNOWN"))) {
                dealerView.setText("Unknown Dealer");
                location.setText("");
                infoIcon.setVisibility(View.GONE);
            }
            else {
                dealerView.setText(String.format("%s - %s", dealer.customer_name,
                        dealer.customer_number));
                location.setText(String.format("%s, %s", dealer.city, dealer.state));
                infoIcon.setVisibility(VISIBLE);
            }

            if (load.originLoad) {
                infoIcon.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CommonUtility.logButtonClick(log, v);
                        showDealerDetails(dealer);
                    }
                });
            } else {
                infoIcon.setVisibility(View.VISIBLE);
                infoIcon.setOnClickListener(new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        CommonUtility.logButtonClick(log, v);
                        showDealerDetails(delivery);
                    }
                });
            }

            if (delivery.dealer != null  && delivery.dealer.hasUpdatedFields()) {
                // set icon to blue
                infoIcon.setBackgroundColor(activity.getResources().getColor(R.color.InformationBlue));
                infoIcon.setTextColor(activity.getResources().getColor(R.color.White));
                infoIcon.setTypeface(Typeface.DEFAULT_BOLD);
                infoIcon.setText(R.string.dealerInfoIconNewInfo);
            }
            else {
                infoIcon.setBackground(ContextCompat.getDrawable(this.activity, R.drawable.button_bg));
                infoIcon.setTextColor(activity.getResources().getColor(R.color.lite_lite_lite_gray));
                infoIcon.setTypeface(Typeface.DEFAULT);
                infoIcon.setText(R.string.dealerInfoIconDefault);
            }
        } else {
            dealerView.setText(this.load.shuttleMove.terminal + ": " + this.load.shuttleMove.getMoveCode());
            location.setText(this.load.shuttleMove.getDestinationName());
            infoIcon.setVisibility(View.GONE);
        }

        TextView groupExpandIcon = (TextView) row
                .findViewById(R.id.groupExpandIcon);
        if (operation == Constants.DELIVERY_OPERATION) {
            groupExpandIcon.setVisibility(View.GONE);
            itemLayout.setBackgroundColor(delivery.isInspected(isHighClaimsDriver) ? Color.LTGRAY
                    : Color.WHITE);
        } else { // preload
            itemLayout
                    .setBackgroundColor(delivery.isPreloadInspected() ? Color.LTGRAY
                            : Color.WHITE);

            boolean highClaims = (delivery.dealer != null) ? delivery.dealer.high_claims : false;
            showBlinkingTextView((TextView)row.findViewById(R.id.callout), highClaims);

            groupExpandIcon.setVisibility(View.VISIBLE);
            groupExpandIcon.setText(isExpanded ? "-" : "+");
        }

        return row;
    }


    private void showBlinkingTextView(TextView tv, boolean on) {
        if (on) {
            Animation anim = new AlphaAnimation(0.0f, 1.0f);
            anim.setDuration(1000); //You can manage the blinking time with this parameter
            anim.setStartOffset(20);
            anim.setRepeatMode(Animation.REVERSE);
            anim.setRepeatCount(Animation.INFINITE);
            tv.startAnimation(anim);
            tv.setVisibility(VISIBLE);
        }
        else {
            tv.setVisibility(GONE);
        }
    }

    private List<String> positions = new ArrayList<String>();

    @Override
    public View getChildView(int groupPosition, int childPosition,
                             boolean isLastChild, View convertView, ViewGroup parent) {

        View row = convertView;

        final Delivery delivery = (Delivery) this.getGroup(groupPosition);
        final DeliveryVin thisDeliveryVin = (DeliveryVin) this.getChild(
                groupPosition, childPosition);

        if (row == null) {
            LayoutInflater inflater = ((Activity) activity).getLayoutInflater();
            row = inflater.inflate(subGroupLayoutId, parent, false);
        }

        TextView vinView = (TextView) row.findViewById(R.id.ACTIVITY_TITLE);
        TextView subtextView = (TextView) row
                .findViewById(R.id.textView1Subtext);
        TextView secondLine = (TextView) row.findViewById(R.id.ACTIVITY_PROMPT);
        TextView loadPos = (TextView) row.findViewById(R.id.LoadPosition);
        TextView drivenBacked = (TextView) row
                .findViewById(R.id.LoadOrientation);
        LinearLayout vinNumArea = (LinearLayout) row
                .findViewById(R.id.LinearLayout1);
        LinearLayout truckPosition = (LinearLayout) row
                .findViewById(R.id.truckPosition);
        final RelativeLayout itemLayout = (RelativeLayout) row
                .findViewById(R.id.RelativeLayout1);
        ImageView backgroundIcon = row.findViewById(R.id.backgroundIcon);

        vinView.setText(HelperFuncs.splitVin(String.format("%s", thisDeliveryVin.vin.vin_number)));

        boolean preload = operation == Constants.PRELOAD_OPERATION;

        if (load == null || !load.shuttleLoad) {
            String color = "", format;
            if (!HelperFuncs.isNullOrEmpty(thisDeliveryVin.vin.colordes)) {
                color = thisDeliveryVin.vin.colordes;
            } else if (!HelperFuncs.isNullOrEmpty(thisDeliveryVin.vin.color)) {
                color = thisDeliveryVin.vin.color;
            }

            if (!HelperFuncs.isNullOrEmpty(color)) {
                format = "%s %s %s - (%s lbs)";
            } else {
                format = "%s%s %s - (%s lbs)";
            }

            if (!preload && delivery.isDealerUnavailable() && !thisDeliveryVin.hasRequiredDealerUnavailableImages(isHighClaimsDriver, delivery.dealer)) {
                format += "\n\n** MORE PHOTOS NEEDED **";
            }

            subtextView
                    .setText(String
                            .format(format,
                                    color,
                                    thisDeliveryVin.vin.type,
                                    thisDeliveryVin.vin.body,
                                    thisDeliveryVin.vin.weight));
        }

        drivenBacked.setVisibility(VISIBLE);
        if (HelperFuncs.isNullOrEmpty(thisDeliveryVin.position) || thisDeliveryVin.position.equals("null")) {
            if (preload) {
                loadPos.setText("Position\nneeded");
                drivenBacked.setVisibility(GONE);
            }
            else {
                loadPos.setText("Pos: --");
            }
        }
        else {
            loadPos.setText(String.format("Pos: %s", thisDeliveryVin.position));
        }

        if (thisDeliveryVin.backdrv.equalsIgnoreCase("D")) {
            drivenBacked.setText("Driven");
        } else if (thisDeliveryVin.backdrv.equalsIgnoreCase("B")) {
            drivenBacked.setText("Backed");
        }

        // if terminal is Lafayette (48) we can have duplicates
        if (!HelperFuncs.isNullOrEmpty(load.originTerminal) && !load.originTerminal.equals("48")) {
            for (Delivery d : this.deliveries) {
                for (DeliveryVin dv : d.getDeliveryVinList(false)) {
                    if (preload
                            && thisDeliveryVin.position != null
                            && !HelperFuncs.noNull(dv.position,"0").trim().equals("0")
                            && positions.indexOf(dv.position) == -1
                            && thisDeliveryVin.delivery_vin_id != dv.delivery_vin_id
                            && thisDeliveryVin.position.equals(dv.position)) {
                        positions.add(dv.position);

                        //We don't want to WARN about 'null'...
                        if(dv.position != null && !dv.position.equalsIgnoreCase("null") && !dv.position.equals("0")) {
                            Builder builder = new AlertDialog.Builder(activity);
                            builder.setTitle("Warning!");
                            TextView msg = new TextView(activity);
                            msg.setGravity(Gravity.CENTER);
                            msg.setTextSize(18);
                            msg.setPadding(15, 10, 15, 15);
                            msg.setText(String.format(
                                    "vin %s\nand\nvin %s\nare both in position %s!",
                                    thisDeliveryVin.vin.vin_number, dv.vin.vin_number,
                                    dv.position));
                            builder.setView(msg);
                            builder.setPositiveButton("OK",
                                    new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                        }
                                    });
                            builder.setCancelable(true);
                            builder.create().show();
                        }
                    }
                }
            }
        }

        vinNumArea.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(final View v) {
                if (BuildConfig.AUTOTRAN_VIN_SCAN_OPTIONAL || activity.isReviewOnly() || (Constants.FEATURE_ALLOW_VIN_CLICK_AFTER_INSPECTION && isVinPickMode)) {
                    CommonUtility.showText("Selected VIN " + thisDeliveryVin.vin.vin_number);
                    activity.onVINSelected(thisDeliveryVin.vin.vin_number, false);
                }
                else {
                    CommonUtility.showText("Please scan the VIN to select it");
                }
            }
        });

        if (!isVinPickMode && preload) {
            if (groupPosition == deliveries.size() - 1 && isLastChild) {
                positions.removeAll(positions);
            }
        }

        if(preload && backgroundIcon != null && relevantTrainings != null) {
            for(TrainingRequirement t : relevantTrainings.finished) {
                if(t.vin != null && t.vin.equals(thisDeliveryVin.vin.vin_number)) {
                    backgroundIcon.setImageResource(R.drawable.coaching_complete_bg_icon);
                    backgroundIcon.setVisibility(View.VISIBLE);
                }
            }
        }

        itemLayout.setBackgroundColor(Color.WHITE);
        truckPosition.setBackgroundColor(Color.WHITE);

        if ((operation == Constants.DELIVERY_OPERATION && (thisDeliveryVin.inspectedDelivery && (!delivery.isDealerUnavailable() || thisDeliveryVin.hasRequiredDealerUnavailableImages(isHighClaimsDriver, delivery.dealer))))
                || (operation == Constants.PRELOAD_OPERATION && thisDeliveryVin.inspectedPreload)) {
            itemLayout.setBackgroundColor(Color.LTGRAY);

            if(backgroundIcon != null) {
                backgroundIcon.setImageResource(R.drawable.coaching_complete_icon);
            }
            if (preload) {
                if (thisDeliveryVin.position == null || thisDeliveryVin.position.equals("null")) {
                    truckPosition.setBackgroundColor(Color.WHITE);
                } else {
                    truckPosition.setBackgroundColor(Color.LTGRAY);
                }
            }
            else {
                truckPosition.setBackgroundColor(Color.LTGRAY);
            }
        }

        StringBuilder secondLineText = new StringBuilder();

        for (Damage dmg : thisDeliveryVin.damages) {
            if (dmg.source.equals("driver") && !dmg.readonly &&
                    ((preload && dmg.preLoadDamage) || (!preload && !dmg.preLoadDamage))
                    ) {
                String[] ats = {dmg.areaCode.getFormattedCode(), dmg.typeCode.getFormattedCode(),
                        dmg.severityCode.getFormattedCode()};

                if (dmg.specialCode != null) {
                    ats[0] = dmg.specialCode.getFormattedAreaCode();
                    ats[1] = dmg.specialCode.getFormattedTypeCode();
                    ats[2] = dmg.specialCode.getFormattedSeverityCode();
                }

                secondLineText.append(ats[0] + "-" + ats[1] + "-" + ats[2]
                        + ", ");
            }
        }

        String separator = "";
        if (secondLineText.length() > 0) {
            secondLineText.insert(0, "Exception(s): ");
            // delete trailing comma and space
            secondLineText.deleteCharAt(secondLineText.length() - 1);
            secondLineText.deleteCharAt(secondLineText.length() - 1);
            separator = "\n";
        }

        if (preload)
        {
            boolean localDamages = false;

            for(Damage damage : thisDeliveryVin.damages) {
                if(damage.source.equals("driver") && !damage.readonly) {
                    localDamages = true;
                }
            }

            if(localDamages && thisDeliveryVin.ats != null && !thisDeliveryVin.ats.trim().isEmpty()) {
                secondLineText.append(separator);
                secondLineText.append("NO SUPERVISOR SIGNATURE");
            }

            secondLineText.insert(0, String.format("Lot: %s   Row/Bay: %s%s", thisDeliveryVin.lot,
                    thisDeliveryVin.rowbay, separator));

            truckPosition.setOnClickListener(new OnClickListener() {
                @SuppressLint("NewApi")
                @Override
                public void onClick(View v) {
                    VehiclePositionDialogFragment dialog = VehiclePositionDialogFragment.newInstance(
                            thisDeliveryVin.delivery_vin_id,(ArrayList<String>)getAvailablePositionStrings(activity, thisDeliveryVin, true),
                            true);
                    dialog.show(((Activity) activity).getFragmentManager(),
                            "dialog");
                }
            });
        } else if(delivery.shuttleLoad) {

            drivenBacked.setVisibility(View.GONE);

            final YardInventory inventory = DataManager.getYardInventoryForDeliveryVin(activity, thisDeliveryVin.delivery_vin_id);
            if (load.shuttleMove.lotLocateRequired) {
                setLotLocationTextView(loadPos, inventory, "Lot location needed");

                loadPos.setOnClickListener(new OnClickListener() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onClick(View v) {
                        LotLocateDialogFragment dialog =
                                new LotLocateDialogFragment(thisDeliveryVin, load, activity, itemLayout, latestLocation, inventory);

                        dialog.show(((Activity) activity).getFragmentManager(),
                                "dialog");
                    }
                });
            }
            else {
                setLotLocationTextView(loadPos, inventory, "Lot location not needed");
            }
        } else if(this.load.originLoad) {
            //drivenBacked.setVisibility(View.GONE);
            loadPos.setText(String.format("%s: %s", "Pos",
                    thisDeliveryVin.position == null || thisDeliveryVin.position.equals("null") ? " -- "
                            : thisDeliveryVin.position));
        } else if(delivery.dealer.lotLocateRequired || thisDeliveryVin.lotLocateRequired()) {

            //Lot locate can by triggered by the do_lotlocate flag
            drivenBacked.setVisibility(View.GONE);
            final YardInventory inventory = DataManager.getYardInventoryForDeliveryVin(activity, thisDeliveryVin.delivery_vin_id);

            setLotLocationTextView(loadPos, inventory, "Lot location needed");
            loadPos.setOnClickListener(new OnClickListener() {
                @SuppressLint("NewApi")
                @Override
                public void onClick(View v) {

                    if (delivery.dealer.lot_code_id < 0) {
                        CommonUtility.logButtonClick(log, "Lot Locate");
                        Intent intent = new Intent(activity, VINInventoryActivity.class);
                        intent.putExtra("vin_number", thisDeliveryVin.vin.vin_number);
                        intent.putExtra("inventory_type", VINInventoryActivity.LOT_LOCATE);
                        intent.putExtra("driverNumber", driverNumber);
                        intent.putExtra("delivery_vin_id", thisDeliveryVin.delivery_vin_id);
                        activity.startActivity(intent);
                        /*
                        String msg = "Terminal and lot code are not set for dealer " +
                                delivery.dealer.customer_
                            activity.getResources().getString(R.string.call_support);
                        CommonUtility.simpleMessageDialog(activity, ); */
                    }
                    else {
                        LotLocateDialogFragment dialog =
                                new LotLocateDialogFragment(thisDeliveryVin, load, delivery, activity, itemLayout, latestLocation, inventory);

                        dialog.show(((Activity) activity).getFragmentManager(), "dialog");
                    }
                }
            });
        }

        secondLine.setText(secondLineText.toString());

        return row;
    }

    private void setLotLocationTextView(TextView tv, YardInventory inventory, String noLocationString) {

        String lotCodeLine = "";
        if(inventory != null) {
            if (inventory.lotCode != null && !HelperFuncs.isNullOrEmpty(inventory.lotCode.code)) {
                lotCodeLine = String.format("L: %s\n", inventory.lotCode.code);
            }
            tv.setText(String.format("%sR: %s\nB: %s", lotCodeLine, HelperFuncs.noNull(inventory.row), HelperFuncs.noNull(inventory.bay)));
        } else {
            tv.setText(noLocationString);
        }
    }

    @NonNull
    public static List<String> getAvailablePositionStrings(Context context, DeliveryVin deliveryVin, boolean includeBlank) {
        ArrayList<String> opts = new ArrayList(DeliveryVin.getValidLoadPositions());

        int load_id = DataManager.getLoadIdForDeliveryVin(context, deliveryVin);
        Load load = DataManager.getLoad(context, load_id);

        if (!HelperFuncs.isNullOrEmpty(load.originTerminal) && !load.originTerminal.equals("48")) {

            if(!load.parentLoad && load.parent_load_id != -1) {
                //get the positions for all delivery vins that are in loads which have
                List<String> siblingPositions = DataManager.getCurrentDVPositions(context, load.parent_load_id);
                for(String position : siblingPositions) {
                    if (!HelperFuncs.isNullOrEmpty(position) && !position.equals(deliveryVin.position) && !position.contains("x")) {
                        opts.remove(position.trim());
                    }
                }
            } else {
                for (Delivery delivery : load.deliveries) {
                    for (DeliveryVin dv : delivery.deliveryVins) {
                        if (!HelperFuncs.isNullOrEmpty(dv.position) && !dv.position.equals(deliveryVin.position) && !dv.position.contains("--")) {
                            opts.remove(dv.position.trim());
                        }
                    }
                }
            }
        }

        if(!includeBlank) {
            opts.remove(0);
        }

        return opts;
    }

    private void showDealerDetails(Delivery delivery) {
        Intent intent = new Intent(activity, DealerDetailsActivity.class);
        intent.putExtra("delivery_id", delivery.delivery_id);
        intent.putExtra("operation", operation);
        ((Activity) activity).startActivity(intent);
    }

    private void showDealerDetails(Dealer dealer) {
        Intent intent = new Intent(activity, DealerDetailsActivity.class);
        intent.putExtra("dealer_id", dealer.dealer_id);
        intent.putExtra("operation", operation);
        ((Activity) activity).startActivity(intent);
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return false;
    }

    @Override
    public long getCombinedChildId(long groupId, long childId) {
        long or = 0x7000000000000000L;
        long group = (groupId & 0x7FFFFFFF) << 32;
        long child = childId & 0xFFFFFFFF;
        return or | group | child;
    }

    public void setLatestLocation(Location latestLocation) {
        this.latestLocation = latestLocation;
    }
}
