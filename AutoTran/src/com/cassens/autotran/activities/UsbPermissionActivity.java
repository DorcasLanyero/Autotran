package com.cassens.autotran.activities;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.text.TextUtils;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.LoadEvent;
import com.cassens.autotran.data.remote.sync.SyncManager;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.cassens.autotran.hardware.PiccoloManager;
import com.sdgsystems.util.SimpleTimeStamp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import static com.cassens.autotran.receivers.UsbPermissionReceiver.ACTION_USB_PERMISSION;

public class UsbPermissionActivity extends Activity {
    private static final Logger log = LoggerFactory.getLogger(UsbPermissionActivity.class.getSimpleName());
    private static final String PICCOLO_CONNECTED_EVENT_TAG = "PICCOLO-CONNECTED";
    private static final String PICCOLO_ERROR_EVENT_TAG = "PICCOLO-ERROR";

    private static final String REFRESH_LOCATION = "com.cassens.autotran.action.REFRESH_LOCATION";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent != null && intent.getAction() != null) {
            if (intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_ATTACHED)) {
                log.debug(Logs.DEBUG, "Got intent " + intent.getAction());

                UsbDevice usbPiccolo = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                log.debug(Logs.DEBUG, "USB device: " + usbPiccolo.getVendorId() + ":" + usbPiccolo.getProductId() + " - " + usbPiccolo.getDeviceName());

                log.debug(Logs.DEBUG, "Found a piccolo, broadcasting detection intent");
                Intent piccoloDetectedIntent = new Intent(Constants.PICCOLO_DETECTED);
                sendBroadcast(piccoloDetectedIntent);

                UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);

                PiccoloManager.setIsDocked(true);

                if(usbManager.hasPermission(usbPiccolo)) {
                    PiccoloManager.requestTruckNum(this, usbPiccolo);
                    log.debug(Logs.PICCOLO_IO, "Found a piccolo, HAD PERMISSION");

                } else {
                    usbManager.requestPermission(usbPiccolo, PendingIntent.getBroadcast(this, 0, new Intent(ACTION_USB_PERMISSION), PendingIntent.FLAG_IMMUTABLE));
                    log.debug(Logs.PICCOLO_IO, "Found a piccolo, NO PERMISSION!!!");
                }

                //send event on connection
                //sendConnectionEvent();
            }
            else if (intent.getAction().equals(REFRESH_LOCATION)) {

            }
        }

        moveTaskToBack(false);
        finish();
    }

    private void sendErrorEvent() {
        SimpleTimeStamp sts = new SimpleTimeStamp();
        String eventString = TextUtils.join(",",
                new String[]{
                        PICCOLO_ERROR_EVENT_TAG,
                        TruckNumberHandler.getTruckNumber(this),
                        CommonUtility.getDriverNumber(this),
                        //CommonUtility.getOpenLoadNumber(this),
                        sts.getUtcDateTime(),
                        sts.getUtcTimeZone()
                });
        LoadEvent dvEvent = new LoadEvent();
        dvEvent.csv = eventString;
        DataManager.insertLoadEvent(this, dvEvent);
        SyncManager.pushLoadEventsLatched(this);
    }

    private void sendConnectionEvent() {
        //this should include: truck number, driver number, open load number(s), VIN# (if have one),
        Date now = new Date();
        SimpleTimeStamp sts = new SimpleTimeStamp();;

        String eventString = TextUtils.join(",",
                new String[]{
                        PICCOLO_CONNECTED_EVENT_TAG,
                        TruckNumberHandler.getTruckNumber(this),
                        CommonUtility.getDriverNumber(this),
                        //CommonUtility.getOpenLoadNumber(this),
                        sts.getUtcDateTime(),
                        sts.getUtcTimeZone()
                });
        LoadEvent dvEvent = new LoadEvent();
        dvEvent.csv = eventString;
        DataManager.insertLoadEvent(this, dvEvent);
        SyncManager.pushLoadEventsLatched(this);
    }
}
