package com.cassens.autotran;

import android.util.Log;

import org.slf4j.Logger;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;

public class Logs {
    public static final Marker UPLOAD = MarkerFactory.getMarker("UPLOAD");
    public static final Marker DISPATCH = MarkerFactory.getMarker("DISPATCH");
    public static final Marker DISPATCH_UPLOAD = MarkerFactory.getMarker("DISPATCH_UPLOAD");
    public static final Marker HIGH_LEVEL = MarkerFactory.getMarker("HIGH_LEVEL");
    public static final Marker INTERACTION = MarkerFactory.getMarker("INTERACTION");
    public static final Marker TRANSACTIONS = MarkerFactory.getMarker("TRANSACTIONS");
    public static final Marker EXCEPTIONS = MarkerFactory.getMarker("EXCEPTIONS");
    public static final Marker SIGNATURES = MarkerFactory.getMarker("SIGNATURES");
    public static final Marker DAMAGES = MarkerFactory.getMarker("DAMAGES");
    public static final Marker DEBUG = MarkerFactory.getMarker("DEBUG");
    public static final Marker DATAMANAGER = MarkerFactory.getMarker("DATAMANAGER");
    public static final Marker UPGRADES = MarkerFactory.getMarker("UPGRADES");
    public static final Marker DELETES = MarkerFactory.getMarker("DELETES");
    public static final Marker EVENT_BUS = MarkerFactory.getMarker("EVENTBUS");
    public static final Marker PICCOLO_IO = MarkerFactory.getMarker("PICCOLO_IO");
    public static final Marker BACKEND_POC = MarkerFactory.getMarker("BACKEND_POC");

    private static final boolean shouldLogcat = /*false &&*/ BuildConfig.PRODUCTION == null || BuildConfig.PRODUCTION == false;
    public static void plusLogcat(Logger log, Marker marker, String message) {
        log.debug(Logs.EVENT_BUS, message);

        if(shouldLogcat) {
            Log.d("EventBusManager", message);
        }
    }
}