package com.cassens.autotran.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;

import com.cassens.autotran.Logs;
import com.cassens.autotran.hardware.PiccoloManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsbPermissionReceiver extends BroadcastReceiver {

    public static final String ACTION_USB_PERMISSION = "com.cassens.autotran.usb_permission";

    private static final Logger log = LoggerFactory.getLogger(UsbPermissionReceiver.class.getSimpleName());

    @Override
    public void onReceive(Context context, Intent intent) {
        log.debug(Logs.DEBUG, "Got intent " + intent.getAction());

        String action = intent.getAction();
        if (ACTION_USB_PERMISSION.equals(action)) {
            synchronized (this) {
                UsbDevice device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);

                if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                    if(device != null){
                        //Do the piccolo communication thing
                        log.debug(Logs.DEBUG, "Requesting piccolo info");
                        PiccoloManager.requestTruckNum(context, device);
                    }
                }
                else {
                    log.debug(Logs.DEBUG, "permission denied for device " + device);
                }
            }
        }
    }
}
