package com.sdgsystems.util;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.text.InputFilter;
import android.preference.PreferenceManager;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.cassens.autotran.R;
import com.cassens.autotran.data.model.Image;
import com.cassens.autotran.data.model.TrendingAlert;
import com.google.android.gms.location.DetectedActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.reflect.TypeToken;

import org.apache.commons.text.WordUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class HelperFuncs {
    public static boolean isNullOrEmpty(String s) {
        return s == null || s.equals("null") || s.isEmpty();
    }

    public static boolean isNullOrWhitespace(String s) {
        return s == null || isNullOrEmpty(s.trim());
    }

    public static String noNullOrWhitespace(String s, String replacement) {
        return isNullOrWhitespace(s) ? replacement : s;
    }

    public static String indentString(String s, int indent) {
        if (indent <= 0) {
            return s;
        }
        String leftPad = String.format("%" + indent + "s", "");
        String lines[] = s.split("\n");
        String newString = "";
        for (int i=0; i < lines.length; i++) {
            if (lines[i].length() == 0) {
                newString += "\n";
            }
            else {
                newString += leftPad + lines[i] + "\n";
            }
        }
        return newString;
    }


    public static String wordWrapString(String s, int lineLength) {
        String lines[] = s.split("\n");
        String newString = "";
        for (int i=0; i < lines.length; i++) {
            if (lines[i].length() <= lineLength) {
                newString += lines[i] + "\n";
            }
            else {
                newString += WordUtils.wrap(lines[i], lineLength, "\n", false) + "\n";
            }
        }
        return newString;
    }

    /**
     * Hide the soft keyboard in the specified activity. Useful if conditional
     * hiding is needed; otherwise AndroidManifest windowSoftInputMode will do.
     */
    public static void hideSoftKeyboard(Activity activity) {
        activity.getWindow().setSoftInputMode(
                WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN
        );
    }

    public static String noNull(String s, String replacement) {
        return ((s == null || s.equalsIgnoreCase("null")) ? replacement : s);
    }

    public static String noNull(String s)
    {
        return noNull(s, "");
    }

    public static Date getTimestamp() {
        return new Date(System.currentTimeMillis());
    }

    public static long dateStringToTimeInMillis(String dateString) {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss z yyyy");
        try {
            Date mDate = sdf.parse(dateString);
            return mDate.getTime();
        } catch (ParseException e) {
            e.printStackTrace();
            return 0;
        }
    }

    public static int stringToInt(String s, int onFailureValue) {
        try {
            return Integer.parseInt(s);
        }
        catch (Exception e) {
            return onFailureValue;
        }
    }

    public static double stringToDouble(String s, double onFailureValue) {
        try {
            return Double.parseDouble(s);
        }
        catch (Exception e) {
            return onFailureValue;
        }
    }

    public static boolean stringToBoolean(String s, boolean onFailureValue) {
        if (s != null) {
            // Convert the input string to lowercase for case-insensitive comparison
            s = s.toLowerCase();
            switch (s) {
                case "y":
                case "yes":
                case "t":
                case "true":
                    return true;
                case "n":
                case "no":
                case "f":
                case "false":
                    return false;
                default:
                    return onFailureValue;
            }
        }
        // If the input string is null, return the onFailureValue
        return onFailureValue;
    }


    public static String getFolderSizeLabel(File file) {
        long size = getFolderSize(file) / 1024; // Get size and convert bytes into Kb.
        if (size >= 1024) {
            return (size / 1024) + " Mb";
        } else {
            return size + " Kb";
        }
    }

    public static long getFolderSize(File file) {
        long size = 0;
        if (file.isDirectory()) {
            for (File child : file.listFiles()) {
                size += getFolderSize(child);
            }
        } else {
            size = file.length();
        }
        return size;
    }

    public static String splitVin(String vin) {
        if (vin == null) {
            return null;
        } else {
            StringBuilder str = new StringBuilder(vin);
            str.insert(10, " ");

            return str.toString();
        }
    }

    public static Bitmap addWatermarkOnBottom(Bitmap originalBitmap, String gText, String subheader, int scale) {
        int watermarkHeight = 30 * scale;
        int textSize = 10 * scale;

        Bitmap newBitmap = Bitmap.createBitmap(originalBitmap.getWidth(),
                originalBitmap.getHeight() + watermarkHeight, Bitmap.Config.ARGB_8888);

        Canvas canvas = new Canvas(newBitmap);
        canvas.drawColor(Color.WHITE);     //background color of bar at the bottom of image
        canvas.drawBitmap(originalBitmap, 0, 0, null);

        DateFormat getTimeZoneShort = new SimpleDateFormat("z", Locale.US);
        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        Calendar calendar = Calendar.getInstance();

        String timestamp = df.format(calendar.getTime()) + " " + getTimeZoneShort.format(Calendar.getInstance().getTime());

        String text = gText + " " + subheader + "\n" + timestamp;

        TextPaint mTextPaint = new TextPaint();
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(Color.BLACK);
        StaticLayout staticLayout = new StaticLayout(text, mTextPaint, newBitmap.getWidth(), Layout.Alignment.ALIGN_CENTER, 1, 1, false);

        int y=(originalBitmap.getHeight());

        canvas.save();
        canvas.translate(0, y);
        staticLayout.draw(canvas);
        canvas.restore();

        originalBitmap.recycle();
        return newBitmap;
    }

    /**
     * Returns a human readable String corresponding to a detected activity type.
     */
    static String getActivityString(Context context, int detectedActivityType) {
        Resources resources = context.getResources();
        switch(detectedActivityType) {
            case DetectedActivity.IN_VEHICLE:
                return resources.getString(R.string.in_vehicle);
            case DetectedActivity.ON_BICYCLE:
                return resources.getString(R.string.on_bicycle);
            case DetectedActivity.ON_FOOT:
                return resources.getString(R.string.on_foot);
            case DetectedActivity.RUNNING:
                return resources.getString(R.string.running);
            case DetectedActivity.STILL:
                return resources.getString(R.string.still);
            case DetectedActivity.TILTING:
                return resources.getString(R.string.tilting);
            case DetectedActivity.UNKNOWN:
                return resources.getString(R.string.unknown);
            case DetectedActivity.WALKING:
                return resources.getString(R.string.walking);
            default:
                return resources.getString(R.string.unidentifiable_activity, detectedActivityType);
        }
    }

    static String detectedActivitiesToJson(ArrayList<DetectedActivity> detectedActivitiesList) {
        Type type = new TypeToken<ArrayList<DetectedActivity>>() {}.getType();
        return new Gson().toJson(detectedActivitiesList, type);
    }

    static ArrayList<DetectedActivity> detectedActivitiesFromJson(String jsonArray) {
        Type listType = new TypeToken<ArrayList<DetectedActivity>>(){}.getType();
        ArrayList<DetectedActivity> detectedActivities = new Gson().fromJson(jsonArray, listType);
        if (detectedActivities == null) {
            detectedActivities = new ArrayList<>();
        }
        return detectedActivities;
    }


    public static String getClearAlpha(int key) {
        //gets a random letter that doesn't sound like another letter.
        ArrayList<String> letterList = new ArrayList<>(Arrays.asList("A","C","F","G","H","I","J","K",
                "L","O","Q","R","S","U","V","W","X","Y","Z"));

        return letterList.get(key % letterList.size());
    }

    public static String getFourLetterCode() {
        //get current time in seconds
        String timeStamp = String.valueOf(TimeUnit.MILLISECONDS.toSeconds(System.currentTimeMillis()));
        //ditch first four digits (don't need them)
        timeStamp = timeStamp.substring(3);


        return getClearAlpha(Integer.parseInt(timeStamp.substring(0,2))) + getClearAlpha(Integer.parseInt(timeStamp.substring(1,3)))
                + getClearAlpha(Integer.parseInt(timeStamp.substring(2,4))) + getClearAlpha(Integer.parseInt(timeStamp.substring(3,5)));
    }

    public static String getEdwardsvilleReadback(String fourLetterKey) {
        long value = Long.valueOf(fourLetterKey, 36);

        return String.valueOf(value % 1000000);
    }

    public static boolean regularBusinessHours() {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeZone(TimeZone.getTimeZone("America/Chicago"));
        boolean business = false;
        int day = calendar.get(Calendar.DAY_OF_WEEK);

        if (day == Calendar.SUNDAY || day == Calendar.SATURDAY) {
            return business;
        }
        int timeOfDay = calendar.get(Calendar.HOUR_OF_DAY);
        //Log.d("NARF", "time: " + timeOfDay);
        if (timeOfDay >= 8 && timeOfDay < 16) {
            business = true;
        }
        return business;
    }

    public static void addAllCapsFilter(EditText editText) {
        InputFilter[] editFilters = editText.getFilters();
        InputFilter[] newFilters = new InputFilter[editFilters.length + 1];
        System.arraycopy(editFilters, 0, newFilters, 0, editFilters.length);
        newFilters[editFilters.length] = new InputFilter.AllCaps();
        editText.setFilters(newFilters);
    }

    public static Image getHiresCopy(Image loResImage) {
        Image hiResImage = new Image();
        hiResImage.delivery_vin_id = loResImage.delivery_vin_id;
        hiResImage.problem_report_guid = loResImage.problem_report_guid;
        hiResImage.inspection_guid = loResImage.inspection_guid;
        hiResImage.load_id = loResImage.load_id;
        hiResImage.delivery_id = loResImage.delivery_id;
        hiResImage.preloadImage = loResImage.preloadImage;
        hiResImage.imageLat = loResImage.imageLat;
        hiResImage.imageLon = loResImage.imageLon;
        hiResImage.filename = loResImage.filename + "_hires";
        hiResImage.retries = loResImage.retries;

        return hiResImage;
    }

    public static void setBoolPref(Context context, String pref, boolean bool) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putBoolean(pref, bool)
                .apply();
    }

    public static void setStringPref(Context context, String pref, String value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(pref, value)
                .apply();
    }

    public static void setLongPref(Context context, String pref, long value) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putLong(pref, value)
                .apply();
    }

    public static boolean getBoolPref(Context context, String pref, boolean defaultVal) {
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean(pref, defaultVal);
    }

    public static String getStringPref(Context context, String pref, String defaultVal) {
        return PreferenceManager.getDefaultSharedPreferences(context).getString(pref, defaultVal);
    }

    public static long getLongPref(Context context, String pref, long defaultVal) {
        return PreferenceManager.getDefaultSharedPreferences(context).getLong(pref, defaultVal);
    }

    public static String dateOnlyString(Date date) {
        return (new SimpleDateFormat("EEE, MMM dd, yyyy")).format(date);
    }

    public static Date addSubtractDays(Date expirationDate, int days) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(expirationDate);
        cal.add(Calendar.DAY_OF_YEAR, days);
        return cal.getTime();
    }

    private static final String SIMPLE_DATE_STRING_PATTERN = "yyyy-MM-dd";

    public static Date simpleDateStringToDate(String string) {
        if (HelperFuncs.isNullOrEmpty(string)) {
            return null;
        }
        try {
            return new SimpleDateFormat(SIMPLE_DATE_STRING_PATTERN).parse(string);
        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static String dateToSimpleDateString(Date date) {
        if (date == null) {
            return null;
        }
        return new SimpleDateFormat(SIMPLE_DATE_STRING_PATTERN).format(date);
    }

    public static Date stringToDateFutureDefault(String string) {
        Date date = simpleDateStringToDate(string);
        if (date == null) {
            //if null, return a future date so we fail safely on expire checks
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.YEAR, 4);
            return calendar.getTime();
        } else {
            return date;
        }
    }

    public static void showAlerts(ArrayList<TrendingAlert> alerts, Activity activity) {
        if (alerts.size() != 0) {
            AlertDialog.Builder builder = new AlertDialog.Builder(activity);
            LayoutInflater inflater = activity.getLayoutInflater();
            final View dialogView = inflater.inflate(R.layout.dialog_trending_alerts, null);
            builder.setView(dialogView);

            TextView alertText = dialogView.findViewById(R.id.alert_view);
            //builder.setTitle("Attention Areas");
            String msg = "Give extra attention to the following areas:";
            for (TrendingAlert alert : alerts) {
                msg +="\n" + alert.order + ". " + alert.alert;
            }
            alertText.setText(msg);
            final Button doneButton = dialogView.findViewById(R.id.done_button);
            Dialog dialog = builder.create();
            doneButton.setOnClickListener(view -> {
                dialog.dismiss();
            });
            dialog.show();
        }
    }

    public static String prettyJson(String uglyJsonString) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonParser jp = new JsonParser();
        JsonElement je = jp.parse(uglyJsonString);
        return gson.toJson(je);
    }
}
