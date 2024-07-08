package com.cassens.autotran.backendpoc;

import java.util.Date;

public class PoCDealer {
    String customerNum;
    String mfg;
    String customerName;
    String city;
    String state;
    String address;
    String zip;
    String contact;
    String email;
    String phone;
    int monam = -1, tueam = -1, wedam = -1, thuam = -1, friam = -1, satam = -1, sunam = -1;
    int monpm = -1, tuepm = -1, wedpm = -1, thupm = -1, fripm = -1, satpm = -1, sunpm = -1;
    String afterHours;
    String comments;
    boolean highClaims;
    boolean alwaysUnattended;
    boolean photosOnUnattended;
    boolean lotLocateRequired;
    int lotCodeId;  // TODO: Might need work to map old lot code ID with new lookup tables
    String lastUpdate; // TODO: Make this a Date to match 2.x implementation?
    String countryCode = "US";

    PoCDealer() {
        customerNum = "9999901";
        mfg = "TO";
        customerName = "TEST TOYOTA #1";
    }
}
