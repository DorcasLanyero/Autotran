package com.cassens.autotran.scanning;

import android.app.Activity;

import com.cassens.autotran.Logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GenericScanManager {
    private static final Logger log = LoggerFactory.getLogger(GenericScanManager.class.getSimpleName());

    // Laser barcode scanners
    private static final HoneywellScanManager sHoneywellScanManager = new HoneywellScanManager();
    private static final PanasonicScanManager sPanasonicScanManager = new PanasonicScanManager();

    // Use ID-Engine for camera-based scanning
    private static final IdEngineScanManager sIdEngineScanManager = new IdEngineScanManager();

    private GenericScanManager() {
    }

    private GenericScanManager(ScannerCallback scannerCallback) {
        this.scannerCallback = scannerCallback;
    }

    public interface ScannerCallback {

        public void onScanResult(String scannedValue);
        public void onScanFailure(String errorMsg);
        public void onConnectSuccess(String msg);
        public void onConnectFailure(String errorMsg);
    }

    private static class DefaultScannerCallback implements GenericScanManager.ScannerCallback {

        @Override
        public void onScanResult(String scannedValue) {
            log.debug(Logs.DEBUG, "Default onScanResult got barcode: " + scannedValue);
        }

        @Override
        public void onScanFailure(String errorMsg) {
            log.debug(Logs.DEBUG, "Default onScanFailure: " + errorMsg);

        }

        @Override
        public void onConnectSuccess(String msg) {
            log.debug(Logs.DEBUG, "Default onConnectSuccess: " + msg);

        }

        @Override
        public void onConnectFailure(String errorMsg) {
            log.debug(Logs.DEBUG, "Default onConnectFailure: " + errorMsg);
        }

    };

    private static GenericScanManager sScanManager = null;
    private static final DefaultScannerCallback sDefaultScannerCallback = new DefaultScannerCallback();
    private static final Object sConnectionChangeLock = new Object();
    private static VendorScanManager sSelectedScanManager = null;
    private static VendorScanManager sSelectedCameraScanManager = null;
    private static Activity sActivity;
    private static ScannerCallback sScannerCallback = sDefaultScannerCallback;
    private static int sClientCount = 0;

    static ScannerCallback getScannerCallback() {
        synchronized (sConnectionChangeLock) {
            return sScannerCallback;
        }
    }

    private ScannerCallback scannerCallback;

    public static GenericScanManager connect(Activity activity, ScannerCallback scannerCallback) {

        sScanManager = new GenericScanManager(scannerCallback);

        synchronized (sConnectionChangeLock) {
            sActivity = activity;
            sScannerCallback = sScanManager.scannerCallback;
            if (sClientCount++ == 0) {
                if (sHoneywellScanManager.isPresentOnDevice()) {
                    sSelectedScanManager = sHoneywellScanManager;
                } else if (sPanasonicScanManager.isPresentOnDevice()) {
                    sSelectedScanManager = sPanasonicScanManager;
                }
                sSelectedCameraScanManager = sIdEngineScanManager;
                if (sSelectedScanManager != null) {
                    sSelectedScanManager.connect(sActivity);
                }
                if (sSelectedCameraScanManager != null) {
                    sSelectedCameraScanManager.connect(sActivity);
                }
                log.debug(Logs.DEBUG, "GenericScanManager.connect(): Connected");
            } else {
                //log.debug(Logs.DEBUG, "GenericScanManager.connect(): Already connected");
            }
        }
        return sScanManager;
    }



    public static void resumeScanManagerIfActive() {
        synchronized (sConnectionChangeLock) {
            if (sSelectedScanManager != null) {
                sSelectedScanManager.connect(sActivity);
            }
        }
    }

    public static void pauseScanManagerIfActive() {
        synchronized (sConnectionChangeLock) {
            if (sSelectedScanManager != null) {
                sSelectedScanManager.disconnect();
            }
        }
    }

    public void disconnect() {
        synchronized (sConnectionChangeLock) {

            if (sClientCount > 0) {
                if (--sClientCount == 0) {
                    log.debug(Logs.DEBUG, "GenericScanManager.disconnect(): Disconnecting last connection");
                    if (sSelectedScanManager != null) {
                        sSelectedScanManager.disconnect();
                        sSelectedScanManager = null;
                    }
                    if (sSelectedCameraScanManager != null) {
                        sSelectedCameraScanManager.disconnect();
                        sSelectedCameraScanManager = null;
                    }
                    sScannerCallback = sDefaultScannerCallback;
                    sSelectedScanManager = null;
                } else {
                    //log.debug(Logs.DEBUG, "GenericScanManager.disconnect(): " + sClientCount -1 + " open connections remain");
                }
            }
            else {
                log.debug(Logs.DEBUG, "GenericScanManager.disconnect(): Warning - called disconnect on already disconnected manager");
            }
        }
    }

    public void resumeCallbacks() {
        synchronized (sConnectionChangeLock) {
            if (scannerCallback != sScannerCallback) {
                //log.debug(Logs.DEBUG, "GenericScanManager.resumeCallbacks() - restoring callbacks");
                sScannerCallback = this.scannerCallback;
            } else {
                //log.debug(Logs.DEBUG, "GenericScanManager.resumeCallbacks() - callback restore NOT NEEDED");
            }
        }
    }

    public void pauseCallbacks() {
        //log.debug(Logs.DEBUG, "GenericScanManager.pauseCallbacks - pausing callbacks()");

        synchronized (sConnectionChangeLock) {
            sScannerCallback = sDefaultScannerCallback;
        }
    }

    public boolean startScan() {
        log.debug(Logs.DEBUG, "GenericScanManager.startScan()");
        if (sSelectedScanManager == null) {
            log.debug(Logs.DEBUG, "startScan() failed. No scanner available");
            return false;
        }
        return sSelectedScanManager.startScan();
    }

    public boolean startCameraScan() {
        log.debug(Logs.DEBUG, "GenericScanManager.startCameraScan()");
        if (sSelectedCameraScanManager == null) {
            log.debug(Logs.DEBUG, "startCameraScan() failed. No camera scanner available");
            return false;
        }
        return sSelectedCameraScanManager.startScan();
    }

    public boolean isScannerEnabled() {
        return sSelectedScanManager != null && sSelectedScanManager.isEnabled();
    }

    public boolean isCameraScannerEnabled() {
        return sSelectedCameraScanManager != null && sSelectedCameraScanManager.isEnabled();
    }

    public boolean isScanning() {
        return sSelectedScanManager != null && sSelectedScanManager.isScanning();
    }
}
