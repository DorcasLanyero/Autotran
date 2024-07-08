package com.cassens.autotran.hardware;

import static com.google.android.gms.flags.impl.SharedPreferencesFactory.getSharedPreferences;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.util.Log;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.BuildConfig;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.GlobalState;
import com.cassens.autotran.Logs;
import com.cassens.autotran.data.remote.tasks.SendPiccoloDockedTask;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.felhr.usbserial.UsbSerialDevice;
import com.felhr.usbserial.UsbSerialInterface;
import com.sdgsystems.util.HelperFuncs;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Iterator;

/****
    Manage the Piccolo device using serial interface.

    The Piccolo device accepts the following serial commands. There is a pdf file with a fuller
    specification ind the shared Google Drive folder "AutoTran Support/Piccolo."

      o "No move" behavior

        When "no move" behavior is on, device will stop sending updates to the server when it
        detects that it's not moving. When testing in the office, we turn "no move" off so that
        we get continue to get refreshes (default refresh frequency is 2 minutes).

        AT*WSGPSNMCFG=0 - Turns off "no move" and sends data every 2 minutes (1 turns it back on)

        AT*WSGPSNMCFG?  - get current settings if you want to reset them later

      o Wi-Fi network settings

        Gets and sets the Wi-Fi settings. There are multiple comma-separated fields. If fields
        are left off at the end, those settings will be left unaffected by the command; however,
        values must be supplied for all arguments up to and including the last field specified.

        The first field is set to 0 to turn off Wi-Fi for the device and 3 to turn it on (see
        spec for more info). The second field is the SSID of the Piccolo Wi-Fi. On Cassens devices
        the SSID is set to the string WLI followed by the truck number, so by retrieving it, we can
        get the number of the truck where the Piccolo is installed. The third field is the SSID
        type. 0 specifies that the SSID name is set as configured in the SSID argument; 1 means
        that SSID is set to "WLI" plus the serial number of the device. For Cassens, this should
        always be set to 0.

        AT*WSWIFINW=0 to turn off
        AT*WSWIFINW=3,WLI10999,0 to turn on and set SSID for truck number 10999
        AT*WSWIFINW=0,WLI10999,0 to turn off but set number
        AT*WSWIFINW=3 to turn back on with previous settings

        Note: Cassens currently turns Wi-Fi off in all trucks but sets the SSID to indicate the
              truck number

      o Get GPS coordinates

        Command: AT*WTPOSITION?

        Example return value:
        AT*WTPOSITION?maps.google.com/maps?q=35.583615,-78.586080UTC=20/04/2023 12:22:07V=0.0km/hOK

      o Miscellaneious

        AT*WCSAVE - saves settings after change (otherwise settings are lost on reset)

        AT*WCUNITRESET - resets the unit

 ****/

public class PiccoloManager {
    private static final Logger log = LoggerFactory.getLogger(PiccoloManager.class.getSimpleName());
    public static final int PICCOLO_VENDOR_ID = 5824;
    public static final int PICCOLO_PRODUCT_ID = 1770;

    // Error Codes
    public static final int PICCOLO_NEVER_DOCKED = -99;
    public static final int PICCOLO_CONFIRMED_UNDOCK = -1; // Got USB device removed notice
    public static final int PICCOLO_INFERRED_UNDOCK = -2; // Unplugged, no device removed notice
    public static final int PICCOLO_DEVICE_NOT_DETECTED = -3;
    public static final int PICCOLO_OPEN_ERROR = -4;
    public static final int PICCOLO_CONNECT_ERROR = -5;
    public static final int PICCOLO_PERMISSION_ERROR = -6;
    public static final int PICCOLO_IO_ERROR = -7;
    public static final int PICCOLO_NULL_PACKET_STORM_DETECTED = -8;
    public static final int PICCOLO_TRUCK_NUM_NOT_SET = -100;
    public static final int PICCOLO_TRUCK_NUM_INVALID = -101;
    public static final int PICCOLO_LOCATION_NO_DATA = -200;
    public static final int PICCOLO_LOCATION_BAD_COORDINATES = -201;
    public static final int PICCOLO_LOCATION_BAD_SPEED_VALUE = -202;
    public static final int PICCOLO_LOCATION_PARSE_ERROR = -203;

    private static final String PICCOLO_CMD_TEXT_SETTINGS = "AT*WTTEXT";
    private static final String PICCOLO_CMD_GET_WIFI_INFO = "AT*WSWIFINW?";
    private static final String PICCOLO_CMD_GET_GPS_POSITION = "AT*WTPOSITION?";
    private static final String PICCOLO_CMD_TERMINATOR = "\nOK\n";
    private static final CharSequence TRUCK_WIFI_RESPONSE_PREFIX = "WLI";
    private static final CharSequence POSITION_RESPONSE_PREFIX = "maps.google.com/maps?q=";

    private static UsbDeviceConnection sConnection = null;
    private static UsbSerialDevice sSerial = null;

    private static void closeUsbConnectionIfOpen() {
        try {
            if (sSerial != null) {
                sSerial.close();
            }
            if (sConnection != null) {
                sConnection.close();
            }
        } catch (Exception ex) {
            // Do nothing
        }
        finally {
            sSerial = null;
            sConnection = null;
        }
    }

    private static boolean mIsDocked = false;
    private static int sNullPacketsReceived;
    private static long lastCallTime = 0;
    private static final int MAX_NULL_PACKETS = 200;

    public static void refreshPiccoloTruckNumberAsNeeded(Context context) {
        if (isPlugged(context)) {
            log.debug(Logs.PICCOLO_IO, "Handheld is plugged in. Requesting truck # from Piccolo.");
            requestTruckNum(context);
            return;
        }
        // If handheld is not plugged in, the Piccolo truck number should be < 0.  If it's not,
        // set it to PICCOLO_NO_STATUS_AVAILABLE; otherwise, don't change it, since it might
        // contain a more useful error code.
        try {
            int truckNum = Integer.parseInt(TruckNumberHandler.getPiccoloTruckNumber(context, true));
            if (truckNum >= 0) {
                log.debug(Logs.PICCOLO_IO, "Handheld not plugged. Changing truck # from " + truckNum + " to " + PICCOLO_INFERRED_UNDOCK);
                setPiccoloTruckNumber(PICCOLO_INFERRED_UNDOCK);
            }
            else {
                log.debug(Logs.PICCOLO_IO, "Handheld not plugged. Truck number already negative: " + truckNum);
            }
        }
        catch (NumberFormatException nfe) {
            log.debug(Logs.PICCOLO_IO, "Handheld not plugged. Setting truck # from " + TruckNumberHandler.getPiccoloTruckNumber(context, true) + " to " + PICCOLO_INFERRED_UNDOCK);
            setPiccoloTruckNumber(PICCOLO_INFERRED_UNDOCK);
        }
        if (isDocked()) {
            log.debug(Logs.PICCOLO_IO, "Handheld was not plugged in, but isDocked is true. Setting it to false.");
            setIsDocked(false);
        }
        else {
            log.debug(Logs.PICCOLO_IO, "Handheld was not plugged in. No need to check for Piccolo.");
        }
    }

    private static boolean doError = true;

    private static void setPiccoloTruckNumber(int truckNumber) {
        // Set the Piccolo truck number SharedPreference to truckNumber. Note that truckNumber
        // might be an error code.
        TruckNumberHandler.setPiccoloTruckNumber(AutoTranApplication.getAppContext(), Integer.toString(truckNumber));
    }

    public static boolean isDocked() {
        return mIsDocked;
    }

    public static void setIsDocked(boolean isDocked) {
        PiccoloManager.mIsDocked = isDocked;

        log.debug(Logs.PICCOLO_IO, "Setting status to " + (isDocked ? "docked" : "undocked"));
        if (!isDocked) {
            closeUsbConnectionIfOpen();
        }
    }

    public static boolean isPlugged(Context context) {
        boolean isPlugged= false;
        Intent intent = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        int plugged = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, -1);
        isPlugged = plugged == BatteryManager.BATTERY_PLUGGED_AC || plugged == BatteryManager.BATTERY_PLUGGED_USB;
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN) {
            isPlugged = isPlugged || plugged == BatteryManager.BATTERY_PLUGGED_WIRELESS;
        }
        return isPlugged;
    }

    private static boolean readingPiccoloResponse = false;
    private static String piccoloResponseSequence;
    public static String currentCmdLabel = null;
    public static String currentResponsePrefix;
    public static PiccoloResponseCallback currentResponseCallback;

    private static void initPiccoloResponseSettings(String cmdLabel, String responsePrefix, PiccoloResponseCallback callback) {
        readingPiccoloResponse = true;
        piccoloResponseSequence = new String();
        currentCmdLabel = cmdLabel;
        currentResponsePrefix = responsePrefix;
        currentResponseCallback = callback;
    }

    // Check to see if the Piccolo USB device is accessible, then request the wifi info
    public static UsbDevice getPiccoloDevice(Context context) {
        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        HashMap<String, UsbDevice> deviceList = usbManager.getDeviceList();

        if (deviceList.size() > 0) {
            Iterator<UsbDevice> deviceIterator = deviceList.values().iterator();

            while (deviceIterator.hasNext()) {
                UsbDevice device = deviceIterator.next();

                //usbManager.requestPermission(device, pendingIntent);
                if (device.getVendorId() == PICCOLO_VENDOR_ID && device.getProductId() == PICCOLO_PRODUCT_ID) {
                    log.debug(Logs.PICCOLO_IO, "Detected Piccolo device on USB.");
                    if (!isDocked()) {
                        log.debug(Logs.PICCOLO_IO, "isDocked was false; setting it to true.");
                        setIsDocked(true);
                    }
                    return device;
                }
                log.debug(Logs.PICCOLO_IO, String.format("Found non-Piccolo USB device: Manufacturer=%s DeviceName=%s DeviceId=%d VendorId=%d ProductId=%d Class=%d Subclass=%d\n",
                        HelperFuncs.noNull(device.getManufacturerName()), HelperFuncs.noNull(device.getDeviceName()), device.getDeviceId(), device.getVendorId(), device.getProductId(),
                        device.getDeviceClass(), device.getDeviceSubclass()));
            }
            log.debug(Logs.PICCOLO_IO, "Detected one or more USB devices, but found no recognized Piccolo device");
        }
        else {
            log.debug(Logs.PICCOLO_IO,  "No USB devices detected");
        }

        setIsDocked(false);
        return null;
    }

    private static int indexAfter(String s, String delim) {
        int index = s.indexOf(delim);
        if (index < 0) {
            return -1;
        }
        index += delim.length();
        if (index >= s.length()) {
            return -1;
        }
        return index;
    }

    private static String parseForFullResponse(String cmdPrefix) {
        // Examine the response from the Piccolo so far, looking for the command prefix
        // and the "OK" suffix.  If both are found, return the body of the command between
        // the prefix and the suffix; otherwise, return null.
        try {
            int startIndex = piccoloResponseSequence.indexOf(cmdPrefix);
            if (startIndex < 0) {
                return null;
            }
            startIndex += cmdPrefix.length();
            int endIndex = piccoloResponseSequence.substring(startIndex).indexOf(PICCOLO_CMD_TERMINATOR);
            if (endIndex < 0) {
                return null;
            }
            endIndex += startIndex;
            return piccoloResponseSequence.substring(startIndex, endIndex);
        }
        catch (IndexOutOfBoundsException ex) {
            return null;
        }
    }

    private static boolean handleGpsPositionResponse(String responseBody) {
        try {
            logCmdMessage(String.format("responseBody '%s'", responseBody.replace("\n", "\\n")));


            final String TMP_DELIM = "##";
            String response = responseBody.replace("\\u00A0", "")
                    .replace("\nUTC=", TMP_DELIM)
                    .replace("\nV=", TMP_DELIM)
                    .replace("km/h", TMP_DELIM);
            
            if (HelperFuncs.isNullOrEmpty(response)) {
                doOnErrorCallback(PICCOLO_LOCATION_NO_DATA,
                        "Location command returned no data");
                return false;
            }

            String fields[] = response.split(TMP_DELIM);
            int fieldsReturned = fields.length;
            if (fieldsReturned >= 1) {
                String gpsCoordinates[] = fields[0].split(",");
                try {
                    if (gpsCoordinates.length != 2) {
                        throw new NumberFormatException();
                    }
                    sLatitude = Double.valueOf(gpsCoordinates[0]);
                    sLongitude = Double.valueOf(gpsCoordinates[1]);
                } catch (NumberFormatException ex) {
                    doOnErrorCallback(PICCOLO_LOCATION_BAD_COORDINATES,
                            "Location cmd read bad coordinates: " + fields[0]);
                    return false;
                }
            }
            if (fieldsReturned >= 2) {
                sTimeStamp = fields[1];
            }
            if (fieldsReturned >= 3) {
                final double MILES_PER_KILOMETER = 0.621371;
                try {
                    // Truck speed is in k/h. Convert it to MPH.
                    sTruckSpeed = Float.valueOf(fields[2]) * MILES_PER_KILOMETER;
                } catch (NumberFormatException ex) {
                    doOnErrorCallback(PICCOLO_LOCATION_BAD_SPEED_VALUE,
                            "Location cmd read bad speed value: " + fields[2]);
                    return false;
                }
            } else {
                // sTruckSpeed is initialized to -1.0 as a signal that an error occurred. If we
                // get here, it means there was no truck speed specified, but all the other fields
                // were correct, so we change sTruckSpeed from -1.0 to 0.0.
                sTruckSpeed = 0.0;
            }
        }
        catch (Exception ex) {
            doOnErrorCallback(PICCOLO_LOCATION_PARSE_ERROR,
                    "Got exception parsing location results: " + ex.toString());
            return false;
        }
        return true;
    }

    private static void handleTruckNumResponse(String responseBody) {

        logCmdMessage(String.format("responseBody '%s'", responseBody.replace("\n", "\\n")));

        String valueReceived;
        if (responseBody.indexOf(",") < 0) {
            valueReceived = responseBody.replace("\\u00A0", "");
        }
        else {
            valueReceived = StringUtils.substringBefore(responseBody, ",")
                    .replace("\\u00A0", "");
        }

        if (BuildConfig.AUTOTRAN_TRUCK_HACK) {
            if (valueReceived.isEmpty()) {
                // For testing purposes, simulate an error every other time device is mounted
                doError = false;
                if (doError) {
                    doError = false;
                    valueReceived = "-100";
                }
                else {
                    doError = true;
                    valueReceived = TruckNumberHandler.getHackedTruckNumber();
                }
            }
        }

        int truckNumber;
        if (HelperFuncs.isNullOrWhitespace(valueReceived)) {
            logCmdMessage("Got empty truck number from Piccolo");
            truckNumber = PICCOLO_TRUCK_NUM_NOT_SET; // Just in case we get whitespace garbage
        }
        else {
            try {
                truckNumber = Integer.parseInt(valueReceived);
            } catch (NumberFormatException ex) {
                truckNumber = PICCOLO_TRUCK_NUM_INVALID;
            }
        }

        logCmdMessage("Got truck number from Piccolo: '" + valueReceived + "'");
        setPiccoloTruckNumber(truckNumber);
        //logCmdMessage("Calling SendPiccoloDockedTask()");
        setCustomVariables(2);
        new SendPiccoloDockedTask(AutoTranApplication.getAppContext()).execute();
    }

    private interface PiccoloResponseCallback {
        boolean onResponse(String responseBody);
        void onError(int errorCode, String msg);
    }

    private static String fixNewlines(String s) {
        return s.replace("\r", "\\r").replace("\n", "\\n");
    }

    private static void logCmdMessage(String msg) {
        if (HelperFuncs.isNullOrEmpty(currentCmdLabel)) {
            log.debug(Logs.PICCOLO_IO, msg);
        }
        else {
            log.debug(Logs.PICCOLO_IO, String.format(" >%s: %s", currentCmdLabel, msg));
        }
    }

    private static void logCmdError(int errorCode, String errorMsg) {
        log.debug(Logs.PICCOLO_IO, String.format("%s FAILED (%d): %s", currentCmdLabel, errorCode, errorMsg));
    }

    private static void sendPiccoloCmd(Context context, UsbDevice device, String cmdLabel, String cmd, String responsePrefix, PiccoloResponseCallback callback) {
        initPiccoloResponseSettings(cmdLabel, responsePrefix, callback);
        log.debug(Logs.PICCOLO_IO, String.format("%s started", currentCmdLabel));
        logCmdMessage(cmd);

        // Docking sometimes generates multiple requests back-to-back, so we ignore requests
        // that are too close together.  This gives the first request time to complete before
        // initiating a new one.
        long now = System.currentTimeMillis();
        if ((now - lastCallTime) < 1500) {
            logCmdMessage("Command ignored: Came < 1500 ms after prior request");
            return;
        }
        lastCallTime = now;

        boolean forceClaim = true;

        UsbManager usbManager = (UsbManager) context.getSystemService(Context.USB_SERVICE);
        UsbInterface intf = device.getInterface(0);
        closeUsbConnectionIfOpen();
        if (intf != null) {
            try {
                sConnection = usbManager.openDevice(device);
                if (sConnection == null) {
                    doOnErrorCallback(PICCOLO_OPEN_ERROR,
                            "PiccoloManager could not open UsbDevice");
                    return;
                }
                sConnection.claimInterface(intf, forceClaim);

                sSerial = UsbSerialDevice.createUsbSerialDevice(device, sConnection);
                sSerial.open();
                sSerial.setBaudRate(115200);
                sSerial.setDataBits(UsbSerialInterface.DATA_BITS_8);
                sSerial.setParity(UsbSerialInterface.PARITY_ODD);
                sSerial.setFlowControl(UsbSerialInterface.FLOW_CONTROL_OFF);
                sSerial.read(cmdResponseCallback);

                String command = cmd + "\r\n";

                logCmdMessage(String.format("Piccolo command sent: '%s'", fixNewlines(command)));

                sNullPacketsReceived = 0;
                sSerial.write(command.getBytes());
            } catch (Exception ex) {
                closeUsbConnectionIfOpen();
                String msg = "Caught exception: " + ex.getClass().getSimpleName();
                log.debug(Logs.EXCEPTIONS, msg);
                log.debug(Logs.EXCEPTIONS, Log.getStackTraceString(ex));
                doOnErrorCallback(PICCOLO_IO_ERROR, msg);
            }
        } else {
            doOnErrorCallback(PICCOLO_CONNECT_ERROR, "Could not get usb device connection");
        }
    }

    private static void sendPiccoloCmd(Context context, String cmdlabel, String cmd, String responsePrefix, PiccoloResponseCallback callback) {
        UsbDevice device = getPiccoloDevice(context);
        if (device == null) {
            currentCmdLabel = cmdlabel;
            callback.onError(PICCOLO_DEVICE_NOT_DETECTED, "Device not detected");
            return;
        }

        sendPiccoloCmd(context, device, cmdlabel, cmd, responsePrefix, callback);
    }

    private static UsbSerialInterface.UsbReadCallback cmdResponseCallback = new UsbSerialInterface.UsbReadCallback() {

        @Override
        public void onReceivedData(byte[] arg0)
        {
            if (arg0 == null || arg0.length == 0) {
                if (arg0 == null) {
                    logCmdMessage("Got null byte array from usb");
                }
                else {
                    logCmdMessage("Got zero-length byte array from usb");
                }
                if (++sNullPacketsReceived > MAX_NULL_PACKETS) {
                    closeUsbConnectionIfOpen();
                    readingPiccoloResponse = false;
                    doOnErrorCallback(PICCOLO_NULL_PACKET_STORM_DETECTED,
                            "Abandoning Piccolo command after receiving "
                                    + sNullPacketsReceived + " null packets");
                }
                return;
            }

            // For easier parsing, remove carriage returns leaving only newlines.
            String data = (new String(arg0)).replaceAll("\\r", "");

            if (!readingPiccoloResponse) {
                log.debug(Logs.DEBUG, "Got info from usb after command response " + data);
                return;
            }

            piccoloResponseSequence += data;

            String responseBody = parseForFullResponse(currentResponsePrefix);
            if (responseBody == null) {
                return;
            }

            readingPiccoloResponse = false;
            closeUsbConnectionIfOpen(); // close to avoid receiving subsequent USB chatter
            logCmdMessage(String.format("Piccolo response received: '%s'", fixNewlines(piccoloResponseSequence)));

            try {
                if (currentResponseCallback.onResponse(responseBody)) {
                    log.debug(Logs.PICCOLO_IO, String.format("%s succeeded", currentCmdLabel));
                }
            } catch (Exception ex) {
                String msg = "Caught an exception in onResponse() callback";
                log.debug(Logs.PICCOLO_IO, msg + ". See EXCEPTIONS log");
                log.debug(Logs.EXCEPTIONS, msg, ex);
                doOnErrorCallback(PICCOLO_IO_ERROR, msg + ex.getClass().getSimpleName());
            }
            currentCmdLabel = null;
        }

    };

    private static void doOnErrorCallback(int errorCode, String msg) {
        currentResponseCallback.onError(errorCode, msg);
        closeUsbConnectionIfOpen();
    }


// Disable echo "ATE0\r\n"  "AT*WCSAVE"

    private static PiccoloResponseCallback requestTruckNumCallback = new PiccoloResponseCallback() {
        @Override
        public boolean onResponse(String responseBody) {
            handleTruckNumResponse(responseBody);
            return true;
        }

        @Override
        public void onError(int errorCode, String msg) {
            logCmdError(errorCode,  msg);
            // Set the Piccolo truck number to the error code. Will be reported in TRUCKLOC event.
            setPiccoloTruckNumber(errorCode);
        }
    };


    public static void requestTruckNum(Context context, UsbDevice device) {
        sendPiccoloCmd(context, device, "RequestTruckNumber", PICCOLO_CMD_GET_WIFI_INFO,
                TRUCK_WIFI_RESPONSE_PREFIX.toString(), requestTruckNumCallback);

    }

    public static void requestTruckNum(Context context) {
        sendPiccoloCmd(context, "RequestTruckNumber", PICCOLO_CMD_GET_WIFI_INFO,
                    TRUCK_WIFI_RESPONSE_PREFIX.toString(), requestTruckNumCallback);
    }

    public static double sLatitude = 0.0;
    public static double sLongitude = 0.0;
    public static String sTimeStamp = "";
    public static double sTruckSpeed = 0.0;

    private static void setPositionValues(double latitude, double longitude, double truckSpeed) {
        sLatitude = latitude;
        sLongitude = longitude;
        sTruckSpeed = truckSpeed;
    }

    private static void resetPositionValues() {
        sLatitude = 0.0;
        sLongitude = 0.0;
        sTimeStamp = "";
        sTruckSpeed = -1.0;
    }

    public static double getLatitude() {
        return sLatitude;
    }

    public static double getLongitude() {
        return sLongitude;
    }

    public static double getTruckSpeed() {
        return sTruckSpeed;
    }

    public static String getTimeStamp() { return sTimeStamp; }

    public static void requestGpsPosition(Context context) {
        resetPositionValues();
        sendPiccoloCmd(context, "GetGpsPosition", PICCOLO_CMD_GET_GPS_POSITION,
                POSITION_RESPONSE_PREFIX.toString(),
                new PiccoloResponseCallback() {
                    @Override
                    public boolean onResponse(String responseBody) {
                        return handleGpsPositionResponse(responseBody);
                    }

                    @Override
                    public void onError(int errorCode, String msg) {
                        logCmdError(errorCode,  msg);
                        sTruckSpeed = (double) errorCode;
                    }
                });
    }

    /****
     *  Set text variables for Piccolo
     *
     *  Command format
     *
     */
    public static void setCustomVariables() {
        String driverNumber = CommonUtility.getDriverNumber(AutoTranApplication.getAppContext());
        String loadNumber = GlobalState.getLastStartedLoadNum();
        if (driverNumber.isEmpty()) {
            driverNumber = "0";
            loadNumber = "0";
        }
        else if (loadNumber.isEmpty()) {
            loadNumber = "0";
        }
        // Strip off inspection group suffix before sending loadNumber to Piccolo
        loadNumber = StringUtils.substringBeforeLast(loadNumber, "-");

        log.debug(Logs.PICCOLO_IO, String.format("Setting Piccolo variables: driverId='%s', loadNumber='%s'",
                driverNumber, loadNumber));

        String cmd = String.format("%s=DRID:%s,LNUM:%s", PICCOLO_CMD_TEXT_SETTINGS, driverNumber, loadNumber);
        sendPiccoloCmd(AutoTranApplication.getAppContext(),
                "SetCustomVariables", cmd, PICCOLO_CMD_TEXT_SETTINGS.toString(),
                new PiccoloResponseCallback() {
                    @Override
                    public boolean onResponse(String response) {
                        return true;
                    }

                    @Override
                    public void onError(int errorCode, String msg) {
                        logCmdError(errorCode, msg);
                    }
                });
    }
    
    private static Handler setCustomVariablesHandler;
    private static Runnable setCustomVariablesRunnable;

    public static void setCustomVariables(long delaySeconds) {
        log.debug(Logs.PICCOLO_IO, "Scheduling Piccolo setCustomValues() in " + delaySeconds + " seconds.");

        setCustomVariablesHandler = new Handler(AutoTranApplication.getAppContext().getMainLooper());
        setCustomVariablesRunnable = new Runnable(){
            public void run(){
                try {
                    if (PiccoloManager.isDocked()) {
                        setCustomVariables();
                    }
                    else {
                        log.debug(Logs.PICCOLO_IO, "Not sending Piccolo command. Not docked.");
                    }
                }
                catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        setCustomVariablesHandler.postDelayed(setCustomVariablesRunnable, delaySeconds * 1000);
    }

    private static final String USB_PERMISSION_ISSUE_DETECTED = "USB_PERMISSION_ISSUE_DETECTED";

    private static void setUsbPermissionIssueDetected(boolean permissionIssueDetected) {
        try {
            getSharedPreferences(AutoTranApplication.getAppContext())
                    .edit().putBoolean(USB_PERMISSION_ISSUE_DETECTED, permissionIssueDetected)
                    .apply();
        } catch (Exception ex) {
            log.debug(Logs.PICCOLO_IO, "setUsbPermissionIssueDetected() got exception");
        }
    }

    public static boolean isUsbPermissionIssueDetected() {
        try {
            return getSharedPreferences(AutoTranApplication.getAppContext())
                    .getBoolean(USB_PERMISSION_ISSUE_DETECTED, false);
        } catch (Exception ex) {
            log.debug(Logs.PICCOLO_IO, "isUsbPermissionIssueDetected() got exception");
            return false;
        }
    }

    public static void detectUsbPermissionState(Context context) {
        if (!BuildConfig.AUTOTRAN_SHOW_PICCOLO_DOCK_WARNING || PiccoloManager.isDocked()) {
            log.debug(Logs.PICCOLO_IO, "Handheld DOCKED. Clearing permissionIssueDetected flag");
            setUsbPermissionIssueDetected(false);
        } else if (!isUsbPermissionIssueDetected() && PiccoloManager.isPlugged(context)) {
            // Note: Once we detect an issue, we keep the USB_PERMISSION_ISSUE_DETECTED
            // state set to true until the device is successfully docked.
            setUsbPermissionIssueDetected(true);
            log.debug(Logs.PICCOLO_IO, "Piccolo permission issue detected. Setting permissionIssueDetected flag");
        }
    }
}
