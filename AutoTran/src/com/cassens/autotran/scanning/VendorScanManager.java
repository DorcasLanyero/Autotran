package com.cassens.autotran.scanning;

import android.app.Activity;

abstract class VendorScanManager {

    public abstract boolean isPresentOnDevice();
    public boolean isCameraScanner() {
        return false;
    }
    public abstract void connect(Activity context);
    public abstract void disconnect();
    public abstract boolean isEnabled();
    public abstract boolean startScan();
    public abstract boolean isScanning();
}