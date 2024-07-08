package com.cassens.autotran.activities;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.View;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SafeDeliveryActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(SafeDeliveryActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    public final static String EXTRA_EXPLANATION = "EXPLANATION";
    public final static String SAFE_AREA = "SAFE";

    private TextView promptExplain;
    private EditText explanation;
    private int explanationMaxLen;
    private TextView charCount;
    private Button continueButton;
    private RadioButton noButton;
    private RadioButton yesButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_safe_delivery);
        promptExplain = (TextView) findViewById(R.id.promptExplain);
        explanation = (EditText) findViewById(R.id.explain);
        charCount = (TextView) findViewById(R.id.charCount);
        updateNotesCharCount();
        continueButton = (Button) findViewById(R.id.btn_continue);
        continueButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                CommonUtility.logButtonClick(log, view);
                continueClicked();
            }
        });
        noButton = (RadioButton) findViewById(R.id.no);
        yesButton = (RadioButton) findViewById(R.id.yes);

        explanationMaxLen = 500;

        noButton.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    showExplain(true);
                } else {
                    showExplain(false);
                }
            }
        });
        this.explanation.addTextChangedListener(notesMessageTextWatcher);
    }

    private void showExplain(boolean show) {
        this.promptExplain.setVisibility(show ? View.VISIBLE : View.GONE);
        this.charCount.setVisibility(show ? View.VISIBLE : View.GONE);
        this.explanation.setVisibility(show ? View.VISIBLE : View.GONE);
        updateNotesCharCount();
    }

    private void updateNotesCharCount() {
        String remaining = explanationMaxLen - explanation.length() + " characters remaining";
        charCount.setText(remaining);
    }

    private final TextWatcher notesMessageTextWatcher = new TextWatcher() {
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        public void onTextChanged(CharSequence s, int start, int before, int count) {
            //This sets a textview to the current length
            updateNotesCharCount();
        }

        public void afterTextChanged(Editable s) {
        }
    };

    private void continueClicked() {
        if (!noButton.isChecked() && !yesButton.isChecked()) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SafeDeliveryActivity.this);
            builder.setTitle("Warning!");
            builder.setMessage("You must answer yes or no!");
            log.debug(Logs.INTERACTION, "Dialog: " + "You must answer yes or no!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    log.debug(Logs.INTERACTION, "safe area yes/no dialog, user clicked 'OK'");
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    log.debug(Logs.INTERACTION, "safe area yes/no dialog, user clicked 'Cancel'");
                }
            });
            builder.setCancelable(true);
            builder.create().show();
            return;
        }
        if (noButton.isChecked() && HelperFuncs.isNullOrEmpty(explanation.getText().toString())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(SafeDeliveryActivity.this);
            builder.setTitle("Warning!");
            builder.setMessage("You must enter an explanation!");
            log.debug(Logs.INTERACTION, "Dialog: " + "You must enter an explanation!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    log.debug(Logs.INTERACTION, "safe area explanation dialog, user clicked 'OK'");
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    log.debug(Logs.INTERACTION, "safe area explanation dialog, user clicked 'Cancel'");
                }
            });
            builder.setCancelable(true);
            builder.create().show();
            return;
        }

        Intent i = new Intent();
        i.putExtra(SAFE_AREA, !(explanation.getVisibility() == View.VISIBLE));
        i.putExtra(EXTRA_EXPLANATION, explanation.getText().toString());
        this.explanation.setMovementMethod(new ScrollingMovementMethod());
        setResult(RESULT_OK, i);
        finish();
    }
}
