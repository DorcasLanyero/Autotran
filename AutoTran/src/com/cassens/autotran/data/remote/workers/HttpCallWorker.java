package com.cassens.autotran.data.remote.workers;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.preference.PreferenceManager;

import androidx.annotation.NonNull;
import androidx.work.Data;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import androidx.work.WorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.cassens.autotran.AutoTranApplication;
import com.cassens.autotran.backendpoc.PoCUtils;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.ConvertStreamToString;
import com.cassens.autotran.Logs;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.DeliveryVin;
import com.cassens.autotran.data.model.dto.Delivery;
import com.cassens.autotran.data.model.dto.Image;
import com.cassens.autotran.data.model.dto.Load;
import com.cassens.autotran.data.model.lookup.ShuttleMove;
import com.cassens.autotran.data.remote.GsonTypeAdapters;
import com.cassens.autotran.data.remote.TokenGenerator;
import com.cassens.autotran.data.remote.UploadResultReceiver;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleTimeStamp;
import com.sdgsystems.workmanagerhelper.WMHelperWorker;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.Date;
import java.util.List;
import java.util.zip.GZIPInputStream;


public class HttpCallWorker extends Worker {
    private static final Logger log = LoggerFactory.getLogger(HttpCallWorker.class.getSimpleName());

    private static String TAG = HttpCallWorker.class.getSimpleName();

    public static final String ACTION_FINISHED = "com.cassens.autotran.data.remote.httpcallworker.FINISHED";
    public static final String EXTRA_OBJECT_ID = "com.cassens.autotran.data.remote.extra.ID";
    public static final String EXTRA_TYPE = "com.cassens.autotran.data.remote.extra.TYPE";
    public static final String EXTRA_URL = "com.cassens.autotran.data.remote.extra.URL";
    public static final String EXTRA_BODY = "com.cassens.autotran.data.remote.extra.BODY";
    public static final String EXTRA_REFETCH = "com.cassens.autotran.data.remote.extra.REFETCH";
    public static final String EXTRA_LAMBDA_CALL = "com.cassens.autotran.data.remote.extra.LAMBDA_CALL";

    private Context context;
    public HttpCallWorker (
            @NonNull Context context,
            @NonNull WorkerParameters params) {

        super(context, params);

        this.context = context;
    }

    public static class HttpRequest {
        private String url;
        private String body;
        private String type;
        private int id;

        public HttpRequest(@NonNull String url, @NonNull String body, @NonNull String type, int id) {
            this.url = url;
            this.body = body;
            this.type = type;
            this.id = id;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getBody() {
            return body;
        }

        public void setBody(String body) {
            this.body = body;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public int getId() {
            return id;
        }

        public void setId(int id) {
            this.id = id;
        }
    };

    @Override
    public Result doWork() {

        //log.debug(Logs.DEBUG, "HTTP_WORKER: in doWork()");
        Data inputData = getInputData();

        HttpRequest httpRequest = (HttpRequest) WMHelperWorker.decodeWMRequest(context, inputData, HttpRequest.class);
        //log.debug(Logs.DEBUG, "HTTP_WORKER: HttpCallWorker.doWork() " + httpRequest.getBody());

        handleActionPost(context,
                httpRequest.url,
                httpRequest.body,
                httpRequest.type,
                httpRequest.id,
                false
                );

        // Indicate whether the work finished successfully with the Result
        // For now, we return success since other parts of the code do the
        // status checking and retries for HTTP calls.  Eventually we should
        // transition to using the built-in retry capabilities of WorkManager.
        return Result.success();
    }

    @Override
    public void onStopped() {
        log.debug(Logs.DEBUG, this.getClass().getSimpleName() + " REQUEST STOPPED " + this.getId());
    }

    public static boolean makeJsonRequest(Context context, String url, Object body, Class type, int id) {

        // TODO: Get rid of this condition and instead have WorkManager queue the request and
        //       execute whenever connection is restored.
        if (CommonUtility.isConnected(context)) {
            String debugUrl = generateDebugUrl(url, context, type, body);
            //log.debug(Logs.DEBUG, "HTTP_WORKER: url: " + debugUrl);

            Gson gson = new GsonBuilder()
                    .registerTypeAdapter(ShuttleMove.class, new GsonTypeAdapters.ShuttleMoveSerializer())
                    .registerTypeAdapter(Date.class, new GsonTypeAdapters.DateSerializer())
                    .create();
            String requestBody = "";
            try {
                requestBody = gson.toJson(new MessageWrapper(debugUrl, body, context), MessageWrapper.class);
                //log.debug(Logs.DEBUG, "HTTP_WORKER: JSON request: " + requestBody);
                log.debug(Logs.DEBUG, "HTTP_JSON_REQUEST: " + HelperFuncs.prettyJson(requestBody));
            } catch (Exception e) {
                e.printStackTrace();
            }

            // Submit the request to WorkManager
            HttpRequest httpRequest = new HttpRequest(debugUrl, requestBody, type.toString(), id);
            WorkRequest httpCallWorkRequest = WMHelperWorker.encodeOneTimeWMRequest(context, httpRequest, HttpRequest.class);
            WorkManager.getInstance(context).enqueue(httpCallWorkRequest);
            List<WorkInfo> requestList = WMHelperWorker.getWMHelperRequests(context);
            if (requestList != null) {
                log.debug(Logs.DEBUG, "HTTP_WORKER: Enqueued new request ID: " + httpCallWorkRequest.getId());
                log.debug(Logs.DEBUG, "HTTP_WORKER: Incomplete requests:");
                int finished = 0;
                for (WorkInfo request : requestList) {
                    if (request.getState().isFinished()) {
                        finished++;
                        switch (request.getState()) {
                            case FAILED:
                            case CANCELLED:
                                log.debug(Logs.DEBUG, "HTTP_WORKER: Unsuccessful request: " + request.getId() + " Status: " + request.getState().toString());
                                break;

                            case SUCCEEDED:
                            default:
                                // No logging for for SUCCEEDED requests
                        }
                    }
                    else {
                        log.debug(Logs.DEBUG, "HTTP_WORKER: Uncompleted request: " + request.getId() + " Status: " + request.getState().toString());
                    }
                }
                log.debug(Logs.DEBUG, "HTTP_WORKER: Finished requests: " + finished + " out of " + requestList.size() + " total requests.");
                int maxFinishedWorkItems = AppSetting.WM_HELPER_FINISHED_WORK_MAX.getInt();
                if (maxFinishedWorkItems > 0 && requestList.size() > maxFinishedWorkItems) {
                    log.debug(Logs.DEBUG, "HTTP_WORKER: Pruning WorkManager work items");
                    WorkManager.getInstance(context).pruneWork();
                }
            }

            return true;
        } else {
            log.debug(Logs.DEBUG, "HTTP_WORKER: Not connected, not posting to : " + url);
            CommonUtility.uploadLogMessage("Not connected, not posting to : " + url);
            return false;
        }
    }


    private static List<WorkInfo> getWorkStatus(Context context, String tag) {
        try {
            return WorkManager.getInstance(context).getWorkInfosByTag(tag).get();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static boolean handleActionPost(Context context, String url, String body, String type, int id, boolean refetch) {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);

        HttpPost post;
        try {
            post = new HttpPost(url);
        } catch (IllegalArgumentException e) {
            log.debug(Logs.DEBUG, "HTTP_WORKER: Call failed due to incorrect arguments: " + e);
            return false;
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


        //area of concern?
        log.debug(Logs.DEBUG, "HTTP_WORKER: Sending HTTP request: " + url);
        log.debug(Logs.UPLOAD, "HTTP_WORKER: Sending HTTP request: " + url);
        if(body.length() > 2000) {
            log.debug(Logs.UPLOAD, "HTTP_WORKER: Request body (truncated): " + body.substring(0, 255) + "...");
        } else {
            CommonUtility.logJson(Logs.UPLOAD, "HTTP_WORKER: Request body", body);
        }

        int statusCode = -1;
        long requestMillis = 0;
        try {
            if (PoCUtils.isLambdaUrl(url)) {
                PoCUtils.logHttpRequest(url, body);
            }
            long requestStartTime = System.currentTimeMillis();
            HttpResponse response = CommonUtility.tryURL(AutoTranApplication.getAppContext(), client, post);
            requestMillis = System.currentTimeMillis() - requestStartTime;
            log.debug(Logs.DEBUG, "HTTP_WORKER: Returned from HTTP request: " + url);

            if (response != null) {
                statusCode = 0;
                if (response.getStatusLine() != null) {
                    statusCode = response.getStatusLine().getStatusCode();
                }
                result = "Status code: " + statusCode;
                switch (statusCode){
                    case 400:
                        result += " The server could not understand the request due the syntax error. The url you provided may not have been correct";
                        break;
                    case 401:
                        result += " You may need to authenticate in order to access this resource.";
                        break;
                    case 404:
                        result += " This specific resource that you are looking for cannot be found.";
                        break;
                    default:
                        InputStream in = response.getEntity().getContent();
                        Header contentEncoding = response.getFirstHeader("Content-Encoding");

                        if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {

                            //log.debug(Logs.DEBUG, "*********** using gzip encoding");
                            in = new GZIPInputStream(in);
                        } else {
                            //log.debug(Logs.DEBUG, "*********** NOT using gzip encoding");
                        }
                        result = ConvertStreamToString.convertStreamToString(in);
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
        log.debug(Logs.DEBUG, "HTTP_WORKER: HTTP request result: " + result);
        CommonUtility.logJson(Logs.UPLOAD, "HTTP_WORKER: Response to " + url, result);
        if (PoCUtils.isLambdaUrl(url)) {
            PoCUtils.logHttpResponse(url, statusCode, result);
            PoCUtils.logHttpCallStats(url, requestMillis, true);
            return true;
        }
        else {
            PoCUtils.logHttpCallStats(url, requestMillis, false);
        }

        Intent broadcastIntent = new Intent(context, UploadResultReceiver.class);
        broadcastIntent.setAction(ACTION_FINISHED);
        //broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        broadcastIntent.putExtra(EXTRA_OBJECT_ID, id);
        broadcastIntent.putExtra(EXTRA_TYPE, type);
        broadcastIntent.putExtra(EXTRA_BODY, result);
        /*
        if (PoCUtils.isLambdaUrl(url)) {
            PoCUtils.log("Logging response from handleActionPost BEFORE broadcast to UploadResultReceiver");
            PoCUtils.logHttpResponse(url, result);
            broadcastIntent.putExtra(EXTRA_LAMBDA_CALL, true);
            context.sendBroadcast(broadcastIntent);
            return true;
        } */
        context.sendBroadcast(broadcastIntent);

        if (AppSetting.POC_ECHO_TO_LAMBDA.getBoolean()
                && !StringUtils.substringBeforeLast(url, "/").equalsIgnoreCase(PoCUtils.LAMBDA_URL)) {
            String lamdbaEndpoint = StringUtils.substringBefore(StringUtils.substringAfterLast(url, "/"), ".json");
            String lambdaUrl = PoCUtils.LAMBDA_URL + lamdbaEndpoint;
            if (AppSetting.POC_ENDPOINTS_TO_ECHO.getAsTrimmedCsvSet().contains(lamdbaEndpoint)) {
                PoCUtils.log("POC_DEBUG: Echoing API call to AWS Lambda: " + lamdbaEndpoint);
                handleActionPost(context, lambdaUrl, body, type, id, refetch);
            }
        }
//log.debug(Logs.DEBUG, "HTTP_WORKER: WorkManager worker returned result for id " + id + ": " + result.substring(0, 255));
        return true;
    }

    private static class MessageWrapper {
        public String token, timestamp, timezone, driver_number, truck_number, version, serial;
        public Object data;

        public MessageWrapper(String HOST_URL, Object payload, Context context) throws UnsupportedEncodingException, PackageManager.NameNotFoundException {
            Long timestampLong = System.currentTimeMillis() / 1000;
            /*
            if (AppSetting.MIRROR_TO_LAMBDA.getBoolean()) {
                this.token = "DUMMY_TOKEN";
            }
            else {
                this.token = new TokenGenerator().getToken(HOST_URL, timestampLong);
            }
             */
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
