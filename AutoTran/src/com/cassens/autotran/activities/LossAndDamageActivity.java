package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputType;
import android.text.TextWatcher;
import android.text.method.ScrollingMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LossAndDamageActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(LossAndDamageActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    public final static String EXTRA_TERM_CALLED = "HOME_TERMINAL_CALLED";
    public final static String EXTRA_SPOKE_WITH = "SPOKE_WITH";
    public final static String EXTRA_EXPLANATION = "EXPLANATION";

    private TextView promptSpokeWith;
    private EditText spokeWith;
    private EditText explanation;
    private int explanationMaxLen;
    private TextView charCount;
    private Button okButton;
    private Activity activity;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        activity = this;
        setContentView(R.layout.activity_loss_and_damage);

        findViewById(R.id.img_back).setVisibility(View.GONE);
        findViewById(R.id.img_menu).setVisibility(View.GONE);

        okButton = (Button)findViewById(R.id.btn_notes_save);
        okButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doneClicked();
            }
        });

        promptSpokeWith = (TextView) findViewById(R.id.promptSpokeWith);
        spokeWith = (EditText) findViewById(R.id.spokeWith);
        spokeWith.setInputType(InputType.TYPE_NULL);
        spokeWith.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View view, boolean hasFocus) {
                if (hasFocus) {
                    showGetFullNameDialog();
                }
            }
        });
        spokeWith.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                showGetFullNameDialog();
            }
        });
        showSpokeWith(false);

        ((RadioButton) findViewById(R.id.no)).setChecked(true);
        ((RadioButton) findViewById(R.id.yes)).setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                if (b) {
                    showSpokeWith(true);
                } else {
                    showSpokeWith(false);
                }
            }
        });

        explanation = (EditText) findViewById(R.id.damage_explanation_note);
        explanation.setMaxLines(20);
        // Surprisingly, there's no way to get the maxLen of an EditText field programmatically,
        // so we have to retrieve it from a resource.
        explanationMaxLen = getResources().getInteger(R.integer.max_delivery_note_length);
        explanation.setHint("Explain damages here (" + explanationMaxLen + " characters max)...");
        charCount = (TextView) findViewById(R.id.charCount);
        updateNotesCharCount();
        explanation.addTextChangedListener(notesMessageTextWatcher);
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

    @Override
    public void onBackPressed() {
        log.debug(Logs.INTERACTION, "user pressed 'back'");
        String msg;
        String okButtonLabel = okButton.getText().toString();
        if (explanation.length() == 0) {
            msg = "You must enter an explanation and press " + okButtonLabel + " to continue.";
        }
        else {
            msg = "You must press " + okButtonLabel + " to continue.";
        }
        CommonUtility.simpleMessageDialog(activity, msg);
        log.debug(Logs.INTERACTION, "message shown: " + msg);
    }

    private void doneClicked() {
        if (spokeWith.getVisibility() == View.VISIBLE && HelperFuncs.isNullOrEmpty(spokeWith.getText().toString())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(LossAndDamageActivity.this);
            builder.setTitle("Warning!");
            builder.setMessage("You must enter the name of who you talked to!");
            log.debug(Logs.INTERACTION, "Dialog: " + "enter the name of who you talked to!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    log.debug(Logs.INTERACTION, "required contact name dialog, user clicked 'OK'");
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    log.debug(Logs.INTERACTION, "required contact name dialog, user clicked 'Cancel'");
                }
            });
            builder.setCancelable(true);
            builder.create().show();
            return;
        }

        if (HelperFuncs.isNullOrEmpty(explanation.getText().toString())) {
            AlertDialog.Builder builder = new AlertDialog.Builder(LossAndDamageActivity.this);
            builder.setTitle("Warning!");
            builder.setMessage("You must enter an explanation for damages!");
            log.debug(Logs.INTERACTION, "Dialog: " + "You must enter an explanation for damages!");
            builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    log.debug(Logs.INTERACTION, "damage explanation dialog, user clicked 'OK'");
                }
            });
            builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    log.debug(Logs.INTERACTION, "damage explanation dialog, user clicked 'Cancel'");
                }
            });
            builder.setCancelable(true);
            builder.create().show();
            return;
        }

        Intent i = new Intent();
        i.putExtra(EXTRA_TERM_CALLED, spokeWith.getVisibility() == View.VISIBLE);
        i.putExtra(EXTRA_SPOKE_WITH, spokeWith.getText().toString());
        i.putExtra(EXTRA_EXPLANATION, explanation.getText().toString());
        this.explanation.setMovementMethod(new ScrollingMovementMethod());
        setResult(RESULT_OK, i);
        finish();
    }

    private void showSpokeWith(boolean show) {
        this.promptSpokeWith.setVisibility(show ? View.VISIBLE : View.GONE);
        this.spokeWith.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showGetFullNameDialog() {
        LayoutInflater inflater = this.getLayoutInflater();
        final View dialogView = inflater.inflate(R.layout.dialog_full_name, null);

        TextView textView = (TextView) dialogView.findViewById(R.id.person_refusing_tv);

        textView.setText("Enter the name of the person you spoke with");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView)
                .setCancelable(true);

        final Button doneButton = (Button) dialogView.findViewById(R.id.done_button);
        final Button cancelButton = (Button) dialogView.findViewById(R.id.cancel_button);

        // We use firstNameTv for full name in this case
        final TextView firstNameTv = (TextView) dialogView.findViewById(R.id.first_name_edit_text);
        final TextView lastNameTv = (TextView) dialogView.findViewById(R.id.last_name_edit_text);
        firstNameTv.setHint("Spoke With...");
        lastNameTv.setVisibility(View.GONE);

        if(spokeWith.getText().toString().trim().contains(" ")) {
            firstNameTv.setText(spokeWith.getText());
        }

        final Dialog dialog;

        dialog = builder.create();

        doneButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (firstNameTv.getText().toString().trim().isEmpty()/* || lastNameTv.getText().toString().trim().isEmpty() */) {
                    CommonUtility.showText("Must enter the name of the person you spoke with");
                } else {
                    spokeWith.setText(firstNameTv.getText().toString().trim()/* + " " + lastNameTv.getText().toString().trim()*/);
                    dialog.dismiss();
                    getCurrentFocus().clearFocus();
                }
            }
        });

        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                CommonUtility.logButtonClick(log, v);
                dialog.dismiss();
            }
        });

        dialog.setCanceledOnTouchOutside(false);
        dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
        dialog.show();
    }

    public static void hideKeyboard(Activity activity) {
        InputMethodManager imm = (InputMethodManager) activity.getSystemService(Activity.INPUT_METHOD_SERVICE);
        //Find the currently focused view, so we can grab the correct window token from it.
        View view = activity.getCurrentFocus();
        //If no view currently has focus, create a new one, just so we can grab a window token from it
        if (view == null) {
            view = new View(activity);
        }
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }


}
