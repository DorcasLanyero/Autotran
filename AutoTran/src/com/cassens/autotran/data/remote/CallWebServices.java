package com.cassens.autotran.data.remote;

import static com.cassens.autotran.BuildConfig.AUTOTRAN_API_URL;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.preference.PreferenceManager;

import com.cassens.autotran.backendpoc.PoCUtils;
import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.ConvertStreamToString;
import com.cassens.autotran.Logs;
import com.cassens.autotran.constants.URLS;
import com.cassens.autotran.data.remote.requests.LoginRequestData;
import com.cassens.autotran.data.remote.requests.PollLoadsRequestData;
import com.cassens.autotran.data.remote.workers.HttpCallWorker;
import com.cassens.autotran.handlers.TruckNumberHandler;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.SimpleTimeStamp;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.TimeZone;
import java.util.zip.GZIPInputStream;

/**
 * Project : AUTOTRAN Description : CallWebServices class hit the url and take response from it
 *
 * @author Hemant Creation Date : 12-11-2013
 */
public class CallWebServices {
    private static final Logger log = LoggerFactory.getLogger(CallWebServices.class.getSimpleName());

    private static final boolean DEBUG = false;

    //called by upload async task
    public static String sendJson(String HOST_URL, List<NameValuePair> nameValuePairs, Context context) throws ClientProtocolException, IOException {
        //Log.d("narf", nameValuePairs.toString());
        String result = "";
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);

        Long timestamp = System.currentTimeMillis() / 1000;
        String token = new TokenGenerator().getToken(HOST_URL, timestamp);
        nameValuePairs.add(new BasicNameValuePair("token", token));
        nameValuePairs.add(new BasicNameValuePair("timestamp", timestamp + ""));
        nameValuePairs.add(new BasicNameValuePair("timezone", SimpleTimeStamp.getUtcTimeZoneCode()));

        nameValuePairs.add(new BasicNameValuePair("driverId", CommonUtility.getDriverNumber(context)));


        try {
            nameValuePairs.add(new BasicNameValuePair("version", context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName));
            HOST_URL += "?version=" + context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            HOST_URL += "?version=unknown";
            nameValuePairs.add(new BasicNameValuePair("version", "unknown"));
        }

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        if(prefs != null) {
            String currentDriverID = CommonUtility.getDriverNumber(context);
            String currentTruckID = TruckNumberHandler.getTruckNumber(context);
            String mac = prefs.getString("MAC_ADDRESS", "");

            char[] driverIdChars = currentDriverID.toCharArray();
            for(int i = 0; i < currentDriverID.length(); i++) {
                int character = driverIdChars[i];
                log.debug("character: " + character);
            }

            if (currentDriverID != null && currentTruckID != null) {
                HOST_URL += "&currentDriverID=" + currentDriverID + "&currentTruckID=" + currentTruckID;
            }

            if(mac != null && !mac.equals("")) {
                HOST_URL += "&mac_address=" + mac;
            }
        }
        try {
        HttpPost post = new HttpPost(HOST_URL);
        if (DEBUG) System.out.println("HOST_URL       " + HOST_URL);
        if (DEBUG) System.out.println("nameValuePairs   -> " + nameValuePairs.toString());
        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        post.addHeader("Accept-Encoding", "gzip");

        long requestStartTime = System.currentTimeMillis();
        HttpResponse response = CommonUtility.tryURL(context, client, post);
        PoCUtils.logHttpCallStats(HOST_URL, System.currentTimeMillis() - requestStartTime, false);

        if (response != null) {
            InputStream in = response.getEntity().getContent();

            Header contentEncoding = response.getFirstHeader("Content-Encoding");
            if (contentEncoding != null && contentEncoding.getValue().equalsIgnoreCase("gzip")) {

                if (DEBUG) System.out.println( "*********** using gzip encoding");
                in = new GZIPInputStream(in);
            } else {
                if (DEBUG) System.out.println("*********** NOT using gzip encoding");
            }

            result = ConvertStreamToString.convertStreamToString(in);
            if (DEBUG) log.debug(Logs.UPLOAD, "result: " + result);
            if (DEBUG) System.out.println( "result: " + result);

            if (AppSetting.POC_ECHO_TO_LAMBDA.getBoolean()) {
                String lambdaEndpoint = StringUtils.substringBefore(StringUtils.substringAfterLast(HOST_URL, "/"), ".json");
                String lambdaUrl = PoCUtils.LAMBDA_URL + lambdaEndpoint;
                if (AppSetting.POC_ENDPOINTS_TO_ECHO.getAsTrimmedCsvSet().contains(lambdaEndpoint)) {
                    boolean errorOnStart = false;
                    if (lambdaEndpoint.equals("poll_loads")) {
                        PollLoadsRequestData req = new PollLoadsRequestData(CommonUtility.getDriverNumber(context));
                        errorOnStart = ! HttpCallWorker.makeJsonRequest(context, lambdaUrl, req, PollLoadsRequestData.class, 0);
                    }
                    else if (lambdaEndpoint.equalsIgnoreCase("login")) {
                        LoginRequestData req = new LoginRequestData(CommonUtility.getDriverNumber(context));
                        errorOnStart = ! HttpCallWorker.makeJsonRequest(context, lambdaUrl, req, LoginRequestData.class, 0);
                    }
                    if (errorOnStart) {
                        PoCUtils.log("Did NOT start " + lambdaEndpoint);
                    }
                }
            }
        }
        } catch (IllegalArgumentException e) {
            log.debug("Call failed due to incorrect arguments: " + e);
        }

        //log.debug(Logs.UPLOAD, result);

        if (DEBUG)
            log.debug(Logs.UPLOAD, "************************");
        if (DEBUG)
            log.debug(Logs.UPLOAD, "Host URL :: " + HOST_URL);
        for (int i = 0; i < nameValuePairs.size(); i++) {
            log.debug(Logs.DEBUG, "---");

            if (DEBUG) {

                if (nameValuePairs.get(i) != null &&
                        nameValuePairs.get(i).getName() != null &&
                        nameValuePairs.get(i).getValue() != null) {
                    NameValuePair pair = nameValuePairs.get(i);
                    log.debug(Logs.DEBUG, "Logging " + pair.getName());
                    log.debug(Logs.UPLOAD, pair.getName() + "::" +
                            (pair.getValue().length() > 99 ?
                                    pair.getValue().substring(0, 100) :
                                    pair.getValue()));
                }

            }
        }
        if (DEBUG) {
            log.debug(Logs.UPLOAD, "##########################");
        }

        return result;
    }

    public static String sendJsonMultiPart(String HOST_URL, MultipartEntity multipartData) {
        String result = "";
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
        HttpResponse response;
        try {
            HttpPost post = new HttpPost(HOST_URL);
            if (DEBUG) System.out.println("HOST_URL       " + HOST_URL);
            if (DEBUG) System.out.println("nameValuePairs   -> " + multipartData.toString());
            //post.setEntity(new UrlEncodedFormEntity(multipartData));
            post.setEntity(multipartData);

            response = client.execute(post);

            if (response != null) {
                InputStream in = response.getEntity().getContent();
                result = ConvertStreamToString.convertStreamToString(in);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        if (DEBUG) System.out.println("************************");
        if (DEBUG) System.out.println("Host URL :: " + HOST_URL);
        /*for(int i = 0; i<multipartData.size(); i++)
        {
			if(DEBUG) System.out.println(nameValuePairs.get(i).getName()+"::"+nameValuePairs.get(i).getValue());
		}*/
        if (DEBUG) System.out.println("##########################");

        return result;
    }

    public static void sendActualJson(String HOST_URL, Object payload, Context context) throws ClientProtocolException, IOException {
        try {
            MessageWrapper httpPayload = new MessageWrapper(HOST_URL, payload,
                    CommonUtility.getDriverNumber(context), context.getPackageManager().getPackageInfo(context.getPackageName(), 0).versionName);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
    }

    private static class MessageWrapper {
        public String token, timestamp, timezone, driver_number, version;
        public Object data;

        public MessageWrapper(String HOST_URL, Object payload, String driver_number, String version) throws UnsupportedEncodingException {
            Long timestampLong = System.currentTimeMillis() / 1000;
            this.token = new TokenGenerator().getToken(HOST_URL, timestampLong);
            this.timestamp = timestampLong.toString();
            this.timezone = TimeZone.getDefault().getDisplayName(true, TimeZone.SHORT);
            this.driver_number = driver_number;
            this.version = version;
            this.data = payload;
        }
    }

    private static final String GOOGLE_PING_URL = "http://google.com";

    public static String pingTests() {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
        HttpGet request;
        HttpResponse response;
        String errorMessage = null;

        // First attempt to communicate with the autotran server.
        request = new HttpGet(AUTOTRAN_API_URL + "images/logo.png");
        try {
            response = client.execute(request);

            log.debug(Logs.DEBUG, "response='" + response.toString() + "'");
            if (response != null)
                log.debug(Logs.DEBUG, "pingTests(): statusLine='" + response.getStatusLine() + "'");

            if (response == null || response.getEntity() == null) {
                errorMessage = new String("Cannot connect to AutoTran server: Null response");
            } else if (response.getStatusLine().getStatusCode() == 200) {
                // If we could communicate with the server, return success.
                log.debug(Logs.DEBUG, "pingTests(): Got valid response from server: " + AUTOTRAN_API_URL);
                return null;
            } else {
                response.getEntity().consumeContent();
                errorMessage = new String("Error communicating with AutoTran server:\n\n"
                        + response.getStatusLine().toString());
            }
        } catch (ClientProtocolException cpe) {
            errorMessage = new String("Http Protocol Error: " + cpe.toString());
        } catch (IOException ioe) {
            log.debug(Logs.DEBUG, "pingTests(): Got Exception from server: " + ioe.toString());
            errorMessage = new String("Got I/O Exception from AutoTran server:\n\n" + ioe.toString());
        } catch (Exception e){
            log.debug("Web Service call failed: " + e.toString());
            errorMessage = new String("Got I/O Exception from AutoTran server:\n\n" + e.toString());
        }


        // Check whether there are generic network problems by attempting to ping google.
        request = new HttpGet(GOOGLE_PING_URL);
        try {
            response = client.execute(request);

            if (response == null || response.getEntity() == null) {
                errorMessage = new String("NULL response from server");
            } else if (response.getStatusLine().getStatusCode() == 200) {
                // If google ping succeeded, the problem is with the Autotran server.  Return that message.
                log.debug(Logs.DEBUG, "google ping succeeded");
                return errorMessage;
            } else {
                errorMessage = new String(response.getStatusLine().toString());
            }
        } catch (ClientProtocolException cpe) {
            errorMessage = new String("Http Protocol Error: " + cpe.toString());
        } catch (IOException ioe) {
            log.debug(Logs.DEBUG, "pingTests(): Got Exception from server: " + ioe.toString());
            errorMessage = new String("Got I/O exception:\n" + ioe.toString());
        }

        return new String("Network connectivity appears to be down.\n\n" + errorMessage);
    }


    public static long downloadTests() {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
        HttpGet request;
        HttpResponse response;
        String errorMessage = null;

        // First attempt to communicate with the autotran server.
        request = new HttpGet(URLS.download_diagnostic_data);
        try {
            response = client.execute(request);

            if (response != null) {
                log.debug(Logs.DEBUG, "response='" + response.toString() + "'");
                log.debug(Logs.DEBUG, "downloadTests(): statusLine='" + response.getStatusLine() + "'");
            }

            if (response == null || response.getEntity() == null) {
                errorMessage = new String("Cannot connect to AutoTran server: Null response");
            } else if (response.getStatusLine().getStatusCode() == 200) {
                // If we could communicate with the server, return success.
                log.debug(Logs.DEBUG, "downloadTests(): Got valid response from server");
                return response.getEntity().getContentLength();
            } else {
                response.getEntity().consumeContent();
                errorMessage = new String("Error communicating with AutoTran server:\n\n"
                        + response.getStatusLine().toString());
            }
        } catch (ClientProtocolException cpe) {
            errorMessage = new String("Http Protocol Error: " + cpe.toString());
        } catch (IOException ioe) {
            log.debug(Logs.DEBUG, "downloadTests(): Got Exception from server: " + ioe.toString());
            errorMessage = new String("Got I/O Exception from AutoTran server:\n\n" + ioe.toString());
        }


        return 0;
    }

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ";
    static Random rnd = new Random();

    static String randomString( int len ){
       StringBuilder sb = new StringBuilder( len );
       for( int i = 0; i < len; i++ )
          sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
       return sb.toString();
    }

    public static long uploadTests() {
        HttpClient client = new DefaultHttpClient();
        HttpConnectionParams.setConnectionTimeout(client.getParams(), 10000);
        HttpResponse response;

        Long timestamp = System.currentTimeMillis() / 1000;

        List<NameValuePair> nameValuePairs = new ArrayList<>();

        int length = 3400;

        nameValuePairs.add(new BasicNameValuePair("test", randomString(length)));


        try {
        HttpPost post = new HttpPost(URLS.upload_diagnostic_data);
        post.setEntity(new UrlEncodedFormEntity(nameValuePairs));

        post.addHeader("Accept-Encoding", "gzip");


            response = client.execute(post);

            if (DEBUG)
                log.debug(Logs.UPLOAD, "************************");
            if (DEBUG)
            for (int i = 0; i < nameValuePairs.size(); i++) {
                log.debug(Logs.DEBUG, "---");

                if (DEBUG) {

                    if (nameValuePairs.get(i) != null &&
                            nameValuePairs.get(i).getName() != null &&
                            nameValuePairs.get(i).getValue() != null) {
                        NameValuePair pair = nameValuePairs.get(i);
                        log.debug(Logs.DEBUG, "Logging " + pair.getName());
                        log.debug(Logs.UPLOAD, pair.getName() + "::" +
                                (pair.getValue().length() > 99 ?
                                        pair.getValue().substring(0, 100) :
                                        pair.getValue()));
                    }

                }
            }
            if (DEBUG)
                log.debug(Logs.UPLOAD, "##########################");

        }
        catch (IOException e) {
            e.printStackTrace();
        }

        return length;
    }

}
