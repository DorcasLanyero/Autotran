package com.cassens.autotran.scanning;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cassens.autotran.Logs;

import com.panasonic.toughpad.android.api.ToughpadApi;
import com.panasonic.toughpad.android.api.ToughpadApiListener;
import com.panasonic.toughpad.android.api.barcode.BarcodeData;
import com.panasonic.toughpad.android.api.barcode.BarcodeException;
import com.panasonic.toughpad.android.api.barcode.BarcodeListener;
import com.panasonic.toughpad.android.api.barcode.BarcodeReader;
import com.panasonic.toughpad.android.api.barcode.BarcodeReaderManager;

import java.util.List;
import java.util.concurrent.TimeoutException;

class PanasonicScanManager extends VendorScanManager implements ToughpadApiListener, BarcodeListener {
    private static final Logger log = LoggerFactory.getLogger(PanasonicScanManager.class.getSimpleName());
    private Context mContext;
    private boolean mScanning = false;
    private boolean mEnabled = false;

    public boolean isPresentOnDevice() {
        if(android.os.Build.MANUFACTURER.toLowerCase().contains("panasonic")) {
            return true;
        } else {
            return false;
        }
    }


    public void connect(Activity context) {
        mContext = context;
        if (!ToughpadApi.isAlreadyInitialized()) {
            ToughpadApi.initialize(mContext, (ToughpadApiListener)this);
        }
    }

    public void disconnect() {
        ToughpadApi.destroy();
        mEnabled = false;
    }

    public boolean startScan() {
        String errorMsg = "Panasonic reader not connected... ";

        if(selectedReader != null && selectedReader.isEnabled()) {
            try {
                mScanning = true;
                selectedReader.pressSoftwareTrigger(true);

                final Handler handler = new Handler();
                handler.postDelayed(() -> {
                    if(mScanning) {
                        try {
                            mScanning = false;
                            selectedReader.pressSoftwareTrigger(false);
                        } catch (BarcodeException e) {
                            e.printStackTrace();
                            GenericScanManager.getScannerCallback().onScanFailure("BarcodeException on pressSoftwareTrigger(): " + e.toString());
                        }
                    }
                }, 3000);

            } catch (BarcodeException e) {
                e.printStackTrace();
                errorMsg = "Scan failed: " + e.getClass().getName();
            }
        }
        GenericScanManager.getScannerCallback().onScanFailure(errorMsg);
        return false;
    }

    public boolean isScanning() {
        return mScanning;
    }

    private BarcodeReader selectedReader = null;


    @Override
    public void onApiConnected(int i) {
        List<BarcodeReader> readers = BarcodeReaderManager.getBarcodeReaders();

        //mTriggeredConnection = false;

        if(readers.size() > 0) {
            selectedReader = readers.get(0);
            selectedReader.clearBarcodeListener();

            try {
                selectedReader.enable(1000);
                selectedReader.addBarcodeListener(this);
                selectedReader.setHardwareTriggerEnabled(true);
                String msg = "Connected to reader " + selectedReader.getDeviceName() + " chosen from a list of " + readers.size();
                log.debug(Logs.DEBUG, msg);
                mEnabled = true;
                GenericScanManager.getScannerCallback().onConnectSuccess(msg);
            } catch (BarcodeException e) {
                e.printStackTrace();
                GenericScanManager.getScannerCallback().onConnectFailure("Panasonic reader connect failed: " + e.getMessage());
            } catch (TimeoutException e) {
                e.printStackTrace();
                GenericScanManager.getScannerCallback().onConnectFailure("Panasonic reader connect failed: " + e.getMessage());
            }
        } else {
            String errorMsg = "No readers detected after connecting to toughpad api...";
            log.debug(Logs.DEBUG, errorMsg);
            GenericScanManager.getScannerCallback().onConnectFailure(errorMsg);
        }
    }

    @Override
    public void onApiDisconnected() {
        //mTriggeredConnection = false;

        if(selectedReader != null && selectedReader.isEnabled()) {
            selectedReader.removeBarcodeListener(this);

            try {
                selectedReader.disable();
                selectedReader.setHardwareTriggerEnabled(false);
            } catch (BarcodeException e) {
                e.printStackTrace();
            }
        }

        selectedReader = null;
    }

    @Override
    public void onRead(BarcodeReader barcodeReader, BarcodeData barcodeData) {
        mScanning = false;
        if(selectedReader != null && selectedReader.isEnabled()) {
            try {
                selectedReader.pressSoftwareTrigger(false);
            } catch (BarcodeException e) {
                e.printStackTrace();
            }
        }
        GenericScanManager.getScannerCallback().onScanResult(barcodeData.getTextData());
    }

    public boolean isEnabled() {
        return mEnabled;
    }
}
