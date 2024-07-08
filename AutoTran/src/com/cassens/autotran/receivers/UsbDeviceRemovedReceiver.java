package com.cassens.autotran.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.preference.PreferenceManager;

import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.Constants;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.cassens.autotran.hardware.PiccoloManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UsbDeviceRemovedReceiver extends BroadcastReceiver {
    public static final Logger log = LoggerFactory.getLogger(UsbDeviceRemovedReceiver.class.getSimpleName());

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent != null && intent.getAction() != null && intent.getAction().equals(UsbManager.ACTION_USB_DEVICE_DETACHED)) {
            UsbDevice usbDevice = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
            if (usbDevice.getProductName() == null || usbDevice.getProductName().toLowerCase().contains("piccolo")) {
                PreferenceManager.getDefaultSharedPreferences(context)
                        .edit()
                        .putLong(Constants.PREF_PICCOLO_UNDOCKED_TIME, System.currentTimeMillis())
                        .commit();
                log.debug(Logs.PICCOLO_IO, "Got USB_DEVICE_DETACHED event. Handheld undocked from Piccolo");
                PiccoloManager.setIsDocked(false);
                TruckNumberHandler.setPiccoloTruckNumber(context, String.valueOf(PiccoloManager.PICCOLO_CONFIRMED_UNDOCK));
            }
        }
    }
}
