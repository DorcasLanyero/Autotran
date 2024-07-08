package com.cassens.autotran.handlers;

import android.annotation.SuppressLint;
import android.content.Context;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Looper;

import androidx.annotation.NonNull;

import com.cassens.autotran.Logs;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LocationHandler {
    private static final Logger log = LoggerFactory.getLogger(LocationHandler.class.getSimpleName());
    // TODO: Transition to Google Play FusedLocationProviderClient API (see comments bellow)
    //
    // References for FusedLocationProviderClient
    // - VehicleInspectionActivity - uses FusedLocationProviderClient already
    // - This Medium article compares LocationManager with FusedLocationProviderClient:
    //   https://maheshikapiumi.medium.com/android-location-services-7894cea13878

    public static final String TAG = "LocationHandler";

    private static LocationHandler instance = null;

    private LocationManager manager;
    private boolean receivingUpdates = false;
    private Location lastLocation = null;

    private LocationListener listener = new LocationListener() {
        public void onLocationChanged(Location location) {
            //log.debug(Logs.DEBUG, String.format("JUNK: Got location update %f,%f", location.getLatitude(), location.getLongitude()));
            setLocation(location);
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
            restartLocationTracking();
        }

        public void onProviderDisabled(String provider) {
            restartLocationTracking();
        }
    };

    private LocationHandler(Context ctx) {
        this.setManager(ctx);
    }

    public static LocationHandler getInstance(Context ctx) {
        if (instance == null) {
            instance = new LocationHandler(ctx);
        }
        return instance;
    }

    private void setManager(Context context) {
        this.manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    private void setLocation(Location location) {
        this.lastLocation = location;
    }

    @SuppressLint("MissingPermission")
    @NonNull
    public Location getLocation() {
        if (this.receivingUpdates && this.lastLocation != null) {
            //log.debug(Logs.DEBUG, "JUNK: returning last location from listener");
            return this.lastLocation;
        } else {
            //log.debug(Logs.DEBUG, "JUNK: returning last location from best provider");
            String bestProvider = this.manager.getBestProvider(new Criteria(), true);
            Location l = null;
            if (bestProvider != null) {
                // Note that getLastKnownLocation() can return null
                l = this.manager.getLastKnownLocation(bestProvider);
            }
            if (l == null) {
                l = new Location("none");
                l.setLatitude(0);
                l.setLongitude(0);
            }
            return l;
        }
    }

    private void restartLocationTracking() {
        this.stopLocationTracking();
        this.startLocationTracking();
    }

    @SuppressLint("MissingPermission")
    public void startLocationTracking(Looper looper) {
        String bestProvider = this.manager.getBestProvider(new Criteria(), true);
        if(bestProvider != null) {
            if (looper == null) {
                this.manager.requestLocationUpdates(bestProvider, 0, 0, this.listener);
            }
            else {
                this.manager.requestLocationUpdates(bestProvider, 0, 0, this.listener, looper);
            }
            receivingUpdates = true;
        }
    }

    @SuppressLint("MissingPermission")
    public void startLocationTracking() {
        startLocationTracking(null);
    }

    public void stopLocationTracking() {
        this.manager.removeUpdates(this.listener);
        this.receivingUpdates = false;

    }

    public boolean isReceivingUpdates() {
        return this.receivingUpdates;
    }
}

/*
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

public class MainActivity extends AppCompatActivity implements LocationListener {

    private static final String TAG = "LocationUpdates";
    private static final int REQUEST_LOCATION_PERMISSION = 1;
    private static final long INTERVAL = 5 * 60 * 1000; // 5 minutes in milliseconds
    private LocationManager locationManager;
    private Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);

        // Check if location permission is granted
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            // Request location permission
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    REQUEST_LOCATION_PERMISSION);
        } else {
            startLocationUpdates();
        }

        // Register a receiver to handle device boot completed event
        IntentFilter filter = new IntentFilter(Intent.ACTION_BOOT_COMPLETED);
        BroadcastReceiver receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
                    startLocationUpdates();
                }
            }
        };
        registerReceiver(receiver, filter);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_LOCATION_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startLocationUpdates();
            } else {
                Log.d(TAG, "Location permission denied");
            }
        }
    }

    private void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            return;
        }

        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, INTERVAL, 0, this);
        Log.d(TAG, "Location updates started");
    }

    private void stopLocationUpdates() {
        locationManager.removeUpdates(this);
        Log.d(TAG, "Location updates stopped");
    }

    @Override
    public void onLocationChanged(Location location) {
        // Log the GPS location to logcat
        Log.d(TAG, "Latitude: " + location.getLatitude() + ", Longitude: " + location.getLongitude());

        // Reschedule the task to run again in 5 minutes
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                startLocationUpdates();
            }
        }, INTERVAL);
    }

    @Override
    public void onProviderEnabled(String provider) {
        // Do nothing
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Stop location updates if GPS is disabled
        if (!Settings.Secure.isLocationProviderEnabled(getContentResolver(), LocationManager.GPS_PROVIDER)) {
            stopLocationUpdates();

 */
