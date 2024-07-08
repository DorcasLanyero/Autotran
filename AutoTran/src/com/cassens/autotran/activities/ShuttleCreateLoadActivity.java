package com.cassens.autotran.activities;

import static android.view.View.GONE;
import static android.view.View.INVISIBLE;
import static android.view.View.VISIBLE;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.ShuttleLoadDefaults;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.data.model.lookup.Terminal;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.google.gson.GsonBuilder;
import com.sdgsystems.util.HelperFuncs;
import com.cassens.autotran.data.adapters.HighlightArrayAdapter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

public class ShuttleCreateLoadActivity extends AutoTranActivity {
    private static final Logger log = LoggerFactory.getLogger(ShuttleCreateLoadActivity.class.getSimpleName());

    @Override
    public Logger getLogger() {
        return log;
    }

    private static final int REQ_CODE_TERMINAL = 0;
    private static final int REQ_CODE_ORIGIN = 1;
    private static final int REQ_CODE_DESTINATION = 2;
    private static final int REQ_CODE_BUILD = 3;
    private static final int TAG_CODE = 0;
    private static final String TAG = "ShuttleLoad";

    TextView terminalNumber, driverNumber, truckNumber, origin, originLabel, destination, destinationLabel;
    EditText trailerNumber;
    protected Button numVehiclesButton;
    protected Spinner numVehiclesSpinner;
    ShuttleMove move;
    SharedPreferences prefs;
    boolean startWithDefaults = false;

    private final String[] vehicles = generateNumVehiclesSelectionList();
    private String[] generateNumVehiclesSelectionList() {
        String [] vehicles = new String[Constants.MAX_VEHICLES_ON_TRUCK];
        for (int i = 0; i < Constants.MAX_VEHICLES_ON_TRUCK; i++) {
            vehicles[i] = String.valueOf(i+1);
        }
        return vehicles;
    }

    @TargetApi(Build.VERSION_CODES.HONEYCOMB_MR1)
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_create_shuttle_load);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);

        Bundle bundle = getIntent().getExtras();
        if (bundle != null) {
            startWithDefaults = bundle.getBoolean("startWithDefaults");
        }

        driverNumber = findViewById(R.id.driverNumber);
        driverNumber.setText(CommonUtility.getDriverNumber(this));
        log.debug(Logs.INTERACTION, "Driver ID: " + driverNumber.getText());

        truckNumber = findViewById(R.id.truckNumber);
        truckNumber.setText(TruckNumberHandler.getTruckNumber(this));
        log.debug(Logs.INTERACTION, "Truck ID: " + truckNumber.getText());

        String defaultTrailerNum = String.valueOf(TruckNumberHandler.getTruckNumberInt(this) + 1);
        trailerNumber = findViewById(R.id.trailerNumber);
        trailerNumber.setText(prefs.getString(getTrailerIdPrefName(this), defaultTrailerNum));
        log.debug(Logs.INTERACTION, "Trailer ID:" + trailerNumber.getText());

        // Spinner widget doesn't allow us to start with no value easily, so we spoof it
        // by using a button for the first click, then making the Spinner widget visible.
        numVehiclesButton = findViewById(R.id.numVehiclesButton);
        numVehiclesButton.setVisibility(View.VISIBLE);
        numVehiclesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                numVehiclesSpinner.performClick();
                numVehiclesButton.setVisibility(GONE);
                numVehiclesSpinner.setVisibility(View.VISIBLE);
            }
        });

        numVehiclesSpinner = findViewById(R.id.numVehiclesSpinner);
        numVehiclesSpinner.setVisibility(GONE);

        HighlightArrayAdapter numVehiclesSpinnerAdapter = new HighlightArrayAdapter(this, R.layout.spinner_item, vehicles);
        numVehiclesSpinner.setAdapter(numVehiclesSpinnerAdapter);

        numVehiclesSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                //CommonUtility.showText("Selected " + position);
                numVehiclesSpinnerAdapter.setSelection(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        origin = findViewById(R.id.origin);
        originLabel = findViewById(R.id.originLabel);
        destination = findViewById(R.id.destination);
        destinationLabel = findViewById(R.id.destinationLabel);
        terminalNumber = findViewById(R.id.terminal);

        log.debug(Logs.INTERACTION, "Shuttle Load");

        ShuttleLoadDefaults defaults = ShuttleLoadDefaults.get(this);
        boolean defaultsExist = DataManager.getShuttleMoves(this, defaults.terminalId, defaults.originText, defaults.destinationText).size() > 0;

        move = DataManager.getShuttleMoveForLoad(this, defaults.shuttleMoveId);

        if (defaultsExist && startWithDefaults && !HelperFuncs.isNullOrEmpty(defaults.terminalId)) {
            setTerminalNum(defaults.terminalId, defaults.terminalText);
            origin.setText(defaults.originText);
            destination.setText(defaults.destinationText);
            if (defaults.numVehicles > 0) {
                numVehiclesSpinner.setSelection(numVehiclesSpinnerAdapter.getPosition(String.valueOf(defaults.numVehicles)));
                numVehiclesButton.setVisibility(GONE);
                numVehiclesSpinner.setVisibility(View.VISIBLE);
            }
        } else {
            Terminal appDefaultTerminal = DataManager.getTerminal(this, CommonUtility.getDefaultTerminalNum(this));
            boolean defaultTerminalHasShuttles = false;
            if (appDefaultTerminal != null) {
                ArrayList<Terminal> terminalList = (ArrayList<Terminal>)DataManager.getShuttleTerminalList(this);
                for (Terminal terminal: terminalList) {
                    if (terminal.terminal_id == appDefaultTerminal.terminal_id) {
                        defaultTerminalHasShuttles = true;
                        break;
                    }
                }
            }
            if (defaultTerminalHasShuttles) {
                setTerminalNum(String.valueOf(appDefaultTerminal.terminal_id), appDefaultTerminal.description);
            }
            else {
                origin.setVisibility(INVISIBLE);
            }
            destination.setVisibility(INVISIBLE);
            setOriginDestinationVisibility();
        }


        trailerNumber.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {

            }

            @Override
            public void afterTextChanged(Editable editable) {
                String numStr = editable.toString();
                if (numStr.isEmpty()) {
                    trailerNumber.setError(null);
                    return;
                }
                if (!numStr.matches("^[0-9]+$")) {
                    trailerNumber.setError(getString(R.string.validation_trailerNum_all_digits));
                    return;
                }

                int num;
                try {
                    num = Integer.parseInt(numStr);
                } catch (NumberFormatException e) {
                    trailerNumber.setError(getString(R.string.validation_trailerNum_general));
                    trailerNumber.setText("");
                    return;
                }
                if (num < 4000) {
                    trailerNumber.setError(getString(R.string.validation_trailerNum_min));
                    return;
                }
                if (num >= 100000) {
                    trailerNumber.setError(getString(R.string.validation_trailerNum_max));
                    return;
                }
                if ((num % 2) == 1) {
                    trailerNumber.setError(getString(R.string.validation_trailerNum_even));
                    return;
                }
                trailerNumber.setError(null);
            }
        });
    }

    private String getTrailerIdPrefName(Context context) {
        return String.format("%s_%s_%s", Constants.TRAILER_ID_PREF_PREFIX,
                CommonUtility.getDriverNumber(context),
                TruckNumberHandler.getTruckNumber(context));
    }

    public void pickTerminal(View v) {
        Intent i = new Intent(ShuttleCreateLoadActivity.this, TerminalCodeList.class);
        i.putExtra(TerminalCodeList.EXTRA_IS_SHUTTLE_LIST, true);
        startActivityForResult(i, REQ_CODE_TERMINAL);
    }

    public void pickOrigin(View v) {
        Intent intent = new Intent(ShuttleCreateLoadActivity.this, ShuttleOriginListActivity.class);

        intent.putExtra("terminal", terminalNumber.getTag().toString());
        startActivityForResult(intent, REQ_CODE_ORIGIN);
    }

    public void pickDestination(View v) {
        Intent intent = new Intent(ShuttleCreateLoadActivity.this, ShuttleDestinationListActivity.class);

        intent.putExtra("terminal", terminalNumber.getTag().toString());
        intent.putExtra("origin", origin.getText());
        startActivityForResult(intent, REQ_CODE_DESTINATION);
    }

    private int getVehicleCount() {
        String countStr = numVehiclesSpinner.getSelectedItem().toString();
        int count;
        int max = Constants.MAX_VEHICLES_ON_TRUCK;
        try {
            count = countStr.length() > 0 ? Integer.parseInt(countStr) : 1;
        } catch (NumberFormatException ex) {
            return max;
        }

        if (count < 1) {
            return 1;
        } else if (count > max) {
            return max;
        } else {
            return count;
        }
    }

    boolean processingStartClick = false;

    @Override
    public void onResume() {
        super.onResume();
        processingStartClick = false;
    }

    private String generateLoadNumber(String driverNumber) {
        return String.format("SL-%s-%s", driverNumber,
                    new SimpleDateFormat("yyyyMMddHHmmss").format(new Date()));
    }

    public void startClicked(View v) {
        if (processingStartClick) {
            log.debug(Logs.INTERACTION, "got extra start click, throwing it out since we were already processing");
            return;
        }

        processingStartClick = true;

        CommonUtility.logButtonClick(log, v);
        if (terminalNumber.getText().toString().isEmpty() || origin.getText().toString().isEmpty()
                || destination.getText().toString().isEmpty()) {
            CommonUtility.showText("Please specify an origin and destination");
        }
        else if (numVehiclesSpinner.getVisibility() != View.VISIBLE) {
            CommonUtility.showText("Please specify the vehicle count");
        }
        else if (origin.getText().toString().equals(destination.getText().toString())
                && !origin.getText().toString().toLowerCase().contains("belvidere")) {
            log.debug(Logs.INTERACTION, "Origin and destinations match, aborting");
            CommonUtility.showText("Please specify an origin and destination that do not match");
        }
        else if (trailerNumber.getText().toString().isEmpty()) {
            log.debug(Logs.INTERACTION, "No trailer number specified, aborting");
            CommonUtility.showText("Please specify a trailer number");
        }
        else if (destination.getText().toString().toLowerCase().contains("voltz")) {
            log.debug(Logs.INTERACTION, "moving to voltz");
            CommonUtility.showText("Moving to Voltz, please choose a load...");
        } else {
            Load newLoad = new Load();

            newLoad.shuttleLoad = true;
            newLoad.loadType = Constants.LOAD_SHUTTLE;

            if (move != null && move.orgDestString != null && move.shuttleMoveId != -1) {
                log.debug(Logs.DEBUG, "Setting the move id = " + move.shuttleMoveId);
                newLoad.shuttleMove = move;
            } else {
                CommonUtility.showText("No known Shuttle Move selected, searching based on description");
                newLoad.shuttleMove = DataManager.getShuttleMoves(this, terminalNumber.getTag().toString(),
                        origin.getText().toString(), destination.getText().toString()).get(0);
            }
            newLoad.originTerminal = newLoad.shuttleMove.terminal;

            SharedPreferences.Editor editor = prefs.edit();
            ShuttleLoadDefaults defaults = new ShuttleLoadDefaults(
                    terminalNumber.getTag().toString(),
                    terminalNumber.getText().toString(),
                    origin.getText().toString(),
                    destination.getText().toString(),
                    newLoad.shuttleMove.shuttleMoveId,
                    Integer.parseInt(numVehiclesSpinner.getSelectedItem().toString())
            );

            editor.putString(Constants.PREF_SHUTTLE_LOAD_DEFAULTS, new GsonBuilder().create().toJson(defaults, ShuttleLoadDefaults.class));
            editor.putInt(Constants.PREF_VEHICLE_COUNT, getVehicleCount());
            editor.commit();

            log.debug(Logs.INTERACTION, "new shuttle load--origin: " + origin.getText().toString() +
                    " destination: " + destination.getText().toString() + " id: " +
                    newLoad.shuttleMove.shuttleMoveId + " count: " + getVehicleCount());

            newLoad.driverNumber = driverNumber.getText().toString();
            newLoad.trailerNumber = trailerNumber.getText().toString();
            newLoad.truckNumber = truckNumber.getText().toString();
            newLoad.loadNumber = generateLoadNumber(newLoad.driverNumber);

            HelperFuncs.setStringPref(this, getTrailerIdPrefName(this), trailerNumber.getText().toString());

            User driver = DataManager.getUserForDriverNumber(this, driverNumber.getText().toString());
            if (driver != null) {
                newLoad.driver_id = driver.user_id;

                //Create the delivery object, but there aren't any VINs in it yet...
                Delivery shuttleLoadDelivery = new Delivery();
                shuttleLoadDelivery.shuttleLoad = true;

                newLoad.deliveries.add(shuttleLoadDelivery);

                newLoad.load_id = (int) DataManager.insertLoadToLocalDB(this, newLoad);

                Intent intent = new Intent(ShuttleCreateLoadActivity.this, ShuttleBuildLoadActivity.class);
                Bundle bundle = getIntent().getExtras();
                bundle.putInt(Constants.CURRENT_LOOKUP_ID, newLoad.load_id);
                bundle.putInt(Constants.PREF_VEHICLE_COUNT, getVehicleCount());
                intent.putExtras(bundle);
                startActivityForResult(intent, REQ_CODE_BUILD);
                return;
            } else {
                log.debug(Logs.INTERACTION, "driver number not found");
                CommonUtility.showText("Driver number not found...");
            }
        }
        processingStartClick = false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            if (requestCode == REQ_CODE_TERMINAL) {
                String code = data.getStringExtra("code");
                String description = data.getStringExtra("description");
                log.debug(Logs.INTERACTION, "setting terminal " + code + description);
                String currentCode = "";
                if (terminalNumber.getTag() != null) {
                    currentCode = terminalNumber.getTag().toString();
                }
                if (currentCode.equals(code) && !currentCode.isEmpty()) {
                    log.debug(Logs.DEBUG, "the terminal code didn't change...");
                }
                else {
                    setTerminalNum(code, description);
                    log.debug(Logs.INTERACTION, "setting terminal code " + code);
                    origin.setText("");
                    destination.setText("");
                    //origin.setVisibility(VISIBLE);
                    setOriginDestinationVisibility();
                }
            } else if (requestCode == REQ_CODE_ORIGIN) {
                String code = data.getStringExtra("origin");
                if (origin.getText().toString().equals(code) && !(origin.getText().toString().trim().length() == 0)) {
                    log.debug(Logs.DEBUG, "the origin code didn't change...");
                } else {
                    origin.setText(code);
                    log.debug(Logs.INTERACTION, "setting origin code " + code);
                    destination.setText("");
                    //destination.setVisibility(VISIBLE);
                    move = null;
                    setOriginDestinationVisibility();
                }
            } else if (requestCode == REQ_CODE_DESTINATION) {
                int moveId = data.getIntExtra("destinationId", -1);

                if(moveId != -1) {
                    move = DataManager.getShuttleMoveForLoad(getApplicationContext(), moveId);
                    destination.setText(move.destination);
                    log.debug(Logs.INTERACTION, "destination set " + destination.getText());
                } else {
                    CommonUtility.showText("No Shuttle move selected");
                    log.debug(Logs.INTERACTION, "No shuttle move selected when ok clicked");
                }
            } else if (requestCode == REQ_CODE_BUILD) {
                log.debug(Logs.INTERACTION, "build shuttle load");
                setResult(RESULT_OK);
                finish();
            }
        }
    }

    private void setTerminalNum(String code, String description) {
        terminalNumber.setText(code + " - " + description);
        terminalNumber.setTag(code);

        move = null;
    }

    public void back(View v) {
        log.debug(Logs.INTERACTION, "Back pressed");
        onBackPressed();
    }

    public void setOriginDestinationVisibility() {
        int originVisibility = VISIBLE;
        int destinationVisibility = VISIBLE;
        if (terminalNumber.getText().toString().isEmpty()) {
            originVisibility = INVISIBLE;
            destinationVisibility = INVISIBLE;
            destination.setText("");
        }
        else if (origin.getText().toString().isEmpty()) {
            destinationVisibility = INVISIBLE;
        }
        origin.setVisibility(originVisibility);
        originLabel.setVisibility(originVisibility);
        /*
        if (originVisibility == INVISIBLE) {
            origin.setText("");
        } */
        destination.setVisibility(destinationVisibility);
        destinationLabel.setVisibility(destinationVisibility);
        /*if (destinationVisibility == INVISIBLE) {
            destination.setText("");
        } */
    }
}
