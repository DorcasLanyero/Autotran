package com.cassens.autotran.backendpoc;

import static com.cassens.autotran.data.local.DataManager.getAllLoadsLazy;

import android.content.Context;

import com.cassens.autotran.CommonUtility;
import com.cassens.autotran.Logs;
import com.cassens.autotran.data.local.DataManager;
import com.cassens.autotran.data.model.Delivery;
import com.cassens.autotran.data.model.Load;
import com.cassens.autotran.data.model.User;
import com.cassens.autotran.data.remote.workers.HttpCallWorker;
import com.sdgsystems.app_config.AppSetting;
import com.sdgsystems.util.HelperFuncs;
import com.sdgsystems.util.SimpleStopwatch;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

/*
 * This class provides a wrapper for the Backend Proof-of-Concept logging. Because the class name
 * is included in logcat messages, using a special class makes it easier to filter out
 * POC-related messages in the logcat output.
 */
public class PoCUtils {
    private static final Logger loggerLog = LoggerFactory.getLogger(PoCUtils.class.getSimpleName());
    public static String LAMBDA_URL = "https://4kdavonrj6.execute-api.us-east-1.amazonaws.com/v1/";
    public static String UPLOAD_LOAD_URL = LAMBDA_URL + "upload_load";
    public static String SAVE_LOAD_URL = LAMBDA_URL + "save_load";
    public static String REPORT_TABLET_STATUS_URL = LAMBDA_URL + "report_tablet_status";

    public static boolean isLambdaUrl(String url) {
        return url.startsWith(LAMBDA_URL);
    }

    private static String getUrlEndpoint(String url) {
        String shortUrl = StringUtils.substringAfterLast(url, "/");
        shortUrl = StringUtils.substringBefore(shortUrl, "?");
        return StringUtils.substringBefore(shortUrl, ".json");
    }

    public static void logHttpRequest(String url, String json) {
        String shortUrl = getUrlEndpoint(url);
        String logMsg;
        if (AppSetting.POC_LOG_PRETTY_PRINT.getBoolean()) {
            logMsg = String.format("%s REQUEST data:\n%s",
                    shortUrl,
                    CommonUtility.formatJson(json));
            if (AppSetting.POC_LOG_ESCAPE_SPECIAL_CHARS.getBoolean()) {
                logMsg = CommonUtility.escapeWhitespaceControlChars(logMsg);
            }
        }
        else {
            logMsg = String.format("%s REQUEST data: %s", shortUrl, json);
        }
        log(logMsg);
    }

    public static void logHttpResponse(String url, int responseCode, String json) {
        String shortUrl = getUrlEndpoint(url);
        String logMsg;
        if (AppSetting.POC_LOG_PRETTY_PRINT.getBoolean()) {
            logMsg = String.format("POC_DEBUG: %s RESPONSE data:\n%s\nresponseCode=%d\n",
                    shortUrl,
                    CommonUtility.formatJson(json),
                    responseCode);
            if (AppSetting.POC_LOG_ESCAPE_SPECIAL_CHARS.getBoolean()) {
                logMsg = CommonUtility.escapeWhitespaceControlChars(logMsg);
            }
        }
        else {
            logMsg = String.format("POC_DEBUG: %s RESPONSE data: rc=%d %s", responseCode, shortUrl, json);
        }
        log(logMsg);
    }

    public static void logHttpCallStats(String url, long responseTime, boolean isLambda) {
        String shortUrl = getUrlEndpoint(url);
        if (AppSetting.POC_ECHO_TO_LAMBDA.getBoolean()
            && AppSetting.POC_ENDPOINTS_TO_ECHO.getAsTrimmedCsvSet().contains(shortUrl)) {
            log(String.format("CallStat|%s|%s|RequestMillis|%d",
                    getUrlEndpoint(url),
                    isLambda ? "Lambda" : "Prod",
                    responseTime));
        }
    }

    public static void log(String msg) {
        loggerLog.debug(Logs.BACKEND_POC, msg);
    }

    public static void sendLambdaUploadLoadRequest(Context context, Load load) {
        PoCLoad pocLoad = PoCLoad.convertFromV2(load);
        if (pocLoad == null) {
            return;
        }

        boolean started = HttpCallWorker.makeJsonRequest(context, UPLOAD_LOAD_URL, pocLoad, pocLoad.getClass(), load.load_id);
        if (!started) {
            log("Did NOT start upload for load " + load.loadNumber);
        }
        else {
            log("Started upload for load " + load.loadNumber);
            loggerLog.debug(Logs.DEBUG, "POC_DEBUG: Started upload for load " + pocLoad.loadNum);
        }
    }

    public static void sendLambdaSaveLoadRequest(Context context, Load load) {
        PoCLoad pocLoad = PoCLoad.convertFromV2(load);
        if (pocLoad == null) {
            return;
        }

        boolean started = HttpCallWorker.makeJsonRequest(context, SAVE_LOAD_URL, pocLoad, pocLoad.getClass(), load.load_id);
        if (!started) {
            log("Did NOT start upload for load " + load.loadNumber);
        }
        else {
            log("Started upload for load " + load.loadNumber);
            loggerLog.debug(Logs.DEBUG, "POC_DEBUG: Started upload for load " + pocLoad.loadNum);
        }
    }

    public static void sendLambdaReportTabletStatusRequest(Context context, PoCTabletStatus tabletStatus) {

        boolean started = HttpCallWorker.makeJsonRequest(context, REPORT_TABLET_STATUS_URL, tabletStatus, tabletStatus.getClass(), 0);
        if (!started) {
            log("Did NOT start request: " + REPORT_TABLET_STATUS_URL);
        }
        else {
            log("Started request:  " + REPORT_TABLET_STATUS_URL);
            loggerLog.debug(Logs.DEBUG, "POC_DEBUG: Started  " + REPORT_TABLET_STATUS_URL);
        }
    }

    public static PoCTabletStatus getTabletStatus(Context context) {
        SimpleStopwatch stopwatch = new SimpleStopwatch();
        stopwatch.startTimer();

        PoCTabletStatus tabletStatus = new PoCTabletStatus();

        tabletStatus.tabletId = CommonUtility.getDeviceSerial();
        tabletStatus.userId = CommonUtility.getDriverNumber(context);
        tabletStatus.numUsers = DataManager.driverCount(context);
        User driver = DataManager.getUserForDriverNumber(context, tabletStatus.userId);
        if (driver == null) {
            log("Failed to get tablet stats: Unable to determine current driver");
            tabletStatus.needsAttention = true;
            return tabletStatus;
        }
        List<Load> allLoads = getAllLoadsLazy(context, driver.user_id);

        Date oldestCompletedLoadDate = null;
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss z");
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        for (Load load : allLoads) {
            String loadNum = load.loadNumber;
            if (load.isChildLoad()) {
                // Skip child loads since delivery signatures are stored in parent delivery
                continue;
            }
            tabletStatus.loadStats.total++;

            Date oldestDelivDateThisLoad = null;

            boolean loadCompleted = true;
            boolean badDelivTimestamp = false;

            if (HelperFuncs.isNullOrEmpty(load.driverPreLoadSignatureSignedAt)) {
                // The load is not counted as in progress until preload signed
                continue;
            }

            for (Delivery delivery : load.deliveries) {
                String thisDelivDateString = delivery.dealerSignatureSignedAt;
                if (delivery.shuttleLoad || load.shuttleLoad) {
                    thisDelivDateString = delivery.driverSignatureSignedAt;
                }
                if (HelperFuncs.isNullOrEmpty(thisDelivDateString)) {
                    loadCompleted = false;
                    break;
                }
                try {
                    Date thisDelivDate = dateFormat.parse(thisDelivDateString + " UTC");
                    oldestDelivDateThisLoad = returnOldest(thisDelivDate, oldestDelivDateThisLoad);
                } catch (ParseException e) {
                    badDelivTimestamp = true;
                    continue;
                }
            }

            if (badDelivTimestamp) {
                log("Load " + load.loadNumber + ": Malformed delivery signature timestamp. Assuming signed.");
                tabletStatus.needsAttention = true;
            }

            if (loadCompleted) {
                tabletStatus.loadStats.completed++;
                oldestCompletedLoadDate = returnOldest(oldestCompletedLoadDate, oldestDelivDateThisLoad);
                try {
                    tabletStatus.loadStats.oldest = dateFormat.format(oldestCompletedLoadDate);
                } catch (Exception e) {
                    log("Malformed timestamp for oldest completed load.");
                }
            } else {
                tabletStatus.loadStats.inTransit++;
            }

        }
        try {
            tabletStatus.updateTime = dateFormat.format(new Date());
        } catch (Exception e) {
            tabletStatus.updateTime = "";
        }

        tabletStatus.performanceStats.tabletStatsQueryTime = stopwatch.stopTimer();
        tabletStatus.message = String.format("Query time: %.2f seconds", tabletStatus.performanceStats.tabletStatsQueryTime);
        return tabletStatus;
    }

    public static Date returnOldest(Date date1, Date date2) {
        if (date1 == null) {
            return date2;
        }
        if (date2 == null) {
            return date1;
        }
        return date1.before(date2) ? date1 : date2;
    }
}
