package com.cassens.autotran.scanning;

import android.app.Activity;
import android.content.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cassens.autotran.Logs;
import com.honeywell.aidc.*;

class HoneywellScanManager extends VendorScanManager implements BarcodeReader.BarcodeListener {
    private static final Logger log = LoggerFactory.getLogger(HoneywellScanManager.class.getSimpleName());
    private AidcManager mHoneywellManager;
    private com.honeywell.aidc.BarcodeReader mHoneywellReader;
    private Context mContext;
    private boolean mScanning = false;
    private boolean mEnabled = false;

    public boolean isPresentOnDevice() {
        if(android.os.Build.MANUFACTURER.toLowerCase().contains("honeywell")) {
            log.debug(Logs.DEBUG, "honeywell scanner detected");
            return true;
        } else {
            log.debug(Logs.DEBUG, "honeywell scanner NOT detected");
            return false;
        }
    }

    public void connect(Activity context) {
        mContext = context;

        // create the AidcManager providing a Context and a CreatedCallback implementation.
        AidcManager.create(mContext, aidcManager -> {
            mHoneywellManager = aidcManager;
            // use the mHoneywellManager to create a BarcodeReader with a session
            // associated with the internal imager.
            mHoneywellReader = mHoneywellManager.createBarcodeReader();

            try {
                // apply settings
                mHoneywellReader.setProperty(com.honeywell.aidc.BarcodeReader.PROPERTY_CODE_39_ENABLED, true);
                mHoneywellReader.setProperty(com.honeywell.aidc.BarcodeReader.PROPERTY_CODE_128_ENABLED, true);
                mHoneywellReader.setProperty(com.honeywell.aidc.BarcodeReader.PROPERTY_QR_CODE_ENABLED, true);
                mHoneywellReader.setProperty(com.honeywell.aidc.BarcodeReader.PROPERTY_DATAMATRIX_ENABLED, true);

                // set the trigger mode to automatic control
                mHoneywellReader.setProperty(com.honeywell.aidc.BarcodeReader.PROPERTY_TRIGGER_CONTROL_MODE,
                        com.honeywell.aidc.BarcodeReader.TRIGGER_CONTROL_MODE_AUTO_CONTROL);
            } catch (UnsupportedPropertyException e) {
                String errorMsg = "Failed to apply properties: " + e.getClass().getName();
                GenericScanManager.getScannerCallback().onConnectFailure(errorMsg);
                return;
            }

            if (claimScanner()) {
                // register bar code event listener
                mHoneywellReader.addBarcodeListener(this);
                mEnabled = true;
                GenericScanManager.getScannerCallback().onConnectSuccess("Connected to Honeywell scanner");
            }

            //log.debug(Logs.DEBUG, "Connected to Honeywell scanner and registered callback.");
        });
    }

    public void disconnect() {

        mEnabled = false;
        try {
            if (mHoneywellReader != null) {
                releaseScanner();
                mHoneywellReader.removeBarcodeListener(this);
                mHoneywellReader.close();
            }
        } catch (Exception e) {
            log.debug(Logs.DEBUG, "Got exception closing out Honeywell reader: " + e.toString());
        }
        mHoneywellReader = null;

        try {
            if (mHoneywellManager != null) {
                // close AidcManager to disconnect from the scanner service.
                // once closed, the object can no longer be used.
                mHoneywellManager.close();
            }
        } catch (Exception e) {
            log.debug(Logs.DEBUG, "Got exception closing Honeywell manager: " + e.toString());
        }
        mHoneywellManager = null;
        //log.debug(Logs.DEBUG, "Disconnected from Honeywell scanner");
    }

    public boolean startScan() {
        String errorMsg;
        if(mHoneywellReader != null) {

            try {
                mHoneywellReader.softwareTrigger(true);
                mScanning = true;
                return true;
            } catch (ScannerNotClaimedException e) {
                e.printStackTrace();
                errorMsg = "Honeywell got ScannerNotClaimed exception";
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
                errorMsg = "Honeywell reader unavailable...";
            }
        } else {
            errorMsg = "Honeywell reader not connected... ";
        }

        GenericScanManager.getScannerCallback().onScanFailure(errorMsg);
        return false;
    }

    public boolean isScanning() {
        return mScanning;
    }

    private boolean claimScanner() {
        String errorMsg;

        try {
            mHoneywellReader.claim();

            //log.debug(Logs.DEBUG, "Claimed Honeywell scanner");
            return true;
        } catch (ScannerUnavailableException e) {
            errorMsg = "Scanner unavailable";
            e.printStackTrace();
        } catch (Exception ex) {
            errorMsg =  "Unexpected error claiming scanner " + ex.getClass().getName();
            ex.printStackTrace();
        }
        log.debug(Logs.DEBUG, errorMsg);
        GenericScanManager.getScannerCallback().onConnectFailure(errorMsg);
        return false;
    }

    private void releaseScanner() {
        //log.debug(Logs.DEBUG, "Released Honeywell scanner");
        mHoneywellReader.release();
    }

    @Override
    public void onBarcodeEvent(final BarcodeReadEvent barcodeReadEvent) {
        honeywellTriggerOff();
        String barcodeData = barcodeReadEvent.getBarcodeData();
        String timestamp = barcodeReadEvent.getTimestamp();

        //log.debug(Logs.DEBUG, "Got Honeywell barcode: " + barcodeData);
        GenericScanManager.getScannerCallback().onScanResult(barcodeData);
    }

    private void honeywellTriggerOff() {
        if(mHoneywellReader != null) {
            try {
                mHoneywellReader.softwareTrigger(false);
                mScanning = false;
            } catch (ScannerNotClaimedException e) {
                e.printStackTrace();
            } catch (ScannerUnavailableException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void onFailureEvent(final BarcodeFailureEvent barcodeFailureEvent) {

        //log.debug(Logs.DEBUG, "Encountered Honeywell barcode failure: " + barcodeFailureEvent.toString());
        honeywellTriggerOff();

        GenericScanManager.getScannerCallback().onScanFailure(barcodeFailureEvent.toString());
    }

    public boolean isEnabled() {
        return mEnabled;
    }
}
