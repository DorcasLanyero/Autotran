package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.nfc.NdefMessage;
import android.nfc.NdefRecord;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.MifareUltralight;
import android.nfc.tech.Ndef;
import android.nfc.tech.NdefFormatable;
import android.nfc.tech.NfcA;
import android.nfc.tech.NfcV;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcelable;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.R;
import com.cassens.autotran.scanning.GenericScanManager;
import com.sdgsystems.util.HelperFuncs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;
import java.util.Date;

//import com.honeywell.aidc.*;


public abstract class GenericScanningActivity extends AutoTranActivity implements GenericScanManager.ScannerCallback, View.OnKeyListener {
    private static final Logger log = LoggerFactory.getLogger(GenericScanningActivity.class.getSimpleName());

    private static final int VIN_OCR_REQUEST = 10541;

    protected boolean mManualEntryEnabled = false;
    protected enum ScanDataType { GENERAL, FULL_VIN, PARTIAL_VIN}
    protected ScanDataType mScanDataType = ScanDataType.GENERAL;

    private ScanDataType prevScanDataType = mScanDataType;
    private boolean prevManualEntryEnabled = mManualEntryEnabled;

    //NFC fields
    protected PendingIntent mPendingIntent;
    protected NfcAdapter mNfcAdapter;
    protected String[][] mTechListsArray;
    protected IntentFilter[] mIntentFiltersArray;

    protected static final int ENABLED_SCANNER_NFC = 1;
    protected static final int ENABLED_SCANNER_BARCODE = 2;
    protected static final int ALL_SCANNER_TYPES = ENABLED_SCANNER_NFC | ENABLED_SCANNER_BARCODE;
    protected int mEnabledScannerTypes;

    protected int getEnabledScannerTypes() {
        return ALL_SCANNER_TYPES;
    }


    public boolean isNfcScanningEnabled() {
        return (mEnabledScannerTypes & ENABLED_SCANNER_NFC) != 0;
    }

    public boolean isBarcodeScanningEnabled() {
        return (mEnabledScannerTypes & ENABLED_SCANNER_BARCODE) != 0;
    }

    private GenericScanManager mScanManager;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mEnabledScannerTypes = getEnabledScannerTypes();
        if (isBarcodeScanningEnabled()) {
            mScanManager = GenericScanManager.connect(this, this);
        }
    }

    @Override
    protected void onDestroy() {
        if (isBarcodeScanningEnabled()) {
            mScanManager.disconnect();
        }
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (isNfcScanningEnabled()) {
            registerForNFC();
        }
        if (isBarcodeScanningEnabled()) {
            mScanManager.resumeCallbacks();
        }
    }

    @Override
    protected void onPause() {
        if (isNfcScanningEnabled()) {
            unregisterForNFC();
        }
        // Do not pause barcode callbacks here.
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(requestCode == VIN_OCR_REQUEST) {
            if(resultCode == Activity.RESULT_OK) {
                String vin = data.getStringExtra(VinOCRActivity.DATA_SCANNED_VIN);
                onScanResult(vin);
            }
            else {
                onScanFailure("OCR scan canceled");
            }
        }
    }

    protected boolean isScannerEnabled() {
        return mScanManager.isScannerEnabled();
    }

    protected void pauseBarcodeCallbacks() {
        mScanManager.pauseCallbacks();
    }

    protected void startScan() {
        if (!mScanManager.startScan()) {
            CommonUtility.showText("Panasonic barcode scanner not connected... ");
            onScanFailure("");
        }
    }

    protected boolean isCameraScannerEnabled() {
        return mScanManager.isCameraScannerEnabled();
    }

    protected void startCameraScan() {
        mScanManager.startCameraScan();
    }

    protected void startOCRScan() {
        Intent i = new Intent(this, VinOCRActivity.class);
        startActivityForResult(i, VIN_OCR_REQUEST);
    }

    protected boolean isScanning() {
        return mScanManager.isScanning();
    }

    /**
     * Override this method to receive data from scans while running on the UI thread.
     *
     * @param scannedValue
     **/
    protected void onScanResultRunOnUiThread(String scannedValue) throws ScannedValueException {
        log.debug(Logs.DEBUG, "onScanResultRunOnUiThread() default callback: " + scannedValue);
    }

    /**
     * Override this method to receive data from scans while still running on the background thread.
     *
     * @param scannedValue
     **/
    public void onScanResult(final String scannedValue) {
        runOnUiThread(() -> {
            try {
                String processedScanValue;
                if (mScanDataType == ScanDataType.FULL_VIN || mScanDataType == ScanDataType.PARTIAL_VIN) {
                    processedScanValue = CommonUtility.processScannedVIN(scannedValue);
                }
                else {
                    processedScanValue = scannedValue;
                }
                validateScannedValue(processedScanValue);
                onScanResultRunOnUiThread(processedScanValue);
            }
            catch (ScannedValueException sve) {
                scanEntryDialog(sve.getFailureMessage(), scannedValue);
            }
        });
    }

    /**
     * Override this method to receive scan failure notifications on the UI thread.
     *
     * @param errorMsg
     **/
    protected void onScanFailureRunOnUiThread(String errorMsg) {
        log.debug(Logs.DEBUG, "onScanFailureRunOnUiThread() default callback: " + errorMsg);
        scanEntryDialog("No value was scanned", null);
    }


    /**
     * Override this method to receive data from scans while still running on the background thread.
     *
     * @param errorMsg
     **/
    public void onScanFailure(String errorMsg) {
        log.debug(Logs.DEBUG, "Scan failed: " + errorMsg);
        runOnUiThread(() -> {
            log.debug(Logs.DEBUG, errorMsg);

            onScanFailureRunOnUiThread(errorMsg);
        });

    }

    /**
     * Override this method to receive connect success notification.
     *
     * @param msg
     **/
    protected void onConnectSuccessRunOnUiThread(String msg) {
        log.debug(Logs.DEBUG, "onConnectSuccessRunOnUiThread() default callback: " + msg);
    }

    /**
     * Override this method to connect success notifications while still running on the background thread.
     *
     * @param msg
     **/
    public void onConnectSuccess(String msg) {
        runOnUiThread(() -> {
            log.debug(Logs.DEBUG, msg);

            onConnectSuccessRunOnUiThread(msg);
        });

    }

    /**
     * Override this method to receive connect success notification.
     *
     * @param errorMsg
     **/
    protected void onConnectFailureRunOnUiThread(String errorMsg) {
        log.debug(Logs.DEBUG, "onConnectFailureRunOnUiThread() default callback: " + errorMsg);
    }

    /**
     * Override this method to connect success notifications while still running on the background thread.
     *
     * @param errorMsg
     **/
    public void onConnectFailure(String errorMsg) {
        runOnUiThread(() -> {
            log.debug(Logs.DEBUG, errorMsg);

            onConnectFailureRunOnUiThread(errorMsg);
        });

    }

    @Override
    public boolean onKeyDown (int keyCode, KeyEvent event) {
        //Log.d(TAG, "onKeyDown: " + event);
        if(keyCode == 303 || keyCode == 305) {
            if(CommonUtility.hasPanasonicScanner() && !mScanManager.isScanning()) {
                //triggerPanasonicScan();
                if (!mScanManager.startScan()) {
                    CommonUtility.showText("Panasonic barcode scanner not connected... ");
                }
                return true;
            }
        } else if(isCharacterKey(keyCode)) {
            keyPressed(event);
            return true;
        } else if(keyCode == KeyEvent.KEYCODE_BACK) {
            back(null);
        }

        return true;
    }

    String mKeyedCode = null;

    protected void back(View view) {
        CommonUtility.logButtonClick(log, "Back");
        onBackPressed();
    }

    @Override
    public boolean onKey(View v, int keyCode, KeyEvent event) {
        //Log.d(TAG, "onKey: " + event);
        if(event.getAction() == KeyEvent.ACTION_UP && isCharacterKey(keyCode)) {
            keyPressed(event);
        }

        return true;
    }

    Long mLastKeyPresstime;
    Handler mKeyedCodeHandler = null;
    Runnable mKeyedCodeRunnable = null;

    private void keyPressed(KeyEvent event) {
        char character = (char) event.getUnicodeChar();

        //Log.d(TAG, "Pressed a character key: " + character);

        long scanTime = new Date().getTime();

        if (mKeyedCode == null) {
            mKeyedCode = "";
        }

        if (mLastKeyPresstime == null) {
            mLastKeyPresstime = 0L;
        }

        //If characters are coming in at 800 millis or less, add them to the latest

        if (scanTime - mLastKeyPresstime < 800) {

            mKeyedCode += String.valueOf(character);

            if(mKeyedCodeHandler != null && mKeyedCodeRunnable != null) {
                mKeyedCodeHandler.removeCallbacks(mKeyedCodeRunnable);
                mKeyedCodeHandler.postDelayed(mKeyedCodeRunnable, 1000);
            }
        } else {
            if(mKeyedCodeHandler != null && mKeyedCodeRunnable != null) {
                mKeyedCodeHandler.removeCallbacks(mKeyedCodeRunnable);
            }

            mKeyedCodeHandler = new Handler();

            mKeyedCode = String.valueOf(character);

            mKeyedCodeRunnable = () -> {
                //Log.d(TAG, "Keyed scan result: " + mKeyedCode);
                GenericScanningActivity.this.onScanResult(mKeyedCode);
            };

            mKeyedCodeHandler.postDelayed(mKeyedCodeRunnable, 1000);
        }

        mLastKeyPresstime = scanTime;
    }

    private boolean isCharacterKey(int keyCode) {

        switch (keyCode) {
            case KeyEvent.KEYCODE_0:
            case KeyEvent.KEYCODE_1:
            case KeyEvent.KEYCODE_2:
            case KeyEvent.KEYCODE_3:
            case KeyEvent.KEYCODE_4:
            case KeyEvent.KEYCODE_5:
            case KeyEvent.KEYCODE_6:
            case KeyEvent.KEYCODE_7:
            case KeyEvent.KEYCODE_8:
            case KeyEvent.KEYCODE_9:
            case KeyEvent.KEYCODE_A:
            case KeyEvent.KEYCODE_B:
            case KeyEvent.KEYCODE_C:
            case KeyEvent.KEYCODE_D:
            case KeyEvent.KEYCODE_E:
            case KeyEvent.KEYCODE_F:
            case KeyEvent.KEYCODE_G:
            case KeyEvent.KEYCODE_H:
            case KeyEvent.KEYCODE_I:
            case KeyEvent.KEYCODE_J:
            case KeyEvent.KEYCODE_K:
            case KeyEvent.KEYCODE_L:
            case KeyEvent.KEYCODE_M:
            case KeyEvent.KEYCODE_N:
            case KeyEvent.KEYCODE_O:
            case KeyEvent.KEYCODE_P:
            case KeyEvent.KEYCODE_Q:
            case KeyEvent.KEYCODE_R:
            case KeyEvent.KEYCODE_S:
            case KeyEvent.KEYCODE_T:
            case KeyEvent.KEYCODE_U:
            case KeyEvent.KEYCODE_V:
            case KeyEvent.KEYCODE_W:
            case KeyEvent.KEYCODE_X:
            case KeyEvent.KEYCODE_Y:
            case KeyEvent.KEYCODE_Z:
                return true;
        }

        return false;
    }

    private void registerForNFC() {
        mPendingIntent = PendingIntent.getActivity(
                this, 0, new Intent(this, getClass()).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP), PendingIntent.FLAG_MUTABLE);

        mNfcAdapter = NfcAdapter.getDefaultAdapter(this);

        IntentFilter ndef = new IntentFilter(NfcAdapter.ACTION_NDEF_DISCOVERED);
        try {
            ndef.addDataType("*/*");    /* Handles all MIME based dispatches.
                                       You should specify only the ones that you need. */
        }
        catch (IntentFilter.MalformedMimeTypeException e) {
            throw new RuntimeException("fail", e);
        }
        mIntentFiltersArray = new IntentFilter[] {ndef, };

        String[] ndefOnly = new String[] { Ndef.class.getName() };
        String[] mifareOnly = new String[] {MifareUltralight.class.getName()};
        String[] nfcaOnly = new String[] { NfcA.class.getName() };
        String[] nfcvOnly = new String[] { NfcV.class.getName() };
        String[] ndefFormatableOnly = new String[] { NdefFormatable.class.getName() };

        mTechListsArray = new String[][] { ndefOnly, mifareOnly, nfcaOnly, nfcvOnly, ndefFormatableOnly };

        if (mNfcAdapter != null) {
            mNfcAdapter.enableForegroundDispatch(this, mPendingIntent, mIntentFiltersArray,
                                                 mTechListsArray);
        }
    }

    private void unregisterForNFC() {
        if (mNfcAdapter != null) {
            mNfcAdapter.disableForegroundDispatch(this);
            mNfcAdapter = null;
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);

        if (intent != null) {
            log.debug(Logs.DEBUG, "Got an intent: " + intent.getAction());

            if(NfcAdapter.ACTION_TECH_DISCOVERED.equals(intent.getAction()) || NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {
                log.debug(Logs.DEBUG, "NDEF Discovered...");

                Tag tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);

                if (tag != null) {

                    byte[] tagId = tag.getId();
                    String hexdump = "";
                    for (int i = 0; i < tagId.length; i++) {
                        String x = Integer.toHexString(((int) tagId[i] & 0xff));
                        if (x.length() == 1) {
                            x = '0' + x;
                        }
                        hexdump += x;
                    }

                    Ndef ndef = Ndef.get(tag);

                    if(ndef != null) {
                        NdefMessage [] messages = getNdefMessages(intent);
                        if(messages != null && messages.length > 0) {
                            hexdump = "";
                            for (int i = 0; i < messages.length; i++) {
                                for (int j = 0; j < messages[0].getRecords().length; j++) {
                                    NdefRecord record = messages[i].getRecords()[j];
                                    hexdump += new String(record.getPayload(), 1, record.getPayload().length - 1, Charset.forName("UTF-8")).substring(2);
                                }
                            }
                        }
                    }

                    log.debug(Logs.DEBUG, "Scanned " + hexdump);

                    onScanResult(hexdump);
                } else {
                    log.debug(Logs.DEBUG, "No Tag found...");
                }
            }
        }
    }

    private NdefMessage[] getNdefMessages(Intent intent) {

        NdefMessage[] message = null;
        if (NfcAdapter.ACTION_NDEF_DISCOVERED.equals(intent.getAction())) {

            Parcelable[] rawMessages = intent.getParcelableArrayExtra(NfcAdapter.EXTRA_NDEF_MESSAGES);

            if(rawMessages != null) {

                message = new NdefMessage[rawMessages.length];
                for(int i = 0; i < rawMessages.length; i++) {

                    message[i] = (NdefMessage) rawMessages[i];

                }
            }
            else {
                byte[] empty = new byte[] {};
                NdefRecord record = new NdefRecord (NdefRecord.TNF_UNKNOWN, empty, empty, empty);
                NdefMessage msg = new NdefMessage (new NdefRecord[] {record});
                message = new NdefMessage[] {msg};
            }

        }
        else {
            // Ignore it
            Log.d("", "Unknown intent.");
        }

        return message;
    }


    private AlertDialog mVinEntryDialog;


    protected static final int INVALID_VALUE_SCANNED = 2;

    public class ScannedValueException extends Exception {
        public static final int EMPTY_VALUE_SCANNED = 1;
        public static final int INVALID_VALUE_SCANNED = 2;

        protected int failureCode;
        protected String failureMessage;

        public ScannedValueException() {
            failureCode = INVALID_VALUE_SCANNED;
            failureMessage = "Invalid value scanned";
        }

        public ScannedValueException(int failureCode, String failureMessage) {
            this.failureCode = failureCode;
            this.failureMessage = failureMessage;
        }

        public String getFailureMessage() {
            return failureMessage;
        }
    }

    protected void validateScannedValue(String scannedValue) throws ScannedValueException {
        if (HelperFuncs.isNullOrEmpty(scannedValue)) {
            throw new ScannedValueException(ScannedValueException.EMPTY_VALUE_SCANNED, "Empty Value Scanned");
        }
    }

    private void showDialog(String msg, boolean cancelable) {
        AlertDialog.Builder builder = new AlertDialog.Builder(GenericScanningActivity.this);
        log.debug(Logs.INTERACTION, "Showed dialog message " + msg);
        builder.setMessage(msg);
        builder.setPositiveButton("OK", (dialog, which) -> CommonUtility.logButtonClick(log, "Ok", msg));
        builder.setCancelable(cancelable);
        builder.create().show();
    }

    private void processManualScanEntry(final String scannedValue) throws ScannedValueException {
        validateScannedValue(scannedValue);
        onScanResultRunOnUiThread(scannedValue);
    }

    protected void scanEntryDialog() {
        scanEntryDialog(false, null, null);
    }

    protected void scanEntryDialog(boolean manualEntryOpen) {
        scanEntryDialog(manualEntryOpen, null, null);
    }

    protected void scanEntryDialog(String failureMessage, String scannedValue) {
        scanEntryDialog(false, failureMessage, scannedValue);
    }

    private boolean isDialogDisplayed = false;

    protected void scanEntryDialog(boolean manualEntryOpen, String failureMessage, String scannedValue) {
        if (isDialogDisplayed) {
            mVinEntryDialog.dismiss();
            return;
        }
        isDialogDisplayed = true;

        try {
            AlertDialog.Builder builder = new AlertDialog.Builder(GenericScanningActivity.this);
            builder.setView(getLayoutInflater().inflate(R.layout.vin_manual_entry, null));
            mVinEntryDialog = builder.create();
            mVinEntryDialog.show();
        } catch (Exception e) {
            isDialogDisplayed = false;
            log.debug(Logs.DEBUG, "Unable to display scanEntryDialog. Exception: " + HelperFuncs.noNull(e.getClass().getName(), "null"));
            log.debug(Logs.DEBUG, "scanEntryDialog() failureMessage: " + HelperFuncs.noNull(failureMessage, "null"));
            log.debug(Logs.DEBUG, "scanEntryDialog() scannedValue: " + HelperFuncs.noNull(scannedValue, "null"));
            e.printStackTrace();
        }

        mVinEntryDialog.setCancelable(true);

        mVinEntryDialog.setOnKeyListener((dialogInterface, i, keyEvent) -> {
            if (!isHardwareScanButtonKeyEvent(keyEvent)) {
                //Log.d(TAG, "onKeyListener: Got OTHER event " + keyEvent.toString());
                return false;
            }
            else if (keyEvent.getRepeatCount() != 0) {
                return false;
            }
            if (keyEvent.getAction() == KeyEvent.ACTION_DOWN) {
                //Log.d(TAG, "onKeyListener: Got HW_SCAN_BUTTON PRESS. Dismissing dialog");
                mVinEntryDialog.setOnKeyListener(null);
                mVinEntryDialog.dismiss();
            }
            else if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
                //Log.d(TAG, "onKeyListener: HW_SCAN_BUTTON RELEASE " + keyEvent.toString());
            }
            return false;
        });

        Button scanButton = mVinEntryDialog.findViewById(R.id.rescanButton);
        scanButton.setOnClickListener(view -> {
            CommonUtility.logButtonClick(log, view);
            startScan();
            mVinEntryDialog.dismiss();
        });
        TextView failureMessageText = mVinEntryDialog.findViewById(R.id.failureMessageText);
        if (failureMessage == null) {
            failureMessageText.setVisibility(View.GONE);
        }
        else {
            if (HelperFuncs.isNullOrWhitespace(scannedValue)) {
                failureMessageText.setText(failureMessage);
            }
            else {
                if (CommonUtility.validVin(CommonUtility.processScannedVIN(scannedValue))) {
                    failureMessageText.setText(String.format("%s\n\n%s", failureMessage, CommonUtility.processScannedVIN(scannedValue)));
                }
                else {
                    failureMessageText.setText(String.format("%s\n\n%s", failureMessage, scannedValue));
                }
            }
            log.debug(Logs.DEBUG, "Error: " + failureMessageText.getText().toString());
            failureMessageText.setVisibility(View.VISIBLE);
            scanButton.setText("Retry Scan");
        }

        Button cameraScanButton = mVinEntryDialog.findViewById(R.id.cameraScanButton);
        if (isCameraScannerEnabled()) {
            cameraScanButton.setOnClickListener(view -> {
                CommonUtility.logButtonClick(log, view);
                startCameraScan();
                mVinEntryDialog.dismiss();
            });
            TextView tv = mVinEntryDialog.findViewById(R.id.idEngineVersion);
            tv.setText("Using ID-Engine for camera scanning");
            tv.setVisibility(View.VISIBLE);
        } else {
            cameraScanButton.setVisibility(View.GONE);
            mVinEntryDialog.findViewById(R.id.idEngineWarning).setVisibility(View.VISIBLE);
        }

        Button ocrButton = mVinEntryDialog.findViewById(R.id.ocrScanButton);
        ocrButton.setOnClickListener(view -> {
            CommonUtility.logButtonClick(log, view);
            startOCRScan();
            mVinEntryDialog.dismiss();
        });

        if (manualEntryOpen) {
            prevManualEntryEnabled = mManualEntryEnabled;
            prevScanDataType = mScanDataType;
            mManualEntryEnabled = true;
            mScanDataType = ScanDataType.FULL_VIN;
            if (!HelperFuncs.isNullOrWhitespace(scannedValue)) {
                EditText firstTenText = mVinEntryDialog.findViewById(R.id.firstTenText);
                EditText lastSevenText = mVinEntryDialog.findViewById(R.id.lastSevenText);
                if (scannedValue.length() <= 10) {
                    firstTenText.setText(scannedValue);
                    lastSevenText.setText("");
                }
                else {
                    firstTenText.setText(scannedValue.substring(0, 10));
                    lastSevenText.setText(scannedValue.substring(10, Integer.min(17, scannedValue.length())));
                }
            }
        }

        if (mManualEntryEnabled) {
            LinearLayout manualEntryLayout = mVinEntryDialog.findViewById(R.id.manualEntryLayout);
            Button manualEntryButton = mVinEntryDialog.findViewById(R.id.manualEntryButton);
            if (mScanDataType == ScanDataType.GENERAL) {
                manualEntryButton.setText("I cannot scan the barcode!");
            }
            Button dialogProceedButton = mVinEntryDialog.findViewById(R.id.proceedButton);
            dialogProceedButton.setOnClickListener(view -> {
                try {
                    failureMessageText.setText("");
                    switch (mScanDataType) {
                        case GENERAL:
                            String barcode = ((TextView) mVinEntryDialog.findViewById(R.id.generalBarcodeText)).getText().toString();
                            CommonUtility.logButtonClick(log, view, "entered: " + barcode);
                            if (HelperFuncs.isNullOrEmpty(barcode)) {
                                showDialog("Please enter a barcode", true);
                            } else {
                                processManualScanEntry(barcode);
                                mVinEntryDialog.dismiss();
                            }
                            break;

                        case FULL_VIN:
                            String firstTen = ((TextView) mVinEntryDialog.findViewById(R.id.firstTenText)).getText().toString();
                            String lastSeven = ((TextView) mVinEntryDialog.findViewById(R.id.lastSevenText)).getText().toString();
                            CommonUtility.logButtonClick(log, view, "entered: " + firstTen + " " + lastSeven);
                            if (HelperFuncs.isNullOrEmpty(firstTen) || firstTen.length() < 10) {
                                showDialog("Please enter ten characters in the first field", true);
                            } else if (HelperFuncs.isNullOrEmpty(lastSeven) || lastSeven.length() < 7) {
                                showDialog("Please enter seven characters in the second field", true);
                            } else {
                                //have valid entries, try to find matching vin
                                String compositeVin = firstTen + lastSeven;
                                if (!CommonUtility.checkVinNoPopup(compositeVin)) {
                                    showDialog(compositeVin + " is not a valid VIN", true);
                                } else {
                                    processManualScanEntry(compositeVin);
                                    mVinEntryDialog.dismiss();
                                }
                            }
                            break;

                        case PARTIAL_VIN:
                            String firstThree = ((TextView) mVinEntryDialog.findViewById(R.id.firstThreeText)).getText().toString();
                            String lastEight = ((TextView) mVinEntryDialog.findViewById(R.id.lastEightText)).getText().toString();
                            CommonUtility.logButtonClick(log, view, "entered: " + firstThree + " " + lastEight);
                            if (HelperFuncs.isNullOrEmpty(firstThree) || firstThree.length() < 3) {
                                showDialog("Please enter three characters in the first field", true);
                            } else if (HelperFuncs.isNullOrEmpty(lastEight) || lastEight.length() < 8) {
                                showDialog("Please enter eight characters in the second field", true);
                            } else {
                                //have valid entries, try to find matching vin
                                String compositeVin = firstThree + "******" + lastEight;
                                processManualScanEntry(compositeVin);
                                mVinEntryDialog.dismiss();
                            }
                            break;

                        default:
                            break;
                    }
                } catch (ScannedValueException sve) {
                    showDialog(sve.getFailureMessage(), true);
                }
            });

            if (manualEntryOpen) {
                manualEntryButton.setVisibility(View.GONE);
                enableManualEntryLayout(scannedValue);
                dialogProceedButton.setVisibility(View.VISIBLE);
            }
            else {
                manualEntryLayout.setVisibility(View.GONE);
                manualEntryButton.setVisibility(View.VISIBLE);
                manualEntryButton.setOnClickListener(view -> {
                    CommonUtility.logButtonClick(log, view);
                    failureMessageText.setVisibility(View.GONE);
                    manualEntryButton.setVisibility(View.GONE);
                    enableManualEntryLayout(scannedValue);
                    dialogProceedButton.setVisibility(View.VISIBLE);

                });
                dialogProceedButton.setVisibility(View.GONE);
            }
        }
        else {
            mVinEntryDialog.findViewById(R.id.manualEntryButton).setVisibility(View.GONE);
            mVinEntryDialog.findViewById(R.id.manualEntryLayout).setVisibility(View.GONE);
            mVinEntryDialog.findViewById(R.id.proceedButton).setVisibility(View.GONE);
        }

        mVinEntryDialog.setOnDismissListener(dialogInterface -> {
            mVinEntryDialog.setOnKeyListener(null);
            isDialogDisplayed = false;
            if (manualEntryOpen) {
                mManualEntryEnabled = prevManualEntryEnabled;
                mScanDataType = prevScanDataType;
            }
        });
    }


    private void enableManualEntryLayout(String scannedValue) {
        mVinEntryDialog.findViewById(R.id.manualEntryLayout).setVisibility(View.VISIBLE);
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        switch (mScanDataType) {
            case GENERAL:
                mVinEntryDialog.findViewById(R.id.generalBarcodeLayout).setVisibility(View.VISIBLE);
                EditText generalBarcodeText = mVinEntryDialog.findViewById(R.id.generalBarcodeText);
                if (!HelperFuncs.isNullOrEmpty(scannedValue)) {
                    generalBarcodeText.setText(scannedValue);
                }
                HelperFuncs.addAllCapsFilter(generalBarcodeText);
                generalBarcodeText.requestFocus();
                imm.showSoftInput(generalBarcodeText, InputMethodManager.SHOW_IMPLICIT);
                break;

            case FULL_VIN:
                mVinEntryDialog.findViewById(R.id.fullVinLayout).setVisibility(View.VISIBLE);
                EditText firstTenText = mVinEntryDialog.findViewById(R.id.firstTenText);
                HelperFuncs.addAllCapsFilter(firstTenText);
                EditText lastSevenText = mVinEntryDialog.findViewById(R.id.lastSevenText);
                HelperFuncs.addAllCapsFilter(lastSevenText);
                /*
                if (!HelperFuncs.isNullOrEmpty(scannedValue) && !CommonUtility.checkVinNoPopup(scannedValue)) {
                    if (scannedValue.length() <= 10) {
                        firstTenText.setText(scannedValue);
                    }
                    else {
                        firstTenText.setText(scannedValue.substring(0, 10));
                        lastSevenText.setText(scannedValue.substring(10));
                    }
                } */

                firstTenText.requestFocus();
                imm.showSoftInput(firstTenText, InputMethodManager.SHOW_IMPLICIT);
                imm.showSoftInput(lastSevenText, InputMethodManager.SHOW_IMPLICIT);

                firstTenText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (firstTenText.getText().toString().length() == 10) {
                            lastSevenText.requestFocus();
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                // TODO: Check for complete and correct VIN on each keystroke and don't make
                //       Proceed button visible until a complete, correct VIN has been entered.
                /*
                lastSevenText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                }); */

                break;

            case PARTIAL_VIN:
                mVinEntryDialog.findViewById(R.id.partialVinLayout).setVisibility(View.VISIBLE);
                EditText firstThreeText = mVinEntryDialog.findViewById(R.id.firstThreeText);
                HelperFuncs.addAllCapsFilter(firstThreeText);
                EditText lastEightText = mVinEntryDialog.findViewById(R.id.lastEightText);
                HelperFuncs.addAllCapsFilter(lastEightText);
                /*
                if (!HelperFuncs.isNullOrEmpty(scannedValue)) {
                    if (scannedValue.length() <= 3) {
                        firstThreeText.setText(scannedValue);
                    }
                    else {
                        firstThreeText.setText(scannedValue.substring(0, 3));
                        if (scannedValue.length() >= 10) {
                            lastEightText.setText(scannedValue.substring(10));
                        }
                    }
                }*/

                firstThreeText.requestFocus();
                imm.showSoftInput(firstThreeText, InputMethodManager.SHOW_IMPLICIT);
                imm.showSoftInput(lastEightText, InputMethodManager.SHOW_IMPLICIT);

                firstThreeText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                        if (firstThreeText.getText().toString().length() == 3) {
                            lastEightText.requestFocus();
                        }
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });

                // TODO: Check for complete and correct VIN on each keystroke and don't make
                //       Proceed button visible until a complete, correct VIN has been entered.
                /*
                lastEightText.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {

                    }

                    @Override
                    public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                    }

                    @Override
                    public void afterTextChanged(Editable editable) {

                    }
                });*/

                break;

            default:
                break;
        }
    }

    private boolean isHardwareScanButtonKeyEvent(KeyEvent event) {
        return event.getKeyCode() == KeyEvent.KEYCODE_UNKNOWN && (event.getScanCode() == 261 || event.getScanCode() == 257);
    }
}
