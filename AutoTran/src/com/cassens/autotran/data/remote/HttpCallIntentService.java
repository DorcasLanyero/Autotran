package com.cassens.autotran.data.remote;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.ConvertStreamToString;
import com.cassens.autotran.Logs;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.dto.Delivery;
import com.cassens.autotran.data.model.dto.Image;
import com.cassens.autotran.data.model.dto.Load;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleTimeStamp;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.NoHttpResponseException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.zip.GZIPInputStream;


public class HttpCallIntentService extends IntentService {
    private static final Logger log = LoggerFactory.getLogger(HttpCallIntentService.class.getSimpleName());

    private static String TAG = "HttpCallIntentService";

    public static final String ACTION_POST = "com.cassens.autotran.data.remote.action.POST";
    public static final String ACTION_FINISHED = "com.cassens.autotran.data.remote.httpcallintentservice.FINISHED";

    public static final String EXTRA_OBJECT_ID = "com.cassens.autotran.data.remote.extra.ID";
    public static final String EXTRA_TYPE = "com.cassens.autotran.data.remote.extra.TYPE";
    public static final String EXTRA_URL = "com.cassens.autotran.data.remote.extra.URL";
    public static final String EXTRA_BODY = "com.cassens.autotran.data.remote.extra.BODY";
    public static final String EXTRA_REFETCH = "com.cassens.autotran.data.remote.extra.REFETCH";

    private static String placeholderText = "image_placeholder";

    private static Intent intentFactory(Context context, int id, Class type, String url, String body, boolean refetchData) {
        Intent intent = new Intent(context, HttpCallIntentService.class);
        intent.setAction(ACTION_POST);
        intent.putExtra(EXTRA_OBJECT_ID, id);
        intent.putExtra(EXTRA_TYPE, type.toString());
        intent.putExtra(EXTRA_URL, url);
        intent.putExtra(EXTRA_BODY, body);
        intent.putExtra(EXTRA_REFETCH, refetchData);

        return intent;
    }

    public static boolean makeJsonRequest(Context context, String url, Object body, Class type, int id) {

        if(CommonUtility.isConnected(context)) {
            url = generateDebugUrl(url, context, type, body);
            log.debug(Logs.DEBUG, "url: " + url);

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(ShuttleMove.class, new GsonTypeAdapters.ShuttleMoveSerializer())
                    .registerTypeAdapter(Date.class, new GsonTypeAdapters.DateSerializer())
                    .create();
            String requestBody = "";
            try {
                requestBody = gson.toJson(new MessageWrapper(url, body, context), MessageWrapper.class);
                log.debug(Logs.DEBUG, "JSON request: " + requestBody);
            } catch (Exception e) {
                e.printStackTrace();
            }

            context.startService(intentFactory(context, id, type, url, requestBody, false));
            return true;
        } else {
            log.debug(Logs.DEBUG, "Not connected, not posting to : " + url);
            log.debug(Logs.UPLOAD, "Not connected, not posting to : " + url);
            CommonUtility.uploadLogMessage("Not connected, not posting to : " + url);
            return false;
        }
    }

    public HttpCallIntentService() {
        super("HttpCallIntentService");
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_POST.equals(action)) {
                final int id = intent.getIntExtra(EXTRA_OBJECT_ID, -1);
                final String type = intent.getStringExtra(EXTRA_TYPE);
                final String url = intent.getStringExtra(EXTRA_URL);
                final String body = intent.getStringExtra(EXTRA_BODY);
                final boolean refetch = intent.getBooleanExtra(EXTRA_REFETCH, false);
                handleActionPost(url, body, type, id, refetch);

            }
        }
    }

    private void handleActionPost(String url, String body, String type, int id, boolean refetch) {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);

        HttpPost post;
        try {
            post = new HttpPost(url);
        } catch (IllegalArgumentException e) {
            log.debug("Call failed due to incorrect arguments: " + e);
            return;
        }

        if(refetch) {
            log.debug(Logs.DEBUG, "Refetching data for type " + type + " not currently supported");
        }

        log.debug(Logs.DEBUG, "creating byte array entity");
            if(post != null) {
                post.setEntity(new ByteArrayEntity(body.getBytes()));
            }
        log.debug(Logs.DEBUG, "created byte array entity");;

        String result = "";

        log.debug(Logs.DEBUG, "outputting portion of body to log...");
        if(body.length() > 2000) {
            log.debug(Logs.UPLOAD, "URL: " + url);
            log.debug(Logs.UPLOAD, body.substring(0, 255));
            log.debug(Logs.DEBUG, body.substring(0, 255));
        } else {
            log.debug(Logs.UPLOAD, "URL: " + url);
            log.debug(Logs.UPLOAD, body);
            log.debug(Logs.DEBUG, body);
        }

        try {
            HttpResponse response = CommonUtility.tryURL(getApplicationContext(), client, post);
            log.debug(Logs.DEBUG, "finished executing call to " + url);

            if (response != null) {
                int statusCode = 0;
                if (response.getStatusLine() != null) {
                    statusCode = response.getStatusLine().getStatusCode();
                }
                result = "Status code: " + statusCode;
                switch (statusCode){
                    case 400:
                        result += "The server could not understand the request due the syntax error. The url you provided may not have been correct";
                        log.debug(Logs.DEBUG, result);
                        break;
                    case 401:
                        result += "You may need to authenticate in order to access this resource.";
                        log.debug(Logs.DEBUG, result);
                        break;
                    case 404:
                        result += "This specific resource that you are looking for cannot be found.";
                        log.debug(Logs.DEBUG, result);
                        break;
                    default:
                        InputStream in = response.getEntity().getContent();
                        Header contentEncoding = response.getFirstHeader("Content-Encoding");

                        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {

                            log.debug(Logs.DEBUG, "*********** using gzip encoding");
                            in = new GZIPInputStream(in);
                        } else {
                            log.debug(Logs.DEBUG, "*********** NOT using gzip encoding");
                        }

                        result = ConvertStreamToString.convertStreamToString(in);

                        log.debug(Logs.DEBUG, result);
                }
            }
        } catch (ClientProtocolException e) {
            log.debug("Web Service call failed: " + e.toString());
            result = "Web Service call failed: " + e.toString();
        } catch (IOException e) {
            log.debug("Web Service call failed: " + e.toString());
            result = "Web Service call failed: " + e.toString();
        } catch (IllegalArgumentException e) {
            log.debug("Web Service call failed: " + e.toString());
            result = "Web Service call failed: " + e.toString();
        } catch (Exception e) {
            result = "Web Service call failed: " + e.toString();
            log.debug("Web Service call failed: " + e.toString());
        }


        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(ACTION_FINISHED);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(EXTRA_OBJECT_ID, id);
        broadcastIntent.putExtra(EXTRA_TYPE, type);
        broadcastIntent.putExtra(EXTRA_BODY, result);
        sendBroadcast(broadcastIntent);
    }

    private static class MessageWrapper {
        public String token, timestamp, timezone, driver_number, truck_number, version, serial;
        public Object data;

        public MessageWrapper(String HOST_URL, Object payload, Context context) throws UnsupportedEncodingException, PackageManager.NameNotFoundException {
            Long timestampLong = System.currentTimeMillis() / 1000;
            this.token = new TokenGenerator().getToken(HOST_URL, timestampLong);
            this.timestamp = timestampLong.toString();
            this.timezone = SimpleTimeStamp.getUtcTimeZoneCode();
            this.driver_number = CommonUtility.getDriverNumber(context);
            this.truck_number = TruckNumberHandler.getTruckNumber(context);
            this.version = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
            this.serial = CommonUtility.getDeviceSerial();
            this.data = payload;
        }
    }

    private static String generateDebugUrl(String url, Context ctx, Class type, Object body) {

        url += "?";

        try {
            url += "version=" + ctx.getPackageManager().getPackageInfo(ctx.getPackageName(), 0).versionName + "";
        } catch (PackageManager.NameNotFoundException e) {
            url += "version=unknown";
        }
        String macAddress = PreferenceManager.getDefaultSharedPreferences(ctx).getString("MAC_ADDRESS", "unknown_address");
        url += "&mac_address=" +macAddress;

        url += "&serial_number=" + CommonUtility.getDeviceSerial();

        switch(type.getSimpleName()) {
            case "Load":
                Load load = (Load) body;

                /*
                //We're not including driver number here since we display it elsewhere...
                //com.cassens.autotran.data.model.Load lLoad = DataManager.getLoad(ctx, load.getLoad_id());
                if (lLoad.driver != null && lLoad.driver.driverNumber != null) {
                    url += "&driver=" + lLoad.driver.driverNumber;
                }*/

                url += "&load=" + load.getLdnbr();
                break;
            case "Delivery":
                Delivery delivery = (Delivery) body;
                com.cassens.autotran.data.model.Load dLoad = DataManager.getLoadForRemoteId(ctx, String.valueOf(delivery.getLoad_id()));
                com.cassens.autotran.data.model.Delivery dDelivery = DataManager.getDelivery(ctx, delivery.getDelivery_id());
                if (dLoad != null && dLoad.driver != null && dLoad.driver.driverNumber != null) {
                    url += "&driver=" + dLoad.driver.driverNumber;
                }

                if(dLoad != null && dLoad.loadNumber != null) {
                    url += "&load=" + dLoad.loadNumber;
                }

                if (dDelivery.dealer != null && dDelivery.dealer.dealer_remote_id != null) {
                    url += "&dealer=" + dDelivery.dealer.dealer_remote_id;
                }
                break;
            case "Image":
                Image image = (Image) body;
                com.cassens.autotran.data.model.Load iLoad = null;
                if (image.getDelivery_vin_id() != null) {
                    String delivery_vin_id = String.valueOf(image.getDelivery_vin_id());
                    if(delivery_vin_id != null) {
                        DeliveryVin dv = DataManager.getDeliveryVinForRemoteId(ctx, delivery_vin_id);
                        if(dv != null) {
                            com.cassens.autotran.data.model.Delivery d = DataManager.getDelivery(ctx, dv.delivery_id);
                            if(d != null) {
                                iLoad = DataManager.getLoad(ctx, d.load_id);
                            }
                        }
                    }
                } else if (image.getLoad_id() != null) {
                    iLoad = DataManager.getLoadForRemoteId(ctx, String.valueOf(image.getLoad_id()));
                } else if (image.getDelivery_id() != null) {
                    iLoad = DataManager.getLoad(ctx, DataManager.getDeliveryForRemoteId(ctx, String.valueOf(image.getDelivery_id())).load_id);
                }
                if (iLoad != null && !HelperFuncs.isNullOrEmpty(iLoad.loadNumber)) {
                    url += "&load=" + Uri.encode(iLoad.loadNumber);
                }
                if (!HelperFuncs.isNullOrEmpty(image.getFilename())) {
                    url += "&image=" + Uri.encode(image.getFilename());
                }
                url += "&image=" + image.getPart();
            default:
                break;
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(ctx);

        if(prefs != null) {
            String currentDriverID = CommonUtility.getDriverNumber(ctx);
            String currentTruckID = TruckNumberHandler.getTruckNumber(ctx);



            if (currentDriverID != null && currentTruckID != null) {
                url += "&currentDriverID=" + currentDriverID + "&currentTruckID=" + currentTruckID;
            }

            /*String mac = prefs.getString("MAC_ADDRESS", "");
            if(mac != null && !mac.equals("")) {
                url += "&MAC=" + mac;
            }*/
        }

        return url;
    }
}
