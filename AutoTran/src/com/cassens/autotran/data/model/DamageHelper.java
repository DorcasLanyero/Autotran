package com.cassens.autotran.data.model;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

//class to facilitate easy damage comparisons to determine if a driver has to contact Edwardsville on delivery
public class DamageHelper {
    public int id;
    public int areaCode;
    public int typeCode;
    public int severityCode;
    public HashMap<String, ArrayList<String>> areaEquivs;
    public HashMap<String, ArrayList<String>> typeEquivs;


    public DamageHelper(Damage damage) {
        this.id = damage.damage_id;
        this.areaCode = Integer.parseInt(damage.getAreaCode("0"));
        this.typeCode = Integer.parseInt(damage.getTypeCode("0"));
        this.severityCode = Integer.parseInt(damage.getSeverityCode("0"));

        areaEquivs = setUpAreaEquivs();
        typeEquivs = setUpTypeEquivs();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }

        if (!(obj instanceof DamageHelper)) {
            return false;
        }
        DamageHelper damageHelper = (DamageHelper)obj;

        if ( (this.typeCode == 14 && this.severityCode == 1)) {
            return areaMatch(this.areaCode, damageHelper.areaCode) && this.typeCode == damageHelper.typeCode &&
                    this.severityCode == damageHelper.severityCode;
        }

        //default check
        return areaMatch(this.areaCode, damageHelper.areaCode) && typeMatch(this.typeCode, damageHelper.typeCode)
                && severityMatch(this.severityCode, damageHelper.severityCode);
    }

    @Override
    public int hashCode() {
        int result = 17;
        result = 31 * result + areaCode;
        result = 31 * result + typeCode;
        result = 31 * result + severityCode;
        return result;
    }

    @Override
    public String toString() {
        return areaCode + "-" + typeCode + "-" + severityCode;
    }

    private boolean areaMatch(int area1, int area2) {
        if (area1 == area2) {
            return true;
        } else if (areaEquivs.containsKey(String.valueOf(area1)) && (areaEquivs.get(String.valueOf(area1)).contains(String.valueOf(area2)))) {
            return true;
        }

        return false;
    }

    private boolean typeMatch (int type1, int type2) {
        if (type1 == type2) {
            return true;
        } else if (typeEquivs.containsKey(String.valueOf(type1)) && (typeEquivs.get(String.valueOf(type1)).contains(String.valueOf(type2)))) {
            return true;
        }

        return false;
    }

    private boolean severityMatch (int severity1, int severity2) {
        if (severity1 == severity2) {
            return true;
        } else if (severity1 != 6 && severity2 != 6 && Math.abs(severity1 - severity2) <= 1) {
            return true;
        }
        return false;
    }

    // This matrix is based on M-22: Finished Vehicle Logistics (FVL) Transportation
    // Damage Handling Processes Standards Guideline Edition 5 issued by the
    // Automotive Industry Action Group (AIAG).
    private HashMap<String, ArrayList<String>> setUpAreaEquivs() {
        areaEquivs = new HashMap<>();

        areaEquivs.put("1", new ArrayList<>(Arrays.asList("23", "37")));
        areaEquivs.put("2", new ArrayList<>(Arrays.asList("99")));
        areaEquivs.put("3", new ArrayList<>(Arrays.asList("5", "42", "86", "22", "92")));
        areaEquivs.put("4", new ArrayList<>(Arrays.asList("6", "86", "92")));
        areaEquivs.put("5", new ArrayList<>(Arrays.asList("3", "42", "86", "22", "92")));
        areaEquivs.put("6", new ArrayList<>(Arrays.asList("4", "86", "92")));
        areaEquivs.put("7", new ArrayList<>(Arrays.asList("52")));
        areaEquivs.put("8", new ArrayList<>(Arrays.asList("52")));
        //areaEquivs.put("9", new ArrayList<>(Arrays.asList("13")));

        //areaEquivs.put("13", new ArrayList<>(Arrays.asList("9")));
        areaEquivs.put("15", new ArrayList<>(Arrays.asList("82")));
        areaEquivs.put("17", new ArrayList<>(Arrays.asList("83")));
        areaEquivs.put("18", new ArrayList<>(Arrays.asList("98", "19", "23")));
        areaEquivs.put("19", new ArrayList<>(Arrays.asList("98", "18", "23")));

        areaEquivs.put("22", new ArrayList<>(Arrays.asList("3", "5")));
        areaEquivs.put("23", new ArrayList<>(Arrays.asList("98", "1", "92", "28", "29", "18", "19", "57", "67", "84")));
        areaEquivs.put("24", new ArrayList<>(Arrays.asList("25")));
        areaEquivs.put("25", new ArrayList<>(Arrays.asList("24")));
        areaEquivs.put("26", new ArrayList<>(Arrays.asList("98")));
        areaEquivs.put("27", new ArrayList<>(Arrays.asList("80")));
        areaEquivs.put("28", new ArrayList<>(Arrays.asList("29", "23", "98")));
        areaEquivs.put("29", new ArrayList<>(Arrays.asList("28", "23", "98")));

        areaEquivs.put("33", new ArrayList<>(Arrays.asList("85", "98")));
        areaEquivs.put("34", new ArrayList<>(Arrays.asList("23", "98")));
        areaEquivs.put("35", new ArrayList<>(Arrays.asList("54")));
        areaEquivs.put("36", new ArrayList<>(Arrays.asList("54")));
        areaEquivs.put("37", new ArrayList<>(Arrays.asList("53", "56", "65", "71", "1", "64")));
        areaEquivs.put("38", new ArrayList<>(Arrays.asList("54", "35")));
        areaEquivs.put("39", new ArrayList<>(Arrays.asList("54", "36")));

        areaEquivs.put("42", new ArrayList<>(Arrays.asList("3", "5")));
        areaEquivs.put("44", new ArrayList<>(Arrays.asList("54")));
        areaEquivs.put("48", new ArrayList<>(Arrays.asList("98")));

        areaEquivs.put("50", new ArrayList<>(Arrays.asList("98")));
        areaEquivs.put("52", new ArrayList<>(Arrays.asList("64", "7", "8")));
        areaEquivs.put("53", new ArrayList<>(Arrays.asList("37")));
        areaEquivs.put("54", new ArrayList<>(Arrays.asList("90", "91", "44", "89", "35", "36", "38", "39")));
        areaEquivs.put("55", new ArrayList<>(Arrays.asList("23", "85","98")));
        areaEquivs.put("56", new ArrayList<>(Arrays.asList("37", "53")));
        areaEquivs.put("57", new ArrayList<>(Arrays.asList("23")));
        areaEquivs.put("58", new ArrayList<>(Arrays.asList("98")));

        areaEquivs.put("61", new ArrayList<>(Arrays.asList("63")));
        areaEquivs.put("64", new ArrayList<>(Arrays.asList("52", "37")));
        areaEquivs.put("65", new ArrayList<>(Arrays.asList("37", "71")));
        areaEquivs.put("66", new ArrayList<>(Arrays.asList("33", "98")));
        areaEquivs.put("67", new ArrayList<>(Arrays.asList("98", "23")));
        areaEquivs.put("68", new ArrayList<>(Arrays.asList("98")));
        areaEquivs.put("69", new ArrayList<>(Arrays.asList("12", "13")));

        areaEquivs.put("70", new ArrayList<>(Arrays.asList("10", "11")));
        areaEquivs.put("71", new ArrayList<>(Arrays.asList("37")));
        areaEquivs.put("72", new ArrayList<>(Arrays.asList("40")));
        areaEquivs.put("73", new ArrayList<>(Arrays.asList("40")));
        areaEquivs.put("74", new ArrayList<>(Arrays.asList("40")));
        areaEquivs.put("75", new ArrayList<>(Arrays.asList("40")));
        areaEquivs.put("76", new ArrayList<>(Arrays.asList("40")));
        areaEquivs.put("77", new ArrayList<>(Arrays.asList("40")));
        areaEquivs.put("78", new ArrayList<>(Arrays.asList("40")));
        areaEquivs.put("79", new ArrayList<>(Arrays.asList("40")));

        areaEquivs.put("82", new ArrayList<>(Arrays.asList("15")));
        areaEquivs.put("83", new ArrayList<>(Arrays.asList("17")));
        areaEquivs.put("84", new ArrayList<>(Arrays.asList("23")));
        areaEquivs.put("85", new ArrayList<>(Arrays.asList("23", "33")));
        areaEquivs.put("86", new ArrayList<>(Arrays.asList("3", "4", "5", "6")));
        areaEquivs.put("89", new ArrayList<>(Arrays.asList("54", "4", "6")));

        areaEquivs.put("90", new ArrayList<>(Arrays.asList("54")));
        areaEquivs.put("91", new ArrayList<>(Arrays.asList("54")));
        areaEquivs.put("92", new ArrayList<>(Arrays.asList("55", "23", "3", "4", "5", "6")));
        areaEquivs.put("93", new ArrayList<>(Arrays.asList("98")));
        areaEquivs.put("94", new ArrayList<>(Arrays.asList("98")));
        areaEquivs.put("95", new ArrayList<>(Arrays.asList("98")));
        areaEquivs.put("96", new ArrayList<>(Arrays.asList("98")));
        areaEquivs.put("97", new ArrayList<>(Arrays.asList("98")));
        areaEquivs.put("98", new ArrayList<>(Arrays.asList("18", "19", "23", "26", "33", "34", "48", "50", "55", "58", "66", "67", "68", "94", "95", "96", "97")));
        areaEquivs.put("93", new ArrayList<>(Arrays.asList("2")));

        return areaEquivs;
    }

    private HashMap<String, ArrayList<String>> setUpTypeEquivs() {
        typeEquivs = new HashMap<>();

        typeEquivs.put("1", new ArrayList<>(Arrays.asList("2", "4")));
        typeEquivs.put("3", new ArrayList<>(Arrays.asList("11", "13")));
        typeEquivs.put("4", new ArrayList<>(Arrays.asList("1", "2", "7")));
        typeEquivs.put("5", new ArrayList<>(Arrays.asList("4", "7")));
        typeEquivs.put("6", new ArrayList<>(Arrays.asList("2", "7", "13")));
        typeEquivs.put("7", new ArrayList<>(Arrays.asList("12", "4", "5", "6")));
        typeEquivs.put("8", new ArrayList<>(Arrays.asList("38")));
        typeEquivs.put("9", new ArrayList<>(Arrays.asList("12")));

        typeEquivs.put("11", new ArrayList<>(Arrays.asList("3", "13")));
        typeEquivs.put("12", new ArrayList<>(Arrays.asList("7", "9")));
        typeEquivs.put("13", new ArrayList<>(Arrays.asList("11", "3")));
        typeEquivs.put("18", new ArrayList<>(Arrays.asList("19", "25", "37", "38")));
        typeEquivs.put("19", new ArrayList<>(Arrays.asList("18", "25", "37", "38")));

        typeEquivs.put("20", new ArrayList<>(Arrays.asList("21", "22", "23")));
        typeEquivs.put("21", new ArrayList<>(Arrays.asList("20", "22", "23")));
        typeEquivs.put("22", new ArrayList<>(Arrays.asList("20", "21", "23")));
        typeEquivs.put("23", new ArrayList<>(Arrays.asList("20", "21", "22")));
        typeEquivs.put("24", new ArrayList<>(Arrays.asList("2", "6", "5", "7", "9", "11", "12")));
        typeEquivs.put("25", new ArrayList<>(Arrays.asList("18", "19")));
        typeEquivs.put("29", new ArrayList<>(Arrays.asList("30")));

        typeEquivs.put("30", new ArrayList<>(Arrays.asList("29")));
        typeEquivs.put("31", new ArrayList<>(Arrays.asList("30")));
        typeEquivs.put("37", new ArrayList<>(Arrays.asList("18", "19")));
        typeEquivs.put("38", new ArrayList<>(Arrays.asList("8", "18", "19")));

        return typeEquivs;
    }
}
