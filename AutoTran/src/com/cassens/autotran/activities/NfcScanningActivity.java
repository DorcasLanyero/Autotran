package com.cassens.autotran.activities;

import android.os.Bundle;

import com.cassens.autotran.scanning.GenericScanManager;

abstract class NfcScanningActivity extends GenericScanningActivity {
    @Override
    protected void onResume() {
        super.onResume();
        pauseBarcodeCallbacks();
    }
}
