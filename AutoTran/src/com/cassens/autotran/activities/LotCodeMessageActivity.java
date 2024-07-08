package com.cassens.autotran.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.LotCodeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class LotCodeMessageActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(LotCodeMessage.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    public static final String LOAD_ID = "load_id";

    private Load mThisLoad = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle bundle = getIntent().getExtras();
        mThisLoad = DataManager.getLoad(this, bundle.getInt(LOAD_ID, -1));
        if (mThisLoad == null) {
            log.debug(Logs.DEBUG, "LotCodeMessageActivity unexpectedly failed to get load.");
            setResult(RESULT_CANCELED);
            finish();
            return;
        }
        Map<Integer,LotCodeMessage> msgSet = new HashMap<>();
        for (DeliveryVin dv : mThisLoad.getDeliveryVinList()) {
            LotCodeMessage msg = DataManager.getLotCodeMsg(this, mThisLoad.originTerminal, dv.lot);
            if (msg != null && msg.id != 0) {
                msgSet.put(msg.id, msg);
            }
        }

        final List<LotCodeMessage> msgs = new ArrayList<>();
        LotCodeMessage genericMsg = DataManager.getGenericLotCodeMsg(this);
        if (genericMsg != null && genericMsg.id != 0) {
            msgs.add(genericMsg);
        }
        for (Integer id : msgSet.keySet()) {
            msgs.add(msgSet.get(id));
        }

        final LotCodeMessage selectedMsg = new LotCodeMessage();

        setContentView(R.layout.activity_lot_code_message);

        if (msgs.size() == 0) {
            CommonUtility.showText("No messages to display.");
            finishWithOkResult();
            return;
        } else {
            Random random = new Random();
            int selected = random.nextInt(msgs.size());
            selectedMsg.prompt = msgs.get(selected).getPrompt();
            selectedMsg.response = msgs.get(selected).getResponse();

            selectedMsg.message = "";
            for (LotCodeMessage msg : msgs) {
                selectedMsg.message += msg.message + "\n\n";
            }
        }

        final TextView message = (TextView) findViewById(R.id.message);
        final TextView prompt = (TextView) findViewById(R.id.prompt);
        final TextView response = (EditText) findViewById(R.id.response);

        message.setText(selectedMsg.message);
        prompt.setText(selectedMsg.prompt);

        findViewById(R.id.doneButton).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtility.logButtonClick(log, view);
                if (response.getText().toString().trim().equalsIgnoreCase(selectedMsg.response.trim())) {
                    finishWithOkResult();
                } else {
                        AlertDialog.Builder builder = new AlertDialog.Builder(LotCodeMessageActivity.this);
                        builder.setMessage("Response required: \"" + selectedMsg.response.trim() + "\"");
                        builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                return;
                            }
                        });

                        builder.show();
                }
            }
        });

        findViewById(R.id.backButtonImageView).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                back(null);
            }
        });
    }

    private void finishWithOkResult() {
        mThisLoad.lotCodeMsgSeen = true;
        DataManager.insertLoadToLocalDB(this, mThisLoad);
        setResult(RESULT_OK);
        finish();
    }

    public void back(View v) {
        setResult(RESULT_CANCELED);
        finish();
    }
}

