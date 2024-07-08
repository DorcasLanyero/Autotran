package com.cassens.autotran.scanning;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;
import android.provider.Settings;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.sdgsystems.idengine.api.Constants;
import com.sdgsystems.idengine.api.DeviceCallback;
import com.sdgsystems.idengine.api.DeviceDataResponse;
import com.sdgsystems.idengine.api.DeviceError;
import com.sdgsystems.idengine.api.DeviceException;
import com.sdgsystems.idengine.api.DeviceInfo;
import com.sdgsystems.idengine.api.DeviceManager;
import com.sdgsystems.idengine.api.DeviceResponse;
import com.sdgsystems.idengine.api.ScanFailedResponse;
import com.sdgsystems.idengine.api.ScanStoppedResponse;
import com.sdgsystems.idengine.barcode.api.BarcodeScanner;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

import static com.sdgsystems.idengine.api.Constants.DeviceErrors.ERROR_NO_SERVICE;

class IdEngineScanManager extends VendorScanManager {
    private static final Logger log = LoggerFactory.getLogger(IdEngineScanManager.class.getSimpleName());
    private static final String ID_ENGINE_PACKAGE_NAME = "com.sdgsystems.idengine";
    private Activity mActivity;
    private boolean mScanning = false;

    private static ScannerDetectorApiCallback sCallback;
    private static DeviceManager sDeviceManager;
    private static BarcodeScanner sScanner;

    private DeviceCallback mScannerCallback = null;
    private DeviceCallback mInternalScannerCallback = new ScanResultCallback();

    private static long sSelectedScanner = -1;
    private static long sDefaultScanner = -1;
    private static int sPreviousScannerCount;
    private static boolean sInitialLoadComplete = false;
    private static boolean sPaused = true;

    // Hacky, yes
    private static boolean sIsZxing = false;
    private static boolean sIsZxingScanning = false;
    private boolean mTempAutoRotationOff = false;
    private static boolean sPromptForScannerChoice = false;


    private static final long ZXING_SCANNER_ID = 0x0123456L;//0x1077800956C3F8ADL;
    private static final long SYMBOLPL_SCANNER_ID = 0xAB8B5928EBDE8626L;

    public boolean isPresentOnDevice() {
        return isIdEngineInstalled();
    }

    @Override
    public boolean isCameraScanner() {
        // For now, we use ID-Engine only for camera scanners.
        return true;
    }

    public void connect(Activity context) {
        // TODO: Should consider using application context here.
        mActivity = context;
        sIsZxingScanning = false;

        try {
            if (sDeviceManager == null) {
                sDeviceManager = new DeviceManager(mActivity);
                if (sCallback == null) {
                    sCallback = new ScannerDetectorApiCallback();
                }
                sDeviceManager.setCallback(sCallback);

                //log.debug(Logs.DEBUG, "Connected to ID-Engine camera scanner and registered callback.");
                return;
            }
            else {
                //log.debug(Logs.DEBUG, "Already connected to ID-Engine");
            }
        } catch (Exception e) {
            //log.error(Logs.DEBUG, "Failed to connect to ID-Engine");
            if (notifyIfServiceMissing()) {
                CommonUtility.showText(mActivity.getString(R.string.settings_init_failed));
            }
        }
        return;
    }



    public void disconnect() {
        mScanning = false;
        try {
            if (sDeviceManager != null) {
                //log.debug(Logs.DEBUG, "Disconnecting from ID-Engine");
                sDeviceManager.setCallback(null);
                if (sDeviceManager.isConnected()) {
                    sDeviceManager.disconnect();
                }
                sDeviceManager = null;
                if (sScanner != null) {
                    sScanner.close();
                    sScanner = null;
                }
                //log.debug(Logs.DEBUG, "Disconnected from ID-Engine camera scanner");
            }
            else {
                //log.debug(Logs.DEBUG, "Already disconnected from ID-Engine");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isConnected() {
        return sDeviceManager != null && sDeviceManager.isConnected();
    }

    public boolean isEnabled() {
        return isConnected() && sScanner != null && sScanner.isScannerEnabled();
    }

    private static boolean sGotOne;

    public boolean startScan() {
        if (!isEnabled()) {
            GenericScanManager.getScannerCallback().onScanFailure("Scanner not enabled");
            return false;
        }

        // For some reason ID-Engine sometimes returns multiple barcodes even when not in batch mode.
        // We use a flag to ensure we return only the first barcode scanned.
        sGotOne = false;
        try {
            sScanner.startScan();
            if (sIsZxing) {
                int currentRotation = Settings.System.getInt(mActivity.getApplicationContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);

                if (currentRotation == 1) {
                    mTempAutoRotationOff = true;
                    Settings.System.putInt(mActivity.getApplicationContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 0);
                }

                sIsZxingScanning = true;
            }
        } catch (Exception e) {
            mScanning = false;
            String errorMsg = "ID-Engine startScan() exception: " + e.getClass().getName();
            log.error(Logs.DEBUG, errorMsg);
            GenericScanManager.getScannerCallback().onScanFailure(errorMsg);
            return false;
        }
        mScanning = true;
        return true;
    }

    public boolean isScanning() {
        return mScanning;
    }

    /**
     * Determines whether to notify the user on activity start if ID-Engine can't be found.
     * Note the user will always be notified if they attempt to initiate a scan and ID-Engine
     * is not installed.
     *
     * @return true to notify the user on activity start, false to ignore failures at that time.
     */
    private boolean notifyIfServiceMissing() {
        return true;
    }

    private ArrayList<DeviceInfo> getAvailableBarcodeScanners(List<DeviceInfo> devices) {
        ArrayList<DeviceInfo> scanners = new ArrayList<DeviceInfo>();

        for (DeviceInfo device: devices) {
            //log.debug(Logs.DEBUG, String.format("%s (%s)", device.getName(), device.getDeviceType()));
            if (device.getDeviceType().equalsIgnoreCase(Constants.DeviceTypes.BARCODE_SCANNER)) {
                //log.debug(Logs.DEBUG, "Added scanner " + device.getName());
                scanners.add(device);
            }
        }
        return scanners;
    }

    private ArrayList<DeviceInfo> getAvailableBarcodeScanners(DeviceManager deviceManager) {
        return getAvailableBarcodeScanners(deviceManager.getAvailableDevices());
    }

    /**
     * Determines whether toast notifications will alert the user to automatic scanner changes.
     *
     * @return true to notify the user; otherwise false.
     */
    protected boolean showToastsForStatus() { return false; }

    /**
     * Determines whether toast notifications will alert the user on some scanner errors
     *
     * @return true to notify the user; otherwise false.
     */
    protected boolean showToastsForErrors() { return true; }


    private class ScannerDetectorApiCallback extends DeviceManager.ApiCallback {
        private boolean containsScannerId(List<DeviceInfo> scanners, long id) {
            for(DeviceInfo scanner: scanners) {
                if(id == scanner.getId()) return true;
            }
            return false;
        }

        @Override
        public void onConnected(boolean isConnected) {
            if(!selectScanner(getAvailableBarcodeScanners(sDeviceManager), sSelectedScanner)) {
                log.debug(Logs.DEBUG, "Selecting scanner");
                selectScanner(getAvailableBarcodeScanners(sDeviceManager), sDefaultScanner);
            }
        }

        @Override
        public void onError(DeviceError error) {
            if (error == ERROR_NO_SERVICE) {
                /*
                if(notifyIfServiceMissing()) {
                    tellUserToInstallCore();
                }*/
            }
            GenericScanManager.getScannerCallback().onConnectFailure("Scanner connect failed: " + error.getDescription());
        }

        private class ScannerChoiceDialogRunner implements Runnable {
            List<DeviceInfo> mScanners;
            long mSelectedId;

            public ScannerChoiceDialogRunner(List<DeviceInfo> scanners, long selectedId) {
                mScanners = scanners;
                mSelectedId = selectedId;
            }

            @Override
            public void run() {
                final String[] scannerNames = new String[mScanners.size()];
                final long[] scannerIds = new long[mScanners.size()];

                int i = 0;
                int checkedItem = -1;
                for (DeviceInfo scanner : mScanners) {
                    scannerNames[i] = scanner.getName();
                    scannerIds[i] = scanner.getId();
                    if (mSelectedId == scannerIds[i]) {
                        checkedItem = i;
                    }

                    i++;
                }

                new AlertDialog.Builder(mActivity)
                        .setSingleChoiceItems(scannerNames, checkedItem, (dialog, which) -> {
                            for (DeviceInfo scanner : mScanners) {
                                if (scanner.getId() == scannerIds[which]) {
                                    selectScanner(scanner);
                                    break;
                                }
                            }
                        })
                        .setTitle(R.string.select_scanner)
                        .setCancelable(false)
                        .setPositiveButton(R.string.button_continue, (dialog, which) -> {
                            dialog.dismiss();
                        })
                        .show();
            }
        }

        @Override
        public void onDevicesAvailable(List<DeviceInfo> devices) {
            final SharedPreferences prefs =
                    PreferenceManager.getDefaultSharedPreferences(mActivity);
            ArrayList<DeviceInfo> scanners = new ArrayList<DeviceInfo>();

            sSelectedScanner = Long.parseLong(prefs.getString("scanner_id", "-1"));
            // TODO: Figure out how to handle default scanner
            /*
            if(sDeviceManager.getDefaultScanner() != null) {
                sDefaultScanner = sDeviceManager.getDefaultScanner().getId();
            } */

            // TODO: Figure out how to filter camera barcode scanners out from other scanners
            // TODO: Determine if ID-Engine will step on Honeywell's scanner

            scanners = getAvailableBarcodeScanners(devices);
            int newScannerCount = scanners.size();

            // If the list size has changed, and if we've been through this code once before:
            // The former because onScannerAvailable callbacks happen multiple times sometimes,
            // and the latter because we generally don't want to throw up a choice dialog on app
            // start (just when something has changed).
            if (sPreviousScannerCount != newScannerCount && sInitialLoadComplete) {
                sPreviousScannerCount = newScannerCount;
                if (newScannerCount == 0) {
                    if(showToastsForStatus()) {
//            // Alert user with toast: no scanners anymore

                        mActivity.runOnUiThread(new ScannerDetectorApiCallback.ToastShowRunner("No scanners connected"));
                    }
                }
                else if (newScannerCount == 1) {
                    if(!sPromptForScannerChoice) {
                        selectScanner();
                    } else {
                        if (containsScannerId(scanners, sSelectedScanner)) {
                            // Do nothing: the selected scanner is the only one present
                        }
                        else {
                            if(showToastsForStatus()) {
                                mActivity.runOnUiThread(new ScannerDetectorApiCallback.ToastShowRunner("Using scanner: " + scanners.get(0).getName()));
                            }
                            selectScanner(scanners.get(0));
                        }
                    }
                }
                else { // more than two scanners
                    // Present options.
                    if(!sPromptForScannerChoice) {
                        selectScanner();
                    } else {
                        mActivity.runOnUiThread(new ScannerChoiceDialogRunner(scanners, sSelectedScanner));
                    }
                }
            }
            if(!sInitialLoadComplete) {
                sInitialLoadComplete = true;
                sPreviousScannerCount = scanners.size();

                // If the user hasn't selected a scanner yet, or if the previously-selected scanner has gone, pick one or ask.
                if (sSelectedScanner == -1 || !containsScannerId(scanners, sSelectedScanner)) {
                    if(newScannerCount == 1) {
                        if(!sPromptForScannerChoice) {
                            selectScanner();
                        } else {
                            if(showToastsForStatus()) {
                                mActivity.runOnUiThread(new ScannerDetectorApiCallback.ToastShowRunner("Using scanner: " + scanners.get(0).getName()));
                            }
                            selectScanner(scanners.get(0));
                        }
                    } else if(sDefaultScanner != -1) {
                        selectScanner(scanners, sDefaultScanner);
                    } else if(newScannerCount > 1) {
                        if(!sPromptForScannerChoice) {
                            selectScanner();
                        } else {
                            mActivity.runOnUiThread(new ScannerChoiceDialogRunner(scanners, sSelectedScanner));
                        }
                    }
                } else if (containsScannerId(scanners, sSelectedScanner)) {
                    if(!sPromptForScannerChoice) {
                        selectScanner();
                    } else {
                        selectScanner(scanners, sSelectedScanner);
                    }
                }
            }
        }

        private class ToastShowRunner implements Runnable {
            String mMessage;
            public ToastShowRunner(String message) {
                mMessage = message;
            }

            @Override
            public void run() {
                CommonUtility.showText(mMessage);
            }
        }
    }

    //Selects snapi by default and camera if necessary
    private void selectScanner() {
        log.debug(Logs.DEBUG, "selecting scanner automatically");
        DeviceInfo chosenScanner = null;

        //priority: symbol, 'camera', 'mobile vision'

        for(DeviceInfo info : getAvailableBarcodeScanners(sDeviceManager)) {
            log.debug(Logs.DEBUG, "saw scanner " + info.getId() + ": " + info.getName());

            if(chosenScanner == null) {
                //If no scanner has been chosen yet
                chosenScanner = info;
            } else if(info.getId() == SYMBOLPL_SCANNER_ID || info.getName().toLowerCase().contains("symbol")) {
                //If the chosen scanner is the diamondback sled
                chosenScanner = info;
            } else if( chosenScanner.getName().toLowerCase().contains("mobile vision")) {
                //If we have chosen mobile vision, default to anything else
                chosenScanner = info;
            }
        }

        if(chosenScanner != null) {
            log.debug(Logs.DEBUG, "Chosen scanner is " + chosenScanner.getName());

            if(chosenScanner.getId() == ZXING_SCANNER_ID || chosenScanner.getName().toLowerCase().contains("camera")
                    || chosenScanner.getName().contains("Mobile Vision")) {
                log.debug(Logs.DEBUG, "Chosen scanner is the camera, the user should turn on the sled to get hardware scanning capability");
                sIsZxing = true;
            }

            selectScanner(chosenScanner);
        } else {
            log.debug(Logs.DEBUG, "not connecting to same or null scanner");
            log.debug(Logs.DEBUG, "not connecting to same or null scanner");
        }
    }

    private boolean selectScanner(List<DeviceInfo> scanners, long id) {
        for(DeviceInfo scanner : scanners) {
            if(scanner.getId() == id) {
                return selectScanner(scanner);
            }
        }

        return false;
    }

    private PackageInfo getIdEnginePackageInfo() {
        PackageInfo pInfo;
        try {
            pInfo = mActivity.getPackageManager().getPackageInfo(ID_ENGINE_PACKAGE_NAME, 0);
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
        return pInfo;
    }

    String getIdEngineInfo() {
        PackageInfo pInfo = getIdEnginePackageInfo();
        if (pInfo == null) {
            return "ID-Engine not installed";
        }
        else {
            return String.format("ID-Engine %s", pInfo.versionName);
        }
    }

    boolean isIdEngineInstalled() {
        return getIdEnginePackageInfo() != null;
    }

    private boolean selectScanner(DeviceInfo deviceInfo) {
        sSelectedScanner = deviceInfo.getId();
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(mActivity).edit();
        editor.putString("scanner_id", String.valueOf(sSelectedScanner));
        editor.apply();

        log.debug(Logs.DEBUG, "Opening scanner " + deviceInfo.getName());
        // TODO: Check for error on openScanner()
        openScanner(deviceInfo);
        return true;
    }

    private void selectScannerAtScanTime() {
        final CharSequence[] scannerNames = new CharSequence[getAvailableBarcodeScanners(sDeviceManager).size()];
        final DeviceInfo[] scanners = new DeviceInfo[getAvailableBarcodeScanners(sDeviceManager).size()];
        int i = 0;
        for (DeviceInfo deviceInfo : getAvailableBarcodeScanners(sDeviceManager)) {
            scanners[i] = deviceInfo;
            scannerNames[i] = deviceInfo.getName();
            i++;
        }

        new AlertDialog.Builder(mActivity)
                .setTitle(R.string.select_scanner)
                .setSingleChoiceItems(
                        scannerNames, -1,
                        (dialogInterface, i1) -> {
                            // Save the selection to preferences.
                            selectScanner(scanners[i1]);
                        }
                )
                .setCancelable(false)
                .setPositiveButton(R.string.button_continue, (dialog, which) -> dialog.dismiss())
                .show();

    }

    private synchronized void openScanner(DeviceInfo deviceInfo) {
        log.debug(Logs.DEBUG, "openScanner(" + deviceInfo + ")");
        try {
            if (sScanner != null) {
                if (sScanner.getDeviceInfo().getName().equalsIgnoreCase(deviceInfo.getName())) {
                    log.debug(Logs.DEBUG, deviceInfo + " is already open");
                }
                log.debug(Logs.DEBUG, "Closing previous scanner " + sScanner);
                sScanner.close();
            }
            log.debug(Logs.DEBUG, "Opening scanner " + deviceInfo);
            sScanner = sDeviceManager.openBarcodeScanner(deviceInfo);
            log.debug(Logs.DEBUG, "Setting callback " + mInternalScannerCallback);
            setScannerCallback();
            sIsZxing = ( deviceInfo.getId() == ZXING_SCANNER_ID || deviceInfo.getName().toLowerCase().contains("camera") || deviceInfo.getName().contains("Mobile Vision"));
            sIsZxingScanning = false;
            GenericScanManager.getScannerCallback().onConnectSuccess("Barcode scanner selected and opened: " + deviceInfo.getName());
        } catch (DeviceException e) {
            String errorMsg = "Could not connect to " + deviceInfo.getName() + ": " + e;
            log.error(Logs.DEBUG, errorMsg);
            if(showToastsForErrors()) {
                CommonUtility.showText(errorMsg);
            }
            GenericScanManager.getScannerCallback().onConnectFailure(errorMsg);
            return;
        }/*
        try {
            log.debug(Logs.DEBUG, "Setting beeps");
            sScanner.setSuccessBeepEnabled(true);
            sScanner.setFailureBeepEnabled(true);
        } catch (DeviceException e) {
            e.printStackTrace();
        }*/
    }

    private void setScannerCallback() {
        if (mScannerCallback != null) {
            sScanner.setCallback(mScannerCallback);
        }
        else {
            //log.debug(Logs.DEBUG, "Setting scanner callback to mInternalScannerCallback");
            sScanner.setCallback(mInternalScannerCallback);
        }
    }

    private class ScanResultCallback extends DeviceCallback {
        @Override
        public void onDeviceAvailable(boolean ready) {
            try {
                //log.debug(Logs.DEBUG, "onDeviceAvailable() callback");
                sScanner.setSuccessBeepEnabled(true);
                sScanner.setFailureBeepEnabled(true);
            } catch (DeviceException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDeviceResponse(DeviceResponse deviceResponse) {
            //log.debug(Logs.DEBUG, "Got response: " + deviceResponse.toString());
            if (deviceResponse instanceof DeviceDataResponse) {
                if (deviceResponse instanceof com.sdgsystems.idengine.barcode.api.BarcodeData) {
                    com.sdgsystems.idengine.barcode.api.BarcodeData data = (com.sdgsystems.idengine.barcode.api.BarcodeData) deviceResponse;
                    //log.debug(Logs.DEBUG, "Got barcode: " + data.getBarcode() + " (" + data.getSymbology() + ")");
                    if (mTempAutoRotationOff) {
//                          mTempAutoRotationOff = false;
//                          Settings.System.putInt(getApplicationContext().getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, 1);
                    }

                    if (sIsZxing) {
//                          if (sPaused && !sIsZxingScanning) {
//                              // Ignore this barcode.
//                              return;
//                          }
                    } else {
                        /* Will always be paused, since we're calling from a new Activity
                        if (sPaused) {
                            Log.d(TAG, "Ignoring barcode. Paused");
                            return; // Ignore this barcode.
                        }*/
                    }
                /*
                String barcode = data.getDataString();
                String symbology = data.getDataSubType();
                barcode = barcode.replace("\n", "<br>");
                String text = "Data: <b>" + barcode
                        + "</b><br>Type: <b>"
                        + symbology + "</b><br>";
                log(Html.fromHtml(text));
                */
                    if (sGotOne) {
                        log.debug(Logs.DEBUG, "Got additional barcode scan. Ignoring.");
                        return;
                    }
                    sGotOne = true;

                    //log.debug(Logs.DEBUG, "Got ID-Engine barcode: " + data.getDataString());
                    GenericScanManager.getScannerCallback().onScanResult(data.getDataString());
                }
                else {
                    //log.debug(Logs.DEBUG, "Got device data response: " + ((DeviceDataResponse)deviceResponse).getDataType());
                }
            } else if (deviceResponse instanceof ScanFailedResponse) {
                //log(Html.fromHtml("<font color=\"#FF4040\">Scan failed</font><br>"));
                //scanStopped();
                mScanning = false;

                GenericScanManager.getScannerCallback().onScanFailure(((ScanFailedResponse)deviceResponse).toString());
            } else if (deviceResponse instanceof ScanStoppedResponse) {
                //log("Scan stopped");
                //scanStopped();
                mScanning = false;
            }
        }
    }

}
