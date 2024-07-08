package com.cassens.autotran.data.model;

import android.content.Context;
import android.preference.PreferenceManager;

import com.cassens.autotran.constants.Constants;
import com.google.gson.GsonBuilder;

public class ShuttleLoadDefaults {
    public String terminalId, terminalText, originText, destinationText;
    public int shuttleMoveId, numVehicles;

    public ShuttleLoadDefaults(String terminalId, String terminalText, String originText, String destinationText, int shuttleMoveId, int numVehicles) {

        //This is a medium-hacky solution to feeding the wrong info into the terminal text.  I'm avoiding
        //yet another db call this way
        if(terminalText.contains(terminalId) && terminalText.contains("-")) {
            terminalText = terminalText.substring(terminalId.length() + new String(" - ").length());
        }

        this.shuttleMoveId = shuttleMoveId;
        this.terminalId = terminalId;
        this.terminalText = terminalText;
        this.originText = originText;
        this.destinationText = destinationText;
        this.numVehicles = numVehicles;
    }

    public static ShuttleLoadDefaults get(Context context) {
        return new GsonBuilder().create().fromJson(
                PreferenceManager.getDefaultSharedPreferences(context)
                        .getString(Constants.PREF_SHUTTLE_LOAD_DEFAULTS, "{}"),
                ShuttleLoadDefaults.class
        );
    }
}