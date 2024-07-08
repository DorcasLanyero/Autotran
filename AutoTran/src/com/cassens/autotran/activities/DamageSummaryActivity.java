package com.cassens.autotran.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ExpandableListView;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.adapters.VINDamagesExpandableAdapter;
import com.cassens.autotran.data.model.Damage;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.interfaces.VehicleBatchInterface;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.List;

public class DamageSummaryActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(DamageSummaryActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_damage_summary);

        Bundle bundle = getIntent().getExtras();
        final int operation = bundle.getInt(Constants.CURRENT_OPERATION);
        final int lookupID = bundle.getInt(Constants.CURRENT_LOOKUP_ID);
        
        VehicleBatchInterface batch = null;
        if (operation == Constants.DELIVERY_OPERATION) {
            batch = DataManager.getDelivery(this, lookupID);

            Delivery delivery = (Delivery) batch;
            if (delivery.dealer != null && !HelperFuncs.isNullOrEmpty(delivery.dealer.customer_name)) {
                ((TextView) findViewById(R.id.title)).setText("Delivery to " + delivery.dealer.customer_name);
            } else if (delivery.shuttleLoad) {
                ((TextView) findViewById(R.id.title)).setText("Shuttle Move");
            }
        } else {
            batch = DataManager.getLoad(this, lookupID);
        }

        if (batch == null) {
            ((TextView) findViewById(R.id.title)).setText("No load or delivery found!");
        }

        ExpandableListView vinsAndDamages = (ExpandableListView) findViewById(R.id.damage_history_list);
        vinsAndDamages.setOnGroupClickListener(new ExpandableListView.OnGroupClickListener() {
            @Override
            public boolean onGroupClick(ExpandableListView parent, View v,
                                        int groupPosition, long id) {
                return true; // This way the expander cannot be collapsed
            }
        });

        List<DeliveryVin> dvl = batch.getDeliveryVinList(false);

        if (operation == Constants.DELIVERY_OPERATION) {
            // For delivery operation, show only delivery damages
            for (DeliveryVin dv : dvl) {
                Iterator itr = dv.damages.iterator(); // Need iterator since we're removing elements
                while (itr.hasNext()) {
                    Damage damage = (Damage) itr.next();
                    if (damage.preLoadDamage) {
                        itr.remove();
                    }
                }
            }
        }
        VINDamagesExpandableAdapter adapter = new VINDamagesExpandableAdapter(this, dvl, R.layout.vin_list_element, R.layout.three_text_view);
        log.debug(Logs.INTERACTION, "delivery vins: " + adapter.getGroupCount());
        vinsAndDamages.setAdapter(adapter);
        for (int i = 0; i < adapter.getGroupCount(); i++) {
            vinsAndDamages.expandGroup(i);
        }

        Button addNotes = (Button) findViewById(R.id.add_notes_button);
        addNotes.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtility.logButtonClick(log, view);
                startSupplementalNotes(lookupID, operation);
            }
        });

        Button doneButton = findViewById(R.id.done_button);
        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtility.logButtonClick(log, view);
                finish();
            }
        });
    }

    private void startSupplementalNotes(int lookupKey, int operation) {
        Intent notesIntent = new Intent(DamageSummaryActivity.this, SupplementalNotesActivity.class);
        notesIntent.putExtra(Constants.CURRENT_LOOKUP_ID, lookupKey);
        notesIntent.putExtra(Constants.CURRENT_OPERATION, operation);
        startActivity(notesIntent);
    }

    public void back(View v)
    {
        Intent i = new Intent();
        finish();
    }
}
